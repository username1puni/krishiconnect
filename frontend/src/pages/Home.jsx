import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'
import { useFarmLocation } from '../context/LocationContext.jsx'
import CategoryIcon from '../components/CategoryIcon.jsx'
import EquipmentCard from '../components/EquipmentCard.jsx'
import { RecommendationCard } from './Seasonal.jsx'

const SHOWCASE = ['TRACTOR', 'COMBINE_HARVESTER', 'AGRI_DRONE', 'SPRAYER', 'ROTAVATOR', 'PADDY_TRANSPLANTER', 'THRESHER', 'WATER_PUMP']

export default function Home() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const { location } = useFarmLocation()
  const [nearby, setNearby] = useState([])
  const [recos, setRecos] = useState([])

  useEffect(() => {
    api.equipment({ lat: location?.lat, lng: location?.lng, size: 4 }).then((p) => setNearby(p.content)).catch(() => {})
    api.recommendations({ lat: location?.lat, lng: location?.lng }).then((r) => setRecos(r.slice(0, 3))).catch(() => {})
  }, [location])

  return (
    <>
      <section className="hero">
        <div className="hero-inner">
          <div>
            <h1>{t('home.title')}</h1>
            <p>{t('home.subtitle')}</p>
            <div className="row mt">
              <Link to="/equipment" className="btn btn-sun">{t('home.findEquipment')}</Link>
              <Link to="/specialists" className="btn btn-outline">{t('home.hireWorkers')}</Link>
              {!user && <Link to="/register" className="btn btn-outline">{t('home.listMachine')}</Link>}
            </div>
          </div>
          <div className="hero-img">
            <img src="/hero.jpg" alt={t('home.heroAlt')} />
          </div>
        </div>
      </section>

      <div className="page">
        <section>
          <h2>{t('home.categories')}</h2>
          <div className="grid-4">
            {SHOWCASE.map((c) => (
              <Link key={c} to={`/equipment?category=${c}`} className="card card-link category-tile">
                <span className="icon"><CategoryIcon category={c} size={28} /></span>
                {t(`category.${c}`)}
              </Link>
            ))}
          </div>
        </section>

        {recos.length > 0 && (
          <section className="section">
            <div className="row between">
              <h2>{t('home.thisSeason')}</h2>
              <Link to="/seasonal">{t('common.seeAll')}</Link>
            </div>
            <div className="grid-3">
              {recos.map((r) => <RecommendationCard key={r.ruleId} r={r} />)}
            </div>
          </section>
        )}

        {nearby.length > 0 && (
          <section className="section">
            <div className="row between">
              <h2>{location ? t('home.nearYou', { place: location.label || '' }) : t('home.popular')}</h2>
              <Link to="/equipment">{t('common.seeAll')}</Link>
            </div>
            <div className="grid">
              {nearby.map((e) => <EquipmentCard key={e.id} item={e} />)}
            </div>
          </section>
        )}

        <section className="section">
          <h2>{t('home.howItWorks')}</h2>
          <div className="grid-3 steps">
            <div className="card"><h3>{t('home.step1Title')}</h3><p className="muted">{t('home.step1')}</p></div>
            <div className="card"><h3>{t('home.step2Title')}</h3><p className="muted">{t('home.step2')}</p></div>
            <div className="card"><h3>{t('home.step3Title')}</h3><p className="muted">{t('home.step3')}</p></div>
          </div>
        </section>
      </div>
    </>
  )
}
