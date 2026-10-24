import { useEffect, useState } from 'react'
import { api } from '../api'
import RealtimeAnalytics, { Breakdown, friendlyName } from './RealtimeAnalytics'
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
    <div className="admin-panel analytics-toolbar">
      <div className="panel-head"><div><h2>{view === 'overview' ? t('Cửa hàng đang được quan tâm ra sao?', 'How is the store doing?') : view === 'audience' ? t('Khách đến từ đâu?', 'Where do visitors come from?') : t('Khách xem gì và làm gì?', 'What do visitors view and do?')}</h2><p>{t('Lượt truy cập chỉ gồm người đã đồng ý thống kê và được tổng hợp đến hết hôm qua.', 'Visits include people who consented to analytics and are counted through yesterday.')}</p></div><button type="button" onClick={() => { setLoading(true); setRefreshKey(key => key + 1) }}>{t('Làm mới', 'Refresh')}</button></div>
      <label className="analytics-range">{t('Xem', 'Show')} <select value={days} onChange={event => { setLoading(true); setDays(Number(event.target.value)) }}><option value={7}>7 {t('ngày', 'days')}</option><option value={30}>30 {t('ngày', 'days')}</option><option value={90}>90 {t('ngày', 'days')}</option></select> {t('gần nhất đã kết thúc', 'most recent finished days')}</label>
      {data?.fromDate && <small>{data.fromDate} – {data.toDate} · {data.timezone}</small>}
    </div>
    {loading && <div className="admin-state">{t('Đang tải...', 'Loading...')}</div>}
    {error && <div className="admin-state error" role="alert">{error}</div>}
    {!loading && !error && data?.status === 'EMPTY' && <div className="admin-state"><strong>{t('Chưa có số liệu truy cập cho khoảng này', 'No visit data for this period yet')}</strong><p>{t('Hoạt động trong 30 phút gần đây xem ở tab “Đang truy cập”. Báo cáo theo ngày có thể xuất hiện muộn hơn.', 'See the last 30 minutes in “Live visits”. Daily reports may appear later.')}</p></div>}
    {!loading && !error && data?.status !== 'EMPTY' && data && view === 'audience' && <div className="analytics-reports">
      <Breakdown title={t('Khách tìm thấy cửa hàng bằng cách nào?', 'How did visitors find the store?')} items={channels} labelKey="channel" valueKey="sessions" lang={lang} translate description={t('Tính theo lượt truy cập, không phải số người.', 'Counts visits, not unique people.')} />
      <Breakdown title={t('Khách dùng thiết bị nào?', 'Which devices do visitors use?')} items={data.devices || []} labelKey="name" valueKey="activeUsers" lang={lang} translate />
      <Breakdown title={t('Khách ở quốc gia nào?', 'Where are visitors located?')} items={data.countries || []} labelKey="name" valueKey="activeUsers" lang={lang} />
      <details className="analytics-details"><summary>{t('Xem thêm nguồn và thiết bị', 'More about sources and devices')}</summary><div className="analytics-reports"><Breakdown title={t('Nguồn truy cập chi tiết', 'Detailed visit sources')} items={data.sourceMedium || []} labelKey="name" valueKey="sessions" lang={lang} /><Breakdown title={t('Trình duyệt', 'Browsers')} items={data.browsers || []} labelKey="name" valueKey="activeUsers" lang={lang} /><Breakdown title={t('Hệ điều hành', 'Operating systems')} items={data.operatingSystems || []} labelKey="name" valueKey="activeUsers" lang={lang} /></div></details>
    </div>}
    {!loading && !error && data?.status !== 'EMPTY' && data && view === 'behavior' && <div className="analytics-reports">
      <Breakdown title={t('Trang được xem nhiều', 'Most viewed pages')} items={data.pageTitles || []} labelKey="title" valueKey="pageViews" lang={lang} />
      <Breakdown title={t('Các thao tác được ghi nhận', 'Recorded actions')} items={data.events || []} labelKey="name" valueKey="eventCount" lang={lang} translate />
      <section className="admin-panel"><h3>{t('Thao tác mua sắm được ghi nhận', 'Recorded shopping actions')}</h3><p className="analytics-caption">{t('Đây là số lần từng thao tác được ghi nhận; không phải số khách đi qua một quy trình mua hàng.', 'These are counts of each action, not the number of shoppers progressing through checkout.')}</p><dl className="analytics-facts">{['view_item', 'add_to_cart', 'begin_checkout', 'purchase'].map(name => { const item = data.events?.find(event => event.name === name); return <div key={name}><dt title={name}>{friendlyName(name, lang)}</dt><dd>{item ? formatNumber(item.eventCount) : t('Chưa ghi nhận', 'Not observed')}</dd></div> })}</dl></section>
      <details className="analytics-details"><summary>{t('Xem đường dẫn trang', 'View page paths')}</summary><Breakdown title={t('Đường dẫn trang', 'Page paths')} items={data.pagePaths || []} labelKey="path" valueKey="pageViews" lang={lang} /></details>
    </div>}
    {!loading && !error && data?.status !== 'EMPTY' && data && view === 'overview' && <>
      <div className="metric-grid">
        {[
          [t('Người có hoạt động', 'Active visitors'), formatNumber(data.activeUsers), t('Số người Google ghi nhận có hoạt động trong kỳ.', 'People Google recorded as active in this period.')],
          [t('Lượt truy cập', 'Visits'), formatNumber(data.sessions), t('Một người có thể quay lại nhiều lần.', 'One person may visit more than once.')],
          [t('Lượt xem trang', 'Page views'), formatNumber(data.pageViews), t('Một lượt truy cập có thể xem nhiều trang.', 'One visit may include several pages.')]
        ].map(([label, value, help]) => <article className="metric" key={label}><span>{label}</span><strong>{value}</strong><small>{help}</small></article>)}
      </div>
      <section className="admin-panel analytics-comparison"><h3>{t('So với khoảng trước đó', 'Compared with the previous period')}</h3>{data.comparison?.status === 'AVAILABLE' ? <dl className="analytics-facts">{[[t('Người có hoạt động', 'Active visitors'), data.comparison.activeUsers], [t('Lượt truy cập', 'Visits'), data.comparison.sessions], [t('Lượt xem trang', 'Page views'), data.comparison.pageViews]].map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value.growthPercent == null ? t('Chưa đủ dữ liệu', 'Not enough data') : `${value.growthPercent >= 0 ? '+' : ''}${value.growthPercent.toLocaleString(lang === 'en' ? 'en-US' : 'vi-VN', { maximumFractionDigits: 1 })}%`}</dd></div>)}</dl> : <p>{t('Chưa có dữ liệu ở khoảng trước để so sánh.', 'No data from the previous period to compare.')}</p>}</section>
      <div className="analytics-reports">
        <section className="admin-panel analytics-trend">
          <div className="panel-head"><h2>{t('Lượt truy cập theo ngày', 'Visits by day')}</h2><span>{t('Mỗi cột là một ngày', 'One bar per day')}</span></div>
          {dailyRows.length ? <>
            <div className="analytics-bars" role="img" aria-label={t('Biểu đồ lượt truy cập theo ngày', 'Daily visits chart')}>
              {dailyRows.map(item => <div className="analytics-bar" key={item.date} title={`${item.date}: ${formatNumber(item.sessions)} ${t('lượt truy cập', 'visits')}`}><span style={{ height: `${Number(item.sessions || 0) / maxDaily * 100}%` }} /></div>)}
            </div>
            <div className="analytics-axis"><span>{dailyRows[0].date}</span><span>{dailyRows.at(-1).date}</span></div>
            <details><summary>{t('Xem số liệu từng ngày', 'View daily figures')}</summary><div className="table-wrap"><table><thead><tr><th>{t('Ngày', 'Date')}</th><th>{t('Người có hoạt động', 'Active visitors')}</th><th>{t('Lượt truy cập', 'Visits')}</th></tr></thead><tbody>{dailyRows.map(item => <tr key={item.date}><td>{item.date}</td><td>{formatNumber(item.activeUsers)}</td><td>{formatNumber(item.sessions)}</td></tr>)}</tbody></table></div></details>
          </> : <p>{t('Chưa có dữ liệu theo ngày.', 'No daily data yet.')}</p>}
        </section>
        <section className="admin-panel analytics-sources">
          <div className="panel-head"><h2>{t('Khách tìm thấy cửa hàng qua đâu?', 'How did visitors find the store?')}</h2><span>{t('Theo lượt truy cập', 'By visits')}</span></div>
          {channels.length ? <ol>{channels.map(item => <li key={item.channel}><div><span title={item.channel}>{friendlyName(item.channel, lang)}</span><strong>{formatNumber(item.sessions)}</strong></div><div className="analytics-track"><span style={{ width: `${Number(item.sessions || 0) / maxChannel * 100}%` }} /></div></li>)}</ol> : <p>{t('Chưa có dữ liệu nguồn truy cập.', 'No traffic source data yet.')}</p>}
        </section>
      </div>
      <details className="analytics-details"><summary>{t('Xem thêm về mức độ quan tâm', 'More about engagement')}</summary><dl className="analytics-facts"><div><dt>{t('Người cuộn gần hết trang', 'People who scrolled near the end')}</dt><dd>{formatNumber(data.scrolledUsers)}</dd></div><div><dt>{t('Tổng thời gian tương tác', 'Total engagement time')}</dt><dd>{formatNumber(data.engagementSeconds)} {t('giây', 'seconds')}</dd></div><div><dt>{t('Lượt truy cập ít tương tác', 'Visits with little engagement')}</dt><dd>{(Number(data.bounceRate || 0) * 100).toLocaleString(lang === 'en' ? 'en-US' : 'vi-VN', { maximumFractionDigits: 1 })}%</dd></div></dl><p className="analytics-caption">{t('Tỷ lệ cuối là phần lượt truy cập không đạt tiêu chí tương tác của Google; không phải tỷ lệ khách chỉ xem một trang.', 'The last rate is the share of visits that did not meet Google’s engagement criteria, not single-page visits.')}</p></details>
    </>}
    {view === 'overview' && <BusinessAnalytics key={days} lang={lang} compact rangeDays={days} />}
  </div>
}

export default function AdminAnalytics({ lang = 'vi' }) {
  const [tab, setTab] = useState('overview')
  return <div>
    <nav className="analytics-tabs" aria-label={lang === 'en' ? 'Analytics views' : 'Chế độ thống kê'}>
      {[["overview", lang === 'en' ? 'Overview' : 'Tổng quan'], ["realtime", lang === 'en' ? 'Live visits' : 'Đang truy cập'], ["audience", lang === 'en' ? 'Visitors' : 'Khách truy cập'], ["behavior", lang === 'en' ? 'Pages & actions' : 'Trang & thao tác'], ["business", lang === 'en' ? 'Business' : 'Kinh doanh']].map(([key, label]) => <button type="button" key={key} aria-current={tab === key ? 'page' : undefined} onClick={() => setTab(key)}>{label}</button>)}
    </nav>
    {tab === 'realtime' ? <RealtimeAnalytics lang={lang} /> : tab === 'business' ? <BusinessAnalytics lang={lang} /> : <HistoricalAnalytics lang={lang} view={tab} />}
  </div>
}
