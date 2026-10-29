@echo off
rem Launcher logic for Windows. Start.bat and Manage.bat call this.
rem Usage: run.bat [start|stop|update|email|reset|logs|menu]

setlocal EnableExtensions DisableDelayedExpansion
cd /d "%~dp0"

set "APP_DIR=%~dp0"
if "%APP_DIR:~-1%"=="\" set "APP_DIR=%APP_DIR:~0,-1%"
set "WIZARD_IMAGE=curlimages/curl:8.23.0"
set "LOG_FILE=%TEMP%\appointment-system-start.log"
set "ACTION=%~1"
if "%ACTION%"=="" set "ACTION=start"

if /i "%ACTION%"=="start"  call :do_start  & goto :finish
if /i "%ACTION%"=="stop"   call :do_stop   & goto :finish
if /i "%ACTION%"=="update" call :do_update & goto :finish
if /i "%ACTION%"=="email"  call :do_email  & goto :finish
if /i "%ACTION%"=="reset"  call :do_reset  & goto :finish
if /i "%ACTION%"=="logs"   call :do_logs   & goto :finish
if /i "%ACTION%"=="menu"   call :do_menu   & goto :finish
echo Unknown action: %ACTION%
exit /b 2

:finish
set "RC=%ERRORLEVEL%"
if exist "%LOG_FILE%" del "%LOG_FILE%" >nul 2>&1
exit /b %RC%

rem ---------------------------------------------------------------- helpers

:read_env
rem read_env NAME  -> sets NAME from .env, without the surrounding quotes
set "%~1="
if not exist ".env" exit /b 0
for /f "usebackq tokens=1,* delims==" %%A in (".env") do if "%%A"=="%~1" set "%~1=%%B"
call set "%~1=%%%~1:'=%%"
exit /b 0

:sleep3
ping -n 4 127.0.0.1 >nul
exit /b 0

:ensure_docker
where docker >nul 2>&1
if errorlevel 1 goto :docker_missing
docker info >nul 2>&1
if not errorlevel 1 goto :docker_ready
if not exist "%ProgramFiles%\Docker\Docker\Docker Desktop.exe" goto :docker_stopped
echo Starting Docker Desktop. This can take a minute or two...
start "" "%ProgramFiles%\Docker\Docker\Docker Desktop.exe"
set /a WAITED=0
:wait_docker
call :sleep3
docker info >nul 2>&1
if not errorlevel 1 goto :docker_ready
set /a WAITED+=1
if %WAITED% LSS 60 goto :wait_docker
:docker_stopped
echo Docker is not running.
echo Please start Docker Desktop, wait until it says it is running, then try again.
exit /b 1
:docker_missing
echo Docker is not installed on this computer.
echo Install Docker Desktop (it is free) from https://www.docker.com/products/docker-desktop/
echo then open it once and run this again.
start "" "https://www.docker.com/products/docker-desktop/"
exit /b 1
:docker_ready
docker compose version >nul 2>&1
if errorlevel 1 goto :docker_old
exit /b 0
:docker_old
echo This version of Docker is too old. Please update Docker Desktop and try again.
exit /b 1

:require_env
if exist ".env" exit /b 0
echo Nothing is set up yet. Use Start first.
exit /b 1

:run_wizard
rem Runs in a throwaway container so it behaves the same on every operating system.
docker run --rm -it --user root -v "%APP_DIR%:/bundle" --entrypoint sh %WIZARD_IMAGE% /bundle/setup.sh %*
exit /b %ERRORLEVEL%

:set_port
docker run --rm --user root -v "%APP_DIR%:/bundle" --entrypoint sh %WIZARD_IMAGE% /bundle/setup.sh --port %~1 >nul 2>&1
exit /b %ERRORLEVEL%

:diagnose
echo.
echo Something went wrong while starting.
findstr /i /c:"pull access denied" /c:"manifest unknown" /c:"no matching manifest" /c:"no such host" /c:"TLS handshake" /c:"i/o timeout" "%LOG_FILE%" >nul 2>&1
if not errorlevel 1 goto :diagnose_download
findstr /i /c:"unhealthy" "%LOG_FILE%" >nul 2>&1
if not errorlevel 1 goto :diagnose_unhealthy
type "%LOG_FILE%"
goto :diagnose_end
:diagnose_download
echo The application could not be downloaded. Check your internet connection and try again.
echo If it keeps happening, contact the person who gave you this package.
goto :diagnose_end
:diagnose_unhealthy
echo The system started but did not become ready. Its latest messages were:
echo.
docker compose logs --no-color --tail 25 api
echo.
echo If this mentions email settings, use Manage, then Change email settings.
:diagnose_end
echo.
echo To get help, use Manage, then Save a support log file, and send the file to your support contact.
exit /b 0

:start_stack
set /a TRIES=0
:start_try
docker compose up -d --wait --wait-timeout 600 >"%LOG_FILE%" 2>&1
if not errorlevel 1 exit /b 0
if %TRIES% GEQ 10 exit /b 1
findstr /i /c:"port is already allocated" /c:"address already in use" /c:"ports are not available" "%LOG_FILE%" >nul 2>&1
if errorlevel 1 exit /b 1
call :read_env APP_PORT
if "%APP_PORT%"=="" set "APP_PORT=8080"
set /a NEWPORT=%APP_PORT%+1
echo Port %APP_PORT% is used by another program, trying %NEWPORT% instead...
call :set_port %NEWPORT%
if errorlevel 1 exit /b 1
set /a TRIES+=1
goto :start_try

