import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Scale } from 'lucide-react';

interface LoadBalanceGaugeProps {
    value: number;
    totalServices: number;
    totalMigrations: number;
}

export function LoadBalanceGauge({ value, totalServices, totalMigrations }: LoadBalanceGaugeProps) {
    const percentage = value * 100;
    const color = percentage >= 85 ? 'text-status-healthy' : percentage >= 70 ? 'text-status-warning' : 'text-status-critical';
    const bgColor = percentage >= 85 ? 'stroke-status-healthy' : percentage >= 70 ? 'stroke-status-warning' : 'stroke-status-critical';

    // SVG arc calculation
    const radius = 60;
    const circumference = Math.PI * radius;
    const offset = circumference - (percentage / 100) * circumference;

    return (
        <Card>
            <CardHeader className="pb-2">
                <CardTitle className="flex items-center gap-2">
                    <Scale className="h-5 w-5" />
                    Load Balance Index
                </CardTitle>
            </CardHeader>
            <CardContent>
                <div className="flex flex-col items-center">
                    <div className="relative w-40 h-24">
                        <svg className="w-full h-full" viewBox="0 0 140 80">
                            {/* Background arc */}
                            <path
                                d="M 10 70 A 60 60 0 0 1 130 70"
                                fill="none"
                                className="stroke-muted"
                                strokeWidth="12"
                                strokeLinecap="round"
                            />
                            {/* Value arc */}
                            <path
                                d="M 10 70 A 60 60 0 0 1 130 70"
                                fill="none"
                                className={bgColor}
                                strokeWidth="12"
                                strokeLinecap="round"
                                strokeDasharray={circumference}
                                strokeDashoffset={offset}
                                style={{ transition: 'stroke-dashoffset 0.5s ease' }}
                            />
                        </svg>
                        <div className="absolute inset-0 flex items-end justify-center pb-1">
                            <span className={`text-3xl font-bold ${color}`}>
                                {percentage.toFixed(1)}%
                            </span>
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-6 mt-4 w-full">
                        <div className="text-center">
                            <p className="text-2xl font-bold">{totalServices}</p>
                            <p className="text-xs text-muted-foreground">Active Services</p>
                        </div>
                        <div className="text-center">
                            <p className="text-2xl font-bold">{totalMigrations}</p>
                            <p className="text-xs text-muted-foreground">Migrations</p>
                        </div>
                    </div>
                </div>
            </CardContent>
        </Card>
    );
}
