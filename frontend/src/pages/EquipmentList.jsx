import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useFarmLocation } from '../context/LocationContext.jsx'
import EquipmentCard from '../components/EquipmentCard.jsx'
import LocationBar from '../components/LocationBar.jsx'
import MapView from '../components/MapView.jsx'
import Pagination from '../components/Pagination.jsx'
import { Empty, ErrorAlert, Spinner } from '../components/Feedback.jsx'
import { CATEGORIES } from '../utils/options'
import { km } from '../utils/format'

export function SourceNote({ page }) {
  const { t } = useTranslation()
  if (!page || page.content.length === 0) return null
  if (page.straightLineDistances > 0 && page.roadDistances === 0) {
    return <div className="alert alert-warn small">{t('distance.fallbackNote')}</div>
  }
  if (page.roadDistances > 0) {
    return <p className="muted small">{t('distance.roadNote', { count: page.roadDistances })}</p>
  }
  return null
}

export default function EquipmentList() {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const { location } = useFarmLocation()
  const [page, setPage] = useState(null)
  const [error, setError] = useState(null)
  const [view, setView] = useState('list')

  const category = params.get('category') || ''
  const sort = params.get('sort') || 'distance'
  const radius = params.get('radius') || '50'
  const pageNo = Number(params.get('page') || 0)

  const update = (k, v) => {
    const next = new URLSearchParams(params)
    if (v) next.set(k, v)
    else next.delete(k)
    if (k !== 'page') next.delete('page')
    setParams(next)
  }

  useEffect(() => {
    setPage(null)
    setError(null)
    api
      .equipment({
        lat: location?.lat,
        lng: location?.lng,
        radiusKm: location ? radius : undefined,
        category,
        sort,
        page: pageNo,
      })
      .then(setPage)
      .catch(setError)
  }, [location, category, sort, radius, pageNo])

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>{t('equipment.title')}</h1>
          <p className="muted">{t('equipment.subtitle')}</p>
        </div>
        <div className="segmented">
          <button type="button" className={view === 'list' ? 'active' : ''} onClick={() => setView('list')}>{t('common.list')}</button>
          <button type="button" className={view === 'map' ? 'active' : ''} onClick={() => setView('map')} disabled={!location}>{t('common.map')}</button>
        </div>
      </div>
      <LocationBar />
      <div className="toolbar">
        <select value={category} onChange={(e) => update('category', e.target.value)} aria-label={t('equipment.category')}>
          <option value="">{t('equipment.allCategories')}</option>
          {CATEGORIES.map((c) => <option key={c} value={c}>{t(`category.${c}`)}</option>)}
        </select>
        <select value={sort} onChange={(e) => update('sort', e.target.value)} aria-label={t('common.sortBy')}>
          <option value="distance">{t('sort.distance')}</option>
          <option value="price">{t('sort.price')}</option>
          <option value="rating">{t('sort.rating')}</option>
        </select>
        {location && (
          <select value={radius} onChange={(e) => update('radius', e.target.value)} aria-label={t('common.within')}>
            {['10', '25', '50', '100'].map((r) => <option key={r} value={r}>{t('common.withinKm', { km: r })}</option>)}
          </select>
        )}
        {page && <span className="muted small">{t('common.results', { count: page.totalElements })}</span>}
      </div>
      {!location && <div className="alert alert-info small">{t('location.setForDistance')}</div>}
      <ErrorAlert error={error} />
      <SourceNote page={page} />
      {!page && !error ? (
        <Spinner />
      ) : page && page.content.length === 0 ? (
        <Empty>{t('equipment.none')}</Empty>
      ) : page && view === 'map' && location ? (
        <MapView
          center={location}
          points={page.content.map((e) => ({
            id: e.id,
            lat: e.latitude,
            lng: e.longitude,
            title: e.title,
            subtitle: e.distanceKm != null ? t('distance.km', { km: km(e.distanceKm) }) : e.village,
            to: `/equipment/${e.id}`,
          }))}
        />
      ) : (
        page && (
          <div className="grid">
            {page.content.map((e) => <EquipmentCard key={e.id} item={e} />)}
          </div>
        )
      )}
      {page && <Pagination page={page.page} totalPages={page.totalPages} onChange={(p) => update('page', String(p))} />}
    </div>
  )
}
