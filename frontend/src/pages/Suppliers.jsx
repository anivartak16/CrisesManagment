import { useEffect, useState } from "react";
import { api } from "../api.js";
import ErrorBanner from "../components/ErrorBanner.jsx";
import RiskGauge from "../components/RiskGauge.jsx";

export default function Suppliers() {
  const [suppliers, setSuppliers] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.getSuppliers().then(setSuppliers).catch((e) => setError(e.message));
  }, []);

  return (
    <div>
      <p className="page-eyebrow">Reference data</p>
      <h1 className="page-title">Suppliers</h1>
      <p className="page-lede">
        Crude producers currently under contract or evaluation, with baseline cost, capacity, and
        risk exposure.
      </p>

      <ErrorBanner message={error} />

      {!error && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Supplier</th>
                <th>Country</th>
                <th>Cost / bbl</th>
                <th>Capacity</th>
                <th>Risk baseline</th>
              </tr>
            </thead>
            <tbody>
              {suppliers === null && (
                <tr>
                  <td colSpan={5}>
                    <div className="empty-state">Loading suppliers…</div>
                  </td>
                </tr>
              )}
              {suppliers?.length === 0 && (
                <tr>
                  <td colSpan={5}>
                    <div className="empty-state">
                      <div className="empty-state-glyph">···</div>
                      No suppliers on file.
                    </div>
                  </td>
                </tr>
              )}
              {suppliers?.map((s) => (
                <tr key={s.id}>
                  <td style={{ fontWeight: 600 }}>{s.name}</td>
                  <td>{s.country}</td>
                  <td className="num">${s.baseCostPerBarrel?.toFixed(2)}</td>
                  <td className="num">{s.capacity?.toLocaleString()} bbl</td>
                  <td>
                    <RiskGauge value={Math.round((s.riskBaseline || 0) * 10)} max={10} size={44} />
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
