import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api.js";
import ErrorBanner from "../components/ErrorBanner.jsx";
import RiskGauge from "../components/RiskGauge.jsx";
import RouteMap from "../components/RouteMap.jsx";

export default function Dashboard() {
    const [suppliers, setSuppliers] = useState(null);
    const [routes, setRoutes] = useState(null);
    const [status, setStatus] = useState(null);
    const [error, setError] = useState("");

    useEffect(() => {
        Promise.all([api.getSuppliers(), api.getRoutes()])
            .then(([s, r]) => {
                setSuppliers(s);
                setRoutes(r);
            })
            .catch((e) => setError(e.message));

        // Non-fatal: the health panel shouldn't block the rest of the dashboard.
        api.getStatus().then(setStatus).catch(() => setStatus(null));
    }, []);

    const loading = suppliers === null || routes === null;

    const totalCapacity = suppliers?.reduce((sum, s) => sum + (s.capacity || 0), 0) ?? 0;
    const avgRoute = routes?.length
        ? routes.reduce((sum, r) => sum + (r.baseRiskScore || 0), 0) / routes.length
        : 0;
    const riskiest = routes?.length
        ? [...routes].sort((a, b) => b.baseRiskScore - a.baseRiskScore)[0]
        : null;

    // A route counts as "currently disrupted" once its live risk score has
    // been pushed above the seeded baseline by a recent event/weather update.
    const disruptedRoutes = routes?.filter(
        (r) => r.seedRiskScore != null && r.baseRiskScore > r.seedRiskScore
    ) ?? [];

    return (
        <div>
            <p className="page-eyebrow">Network overview</p>
            <h1 className="page-title">Crude import desk</h1>
            <p className="page-lede">
                Live read on the supplier and shipping-route network feeding India's crude imports.
                Log a disruption in the Scenario Console to simulate its effect and pull a procurement plan.
            </p>

            <ErrorBanner message={error} />

            {!loading && !error && disruptedRoutes.length > 0 && (
                <div
                    className="panel panel-pad"
                    style={{
                        marginBottom: 20,
                        borderColor: "var(--rust, #b5432a)",
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                        gap: 16,
                        flexWrap: "wrap",
                    }}
                >
                    <div>
                        <div style={{ fontFamily: "var(--font-display)", fontWeight: 600, fontSize: 15 }}>
                            {disruptedRoutes.length} route{disruptedRoutes.length > 1 ? "s" : ""} currently disrupted
                        </div>
                        <div style={{ color: "var(--muted)", fontSize: 13, marginTop: 4 }}>
                            {disruptedRoutes.map((r) => r.name).join(", ")}
                        </div>
                    </div>
                    <Link to="/routes" className="btn">
                        View affected routes →
                    </Link>
                </div>
            )}

            {!error && (
                <div className="stat-grid">
                    <div className="stat-card">
                        <p className="stat-label">Suppliers on file</p>
                        <div className="stat-value">{loading ? "—" : suppliers.length}</div>
                    </div>
                    <div className="stat-card">
                        <p className="stat-label">Active routes</p>
                        <div className="stat-value">{loading ? "—" : routes.length}</div>
                    </div>
                    <div className="stat-card">
                        <p className="stat-label">Combined capacity</p>
                        <div className="stat-value">
                            {loading ? "—" : totalCapacity.toLocaleString()}
                            <small>bbl</small>
                        </div>
                    </div>
                    <div className="stat-card">
                        <p className="stat-label">Avg. route risk</p>
                        <div className="stat-value">{loading ? "—" : avgRoute.toFixed(2)}</div>
                    </div>
                </div>
            )}

            {!loading && !error && (
                <>
                    <p className="section-title">Route map</p>
                    <div className="panel panel-pad" style={{ marginBottom: 24 }}>
                        <RouteMap routes={routes} />
                    </div>

                    <p className="section-title">Highest-risk corridor</p>
                    <div className="panel panel-pad" style={{ marginBottom: 32, display: "flex", justifyContent: "space-between", alignItems: "center", gap: 20, flexWrap: "wrap" }}>
                        {riskiest ? (
                            <>
                                <div>
                                    <div style={{ fontFamily: "var(--font-display)", fontWeight: 600, fontSize: 16 }}>
                                        {riskiest.name}
                                    </div>
                                    <div style={{ color: "var(--muted)", fontSize: 13, marginTop: 4 }}>
                                        origin: {riskiest.originSupplier?.name || "—"} · {riskiest.distanceKm?.toLocaleString()} km
                                    </div>
                                </div>
                                <RiskGauge value={Math.round((riskiest.baseRiskScore || 0) * 10)} max={10} />
                            </>
                        ) : (
                            <span style={{ color: "var(--muted)" }}>No routes on file yet.</span>
                        )}
                    </div>

                    <div className="panel panel-pad" style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 16, flexWrap: "wrap" }}>
                        <div>
                            <div style={{ fontFamily: "var(--font-display)", fontWeight: 600, fontSize: 15, marginBottom: 4 }}>
                                Log a disruption
                            </div>
                            <div style={{ color: "var(--muted)", fontSize: 13 }}>
                                Paste a report or news snippet and run it through the three-step scenario pipeline.
                            </div>
                        </div>
                        <Link to="/console" className="btn btn-primary">
                            Open Scenario Console →
                        </Link>
                    </div>
                </>
            )}

            {status?.integrations && (
                <>
                    <p className="section-title" style={{ marginTop: 32 }}>Integration status</p>
                    <div className="panel panel-pad">
                        {status.integrations.map((i) => (
                            <div
                                key={i.name}
                                style={{
                                    display: "flex",
                                    justifyContent: "space-between",
                                    alignItems: "center",
                                    gap: 16,
                                    padding: "8px 0",
                                    borderBottom: "1px solid var(--border, #2a2a2a)",
                                }}
                            >
                                <div>
                                    <div style={{ fontWeight: 600, fontSize: 13 }}>{i.name}</div>
                                    <div style={{ color: "var(--muted)", fontSize: 12, marginTop: 2 }}>{i.detail}</div>
                                </div>
                                <span className={`badge ${i.live ? "badge-sea" : "badge-neutral"}`}>
                  {i.live ? "LIVE" : "FALLBACK"}
                </span>
                            </div>
                        ))}
                    </div>
                </>
            )}
        </div>
    );
}