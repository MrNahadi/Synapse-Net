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

# Cleanup function
cleanup() {
    echo ""
    echo -e "${YELLOW}Shutting down servers...${NC}"
    if [ ! -z "$BACKEND_PID" ]; then
        kill $BACKEND_PID 2>/dev/null || true
    fi
    if [ ! -z "$FRONTEND_PID" ]; then
        kill $FRONTEND_PID 2>/dev/null || true
    fi
    exit 0
}

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
    echo "Python simulation and dashboard backend will be skipped"
fi

# Check for Node.js
NODE_AVAILABLE=0
if command -v node &> /dev/null; then
    NODE_AVAILABLE=1
else
    echo -e "${YELLOW}[WARNING] Node.js is not installed or not in PATH${NC}"
    echo "Dashboard frontend will be skipped"
fi

echo ""
echo "[STEP 1/6] Cleaning previous builds..."
echo "------------------------------------------------------------"
mvn clean -q
echo -e "${GREEN}Clean completed.${NC}"

echo ""
echo "[STEP 2/6] Building Java project..."
echo "------------------------------------------------------------"
mvn install -DskipTests -q
echo -e "${GREEN}Build completed successfully.${NC}"

echo ""
echo "[STEP 3/6] Running Java tests..."
echo "------------------------------------------------------------"
if mvn test; then
    echo -e "${GREEN}Java tests completed successfully.${NC}"
else
    echo -e "${YELLOW}[WARNING] Some Java tests failed${NC}"
fi

echo ""
echo "[STEP 4/6] Running Python simulation..."
echo "------------------------------------------------------------"
if [ -n "$PYTHON_CMD" ]; then
    cd python_simulation
    $PYTHON_CMD demo.py || echo -e "${YELLOW}[WARNING] Python simulation encountered errors${NC}"
    cd ..
else
    echo "Skipped - Python not available"
fi

echo ""
echo "[STEP 5/6] Building Dashboard..."
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
echo "[STEP 6/6] Starting Dashboard Services..."
echo "------------------------------------------------------------"
if [ -n "$PYTHON_CMD" ] && [ "$NODE_AVAILABLE" -eq 1 ]; then
    echo "Starting dashboard backend and frontend..."
    echo ""
    
    # Set up cleanup trap
    trap cleanup SIGINT SIGTERM
    
    # Create and activate virtual environment for backend
    cd dashboard/backend
    if [ ! -d "venv" ]; then
        echo -e "${BLUE}Creating Python virtual environment...${NC}"
        $PYTHON_CMD -m venv venv
    fi
    
    # Activate virtual environment and install dependencies
    source venv/bin/activate
    echo -e "${BLUE}Installing Python dependencies...${NC}"
    pip install -r requirements.txt -q 2>/dev/null || pip install -r requirements.txt
    
    # Start backend in background
    echo -e "${BLUE}Starting backend server on http://localhost:8000 ...${NC}"
    $PYTHON_CMD main.py &
    BACKEND_PID=$!
    cd ../..
    
    # Wait for backend to start
    sleep 3
    
    # Start frontend in background
    cd dashboard
    echo -e "${BLUE}Starting frontend server on http://localhost:5173 ...${NC}"
    npm run dev &
    FRONTEND_PID=$!
    cd ..
    
    # Wait for frontend to start
    sleep 5
    
    # Open browser (platform-specific)
    echo -e "${BLUE}Opening dashboard in browser...${NC}"
    if command -v xdg-open &> /dev/null; then
        xdg-open http://localhost:5173 2>/dev/null &
    elif command -v open &> /dev/null; then
        open http://localhost:5173 2>/dev/null &
    else
        echo "Please open http://localhost:5173 in your browser"
    fi
    
    echo ""
    echo "============================================================"
    echo -e "${GREEN} Dashboard is running!${NC}"
    echo "============================================================"
    echo -e " Backend:  ${BLUE}http://localhost:8000${NC}"
    echo -e " Frontend: ${BLUE}http://localhost:5173${NC}"
    echo ""
    echo " Press Ctrl+C to stop the servers."
    echo "============================================================"
    echo ""
    
    # Wait for user to stop
    wait
else
    echo "Skipped - Python or Node.js not available"
    echo ""
    echo "============================================================"
    echo -e "${GREEN} Build Complete!${NC}"
    echo "============================================================"
fi
