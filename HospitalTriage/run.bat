@echo off
setlocal enabledelayedexpansion
REM Compile and run without an IDE.
REM Usage: run.bat            -> Swing GUI
REM        run.bat --console  -> console menu
REM        run.bat --test     -> automated tests
REM        run.bat --demo     -> OOP concept demonstration
cd /d "%~dp0"
chcp 65001 >nul
if exist out rmdir /s /q out
mkdir out
set "ROOT=%CD%\"
(for /r src %%f in (*.java) do (
    set "p=%%f"
    set "p=!p:%ROOT%=!"
    echo !p:\=/!
)) > out\sources.txt
javac -encoding UTF-8 --release 17 -d out @out\sources.txt
if errorlevel 1 (echo Compile failed & pause & exit /b 1)
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out hospital.Main %*
