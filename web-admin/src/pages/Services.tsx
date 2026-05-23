import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, Card, Form, Input, InputNumber, Modal, Select, Space, Switch, Table, Tag, Typography, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { createMedicalService, deleteMedicalService, listMedicalServices, listSpecialties, updateMedicalService, type MedicalService } from '../api/admin'
import type { Specialty } from '../api/types'
import TableCard from '../ui/TableCard'
import { confirmDanger } from '../ui/confirm'
import EmptyState from '../ui/EmptyState'
import TableShell from '../ui/TableShell'

type FormValues = {
  specialtyId?: number
  code?: string
  name: string
  description?: string
  durationMinutes: number
  isActive: boolean
}

export default function ServicesPage() {
  const qc = useQueryClient()
  const [form] = Form.useForm<FormValues>()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<MedicalService | null>(null)

  const qSpecs = useQuery({ queryKey: ['admin', 'specialties'], queryFn: listSpecialties })
  const q = useQuery({ queryKey: ['admin', 'services'], queryFn: () => listMedicalServices() })

  const specMap = useMemo(() => {
    const m = new Map<number, Specialty>()
    for (const s of qSpecs.data ?? []) m.set(s.specialtyId, s)
    return m
  }, [qSpecs.data])

  const createMut = useMutation({
    mutationFn: createMedicalService,
    onSuccess: async () => {
      message.success('Tạo dịch vụ thành công')
      setOpen(false)
      setEditing(null)
      form.resetFields()
      await qc.invalidateQueries({ queryKey: ['admin', 'services'] })
    },
    onError: (e: any) => message.error(e?.response?.data?.message ?? e?.message ?? 'Không tạo được dịch vụ'),
  })

  const updateMut = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: any }) => updateMedicalService(id, payload),
    onSuccess: async () => {
      message.success('Cập nhật dịch vụ thành công')
      setOpen(false)
      setEditing(null)
      form.resetFields()
      await qc.invalidateQueries({ queryKey: ['admin', 'services'] })
    },
    onError: (e: any) => message.error(e?.response?.data?.message ?? e?.message ?? 'Không cập nhật được dịch vụ'),
  })

  const deleteMut = useMutation({
    mutationFn: deleteMedicalService,
    onSuccess: async () => {
      message.success('Đã xóa')
      await qc.invalidateQueries({ queryKey: ['admin', 'services'] })
    },
    onError: (e: any) => message.error(e?.response?.data?.message ?? e?.message ?? 'Không xóa được dịch vụ'),
  })

  const columns: ColumnsType<MedicalService> = useMemo(
    () => [
      { title: 'ID', dataIndex: 'id', width: 90 },
      {
        title: 'Chuyên khoa',
        dataIndex: 'specialtyId',
        width: 220,
        render: (sid) => (sid ? specMap.get(sid)?.name ?? `#${sid}` : <Typography.Text type="secondary">—</Typography.Text>),
      },
      { title: 'Mã', dataIndex: 'code', width: 140, render: (v) => (v ? <code>{v}</code> : '—') },
      { title: 'Tên dịch vụ', dataIndex: 'name', width: 280 },
      { title: 'Thời lượng', dataIndex: 'durationMinutes', width: 120, render: (v) => `${v} phút` },
      { title: 'Trạng thái', dataIndex: 'isActive', width: 110, render: (v) => (v ? <Tag color="green">Đang dùng</Tag> : <Tag>Tắt</Tag>) },
      {
        title: 'Thao tác',
        key: 'actions',
        width: 220,
        render: (_, row) => (
          <Space>
            <Button
              onClick={() => {
                setEditing(row)
                setOpen(true)
                form.setFieldsValue({
                  specialtyId: row.specialtyId ?? undefined,
                  code: row.code ?? undefined,
                  name: row.name,
                  description: row.description ?? undefined,
                  durationMinutes: row.durationMinutes,
                  isActive: row.isActive,
                })
              }}
            >
              Sửa
            </Button>
            <Button
              danger
              loading={deleteMut.isPending}
              onClick={() =>
                confirmDanger({
                  title: 'Xóa dịch vụ?',
                  content: `Xóa "${row.name}"?`,
                  okText: 'Xóa',
                  onOk: () => deleteMut.mutateAsync(row.id),
                })
              }
            >
              Xóa
            </Button>
          </Space>
        ),
      },
    ],
    [deleteMut, form, specMap],
  )

  const specOptions = (qSpecs.data ?? []).map((s) => ({ value: s.specialtyId, label: s.name }))

  const submit = async () => {
    const v = await form.validateFields()
    const payload = {
      specialtyId: v.specialtyId ?? null,
      code: v.code?.trim() ? v.code.trim() : null,
      name: v.name.trim(),
      description: v.description?.trim() ? v.description.trim() : null,
      durationMinutes: v.durationMinutes,
      priceCents: 0,
      isActive: v.isActive,
    }
    if (editing) await updateMut.mutateAsync({ id: editing.id, payload })
    else await createMut.mutateAsync(payload)
  }

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Card className="card-soft">
        <Space style={{ width: '100%', justifyContent: 'space-between' }}>
          <div>
            <Typography.Title level={4} style={{ marginTop: 0, marginBottom: 4 }}>
              Dịch vụ khám
            </Typography.Title>
            <Typography.Text type="secondary">Danh mục dịch vụ dùng khi đặt lịch (thời lượng khám, chuyên khoa)</Typography.Text>
          </div>
          <Button
            type="primary"
            onClick={() => {
              setEditing(null)
              form.resetFields()
              form.setFieldsValue({ durationMinutes: 15, isActive: true } as any)
              setOpen(true)
            }}
          >
            Thêm mới
          </Button>
        </Space>
      </Card>

      <TableCard>
        <TableShell loading={q.isLoading}>
          <Table
            rowKey="id"
            loading={false}
            dataSource={q.data ?? []}
            columns={columns}
            pagination={{ pageSize: 10 }}
            scroll={{ x: 1100, y: 'calc(100vh - 480px)' as any }}
            sticky
            locale={{
              emptyText: <EmptyState title="Chưa có dịch vụ" description="Thêm dịch vụ để gán vào quy trình đặt lịch." />,
            }}
          />
        </TableShell>
      </TableCard>

      <Modal
        title={editing ? 'Cập nhật dịch vụ' : 'Thêm dịch vụ'}
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
        width={760}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="specialtyId" label="Chuyên khoa (tuỳ chọn)">
            <Select allowClear options={specOptions} loading={qSpecs.isLoading} placeholder="Chọn chuyên khoa..." />
          </Form.Item>
          <Space size={12} style={{ width: '100%' }} align="start">
            <Form.Item name="code" label="Mã (tuỳ chọn)" style={{ flex: 1 }}>
              <Input />
            </Form.Item>
            <Form.Item name="name" label="Tên dịch vụ" style={{ flex: 2 }} rules={[{ required: true, message: 'Nhập tên dịch vụ' }]}>
              <Input />
            </Form.Item>
          </Space>
          <Form.Item name="description" label="Mô tả (tuỳ chọn)">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Space size={12} style={{ width: '100%' }} align="start">
            <Form.Item name="durationMinutes" label="Thời lượng (phút)" style={{ flex: 1 }} rules={[{ required: true }]}>
              <InputNumber style={{ width: '100%' }} min={5} max={1440} />
            </Form.Item>
            <Form.Item name="isActive" label="Đang sử dụng" valuePropName="checked" style={{ flex: 1 }}>
              <Switch />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </Space>
  )
}
