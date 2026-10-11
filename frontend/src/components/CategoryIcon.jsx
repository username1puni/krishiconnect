// Simple line icons so cards without a photo still look right
const PATHS = {
  TRACTOR: 'M6 17a3 3 0 1 0 0 .01M18 18a2 2 0 1 0 0 .01M4 14V9h6l2 5M12 14h6l1-4h-5V6h-3',
  COMBINE_HARVESTER: 'M3 17h4M7 18a2 2 0 1 0 0 .01M17 17a3 3 0 1 0 0 .01M5 15V8h9v7M14 10h4l2 5M3 11h2',
  SPRAYER: 'M8 3h6v4H8zM11 7v4M6 11h10l1 4H5zM7 15v4M15 15v4M4 21h14',
  AGRI_DRONE: 'M5 6h4M15 6h4M7 6v3M17 6v3M8 10h8v3H8zM10 13l-2 4M14 13l2 4',
  WATER_PUMP: 'M4 14h6v5H4zM10 16h4c3 0 5-2 5-5V5M17 5h4M7 14v-3',
  DEFAULT: 'M4 18h16M6 18V9l6-4 6 4v9M10 18v-5h4v5',
}

export default function CategoryIcon({ category, size = 40 }) {
  const d = PATHS[category] || PATHS.DEFAULT
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6"
      strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d={d} />
    </svg>
  )
}
