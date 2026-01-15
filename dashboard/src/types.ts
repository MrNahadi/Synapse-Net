export type NodeStatus = 'healthy' | 'warning' | 'critical' | 'failed';

export interface NodeData {
    id: string;
    name: string;
    layer: 'Edge' | 'Core' | 'Cloud';
    status: NodeStatus;
    latency: number;
    throughput: number;
    cpu: number;
    memory: number;
    transactions: number;
    lockContention: number;
    failureType: string;
}

export interface TransactionData {
    id: string;
    status: 'committed' | 'pending' | 'preparing' | 'aborted';
    nodes: string[];
    startTime: string;
    duration: number;
}

export interface FailoverEvent {
    id: string;
    timestamp: string;
    sourceNode: string;
    targetNode: string;
    reason: string;
    status: 'completed' | 'in_progress' | 'failed';
}

export interface SystemMetrics {
    nodes: NodeData[];
    loadBalanceIndex: number;
    totalServices: number;
    activeTransactions: number;
    failedNodes: number;
    totalMigrations: number;
}
