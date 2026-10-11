export function Stars({ rating, count }) {
  if (!count) return null
  const full = Math.round(rating)
  return (
    <span className="small" title={rating.toFixed(1)}>
      <span className="stars">{'★'.repeat(full)}{'☆'.repeat(5 - full)}</span>{' '}
      <span className="muted">{rating.toFixed(1)} ({count})</span>
    </span>
  )
}

export function StarInput({ value, onChange }) {
  return (
    <div className="star-input" role="radiogroup">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          className={n <= value ? 'on' : ''}
          onClick={() => onChange(n)}
          aria-label={`${n}`}
          aria-checked={n === value}
          role="radio"
        >
          ★
        </button>
      ))}
    </div>
  )
}
