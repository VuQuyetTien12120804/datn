import { Empty, Typography } from 'antd'
import type { ReactNode } from 'react'

export default function EmptyState(props: { title?: string; description?: ReactNode }) {
  return (
    <div style={{ padding: 24 }}>
      <Empty
        image={Empty.PRESENTED_IMAGE_SIMPLE}
        description={
          <div>
            {props.title ? (
              <Typography.Text strong>{props.title}</Typography.Text>
            ) : (
              <Typography.Text strong>Chưa có dữ liệu</Typography.Text>
            )}
            {props.description ? <div style={{ marginTop: 6 }}>{props.description}</div> : null}
          </div>
        }
      />
    </div>
  )
}

