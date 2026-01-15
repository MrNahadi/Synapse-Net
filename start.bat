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
    echo Python simulation will be skipped
    set PYTHON_AVAILABLE=0
) else (
    set PYTHON_AVAILABLE=1
)

REM Check for Node.js
where node >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [WARNING] Node.js is not installed or not in PATH
    echo Dashboard will be skipped
    set NODE_AVAILABLE=0
) else (
    set NODE_AVAILABLE=1
)

echo.
echo [STEP 1/5] Cleaning previous builds...
echo ------------------------------------------------------------
call mvn clean -q
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven clean failed
    exit /b 1
)
echo Clean completed.

echo.
echo [STEP 2/5] Building Java project...
echo ------------------------------------------------------------
call mvn install -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven build failed
    exit /b 1
)
echo Build completed successfully.

echo.
echo [STEP 3/5] Running Java tests...
echo ------------------------------------------------------------
call mvn test
if %ERRORLEVEL% neq 0 (
    echo [WARNING] Some Java tests failed
) else (
    echo Java tests completed successfully.
)

echo.
echo [STEP 4/5] Running Python simulation...
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
echo [STEP 5/5] Building Dashboard...
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
echo ============================================================
echo  Build and Run Complete!
echo ============================================================
echo.
echo To start the dashboard:
echo   1. Start backend: cd dashboard\backend ^&^& python main.py
echo   2. Start frontend: cd dashboard ^&^& npm run dev
echo   3. Open http://localhost:5173
echo.

endlocal
pause
