import { Link } from "react-router-dom";
import { RISK_THRESHOLDS, RISK_LABELS } from "../utils/riskColor.js";

export default function About() {
    return (
        <div>
            <p className="page-eyebrow">Start here</p>
            <h1 className="page-title">What is Crude Line?</h1>
            <p className="page-lede">
                A monitoring desk for India's crude oil import network — it watches your
                suppliers and shipping routes, flags which ones are currently risky, and
                lets you simulate what happens if one gets disrupted.
            </p>

            <p className="section-title">Who this is for</p>
            <div className="panel panel-pad" style={{ marginBottom: 24 }}>
                <p style={{ margin: 0, lineHeight: 1.6 }}>
                    Anyone responsible for keeping crude imports flowing — a procurement or
                    supply-chain team that needs to know, at a glance, which routes are safe
                    right now and which ones need a backup plan. If a storm, conflict, or
                    port issue hits one of your shipping lanes, this is where you'd see it
                    first and figure out what to do about it.
                </p>
            </div>

            <p className="section-title">How to read the dashboard</p>
            <div className="panel panel-pad" style={{ marginBottom: 24 }}>
                <ul style={{ margin: 0, paddingLeft: 20, lineHeight: 1.8 }}>
                    <li><strong>Suppliers on file</strong> — how many crude suppliers you're tracking.</li>
                    <li><strong>Active routes</strong> — how many shipping routes connect those suppliers to you.</li>
                    <li><strong>Combined capacity</strong> — total barrels those routes can move.</li>
                    <li><strong>Avg. route risk</strong> — how risky the network is overall, on a 0–10 scale.</li>
                </ul>

                <p style={{ marginTop: 16, marginBottom: 8 }}>
                    On the map, every dot is a route. The color tells you how risky it is right now:
                </p>
                <ul style={{ margin: 0, paddingLeft: 20, lineHeight: 1.8 }}>
                    <li>
                        <span style={{ color: "var(--rust)" }}>●</span>{" "}
                        <strong>{RISK_LABELS.high}</strong> — score {RISK_THRESHOLDS.high} and above.
                        Worth acting on.
                    </li>
                    <li>
                        <span style={{ color: "var(--amber)" }}>●</span>{" "}
                        <strong>{RISK_LABELS.medium}</strong> — score {RISK_THRESHOLDS.medium} to {RISK_THRESHOLDS.high - 0.1}.
                        Keep an eye on it.
                    </li>
                    <li>
                        <span style={{ color: "var(--sea)" }}>●</span>{" "}
                        <strong>{RISK_LABELS.low}</strong> — below {RISK_THRESHOLDS.medium}. Fine for now.
                    </li>
                </ul>
            </div>

            <p className="section-title">Simulating a disruption</p>
            <div className="panel panel-pad" style={{ marginBottom: 24 }}>
                <p style={{ margin: 0, lineHeight: 1.6 }}>
                    Heard about a storm, strike, or attack that might affect a route? Paste a
                    report or news snippet into the <strong>Scenario Console</strong>. It reads
                    the snippet, works out which route it affects and how risky it now is, and
                    gives you a procurement plan to route around it — all before it actually
                    disrupts your supply.
                </p>
            </div>

            <div className="panel panel-pad" style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 16, flexWrap: "wrap" }}>
                <div>
                    <div style={{ fontFamily: "var(--font-display)", fontWeight: 600, fontSize: 15, marginBottom: 4 }}>
                        Ready to look at the live network?
                    </div>
                    <div style={{ color: "var(--muted)", fontSize: 13 }}>
                        Head to the dashboard for the current suppliers, routes, and risk map.
                    </div>
                </div>
                <Link to="/" className="btn btn-primary">
                    Go to dashboard →
                </Link>
            </div>
        </div>
    );
}