import { RISK_THRESHOLDS, RISK_LABELS } from "../utils/riskColor.js";

export default function RouteMapLegend() {
    const items = [
        { color: "var(--rust)", label: RISK_LABELS.high, range: `${RISK_THRESHOLDS.high}–10` },
        { color: "var(--amber)", label: RISK_LABELS.medium, range: `${RISK_THRESHOLDS.medium}–${RISK_THRESHOLDS.high - 0.1}` },
        { color: "var(--sea)", label: RISK_LABELS.low, range: `0–${RISK_THRESHOLDS.medium - 0.1}` },
    ];

    return (
        <div
            style={{
                display: "flex",
                gap: 20,
                marginTop: 12,
                flexWrap: "wrap",
                fontSize: 13,
                color: "var(--text-dim, #9aa)",
            }}
        >
            {items.map((item) => (
                <div key={item.label} style={{ display: "flex", alignItems: "center", gap: 6 }}>
                    <span
                        style={{
                            width: 10,
                            height: 10,
                            borderRadius: "50%",
                            background: item.color,
                            display: "inline-block",
                        }}
                    />
                    <span>{item.label} ({item.range})</span>
                </div>
            ))}
        </div>
    );
}