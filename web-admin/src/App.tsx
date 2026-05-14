import { Navigate, Route, Routes } from 'react-router-dom'
import AdminLayout from './layout/AdminLayout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import SpecialtiesPage from './pages/Specialties'
import DoctorsPage from './pages/Doctors'
import SlotsPage from './pages/Slots'
import AppointmentsPage from './pages/Appointments'
import AccountsPage from './pages/Accounts'
import ServicesPage from './pages/Services'
import RoomsPage from './pages/Rooms'
import PatientsPage from './pages/Patients'
import { loadAdminSession } from './auth/storage'

function RequireAdmin({ children }: { children: React.ReactNode }) {
  const ok = !!loadAdminSession()
  return ok ? <>{children}</> : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />

      <Route
        path="/"
        element={
          <RequireAdmin>
            <AdminLayout />
          </RequireAdmin>
        }
      >
        <Route index element={<Dashboard />} />
        <Route path="specialties" element={<SpecialtiesPage />} />
        <Route path="doctors" element={<DoctorsPage />} />
        <Route path="services" element={<ServicesPage />} />
        <Route path="rooms" element={<RoomsPage />} />
        <Route path="slots" element={<SlotsPage />} />
        <Route path="appointments" element={<AppointmentsPage />} />
        <Route path="patients" element={<PatientsPage />} />
        <Route path="accounts" element={<AccountsPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
