import { useEffect, useState } from 'react'
import { api } from '../api'
import RealtimeAnalytics, { Breakdown } from './RealtimeAnalytics'
import BusinessAnalytics from './BusinessAnalytics'

const formatNumber = value => Number(value || 0).toLocaleString('vi-VN')

function HistoricalAnalytics({ lang = 'vi', view = 'overview' }) {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [days, setDays] = useState(30)
  const [refreshKey, setRefreshKey] = useState(0)
  const t = (vi, en) => lang === 'en' ? en : vi
  const dailyRows = data?.daily || []
  const channels = data?.channels || []
  const maxDaily = Math.max(1, ...dailyRows.map(item => Number(item.sessions || 0)))
  const maxChannel = Math.max(1, ...channels.map(item => Number(item.sessions || 0)))

  useEffect(() => {
    let active = true
    const controller = new AbortController()
    api.adminAnalyticsHistorical(days, controller.signal).then(value => { if (active) { setData(value); setError('') } })
      .catch(cause => { if (active && !controller.signal.aborted) { setData(null); setError(cause.message) } })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false; controller.abort() }
  }, [days, refreshKey])

  return <div className="admin-analytics">
    <div className="admin-panel">
      <div className="panel-head"><h2>Google Analytics 4</h2><button type="button" onClick={() => { setLoading(true); setRefreshKey(key => key + 1) }}>{t('Làm mới', 'Refresh')}</button></div>
      <label>{t('Khoảng thời gian', 'Date range')} <select value={days} onChange={event => { setLoading(true); setDays(Number(event.target.value)) }}><option value={7}>7 {t('ngày', 'days')}</option><option value={30}>30 {t('ngày', 'days')}</option><option value={90}>90 {t('ngày', 'days')}</option></select></label>
      <p>{t(`${days} ngày hoàn chỉnh đến hôm qua; chỉ gồm khách đã đồng ý Analytics.`, `${days} completed days through yesterday; includes visitors who consented to Analytics.`)}</p>
      {data?.fromDate && <small>{data.fromDate} – {data.toDate} · {data.timezone}</small>}
    </div>
    {view === 'overview' && <BusinessAnalytics key={days} lang={lang} compact rangeDays={days} />}
    {loading && <div className="admin-state">{t('Đang tải...', 'Loading...')}</div>}
    {error && <div className="admin-state error" role="alert">{error}</div>}
    {!loading && !error && data?.status === 'EMPTY' && <div className="admin-state">{t('GA4 chưa có dữ liệu cho kỳ đã chọn. Báo cáo ngày có thể cập nhật chậm.', 'GA4 has no data for this completed period yet. Daily reports may be delayed.')}</div>}
    {!loading && !error && data?.status !== 'EMPTY' && data && view === 'audience' && <div className="analytics-reports">
      <Breakdown title={t('Kênh truy cập', 'Traffic channels')} items={channels} labelKey="channel" valueKey="sessions" lang={lang} />
      <Breakdown title={t('Nguồn / phương tiện', 'Source / medium')} items={data.sourceMedium || []} labelKey="name" valueKey="sessions" lang={lang} />
      <Breakdown title={t('Thiết bị', 'Devices')} items={data.devices || []} labelKey="name" valueKey="activeUsers" lang={lang} />
      <Breakdown title={t('Trình duyệt', 'Browsers')} items={data.browsers || []} labelKey="name" valueKey="activeUsers" lang={lang} />
      <Breakdown title={t('Hệ điều hành', 'Operating systems')} items={data.operatingSystems || []} labelKey="name" valueKey="activeUsers" lang={lang} />
      <Breakdown title={t('Quốc gia', 'Countries')} items={data.countries || []} labelKey="name" valueKey="activeUsers" lang={lang} />
    </div>}
    {!loading && !error && data?.status !== 'EMPTY' && data && view === 'behavior' && <div className="analytics-reports">
      <Breakdown title={t('Tiêu đề trang', 'Page titles')} items={data.pageTitles || []} labelKey="title" valueKey="pageViews" lang={lang} />
      <Breakdown title={t('Đường dẫn trang', 'Page paths')} items={data.pagePaths || []} labelKey="path" valueKey="pageViews" lang={lang} />
      <Breakdown title={t('Sự kiện', 'Events')} items={data.events || []} labelKey="name" valueKey="eventCount" lang={lang} />
      <section className="admin-panel"><h3>{t('Hoạt động sự kiện mua sắm', 'Ecommerce event activity')}</h3><p>{t('Chỉ là số lần sự kiện được GA4 ghi nhận; chưa đủ điều kiện để lập phễu chuyển đổi có thứ tự.', 'These are GA4 event occurrences, not an ordered conversion funnel.')}</p><dl className="analytics-facts">{['view_item', 'add_to_cart', 'begin_checkout', 'purchase'].map(name => { const item = data.events?.find(event => event.name === name); return <div key={name}><dt>{name}</dt><dd>{item ? formatNumber(item.eventCount) : t('Chưa ghi nhận', 'Not observed')}</dd></div> })}</dl></section>
    </div>}
    {!loading && !error && data?.status !== 'EMPTY' && data && view === 'overview' && <>
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
      <p>{data.comparison?.status === 'AVAILABLE' ? [
        [t('Người dùng', 'Users'), data.comparison.activeUsers],
        [t('Phiên', 'Sessions'), data.comparison.sessions],
        [t('Lượt xem', 'Views'), data.comparison.pageViews]
      ].map(([label, value]) => `${label}: ${value.growthPercent == null ? t('không có cơ sở so sánh', 'no comparison basis') : `${value.growthPercent >= 0 ? '+' : ''}${value.growthPercent.toLocaleString(lang === 'en' ? 'en-US' : 'vi-VN', { maximumFractionDigits: 1 })}%`}`).join(' · ') : t('Chưa có dữ liệu kỳ trước để so sánh.', 'No previous-period data for comparison.')}</p>
      <p>{t('Tỷ lệ thoát là tỷ lệ phiên không tương tác; không phải tỷ lệ khách chỉ xem một trang.', 'Bounce rate is the share of unengaged sessions, not single-page visits.')}</p>
      <div className="analytics-reports">
        <section className="admin-panel analytics-trend">
          <div className="panel-head"><h2>{t('Xu hướng truy cập', 'Visit trend')}</h2><span>{t('Phiên mỗi ngày', 'Sessions per day')}</span></div>
          {dailyRows.length ? <>
            <div className="analytics-bars" role="img" aria-label={t('Biểu đồ số phiên theo ngày', 'Daily sessions chart')}>
              {dailyRows.map(item => <div className="analytics-bar" key={item.date} title={`${item.date}: ${formatNumber(item.sessions)} ${t('phiên', 'sessions')}`}><span style={{ height: `${Math.max(3, Number(item.sessions || 0) / maxDaily * 100)}%` }} /></div>)}
            </div>
            <div className="analytics-axis"><span>{dailyRows[0].date}</span><span>{dailyRows.at(-1).date}</span></div>
            <details><summary>{t('Xem số liệu từng ngày', 'View daily figures')}</summary><div className="table-wrap"><table><thead><tr><th>{t('Ngày', 'Date')}</th><th>{t('Người dùng', 'Users')}</th><th>{t('Phiên', 'Sessions')}</th></tr></thead><tbody>{dailyRows.map(item => <tr key={item.date}><td>{item.date}</td><td>{formatNumber(item.activeUsers)}</td><td>{formatNumber(item.sessions)}</td></tr>)}</tbody></table></div></details>
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

export default function AdminAnalytics({ lang = 'vi' }) {
  const [tab, setTab] = useState('overview')
  return <div>
    <nav className="analytics-tabs" aria-label={lang === 'en' ? 'Analytics views' : 'Chế độ thống kê'}>
      {[["overview", lang === 'en' ? 'Overview' : 'Tổng quan'], ["realtime", lang === 'en' ? 'Realtime' : 'Trực tiếp'], ["audience", lang === 'en' ? 'Audience' : 'Đối tượng'], ["behavior", lang === 'en' ? 'Behavior' : 'Hành vi'], ["business", lang === 'en' ? 'Business' : 'Kinh doanh']].map(([key, label]) => <button type="button" key={key} aria-current={tab === key ? 'page' : undefined} onClick={() => setTab(key)}>{label}</button>)}
    </nav>
    {tab === 'realtime' ? <RealtimeAnalytics lang={lang} /> : tab === 'business' ? <BusinessAnalytics lang={lang} /> : <HistoricalAnalytics lang={lang} view={tab} />}
  </div>
}
