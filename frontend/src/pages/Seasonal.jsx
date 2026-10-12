import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'
import { useFarmLocation } from '../context/LocationContext.jsx'
import { Empty, Spinner } from '../components/Feedback.jsx'

const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1)

export function monthName(m, lang) {
  return new Date(2026, m - 1, 1).toLocaleDateString(lang, { month: 'short' })
}

export function RecommendationCard({ r }) {
  const { t, i18n } = useTranslation()
  const target = r.equipmentCategory
    ? { to: `/equipment?category=${r.equipmentCategory}`, label: t(`category.${r.equipmentCategory}`) }
    : { to: `/specialists?skill=${r.skill}`, label: t(`skill.${r.skill}`) }
  return (
    <div className={r.yourCrop ? 'card reco mine' : 'card reco'}>
      <div className="row between">
        <strong>{t(`crop.${r.crop}`)}</strong>
        <span className="chip chip-sun">{t(`season.${r.season}`)}</span>
      </div>
      <div className="muted small">
        {t(`activity.${r.activity}`)} · {monthName(r.monthFrom, i18n.language)} - {monthName(r.monthTo, i18n.language)}
      </div>
      <p className="mt small">{r.tip}</p>
      <div className="row between">
        <Link to={target.to} className="btn btn-outline btn-sm">{target.label}</Link>
        {r.availableNearby !== null && r.availableNearby !== undefined && (
          <span className="muted small">{t('seasonal.availableNearby', { count: r.availableNearby })}</span>
        )}
      </div>
      {r.yourCrop && <div className="small verified mt">{t('seasonal.yourCrop')}</div>}
    </div>
  )
}

export default function Seasonal() {
  const { t, i18n } = useTranslation()
  const { user } = useAuth()
  const { location } = useFarmLocation()
  const [month, setMonth] = useState(new Date().getMonth() + 1)
  const [crop, setCrop] = useState('')
  const [crops, setCrops] = useState([])
  const [items, setItems] = useState(null)

  useEffect(() => {
    api.crops().then(setCrops).catch(() => {})
  }, [])

  useEffect(() => {
    setItems(null)
    api
      .recommendations({ month, crop, lat: location?.lat, lng: location?.lng })
      .then(setItems)
      .catch(() => setItems([]))
  }, [month, crop, location])

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>{t('seasonal.title')}</h1>
          <p className="muted">{t('seasonal.subtitle')}</p>
        </div>
      </div>
      <div className="toolbar">
        <select value={month} onChange={(e) => setMonth(Number(e.target.value))} aria-label={t('seasonal.month')}>
          {MONTHS.map((m) => (
            <option key={m} value={m}>
              {new Date(2026, m - 1, 1).toLocaleDateString(i18n.language, { month: 'long' })}
            </option>
          ))}
        </select>
        <select value={crop} onChange={(e) => setCrop(e.target.value)} aria-label={t('seasonal.crop')}>
          <option value="">{t('seasonal.allCrops')}</option>
          {crops.filter((c) => c !== 'ALL').map((c) => <option key={c} value={c}>{t(`crop.${c}`)}</option>)}
        </select>
        {!user && <span className="muted small">{t('seasonal.loginHint')}</span>}
        {user && !user.crops?.length && user.role === 'SEEKER' && (
          <Link to="/profile" className="small">{t('seasonal.addCrops')}</Link>
        )}
      </div>
      {items === null ? (
        <Spinner />
      ) : items.length === 0 ? (
        <Empty>{t('seasonal.none')}</Empty>
      ) : (
        <div className="grid">
          {items.map((r) => <RecommendationCard key={r.ruleId} r={r} />)}
        </div>
      )}
      <p className="muted small mt">{t('seasonal.howItWorks')}</p>
    </div>
  )
}
