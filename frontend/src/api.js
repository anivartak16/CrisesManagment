const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

async function request(path, options = {}) {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });

  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const body = await res.json();
      message = body.message || body.error || message;
    } catch {
      // response had no JSON body — keep the generic message
    }
    throw new Error(message);
  }

  if (res.status === 204) return null;
  return res.json();
}

export const api = {
  // Reference data
  getSuppliers: () => request("/api/suppliers"),
  getSupplier: (id) => request(`/api/suppliers/${id}`),
  getMarketStatus: () => request("/api/suppliers/market-status"),
  getRoutes: () => request("/api/routes"),
  getRoute: (id) => request(`/api/routes/${id}`),

  // Crisis pipeline: log event -> simulate scenario -> read recommendations
  createEvent: ({ rawText, routeId, severity, eventType, durationDays }) =>
    request("/api/events", {
      method: "POST",
      body: JSON.stringify({ rawText, routeId, severity, eventType, durationDays }),
    }),
  simulateScenario: (eventId) =>
    request(`/api/scenarios/${eventId}/simulate`, { method: "POST" }),
  getScenario: (id) => request(`/api/scenarios/${id}`),
  getRecommendations: (scenarioId) => request(`/api/recommendations/${scenarioId}`),
  acceptRecommendation: (recommendationId) =>
    request(`/api/recommendations/${recommendationId}/accept`, { method: "PATCH" }),

  // Weather: which routes are currently affected by weather (wind/storms)
  getWeatherRisks: () => request("/api/weather/routes"),

  // Integration health panel (EIA / Gemini / GDELT / Open-Meteo)
  getStatus: () => request("/api/status"),
};

export { BASE_URL };
