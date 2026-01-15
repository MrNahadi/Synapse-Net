# Distributed Telecom Dashboard

Real-time monitoring dashboard for the Distributed Telecom System.

## Features

- Real-time node status monitoring (Edge1, Edge2, Core1, Core2, Cloud1)
- Metrics visualization (latency, throughput, CPU, memory)
- Transaction monitoring with status tracking
- Fault tolerance status and failover events
- Load balancing visualization with balance index gauge
- Interactive system topology view
- Dark/Light mode support

## Quick Start

### Frontend Only (Demo Mode)

```bash
cd dashboard
npm install
npm run dev
```

The dashboard will run with mock data at http://localhost:5173

### With Backend (Live Data)

1. Start the FastAPI backend:
```bash
cd dashboard/backend
pip install -r requirements.txt
python main.py
```

2. Start the frontend:
```bash
cd dashboard
npm install
npm run dev
```

## Tech Stack

- React 18 + TypeScript
- Tailwind CSS v4 with shadcn/ui design tokens
- Recharts for data visualization
- FastAPI backend with WebSocket support
- Connects to Python simulation for live data

## Architecture

```
dashboard/
├── backend/           # FastAPI server
│   ├── main.py       # API endpoints & WebSocket
│   └── requirements.txt
├── src/
│   ├── components/   # React components
│   │   ├── ui/       # Base UI components
│   │   ├── NodeCard.tsx
│   │   ├── MetricsChart.tsx
│   │   ├── TransactionList.tsx
│   │   ├── LoadBalanceGauge.tsx
│   │   ├── FailoverEvents.tsx
│   │   ├── SystemTopology.tsx
│   │   └── StatsCards.tsx
│   ├── hooks/        # Custom React hooks
│   ├── lib/          # Utilities
│   └── App.tsx       # Main application
└── index.html
```
