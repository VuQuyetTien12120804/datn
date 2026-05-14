import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, Card, Form, Input, InputNumber, Modal, Select, Space, Table, Tag, Typography, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { DoctorAdmin } from '../api/types'
import { deactivateSlot, generateSlots, listDoctors, listSlots, type DoctorSlot } from '../api/admin'
import { confirmDanger } from '../ui/confirm'
import { reHm, reYmd } from '../ui/validators'
import EmptyState from '../ui/EmptyState'
import TableShell from '../ui/TableShell'

type FilterValues = {
  doctorId?: number
  date?: string
}

type GenerateValues = {
  doctorId: number
  date: string
  startTime: string
  endTime: string
  slotMinutes: number
  capacity: number
  roomId?: number
}

function todayYmd() {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}

export default function SlotsPage() {
  const qc = useQueryClient()
  const [filterForm] = Form.useForm<FilterValues>()
  const [genForm] = Form.useForm<GenerateValues>()
  const [genOpen, setGenOpen] = useState(false)

  const qDoctors = useQuery({
    queryKey: ['admin', 'doctors'],
    queryFn: listDoctors,
  })

  const selectedDoctorId = Form.useWatch('doctorId', filterForm)
  const selectedDate = Form.useWatch('date', filterForm)

  const qSlots = useQuery({
    queryKey: ['admin', 'slots', selectedDoctorId, selectedDate],
    enabled: !!selectedDoctorId && !!selectedDate,
    queryFn: () => listSlots(selectedDoctorId!, selectedDate!),
  })

  const genMut = useMutation({
    mutationFn: generateSlots,
    onSuccess: async () => {
      message.success('Tạo slot thành công')
      setGenOpen(false)
      await qc.invalidateQueries({ queryKey: ['admin', 'slots', selectedDoctorId, selectedDate] })
    },
    onError: (e: any) => {
      message.error(e?.response?.data?.message ?? e?.message ?? 'Không tạo được slot')
    },
  })

  const deactMut = useMutation({
    mutationFn: deactivateSlot,
    onSuccess: async () => {
      message.success('Đã khóa slot')
      await qc.invalidateQueries({ queryKey: ['admin', 'slots', selectedDoctorId, selectedDate] })
    },
    onError: (e: any) => {
      message.error(e?.response?.data?.message ?? e?.message ?? 'Không khóa được slot')
    },
  })

  const doctorOptions = (qDoctors.data ?? []).map((d: DoctorAdmin) => ({
    value: d.doctorId,
    label: `${d.fullName} (#${d.doctorId})`,
  }))

  const columns: ColumnsType<DoctorSlot> = useMemo(
    () => [
      { title: 'Slot ID', dataIndex: 'slotId', width: 90 },
      { title: 'Bác sĩ', dataIndex: 'doctorId', width: 90 },
      { title: 'Bắt đầu', dataIndex: 'startsAt', width: 210 },
      { title: 'Kết thúc', dataIndex: 'endsAt', width: 210 },
      {
        title: 'Sức chứa',
        key: 'cap',
        width: 130,
        render: (_, r) => (
          <span>
            {r.bookedCount}/{r.capacity}
          </span>
        ),
      },
      {
        title: 'Trạng thái',
        dataIndex: 'isActive',
        width: 120,
        render: (v) => (v ? <Tag color="green">ACTIVE</Tag> : <Tag>INACTIVE</Tag>),
      },
      {
        title: 'Thao tác',
        key: 'actions',
        width: 140,
        render: (_, row) => (
          <Button
            disabled={!row.isActive}
            danger
            loading={deactMut.isPending}
            onClick={() =>
              confirmDanger({
                title: 'Khóa slot?',
                content: `Khóa slot #${row.slotId} (${row.startsAt} - ${row.endsAt})`,
                okText: 'Khóa',
                onOk: async () => {
                  await deactMut.mutateAsync(row.slotId)
                },
              })
            }
          >
            Khóa
          </Button>
        ),
      },
    ],
    [deactMut],
  )

  const openGenerate = () => {
    const dId = selectedDoctorId
    const dt = selectedDate ?? todayYmd()
    if (dId) {
      genForm.setFieldsValue({
        doctorId: dId,
        date: dt,
        startTime: '08:00',
        endTime: '11:30',
        slotMinutes: 30,
        capacity: 1,
      })
    } else {
      genForm.setFieldsValue({
        date: dt,
        startTime: '08:00',
        endTime: '11:30',
        slotMinutes: 30,
        capacity: 1,
      } as any)
    }
    setGenOpen(true)
  }

  const submitGenerate = async () => {
    const v = await genForm.validateFields()
    await genMut.mutateAsync({
      doctorId: v.doctorId,
      roomId: v.roomId ?? null,
      date: v.date,
      startTime: v.startTime,
      endTime: v.endTime,
      slotMinutes: v.slotMinutes,
      capacity: v.capacity,
    })
  }

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Card>
        <Space style={{ width: '100%', justifyContent: 'space-between' }} align="start">
          <div>
            <Typography.Title level={4} style={{ marginTop: 0, marginBottom: 4 }}>
              Slot
            </Typography.Title>
            <Typography.Text type="secondary">
              Generate slot theo ngày/ca, xem danh sách slot còn trống và khóa slot
            </Typography.Text>
          </div>
          <Button type="primary" onClick={openGenerate}>
            Generate slots
          </Button>
        </Space>
      </Card>

      <Card>
        <Form<FilterValues> form={filterForm} layout="inline" initialValues={{ date: todayYmd() }}>
          <Form.Item name="doctorId" label="Bác sĩ" style={{ minWidth: 360 }}>
            <Select
              showSearch
              optionFilterProp="label"
              options={doctorOptions}
              placeholder="Chọn bác sĩ..."
              loading={qDoctors.isLoading}
            />
          </Form.Item>
          <Form.Item
            name="date"
            label="Ngày (yyyy-mm-dd)"
            rules={[{ pattern: reYmd, message: 'Định dạng yyyy-mm-dd' }]}
          >
            <Input style={{ width: 160 }} placeholder="2026-04-22" />
          </Form.Item>
          <Button
            onClick={() => {
              const v = filterForm.getFieldsValue()
              if (v.doctorId && v.date) {
                qc.invalidateQueries({ queryKey: ['admin', 'slots', v.doctorId, v.date] })
              }
            }}
          >
            Tải lại
          </Button>
        </Form>
      </Card>

      <Card bodyStyle={{ padding: 0 }}>
        <div style={{ height: 'calc(100vh - 360px)', padding: 16 }}>
          <TableShell loading={qSlots.isLoading}>
            <Table<DoctorSlot>
              rowKey="slotId"
              loading={false}
              dataSource={qSlots.data ?? []}
              columns={columns}
              pagination={{ pageSize: 10 }}
              scroll={{ x: 1100, y: 'calc(100vh - 480px)' as any }}
              sticky
              locale={{
                emptyText: <EmptyState title="Chưa có slot" description="Chọn bác sĩ + ngày, rồi bấm “Generate slots” để tạo lịch khám." />,
              }}
            />
          </TableShell>
        </div>
      </Card>

      <Modal
        title="Generate slots"
        open={genOpen}
        onCancel={() => setGenOpen(false)}
        okText="Tạo"
        cancelText="Hủy"
        confirmLoading={genMut.isPending}
        onOk={submitGenerate}
        destroyOnClose
        width={720}
      >
        <Form<GenerateValues> form={genForm} layout="vertical">
          <Form.Item name="doctorId" label="Bác sĩ" rules={[{ required: true, message: 'Chọn bác sĩ' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              options={doctorOptions}
              placeholder="Chọn bác sĩ..."
              loading={qDoctors.isLoading}
            />
          </Form.Item>

          <Space size={12} style={{ width: '100%' }} align="start">
            <Form.Item
              name="date"
              label="Ngày (yyyy-mm-dd)"
              style={{ flex: 1 }}
              rules={[
                { required: true, message: 'Nhập ngày' },
                { pattern: reYmd, message: 'Định dạng yyyy-mm-dd' },
              ]}
            >
              <Input placeholder="2026-04-22" />
            </Form.Item>
            <Form.Item name="roomId" label="Room ID (tuỳ chọn)" style={{ flex: 1 }}>
              <InputNumber style={{ width: '100%' }} min={1} />
            </Form.Item>
          </Space>

          <Space size={12} style={{ width: '100%' }} align="start">
            <Form.Item
              name="startTime"
              label="Giờ bắt đầu (HH:mm)"
              style={{ flex: 1 }}
              rules={[
                { required: true, message: 'Nhập giờ bắt đầu' },
                { pattern: reHm, message: 'Định dạng HH:mm' },
              ]}
            >
              <Input placeholder="08:00" />
            </Form.Item>
            <Form.Item
              name="endTime"
              label="Giờ kết thúc (HH:mm)"
              style={{ flex: 1 }}
              rules={[
                { required: true, message: 'Nhập giờ kết thúc' },
                { pattern: reHm, message: 'Định dạng HH:mm' },
              ]}
            >
              <Input placeholder="11:30" />
            </Form.Item>
          </Space>

          <Space size={12} style={{ width: '100%' }} align="start">
            <Form.Item name="slotMinutes" label="Slot phút" style={{ flex: 1 }} rules={[{ required: true }]}>
              <InputNumber style={{ width: '100%' }} min={30} step={30} />
            </Form.Item>
            <Form.Item name="capacity" label="Sức chứa" style={{ flex: 1 }} rules={[{ required: true }]}>
              <InputNumber style={{ width: '100%' }} min={1} step={1} />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </Space>
  )
}

