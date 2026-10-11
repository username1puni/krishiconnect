import { useTranslation } from 'react-i18next'
import { km } from '../utils/format'

/** "12 km by road, 18 min" or "9.4 km straight line" when the routing API was not used. */
export default function DistanceInfo({ distanceKm, durationMinutes, source }) {
  const { t } = useTranslation()
  if (distanceKm === null || distanceKm === undefined) return null
  const road = source === 'ROAD'
  return (
    <span className="distance">
      <span>{t('distance.km', { km: km(distanceKm) })}</span>
      {road ? (
        <span className="chip chip-blue" title={t('distance.roadHint')}>
          {t('distance.road')}
          {durationMinutes ? ` · ${t('distance.minutes', { count: durationMinutes })}` : ''}
        </span>
      ) : (
        <span className="chip chip-grey" title={t('distance.straightHint')}>{t('distance.straight')}</span>
      )}
    </span>
  )
}
