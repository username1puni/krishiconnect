import { useTranslation } from 'react-i18next'

export default function StatusChip({ status }) {
  const { t } = useTranslation()
  return <span className={`chip status-${status}`}>{t(`status.${status}`)}</span>
}
