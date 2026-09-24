@echo off
cd /d "%~dp0"
if not exist bin mkdir bin
echo Compiling ASTRO 3D ...
javac -encoding UTF-8 -d bin src\astro3d\*.java src\astro3d\core\*.java src\astro3d\model\*.java src\astro3d\scene\*.java src\astro3d\ui\*.java
if errorlevel 1 (
  echo.
  echo Build failed. Make sure JDK 17+ is installed.
  pause
  exit /b 1
)
start "" javaw -Dfile.encoding=UTF-8 -cp bin astro3d.Main
