import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

export default function NotFound() {
  const { t } = useTranslation()
  return (
    <div className="page center">
      <h1>{t('notFound.title')}</h1>
      <p className="muted">{t('notFound.text')}</p>
      <Link to="/" className="btn btn-primary">{t('notFound.home')}</Link>
    </div>
  )
}
