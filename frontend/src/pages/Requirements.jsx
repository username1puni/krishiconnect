import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'
import { useFarmLocation } from '../context/LocationContext.jsx'
import LocationBar from '../components/LocationBar.jsx'
import Pagination from '../components/Pagination.jsx'
import StatusChip from '../components/StatusChip.jsx'
import { Empty, ErrorAlert, Spinner } from '../components/Feedback.jsx'
import { km, money, timeRange } from '../utils/format'

export function RequirementSummary({ r }) {
  const { t, i18n } = useTranslation()
  const what = r.needType === 'EQUIPMENT' ? t(`category.${r.category}`) : t(`skill.${r.skill}`)
  return (
    <Link to={`/requirements/${r.id}`} className="card card-link">
      <div className="row between">
        <strong>{r.title}</strong>
        <StatusChip status={r.status} />
      </div>
      <div className="row mt" style={{ gap: '0.4rem' }}>
        <span className="chip">{what}</span>
        {r.crop && <span className="chip chip-sun">{t(`crop.${r.crop}`, r.crop)}</span>}
        {r.areaAcres && <span className="chip chip-grey">{t('requirement.acres', { count: Number(r.areaAcres) })}</span>}
      </div>
      <div className="muted small mt">
        {r.village}, {r.district}
        {r.distanceKm != null && ` · ${t('distance.km', { km: km(r.distanceKm) })}`} · {r.seekerName}
      </div>
      <div className="small">{timeRange(r.startAt, r.endAt, i18n.language)}</div>
      <div className="row between mt">
        <span className="small">{r.budget ? `${t('requirement.budget')}: ${money(r.budget, i18n.language)}` : t('requirement.noBudget')}</span>
        <span className="muted small">{t('requirement.quotes', { count: r.quoteCount })}</span>
      </div>
    </Link>
  )
}

export default function Requirements() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const { location } = useFarmLocation()
  const [tab, setTab] = useState('board')
  const [needType, setNeedType] = useState('')
  const [pageNo, setPageNo] = useState(0)
  const [page, setPage] = useState(null)
  const [mine, setMine] = useState(null)
  const [error, setError] = useState(null)
  const isSeeker = user?.role === 'SEEKER'

  useEffect(() => {
    if (tab !== 'board') return
    setPage(null)
    api
      .requirements({ lat: location?.lat, lng: location?.lng, radiusKm: location ? 60 : undefined, needType, page: pageNo })
      .then(setPage)
      .catch(setError)
  }, [tab, location, needType, pageNo])

  useEffect(() => {
    if (tab === 'mine' && isSeeker) api.myRequirements().then(setMine).catch(setError)
  }, [tab, isSeeker])

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>{t('requirement.title')}</h1>
          <p className="muted">{t('requirement.subtitle')}</p>
        </div>
        {isSeeker && <Link to="/requirements/new" className="btn btn-primary">{t('requirement.post')}</Link>}
      </div>
      {isSeeker && (
        <div className="tabs">
          <button type="button" className={tab === 'board' ? 'active' : ''} onClick={() => setTab('board')}>{t('requirement.board')}</button>
          <button type="button" className={tab === 'mine' ? 'active' : ''} onClick={() => setTab('mine')}>{t('requirement.mine')}</button>
        </div>
      )}
      <ErrorAlert error={error} />
      {tab === 'board' ? (
        <>
          <LocationBar />
          <div className="toolbar">
            <select value={needType} onChange={(e) => { setNeedType(e.target.value); setPageNo(0) }} aria-label={t('requirement.needType')}>
              <option value="">{t('requirement.allNeeds')}</option>
              <option value="EQUIPMENT">{t('requirement.needEquipment')}</option>
              <option value="SPECIALIST">{t('requirement.needSpecialist')}</option>
            </select>
            {!user && <span className="muted small">{t('requirement.loginToQuote')}</span>}
          </div>
          {!page ? <Spinner /> : page.content.length === 0 ? (
            <Empty>{t('requirement.none')}</Empty>
          ) : (
            <div className="grid">{page.content.map((r) => <RequirementSummary key={r.id} r={r} />)}</div>
          )}
          {page && <Pagination page={page.page} totalPages={page.totalPages} onChange={setPageNo} />}
        </>
      ) : !mine ? <Spinner /> : mine.length === 0 ? (
        <Empty>{t('requirement.noneMine')}</Empty>
      ) : (
        <div className="grid">{mine.map((r) => <RequirementSummary key={r.id} r={r} />)}</div>
      )}
    </div>
  )
}
