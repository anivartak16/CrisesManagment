const TONE_MAP = {
  sea: "badge-sea",
  amber: "badge-amber",
  rust: "badge-rust",
  neutral: "badge-neutral",
};

// Small glyph per tone so the badge reads at a glance even before the
// color registers — same instrument-panel logic as the RiskGauge needle.
const GLYPH_MAP = {
  sea: "●",
  amber: "▲",
  rust: "⬥",
  neutral: "·",
};

export default function StatusBadge({ tone = "neutral", children }) {
  return (
      <span className={`badge ${TONE_MAP[tone] || TONE_MAP.neutral}`}>
      <span className="badge-glyph" aria-hidden="true">
        {GLYPH_MAP[tone] || GLYPH_MAP.neutral}
      </span>
        {children}
    </span>
  );
}