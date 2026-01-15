import { useMetrics, useTransactions, useFailoverEvents } from './hooks/useMetrics';
import { NodeCard } from './components/NodeCard';
import { MetricsChart } from './components/MetricsChart';
import { TransactionList } from './components/TransactionList';
import { LoadBalanceGauge } from './components/LoadBalanceGauge';
import { FailoverEvents } from './components/FailoverEvents';
import { SystemTopology } from './components/SystemTopology';
import { StatsCards } from './components/StatsCards';
import { Badge } from './components/ui/badge';
import { Activity, Moon, Sun } from 'lucide-react';
import { useState, useEffect } from 'react';

function App() {
    const { metrics, loading, useMock } = useMetrics();
    const transactions = useTransactions();
    const failoverEvents = useFailoverEvents();
    const [darkMode, setDarkMode] = useState(true);

    useEffect(() => {
        document.documentElement.classList.toggle('dark', darkMode);
    }, [darkMode]);

    if (loading || !metrics) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-background">
                <div className="flex items-center gap-3">
                    <Activity className="h-6 w-6 animate-pulse" />
                    <span className="text-lg">Loading dashboard...</span>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-background">
            {/* Header */}
            <header className="border-b border-border bg-card sticky top-0 z-10">
                <div className="container mx-auto px-4 py-4 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <Activity className="h-6 w-6 text-primary" />
                        <h1 className="text-xl font-bold">Distributed Telecom Dashboard</h1>
                        {useMock && (
                            <Badge variant="secondary" className="text-xs">Demo Mode</Badge>
                        )}
                    </div>
                    <div className="flex items-center gap-4">
                        <span className="text-sm text-muted-foreground">
                            Last updated: {new Date().toLocaleTimeString()}
                        </span>
                        <button
                            onClick={() => setDarkMode(!darkMode)}
                            className="p-2 rounded-lg hover:bg-muted transition-colors"
                        >
                            {darkMode ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
                        </button>
                    </div>
                </div>
            </header>

            {/* Main Content */}
            <main className="container mx-auto px-4 py-6 space-y-6">
                {/* Stats Overview */}
                <StatsCards metrics={metrics} />

                {/* Topology and Load Balance */}
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    <div className="lg:col-span-2">
                        <SystemTopology nodes={metrics.nodes} />
                    </div>
                    <LoadBalanceGauge
                        value={metrics.loadBalanceIndex}
                        totalServices={metrics.totalServices}
                        totalMigrations={metrics.totalMigrations}
                    />
                </div>

                {/* Node Status Grid */}
                <div>
                    <h2 className="text-lg font-semibold mb-4">Node Status</h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-4">
                        {metrics.nodes.map((node) => (
                            <NodeCard key={node.id} node={node} />
                        ))}
                    </div>
                </div>

                {/* Charts and Metrics */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                    <MetricsChart nodes={metrics.nodes} />
                    <FailoverEvents events={failoverEvents} />
                </div>

                {/* Transactions */}
                <TransactionList transactions={transactions} />
            </main>

            {/* Footer */}
            <footer className="border-t border-border bg-card mt-8">
                <div className="container mx-auto px-4 py-4 text-center text-sm text-muted-foreground">
                    Distributed Telecom System Dashboard • Real-time monitoring for Edge-Core-Cloud architecture
                </div>
            </footer>
        </div>
    );
}

export default App;
