import { Tag } from 'antd'
import type { AppointmentStatus } from '../api/admin'

export const APPOINTMENT_STATUS_OPTIONS: { value: AppointmentStatus; label: string; color?: string }[] = [
  { value: 'PENDING', label: 'Chờ duyệt', color: 'default' },
  { value: 'CONFIRMED', label: 'Đã xác nhận', color: 'blue' },
  { value: 'CHECKED_IN', label: 'Đã check-in', color: 'cyan' },
  { value: 'COMPLETED', label: 'Hoàn tất', color: 'green' },
  { value: 'CANCELLED', label: 'Đã hủy', color: 'red' },
  { value: 'NO_SHOW', label: 'Không đến', color: 'orange' },
]

export function normalizeAppointmentStatus(raw?: string | null): AppointmentStatus {
  const s = (raw ?? 'PENDING').toUpperCase()
  return (APPOINTMENT_STATUS_OPTIONS.some((x) => x.value === s) ? s : 'PENDING') as AppointmentStatus
}

export function appointmentStatusLabel(raw?: string | null): string {
  const key = normalizeAppointmentStatus(raw)
  return APPOINTMENT_STATUS_OPTIONS.find((x) => x.value === key)?.label ?? key
}

export function appointmentStatusTag(raw?: string | null) {
  const key = normalizeAppointmentStatus(raw)
  const meta = APPOINTMENT_STATUS_OPTIONS.find((x) => x.value === key)
  return <Tag color={meta?.color}>{meta?.label ?? key}</Tag>
}
