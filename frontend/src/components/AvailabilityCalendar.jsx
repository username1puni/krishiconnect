import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { todayIso } from '../utils/format'

const DAY_START = 6
const DAY_END = 18

/** How many working hours (6 AM to 6 PM) of the given day are covered by busy ranges. */
export function busyHours(dayIso, ranges) {
  const from = new Date(`${dayIso}T${String(DAY_START).padStart(2, '0')}:00:00`).getTime()
  const to = new Date(`${dayIso}T${DAY_END}:00:00`).getTime()
  let ms = 0
  for (const r of ranges) {
    const s = Math.max(from, new Date(r.startAt).getTime())
    const e = Math.min(to, new Date(r.endAt).getTime())
    if (e > s) ms += e - s
  }
  return ms / 3600000
}

export default function AvailabilityCalendar({ load, refreshKey }) {
  const { t, i18n } = useTranslation()
  const [ranges, setRanges] = useState([])
  const today = todayIso()

  useEffect(() => {
    load(today, 28).then(setRanges).catch(() => setRanges([]))
  }, [load, today, refreshKey])

  // start the grid on Monday of this week
  const first = new Date(`${today}T00:00:00`)
  first.setDate(first.getDate() - ((first.getDay() + 6) % 7))
  const days = []
  for (let i = 0; i < 35; i++) {
    const d = new Date(first)
    d.setDate(first.getDate() + i)
    const iso = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    days.push({ iso, d })
  }
  const dow = days.slice(0, 7).map(({ d }) => d.toLocaleDateString(i18n.language, { weekday: 'narrow' }))

  return (
    <div>
      <div className="calendar">
        {dow.map((name, i) => (
          <div key={i} className="dow">{name}</div>
        ))}
        {days.map(({ iso, d }) => {
          const hours = iso < today ? 0 : busyHours(iso, ranges)
          let cls = 'day'
          if (iso < today) cls += ' past'
          else if (hours >= DAY_END - DAY_START) cls += ' busy'
          else if (hours > 0) cls += ' partly'
          if (iso === today) cls += ' today'
          return (
            <div key={iso} className={cls} title={hours > 0 ? t('calendar.bookedHours', { count: Math.round(hours) }) : ''}>
              {d.getDate()}
              {d.getDate() === 1 && <small>{d.toLocaleDateString(i18n.language, { month: 'short' })}</small>}
            </div>
          )
        })}
      </div>
      <div className="legend">
        <span className="l-free">{t('calendar.free')}</span>
        <span className="l-partly">{t('calendar.partly')}</span>
        <span className="l-busy">{t('calendar.busy')}</span>
      </div>
    </div>
  )
}
