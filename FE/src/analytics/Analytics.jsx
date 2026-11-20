import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { analyticsItem } from './ecommerce'

const MEASUREMENT_ID = import.meta.env.VITE_GA4_MEASUREMENT_ID
const CONSENT_KEY = 'dx.analytics.consent.v1'
let lastPage = null
let tagStarted = false
let tagConsentGranted = false
let audience = 'pending'
const isPreview = () => new URLSearchParams(window.location.search).get('presentationPreview') === '1'
const publicPaths = new Set(['/', '/san-pham', '/trai-nghiem', '/lien-he', '/products', '/collections', '/workshop', '/cart', '/checkout', '/orders', '/account', '/ve-chung-toi', '/support', '/faq', '/policy'])
const safePath = path => /^\/products\/\d+$/.test(path) ? path : /^\/orders\/\d+$/.test(path) ? '/orders/detail' : publicPaths.has(path) ? path : '/other'
const safeCampaign = value => /^[\p{L}\p{N}_. -]{1,80}$/u.test(value) && !/\d{7}/.test(value)
export const safeSearchTerm = value => /^[\p{L}\p{N} -]{2,60}$/u.test(value) && !/\d{7}/.test(value)
export function safePageLocation() {
  const url = new URL(window.location.href)
  const safe = new URL(safePath(url.pathname), url.origin)
  for (const key of ['utm_source', 'utm_medium', 'utm_campaign', 'utm_content', 'utm_term', 'gclid', 'gbraid', 'wbraid']) {
    const value = url.searchParams.get(key)
    if (value && (key === 'gclid' || key === 'gbraid' || key === 'wbraid' ? /^[A-Za-z0-9_-]{1,200}$/.test(value) : safeCampaign(value))) safe.searchParams.set(key, value)
  }
  return safe.href
}
export const isAnalyticsAllowed = () => { try { return localStorage.getItem(CONSENT_KEY) === 'granted' } catch { return false } }
function updateTagConsent(granted) {
  if (!tagStarted || tagConsentGranted === granted) return
  try {
    window.gtag('consent', 'update', { analytics_storage: granted ? 'granted' : 'denied', ad_storage: 'denied', ad_user_data: 'denied', ad_personalization: 'denied' })
    tagConsentGranted = granted
  } catch { /* A blocked tag must not interrupt authentication or navigation. */ }
}
export function setAnalyticsIdentity(user) {
  const next = user?.role === 'ADMIN' ? 'admin' : 'visitor'
  if (audience === next) return
  audience = next
  if (next === 'admin') lastPage = null
  updateTagConsent(next === 'visitor' && isAnalyticsAllowed() && !isPreview() && !window.location.pathname.startsWith('/admin'))
  window.dispatchEvent(new Event('dx-analytics-identity'))
}
function startTag() {
  if (!MEASUREMENT_ID || audience !== 'visitor' || !isAnalyticsAllowed() || isPreview() || window.location.pathname.startsWith('/admin')) return false
  if (tagStarted) { updateTagConsent(true); return true }
  window.dataLayer = window.dataLayer || []
  window.gtag = function gtag() { window.dataLayer.push(arguments) }
  window.gtag('consent', 'default', { analytics_storage: 'denied', ad_storage: 'denied', ad_user_data: 'denied', ad_personalization: 'denied' })
  window.gtag('consent', 'update', { analytics_storage: 'granted', ad_storage: 'denied', ad_user_data: 'denied', ad_personalization: 'denied' })
  tagConsentGranted = true
  window.gtag('js', new Date()); window.gtag('config', MEASUREMENT_ID, { send_page_view: false, page_location: safePageLocation(), page_referrer: document.referrer ? new URL(document.referrer).origin : undefined })
  const script = document.createElement('script'); script.async = true; script.src = `https://www.googletagmanager.com/gtag/js?id=${MEASUREMENT_ID}`; document.head.appendChild(script); tagStarted = true; return true
}
export function trackAnalytics(eventName, params = {}) {
  try {
    if (!startTag()) return false
    window.gtag('event', eventName, { ...params, page_location: safePageLocation(), page_referrer: document.referrer ? new URL(document.referrer).origin : undefined })
    return true
  } catch { return false }
}
export function trackPaidPurchase(order) {
  if (order?.paymentStatus !== 'PAID') return false
  const key = `dx.analytics.purchase.${order.id}`
  try { if (localStorage.getItem(key)) return false } catch { return false }
  const sent = trackAnalytics('purchase', { transaction_id: String(order.orderCode || order.id), currency: 'VND', value: Number(order.paymentAmount ?? order.totalAmount), items: (order.items || []).map(item => analyticsItem(item, item.quantity)) })
  if (sent) { try { localStorage.setItem(key, '1') } catch { /* Best-effort measurement must not block payment UI. */ } }
  return sent
}
export function useAnalyticsConsent() { const [allowed, setAllowed] = useState(isAnalyticsAllowed); useEffect(() => { const update = () => setAllowed(isAnalyticsAllowed()); window.addEventListener('dx-analytics-consent', update); window.addEventListener('storage', update); return () => { window.removeEventListener('dx-analytics-consent', update); window.removeEventListener('storage', update) } }, []); return allowed }
function useAnalyticsAudience() { const [current, setCurrent] = useState(audience); useEffect(() => { const update = () => setCurrent(audience); window.addEventListener('dx-analytics-identity', update); update(); return () => window.removeEventListener('dx-analytics-identity', update) }, []); return current }
function ConsentBanner({ currentAudience }) {
  const [decision, setDecision] = useState(() => { try { return ['granted', 'denied'].includes(localStorage.getItem(CONSENT_KEY)) ? localStorage.getItem(CONSENT_KEY) : 'unset' } catch { return 'unset' } })
  const [editing, setEditing] = useState(false)
  const [lang, setLang] = useState(() => { try { return localStorage.getItem('dxLang') || 'vi' } catch { return 'vi' } })
  const location = useLocation()
  useEffect(() => { const update = () => setLang(document.documentElement.lang || 'vi'); window.addEventListener('dx-language-change', update); return () => window.removeEventListener('dx-language-change', update) }, [])
  if (currentAudience !== 'visitor' || location.pathname.startsWith('/admin') || isPreview()) return null
  const t = (vi, en) => lang === 'en' ? en : vi
  function choose(value) {
    try { localStorage.setItem(CONSENT_KEY, value) } catch { return }
    setDecision(value)
    setEditing(false)
    updateTagConsent(value === 'granted' && audience === 'visitor' && !isPreview() && !location.pathname.startsWith('/admin'))
    if (value !== 'granted') lastPage = null
    window.dispatchEvent(new Event('dx-analytics-consent'))
  }
  if (decision !== 'unset' && !editing) return <button className="analytics-settings" type="button" onClick={() => setEditing(true)}>{t('Cài đặt quyền riêng tư', 'Privacy settings')}</button>
  return <section className="analytics-consent" role="dialog" aria-labelledby="analytics-consent-title" aria-describedby="analytics-consent-description">
    <div className="analytics-consent-copy">
      <strong id="analytics-consent-title">{t('Quyền riêng tư của bạn', 'Your privacy matters')}</strong>
      <p id="analytics-consent-description">{t('Chúng tôi chỉ dùng Google Analytics để thống kê lượt truy cập khi bạn đồng ý. Từ chối không ảnh hưởng đến việc mua sắm. Bạn có thể đổi lựa chọn bất kỳ lúc nào.', 'We use Google Analytics to measure visits only if you agree. Declining will not affect shopping. You can change your choice at any time.')}</p>
    </div>
    <div className="analytics-consent-actions">
      <button type="button" onClick={() => choose('denied')}>{t('Từ chối', 'Decline')}</button>
      <button type="button" onClick={() => choose('granted')}>{t('Đồng ý thống kê', 'Allow analytics')}</button>
    </div>
  </section>
}
export function AnalyticsRoot({ children }) {
  const location = useLocation()
  const allowed = useAnalyticsConsent()
  const currentAudience = useAnalyticsAudience()
  useEffect(() => {
    const path = location.pathname
    if (!allowed || currentAudience !== 'visitor' || path.startsWith('/admin') || path === '/about' || path === '/login' || isPreview()) {
      updateTagConsent(false)
      lastPage = null
      return
    }
    const page = safePageLocation()
    const key = `${path}${new URL(page).search}`
    if (lastPage === key) return
    if (trackAnalytics('page_view', { page_path: safePath(path), page_title: document.title })) lastPage = key
  }, [location.pathname, location.search, allowed, currentAudience])
  useEffect(() => {
    const click = event => {
      const link = event.target.closest?.('a[href]')
      if (!link) return
      if (link.href.startsWith('tel:')) trackAnalytics('click_phone')
      else if (new URL(link.href).hostname === 'zalo.me') trackAnalytics('click_zalo')
    }
    document.addEventListener('click', click)
    return () => document.removeEventListener('click', click)
  }, [])
  return <>{children}<ConsentBanner currentAudience={currentAudience} /></>
}
