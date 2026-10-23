import { useEffect, useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './api'
import { HelpCircle, MessageCircle, MessageSquare } from 'lucide-react'
import { contrastText } from './design/sitePresentation'

export function ChatWidget({ lang = 'vi', presentation }) {
  const [open, setOpen] = useState(false)
  const [body, setBody] = useState('')
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')
  const endRef = useRef(null)
  const client = useQueryClient()
  const copy = (vi, en) => lang === 'en' ? en : vi
  const icons = { message: MessageCircle, bubble: MessageSquare, help: HelpCircle }
  const ChatIcon = icons[presentation?.pages?.brand?.chatIcon?.vi] || MessageCircle
  const savedColor = presentation?.pages?.brand?.chatIcon?.color
  const chatColor = /^#[0-9a-fA-F]{6}$/.test(savedColor || '') ? savedColor : '#0877e8'
  const chat = useQuery({ queryKey: ['chat-session'], queryFn: api.chatSession, enabled: open, refetchInterval: open ? 10000 : false })

  useEffect(() => { endRef.current?.scrollIntoView({ block: 'end' }) }, [chat.data?.messages?.length, open])

  async function send(event) {
    event.preventDefault()
    if (sending || !body.trim()) return
    setSending(true)
    setError('')
    try {
      const thread = await api.chatSend({ body: body.trim(), guestName: name || undefined, guestEmail: email || undefined })
      client.setQueryData(['chat-session'], thread)
      setBody('')
    } catch (cause) {
      setError(cause.message)
    } finally { setSending(false) }
  }

  return <div className="chat-widget" style={{ '--chat-color': chatColor, '--chat-foreground': contrastText(chatColor) }}>
    <button className={`chat-toggle${open ? ' is-open' : ''}`} type="button" aria-expanded={open} aria-controls="store-chat-panel" aria-label={copy('Chat với cửa hàng', 'Chat with the store')} onClick={() => setOpen(value => !value)}><ChatIcon aria-hidden="true" size={23} /></button>
    {open && <section className="chat-box" id="store-chat-panel" aria-label={copy('Chat với cửa hàng', 'Chat with the store')}>
      <header className="chat-box-header"><div><strong>{copy('Đàng Xem hỗ trợ', 'Đàng Xem support')}</strong><span>{copy('Gửi tin nhắn, cửa hàng sẽ phản hồi tại đây', 'Send a message and we’ll reply here')}</span></div><button type="button" onClick={() => setOpen(false)} aria-label={copy('Đóng chat', 'Close chat')}>×</button></header>
      <div className="chat-messages" role="log" aria-live="polite" aria-relevant="additions text">
        {chat.isLoading ? <p className="chat-note">{copy('Đang tải hội thoại…', 'Loading conversation…')}</p> : chat.isError ? <p className="chat-note" role="alert">{chat.error.message} <button type="button" onClick={() => chat.refetch()}>{copy('Thử lại', 'Retry')}</button></p> : chat.data?.messages?.length ? chat.data.messages.map(message => <div className={`chat-message ${message.sender.toLowerCase()}`} key={message.id}><span>{message.body}</span><time dateTime={message.createdAt}>{new Date(message.createdAt).toLocaleTimeString(lang === 'en' ? 'en-US' : 'vi-VN', { hour: '2-digit', minute: '2-digit' })}</time></div>) : <div className="chat-empty"><strong>{copy('Xin chào! 👋', 'Hello! 👋')}</strong><p>{copy('Bạn cần tư vấn sản phẩm hay đơn hàng? Hãy để lại lời nhắn bên dưới.', 'Need help with a product or an order? Leave us a message below.')}</p></div>}
        <div ref={endRef} />
      </div>
      {chat.data?.conversation?.status === 'CLOSED' && <p className="chat-note">{copy('Hội thoại đã kết thúc. Gửi tin nhắn để bắt đầu cuộc trò chuyện mới.', 'This conversation ended. Send a message to start a new one.')}</p>}
      <form className="chat-compose" onSubmit={send}>
        {!chat.data?.conversation && <div className="chat-guest-fields"><input aria-label={copy('Tên của bạn', 'Your name')} placeholder={copy('Tên của bạn (không bắt buộc)', 'Your name (optional)')} value={name} onChange={event => setName(event.target.value)} maxLength={100} /><input aria-label="Email" type="email" placeholder={copy('Email (không bắt buộc)', 'Email (optional)')} value={email} onChange={event => setEmail(event.target.value)} maxLength={320} /></div>}
        <div className="chat-compose-row"><textarea aria-label={copy('Tin nhắn', 'Message')} value={body} onChange={event => setBody(event.target.value)} maxLength={2000} rows={2} required placeholder={copy('Nhập tin nhắn…', 'Write a message…')} disabled={sending || chat.isLoading || chat.isError} /><button type="submit" disabled={sending || !body.trim() || chat.isLoading || chat.isError} aria-label={copy('Gửi tin nhắn', 'Send message')}>{sending ? '…' : '➤'}</button></div>
        {error && <p className="chat-error" role="alert">{error}</p>}
      </form>
    </section>}
  </div>
}
