import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import i18n from '../i18n'
import { api, tokenStore } from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(() => Boolean(tokenStore.get()))

  const applyUser = useCallback((u) => {
    setUser(u)
    // the language saved on the account wins over the one in this browser
    if (u?.preferredLang && u.preferredLang !== i18n.language) {
      i18n.changeLanguage(u.preferredLang)
    }
  }, [])

  useEffect(() => {
    if (!tokenStore.get()) return
    api.me()
      .then(applyUser)
      .catch(() => tokenStore.clear())
      .finally(() => setLoading(false))
  }, [applyUser])

  useEffect(() => {
    const onLogout = () => setUser(null)
    window.addEventListener('krishi:logout', onLogout)
    return () => window.removeEventListener('krishi:logout', onLogout)
  }, [])

  const login = useCallback(async (identifier, password) => {
    const res = await api.login(identifier, password)
    tokenStore.set(res.token)
    applyUser(res.user)
    return res.user
  }, [applyUser])

  const register = useCallback(async (data) => {
    const res = await api.register(data)
    tokenStore.set(res.token)
    applyUser(res.user)
    return res.user
  }, [applyUser])

  const logout = useCallback(() => {
    tokenStore.clear()
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ user, loading, login, register, logout, setUser }),
    [user, loading, login, register, logout],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  return useContext(AuthContext)
}
