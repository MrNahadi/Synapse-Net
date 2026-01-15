import { Modal, ModalHeader, ModalTitle, ModalDescription, ModalContent } from './ui/modal';
import { Badge } from './ui/badge';
import { Progress } from './ui/progress';
import {
    Cpu, HardDrive, Zap, Clock, Activity, Lock,
    Server, AlertTriangle, CheckCircle, XCircle
} from 'lucide-react';
import type { NodeData, NodeStatus } from '../types';

interface NodeDetailModalProps {
    node: NodeData | null;
    open: boolean;
    onClose: () => void;
}

const statusVariant: Record<NodeStatus, 'healthy' | 'warning' | 'critical' | 'failed'> = {
    healthy: 'healthy',
    warning: 'warning',
    critical: 'critical',
    failed: 'failed'
};

const statusIcons: Record<NodeStatus, React.ReactNode> = {
    healthy: <CheckCircle className="h-5 w-5 text-status-healthy" />,
    warning: <AlertTriangle className="h-5 w-5 text-status-warning" />,
    critical: <AlertTriangle className="h-5 w-5 text-status-critical" />,
    failed: <XCircle className="h-5 w-5 text-status-failed" />
};

const layerDescriptions: Record<string, string> = {
    Edge: 'Handles local processing and initial data aggregation',
    Core: 'Manages routing, load balancing, and inter-node communication',
    Cloud: 'Provides centralized storage, analytics, and backup services'
};

export function NodeDetailModal({ node, open, onClose }: NodeDetailModalProps) {
    if (!node) return null;

    const cpuColor = node.cpu < 60 ? 'bg-status-healthy' : node.cpu < 80 ? 'bg-status-warning' : 'bg-status-critical';
    const memoryPercent = (node.memory / 16) * 100;
    const memoryColor = memoryPercent < 60 ? 'bg-status-healthy' : memoryPercent < 80 ? 'bg-status-warning' : 'bg-status-critical';

    return (
        <Modal open={open} onClose={onClose} className="max-w-md">
            <ModalHeader>
                <div className="flex items-center gap-3">
                    {statusIcons[node.status]}
                    <div>
                        <ModalTitle className="flex items-center gap-2">
                            <Server className="h-5 w-5" />
                            {node.name}
                            <Badge variant={statusVariant[node.status]} className="ml-2">
                                {node.status}
                            </Badge>
                        </ModalTitle>
                        <ModalDescription>
                            {node.layer} Layer • {layerDescriptions[node.layer]}
                        </ModalDescription>
                    </div>
                </div>
            </ModalHeader>

            <ModalContent>
                {/* Resource Usage */}
                <div className="space-y-4">
                    <h4 className="text-sm font-medium text-muted-foreground">Resource Usage</h4>

                    <div className="space-y-3">
                        <div className="space-y-1.5">
                            <div className="flex items-center justify-between text-sm">
                                <span className="flex items-center gap-2">
                                    <Cpu className="h-4 w-4 text-muted-foreground" />
                                    CPU Usage
                                </span>
                                <span className="font-medium">{node.cpu.toFixed(1)}%</span>
                            </div>
                            <Progress value={node.cpu} indicatorClassName={cpuColor} />
                        </div>

                        <div className="space-y-1.5">
                            <div className="flex items-center justify-between text-sm">
                                <span className="flex items-center gap-2">
                                    <HardDrive className="h-4 w-4 text-muted-foreground" />
                                    Memory Usage
                                </span>
                                <span className="font-medium">{node.memory.toFixed(1)} GB / 16 GB</span>
                            </div>
                            <Progress value={memoryPercent} indicatorClassName={memoryColor} />
                        </div>
                    </div>
                </div>

                {/* Performance Metrics */}
                <div className="space-y-3">
                    <h4 className="text-sm font-medium text-muted-foreground">Performance Metrics</h4>

                    <div className="grid grid-cols-2 gap-4">
                        <div className="p-3 rounded-lg bg-muted/50">
                            <div className="flex items-center gap-2 text-muted-foreground mb-1">
                                <Clock className="h-4 w-4" />
                                <span className="text-xs">Latency</span>
                            </div>
                            <p className="text-xl font-bold">{node.latency.toFixed(1)}<span className="text-sm font-normal text-muted-foreground">ms</span></p>
                        </div>

                        <div className="p-3 rounded-lg bg-muted/50">
                            <div className="flex items-center gap-2 text-muted-foreground mb-1">
                                <Zap className="h-4 w-4" />
                                <span className="text-xs">Throughput</span>
                            </div>
                            <p className="text-xl font-bold">{node.throughput.toFixed(0)}<span className="text-sm font-normal text-muted-foreground">Mbps</span></p>
                        </div>

                        <div className="p-3 rounded-lg bg-muted/50">
                            <div className="flex items-center gap-2 text-muted-foreground mb-1">
                                <Activity className="h-4 w-4" />
                                <span className="text-xs">Transactions/sec</span>
                            </div>
                            <p className="text-xl font-bold">{node.transactions}</p>
                        </div>

                        <div className="p-3 rounded-lg bg-muted/50">
                            <div className="flex items-center gap-2 text-muted-foreground mb-1">
                                <Lock className="h-4 w-4" />
                                <span className="text-xs">Lock Contention</span>
                            </div>
                            <p className="text-xl font-bold">{node.lockContention.toFixed(1)}<span className="text-sm font-normal text-muted-foreground">%</span></p>
                        </div>
                    </div>
                </div>

                {/* Failure Configuration */}
                <div className="p-3 rounded-lg border border-border">
                    <div className="flex items-center justify-between">
                        <span className="text-sm text-muted-foreground">Failure Mode</span>
                        <Badge variant="secondary">{node.failureType}</Badge>
                    </div>
                </div>
            </ModalContent>
        </Modal>
    );
}
