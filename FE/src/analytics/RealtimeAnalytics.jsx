import { useEffect, useState } from 'react'
import { api } from '../api'

const number = value => Number(value || 0).toLocaleString('vi-VN')

const names = {
  Direct: ['Truy cập trực tiếp', 'Direct visits'],
  'Organic Search': ['Từ tìm kiếm', 'Search results'],
  'Organic Social': ['Từ mạng xã hội', 'Social media'],
  Referral: ['Từ website khác', 'Other websites'],
  'Paid Search': ['Từ quảng cáo tìm kiếm', 'Search ads'],
  Unassigned: ['Chưa xác định nguồn', 'Unknown source'],
  desktop: ['Máy tính', 'Computer'],
  mobile: ['Điện thoại', 'Phone'],
  tablet: ['Máy tính bảng', 'Tablet'],
  page_view: ['Xem trang', 'Page view'],
  view_item: ['Xem sản phẩm', 'Product view'],
  add_to_cart: ['Thêm vào giỏ', 'Added to cart'],
  begin_checkout: ['Bắt đầu thanh toán', 'Checkout started'],
  purchase: ['Đơn đã thanh toán', 'Paid purchase'],
  scroll: ['Cuộn gần hết trang', 'Scrolled near the end']
}

export const friendlyName = (value, lang) => names[value]?.[lang === 'en' ? 1 : 0] || value

export function Breakdown({ title, items, labelKey, valueKey, lang, description, translate = false }) {
  const max = Math.max(1, ...items.map(item => Number(item[valueKey] || 0)))
  return <section className="admin-panel analytics-sources">
    <h3>{title}</h3>
    {description && <p className="analytics-caption">{description}</p>}
    {items.length ? <ol>{items.map((item, index) => <li key={`${item[labelKey]}-${index}`}>
      <div><span title={translate ? item[labelKey] : undefined}>{translate ? friendlyName(item[labelKey], lang) : item[labelKey]}</span><strong>{number(item[valueKey])}</strong></div>
      <div className="analytics-track"><span style={{ width: `${Number(item[valueKey] || 0) / max * 100}%` }} /></div>
    </li>)}</ol> : <p>{lang === 'en' ? 'No data in this window.' : 'Chưa có dữ liệu trong khoảng này.'}</p>}
  </section>
}

export default function RealtimeAnalytics({ lang }) {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [refreshKey, setRefreshKey] = useState(0)
  const t = (vi, en) => lang === 'en' ? en : vi

  useEffect(() => {
    let active = true
    let timer
    let controller
    const load = async () => {
      if (!active || document.hidden) return
      controller?.abort()
      controller = new AbortController()
      const current = controller
      try {
        const value = await api.adminAnalyticsRealtime(current.signal)
        if (active && !current.signal.aborted) { setData(value); setError('') }
      } catch (cause) {
        if (active && !current.signal.aborted) { setData(null); setError(cause.message) }
      } finally {
        if (active && !document.hidden && !current.signal.aborted) {
          setLoading(false)
          timer = window.setTimeout(load, 30000)
        }
      }
    }
    const visibility = () => {
      window.clearTimeout(timer)
      if (document.hidden) controller?.abort()
      else load()
    }
    document.addEventListener('visibilitychange', visibility)
    load()
    return () => { active = false; window.clearTimeout(timer); controller?.abort(); document.removeEventListener('visibilitychange', visibility) }
  }, [refreshKey])

  const metrics = data?.metrics || {}
  const trend = data?.trend || []
  const maxTrend = Math.max(1, ...trend.map(item => Number(item.activeUsers || 0)))
  return <div className="admin-analytics">
    <section className="admin-panel analytics-toolbar">
      <div className="panel-head"><div><h2>{t('Hoạt động 30 phút gần đây', 'Activity in the last 30 minutes')}</h2><p>{t('Tự cập nhật sau mỗi 30 giây khi bạn mở tab này.', 'Refreshes every 30 seconds while this tab is open.')}</p></div><button type="button" onClick={() => setRefreshKey(key => key + 1)}>{t('Làm mới', 'Refresh')}</button></div>
      {data?.fetchedAt && <small>{t('Cập nhật lúc', 'Updated at')} {new Date(data.fetchedAt).toLocaleTimeString(lang === 'en' ? 'en-US' : 'vi-VN')}</small>}
    </section>
    {loading && <div className="admin-state">{t('Đang tải...', 'Loading...')}</div>}
    {error && <div className="admin-state error" role="alert">{error}</div>}
    {!loading && data && <>
      {data.status === 'EMPTY' && <div className="admin-state">{t('Chưa ghi nhận khách ghé thăm trong 30 phút qua. Số liệu sẽ tự cập nhật khi có hoạt động.', 'No visits were recorded in the last 30 minutes. This view will refresh when activity appears.')}</div>}
      <div className="metric-grid analytics-two-pairs">
        {[[t('Người có hoạt động', 'Active visitors'), metrics.activeUsers, t('Không phải số người đang mở trang cùng lúc.', 'Not the number of people online at this instant.')], [t('Lượt xem trang', 'Page views'), metrics.pageViews, t('Một người có thể xem nhiều trang.', 'One person may view several pages.')], [t('Lượt tương tác', 'Recorded actions'), metrics.eventCount, t('Gồm cả xem trang và các thao tác khác.', 'Includes page views and other actions.')], [t('Hành động quan trọng', 'Important actions'), metrics.keyEvents, t('Các sự kiện được đánh dấu quan trọng trong Google.', 'Events marked as important in Google.')]].map(([label, value, help]) => <article className="metric" key={label}><span>{label}</span><strong>{number(value)}</strong><small>{help}</small></article>)}
      </div>
      <section className="admin-panel analytics-trend">
        <h3>{t('Khách hoạt động thay đổi thế nào?', 'How has visitor activity changed?')}</h3>
        <p className="analytics-caption">{t('Mỗi cột là số người có hoạt động trong một phút.', 'Each bar shows people active in one minute.')}</p>
        {trend.length ? <><div className="analytics-bars" role="img" aria-label={t('Xu hướng 30 phút', '30-minute trend')}>{trend.map(item => <div className="analytics-bar" key={item.minute} title={`${item.minute} ${t('phút trước', 'minutes ago')}: ${number(item.activeUsers)}`}><span style={{ height: `${Number(item.activeUsers || 0) / maxTrend * 100}%` }} /></div>)}</div><div className="analytics-axis"><span>{t('29 phút trước', '29 minutes ago')}</span><span>{t('Hiện tại', 'Now')}</span></div></> : <p>{t('Chưa có dữ liệu theo phút.', 'No minute data yet.')}</p>}
      </section>
      <div className="analytics-reports">
        <Breakdown title={t('Trang đang được xem', 'Pages being viewed')} items={data.pages || []} labelKey="title" valueKey="pageViews" lang={lang} />
        <Breakdown title={t('Khách đang làm gì?', 'What are visitors doing?')} items={data.events || []} labelKey="name" valueKey="eventCount" lang={lang} translate />
        <Breakdown title={t('Khách dùng thiết bị nào?', 'Visitor devices')} items={data.devices || []} labelKey="name" valueKey="activeUsers" lang={lang} translate />
        <Breakdown title={t('Khách ở quốc gia nào?', 'Visitor countries')} items={data.countries || []} labelKey="name" valueKey="activeUsers" lang={lang} />
      </div>
    </>}
  </div>
}
