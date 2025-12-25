@echo off
REM Build script for native compilation with Tauri
REM Must be run from x64 Native Tools Command Prompt for VS
REM Ensure cargo is in PATH or run: set PATH=%USERPROFILE%\.cargo\bin;%PATH%

set GRAALVM_HOME=C:\GraalVM\graalvm-jdk-21.0.9+7.1
set JAVA_HOME=%GRAALVM_HOME%
set PATH=%USERPROFILE%\.cargo\bin;%PATH%

echo ========================================
echo Step 0: Syncing configuration...
echo ========================================
call node sync-config.js
if errorlevel 1 (
    echo ERROR: Config sync failed!
    exit /b 1
)

echo.
echo ========================================
echo Step 1: Cleaning previous build...
echo ========================================
call gradlew clean
if errorlevel 1 (
    echo ERROR: Clean failed!
    exit /b 1
)

echo.
echo ========================================
echo Step 2: Building Angular frontend...
echo ========================================
call gradlew buildAngular
if errorlevel 1 (
    echo ERROR: Angular build failed!
    exit /b 1
)

echo.
echo ========================================
echo Step 3: Compiling native backend (API only)...
echo ========================================
call gradlew nativeCompile
if errorlevel 1 (
    echo ERROR: Native compile failed!
    exit /b 1
)

echo.
echo ========================================
echo Step 4: Copying backend to launcher...
echo ========================================
if not exist "launcher\backend" mkdir "launcher\backend"
copy /Y "build\native\nativeCompile\desktop-backend.exe" "launcher\backend\desktop-backend.exe"
if errorlevel 1 (
    echo ERROR: Backend copy failed!
    exit /b 1
)

echo.
echo ========================================
echo Step 5: Building Tauri application...
echo ========================================
cd launcher
call cargo build --release
if errorlevel 1 (
    echo ERROR: Tauri build failed!
    cd ..
    exit /b 1
)
cd ..

echo.
echo ========================================
echo Build complete!
echo ========================================
echo.
echo Angular frontend: bundled in Tauri (frontend/dist)
echo Backend API: embedded in exe (launcher/backend)
echo.
echo Output: launcher\target\release\desktop-app.exe
echo.
