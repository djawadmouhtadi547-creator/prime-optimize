@echo off
setlocal
set GRADLE_VERSION=8.7
if "%GRADLE_USER_HOME%"=="" set GRADLE_USER_HOME=%USERPROFILE%\.gradle
set DIST_ROOT=%GRADLE_USER_HOME%\wrapper\dists\prime-optimize\gradle-%GRADLE_VERSION%-bin
set GRADLE_BIN=%DIST_ROOT%\gradle-%GRADLE_VERSION%\bin\gradle.bat
if not exist "%GRADLE_BIN%" (
  if not exist "%DIST_ROOT%" mkdir "%DIST_ROOT%"
  set ZIP=%DIST_ROOT%\gradle-%GRADLE_VERSION%-bin.zip
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $dest='%DIST_ROOT%'; $zip=Join-Path $dest 'gradle-%GRADLE_VERSION%-bin.zip'; if (!(Test-Path $zip)) { Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile $zip }; Expand-Archive -LiteralPath $zip -DestinationPath $dest -Force"
  if errorlevel 1 exit /b 1
)
call "%GRADLE_BIN%" %*
endlocal
