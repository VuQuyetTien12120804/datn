import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Button,
  Card,
  Col,
  Divider,
  Form,
  Input,
  InputNumber,
  Modal,
  Pagination,
  Row,
  Select,
  Space,
  Tag,
  Typography,
  message,
} from 'antd'
import type { DoctorAdmin, Specialty } from '../api/types'
import { createDoctor, deleteDoctor, listAccounts, listDoctors, listSpecialties, updateDoctor, type Account } from '../api/admin'
import { confirmDanger } from '../ui/confirm'
import { reEmail, rePhoneVN, reYmd } from '../ui/validators'
import { Avatar } from 'antd'
import {
  CalendarOutlined,
  DeleteOutlined,
  EditOutlined,
  EnvironmentOutlined,
  IdcardOutlined,
  MailOutlined,
  PhoneOutlined,
  PlusOutlined,
  StarFilled,
  UserOutlined,
} from '@ant-design/icons'

type FormValues = {
  accountId?: number
  fullName: string
  gender?: string
  dob?: string
  phone?: string
  email?: string
  licenseNo?: string
  bio?: string
  avatarUrl?: string
  rating?: number
  visitsCount?: number
  roomLocation?: string
  scheduleText?: string
  education?: string[]
  certificates?: string[]
  specialtyIds?: number[]
}

const genderOptions = [
  { value: 'unknown', label: 'Không rõ' },
  { value: 'male', label: 'Nam' },
  { value: 'female', label: 'Nữ' },
  { value: 'other', label: 'Khác' },
]

const avatarPresets = [
  {
    label: 'Nam (icon chung)',
    value: 'https://api.dicebear.com/7.x/avataaars/png?seed=doctor-male',
  },
  {
    label: 'Nữ (icon chung)',
    value: 'https://api.dicebear.com/7.x/avataaars/png?seed=doctor-female',
  },
]


