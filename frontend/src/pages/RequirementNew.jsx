import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'
import { ErrorAlert } from '../components/Feedback.jsx'
import { PlaceSearch } from '../components/LocationBar.jsx'
import { CATEGORIES, CROPS, SKILLS } from '../utils/options'
import { localHour, todayIso } from '../utils/format'

export default function RequirementNew() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    title: '',
    description: '',
    needType: 'EQUIPMENT',
    category: 'TRACTOR',
    skill: 'TRACTOR_DRIVER',
    crop: user?.crops?.[0] || 'PADDY',
    areaAcres: '',
    startDate: todayIso(3),
    endDate: todayIso(3),
    budget: '',
  })
  const [place, setPlace] = useState(
    user?.latitude ? { name: user.village, district: user.district, latitude: user.latitude, longitude: user.longitude } : null,
  )
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    if (!place) {
      setError({ message: t('requirement.pickVillage') })
      return
    }
    setBusy(true)
    setError(null)
    try {
      const created = await api.createRequirement({
        title: form.title,
        description: form.description || null,
        needType: form.needType,
        category: form.needType === 'EQUIPMENT' ? form.category : null,
        skill: form.needType === 'SPECIALIST' ? form.skill : null,
        crop: form.crop || null,
        areaAcres: form.areaAcres ? Number(form.areaAcres) : null,
        startAt: localHour(form.startDate, 6),
        endAt: localHour(form.endDate, 18),
        village: place.name,
        district: place.district,
        latitude: place.latitude,
        longitude: place.longitude,
        budget: form.budget ? Number(form.budget) : null,
      })
      navigate(`/requirements/${created.id}`)
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page page-narrow">
      <div className="card">
        <h1>{t('requirement.newTitle')}</h1>
        <p className="muted">{t('requirement.newSubtitle')}</p>
        <ErrorAlert error={error} />
        <form onSubmit={submit}>
          <div className="field">
            <label htmlFor="title">{t('requirement.titleLabel')}</label>
            <input id="title" value={form.title} onChange={set('title')} placeholder={t('requirement.titlePlaceholder')} required />
          </div>
          <div className="field">
            <label>{t('requirement.needType')}</label>
            <div className="segmented">
              {['EQUIPMENT', 'SPECIALIST'].map((n) => (
                <button key={n} type="button" className={form.needType === n ? 'active' : ''} onClick={() => setForm({ ...form, needType: n })}>
                  {n === 'EQUIPMENT' ? t('requirement.needEquipment') : t('requirement.needSpecialist')}
                </button>
              ))}
            </div>
          </div>
          <div className="form-row">
            {form.needType === 'EQUIPMENT' ? (
              <div className="field">
                <label htmlFor="category">{t('equipment.category')}</label>
                <select id="category" value={form.category} onChange={set('category')}>
                  {CATEGORIES.map((c) => <option key={c} value={c}>{t(`category.${c}`)}</option>)}
                </select>
              </div>
            ) : (
              <div className="field">
                <label htmlFor="skill">{t('auth.skill')}</label>
                <select id="skill" value={form.skill} onChange={set('skill')}>
                  {SKILLS.map((s) => <option key={s} value={s}>{t(`skill.${s}`)}</option>)}
                </select>
              </div>
            )}
            <div className="field">
              <label htmlFor="crop">{t('seasonal.crop')}</label>
              <select id="crop" value={form.crop} onChange={set('crop')}>
                {CROPS.map((c) => <option key={c} value={c}>{t(`crop.${c}`)}</option>)}
              </select>
            </div>
          </div>
          <div className="form-row">
            <div className="field">
              <label htmlFor="sd">{t('booking.from')}</label>
              <input id="sd" type="date" min={todayIso()} value={form.startDate} onChange={set('startDate')} required />
            </div>
            <div className="field">
              <label htmlFor="ed">{t('booking.to')}</label>
              <input id="ed" type="date" min={form.startDate} value={form.endDate} onChange={set('endDate')} required />
            </div>
          </div>
          <div className="form-row">
            <div className="field">
              <label htmlFor="acres">{t('booking.acres')}</label>
              <input id="acres" type="number" min="0" step="0.5" value={form.areaAcres} onChange={set('areaAcres')} />
            </div>
            <div className="field">
              <label htmlFor="budget">{t('requirement.budgetOptional')}</label>
              <input id="budget" type="number" min="0" step="100" value={form.budget} onChange={set('budget')} />
            </div>
          </div>
          <div className="field">
            <label htmlFor="desc">{t('requirement.details')}</label>
            <textarea id="desc" value={form.description} onChange={set('description')} placeholder={t('requirement.detailsPlaceholder')} />
          </div>
          <div className="field">
            <label>{t('requirement.where')}</label>
            {place ? (
              <div className="row">
                <span className="chip">{place.name}, {place.district}</span>
                <button type="button" className="btn btn-outline btn-sm" onClick={() => setPlace(null)}>{t('common.change')}</button>
              </div>
            ) : (
              <PlaceSearch onPick={setPlace} />
            )}
          </div>
          <button className="btn btn-primary btn-block" disabled={busy}>{busy ? t('common.wait') : t('requirement.post')}</button>
        </form>
      </div>
    </div>
  )
}
