import { useEffect, useState } from "react";
import { api } from "../api.js";
import ErrorBanner from "../components/ErrorBanner.jsx";
import StatusBadge from "../components/StatusBadge.jsx";

function riskTone(level) {
  if (level === "HIGH") return "rust";
  if (level === "MODERATE") return "amber";
  if (level === "LOW") return "sea";
  return "neutral";
}

export default function WeatherImpact() {
  const [weather, setWeather] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [lastChecked, setLastChecked] = useState(null);

  function load() {
    setLoading(true);
    setError("");
    api
      .getWeatherRisks()
      .then((data) => {
        setWeather(data);
        setLastChecked(new Date());
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    load();
  }, []);

  const disruptedCount = weather?.filter((w) => w.disrupted).length || 0;

  return (
    <div>
      <p className="page-eyebrow">Live conditions</p>
      <h1 className="page-title">Weather impact</h1>
      <p className="page-lede">
        Current wind/storm conditions at each route's origin (Open-Meteo, no key required),
        flagging any corridor severe weather could currently disrupt.
      </p>

      <ErrorBanner message={error} />

      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14 }}>
        <span className="mono" style={{ fontSize: 12, color: "var(--muted)" }}>
          {lastChecked ? `checked ${lastChecked.toLocaleTimeString()}` : ""}
          {weather && ` · ${disruptedCount} of ${weather.length} route(s) currently disrupted`}
        </span>
        <button className="btn" onClick={load} disabled={loading}>
          {loading ? "Checking…" : "Refresh"}
        </button>
      </div>

      {!error && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Route</th>
                <th>Conditions</th>
                <th>Wind</th>
                <th>Gusts</th>
                <th>Risk</th>
              </tr>
            </thead>
            <tbody>
              {weather === null && (
                <tr>
                  <td colSpan={5}>
                    <div className="empty-state">Checking live weather…</div>
                  </td>
                </tr>
              )}
              {weather?.length === 0 && (
                <tr>
                  <td colSpan={5}>
                    <div className="empty-state">
                      <div className="empty-state-glyph">···</div>
                      No routes on file.
                    </div>
                  </td>
                </tr>
              )}
              {weather?.map((w) => (
                <tr key={w.routeId}>
                  <td style={{ fontWeight: 600 }}>
                    {w.routeName}
                    {w.disrupted && <span style={{ marginLeft: 8 }}>⚠</span>}
                  </td>
                  <td>{w.error ? <span style={{ color: "var(--muted)" }}>{w.error}</span> : w.weatherDescription}</td>
                  <td className="num">{w.windSpeedKph != null ? `${w.windSpeedKph.toFixed(0)} km/h` : "—"}</td>
                  <td className="num">{w.windGustsKph != null ? `${w.windGustsKph.toFixed(0)} km/h` : "—"}</td>
                  <td>
                    {w.riskLevel ? <StatusBadge tone={riskTone(w.riskLevel)}>{w.riskLevel}</StatusBadge> : "—"}
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
