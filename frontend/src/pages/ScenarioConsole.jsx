import { useEffect, useState } from "react";
import { api } from "../api.js";
import ErrorBanner from "../components/ErrorBanner.jsx";
import RiskGauge from "../components/RiskGauge.jsx";
import AllocationChart from "../components/AllocationChart.jsx";

function prettyJson(raw) {
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
}

const SAMPLE_TEXT =
  "Houthi forces launched a missile strike near a tanker transiting the Bab-el-Mandeb strait " +
  "on Tuesday, prompting several shipping lines to reroute crude cargoes via the Cape of Good " +
  "Hope. Insurers have widened war-risk premiums for the corridor for the third time this month.";

export default function ScenarioConsole() {
  const [routes, setRoutes] = useState(null);
  const [rawText, setRawText] = useState("");
  const [routeId, setRouteId] = useState("");
  const [severity, setSeverity] = useState(7);
  const [eventType, setEventType] = useState("CLOSURE");

  const [event, setEvent] = useState(null);
  const [scenario, setScenario] = useState(null);
  const [recommendations, setRecommendations] = useState(null);

  const [loadingEvent, setLoadingEvent] = useState(false);
  const [loadingScenario, setLoadingScenario] = useState(false);
  const [loadingRecs, setLoadingRecs] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    api.getRoutes().then(setRoutes).catch((e) => setError(e.message));
  }, []);

  const step1Done = !!event;
  const step2Done = !!scenario;

  async function handleLogEvent(e) {
    e.preventDefault();
    if (!rawText.trim()) return;
    setError("");
    setScenario(null);
    setRecommendations(null);
    setLoadingEvent(true);
    try {
      const result = await api.createEvent({
        rawText: rawText.trim(),
        routeId: routeId ? Number(routeId) : null,
        severity: Number(severity),
        eventType,
      });
      setEvent(result);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoadingEvent(false);
    }
  }

  async function handleSimulate() {
    if (!event) return;
    setError("");
    setRecommendations(null);
    setLoadingScenario(true);
    try {
      const result = await api.simulateScenario(event.id);
      setScenario(result);
      // Recommendations follow immediately once a scenario exists.
      setLoadingRecs(true);
      try {
        const recs = await api.getRecommendations(result.id);
        setRecommendations(recs);
      } catch (recErr) {
        setError(recErr.message);
      } finally {
        setLoadingRecs(false);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoadingScenario(false);
    }
  }

  function resetPipeline() {
    setRawText("");
    setRouteId("");
    setSeverity(7);
    setEvent(null);
    setScenario(null);
    setRecommendations(null);
    setError("");
  }

  return (
    <div>
      <p className="page-eyebrow">Crisis pipeline</p>
      <h1 className="page-title">Scenario console</h1>
      <p className="page-lede">
        Run a disruption report through extraction, simulation, and procurement recommendation —
        in that order, each step unlocked by the last.
      </p>

      <ErrorBanner message={error} />

      <div className="pipeline">
        {/* Step 1 — log event */}
        <div className="pipe-step">
          <div className="pipe-rail">
            <div className={`pipe-num${step1Done ? " done" : " current"}`}>
              {step1Done ? "✓" : "1"}
            </div>
            <div className="pipe-connector" />
          </div>
          <div className="pipe-body">
            <div className="pipe-label">Log the disruption</div>
            <p className="pipe-desc">
              Paste raw text — a news report, cable, or field note — and tag which route it hits
              and how severe it is. (Manual tagging for now: Gemini extraction stores the raw
              response but doesn't parse severity/route yet, so the optimizer reads these fields.)
            </p>

            <div className="panel panel-pad">
              <form onSubmit={handleLogEvent}>
                <div className="field">
                  <label htmlFor="rawText">Raw report text</label>
                  <textarea
                    id="rawText"
                    rows={5}
                    value={rawText}
                    onChange={(e) => setRawText(e.target.value)}
                    placeholder="Paste a disruption report here…"
                  />
                </div>

                <div style={{ display: "flex", gap: 14, flexWrap: "wrap", marginTop: 4 }}>
                  <div className="field" style={{ minWidth: 200 }}>
                    <label htmlFor="routeId">Affected route</label>
                    <select id="routeId" value={routeId} onChange={(e) => setRouteId(e.target.value)}>
                      <option value="">— none —</option>
                      {routes?.map((r) => (
                        <option key={r.id} value={r.id}>{r.name}</option>
                      ))}
                    </select>
                  </div>

                  <div className="field" style={{ minWidth: 160 }}>
                    <label htmlFor="severity">Severity (0-10)</label>
                    <input
                      id="severity"
                      type="number"
                      min={0}
                      max={10}
                      value={severity}
                      onChange={(e) => setSeverity(e.target.value)}
                    />
                  </div>

                  <div className="field" style={{ minWidth: 160 }}>
                    <label htmlFor="eventType">Event type</label>
                    <select id="eventType" value={eventType} onChange={(e) => setEventType(e.target.value)}>
                      <option value="CLOSURE">Closure</option>
                      <option value="ATTACK">Attack</option>
                      <option value="SANCTIONS">Sanctions</option>
                      <option value="CONGESTION">Congestion</option>
                    </select>
                  </div>
                </div>

                <div style={{ display: "flex", gap: 10, marginTop: 14 }}>
                  <button type="submit" className="btn btn-primary" disabled={loadingEvent || !rawText.trim()}>
                    {loadingEvent ? "Extracting…" : "Log event"}
                  </button>
                  <button
                    type="button"
                    className="btn"
                    onClick={() => setRawText(SAMPLE_TEXT)}
                    disabled={loadingEvent}
                  >
                    Use sample report
                  </button>
                  {event && (
                    <button type="button" className="btn" onClick={resetPipeline}>
                      Reset
                    </button>
                  )}
                </div>
              </form>

              {event && (
                <div style={{ marginTop: 18, borderTop: "1px solid var(--line-soft)", paddingTop: 16 }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 10 }}>
                    <span className="mono" style={{ fontSize: 12, color: "var(--muted)" }}>
                      event #{event.id}
                    </span>
                    <RiskGauge value={Number(severity)} max={10} size={44} />
                  </div>
                  <pre className="json-block">{prettyJson(event.extractedJson)}</pre>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Step 2 — simulate scenario */}
        <div className="pipe-step">
          <div className="pipe-rail">
            <div className={`pipe-num${step2Done ? " done" : step1Done ? " current" : ""}`}>
              {step2Done ? "✓" : "2"}
            </div>
            <div className="pipe-connector" />
          </div>
          <div className="pipe-body">
            <div className="pipe-label">Simulate the scenario</div>
            <p className="pipe-desc">
              Runs the event against the route and supplier network to model the downstream
              effect on supply.
            </p>

            <div className="panel panel-pad">
              {!step1Done ? (
                <div className="empty-state">
                  <div className="empty-state-glyph">···</div>
                  Log an event above to unlock simulation.
                </div>
              ) : (
                <>
                  <button className="btn btn-primary" onClick={handleSimulate} disabled={loadingScenario}>
                    {loadingScenario ? "Simulating…" : "Simulate scenario"}
                  </button>

                  {scenario && (
                    <div style={{ marginTop: 16, borderTop: "1px solid var(--line-soft)", paddingTop: 16 }}>
                      <span className="mono" style={{ fontSize: 12, color: "var(--muted)" }}>
                        scenario #{scenario.id}
                      </span>
                      <p style={{ marginTop: 8, marginBottom: 0, lineHeight: 1.6 }}>{scenario.summary}</p>
                      {scenario.supplyGapBarrels > 0 && (
                        <p className="mono" style={{ marginTop: 6, fontSize: 12, color: "var(--amber)" }}>
                          supply gap: {scenario.supplyGapBarrels.toLocaleString()} bbl/day
                        </p>
                      )}
                    </div>
                  )}
                </>
              )}
            </div>
          </div>
        </div>

        {/* Step 3 — recommendations */}
        <div className="pipe-step">
          <div className="pipe-rail">
            <div className={`pipe-num${recommendations ? " done" : step2Done ? " current" : ""}`}>
              {recommendations ? "✓" : "3"}
            </div>
          </div>
          <div className="pipe-body">
            <div className="pipe-label">Review procurement recommendations</div>
            <p className="pipe-desc">
              Ranked allocation plans from the optimizer — cost-optimal, risk-minimal, and
              balanced — each showing which suppliers cover the gap and at what cost/risk.
            </p>

            <div className="panel panel-pad">
              {!step2Done ? (
                <div className="empty-state">
                  <div className="empty-state-glyph">···</div>
                  Simulate a scenario above to generate recommendations.
                </div>
              ) : loadingRecs ? (
                <div className="empty-state">Running optimizer…</div>
              ) : recommendations?.length ? (
                <>
                  <div className="recommendation-card" style={{ marginBottom: 14 }}>
                    <AllocationChart plans={recommendations} />
                  </div>
                  {recommendations.map((rec) => (
                    <div className="recommendation-card" key={rec.id}>
                      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline" }}>
                        <p className="recommendation-action">
                          {rec.planName}
                          {rec.isOptimal && <span className="badge-optimal">recommended</span>}
                        </p>
                        <span className="mono" style={{ fontSize: 12, color: "var(--muted)" }}>
                          #{rec.id}
                        </span>
                      </div>
                      <p className="recommendation-reason">
                        Total cost ${rec.totalCost?.toLocaleString()} · avg risk {(rec.totalRisk * 100).toFixed(1)}%
                        {rec.supplyGap > 0 && ` · ${rec.supplyGap.toLocaleString()} bbl/day unmet`}
                      </p>

                      {rec.allocations?.length > 0 && (
                        <table className="allocation-table">
                          <thead>
                            <tr>
                              <th>Supplier</th>
                              <th>Route</th>
                              <th>Barrels/day</th>
                              <th>Share</th>
                              <th>Cost</th>
                            </tr>
                          </thead>
                          <tbody>
                            {rec.allocations.map((a, idx) => (
                              <tr key={idx}>
                                <td>{a.supplierName}</td>
                                <td>{a.routeName}</td>
                                <td className="num">{a.allocatedBarrels?.toLocaleString()}</td>
                                <td className="num">{a.allocatedPct?.toFixed(1)}%</td>
                                <td className="num">${a.cost?.toLocaleString()}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      )}
                    </div>
                  ))}
                </>
              ) : (
                <div className="empty-state">
                  <div className="empty-state-glyph">···</div>
                  No recommendations returned for this scenario.
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
