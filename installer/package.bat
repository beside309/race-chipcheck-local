@echo off
chcp 936 >nul 2>nul
echo ========================================
echo RaceChipCheck Offline - Windows Packager
echo Version: 1.0.2
echo ========================================
echo.

REM Use system Java
set JAVA_HOME=%JAVA_HOME%

REM Check Java
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo Error: Java not found. Please install Java 17+
    pause
    exit /b 1
)

echo Step 1: Clean previous build...
if exist target rmdir /s /q target
if exist installer\output rmdir /s /q installer\output

echo.
echo Step 2: Maven compile and package...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo Error: Maven packaging failed
    pause
    exit /b 1
)

echo.
echo Step 3: Create Windows installer with jpackage...
jpackage --version >nul 2>nul
if %errorlevel% neq 0 (
    echo Error: jpackage not found. Please use Java 17+
    pause
    exit /b 1
)

REM Create output dir
if not exist installer\output mkdir installer\output

REM Run jpackage
jpackage ^
  --input target ^
  --name "RaceChipCheckOffline" ^
  --main-jar race-chipcheck-offline-1.0.2.jar ^
  --main-class com.race.chipcheck.RaceChipCheckApp ^
  --type exe ^
  --dest installer\output ^
  --app-version 1.0.2 ^
  --description "RaceChipCheck Offline" ^
  --vendor "iNRace System" ^
  --copyright "Copyright 2026" ^
  --win-dir-chooser ^
  --win-menu ^
  --win-shortcut

if %errorlevel% neq 0 (
    echo Error: jpackage failed
    pause
    exit /b 1
)

echo.
echo ========================================
echo Done!
echo Installer: installer\output\
echo ========================================
pause
