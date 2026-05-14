import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { DoctorAdmin } from '../api/types'
import {
  getAppointmentDetail,
  listDoctors,
  rescheduleAppointment,
  searchAppointments,
  updateAppointmentStatus,
  type AppointmentStatus,
  type AppointmentAdminDetail,
  type AppointmentAdminRow,
} from '../api/admin'
import { reYmd } from '../ui/validators'
import TableCard from '../ui/TableCard'
import EmptyState from '../ui/EmptyState'
import TableShell from '../ui/TableShell'

type FilterValues = {
  doctorId?: number
  patientId?: any
  status?: AppointmentStatus
  q?: string
  fromDate?: string
  toDate?: string
}

type UpdateValues = {
  status: AppointmentStatus
  note?: string
  cancelReason?: string
}

type RescheduleValues = {
  slotId: number
}

const statusOptions: { value: AppointmentStatus; label: string; color?: string }[] = [
  { value: 'PENDING', label: 'PENDING', color: 'default' },
  { value: 'CONFIRMED', label: 'CONFIRMED', color: 'blue' },
  { value: 'CHECKED_IN', label: 'CHECKED_IN', color: 'cyan' },
  { value: 'COMPLETED', label: 'COMPLETED', color: 'green' },
  { value: 'CANCELLED', label: 'CANCELLED', color: 'red' },
  { value: 'NO_SHOW', label: 'NO_SHOW', color: 'orange' },
]

function statusTag(s: AppointmentStatus) {
  const meta = statusOptions.find((x) => x.value === s)
  return <Tag color={meta?.color}>{s}</Tag>
}

