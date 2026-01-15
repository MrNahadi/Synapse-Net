import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Badge } from './ui/badge';
import { AlertTriangle, ArrowRight, CheckCircle, Clock, XCircle } from 'lucide-react';
import type { FailoverEvent } from '../types';

interface FailoverEventsProps {
    events: FailoverEvent[];
}

const statusIcons = {
    completed: <CheckCircle className="h-4 w-4 text-status-healthy" />,
    in_progress: <Clock className="h-4 w-4 text-status-warning" />,
    failed: <XCircle className="h-4 w-4 text-status-critical" />
};

const statusVariant: Record<string, 'healthy' | 'warning' | 'critical'> = {
    completed: 'healthy',
    in_progress: 'warning',
    failed: 'critical'
};

export function FailoverEvents({ events }: FailoverEventsProps) {
    return (
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2">
                    <AlertTriangle className="h-5 w-5" />
                    Failover Events
                </CardTitle>
            </CardHeader>
            <CardContent>
                <div className="space-y-3 max-h-[300px] overflow-y-auto">
                    {events.length === 0 ? (
                        <p className="text-sm text-muted-foreground text-center py-4">
                            No recent failover events
                        </p>
                    ) : (
                        events.map((event) => (
                            <div key={event.id} className="flex items-start gap-3 p-3 rounded-lg bg-muted/50">
                                {statusIcons[event.status]}
                                <div className="flex-1 min-w-0">
                                    <div className="flex items-center gap-2 mb-1">
                                        <span className="font-mono text-sm font-medium">{event.id}</span>
                                        <Badge variant={statusVariant[event.status]} className="text-xs">
                                            {event.status.replace('_', ' ')}
                                        </Badge>
                                    </div>
                                    <div className="flex items-center gap-1 text-sm text-muted-foreground mb-1">
                                        <span className="font-medium text-foreground">{event.sourceNode}</span>
                                        <ArrowRight className="h-3 w-3" />
                                        <span className="font-medium text-foreground">{event.targetNode}</span>
                                    </div>
                                    <p className="text-xs text-muted-foreground">{event.reason}</p>
                                </div>
                                <span className="text-xs text-muted-foreground whitespace-nowrap">
                                    {new Date(event.timestamp).toLocaleTimeString()}
                                </span>
                            </div>
                        ))
                    )}
                </div>
            </CardContent>
        </Card>
    );
}
