import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useFarmLocation } from '../context/LocationContext.jsx'
import LocationBar from '../components/LocationBar.jsx'
import Pagination from '../components/Pagination.jsx'
import SpecialistCard from '../components/SpecialistCard.jsx'
import { Empty, ErrorAlert, Spinner } from '../components/Feedback.jsx'
import { SKILLS } from '../utils/options'
import { SourceNote } from './EquipmentList.jsx'

export default function Specialists() {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const { location } = useFarmLocation()
  const [page, setPage] = useState(null)
  const [error, setError] = useState(null)
  const skill = params.get('skill') || ''
  const sort = params.get('sort') || 'distance'
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
      .specialists({ lat: location?.lat, lng: location?.lng, radiusKm: location ? 60 : undefined, skill, sort, page: pageNo })
      .then(setPage)
      .catch(setError)
  }, [location, skill, sort, pageNo])

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>{t('specialist.title')}</h1>
          <p className="muted">{t('specialist.subtitle')}</p>
        </div>
      </div>
      <LocationBar />
      <div className="toolbar">
        <select value={skill} onChange={(e) => update('skill', e.target.value)} aria-label={t('auth.skill')}>
          <option value="">{t('specialist.allSkills')}</option>
          {SKILLS.map((s) => <option key={s} value={s}>{t(`skill.${s}`)}</option>)}
        </select>
        <select value={sort} onChange={(e) => update('sort', e.target.value)} aria-label={t('common.sortBy')}>
          <option value="distance">{t('sort.distance')}</option>
          <option value="price">{t('sort.price')}</option>
          <option value="rating">{t('sort.rating')}</option>
        </select>
        {page && <span className="muted small">{t('common.results', { count: page.totalElements })}</span>}
      </div>
      <ErrorAlert error={error} />
      <SourceNote page={page} />
      {!page && !error ? (
        <Spinner />
      ) : page && page.content.length === 0 ? (
        <Empty>{t('specialist.none')}</Empty>
      ) : (
        page && (
          <div className="grid">
            {page.content.map((s) => <SpecialistCard key={s.id} item={s} />)}
          </div>
        )
      )}
      {page && <Pagination page={page.page} totalPages={page.totalPages} onChange={(p) => update('page', String(p))} />}
    </div>
  )
}
