import { MapContainer, TileLayer, CircleMarker, Popup } from "react-leaflet";

const severityColor = (s) =>
    s >= 7 ? "var(--rust)" : s >= 4 ? "var(--amber)" : "var(--sea)";

export default function RouteMap({ routes }) {
    const mappableRoutes = (routes ?? []).filter(
        (r) => Number.isFinite(r.originLat) && Number.isFinite(r.originLng)
    );

    const riskScore = (route) => {
        const raw = route.baseRiskScore ?? route.riskScore ?? 0;
        return raw <= 1 ? raw * 10 : raw;
    };

    const center = mappableRoutes.length
        ? [mappableRoutes[0].originLat, mappableRoutes[0].originLng]
        : [20.5937, 78.9629];

    return (
        <MapContainer
            center={center}
            zoom={3}
            style={{ height: "420px", borderRadius: "var(--radius-lg)" }}
        >
            <TileLayer
                url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
                attribution="&copy; OpenStreetMap &copy; CARTO"
            />
            {mappableRoutes.map((r) => (
                <CircleMarker
                    key={r.id}
                    center={[r.originLat, r.originLng]}
                    radius={8}
                    pathOptions={{ color: severityColor(riskScore(r)), fillOpacity: 0.8 }}
                >
                    <Popup>
                        <strong>{r.name}</strong><br />
                        Risk: {riskScore(r).toFixed(1)}/10
                    </Popup>
                </CircleMarker>
            ))}
        </MapContainer>
    );
}