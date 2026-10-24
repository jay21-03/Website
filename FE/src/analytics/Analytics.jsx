import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'

const MEASUREMENT_ID = import.meta.env.VITE_GA4_MEASUREMENT_ID
const CONSENT_KEY = 'dx.analytics.consent.v1'
let lastPage = null
let tagStarted = false
const isPreview = () => new URLSearchParams(window.location.search).get('presentationPreview') === '1'
export const isAnalyticsAllowed = () => localStorage.getItem(CONSENT_KEY) === 'granted'
function startTag() {
  if (!MEASUREMENT_ID || !isAnalyticsAllowed() || isPreview() || window.location.pathname.startsWith('/admin')) return false
  if (tagStarted) return true
  window.dataLayer = window.dataLayer || []
  window.gtag = function gtag() { window.dataLayer.push(arguments) }
  window.gtag('consent', 'default', { analytics_storage: 'denied', ad_storage: 'denied', ad_user_data: 'denied', ad_personalization: 'denied' })
  window.gtag('consent', 'update', { analytics_storage: 'granted', ad_storage: 'denied', ad_user_data: 'denied', ad_personalization: 'denied' })
  window.gtag('js', new Date()); window.gtag('config', MEASUREMENT_ID, { send_page_view: false })
  const script = document.createElement('script'); script.async = true; script.src = `https://www.googletagmanager.com/gtag/js?id=${MEASUREMENT_ID}`; document.head.appendChild(script); tagStarted = true; return true
}
export function trackAnalytics(eventName, params = {}) { if (!startTag()) return false; window.gtag('event', eventName, params); return true }
export function useAnalyticsConsent() { const [allowed, setAllowed] = useState(isAnalyticsAllowed); useEffect(() => { const update = () => setAllowed(isAnalyticsAllowed()); window.addEventListener('dx-analytics-consent', update); window.addEventListener('storage', update); return () => { window.removeEventListener('dx-analytics-consent', update); window.removeEventListener('storage', update) } }, []); return allowed }
function ConsentBanner() {
  const [decision, setDecision] = useState(() => ['granted', 'denied'].includes(localStorage.getItem(CONSENT_KEY)) ? localStorage.getItem(CONSENT_KEY) : 'unset')
  const [editing, setEditing] = useState(false)
  const [lang, setLang] = useState(() => localStorage.getItem('dxLang') || 'vi')
  const location = useLocation()
  useEffect(() => { const update = () => setLang(document.documentElement.lang || 'vi'); window.addEventListener('dx-language-change', update); return () => window.removeEventListener('dx-language-change', update) }, [])
  if (location.pathname.startsWith('/admin') || isPreview()) return null
  const t = (vi, en) => lang === 'en' ? en : vi
  function choose(value) {
    localStorage.setItem(CONSENT_KEY, value)
    setDecision(value)
    setEditing(false)
    if (window.gtag) window.gtag('consent', 'update', { analytics_storage: value === 'granted' ? 'granted' : 'denied', ad_storage: 'denied', ad_user_data: 'denied', ad_personalization: 'denied' })
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
export function AnalyticsRoot({ children }) { const location = useLocation(); const allowed = useAnalyticsConsent(); useEffect(() => { if (!allowed) return; const path = location.pathname; if (path.startsWith('/admin') || isPreview()) { lastPage = null; return } if (lastPage === path) return; if (trackAnalytics('page_view', { page_path: path, page_location: `${window.location.origin}${path}`, page_title: document.title })) lastPage = path }, [location.pathname, allowed]); return <>{children}<ConsentBanner /></> }
