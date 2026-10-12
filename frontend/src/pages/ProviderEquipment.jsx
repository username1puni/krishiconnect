import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'
import Photo from '../components/Photo.jsx'
import { Empty, ErrorAlert, Spinner } from '../components/Feedback.jsx'
import { mainRate } from '../components/EquipmentCard.jsx'

export default function ProviderEquipment() {
  const { t, i18n } = useTranslation()
  const { user } = useAuth()
  const [items, setItems] = useState(null)
  const [error, setError] = useState(null)

  const load = useCallback(() => api.myEquipment().then(setItems).catch(setError), [])
  useEffect(() => {
    load()
  }, [load])

  const toggle = async (e) => {
    try {
      await api.setEquipmentStatus(e.id, e.status === 'ACTIVE' ? 'PAUSED' : 'ACTIVE')
      load()
    } catch (err) {
      setError(err)
    }
  }

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>{t('provider.equipmentTitle')}</h1>
          <p className="muted">{t('provider.equipmentSubtitle')}</p>
        </div>
        <Link to="/provider/equipment/new" className="btn btn-primary">{t('equipment.add')}</Link>
      </div>
      {!user.verified && <div className="alert alert-warn small">{t('provider.unverifiedNote')}</div>}
      <ErrorAlert error={error} />
      {!items ? <Spinner /> : items.length === 0 ? (
        <Empty>{t('provider.noEquipment')}</Empty>
      ) : (
        <div className="grid">
          {items.map((e) => (
            <div key={e.id} className="card item-card">
              <div className="photo"><Photo id={e.photoIds[0]} alt={e.title} category={e.category} /></div>
              <div className="body">
                <div className="row" style={{ gap: '0.35rem' }}>
                  <span className={`chip status-${e.moderationStatus}`}>{t(`moderation.${e.moderationStatus}`)}</span>
                  {e.status === 'PAUSED' && <span className="chip chip-grey">{t('equipment.paused')}</span>}
                </div>
                <Link to={`/equipment/${e.id}`} className="title">{e.title}</Link>
                {e.moderationNote && <div className="small field-error">{e.moderationNote}</div>}
                <div className="price">{mainRate(e, t, i18n.language)}</div>
                <div className="muted small">{t('equipment.photos', { count: e.photoIds.length })}</div>
                <div className="grow" />
                <div className="row">
                  <Link to={`/provider/equipment/${e.id}/edit`} className="btn btn-outline btn-sm">{t('equipment.edit')}</Link>
                  <button type="button" className="btn btn-ghost btn-sm" onClick={() => toggle(e)}>
                    {e.status === 'ACTIVE' ? t('equipment.pause') : t('equipment.resume')}
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
