@echo off
REM Placeholder gradlew.bat - scaffold only. This file does NOT include gradle-wrapper.jar.
SETLOCAL
SET SCRIPT_DIR=%~dp0
IF EXIST "%SCRIPT_DIR%gradlew" (
  "%SCRIPT_DIR%gradlew" %*
  EXIT /B %ERRORLEVEL%
)
where gradle >nul 2>&1
IF %ERRORLEVEL% EQU 0 (
  echo Gradle found on PATH. Generating wrapper...
  gradle wrapper
  echo Run "%SCRIPT_DIR%gradlew.bat" build
  EXIT /B 0
) ELSE (
  echo Gradle is not installed on this machine.
  echo Please either:
  echo  1) Install Gradle and run: gradle wrapper
  echo  2) Run 'gradle wrapper' on another machine and copy gradle\wrapper\gradle-wrapper.jar into this project.
  echo See BUILD_INSTRUCTIONS.md for details.
  EXIT /B 1
)