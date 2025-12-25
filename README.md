# Desktop Application

Desktop application with Spring Boot backend (API) and Angular frontend, packaged with Tauri.

## Architecture

```
+--------------------------------------------------+
|            Single Executable (~94MB)             |
|  +--------------------------------------------+  |
|  |  Tauri (Rust)                              |  |
|  |  +------------+      +-----------------+   |  |
|  |  |  WebView   | HTTP |  Backend API    |   |  |
|  |  |  (Native)  |<---->|  (Spring Boot)  |   |  |
|  |  |  Angular   |      |  (Embedded)     |   |  |
|  |  +------------+      +-----------------+   |  |
|  +--------------------------------------------+  |
+--------------------------------------------------+
```

- **Backend**: Spring Boot compiled to native with GraalVM (API only)
- **Frontend**: Angular served directly by Tauri WebView
- **Launcher**: Tauri (Rust) with native WebView and embedded backend

## Features

- Frameless window with custom titlebar
- Dynamic port allocation (no conflicts)
- Splash screen during backend startup
- Single portable executable

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
npm run start
```

Access: http://localhost:4200

### Full development mode

Run both in separate terminals:

```bash
# Terminal 1 - Backend
./gradlew bootRun

# Terminal 2 - Frontend
cd frontend
npm run start
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
├── frontend/              # Angular frontend
│   ├── src/app/
│   │   ├── splash/        # Loading screen component
│   │   ├── home/          # Main content component
│   │   ├── titlebar/      # Custom window titlebar
│   │   └── services/      # Angular services
│   └── dist/              # Build output (embedded in Tauri)
├── backend/               # Spring Boot backend
│   └── main/java/sample/app/desktop/
│       ├── controller/    # REST API controllers
│       └── config/        # Configuration (CORS, etc.)
├── launcher/              # Tauri application (Rust)
│   ├── src/main.rs        # Rust launcher code
│   ├── backend/           # Embedded native backend
│   └── capabilities/      # Tauri permissions
└── app.config.json        # Application configuration
```

## Configuration

Edit `app.config.json` at the project root:

```json
{
  "name": "Desktop App",
  "id": "com.example.desktop-app",
  "version": "1.0.0",
  "description": "My desktop application"
}
```

## Window Controls

The application uses a custom titlebar with:
- Drag to move window
- Minimize, maximize, close buttons
- Application icon and name
