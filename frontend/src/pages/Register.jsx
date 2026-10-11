import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext.jsx'
import { ErrorAlert } from '../components/Feedback.jsx'
import { PlaceSearch } from '../components/LocationBar.jsx'
import { SKILLS } from '../utils/options'

const ROLES = ['SEEKER', 'PROVIDER', 'SPECIALIST']

export default function Register() {
  const { t, i18n } = useTranslation()
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ fullName: '', phone: '', email: '', password: '', role: 'SEEKER', skill: 'TRACTOR_DRIVER' })
  const [place, setPlace] = useState(null)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })
  const fieldError = (k) => error?.fields?.[k] && <div className="field-error">{error.fields[k]}</div>

  const submit = async (e) => {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try {
      const user = await register({
        ...form,
        email: form.email || null,
        skill: form.role === 'SPECIALIST' ? form.skill : null,
        village: place?.name || null,
        district: place?.district || null,
        latitude: place?.latitude ?? null,
        longitude: place?.longitude ?? null,
        preferredLang: i18n.language,
      })
      navigate(user.role === 'SEEKER' ? '/equipment' : '/profile', { replace: true })
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page page-narrow">
      <div className="card">
        <h1>{t('auth.registerTitle')}</h1>
        <p className="muted">{t('auth.registerSubtitle')}</p>
        <ErrorAlert error={error && !error.fields ? error : null} />
        <form onSubmit={submit}>
          <div className="field">
            <label>{t('auth.iWantTo')}</label>
            <div className="stack" style={{ gap: '0.4rem' }}>
              {ROLES.map((r) => (
                <label key={r} className="check">
                  <input type="radio" name="role" value={r} checked={form.role === r} onChange={set('role')} />
                  <span><strong>{t(`role.${r}`)}</strong> <span className="muted small">{t(`auth.roleHint.${r}`)}</span></span>
                </label>
              ))}
            </div>
          </div>
          {form.role === 'SPECIALIST' && (
            <div className="field">
              <label htmlFor="skill">{t('auth.skill')}</label>
              <select id="skill" value={form.skill} onChange={set('skill')}>
                {SKILLS.map((s) => <option key={s} value={s}>{t(`skill.${s}`)}</option>)}
              </select>
            </div>
          )}
          <div className="field">
            <label htmlFor="fullName">{t('auth.fullName')}</label>
            <input id="fullName" value={form.fullName} onChange={set('fullName')} required />
            {fieldError('fullName')}
          </div>
          <div className="form-row">
            <div className="field">
              <label htmlFor="phone">{t('auth.phone')}</label>
              <input id="phone" inputMode="numeric" maxLength={10} value={form.phone} onChange={set('phone')}
                placeholder="98xxxxxxxx" required />
              {fieldError('phone')}
            </div>
            <div className="field">
              <label htmlFor="email">{t('auth.emailOptional')}</label>
              <input id="email" type="email" value={form.email} onChange={set('email')} />
              {fieldError('email')}
            </div>
          </div>
          <div className="field">
            <label htmlFor="password">{t('auth.password')}</label>
            <input id="password" type="password" minLength={6} value={form.password} onChange={set('password')}
              autoComplete="new-password" required />
            {fieldError('password')}
          </div>
          <div className="field">
            <label>{t('auth.village')}</label>
            {place ? (
              <div className="row">
                <span className="chip">{place.name}, {place.district}</span>
                <button type="button" className="btn btn-outline btn-sm" onClick={() => setPlace(null)}>{t('common.change')}</button>
              </div>
            ) : (
              <PlaceSearch onPick={setPlace} placeholder={t('auth.villagePlaceholder')} />
            )}
            <div className="muted small">{t('auth.villageHint')}</div>
          </div>
          <button className="btn btn-primary btn-block" disabled={busy}>
            {busy ? t('common.wait') : t('auth.createAccount')}
          </button>
        </form>
        {form.role !== 'SEEKER' && <p className="muted small mt">{t('auth.verifyNote')}</p>}
        <p className="small mt">
          {t('auth.haveAccount')} <Link to="/login">{t('auth.loginLink')}</Link>
        </p>
      </div>
    </div>
  )
}
