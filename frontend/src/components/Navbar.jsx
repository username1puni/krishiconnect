import { useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext.jsx'
import LanguageSwitcher from './LanguageSwitcher.jsx'
import NotificationBell from './NotificationBell.jsx'
import Logo from './Logo.jsx'

export default function Navbar() {
  const { t } = useTranslation()
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const role = user?.role

  const links = [
    { to: '/equipment', label: t('nav.equipment') },
    { to: '/specialists', label: t('nav.specialists') },
    { to: '/requirements', label: t('nav.requirements') },
    { to: '/seasonal', label: t('nav.seasonal') },
  ]
  if (role && role !== 'ADMIN') links.push({ to: '/bookings', label: t('nav.myBookings') })
  if (role === 'PROVIDER') links.push({ to: '/provider/equipment', label: t('nav.myEquipment') })
  if (role === 'PROVIDER' || role === 'SPECIALIST') links.push({ to: '/provider/bookings', label: t('nav.requests') })
  if (role === 'ADMIN') links.push({ to: '/admin', label: t('nav.admin') })

  return (
    <header className="navbar">
      <div className="navbar-inner">
        <Link to="/" className="brand" onClick={() => setOpen(false)}>
          <Logo />
          <span>KrishiConnect</span>
        </Link>
        <button type="button" className="menu-toggle" onClick={() => setOpen(!open)} aria-expanded={open}>
          {t('nav.menu')}
        </button>
        <nav className={open ? 'nav-links open' : 'nav-links'} onClick={() => setOpen(false)}>
          {links.map((l) => (
            <NavLink key={l.to} to={l.to}>
              {l.label}
            </NavLink>
          ))}
        </nav>
        <div className="nav-right">
          <LanguageSwitcher />
          {user ? (
            <>
              <NotificationBell />
              <Link to="/profile" className="nav-user" title={t('nav.profile')}>
                {user.fullName.split(' ')[0]}
              </Link>
              <button
                type="button"
                className="btn btn-ghost btn-sm"
                onClick={() => {
                  logout()
                  navigate('/')
                }}
              >
                {t('nav.logout')}
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn btn-ghost btn-sm">{t('nav.login')}</Link>
              <Link to="/register" className="btn btn-primary btn-sm">{t('nav.register')}</Link>
            </>
          )}
        </div>
      </div>
    </header>
  )
}
