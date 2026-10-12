import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api } from '../../api/client'
import BarChart from '../../components/BarChart.jsx'
import { ErrorAlert, Spinner } from '../../components/Feedback.jsx'
import { money } from '../../utils/format'

const COLORS = ['#2b6a3f', '#f2b632', '#2563a8', '#8a5a2b', '#9fd36b', '#b0b8ad']

const monthLabel = (m, lang) => new Date(`${m}-01T00:00:00`).toLocaleDateString(lang, { month: 'short' })

export default function Dashboard() {
  const { t, i18n } = useTranslation()
  const [d, setD] = useState(null)
  const [error, setError] = useState(null)
  const lang = i18n.language

  useEffect(() => {
    api.admin.dashboard().then(setD).catch(setError)
  }, [])

  if (error) return <ErrorAlert error={error} />
  if (!d) return <Spinner />

  const users = Object.values(d.usersByRole).reduce((a, b) => a + b, 0)
  const bookings = Object.values(d.bookingsByStatus).reduce((a, b) => a + b, 0)

  // seasonal demand: the five busiest categories get their own colour, the rest are grouped
  const top = d.topCategories.slice(0, 5).map((c) => c.category)
  const months = [...new Set(d.seasonalDemand.map((x) => x.month))].sort()
  const seasonal = months.map((m) => {
    const values = {}
    d.seasonalDemand.filter((x) => x.month === m).forEach((x) => {
      const key = top.includes(x.category) ? x.category : 'OTHER'
      values[key] = (values[key] || 0) + x.bookings
    })
    return { label: monthLabel(m, lang), values }
  })
  const seasonalSeries = [...top, 'OTHER'].map((k, i) => ({
    key: k,
    label: k === 'OTHER' ? t('admin.other') : t(`category.${k}`),
    color: COLORS[i],
  }))

  return (
    <div className="stack">
      <div className="grid-4">
        <div className="card stat"><div className="value">{users}</div><div className="label">{t('admin.users')}</div></div>
        <div className="card stat"><div className="value">{d.equipmentListed}</div><div className="label">{t('admin.machines')}</div></div>
        <div className="card stat"><div className="value">{bookings}</div><div className="label">{t('admin.bookings')}</div></div>
        <div className="card stat"><div className="value">{money(d.bookedValue, lang)}</div><div className="label">{t('admin.bookedValue')}</div></div>
      </div>
      <div className="grid-4">
        <div className="card stat"><div className="value">{d.pendingVerifications}</div><div className="label">{t('admin.pendingVerifications')}</div></div>
        <div className="card stat"><div className="value">{d.pendingListings}</div><div className="label">{t('admin.pendingListings')}</div></div>
        <div className="card stat"><div className="value">{d.openRequirements}</div><div className="label">{t('admin.openRequirements')}</div></div>
        <div className="card stat">
          <div className="value" style={{ fontSize: '1.1rem' }}>
            {d.routing.enabled ? (d.routing.coolingDown ? t('admin.routingCooling') : t('admin.routingOn')) : t('admin.routingOff')}
          </div>
          <div className="label">{t('admin.routing')}</div>
        </div>
      </div>
      <div className="two-col">
        <div className="card">
          <h3>{t('admin.seasonalDemand')}</h3>
          <p className="muted small">{t('admin.seasonalHint')}</p>
          <BarChart data={seasonal} series={seasonalSeries} />
        </div>
        <div className="card">
          <h3>{t('admin.byStatus')}</h3>
          <table>
            <tbody>
              {Object.entries(d.bookingsByStatus).map(([s, n]) => (
                <tr key={s}><td><span className={`chip status-${s}`}>{t(`status.${s}`)}</span></td><td>{n}</td></tr>
              ))}
            </tbody>
          </table>
          <h3 className="mt">{t('admin.usersByRole')}</h3>
          <table>
            <tbody>
              {Object.entries(d.usersByRole).map(([r, n]) => <tr key={r}><td>{t(`role.${r}`)}</td><td>{n}</td></tr>)}
            </tbody>
          </table>
        </div>
      </div>
      <div className="card">
        <h3>{t('admin.monthly')}</h3>
        <BarChart
          data={d.monthly.map((m) => ({ label: monthLabel(m.month, lang), values: { b: m.bookings } }))}
          series={[{ key: 'b', label: t('admin.bookings'), color: '#3a8550' }]}
          height={180}
        />
      </div>
    </div>
  )
}
