import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'
import StatusChip from '../components/StatusChip.jsx'
import { ErrorAlert, Spinner } from '../components/Feedback.jsx'
import { dateTime, money, timeRange } from '../utils/format'

function QuoteForm({ requirement, onSent }) {
  const { t } = useTranslation()
  const { user } = useAuth()
  const [machines, setMachines] = useState([])
  const [equipmentId, setEquipmentId] = useState('')
  const [amount, setAmount] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const needsMachine = requirement.needType === 'EQUIPMENT'

  useEffect(() => {
    if (!needsMachine || user.role !== 'PROVIDER') return
    api.myEquipment().then((list) => {
      const usable = list.filter((e) => e.status === 'ACTIVE' && e.moderationStatus === 'APPROVED')
      // put machines of the requested type first
      usable.sort((a, b) => (b.category === requirement.category) - (a.category === requirement.category))
      setMachines(usable)
      if (usable[0]) setEquipmentId(String(usable[0].id))
    })
  }, [needsMachine, user.role, requirement.category])

  const roleFits = needsMachine ? user.role === 'PROVIDER' : user.role === 'SPECIALIST'
  if (!roleFits) {
    return <p className="muted small">{needsMachine ? t('quote.onlyProviders') : t('quote.onlySpecialists')}</p>
  }

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await api.sendQuote(requirement.id, {
        equipmentId: needsMachine ? Number(equipmentId) : null,
        amount: Number(amount),
        message: message || null,
      })
      onSent()
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit}>
      <ErrorAlert error={error} />
      {needsMachine && (
        <div className="field">
          <label htmlFor="q-eq">{t('quote.machine')}</label>
          {machines.length === 0 ? (
            <p className="muted small">{t('quote.noMachines')} <Link to="/provider/equipment/new">{t('equipment.add')}</Link></p>
          ) : (
            <select id="q-eq" value={equipmentId} onChange={(e) => setEquipmentId(e.target.value)}>
              {machines.map((m) => <option key={m.id} value={m.id}>{m.title}</option>)}
            </select>
          )}
        </div>
      )}
      <div className="field">
        <label htmlFor="q-amount">{t('quote.amount')}</label>
        <input id="q-amount" type="number" min="1" value={amount} onChange={(e) => setAmount(e.target.value)} required />
      </div>
      <div className="field">
        <label htmlFor="q-msg">{t('quote.message')}</label>
        <textarea id="q-msg" value={message} maxLength={500} onChange={(e) => setMessage(e.target.value)} placeholder={t('quote.messagePlaceholder')} />
      </div>
      <button className="btn btn-primary btn-block" disabled={busy || (needsMachine && !equipmentId)}>
        {busy ? t('common.wait') : t('quote.send')}
      </button>
    </form>
  )
}

