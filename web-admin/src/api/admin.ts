import { http } from './http'
import type { ApiEnvelope, DoctorAdmin, PageResponse, Specialty } from './types'

export type UpsertSpecialtyRequest = {
  code?: string | null
  name: string
  description?: string | null
}

export async function listSpecialties(): Promise<Specialty[]> {
  const res = await http.get<ApiEnvelope<Specialty[]>>('/api/v1/admin/specialties')
  return res.data.data ?? []
}

export async function createSpecialty(payload: UpsertSpecialtyRequest): Promise<Specialty> {
  const res = await http.post<ApiEnvelope<Specialty>>('/api/v1/admin/specialties', payload)
  return res.data.data
}

export async function updateSpecialty(id: number, payload: UpsertSpecialtyRequest): Promise<Specialty> {
  const res = await http.put<ApiEnvelope<Specialty>>(`/api/v1/admin/specialties/${id}`, payload)
  return res.data.data
}

export async function deleteSpecialty(id: number): Promise<void> {
  await http.delete(`/api/v1/admin/specialties/${id}`)
}

export type UpsertDoctorRequest = {
  accountId?: number | null
  fullName: string
  gender?: string | null
  dob?: string | null // yyyy-mm-dd
  phone?: string | null
  email?: string | null
  licenseNo?: string | null
  bio?: string | null
  avatarUrl?: string | null
  rating?: number | null
  visitsCount?: number | null
  roomLocation?: string | null
  scheduleText?: string | null
  education?: string[] | null
  certificates?: string[] | null
  specialtyIds?: number[] | null
}

export async function listDoctors(): Promise<DoctorAdmin[]> {
  const res = await http.get<ApiEnvelope<DoctorAdmin[]>>('/api/v1/admin/doctors')
  return res.data.data ?? []
}

export async function createDoctor(payload: UpsertDoctorRequest): Promise<DoctorAdmin> {
  const res = await http.post<ApiEnvelope<DoctorAdmin>>('/api/v1/admin/doctors', payload)
  return res.data.data
}

export async function updateDoctor(id: number, payload: UpsertDoctorRequest): Promise<DoctorAdmin> {
  const res = await http.put<ApiEnvelope<DoctorAdmin>>(`/api/v1/admin/doctors/${id}`, payload)
  return res.data.data
}

export async function deleteDoctor(id: number): Promise<void> {
  await http.delete(`/api/v1/admin/doctors/${id}`)
}

export type GenerateSlotsRequest = {
  doctorId: number
  roomId?: number | null
  date: string // yyyy-mm-dd
  startTime: string // HH:mm
  endTime: string // HH:mm
  slotMinutes: number
  capacity: number
}

export type DoctorSlot = {
  slotId: number
  doctorId: number
  roomId?: number | null
  startsAt: string
  endsAt: string
  capacity: number
  bookedCount: number
  isActive: boolean
  createdAt?: string | null
}

export async function generateSlots(payload: GenerateSlotsRequest): Promise<DoctorSlot[]> {
  const res = await http.post<ApiEnvelope<DoctorSlot[]>>('/api/v1/admin/slots/generate', payload)
  return res.data.data ?? []
}

export async function listSlots(doctorId: number, date: string): Promise<DoctorSlot[]> {
  const res = await http.get<ApiEnvelope<DoctorSlot[]>>('/api/v1/admin/slots', {
    params: { doctorId, date },
  })
  return res.data.data ?? []
}

export async function deactivateSlot(slotId: number): Promise<DoctorSlot> {
  const res = await http.patch<ApiEnvelope<DoctorSlot>>(`/api/v1/admin/slots/${slotId}/deactivate`, {})
  return res.data.data
}

export type AppointmentStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'CHECKED_IN'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'NO_SHOW'

export type Appointment = {
  appointmentId: number
  patientId: number
  doctorId: number
  serviceId?: number | null
  slotId?: number | null
  roomId?: number | null
  startsAt: string
  endsAt: string
  status: AppointmentStatus
  reason?: string | null
  note?: string | null
  cancelReason?: string | null
  cancelledAt?: string | null
  confirmedAt?: string | null
  checkedInAt?: string | null
  completedAt?: string | null
  createdByAccountId?: number | null
  createdAt?: string | null
  updatedAt?: string | null
}

export async function listAppointments(params?: {
  doctorId?: number
  patientId?: number
  status?: AppointmentStatus
}): Promise<Appointment[]> {
  const res = await http.get<ApiEnvelope<Appointment[]>>('/api/v1/admin/appointments', { params })
  return res.data.data ?? []
}

export type AppointmentAdminRow = {
  appointmentId: number
  patientId?: number | null
  patientName?: string | null
  doctorId?: number | null
  doctorName?: string | null
  serviceId?: number | null
  serviceName?: string | null
  roomId?: number | null
  roomName?: string | null
  slotId?: number | null
  startsAt?: string | null
  endsAt?: string | null
  status?: string | null
}

