export type ApiEnvelope<T> = {
  success: boolean
  code: number
  message: string
  data: T
}

export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  empty: boolean
  numberOfElements?: number
  sorted?: boolean
}

export type Specialty = {
  specialtyId: number
  code?: string | null
  name: string
  description?: string | null
  createdAt?: string | null
}

export type DoctorAdmin = {
  doctorId: number
  accountId?: number | null
  fullName: string
  gender?: string | null
  dob?: string | null
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
  createdAt?: string | null
  updatedAt?: string | null
  specialtyIds?: number[] | null
}

