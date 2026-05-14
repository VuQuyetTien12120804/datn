import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, Card, Form, Input, Modal, Space, Table, Typography, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { createRoom, deleteRoom, listRooms, updateRoom, type Room } from '../api/admin'
import TableCard from '../ui/TableCard'
import { confirmDanger } from '../ui/confirm'
import EmptyState from '../ui/EmptyState'
import TableShell from '../ui/TableShell'

type FormValues = { code?: string; name: string; floor?: string; note?: string }

export default function RoomsPage() {
  const qc = useQueryClient()
  const [form] = Form.useForm<FormValues>()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Room | null>(null)

  const q = useQuery({ queryKey: ['admin', 'rooms'], queryFn: listRooms })

  const createMut = useMutation({
    mutationFn: createRoom,
    onSuccess: async () => {
      message.success('Tạo phòng thành công')
      setOpen(false)
      setEditing(null)
      form.resetFields()
      await qc.invalidateQueries({ queryKey: ['admin', 'rooms'] })
    },
    onError: (e: any) => message.error(e?.response?.data?.message ?? e?.message ?? 'Không tạo được phòng'),
  })

  const updateMut = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: FormValues }) => updateRoom(id, payload),
    onSuccess: async () => {
      message.success('Cập nhật phòng thành công')
      setOpen(false)
      setEditing(null)
      form.resetFields()
      await qc.invalidateQueries({ queryKey: ['admin', 'rooms'] })
    },
    onError: (e: any) => message.error(e?.response?.data?.message ?? e?.message ?? 'Không cập nhật được phòng'),
  })

  const deleteMut = useMutation({
    mutationFn: deleteRoom,
    onSuccess: async () => {
      message.success('Đã xóa')
      await qc.invalidateQueries({ queryKey: ['admin', 'rooms'] })
    },
    onError: (e: any) => message.error(e?.response?.data?.message ?? e?.message ?? 'Không xóa được phòng'),
  })

  const columns: ColumnsType<Room> = useMemo(
    () => [
      { title: 'ID', dataIndex: 'id', width: 90 },
      { title: 'Mã', dataIndex: 'code', width: 140, render: (v) => (v ? <code>{v}</code> : '—') },
      { title: 'Tên phòng', dataIndex: 'name', width: 260 },
      { title: 'Tầng', dataIndex: 'floor', width: 120, render: (v) => v ?? '—' },
      { title: 'Ghi chú', dataIndex: 'note', render: (v) => (v ? v : <Typography.Text type="secondary">—</Typography.Text>) },
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
                  code: row.code ?? undefined,
                  name: row.name,
                  floor: row.floor ?? undefined,
                  note: row.note ?? undefined,
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
                  title: 'Xóa phòng?',
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
    [deleteMut, form],
  )

  const submit = async () => {
    const v = await form.validateFields()
    const payload = {
      code: v.code?.trim() ? v.code.trim() : undefined,
      name: v.name.trim(),
      floor: v.floor?.trim() ? v.floor.trim() : undefined,
      note: v.note?.trim() ? v.note.trim() : undefined,
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
              Rooms (Phòng khám)
            </Typography.Title>
            <Typography.Text type="secondary">Quản lý phòng khám bệnh để gán vào slot</Typography.Text>
          </div>
          <Button
            type="primary"
            onClick={() => {
              setEditing(null)
              form.resetFields()
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
              emptyText: <EmptyState title="Chưa có phòng" description="Tạo phòng để gán vào slot (roomId)." />,
            }}
          />
        </TableShell>
      </TableCard>

      <Modal
        title={editing ? 'Cập nhật phòng' : 'Thêm phòng'}
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
        width={680}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="code" label="Mã (tuỳ chọn)">
            <Input placeholder="VD: P101" />
          </Form.Item>
          <Form.Item name="name" label="Tên phòng" rules={[{ required: true, message: 'Nhập tên phòng' }]}>
            <Input placeholder="VD: Phòng khám 1" />
          </Form.Item>
          <Form.Item name="floor" label="Tầng (tuỳ chọn)">
            <Input placeholder="VD: Tầng 1" />
          </Form.Item>
          <Form.Item name="note" label="Ghi chú (tuỳ chọn)">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  )
}

