import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import Dashboard from './Dashboard.jsx'
import Users from './Users.jsx'
import Listings from './Listings.jsx'
import Bookings from './Bookings.jsx'
import Routing from './Routing.jsx'

const TABS = { dashboard: Dashboard, users: Users, listings: Listings, bookings: Bookings, routing: Routing }

export default function Admin() {
  const { t } = useTranslation()
  const [tab, setTab] = useState('dashboard')
  const Tab = TABS[tab]
  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>{t('admin.title')}</h1>
          <p className="muted">{t('admin.subtitle')}</p>
        </div>
      </div>
      <div className="tabs">
        {Object.keys(TABS).map((k) => (
          <button key={k} type="button" className={tab === k ? 'active' : ''} onClick={() => setTab(k)}>{t(`admin.tab.${k}`)}</button>
        ))}
      </div>
      <Tab />
    </div>
  )
}
