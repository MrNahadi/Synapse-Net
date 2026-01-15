import { Card, CardContent } from './ui/card';
import { Activity, Server, AlertTriangle, GitBranch } from 'lucide-react';
import type { SystemMetrics } from '../types';

interface StatsCardsProps {
    metrics: SystemMetrics;
}

export function StatsCards({ metrics }: StatsCardsProps) {
    const healthyNodes = metrics.nodes.filter(n => n.status === 'healthy').length;
    const totalNodes = metrics.nodes.length;

    const stats = [
        {
            label: 'Active Nodes',
            value: `${healthyNodes}/${totalNodes}`,
            icon: Server,
            color: healthyNodes === totalNodes ? 'text-status-healthy' : 'text-status-warning'
        },
        {
            label: 'Active Transactions',
            value: metrics.activeTransactions,
            icon: Activity,
            color: 'text-chart-1'
        },
        {
            label: 'Failed Nodes',
            value: metrics.failedNodes,
            icon: AlertTriangle,
            color: metrics.failedNodes > 0 ? 'text-status-critical' : 'text-status-healthy'
        },
        {
            label: 'Total Migrations',
            value: metrics.totalMigrations,
            icon: GitBranch,
            color: 'text-chart-3'
        }
    ];

    return (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {stats.map((stat) => (
                <Card key={stat.label}>
                    <CardContent className="p-4">
                        <div className="flex items-center gap-3">
                            <div className={`p-2 rounded-lg bg-muted ${stat.color}`}>
                                <stat.icon className="h-5 w-5" />
                            </div>
                            <div>
                                <p className="text-2xl font-bold">{stat.value}</p>
                                <p className="text-xs text-muted-foreground">{stat.label}</p>
                            </div>
                        </div>
                    </CardContent>
                </Card>
            ))}
        </div>
    );
}
