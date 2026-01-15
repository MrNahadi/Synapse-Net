import { Wifi, WifiOff, RefreshCw } from 'lucide-react';
import { cn } from '@/lib/utils';

interface ConnectionStatusProps {
    connected: boolean;
    useMock: boolean;
    lastUpdate: Date;
    onRefresh?: () => void;
    isRefreshing?: boolean;
}

export function ConnectionStatus({ connected, useMock, lastUpdate, onRefresh, isRefreshing }: ConnectionStatusProps) {
    return (
        <div className="flex items-center gap-3">
            <div className={cn(
                "flex items-center gap-1.5 px-2 py-1 rounded-full text-xs font-medium",
                connected
                    ? "bg-status-healthy/10 text-status-healthy"
                    : "bg-status-warning/10 text-status-warning"
            )}>
                {connected ? (
                    <>
                        <Wifi className="h-3 w-3" />
                        <span>Live</span>
                        <span className="relative flex h-2 w-2">
                            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-status-healthy opacity-75"></span>
                            <span className="relative inline-flex rounded-full h-2 w-2 bg-status-healthy"></span>
                        </span>
                    </>
                ) : (
                    <>
                        <WifiOff className="h-3 w-3" />
                        <span>{useMock ? 'Demo' : 'Offline'}</span>
                    </>
                )}
            </div>

            <span className="text-xs text-muted-foreground">
                {lastUpdate.toLocaleTimeString()}
            </span>

            {onRefresh && (
                <button
                    onClick={onRefresh}
                    disabled={isRefreshing}
                    className="p-1.5 rounded-md hover:bg-muted transition-colors disabled:opacity-50"
                    title="Refresh data"
                >
                    <RefreshCw className={cn("h-4 w-4", isRefreshing && "animate-spin")} />
                </button>
            )}
        </div>
    );
}
