import { Route, Routes } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import Navbar from './components/Navbar.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import EquipmentList from './pages/EquipmentList.jsx'
import EquipmentDetail from './pages/EquipmentDetail.jsx'

export default function App() {
  const { t } = useTranslation()
  return (
    <>
      <Navbar />
      <main>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/equipment" element={<EquipmentList />} />
          <Route path="/equipment/:id" element={<EquipmentDetail />} />
        </Routes>
      </main>
      <footer className="footer">
        KrishiConnect · {t('footer.tagline')} ·{' '}
        <a href="/swagger-ui.html" target="_blank" rel="noreferrer">API docs</a>
      </footer>
    </>
  )
}
