import { useState } from 'react'
import CategoryIcon from './CategoryIcon.jsx'

export const photoUrl = (id) => `/api/photos/${id}`

export default function Photo({ id, alt, category }) {
  const [broken, setBroken] = useState(false)
  if (!id || broken) {
    return (
      <div className="photo-fallback">
        <CategoryIcon category={category} size={56} />
      </div>
    )
  }
  return <img src={photoUrl(id)} alt={alt} loading="lazy" onError={() => setBroken(true)} />
}
