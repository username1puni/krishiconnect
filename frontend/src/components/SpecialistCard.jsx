import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { money } from '../utils/format'
import DistanceInfo from './DistanceInfo.jsx'
import { Stars } from './Stars.jsx'

export const initials = (name) =>
  name
    .split(' ')
    .map((p) => p[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()

export default function SpecialistCard({ item }) {
  const { t, i18n } = useTranslation()
  return (
    <Link to={`/specialists/${item.id}`} className="card card-link">
      <div className="list-item">
        <div className="avatar">{initials(item.fullName)}</div>
        <div className="stack grow" style={{ gap: '0.3rem' }}>
          <div className="row between">
            <strong>{item.fullName}</strong>
            {item.verified && <span className="verified">✓ {t('common.verified')}</span>}
          </div>
          <div className="row" style={{ gap: '0.4rem' }}>
            <span className="chip">{t(`skill.${item.skill}`)}</span>
            {!item.available && <span className="chip chip-grey">{t('specialist.notAvailable')}</span>}
          </div>
          <div className="muted small">
            {item.village}, {item.district} · {t('specialist.years', { count: item.experienceYears })}
          </div>
          <DistanceInfo distanceKm={item.distanceKm} durationMinutes={item.durationMinutes} source={item.distanceSource} />
          <Stars rating={item.rating} count={item.ratingCount} />
          <div className="price">
            {item.ratePerDay && `${money(item.ratePerDay, i18n.language)} ${t('price.perDay')}`}
            {item.ratePerHour && item.ratePerDay && ' · '}
            {item.ratePerHour && `${money(item.ratePerHour, i18n.language)} ${t('price.perHour')}`}
          </div>
        </div>
      </div>
    </Link>
  )
}
