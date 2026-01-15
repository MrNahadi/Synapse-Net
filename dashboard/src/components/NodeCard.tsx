import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Badge } from './ui/badge';
import { Progress } from './ui/progress';
import { Activity, Cpu, HardDrive, Zap, Clock, Lock } from 'lucide-react';
import type { NodeData, NodeStatus } from '../types';

interface NodeCardProps {
    node: NodeData;
}

const statusVariant: Record<NodeStatus, 'healthy' | 'warning' | 'critical' | 'failed'> = {
    healthy: 'healthy',
    warning: 'warning',
    critical: 'critical',
    failed: 'failed'
};

const layerColors: Record<string, string> = {
    Edge: 'text-chart-1',
    Core: 'text-chart-3',
    Cloud: 'text-chart-5'
};

export function NodeCard({ node }: NodeCardProps) {
    const cpuColor = node.cpu < 60 ? 'bg-status-healthy' : node.cpu < 80 ? 'bg-status-warning' : 'bg-status-critical';

    return (
        <Card className="hover:shadow-md transition-shadow">
            <CardHeader className="pb-2">
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                        <CardTitle className="text-base">{node.name}</CardTitle>
                        <span className={`text-xs font-medium ${layerColors[node.layer]}`}>
                            {node.layer}
                        </span>
                    </div>
                    <Badge variant={statusVariant[node.status]}>
                        {node.status}
                    </Badge>
                </div>
                <p className="text-xs text-muted-foreground">
                    Failure Mode: {node.failureType}
                </p>
            </CardHeader>
            <CardContent className="space-y-3">
                <div className="space-y-1">
                    <div className="flex items-center justify-between text-sm">
                        <span className="flex items-center gap-1 text-muted-foreground">
                            <Cpu className="h-3 w-3" /> CPU
                        </span>
                        <span className="font-medium">{node.cpu.toFixed(1)}%</span>
                    </div>
                    <Progress value={node.cpu} indicatorClassName={cpuColor} />
                </div>

                <div className="grid grid-cols-2 gap-3 text-sm">
                    <div className="flex items-center gap-2">
                        <HardDrive className="h-3 w-3 text-muted-foreground" />
                        <span className="text-muted-foreground">Memory</span>
                        <span className="ml-auto font-medium">{node.memory.toFixed(1)}GB</span>
                    </div>
                    <div className="flex items-center gap-2">
                        <Clock className="h-3 w-3 text-muted-foreground" />
                        <span className="text-muted-foreground">Latency</span>
                        <span className="ml-auto font-medium">{node.latency.toFixed(1)}ms</span>
                    </div>
                    <div className="flex items-center gap-2">
                        <Zap className="h-3 w-3 text-muted-foreground" />
                        <span className="text-muted-foreground">Throughput</span>
                        <span className="ml-auto font-medium">{node.throughput.toFixed(0)}Mbps</span>
                    </div>
                    <div className="flex items-center gap-2">
                        <Activity className="h-3 w-3 text-muted-foreground" />
                        <span className="text-muted-foreground">Tx/sec</span>
                        <span className="ml-auto font-medium">{node.transactions}</span>
                    </div>
                </div>

                <div className="flex items-center gap-2 text-sm pt-1 border-t border-border">
                    <Lock className="h-3 w-3 text-muted-foreground" />
                    <span className="text-muted-foreground">Lock Contention</span>
                    <span className="ml-auto font-medium">{node.lockContention.toFixed(1)}%</span>
                </div>
            </CardContent>
        </Card>
    );
}
