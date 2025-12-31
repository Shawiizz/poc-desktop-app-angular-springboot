//! Desktop Application Launcher
//!
//! Single portable executable with embedded Spring Boot backend.
//! The backend self-terminates when this launcher process dies.

#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use std::env;
use std::fs;
use std::io::{BufRead, BufReader, Write};
use std::path::PathBuf;
use std::process::{Command, Stdio};
use std::time::Duration;
use tauri::Manager;

#[cfg(target_os = "windows")]
use std::os::windows::process::CommandExt;

/// Embedded backend binary (included at compile time)
#[cfg(target_os = "windows")]
const EMBEDDED_BACKEND: &[u8] = include_bytes!("../backend/desktop-backend.exe");

#[cfg(target_os = "linux")]
const EMBEDDED_BACKEND: &[u8] = include_bytes!("../backend/desktop-backend");

#[cfg(target_os = "macos")]
const EMBEDDED_BACKEND: &[u8] = include_bytes!("../backend/desktop-backend");

/// Windows: CREATE_NO_WINDOW flag
#[cfg(target_os = "windows")]
const CREATE_NO_WINDOW: u32 = 0x08000000;

/// Application configuration
struct Config {
    app_id: &'static str,
    health_endpoint: &'static str,
    startup_timeout: Duration,
    health_check_interval: Duration,
}

const CONFIG: Config = Config {
    app_id: "desktop-app",
    health_endpoint: "/actuator/health",
    startup_timeout: Duration::from_secs(30),
    health_check_interval: Duration::from_millis(200),
};

/// Get the application data directory
fn get_app_data_dir() -> PathBuf {
    #[cfg(target_os = "windows")]
    {
        let appdata = env::var("LOCALAPPDATA").unwrap_or_else(|_| ".".to_string());
        PathBuf::from(appdata).join(CONFIG.app_id)
    }
    #[cfg(target_os = "macos")]
    {
        let home = env::var("HOME").unwrap_or_else(|_| ".".to_string());
        PathBuf::from(home)
            .join("Library")
            .join("Application Support")
            .join(CONFIG.app_id)
    }
    #[cfg(target_os = "linux")]
    {
        let home = env::var("HOME").unwrap_or_else(|_| ".".to_string());
        PathBuf::from(home).join(".local").join("share").join(CONFIG.app_id)
    }
}

/// Get the backend binary name for the current platform
fn get_backend_name() -> &'static str {
    #[cfg(target_os = "windows")]
    {
        "desktop-backend.exe"
    }
    #[cfg(not(target_os = "windows"))]
    {
        "desktop-backend"
    }
}

/// Extract the embedded backend if needed (based on hash comparison)
fn extract_backend() -> Result<PathBuf, String> {
    let app_dir = get_app_data_dir();
    fs::create_dir_all(&app_dir).map_err(|e| format!("Failed to create app dir: {}", e))?;

    let backend_name = get_backend_name();
    let dest_path = app_dir.join(backend_name);
    let hash_path = app_dir.join("backend.hash");

    let embedded_hash = format!("{:x}", md5::compute(EMBEDDED_BACKEND));

    let needs_extraction = if dest_path.exists() && hash_path.exists() {
        let stored_hash = fs::read_to_string(&hash_path).unwrap_or_default();
        stored_hash.trim() != embedded_hash
    } else {
        true
    };

    if needs_extraction {
        println!("Extracting backend (hash: {})...", &embedded_hash[..8]);
        
        let mut file = fs::File::create(&dest_path)
            .map_err(|e| format!("Failed to create backend file: {}", e))?;
        file.write_all(EMBEDDED_BACKEND)
            .map_err(|e| format!("Failed to write backend: {}", e))?;

        #[cfg(unix)]
        {
            use std::os::unix::fs::PermissionsExt;
            fs::set_permissions(&dest_path, fs::Permissions::from_mode(0o755))
                .map_err(|e| format!("Failed to set permissions: {}", e))?;
        }

        fs::write(&hash_path, &embedded_hash)
            .map_err(|e| format!("Failed to write hash file: {}", e))?;
    }

    Ok(dest_path)
}