export type AppointmentAdminDetail = {
  appointmentId: number
  status?: string | null
  startsAt?: string | null
  endsAt?: string | null
  doctorId?: number | null
  doctorName?: string | null
  patientId?: number | null
  patientName?: string | null
  patientPhone?: string | null
  patientEmail?: string | null
  patientGender?: string | null
  patientDob?: string | null
  patientAddress?: string | null
  slotId?: number | null
  roomId?: number | null
  roomName?: string | null
  serviceId?: number | null
  serviceName?: string | null
  serviceDurationMinutes?: number | null
  reason?: string | null
  note?: string | null
  cancelReason?: string | null
}

export async function searchAppointments(params: {
  doctorId?: number
  patientId?: number
  status?: AppointmentStatus
  q?: string
  fromDate?: string
  toDate?: string
  page?: number
  size?: number
}): Promise<PageResponse<AppointmentAdminRow>> {
  const res = await http.get<ApiEnvelope<PageResponse<AppointmentAdminRow>>>('/api/v1/admin/appointments/search', {
    params,
  })
  return res.data.data
}

export async function searchAllAppointments(
  params: Omit<Parameters<typeof searchAppointments>[0], 'page' | 'size'>,
): Promise<AppointmentAdminRow[]> {
  const rows: AppointmentAdminRow[] = []
  let page = 0
  const size = 100
  while (true) {
    const res = await searchAppointments({ ...params, page, size })
    rows.push(...(res.content ?? []))
    if (res.last || (res.content ?? []).length === 0) break
    page += 1
  }
  return rows
}

export async function getAppointmentDetail(id: number): Promise<AppointmentAdminDetail> {
  const res = await http.get<ApiEnvelope<AppointmentAdminDetail>>(`/api/v1/admin/appointments/${id}/detail`)
  return res.data.data
}

export async function rescheduleAppointment(id: number, slotId: number): Promise<Appointment> {
  const res = await http.patch<ApiEnvelope<Appointment>>(`/api/v1/admin/appointments/${id}/reschedule`, null, {
    params: { slotId },
  })
  return res.data.data
}

export async function updateAppointmentStatus(
  id: number,
  payload: { status: AppointmentStatus; note?: string; cancelReason?: string },
): Promise<Appointment> {
  const res = await http.patch<ApiEnvelope<Appointment>>(`/api/v1/admin/appointments/${id}/status`, payload)
  return res.data.data
}

export type Role = {
  id: number
  code: string
  name: string
  description?: string | null
}

