const TONE_MAP = {
  sea: "badge-sea",
  amber: "badge-amber",
  rust: "badge-rust",
  neutral: "badge-neutral",
};

export default function StatusBadge({ tone = "neutral", children }) {
  return <span className={`badge ${TONE_MAP[tone] || TONE_MAP.neutral}`}>{children}</span>;
}
