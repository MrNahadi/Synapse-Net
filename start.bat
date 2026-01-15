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

echo.
echo [STEP 1/4] Cleaning previous builds...
echo ------------------------------------------------------------
call mvn clean -q
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven clean failed
    exit /b 1
)
echo Clean completed.

echo.
echo [STEP 2/4] Building Java project...
echo ------------------------------------------------------------
call mvn install -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven build failed
    exit /b 1
)
echo Build completed successfully.

echo.
echo [STEP 3/4] Running Java tests...
echo ------------------------------------------------------------
call mvn test
if %ERRORLEVEL% neq 0 (
    echo [WARNING] Some Java tests failed
) else (
    echo Java tests completed successfully.
)

echo.
echo [STEP 4/4] Running Python simulation...
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
echo ============================================================
echo  Build and Run Complete!
echo ============================================================
echo.

endlocal
pause
