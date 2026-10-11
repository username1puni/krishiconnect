import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext.jsx'
import { ErrorAlert } from '../components/Feedback.jsx'

// accounts created by the demo seed data
const DEMO = [
  { key: 'seeker', id: '9900000001' },
  { key: 'provider', id: '9900000002' },
  { key: 'specialist', id: '9900000003' },
]

export default function Login() {
  const { t } = useTranslation()
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const user = await login(identifier.trim(), password)
      const home = user.role === 'ADMIN' ? '/admin' : user.role === 'SEEKER' ? '/equipment' : '/provider/bookings'
      navigate(location.state?.from || home, { replace: true })
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page page-narrow">
      <div className="card">
        <h1>{t('auth.loginTitle')}</h1>
        <p className="muted">{t('auth.loginSubtitle')}</p>
        <ErrorAlert error={error} />
        <form onSubmit={submit}>
          <div className="field">
            <label htmlFor="identifier">{t('auth.phoneOrEmail')}</label>
            <input id="identifier" value={identifier} onChange={(e) => setIdentifier(e.target.value)}
              autoComplete="username" required />
          </div>
          <div className="field">
            <label htmlFor="password">{t('auth.password')}</label>
            <input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password" required />
          </div>
          <button className="btn btn-primary btn-block" disabled={busy}>
            {busy ? t('common.wait') : t('auth.login')}
          </button>
        </form>
        <p className="mt small">
          {t('auth.noAccount')} <Link to="/register">{t('auth.registerLink')}</Link>
        </p>
        <div className="alert alert-info demo-logins">
          <strong>{t('auth.demoTitle')}</strong> {t('auth.demoText')}
          <div>
            {DEMO.map((d) => (
              <button key={d.key} type="button" className="btn btn-outline btn-sm"
                onClick={() => {
                  setIdentifier(d.id)
                  setPassword('demo1234')
                }}>
                {t(`role.${d.key.toUpperCase()}`)}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
