# Crude Line — Crisis Console (frontend)

A React + Vite frontend for the `CrisesManagment` Spring Boot backend: a
crisis-monitoring console for India's crude oil supply chain (suppliers,
shipping routes, AI-extracted risk events, scenario simulation, and
procurement recommendations).

## Setup

```bash
cd frontend
npm install
cp .env.example .env   # point VITE_API_BASE_URL at your backend if not localhost:8080
npm run dev
```

The app runs at `http://localhost:5173`, which the backend's `CorsConfig`
already allows.

Make sure the Spring Boot backend is running first (`./mvnw spring-boot:run`
from the `CrisesManagment` folder) with Postgres available and, if you want
real event extraction, a `GEMINI_API_KEY` set.

## Pages

- **Overview** (`/`) — network stats pulled from `GET /api/suppliers` and `GET /api/routes`.
- **Suppliers** (`/suppliers`) — `GET /api/suppliers`.
- **Shipping Routes** (`/routes`) — `GET /api/routes`.
- **Scenario Console** (`/console`) — the three-step crisis pipeline:
  1. `POST /api/events` — log a raw report, get back the extracted event.
  2. `POST /api/scenarios/{eventId}/simulate` — simulate its effect.
  3. `GET /api/recommendations/{scenarioId}` — read the resulting procurement plan.

## Notes

- The backend doesn't expose "list all events" or "list all scenarios"
  endpoints, so the console only tracks the event/scenario/recommendations
  created in the current session — nothing is persisted client-side beyond
  that.
- `RecommendationDto` on the backend currently only returns `action` and
  `reason`; the richer `Recommendation` entity fields (`totalCost`,
  `totalRisk`, `supplyGap`, allocations) aren't in that response yet, so
  they're not shown here either. Extend the DTO and this page together if
  you want them surfaced.
