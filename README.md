# Desktop Application

Desktop application with Spring Boot backend and Angular frontend, rendered in a native WebView.

## Architecture

```
+--------------------------------------------------+
|            Single Executable (~200MB)            |
|  +--------------------------------------------+  |
|  |  Launcher (Bun + WebView)                  |  |
|  |  +------------+      +-----------------+   |  |
|  |  |  WebView   | HTTP |  Backend        |   |  |
|  |  |  (Native)  |<---->|  (Spring Boot)  |   |  |
|  |  +------------+      +-----------------+   |  |
|  +--------------------------------------------+  |
+--------------------------------------------------+
```

- Backend: Spring Boot compiled to native with GraalVM
- Frontend: Angular bundled in backend static resources
- Launcher: Bun executable with native WebView (Edge/WebKit/GTK)

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Java | 21+ | Backend development |
| Node.js | 18+ | Frontend development |
| GraalVM | 21+ | Native compilation |
| Bun | 1.0+ | Launcher build |

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

Access: http://localhost:4200 (proxies API to backend)

### Full development mode

Run both in separate terminals:

```bash
# Terminal 1 - Backend
./gradlew bootRun

# Terminal 2 - Frontend
cd frontend
npm run start
```

Open http://localhost:4200 for hot-reload development.

## Build

### Development JAR

```bash
./gradlew build
```

Output: `build/libs/desktop-*.jar`

### Production (Native Executable)

```bash
# 1. Compile backend to native
./gradlew nativeCompile

# 2. Build launcher with embedded backend
cd launcher
bun install
bun run build
```

Output: `launcher/dist/windows/desktop-app.exe`

### Build targets

```bash
bun run build              # Current platform
bun run build:windows      # Windows x64
bun run build:linux        # Linux x64
bun run build:macos        # macOS x64
bun run build:macos-arm    # macOS ARM64
```

## Output Structure

```
launcher/dist/
  windows/
    desktop-app.exe    # Single file (~200MB)
  linux/
    desktop-app
  macos/
    desktop-app
```

## Runtime Data

The backend binary is extracted on first launch:

| Platform | Path |
|----------|------|
| Windows | `%APPDATA%\desktop-app\` |
| macOS | `~/Library/Application Support/desktop-app/` |
| Linux | `~/.local/share/desktop-app/` |

## Project Structure

```
desktop/
  build.gradle           # Backend build config
  src/
    main/
      java/              # Spring Boot backend
      resources/
        static/          # Angular build output
  frontend/
    src/                 # Angular source
  launcher/
    src/
      main.ts            # Launcher entry point
    scripts/
      build.ts           # Build script
```

## WebView Engines

| Platform | Engine |
|----------|--------|
| Windows | Edge WebView2 |
| macOS | WebKit |
| Linux | WebKitGTK |
