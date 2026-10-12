import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import Pagination from '../components/Pagination.jsx'
import { Empty, Spinner } from '../components/Feedback.jsx'
import { dateTime } from '../utils/format'

export default function Notifications() {
  const { t, i18n } = useTranslation()
  const [page, setPage] = useState(null)
  const [pageNo, setPageNo] = useState(0)

  const load = useCallback(() => api.notifications(pageNo).then(setPage).catch(() => setPage({ content: [] })), [pageNo])
  useEffect(() => {
    load()
  }, [load])

  const open = (n) => {
    if (!n.read) api.markRead(n.id).then(load).catch(() => {})
  }

  return (
    <div className="page page-narrow" style={{ maxWidth: 760 }}>
      <div className="page-head">
        <h1>{t('notifications.title')}</h1>
        <button type="button" className="btn btn-outline btn-sm" onClick={() => api.markAllRead().then(load)}>
          {t('notifications.markAll')}
        </button>
      </div>
      {!page ? <Spinner /> : page.content.length === 0 ? (
        <Empty>{t('notifications.none')}</Empty>
      ) : (
        <div className="list">
          {page.content.map((n) => (
            <div key={n.id} className={n.read ? 'card notification' : 'card notification unread'} onClick={() => open(n)}>
              {!n.read && <span className="dot" />}
              <div className="grow">
                <div className="row between">
                  <strong>{t(`notifications.type.${n.type}`, n.title)}</strong>
                  <span className="muted small">{dateTime(n.createdAt, i18n.language)}</span>
                </div>
                <p className="small" style={{ margin: '0.2rem 0' }}>{n.message}</p>
                {n.link && <Link to={n.link} className="small">{t('notifications.open')}</Link>}
              </div>
            </div>
          ))}
        </div>
      )}
      {page && <Pagination page={page.page} totalPages={page.totalPages} onChange={setPageNo} />}
    </div>
  )
}
