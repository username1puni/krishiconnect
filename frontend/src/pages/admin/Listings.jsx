import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../../api/client'
import Pagination from '../../components/Pagination.jsx'
import Photo from '../../components/Photo.jsx'
import { Empty, ErrorAlert, Spinner } from '../../components/Feedback.jsx'
import { mainRate } from '../../components/EquipmentCard.jsx'

export default function Listings() {
  const { t, i18n } = useTranslation()
  const [status, setStatus] = useState('PENDING')
  const [pageNo, setPageNo] = useState(0)
  const [page, setPage] = useState(null)
  const [error, setError] = useState(null)

  const load = useCallback(() => api.admin.listings({ status, page: pageNo }).then(setPage).catch(setError), [status, pageNo])
  useEffect(() => {
    load()
  }, [load])

  const moderate = (id, s) => {
    const note = s === 'REJECTED' ? window.prompt(t('admin.rejectReason')) : null
    if (s === 'REJECTED' && note === null) return
    api.admin.moderate(id, s, note).then(load).catch(setError)
  }

  return (
    <div>
      <div className="toolbar">
        <div className="segmented">
          {['PENDING', 'APPROVED', 'REJECTED', ''].map((s) => (
            <button key={s || 'all'} type="button" className={status === s ? 'active' : ''} onClick={() => { setStatus(s); setPageNo(0) }}>
              {s ? t(`moderation.${s}`) : t('common.all')}
            </button>
          ))}
        </div>
      </div>
      <ErrorAlert error={error} />
      {!page ? <Spinner /> : page.content.length === 0 ? <Empty>{t('admin.noListings')}</Empty> : (
        <div className="list">
          {page.content.map((e) => (
            <div key={e.id} className="card list-item">
              <div style={{ width: 120, height: 90, borderRadius: 8, overflow: 'hidden', background: 'var(--green-100)', flexShrink: 0 }}>
                <Photo id={e.photoIds[0]} alt="" category={e.category} />
              </div>
              <div className="grow">
                <div className="row between">
                  <Link to={`/equipment/${e.id}`}><strong>{e.title}</strong></Link>
                  <span className={`chip status-${e.moderationStatus}`}>{t(`moderation.${e.moderationStatus}`)}</span>
                </div>
                <div className="muted small">
                  {t(`category.${e.category}`)} · {e.ownerName} {e.ownerVerified ? '✓' : `(${t('admin.notVerified')})`} · {e.village}, {e.district}
                </div>
                <div className="small">{mainRate(e, t, i18n.language)} · {t('equipment.photos', { count: e.photoIds.length })}</div>
                {e.moderationNote && <div className="small field-error">{e.moderationNote}</div>}
                <div className="row mt" style={{ gap: '0.4rem' }}>
                  {e.moderationStatus !== 'APPROVED' && <button type="button" className="btn btn-primary btn-sm" onClick={() => moderate(e.id, 'APPROVED')}>{t('admin.approve')}</button>}
                  {e.moderationStatus !== 'REJECTED' && <button type="button" className="btn btn-danger btn-sm" onClick={() => moderate(e.id, 'REJECTED')}>{t('admin.reject')}</button>}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
      {page && <Pagination page={page.page} totalPages={page.totalPages} onChange={setPageNo} />}
    </div>
  )
}
