import { TrendingUp, TrendingDown, Minus } from 'lucide-react';
import { cn } from '@/lib/utils';

interface TrendIndicatorProps {
    current: number;
    previous: number;
    /** If true, higher values are better (e.g., throughput). If false, lower is better (e.g., latency) */
    higherIsBetter?: boolean;
    showPercentage?: boolean;
    className?: string;
}

export function TrendIndicator({
    current,
    previous,
    higherIsBetter = true,
    showPercentage = true,
    className
}: TrendIndicatorProps) {
    if (previous === 0) return null;

    const diff = current - previous;
    const percentChange = ((diff / previous) * 100);
    const isUp = diff > 0;
    const isNeutral = Math.abs(percentChange) < 1;

    // Determine if the change is positive (good) or negative (bad)
    const isPositive = higherIsBetter ? isUp : !isUp;

    if (isNeutral) {
        return (
            <span className={cn("inline-flex items-center gap-0.5 text-muted-foreground", className)}>
                <Minus className="h-3 w-3" />
            </span>
        );
    }

    return (
        <span className={cn(
            "inline-flex items-center gap-0.5 text-xs font-medium",
            isPositive ? "text-status-healthy" : "text-status-critical",
            className
        )}>
            {isUp ? (
                <TrendingUp className="h-3 w-3" />
            ) : (
                <TrendingDown className="h-3 w-3" />
            )}
            {showPercentage && (
                <span>{Math.abs(percentChange).toFixed(1)}%</span>
            )}
        </span>
    );
}
