import { useEffect, useState } from 'react'
import { api } from '../api'

const count = value => Number(value || 0).toLocaleString('vi-VN')
const money = value => value == null ? '—' : `${BigInt(value).toLocaleString('vi-VN')} ₫`

export default function BusinessAnalytics({ lang, compact = false, rangeDays = 30 }) {
  const [days, setDays] = useState(rangeDays)
  const [refreshKey, setRefreshKey] = useState(0)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const t = (vi, en) => lang === 'en' ? en : vi

  useEffect(() => {
    let active = true
    const controller = new AbortController()
    api.adminAnalyticsBusiness(days, controller.signal).then(value => { if (active) { setData(value); setError('') } })
      .catch(cause => { if (active && !controller.signal.aborted) { setData(null); setError(cause.message) } })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false; controller.abort() }
  }, [days, refreshKey])

  const metrics = data?.metrics || {}
  const cards = [
    [t('Giá trị đơn đã hoàn tất', 'Completed order value'), money(metrics.completed_paid_revenue), t('Chỉ tính đơn hoàn tất và đã thanh toán; chưa trừ phí, hoàn tiền.', 'Only completed, paid orders; before fees and refunds.')],
    [t('Đơn hoàn tất, đã thanh toán', 'Completed, paid orders'), count(metrics.completed_paid_orders), t('Số đơn tạo ra giá trị bên cạnh.', 'Orders included in the value shown here.')],
    [t('Trung bình mỗi đơn', 'Average per order'), money(metrics.average_order_value), t('Chưa có số trung bình nếu chưa có đơn đủ điều kiện.', 'Unavailable until there is a qualifying order.')]
  ]
  const revenue = data?.revenueTrend || []
  const maxRevenue = Math.max(1, ...revenue.map(item => Number(item.revenue || 0)))
  if (compact) return <section className="admin-panel">
    <h2>{t('Bán hàng · tính cả hôm nay', 'Sales · includes today')}</h2>
    {loading && <p>{t('Đang tải...', 'Loading...')}</p>}
    {error && <p className="admin-state error" role="alert">{error}</p>}
    {!loading && data && <div className="metric-grid">{[
      [t('Giá trị đơn đã hoàn tất', 'Completed order value'), money(metrics.completed_paid_revenue)],
      [t('Đơn hoàn tất, đã thanh toán', 'Completed, paid orders'), count(metrics.completed_paid_orders)],
      [t('Lượt đăng ký trải nghiệm', 'Workshop bookings'), count(metrics.workshop_bookings)]
    ].map(([label, value]) => <article className="metric" key={label}><span>{label}</span><strong>{value}</strong></article>)}</div>}
  </section>
  return <div className="admin-analytics">
    <section className="admin-panel analytics-toolbar">
      <div className="panel-head"><div><h2>{t('Kết quả bán hàng', 'Sales results')}</h2><p>{t('Lấy từ đơn hàng của cửa hàng, tính cả hôm nay. Số đơn theo trạng thái hiện tại.', 'From store orders, including today. Order counts use their current status.')}</p></div><button type="button" onClick={() => { setLoading(true); setRefreshKey(key => key + 1) }}>{t('Làm mới', 'Refresh')}</button></div>
      <label className="analytics-range">{t('Xem', 'Show')} <select value={days} onChange={event => { setLoading(true); setDays(Number(event.target.value)) }}><option value={7}>7 {t('ngày', 'days')}</option><option value={30}>30 {t('ngày', 'days')}</option><option value={90}>90 {t('ngày', 'days')}</option></select> {t('gần nhất, gồm hôm nay', 'most recent days, including today')}</label>
      {data?.period && <small>{data.period.fromDate} – {data.period.toDate} · {data.timezone}</small>}
    </section>
    {loading && <div className="admin-state">{t('Đang tải...', 'Loading...')}</div>}
    {error && <div className="admin-state error" role="alert">{error}</div>}
    {!loading && data && <>
      <div className="metric-grid">{cards.map(([label, value, help]) => <article className="metric" key={label}><span>{label}</span><strong>{value}</strong><small>{help}</small></article>)}</div>
      <section className="admin-panel"><h3>{t('Hoạt động cửa hàng trong kỳ', 'Store activity in this period')}</h3><dl className="analytics-facts">{[[t('Đơn được tạo', 'Orders created'), metrics.orders_created], [t('Đơn hoàn tất trong kỳ', 'Orders completed in this period'), metrics.orders_completed], [t('Sản phẩm đã bán từ đơn đủ điều kiện', 'Items sold from qualifying orders'), metrics.sold_quantity], [t('Tài khoản khách mới', 'New customer accounts'), metrics.new_customer_accounts]].map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{count(value)}</dd></div>)}</dl></section>
      <div className="analytics-reports">
        <section className="admin-panel analytics-trend"><h3>{days === 90 ? t('Giá trị đơn theo tuần', 'Order value by week') : t('Giá trị đơn theo ngày', 'Order value by day')}</h3><p className="analytics-caption">{t('Chỉ gồm đơn đã hoàn tất và thanh toán.', 'Only completed, paid orders are included.')}</p>{revenue.length ? <><div className="analytics-bars" role="img" aria-label={t('Biểu đồ giá trị đơn', 'Order value chart')}>{revenue.map(item => <div className="analytics-bar" key={item.periodStart} title={`${item.periodStart}: ${money(item.revenue)}`}><span style={{ height: `${Number(item.revenue || 0) / maxRevenue * 100}%` }} /></div>)}</div><div className="analytics-axis"><span>{revenue[0].periodStart}</span><span>{revenue.at(-1).periodStart}</span></div></> : <p>{t('Chưa có đơn hoàn tất đã thanh toán trong kỳ.', 'No completed paid orders in this period.')}</p>}</section>
        <details className="analytics-details"><summary>{t('Chi tiết trạng thái đơn và thanh toán', 'Order and payment status details')}</summary><dl className="analytics-facts">{[
          [t('Đơn mới', 'New orders'), metrics.orders_new], [t('Đơn xác nhận', 'Confirmed orders'), metrics.orders_confirmed], [t('Đơn hoàn tất trong nhóm đơn tạo kỳ này', 'Completed among orders created this period'), metrics.orders_completed_created], [t('Đơn hủy', 'Cancelled orders'), metrics.orders_cancelled],
          [t('Thanh toán chờ', 'Pending payments'), metrics.payments_pending], [t('Thanh toán đã nhận', 'Paid payments'), metrics.payments_paid], [t('Thanh toán thất bại', 'Failed payments'), metrics.payments_failed], [t('Thanh toán hết hạn', 'Expired payments'), metrics.payments_expired], [t('Thanh toán đã hoàn', 'Refunded payments'), metrics.payments_refunded]
        ].map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{count(value)}</dd></div>)}</dl></details>
        <section className="admin-panel"><h3>{t('Trải nghiệm làm gốm và đánh giá', 'Workshops and reviews')}</h3><dl className="analytics-facts">{[[t('Lượt đăng ký trải nghiệm', 'Workshop bookings'), metrics.workshop_bookings], [t('Người đăng ký, không tính lượt hủy', 'Booked participants, excluding cancellations'), metrics.workshop_participants], [t('Đánh giá sản phẩm', 'Product reviews'), metrics.reviews_total], [t('Đánh giá đang hiển thị', 'Visible reviews'), metrics.reviews_visible], [t('Điểm trung bình đang hiển thị', 'Visible average rating'), metrics.visible_average_rating == null ? '—' : metrics.visible_average_rating]].map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{typeof value === 'number' ? count(value) : value}</dd></div>)}</dl><h4>{t('Trải nghiệm được quan tâm', 'Workshop interest')}</h4>{data.workshopInterest?.length ? <ol>{data.workshopInterest.map(item => <li key={item.title}>{item.title}: {count(item.bookings)} {t('lượt đăng ký', 'bookings')} · {count(item.participants)} {t('người đăng ký', 'participants')}</li>)}</ol> : <p>{t('Chưa có lượt đăng ký trong kỳ.', 'No bookings in this period.')}</p>}<p className="analytics-caption">{t('Đăng ký trải nghiệm chưa có dữ liệu thanh toán, nên không tính vào giá trị đơn.', 'Workshop bookings have no payment record and are not included in order value.')}</p></section>
        <section className="admin-panel"><h3>{t('Việc cần chú ý', 'Things to watch')}</h3><dl className="analytics-facts">{[[t('Sản phẩm sắp hết', 'Low-stock products'), metrics.low_stock_products], [t('Sản phẩm hết hàng', 'Out-of-stock products'), metrics.out_of_stock_products], [t('Hội thoại đang mở', 'Open chats'), metrics.open_chats], [t('Thông báo đã tạo', 'Notifications created'), metrics.notification_count], [t('Lượt gửi đến quản trị viên', 'Deliveries to administrators'), metrics.notification_recipient_count], [t('Lượt đã đọc', 'Read deliveries'), metrics.notification_read_count]].map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{count(value)}</dd></div>)}</dl></section>
        <section className="admin-panel"><h3>{t('Sản phẩm bán chạy', 'Best-selling products')}</h3>{data.bestSelling?.length ? <ol>{data.bestSelling.map(item => <li key={item.productId}>{lang === 'en' ? item.productNameEn : item.productNameVi}: {count(item.soldQuantity)}</li>)}</ol> : <p>{t('Chưa có sản phẩm bán từ đơn hoàn tất đã thanh toán.', 'No products sold from completed paid orders yet.')}</p>}</section>
      </div>
      <details className="analytics-details"><summary>{t('Xem thêm về tiền đã nhận và hoàn trả', 'More about payments and refunds')}</summary><dl className="analytics-facts"><div><dt>{t('Giá trị khoản thanh toán đang ở trạng thái đã nhận', 'Value of payments currently marked paid')}</dt><dd>{money(metrics.paid_payment_amount)}</dd></div><div><dt>{t('Giá trị khoản thanh toán đã hoàn', 'Value of refunded payments')}</dt><dd>{money(metrics.refunded_payment_amount)}</dd></div></dl><p className="analytics-caption">{t('Hai số này không phải lợi nhuận hay doanh thu sau khi trừ hoàn tiền.', 'These are not profit or revenue net of refunds.')}</p></details>
    </>}
  </div>
}
