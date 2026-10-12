import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'
import { estimate, localHour, money, todayIso } from '../utils/format'
import { ErrorAlert } from './Feedback.jsx'

const HOURS = Array.from({ length: 16 }, (_, i) => i + 5) // 5 AM to 8 PM

// which kind of operator makes sense next to which machine
export const OPERATOR_FOR = {
  TRACTOR: 'TRACTOR_DRIVER',
  ROTAVATOR: 'TRACTOR_DRIVER',
  CULTIVATOR: 'TRACTOR_DRIVER',
  SEED_DRILL: 'TRACTOR_DRIVER',
  BALER: 'TRACTOR_DRIVER',
  COMBINE_HARVESTER: 'HARVESTER_OPERATOR',
  SPRAYER: 'SPRAY_OPERATOR',
  AGRI_DRONE: 'DRONE_PILOT',
}

function rateOptions(resource, kind) {
  const r = kind === 'EQUIPMENT'
    ? { HOUR: resource.pricePerHour, DAY: resource.pricePerDay, ACRE: resource.pricePerAcre }
    : { HOUR: resource.ratePerHour, DAY: resource.ratePerDay }
  return Object.entries(r).filter(([, v]) => v)
}

const specialistRate = (s, unit) => (unit === 'HOUR' ? s.ratePerHour : s.ratePerDay)

