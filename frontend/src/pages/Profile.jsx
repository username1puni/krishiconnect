import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'
import { ErrorAlert } from '../components/Feedback.jsx'
import { PlaceSearch } from '../components/LocationBar.jsx'
import { CROPS, SKILLS } from '../utils/options'

function SpecialistProfileForm({ profile, onSaved }) {
  const { t } = useTranslation()
  const [form, setForm] = useState({
    skill: profile?.skill || 'TRACTOR_DRIVER',
    bio: profile?.bio || '',
    experienceYears: profile?.experienceYears ?? 1,
    ratePerHour: profile?.ratePerHour ?? '',
    ratePerDay: profile?.ratePerDay ?? '',
    languages: profile?.languages || 'Kannada',
    available: profile?.available ?? true,
  })
  const [error, setError] = useState(null)
  const [ok, setOk] = useState(false)
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setError(null)
    setOk(false)
    try {
      const user = await api.updateSpecialist({
        ...form,
        experienceYears: Number(form.experienceYears),
        ratePerHour: form.ratePerHour === '' ? null : Number(form.ratePerHour),
        ratePerDay: form.ratePerDay === '' ? null : Number(form.ratePerDay),
      })
      onSaved(user)
      setOk(true)
    } catch (err) {
      setError(err)
    }
  }

  return (
    <form onSubmit={submit} className="card mt">
      <h2>{t('profile.workTitle')}</h2>
      {ok && <div className="alert alert-ok">{t('profile.saved')}</div>}
      <ErrorAlert error={error} />
      <div className="form-row">
        <div className="field">
          <label htmlFor="sp-skill">{t('auth.skill')}</label>
          <select id="sp-skill" value={form.skill} onChange={set('skill')}>
            {SKILLS.map((s) => <option key={s} value={s}>{t(`skill.${s}`)}</option>)}
          </select>
        </div>
        <div className="field">
          <label htmlFor="sp-exp">{t('profile.experience')}</label>
          <input id="sp-exp" type="number" min="0" max="60" value={form.experienceYears} onChange={set('experienceYears')} />
        </div>
      </div>
      <div className="form-row">
        <div className="field">
          <label htmlFor="sp-ph">{t('price.perHour')}</label>
          <input id="sp-ph" type="number" min="0" value={form.ratePerHour} onChange={set('ratePerHour')} />
        </div>
        <div className="field">
          <label htmlFor="sp-pd">{t('price.perDay')}</label>
          <input id="sp-pd" type="number" min="0" value={form.ratePerDay} onChange={set('ratePerDay')} />
        </div>
      </div>
      <div className="field">
        <label htmlFor="sp-lang">{t('profile.languages')}</label>
        <input id="sp-lang" value={form.languages} onChange={set('languages')} />
      </div>
      <div className="field">
        <label htmlFor="sp-bio">{t('profile.bio')}</label>
        <textarea id="sp-bio" value={form.bio} onChange={set('bio')} />
      </div>
      <label className="check field">
        <input type="checkbox" checked={form.available} onChange={set('available')} /> {t('profile.available')}
      </label>
      <button className="btn btn-primary">{t('common.save')}</button>
    </form>
  )
}

export default function Profile() {
  const { t, i18n } = useTranslation()
  const { user, setUser } = useAuth()
  const [form, setForm] = useState({ fullName: user.fullName, email: user.email || '' })
  const [place, setPlace] = useState(
    user.latitude ? { name: user.village, district: user.district, latitude: user.latitude, longitude: user.longitude } : null,
  )
  const [crops, setCrops] = useState(user.crops || [])
  const [error, setError] = useState(null)
  const [ok, setOk] = useState(false)

  const toggleCrop = (c) => setCrops(crops.includes(c) ? crops.filter((x) => x !== c) : [...crops, c])

  const submit = async (e) => {
    e.preventDefault()
    setError(null)
    setOk(false)
    try {
      const updated = await api.updateProfile({
        fullName: form.fullName,
        email: form.email || null,
        village: place?.name || null,
        district: place?.district || null,
        latitude: place?.latitude ?? null,
        longitude: place?.longitude ?? null,
        preferredLang: i18n.language,
        crops,
      })
      setUser(updated)
      setOk(true)
    } catch (err) {
      setError(err)
    }
  }

  return (
    <div className="page page-narrow">
      <div className="card">
        <div className="row between">
          <h1>{t('profile.title')}</h1>
          <span className="chip">{t(`role.${user.role}`)}</span>
        </div>
        <p className="muted small">
          {user.phone}
          {user.role !== 'SEEKER' && user.role !== 'ADMIN' && (
            <> · {user.verified ? <span className="verified">✓ {t('common.verified')}</span> : t('provider.awaitingVerification')}</>
          )}
        </p>
        {ok && <div className="alert alert-ok">{t('profile.saved')}</div>}
        <ErrorAlert error={error} />
        <form onSubmit={submit}>
          <div className="field">
            <label htmlFor="p-name">{t('auth.fullName')}</label>
            <input id="p-name" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required />
          </div>
          <div className="field">
            <label htmlFor="p-email">{t('auth.emailOptional')}</label>
            <input id="p-email" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
          </div>
          <div className="field">
            <label>{t('auth.village')}</label>
            {place ? (
              <div className="row">
                <span className="chip">{place.name}, {place.district}</span>
                <button type="button" className="btn btn-outline btn-sm" onClick={() => setPlace(null)}>{t('common.change')}</button>
              </div>
            ) : (
              <PlaceSearch onPick={setPlace} />
            )}
          </div>
          {user.role === 'SEEKER' && (
            <div className="field">
              <label>{t('profile.myCrops')}</label>
              <p className="muted small">{t('profile.cropsHint')}</p>
              <div className="row" style={{ gap: '0.4rem' }}>
                {CROPS.map((c) => (
                  <button key={c} type="button" className={crops.includes(c) ? 'btn btn-primary btn-sm' : 'btn btn-outline btn-sm'} onClick={() => toggleCrop(c)}>
                    {t(`crop.${c}`)}
                  </button>
                ))}
              </div>
            </div>
          )}
          <button className="btn btn-primary">{t('common.save')}</button>
        </form>
      </div>
      {user.role === 'SPECIALIST' && <SpecialistProfileForm profile={user.specialist} onSaved={setUser} />}
    </div>
  )
}
