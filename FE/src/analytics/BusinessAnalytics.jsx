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
    [t('Đơn được tạo', 'Orders created'), count(metrics.orders_created)],
    [t('Đơn hoàn tất trong kỳ', 'Orders completed in period'), count(metrics.orders_completed)],
    [t('Đơn hoàn tất đã thanh toán', 'Completed paid orders'), count(metrics.completed_paid_orders)],
    [t('Giá trị đơn hoàn tất đã thanh toán', 'Completed paid order value'), money(metrics.completed_paid_revenue)],
    [t('Giá trị đơn trung bình', 'Average order value'), money(metrics.average_order_value)],
    [t('Sản phẩm đã bán', 'Items sold'), count(metrics.sold_quantity)],
    [t('Tài khoản khách mới', 'New customer accounts'), count(metrics.new_customer_accounts)]
  ]
  const revenue = data?.revenueTrend || []
  const maxRevenue = Math.max(1, ...revenue.map(item => Number(item.revenue || 0)))
  if (compact) return <section className="admin-panel">
    <h2>{t('Kinh doanh · gồm hôm nay', 'Business · includes today')}</h2>
    {loading && <p>{t('Đang tải...', 'Loading...')}</p>}
    {error && <p className="admin-state error" role="alert">{error}</p>}
    {!loading && data && <div className="metric-grid">{[
      [t('Giá trị đơn hoàn tất đã thanh toán', 'Completed paid order value'), money(metrics.completed_paid_revenue)],
      [t('Đơn hoàn tất đã thanh toán', 'Completed paid orders'), count(metrics.completed_paid_orders)],
      [t('Giá trị đơn trung bình', 'Average order value'), money(metrics.average_order_value)],
      [t('Lượt đặt Workshop', 'Workshop bookings'), count(metrics.workshop_bookings)]
    ].map(([label, value]) => <article className="metric" key={label}><span>{label}</span><strong>{value}</strong></article>)}</div>}
  </section>
  return <div className="admin-analytics">
    <section className="admin-panel">
      <div className="panel-head"><h2>{t('Thống kê kinh doanh', 'Business statistics')}</h2><button type="button" onClick={() => { setLoading(true); setRefreshKey(key => key + 1) }}>{t('Làm mới', 'Refresh')}</button></div>
      <label>{t('Khoảng thời gian', 'Date range')} <select value={days} onChange={event => { setLoading(true); setDays(Number(event.target.value)) }}><option value={7}>7 {t('ngày', 'days')}</option><option value={30}>30 {t('ngày', 'days')}</option><option value={90}>90 {t('ngày', 'days')}</option></select></label>
      <p>{t('Nguồn: PostgreSQL. Kỳ này gồm cả hôm nay; trạng thái đơn và thanh toán là trạng thái hiện tại.', 'Source: PostgreSQL. This period includes today; order and payment statuses are current states.')}</p>
      {data?.period && <small>{data.period.fromDate} – {data.period.toDate} · {data.timezone}</small>}
    </section>
    {loading && <div className="admin-state">{t('Đang tải...', 'Loading...')}</div>}
    {error && <div className="admin-state error" role="alert">{error}</div>}
    {!loading && data && <>
      <div className="metric-grid">{cards.map(([label, value]) => <article className="metric" key={label}><span>{label}</span><strong>{value}</strong></article>)}</div>
      <div className="analytics-reports">
        <section className="admin-panel analytics-trend"><h3>{t('Giá trị đơn hoàn tất đã thanh toán theo kỳ', 'Completed paid order value by period')}</h3>{revenue.length ? <><div className="analytics-bars" role="img" aria-label={t('Biểu đồ giá trị đơn', 'Order value chart')}>{revenue.map(item => <div className="analytics-bar" key={item.periodStart} title={`${item.periodStart}: ${money(item.revenue)}`}><span style={{ height: `${Math.max(3, Number(item.revenue || 0) / maxRevenue * 100)}%` }} /></div>)}</div><div className="analytics-axis"><span>{revenue[0].periodStart}</span><span>{revenue.at(-1).periodStart}</span></div></> : <p>{t('Chưa có đơn hoàn tất đã thanh toán trong kỳ.', 'No completed paid orders in this period.')}</p>}</section>
        <section className="admin-panel"><h3>{t('Trạng thái đơn và thanh toán', 'Order and payment status')}</h3><dl className="analytics-facts">{[
          [t('Đơn mới', 'New orders'), metrics.orders_new], [t('Đơn xác nhận', 'Confirmed orders'), metrics.orders_confirmed], [t('Đơn hoàn tất trong nhóm đơn tạo kỳ này', 'Completed among orders created this period'), metrics.orders_completed_created], [t('Đơn hủy', 'Cancelled orders'), metrics.orders_cancelled],
          [t('Thanh toán chờ', 'Pending payments'), metrics.payments_pending], [t('Thanh toán đã nhận', 'Paid payments'), metrics.payments_paid], [t('Thanh toán thất bại', 'Failed payments'), metrics.payments_failed], [t('Thanh toán hết hạn', 'Expired payments'), metrics.payments_expired], [t('Thanh toán đã hoàn', 'Refunded payments'), metrics.payments_refunded]
        ].map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{count(value)}</dd></div>)}</dl></section>
        <section className="admin-panel"><h3>{t('Workshop và đánh giá', 'Workshops and reviews')}</h3><dl className="analytics-facts">{[[t('Lượt đặt Workshop', 'Workshop bookings'), metrics.workshop_bookings], [t('Người đăng ký, trừ booking hủy', 'Booked participants, excluding cancellations'), metrics.workshop_participants], [t('Đánh giá', 'Reviews'), metrics.reviews_total], [t('Đánh giá hiển thị', 'Visible reviews'), metrics.reviews_visible], [t('Điểm trung bình hiển thị', 'Visible average rating'), metrics.visible_average_rating == null ? '—' : metrics.visible_average_rating]].map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{typeof value === 'number' ? count(value) : value}</dd></div>)}</dl><h4>{t('Workshop được quan tâm', 'Workshop interest')}</h4>{data.workshopInterest?.length ? <ol>{data.workshopInterest.map(item => <li key={item.title}>{item.title}: {count(item.bookings)} {t('lượt đặt', 'bookings')} · {count(item.participants)} {t('người đăng ký', 'participants')}</li>)}</ol> : <p>{t('Chưa có lượt đặt trong kỳ.', 'No bookings in this period.')}</p>}<p>{t('Không có dữ liệu thanh toán Workshop; không hiển thị doanh thu Workshop.', 'Workshop payment data is unavailable; Workshop revenue is not shown.')}</p></section>
        <section className="admin-panel"><h3>{t('Vận hành', 'Operations')}</h3><dl className="analytics-facts">{[[t('Sản phẩm sắp hết', 'Low-stock products'), metrics.low_stock_products], [t('Sản phẩm hết hàng', 'Out-of-stock products'), metrics.out_of_stock_products], [t('Hội thoại đang mở', 'Open chats'), metrics.open_chats], [t('Thông báo', 'Notifications'), metrics.notification_count], [t('Lượt gửi thông báo tới Admin', 'Admin notification recipients'), metrics.notification_recipient_count], [t('Đã đọc', 'Read by Admins'), metrics.notification_read_count]].map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{count(value)}</dd></div>)}</dl></section>
        <section className="admin-panel"><h3>{t('Sản phẩm bán chạy hợp lệ', 'Eligible best sellers')}</h3>{data.bestSelling?.length ? <ol>{data.bestSelling.map(item => <li key={item.productId}>{lang === 'en' ? item.productNameEn : item.productNameVi}: {count(item.soldQuantity)}</li>)}</ol> : <p>{t('Chưa có sản phẩm bán từ đơn hoàn tất đã thanh toán.', 'No products sold from completed paid orders yet.')}</p>}</section>
      </div>
      <p>{t('Giá trị thanh toán hiện còn PAID', 'Currently paid payment value')}: {money(metrics.paid_payment_amount)} · {t('Giá trị hoàn tiền hiện tại', 'Currently refunded payment value')}: {money(metrics.refunded_payment_amount)}. {t('Đây không phải doanh thu thuần.', 'These are not net revenue.')}</p>
    </>}
  </div>
}
