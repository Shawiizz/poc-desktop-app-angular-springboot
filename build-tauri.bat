@echo off
REM Build script for native compilation with Tauri
REM Must be run from x64 Native Tools Command Prompt for VS
REM Ensure cargo is in PATH or run: set PATH=%USERPROFILE%\.cargo\bin;%PATH%

set GRAALVM_HOME=C:\GraalVM\graalvm-jdk-21.0.9+7.1
set JAVA_HOME=%GRAALVM_HOME%
set PATH=%USERPROFILE%\.cargo\bin;%PATH%

echo ========================================
echo Step 1: Cleaning previous build...
echo ========================================
call gradlew clean

echo.
echo ========================================
echo Step 2: Building Angular frontend...
echo ========================================
call gradlew buildAngular

echo.
echo ========================================
echo Step 3: Compiling native backend (API only)...
echo ========================================
call gradlew nativeCompile

echo.
echo ========================================
echo Step 4: Copying backend to Tauri...
echo ========================================
if not exist "src-tauri\backend" mkdir "src-tauri\backend"
copy /Y "build\native\nativeCompile\desktop-backend.exe" "src-tauri\backend\desktop-backend.exe"

echo.
echo ========================================
echo Step 5: Building Tauri application...
echo ========================================
cd src-tauri
call cargo build --release
cd ..

echo.
echo ========================================
echo Build complete!
echo ========================================
echo.
echo Angular frontend: bundled in Tauri (frontend/dist)
echo Backend API: embedded in exe (src-tauri/backend)
echo.
echo Output: src-tauri\target\release\desktop-app.exe
echo.
