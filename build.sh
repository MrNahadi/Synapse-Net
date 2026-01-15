#!/bin/bash
# ============================================================
# Distributed Telecom System - Linux/macOS Build & Run Script
# ============================================================

set -e

echo "============================================================"
echo " Distributed Telecom System - Build and Run"
echo "============================================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check for Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}[ERROR] Java is not installed or not in PATH${NC}"
    echo "Please install Java 11 or higher"
    exit 1
fi

# Check for Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}[ERROR] Maven is not installed or not in PATH${NC}"
    echo "Please install Maven 3.6 or higher"
    exit 1
fi

# Check for Python
PYTHON_CMD=""
if command -v python3 &> /dev/null; then
    PYTHON_CMD="python3"
elif command -v python &> /dev/null; then
    PYTHON_CMD="python"
else
    echo -e "${YELLOW}[WARNING] Python is not installed or not in PATH${NC}"
    echo "Python simulation will be skipped"
fi

# Check for Node.js
NODE_AVAILABLE=0
if command -v node &> /dev/null; then
    NODE_AVAILABLE=1
else
    echo -e "${YELLOW}[WARNING] Node.js is not installed or not in PATH${NC}"
    echo "Dashboard will be skipped"
fi

echo ""
echo "[STEP 1/5] Cleaning previous builds..."
echo "------------------------------------------------------------"
mvn clean -q
echo -e "${GREEN}Clean completed.${NC}"

echo ""
echo "[STEP 2/5] Building Java project..."
echo "------------------------------------------------------------"
mvn install -DskipTests -q
echo -e "${GREEN}Build completed successfully.${NC}"

echo ""
echo "[STEP 3/5] Running Java tests..."
echo "------------------------------------------------------------"
if mvn test; then
    echo -e "${GREEN}Java tests completed successfully.${NC}"
else
    echo -e "${YELLOW}[WARNING] Some Java tests failed${NC}"
fi

echo ""
echo "[STEP 4/5] Running Python simulation..."
echo "------------------------------------------------------------"
if [ -n "$PYTHON_CMD" ]; then
    cd python_simulation
    $PYTHON_CMD demo.py || echo -e "${YELLOW}[WARNING] Python simulation encountered errors${NC}"
    cd ..
else
    echo "Skipped - Python not available"
fi

echo ""
echo "[STEP 5/5] Building Dashboard..."
echo "------------------------------------------------------------"
if [ "$NODE_AVAILABLE" -eq 1 ]; then
    cd dashboard
    if [ ! -d "node_modules" ]; then
        echo "Installing dashboard dependencies..."
        npm install --legacy-peer-deps
    fi
    echo "Building dashboard..."
    if npm run build; then
        echo -e "${GREEN}Dashboard built successfully.${NC}"
    else
        echo -e "${YELLOW}[WARNING] Dashboard build encountered errors${NC}"
    fi
    cd ..
else
    echo "Skipped - Node.js not available"
fi

echo ""
echo "============================================================"
echo -e "${GREEN} Build and Run Complete!${NC}"
echo "============================================================"
echo ""
echo -e "${BLUE}To start the dashboard:${NC}"
echo "  1. Start backend: cd dashboard/backend && python main.py"
echo "  2. Start frontend: cd dashboard && npm run dev"
echo "  3. Open http://localhost:5173"
echo ""
