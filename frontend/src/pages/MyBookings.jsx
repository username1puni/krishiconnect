import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import BookingCard from '../components/BookingCard.jsx'
import Pagination from '../components/Pagination.jsx'
import { Empty, ErrorAlert, Spinner } from '../components/Feedback.jsx'

const FILTERS = ['', 'PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'EXPIRED', 'REJECTED']

export default function MyBookings() {
  const { t } = useTranslation()
  const [status, setStatus] = useState('')
  const [pageNo, setPageNo] = useState(0)
  const [page, setPage] = useState(null)
  const [reviewed, setReviewed] = useState([])
  const [error, setError] = useState(null)

  const load = useCallback(() => {
    api.myBookings({ status, page: pageNo }).then(setPage).catch(setError)
    api.myReviewedItems().then(setReviewed).catch(() => {})
  }, [status, pageNo])

  useEffect(() => {
    setPage(null)
    load()
  }, [load])

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>{t('booking.myTitle')}</h1>
          <p className="muted">{t('booking.mySubtitle')}</p>
        </div>
        <Link to="/equipment" className="btn btn-primary">{t('home.findEquipment')}</Link>
      </div>
      <div className="tabs">
        {FILTERS.map((f) => (
          <button key={f || 'all'} type="button" className={status === f ? 'active' : ''} onClick={() => { setStatus(f); setPageNo(0) }}>
            {f ? t(`status.${f}`) : t('common.all')}
          </button>
        ))}
      </div>
      <ErrorAlert error={error} />
      {!page ? <Spinner /> : page.content.length === 0 ? (
        <Empty>{t('booking.none')}</Empty>
      ) : (
        <div className="list">
          {page.content.map((b) => <BookingCard key={b.id} booking={b} mode="seeker" reviewed={reviewed} onChange={load} />)}
        </div>
      )}
      {page && <Pagination page={page.page} totalPages={page.totalPages} onChange={setPageNo} />}
    </div>
  )
}