export default function RequirementDetail() {
  const { id } = useParams()
  const { t, i18n } = useTranslation()
  const { user } = useAuth()
  const [r, setR] = useState(null)
  const [error, setError] = useState(null)
  const [actionError, setActionError] = useState(null)
  const [notice, setNotice] = useState(null)
  const lang = i18n.language

  const load = useCallback(() => api.requirement(id).then(setR).catch(setError), [id])
  useEffect(() => {
    load()
  }, [load])

  const act = async (fn, okText) => {
    setActionError(null)
    try {
      await fn()
      setNotice(okText)
      load()
    } catch (err) {
      setActionError(err)
    }
  }

  if (error) return <div className="page"><ErrorAlert error={error} /></div>
  if (!r) return <Spinner />
  const isOwner = user?.id === r.seekerId
  const myQuote = user && r.quotes?.find((q) => q.providerId === user.id)
  const what = r.needType === 'EQUIPMENT' ? t(`category.${r.category}`) : t(`skill.${r.skill}`)

  return (
    <div className="page">
      <p className="small"><Link to="/requirements">← {t('requirement.back')}</Link></p>
      <div className="two-col">
        <div>
          <div className="card">
            <div className="row between">
              <h1>{r.title}</h1>
              <StatusChip status={r.status} />
            </div>
            <div className="row" style={{ gap: '0.4rem' }}>
              <span className="chip">{what}</span>
              {r.crop && <span className="chip chip-sun">{t(`crop.${r.crop}`, r.crop)}</span>}
              {r.areaAcres && <span className="chip chip-grey">{t('requirement.acres', { count: Number(r.areaAcres) })}</span>}
            </div>
            <dl className="specs">
              <div><dt>{t('requirement.when')}</dt><dd>{timeRange(r.startAt, r.endAt, lang)}</dd></div>
              <div><dt>{t('requirement.where')}</dt><dd>{r.village}, {r.district}</dd></div>
              <div><dt>{t('requirement.budget')}</dt><dd>{r.budget ? money(r.budget, lang) : '-'}</dd></div>
              <div><dt>{t('requirement.postedBy')}</dt><dd>{r.seekerName}</dd></div>
            </dl>
            {r.description && <p>{r.description}</p>}
            <p className="muted small">{t('requirement.posted')} {dateTime(r.createdAt, lang)}</p>
            {isOwner && r.status === 'OPEN' && (
              <button type="button" className="btn btn-danger btn-sm"
                onClick={() => act(() => api.closeRequirement(r.id), t('requirement.closed'))}>
                {t('requirement.close')}
              </button>
            )}
          </div>

          <div className="card mt">
            <h2>{t('requirement.quotes', { count: r.quoteCount })}</h2>
            {notice && <div className="alert alert-ok">{notice}</div>}
            <ErrorAlert error={actionError} />
            {!r.quotes || r.quotes.length === 0 ? (
              <p className="muted small">{isOwner ? t('quote.noneYet') : t('quote.hidden')}</p>
            ) : (
              <div className="list">
                {r.quotes.map((q) => (
                  <div key={q.id} className="booking-item">
                    <div className="row between">
                      <strong>{q.providerName} {q.providerVerified && <span className="verified">✓</span>}</strong>
                      <span className="price">{money(q.amount, lang)}</span>
                    </div>
                    <div className="muted small">
                      {t(`role.${q.providerRole}`)} · {q.providerVillage}
                      {q.equipmentTitle && <> · <Link to={`/equipment/${q.equipmentId}`}>{q.equipmentTitle}</Link></>}
                    </div>
                    {q.message && <p className="small" style={{ margin: '0.3rem 0' }}>{q.message}</p>}
                    <div className="row between">
                      <StatusChip status={q.status} />
                      {isOwner && r.status === 'OPEN' && q.status === 'PENDING' && (
                        <button type="button" className="btn btn-primary btn-sm"
                          onClick={() => act(() => api.acceptQuote(r.id, q.id), t('quote.acceptedNotice'))}>
                          {t('quote.accept')}
                        </button>
                      )}
                      {myQuote?.id === q.id && q.status === 'PENDING' && (
                        <button type="button" className="btn btn-danger btn-sm"
                          onClick={() => act(() => api.withdrawQuote(q.id), t('quote.withdrawn'))}>
                          {t('quote.withdraw')}
                        </button>
                      )}
                      {q.bookingId && (isOwner || myQuote?.id === q.id) && (
                        <Link to={isOwner ? '/bookings' : '/provider/bookings'} className="small">{t('quote.seeBooking')}</Link>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <aside className="card booking-box">
          {isOwner ? (
            <>
              <h3>{t('requirement.yours')}</h3>
              <p className="muted small">{t('requirement.yoursHint')}</p>
            </>
          ) : !user ? (
            <p><Link to="/login">{t('requirement.loginToQuote')}</Link></p>
          ) : r.status !== 'OPEN' ? (
            <p className="muted">{t('requirement.notOpen')}</p>
          ) : myQuote ? (
            <p className="muted">{t('quote.alreadySent')}</p>
          ) : (
            <>
              <h3>{t('quote.title')}</h3>
              <QuoteForm requirement={r} onSent={() => { setNotice(t('quote.sentNotice')); load() }} />
            </>
          )}
        </aside>
      </div>
    </div>
  )
}
