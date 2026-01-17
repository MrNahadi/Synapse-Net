# Docker Deployment Guide

Complete guide for running the Distributed Telecom System with Docker.

## Prerequisites

- Docker Engine 20.10+ or Docker Desktop
- Docker Compose 2.0+
- 4GB RAM available
- 10GB disk space

## Quick Start

### Start Everything

```bash
# Linux/macOS
./docker-start.sh

# Windows
docker-start.bat

# Or manually
docker-compose up --build
```

### Access the Services

- **Dashboard Frontend**: http://localhost:5173
- **Dashboard Backend API**: http://localhost:8000
- **API Documentation**: http://localhost:8000/docs

### Stop the Services

```bash
docker-compose down
```

## Common Commands

```bash
# Start services
docker-compose up -d

# View logs
docker-compose logs -f

# View logs for specific service
docker-compose logs -f dashboard

# Restart a service
docker-compose restart dashboard

# Check service status
docker-compose ps

# Stop services
docker-compose down

# Rebuild and restart
docker-compose up --build
```

## Configuration

### Environment Variables

You can customize the deployment by setting environment variables in `docker-compose.yml`:

```yaml
environment:
  - JAVA_OPTS=-Xmx2g -Xms512m
  - BACKEND_HOST=0.0.0.0
  - BACKEND_PORT=8000
```

### Volume Mounts

Logs are persisted to the host:

```yaml
volumes:
  - ./logs:/app/logs
```

### Port Mapping

Default ports:
- `8000`: Dashboard backend API
- `5173`: Dashboard frontend
- `8080`: Core system (internal)

To change ports, modify the `docker-compose.yml`:

```yaml
ports:
  - "3000:8000"  # Map host port 3000 to container port 8000
```

## Development Workflow

### Live Development with Hot Reload

For frontend development with hot reload:

```bash
# Run backend in Docker
docker-compose up dashboard

# In another terminal, run frontend locally
cd dashboard
npm run dev
```

### Rebuild After Code Changes

```bash
# Rebuild specific service
docker-compose up --build dashboard

# Rebuild all services
docker-compose up --build
```

### View Logs

```bash
# View all logs
docker-compose logs

# Follow logs in real-time
docker-compose logs -f

# View logs for specific service
docker-compose logs -f dashboard
```

## Troubleshooting

### Port Already in Use

Change ports in `docker-compose.yml`:
```yaml
ports:
  - "3000:8000"  # Use port 3000 instead of 8000
```

### Out of Memory

Reduce Java heap size in `docker-compose.yml`:
```yaml
environment:
  - JAVA_OPTS=-Xmx1g -Xms256m
```

### Services Won't Start

Check logs:
```bash
docker-compose logs
```

Clean and rebuild:
```bash
docker-compose down -v
docker system prune -a
docker-compose up --build
```

### Can't Connect to Dashboard

1. Check if services are running: `docker-compose ps`
2. Check backend health: `curl http://localhost:8000/`
3. View logs: `docker-compose logs dashboard`

## Architecture

The Docker setup includes three services:

- **core**: Java distributed system (Maven + OpenJDK 11)
- **simulation**: Python load balancing simulation
- **dashboard**: React frontend + FastAPI backend

All services communicate over a Docker bridge network.

---

For issues, check logs with `docker-compose logs` or refer to the main [README.md](README.md).
