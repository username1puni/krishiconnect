import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { dateOnly } from '../utils/format'
import Pagination from './Pagination.jsx'

export default function ReviewList({ load }) {
  const { t, i18n } = useTranslation()
  const [page, setPage] = useState(null)
  const [pageNo, setPageNo] = useState(0)

  useEffect(() => {
    load(pageNo).then(setPage).catch(() => setPage(null))
  }, [load, pageNo])

  if (!page) return null
  if (page.content.length === 0) return <p className="muted small">{t('reviews.none')}</p>
  return (
    <div className="list">
      {page.content.map((r) => (
        <div key={r.id} className="booking-item" style={{ marginTop: 0 }}>
          <div className="row between">
            <strong>{r.reviewerName}</strong>
            <span className="stars">{'★'.repeat(r.rating)}{'☆'.repeat(5 - r.rating)}</span>
          </div>
          <div className="muted small">{r.reviewerVillage} · {dateOnly(r.createdAt, i18n.language)}</div>
          {r.comment && <p className="small" style={{ marginTop: '0.3rem' }}>{r.comment}</p>}
        </div>
      ))}
      <Pagination page={page.page} totalPages={page.totalPages} onChange={setPageNo} />
    </div>
  )
}
