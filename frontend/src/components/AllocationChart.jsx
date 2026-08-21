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
                <YAxis stroke="var(--muted)" fontSize={12} />
                <Tooltip
                    contentStyle={{ background: "var(--panel-raised)", border: "1px solid var(--line)" }}
                />
                <Legend />
                <Bar dataKey="totalCost" fill="var(--amber)" name="Total Cost" />
                <Bar dataKey="avgRisk" fill="var(--rust)" name="Total Risk (%)" />
            </BarChart>
        </ResponsiveContainer>
    );
}