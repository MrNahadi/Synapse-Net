import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Badge } from './ui/badge';
import { ArrowRight } from 'lucide-react';
import type { TransactionData } from '../types';

interface TransactionListProps {
    transactions: TransactionData[];
}

const statusColors: Record<string, 'healthy' | 'warning' | 'critical' | 'secondary'> = {
    committed: 'healthy',
    pending: 'warning',
    preparing: 'secondary',
    aborted: 'critical'
};

export function TransactionList({ transactions }: TransactionListProps) {
    return (
        <Card>
            <CardHeader>
                <CardTitle>Recent Transactions</CardTitle>
            </CardHeader>
            <CardContent>
                <div className="space-y-3 max-h-[400px] overflow-y-auto">
                    {transactions.map((tx) => (
                        <div key={tx.id} className="flex items-center justify-between p-3 rounded-lg bg-muted/50">
                            <div className="flex items-center gap-3">
                                <span className="font-mono text-sm font-medium">{tx.id}</span>
                                <Badge variant={statusColors[tx.status]}>{tx.status}</Badge>
                            </div>
                            <div className="flex items-center gap-2 text-sm text-muted-foreground">
                                {tx.nodes.map((node, i) => (
                                    <span key={node} className="flex items-center gap-1">
                                        {node}
                                        {i < tx.nodes.length - 1 && <ArrowRight className="h-3 w-3" />}
                                    </span>
                                ))}
                            </div>
                            <span className="text-xs text-muted-foreground">
                                {(tx.duration * 1000).toFixed(0)}ms
                            </span>
                        </div>
                    ))}
                </div>
            </CardContent>
        </Card>
    );
}
