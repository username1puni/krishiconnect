const TOKEN_KEY = 'krishi.token'

export class ApiError extends Error {
  constructor(body) {
    super(body.message)
    this.status = body.status
    this.fields = body.fields
  }
}

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

async function request(path, options = {}) {
  const headers = new Headers(options.headers)
  const token = tokenStore.get()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json')

  let response
  try {
    response = await fetch(path, { ...options, headers })
  } catch {
    throw new ApiError({ status: 0, message: 'Cannot reach the server. Check your internet connection.' })
  }
  if (response.status === 401 && token) {
    // token expired or account blocked
    tokenStore.clear()
    window.dispatchEvent(new Event('krishi:logout'))
  }
  if (!response.ok) {
    let body = { status: response.status, message: 'Something went wrong. Please try again.' }
    try {
      body = await response.json()
    } catch {
      // not JSON, keep the generic message
    }
    throw new ApiError(body)
  }
  if (response.status === 204) return undefined
  return response.json()
}

const json = (data) => JSON.stringify(data)

export const query = (params) => {
  const q = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') q.set(k, String(v))
  })
  const s = q.toString()
  return s ? `?${s}` : ''
}

export const api = {
  register: (data) => request('/api/auth/register', { method: 'POST', body: json(data) }),
  login: (identifier, password) => request('/api/auth/login', { method: 'POST', body: json({ identifier, password }) }),
  me: () => request('/api/users/me'),
  updateProfile: (data) => request('/api/users/me', { method: 'PUT', body: json(data) }),
  saveLanguage: (lang) => request('/api/users/me/language', { method: 'PUT', body: json({ lang }) }),
  updateSpecialist: (data) => request('/api/users/me/specialist', { method: 'PUT', body: json(data) }),

  places: (q) => request(`/api/places${query({ q })}`),
  nearestPlace: (lat, lng) => request(`/api/places/nearest${query({ lat, lng })}`),

  equipment: (params) => request(`/api/equipment${query(params)}`),
  equipmentById: (id) => request(`/api/equipment/${id}`),
  myEquipment: () => request('/api/equipment/mine'),
  createEquipment: (data) => request('/api/equipment', { method: 'POST', body: json(data) }),
  updateEquipment: (id, data) => request(`/api/equipment/${id}`, { method: 'PUT', body: json(data) }),
  setEquipmentStatus: (id, status) => request(`/api/equipment/${id}/status`, { method: 'PATCH', body: json({ status }) }),
  uploadPhoto: (id, file) => {
    const form = new FormData()
    form.append('file', file)
    return request(`/api/equipment/${id}/photos`, { method: 'POST', body: form })
  },
  deletePhoto: (id, photoId) => request(`/api/equipment/${id}/photos/${photoId}`, { method: 'DELETE' }),
  equipmentAvailability: (id, from, days = 31) => request(`/api/equipment/${id}/availability${query({ from, days })}`),
  equipmentReviews: (id, page = 0) => request(`/api/equipment/${id}/reviews${query({ page })}`),

  specialists: (params) => request(`/api/specialists${query(params)}`),
  specialistById: (id) => request(`/api/specialists/${id}`),
  specialistAvailability: (id, from, days = 31) => request(`/api/specialists/${id}/availability${query({ from, days })}`),
  specialistReviews: (id, page = 0) => request(`/api/specialists/${id}/reviews${query({ page })}`),

  createBooking: (data) => request('/api/bookings', { method: 'POST', body: json(data) }),
  myBookings: (params) => request(`/api/bookings${query(params)}`),
  cancelBooking: (id) => request(`/api/bookings/${id}/cancel`, { method: 'POST' }),
  incoming: (params) => request(`/api/provider/bookings${query(params)}`),
  acceptItem: (itemId) => request(`/api/provider/bookings/items/${itemId}/accept`, { method: 'POST' }),
  rejectItem: (itemId) => request(`/api/provider/bookings/items/${itemId}/reject`, { method: 'POST' }),
  completeItem: (itemId) => request(`/api/provider/bookings/items/${itemId}/complete`, { method: 'POST' }),

  requirements: (params) => request(`/api/requirements${query(params)}`),
  myRequirements: () => request('/api/requirements/mine'),
  requirement: (id) => request(`/api/requirements/${id}`),
  createRequirement: (data) => request('/api/requirements', { method: 'POST', body: json(data) }),
  closeRequirement: (id) => request(`/api/requirements/${id}/close`, { method: 'POST' }),
  sendQuote: (id, data) => request(`/api/requirements/${id}/quotes`, { method: 'POST', body: json(data) }),
  acceptQuote: (id, quoteId) => request(`/api/requirements/${id}/quotes/${quoteId}/accept`, { method: 'POST' }),
  myQuotes: () => request('/api/quotes/mine'),
  withdrawQuote: (quoteId) => request(`/api/quotes/${quoteId}/withdraw`, { method: 'POST' }),

  notifications: (page = 0) => request(`/api/notifications${query({ page })}`),
  unreadCount: () => request('/api/notifications/unread-count'),
  markRead: (id) => request(`/api/notifications/${id}/read`, { method: 'POST' }),
  markAllRead: () => request('/api/notifications/read-all', { method: 'POST' }),

  recommendations: (params) => request(`/api/recommendations${query(params)}`),
  crops: () => request('/api/recommendations/crops'),

  addReview: (data) => request('/api/reviews', { method: 'POST', body: json(data) }),
  myReviewedItems: () => request('/api/reviews/mine'),

  admin: {
    dashboard: () => request('/api/admin/dashboard'),
    users: (params) => request(`/api/admin/users${query(params)}`),
    verify: (id, value) => request(`/api/admin/users/${id}/verify`, { method: 'PATCH', body: json({ value }) }),
    enable: (id, value) => request(`/api/admin/users/${id}/enabled`, { method: 'PATCH', body: json({ value }) }),
    listings: (params) => request(`/api/admin/equipment${query(params)}`),
    moderate: (id, status, note) =>
      request(`/api/admin/equipment/${id}/moderation`, { method: 'PATCH', body: json({ status, note }) }),
    bookings: (params) => request(`/api/admin/bookings${query(params)}`),
    routing: () => request('/api/admin/routing'),
    setRouting: (value) => request('/api/admin/routing', { method: 'PUT', body: json({ value }) }),
    clearRoutingCache: () => request('/api/admin/routing/clear-cache', { method: 'POST' }),
  },
}
