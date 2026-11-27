@echo off
setlocal
cd /d "%~dp0.."
echo Installing NTM Survival Energy Expansion from:
echo %~dp0
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0INSTALL-PATCH.ps1"
if errorlevel 1 (
  echo.
  echo Patch installation failed. Read the error above; your original files were backed up where possible.
  pause
  exit /b 1
)
echo.
echo Patch installed. Run the Gradle build from this project root.
pause
