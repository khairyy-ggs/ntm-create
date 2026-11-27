@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0INSTALL-HOTFIX.ps1"
if errorlevel 1 (
  echo.
  echo Hotfix failed. Read the error above.
  pause
  exit /b 1
)
echo.
echo Hotfix installed. You can now run the Gradle build.
pause
