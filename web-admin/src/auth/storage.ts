export type AdminSession = {
  accessToken: string
  role?: string
  email?: string
  fullName?: string
}

const KEY = 'admin_session_v1'

export function saveAdminSession(s: AdminSession) {
  localStorage.setItem(KEY, JSON.stringify(s))
}

export function loadAdminSession(): AdminSession | null {
  const raw = localStorage.getItem(KEY)
  if (!raw) return null
  try {
    const v = JSON.parse(raw) as Partial<AdminSession>
    if (!v.accessToken) return null
    return { accessToken: v.accessToken, role: v.role, email: v.email, fullName: v.fullName }
  } catch {
    return null
  }
}

export function clearAdminSession() {
  localStorage.removeItem(KEY)
}

