import { useState } from 'react'

const copy = (lang, vi, en) => lang === 'vi' ? vi : en

function ContactIcon({ kind }) {
  const paths = {
    address: <><path d="M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0Z" /><circle cx="12" cy="10" r="3" /></>,
    phone: <path d="M22 16v4a2 2 0 0 1-2 2A18 18 0 0 1 2 4a2 2 0 0 1 2-2h4l2 6-3 2a14 14 0 0 0 7 7l2-3Z" />,
    email: <><rect x="2" y="4" width="20" height="16" rx="2" /><path d="m2 6 10 7L22 6" /></>,
    hours: <><circle cx="12" cy="12" r="10" /><path d="M12 6v6l4 2" /></>
  }
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[kind]}</svg>
}

export default function About({ lang, support }) {
  const [topic, setTopic] = useState('order')
  const topics = [
    ['order', 'Đặt hàng', 'Place an order'],
    ['workshop', 'Đặt lịch workshop', 'Book a workshop'],
    ['business', 'Hợp tác B2B', 'B2B partnership'],
    ['other', 'Khác', 'Other']
  ]
  function composeEmail(event) {
    event.preventDefault()
    const fields = new FormData(event.currentTarget)
    const selected = topics.find(item => item[0] === topic)
    const subject = copy(lang, selected[1], selected[2])
    const body = `${copy(lang, 'Họ tên', 'Name')}: ${fields.get('name')}\nEmail: ${fields.get('email')}\n${copy(lang, 'Điện thoại', 'Phone')}: ${fields.get('phone')}\n\n${fields.get('message')}`
    window.location.assign(`mailto:${support.email}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`)
  }
  function requestCatalog() {
    setTopic('business')
    document.getElementById('about-contact-form').scrollIntoView({ block: 'center' })
    document.getElementById('about-name').focus({ preventScroll: true })
  }
  return <main className="about-page">
    <section className="about-hero">
      <div className="about-container">
        <h1>{copy(lang, 'Kết Nối Với Chúng Tôi', 'Connect With Us')}</h1>
        <p>{copy(lang, 'Ghé thăm xưởng, đặt hàng hoặc hẹn lịch trải nghiệm làm gốm.', 'Visit our studio, order a piece, or arrange a pottery experience.')}</p>
      </div>
    </section>
    <section className="about-container about-contact-grid">
      <div>
        <div className="about-section-heading"><span>{copy(lang, 'Thông tin', 'Information')}</span><h2>{copy(lang, 'Xưởng Gốm Đàng Xem', 'Dang Xem Pottery Studio')}</h2></div>
        <ul className="about-details">
          <li><ContactIcon kind="address" /><div><h3>{copy(lang, 'Địa chỉ', 'Address')}</h3>{support.map ? <a href={support.map} target="_blank" rel="noreferrer">{support.address}</a> : <p>{support.address}</p>}</div></li>
          <li><ContactIcon kind="phone" /><div><h3>{copy(lang, 'Điện thoại', 'Phone')}</h3>{support.phones.map(phone => <a key={phone} href={`tel:${phone}`}>{phone}</a>)}</div></li>
          <li><ContactIcon kind="email" /><div><h3>Email</h3><a href={`mailto:${support.email}`}>{support.email}</a></div></li>
          {support.openingHours && <li><ContactIcon kind="hours" /><div><h3>{copy(lang, 'Giờ mở cửa', 'Opening hours')}</h3><p>{support.openingHours}</p></div></li>}
        </ul>
        <div className="about-socials">
          {support.facebook && <a href={support.facebook} target="_blank" rel="noreferrer">Facebook ↗</a>}
          {support.phones[0] && <a href={`https://zalo.me/${support.phones[0]}`} target="_blank" rel="noreferrer">Zalo ↗</a>}
        </div>
      </div>
      <div>
        <div className="about-section-heading"><span>{copy(lang, 'Gửi tin', 'Get in touch')}</span><h2>{copy(lang, 'Biểu Mẫu Liên Hệ', 'Contact Form')}</h2></div>
        <form id="about-contact-form" className="about-form" onSubmit={composeEmail}>
          <div className="about-form-fields">
            <label>{copy(lang, 'Họ tên', 'Full name')}<input id="about-name" name="name" autoComplete="name" maxLength="200" required /></label>
            <label>Email<input name="email" type="email" autoComplete="email" maxLength="320" required /></label>
            <label>{copy(lang, 'Số điện thoại', 'Phone number')}<input name="phone" type="tel" autoComplete="tel" maxLength="32" /></label>
            <label>{copy(lang, 'Chủ đề', 'Topic')}<select name="topic" value={topic} onChange={event => setTopic(event.target.value)}>{topics.map(([value, vi, en]) => <option key={value} value={value}>{copy(lang, vi, en)}</option>)}</select></label>
          </div>
          <label>{copy(lang, 'Tin nhắn', 'Message')}<textarea name="message" rows="5" maxLength="3000" required /></label>
          <p className="about-form-note">{copy(lang, 'Nút bên dưới mở ứng dụng email với nội dung đã điền để bạn kiểm tra và gửi.', 'The button opens your email app with your message ready to review and send.')}</p>
          <button className="button dark" type="submit">{copy(lang, 'Soạn Email Liên Hệ', 'Compose Contact Email')} <span aria-hidden="true">↗</span></button>
        </form>
      </div>
    </section>
    <section className="about-container about-map" aria-label={copy(lang, 'Bản đồ', 'Map')}>
      <iframe title={copy(lang, 'Bản đồ làng Bàu Trúc, Ninh Phước', 'Map of Bau Truc village, Ninh Phuoc')} src="https://www.openstreetmap.org/export/embed.html?bbox=108.90%2C11.50%2C109.10%2C11.65&layer=mapnik" loading="lazy" />
      {support.map && <a href={support.map} target="_blank" rel="noreferrer">{copy(lang, 'Xem đường đến xưởng', 'Get directions to the studio')} ↗</a>}
    </section>
    <section className="about-partners"><div>
      <h2>{copy(lang, 'Dành Cho Đối Tác & Resort', 'For Partners & Resorts')}</h2>
      <p>{copy(lang, 'Chúng tôi cung cấp sản phẩm gốm độc bản cho khách sạn, resort và doanh nghiệp. Liên hệ để nhận báo giá sỉ và catalog sản phẩm.', 'We supply unique pottery for hotels, resorts, and businesses. Contact us for wholesale pricing and a product catalog.')}</p>
      <button type="button" className="button dark" onClick={requestCatalog}>{copy(lang, 'Yêu Cầu Catalog B2B', 'Request B2B Catalog')} <span aria-hidden="true">→</span></button>
    </div></section>
  </main>
}
