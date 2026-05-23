import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { QueryClient } from '@tanstack/react-query'
import {
  Button,
  Card,
  Descriptions,
  Dropdown,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Typography,
  message,
} from 'antd'
import type { MenuProps } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { DoctorAdmin } from '../api/types'
import {
  getAppointmentDetail,
  listDoctors,
  listSlots,
  rescheduleAppointment,
  searchAllAppointments,
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
import {
  APPOINTMENT_STATUS_OPTIONS,
  appointmentStatusLabel,
  appointmentStatusTag,
  normalizeAppointmentStatus,
} from '../ui/appointmentStatus'

type FilterValues = {
  doctorId?: number
  patientId?: string
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
  resDate: string
  slotId: number
}

function todayYmd() {
  return new Date().toISOString().slice(0, 10)
}

function parseFilterValues(v: FilterValues) {
  return {
    doctorId: v.doctorId,
    patientId:
      v.patientId !== undefined && v.patientId !== null && String(v.patientId).trim().length > 0
        ? Number(v.patientId)
        : undefined,
    status: v.status,
    q: v.q?.trim() ? v.q.trim() : undefined,
    fromDate: v.fromDate?.trim() ? v.fromDate.trim() : undefined,
    toDate: v.toDate?.trim() ? v.toDate.trim() : undefined,
  }
}

async function invalidateAppointmentQueries(qc: QueryClient) {
  await qc.invalidateQueries({ queryKey: ['admin', 'appointments.search'] })
  await qc.invalidateQueries({ queryKey: ['admin', 'appointments.detail'] })
  await qc.invalidateQueries({ queryKey: ['admin', 'reports'] })
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
  const [appliedFilters, setAppliedFilters] = useState<FilterValues>({})
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [exporting, setExporting] = useState(false)

  const qDoctors = useQuery({
    queryKey: ['admin', 'doctors'],
    queryFn: listDoctors,
  })

  const q = useQuery({
    queryKey: ['admin', 'appointments.search', appliedFilters, page, pageSize],
    queryFn: () =>
      searchAppointments({
        ...parseFilterValues(appliedFilters),
        page: page - 1,
        size: pageSize,
      }),
  })

  const resDate = Form.useWatch('resDate', resForm) as string | undefined

  const qResSlots = useQuery({
    queryKey: ['admin', 'slots.reschedule', selected?.doctorId, resDate],
    enabled: resOpen && !!selected?.doctorId && !!resDate && reYmd.test(resDate),
    queryFn: () => listSlots(selected!.doctorId!, resDate!),
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
      await invalidateAppointmentQueries(qc)
    },
    onError: (e: any) => {
      message.error(e?.response?.data?.message ?? e?.message ?? 'Không cập nhật được')
    },
  })

  const resMut = useMutation({
    mutationFn: ({ id, slotId }: { id: number; slotId: number }) => rescheduleAppointment(id, slotId),
    onSuccess: async () => {
      message.success('Đổi khung giờ thành công')
      setResOpen(false)
      await invalidateAppointmentQueries(qc)
    },
    onError: (e: any) => {
      message.error(e?.response?.data?.message ?? e?.message ?? 'Không đổi được khung giờ')
    },
  })

  const doctorOptions = (qDoctors.data ?? []).map((d: DoctorAdmin) => ({
    value: d.doctorId,
    label: `${d.fullName} (#${d.doctorId})`,
  }))

  const slotOptions = useMemo(
    () =>
      (qResSlots.data ?? []).map((s) => ({
        value: s.slotId,
        label: `#${s.slotId} · ${formatSlotTime(s.startsAt)}–${formatSlotTime(s.endsAt)} · ${s.bookedCount}/${s.capacity} đã đặt`,
        disabled: !s.isActive || s.bookedCount >= s.capacity,
      })),
    [qResSlots.data],
  )

  const applyFilters = async () => {
    try {
      await filterForm.validateFields()
      setAppliedFilters(filterForm.getFieldsValue())
      setPage(1)
    } catch {
      /* validation messages */
    }
  }

  const resetFilters = () => {
    filterForm.resetFields()
    setAppliedFilters({})
    setPage(1)
  }

  const exportCsv = async () => {
    try {
      await filterForm.validateFields()
      setExporting(true)
      const params = parseFilterValues(filterForm.getFieldsValue())
      const rows = await searchAllAppointments(params)
      const columns = [
        { key: 'appointmentId', label: 'Mã lịch' },
        { key: 'patientId', label: 'Mã BN' },
        { key: 'patientName', label: 'Bệnh nhân' },
        { key: 'doctorId', label: 'Mã BS' },
        { key: 'doctorName', label: 'Bác sĩ' },
        { key: 'serviceName', label: 'Dịch vụ' },
        { key: 'roomName', label: 'Phòng' },
        { key: 'slotId', label: 'Slot' },
        { key: 'startsAt', label: 'Bắt đầu' },
        { key: 'endsAt', label: 'Kết thúc' },
        { key: 'status', label: 'Trạng thái' },
      ] as const
      const csv = [
        columns.map((c) => c.label).join(','),
        ...rows.map((r) =>
          columns
            .map(({ key }) => {
              const raw = (r as Record<string, unknown>)[key]
              const val = key === 'status' ? appointmentStatusLabel(String(raw ?? '')) : raw
              const s = val === null || val === undefined ? '' : String(val)
              return `"${s.replaceAll('"', '""')}"`
            })
            .join(','),
        ),
      ].join('\n')
      const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `lich_hen_${new Date().toISOString().slice(0, 10)}.csv`
      a.click()
      URL.revokeObjectURL(url)
      message.success(`Đã xuất CSV (${rows.length} dòng)`)
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? e?.message ?? 'Xuất CSV thất bại')
    } finally {
      setExporting(false)
    }
  }

  const openReschedule = (row: AppointmentAdminRow) => {
    setSelected(row)
    setResOpen(true)
    const date = row.startsAt?.slice(0, 10) ?? todayYmd()
    resForm.setFieldsValue({ resDate: date, slotId: undefined })
  }

  const columns: ColumnsType<AppointmentAdminRow> = useMemo(
    () => [
      { title: 'Mã', dataIndex: 'appointmentId', width: 80 },
      {
        title: 'Bệnh nhân',
        dataIndex: 'patientName',
        width: 200,
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
        width: 200,
        render: (v, r) => (
          <div>
            <div>{v ?? `#${r.doctorId ?? ''}`}</div>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              ID: {r.doctorId ?? '—'}
            </Typography.Text>
          </div>
        ),
      },
      { title: 'Bắt đầu', dataIndex: 'startsAt', width: 180 },
      {
        title: 'Trạng thái',
        dataIndex: 'status',
        width: 130,
        render: (s: string) => appointmentStatusTag(s),
      },
      {
        title: 'Thao tác',
        key: 'actions',
        width: 280,
        fixed: 'right',
        render: (_, row) => {
          const st = (row.status ?? '').toLowerCase()
          const quickItems: MenuProps['items'] = [
            {
              key: 'confirm',
              label: 'Xác nhận nhanh',
              disabled: st === 'confirmed' || st === 'completed' || st === 'cancelled',
              onClick: () => updateMut.mutate({ id: row.appointmentId, payload: { status: 'CONFIRMED' } }),
            },
            {
              key: 'reject',
              label: 'Từ chối nhanh',
              danger: true,
              disabled: st === 'cancelled' || st === 'completed',
              onClick: () =>
                updateMut.mutate({
                  id: row.appointmentId,
                  payload: { status: 'CANCELLED', cancelReason: 'Admin từ chối' },
                }),
            },
          ]
          return (
            <Space size={4} wrap>
              <Button size="small" onClick={() => { setSelected(row); setDetail(null); setDetailOpen(true) }}>
                Chi tiết
              </Button>
              <Button
                size="small"
                type="primary"
                onClick={() => {
                  setSelected(row)
                  setUpdateOpen(true)
                  updateForm.setFieldsValue({ status: normalizeAppointmentStatus(row.status) })
                }}
              >
                Trạng thái
              </Button>
              <Button size="small" onClick={() => openReschedule(row)}>
                Đổi giờ
              </Button>
              <Dropdown menu={{ items: quickItems }} trigger={['click']}>
                <Button size="small">⋯</Button>
              </Dropdown>
            </Space>
          )
        },
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
            <Typography.Text type="secondary">Duyệt và quản lý lịch hẹn từ app bệnh nhân</Typography.Text>
          </div>
          <Button onClick={() => qc.invalidateQueries({ queryKey: ['admin', 'appointments.search'] })}>
            Tải lại
          </Button>
        </Space>
      </Card>

      <Card>
        <Form<FilterValues> form={filterForm} layout="inline" onFinish={applyFilters}>
          <Form.Item name="q" label="Tìm kiếm">
            <Input placeholder="Tên BN/BS, mã lịch..." style={{ width: 220 }} allowClear />
          </Form.Item>
          <Form.Item name="doctorId" label="Bác sĩ" style={{ minWidth: 280 }}>
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              options={doctorOptions}
              placeholder="Tất cả"
              loading={qDoctors.isLoading}
              style={{ minWidth: 240 }}
            />
          </Form.Item>
          <Form.Item name="patientId" label="Mã BN">
            <Input placeholder="VD: 12" style={{ width: 100 }} allowClear />
          </Form.Item>
          <Form.Item name="fromDate" label="Từ ngày" rules={[{ pattern: reYmd, message: 'yyyy-mm-dd' }]}>
            <Input placeholder="2026-05-01" style={{ width: 130 }} allowClear />
          </Form.Item>
          <Form.Item name="toDate" label="Đến ngày" rules={[{ pattern: reYmd, message: 'yyyy-mm-dd' }]}>
            <Input placeholder="2026-05-31" style={{ width: 130 }} allowClear />
          </Form.Item>
          <Form.Item name="status" label="Trạng thái" style={{ minWidth: 180 }}>
            <Select
              allowClear
              options={APPOINTMENT_STATUS_OPTIONS.map((s) => ({ value: s.value, label: s.label }))}
              placeholder="Tất cả"
              style={{ minWidth: 160 }}
            />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                Áp dụng
              </Button>
              <Button loading={exporting} onClick={exportCsv}>
                Xuất CSV
              </Button>
              <Button onClick={resetFilters}>Xóa lọc</Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <TableCard heightOffset={420}>
        <TableShell loading={q.isLoading}>
          <Table<AppointmentAdminRow>
            rowKey="appointmentId"
            loading={false}
            dataSource={q.data?.content ?? []}
            columns={columns}
            scroll={{ x: 1100, y: 'calc(100vh - 540px)' as any }}
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
              emptyText: (
                <EmptyState title="Chưa có lịch hẹn" description="Khi bệnh nhân đặt lịch, lịch hẹn sẽ xuất hiện ở đây." />
              ),
            }}
          />
        </TableShell>
      </TableCard>

      <Modal title="Chi tiết lịch hẹn" open={detailOpen} onCancel={() => setDetailOpen(false)} footer={null} destroyOnClose width={760}>
        {detail ? (
          <Descriptions bordered size="small" column={2}>
            <Descriptions.Item label="Mã lịch">{detail.appointmentId}</Descriptions.Item>
            <Descriptions.Item label="Trạng thái">{appointmentStatusTag(detail.status)}</Descriptions.Item>
            <Descriptions.Item label="Bác sĩ" span={2}>
              {detail.doctorName ?? '—'} (#{detail.doctorId ?? '—'})
            </Descriptions.Item>
            <Descriptions.Item label="Bệnh nhân" span={2}>
              {detail.patientName ?? '—'} (#{detail.patientId ?? '—'}) — {detail.patientPhone ?? '—'} —{' '}
              {detail.patientEmail ?? '—'}
            </Descriptions.Item>
            <Descriptions.Item label="Bắt đầu">{detail.startsAt ?? '—'}</Descriptions.Item>
            <Descriptions.Item label="Kết thúc">{detail.endsAt ?? '—'}</Descriptions.Item>
            <Descriptions.Item label="Phòng">
              {detail.roomName ?? '—'} (#{detail.roomId ?? '—'})
            </Descriptions.Item>
            <Descriptions.Item label="Dịch vụ">
              {detail.serviceName ?? '—'} {detail.serviceDurationMinutes ? `(${detail.serviceDurationMinutes} phút)` : ''}
            </Descriptions.Item>
            <Descriptions.Item label="Khung giờ (slot)">{detail.slotId ?? '—'}</Descriptions.Item>
            <Descriptions.Item label="Địa chỉ">{detail.patientAddress ?? '—'}</Descriptions.Item>
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
            <Select options={APPOINTMENT_STATUS_OPTIONS.map((s) => ({ value: s.value, label: s.label }))} />
          </Form.Item>
          <Form.Item name="note" label="Ghi chú (tuỳ chọn)">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item
            name="cancelReason"
            label="Lý do hủy (bắt buộc khi hủy)"
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
        title={selected ? `Đổi khung giờ (#${selected.appointmentId})` : 'Đổi khung giờ'}
        open={resOpen}
        onCancel={() => setResOpen(false)}
        okText="Lưu"
        cancelText="Hủy"
        confirmLoading={resMut.isPending}
        onOk={submitRes}
        destroyOnClose
        width={560}
      >
        {selected ? (
          <Typography.Paragraph type="secondary" style={{ marginTop: 0 }}>
            Bác sĩ: {selected.doctorName ?? `#${selected.doctorId}`}
          </Typography.Paragraph>
        ) : null}
        <Form<RescheduleValues> form={resForm} layout="vertical">
          <Form.Item
            name="resDate"
            label="Ngày khám"
            rules={[
              { required: true, message: 'Chọn ngày' },
              { pattern: reYmd, message: 'Định dạng yyyy-mm-dd' },
            ]}
          >
            <Input placeholder="2026-05-22" onChange={() => resForm.setFieldValue('slotId', undefined)} />
          </Form.Item>
          <Form.Item name="slotId" label="Khung giờ mới" rules={[{ required: true, message: 'Chọn khung giờ' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              placeholder={qResSlots.isLoading ? 'Đang tải slot...' : 'Chọn khung giờ còn chỗ'}
              options={slotOptions}
              loading={qResSlots.isLoading}
              notFoundContent={
                qResSlots.isLoading ? 'Đang tải...' : 'Không có slot khả dụng — thử ngày khác hoặc tạo slot mới'
              }
            />
          </Form.Item>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            Chỉ hiển thị slot đang mở và còn chỗ trống.
          </Typography.Paragraph>
        </Form>
      </Modal>
    </Space>
  )
}

function formatSlotTime(iso: string) {
  if (!iso) return ''
  try {
    const d = new Date(iso)
    return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false })
  } catch {
    return iso.slice(11, 16)
  }
}
