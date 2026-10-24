import { useEffect, useState } from 'react'
import { api } from '../api'

const number = value => Number(value || 0).toLocaleString('vi-VN')

export function Breakdown({ title, items, labelKey, valueKey, lang }) {
  const max = Math.max(1, ...items.map(item => Number(item[valueKey] || 0)))
  return <section className="admin-panel analytics-sources">
    <h3>{title}</h3>
    {items.length ? <ol>{items.map((item, index) => <li key={`${item[labelKey]}-${index}`}>
      <div><span>{item[labelKey]}</span><strong>{number(item[valueKey])}</strong></div>
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
    <section className="admin-panel">
      <div className="panel-head"><h2>{t('Trong 30 phút qua', 'Last 30 minutes')}</h2><button type="button" onClick={() => setRefreshKey(key => key + 1)}>{t('Làm mới', 'Refresh')}</button></div>
      <p>{t('GA4 Realtime, cập nhật khoảng 30 giây một lần khi tab đang mở. Người dùng hoạt động không phải số kết nối đồng thời.', 'GA4 Realtime updates about every 30 seconds while this tab is visible. Active users are not concurrent connections.')}</p>
      {data?.fetchedAt && <small>{t('Lấy dữ liệu lúc', 'Fetched at')} {new Date(data.fetchedAt).toLocaleTimeString(lang === 'en' ? 'en-US' : 'vi-VN')}</small>}
    </section>
    {loading && <div className="admin-state">{t('Đang tải...', 'Loading...')}</div>}
    {error && <div className="admin-state error" role="alert">{error}</div>}
    {!loading && data && <>
      {data.status === 'EMPTY' && <div className="admin-state">{t('GA4 chưa ghi nhận hoạt động trong 30 phút gần nhất.', 'GA4 has no recorded activity in the last 30 minutes.')}</div>}
      <div className="metric-grid">
        {[[t('Người dùng hoạt động', 'Active users'), metrics.activeUsers], [t('Lượt xem trang', 'Page views'), metrics.pageViews], [t('Tổng sự kiện', 'Events'), metrics.eventCount], [t('Sự kiện quan trọng', 'Key events'), metrics.keyEvents]].map(([label, value]) => <article className="metric" key={label}><span>{label}</span><strong>{number(value)}</strong></article>)}
      </div>
      <section className="admin-panel analytics-trend">
        <h3>{t('Người dùng hoạt động theo phút', 'Active users by minute')}</h3>
        {trend.length ? <><div className="analytics-bars" role="img" aria-label={t('Xu hướng 30 phút', '30-minute trend')}>{trend.map(item => <div className="analytics-bar" key={item.minute} title={`${item.minute} ${t('phút trước', 'minutes ago')}: ${number(item.activeUsers)}`}><span style={{ height: `${Math.max(3, Number(item.activeUsers || 0) / maxTrend * 100)}%` }} /></div>)}</div><div className="analytics-axis"><span>{t('29 phút trước', '29 minutes ago')}</span><span>{t('Hiện tại', 'Now')}</span></div></> : <p>{t('Chưa có dữ liệu theo phút.', 'No minute data yet.')}</p>}
      </section>
      <div className="analytics-reports">
        <Breakdown title={t('Tiêu đề trang', 'Page titles')} items={data.pages || []} labelKey="title" valueKey="pageViews" lang={lang} />
        <Breakdown title={t('Sự kiện', 'Events')} items={data.events || []} labelKey="name" valueKey="eventCount" lang={lang} />
        <Breakdown title={t('Thiết bị', 'Devices')} items={data.devices || []} labelKey="name" valueKey="activeUsers" lang={lang} />
        <Breakdown title={t('Quốc gia', 'Countries')} items={data.countries || []} labelKey="name" valueKey="activeUsers" lang={lang} />
      </div>
    </>}
  </div>
}
