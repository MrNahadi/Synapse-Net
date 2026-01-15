import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import type { NodeData } from '../types';

interface MetricsChartProps {
    nodes: NodeData[];
}

export function MetricsChart({ nodes }: MetricsChartProps) {
    const data = nodes.map(node => ({
        name: node.name,
        CPU: node.cpu,
        Memory: (node.memory / 16) * 100, // Normalize to percentage
        Latency: (node.latency / 25) * 100, // Normalize to percentage
    }));

    return (
        <Card>
            <CardHeader>
                <CardTitle>Resource Utilization</CardTitle>
            </CardHeader>
            <CardContent>
                <ResponsiveContainer width="100%" height={300}>
                    <BarChart data={data} margin={{ top: 20, right: 30, left: 0, bottom: 5 }}>
                        <CartesianGrid strokeDasharray="3 3" className="stroke-border" />
                        <XAxis dataKey="name" className="text-xs" />
                        <YAxis className="text-xs" />
                        <Tooltip
                            contentStyle={{
                                backgroundColor: 'var(--card)',
                                border: '1px solid var(--border)',
                                borderRadius: 'var(--radius)'
                            }}
                        />
                        <Legend />
                        <Bar dataKey="CPU" fill="var(--chart-1)" radius={[4, 4, 0, 0]} />
                        <Bar dataKey="Memory" fill="var(--chart-3)" radius={[4, 4, 0, 0]} />
                        <Bar dataKey="Latency" fill="var(--chart-5)" radius={[4, 4, 0, 0]} />
                    </BarChart>
                </ResponsiveContainer>
            </CardContent>
        </Card>
    );
}
