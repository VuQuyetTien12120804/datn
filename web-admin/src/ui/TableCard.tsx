import type { ReactNode } from 'react'
import { Card } from 'antd'

export default function TableCard(props: { children: ReactNode; heightOffset?: number }) {
  const offset = props.heightOffset ?? 360
  const h = `calc(100vh - ${offset}px)`
  return (
    <Card className="card-soft" bodyStyle={{ padding: 0 }}>
      <div style={{ height: h, padding: 16 }}>{props.children}</div>
    </Card>
  )
}

