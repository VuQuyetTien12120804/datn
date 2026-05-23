import axios from 'axios'
import { loadAdminSession } from '../auth/storage'
import { clearAdminSession } from '../auth/storage'

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8085'

export const http = axios.create({
  baseURL,
  timeout: 30_000,
})

http.interceptors.request.use((config) => {
  const s = loadAdminSession()
  if (s?.accessToken) {
    config.headers = config.headers ?? {}
    config.headers['Authorization'] = `Bearer ${s.accessToken}`
  }
  return config
})

http.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err?.response?.status
    if (status === 401) {
      // token hết hạn -> logout về login (403 = lỗi quyền/nghiệp vụ, giữ phiên)
      clearAdminSession()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(err)
  },
)

