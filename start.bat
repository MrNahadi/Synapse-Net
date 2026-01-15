@echo off
REM ============================================================
REM Distributed Telecom System - Windows Build & Run Script
REM ============================================================

setlocal enabledelayedexpansion

echo ============================================================
echo  Distributed Telecom System - Build and Run
echo ============================================================
echo.

REM Check for Java
where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java is not installed or not in PATH
    echo Please install Java 11 or higher
    exit /b 1
)

REM Auto-detect JAVA_HOME if not set
if "%JAVA_HOME%"=="" (
    echo [INFO] JAVA_HOME not set, attempting to auto-detect...
    for /f "tokens=*" %%i in ('where java') do (
        set "JAVA_PATH=%%i"
        goto :found_java
    )
    :found_java
    for %%i in ("!JAVA_PATH!") do set "JAVA_BIN=%%~dpi"
    for %%i in ("!JAVA_BIN!..") do set "JAVA_HOME=%%~fi"
    echo [INFO] JAVA_HOME set to: !JAVA_HOME!
)

REM Check for Maven
where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven is not installed or not in PATH
    echo Please install Maven 3.6 or higher
    exit /b 1
)

REM Check for Python
where python >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [WARNING] Python is not installed or not in PATH
    echo Python simulation and dashboard backend will be skipped
    set PYTHON_AVAILABLE=0
) else (
    set PYTHON_AVAILABLE=1
)

REM Check for Node.js
where node >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [WARNING] Node.js is not installed or not in PATH
    echo Dashboard frontend will be skipped
    set NODE_AVAILABLE=0
) else (
    set NODE_AVAILABLE=1
)

echo.
echo [STEP 1/6] Cleaning previous builds...
echo ------------------------------------------------------------
call mvn clean -q
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven clean failed
    exit /b 1
)
echo Clean completed.

echo.
echo [STEP 2/6] Building Java project...
echo ------------------------------------------------------------
call mvn install -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven build failed
    exit /b 1
)
echo Build completed successfully.

echo.
echo [STEP 3/6] Running Java tests...
echo ------------------------------------------------------------
call mvn test
if %ERRORLEVEL% neq 0 (
    echo [WARNING] Some Java tests failed
) else (
    echo Java tests completed successfully.
)

echo.
echo [STEP 4/6] Running Python simulation...
echo ------------------------------------------------------------
if %PYTHON_AVAILABLE%==1 (
    cd python_simulation
    python demo.py
    if %ERRORLEVEL% neq 0 (
        echo [WARNING] Python simulation encountered errors
    )
    cd ..
) else (
    echo Skipped - Python not available
)

echo.
echo [STEP 5/6] Building Dashboard...
echo ------------------------------------------------------------
if %NODE_AVAILABLE%==1 (
    cd dashboard
    if not exist node_modules (
        echo Installing dashboard dependencies...
        call npm install --legacy-peer-deps
    )
    echo Building dashboard...
    call npm run build
    if %ERRORLEVEL% neq 0 (
        echo [WARNING] Dashboard build encountered errors
    ) else (
        echo Dashboard built successfully.
    )
    cd ..
) else (
    echo Skipped - Node.js not available
)

echo.
echo [STEP 6/6] Starting Dashboard Services...
echo ------------------------------------------------------------
if %PYTHON_AVAILABLE%==1 if %NODE_AVAILABLE%==1 (
    echo Starting dashboard backend and frontend...
    echo.
    
    REM Create and activate virtual environment for backend
    cd dashboard\backend
    if not exist venv (
        echo Creating Python virtual environment...
        python -m venv venv
    )
    
    REM Install Python dependencies in virtual environment
    echo Installing Python dependencies...
    call venv\Scripts\activate.bat
    pip install -r requirements.txt -q 2>nul
    
    REM Start backend in a new window (with venv activated)
    echo Starting backend server on http://localhost:8000 ...
    start "Dashboard Backend" cmd /c "venv\Scripts\activate.bat && python main.py"
    cd ..\..
    
    REM Wait for backend to start
    timeout /t 3 /nobreak >nul
    
    REM Start frontend in a new window
    cd dashboard
    echo Starting frontend server on http://localhost:5173 ...
    start "Dashboard Frontend" cmd /c "npm run dev"
    cd ..
    
    REM Wait for frontend to start
    timeout /t 5 /nobreak >nul
    
    REM Open browser
    echo Opening dashboard in browser...
    start http://localhost:5173
    
    echo.
    echo ============================================================
    echo  Dashboard is running!
    echo ============================================================
    echo  Backend:  http://localhost:8000
    echo  Frontend: http://localhost:5173
    echo.
    echo  Close the "Dashboard Backend" and "Dashboard Frontend"
    echo  windows to stop the servers.
    echo ============================================================
) else (
    echo Skipped - Python or Node.js not available
    echo.
    echo ============================================================
    echo  Build Complete!
    echo ============================================================
)

echo.
endlocal
pause
