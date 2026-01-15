import { useState, useEffect, useCallback } from 'react';
import type { SystemMetrics, TransactionData, FailoverEvent } from '../types';

const API_BASE = '/api';

// Generate mock data for demo when backend is not available
function generateMockMetrics(): SystemMetrics {
    const baseMetrics = {
        EDGE1: { latency: 12, throughput: 500, cpu: 45, memory: 8.0, tx: 150, lock: 8 },
        EDGE2: { latency: 15, throughput: 470, cpu: 50, memory: 4.5, tx: 100, lock: 12 },
        CORE1: { latency: 8, throughput: 1000, cpu: 60, memory: 12.0, tx: 250, lock: 5 },
        CORE2: { latency: 10, throughput: 950, cpu: 55, memory: 10.0, tx: 200, lock: 10 },
        CLOUD1: { latency: 22, throughput: 1250, cpu: 72, memory: 16.0, tx: 300, lock: 15 },
    };

    const failureTypes: Record<string, string> = {
        EDGE1: 'Crash', EDGE2: 'Omission', CORE1: 'Byzantine', CORE2: 'Crash', CLOUD1: 'Omission'
    };

    const getLayer = (id: string) => id.startsWith('EDGE') ? 'Edge' : id.startsWith('CORE') ? 'Core' : 'Cloud';

    const nodes = Object.entries(baseMetrics).map(([id, base]) => {
        const variance = 0.9 + Math.random() * 0.2;
        const cpu = Math.min(95, base.cpu * variance);
        return {
            id,
            name: id,
            layer: getLayer(id) as 'Edge' | 'Core' | 'Cloud',
            status: (cpu < 60 ? 'healthy' : cpu < 75 ? 'warning' : 'critical') as 'healthy' | 'warning' | 'critical',
            latency: base.latency * (0.95 + Math.random() * 0.1),
            throughput: base.throughput * (0.95 + Math.random() * 0.1),
            cpu,
            memory: base.memory * (0.95 + Math.random() * 0.1),
            transactions: Math.floor(base.tx * (0.9 + Math.random() * 0.2)),
            lockContention: base.lock * (0.9 + Math.random() * 0.2),
            failureType: failureTypes[id]
        };
    });

    return {
        nodes,
        loadBalanceIndex: 0.82 + Math.random() * 0.13,
        totalServices: 80 + Math.floor(Math.random() * 40),
        activeTransactions: 50 + Math.floor(Math.random() * 100),
        failedNodes: 0,
        totalMigrations: 5 + Math.floor(Math.random() * 15)
    };
}

function generateMockTransactions(): TransactionData[] {
    const statuses: TransactionData['status'][] = ['committed', 'pending', 'preparing', 'aborted'];
    const allNodes = ['EDGE1', 'EDGE2', 'CORE1', 'CORE2', 'CLOUD1'];

    return Array.from({ length: 10 }, (_, i) => ({
        id: `TX-${1000 + Math.floor(Math.random() * 9000)}`,
        status: statuses[Math.floor(Math.random() * statuses.length)],
        nodes: allNodes.slice(0, 2 + Math.floor(Math.random() * 3)),
        startTime: new Date(Date.now() - i * 60000).toISOString(),
        duration: 0.01 + Math.random() * 0.49
    }));
}

function generateMockFailoverEvents(): FailoverEvent[] {
    const reasons = ['Node crash detected', 'Omission failure', 'Byzantine behavior', 'Network partition'];
    const statuses: FailoverEvent['status'][] = ['completed', 'in_progress', 'failed'];
    const allNodes = ['EDGE1', 'EDGE2', 'CORE1', 'CORE2', 'CLOUD1'];

    return Array.from({ length: 5 }, (_, i) => {
        const source = allNodes[Math.floor(Math.random() * allNodes.length)];
        const target = allNodes.filter(n => n !== source)[Math.floor(Math.random() * 4)];
        return {
            id: `FO-${100 + Math.floor(Math.random() * 900)}`,
            timestamp: new Date(Date.now() - i * 120000).toISOString(),
            sourceNode: source,
            targetNode: target,
            reason: reasons[Math.floor(Math.random() * reasons.length)],
            status: statuses[Math.floor(Math.random() * statuses.length)]
        };
    });
}

export function useMetrics(refreshInterval = 2000) {
    const [metrics, setMetrics] = useState<SystemMetrics | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [useMock, setUseMock] = useState(false);

    const fetchMetrics = useCallback(async () => {
        if (useMock) {
            setMetrics(generateMockMetrics());
            setLoading(false);
            return;
        }

        try {
            const response = await fetch(`${API_BASE}/metrics`);
            if (!response.ok) throw new Error('Failed to fetch metrics');
            const data = await response.json();
            setMetrics(data);
            setError(null);
        } catch {
            // Fall back to mock data
            setUseMock(true);
            setMetrics(generateMockMetrics());
        } finally {
            setLoading(false);
        }
    }, [useMock]);

    useEffect(() => {
        fetchMetrics();
        const interval = setInterval(fetchMetrics, refreshInterval);
        return () => clearInterval(interval);
    }, [fetchMetrics, refreshInterval]);

    return { metrics, loading, error, useMock };
}

export function useTransactions() {
    const [transactions, setTransactions] = useState<TransactionData[]>([]);
    const [useMock, setUseMock] = useState(false);

    useEffect(() => {
        const fetchTransactions = async () => {
            if (useMock) {
                setTransactions(generateMockTransactions());
                return;
            }
            try {
                const response = await fetch(`${API_BASE}/transactions`);
                if (!response.ok) throw new Error('Failed');
                setTransactions(await response.json());
            } catch {
                setUseMock(true);
                setTransactions(generateMockTransactions());
            }
        };

        fetchTransactions();
        const interval = setInterval(fetchTransactions, 3000);
        return () => clearInterval(interval);
    }, [useMock]);

    return transactions;
}

export function useFailoverEvents() {
    const [events, setEvents] = useState<FailoverEvent[]>([]);
    const [useMock, setUseMock] = useState(false);

    useEffect(() => {
        const fetchEvents = async () => {
            if (useMock) {
                setEvents(generateMockFailoverEvents());
                return;
            }
            try {
                const response = await fetch(`${API_BASE}/failover-events`);
                if (!response.ok) throw new Error('Failed');
                setEvents(await response.json());
            } catch {
                setUseMock(true);
                setEvents(generateMockFailoverEvents());
            }
        };

        fetchEvents();
        const interval = setInterval(fetchEvents, 5000);
        return () => clearInterval(interval);
    }, [useMock]);

    return events;
}
