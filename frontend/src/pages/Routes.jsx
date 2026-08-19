import { useEffect, useState } from "react";
import { api } from "../api.js";
import ErrorBanner from "../components/ErrorBanner.jsx";
import RiskGauge from "../components/RiskGauge.jsx";

export default function RoutesPage() {
  const [routes, setRoutes] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.getRoutes().then(setRoutes).catch((e) => setError(e.message));
  }, []);

  return (
    <div>
      <p className="page-eyebrow">Reference data</p>
      <h1 className="page-title">Shipping routes</h1>
      <p className="page-lede">
        Corridors linking each supplier to India, with distance, base shipping cost, and the risk
        score used when a disruption is simulated.
      </p>

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
                  <td>{r.originSupplier?.name || "—"}</td>
                  <td>
                    <div className="route-track">
                      <span className="mono" style={{ fontSize: 11, color: "var(--muted)" }}>
                        {r.originSupplier?.country?.slice(0, 3).toUpperCase() || "ORG"}
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
