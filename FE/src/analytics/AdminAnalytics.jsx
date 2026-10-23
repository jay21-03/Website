import { useEffect, useState } from 'react'
import { api } from '../api'

const formatNumber = value => Number(value || 0).toLocaleString('vi-VN')

export default function AdminAnalytics({ lang = 'vi' }) {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const t = (vi, en) => lang === 'en' ? en : vi
  const days = data?.daily || []
  const channels = data?.channels || []
  const maxDaily = Math.max(1, ...days.map(item => Number(item.sessions || 0)))
  const maxChannel = Math.max(1, ...channels.map(item => Number(item.sessions || 0)))

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
      <div className="analytics-reports">
        <section className="admin-panel analytics-trend">
          <div className="panel-head"><h2>{t('Xu hướng truy cập', 'Visit trend')}</h2><span>{t('Phiên mỗi ngày', 'Sessions per day')}</span></div>
          {days.length ? <>
            <div className="analytics-bars" role="img" aria-label={t('Biểu đồ số phiên theo ngày', 'Daily sessions chart')}>
              {days.map(item => <div className="analytics-bar" key={item.date} title={`${item.date}: ${formatNumber(item.sessions)} ${t('phiên', 'sessions')}`}><span style={{ height: `${Math.max(3, Number(item.sessions || 0) / maxDaily * 100)}%` }} /></div>)}
            </div>
            <div className="analytics-axis"><span>{days[0].date}</span><span>{days.at(-1).date}</span></div>
            <details><summary>{t('Xem số liệu từng ngày', 'View daily figures')}</summary><div className="table-wrap"><table><thead><tr><th>{t('Ngày', 'Date')}</th><th>{t('Người dùng', 'Users')}</th><th>{t('Phiên', 'Sessions')}</th></tr></thead><tbody>{days.map(item => <tr key={item.date}><td>{item.date}</td><td>{formatNumber(item.activeUsers)}</td><td>{formatNumber(item.sessions)}</td></tr>)}</tbody></table></div></details>
          </> : <p>{t('Chưa có dữ liệu theo ngày.', 'No daily data yet.')}</p>}
        </section>
        <section className="admin-panel analytics-sources">
          <div className="panel-head"><h2>{t('Nguồn truy cập', 'Traffic sources')}</h2><span>{t('Theo số phiên', 'By sessions')}</span></div>
          {channels.length ? <ol>{channels.map(item => <li key={item.channel}><div><span>{item.channel}</span><strong>{formatNumber(item.sessions)}</strong></div><div className="analytics-track"><span style={{ width: `${Number(item.sessions || 0) / maxChannel * 100}%` }} /></div></li>)}</ol> : <p>{t('Chưa có dữ liệu nguồn truy cập.', 'No traffic source data yet.')}</p>}
        </section>
      </div>
    </>}
  </div>
}
