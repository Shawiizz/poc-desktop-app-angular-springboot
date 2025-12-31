# Desktop Application

Desktop application with Spring Boot backend (API) and Angular frontend, packaged with Tauri.

## Architecture

```
┌──────────────────────────────────────────────────┐
│            Single Executable (~94MB)             │
│  ┌────────────────────────────────────────────┐  │
│  │  Tauri (Rust)           TAURI_PARENT_PID   │  │
│  │  ┌────────────┐      ┌─────────────────┐   │  │
│  │  │  WebView   │ HTTP │  Backend API    │   │  │
│  │  │  (Native)  │◄────►│  (Spring Boot)  │   │  │
│  │  │  Angular   │      │  + Watchdog     │   │  │
│  │  └────────────┘      └─────────────────┘   │  │
│  └────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────┘
```

- **Backend**: Spring Boot compiled to native with GraalVM
- **Frontend**: Angular served directly by Tauri WebView
- **Launcher**: Tauri (Rust) with native WebView and embedded backend
- **Watchdog**: Backend auto-terminates when launcher dies (no orphan processes)

## Features

- Frameless window with custom titlebar
- Dynamic port allocation (no conflicts)
- Splash screen during backend startup
- Single portable executable
- Single-instance mode (configurable)
- Automatic backend cleanup on exit/crash

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Java | 21+ | Backend development |
| Node.js | 18+ | Frontend development |
| GraalVM | 21+ | Native backend compilation |
| Rust | 1.70+ | Tauri build |

## Development

### Backend only

```bash
./gradlew bootRun
```

Access: http://localhost:8080

### Frontend only

```bash
cd frontend
npm install
npm start
```

Access: http://localhost:4200

### Full development mode

Run both in separate terminals:

```bash
# Terminal 1 - Backend
./gradlew bootRun

# Terminal 2 - Frontend
cd frontend
npm start
```

## Production Build

### Quick build (frontend + Tauri only)

```bash
cd frontend
npm run build

cd ../launcher
cargo build --release
```

Output: `launcher/target/release/desktop-app.exe`

### Full build (with native backend)

Use the build script from x64 Native Tools Command Prompt:

```bash
build-app.bat
```

This will:
1. Build Angular frontend
2. Compile Spring Boot to native with GraalVM
3. Embed backend in Tauri executable
4. Create final portable exe

## Project Structure

```
desktop/
├── app.config.json        # Central configuration
├── favicon.ico            # Application icon (synced to frontend & launcher)
├── frontend/              # Angular frontend
│   └── src/app/
│       ├── splash/        # Loading screen
│       ├── home/          # Main content
│       └── titlebar/      # Custom window titlebar
├── backend/               # Spring Boot backend
│   └── main/java/sample/app/desktop/
│       ├── controller/    # REST API
│       └── config/        # CORS, Watchdog, etc.
└── launcher/              # Tauri (Rust)
    ├── src/main.rs        # Launcher + backend embedding
    └── backend/           # Native backend binary
```

## Configuration

Edit `app.config.json`:

```json
{
  "name": "Desktop App",
  "id": "desktop-app",
  "version": "1.0.0",
  "description": "Desktop application with Spring Boot and Angular",
  "singleInstance": true
}
```

| Option | Description |
|--------|-------------|
| `name` | Display name |
| `id` | Unique identifier (used for app data folder) |
| `version` | Semantic version |
| `singleInstance` | `true` = one instance max, `false` = allow multiple |

## Window Controls

The application uses a custom titlebar with:
- Drag to move window
- Minimize, maximize, close buttons
- Application icon and name
