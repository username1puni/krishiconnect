import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { money } from '../utils/format'
import DistanceInfo from './DistanceInfo.jsx'
import Photo from './Photo.jsx'
import { Stars } from './Stars.jsx'

export function mainRate(item, t, lang) {
  if (item.pricePerHour) return `${money(item.pricePerHour, lang)} ${t('price.perHour')}`
  if (item.pricePerDay) return `${money(item.pricePerDay, lang)} ${t('price.perDay')}`
  if (item.pricePerAcre) return `${money(item.pricePerAcre, lang)} ${t('price.perAcre')}`
  return ''
}

export default function EquipmentCard({ item }) {
  const { t, i18n } = useTranslation()
  return (
    <Link to={`/equipment/${item.id}`} className="card card-link item-card">
      <div className="photo">
        <Photo id={item.coverPhotoId} alt={item.title} category={item.category} />
      </div>
      <div className="body">
        <div className="row between">
          <span className="chip">{t(`category.${item.category}`)}</span>
          {item.withOperator && <span className="chip chip-sun">{t('equipment.withOperator')}</span>}
        </div>
        <div className="title">{item.title}</div>
        <div className="muted small">
          {item.village}, {item.district} · {item.ownerName}
          {item.ownerVerified && <span className="verified"> ✓</span>}
        </div>
        <DistanceInfo distanceKm={item.distanceKm} durationMinutes={item.durationMinutes} source={item.distanceSource} />
        <Stars rating={item.rating} count={item.ratingCount} />
        <div className="grow" />
        <div className="price">{mainRate(item, t, i18n.language)}</div>
      </div>
    </Link>
  )
}
