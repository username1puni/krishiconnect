import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { api } from '../api/client'
import { useAuth } from './AuthContext.jsx'

const LocationContext = createContext(null)
const KEY = 'krishi.location'

function load() {
  try {
    return JSON.parse(localStorage.getItem(KEY))
  } catch {
    return null
  }
}

/**
 * Where the user is searching from: browser GPS, a village picked from the list, or the
 * location saved on their profile. Saved in localStorage so it survives a refresh.
 */
export function LocationProvider({ children }) {
  const { user } = useAuth()
  const [location, setLocationState] = useState(load)
  const [locating, setLocating] = useState(false)
  const [error, setError] = useState(null)

  const setLocation = useCallback((loc) => {
    setLocationState(loc)
    if (loc) localStorage.setItem(KEY, JSON.stringify(loc))
    else localStorage.removeItem(KEY)
  }, [])

  // fall back to the profile location if nothing was chosen yet
  useEffect(() => {
    if (!location && user?.latitude && user?.longitude) {
      setLocation({ lat: user.latitude, lng: user.longitude, label: user.village || '', source: 'profile' })
    }
  }, [user, location, setLocation])

  const useGps = useCallback(() => {
    if (!navigator.geolocation) {
      setError('gps_unavailable')
      return
    }
    setLocating(true)
    setError(null)
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        const lat = Number(pos.coords.latitude.toFixed(5))
        const lng = Number(pos.coords.longitude.toFixed(5))
        let label = ''
        try {
          const place = await api.nearestPlace(lat, lng)
          label = place.name
        } catch {
          // no town nearby, show coordinates only
        }
        setLocation({ lat, lng, label, source: 'gps' })
        setLocating(false)
      },
      () => {
        setError('gps_denied')
        setLocating(false)
      },
      { enableHighAccuracy: false, timeout: 10000, maximumAge: 300000 },
    )
  }, [setLocation])

  const value = useMemo(
    () => ({ location, setLocation, useGps, locating, error }),
    [location, setLocation, useGps, locating, error],
  )
  return <LocationContext.Provider value={value}>{children}</LocationContext.Provider>
}

export function useFarmLocation() {
  return useContext(LocationContext)
}
