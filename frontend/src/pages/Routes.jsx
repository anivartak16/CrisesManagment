import { useEffect, useState } from "react";
import { api } from "../api.js";
import ErrorBanner from "../components/ErrorBanner.jsx";
import RiskGauge from "../components/RiskGauge.jsx";

export default function RoutesPage() {
    const [routes, setRoutes] = useState(null);
    const [riskStatus, setRiskStatus] = useState(null);
    const [error, setError] = useState("");

    useEffect(() => {
        api.getRoutes().then(setRoutes).catch((e) => setError(e.message));
        api.getRiskStatus().then(setRiskStatus).catch(() => {});
    }, []);

    return (
        <div>
            <p className="page-eyebrow">Live conditions</p>
            <h1 className="page-title">Shipping routes</h1>
            <p className="page-lede">
                Corridors linking each supplier to India. Distance and shipping cost are reference data;
                risk score is recomputed live from active GDELT news + Gemini event scoring on each
                route — not a static input.
            </p>

            {riskStatus && (
                <div className="evidence-banner live">
                    <strong>● Live</strong>
                    {" — checked "}
                    {riskStatus.lastCheckedAt
                        ? new Date(riskStatus.lastCheckedAt).toLocaleTimeString()
                        : "…"}
                    {" · "}
                    {riskStatus.disruptedRoutes} of {riskStatus.totalRoutes} route(s) currently disrupted
                </div>
            )}

            <ErrorBanner message={error} />

            {!error && (
                <div className="table-wrap">
                    <table>
                        <thead>
                        <tr>
                            <th>Route</th>
                            <th>Origin</th>
                            <th>Track</th>
                            <th>Shipping cost</th>
                            <th>Risk score</th>
                        </tr>
                        </thead>
                        <tbody>
                        {routes === null && (
                            <tr>
                                <td colSpan={5}>
                                    <div className="empty-state">Loading routes…</div>
                                </td>
                            </tr>
                        )}
                        {routes?.length === 0 && (
                            <tr>
                                <td colSpan={5}>
                                    <div className="empty-state">
                                        <div className="empty-state-glyph">···</div>
                                        No routes on file.
                                    </div>
                                </td>
                            </tr>
                        )}
                        {routes?.map((r) => (
                            <tr key={r.id}>
                                <td style={{ fontWeight: 600 }}>{r.name}</td>
                                <td>{r.originSupplierName || "—"}</td>
                                <td>
                                    <div className="route-track">
                      <span className="mono" style={{ fontSize: 11, color: "var(--muted)" }}>
                        {r.originCountry?.slice(0, 3).toUpperCase() || "ORG"}
                      </span>
                                        <span className="route-track-line" />
                                        <span className="route-track-ship" title={`${r.distanceKm?.toLocaleString()} km`}>
                        ⛴
                      </span>
                                        <span className="route-track-line" />
                                        <span className="mono" style={{ fontSize: 11, color: "var(--muted)" }}>IN</span>
                                    </div>
                                </td>
                                <td className="num">${r.baseShippingCost?.toFixed(2)}/bbl</td>
                                <td>
                                    <RiskGauge value={Math.round((r.baseRiskScore || 0) * 10)} max={10} size={44} />
                                    <div style={{ fontSize: 11, color: "var(--muted)", marginTop: 2 }}>
                                        {r.activeEventCount > 0
                                            ? `${r.activeEventCount} active event${r.activeEventCount > 1 ? "s" : ""}${
                                                r.topEventType ? ` — ${r.topEventType}` : ""
                                            }`
                                            : "no active events"}
                                    </div>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
}