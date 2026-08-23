import { useEffect, useState } from "react";
import { api } from "../api.js";
import ErrorBanner from "../components/ErrorBanner.jsx";

export default function History() {
  const [scenarios, setScenarios] = useState(null);
  const [error, setError] = useState("");
  const [expandedId, setExpandedId] = useState(null);
  const [recsByScenario, setRecsByScenario] = useState({});
  const [loadingRecsFor, setLoadingRecsFor] = useState(null);

  useEffect(() => {
    api.getScenarios().then(setScenarios).catch((e) => setError(e.message));
  }, []);

  async function toggleExpand(scenarioId) {
    if (expandedId === scenarioId) {
      setExpandedId(null);
      return;
    }
    setExpandedId(scenarioId);
    if (recsByScenario[scenarioId]) return;

    setLoadingRecsFor(scenarioId);
    try {
      const recs = await api.getRecommendations(scenarioId);
      setRecsByScenario((prev) => ({ ...prev, [scenarioId]: recs }));
    } catch (e) {
      setError(e.message);
    } finally {
      setLoadingRecsFor(null);
    }
  }

  const loading = scenarios === null;

  return (
      <div>
        <p className="page-eyebrow">Past runs</p>
        <h1 className="page-title">Scenario history</h1>
        <p className="page-lede">
          Every scenario simulated so far, most recent first. Expand one to see the procurement
          plans it produced and which one (if any) was accepted.
        </p>

        <ErrorBanner message={error} />

        {loading && !error && <div className="empty-state">Loading…</div>}

        {!loading && !error && scenarios.length === 0 && (
            <div className="empty-state">
              <div className="empty-state-glyph">···</div>
              No scenarios simulated yet — run one from the Scenario Console.
            </div>
        )}

        {!loading &&
            !error &&
            scenarios.map((s) => {
              const expanded = expandedId === s.id;
              const recs = recsByScenario[s.id];
              const accepted = recs?.find((r) => r.status === "ACCEPTED");

              return (
                  <div className="panel panel-pad" key={s.id} style={{ marginBottom: 12 }}>
                    <div
                        style={{
                          display: "flex",
                          justifyContent: "space-between",
                          alignItems: "center",
                          gap: 16,
                          flexWrap: "wrap",
                          cursor: "pointer",
                        }}
                        onClick={() => toggleExpand(s.id)}
                    >
                      <div>
                  <span className="mono" style={{ fontSize: 12, color: "var(--muted)" }}>
                    scenario #{s.id}
                  </span>
                        <p style={{ margin: "6px 0 0", lineHeight: 1.5 }}>{s.summary}</p>
                        {s.disruptedRouteName && (
                            <p className="mono" style={{ margin: "4px 0 0", fontSize: 12, color: "var(--muted)" }}>
                              route: {s.disruptedRouteName}
                            </p>
                        )}
                      </div>
                      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                        {accepted && <span className="badge badge-sea">plan #{accepted.id} accepted</span>}
                        <span className="badge badge-neutral">{s.status}</span>
                        <span className="mono" style={{ fontSize: 12 }}>{expanded ? "▲" : "▼"}</span>
                      </div>
                    </div>

                    {expanded && (
                        <div style={{ marginTop: 16, borderTop: "1px solid var(--line-soft)", paddingTop: 16 }}>
                          {loadingRecsFor === s.id ? (
                              <div className="empty-state">Loading recommendations…</div>
                          ) : recs?.length ? (
                              recs.map((rec) => (
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
                                      Total cost ${rec.totalCost?.toLocaleString()} · avg risk{" "}
                                      {(rec.totalRisk * 100).toFixed(1)}%
                                      {rec.supplyGap > 0 && ` · ${rec.supplyGap.toLocaleString()} bbl/day unmet`}
                                    </p>
                                    <div style={{ margin: "6px 0" }}>
                                      {rec.status === "ACCEPTED" && <span className="badge badge-sea">accepted</span>}
                                      {rec.status === "REJECTED" && <span className="badge badge-neutral">not chosen</span>}
                                      {(!rec.status || rec.status === "PROPOSED") && (
                                          <span className="badge badge-amber">proposed</span>
                                      )}
                                    </div>
                                  </div>
                              ))
                          ) : (
                              <div className="empty-state">No recommendations were generated for this scenario.</div>
                          )}
                        </div>
                    )}
                  </div>
              );
            })}
      </div>
  );
}