import { useTranslation } from 'react-i18next'

export function Spinner() {
  return <div className="spinner" role="status" aria-label="loading" />
}

export function Empty({ children }) {
  return <div className="empty">{children}</div>
}

export function ErrorAlert({ error }) {
  const { t } = useTranslation()
  if (!error) return null
  // 409 means somebody else got there first, show the server's reason under a friendly title
  if (error.status === 409) {
    return (
      <div className="alert alert-error" role="alert">
        <strong>{t('errors.conflictTitle')}</strong> {error.message}
      </div>
    )
  }
  return (
    <div className="alert alert-error" role="alert">
      {error.status === 0 ? t('errors.network') : error.message || t('errors.generic')}
    </div>
  )
}
