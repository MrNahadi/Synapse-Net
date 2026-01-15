#!/bin/bash
# ============================================================
# Distributed Telecom System - Docker Quick Start
# ============================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "============================================================"
echo " Distributed Telecom System - Docker Deployment"
echo "============================================================"
echo ""

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}[ERROR] Docker is not installed${NC}"
    echo "Please install Docker from https://docs.docker.com/get-docker/"
    exit 1
fi

# Check if Docker Compose is available
if ! docker compose version &> /dev/null && ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}[ERROR] Docker Compose is not installed${NC}"
    echo "Please install Docker Compose from https://docs.docker.com/compose/install/"
    exit 1
fi

# Use docker compose or docker-compose based on availability
COMPOSE_CMD="docker compose"
if ! docker compose version &> /dev/null; then
    COMPOSE_CMD="docker-compose"
fi

# Check if Docker daemon is running
if ! docker info &> /dev/null; then
    echo -e "${RED}[ERROR] Docker daemon is not running${NC}"
    echo "Please start Docker Desktop or the Docker daemon"
    exit 1
fi

echo -e "${BLUE}Docker environment verified successfully${NC}"
echo ""

# Parse command line arguments
ACTION=${1:-up}

case $ACTION in
    up|start)
        echo "[ACTION] Starting all services..."
        echo "------------------------------------------------------------"
        $COMPOSE_CMD up --build -d
        echo ""
        echo -e "${GREEN}Services started successfully!${NC}"
        echo ""
        echo "Waiting for services to be ready..."
        sleep 5
        echo ""
        echo "============================================================"
        echo -e "${GREEN} Distributed Telecom System is Running!${NC}"
        echo "============================================================"
        echo -e " Dashboard Frontend: ${BLUE}http://localhost:5173${NC}"
        echo -e " Dashboard Backend:  ${BLUE}http://localhost:8000${NC}"
        echo -e " API Documentation:  ${BLUE}http://localhost:8000/docs${NC}"
        echo ""
        echo " View logs:    ${YELLOW}$COMPOSE_CMD logs -f${NC}"
        echo " Stop:         ${YELLOW}$COMPOSE_CMD down${NC}"
        echo " Restart:      ${YELLOW}$COMPOSE_CMD restart${NC}"
        echo "============================================================"
        echo ""
        
        # Try to open browser
        if command -v xdg-open &> /dev/null; then
            xdg-open http://localhost:5173 2>/dev/null &
        elif command -v open &> /dev/null; then
            open http://localhost:5173 2>/dev/null &
        fi
        ;;
        
    down|stop)
        echo "[ACTION] Stopping all services..."
        echo "------------------------------------------------------------"
        $COMPOSE_CMD down
        echo ""
        echo -e "${GREEN}Services stopped successfully${NC}"
        ;;
        
    restart)
        echo "[ACTION] Restarting all services..."
        echo "------------------------------------------------------------"
        $COMPOSE_CMD restart
        echo ""
        echo -e "${GREEN}Services restarted successfully${NC}"
        ;;
        
    logs)
        echo "[ACTION] Showing logs (Ctrl+C to exit)..."
        echo "------------------------------------------------------------"
        $COMPOSE_CMD logs -f
        ;;
        
    build)
        echo "[ACTION] Building all images..."
        echo "------------------------------------------------------------"
        $COMPOSE_CMD build
        echo ""
        echo -e "${GREEN}Build completed successfully${NC}"
        ;;
        
    clean)
        echo "[ACTION] Cleaning up Docker resources..."
        echo "------------------------------------------------------------"
        $COMPOSE_CMD down -v --rmi local
        echo ""
        echo -e "${GREEN}Cleanup completed${NC}"
        ;;
        
    status)
        echo "[ACTION] Checking service status..."
        echo "------------------------------------------------------------"
        $COMPOSE_CMD ps
        ;;
        
    *)
        echo "Usage: $0 {up|down|restart|logs|build|clean|status}"
        echo ""
        echo "Commands:"
        echo "  up      - Build and start all services (default)"
        echo "  down    - Stop and remove all services"
        echo "  restart - Restart all services"
        echo "  logs    - Show and follow logs"
        echo "  build   - Build all Docker images"
        echo "  clean   - Stop services and remove volumes/images"
        echo "  status  - Show status of all services"
        exit 1
        ;;
esac
