import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'
import BookingCard from '../components/BookingCard.jsx'
import Pagination from '../components/Pagination.jsx'
import StatusChip from '../components/StatusChip.jsx'
import { Empty, ErrorAlert, Spinner } from '../components/Feedback.jsx'
import { dateTime, money } from '../utils/format'

const FILTERS = ['PENDING', 'ACCEPTED', 'COMPLETED', '']

function MyQuotes() {
  const { t, i18n } = useTranslation()
  const [quotes, setQuotes] = useState(null)
  useEffect(() => {
    api.myQuotes().then(setQuotes).catch(() => setQuotes([]))
  }, [])
  if (!quotes) return <Spinner />
  if (quotes.length === 0) return <Empty>{t('quote.noneSent')} <Link to="/requirements">{t('requirement.board')}</Link></Empty>
  return (
    <div className="table-wrap card">
      <table>
        <thead>
          <tr><th>{t('requirement.titleLabel')}</th><th>{t('quote.amount')}</th><th>{t('common.status')}</th><th>{t('common.sent')}</th></tr>
        </thead>
        <tbody>
          {quotes.map((q) => (
            <tr key={q.id}>
              <td><Link to={`/requirements/${q.requirementId}`}>{q.requirementTitle}</Link>{q.equipmentTitle && <div className="muted small">{q.equipmentTitle}</div>}</td>
              <td>{money(q.amount, i18n.language)}</td>
              <td><StatusChip status={q.status} /></td>
              <td className="small">{dateTime(q.createdAt, i18n.language)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default function ProviderRequests() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const [tab, setTab] = useState('PENDING')
  const [pageNo, setPageNo] = useState(0)
  const [page, setPage] = useState(null)
  const [error, setError] = useState(null)

  const load = useCallback(() => {
    if (tab === 'quotes') return
    api.incoming({ status: tab, page: pageNo }).then(setPage).catch(setError)
  }, [tab, pageNo])

  useEffect(() => {
    setPage(null)
    load()
  }, [load])

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>{t('provider.requestsTitle')}</h1>
          <p className="muted">{t('provider.requestsSubtitle')}</p>
        </div>
        {user.role === 'SPECIALIST' && !user.verified && <span className="chip chip-sun">{t('provider.awaitingVerification')}</span>}
      </div>
      <div className="tabs">
        {FILTERS.map((f) => (
          <button key={f || 'all'} type="button" className={tab === f ? 'active' : ''} onClick={() => { setTab(f); setPageNo(0) }}>
            {f ? t(`status.${f}`) : t('common.all')}
          </button>
        ))}
        <button type="button" className={tab === 'quotes' ? 'active' : ''} onClick={() => setTab('quotes')}>{t('provider.myQuotes')}</button>
      </div>
      <ErrorAlert error={error} />
      {tab === 'quotes' ? <MyQuotes /> : !page ? <Spinner /> : page.content.length === 0 ? (
        <Empty>{t('provider.noRequests')}</Empty>
      ) : (
        <div className="list">
          {page.content.map((b, i) => <BookingCard key={`${b.id}-${i}`} booking={b} mode="provider" me={user.id} onChange={load} />)}
        </div>
      )}
      {page && tab !== 'quotes' && <Pagination page={page.page} totalPages={page.totalPages} onChange={setPageNo} />}
    </div>
  )
}
