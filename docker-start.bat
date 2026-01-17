@echo off
REM ============================================================
REM Distributed Telecom System - Docker Quick Start (Windows)
REM ============================================================

setlocal enabledelayedexpansion

echo ============================================================
echo  Distributed Telecom System - Docker Deployment
echo ============================================================
echo.

REM Check if Docker is installed
where docker >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker is not installed
    echo Please install Docker Desktop from https://docs.docker.com/desktop/install/windows-install/
    pause
    exit /b 1
)

REM Check if Docker Compose is available
docker compose version >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set COMPOSE_CMD=docker compose
) else (
    where docker-compose >nul 2>nul
    if %ERRORLEVEL% EQU 0 (
        set COMPOSE_CMD=docker-compose
    ) else (
        echo [ERROR] Docker Compose is not installed
        echo Please install Docker Compose
        pause
        exit /b 1
    )
)

REM Check if Docker daemon is running
docker info >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker daemon is not running
    echo Please start Docker Desktop
    pause
    exit /b 1
)

echo Docker environment verified successfully
echo.

REM Parse command line arguments
set ACTION=%1
if "%ACTION%"=="" set ACTION=up

if "%ACTION%"=="up" goto :start
if "%ACTION%"=="start" goto :start
if "%ACTION%"=="down" goto :stop
if "%ACTION%"=="stop" goto :stop
if "%ACTION%"=="restart" goto :restart
if "%ACTION%"=="logs" goto :logs
if "%ACTION%"=="build" goto :build
if "%ACTION%"=="clean" goto :clean
if "%ACTION%"=="status" goto :status
goto :usage

:start
echo [ACTION] Starting all services...
echo ------------------------------------------------------------
%COMPOSE_CMD% up --build -d
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to start services
    pause
    exit /b 1
)
echo.
echo Services started successfully!
echo.
echo Waiting for services to be ready...
timeout /t 5 /nobreak >nul
echo.
echo ============================================================
echo  Distributed Telecom System is Running!
echo ============================================================
echo  Dashboard (Frontend + Backend): http://localhost:8000
echo  API Documentation:              http://localhost:8000/docs
echo.
echo  View logs:    %COMPOSE_CMD% logs -f
echo  Stop:         %COMPOSE_CMD% down
echo  Restart:      %COMPOSE_CMD% restart
echo ============================================================
echo.

REM Open browser
start http://localhost:8000
goto :end

:stop
echo [ACTION] Stopping all services...
echo ------------------------------------------------------------
%COMPOSE_CMD% down
echo.
echo Services stopped successfully
goto :end

:restart
echo [ACTION] Restarting all services...
echo ------------------------------------------------------------
%COMPOSE_CMD% restart
echo.
echo Services restarted successfully
goto :end

:logs
echo [ACTION] Showing logs (Ctrl+C to exit)...
echo ------------------------------------------------------------
%COMPOSE_CMD% logs -f
goto :end

:build
echo [ACTION] Building all images...
echo ------------------------------------------------------------
%COMPOSE_CMD% build
echo.
echo Build completed successfully
goto :end

:clean
echo [ACTION] Cleaning up Docker resources...
echo ------------------------------------------------------------
%COMPOSE_CMD% down -v --rmi local
echo.
echo Cleanup completed
goto :end

:status
echo [ACTION] Checking service status...
echo ------------------------------------------------------------
%COMPOSE_CMD% ps
goto :end

:usage
echo Usage: %0 {up^|down^|restart^|logs^|build^|clean^|status}
echo.
echo Commands:
echo   up      - Build and start all services (default)
echo   down    - Stop and remove all services
echo   restart - Restart all services
echo   logs    - Show and follow logs
echo   build   - Build all Docker images
echo   clean   - Stop services and remove volumes/images
echo   status  - Show status of all services
goto :end

:end
pause
