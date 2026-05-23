import { useEffect, useMemo, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Badge, Button, Card, Empty, Input, List, Space, Typography, message } from 'antd'
import {
  listSupportMessages,
  listSupportThreads,
  markSupportThreadRead,
  sendSupportMessage,
  type ChatMessage,
  type ChatThread,
} from '../api/admin'

function formatTime(ms: number) {
  if (!ms) return ''
  return new Date(ms).toLocaleString('vi-VN')
}

export default function MessagesPage() {
  const qc = useQueryClient()
  const [selectedKey, setSelectedKey] = useState<string | null>(null)
  const [draft, setDraft] = useState('')
  const bottomRef = useRef<HTMLDivElement | null>(null)

  const threadsQuery = useQuery({
    queryKey: ['admin-support-threads'],
    queryFn: listSupportThreads,
    refetchInterval: 8000,
  })

  const threads = threadsQuery.data ?? []

  useEffect(() => {
    if (!selectedKey && threads.length > 0) {
      setSelectedKey(threads[0].threadKey)
    }
  }, [threads, selectedKey])

  const messagesQuery = useQuery({
    queryKey: ['admin-support-messages', selectedKey],
    queryFn: () => listSupportMessages(selectedKey!),
    enabled: !!selectedKey,
    refetchInterval: 5000,
  })

  useEffect(() => {
    if (selectedKey) {
      markSupportThreadRead(selectedKey).then(() => {
        qc.invalidateQueries({ queryKey: ['admin-support-threads'] })
      })
    }
  }, [selectedKey, qc])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messagesQuery.data])

  const sendMutation = useMutation({
    mutationFn: (content: string) => sendSupportMessage(selectedKey!, content),
    onSuccess: () => {
      setDraft('')
      qc.invalidateQueries({ queryKey: ['admin-support-messages', selectedKey] })
      qc.invalidateQueries({ queryKey: ['admin-support-threads'] })
    },
    onError: (err: any) => {
      message.error(err?.response?.data?.message ?? 'Không gửi được tin nhắn')
    },
  })

  const selectedThread = useMemo(
    () => threads.find((t) => t.threadKey === selectedKey) ?? null,
    [threads, selectedKey],
  )

  const messages = messagesQuery.data ?? []

  return (
    <div style={{ display: 'grid', gridTemplateColumns: '320px 1fr', gap: 16, minHeight: '70vh' }}>
      <Card title="CSKH — Hộp thư" loading={threadsQuery.isLoading}>
        <List
          dataSource={threads}
          locale={{ emptyText: <Empty description="Chưa có hội thoại" /> }}
          renderItem={(item: ChatThread) => (
            <List.Item
              style={{
                cursor: 'pointer',
                background: item.threadKey === selectedKey ? 'rgba(22,119,255,0.08)' : undefined,
                borderRadius: 8,
                paddingInline: 12,
              }}
              onClick={() => setSelectedKey(item.threadKey)}
            >
              <List.Item.Meta
                title={
                  <Space>
                    <span>{item.title}</span>
                    {item.unreadCount > 0 ? <Badge count={item.unreadCount} /> : null}
                  </Space>
                }
                description={
                  <>
                    <Typography.Text type="secondary" ellipsis style={{ display: 'block' }}>
                      {item.lastMessage || 'Chưa có tin nhắn'}
                    </Typography.Text>
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      {formatTime(item.updatedAtMs)}
                    </Typography.Text>
                  </>
                }
              />
            </List.Item>
          )}
        />
      </Card>

      <Card
        title={selectedThread ? `Chat với ${selectedThread.title}` : 'Chọn hội thoại'}
        loading={!!selectedKey && messagesQuery.isLoading}
      >
        {!selectedKey ? (
          <Empty description="Chọn bệnh nhân bên trái để trả lời" />
        ) : (
          <>
            <div
              style={{
                minHeight: 420,
                maxHeight: '58vh',
                overflowY: 'auto',
                padding: '8px 4px',
                marginBottom: 16,
                background: '#fafafa',
                borderRadius: 8,
              }}
            >
              {messages.length === 0 ? (
                <Empty description="Chưa có tin nhắn" />
              ) : (
                messages.map((m: ChatMessage) => (
                  <div
                    key={m.messageId}
                    style={{
                      display: 'flex',
                      justifyContent: m.fromMe ? 'flex-end' : 'flex-start',
                      marginBottom: 10,
                    }}
                  >
                    <div
                      style={{
                        maxWidth: '72%',
                        background: m.fromMe ? '#1677ff' : '#fff',
                        color: m.fromMe ? '#fff' : '#111',
                        border: m.fromMe ? 'none' : '1px solid #eee',
                        borderRadius: 12,
                        padding: '8px 12px',
                      }}
                    >
                      <div>{m.content}</div>
                      <Typography.Text
                        style={{
                          fontSize: 11,
                          color: m.fromMe ? 'rgba(255,255,255,0.75)' : '#888',
                          display: 'block',
                          marginTop: 4,
                        }}
                      >
                        {formatTime(m.createdAtMs)} · {m.senderRole}
                      </Typography.Text>
                    </div>
                  </div>
                ))
              )}
              <div ref={bottomRef} />
            </div>

            <Space.Compact style={{ width: '100%' }}>
              <Input.TextArea
                value={draft}
                onChange={(e) => setDraft(e.target.value)}
                placeholder="Nhập phản hồi CSKH..."
                autoSize={{ minRows: 2, maxRows: 4 }}
                onPressEnter={(e) => {
                  if (!e.shiftKey) {
                    e.preventDefault()
                    const text = draft.trim()
                    if (text) sendMutation.mutate(text)
                  }
                }}
              />
              <Button
                type="primary"
                loading={sendMutation.isPending}
                onClick={() => {
                  const text = draft.trim()
                  if (!text) return
                  sendMutation.mutate(text)
                }}
              >
                Gửi
              </Button>
            </Space.Compact>
          </>
        )}
      </Card>
    </div>
  )
}
