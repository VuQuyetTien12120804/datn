import { Alert } from 'antd'

export default function Placeholder({ title }: { title: string }) {
  return <Alert type="info" showIcon message={title} description="Trang này sẽ làm tiếp (CRUD + bảng + form)." />
}

