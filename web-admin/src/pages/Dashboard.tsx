import { useState } from 'react'
import { Card, Col, Row, Segmented, Skeleton, Space, Statistic, Typography } from 'antd'
import { useQuery } from '@tanstack/react-query'
import {
  reportRevenueSummary,
  reportTimeseries,
  reportTopDoctors,
  reportTopSpecialties,
  type ReportTimeseriesPoint,
} from '../api/admin'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'

export default function Dashboard() {
  const [range, setRange] = useState<'7d' | '30d' | '90d'>('30d')

  const today = new Date()
  const to = today.toISOString().slice(0, 10)
  const from = (() => {
    const d = new Date(today)
    d.setDate(d.getDate() - (range === '7d' ? 6 : range === '90d' ? 89 : 29))
    return d.toISOString().slice(0, 10)
  })()

  const qRev = useQuery({
    queryKey: ['admin', 'reports', 'revenue', from, to],
    queryFn: () => reportRevenueSummary({ from, to }),
  })

  const qTs = useQuery({
    queryKey: ['admin', 'reports', 'timeseries', from, to],
    queryFn: () => reportTimeseries({ granularity: 'day', from, to }),
  })

  const qTopDoctors = useQuery({
    queryKey: ['admin', 'reports', 'topDoctors', from, to],
    queryFn: () => reportTopDoctors({ from, to, limit: 10 }),
  })

  const qTopSpecs = useQuery({
    queryKey: ['admin', 'reports', 'topSpecs', from, to],
    queryFn: () => reportTopSpecialties({ from, to, limit: 10 }),
  })

  const rev = qRev.data
  const total = rev?.total ?? 0
  const cancelled = rev?.cancelled ?? 0
  const noShow = rev?.noShow ?? 0
  const revenue = rev?.revenueCents ?? 0
  const cancelRate = total > 0 ? (cancelled / total) * 100 : 0
  const noShowRate = total > 0 ? (noShow / total) * 100 : 0

  const ts: ReportTimeseriesPoint[] = qTs.data ?? []
  const loading = qRev.isLoading || qTs.isLoading || qTopDoctors.isLoading || qTopSpecs.isLoading

  const totalCompleted = ts.reduce((s, p) => s + (p.completed ?? 0), 0)
  const totalConfirmed = ts.reduce((s, p) => s + (p.confirmed ?? 0), 0)
  const totalCancelled = ts.reduce((s, p) => s + (p.cancelled ?? 0), 0)
  const totalNoShow = ts.reduce((s, p) => s + (p.noShow ?? 0), 0)
  const totalAll = ts.reduce((s, p) => s + (p.total ?? 0), 0)
  const totalOther = Math.max(0, totalAll - totalCompleted - totalConfirmed - totalCancelled - totalNoShow)

  const statusDistribution = [
    { key: 'completed', name: 'Hoàn tất', value: totalCompleted, color: '#16A34A' },
    { key: 'confirmed', name: 'Đã xác nhận', value: totalConfirmed, color: '#0B84FF' },
    { key: 'cancelled', name: 'Hủy', value: totalCancelled, color: '#DC2626' },
    { key: 'noShow', name: 'No-show', value: totalNoShow, color: '#F59E0B' },
    { key: 'other', name: 'Khác', value: totalOther, color: '#94A3B8' },
  ].filter((d) => d.value > 0)

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Card className="card-soft">
        <Space style={{ width: '100%', justifyContent: 'space-between' }} align="center">
          <div>
            <Typography.Title level={4} style={{ marginTop: 0, marginBottom: 4 }}>
              Tổng quan
            </Typography.Title>
            <Typography.Text type="secondary">
              Khoảng thời gian: {from} → {to}
            </Typography.Text>
          </div>
          <Segmented
            value={range}
            onChange={(v) => setRange(v as any)}
            options={[
              { label: '7 ngày', value: '7d' },
              { label: '30 ngày', value: '30d' },
              { label: '90 ngày', value: '90d' },
            ]}
          />
        </Space>
      </Card>

      <div style={{ height: 'calc(100vh - 220px)', overflow: 'auto', paddingRight: 4 }}>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Row gutter={[16, 16]}>
            <Col xs={24} md={12} lg={6}>
              <Card className="card-soft">
                {loading ? <Skeleton active paragraph={{ rows: 1 }} /> : <Statistic title="Tổng lịch hẹn" value={total} />}
              </Card>
            </Col>
            <Col xs={24} md={12} lg={6}>
              <Card className="card-soft">
                {loading ? <Skeleton active paragraph={{ rows: 1 }} /> : <Statistic title="Tỉ lệ hủy" value={cancelRate.toFixed(1)} suffix="%" />}
              </Card>
            </Col>
            <Col xs={24} md={12} lg={6}>
              <Card className="card-soft">
                {loading ? <Skeleton active paragraph={{ rows: 1 }} /> : <Statistic title="Tỉ lệ no-show" value={noShowRate.toFixed(1)} suffix="%" />}
              </Card>
            </Col>
            <Col xs={24} md={12} lg={6}>
              <Card className="card-soft">
                {loading ? <Skeleton active paragraph={{ rows: 1 }} /> : <Statistic title="Doanh thu dự kiến" value={revenue} />}
              </Card>
            </Col>
          </Row>

          <Row gutter={[16, 16]}>
            <Col xs={24} lg={14}>
              <Card className="card-soft" title="Biểu đồ lịch hẹn theo ngày">
                <div style={{ height: 320 }}>
                  {loading ? (
                    <Skeleton active paragraph={{ rows: 8 }} />
                  ) : (
                    <ResponsiveContainer width="100%" height="100%">
                      <LineChart data={ts}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="period" hide={ts.length > 14} />
                        <YAxis />
                        <Tooltip />
                        <Legend />
                        <Line type="monotone" dataKey="total" stroke="#0B84FF" strokeWidth={2} name="Tổng" dot={false} />
                        <Line type="monotone" dataKey="completed" stroke="#16A34A" strokeWidth={2} name="Hoàn tất" dot={false} />
                        <Line type="monotone" dataKey="cancelled" stroke="#DC2626" strokeWidth={2} name="Hủy" dot={false} />
                        <Line type="monotone" dataKey="noShow" stroke="#F59E0B" strokeWidth={2} name="No-show" dot={false} />
                      </LineChart>
                    </ResponsiveContainer>
                  )}
                </div>
              </Card>
            </Col>
            <Col xs={24} lg={10}>
              <Card className="card-soft" title="Top bác sĩ (số lịch)">
                <div style={{ height: 320 }}>
                  {loading ? (
                    <Skeleton active paragraph={{ rows: 8 }} />
                  ) : (
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart data={qTopDoctors.data ?? []} layout="vertical" margin={{ left: 20 }}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis type="number" />
                        <YAxis type="category" dataKey="name" width={120} />
                        <Tooltip />
                        <Bar dataKey="total" fill="#0B84FF" name="Tổng" />
                      </BarChart>
                    </ResponsiveContainer>
                  )}
                </div>
              </Card>
            </Col>
          </Row>

          <Row gutter={[16, 16]}>
            <Col xs={24} lg={12}>
              <Card className="card-soft" title="Top chuyên khoa (số lịch)">
                <div style={{ height: 320 }}>
                  {loading ? (
                    <Skeleton active paragraph={{ rows: 8 }} />
                  ) : (
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart data={qTopSpecs.data ?? []}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="name" hide />
                        <YAxis />
                        <Tooltip />
                        <Bar dataKey="total" fill="#0EA5E9" name="Tổng" />
                      </BarChart>
                    </ResponsiveContainer>
                  )}
                </div>
              </Card>
            </Col>
            <Col xs={24} lg={12}>
              <Card className="card-soft" title="Phân bố trạng thái lịch hẹn">
                <div style={{ height: 320 }}>
                  {loading ? (
                    <Skeleton active paragraph={{ rows: 8 }} />
                  ) : statusDistribution.length === 0 ? (
                    <Typography.Text type="secondary">Chưa có dữ liệu trong khoảng thời gian này.</Typography.Text>
                  ) : (
                    <ResponsiveContainer width="100%" height="100%">
                      <PieChart>
                        <Tooltip
                          formatter={(value: number, _name, item: any) => {
                            const pct = totalAll > 0 ? ((value / totalAll) * 100).toFixed(1) : '0.0'
                            return [`${value} (${pct}%)`, item?.payload?.name ?? '']
                          }}
                        />
                        <Legend verticalAlign="bottom" height={32} />
                        <Pie
                          data={statusDistribution}
                          dataKey="value"
                          nameKey="name"
                          innerRadius={60}
                          outerRadius={100}
                          paddingAngle={2}
                        >
                          {statusDistribution.map((entry) => (
                            <Cell key={entry.key} fill={entry.color} />
                          ))}
                        </Pie>
                      </PieChart>
                    </ResponsiveContainer>
                  )}
                </div>
              </Card>
            </Col>
          </Row>
        </Space>
      </div>
    </Space>
  )
}

