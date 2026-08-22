import { NavLink, Route, Routes as RouterRoutes } from "react-router-dom";
import { BASE_URL } from "./api.js";
import Dashboard from "./pages/Dashboard.jsx";
import Suppliers from "./pages/Suppliers.jsx";
import RoutesPage from "./pages/Routes.jsx";
import ScenarioConsole from "./pages/ScenarioConsole.jsx";
import WeatherImpact from "./pages/WeatherImpact.jsx";
import History from "./pages/History.jsx";
import ActivityLog from "./pages/ActivityLog.jsx";

const NAV = [
  { to: "/", label: "Overview", glyph: "01", end: true },
  { to: "/suppliers", label: "Suppliers", glyph: "02" },
  { to: "/routes", label: "Shipping Routes", glyph: "03" },
  { to: "/weather", label: "Weather Impact", glyph: "04" },
  { to: "/console", label: "Scenario Console", glyph: "05" },
  { to: "/history", label: "Scenario History", glyph: "06" },
  { to: "/activity", label: "Activity Log", glyph: "07" },
];

export default function App() {
  return (
    <div className="shell">
      <aside className="rail">
        <div>
          <div className="rail-brand">
            <span className="rail-brand-mark" aria-hidden="true" />
            <span className="rail-brand-text">CRUDE LINE</span>
          </div>
          <div className="rail-brand-sub">crisis console</div>
        </div>

        <nav className="rail-nav">
          {NAV.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => `rail-link${isActive ? " active" : ""}`}
            >
              <span className="rail-link-glyph">{item.glyph}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="rail-footer">
          <strong>India crude imports</strong>
          <br />
          supplier &amp; route risk desk
        </div>
      </aside>

      <div className="main">
        <div className="topstrip">
          <div className="topstrip-item">
            <span className="dot" aria-hidden="true" />
            API {BASE_URL}
          </div>
          <div className="topstrip-item">v0.1 — session log, not persisted</div>
        </div>

        <div className="content">
          <RouterRoutes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/suppliers" element={<Suppliers />} />
            <Route path="/routes" element={<RoutesPage />} />
            <Route path="/weather" element={<WeatherImpact />} />
            <Route path="/console" element={<ScenarioConsole />} />
            <Route path="/history" element={<History />} />
            <Route path="/activity" element={<ActivityLog />} />
          </RouterRoutes>
        </div>
      </div>
    </div>
  );
}
