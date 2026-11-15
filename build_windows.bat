@echo off
setlocal EnableExtensions
chcp 65001 >nul
cd /d "%~dp0"
echo [HoYo Codes] Debug APK build

if not defined JAVA_HOME (
  if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
  )
)
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
where java >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Java 17+ required. Install Android Studio.
  goto :failure
)

if not defined ANDROID_HOME (
  if defined ANDROID_SDK_ROOT (
    set "ANDROID_HOME=%ANDROID_SDK_ROOT%"
  ) else (
    set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
  )
)
if not exist "%ANDROID_HOME%\platforms\android-35\android.jar" (
  echo [ERROR] Android SDK Platform 35 is missing.
  echo In Android Studio: Tools ^> SDK Manager ^> Android 15 / API 35.
  goto :failure
)

set "GRADLE_DIR=%~dp0.tools\gradle-8.9"
if not exist "%GRADLE_DIR%\bin\gradle.bat" (
  echo Downloading official Gradle 8.9 distribution (first build only)...
  if not exist ".tools" mkdir ".tools"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $u='https://services.gradle.org/distributions/gradle-8.9-bin.zip'; $p='.tools\\gradle-8.9-bin.zip'; Invoke-WebRequest -UseBasicParsing -Uri $u -OutFile $p; Expand-Archive -Force -LiteralPath $p -DestinationPath '.tools'; Remove-Item $p"
  if errorlevel 1 (
    echo [ERROR] Gradle download failed. Check internet connection.
    goto :failure
  )
)

call "%GRADLE_DIR%\bin\gradle.bat" --no-daemon assembleDebug
if errorlevel 1 goto :failure
if not exist "app\build\outputs\apk\debug\app-debug.apk" goto :failure
copy /y "app\build\outputs\apk\debug\app-debug.apk" "HoYoCodes-debug.apk" >nul

echo.
echo [SUCCESS] %~dp0HoYoCodes-debug.apk
pause
exit /b 0
:failure
echo.
echo [FAILED] See messages above. APK was not generated.
pause
exit /b 1
