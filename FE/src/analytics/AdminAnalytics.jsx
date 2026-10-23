import { useEffect, useState } from 'react'
import { api } from '../api'

const formatNumber = value => Number(value || 0).toLocaleString('vi-VN')

export default function AdminAnalytics({ lang = 'vi' }) {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const t = (vi, en) => lang === 'en' ? en : vi

  async function load() {
    setLoading(true)
    setError('')
    try { setData(await api.adminAnalytics()) }
    catch (cause) { setError(cause.message) }
    finally { setLoading(false) }
  }

  useEffect(() => {
    let active = true
    api.adminAnalytics().then(value => { if (active) setData(value) })
      .catch(cause => { if (active) setError(cause.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  return <div className="admin-analytics">
    <div className="admin-panel">
      <div className="panel-head"><h2>Google Analytics 4</h2><button type="button" onClick={load}>{t('Làm mới', 'Refresh')}</button></div>
      <p>{t('30 ngày trước đến hôm qua; chỉ gồm khách đã đồng ý Analytics.', 'Previous 30 days through yesterday; includes visitors who consented to Analytics.')}</p>
    </div>
    {loading && <div className="admin-state">{t('Đang tải...', 'Loading...')}</div>}
    {error && <div className="admin-state error" role="alert">{error}</div>}
    {!loading && !error && data && <>
      <div className="metric-grid">
        {[
          [t('Người dùng hoạt động', 'Active users'), formatNumber(data.activeUsers)],
          [t('Phiên truy cập', 'Sessions'), formatNumber(data.sessions)],
          [t('Lượt xem trang', 'Page views'), formatNumber(data.pageViews)],
          [t('Người cuộn ≥90% trang', 'Users scrolled ≥90%'), formatNumber(data.scrolledUsers)],
          [t('Thời gian tương tác', 'Engagement time'), `${formatNumber(data.engagementSeconds)} s`],
          [t('Tỷ lệ thoát GA4', 'GA4 bounce rate'), `${(Number(data.bounceRate || 0) * 100).toLocaleString('vi-VN', { maximumFractionDigits: 1 })}%`]
        ].map(([label, value]) => <article className="metric" key={label}><span>{label}</span><strong>{value}</strong></article>)}
      </div>
      <p>{t('Tỷ lệ thoát là tỷ lệ phiên không tương tác; không phải tỷ lệ khách chỉ xem một trang.', 'Bounce rate is the share of unengaged sessions, not single-page visits.')}</p>
      <div className="admin-grid">
        <section className="admin-panel"><h2>{t('Xu hướng theo ngày', 'Daily trend')}</h2><div className="table-wrap"><table><thead><tr><th>{t('Ngày', 'Date')}</th><th>{t('Người dùng', 'Users')}</th><th>{t('Phiên', 'Sessions')}</th></tr></thead><tbody>{(data.daily || []).map(item => <tr key={item.date}><td>{item.date}</td><td>{formatNumber(item.activeUsers)}</td><td>{formatNumber(item.sessions)}</td></tr>)}</tbody></table></div></section>
        <section className="admin-panel"><h2>{t('Nguồn truy cập', 'Traffic channels')}</h2><div className="table-wrap"><table><thead><tr><th>{t('Kênh', 'Channel')}</th><th>{t('Phiên', 'Sessions')}</th></tr></thead><tbody>{(data.channels || []).map(item => <tr key={item.channel}><td>{item.channel}</td><td>{formatNumber(item.sessions)}</td></tr>)}</tbody></table></div></section>
      </div>
    </>}
  </div>
}
