import { estimate, km, localHour } from '../utils/format'
import { busyHours } from '../components/AvailabilityCalendar.jsx'

describe('estimate', () => {
  it('charges per hour', () => {
    expect(estimate('HOUR', 900, '2026-11-02', 7, '2026-11-02', 13)).toEqual({ quantity: 6, amount: 5400 })
  })

  it('counts every calendar day the booking touches', () => {
    expect(estimate('DAY', 3500, '2026-11-02', 6, '2026-11-02', 18).quantity).toBe(1)
    expect(estimate('DAY', 3500, '2026-11-02', 6, '2026-11-03', 18)).toEqual({ quantity: 2, amount: 7000 })
  })

  it('charges per acre using the land size', () => {
    expect(estimate('ACRE', 1200, '2026-11-02', 6, '2026-11-02', 10, '2.5')).toEqual({ quantity: 2.5, amount: 3000 })
    expect(estimate('ACRE', 1200, '2026-11-02', 6, '2026-11-02', 10, '')).toBeNull()
  })

  it('returns nothing when the end is before the start', () => {
    expect(estimate('HOUR', 900, '2026-11-02', 13, '2026-11-02', 7)).toBeNull()
  })
})

describe('helpers', () => {
  it('formats kilometres', () => {
    expect(km(3.456)).toBe('3.5')
    expect(km(37.6)).toBe('38')
  })

  it('builds the local time string the API expects', () => {
    expect(localHour('2026-11-02', 7)).toBe('2026-11-02T07:00:00')
  })

  it('works out booked working hours of a day for the calendar', () => {
    const ranges = [
      { startAt: '2026-11-02T05:00:00', endAt: '2026-11-02T09:00:00' },
      { startAt: '2026-11-02T14:00:00', endAt: '2026-11-03T10:00:00' },
    ]
    // 6-9 and 14-18 on the 2nd, 6-10 on the 3rd
    expect(busyHours('2026-11-02', ranges)).toBe(7)
    expect(busyHours('2026-11-03', ranges)).toBe(4)
    expect(busyHours('2026-11-04', ranges)).toBe(0)
  })
})
