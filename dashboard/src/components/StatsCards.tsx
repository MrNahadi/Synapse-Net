import { Card, CardContent } from './ui/card';
import { Activity, Server, AlertTriangle, GitBranch } from 'lucide-react';
import { TrendIndicator } from './TrendIndicator';
import type { SystemMetrics } from '../types';

interface StatsCardsProps {
    metrics: SystemMetrics;
    previousMetrics?: SystemMetrics | null;
}

export function StatsCards({ metrics, previousMetrics }: StatsCardsProps) {
    const healthyNodes = metrics.nodes.filter(n => n.status === 'healthy').length;
    const totalNodes = metrics.nodes.length;
    const prevHealthyNodes = previousMetrics?.nodes.filter(n => n.status === 'healthy').length ?? healthyNodes;

    const stats = [
        {
            label: 'Active Nodes',
            value: `${healthyNodes}/${totalNodes}`,
            numericValue: healthyNodes,
            prevValue: prevHealthyNodes,
            icon: Server,
            color: healthyNodes === totalNodes ? 'text-status-healthy' : 'text-status-warning',
            higherIsBetter: true
        },
        {
            label: 'Active Transactions',
            value: metrics.activeTransactions,
            numericValue: metrics.activeTransactions,
            prevValue: previousMetrics?.activeTransactions ?? metrics.activeTransactions,
            icon: Activity,
            color: 'text-chart-1',
            higherIsBetter: true
        },
        {
            label: 'Failed Nodes',
            value: metrics.failedNodes,
            numericValue: metrics.failedNodes,
            prevValue: previousMetrics?.failedNodes ?? metrics.failedNodes,
            icon: AlertTriangle,
            color: metrics.failedNodes > 0 ? 'text-status-critical' : 'text-status-healthy',
            higherIsBetter: false
        },
        {
            label: 'Total Migrations',
            value: metrics.totalMigrations,
            numericValue: metrics.totalMigrations,
            prevValue: previousMetrics?.totalMigrations ?? metrics.totalMigrations,
            icon: GitBranch,
            color: 'text-chart-3',
            higherIsBetter: false
        }
    ];

    return (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {stats.map((stat) => (
                <Card key={stat.label} className="hover:shadow-md transition-shadow">
                    <CardContent className="p-4">
                        <div className="flex items-center gap-3">
                            <div className={`p-2 rounded-lg bg-muted ${stat.color}`}>
                                <stat.icon className="h-5 w-5" />
                            </div>
                            <div className="flex-1 min-w-0">
                                <div className="flex items-center gap-2">
                                    <p className="text-2xl font-bold">{stat.value}</p>
                                    {previousMetrics && (
                                        <TrendIndicator
                                            current={stat.numericValue}
                                            previous={stat.prevValue}
                                            higherIsBetter={stat.higherIsBetter}
                                        />
                                    )}
                                </div>
                                <p className="text-xs text-muted-foreground truncate">{stat.label}</p>
                            </div>
                        </div>
                    </CardContent>
                </Card>
            ))}
        </div>
    );
}
