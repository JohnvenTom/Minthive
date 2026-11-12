@echo off
chcp 65001 >nul
rem MintHive 一键启动：双击本文件即可启动后端 + 用户端
rem 参数透传，例如：start-dev.cmd -WithAdmin / -Stop
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-dev.ps1" %*
pause
