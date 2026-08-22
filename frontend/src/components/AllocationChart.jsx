import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Legend, LabelList } from "recharts";

export default function AllocationChart({ plans }) {
    const chartData = (plans ?? []).map((plan) => ({
        strategy: plan.planName ?? plan.strategy ?? "Plan",
        totalCost: plan.totalCost ?? 0,
        avgRisk: plan.avgRisk ?? (plan.totalRisk ?? 0) * 100,
    }));

    return (
        <ResponsiveContainer width="100%" height={280}>
            <BarChart data={chartData} margin={{ top: 24, right: 8, left: 0, bottom: 0 }}>
                <XAxis dataKey="strategy" stroke="var(--muted)" fontSize={12} />
                {/* Cost and risk live on wildly different scales (₹100k+ vs a
                    handful of %), so they each need their own axis — sharing
                    one axis made the risk bar collapse to a sliver next to
                    the cost bar. */}
                {/* domain padding: without this, Recharts scales the axis to
                    ~dataMax, so bars from plans with similar values all sit
                    near the top edge and look almost identical in height —
                    the 20% headroom here plus the value labels below make
                    small real differences between plans actually legible. */}
                <YAxis
                    yAxisId="cost"
                    stroke="var(--muted)"
                    fontSize={12}
                    domain={[0, (dataMax) => Math.ceil(dataMax * 1.2)]}
                    tickFormatter={(v) => `$${(v / 1000).toFixed(0)}k`}
                />
                <YAxis
                    yAxisId="risk"
                    orientation="right"
                    stroke="var(--rust)"
                    fontSize={12}
                    domain={[0, (dataMax) => Math.ceil(dataMax * 1.2)]}
                    tickFormatter={(v) => `${v}%`}
                />
                <Tooltip
                    contentStyle={{ background: "var(--panel-raised)", border: "1px solid var(--line)" }}
                />
                <Legend />
                <Bar yAxisId="cost" dataKey="totalCost" fill="var(--amber)" name="Total Cost">
                    <LabelList
                        dataKey="totalCost"
                        position="top"
                        fontSize={11}
                        fill="var(--amber)"
                        formatter={(v) => `$${(v / 1000).toFixed(0)}k`}
                    />
                </Bar>
                <Bar yAxisId="risk" dataKey="avgRisk" fill="var(--rust)" name="Total Risk (%)">
                    <LabelList
                        dataKey="avgRisk"
                        position="top"
                        fontSize={11}
                        fill="var(--rust)"
                        formatter={(v) => `${v.toFixed(1)}%`}
                    />
                </Bar>
            </BarChart>
        </ResponsiveContainer>
    );
}