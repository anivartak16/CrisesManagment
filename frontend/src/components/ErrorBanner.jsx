export default function ErrorBanner({ message }) {
    if (!message) return null;
    return (
        <div className="error-banner">
            <span className="mono">✕</span>
            <span>{message}</span>
        </div>
    );
}