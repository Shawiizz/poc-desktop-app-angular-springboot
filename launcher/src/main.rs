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
use tauri::{Emitter, Manager};

#[cfg(target_os = "windows")]
use std::os::windows::process::CommandExt;

/// Embedded backend binary (included at compile time)
#[cfg(target_os = "windows")]
const EMBEDDED_BACKEND: &[u8] = include_bytes!("../backend/desktop-backend.exe");

#[cfg(target_os = "linux")]
const EMBEDDED_BACKEND: &[u8] = include_bytes!("../backend/desktop-backend");

#[cfg(target_os = "macos")]
const EMBEDDED_BACKEND: &[u8] = include_bytes!("../backend/desktop-backend");

/// Embedded backend hash (precalculated at build time)
const EMBEDDED_HASH: &str = include_str!("../backend/backend.hash");

/// Windows: CREATE_NO_WINDOW flag
#[cfg(target_os = "windows")]
const CREATE_NO_WINDOW: u32 = 0x08000000;

/// Application configuration
struct Config {
    app_id: &'static str,
    startup_timeout: Duration,
    event_emit_max_attempts: u32,
    event_emit_interval: Duration,
}

const CONFIG: Config = Config {
    app_id: "desktop-app",
    startup_timeout: Duration::from_secs(30),
    event_emit_max_attempts: 50,
    event_emit_interval: Duration::from_millis(50),
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

/// Extract the embedded backend if needed (hash comparison with precalculated hash)
fn extract_backend() -> Result<PathBuf, String> {
    let app_dir = get_app_data_dir();
    fs::create_dir_all(&app_dir).map_err(|e| format!("Failed to create app dir: {}", e))?;

    let backend_name = get_backend_name();
    let dest_path = app_dir.join(backend_name);
    let hash_path = app_dir.join("backend.hash");

    let embedded_hash = EMBEDDED_HASH.trim();

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

        fs::write(&hash_path, embedded_hash)
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

/// Wait for the backend port from stdout (backend prints port only when fully ready)
fn wait_for_backend(port_rx: &std::sync::mpsc::Receiver<u16>) -> Option<u16> {
    let start = std::time::Instant::now();

    loop {
        if start.elapsed() > CONFIG.startup_timeout {
            return None;
        }
        
        match port_rx.recv_timeout(Duration::from_millis(100)) {
            Ok(port) => return Some(port),
            Err(std::sync::mpsc::RecvTimeoutError::Timeout) => continue,
            Err(std::sync::mpsc::RecvTimeoutError::Disconnected) => return None,
        }
    }
}

/// Embedded app configuration (included at compile time)
const EMBEDDED_CONFIG: &str = include_str!("../../app.config.json");

/// Load app.config.json (embedded at compile time)
fn load_app_config() -> Option<serde_json::Value> {
    serde_json::from_str(EMBEDDED_CONFIG).ok()
}

/// Check if single instance mode is enabled
fn is_single_instance_mode() -> bool {
    load_app_config()
        .and_then(|json| json.get("singleInstance").and_then(|v| v.as_bool()))
        .unwrap_or(true)
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
                match wait_for_backend(&port_rx) {
                    Some(port) => emit_backend_ready(&app_handle, port),
                    None => emit_backend_error(&app_handle),
                }
            });

            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("Error while running Tauri application");
}

/// Emit backend-ready event to the frontend
fn emit_backend_ready(app_handle: &tauri::AppHandle, port: u16) {
    println!("Backend is ready on port {}!", port);
    
    for attempt in 0..CONFIG.event_emit_max_attempts {
        if let Some(window) = app_handle.get_webview_window("main") {
            if window.emit("backend-ready", serde_json::json!({ "port": port })).is_ok() {
                println!("Backend-ready event emitted on attempt {}", attempt + 1);
                return;
            }
        }
        std::thread::sleep(CONFIG.event_emit_interval);
    }
    eprintln!("Failed to emit backend-ready event after {} attempts", CONFIG.event_emit_max_attempts);
}

/// Emit backend-error event to the frontend
fn emit_backend_error(app_handle: &tauri::AppHandle) {
    eprintln!("Backend failed to start!");
    
    for _ in 0..CONFIG.event_emit_max_attempts {
        if let Some(window) = app_handle.get_webview_window("main") {
            if window.emit("backend-error", ()).is_ok() {
                return;
            }
        }
        std::thread::sleep(CONFIG.event_emit_interval);
    }
}
