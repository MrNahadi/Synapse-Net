import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Network } from 'lucide-react';
import type { NodeData, NodeStatus } from '../types';

interface SystemTopologyProps {
    nodes: NodeData[];
}

const statusColors: Record<NodeStatus, string> = {
    healthy: 'fill-status-healthy stroke-status-healthy',
    warning: 'fill-status-warning stroke-status-warning',
    critical: 'fill-status-critical stroke-status-critical',
    failed: 'fill-status-failed stroke-status-failed'
};

const nodePositions: Record<string, { x: number; y: number }> = {
    CLOUD1: { x: 200, y: 30 },
    CORE1: { x: 120, y: 100 },
    CORE2: { x: 280, y: 100 },
    EDGE1: { x: 80, y: 180 },
    EDGE2: { x: 320, y: 180 },
};

const connections = [
    ['EDGE1', 'CORE1'],
    ['EDGE1', 'CORE2'],
    ['EDGE2', 'CORE1'],
    ['EDGE2', 'CORE2'],
    ['CORE1', 'CLOUD1'],
    ['CORE2', 'CLOUD1'],
    ['EDGE1', 'EDGE2'],
    ['CORE1', 'CORE2'],
];

export function SystemTopology({ nodes }: SystemTopologyProps) {
    return (
        <Card>
            <CardHeader className="pb-2">
                <CardTitle className="flex items-center gap-2">
                    <Network className="h-5 w-5" />
                    System Topology
                </CardTitle>
            </CardHeader>
            <CardContent>
                <svg viewBox="0 0 400 220" className="w-full h-auto">
                    {/* Layer backgrounds */}
                    <rect x="10" y="10" width="380" height="50" rx="8" className="fill-chart-5/10" />
                    <rect x="10" y="70" width="380" height="60" rx="8" className="fill-chart-3/10" />
                    <rect x="10" y="140" width="380" height="70" rx="8" className="fill-chart-1/10" />

                    {/* Layer labels */}
                    <text x="20" y="28" className="fill-muted-foreground text-[10px]">Cloud</text>
                    <text x="20" y="88" className="fill-muted-foreground text-[10px]">Core</text>
                    <text x="20" y="158" className="fill-muted-foreground text-[10px]">Edge</text>

                    {/* Connections */}
                    {connections.map(([from, to], i) => {
                        const fromPos = nodePositions[from];
                        const toPos = nodePositions[to];
                        const isDashed = (from === 'EDGE1' && to === 'EDGE2') || (from === 'CORE1' && to === 'CORE2');
                        return (
                            <line
                                key={i}
                                x1={fromPos.x}
                                y1={fromPos.y}
                                x2={toPos.x}
                                y2={toPos.y}
                                className="stroke-border"
                                strokeWidth="2"
                                strokeDasharray={isDashed ? "4 4" : undefined}
                            />
                        );
                    })}

                    {/* Nodes */}
                    {nodes.map((node) => {
                        const pos = nodePositions[node.id];
                        if (!pos) return null;
                        return (
                            <g key={node.id} transform={`translate(${pos.x}, ${pos.y})`}>
                                <circle
                                    r="18"
                                    className={`${statusColors[node.status]} fill-opacity-20 stroke-2`}
                                />
                                <circle
                                    r="6"
                                    className={statusColors[node.status]}
                                />
                                <text
                                    y="32"
                                    textAnchor="middle"
                                    className="fill-foreground text-[10px] font-medium"
                                >
                                    {node.name}
                                </text>
                                <text
                                    y="44"
                                    textAnchor="middle"
                                    className="fill-muted-foreground text-[8px]"
                                >
                                    {node.cpu.toFixed(0)}% | {node.latency.toFixed(0)}ms
                                </text>
                            </g>
                        );
                    })}
                </svg>
            </CardContent>
        </Card>
    );
}
