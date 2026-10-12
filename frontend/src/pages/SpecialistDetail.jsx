import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useFarmLocation } from '../context/LocationContext.jsx'
import AvailabilityCalendar from '../components/AvailabilityCalendar.jsx'
import BookingForm from '../components/BookingForm.jsx'
import DistanceInfo from '../components/DistanceInfo.jsx'
import ReviewList from '../components/ReviewList.jsx'
import { initials } from '../components/SpecialistCard.jsx'
import { ErrorAlert, Spinner } from '../components/Feedback.jsx'
import { Stars } from '../components/Stars.jsx'
import { money } from '../utils/format'

export default function SpecialistDetail() {
  const { id } = useParams()
  const { t, i18n } = useTranslation()
  const { location } = useFarmLocation()
  const [item, setItem] = useState(null)
  const [error, setError] = useState(null)
  const [refresh, setRefresh] = useState(0)

  useEffect(() => {
    api.specialistById(id).then(setItem).catch(setError)
  }, [id])

  const loadAvailability = useCallback((from, days) => api.specialistAvailability(id, from, days), [id])
  const loadReviews = useCallback((page) => api.specialistReviews(id, page), [id])

  if (error) return <div className="page"><ErrorAlert error={error} /></div>
  if (!item) return <Spinner />
  const lang = i18n.language

  return (
    <div className="page">
      <p className="small"><Link to="/specialists">← {t('specialist.back')}</Link></p>
      <div className="two-col">
        <div>
          <div className="card">
            <div className="list-item">
              <div className="avatar" style={{ width: 72, height: 72, fontSize: '1.5rem' }}>{initials(item.fullName)}</div>
              <div className="grow">
                <h1>{item.fullName}</h1>
                <div className="row" style={{ gap: '0.4rem' }}>
                  <span className="chip">{t(`skill.${item.skill}`)}</span>
                  {item.verified && <span className="verified">✓ {t('common.verified')}</span>}
                  {!item.available && <span className="chip chip-grey">{t('specialist.notAvailable')}</span>}
                </div>
                <div className="muted small mt">
                  {item.village}, {item.district} · {t('specialist.years', { count: item.experienceYears })}
                  {item.languages && ` · ${t('specialist.speaks')} ${item.languages}`}
                </div>
                {location && <DistanceInfo distanceKm={item.distanceKm} durationMinutes={item.durationMinutes} source={item.distanceSource} />}
                <Stars rating={item.rating} count={item.ratingCount} />
              </div>
            </div>
            {item.bio && <p className="mt">{item.bio}</p>}
            <div className="rates">
              {item.ratePerHour && <div className="rate"><strong>{money(item.ratePerHour, lang)}</strong><span className="small muted">{t('price.perHour')}</span></div>}
              {item.ratePerDay && <div className="rate"><strong>{money(item.ratePerDay, lang)}</strong><span className="small muted">{t('price.perDay')}</span></div>}
            </div>
          </div>
          <div className="card mt">
            <h3>{t('calendar.title')}</h3>
            <AvailabilityCalendar load={loadAvailability} refreshKey={refresh} />
          </div>
          <div className="card mt">
            <h3>{t('reviews.title')}</h3>
            <ReviewList load={loadReviews} />
          </div>
        </div>
        <aside className="card booking-box">
          <h2>{t('booking.hireTitle')}</h2>
          {item.available ? (
            <BookingForm kind="SPECIALIST" resource={item} onBooked={() => setRefresh((n) => n + 1)} />
          ) : (
            <p className="muted">{t('specialist.notAvailableText')}</p>
          )}
        </aside>
      </div>
    </div>
  )
}
