<div align="center">

# 🚨 CrisesManagment

**A supply chain risk management console** — turns crisis signals into structured risk data and actionable procurement recommendations.

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-brightgreen?logo=springboot)
![React](https://img.shields.io/badge/React-18-61DAFB?logo=react)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-database-336791?logo=postgresql)
![Status](https://img.shields.io/badge/status-hackathon%20project-yellow)

</div>

---

## Table of contents

- [What it does](#what-it-does)
- [How it works](#how-it-works)
- [Tech stack](#tech-stack)
- [Project structure](#project-structure)
- [API endpoints](#api-endpoints)
- [Getting started](#getting-started)
- [Notes](#notes)

## What it does

Supply chains break for reasons that are visible hours or days before they hit — a storm on a shipping lane, a commodity price spike, a headline about a port strike. CrisesManagment watches for those signals, turns them into structured risk events, and tells you which routes and suppliers are exposed and what to do about it.

| Capability | Description |
|---|---|
| 📰 **Risk event ingestion** | Scheduled news monitor (GDELT) pulls headlines; Gemini extracts structured risk data (event type, severity, duration, affected route) |
| 🔀 **Dual-AI cross-check** | When a Groq key is set, every headline is run through both Gemini and Groq and reconciled (averaged severity/duration, majority-agreed type/route) instead of trusting a single model |
| 🌦️ **Weather risk** | Live conditions per route origin via Open-Meteo; routes above a configurable wind-speed threshold get flagged |
| 📈 **Market data** | Commodity/market signals from the EIA API feed into risk scoring |
| 🧭 **Route risk scoring** | Combines weather, market, and event signals into a per-route risk view |
| 🧮 **Allocation optimization** | Optimization engine proposes procurement re-allocations across suppliers given current risk state |
| 🧪 **Scenario simulation** | Run "what-if" scenarios against the live supplier/route graph |
| 📊 **Dashboard UI** | Risk gauge, allocation charts, interactive Leaflet route map, activity log, and dedicated pages per domain |

## How it works

```
GDELT headlines ──▶ NewsMonitorService ──▶ GeminiExtractionService (+ Groq cross-check)
                                                      │
                                                      ▼
                                              RiskEvent (stored)
                                                      │
                     Open-Meteo ──▶ WeatherService    │    EIA ──▶ MarketDataService
                                            │          │          │
                                            └──────────┼──────────┘
                                                        ▼
                                          RouteRiskService (per-route score)
                                                        │
                                                        ▼
                                        OptimizationEngine ──▶ Recommendation
                                                        │
                                                        ▼
                                          React dashboard (risk gauge, map, charts)
```

## Tech stack

**Backend** — Java 21, Spring Boot 4.1 (Web MVC + WebFlux for outbound calls), Spring Data JPA/Hibernate, PostgreSQL, Lombok, Bean Validation.
**Frontend** — React 18, Vite, React Router, Recharts, Leaflet / react-leaflet.
**External APIs** — Gemini (extraction), Groq (optional cross-check), Open-Meteo (weather, no key needed), EIA (market data), GDELT (news).

## Project structure

```
src/main/java/.../crisesmanagment/
├── Controller/       # REST endpoints
├── service/          # GeminiExtractionService, NewsMonitorService, WeatherService, MarketDataService, RouteRiskService, ...
├── optimization/      # Allocation optimization engine
├── model/ repo/ dto/ config/ exception/

frontend/src/
├── pages/            # Dashboard, Suppliers, Routes, WeatherImpact, ScenarioConsole, History, ActivityLog
├── components/       # RiskGauge, AllocationChart, RouteMap, StatusBadge, ErrorBanner
```

## API endpoints

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/status` | Health/status check |
| `GET` | `/api/events` | List risk events |
| `POST` | `/api/events` | Submit a new risk event |
| `GET` | `/api/events/route/{routeId}` | Risk events for a specific route |
| `GET` | `/api/routes` | List routes |
| `GET` | `/api/routes/{id}` | Route detail |
| `GET` | `/api/routes/risk-status` | Risk status across all routes |
| `GET` | `/api/suppliers` | List suppliers |
| `GET` | `/api/suppliers/{id}` | Supplier detail |
| `GET` | `/api/suppliers/market-status` | Market-driven supplier status |
| `GET` | `/api/weather/routes` | Weather risk per route |
| `GET` | `/api/scenarios` | List scenarios |
| `GET` | `/api/scenarios/{id}` | Scenario detail |
| `POST` | `/api/scenarios/{eventId}/simulate` | Simulate a scenario from a risk event |
| `GET` | `/api/recommendations/{scenarioId}` | Allocation recommendations for a scenario |

## Getting started

### Backend

Requires **Java 21+** and **PostgreSQL** running locally (or a `DB_URL` pointing elsewhere).

1. Copy the local config template and fill in your keys:
   ```bash
   cp src/main/resources/application-local.properties.template src/main/resources/application-local.properties
   ```
2. Set the required environment variables (or edit the local properties file):

   | Variable | Required | Default |
   |---|---|---|
   | `DB_URL` | recommended | `jdbc:postgresql://localhost:5432/CrisesManagment` |
   | `DB_USER` / `DB_PASSWORD` | recommended | `postgres` / `root` |
   | `GEMINI_API_KEY` | **yes** — AI extraction won't work without it | — |
   | `GROQ_API_KEY` | optional — enables dual cross-check | — |
   | `EIA_API_KEY` | optional — enables market data | — |

3. Run it:
   ```bash
   ./mvnw spring-boot:run
   ```
   API comes up on `http://localhost:8080`.

### Frontend

Requires **Node.js**.

```bash
cd frontend
cp .env.example .env   # points VITE_API_BASE_URL at the backend
npm install
npm run dev
```

Runs on `http://localhost:5173`. The backend's CORS config already allows `localhost:5173` and `localhost:3000`.

## Notes

- DevTools hot-reload is **off by default** (`DEVTOOLS_RESTART_ENABLED=false`) — the scheduled GDELT news poller fires again on every restart, and fast restarts can burst its rate limit. Turn it on if you don't mind occasional rate-limit warnings.
- `application-local.properties` is gitignored — never commit real API keys there.

---

<div align="center">Built for a hackathon 🚀</div>
