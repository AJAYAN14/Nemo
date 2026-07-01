@echo off
title Nemo Web Dashboard
echo ==============================================
echo        Starting Nemo Web Dashboard
echo ==============================================

set PATH=D:\nodejs;%PATH%

echo Starting browser...
start http://localhost:3000

echo.
echo [INFO] The dashboard will open in your default browser.
echo [INFO] Close this console window to stop the server.
echo.
echo ================== Server Logs ==================
echo.

call npm run dev

echo.
echo ==============================================
echo [ERROR] Server exited unexpectedly! Check the logs above.
echo ==============================================
pause