export default function DoctorsPage() {
  const qc = useQueryClient()
  const [form] = Form.useForm<FormValues>()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<DoctorAdmin | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)
  const [selected, setSelected] = useState<DoctorAdmin | null>(null)
  const [filterSpec, setFilterSpec] = useState<number | 'all'>('all')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(12)

  const [eduInput, setEduInput] = useState('')
  const [certInput, setCertInput] = useState('')

  const qAccounts = useQuery({
    queryKey: ['admin', 'accounts'],
    queryFn: listAccounts,
  })

  const qDoctors = useQuery({
    queryKey: ['admin', 'doctors'],
    queryFn: listDoctors,
  })

  const qSpecs = useQuery({
    queryKey: ['admin', 'specialties'],
    queryFn: listSpecialties,
  })

  const createMut = useMutation({
    mutationFn: createDoctor,
    onSuccess: async () => {
      message.success('Tạo bác sĩ thành công')
      setOpen(false)
      setEditing(null)
      form.resetFields()
      await qc.invalidateQueries({ queryKey: ['admin', 'doctors'] })
    },
    onError: (e: any) => {
      message.error(e?.response?.data?.message ?? e?.message ?? 'Không tạo được bác sĩ')
    },
  })

  const updateMut = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: FormValues }) => updateDoctor(id, payload),
    onSuccess: async () => {
      message.success('Cập nhật bác sĩ thành công')
      setOpen(false)
      setEditing(null)
      form.resetFields()
      await qc.invalidateQueries({ queryKey: ['admin', 'doctors'] })
    },
    onError: (e: any) => {
      message.error(e?.response?.data?.message ?? e?.message ?? 'Không cập nhật được bác sĩ')
    },
  })

  const deleteMut = useMutation({
    mutationFn: deleteDoctor,
    onSuccess: async () => {
      message.success('Đã xóa')
      await qc.invalidateQueries({ queryKey: ['admin', 'doctors'] })
    },
    onError: (e: any) => {
      message.error(e?.response?.data?.message ?? e?.message ?? 'Không xóa được bác sĩ')
    },
  })

  const specialtyMap = useMemo(() => {
    const m = new Map<number, Specialty>()
    for (const s of qSpecs.data ?? []) m.set(s.specialtyId, s)
    return m
  }, [qSpecs.data])

  const submit = async () => {
    const v = await form.validateFields()
    const payload = {
      accountId: v.accountId ?? undefined,
      fullName: v.fullName.trim(),
      gender: v.gender?.trim() ? v.gender.trim() : undefined,
      dob: v.dob?.trim() ? v.dob.trim() : undefined,
      phone: v.phone?.trim() ? v.phone.trim() : undefined,
      email: v.email?.trim() ? v.email.trim() : undefined,
      licenseNo: v.licenseNo?.trim() ? v.licenseNo.trim() : undefined,
      bio: v.bio?.trim() ? v.bio.trim() : undefined,
      avatarUrl: v.avatarUrl?.trim() ? v.avatarUrl.trim() : undefined,
      rating: typeof v.rating === 'number' ? v.rating : undefined,
      visitsCount: typeof v.visitsCount === 'number' ? v.visitsCount : undefined,
      roomLocation: v.roomLocation?.trim() ? v.roomLocation.trim() : undefined,
      scheduleText: v.scheduleText?.trim() ? v.scheduleText.trim() : undefined,
      education: v.education?.length ? v.education : undefined,
      certificates: v.certificates?.length ? v.certificates : undefined,
      specialtyIds: v.specialtyIds?.length ? v.specialtyIds : undefined,
    }
    if (editing) {
      await updateMut.mutateAsync({ id: editing.doctorId, payload })
    } else {
      await createMut.mutateAsync(payload)
    }
  }

  const specOptions = (qSpecs.data ?? []).map((s) => ({
    value: s.specialtyId,
    label: s.name,
  }))

  const doctorAccounts = useMemo(() => {
    const accs = qAccounts.data ?? []
    return accs.filter((a: Account) => {
      const code = (a.role?.code ?? '').toString().toLowerCase()
      return code === 'doctor'
    })
  }, [qAccounts.data])

  const doctorAccountOptions = doctorAccounts.map((a) => ({
    value: a.userId,
    label: `${a.fullName} (#${a.userId})${a.email ? ` - ${a.email}` : ''}${a.phone ? ` - ${a.phone}` : ''}`,
  }))

  const avatarUrl = Form.useWatch('avatarUrl', form) as string | undefined

  const filteredDoctors = useMemo(() => {
    const all = qDoctors.data ?? []
    if (filterSpec === 'all') return all
    return all.filter((d) => (d.specialtyIds ?? []).includes(filterSpec))
  }, [qDoctors.data, filterSpec])

  const pagedDoctors = useMemo(() => {
    const start = (page - 1) * pageSize
    return filteredDoctors.slice(start, start + pageSize)
  }, [filteredDoctors, page, pageSize])

  const primarySpecialtyLabel = (d: DoctorAdmin) => {
    const ids = d.specialtyIds ?? []
    if (!ids.length) return 'Bác sĩ'
    return specialtyMap.get(ids[0])?.name ?? 'Bác sĩ'
  }

  const openDetail = (d: DoctorAdmin) => {
    setSelected(d)
    setDetailOpen(true)
  }

  const openEdit = (d: DoctorAdmin) => {
    setEditing(d)
    setOpen(true)
    form.setFieldsValue({
      accountId: d.accountId ?? undefined,
      fullName: d.fullName,
      gender: d.gender ?? undefined,
      dob: d.dob ?? undefined,
      phone: d.phone ?? undefined,
      email: d.email ?? undefined,
      licenseNo: d.licenseNo ?? undefined,
      bio: d.bio ?? undefined,
      avatarUrl: d.avatarUrl ?? undefined,
      rating: (d.rating ?? 4.8) as any,
      visitsCount: (d.visitsCount ?? 0) as any,
      roomLocation: d.roomLocation ?? undefined,
      scheduleText: d.scheduleText ?? undefined,
      education: (d.education ?? []) as any,
      certificates: (d.certificates ?? []) as any,
      specialtyIds: d.specialtyIds ?? [],
    })

    setEduInput('')
    setCertInput('')
  }

  const addChip = (field: 'education' | 'certificates', value: string) => {
    const v = value.trim()
    if (!v) return
    const arr = (form.getFieldValue(field) as string[] | undefined) ?? []
    form.setFieldsValue({ [field]: [...arr, v] } as any)
  }

  const removeChip = (field: 'education' | 'certificates', idx: number) => {
    const arr = (form.getFieldValue(field) as string[] | undefined) ?? []
    form.setFieldsValue({ [field]: arr.filter((_, i) => i !== idx) } as any)
  }

  return (
    <div className="page-col doctor-page">
      <div className="doctor-page-head">
        <div>
          <Typography.Title level={3} style={{ margin: 0 }}>
            Danh Sách Bác Sĩ
          </Typography.Title>
          <Typography.Text type="secondary">Quản lý thông tin và lịch làm việc của các bác sĩ</Typography.Text>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => {
            setEditing(null)
            form.resetFields()
            form.setFieldsValue({
              gender: 'unknown',
              rating: 4.8,
              visitsCount: 0,
              education: [],
              certificates: [],
              specialtyIds: [],
            })
            setOpen(true)
          }}
        >
          Thêm Bác Sĩ Mới
        </Button>
      </div>

      <Card className="card-soft doctor-filter-card">
        <Typography.Text strong>Bộ Lọc</Typography.Text>
        <div style={{ height: 8 }} />
        <Typography.Text type="secondary">Lọc bác sĩ theo khoa chuyên môn</Typography.Text>
        <div style={{ height: 12 }} />
        <Select
          value={filterSpec}
          onChange={(v) => {
            setFilterSpec(v as any)
            setPage(1)
          }}
          style={{ width: 260 }}
          options={[
            { value: 'all', label: 'Tất Cả Các Khoa' },
            ...(qSpecs.data ?? []).map((s) => ({ value: s.specialtyId, label: s.name })),
          ]}
        />
      </Card>

      <div className="page-scroll doctor-list-scroll">
        <Row gutter={[16, 16]}>
          {pagedDoctors.map((d) => {
            const rating = d.rating ?? 4.8
            const visits = d.visitsCount ?? 0
            const primary = primarySpecialtyLabel(d)
            const tagId = (d.specialtyIds ?? [])[0]
            const tagLabel = tagId ? specialtyMap.get(tagId)?.name ?? 'Chuyên khoa' : 'Chuyên khoa'
            return (
              <Col key={d.doctorId} xs={24} sm={12} md={8} lg={6}>
                <Card className="doctor-card" hoverable onClick={() => openDetail(d)}>
                  <div className="doctor-card-top">
                    <Avatar size={44} src={d.avatarUrl ?? undefined} style={{ background: '#e2e8f0', color: '#0f172a' }}>
                      {d.fullName?.trim()?.[0]?.toUpperCase?.() ?? 'B'}
                    </Avatar>
                    <div className="doctor-card-title">
                      <Typography.Text strong>{d.fullName}</Typography.Text>
                      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                        {primary}
                      </Typography.Text>
                    </div>
                  </div>

                  <div style={{ height: 10 }} />

                  <Tag className="doctor-pill">{tagLabel}</Tag>

                  <div style={{ height: 10 }} />

                  <div className="doctor-card-metrics">
                    <div className="doctor-metric">
                      <UserOutlined style={{ opacity: 0.7 }} />
                      <span>{visits} lượt khám</span>
                    </div>
                    <div className="doctor-metric">
                      <StarFilled style={{ color: '#f59e0b' }} />
                      <span>Đánh giá: {Number(rating).toFixed(1)}/5.0</span>
                    </div>
                  </div>
                </Card>
              </Col>
            )
          })}
        </Row>
      </div>

      <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
        <Pagination
          current={page}
          pageSize={pageSize}
          total={filteredDoctors.length}
          showSizeChanger
          showQuickJumper
          pageSizeOptions={[8, 12, 16, 24]}
          onChange={(nextPage, nextPageSize) => {
            setPage(nextPage)
            if (nextPageSize !== pageSize) {
              setPageSize(nextPageSize)
              setPage(1)
            }
          }}
          showTotal={(t) => `Tổng ${t}`}
        />
      </div>

      <Modal
        title={editing ? 'Sửa Thông Tin Bác Sĩ' : 'Thêm Bác Sĩ Mới'}
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
        width={920}
        className="doctor-edit-modal"
      >
        <Form<FormValues> form={form} layout="vertical">
          <Row gutter={[16, 12]}>
            <Col xs={24} md={14}>
              <Card className="card-soft doctor-form-card" bodyStyle={{ padding: 14 }}>
                <Typography.Text strong>Thông tin cơ bản</Typography.Text>
                <div style={{ height: 8 }} />

                <Form.Item
                  name="fullName"
                  label="Họ và tên"
                  rules={[{ required: true, message: 'Nhập họ tên bác sĩ' }]}
                >
                  <Input prefix={<UserOutlined />} placeholder="VD: BS. Nguyễn Văn A" />
                </Form.Item>

                <Row gutter={12}>
                  <Col span={12}>
                    <Form.Item name="gender" label="Giới tính">
                      <Select options={genderOptions} />
                    </Form.Item>
                  </Col>
                  <Col span={12}>
                    <Form.Item
                      name="dob"
                      label="Ngày sinh"
                      rules={[{ pattern: reYmd, message: 'Định dạng yyyy-mm-dd' }]}
                    >
                      <Input placeholder="1990-01-31" />
                    </Form.Item>
                  </Col>
                </Row>

                <Form.Item name="specialtyIds" label="Chuyên khoa">
                  <Select
                    mode="multiple"
                    options={specOptions}
                    loading={qSpecs.isLoading}
                    placeholder="Chọn chuyên khoa..."
                  />
                </Form.Item>
              </Card>
            </Col>

            <Col xs={24} md={10}>
              <Card className="card-soft doctor-form-card" bodyStyle={{ padding: 14 }}>
                <Typography.Text strong>Avatar</Typography.Text>
                <div style={{ height: 10 }} />

                <div className="doctor-avatar-row">
                  <Avatar
                    size={56}
                    src={avatarUrl}
                    style={{ background: 'rgba(37,99,235,0.12)', color: '#1d4ed8' }}
                  >
                    {((form.getFieldValue('fullName') ?? 'BS') as string).trim()?.[0]?.toUpperCase?.() ?? 'B'}
                  </Avatar>
                  <div style={{ flex: 1 }}>
                    <Form.Item name="avatarUrl" label="Chọn nhanh" style={{ marginBottom: 0 }}>
                      <Select
                        allowClear
                        placeholder="Chọn avatar..."
                        options={avatarPresets}
                      />
                    </Form.Item>
                  </div>
                </div>

                <div style={{ height: 12 }} />

                <Typography.Text strong>Thông tin liên hệ</Typography.Text>
                <div style={{ height: 8 }} />

                <Form.Item
                  name="email"
                  label="Email"
                  rules={[{ pattern: reEmail, message: 'Email không hợp lệ' }]}
                >
                  <Input prefix={<MailOutlined />} placeholder="VD: bs@clinic.com" />
                </Form.Item>

                <Form.Item
                  name="phone"
                  label="Số điện thoại"
                  rules={[{ pattern: rePhoneVN, message: 'SĐT không hợp lệ (0xxxxxxxxx hoặc +84xxxxxxxxx)' }]}
                >
                  <Input prefix={<PhoneOutlined />} placeholder="VD: 090..." />
                </Form.Item>
              </Card>
            </Col>

            <Col span={24}>
              <Card className="card-soft doctor-form-card" bodyStyle={{ padding: 14 }}>
                <Typography.Text strong>Thông tin hệ thống</Typography.Text>
                <div style={{ height: 8 }} />

                <Form.Item
                  name="accountId"
                  label="Tài khoản bác sĩ (DOCTOR) - dùng để đăng nhập app bác sĩ"
                  tooltip="Chọn userId có role DOCTOR để bác sĩ app xem/nhận lịch. Có thể để trống."
                >
                  <Select
                    allowClear
                    showSearch
                    placeholder="Chọn tài khoản DOCTOR (optional)"
                    options={doctorAccountOptions}
                    optionFilterProp="label"
                    loading={qAccounts.isLoading}
                  />
                </Form.Item>

                <Form.Item name="licenseNo" label="Số chứng chỉ (tuỳ chọn)">
                  <Input prefix={<IdcardOutlined />} />
                </Form.Item>

                <Form.Item name="bio" label="Giới thiệu (tuỳ chọn)">
                  <Input.TextArea rows={3} placeholder="Mô tả ngắn về bác sĩ..." />
                </Form.Item>

                <Divider style={{ margin: '10px 0' }} />
                <Row gutter={12}>
                  <Col xs={24} md={12}>
                    <Form.Item name="rating" label="Đánh giá (1-5)">
                      <InputNumber min={1} max={5} step={0.1} style={{ width: '100%' }} />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={12}>
                    <Form.Item name="visitsCount" label="Lượt khám">
                      <InputNumber min={0} step={1} style={{ width: '100%' }} />
                    </Form.Item>
                  </Col>
                </Row>

                <Row gutter={12}>
                  <Col xs={24} md={12}>
                    <Form.Item name="roomLocation" label="Vị trí phòng khám">
                      <Input placeholder="VD: Phòng 201, Tầng 2" />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={12}>
                    <Form.Item name="scheduleText" label="Lịch làm việc">
                      <Input placeholder="VD: Thứ 2-6: 8:00 - 17:00" />
                    </Form.Item>
                  </Col>
                </Row>

                <Row gutter={12}>
                  <Col xs={24} md={12}>
                    <div className="doctor-chip-header">
                      <Typography.Text strong>Học vấn</Typography.Text>
                      <Button
                        icon={<PlusOutlined />}
                        onClick={() => {
                          addChip('education', eduInput)
                          setEduInput('')
                        }}
                      >
                        Thêm
                      </Button>
                    </div>
                    <Input value={eduInput} onChange={(e) => setEduInput(e.target.value)} placeholder="VD: Thạc sĩ Tim mạch - ĐH Y Dược TP.HCM" />
                    <Form.Item name="education" hidden />
                    <div style={{ height: 10 }} />
                    <Space wrap>
                      {((form.getFieldValue('education') as string[] | undefined) ?? []).map((t, idx) => (
                        <Tag
                          key={idx}
                          closable
                          onClose={(e) => {
                            e.preventDefault()
                            removeChip('education', idx)
                          }}
                        >
                          {t}
                        </Tag>
                      ))}
                    </Space>
                  </Col>
                  <Col xs={24} md={12}>
                    <div className="doctor-chip-header">
                      <Typography.Text strong>Chứng chỉ</Typography.Text>
                      <Button
                        icon={<PlusOutlined />}
                        onClick={() => {
                          addChip('certificates', certInput)
                          setCertInput('')
                        }}
                      >
                        Thêm
                      </Button>
                    </div>
                    <Input value={certInput} onChange={(e) => setCertInput(e.target.value)} placeholder="VD: Chứng chỉ Siêu âm Tim" />
                    <Form.Item name="certificates" hidden />
                    <div style={{ height: 10 }} />
                    <Space wrap>
                      {((form.getFieldValue('certificates') as string[] | undefined) ?? []).map((t, idx) => (
                        <Tag
                          key={idx}
                          color="green"
                          closable
                          onClose={(e) => {
                            e.preventDefault()
                            removeChip('certificates', idx)
                          }}
                        >
                          {t}
                        </Tag>
                      ))}
                    </Space>
                  </Col>
                </Row>
              </Card>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Modal
        open={detailOpen}
        onCancel={() => {
          setDetailOpen(false)
          setSelected(null)
        }}
        footer={null}
        width={560}
        className="doctor-detail-modal"
        destroyOnClose
      >
        {selected ? (
          <div>
            <div className="doctor-detail-head">
              <Avatar size={56} src={selected.avatarUrl ?? undefined} style={{ background: '#e2e8f0', color: '#0f172a' }}>
                {selected.fullName?.trim()?.[0]?.toUpperCase?.() ?? 'B'}
              </Avatar>
              <div style={{ flex: 1 }}>
                <Typography.Title level={4} style={{ margin: 0 }}>
                  {selected.fullName}
                </Typography.Title>
                <Typography.Text type="secondary">{primarySpecialtyLabel(selected)}</Typography.Text>
              </div>
              <Space>
                <Button icon={<EditOutlined />} onClick={() => openEdit(selected)}>
                  Sửa
                </Button>
                <Button
                  danger
                  icon={<DeleteOutlined />}
                  loading={deleteMut.isPending}
                  onClick={() => {
                    confirmDanger({
                      title: 'Xóa bác sĩ?',
                      content: `Bạn chắc chắn muốn xóa "${selected.fullName}"?`,
                      okText: 'Xóa',
                      onOk: async () => {
                        await deleteMut.mutateAsync(selected.doctorId)
                        setDetailOpen(false)
                        setSelected(null)
                      },
                    })
                  }}
                >
                  Xóa
                </Button>
              </Space>
            </div>

            <Divider style={{ margin: '14px 0' }} />

            <Typography.Title level={5} style={{ margin: 0 }}>
              Thông Tin Cơ Bản
            </Typography.Title>
            <div style={{ height: 10 }} />

            <Space wrap>
              {(selected.specialtyIds ?? []).slice(0, 3).map((id) => (
                <Tag key={id} className="doctor-pill">
                  {specialtyMap.get(id)?.name ?? `#${id}`}
                </Tag>
              ))}
            </Space>

            <div style={{ height: 12 }} />

            <div className="doctor-detail-kpis">
              <div className="doctor-kpi">
                <UserOutlined />
                <span>{selected.visitsCount ?? 0} lượt khám</span>
              </div>
              <div className="doctor-kpi">
                <StarFilled style={{ color: '#f59e0b' }} />
                <span>Đánh giá: {Number(selected.rating ?? 4.8).toFixed(1)}/5.0</span>
              </div>
            </div>

            <Divider style={{ margin: '14px 0' }} />

            <Typography.Title level={5} style={{ margin: 0 }}>
              Thông Tin Liên Hệ
            </Typography.Title>
            <div style={{ height: 10 }} />

            <div className="doctor-detail-lines">
              <div className="doctor-line">
                <MailOutlined />
                <span>{selected.email ?? '—'}</span>
              </div>
              <div className="doctor-line">
                <PhoneOutlined />
                <span>{selected.phone ?? '—'}</span>
              </div>
              <div className="doctor-line">
                <EnvironmentOutlined />
                <span>{selected.roomLocation ?? '—'}</span>
              </div>
              <div className="doctor-line">
                <CalendarOutlined />
                <span>{selected.scheduleText ?? '—'}</span>
              </div>
            </div>

            <Divider style={{ margin: '14px 0' }} />

            <Typography.Title level={5} style={{ margin: 0 }}>
              Học Vấn
            </Typography.Title>
            <div style={{ height: 10 }} />
            <ul className="doctor-bullets">
              {((selected.education ?? []).length ? (selected.education ?? []) : ['—']).map((t, idx) => (
                <li key={idx}>{t}</li>
              ))}
            </ul>

            <Divider style={{ margin: '14px 0' }} />

            <Typography.Title level={5} style={{ margin: 0 }}>
              Chứng Chỉ &amp; Chuyên Môn
            </Typography.Title>
            <div style={{ height: 10 }} />
            <Space wrap>
              {((selected.certificates ?? []).length ? (selected.certificates ?? []) : ['—']).map((t, idx) => (
                <Tag key={idx} color="green">
                  {t}
                </Tag>
              ))}
            </Space>
          </div>
        ) : null}
      </Modal>
    </div>
  )
}

