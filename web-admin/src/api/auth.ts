import { http } from './http'
import type { ApiEnvelope } from './types'

export type AuthResponse = {
  userId: string
  accessToken: string
  refreshToken: string
  role: string
  fullName: string
  email: string
}

export async function adminLogin(email: string, password: string): Promise<AuthResponse> {
  const res = await http.post<ApiEnvelope<AuthResponse>>('/api/v1/auth/login', { email, password })
  return res.data.data
}