export default function BookingForm({ kind, resource, onBooked }) {
  const { t, i18n } = useTranslation()
  const { user } = useAuth()
  const options = rateOptions(resource, kind)
  const [unit, setUnit] = useState(options[0]?.[0] || 'DAY')
  const [startDate, setStartDate] = useState(todayIso(1))
  const [startHour, setStartHour] = useState(7)
  const [endDate, setEndDate] = useState(todayIso(1))
  const [endHour, setEndHour] = useState(13)
  const [acres, setAcres] = useState('')
  const [note, setNote] = useState('')
  const [operators, setOperators] = useState([])
  const [operatorId, setOperatorId] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const [done, setDone] = useState(null)

  const operatorSkill = kind === 'EQUIPMENT' && !resource.withOperator ? OPERATOR_FOR[resource.category] : null

  useEffect(() => {
    if (!operatorSkill) return
    api
      .specialists({ lat: resource.latitude, lng: resource.longitude, skill: operatorSkill, radiusKm: 40, size: 6 })
      .then((page) => setOperators(page.content.filter((s) => s.available)))
      .catch(() => setOperators([]))
  }, [operatorSkill, resource.latitude, resource.longitude])

  const rate = Object.fromEntries(options)[unit]
  const main = estimate(unit, rate, startDate, startHour, endDate, endHour, acres)
  const operator = operators.find((o) => String(o.id) === String(operatorId))
  const operatorUnit = operator && unit === 'HOUR' && operator.ratePerHour ? 'HOUR' : 'DAY'
  const extra = operator
    ? estimate(operatorUnit, specialistRate(operator, operatorUnit), startDate, startHour, endDate, endHour)
    : null
  const total = (main?.amount || 0) + (extra?.amount || 0)

  const ownListing = useMemo(
    () => user && (resource.ownerId === user.id || (kind === 'SPECIALIST' && resource.id === user.id)),
    [user, resource, kind],
  )

  if (!user) {
    return (
      <div className="alert alert-info">
        <Link to="/login">{t('booking.loginToBook')}</Link>
      </div>
    )
  }
  if (user.role === 'ADMIN' || ownListing) {
    return <p className="muted small">{t('booking.cannotBookOwn')}</p>
  }
  if (done) {
    return (
      <div className="alert alert-ok">
        <strong>{t('booking.sentTitle')}</strong>
        <p className="small">{t('booking.sentText')}</p>
        <Link to="/bookings" className="btn btn-primary btn-sm">{t('booking.viewBookings')}</Link>
      </div>
    )
  }

  const submit = async (e) => {
    e.preventDefault()
    setError(null)
    const items = [{ type: kind, resourceId: resource.id, priceUnit: unit, acres: unit === 'ACRE' ? Number(acres) : null }]
    if (operator) items.push({ type: 'SPECIALIST', resourceId: operator.id, priceUnit: operatorUnit, acres: null })
    setBusy(true)
    try {
      const booking = await api.createBooking({
        items,
        startAt: localHour(startDate, startHour),
        endAt: localHour(endDate, endHour),
        note: note || null,
      })
      setDone(booking)
      onBooked?.(booking)
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit}>
      <ErrorAlert error={error} />
      {options.length > 1 && (
        <div className="field">
          <label>{t('booking.chargeBy')}</label>
          <div className="segmented">
            {options.map(([u]) => (
              <button key={u} type="button" className={unit === u ? 'active' : ''} onClick={() => setUnit(u)}>
                {t(`unit.${u}`)}
              </button>
            ))}
          </div>
        </div>
      )}
      <div className="form-row">
        <div className="field">
          <label htmlFor="bf-sd">{t('booking.from')}</label>
          <input id="bf-sd" type="date" min={todayIso()} value={startDate}
            onChange={(e) => {
              setStartDate(e.target.value)
              if (endDate < e.target.value) setEndDate(e.target.value)
            }} />
        </div>
        <div className="field">
          <label htmlFor="bf-sh">{t('booking.time')}</label>
          <select id="bf-sh" value={startHour} onChange={(e) => setStartHour(Number(e.target.value))}>
            {HOURS.map((h) => <option key={h} value={h}>{`${String(h).padStart(2, '0')}:00`}</option>)}
          </select>
        </div>
      </div>
      <div className="form-row">
        <div className="field">
          <label htmlFor="bf-ed">{t('booking.to')}</label>
          <input id="bf-ed" type="date" min={startDate} value={endDate} onChange={(e) => setEndDate(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="bf-eh">{t('booking.time')}</label>
          <select id="bf-eh" value={endHour} onChange={(e) => setEndHour(Number(e.target.value))}>
            {HOURS.map((h) => <option key={h} value={h}>{`${String(h).padStart(2, '0')}:00`}</option>)}
          </select>
        </div>
      </div>
      {unit === 'ACRE' && (
        <div className="field">
          <label htmlFor="bf-acres">{t('booking.acres')}</label>
          <input id="bf-acres" type="number" min="0.5" step="0.5" value={acres} onChange={(e) => setAcres(e.target.value)} required />
        </div>
      )}

      {operatorSkill && (
        <div className="addon">
          <label htmlFor="bf-op">{t('booking.addOperator', { skill: t(`skill.${operatorSkill}`) })}</label>
          {operators.length === 0 ? (
            <p className="muted small" style={{ margin: 0 }}>{t('booking.noOperators')}</p>
          ) : (
            <>
              <select id="bf-op" value={operatorId} onChange={(e) => setOperatorId(e.target.value)}>
                <option value="">{t('booking.noOperator')}</option>
                {operators.map((o) => (
                  <option key={o.id} value={o.id}>
                    {o.fullName} · {o.village} · {money(o.ratePerDay, i18n.language)} {t('price.perDay')}
                  </option>
                ))}
              </select>
              <p className="muted small" style={{ margin: '0.35rem 0 0' }}>{t('booking.bundleHint')}</p>
            </>
          )}
        </div>
      )}

      <div className="field">
        <label htmlFor="bf-note">{t('booking.note')}</label>
        <input id="bf-note" value={note} maxLength={300} onChange={(e) => setNote(e.target.value)}
          placeholder={t('booking.notePlaceholder')} />
      </div>

      {main && (
        <div className="mb">
          <div className="price-line">
            <span>{resource.title || resource.fullName} · {main.quantity} {t(`unit.${unit}`)}</span>
            <span>{money(main.amount, i18n.language)}</span>
          </div>
          {extra && (
            <div className="price-line">
              <span>{operator.fullName} · {extra.quantity} {t(`unit.${operatorUnit}`)}</span>
              <span>{money(extra.amount, i18n.language)}</span>
            </div>
          )}
          <div className="price-line price-total">
            <span>{t('booking.estimate')}</span>
            <span>{money(total, i18n.language)}</span>
          </div>
        </div>
      )}
      <button className="btn btn-primary btn-block" disabled={busy}>
        {busy ? t('common.wait') : operator ? t('booking.bookBoth') : t('booking.book')}
      </button>
      <p className="muted small mt" style={{ marginBottom: 0 }}>{t('booking.payHint')}</p>
    </form>
  )
}
