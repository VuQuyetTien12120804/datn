export const PAGE_TITLES: Record<string, string> = {
  '/': 'Tổng quan',
  '/specialties': 'Chuyên khoa',
  '/doctors': 'Bác sĩ',
  '/services': 'Dịch vụ',
  '/rooms': 'Phòng khám',
  '/slots': 'Khung giờ',
  '/appointments': 'Lịch hẹn',
  '/patients': 'Bệnh nhân',
  '/messages': 'Tin nhắn CSKH',
  '/accounts': 'Tài khoản',
}

export function pageTitle(pathname: string): string {
  return PAGE_TITLES[pathname] ?? 'Quản trị'
}
