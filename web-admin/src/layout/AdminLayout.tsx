import { Layout, Menu, Typography, Button, Breadcrumb, Space, Avatar } from 'antd'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { clearAdminSession, loadAdminSession } from '../auth/storage'
import {
  DashboardOutlined,
  MedicineBoxOutlined,
  UserOutlined,
  ProfileOutlined,
  AppstoreOutlined,
  HomeOutlined,
  CalendarOutlined,
  ScheduleOutlined,
  TeamOutlined,
  MessageOutlined,
  LogoutOutlined,
} from '@ant-design/icons'

import { pageTitle } from '../ui/pageTitles'

const { Header, Sider, Content } = Layout

const items = [
  { key: '/', icon: <DashboardOutlined />, label: <Link to="/">Tổng quan</Link> },
  { key: '/specialties', icon: <MedicineBoxOutlined />, label: <Link to="/specialties">Chuyên khoa</Link> },
  { key: '/doctors', icon: <UserOutlined />, label: <Link to="/doctors">Bác sĩ</Link> },
  { key: '/services', icon: <AppstoreOutlined />, label: <Link to="/services">Dịch vụ</Link> },
  { key: '/rooms', icon: <HomeOutlined />, label: <Link to="/rooms">Phòng khám</Link> },
  { key: '/slots', icon: <ScheduleOutlined />, label: <Link to="/slots">Khung giờ</Link> },
  { key: '/appointments', icon: <CalendarOutlined />, label: <Link to="/appointments">Lịch hẹn</Link> },
  { key: '/patients', icon: <TeamOutlined />, label: <Link to="/patients">Bệnh nhân</Link> },
  { key: '/messages', icon: <MessageOutlined />, label: <Link to="/messages">Tin nhắn CSKH</Link> },
  { key: '/accounts', icon: <ProfileOutlined />, label: <Link to="/accounts">Tài khoản</Link> },
]

export default function AdminLayout() {
  const loc = useLocation()
  const nav = useNavigate()
  const s = loadAdminSession()
  const pageName = pageTitle(loc.pathname)

  return (
    <Layout className="app-shell">
      <Sider width={272} theme="light" className="admin-sider" style={{ borderRight: '1px solid rgba(15,23,42,0.06)' }}>
        <div className="sider-brand">
          <div className="brand-chip">
            <div className="brand-logo">QT</div>
            <div className="page-title">
              <Typography.Text strong style={{ fontSize: 14 }}>
                ClinicBooking
              </Typography.Text>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                Phòng khám Quyết Tiến · {s?.fullName ?? 'Quản trị viên'}
              </Typography.Text>
            </div>
          </div>
        </div>
        <Menu
          selectedKeys={[loc.pathname]}
          items={items}
          className="admin-menu"
          style={{ paddingBottom: 12 }}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            background: 'rgba(255,255,255,0.9)',
            backdropFilter: 'blur(10px)',
            borderBottom: '1px solid rgba(15,23,42,0.06)',
            padding: '10px 18px',
            height: 'auto',
          }}
        >
          <Space style={{ width: '100%', justifyContent: 'space-between' }} align="center">
            <div>
              <Breadcrumb
                items={[
                  { title: 'Quản trị' },
                  { title: <span>{pageName}</span> },
                ]}
              />
              <Typography.Text strong style={{ fontSize: 16 }}>
                {pageName}
              </Typography.Text>
            </div>
            <Space size={10} align="center">
              <div className="header-user">
                <Avatar size={34} style={{ backgroundColor: '#0B84FF' }}>
                  {(s?.fullName?.trim()?.[0] ?? 'A').toUpperCase()}
                </Avatar>
                <div className="header-user-meta">
                  <Typography.Text strong style={{ fontSize: 13, lineHeight: 1.1 }}>
                    {s?.fullName ?? 'Admin'}
                  </Typography.Text>
                  <Typography.Text type="secondary" style={{ fontSize: 12, lineHeight: 1.1 }}>
                    {(s?.role ?? 'admin').toUpperCase()}
                  </Typography.Text>
                </div>
              </div>

              <Button
                type="primary"
                icon={<LogoutOutlined />}
                onClick={() => {
                  clearAdminSession()
                  nav('/login')
                }}
              >
                Đăng xuất
              </Button>
            </Space>
          </Space>
        </Header>
        <Content style={{ padding: 16 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

