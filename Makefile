.PHONY: help docker-build docker-up docker-down docker-restart docker-logs docker-clean local-build local-run test

# Default target
help:
	@echo "Distributed Telecom System - Make Commands"
	@echo ""
	@echo "Docker Commands:"
	@echo "  make docker-build    - Build all Docker images"
	@echo "  make docker-up       - Start all services"
	@echo "  make docker-down     - Stop all services"
	@echo "  make docker-restart  - Restart all services"
	@echo "  make docker-logs     - Show logs"
	@echo "  make docker-clean    - Clean up Docker resources"
	@echo ""
	@echo "Local Development Commands:"
	@echo "  make local-build     - Build Java and install dependencies"
	@echo "  make local-run       - Run the system locally"
	@echo "  make test            - Run all tests"
	@echo ""

# Docker commands
docker-build:
	@echo "Building Docker images..."
	docker compose build

docker-up:
	@echo "Starting services..."
	docker compose up -d
	@echo ""
	@echo "Services started!"
	@echo "Dashboard: http://localhost:5173"
	@echo "API: http://localhost:8000"

docker-down:
	@echo "Stopping services..."
	docker compose down

docker-restart:
	@echo "Restarting services..."
	docker compose restart

docker-logs:
	docker compose logs -f

docker-clean:
	@echo "Cleaning up Docker resources..."
	docker compose down -v --rmi local
	@echo "Cleanup complete"

# Local development commands
local-build:
	@echo "Building Java project..."
	mvn clean install -DskipTests
	@echo ""
	@echo "Installing Python dependencies..."
	cd python_simulation && pip install -r requirements.txt || pip3 install -r requirements.txt
	@echo ""
	@echo "Installing Dashboard dependencies..."
	cd dashboard && npm install --legacy-peer-deps
	@echo ""
	@echo "Build complete!"

local-run:
	@echo "Starting local development..."
	./build.sh

test:
	@echo "Running Java tests..."
	mvn test
	@echo ""
	@echo "Running Python tests..."
	cd python_simulation && python3 run_tests.py

# Quick shortcuts
up: docker-up
down: docker-down
logs: docker-logs
build: docker-build
clean: docker-clean