:mail_check
rem Sends a real verification email through the running system, as a customer would trigger it.
where curl >nul 2>&1
if errorlevel 1 exit /b 0
call :read_env ADMIN_EMAIL
call :read_env APP_PORT
echo.
echo Final check: sending a real verification email to %ADMIN_EMAIL% ...
set "STATUS="
set "PAYLOAD=%TEMP%\appointment-check.json"
>"%PAYLOAD%" echo {"name":"Setup","surname":"Check","email":"%ADMIN_EMAIL%","phone":"0000000000"}
for /f %%S in ('curl -s -o nul -w "%%{http_code}" -X POST "http://localhost:%APP_PORT%/api/auth/guests/code" -H "Content-Type: application/json" --data-binary "@%PAYLOAD%"') do set "STATUS=%%S"
del "%PAYLOAD%" >nul 2>&1
if "%STATUS%"=="202" goto :mail_sent
echo The system did not accept the test request, code %STATUS%. Use Manage, then Save a support log file.
exit /b 0
:mail_sent
echo It can take up to a minute to arrive.
choice /c YN /m "Did an email with a verification code arrive? Check Spam too"
if errorlevel 2 goto :mail_missing
echo Email is working.
exit /b 0
:mail_missing
echo.
echo Email did not arrive. Things to try:
echo   - wait another minute and check the Spam folder
echo   - use Manage, then Change email settings, and re-enter the details
echo   - Gmail needs an App Password, not your normal password. See README.txt, section 3.
exit /b 0

rem ---------------------------------------------------------------- actions

:do_start
call :ensure_docker
if errorlevel 1 exit /b 1
set "FIRST=0"
if exist ".env" goto :start_have_env
set "FIRST=1"
call :run_wizard
:start_have_env
if exist ".env" goto :start_pull
echo Setup was not completed. Run Start again to retry.
exit /b 1
:start_pull
echo.
echo Step 1 of 2: getting the application. First time only, this can take several minutes...
docker compose pull --ignore-pull-failures
echo.
echo Step 2 of 2: starting. The first start also prepares the database, so please be patient...
call :start_stack
if errorlevel 1 goto :start_failed
call :read_env APP_PORT
echo.
echo The system is running at http://localhost:%APP_PORT%
start "" "http://localhost:%APP_PORT%"
if "%FIRST%"=="1" call :mail_check
echo.
echo Keep Docker Desktop open while people use the system. To stop it, use Manage.
exit /b 0
:start_failed
call :diagnose
exit /b 1

:do_stop
call :require_env
if errorlevel 1 exit /b 1
call :ensure_docker
if errorlevel 1 exit /b 1
docker compose down
if errorlevel 1 exit /b 1
echo Stopped. Your data is kept. Use Start to run it again.
exit /b 0

:do_update
call :require_env
if errorlevel 1 exit /b 1
call :ensure_docker
if errorlevel 1 exit /b 1
echo Getting the latest version...
docker compose pull --ignore-pull-failures
call :start_stack
if errorlevel 1 goto :update_failed
docker image prune -f >nul 2>&1
call :read_env APP_PORT
echo Up to date and running at http://localhost:%APP_PORT%
exit /b 0
:update_failed
call :diagnose
exit /b 1

:do_email
call :require_env
if errorlevel 1 exit /b 1
call :ensure_docker
if errorlevel 1 exit /b 1
call :run_wizard --email
if errorlevel 1 exit /b 1
echo Restarting with the new email settings...
call :start_stack
if errorlevel 1 goto :email_failed
call :mail_check
exit /b 0
:email_failed
call :diagnose
exit /b 1

:do_reset
call :require_env
if errorlevel 1 exit /b 1
call :ensure_docker
if errorlevel 1 exit /b 1
echo This permanently erases ALL appointments, customers and accounts.
echo Your email settings are kept.
set "CONFIRM="
set /p "CONFIRM=Type RESET (in capitals) to continue, or press Enter to cancel: "
if "%CONFIRM%"=="RESET" goto :reset_go
echo Cancelled. Nothing was erased.
exit /b 0
:reset_go
docker compose down -v
if errorlevel 1 exit /b 1
echo All data erased. Use Start to begin with a fresh system.
exit /b 0

:do_logs
call :require_env
if errorlevel 1 exit /b 1
call :ensure_docker
if errorlevel 1 exit /b 1
docker compose logs --no-color --tail 500 >support-logs.txt 2>&1
echo Saved: %APP_DIR%\support-logs.txt
echo Send that file to your support contact. It does not contain your passwords.
exit /b 0

:do_menu
echo.
echo ===== Appointment System: Manage =====
echo.
echo   1  Stop the system
echo   2  Update to the latest version
echo   3  Change email settings
echo   4  Save a support log file
echo   5  Erase all data and start fresh
echo   0  Close
echo.
choice /c 123450 /n /m "Choose a number: "
if errorlevel 6 exit /b 0
if errorlevel 5 call :do_reset & goto :do_menu
if errorlevel 4 call :do_logs & goto :do_menu
if errorlevel 3 call :do_email & goto :do_menu
if errorlevel 2 call :do_update & goto :do_menu
if errorlevel 1 call :do_stop & goto :do_menu
exit /b 0
