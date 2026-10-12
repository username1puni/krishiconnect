import en from '../locales/en/common.json'
import kn from '../locales/kn/common.json'
import hi from '../locales/hi/common.json'

function keys(obj, prefix = '') {
  return Object.entries(obj).flatMap(([k, v]) =>
    typeof v === 'object' ? keys(v, `${prefix}${k}.`) : [`${prefix}${k}`],
  )
}

describe('translations', () => {
  const english = keys(en).sort()

  it.each([
    ['kn', kn],
    ['hi', hi],
  ])('%s has exactly the same keys as English', (_, locale) => {
    expect(keys(locale).sort()).toEqual(english)
  })

  it.each([
    ['kn', kn],
    ['hi', hi],
  ])('%s keeps the {{placeholders}} of every string', (_, locale) => {
    const flat = (o, p = '') =>
      Object.entries(o).flatMap(([k, v]) => (typeof v === 'object' ? flat(v, `${p}${k}.`) : [[`${p}${k}`, v]]))
    const translated = Object.fromEntries(flat(locale))
    for (const [key, text] of flat(en)) {
      const vars = (text.match(/{{\w+}}/g) || []).sort()
      expect([key, (translated[key].match(/{{\w+}}/g) || []).sort()]).toEqual([key, vars])
    }
  })

  it('is not just English copied over', () => {
    expect(kn.nav.equipment).not.toEqual(en.nav.equipment)
    expect(hi.booking.book).not.toEqual(en.booking.book)
  })
})
