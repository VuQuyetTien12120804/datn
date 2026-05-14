import type { ReactNode } from 'react'
import { Skeleton } from 'antd'

export default function TableShell(props: { loading?: boolean; children: ReactNode }) {
  if (props.loading) {
    return (
      <div style={{ padding: 8 }}>
        <Skeleton active paragraph={{ rows: 10 }} />
      </div>
    )
  }
  return <>{props.children}</>
}

