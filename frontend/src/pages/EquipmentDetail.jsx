import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'
import { useFarmLocation } from '../context/LocationContext.jsx'
import AvailabilityCalendar from '../components/AvailabilityCalendar.jsx'
import BookingForm from '../components/BookingForm.jsx'
import MapView from '../components/MapView.jsx'
import Photo from '../components/Photo.jsx'
import ReviewList from '../components/ReviewList.jsx'
import { ErrorAlert, Spinner } from '../components/Feedback.jsx'
import { Stars } from '../components/Stars.jsx'
import { money } from '../utils/format'

export default function EquipmentDetail() {
  const { id } = useParams()
  const { t, i18n } = useTranslation()
  const { user } = useAuth()
  const { location } = useFarmLocation()
  const [item, setItem] = useState(null)
  const [error, setError] = useState(null)
  const [photo, setPhoto] = useState(0)
  const [refresh, setRefresh] = useState(0)

  useEffect(() => {
    api.equipmentById(id).then(setItem).catch(setError)
  }, [id])

  const loadAvailability = useCallback((from, days) => api.equipmentAvailability(id, from, days), [id])
  const loadReviews = useCallback((page) => api.equipmentReviews(id, page), [id])

  if (error) return <div className="page"><ErrorAlert error={error} /></div>
  if (!item) return <Spinner />
  const lang = i18n.language
  const isOwner = user?.id === item.ownerId

  return (
    <div className="page">
      <p className="small"><Link to="/equipment">← {t('equipment.back')}</Link></p>
      <div className="two-col">
        <div>
          <div className="gallery-main">
            <Photo id={item.photoIds[photo]} alt={item.title} category={item.category} />
          </div>
          {item.photoIds.length > 1 && (
            <div className="thumbs">
              {item.photoIds.map((pid, i) => (
                <button key={pid} type="button" className={i === photo ? 'active' : ''} onClick={() => setPhoto(i)}>
                  <Photo id={pid} alt="" category={item.category} />
                </button>
              ))}
            </div>
          )}

          <div className="mt">
            <div className="row">
              <span className="chip">{t(`category.${item.category}`)}</span>
              {item.withOperator && <span className="chip chip-sun">{t('equipment.withOperator')}</span>}
              {item.status === 'PAUSED' && <span className="chip chip-grey">{t('equipment.paused')}</span>}
              {item.moderationStatus !== 'APPROVED' && <span className={`chip status-${item.moderationStatus}`}>{t(`moderation.${item.moderationStatus}`)}</span>}
            </div>
            <h1 className="mt">{item.title}</h1>
            <div className="muted">
              {item.village}, {item.district} · {t('equipment.ownedBy')} <strong>{item.ownerName}</strong>
              {item.ownerVerified && <span className="verified"> ✓ {t('common.verified')}</span>}
            </div>
            <Stars rating={item.rating} count={item.ratingCount} />
            {isOwner && (
              <div className="mt"><Link to={`/provider/equipment/${item.id}/edit`} className="btn btn-outline btn-sm">{t('equipment.edit')}</Link></div>
            )}
          </div>

          <div className="rates mt">
            {item.pricePerHour && <div className="rate"><strong>{money(item.pricePerHour, lang)}</strong><span className="small muted">{t('price.perHour')}</span></div>}
            {item.pricePerDay && <div className="rate"><strong>{money(item.pricePerDay, lang)}</strong><span className="small muted">{t('price.perDay')}</span></div>}
            {item.pricePerAcre && <div className="rate"><strong>{money(item.pricePerAcre, lang)}</strong><span className="small muted">{t('price.perAcre')}</span></div>}
          </div>

          <dl className="specs">
            {item.brand && <div><dt>{t('equipment.brand')}</dt><dd>{item.brand} {item.model}</dd></div>}
            {item.horsePower && <div><dt>{t('equipment.hp')}</dt><dd>{item.horsePower} HP</dd></div>}
            {item.yearOfMake && <div><dt>{t('equipment.year')}</dt><dd>{item.yearOfMake}</dd></div>}
            <div><dt>{t('equipment.operator')}</dt><dd>{item.withOperator ? t('common.yes') : t('common.no')}</dd></div>
          </dl>
          {item.description && <p>{item.description}</p>}

          <div className="card mt">
            <h3>{t('calendar.title')}</h3>
            <p className="muted small">{t('calendar.hint')}</p>
            <AvailabilityCalendar load={loadAvailability} refreshKey={refresh} />
          </div>

          <div className="card mt">
            <h3>{t('equipment.location')}</h3>
            <MapView
              small
              zoom={11}
              center={location || { lat: item.latitude, lng: item.longitude, label: item.village }}
              points={[{ id: item.id, lat: item.latitude, lng: item.longitude, title: item.title, subtitle: item.village }]}
            />
          </div>

          <div className="card mt">
            <h3>{t('reviews.title')}</h3>
            <ReviewList load={loadReviews} />
          </div>
        </div>

        <aside className="card booking-box">
          <h2>{t('booking.title')}</h2>
          {item.status === 'PAUSED' || item.moderationStatus !== 'APPROVED' ? (
            <p className="muted">{t('booking.notBookable')}</p>
          ) : (
            <BookingForm kind="EQUIPMENT" resource={item} onBooked={() => setRefresh((n) => n + 1)} />
          )}
        </aside>
      </div>
    </div>
  )
}