/// Start the backend process with parent PID for watchdog
fn start_backend(backend_path: &PathBuf) -> Result<std::sync::mpsc::Receiver<u16>, String> {
    let parent_pid = std::process::id().to_string();
    
    let mut cmd = Command::new(backend_path);
    cmd.env("TAURI_PARENT_PID", &parent_pid)
        .stdout(Stdio::piped())
        .stderr(Stdio::null());
    
    #[cfg(target_os = "windows")]
    cmd.creation_flags(CREATE_NO_WINDOW);
    
    let mut child = cmd.spawn()
        .map_err(|e| format!("Failed to start backend: {}", e))?;

    println!("Backend started with PID {}, parent PID: {}", child.id(), parent_pid);

    let stdout = child.stdout.take()
        .ok_or_else(|| "Failed to capture stdout".to_string())?;
    
    let (tx, rx) = std::sync::mpsc::channel();
    
    std::thread::spawn(move || {
        let reader = BufReader::new(stdout);
        let mut port_sent = false;
        
        for line in reader.lines() {
            if let Ok(line) = line {
                if !port_sent && line.starts_with("BACKEND_PORT:") {
                    if let Some(port_str) = line.strip_prefix("BACKEND_PORT:") {
                        if let Ok(port) = port_str.trim().parse::<u16>() {
                            let _ = tx.send(port);
                            port_sent = true;
                            println!("Captured backend port: {}", port);
                        }
                    }
                }
            }
        }
        
        let _ = child.wait();
    });
    
    Ok(rx)
}

/// Wait for the backend port from stdout and verify it's healthy
fn wait_for_backend(port_rx: &std::sync::mpsc::Receiver<u16>) -> Option<u16> {
    let client = reqwest::blocking::Client::builder()
        .timeout(Duration::from_secs(2))
        .build()
        .unwrap();

    let start = std::time::Instant::now();

    let port = loop {
        if start.elapsed() > CONFIG.startup_timeout {
            return None;
        }
        
        match port_rx.recv_timeout(Duration::from_millis(100)) {
            Ok(port) => break port,
            Err(std::sync::mpsc::RecvTimeoutError::Timeout) => continue,
            Err(std::sync::mpsc::RecvTimeoutError::Disconnected) => return None,
        }
    };

    while start.elapsed() < CONFIG.startup_timeout {
        let health_url = format!("http://localhost:{}{}", port, CONFIG.health_endpoint);
        if let Ok(response) = client.get(&health_url).send() {
            if response.status().is_success() {
                return Some(port);
            }
        }
        std::thread::sleep(CONFIG.health_check_interval);
    }

    None
}

/// Read app.config.json to check singleInstance setting
fn is_single_instance_mode() -> bool {
    if let Ok(exe_path) = env::current_exe() {
        if let Some(exe_dir) = exe_path.parent() {
            let config_path = exe_dir.join("app.config.json");
            if let Ok(content) = fs::read_to_string(&config_path) {
                if let Ok(json) = serde_json::from_str::<serde_json::Value>(&content) {
                    return json.get("singleInstance").and_then(|v| v.as_bool()).unwrap_or(true);
                }
            }
        }
    }
    true
}

fn main() {
    let mut builder = tauri::Builder::default()
        .plugin(tauri_plugin_shell::init());
    
    if is_single_instance_mode() {
        builder = builder.plugin(tauri_plugin_single_instance::init(|app, _args, _cwd| {
            if let Some(window) = app.get_webview_window("main") {
                let _ = window.set_focus();
                let _ = window.unminimize();
            }
        }));
    }
    
    builder
        .setup(|app| {
            let backend_path = extract_backend()?;
            
            println!("Starting backend...");
            let port_rx = start_backend(&backend_path)?;

            let app_handle = app.handle().clone();
            
            std::thread::spawn(move || {
                println!("Waiting for backend port from stdout...");
                if let Some(port) = wait_for_backend(&port_rx) {
                    println!("Backend is ready on port {}!", port);
                    
                    for attempt in 0..20 {
                        if let Some(window) = app_handle.get_webview_window("main") {
                            let js = format!(
                                "localStorage.setItem('backend_port', '{}'); \
                                 localStorage.removeItem('backend_error'); \
                                 console.log('Backend port injected:', {});",
                                port, port
                            );
                            if window.eval(&js).is_ok() {
                                println!("Port injected successfully on attempt {}", attempt + 1);
                                break;
                            }
                        }
                        std::thread::sleep(Duration::from_millis(100));
                    }
                } else {
                    eprintln!("Backend failed to start!");
                    for _ in 0..20 {
                        if let Some(window) = app_handle.get_webview_window("main") {
                            if window.eval("localStorage.setItem('backend_error', 'true');").is_ok() {
                                break;
                            }
                        }
                        std::thread::sleep(Duration::from_millis(100));
                    }
                }
            });

            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("Error while running Tauri application");
}
