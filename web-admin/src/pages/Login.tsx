import { Button, Card, Form, Input, Typography, message } from 'antd'
import { saveAdminSession } from '../auth/storage'
import { useNavigate } from 'react-router-dom'
import { adminLogin } from '../api/auth'

export default function Login() {
  const nav = useNavigate()

  return (
    <div
      style={{
        minHeight: '100%',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 16,
      }}
    >
      <Card style={{ width: 440 }} bordered={false} className="card-soft">
        <Typography.Title level={3} style={{ marginTop: 0 }}>
          Đăng nhập quản trị
        </Typography.Title>
        <Typography.Paragraph type="secondary" style={{ marginTop: -8 }}>
          ClinicBooking — Phòng khám đa khoa Quyết Tiến
        </Typography.Paragraph>

        <Form
          layout="vertical"
          onFinish={async (v) => {
            try {
              const r = await adminLogin(v.email, v.password)
              if (!r?.accessToken) {
                message.error('Đăng nhập thất bại')
                return
              }
              // allow ADMIN only (đồ án không dùng STAFF)
              const role = (r.role ?? '').toLowerCase()
              if (role !== 'admin') {
                message.error('Tài khoản không có quyền admin')
                return
              }
              saveAdminSession({ accessToken: r.accessToken, role: r.role, email: r.email, fullName: r.fullName })
              message.success('Đăng nhập thành công')
              nav('/')
            } catch (e: any) {
              message.error(e?.response?.data?.message ?? e?.message ?? 'Đăng nhập thất bại')
            }
          }}
        >
          <Form.Item
            name="email"
            label="Email"
            rules={[{ required: true, message: 'Nhập email' }]}
          >
            <Input autoFocus />
          </Form.Item>
          <Form.Item
            name="password"
            label="Mật khẩu"
            rules={[{ required: true, message: 'Nhập mật khẩu' }]}
          >
            <Input.Password />
          </Form.Item>

          <Button type="primary" htmlType="submit" block>
            Đăng nhập
          </Button>
        </Form>
      </Card>
    </div>
  )
}

