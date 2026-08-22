import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Legend } from "recharts";

export default function AllocationChart({ plans }) {
    const chartData = (plans ?? []).map((plan) => ({
        strategy: plan.planName ?? plan.strategy ?? "Plan",
        totalCost: plan.totalCost ?? 0,
        avgRisk: plan.avgRisk ?? (plan.totalRisk ?? 0) * 100,
    }));

    return (
        <ResponsiveContainer width="100%" height={280}>
            <BarChart data={chartData}>
                <XAxis dataKey="strategy" stroke="var(--muted)" fontSize={12} />
                {/* Cost and risk live on wildly different scales (₹100k+ vs a
                    handful of %), so they each need their own axis — sharing
                    one axis made the risk bar collapse to a sliver next to
                    the cost bar. */}
                <YAxis
                    yAxisId="cost"
                    stroke="var(--muted)"
                    fontSize={12}
                    tickFormatter={(v) => `$${(v / 1000).toFixed(0)}k`}
                />
                <YAxis
                    yAxisId="risk"
                    orientation="right"
                    stroke="var(--rust)"
                    fontSize={12}
                    tickFormatter={(v) => `${v}%`}
                />
                <Tooltip
                    contentStyle={{ background: "var(--panel-raised)", border: "1px solid var(--line)" }}
                />
                <Legend />
                <Bar yAxisId="cost" dataKey="totalCost" fill="var(--amber)" name="Total Cost" />
                <Bar yAxisId="risk" dataKey="avgRisk" fill="var(--rust)" name="Total Risk (%)" />
            </BarChart>
        </ResponsiveContainer>
    );
}