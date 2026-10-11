import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useFarmLocation } from '../context/LocationContext.jsx'

export function PlaceSearch({ onPick, placeholder }) {
  const { t } = useTranslation()
  const [q, setQ] = useState('')
  const [results, setResults] = useState([])

  useEffect(() => {
    if (q.trim().length < 2) {
      setResults([])
      return
    }
    const timer = setTimeout(() => {
      api.places(q.trim()).then(setResults).catch(() => setResults([]))
    }, 250)
    return () => clearTimeout(timer)
  }, [q])

  return (
    <div className="place-search">
      <input
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder={placeholder || t('location.searchPlaceholder')}
        aria-label={t('location.searchPlaceholder')}
      />
      {results.length > 0 && (
        <ul className="place-results">
          {results.map((p) => (
            <li key={p.id}>
              <button
                type="button"
                onClick={() => {
                  onPick(p)
                  setQ('')
                  setResults([])
                }}
              >
                {p.name} <span className="muted small">{p.district}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default function LocationBar() {
  const { t } = useTranslation()
  const { location, setLocation, useGps, locating, error } = useFarmLocation()

  return (
    <div className="location-bar">
      <span className="muted small">{t('location.searchingFrom')}</span>
      <span className="where">
        {location
          ? location.label || `${location.lat.toFixed(3)}, ${location.lng.toFixed(3)}`
          : t('location.notSet')}
      </span>
      {location?.source === 'gps' && <span className="chip">{t('location.gps')}</span>}
      <span className="grow" />
      <button type="button" className="btn btn-outline btn-sm" onClick={useGps} disabled={locating}>
        {locating ? t('location.locating') : t('location.useGps')}
      </button>
      <PlaceSearch
        onPick={(p) => setLocation({ lat: p.latitude, lng: p.longitude, label: p.name, source: 'place' })}
      />
      {error && <span className="field-error">{t(`location.${error}`)}</span>}
    </div>
  )
}
