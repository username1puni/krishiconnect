import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { dateTime, money, timeRange } from '../utils/format'
import StatusChip from './StatusChip.jsx'
import { ErrorAlert } from './Feedback.jsx'
import { StarInput } from './Stars.jsx'

function ReviewForm({ item, onDone }) {
  const { t } = useTranslation()
  const [rating, setRating] = useState(5)
  const [comment, setComment] = useState('')
  const [error, setError] = useState(null)
  const submit = async (e) => {
    e.preventDefault()
    try {
      await api.addReview({ bookingItemId: item.id, rating, comment: comment || null })
      onDone()
    } catch (err) {
      setError(err)
    }
  }
  return (
    <form onSubmit={submit} className="addon">
      <ErrorAlert error={error} />
      <label>{t('reviews.rate', { name: item.resourceName })}</label>
      <StarInput value={rating} onChange={setRating} />
      <textarea value={comment} maxLength={500} onChange={(e) => setComment(e.target.value)} placeholder={t('reviews.placeholder')} />
      <button className="btn btn-primary btn-sm mt">{t('reviews.submit')}</button>
    </form>
  )
}

/**
 * mode "seeker": the farmer's own booking (cancel, review).
 * mode "provider": a booking that contains one of my machines or me (accept, reject, complete).
 */
export default function BookingCard({ booking, mode, me, reviewed = [], onChange }) {
  const { t, i18n } = useTranslation()
  const [error, setError] = useState(null)
  const [reviewing, setReviewing] = useState(null)
  const lang = i18n.language

  const run = async (fn) => {
    setError(null)
    try {
      await fn()
      onChange()
    } catch (err) {
      setError(err)
    }
  }

  const items = booking.items
  return (
    <div className="card">
      <div className="row between">
        <div>
          <strong>
            {booking.kind === 'BUNDLE' ? t('booking.bundle') : t('booking.single')} #{booking.id}
          </strong>
          <div className="muted small">{timeRange(booking.startAt, booking.endAt, lang)}</div>
        </div>
        <StatusChip status={booking.status} />
      </div>
      {mode === 'provider' && (
        <div className="small mt">
          {t('booking.farmer')}: <strong>{booking.seekerName}</strong> · {booking.seekerVillage}
          {booking.seekerPhone && <> · <a href={`tel:${booking.seekerPhone}`}>{booking.seekerPhone}</a></>}
        </div>
      )}
      {booking.note && <p className="small muted" style={{ margin: '0.3rem 0 0' }}>“{booking.note}”</p>}
      {booking.status === 'PENDING' && booking.expiresAt && (
        <div className="alert alert-warn small mt" style={{ marginBottom: 0 }}>
          {mode === 'provider' ? t('booking.respondBy', { time: dateTime(booking.expiresAt, lang) }) : t('booking.waitingUntil', { time: dateTime(booking.expiresAt, lang) })}
        </div>
      )}
      <ErrorAlert error={error} />
      {items.map((it) => {
        const mine = mode === 'provider' && it.providerId === me
        return (
          <div key={it.id} className="booking-item" style={mode === 'provider' && !mine ? { opacity: 0.65 } : undefined}>
            <div className="row between">
              <div>
                <Link to={it.resourceType === 'EQUIPMENT' ? `/equipment/${it.resourceId}` : `/specialists/${it.resourceId}`}>
                  <strong>{it.resourceName}</strong>
                </Link>{' '}
                <span className="chip chip-grey">
                  {it.resourceType === 'EQUIPMENT' ? t(`category.${it.category}`) : t('role.SPECIALIST')}
                </span>
              </div>
              <StatusChip status={it.status} />
            </div>
            <div className="muted small">
              {mode === 'seeker' && (
                <>
                  {it.providerName}
                  {it.providerPhone && <> · <a href={`tel:${it.providerPhone}`}>{it.providerPhone}</a></>}
                  {' · '}
                </>
              )}
              {it.quantity} {t(`unit.${it.priceUnit}`)} × {money(it.unitPrice, lang)} = <strong>{money(it.amount, lang)}</strong>
            </div>
            <div className="row mt" style={{ gap: '0.4rem' }}>
              {mine && it.status === 'PENDING' && (
                <>
                  <button type="button" className="btn btn-primary btn-sm" onClick={() => run(() => api.acceptItem(it.id))}>{t('booking.accept')}</button>
                  <button type="button" className="btn btn-danger btn-sm" onClick={() => run(() => api.rejectItem(it.id))}>{t('booking.reject')}</button>
                </>
              )}
              {mine && it.status === 'ACCEPTED' && booking.status === 'CONFIRMED' && (
                <button type="button" className="btn btn-outline btn-sm" onClick={() => run(() => api.completeItem(it.id))}>{t('booking.markDone')}</button>
              )}
              {mode === 'seeker' && it.status === 'COMPLETED' && !reviewed.includes(it.id) && reviewing !== it.id && (
                <button type="button" className="btn btn-sun btn-sm" onClick={() => setReviewing(it.id)}>{t('reviews.write')}</button>
              )}
              {mode === 'seeker' && reviewed.includes(it.id) && <span className="small verified">✓ {t('reviews.done')}</span>}
            </div>
            {reviewing === it.id && <ReviewForm item={it} onDone={() => { setReviewing(null); onChange() }} />}
          </div>
        )
      })}
      <div className="row between mt">
        <strong>{t('booking.total')}: {money(booking.totalAmount, lang)}</strong>
        {mode === 'seeker' && (booking.status === 'PENDING' || booking.status === 'CONFIRMED') && (
          <button type="button" className="btn btn-danger btn-sm"
            onClick={() => window.confirm(t('booking.cancelConfirm')) && run(() => api.cancelBooking(booking.id))}>
            {t('booking.cancel')}
          </button>
        )}
      </div>
    </div>
  )
}