export default function AppointmentsPage() {
  const qc = useQueryClient()
  const [filterForm] = Form.useForm<FilterValues>()
  const [updateForm] = Form.useForm<UpdateValues>()
  const [resForm] = Form.useForm<RescheduleValues>()
  const [detailOpen, setDetailOpen] = useState(false)
  const [updateOpen, setUpdateOpen] = useState(false)
  const [resOpen, setResOpen] = useState(false)
  const [selected, setSelected] = useState<AppointmentAdminRow | null>(null)
  const [detail, setDetail] = useState<AppointmentAdminDetail | null>(null)
  const [page, setPage] = useState(1) // antd is 1-based
  const [pageSize, setPageSize] = useState(10)

  const qDoctors = useQuery({
    queryKey: ['admin', 'doctors'],
    queryFn: listDoctors,
  })

  const filters = Form.useWatch([], filterForm) as FilterValues | undefined

  const q = useQuery({
    queryKey: [
      'admin',
      'appointments.search',
      filters?.doctorId,
      filters?.patientId,
      filters?.status,
      filters?.q,
      filters?.fromDate,
      filters?.toDate,
      page,
      pageSize,
    ],
    queryFn: () =>
      searchAppointments({
        doctorId: filters?.doctorId,
        patientId:
          filters?.patientId !== undefined && filters?.patientId !== null && String(filters?.patientId).trim().length > 0
            ? Number(filters?.patientId)
            : undefined,
        status: filters?.status,
        q: filters?.q?.trim() ? filters?.q.trim() : undefined,
        fromDate: filters?.fromDate?.trim() ? filters?.fromDate.trim() : undefined,
        toDate: filters?.toDate?.trim() ? filters?.toDate.trim() : undefined,
        page: page - 1,
        size: pageSize,
      }),
  })

  const qDetail = useQuery({
    queryKey: ['admin', 'appointments.detail', selected?.appointmentId],
    enabled: !!selected?.appointmentId && detailOpen,
    queryFn: async () => {
      const d = await getAppointmentDetail(selected!.appointmentId)
      setDetail(d)
      return d
    },
  })

  const updateMut = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: UpdateValues }) => updateAppointmentStatus(id, payload),
    onSuccess: async () => {
      message.success('Cập nhật trạng thái thành công')
      setUpdateOpen(false)
      await qc.invalidateQueries({ queryKey: ['admin', 'appointments.search'] })
      await qc.invalidateQueries({ queryKey: ['admin', 'appointments.detail'] })
    },
    onError: (e: any) => {
      message.error(e?.response?.data?.message ?? e?.message ?? 'Không cập nhật được')
    },
  })

  const resMut = useMutation({
    mutationFn: ({ id, slotId }: { id: number; slotId: number }) => rescheduleAppointment(id, slotId),
    onSuccess: async () => {
      message.success('Đổi slot thành công')
      setResOpen(false)
      await qc.invalidateQueries({ queryKey: ['admin', 'appointments.search'] })
      await qc.invalidateQueries({ queryKey: ['admin', 'appointments.detail'] })
    },
    onError: (e: any) => {
      message.error(e?.response?.data?.message ?? e?.message ?? 'Không đổi được slot')
    },
  })

  const doctorOptions = (qDoctors.data ?? []).map((d: DoctorAdmin) => ({
    value: d.doctorId,
    label: `${d.fullName} (#${d.doctorId})`,
  }))

  const columns: ColumnsType<AppointmentAdminRow> = useMemo(
    () => [
      { title: 'ID', dataIndex: 'appointmentId', width: 90 },
      {
        title: 'Bệnh nhân',
        dataIndex: 'patientName',
        width: 220,
        render: (v, r) => (
          <div>
            <div>{v ?? `#${r.patientId ?? ''}`}</div>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              ID: {r.patientId ?? '—'}
            </Typography.Text>
          </div>
        ),
      },
      {
        title: 'Bác sĩ',
        dataIndex: 'doctorName',
        width: 220,
        render: (v, r) => (
          <div>
            <div>{v ?? `#${r.doctorId ?? ''}`}</div>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              ID: {r.doctorId ?? '—'}
            </Typography.Text>
          </div>
        ),
      },
      { title: 'Bắt đầu', dataIndex: 'startsAt', width: 210 },
      { title: 'Kết thúc', dataIndex: 'endsAt', width: 210 },
      {
        title: 'Trạng thái',
        dataIndex: 'status',
        width: 140,
        render: (s: string) => statusTag((s?.toUpperCase?.() ?? 'PENDING') as AppointmentStatus),
      },
      {
        title: 'Thao tác',
        key: 'actions',
        width: 420,
        render: (_, row) => (
          <Space>
            <Button
              onClick={() => {
                setSelected(row)
                setDetail(null)
                setDetailOpen(true)
              }}
            >
              Chi tiết
            </Button>
            <Button
              type="primary"
              onClick={() => {
                setSelected(row)
                setUpdateOpen(true)
                updateForm.setFieldsValue({
                  status: ((row.status ?? 'pending').toUpperCase() as AppointmentStatus) ?? 'PENDING',
                })
              }}
            >
              Cập nhật
            </Button>
            <Button
              onClick={() => {
                setSelected(row)
                setResOpen(true)
                resForm.setFieldsValue({ slotId: row.slotId ?? undefined } as any)
              }}
            >
              Đổi slot
            </Button>
            <Button
              onClick={() => updateMut.mutate({ id: row.appointmentId, payload: { status: 'CONFIRMED' } })}
              disabled={(row.status ?? '').toLowerCase() === 'confirmed'}
            >
              Xác nhận
            </Button>
            <Button danger onClick={() => updateMut.mutate({ id: row.appointmentId, payload: { status: 'CANCELLED', cancelReason: 'Admin từ chối' } })}>
              Từ chối
            </Button>
          </Space>
        ),
      },
    ],
    [resForm, updateForm, updateMut],
  )

  const submitUpdate = async () => {
    if (!selected) return
    const v = await updateForm.validateFields()
    await updateMut.mutateAsync({
      id: selected.appointmentId,
      payload: {
        status: v.status,
        note: v.note?.trim() ? v.note.trim() : undefined,
        cancelReason: v.cancelReason?.trim() ? v.cancelReason.trim() : undefined,
      },
    })
  }

  const submitRes = async () => {
    if (!selected) return
    const v = await resForm.validateFields()
    await resMut.mutateAsync({ id: selected.appointmentId, slotId: v.slotId })
  }

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Card>
        <Space style={{ width: '100%', justifyContent: 'space-between' }} align="start">
          <div>
            <Typography.Title level={4} style={{ marginTop: 0, marginBottom: 4 }}>
              Lịch hẹn
            </Typography.Title>
            <Typography.Text type="secondary">Xem và duyệt lịch hẹn (admin)</Typography.Text>
          </div>
          <Button
            onClick={() => {
              qc.invalidateQueries({
                queryKey: ['admin', 'appointments.search'],
              })
            }}
          >
            Tải lại
          </Button>
        </Space>
      </Card>

      <Card>
        <Form<FilterValues> form={filterForm} layout="inline">
          <Form.Item name="q" label="Tìm kiếm">
            <Input placeholder="Tên bệnh nhân/bác sĩ/lý do..." style={{ width: 260 }} />
          </Form.Item>
          <Form.Item name="doctorId" label="Bác sĩ" style={{ minWidth: 360 }}>
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              options={doctorOptions}
              placeholder="Tất cả"
              loading={qDoctors.isLoading}
            />
          </Form.Item>
          <Form.Item name="patientId" label="Patient ID">
            <Input placeholder="VD: 12" style={{ width: 140 }} />
          </Form.Item>
          <Form.Item
            name="fromDate"
            label="Từ ngày"
            rules={[{ pattern: reYmd, message: 'yyyy-mm-dd' }]}
          >
            <Input placeholder="2026-04-01" style={{ width: 140 }} />
          </Form.Item>
          <Form.Item name="toDate" label="Đến ngày" rules={[{ pattern: reYmd, message: 'yyyy-mm-dd' }]}>
            <Input placeholder="2026-04-30" style={{ width: 140 }} />
          </Form.Item>
          <Form.Item name="status" label="Status" style={{ minWidth: 200 }}>
            <Select
              allowClear
              options={statusOptions.map((s) => ({ value: s.value, label: s.label }))}
              placeholder="Tất cả"
            />
          </Form.Item>
          <Button
            onClick={() => {
              setPage(1)
              qc.invalidateQueries({ queryKey: ['admin', 'appointments.search'] })
            }}
          >
            Áp dụng
          </Button>
          <Button
            onClick={async () => {
              try {
                const v = filterForm.getFieldsValue()
                const data = await searchAppointments({
                  doctorId: v.doctorId,
                  patientId: v.patientId ? Number(v.patientId) : undefined,
                  status: v.status,
                  q: v.q?.trim() ? v.q.trim() : undefined,
                  fromDate: v.fromDate?.trim() ? v.fromDate.trim() : undefined,
                  toDate: v.toDate?.trim() ? v.toDate.trim() : undefined,
                  page: 0,
                  size: 5000,
                })
                const rows = data.content ?? []
                const header = [
                  'appointmentId',
                  'patientId',
                  'patientName',
                  'doctorId',
                  'doctorName',
                  'serviceName',
                  'roomName',
                  'slotId',
                  'startsAt',
                  'endsAt',
                  'status',
                ]
                const csv = [
                  header.join(','),
                  ...rows.map((r) =>
                    header
                      .map((k) => {
                        const val = (r as any)[k]
                        const s = val === null || val === undefined ? '' : String(val)
                        const escaped = s.replaceAll('"', '""')
                        return `"${escaped}"`
                      })
                      .join(','),
                  ),
                ].join('\n')
                const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
                const url = URL.createObjectURL(blob)
                const a = document.createElement('a')
                a.href = url
                a.download = `appointments_${new Date().toISOString().slice(0, 10)}.csv`
                a.click()
                URL.revokeObjectURL(url)
                message.success(`Đã export CSV (${rows.length} dòng)`)
              } catch (e: any) {
                message.error(e?.response?.data?.message ?? e?.message ?? 'Export thất bại')
              }
            }}
          >
            Export CSV
          </Button>
          <Button
            onClick={() => {
              filterForm.resetFields()
              setPage(1)
            }}
          >
            Reset
          </Button>
        </Form>
      </Card>

      <TableCard heightOffset={420}>
        <TableShell loading={q.isLoading}>
          <Table<AppointmentAdminRow>
            rowKey="appointmentId"
            loading={false}
            dataSource={q.data?.content ?? []}
            columns={columns}
            scroll={{ x: 1200, y: 'calc(100vh - 540px)' as any }}
            sticky
            pagination={{
              current: page,
              pageSize,
              total: q.data?.totalElements ?? 0,
              showSizeChanger: true,
              onChange: (p, ps) => {
                setPage(p)
                setPageSize(ps)
              },
            }}
            locale={{
              emptyText: <EmptyState title="Chưa có lịch hẹn" description="Khi bệnh nhân đặt lịch, lịch hẹn sẽ xuất hiện ở đây." />,
            }}
          />
        </TableShell>
      </TableCard>

      <Modal
        title="Chi tiết lịch hẹn"
        open={detailOpen}
        onCancel={() => setDetailOpen(false)}
        footer={null}
        destroyOnClose
        width={760}
      >
        {detail ? (
          <Descriptions bordered size="small" column={2}>
            <Descriptions.Item label="Appointment ID">{detail.appointmentId}</Descriptions.Item>
            <Descriptions.Item label="Status">{statusTag((detail.status as any) ?? 'PENDING')}</Descriptions.Item>
            <Descriptions.Item label="Bác sĩ" span={2}>
              {detail.doctorName ?? '—'} (#{detail.doctorId ?? '—'})
            </Descriptions.Item>
            <Descriptions.Item label="Bệnh nhân" span={2}>
              {detail.patientName ?? '—'} (#{detail.patientId ?? '—'}) — {detail.patientPhone ?? '—'} — {detail.patientEmail ?? '—'}
            </Descriptions.Item>
            <Descriptions.Item label="Starts">{detail.startsAt ?? '—'}</Descriptions.Item>
            <Descriptions.Item label="Ends">{detail.endsAt ?? '—'}</Descriptions.Item>
            <Descriptions.Item label="Room">{detail.roomName ?? '—'} (#{detail.roomId ?? '—'})</Descriptions.Item>
            <Descriptions.Item label="Service">
              {detail.serviceName ?? '—'} {detail.serviceDurationMinutes ? `(${detail.serviceDurationMinutes} phút)` : ''}
            </Descriptions.Item>
            <Descriptions.Item label="Slot ID">{detail.slotId ?? '—'}</Descriptions.Item>
            <Descriptions.Item label="Địa chỉ" span={2}>
              {detail.patientAddress ?? '—'}
            </Descriptions.Item>
            <Descriptions.Item label="Lý do" span={2}>
              {detail.reason ?? '—'}
            </Descriptions.Item>
            <Descriptions.Item label="Ghi chú" span={2}>
              {detail.note ?? '—'}
            </Descriptions.Item>
            <Descriptions.Item label="Lý do hủy" span={2}>
              {detail.cancelReason ?? '—'}
            </Descriptions.Item>
          </Descriptions>
        ) : (
          <Typography.Text type="secondary">{qDetail.isLoading ? 'Đang tải...' : 'Chưa có dữ liệu'}</Typography.Text>
        )}
      </Modal>

      <Modal
        title={selected ? `Cập nhật trạng thái (#${selected.appointmentId})` : 'Cập nhật trạng thái'}
        open={updateOpen}
        onCancel={() => setUpdateOpen(false)}
        okText="Lưu"
        cancelText="Hủy"
        confirmLoading={updateMut.isPending}
        onOk={submitUpdate}
        destroyOnClose
        width={680}
      >
        <Form<UpdateValues> form={updateForm} layout="vertical">
          <Form.Item name="status" label="Trạng thái" rules={[{ required: true, message: 'Chọn trạng thái' }]}>
            <Select options={statusOptions.map((s) => ({ value: s.value, label: s.label }))} />
          </Form.Item>
          <Form.Item name="note" label="Ghi chú (tuỳ chọn)">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item
            name="cancelReason"
            label="Lý do hủy (chỉ cần khi CANCELLED)"
            rules={[
              ({ getFieldValue }) => ({
                validator(_, value) {
                  const st = getFieldValue('status') as AppointmentStatus | undefined
                  if (st === 'CANCELLED' && (!value || String(value).trim().length === 0)) {
                    return Promise.reject(new Error('Nhập lý do hủy'))
                  }
                  return Promise.resolve()
                },
              }),
            ]}
          >
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={selected ? `Đổi slot (#${selected.appointmentId})` : 'Đổi slot'}
        open={resOpen}
        onCancel={() => setResOpen(false)}
        okText="Đổi"
        cancelText="Hủy"
        confirmLoading={resMut.isPending}
        onOk={submitRes}
        destroyOnClose
        width={520}
      >
        <Form<RescheduleValues> form={resForm} layout="vertical">
          <Form.Item name="slotId" label="Slot ID mới" rules={[{ required: true, message: 'Nhập slotId' }]}>
            <InputNumber style={{ width: '100%' }} min={1} />
          </Form.Item>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            Lưu ý: slot phải ACTIVE và còn chỗ (booked_count &lt; capacity).
          </Typography.Paragraph>
        </Form>
      </Modal>
    </Space>
  )
}

