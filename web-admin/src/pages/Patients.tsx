import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Button, Card, Descriptions, Modal, Space, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { listPatients, type Patient } from '../api/admin'
import TableCard from '../ui/TableCard'
import EmptyState from '../ui/EmptyState'
import TableShell from '../ui/TableShell'

export default function PatientsPage() {
  const q = useQuery({ queryKey: ['admin', 'patients'], queryFn: listPatients })
  const [selected, setSelected] = useState<Patient | null>(null)
  const [open, setOpen] = useState(false)

  const columns: ColumnsType<Patient> = useMemo(
    () => [
      { title: 'ID', dataIndex: 'id', width: 90 },
      { title: 'Họ tên', dataIndex: 'fullName', width: 240 },
      { title: 'Giới tính', dataIndex: 'gender', width: 120, render: (v) => <Tag>{v ?? 'unknown'}</Tag> },
      { title: 'Ngày sinh', dataIndex: 'dob', width: 140, render: (v) => v ?? '—' },
      { title: 'Phone', dataIndex: 'phone', width: 140, render: (v) => v ?? '—' },
      { title: 'Email', dataIndex: 'email', width: 240, render: (v) => v ?? '—' },
      {
        title: 'Thao tác',
        key: 'actions',
        width: 140,
        render: (_, row) => (
          <Button
            onClick={() => {
              setSelected(row)
              setOpen(true)
            }}
          >
            Xem
          </Button>
        ),
      },
    ],
    [],
  )

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Card className="card-soft">
        <div>
          <Typography.Title level={4} style={{ marginTop: 0, marginBottom: 4 }}>
            Patients (Bệnh nhân)
          </Typography.Title>
          <Typography.Text type="secondary">Danh sách bệnh nhân + xem thông tin cơ bản</Typography.Text>
        </div>
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
              emptyText: <EmptyState title="Chưa có bệnh nhân" description="Bệnh nhân sẽ xuất hiện sau khi đặt lịch thành công." />,
            }}
          />
        </TableShell>
      </TableCard>

      <Modal title={selected ? `Bệnh nhân #${selected.id}` : 'Bệnh nhân'} open={open} onCancel={() => setOpen(false)} footer={null} width={760}>
        {selected ? (
          <Descriptions bordered size="small" column={2}>
            <Descriptions.Item label="ID">{selected.id}</Descriptions.Item>
            <Descriptions.Item label="Account ID">{selected.accountId ?? '—'}</Descriptions.Item>
            <Descriptions.Item label="Họ tên">{selected.fullName}</Descriptions.Item>
            <Descriptions.Item label="Giới tính">{selected.gender ?? 'unknown'}</Descriptions.Item>
            <Descriptions.Item label="Ngày sinh">{selected.dob ?? '—'}</Descriptions.Item>
            <Descriptions.Item label="Phone">{selected.phone ?? '—'}</Descriptions.Item>
            <Descriptions.Item label="Email">{selected.email ?? '—'}</Descriptions.Item>
            <Descriptions.Item label="Địa chỉ" span={2}>
              {selected.address ?? '—'}
            </Descriptions.Item>
            <Descriptions.Item label="BHYT" span={2}>
              {selected.insuranceNo ?? '—'}
            </Descriptions.Item>
            <Descriptions.Item label="Người liên hệ" span={2}>
              {selected.emergencyContactName ?? '—'} {selected.emergencyContactPhone ? `(${selected.emergencyContactPhone})` : ''}
            </Descriptions.Item>
          </Descriptions>
        ) : null}
      </Modal>
    </Space>
  )
}

