import { useEffect, useMemo, useState } from "react";
import { api } from "../api.js";
import ErrorBanner from "../components/ErrorBanner.jsx";

function formatTimestamp(value) {
    if (!value) return "—";
    try {
        return new Date(value).toLocaleString();
    } catch {
        return value;
    }
}

export default function ActivityLog() {
    const [events, setEvents] = useState(null);
    const [routes, setRoutes] = useState(null);
    const [routeFilter, setRouteFilter] = useState("");
    const [error, setError] = useState("");

    useEffect(() => {
        Promise.all([api.getEvents(), api.getRoutes()])
            .then(([e, r]) => {
                setEvents(e);
                setRoutes(r);
            })
            .catch((err) => setError(err.message));
    }, []);

    const filtered = useMemo(() => {
        if (!events) return [];
        if (!routeFilter) return events;
        return events.filter((e) => String(e.routeId) === routeFilter);
    }, [events, routeFilter]);

    const loading = events === null || routes === null;

    return (
        <div>
            <p className="page-eyebrow">Event feed</p>
            <h1 className="page-title">Activity log</h1>
            <p className="page-lede">
                Every risk event logged so far — auto-detected by GDELT + Gemini or entered manually —
                across the whole route network.
            </p>

            <ErrorBanner message={error} />

            {!loading && !error && (
                <div className="field" style={{ maxWidth: 280, marginBottom: 16 }}>
                    <label htmlFor="routeFilter">Filter by route</label>
                    <select id="routeFilter" value={routeFilter} onChange={(e) => setRouteFilter(e.target.value)}>
                        <option value="">All routes</option>
                        {routes.map((r) => (
                            <option key={r.id} value={r.id}>{r.name}</option>
                        ))}
                    </select>
                </div>
            )}

            {loading && !error && <div className="empty-state">Loading…</div>}

            {!loading && !error && filtered.length === 0 && (
                <div className="empty-state">
                    <div className="empty-state-glyph">···</div>
                    No events logged{routeFilter ? " for this route" : ""} yet.
                </div>
            )}

            {!loading && !error && filtered.length > 0 && (
                <table className="allocation-table">
                    <thead>
                    <tr>
                        <th>When</th>
                        <th>Route</th>
                        <th>Type</th>
                        <th>Severity</th>
                        <th>Source</th>
                        <th>Headline / text</th>
                    </tr>
                    </thead>
                    <tbody>
                    {filtered.map((e) => (
                        <tr key={e.id}>
                            <td className="mono">{formatTimestamp(e.createdAt)}</td>
                            <td>{e.routeName || "unmatched"}</td>
                            <td>
                                <span className="badge badge-neutral">{e.eventType || "—"}</span>
                            </td>
                            <td className="num">{e.severity ?? "—"}</td>
                            <td className="mono">{e.source}</td>
                            <td style={{ maxWidth: 420 }}>{e.rawText}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            )}
        </div>
    );
}