export type Account = {
  userId: number
  role?: Role | null
  email?: string | null
  phone?: string | null
  fullName: string
  status?: string | null
  isEmailVerified?: boolean | null
  lastLoginAt?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export type UpsertAccountRequest = {
  roleId: number
  email?: string
  phone?: string
  fullName: string
  status?: string
  password?: string
}

export async function listAccounts(): Promise<Account[]> {
  const res = await http.get<ApiEnvelope<Account[]>>('/api/v1/admin/accounts')
  return res.data.data ?? []
}

export async function listRoles(): Promise<Role[]> {
  const res = await http.get<ApiEnvelope<Role[]>>('/api/v1/admin/accounts/roles')
  return res.data.data ?? []
}

export async function createAccount(payload: UpsertAccountRequest): Promise<Account> {
  const res = await http.post<ApiEnvelope<Account>>('/api/v1/admin/accounts', payload)
  return res.data.data
}

export async function updateAccount(id: number, payload: UpsertAccountRequest): Promise<Account> {
  const res = await http.put<ApiEnvelope<Account>>(`/api/v1/admin/accounts/${id}`, payload)
  return res.data.data
}

export async function blockAccount(id: number): Promise<Account> {
  const res = await http.patch<ApiEnvelope<Account>>(`/api/v1/admin/accounts/${id}/block`, {})
  return res.data.data
}

export async function unblockAccount(id: number): Promise<Account> {
  const res = await http.patch<ApiEnvelope<Account>>(`/api/v1/admin/accounts/${id}/unblock`, {})
  return res.data.data
}

export type Room = {
  id: number
  code?: string | null
  name: string
  floor?: string | null
  note?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export type MedicalService = {
  id: number
  specialtyId?: number | null
  code?: string | null
  name: string
  description?: string | null
  durationMinutes: number
  priceCents: number
  isActive: boolean
  createdAt?: string | null
  updatedAt?: string | null
}

export async function listRooms(): Promise<Room[]> {
  const res = await http.get<ApiEnvelope<Room[]>>('/api/v1/admin/rooms')
  return res.data.data ?? []
}

export async function createRoom(payload: { code?: string; name: string; floor?: string; note?: string }): Promise<Room> {
  const res = await http.post<ApiEnvelope<Room>>('/api/v1/admin/rooms', payload)
  return res.data.data
}

export async function updateRoom(
  id: number,
  payload: { code?: string; name: string; floor?: string; note?: string },
): Promise<Room> {
  const res = await http.put<ApiEnvelope<Room>>(`/api/v1/admin/rooms/${id}`, payload)
  return res.data.data
}

export async function deleteRoom(id: number): Promise<void> {
  await http.delete(`/api/v1/admin/rooms/${id}`)
}

export async function listMedicalServices(params?: { specialtyId?: number }): Promise<MedicalService[]> {
  const res = await http.get<ApiEnvelope<MedicalService[]>>('/api/v1/admin/services', { params })
  return res.data.data ?? []
}

export async function createMedicalService(payload: {
  specialtyId?: number | null
  code?: string | null
  name: string
  description?: string | null
  durationMinutes: number
  priceCents: number
  isActive?: boolean
}): Promise<MedicalService> {
  const res = await http.post<ApiEnvelope<MedicalService>>('/api/v1/admin/services', payload)
  return res.data.data
}

export async function updateMedicalService(
  id: number,
  payload: {
    specialtyId?: number | null
    code?: string | null
    name: string
    description?: string | null
    durationMinutes: number
    priceCents: number
    isActive?: boolean
  },
): Promise<MedicalService> {
  const res = await http.put<ApiEnvelope<MedicalService>>(`/api/v1/admin/services/${id}`, payload)
  return res.data.data
}

export async function deleteMedicalService(id: number): Promise<void> {
  await http.delete(`/api/v1/admin/services/${id}`)
}

export type Patient = {
  id: number
  accountId?: number | null
  fullName: string
  dob?: string | null
  gender?: string | null
  phone?: string | null
  email?: string | null
  address?: string | null
  insuranceNo?: string | null
  emergencyContactName?: string | null
  emergencyContactPhone?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export async function listPatients(): Promise<Patient[]> {
  const res = await http.get<ApiEnvelope<Patient[]>>('/api/v1/admin/patients')
  return res.data.data ?? []
}

export type ReportTimeseriesPoint = {
  period: string
  total: number
  confirmed: number
  completed: number
  cancelled: number
  noShow: number
}

export type TopItemPoint = {
  id: number
  name: string
  total: number
  completed: number
  cancelled: number
  noShow: number
}

export async function reportTimeseries(params: {
  granularity: 'day' | 'week' | 'month'
  from?: string
  to?: string
}): Promise<ReportTimeseriesPoint[]> {
  const res = await http.get<ApiEnvelope<ReportTimeseriesPoint[]>>('/api/v1/admin/reports/appointments/timeseries', {
    params,
  })
  return res.data.data ?? []
}

export async function reportTopDoctors(params: { from?: string; to?: string; limit?: number }): Promise<TopItemPoint[]> {
  const res = await http.get<ApiEnvelope<TopItemPoint[]>>('/api/v1/admin/reports/appointments/top-doctors', { params })
  return res.data.data ?? []
}

export async function reportTopSpecialties(params: { from?: string; to?: string; limit?: number }): Promise<TopItemPoint[]> {
  const res = await http.get<ApiEnvelope<TopItemPoint[]>>('/api/v1/admin/reports/appointments/top-specialties', { params })
  return res.data.data ?? []
}

export async function reportAppointmentSummary(params: { from?: string; to?: string }): Promise<{
  total: number
  completed: number
  cancelled: number
  noShow: number
  pending: number
  confirmed: number
}> {
  const res = await http.get<ApiEnvelope<any>>('/api/v1/admin/reports/appointments/summary', { params })
  return res.data.data
}

export type ChatThread = {
  threadId: number
  threadKey: string
  threadType: string
  title: string
  subtitle: string
  lastMessage: string
  updatedAtMs: number
  unreadCount: number
  locked: boolean
  canSend: boolean
}

export type ChatMessage = {
  messageId: number
  senderRole: string
  content: string
  fromMe: boolean
  createdAtMs: number
}

export async function listSupportThreads(): Promise<ChatThread[]> {
  const res = await http.get<ApiEnvelope<ChatThread[]>>('/api/v1/admin/messages/threads')
  return res.data.data ?? []
}

export async function listSupportMessages(threadKey: string): Promise<ChatMessage[]> {
  const encoded = encodeURIComponent(threadKey)
  const res = await http.get<ApiEnvelope<ChatMessage[]>>(`/api/v1/admin/messages/threads/${encoded}/messages`)
  return res.data.data ?? []
}

export async function sendSupportMessage(threadKey: string, content: string): Promise<ChatMessage> {
  const encoded = encodeURIComponent(threadKey)
  const res = await http.post<ApiEnvelope<ChatMessage>>(`/api/v1/admin/messages/threads/${encoded}/messages`, { content })
  return res.data.data
}

export async function markSupportThreadRead(threadKey: string): Promise<void> {
  const encoded = encodeURIComponent(threadKey)
  await http.post(`/api/v1/admin/messages/threads/${encoded}/read`)
}

