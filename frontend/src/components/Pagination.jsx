import { useTranslation } from 'react-i18next'

export default function Pagination({ page, totalPages, onChange }) {
  const { t } = useTranslation()
  if (!totalPages || totalPages <= 1) return null
  return (
    <div className="pagination">
      <button type="button" className="btn btn-outline btn-sm" disabled={page === 0} onClick={() => onChange(page - 1)}>
        {t('common.prev')}
      </button>
      <span className="muted small">{t('common.pageOf', { page: page + 1, total: totalPages })}</span>
      <button
        type="button"
        className="btn btn-outline btn-sm"
        disabled={page + 1 >= totalPages}
        onClick={() => onChange(page + 1)}
      >
        {t('common.next')}
      </button>
    </div>
  )
}
