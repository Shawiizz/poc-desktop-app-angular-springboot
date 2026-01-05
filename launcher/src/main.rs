//! Desktop Application Launcher
//!
//! Single portable executable with embedded Quarkus backend.
//! The backend self-terminates when this launcher process dies.

#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use std::env;
use std::fs;
use std::io::{Cursor, Write};
use std::net::TcpListener;
use std::path::PathBuf;
use std::process::{Child, Command, Stdio};
use std::time::Duration;
use tauri::{Emitter, Manager};
use zstd::stream::decode_all;

#[cfg(target_os = "windows")]
use std::os::windows::process::CommandExt;

/// Embedded backend binary - COMPRESSED with zstd (included at compile time)
#[cfg(target_os = "windows")]
const EMBEDDED_BACKEND_COMPRESSED: &[u8] = include_bytes!("../backend/desktop-backend.exe.zst");

#[cfg(target_os = "linux")]
const EMBEDDED_BACKEND_COMPRESSED: &[u8] = include_bytes!("../backend/desktop-backend.zst");

#[cfg(target_os = "macos")]
const EMBEDDED_BACKEND_COMPRESSED: &[u8] = include_bytes!("../backend/desktop-backend.zst");

/// Embedded backend hash (precalculated at build time)
const EMBEDDED_HASH: &str = include_str!("../backend/backend.hash");

/// Windows: CREATE_NO_WINDOW flag
#[cfg(target_os = "windows")]
const CREATE_NO_WINDOW: u32 = 0x08000000;

/// Application configuration
struct Config {
    app_id: &'static str,
    startup_timeout: Duration,
    health_check_interval: Duration,
    event_emit_max_attempts: u32,
    event_emit_interval: Duration,
}

const CONFIG: Config = Config {
    app_id: "desktop-app",
    startup_timeout: Duration::from_secs(30),
    health_check_interval: Duration::from_millis(100),
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

/// Find an available port by binding to port 0
fn find_available_port() -> Result<u16, String> {
    let listener = TcpListener::bind("127.0.0.1:0")
        .map_err(|e| format!("Failed to find available port: {}", e))?;
    let port = listener.local_addr()
        .map_err(|e| format!("Failed to get local address: {}", e))?
        .port();
    drop(listener);
    Ok(port)
}

/// Extract the embedded backend if needed (hash comparison)
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
        
        println!("Decompressing backend ({} bytes compressed)...", EMBEDDED_BACKEND_COMPRESSED.len());
        let decompressed = decode_all(Cursor::new(EMBEDDED_BACKEND_COMPRESSED))
            .map_err(|e| format!("Failed to decompress backend: {}", e))?;
        println!("Decompressed to {} bytes", decompressed.len());
        
        let mut file = fs::File::create(&dest_path)
            .map_err(|e| format!("Failed to create backend file: {}", e))?;
        file.write_all(&decompressed)
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

/// Start the backend process with the specified port
fn start_backend(backend_path: &PathBuf, port: u16) -> Result<Child, String> {
    let parent_pid = std::process::id().to_string();
    
    let mut cmd = Command::new(backend_path);
    cmd.env("TAURI_PARENT_PID", &parent_pid)
        .env("BACKEND_PORT", port.to_string())
        .stdout(Stdio::null())
        .stderr(Stdio::null());
    
    #[cfg(target_os = "windows")]
    cmd.creation_flags(CREATE_NO_WINDOW);
    
    let child = cmd.spawn()
        .map_err(|e| format!("Failed to start backend: {}", e))?;

    println!("Backend started with PID {}, port {}, parent PID: {}", child.id(), port, parent_pid);

    Ok(child)
}

/// Wait for the backend to be ready via health check
fn wait_for_backend_health(port: u16) -> bool {
    let url = format!("http://127.0.0.1:{}/api/hello", port);
    let start = std::time::Instant::now();

    println!("Waiting for backend health at {}...", url);

    while start.elapsed() < CONFIG.startup_timeout {
        match ureq::post(&url)
            .set("Content-Type", "text/plain")
            .timeout(Duration::from_secs(2))
            .send_string("health-check")
        {
            Ok(response) if response.status() == 200 => {
                println!("Backend is healthy! (took {:?})", start.elapsed());
                return true;
            }
            _ => {
                std::thread::sleep(CONFIG.health_check_interval);
            }
        }
    }

    eprintln!("Backend health check timed out after {:?}", CONFIG.startup_timeout);
    false
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
            // Find available port
            let port = find_available_port()?;
            println!("Allocated port: {}", port);

            // Extract and start backend
            let backend_path = extract_backend()?;
            println!("Starting backend on port {}...", port);
            let _child = start_backend(&backend_path, port)?;

            let app_handle = app.handle().clone();
            
            // Health check in background thread
            std::thread::spawn(move || {
                if wait_for_backend_health(port) {
                    emit_backend_ready(&app_handle, port);
                } else {
                    emit_backend_error(&app_handle);
                }
            });

            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("Error while running Tauri application");
}

/// Emit backend-ready event to the frontend
fn emit_backend_ready(app_handle: &tauri::AppHandle, port: u16) {
    println!("Emitting backend-ready with port {}...", port);
    
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
