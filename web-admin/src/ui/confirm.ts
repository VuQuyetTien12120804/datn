import { Modal } from 'antd'

export function confirmDanger(opts: { title: string; content?: string; okText?: string; onOk: () => Promise<void> | void }) {
  Modal.confirm({
    title: opts.title,
    content: opts.content,
    okText: opts.okText ?? 'Xác nhận',
    okButtonProps: { danger: true },
    cancelText: 'Hủy',
    onOk: opts.onOk,
  })
}

