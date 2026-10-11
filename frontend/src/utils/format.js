const LOCALES = { en: 'en-IN', kn: 'kn-IN', hi: 'hi-IN' }

export function money(value, lang = 'en') {
  if (value === null || value === undefined) return ''
  return new Intl.NumberFormat(LOCALES[lang] || 'en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(value)
}

export function km(value) {
  if (value === null || value === undefined) return ''
  return value < 10 ? value.toFixed(1) : Math.round(value).toString()
}

export function dateTime(value, lang = 'en') {
  if (!value) return ''
  const d = new Date(value)
  return d.toLocaleString(LOCALES[lang] || 'en-IN', {
    day: 'numeric',
    month: 'short',
    hour: 'numeric',
    minute: '2-digit',
  })
}

export function dateOnly(value, lang = 'en') {
  if (!value) return ''
  return new Date(value).toLocaleDateString(LOCALES[lang] || 'en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

/** "22 Oct, 6 AM - 2 PM" style range for booking times (local time strings from the API). */
export function timeRange(start, end, lang = 'en') {
  const s = new Date(start)
  const e = new Date(end)
  const loc = LOCALES[lang] || 'en-IN'
  const day = (d) => d.toLocaleDateString(loc, { day: 'numeric', month: 'short' })
  const hour = (d) => d.toLocaleTimeString(loc, { hour: 'numeric' })
  if (s.toDateString() === e.toDateString()) return `${day(s)}, ${hour(s)} - ${hour(e)}`
  return `${day(s)} ${hour(s)} - ${day(e)} ${hour(e)}`
}

/** Local "YYYY-MM-DDTHH:00:00" string the API expects for booking times. */
export function localHour(date, hour) {
  return `${date}T${String(hour).padStart(2, '0')}:00:00`
}

export function todayIso(offsetDays = 0) {
  const d = new Date()
  d.setDate(d.getDate() + offsetDays)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** Price estimate shown before booking; the server works out the final amount the same way. */
export function estimate(unit, rate, startDate, startHour, endDate, endHour, acres) {
  if (!rate) return null
  const start = new Date(localHour(startDate, startHour))
  const end = new Date(localHour(endDate, endHour))
  const hours = Math.round((end - start) / 3600000)
  if (hours <= 0) return null
  if (unit === 'HOUR') return { quantity: hours, amount: hours * rate }
  if (unit === 'DAY') {
    const last = new Date(end.getTime() - 1000)
    const days = Math.round((new Date(last.toDateString()) - new Date(start.toDateString())) / 86400000) + 1
    return { quantity: days, amount: days * rate }
  }
  if (unit === 'ACRE') {
    const a = Number(acres)
    return a > 0 ? { quantity: a, amount: a * rate } : null
  }
  return null
}
