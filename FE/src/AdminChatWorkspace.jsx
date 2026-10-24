import { useEffect, useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { api } from './api'

export default function AdminChatWorkspace({ lang = 'vi' }) {
  const [searchParams] = useSearchParams()
  const conversationId = searchParams.get('conversation')
  const [status, setStatus] = useState('OPEN')
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState(() => /^\d+$/.test(conversationId || '') ? Number(conversationId) : null)
  const [body, setBody] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const endRef = useRef(null)
  const client = useQueryClient()
  const copy = (vi, en) => lang === 'en' ? en : vi
  const list = useQuery({ queryKey: ['admin-chat', status, page], queryFn: () => api.adminChatConversations(status, page), refetchInterval: 2000, refetchIntervalInBackground: false, retry: 1 })
  const thread = useQuery({ queryKey: ['admin-chat-thread', selected], queryFn: () => api.adminChatThread(selected), enabled: selected != null, refetchInterval: selected != null ? 2000 : false, refetchIntervalInBackground: false, retry: 1 })
  const conversation = thread.data?.conversation
  const label = item => item.guestName || (item.userId ? `${copy('Khách hàng', 'Customer')} #${item.userId}` : copy('Khách vãng lai', 'Guest'))
  const time = value => new Date(value).toLocaleString(lang === 'en' ? 'en-US' : 'vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' })

  useEffect(() => { if (/^\d+$/.test(conversationId || '')) setSelected(Number(conversationId)) }, [conversationId])
  useEffect(() => { endRef.current?.scrollIntoView({ block: 'end' }) }, [selected, thread.data?.messages?.length])

  async function act(action) {
    setBusy(true)
    setError('')
    try {
      const result = await action()
      await client.cancelQueries({ queryKey: ['admin-chat-thread', selected] })
      client.setQueryData(['admin-chat-thread', selected], result)
      await client.invalidateQueries({ queryKey: ['admin-chat'] })
      return true
    } catch (cause) {
      setError(cause.message)
      return false
    } finally { setBusy(false) }
  }

  async function send(event) {
    event.preventDefault()
    if (!body.trim() || busy) return
    if (await act(() => api.adminChatReply(selected, { body: body.trim() }))) setBody('')
  }

  return <main className="admin-main chat-admin">
    <header className="chat-admin-heading"><div><h1>{copy('Hội thoại khách hàng', 'Customer conversations')}</h1><p>{copy('Chọn hội thoại để xem và trả lời.', 'Select a conversation to read and reply.')}</p></div><span>{list.data?.totalElements ?? 0} {status === 'OPEN' ? copy('đang mở', 'open') : copy('đã đóng', 'closed')}</span></header>
    <div className="chat-admin-workspace">
      <section className="chat-admin-list" aria-label={copy('Danh sách hội thoại', 'Conversation list')}>
        <div className="chat-admin-tabs" role="group" aria-label={copy('Trạng thái hội thoại', 'Conversation status')}>
          {['OPEN', 'CLOSED'].map(value => <button key={value} type="button" aria-pressed={status === value} onClick={() => { setStatus(value); setPage(0); setSelected(null); setError('') }}>{value === 'OPEN' ? copy('Đang mở', 'Open') : copy('Đã đóng', 'Closed')}</button>)}
        </div>
        <div className="chat-admin-list-body">
          {list.isLoading ? <p className="chat-admin-state">{copy('Đang tải hội thoại…', 'Loading conversations…')}</p> : list.isError ? <p className="chat-admin-state" role="alert">{list.error.message} <button type="button" onClick={() => list.refetch()}>{copy('Thử lại', 'Retry')}</button></p> : !list.data?.content?.length ? <p className="chat-admin-state">{status === 'OPEN' ? copy('Chưa có hội thoại nào đang mở.', 'No open conversations.') : copy('Chưa có hội thoại đã đóng.', 'No closed conversations.')}</p> : list.data.content.map(item => <button className="chat-admin-item" aria-current={selected === item.id ? 'true' : undefined} key={item.id} type="button" onClick={() => { setSelected(item.id); setError(''); setBody('') }}><span className="chat-admin-avatar" aria-hidden="true">{label(item).charAt(0).toUpperCase()}</span><span className="chat-admin-item-text"><strong>{label(item)}</strong><small>{item.guestEmail || `${copy('Hội thoại', 'Conversation')} #${item.id}`}</small><time dateTime={item.updatedAt}>{time(item.updatedAt)}</time></span></button>)}
        </div>
        {list.data?.totalPages > 1 && <nav className="chat-admin-pages" aria-label={copy('Trang hội thoại', 'Conversation pages')}><button type="button" disabled={page === 0} onClick={() => { setPage(value => value - 1); setSelected(null) }}>‹</button><span>{page + 1}/{list.data.totalPages}</span><button type="button" disabled={page + 1 >= list.data.totalPages} onClick={() => { setPage(value => value + 1); setSelected(null) }}>›</button></nav>}
      </section>
      <section className="chat-admin-detail" aria-label={copy('Nội dung hội thoại', 'Conversation details')}>
        {selected == null ? <div className="chat-admin-placeholder"><strong>{copy('Chọn một hội thoại', 'Choose a conversation')}</strong><p>{copy('Nội dung và thao tác trả lời sẽ xuất hiện tại đây.', 'Messages and reply controls will appear here.')}</p></div> : thread.isLoading ? <p className="chat-admin-state">{copy('Đang tải tin nhắn…', 'Loading messages…')}</p> : thread.isError ? <p className="chat-admin-state" role="alert">{thread.error.message} <button type="button" onClick={() => thread.refetch()}>{copy('Thử lại', 'Retry')}</button></p> : <>
          <header className="chat-admin-detail-head"><div><strong>{label(conversation)}</strong><small>{conversation.guestEmail || `${copy('Hội thoại', 'Conversation')} #${conversation.id}`}</small></div><span className="chat-admin-status">{conversation.status === 'OPEN' ? copy('Đang mở', 'Open') : copy('Đã đóng', 'Closed')}</span></header>
          <div className="chat-admin-messages" role="log" aria-live="polite">{thread.data.messages.map(message => <div className={`chat-admin-message ${message.sender.toLowerCase()}`} key={message.id}><small>{message.sender === 'ADMIN' ? copy('Cửa hàng', 'Store') : copy('Khách hàng', 'Customer')} · {time(message.createdAt)}</small><p>{message.body}</p></div>)}<div ref={endRef} /></div>
          {conversation.status === 'OPEN' ? <form className="chat-admin-compose" onSubmit={send}><label htmlFor="admin-chat-reply">{copy('Trả lời khách hàng', 'Reply to customer')}</label><textarea id="admin-chat-reply" rows={3} maxLength={2000} required value={body} onChange={event => setBody(event.target.value)} placeholder={copy('Nhập tin nhắn…', 'Write a reply…')} disabled={busy} /><div><button type="button" className="chat-admin-close" disabled={busy} onClick={() => act(() => api.adminChatClose(selected))}>{copy('Đóng hội thoại', 'Close conversation')}</button><button type="submit" className="admin-primary" disabled={busy || !body.trim()}>{busy ? copy('Đang xử lý…', 'Working…') : copy('Gửi trả lời', 'Send reply')}</button></div>{error && <p role="alert" className="chat-admin-error">{error}</p>}</form> : <p className="chat-admin-closed">{copy('Hội thoại đã đóng. Không thể gửi thêm tin nhắn.', 'This conversation is closed. No more replies can be sent.')}</p>}
        </>}
      </section>
    </div>
  </main>
}
