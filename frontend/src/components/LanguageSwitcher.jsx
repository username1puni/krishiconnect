import { useTranslation } from 'react-i18next'
import { LANGUAGES } from '../i18n'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'

export default function LanguageSwitcher() {
  const { i18n, t } = useTranslation()
  const { user } = useAuth()

  const change = (code) => {
    i18n.changeLanguage(code)
    // remember it on the account too, so it follows the user to another phone
    if (user) api.saveLanguage(code).catch(() => {})
  }

  return (
    <div className="lang-switch" role="group" aria-label={t('nav.language')}>
      {LANGUAGES.map((l) => (
        <button
          key={l.code}
          type="button"
          className={i18n.language === l.code ? 'active' : ''}
          onClick={() => change(l.code)}
          lang={l.code}
        >
          {l.label}
        </button>
      ))}
    </div>
  )
}
