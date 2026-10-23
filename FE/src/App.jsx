import { Fragment, createContext, useContext, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Bell } from 'lucide-react'
import { Link, Navigate, Route, Routes, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { api } from './api'
import Admin from './Admin'
import { ProductReviews } from './ProductReviews'
import ReferenceHome from './design/Home'
import { ExperienceIntro } from './design/ExperienceIntro'
import About from './design/About'
import { SiteHeader } from './design/SiteHeader'
import { MobileActionBar } from './design/MobileActionBar'
import { PageHero } from './design/PageHero'
import { ThemeProvider, useTheme } from './design/theme'
import { DesignContext } from './design/i18n'
import { validateCheckout } from './domain/checkout'
import { useCheckout, useMyOrder, useMyOrders } from './hooks/useOrders'
import { addGuestQuantity, guestCartFromProducts, hydrateGuestCart, mergeGuestCart, readGuestCart, setGuestQuantity, writeGuestCart } from './services/guestCart'
import { businessLocalDateTimeToOffset } from './utils/businessDate'
import { formatBusinessDateTime, formatCurrencyVnd } from './utils/format'
import { orderStatusLabel, paymentStatusLabel } from './utils/status'
import { formatNotification } from './utils/notifications'
import { presentationNode, presentationDefaults, presentationMode, presentationPalette, fontPresets, contrastText, visibleCheckoutFields } from './design/sitePresentation'

const Store = createContext(null)
const fallbackImage = '/assets/images/vase.jpg'
const money = formatCurrencyVnd
const date = formatBusinessDateTime
const pick = (lang, vi, en) => lang === 'vi' ? vi : en
const contact = {
  email: 'dangxembautrucpottery@gmail.com',
  phones: ['0343478155', '0966477160'],
  facebook: 'https://www.facebook.com/share/18jwSfSPD7/?mibextid=wwXIfr',
  address: '35 Bàu Trúc, thôn Vĩnh Thuận, xã Ninh Phước, tỉnh Khánh Hòa',
  map: 'https://www.google.com/maps/search/?api=1&query=35%20B%C3%A0u%20Tr%C3%BAc%2C%20th%C3%B4n%20V%C4%A9nh%20Thu%E1%BA%ADn%2C%20x%C3%A3%20Ninh%20Ph%C6%B0%E1%BB%9Bc%2C%20t%E1%BB%89nh%20Kh%C3%A1nh%20H%C3%B2a'
}
const supportToContact = settings => settings ? {
  email: settings.email,
  phones: [settings.zaloPhone, settings.secondaryPhone].filter(Boolean),
  facebook: settings.facebookUrl,
  address: settings.address,
  map: settings.mapUrl,
  openingHours: settings.openingHours
} : contact
const emptyCart = () => ({ items: [], totalAmount: 0 })
const mergeStoredCart = () => mergeGuestCart({ loadCart: api.cart, addItem: api.addCart })
const CHECKOUT_INTENT_KEY = 'bautruc.checkout-intent'
const waitForStateCommit = () => new Promise(resolve => window.setTimeout(resolve, 0))

function StoreProvider({ children }) {
  const preview = new URLSearchParams(window.location.search).get('presentationPreview') === '1'
  const { theme } = useTheme()
  const navigate = useNavigate()
  const navigateRef = useRef(navigate)
  navigateRef.current = navigate
  const homepageQuery = useQuery({ queryKey: ['home'], queryFn: api.home })
  const presentationQuery = useQuery({ queryKey: ['site-presentation'], queryFn: api.sitePresentation })
  const [draftPresentation, setDraftPresentation] = useState(null)
  const [lang, setLang] = useState(() => preview ? new URLSearchParams(window.location.search).get('lang') || 'vi' : localStorage.getItem('dxLang') || 'vi')
  const rawPresentation = draftPresentation || presentationQuery.data
  const activeContent = useMemo(() => presentationMode(rawPresentation?.content || presentationDefaults, theme), [rawPresentation, theme])
  const presentation = useMemo(() => {
    const media = { ...rawPresentation?.media }
    if (theme === 'dark') ['SITE_LOGO', 'CATALOG_HERO', 'WORKSHOP_HERO', 'SUPPORT_HERO', 'CHECKOUT_HERO'].forEach(slot => {
      media[slot] = media[`${slot}_DARK`] || media[slot]
    })
    return { ...rawPresentation, content: activeContent, media }
  }, [rawPresentation, activeContent, theme])
  useEffect(() => {
    if (!preview) return undefined
    const receive = event => {
      if (event.origin === window.location.origin && event.source === window.parent && event.data?.type === 'site-presentation-preview') {
        setDraftPresentation({ content: event.data.content, media: event.data.media })
      }
    }
    window.addEventListener('message', receive)
    window.parent.postMessage({ type: 'site-presentation-preview-ready' }, window.location.origin)
    return () => window.removeEventListener('message', receive)
  }, [preview])
  const [products, setProducts] = useState([]), [collections, setCollections] = useState([])
  const [cart, setCart] = useState({ items: [], totalAmount: 0 }), [user, setUser] = useState(null)
  const [support, setSupport] = useState(contact)
  const [catalogLoading, setCatalogLoading] = useState(true), [toast, setToast] = useState('')
  const [authLoading, setAuthLoading] = useState(true)
  const [googleReady, setGoogleReady] = useState(Boolean(window.google?.accounts?.id))
  const langRef = useRef(lang)
  langRef.current = lang
  const notify = useCallback(message => { setToast(message); window.setTimeout(() => setToast(''), 2500) }, [])
  const googleCallbackRef = useRef(null)
  const googleLoginInFlightRef = useRef(false)
  useEffect(() => {
    let active = true
    Promise.all([api.products(), api.collections()]).then(([page, groups]) => {
      if (active) { setProducts(page.content); setCollections(groups) }
    }).catch(error => { if (active) notify(error.message) }).finally(() => { if (active) setCatalogLoading(false) })
    api.supportSettings().then(settings => { if (active) setSupport(supportToContact(settings)) }).catch(() => {})
    async function initializeCart() {
      let current
      try { current = await api.me() } catch {
        const guestCart = await hydrateGuestCart(api.product)
        if (!active) return
        setUser(null)
        setCart(guestCart)
        setAuthLoading(false)
        return
      }
      if (!active) return
      setUser(current)
      if (current.role !== 'USER') setCart(emptyCart())
      else {
        try {
          const result = await mergeStoredCart()
          if (!active) return
          setCart(result.cart)
          if (result.failed) notify(pick(langRef.current, 'Một số sản phẩm trong giỏ tạm chưa thể hợp nhất do giá hoặc tồn kho đã thay đổi.', 'Some guest-cart items could not be merged because price or inventory changed.'))
          else if (sessionStorage.getItem(CHECKOUT_INTENT_KEY) === 'checkout') {
            sessionStorage.removeItem(CHECKOUT_INTENT_KEY)
            if (result.cart.items.length) {
              await waitForStateCommit()
              navigateRef.current('/checkout', { replace: true })
            }
          }
        } catch (error) {
          let accountCart
          try { accountCart = await api.cart() } catch { accountCart = emptyCart() }
          if (!active) return
          setCart(accountCart)
          notify(error.message)
        }
      }
      if (active) setAuthLoading(false)
    }
    initializeCart()
    return () => { active = false }
  }, [notify])
  useEffect(() => { if (!preview) localStorage.setItem('dxLang', lang); document.documentElement.lang = lang }, [lang, preview])
  useEffect(() => {
    const root = document.documentElement
    const colors = presentationPalette(activeContent)
    const font = fontPresets[activeContent.fontPreset] || fontPresets.heritage
    const values = {
      '--background': colors.background, '--card': colors.card, '--primary': colors.primary,
      '--foreground': colors.text, '--wood': colors.secondary, '--gold': colors.accent,
      '--clay': colors.primary, '--accent': colors.accent, '--ring': colors.primary,
      '--card-foreground': colors.text, '--popover': colors.card, '--popover-foreground': colors.text,
      '--secondary': colors.card, '--secondary-foreground': colors.text, '--muted': colors.card,
      '--muted-foreground': colors.secondary, '--border': `color-mix(in srgb, ${colors.secondary} 35%, ${colors.background})`,
      '--input': `color-mix(in srgb, ${colors.secondary} 35%, ${colors.background})`,
      '--primary-foreground': contrastText(colors.primary), '--accent-foreground': contrastText(colors.accent),
      '--ink': colors.text, '--white': colors.background, '--soft': colors.card,
      '--line': `color-mix(in srgb, ${colors.secondary} 35%, ${colors.background})`,
      '--font-display': `"${font.heading}", "Be Vietnam Pro", sans-serif`,
      '--font-sans': `"${font.body}", system-ui, sans-serif`,
      '--font-serif': activeContent.fontPreset === 'heritage' ? '"Cormorant Garamond", "Be Vietnam Pro", serif' : `"${font.heading}", serif`
    }
    root.dataset.colorPreset = activeContent.colorPreset || 'clay'
    root.dataset.fontPreset = activeContent.fontPreset || 'heritage'
    Object.entries(values).forEach(([key, value]) => root.style.setProperty(key, value))
    return () => Object.keys(values).forEach(key => root.style.removeProperty(key))
  }, [activeContent])
  googleCallbackRef.current = async credential => {
    if (googleLoginInFlightRef.current) return
    googleLoginInFlightRef.current = true
    const checkoutIntent = sessionStorage.getItem(CHECKOUT_INTENT_KEY) === 'checkout'
    try {
      const result = await api.googleLogin(credential)
      setUser(result.user)
      if (result.user.role !== 'USER') {
        sessionStorage.removeItem(CHECKOUT_INTENT_KEY)
        setCart(emptyCart())
        notify(pick(langRef.current, 'Tài khoản quản trị không thể thanh toán.', 'Admin accounts cannot checkout.'))
        await waitForStateCommit()
        navigate('/admin', { replace: true })
        return
      }
      const merged = await mergeStoredCart()
      setCart(merged.cart)
      if (merged.failed) {
        notify(pick(langRef.current, 'Chưa thể đồng bộ toàn bộ giỏ hàng. Giỏ tạm vẫn được giữ để bạn thử lại.', 'The cart could not be fully merged. Your guest cart is preserved for retry.'))
        return
      }
      notify(pick(langRef.current, 'Đăng nhập thành công.', 'Signed in successfully.'))
      if (checkoutIntent && merged.cart.items.length) {
        sessionStorage.removeItem(CHECKOUT_INTENT_KEY)
        await waitForStateCommit()
        navigate('/checkout')
      }
    } catch (error) {
      notify(error.message)
    } finally {
      googleLoginInFlightRef.current = false
    }
  }
  useEffect(() => {
    const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID
    if (!clientId) return
    const initialize = () => {
      window.google.accounts.id.initialize({ client_id: clientId, callback: response => googleCallbackRef.current(response.credential), auto_select: false, cancel_on_tap_outside: false })
      setGoogleReady(true)
    }
    if (window.google?.accounts?.id) initialize()
    else {
      let script = document.querySelector('script[data-google-identity]')
      if (!script) { script = document.createElement('script'); script.src = 'https://accounts.google.com/gsi/client'; script.async = true; script.dataset.googleIdentity = 'true'; document.head.appendChild(script) }
      script.addEventListener('load', initialize)
      return () => script.removeEventListener('load', initialize)
    }
  }, [])
  const prepareCheckoutLogin = useCallback(() => {
    if (!cart.items.length) {
      notify(pick(lang, 'Giỏ hàng đang trống.', 'Your cart is empty.'))
      return false
    }
    sessionStorage.setItem(CHECKOUT_INTENT_KEY, 'checkout')
    return true
  }, [cart.items.length, lang, notify])
  function startGoogleLogin(forCheckout = false) {
    if (forCheckout) {
      if (!prepareCheckoutLogin()) return
    } else sessionStorage.removeItem(CHECKOUT_INTENT_KEY)
    if (!import.meta.env.VITE_GOOGLE_CLIENT_ID) return notify(pick(lang, 'Chưa cấu hình Google Client ID.', 'Google Client ID is not configured.'))
    if (!googleReady) return notify(pick(lang, 'Google đang tải, vui lòng thử lại.', 'Google is loading, please try again.'))
    window.google.accounts.id.prompt(notification => {
      if (notification.isNotDisplayed?.() || notification.isSkippedMoment?.()) notify(pick(lang, 'Google không thể mở hộp đăng nhập. Vui lòng thử lại trên trình duyệt hỗ trợ Google.', 'Google could not open sign-in. Please retry in a browser that supports Google Sign-In.'))
    })
  }
  async function add(product) {
    const productId = product.id
    if (!user) {
      const entries = addGuestQuantity(readGuestCart(), productId)
      writeGuestCart(entries)
      setCart(current => guestCartFromProducts(entries, [...current.items, product]))
      notify(pick(lang, 'Đã thêm vào giỏ hàng.', 'Added to cart.'))
      return true
    }
    if (user.role !== 'USER') { notify(pick(lang, 'Tài khoản quản trị không có giỏ hàng.', 'Admin accounts do not have carts.')); return false }
    try { setCart(await api.addCart(productId)); notify(pick(lang, 'Đã thêm vào giỏ hàng.', 'Added to cart.')); return true } catch (error) { notify(error.message); return false }
  }
  return <Store.Provider value={{ products, collections, cart, setCart, user, setUser, support, setSupport, homepage: homepageQuery.data, presentation, catalogLoading, authLoading, googleReady, startGoogleLogin, prepareCheckoutLogin, add, notify, lang, setLang }}>{children}{toast && <div className="toast show">{toast}</div>}</Store.Provider>
}
const useStore = () => useContext(Store)

function Layout({ children }) {
  const { cart, user, lang, setLang, support, presentation } = useStore()
  const { pathname } = useLocation()
  useEffect(() => { window.scrollTo(0, 0) }, [pathname])
  const account = user ? <>{user.role === 'USER' && <CustomerNotificationLink userId={user.id} lang={lang} />}<Link className="icon-btn" to={user.role === 'ADMIN' ? '/admin' : '/account'}>{user.fullName || pick(lang, 'Tài khoản', 'Account')}</Link></> : <DirectGoogleLogin />
  return <DesignContext.Provider value={{ lang, setLang, t: (vi, en) => pick(lang, vi, en), count: cart.items.reduce((sum, item) => sum + item.quantity, 0) }}>
    <SiteHeader account={account} phone={support.phones[0]} brandName={presentationNode(presentation?.content, 'brand', 'name', lang)} logoUrl={presentation?.media?.SITE_LOGO} menuLabels={{ booking: presentationNode(presentation?.content, 'brand', 'menuBooking', lang), call: presentationNode(presentation?.content, 'brand', 'menuCall', lang) }} navLabels={Object.fromEntries([['/', 'navHome'], ['/san-pham', 'navProducts'], ['/trai-nghiem', 'navExperience'], ['/ve-chung-toi', 'navAbout'], ['/lien-he', 'navContact']].map(([route, key]) => [route, presentationNode(presentation?.content, 'brand', key, lang)]))} />
    {children}
    <BilingualFooter />
    <MobileActionBar phone={support.phones[0]} presentation={presentation?.content} />
    <div className="floating-chat">{support.phones[0] && <a className="zalo" href={`https://zalo.me/${support.phones[0]}`} target="_blank" rel="noreferrer" aria-label="Zalo">Z</a>}{support.facebook && <a className="facebook" href={support.facebook} target="_blank" rel="noreferrer" aria-label="Facebook">f</a>}</div>
  </DesignContext.Provider>
}
function CustomerNotificationLink({ userId, lang }) {
  // shortcut: the unread count can lag by 30 seconds, add customer push events if instant updates are needed.
  const unread = useQuery({ queryKey: ['my-notifications', userId, 'unread'], queryFn: () => api.myNotifications(0, false), refetchInterval: 30000 })
  const count = unread.data?.totalElements || 0
  return <Link to="/notifications" aria-label={pick(lang, `Thông báo${count ? `, ${count} chưa đọc` : ''}`, `Notifications${count ? `, ${count} unread` : ''}`)} className="relative rounded-sm border border-border p-2.5 text-foreground/80 hover:border-primary hover:text-primary"><Bell className="h-4 w-4" />{count > 0 && <span className="absolute -right-2 -top-2 min-w-5 rounded-full bg-primary px-1.5 py-0.5 text-center text-[10px] text-primary-foreground">{count}</span>}</Link>
}

function CustomerNotifications() {
  const { user, lang, authLoading, notify } = useStore()
  const [pageNumber, setPageNumber] = useState(0)
  const queryClient = useQueryClient()
  const list = useQuery({ queryKey: ['my-notifications', user?.id, pageNumber], queryFn: () => api.myNotifications(pageNumber), enabled: user?.role === 'USER' })
  async function markRead(id) {
    try {
      await api.readMyNotification(id)
      queryClient.invalidateQueries({ queryKey: ['my-notifications', user.id] })
    } catch (error) { notify(error.message) }
  }
  const page = list.data
  return <Layout><main className="page"><div className="page-title"><span className="kicker">{pick(lang, 'Tài khoản', 'Account')}</span><h1>{pick(lang, 'Thông báo của tôi', 'My notifications')}</h1></div>{authLoading ? <Loading text={pick(lang, 'Đang tải...', 'Loading...')} /> : !user || user.role !== 'USER' ? <Empty>{pick(lang, 'Vui lòng đăng nhập bằng tài khoản khách hàng.', 'Please sign in with a customer account.')}</Empty> : list.isLoading ? <Loading text={pick(lang, 'Đang tải thông báo...', 'Loading notifications...')} /> : list.isError ? <div className="state-box error" role="alert">{list.error.message}<button onClick={() => list.refetch()}>{pick(lang, 'Thử lại', 'Retry')}</button></div> : !page?.content.length ? <Empty>{pick(lang, 'Bạn chưa có thông báo.', 'You have no notifications yet.')}</Empty> : <><div className="mx-auto grid max-w-3xl gap-3">{page.content.map(item => { const copy = formatNotification(item, lang); return <article key={item.id} className={`rounded-sm border border-border p-4 ${item.isRead ? '' : 'bg-primary/5'}`}><b>{copy.title}</b><p>{copy.message}</p><small>{date(item.createdAt, lang)}</small><div className="mt-3 flex gap-4"><Link to={item.referenceType === 'ORDER' ? `/orders/${item.referenceId}` : '/orders'}>{pick(lang, 'Xem đơn hàng', 'View orders')}</Link>{!item.isRead && <button type="button" onClick={() => markRead(item.id)}>{pick(lang, 'Đánh dấu đã đọc', 'Mark as read')}</button>}</div></article> })}</div>{page.totalPages > 1 && <nav className="pagination" aria-label={pick(lang, 'Phân trang thông báo', 'Notification pagination')}><button disabled={page.first} onClick={() => setPageNumber(value => value - 1)}>{pick(lang, 'Trang trước', 'Previous')}</button><span>{pick(lang, 'Trang', 'Page')} {page.page + 1}/{page.totalPages}</span><button disabled={page.last} onClick={() => setPageNumber(value => value + 1)}>{pick(lang, 'Trang sau', 'Next')}</button></nav>}</>}</main></Layout>
}

function DirectGoogleLogin() {
  const { lang, startGoogleLogin } = useStore()
  return <button className="icon-btn direct-login" type="button" onClick={() => startGoogleLogin(false)}>{pick(lang, 'Đăng nhập', 'Sign in')}</button>
}

function BilingualFooter() {
  const { lang, support, homepage, presentation } = useStore()
  const slogan = pick(lang, homepage?.sloganVi || 'Tinh hoa gốm Chăm – Gìn giữ hồn di sản', homepage?.sloganEn || 'The essence of Cham pottery – preserving the soul of heritage')
  return <footer className="site-footer"><div className="footer-grid"><div><Link className="brand" to="/">{presentation?.media?.SITE_LOGO && <img src={presentation.media.SITE_LOGO} alt="" className="presentation-footer-logo" onError={event => { event.currentTarget.hidden = true }} />}{presentationNode(presentation?.content, 'brand', 'name', lang)}</Link><p className="footer-slogan">{slogan}</p><p>{presentationNode(presentation?.content, 'brand', 'footerDescription', lang)}</p></div><div className="footer-col"><h3>{presentationNode(presentation?.content, 'brand', 'footerLinksTitle', lang)}</h3><Link to="/products">{presentationNode(presentation?.content, 'brand', 'navProducts', lang)}</Link><Link to="/workshop">{presentationNode(presentation?.content, 'brand', 'navExperience', lang)}</Link><Link to="/cart">{presentationNode(presentation?.content, 'brand', 'footerCart', lang)}</Link><Link to="/orders">{presentationNode(presentation?.content, 'brand', 'footerOrders', lang)}</Link><Link to="/ve-chung-toi">{presentationNode(presentation?.content, 'brand', 'navAbout', lang)}</Link><Link to="/support">{presentationNode(presentation?.content, 'brand', 'footerSupport', lang)}</Link><Link to="/faq">{presentationNode(presentation?.content, 'faq', 'title', lang)}</Link><Link to="/policy">{presentationNode(presentation?.content, 'brand', 'footerPolicy', lang)}</Link></div><div className="footer-col"><h3>{presentationNode(presentation?.content, 'brand', 'footerContactTitle', lang)}</h3><a href={`mailto:${support.email}`}>{support.email}</a>{support.phones.map(phone => <a key={phone} href={`https://zalo.me/${phone}`} target="_blank" rel="noreferrer">Zalo: {phone}</a>)}{support.facebook && <a href={support.facebook} target="_blank" rel="noreferrer">Facebook</a>}{support.map && <a href={support.map} target="_blank" rel="noreferrer">{support.address}</a>}{support.openingHours && <p>{support.openingHours}</p>}</div></div></footer>
}
function Loading({ text = 'Đang tải dữ liệu...' }) { return <div className="state-box">{text}</div> }
function Empty({ children }) { return <div className="state-box">{children}</div> }

function CatalogCard({ product }) {
  const { lang } = useStore()
  const name = lang === 'vi' ? product.nameVi : product.nameEn
  return <article className="product-card catalog-card">
    <Link className="product-image" to={`/products/${product.id}`}><img src={product.thumbnailUrl || fallbackImage} alt={name} loading="lazy" width="700" height="700" onError={event => { if (!event.currentTarget.src.endsWith(fallbackImage)) event.currentTarget.src = fallbackImage }} /></Link>
    <div className="product-info">
      <h3>{name}</h3><p className="catalog-translation">{lang === 'vi' ? product.nameEn : product.nameVi}</p>
      <p className="catalog-description">{(lang === 'vi' ? product.descriptionVi : product.descriptionEn) || pick(lang, 'Gốm Bàu Trúc được tạo hình bằng tay theo kỹ thuật truyền thống của người Chăm.', 'Bau Truc pottery shaped by hand using traditional Cham techniques.')}</p>
      <p className="catalog-price">{money(product.sellingPrice, lang)}{product.sellingPrice < product.basePrice && <s>{money(product.basePrice, lang)}</s>}</p>
      <Link className="catalog-more" to={`/products/${product.id}`}>{pick(lang, 'Tìm Hiểu Thêm', 'Learn More')} <span aria-hidden="true">→</span></Link>
    </div>
  </article>
}
function Home() {
  const { products, homepage, presentation } = useStore()
  return <Layout><div className="reference-page"><ReferenceHome home={homepage} fallbackProducts={products} presentation={presentation?.content} /></div></Layout>
}
function Products() {
  const { collections, lang, presentation } = useStore(); const [searchParams] = useSearchParams(); const [filters, setFilters] = useState(() => ({ keyword: searchParams.get('keyword') || '', collectionId: searchParams.get('collectionId') || '', minPrice: searchParams.get('minPrice') || '', maxPrice: searchParams.get('maxPrice') || '', page: Number(searchParams.get('page') || 0), size: 20, sort: searchParams.get('sort') || 'createdAt,desc' }))
  const copy = key => presentationNode(presentation?.content, 'catalog', key, lang)
  const updateFilter = changes => setFilters(value => ({ ...value, ...changes, page: changes.page ?? 0 }))
  const catalog = useQuery({ queryKey: ['products', filters], queryFn: () => api.products(filters), placeholderData: previous => previous })
  const page = catalog.data
  return <Layout><main className="page catalog-page"><section className="catalog-hero" style={presentation?.media?.CATALOG_HERO ? { backgroundImage: `linear-gradient(rgba(251,246,236,.86), rgba(251,246,236,.86)), url("${presentation.media.CATALOG_HERO}")` } : undefined}><div><h1>{copy('title')}</h1><p>{copy('subtitle')}</p></div></section><div className="catalog-content"><nav className="collection-tabs" aria-label={pick(lang, 'Lọc bộ sưu tập', 'Filter collections')}><button type="button" aria-pressed={!filters.collectionId} onClick={() => updateFilter({ collectionId: '' })}>{pick(lang, 'Tất Cả', 'All')}</button>{collections.map(item => <button type="button" key={item.id} aria-pressed={String(filters.collectionId) === String(item.id)} onClick={() => updateFilter({ collectionId: String(item.id) })}>{lang === 'vi' ? item.nameVi : item.nameEn}</button>)}</nav><section className="catalog-layout"><details className="catalog-advanced"><summary>{pick(lang, 'Tìm kiếm & bộ lọc', 'Search & filters')}</summary><aside className="filters open"><label>{pick(lang, 'Tìm kiếm', 'Search')}</label><input value={filters.keyword} onChange={event => updateFilter({ keyword: event.target.value })} placeholder={pick(lang, 'Tên sản phẩm...', 'Product name...')} /><label>{pick(lang, 'Bộ sưu tập', 'Collection')}</label><select value={filters.collectionId} onChange={event => updateFilter({ collectionId: event.target.value })}><option value="">{pick(lang, 'Tất cả', 'All')}</option>{collections.map(item => <option key={item.id} value={item.id}>{lang === 'vi' ? item.nameVi : item.nameEn}</option>)}</select><label>{pick(lang, 'Giá từ', 'Min price')}</label><input inputMode="numeric" value={filters.minPrice} onChange={event => updateFilter({ minPrice: event.target.value.replace(/\D/g, '') })} /><label>{pick(lang, 'Giá đến', 'Max price')}</label><input inputMode="numeric" value={filters.maxPrice} onChange={event => updateFilter({ maxPrice: event.target.value.replace(/\D/g, '') })} /><label>{pick(lang, 'Sắp xếp', 'Sort')}</label><select value={filters.sort} onChange={event => updateFilter({ sort: event.target.value })}><option value="createdAt,desc">{pick(lang, 'Mới nhất', 'Newest')}</option><option value="sellingPrice,asc">{pick(lang, 'Giá thấp → cao', 'Price low → high')}</option><option value="sellingPrice,desc">{pick(lang, 'Giá cao → thấp', 'Price high → low')}</option><option value="nameVi,asc">{pick(lang, 'Tên A → Z', 'Name A → Z')}</option></select></aside></details><div>{catalog.isLoading ? <Loading text={pick(lang, 'Đang tải dữ liệu...', 'Loading data...')} /> : catalog.isError ? <div className="state-box error" role="alert">{catalog.error.message}<button onClick={() => catalog.refetch()}>{pick(lang, 'Thử lại', 'Retry')}</button></div> : !page?.content.length ? <Empty>{pick(lang, 'Không tìm thấy sản phẩm phù hợp.', 'No matching products found.')}</Empty> : <><div className="results-bar"><span>{page.totalElements} {pick(lang, 'sản phẩm', 'products')}</span>{catalog.isFetching && <span>{pick(lang, 'Đang cập nhật...', 'Refreshing...')}</span>}</div><div className="product-grid">{page.content.map(product => <CatalogCard key={product.id} product={product} />)}</div>{page.totalPages > 1 && <nav className="pagination" aria-label={pick(lang, 'Phân trang sản phẩm', 'Product pagination')}><button disabled={page.first || catalog.isFetching} onClick={() => updateFilter({ page: page.page - 1 })}>{pick(lang, 'Trang trước', 'Previous')}</button><span>{pick(lang, 'Trang', 'Page')} {page.page + 1}/{page.totalPages}</span><button disabled={page.last || catalog.isFetching} onClick={() => updateFilter({ page: page.page + 1 })}>{pick(lang, 'Trang sau', 'Next')}</button></nav>}</>}</div></section><div className="catalog-contact"><p>{copy('contactBody')}</p><Link className="button dark" to="/ve-chung-toi">{copy('contactAction')}</Link></div></div></main></Layout>
}
function Account() {
  const { user, setUser, setCart, lang } = useStore(); const navigate = useNavigate()
  async function logout() { try { await api.logout() } finally { setUser(null); setCart(await hydrateGuestCart(api.product)); navigate('/') } }
  if (!user) return <Navigate to="/" replace />
  return <Layout><main className="auth-page"><section className="auth-card"><span className="account-avatar">{user.fullName?.charAt(0)}</span><span className="kicker">{pick(lang, 'Tài khoản', 'Account')}</span><h1>{pick(lang, 'Xin chào', 'Hello')}, {user.fullName}</h1><p>{user.email}</p><div className="account-actions">{user.role === 'ADMIN' ? <Link className="button dark" to="/admin">{pick(lang, 'Vào quản trị', 'Open admin')}</Link> : <Link className="button dark" to="/orders">{pick(lang, 'Xem đơn hàng', 'View orders')}</Link>}<button className="button light" onClick={logout}>{pick(lang, 'Đăng xuất', 'Sign out')}</button></div></section></main></Layout>
}
function CollectionsPage() {
  const { collections, catalogLoading, lang } = useStore()
  return <Layout><main className="page"><div className="page-title"><span className="kicker">{pick(lang, 'Bộ sưu tập', 'Collections')}</span><h1>{pick(lang, 'Khám phá theo bộ sưu tập', 'Explore by collection')}</h1></div>{catalogLoading ? <Loading text={pick(lang, 'Đang tải dữ liệu...', 'Loading data...')} /> : !collections.length ? <Empty>{pick(lang, 'Chưa có bộ sưu tập đang hiển thị.', 'No active collections are available.')}</Empty> : <section className="collection-grid">{collections.map(item => <article className="collection-card" key={item.id}><h2>{lang === 'vi' ? item.nameVi : item.nameEn}</h2><p>{lang === 'vi' ? item.descriptionVi : item.descriptionEn}</p><Link className="button light full" to={`/products?collectionId=${item.id}`}>{pick(lang, 'Xem sản phẩm', 'View products')}</Link></article>)}</section>}</main></Layout>
}

function ProductDetail() {
  const { id } = useParams(); const { add, lang, presentation } = useStore(); const navigate = useNavigate(); const [selected, setSelected] = useState(0); const [zoom, setZoom] = useState(1); const [touchStart, setTouchStart] = useState(null); const [activeTab, setActiveTab] = useState('description'); const productQuery = useQuery({ queryKey: ['product', id], queryFn: () => api.product(id), enabled: Boolean(id) }); const product = productQuery.data
  const copy = key => presentationNode(presentation?.content, 'catalog', key, lang)
  const relatedQuery = useQuery({ queryKey: ['related-products', product?.collectionId], queryFn: () => api.products({ collectionId: product.collectionId, page: 0, size: 5, sort: 'createdAt,desc' }), enabled: Boolean(product?.collectionId) })
  useEffect(() => { setSelected(0); setZoom(1); setTouchStart(null); setActiveTab('description') }, [id])
  const images = product?.images?.length ? product.images : [{ id: 'fallback', url: product?.thumbnailUrl || fallbackImage, thumbnail: true }]
  const activeImage = images[Math.min(selected, images.length - 1)] || images[0]
  const selectImage = index => { setSelected(index); setZoom(1) }
  const moveImage = direction => selectImage((selected + direction + images.length) % images.length)
  const handleTouchEnd = event => {
    if (touchStart == null || images.length < 2) return
    const delta = event.changedTouches[0].clientX - touchStart
    if (Math.abs(delta) > 40) moveImage(delta < 0 ? 1 : -1)
    setTouchStart(null)
  }
  const handleTabKeyDown = event => {
    if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return
    event.preventDefault()
    const next = event.key === 'End' ? 'specifications' : event.key === 'Home' ? 'description' : activeTab === 'description' ? 'specifications' : 'description'
    setActiveTab(next)
    event.currentTarget.querySelector(`#product-${next}-tab`)?.focus()
  }
  if (productQuery.isLoading) return <Layout><main className="page"><Loading text={pick(lang, 'Đang tải dữ liệu...', 'Loading data...')} /></main></Layout>
  if (productQuery.isError) return <Layout><main className="page"><div className="state-box error" role="alert">{productQuery.error.message}<button onClick={() => productQuery.refetch()}>{pick(lang, 'Thử lại', 'Retry')}</button></div></main></Layout>
  if (!product) return <Layout><main className="page"><Empty>{pick(lang, 'Không tìm thấy sản phẩm.', 'Product not found.')}</Empty></main></Layout>
  const name = lang === 'vi' ? product.nameVi : product.nameEn
  const description = lang === 'vi' ? product.descriptionVi : product.descriptionEn
  const relatedProducts = (relatedQuery.data?.content || []).filter(item => item.id !== product.id).slice(0, 4)
  return <Layout><main className="page product-detail-page"><div className="product-detail-layout"><section><div className="product-detail-image" onTouchStart={event => setTouchStart(event.changedTouches[0].clientX)} onTouchEnd={handleTouchEnd}><img style={{ transform: `scale(${zoom})` }} src={activeImage?.url || fallbackImage} alt={name} onError={event => { event.currentTarget.src = fallbackImage }} /></div><div className="gallery-controls"><button type="button" aria-label={pick(lang, 'Thu nhỏ ảnh', 'Zoom out')} disabled={zoom <= 1} onClick={() => setZoom(value => Math.max(1, Number((value - 0.25).toFixed(2))))}>−</button><span>{Math.round(zoom * 100)}%</span><button type="button" aria-label={pick(lang, 'Phóng to ảnh', 'Zoom in')} disabled={zoom >= 2} onClick={() => setZoom(value => Math.min(2, Number((value + 0.25).toFixed(2))))}>+</button></div>{images.length > 1 && <><div className="gallery-step"><button type="button" aria-label={pick(lang, 'Ảnh trước', 'Previous image')} onClick={() => moveImage(-1)}>‹</button><button type="button" aria-label={pick(lang, 'Ảnh sau', 'Next image')} onClick={() => moveImage(1)}>›</button></div><div className="product-thumbs">{images.map((image, index) => <button key={image.id} className={index === selected ? 'active' : ''} onClick={() => selectImage(index)} aria-label={`${pick(lang, 'Chọn ảnh', 'Select image')} ${index + 1}`}><img src={image.url} alt="" /></button>)}</div></>}</section><section className="product-detail-info"><span className="kicker">{pick(lang, 'Chi tiết sản phẩm', 'Product detail')}</span><h1>{name}</h1><p>{description || pick(lang, 'Sản phẩm gốm Bàu Trúc được tạo hình thủ công.', 'A handcrafted Bau Truc pottery piece.')}</p><b className="detail-price">{money(product.sellingPrice, lang)}</b>{product.sellingPrice < product.basePrice && <p><s>{money(product.basePrice, lang)}</s></p>}<button className="button dark full" onClick={() => add(product)}>{copy('addToCart')}</button><button type="button" className="button light full" onClick={() => navigate(`/checkout?buyNow=${product.id}`)}>{copy('buyNow')}</button><Link className="button light full" to="/products">{pick(lang, 'Quay lại danh sách', 'Back to products')}</Link></section></div>
    <section className="product-detail-extra">
      <div className="product-detail-tabs" role="tablist" aria-label={pick(lang, 'Thông tin sản phẩm', 'Product information')} onKeyDown={handleTabKeyDown}>
        <button id="product-description-tab" type="button" role="tab" aria-selected={activeTab === 'description'} aria-controls="product-detail-panel" tabIndex={activeTab === 'description' ? 0 : -1} className={activeTab === 'description' ? 'active' : ''} onClick={() => setActiveTab('description')}>{copy('descriptionTab')}</button>
        <button id="product-specifications-tab" type="button" role="tab" aria-selected={activeTab === 'specifications'} aria-controls="product-detail-panel" tabIndex={activeTab === 'specifications' ? 0 : -1} className={activeTab === 'specifications' ? 'active' : ''} onClick={() => setActiveTab('specifications')}>{copy('specificationsTab')}</button>
      </div>
      <div id="product-detail-panel" role="tabpanel" aria-labelledby={activeTab === 'description' ? 'product-description-tab' : 'product-specifications-tab'} className="product-detail-panel">
        {activeTab === 'description' ? <p className="product-long-description">{description || pick(lang, 'Mô tả chi tiết đang được cập nhật.', 'Detailed description is being updated.')}</p> : <dl className="product-specifications"><div><dt>{pick(lang, 'Kích thước', 'Size')}</dt><dd>{product.sizeCm ? `${product.sizeCm} cm` : pick(lang, 'Chưa cập nhật', 'Not available')}</dd></div><div><dt>{pick(lang, 'Cân nặng', 'Weight')}</dt><dd>{product.weightGrams != null ? `${product.weightGrams.toLocaleString(lang === 'vi' ? 'vi-VN' : 'en-US')} g` : pick(lang, 'Chưa cập nhật', 'Not available')}</dd></div></dl>}
      </div>
    </section>
    <ProductReviews productId={product.id} user={useStore().user} lang={lang} />
    <section className="product-related"><h2>{pick(lang, 'Sản phẩm liên quan', 'Related products')}</h2>
      {relatedQuery.isLoading ? <Loading text={pick(lang, 'Đang tải sản phẩm liên quan...', 'Loading related products...')} /> : relatedQuery.isError ? <div className="state-box error" role="alert">{pick(lang, 'Không thể tải sản phẩm liên quan.', 'Unable to load related products.')}<button type="button" onClick={() => relatedQuery.refetch()}>{pick(lang, 'Thử lại', 'Retry')}</button></div> : relatedProducts.length ? <div className="product-related-grid">{relatedProducts.map(item => <CatalogCard key={item.id} product={item} />)}</div> : <p>{pick(lang, 'Chưa có sản phẩm liên quan.', 'No related products available.')}</p>}
    </section></main></Layout>
}
function Cart() {
  const { cart, setCart, user, notify, lang, authLoading, startGoogleLogin, presentation } = useStore()
  const copy = key => presentationNode(presentation?.content, 'cart', key, lang)
  async function change(item, quantity) {
    if (!user) {
      const entries = setGuestQuantity(readGuestCart(), item.productId, quantity)
      writeGuestCart(entries)
      setCart(current => guestCartFromProducts(entries, current.items))
      return
    }
    try { setCart(quantity < 1 ? await api.removeCart(item.id) : await api.updateCart(item.id, quantity)) } catch (error) { notify(error.message) }
  }
  return <Layout><main className="page"><div className="page-title"><span className="kicker">{pick(lang, 'Giỏ hàng', 'Cart')}</span><h1>{copy('title')}</h1></div>{authLoading ? <Loading text={pick(lang, 'Đang kiểm tra tài khoản...', 'Checking account...')} /> : user?.role === 'ADMIN' ? <Empty>{pick(lang, 'Tài khoản quản trị không có giỏ hàng.', 'Admin accounts do not have carts.')}</Empty> : !cart.items.length ? <Empty>{copy('empty')} <Link to="/products">{pick(lang, 'Tiếp tục mua sắm', 'Continue shopping')}</Link></Empty> : <div className="checkout-grid"><section className="order-summary cart-panel">{cart.items.map(item => <div className="checkout-item" key={item.id}><img src={item.thumbnailUrl || fallbackImage} alt="" /><div><b>{lang === 'vi' ? item.nameVi : item.nameEn}</b>{user && <span>{pick(lang, 'Còn có thể đặt', 'Available to order')}: {item.availableQuantity}</span>}<span><button type="button" aria-label={pick(lang, 'Giảm số lượng', 'Decrease quantity')} onClick={() => change(item, item.quantity - 1)}>−</button> {item.quantity} <button type="button" aria-label={pick(lang, 'Tăng số lượng', 'Increase quantity')} disabled={item.quantity >= item.availableQuantity} onClick={() => change(item, item.quantity + 1)}>+</button></span><button type="button" className="link-danger" onClick={() => change(item, 0)}>{pick(lang, 'Xóa', 'Remove')}</button></div><div><small>{money(item.sellingPrice, lang)} × {item.quantity}</small><b>{money(item.lineTotal, lang)}</b></div></div>)}<div className="summary-line"><span>{pick(lang, 'Tạm tính', 'Subtotal')}</span><b>{money(cart.totalAmount, lang)}</b></div><div className="summary-line total"><span>{pick(lang, 'Tổng cộng', 'Total')}</span><b>{money(cart.totalAmount, lang)}</b></div>{user ? <Link className="button dark full" to="/checkout">{copy('checkoutAction')}</Link> : <><button className="button dark full" type="button" onClick={() => startGoogleLogin(true)}>{pick(lang, 'Đăng nhập để thanh toán', 'Sign in to checkout')}</button><small className="cart-signin-note">{pick(lang, 'Giỏ hàng đang được lưu tạm trên thiết bị này. Giá và tồn kho sẽ được kiểm tra khi đăng nhập.', 'This cart is saved on this device. Price and inventory will be checked after sign-in.')}</small></>}</section></div>}</main></Layout>
}
function Checkout() {
  const {
    cart,
    setCart,
    user,
    notify,
    lang,
    authLoading,
    startGoogleLogin,
    presentation
  } = useStore()

  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const checkout = useCheckout()
  const siteCopy = (key) => presentationNode(presentation?.content, 'checkout', key, lang)
  const checkoutLayout = presentation?.content?.checkout || presentationDefaults.checkout

  const [errors, setErrors] = useState({})
  const [submitError, setSubmitError] = useState('')

  const buyNowId = searchParams.get('buyNow')
  const isBuyNow = buyNowId !== null
  const validBuyNowId =
    isBuyNow && /^[1-9]\d*$/.test(buyNowId)

  const buyNowQuery = useQuery({
    queryKey: ['buy-now-product', buyNowId],
    queryFn: () => api.product(buyNowId),
    enabled: Boolean(validBuyNowId && user?.role === 'USER')
  })

  const buyNowProduct = buyNowQuery.data

  const buyNowItem = buyNowProduct
    ? {
        id: `buy-now-${buyNowProduct.id}`,
        productId: buyNowProduct.id,
        nameVi: buyNowProduct.nameVi,
        nameEn: buyNowProduct.nameEn,
        thumbnailUrl: buyNowProduct.thumbnailUrl,
        quantity: 1,
        sellingPrice: buyNowProduct.sellingPrice,
        lineTotal: buyNowProduct.sellingPrice
      }
    : null

  const items = isBuyNow
    ? (buyNowItem ? [buyNowItem] : [])
    : cart.items

  const total = isBuyNow
    ? (buyNowItem?.lineTotal ?? 0)
    : cart.totalAmount

  async function change(item, quantity) {
    if (isBuyNow) return

    try {
      const updated = quantity < 1
        ? await api.removeCart(item.id)
        : await api.updateCart(item.id, quantity)

      setCart(updated)
    } catch (error) {
      notify(error.message)
    }
  }

  async function submit(event) {
    event.preventDefault()

    if (user?.role !== 'USER') {
      return navigate('/account')
    }

    if (checkout.isPending) return
    if (isBuyNow && !buyNowProduct) return

    const values = { email: '', note: '', ...Object.fromEntries(new FormData(event.currentTarget)) }

    const result = validateCheckout(values, lang)

    setErrors(result.errors)
    setSubmitError('')

    if (!result.data) return

    const payload = isBuyNow
      ? {
          ...result.data,
          productId: buyNowProduct.id,
          quantity: 1
        }
      : result.data

    try {
      const created = await checkout.mutateAsync(payload)

      if (!isBuyNow) {
        setCart({ items: [], totalAmount: 0 })
      }

      navigate(`/orders/${created.orderId}`, {
        replace: true
      })
    } catch (error) {
      setSubmitError(error.message)

      if (!isBuyNow && [400, 409].includes(error.status)) {
        try {
          setCart(await api.cart())
        } catch {
          // Keep the original checkout error.
        }
      }
    }
  }

  const field = (name, label, control) => (
    <label className={
      ['email', 'address', 'note'].includes(name) ? 'wide' : ''
    }>
      {label}
      {control}
      {errors[name] && (
        <small className="field-error" role="alert">
          {errors[name]}
        </small>
      )}
    </label>
  )

  const checkoutFields = {
    receiverName: field('receiverName', siteCopy('receiverName'), <input name="receiverName" defaultValue={user?.fullName} maxLength="255" aria-invalid={Boolean(errors.receiverName)} />),
    phone: field('phone', siteCopy('phone'), <input name="phone" maxLength="32" aria-invalid={Boolean(errors.phone)} />),
    email: field('email', siteCopy('email'), <input name="email" type="email" defaultValue={user?.email} maxLength="320" aria-invalid={Boolean(errors.email)} />),
    address: field('address', siteCopy('address'), <input name="address" maxLength="500" aria-invalid={Boolean(errors.address)} />),
    note: field('note', siteCopy('note'), <textarea name="note" maxLength="1000" aria-invalid={Boolean(errors.note)} />)
  }

  let content

  if (authLoading) {
    content = (
      <Loading
        text={pick(
          lang,
          'Đang kiểm tra tài khoản...',
          'Checking account...'
        )}
      />
    )
  } else if (user?.role !== 'USER') {
    content = (
      <Empty>
        <p>
          {pick(
            lang,
            'Bạn cần đăng nhập bằng tài khoản khách hàng để đặt hàng.',
            'Sign in with a customer account to checkout.'
          )}
        </p>

        {!user && (
          <button
            type="button"
            className="button dark"
            onClick={() => startGoogleLogin(false)}
          >
            {pick(lang, 'Đăng nhập Google', 'Sign in with Google')}
          </button>
        )}
      </Empty>
    )
  } else if (isBuyNow && !validBuyNowId) {
    content = (
      <Empty>
        {pick(
          lang,
          'Đường dẫn sản phẩm không hợp lệ.',
          'Invalid product link.'
        )}
      </Empty>
    )
  } else if (isBuyNow && buyNowQuery.isLoading) {
    content = (
      <Loading
        text={pick(
          lang,
          'Đang tải sản phẩm...',
          'Loading product...'
        )}
      />
    )
  } else if (isBuyNow && buyNowQuery.isError) {
    content = (
      <div className="state-box error" role="alert">
        <p>{buyNowQuery.error.message}</p>
        <button
          type="button"
          onClick={() => buyNowQuery.refetch()}
        >
          {pick(lang, 'Thử lại', 'Retry')}
        </button>
      </div>
    )
  } else if (!items.length) {
    content = (
      <Empty>
        {pick(
          lang,
          isBuyNow
            ? 'Không tìm thấy sản phẩm.'
            : 'Giỏ hàng đang trống.',
          isBuyNow
            ? 'Product not found.'
            : 'Your cart is empty.'
        )}
        {' '}
        <Link to="/products">
          {pick(lang, 'Tiếp tục mua sắm', 'Continue shopping')}
        </Link>
      </Empty>
    )
  } else {
    const formPanel = (
        <form
          className="checkout-form"
          onSubmit={submit}
          noValidate
        >
          <h3>
            {siteCopy('formTitle')}
          </h3>

          {submitError && (
            <div className="form-error" role="alert">
              {submitError}
            </div>
          )}

          <div className="form-grid">
            {visibleCheckoutFields(checkoutLayout).map(name => <div key={name} className={['email', 'address', 'note'].includes(name) ? 'wide' : ''}>{checkoutFields[name]}</div>)}
          </div>

          {isBuyNow && (
            <p className="cart-signin-note">
              {siteCopy('buyNowNotice')}
            </p>
          )}

          <button
            type="submit"
            className="button dark full"
            disabled={checkout.isPending}
          >
            {checkout.isPending
              ? siteCopy('submitting')
              : `${siteCopy('submit')} ${money(total, lang)}`}
          </button>
        </form>
    )
    const summaryPanel = (
        <aside className="order-summary">
          <h3>
            {siteCopy(isBuyNow ? 'buyNowTitle' : 'summaryTitle')}
          </h3>

          {items.map(item => (
            <div
              className="checkout-item"
              key={item.id}
            >
              <img
                src={item.thumbnailUrl || fallbackImage}
                alt=""
              />

              <div>
                <b>
                  {lang === 'vi'
                    ? item.nameVi
                    : item.nameEn}
                </b>

                {isBuyNow ? (
                  <span>
                    {siteCopy('quantity')}: 1
                  </span>
                ) : (
                  <>
                    <span>
                      <button
                        type="button"
                        aria-label={pick(
                          lang,
                          'Giảm số lượng',
                          'Decrease quantity'
                        )}
                        disabled={checkout.isPending}
                        onClick={() =>
                          change(item, item.quantity - 1)
                        }
                      >
                        −
                      </button>

                      {' '}{item.quantity}{' '}

                      <button
                        type="button"
                        aria-label={pick(
                          lang,
                          'Tăng số lượng',
                          'Increase quantity'
                        )}
                        disabled={
                          checkout.isPending ||
                          item.quantity >= item.availableQuantity
                        }
                        onClick={() =>
                          change(item, item.quantity + 1)
                        }
                      >
                        +
                      </button>
                    </span>

                    <button
                      type="button"
                      className="link-danger"
                      disabled={checkout.isPending}
                      onClick={() => change(item, 0)}
                    >
                      {siteCopy('remove')}
                    </button>
                  </>
                )}
              </div>

              <div>
                <small>
                  {money(item.sellingPrice, lang)}
                  {' × '}{item.quantity}
                </small>
                <b>{money(item.lineTotal, lang)}</b>
              </div>
            </div>
          ))}

          <div className="summary-line total">
            <span>{siteCopy('total')}</span>
            <b>{money(total, lang)}</b>
          </div>
        </aside>
    )
    content = (
      <div className={`checkout-grid ${checkoutLayout.sectionOrder[0] === 'summary' ? 'summary-first' : ''}`}>
        {checkoutLayout.sectionOrder.map(section => <Fragment key={section}>{section === 'form' ? formPanel : summaryPanel}</Fragment>)}
      </div>
    )
  }

  return (
    <Layout>
      <main className="page checkout-page">
        {presentation?.media?.CHECKOUT_HERO && <img key={presentation.media.CHECKOUT_HERO} className="presentation-checkout-image" src={presentation.media.CHECKOUT_HERO} alt="" onError={event => { event.currentTarget.hidden = true }} />}
        <div className="page-title">
          <span className="kicker">
            {pick(
              lang,
              isBuyNow ? 'Mua ngay' : 'Giỏ hàng',
              isBuyNow ? 'Buy Now' : 'Cart'
            )}
          </span>

          <h1>
            {siteCopy('title')}
          </h1>
        </div>

        {content}
      </main>
    </Layout>
  )
}
function Orders() {
  const { user, lang, authLoading, presentation } = useStore(); const [pageNumber, setPageNumber] = useState(0); const orders = useMyOrders(pageNumber, user?.role === 'USER'); const page = orders.data
  return <Layout><main className="page"><div className="page-title"><span className="kicker">{pick(lang, 'Tài khoản', 'Account')}</span><h1>{presentationNode(presentation?.content, 'orders', 'title', lang)}</h1></div>{authLoading || orders.isLoading ? <Loading text={pick(lang, 'Đang tải đơn hàng...', 'Loading orders...')} /> : !user || user.role !== 'USER' ? <Empty>{pick(lang, 'Vui lòng đăng nhập bằng tài khoản khách hàng.', 'Please sign in with a customer account.')}</Empty> : orders.isError ? <div className="state-box error" role="alert">{orders.error.message}<button onClick={() => orders.refetch()}>{pick(lang, 'Thử lại', 'Retry')}</button></div> : !page?.content.length ? <Empty>{pick(lang, 'Bạn chưa có đơn hàng.', 'You do not have any orders yet.')}</Empty> : <><div className="order-list">{page.content.map(order => <Link className="order-row" to={`/orders/${order.id}`} key={order.id}><div><b>{order.orderCode}</b><small>{date(order.createdAt, lang)}</small></div><span>{pick(lang, 'Đơn hàng', 'Order')}: {orderStatusLabel(order.orderStatus, lang)}</span><span>{pick(lang, 'Thanh toán', 'Payment')}: {paymentStatusLabel(order.paymentStatus, lang)}</span><b>{money(order.totalAmount, lang)}</b></Link>)}</div>{page.totalPages > 1 && <nav className="pagination" aria-label={pick(lang, 'Phân trang đơn hàng', 'Order pagination')}><button disabled={page.first} onClick={() => setPageNumber(value => value - 1)}>{pick(lang, 'Trang trước', 'Previous')}</button><span>{pick(lang, 'Trang', 'Page')} {page.page + 1}/{page.totalPages}</span><button disabled={page.last} onClick={() => setPageNumber(value => value + 1)}>{pick(lang, 'Trang sau', 'Next')}</button></nav>}</>}</main></Layout>
}
function OrderDetail() {
  const { id } = useParams(); const { user, lang, authLoading } = useStore(); const query = useMyOrder(id, user?.role === 'USER'); const order = query.data
  const pendingPayment = order?.paymentStatus === 'PENDING'
  return <Layout><main className="page"><div className="page-title"><span className="kicker">{pick(lang, 'Chi tiết đơn hàng', 'Order detail')}</span><h1>{order?.orderCode || pick(lang, 'Đơn hàng', 'Order')}</h1></div>{authLoading || query.isLoading ? <Loading text={pick(lang, 'Đang tải đơn hàng...', 'Loading order...')} /> : !user || user.role !== 'USER' ? <Empty>{pick(lang, 'Vui lòng đăng nhập bằng tài khoản khách hàng.', 'Please sign in with a customer account.')}</Empty> : query.isError && query.error.status === 404 ? <Empty>{pick(lang, 'Không tìm thấy đơn hàng.', 'Order not found.')}</Empty> : query.isError ? <div className="state-box error" role="alert">{query.error.message}<button onClick={() => query.refetch()}>{pick(lang, 'Thử lại', 'Retry')}</button></div> : <div className="order-detail"><section><h3>{pick(lang, 'Thông tin nhận hàng', 'Recipient information')}</h3><p>{order.receiverName} · {order.phone}</p>{order.email && <p>{order.email}</p>}<p>{order.address}</p>{order.note && <p>{pick(lang, 'Ghi chú', 'Note')}: {order.note}</p>}<p>{pick(lang, 'Ngày tạo', 'Created at')}: {date(order.createdAt, lang)}</p><p>{pick(lang, 'Đơn hàng', 'Order')}: <b>{orderStatusLabel(order.orderStatus, lang)}</b></p><p>{pick(lang, 'Thanh toán', 'Payment')}: <b>{paymentStatusLabel(order.paymentStatus, lang)}</b></p>{pendingPayment && <small>{pick(lang, 'Trang này tự cập nhật trạng thái thanh toán mỗi 5 giây.', 'This page refreshes payment status every 5 seconds.')}</small>}<div className="payment-panel"><h3>{pick(lang, 'Thanh toán payOS', 'payOS payment')}</h3><div className="summary-line"><span>{pick(lang, 'Trạng thái', 'Status')}</span><b>{paymentStatusLabel(order.paymentStatus, lang)}</b></div><div className="summary-line"><span>{pick(lang, 'Số tiền thanh toán', 'Payment amount')}</span><b>{money(order.paymentAmount ?? order.totalAmount, lang)}</b></div>{order.expiresAt && <div className="summary-line"><span>{pick(lang, 'Hết hạn', 'Expires at')}</span><b>{date(order.expiresAt, lang)}</b></div>}{order.qrCode && <img className="payment-qr" src={order.qrCode} alt={pick(lang, 'Mã QR thanh toán payOS', 'payOS payment QR code')} />}{order.checkoutUrl ? <a className="button dark full" href={order.checkoutUrl} target="_blank" rel="noreferrer">{pick(lang, 'Thanh toán qua payOS', 'Pay with payOS')}</a> : pendingPayment && <p className="payment-note">{pick(lang, 'Cổng thanh toán đang chờ cấu hình hoặc thông tin thanh toán chưa khả dụng. Trạng thái đơn hàng vẫn được lấy từ hệ thống.', 'Payment gateway configuration or payment details are not available yet. Order state remains system-authoritative.')}</p>}</div></section><aside className="order-summary">{order.items.map(item => <div className="summary-line" key={item.productId}><span>{lang === 'vi' ? item.productNameVi : item.productNameEn} × {item.quantity}<small>{money(item.sellingPrice, lang)} / {pick(lang, 'sản phẩm', 'item')}</small></span><b>{money(item.totalPrice, lang)}</b></div>)}<div className="summary-line"><span>{pick(lang, 'Tạm tính', 'Subtotal')}</span><b>{money(order.subtotal, lang)}</b></div><div className="summary-line total"><span>{pick(lang, 'Tổng cộng', 'Total')}</span><b>{money(order.totalAmount, lang)}</b></div></aside></div>}</main></Layout>
}
function Faq() {
  const { lang, presentation } = useStore()
  const copy = key => presentationNode(presentation?.content, 'faq', key, lang)
  return <Layout><main className="page support-page"><div className="page-title"><span className="kicker">FAQ</span><h1>{copy('title')}</h1></div><section className="faq-list">{[1, 2, 3].map(number => <details key={number} open={number === 1}><summary>{copy(`question${number}`)}</summary><p>{copy(`answer${number}`)}</p></details>)}</section></main></Layout>
}
function Policy() {
  const { lang, presentation } = useStore()
  const copy = key => presentationNode(presentation?.content, 'policy', key, lang)
  return <Layout><main className="page support-page"><div className="page-title"><span className="kicker">{pick(lang, 'Chính sách', 'Policy')}</span><h1>{copy('title')}</h1></div><section className="policy">{['payment', 'refund', 'shipping'].map(section => <Fragment key={section}><h2>{copy(`${section}Title`)}</h2><p>{copy(`${section}Body`)}</p></Fragment>)}</section></main></Layout>
}
function Workshop() {
  const { user, notify, lang, support, presentation } = useStore()
  const copy = key => presentationNode(presentation?.content, 'workshop', key, lang)
  const [workshops, setWorkshops] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [created, setCreated] = useState(null)
  useEffect(() => { api.workshops().then(setWorkshops).catch(error => notify(error.message)) }, [notify])
  const defaultName = user?.fullName || ''
  const defaultEmail = user?.email || ''
  async function submit(event) {
    event.preventDefault()
    if (submitting) return
    const form = event.currentTarget
    const values = Object.fromEntries(new FormData(form))
    values.workshopId = values.workshopId ? Number(values.workshopId) : null
    values.participants = Number(values.participants)
    values.preferredAt = businessLocalDateTimeToOffset(values.preferredAt)
    setSubmitting(true)
    try {
      const result = await api.workshopBooking(values)
      setCreated(result)
      form.reset()
      notify(pick(lang, 'Đã gửi yêu cầu đặt lịch workshop.', 'Workshop booking request sent.'))
    } catch (error) { notify(error.message) } finally { setSubmitting(false) }
  }
  return <Layout><main className="page workshop-page"><div className="reference-page"><PageHero title={copy('heroTitle')} subtitle={copy('heroSubtitle')} seed="experience-hero" imageUrl={presentation?.media?.WORKSHOP_HERO} /><ExperienceIntro presentation={presentation?.content} /></div>{workshops.length > 0 && <section className="workshop-cards">{workshops.map(item => <article className={String(item.id) === String(selectedId) ? 'selected' : ''} key={item.id}><img src={item.imageUrl || '/assets/images/tour.jpg'} alt={item.title} /><div><h2>{item.title}</h2><p>{item.description}</p><b>{money(item.priceAmount, lang)}</b><span>{item.durationMinutes} {pick(lang, 'phút', 'minutes')} · {pick(lang, 'tối đa', 'up to')} {item.maxParticipants} {pick(lang, 'khách', 'guests')}</span><button className="button light full" onClick={() => setSelectedId(String(item.id))}>{pick(lang, 'Chọn gói này', 'Select this offering')}</button></div></article>)}</section>}<section className="workshop-layout"><form className="workshop-form" onSubmit={submit}><h2>{copy('bookingTitle')}</h2>{created && <div className="form-success" role="status">{pick(lang, `Yêu cầu #${created.id} đã được ghi nhận. Cửa hàng sẽ liên hệ xác nhận.`, `Request #${created.id} has been recorded. The store will contact you to confirm.`)}</div>}<label className="wide">{pick(lang, 'Gói workshop', 'Workshop offering')}<select name="workshopId" value={selectedId} onChange={e => setSelectedId(e.target.value)}><option value="">{pick(lang, 'Tư vấn chọn gói', 'Help me choose')}</option>{workshops.map(item => <option key={item.id} value={item.id}>{item.title} · {money(item.priceAmount, lang)}</option>)}</select></label><label>{pick(lang, 'Họ và tên', 'Full name')}<input name="fullName" defaultValue={defaultName} maxLength="255" required /></label><label>Email<input name="email" type="email" defaultValue={defaultEmail} maxLength="320" required /></label><label>{pick(lang, 'Số điện thoại', 'Phone number')}<input name="phone" maxLength="32" required /></label><label>{pick(lang, 'Ngày giờ mong muốn', 'Preferred date and time')}<input name="preferredAt" type="datetime-local" required /></label><label>{pick(lang, 'Số người', 'Participants')}<input name="participants" type="number" min="1" max="30" defaultValue="2" required /></label><label>{pick(lang, 'Ghi chú', 'Note')}<textarea name="note" maxLength="1000" placeholder={pick(lang, 'Ví dụ: có trẻ em đi cùng, cần tư vấn gói trải nghiệm...', 'Example: children joining, need help choosing an offering...')} /></label><button className="button dark full" disabled={submitting}>{submitting ? pick(lang, 'Đang gửi...', 'Sending...') : pick(lang, 'Gửi yêu cầu đặt lịch', 'Submit booking request')}</button></form><aside className="workshop-info"><h2>{copy('informationTitle')}</h2><p><b>{pick(lang, 'Địa điểm', 'Location')}:</b> {support.address}</p><p><b>{pick(lang, 'Liên hệ', 'Contact')}:</b> {support.phones.join(' · ')}</p>{support.openingHours && <p><b>{pick(lang, 'Giờ mở cửa', 'Opening hours')}:</b> {support.openingHours}</p>}<p><b>{pick(lang, 'Lưu ý', 'Note')}:</b> {pick(lang, 'Đây là yêu cầu đặt lịch. Admin sẽ xác nhận lại thời gian qua điện thoại hoặc email.', 'This is a booking request. Admin will confirm the time by phone or email.')}</p><Link className="button light full" to="/support">{pick(lang, 'Cần tư vấn thêm', 'Need more help')}</Link></aside></section></main></Layout>
}
function AboutPage() { return <Layout><div className="reference-page"><About /></div></Layout> }

function Support() { const { lang, support, presentation } = useStore(); const copy = key => presentationNode(presentation?.content, 'support', key, lang); return <Layout><main className="page support-page"><div className="reference-page"><PageHero title={copy('heroTitle')} subtitle={copy('heroSubtitle')} seed="contact-hero" imageUrl={presentation?.media?.SUPPORT_HERO} /></div><div className="support-layout"><section className="contact-card"><h2>{copy('contactTitle')}</h2><p><b>Email:</b> <a href={`mailto:${support.email}`}>{support.email}</a></p><p><b>Zalo:</b> {support.phones.map((phone, index) => <span key={phone}>{index > 0 && ' · '}<a href={`https://zalo.me/${phone}`} target="_blank" rel="noreferrer">{phone}</a></span>)}</p>{support.facebook && <p><b>Facebook:</b> <a href={support.facebook} target="_blank" rel="noreferrer">{pick(lang, 'Xem trang Facebook', 'Open Facebook page')}</a></p>}<p><b>{pick(lang, 'Địa chỉ', 'Address')}:</b> {support.map ? <a href={support.map} target="_blank" rel="noreferrer">{support.address}</a> : support.address}</p>{support.openingHours && <p><b>{pick(lang, 'Giờ mở cửa', 'Opening hours')}:</b> {support.openingHours}</p>}</section><section className="faq-list"><details open><summary>{copy('question1')}</summary><p>{copy('answer1')}</p></details><details><summary>{copy('question2')}</summary><p>{copy('answer2')}</p></details></section></div></main></Layout> }

function AdminPortal() {
  const store = useStore()
  async function logout() { try { await api.logout() } finally { store.setUser(null); window.location.assign('/') } }
  if (store.authLoading) return <div className="admin-state">{pick(store.lang, 'Đang xác thực quyền quản trị...', 'Checking admin access...')}</div>
  return <Admin user={store.user} notify={store.notify} onLogout={logout} lang={store.lang} />
}

export default function App() { return <ThemeProvider><StoreProvider><Routes><Route path="/san-pham" element={<Products />} /><Route path="/trai-nghiem" element={<Workshop />} /><Route path="/lien-he" element={<Support />} /><Route path="/" element={<Home />} /><Route path="/products" element={<Products />} /><Route path="/products/:id" element={<ProductDetail />} /><Route path="/collections" element={<CollectionsPage />} /><Route path="/workshop" element={<Workshop />} /><Route path="/login" element={<Account />} /><Route path="/account" element={<Account />} /><Route path="/notifications" element={<CustomerNotifications />} /><Route path="/cart" element={<Cart />} /><Route path="/checkout" element={<Checkout />} /><Route path="/orders" element={<Orders />} /><Route path="/orders/:id" element={<OrderDetail />} /><Route path="/ve-chung-toi" element={<AboutPage />} /><Route path="/about" element={<Navigate to="/ve-chung-toi" replace />} /><Route path="/support" element={<Support />} /><Route path="/faq" element={<Faq />} /><Route path="/policy" element={<Policy />} /><Route path="/admin" element={<AdminPortal />} /><Route path="*" element={<Home />} /></Routes></StoreProvider></ThemeProvider> }
