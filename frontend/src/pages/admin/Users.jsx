import { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api } from '../../api/client'
import Pagination from '../../components/Pagination.jsx'
import { ErrorAlert, Spinner } from '../../components/Feedback.jsx'
import { dateOnly } from '../../utils/format'

export default function Users() {
  const { t, i18n } = useTranslation()
  const [role, setRole] = useState('')
  const [verified, setVerified] = useState('')
  const [q, setQ] = useState('')
  const [pageNo, setPageNo] = useState(0)
  const [page, setPage] = useState(null)
  const [error, setError] = useState(null)

  const load = useCallback(() => {
    api.admin.users({ role, verified, q, page: pageNo }).then(setPage).catch(setError)
  }, [role, verified, q, pageNo])
  useEffect(() => {
    const timer = setTimeout(load, 250)
    return () => clearTimeout(timer)
  }, [load])

  const run = (fn) => fn().then(load).catch(setError)

  return (
    <div>
      <div className="toolbar">
        <select value={role} onChange={(e) => { setRole(e.target.value); setPageNo(0) }} aria-label={t('admin.role')}>
          <option value="">{t('admin.allRoles')}</option>
          {['SEEKER', 'PROVIDER', 'SPECIALIST', 'ADMIN'].map((r) => <option key={r} value={r}>{t(`role.${r}`)}</option>)}
        </select>
        <select value={verified} onChange={(e) => { setVerified(e.target.value); setPageNo(0) }} aria-label={t('common.verified')}>
          <option value="">{t('admin.anyVerification')}</option>
          <option value="false">{t('admin.notVerified')}</option>
          <option value="true">{t('common.verified')}</option>
        </select>
        <input value={q} onChange={(e) => { setQ(e.target.value); setPageNo(0) }} placeholder={t('admin.searchUsers')} />
      </div>
      <ErrorAlert error={error} />
      {!page ? <Spinner /> : (
        <div className="card table-wrap">
          <table>
            <thead>
              <tr><th>{t('auth.fullName')}</th><th>{t('admin.role')}</th><th>{t('auth.village')}</th><th>{t('admin.joined')}</th><th>{t('common.status')}</th><th /></tr>
            </thead>
            <tbody>
              {page.content.map((u) => (
                <tr key={u.id}>
                  <td><strong>{u.fullName}</strong><div className="muted small">{u.phone}{u.email && ` · ${u.email}`}</div></td>
                  <td>{t(`role.${u.role}`)}</td>
                  <td className="small">{u.village}{u.district && `, ${u.district}`}</td>
                  <td className="small">{dateOnly(u.createdAt, i18n.language)}</td>
                  <td>
                    {u.verified ? <span className="chip">{t('common.verified')}</span> : <span className="chip chip-sun">{t('admin.notVerified')}</span>}{' '}
                    {!u.enabled && <span className="chip chip-red">{t('admin.blocked')}</span>}
                  </td>
                  <td>
                    {u.role !== 'ADMIN' && (
                      <div className="row" style={{ gap: '0.3rem', flexWrap: 'nowrap' }}>
                        <button type="button" className="btn btn-outline btn-sm" onClick={() => run(() => api.admin.verify(u.id, !u.verified))}>
                          {u.verified ? t('admin.unverify') : t('admin.verify')}
                        </button>
                        <button type="button" className="btn btn-danger btn-sm" onClick={() => run(() => api.admin.enable(u.id, !u.enabled))}>
                          {u.enabled ? t('admin.block') : t('admin.unblock')}
                        </button>
                      </div>
                    )}
                  </td>
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
