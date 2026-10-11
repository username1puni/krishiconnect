import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'

// Every locales/<lang>/<namespace>.json file is picked up automatically
const files = import.meta.glob('./locales/*/*.json', { eager: true })

const resources = {}
for (const [path, mod] of Object.entries(files)) {
  const [, lang, ns] = path.match(/locales\/(\w+)\/(\w+)\.json$/)
  resources[lang] = resources[lang] || {}
  resources[lang][ns] = mod.default
}

export const LANGUAGES = [
  { code: 'en', label: 'English' },
  { code: 'kn', label: 'ಕನ್ನಡ' },
  { code: 'hi', label: 'हिन्दी' },
]

const saved = typeof localStorage !== 'undefined' ? localStorage.getItem('krishi.lang') : null

i18n.use(initReactI18next).init({
  resources,
  lng: saved || 'en',
  fallbackLng: 'en',
  defaultNS: 'common',
  interpolation: { escapeValue: false },
})

i18n.on('languageChanged', (lng) => {
  localStorage.setItem('krishi.lang', lng)
  document.documentElement.lang = lng
})

export default i18n
