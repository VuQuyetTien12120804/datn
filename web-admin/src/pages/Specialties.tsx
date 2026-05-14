import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, Card, Col, Form, Input, Modal, Row, Space, Tag, Typography, message } from 'antd'
import type { Specialty } from '../api/types'
import { createSpecialty, deleteSpecialty, listSpecialties, updateSpecialty } from '../api/admin'
import { confirmDanger } from '../ui/confirm'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'

type FormValues = {
  code?: string
  name: string
  description?: string
}

export default function SpecialtiesPage() {
  const qc = useQueryClient()
  const [form] = Form.useForm<FormValues>()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Specialty | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)
  const [selected, setSelected] = useState<Specialty | null>(null)

  const q = useQuery({
    queryKey: ['admin', 'specialties'],
    queryFn: listSpecialties,
  })

  const createMut = useMutation({
    mutationFn: createSpecialty,
    onSuccess: async () => {
      message.success('Tạo chuyên khoa thành công')
      setOpen(false)
      form.resetFields()
      await qc.invalidateQueries({ queryKey: ['admin', 'specialties'] })
    },
    onError: (e: any) => {
      message.error(e?.response?.data?.message ?? e?.message ?? 'Không tạo được chuyên khoa')
    },
  })

  const updateMut = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: FormValues }) => updateSpecialty(id, payload),
    onSuccess: async () => {
      message.success('Cập nhật chuyên khoa thành công')
      setOpen(false)
      setEditing(null)
      form.resetFields()
      await qc.invalidateQueries({ queryKey: ['admin', 'specialties'] })
    },
    onError: (e: any) => {
      message.error(e?.response?.data?.message ?? e?.message ?? 'Không cập nhật được chuyên khoa')
    },
  })

  const deleteMut = useMutation({
    mutationFn: deleteSpecialty,
    onSuccess: async () => {
      message.success('Đã xóa')
      await qc.invalidateQueries({ queryKey: ['admin', 'specialties'] })
    },
    onError: (e: any) => {
      message.error(e?.response?.data?.message ?? e?.message ?? 'Không xóa được chuyên khoa')
    },
  })

  const openEdit = (row: Specialty) => {
    setEditing(row)
    setOpen(true)
    form.setFieldsValue({
      code: row.code ?? undefined,
      name: row.name,
      description: row.description ?? undefined,
    })
  }

  const openDetail = (row: Specialty) => {
    setSelected(row)
    setDetailOpen(true)
  }

  const submit = async () => {
    const v = await form.validateFields()
    const payload = {
      code: v.code?.trim() ? v.code.trim() : undefined,
      name: v.name.trim(),
      description: v.description?.trim() ? v.description.trim() : undefined,
    }
    if (editing) {
      await updateMut.mutateAsync({ id: editing.specialtyId, payload })
    } else {
      await createMut.mutateAsync(payload)
    }
  }

  return (
    <div className="page-col specialty-page">
      <div className="doctor-page-head">
        <div>
          <Typography.Title level={3} style={{ margin: 0 }}>
            Danh Sách Chuyên Khoa
          </Typography.Title>
          <Typography.Text type="secondary">Quản lý chuyên khoa và mô tả hiển thị trong hệ thống</Typography.Text>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => {
            setEditing(null)
            form.resetFields()
            setOpen(true)
          }}
        >
          Thêm Chuyên Khoa
        </Button>
      </div>

      <div className="page-scroll specialty-list-scroll">
        <Row gutter={[16, 16]}>
          {(q.data ?? []).map((s) => (
            <Col key={s.specialtyId} xs={24} sm={12} md={8} lg={6}>
              <Card className="specialty-card" hoverable onClick={() => openDetail(s)}>
                <div className="specialty-card-head">
                  <Typography.Text strong>{s.name}</Typography.Text>
                  <Tag className="doctor-pill">{s.code ? s.code : `#${s.specialtyId}`}</Tag>
                </div>
                <div style={{ height: 10 }} />
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  {s.description?.trim() ? s.description : '—'}
                </Typography.Text>
              </Card>
            </Col>
          ))}
        </Row>
      </div>

      <Modal
        title={editing ? 'Cập nhật chuyên khoa' : 'Thêm chuyên khoa'}
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
      >
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="code" label="Mã (tuỳ chọn)">
            <Input placeholder="VD: NOITONGQUAT" />
          </Form.Item>
          <Form.Item name="name" label="Tên chuyên khoa" rules={[{ required: true, message: 'Nhập tên chuyên khoa' }]}>
            <Input placeholder="VD: Nội tổng quát" />
          </Form.Item>
          <Form.Item name="description" label="Mô tả (tuỳ chọn)">
            <Input.TextArea rows={4} placeholder="Giới thiệu chuyên khoa..." />
          </Form.Item>
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
        className="specialty-detail-modal"
        destroyOnClose
      >
        {selected ? (
          <div>
            <div className="specialty-detail-head">
              <div style={{ flex: 1 }}>
                <Typography.Title level={4} style={{ margin: 0 }}>
                  {selected.name}
                </Typography.Title>
                <Typography.Text type="secondary">{selected.code ? selected.code : `#${selected.specialtyId}`}</Typography.Text>
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
                      title: 'Xóa chuyên khoa?',
                      content: `Bạn chắc chắn muốn xóa "${selected.name}"?`,
                      okText: 'Xóa',
                      onOk: async () => {
                        await deleteMut.mutateAsync(selected.specialtyId)
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

            <div style={{ height: 14 }} />
            <Typography.Title level={5} style={{ margin: 0 }}>
              Mô tả
            </Typography.Title>
            <div style={{ height: 10 }} />
            <Typography.Text type="secondary">{selected.description?.trim() ? selected.description : '—'}</Typography.Text>
          </div>
        ) : null}
      </Modal>
    </div>
  )
}

