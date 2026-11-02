@echo off
setlocal
set "GRADLE_VERSION=9.5.0"
if "%GRADLE_USER_HOME%"=="" set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
set "INSTALL_DIR=%GRADLE_USER_HOME%\sample-pack-creator\gradle-%GRADLE_VERSION%"
set "GRADLE_BIN=%INSTALL_DIR%\gradle-%GRADLE_VERSION%\bin\gradle.bat"
set "GRADLE_ZIP=%INSTALL_DIR%\gradle-%GRADLE_VERSION%-bin.zip"

if not exist "%GRADLE_BIN%" (
    if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%GRADLE_ZIP%'; Expand-Archive -LiteralPath '%GRADLE_ZIP%' -DestinationPath '%INSTALL_DIR%' -Force"
    if errorlevel 1 exit /b 1
    del "%GRADLE_ZIP%"
)

call "%GRADLE_BIN%" -p "%~dp0" %*
exit /b %ERRORLEVEL%
