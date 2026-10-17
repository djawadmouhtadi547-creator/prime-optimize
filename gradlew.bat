@echo off
set GRADLE_VERSION=8.7
set GRADLE_BIN=%USERPROFILE%\.gradle\prime-wrapper\gradle-%GRADLE_VERSION%\bin\gradle.bat
if not exist "%GRADLE_BIN%" (
  echo Please install Gradle 8.7 or run the build through GitHub Actions.
  exit /b 1
)
call "%GRADLE_BIN%" %*
