import { Route, Routes } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import Navbar from './components/Navbar.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import Home from './pages/Home.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import EquipmentList from './pages/EquipmentList.jsx'
import EquipmentDetail from './pages/EquipmentDetail.jsx'
import Specialists from './pages/Specialists.jsx'
import SpecialistDetail from './pages/SpecialistDetail.jsx'
import Requirements from './pages/Requirements.jsx'
import RequirementNew from './pages/RequirementNew.jsx'
import RequirementDetail from './pages/RequirementDetail.jsx'
import MyBookings from './pages/MyBookings.jsx'
import ProviderEquipment from './pages/ProviderEquipment.jsx'
import EquipmentForm from './pages/EquipmentForm.jsx'
import ProviderRequests from './pages/ProviderRequests.jsx'
import Seasonal from './pages/Seasonal.jsx'
import NotFound from './pages/NotFound.jsx'

export default function App() {
  const { t } = useTranslation()
  return (
    <>
      <Navbar />
      <main>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/equipment" element={<EquipmentList />} />
          <Route path="/equipment/:id" element={<EquipmentDetail />} />
          <Route path="/specialists" element={<Specialists />} />
          <Route path="/specialists/:id" element={<SpecialistDetail />} />
          <Route path="/requirements" element={<Requirements />} />
          <Route path="/requirements/:id" element={<RequirementDetail />} />
          <Route path="/seasonal" element={<Seasonal />} />
          <Route
            path="/requirements/new"
            element={<ProtectedRoute roles={['SEEKER']}><RequirementNew /></ProtectedRoute>}
          />
          <Route
            path="/bookings"
            element={<ProtectedRoute roles={['SEEKER', 'PROVIDER', 'SPECIALIST']}><MyBookings /></ProtectedRoute>}
          />
          <Route
            path="/provider/equipment"
            element={<ProtectedRoute roles={['PROVIDER']}><ProviderEquipment /></ProtectedRoute>}
          />
          <Route
            path="/provider/equipment/new"
            element={<ProtectedRoute roles={['PROVIDER']}><EquipmentForm /></ProtectedRoute>}
          />
          <Route
            path="/provider/equipment/:id/edit"
            element={<ProtectedRoute roles={['PROVIDER']}><EquipmentForm /></ProtectedRoute>}
          />
          <Route
            path="/provider/bookings"
            element={<ProtectedRoute roles={['PROVIDER', 'SPECIALIST']}><ProviderRequests /></ProtectedRoute>}
          />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </main>
      <footer className="footer">
        KrishiConnect · {t('footer.tagline')} ·{' '}
        <a href="/swagger-ui.html" target="_blank" rel="noreferrer">API docs</a>
      </footer>
    </>
  )
}
