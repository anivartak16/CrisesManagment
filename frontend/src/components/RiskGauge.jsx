/**
 * RiskGauge — a semicircular instrument dial modeled on a ship's engine
 * order telegraph. Used everywhere a risk/severity number appears so the
 * whole app reads its numbers the same way, like readouts on one console.
 *
 * `value` and `max` define the sweep; the needle rotates from -90deg
 * (min) to +90deg (max) across a track that shades from sea to amber to rust.
 */
export default function RiskGauge({ value, max = 10, size = 64, label }) {
  const clamped = Math.max(0, Math.min(value, max));
  const pct = clamped / max;
  const angle = -90 + pct * 180;

  const cx = size / 2;
  const cy = size / 2;
  const r = size / 2 - 6;

  const polarToXY = (deg) => {
    const rad = (deg * Math.PI) / 180;
    return [cx + r * Math.cos(rad), cy + r * Math.sin(rad)];
  };

  const arcPath = (startDeg, endDeg) => {
    const [x1, y1] = polarToXY(startDeg);
    const [x2, y2] = polarToXY(endDeg);
    const largeArc = endDeg - startDeg > 180 ? 1 : 0;
    return `M ${x1} ${y1} A ${r} ${r} 0 ${largeArc} 1 ${x2} ${y2}`;
  };

  const [needleX, needleY] = polarToXY(180 + angle);

  let tone = "var(--sea)";
  if (pct >= 0.75) tone = "var(--rust)";
  else if (pct >= 0.45) tone = "var(--amber)";

  return (
    <div className="gauge">
      <svg width={size} height={size / 2 + 8} viewBox={`0 0 ${size} ${size / 2 + 8}`}>
        <path
          d={arcPath(180, 360)}
          fill="none"
          stroke="var(--line)"
          strokeWidth="5"
          strokeLinecap="round"
        />
        <path
          d={arcPath(180, 180 + pct * 180)}
          fill="none"
          stroke={tone}
          strokeWidth="5"
          strokeLinecap="round"
        />
        <line
          x1={cx}
          y1={cy}
          x2={needleX}
          y2={needleY}
          stroke="var(--paper)"
          strokeWidth="2"
          strokeLinecap="round"
        />
        <circle cx={cx} cy={cy} r="3" fill="var(--paper)" />
      </svg>
      {label !== false && (
        <div className="gauge-value">
          <strong style={{ color: tone }}>{clamped}</strong>
          <span style={{ color: "var(--muted)" }}> / {max}</span>
        </div>
      )}
    </div>
  );
}
