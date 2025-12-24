@echo off
REM Build script for native compilation
REM Must be run from x64 Native Tools Command Prompt for VS

set GRAALVM_HOME=C:\GraalVM\graalvm-jdk-21.0.9+7.1
set JAVA_HOME=%GRAALVM_HOME%

echo Cleaning previous build...
call gradlew clean

echo.
echo Building Angular frontend...
call gradlew build

echo.
echo Compiling native backend...
call gradlew nativeCompile

echo.
echo Building launcher...
cd launcher
call bun install
call bun run build
cd ..

echo.
echo Build complete!
echo Output: launcher\dist\windows\desktop-app.exe
