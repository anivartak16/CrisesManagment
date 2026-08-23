// Single source of truth for risk severity thresholds and their colors.
// Score is expected on a 0–10 scale. Update the thresholds here only —
// every component that shows risk color pulls from this file.

export const RISK_THRESHOLDS = {
    high: 7,
    medium: 4,
};

export const RISK_LABELS = {
    high: "High risk",
    medium: "Medium risk",
    low: "Low risk",
};

export function riskTone(score) {
    if (score >= RISK_THRESHOLDS.high) return "var(--rust)";
    if (score >= RISK_THRESHOLDS.medium) return "var(--amber)";
    return "var(--sea)";
}

export function riskLevel(score) {
    if (score >= RISK_THRESHOLDS.high) return "high";
    if (score >= RISK_THRESHOLDS.medium) return "medium";
    return "low";
}