import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Network } from 'lucide-react';
import { NodeDetailModal } from './NodeDetailModal';
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
    CLOUD1: { x: 200, y: 40 },
    CORE1: { x: 130, y: 115 },
    CORE2: { x: 270, y: 115 },
    EDGE1: { x: 110, y: 200 },
    EDGE2: { x: 290, y: 200 },
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
    const [selectedNode, setSelectedNode] = useState<NodeData | null>(null);
    const [hoveredNode, setHoveredNode] = useState<string | null>(null);

    const getConnectedNodes = (nodeId: string) => {
        return connections
            .filter(([from, to]) => from === nodeId || to === nodeId)
            .flatMap(([from, to]) => [from, to])
            .filter(id => id !== nodeId);
    };

    const isConnectionHighlighted = (from: string, to: string) => {
        if (!hoveredNode) return false;
        return (from === hoveredNode || to === hoveredNode);
    };

    return (
        <>
            <Card>
                <CardHeader className="pb-2">
                    <CardTitle className="flex items-center gap-2">
                        <Network className="h-5 w-5" />
                        System Topology
                        <span className="text-xs font-normal text-muted-foreground ml-2">
                            Click a node for details
                        </span>
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <svg viewBox="0 0 400 270" className="w-full h-auto">
                        {/* Layer backgrounds */}
                        <rect x="10" y="10" width="380" height="65" rx="8" className="fill-chart-5/10" />
                        <rect x="10" y="80" width="380" height="80" rx="8" className="fill-chart-3/10" />
                        <rect x="10" y="165" width="380" height="95" rx="8" className="fill-chart-1/10" />

                        {/* Layer labels */}
                        <text x="20" y="30" className="fill-muted-foreground text-[10px]">Cloud</text>
                        <text x="20" y="100" className="fill-muted-foreground text-[10px]">Core</text>
                        <text x="20" y="185" className="fill-muted-foreground text-[10px]">Edge</text>

                        {/* Connections */}
                        {connections.map(([from, to], i) => {
                            const fromPos = nodePositions[from];
                            const toPos = nodePositions[to];
                            const isDashed = (from === 'EDGE1' && to === 'EDGE2') || (from === 'CORE1' && to === 'CORE2');
                            const isHighlighted = isConnectionHighlighted(from, to);
                            return (
                                <line
                                    key={i}
                                    x1={fromPos.x}
                                    y1={fromPos.y}
                                    x2={toPos.x}
                                    y2={toPos.y}
                                    className={isHighlighted ? "stroke-primary" : "stroke-border"}
                                    strokeWidth={isHighlighted ? "3" : "2"}
                                    strokeDasharray={isDashed ? "4 4" : undefined}
                                    style={{ transition: 'stroke 0.2s, stroke-width 0.2s' }}
                                />
                            );
                        })}

                        {/* Nodes */}
                        {nodes.map((node) => {
                            const pos = nodePositions[node.id];
                            if (!pos) return null;
                            const isHovered = hoveredNode === node.id;
                            const isConnected = hoveredNode ? getConnectedNodes(hoveredNode).includes(node.id) : false;
                            const shouldHighlight = isHovered || isConnected;

                            return (
                                <g
                                    key={node.id}
                                    transform={`translate(${pos.x}, ${pos.y})`}
                                    className="cursor-pointer"
                                    onClick={() => setSelectedNode(node)}
                                    onMouseEnter={() => setHoveredNode(node.id)}
                                    onMouseLeave={() => setHoveredNode(null)}
                                >
                                    {/* Pulse animation for non-healthy nodes */}
                                    {node.status !== 'healthy' && (
                                        <circle
                                            r="22"
                                            className={`${statusColors[node.status]} fill-opacity-20 animate-ping`}
                                            style={{ animationDuration: '2s' }}
                                        />
                                    )}

                                    {/* Outer ring */}
                                    <circle
                                        r={shouldHighlight ? 22 : 18}
                                        className={`${statusColors[node.status]} fill-opacity-20 stroke-2`}
                                        style={{ transition: 'r 0.2s' }}
                                    />

                                    {/* Inner dot */}
                                    <circle
                                        r={shouldHighlight ? 8 : 6}
                                        className={statusColors[node.status]}
                                        style={{ transition: 'r 0.2s' }}
                                    />

                                    {/* Node name */}
                                    <text
                                        y="32"
                                        textAnchor="middle"
                                        className="fill-foreground text-[10px] font-medium pointer-events-none"
                                    >
                                        {node.name}
                                    </text>

                                    {/* Metrics */}
                                    <text
                                        y="44"
                                        textAnchor="middle"
                                        className="fill-muted-foreground text-[8px] pointer-events-none"
                                    >
                                        {node.cpu.toFixed(0)}% | {node.latency.toFixed(0)}ms
                                    </text>
                                </g>
                            );
                        })}
                    </svg>
                </CardContent>
            </Card>

            <NodeDetailModal
                node={selectedNode}
                open={!!selectedNode}
                onClose={() => setSelectedNode(null)}
            />
        </>
    );
}
