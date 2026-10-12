import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'
import { PlaceSearch } from '../components/LocationBar.jsx'
import { photoUrl } from '../components/Photo.jsx'
import { ErrorAlert, Spinner } from '../components/Feedback.jsx'
import { CATEGORIES } from '../utils/options'

const EMPTY = {
  title: '', category: 'TRACTOR', brand: '', model: '', yearOfMake: '', horsePower: '', description: '',
  pricePerHour: '', pricePerDay: '', pricePerAcre: '', withOperator: false,
}
const num = (v) => (v === '' || v === null || v === undefined ? null : Number(v))

export default function EquipmentForm() {
  const { id } = useParams()
  const { t } = useTranslation()
  const { user } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState(EMPTY)
  const [place, setPlace] = useState(
    user?.latitude ? { name: user.village, district: user.district, latitude: user.latitude, longitude: user.longitude } : null,
  )
  const [photos, setPhotos] = useState([])
  const [loading, setLoading] = useState(Boolean(id))
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    if (!id) return
    api.equipmentById(id).then((e) => {
      const f = { ...EMPTY }
      Object.keys(EMPTY).forEach((k) => {
        f[k] = e[k] ?? EMPTY[k]
      })
      setForm(f)
      setPlace({ name: e.village, district: e.district, latitude: e.latitude, longitude: e.longitude })
      setPhotos(e.photoIds)
      setLoading(false)
    }).catch(setError)
  }, [id])

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    if (!place) {
      setError({ message: t('requirement.pickVillage') })
      return
    }
    setBusy(true)
    setError(null)
    const body = {
      ...form,
      yearOfMake: num(form.yearOfMake),
      horsePower: num(form.horsePower),
      pricePerHour: num(form.pricePerHour),
      pricePerDay: num(form.pricePerDay),
      pricePerAcre: num(form.pricePerAcre),
      village: place.name,
      district: place.district,
      latitude: place.latitude,
      longitude: place.longitude,
    }
    try {
      if (id) {
        await api.updateEquipment(id, body)
        setSaved(true)
      } else {
        const created = await api.createEquipment(body)
        navigate(`/provider/equipment/${created.id}/edit`, { replace: true, state: { created: true } })
      }
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  const upload = async (e) => {
    const file = e.target.files[0]
    e.target.value = ''
    if (!file) return
    setError(null)
    try {
      const res = await api.uploadPhoto(id, file)
      setPhotos((p) => [...p, res.id])
    } catch (err) {
      setError(err)
    }
  }

  const removePhoto = async (pid) => {
    try {
      await api.deletePhoto(id, pid)
      setPhotos((p) => p.filter((x) => x !== pid))
    } catch (err) {
      setError(err)
    }
  }

  if (loading && !error) return <Spinner />
  const fe = (k) => error?.fields?.[k] && <div className="field-error">{error.fields[k]}</div>

  return (
    <div className="page page-narrow">
      <p className="small"><Link to="/provider/equipment">← {t('provider.equipmentTitle')}</Link></p>
      <div className="card">
        <h1>{id ? t('equipment.editTitle') : t('equipment.addTitle')}</h1>
        {!id && <p className="muted small">{t('equipment.reviewNote')}</p>}
        {saved && <div className="alert alert-ok">{t('equipment.saved')}</div>}
        <ErrorAlert error={error && !error.fields ? error : null} />
        <form onSubmit={submit}>
          <div className="field">
            <label htmlFor="title">{t('equipment.titleLabel')}</label>
            <input id="title" value={form.title} onChange={set('title')} placeholder="Mahindra 575 DI tractor with rotavator" required />
            {fe('title')}
          </div>
          <div className="form-row">
            <div className="field">
              <label htmlFor="category">{t('equipment.category')}</label>
              <select id="category" value={form.category} onChange={set('category')}>
                {CATEGORIES.map((c) => <option key={c} value={c}>{t(`category.${c}`)}</option>)}
              </select>
            </div>
            <div className="field">
              <label htmlFor="brand">{t('equipment.brand')}</label>
              <input id="brand" value={form.brand} onChange={set('brand')} />
            </div>
          </div>
          <div className="form-row">
            <div className="field">
              <label htmlFor="model">{t('equipment.model')}</label>
              <input id="model" value={form.model} onChange={set('model')} />
            </div>
            <div className="field">
              <label htmlFor="hp">{t('equipment.hp')}</label>
              <input id="hp" type="number" min="1" value={form.horsePower} onChange={set('horsePower')} />
            </div>
          </div>
          <div className="field">
            <label htmlFor="year">{t('equipment.year')}</label>
            <input id="year" type="number" min="1980" max="2030" value={form.yearOfMake} onChange={set('yearOfMake')} />
          </div>
          <label>{t('equipment.rates')}</label>
          <p className="muted small">{t('equipment.ratesHint')}</p>
          <div className="form-row">
            <div className="field">
              <label htmlFor="ph" className="small">{t('price.perHour')}</label>
              <input id="ph" type="number" min="0" value={form.pricePerHour} onChange={set('pricePerHour')} />
            </div>
            <div className="field">
              <label htmlFor="pd" className="small">{t('price.perDay')}</label>
              <input id="pd" type="number" min="0" value={form.pricePerDay} onChange={set('pricePerDay')} />
            </div>
          </div>
          <div className="field">
            <label htmlFor="pa" className="small">{t('price.perAcre')}</label>
            <input id="pa" type="number" min="0" value={form.pricePerAcre} onChange={set('pricePerAcre')} />
            {fe('pricePerHour')}
          </div>
          <label className="check field">
            <input type="checkbox" checked={form.withOperator} onChange={set('withOperator')} /> {t('equipment.withOperatorLabel')}
          </label>
          <div className="field">
            <label htmlFor="desc">{t('equipment.description')}</label>
            <textarea id="desc" value={form.description} onChange={set('description')} />
          </div>
          <div className="field">
            <label>{t('equipment.location')}</label>
            {place ? (
              <div className="row">
                <span className="chip">{place.name}, {place.district}</span>
                <button type="button" className="btn btn-outline btn-sm" onClick={() => setPlace(null)}>{t('common.change')}</button>
              </div>
            ) : (
              <PlaceSearch onPick={setPlace} />
            )}
          </div>
          <button className="btn btn-primary btn-block" disabled={busy}>
            {busy ? t('common.wait') : id ? t('common.save') : t('equipment.addAndPhotos')}
          </button>
        </form>
      </div>

      {id && (
        <div className="card mt">
          <h2>{t('equipment.photosTitle')}</h2>
          <p className="muted small">{t('equipment.photosHint')}</p>
          <div className="thumbs" style={{ flexWrap: 'wrap' }}>
            {photos.map((pid) => (
              <div key={pid} style={{ position: 'relative' }}>
                <img src={photoUrl(pid)} alt="" style={{ width: 120, height: 90, objectFit: 'cover', borderRadius: 8 }} />
                <button type="button" className="btn btn-danger btn-sm" style={{ position: 'absolute', top: 4, right: 4, padding: '0 6px' }}
                  onClick={() => removePhoto(pid)} aria-label={t('common.delete')}>×</button>
              </div>
            ))}
          </div>
          {photos.length < 4 && (
            <label className="btn btn-outline mt" style={{ display: 'inline-flex' }}>
              {t('equipment.uploadPhoto')}
              <input type="file" accept="image/jpeg,image/png,image/webp" onChange={upload} hidden />
            </label>
          )}
        </div>
      )}
    </div>
  )
}
