import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'

const POLL_MS = 20000

export default function NotificationBell() {
  const { t } = useTranslation()
  const [count, setCount] = useState(0)
  const location = useLocation()

  useEffect(() => {
    let alive = true
    const load = () => api.unreadCount().then((r) => alive && setCount(r.count)).catch(() => {})
    load()
    const timer = setInterval(load, POLL_MS)
    return () => {
      alive = false
      clearInterval(timer)
    }
  }, [location.pathname])

  return (
    <Link to="/notifications" className="bell" aria-label={t('nav.notifications')} title={t('nav.notifications')}>
      <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
        <path
          fill="currentColor"
          d="M12 22a2.5 2.5 0 0 0 2.45-2h-4.9A2.5 2.5 0 0 0 12 22zm7-6V11a7 7 0 0 0-5.5-6.84V3.5a1.5 1.5 0 0 0-3 0v.66A7 7 0 0 0 5 11v5l-2 2v1h18v-1z"
        />
      </svg>
      {count > 0 && <span className="badge">{count > 99 ? '99+' : count}</span>}
    </Link>
  )
}
