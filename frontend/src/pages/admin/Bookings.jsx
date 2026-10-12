import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api } from '../../api/client'
import Pagination from '../../components/Pagination.jsx'
import StatusChip from '../../components/StatusChip.jsx'
import { ErrorAlert, Spinner } from '../../components/Feedback.jsx'
import { money, timeRange } from '../../utils/format'

export default function Bookings() {
  const { t, i18n } = useTranslation()
  const [status, setStatus] = useState('')
  const [pageNo, setPageNo] = useState(0)
  const [page, setPage] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    api.admin.bookings({ status, page: pageNo }).then(setPage).catch(setError)
  }, [status, pageNo])

  return (
    <div>
      <div className="toolbar">
        <select value={status} onChange={(e) => { setStatus(e.target.value); setPageNo(0) }} aria-label={t('common.status')}>
          <option value="">{t('common.all')}</option>
          {['PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'EXPIRED', 'REJECTED'].map((s) => <option key={s} value={s}>{t(`status.${s}`)}</option>)}
        </select>
      </div>
      <ErrorAlert error={error} />
      {!page ? <Spinner /> : (
        <div className="card table-wrap">
          <table>
            <thead>
              <tr><th>#</th><th>{t('booking.farmer')}</th><th>{t('admin.items')}</th><th>{t('requirement.when')}</th><th>{t('booking.total')}</th><th>{t('common.status')}</th></tr>
            </thead>
            <tbody>
              {page.content.map((b) => (
                <tr key={b.id}>
                  <td>{b.id}{b.kind === 'BUNDLE' && <div className="chip chip-sun">{t('booking.bundle')}</div>}</td>
                  <td>{b.seekerName}<div className="muted small">{b.seekerVillage}</div></td>
                  <td className="small">{b.items.map((i) => <div key={i.id}>{i.resourceName} <span className="muted">({i.providerName})</span></div>)}</td>
                  <td className="small">{timeRange(b.startAt, b.endAt, i18n.language)}</td>
                  <td>{money(b.totalAmount, i18n.language)}</td>
                  <td><StatusChip status={b.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {page && <Pagination page={page.page} totalPages={page.totalPages} onChange={setPageNo} />}
    </div>
  )
}
