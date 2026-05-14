import { Layout, Menu, Typography, Button, Breadcrumb, Space, Avatar, Tooltip } from 'antd'
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
  SettingOutlined,
  LogoutOutlined,
} from '@ant-design/icons'

const { Header, Sider, Content } = Layout

const items = [
  { key: '/', icon: <DashboardOutlined />, label: <Link to="/">Dashboard</Link> },
  { key: '/specialties', icon: <MedicineBoxOutlined />, label: <Link to="/specialties">Chuyên khoa</Link> },
  { key: '/doctors', icon: <UserOutlined />, label: <Link to="/doctors">Bác sĩ</Link> },
  { key: '/services', icon: <AppstoreOutlined />, label: <Link to="/services">Dịch vụ</Link> },
  { key: '/rooms', icon: <HomeOutlined />, label: <Link to="/rooms">Phòng khám</Link> },
  { key: '/slots', icon: <ScheduleOutlined />, label: <Link to="/slots">Slot</Link> },
  { key: '/appointments', icon: <CalendarOutlined />, label: <Link to="/appointments">Lịch hẹn</Link> },
  { key: '/patients', icon: <TeamOutlined />, label: <Link to="/patients">Bệnh nhân</Link> },
  { key: '/accounts', icon: <ProfileOutlined />, label: <Link to="/accounts">Tài khoản</Link> },
]

export default function AdminLayout() {
  const loc = useLocation()
  const nav = useNavigate()
  const s = loadAdminSession()
  const pageName =
    items.find((i) => i.key === loc.pathname)?.label?.props?.children ??
    (loc.pathname === '/' ? 'Dashboard' : loc.pathname.replace('/', ''))

  return (
    <Layout className="app-shell">
      <Sider width={272} theme="light" className="admin-sider" style={{ borderRight: '1px solid rgba(15,23,42,0.06)' }}>
        <div className="sider-brand">
          <div className="brand-chip">
            <div className="brand-logo">BC</div>
            <div className="page-title">
              <Typography.Text strong style={{ fontSize: 14 }}>
                BookingCare Clinic
              </Typography.Text>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                {s?.fullName ? `${s.fullName} (${(s.role ?? '').toUpperCase()})` : 'Admin Dashboard'}
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
                  { title: 'Admin' },
                  { title: <span style={{ textTransform: 'capitalize' }}>{pageName}</span> },
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

              <Tooltip title="Cài đặt (sắp có)">
                <Button icon={<SettingOutlined />} />
              </Tooltip>

              <Button
                type="primary"
                icon={<LogoutOutlined />}
                onClick={() => {
                  clearAdminSession()
                  nav('/login')
                }}
              >
                Logout
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

