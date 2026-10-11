import { CircleMarker, MapContainer, Popup, TileLayer } from 'react-leaflet'
import { Link } from 'react-router-dom'

/**
 * Plain circle markers instead of Leaflet's default pin images, which break with bundlers.
 * points: [{ id, lat, lng, title, subtitle, to }]
 */
export default function MapView({ center, points, small = false, zoom = 10 }) {
  if (!center) return null
  return (
    <div className={small ? 'map map-small' : 'map'}>
      <MapContainer center={[center.lat, center.lng]} zoom={zoom} scrollWheelZoom={false} style={{ height: '100%' }}>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <CircleMarker center={[center.lat, center.lng]} radius={9} pathOptions={{ color: '#c0392b', fillOpacity: 0.9 }}>
          <Popup>{center.label || 'You'}</Popup>
        </CircleMarker>
        {points.map((p) => (
          <CircleMarker
            key={p.id}
            center={[p.lat, p.lng]}
            radius={7}
            pathOptions={{ color: '#1f5130', fillColor: '#3a8550', fillOpacity: 0.85 }}
          >
            <Popup>
              {p.to ? <Link to={p.to}>{p.title}</Link> : p.title}
              {p.subtitle && <div>{p.subtitle}</div>}
            </Popup>
          </CircleMarker>
        ))}
      </MapContainer>
    </div>
  )
}
