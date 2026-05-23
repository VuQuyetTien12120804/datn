import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, Card, Form, Input, Modal, Select, Space, Table, Tag, Typography, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import {
  blockAccount,
  createAccount,
  listAccounts,
  listRoles,
  unblockAccount,
  updateAccount,
  type Account,
  type Role,
  type UpsertAccountRequest,
} from '../api/admin'
import TableCard from '../ui/TableCard'
import { confirmDanger } from '../ui/confirm'
import EmptyState from '../ui/EmptyState'
import TableShell from '../ui/TableShell'

type FormValues = {
  roleId: number
  email?: string
  phone?: string
  fullName: string
  status?: string
  password?: string
}

const statusOptions = [
  { value: 'active', label: 'Hoạt động' },
  { value: 'inactive', label: 'Ngưng' },
  { value: 'blocked', label: 'Đã khóa' },
]

function accountStatusTag(v?: string | null) {
  const s = v ?? 'active'
  const color = s === 'blocked' ? 'red' : s === 'inactive' ? 'orange' : 'green'
  const label = statusOptions.find((x) => x.value === s)?.label ?? s
  return <Tag color={color}>{label}</Tag>
}

export default function AccountsPage() {
  const qc = useQueryClient()
  const [form] = Form.useForm<FormValues>()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Account | null>(null)

  const qRoles = useQuery({ queryKey: ['admin', 'roles'], queryFn: listRoles })
  const qAccounts = useQuery({ queryKey: ['admin', 'accounts'], queryFn: listAccounts })

  const roleOptions = (qRoles.data ?? []).map((r: Role) => ({ value: r.id, label: `${r.name} (${r.code})` }))

  const createMut = useMutation({
    mutationFn: createAccount,
    onSuccess: async () => {
      message.success('Tạo tài khoản thành công')
      setOpen(false)
      setEditing(null)
      form.resetFields()
      await qc.invalidateQueries({ queryKey: ['admin', 'accounts'] })
    },
    onError: (e: any) => message.error(e?.response?.data?.message ?? e?.message ?? 'Không tạo được tài khoản'),
  })

  const updateMut = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: UpsertAccountRequest }) => updateAccount(id, payload),
    onSuccess: async () => {
      message.success('Cập nhật tài khoản thành công')
      setOpen(false)
      setEditing(null)
      form.resetFields()
      await qc.invalidateQueries({ queryKey: ['admin', 'accounts'] })
    },
    onError: (e: any) => message.error(e?.response?.data?.message ?? e?.message ?? 'Không cập nhật được tài khoản'),
  })

  const blockMut = useMutation({
    mutationFn: blockAccount,
    onSuccess: async () => {
      message.success('Đã khóa')
      await qc.invalidateQueries({ queryKey: ['admin', 'accounts'] })
    },
    onError: (e: any) => message.error(e?.response?.data?.message ?? e?.message ?? 'Không khóa được'),
  })

  const unblockMut = useMutation({
    mutationFn: unblockAccount,
    onSuccess: async () => {
      message.success('Đã mở khóa')
      await qc.invalidateQueries({ queryKey: ['admin', 'accounts'] })
    },
    onError: (e: any) => message.error(e?.response?.data?.message ?? e?.message ?? 'Không mở khóa được'),
  })

  const columns: ColumnsType<Account> = useMemo(
    () => [
      { title: 'ID', dataIndex: 'userId', width: 90 },
      {
        title: 'Vai trò',
        dataIndex: 'role',
        width: 200,
        render: (r: Role | null | undefined) =>
          r ? (
            <Space>
              <Tag color="blue">{r.code}</Tag>
              <span>{r.name}</span>
            </Space>
          ) : (
            <Typography.Text type="secondary">—</Typography.Text>
          ),
      },
      { title: 'Họ tên', dataIndex: 'fullName', width: 220 },
      { title: 'Email', dataIndex: 'email', width: 240, render: (v) => v ?? '—' },
      { title: 'SĐT', dataIndex: 'phone', width: 140, render: (v) => v ?? '—' },
      {
        title: 'Trạng thái',
        dataIndex: 'status',
        width: 130,
        render: (v: string | null | undefined) => accountStatusTag(v),
      },
      {
        title: 'Thao tác',
        key: 'actions',
        width: 280,
        render: (_, row) => {
          const s = row.status ?? 'active'
          return (
            <Space>
              <Button
                onClick={() => {
                  setEditing(row)
                  setOpen(true)
                  form.setFieldsValue({
                    roleId: row.role?.id ?? 1,
                    email: row.email ?? undefined,
                    phone: row.phone ?? undefined,
                    fullName: row.fullName,
                    status: row.status ?? 'active',
                    password: undefined,
                  })
                }}
              >
                Sửa
              </Button>
              {s === 'blocked' ? (
                <Button
                  loading={unblockMut.isPending}
                  onClick={() => {
                    unblockMut.mutate(row.userId)
                  }}
                >
                  Mở khóa
                </Button>
              ) : (
                <Button
                  danger
                  loading={blockMut.isPending}
                  onClick={() => {
                    confirmDanger({
                      title: 'Khóa tài khoản?',
                      content: `Khóa tài khoản #${row.userId} (${row.fullName})`,
                      okText: 'Khóa',
                      onOk: async () => {
                        await blockMut.mutateAsync(row.userId)
                      },
                    })
                  }}
                >
                  Khóa
                </Button>
              )}
            </Space>
          )
        },
      },
    ],
    [blockMut, unblockMut, form],
  )

  const submit = async () => {
    const v = await form.validateFields()
    const payload: UpsertAccountRequest = {
      roleId: v.roleId,
      email: v.email?.trim() ? v.email.trim() : undefined,
      phone: v.phone?.trim() ? v.phone.trim() : undefined,
      fullName: v.fullName.trim(),
      status: v.status ?? 'active',
      password: v.password?.trim() ? v.password.trim() : undefined,
    }
    if (editing) {
      await updateMut.mutateAsync({ id: editing.userId, payload })
    } else {
      await createMut.mutateAsync(payload)
    }
  }

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Card>
        <Space style={{ width: '100%', justifyContent: 'space-between' }}>
          <div>
            <Typography.Title level={4} style={{ marginTop: 0, marginBottom: 4 }}>
              Tài khoản
            </Typography.Title>
            <Typography.Text type="secondary">Quản lý tài khoản đăng nhập (tạo, sửa, khóa/mở khóa)</Typography.Text>
          </div>

          <Button
            type="primary"
            onClick={() => {
              setEditing(null)
              form.resetFields()
              form.setFieldsValue({ status: 'active' } as any)
              setOpen(true)
            }}
          >
            Thêm mới
          </Button>
        </Space>
      </Card>

      <TableCard>
        <TableShell loading={qAccounts.isLoading}>
          <Table<Account>
            rowKey="userId"
            loading={false}
            dataSource={qAccounts.data ?? []}
            columns={columns}
            scroll={{ x: 1200, y: 'calc(100vh - 480px)' as any }}
            sticky
            pagination={{ pageSize: 10 }}
            locale={{
              emptyText: <EmptyState title="Chưa có tài khoản" description="Tạo tài khoản ADMIN để đăng nhập quản trị, hoặc DOCTOR để đăng nhập app bác sĩ." />,
            }}
          />
        </TableShell>
      </TableCard>

      <Modal
        title={editing ? 'Cập nhật tài khoản' : 'Thêm tài khoản'}
        open={open}
        onCancel={() => {
          setOpen(false)
          setEditing(null)
          form.resetFields()
        }}
        okText={editing ? 'Lưu' : 'Tạo'}
        cancelText="Hủy"
        confirmLoading={createMut.isPending || updateMut.isPending}
        onOk={submit}
        destroyOnClose
        width={720}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="roleId" label="Role" rules={[{ required: true, message: 'Chọn role' }]}>
            <Select options={roleOptions} loading={qRoles.isLoading} placeholder="Chọn role..." />
          </Form.Item>

          <Space size={12} style={{ width: '100%' }} align="start">
            <Form.Item name="email" label="Email" style={{ flex: 1 }}>
              <Input placeholder="user@email.com" />
            </Form.Item>
            <Form.Item name="phone" label="SĐT" style={{ flex: 1 }}>
              <Input placeholder="090..." />
            </Form.Item>
          </Space>

          <Form.Item name="fullName" label="Họ tên" rules={[{ required: true, message: 'Nhập họ tên' }]}>
            <Input />
          </Form.Item>

          <Space size={12} style={{ width: '100%' }} align="start">
            <Form.Item name="status" label="Trạng thái" style={{ flex: 1 }}>
              <Select options={statusOptions} />
            </Form.Item>
            <Form.Item
              name="password"
              label={editing ? 'Password mới (tuỳ chọn)' : 'Password (tuỳ chọn)'}
              style={{ flex: 1 }}
            >
              <Input.Password placeholder="Để trống nếu không đổi" />
            </Form.Item>
          </Space>

          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            Lưu ý: Password chỉ set khi bạn nhập vào ô password.
          </Typography.Paragraph>
        </Form>
      </Modal>
    </Space>
  )
}

