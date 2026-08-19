import { useState } from "react";
import { api } from "../api.js";
import ErrorBanner from "../components/ErrorBanner.jsx";
import RiskGauge from "../components/RiskGauge.jsx";

function prettyJson(raw) {
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
}

function extractSeverity(raw) {
  try {
    const parsed = JSON.parse(raw);
    const val = parsed.severity ?? parsed.severityScore ?? null;
    return typeof val === "number" ? val : null;
  } catch {
    return null;
  }
}

const SAMPLE_TEXT =
  "Houthi forces launched a missile strike near a tanker transiting the Bab-el-Mandeb strait " +
  "on Tuesday, prompting several shipping lines to reroute crude cargoes via the Cape of Good " +
  "Hope. Insurers have widened war-risk premiums for the corridor for the third time this month.";

export default function ScenarioConsole() {
  const [rawText, setRawText] = useState("");
  const [event, setEvent] = useState(null);
  const [scenario, setScenario] = useState(null);
  const [recommendations, setRecommendations] = useState(null);

  const [loadingEvent, setLoadingEvent] = useState(false);
  const [loadingScenario, setLoadingScenario] = useState(false);
  const [loadingRecs, setLoadingRecs] = useState(false);
  const [error, setError] = useState("");

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
      const result = await api.createEvent(rawText.trim());
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
    setEvent(null);
    setScenario(null);
    setRecommendations(null);
    setError("");
  }

  const severity = event ? extractSeverity(event.extractedJson) : null;

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
              Paste raw text — a news report, cable, or field note. It's sent to the extraction
              service to identify the affected route, event type, and severity.
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
                <div style={{ display: "flex", gap: 10 }}>
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
                    {severity !== null && <RiskGauge value={severity} max={10} size={44} />}
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
              Actions the desk should take to cover the resulting supply gap, ranked by the
              simulation.
            </p>

            <div className="panel panel-pad">
              {!step2Done ? (
                <div className="empty-state">
                  <div className="empty-state-glyph">···</div>
                  Simulate a scenario above to generate recommendations.
                </div>
              ) : loadingRecs ? (
                <div className="empty-state">Pulling recommendations…</div>
              ) : recommendations?.length ? (
                recommendations.map((rec, i) => (
                  <div className="recommendation-card" key={i}>
                    <p className="recommendation-action">{rec.action}</p>
                    <p className="recommendation-reason">{rec.reason}</p>
                  </div>
                ))
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
