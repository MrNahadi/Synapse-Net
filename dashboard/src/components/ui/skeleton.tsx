import { cn } from "@/lib/utils"

interface SkeletonProps extends React.HTMLAttributes<HTMLDivElement> { }

export function Skeleton({ className, ...props }: SkeletonProps) {
    return (
        <div
            className={cn("animate-pulse rounded-md bg-muted", className)}
            {...props}
        />
    )
}

export function CardSkeleton() {
    return (
        <div className="rounded-lg border border-border bg-card p-6 space-y-4">
            <div className="flex items-center justify-between">
                <Skeleton className="h-5 w-24" />
                <Skeleton className="h-5 w-16 rounded-full" />
            </div>
            <Skeleton className="h-2 w-full" />
            <div className="grid grid-cols-2 gap-3">
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-full" />
            </div>
        </div>
    )
}

export function StatsCardSkeleton() {
    return (
        <div className="rounded-lg border border-border bg-card p-4">
            <div className="flex items-center gap-3">
                <Skeleton className="h-9 w-9 rounded-lg" />
                <div className="space-y-2">
                    <Skeleton className="h-6 w-12" />
                    <Skeleton className="h-3 w-20" />
                </div>
            </div>
        </div>
    )
}

export function ChartSkeleton() {
    return (
        <div className="rounded-lg border border-border bg-card p-6 space-y-4">
            <Skeleton className="h-5 w-32" />
            <div className="flex items-end gap-2 h-[250px]">
                {[40, 65, 45, 80, 55].map((h, i) => (
                    <Skeleton key={i} className="flex-1" style={{ height: `${h}%` }} />
                ))}
            </div>
        </div>
    )
}

export function TopologySkeleton() {
    return (
        <div className="rounded-lg border border-border bg-card p-6 space-y-4">
            <div className="flex items-center gap-2">
                <Skeleton className="h-5 w-5" />
                <Skeleton className="h-5 w-32" />
            </div>
            <div className="flex flex-col items-center gap-4 py-8">
                <Skeleton className="h-12 w-12 rounded-full" />
                <div className="flex gap-16">
                    <Skeleton className="h-10 w-10 rounded-full" />
                    <Skeleton className="h-10 w-10 rounded-full" />
                </div>
                <div className="flex gap-24">
                    <Skeleton className="h-10 w-10 rounded-full" />
                    <Skeleton className="h-10 w-10 rounded-full" />
                </div>
            </div>
        </div>
    )
}
