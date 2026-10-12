import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api } from '../../api/client'
import { ErrorAlert, Spinner } from '../../components/Feedback.jsx'
import { dateTime } from '../../utils/format'

export default function Routing() {
  const { t, i18n } = useTranslation()
  const [s, setS] = useState(null)
  const [error, setError] = useState(null)
  const [msg, setMsg] = useState(null)

  const load = () => api.admin.routing().then(setS).catch(setError)
  useEffect(() => {
    load()
  }, [])

  if (!s) return error ? <ErrorAlert error={error} /> : <Spinner />
  const lang = i18n.language
  return (
    <div className="card" style={{ maxWidth: 640 }}>
      <h3>{t('admin.routingTitle')}</h3>
      <p className="muted small">{t('admin.routingHint')}</p>
      <ErrorAlert error={error} />
      {msg && <div className="alert alert-ok">{msg}</div>}
      <dl className="specs">
        <div><dt>{t('common.status')}</dt><dd>{s.enabled ? (s.coolingDown ? t('admin.routingCooling') : t('admin.routingOn')) : t('admin.routingOff')}</dd></div>
        <div><dt>{t('admin.apiCalls')}</dt><dd>{s.apiCalls}</dd></div>
        <div><dt>{t('admin.apiFailures')}</dt><dd>{s.apiFailures}</dd></div>
        <div><dt>{t('admin.cacheHits')}</dt><dd>{s.cacheHits}</dd></div>
        <div><dt>{t('admin.cachedRoutes')}</dt><dd>{s.cachedRoutes}</dd></div>
        <div><dt>{t('admin.maxDestinations')}</dt><dd>{s.maxDestinations}</dd></div>
      </dl>
      {s.lastSuccessAt && <p className="small">{t('admin.lastSuccess')}: {dateTime(s.lastSuccessAt, lang)}</p>}
      {s.lastFailureAt && <p className="small">{t('admin.lastFailure')}: {dateTime(s.lastFailureAt, lang)} {s.lastError && <span className="muted">({s.lastError})</span>}</p>}
      <div className="row">
        <button type="button" className={s.enabled ? 'btn btn-danger' : 'btn btn-primary'}
          onClick={() => api.admin.setRouting(!s.enabled).then((r) => { setS(r); setMsg(r.enabled ? t('admin.routingEnabled') : t('admin.routingDisabled')) }).catch(setError)}>
          {s.enabled ? t('admin.disableRouting') : t('admin.enableRouting')}
        </button>
        <button type="button" className="btn btn-outline"
          onClick={() => api.admin.clearRoutingCache().then(() => { setMsg(t('admin.cacheCleared')); load() }).catch(setError)}>
          {t('admin.clearCache')}
        </button>
      </div>
    </div>
  )
}
