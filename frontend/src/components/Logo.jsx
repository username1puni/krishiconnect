export default function Logo({ size = 30 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 64 64" aria-hidden="true">
      <rect width="64" height="64" rx="14" fill="#1f5130" />
      <path d="M32 50c0-14 6-24 18-30-2 14-8 24-18 30z" fill="#9fd36b" />
      <path d="M32 50c0-12-5-20-15-25 1 12 6 20 15 25z" fill="#d9f2b4" />
      <rect x="30.5" y="34" width="3" height="18" rx="1.5" fill="#d9f2b4" />
    </svg>
  )
}
