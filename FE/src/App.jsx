import { createContext, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { Link, Navigate, NavLink, Route, Routes, useNavigate, useParams } from 'react-router-dom'
import { api } from './api'
import Admin from './Admin'

const Store = createContext(null)
const fallbackImage = '/assets/images/vase.jpg'
const money = (value, lang = 'vi') => new Intl.NumberFormat(lang === 'vi' ? 'vi-VN' : 'en-US', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value || 0)
const date = (value, lang = 'vi') => new Intl.DateTimeFormat(lang === 'vi' ? 'vi-VN' : 'en-US', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
const pick = (lang, vi, en) => lang === 'vi' ? vi : en
const contact = {
  email: 'Cosogombautrucdangxem@gmail.com',
  phones: ['0343478155', '0966477160'],
  facebook: 'https://www.facebook.com/share/18jwSfSPD7/?mibextid=wwXIfr',
  address: '35 Bàu Trúc, thôn Vĩnh Thuận, xã Ninh Phước, tỉnh Khánh Hòa',
  map: 'https://www.google.com/maps/search/?api=1&query=35%20B%C3%A0u%20Tr%C3%BAc%2C%20th%C3%B4n%20V%C4%A9nh%20Thu%E1%BA%ADn%2C%20x%C3%A3%20Ninh%20Ph%C6%B0%E1%BB%9Bc%2C%20t%E1%BB%89nh%20Kh%C3%A1nh%20H%C3%B2a'
}
const statusText = { PENDING: 'Chờ xử lý', AWAITING_PAYMENT: 'Chờ thanh toán', PAID: 'Đã thanh toán', PROCESSING: 'Đang xử lý', SHIPPING: 'Đang giao', COMPLETED: 'Hoàn thành', CANCELLED: 'Đã hủy', EXPIRED: 'Hết hạn', FAILED: 'Thất bại', REFUNDED: 'Đã hoàn tiền' }

function StoreProvider({ children }) {
  const [lang, setLang] = useState(() => localStorage.getItem('dxLang') || 'vi')
  const [products, setProducts] = useState([]), [collections, setCollections] = useState([])
  const [cart, setCart] = useState({ items: [], totalAmount: 0 }), [user, setUser] = useState(null)
  const [catalogLoading, setCatalogLoading] = useState(true), [toast, setToast] = useState('')
  const [authLoading, setAuthLoading] = useState(true)
  const notify = message => { setToast(message); window.setTimeout(() => setToast(''), 2500) }
  useEffect(() => {
    Promise.all([api.products(), api.collections()]).then(([page, groups]) => { setProducts(page.content); setCollections(groups) }).catch(error => notify(error.message)).finally(() => setCatalogLoading(false))
    api.me().then(current => { setUser(current); return current.role === 'USER' ? api.cart() : null }).then(value => value && setCart(value)).catch(() => {}).finally(() => setAuthLoading(false))
  }, [])
  useEffect(() => { localStorage.setItem('dxLang', lang); document.documentElement.lang = lang }, [lang])
  async function add(productId) {
    if (!user) { notify('Vui lòng đăng nhập để thêm sản phẩm.'); return false }
    if (user.role !== 'USER') { notify('Tài khoản quản trị không có giỏ hàng.'); return false }
    try { setCart(await api.addCart(productId)); notify('Đã thêm vào giỏ hàng.'); return true } catch (error) { notify(error.message); return false }
  }
  return <Store.Provider value={{ products, collections, cart, setCart, user, setUser, catalogLoading, authLoading, add, notify, lang, setLang }}>{children}{toast && <div className="toast show">{toast}</div>}</Store.Provider>
}
const useStore = () => useContext(Store)

function Layout({ children }) {
  const { cart, user, lang, setLang } = useStore()
  return <><header className="site-header"><Link className="brand" to="/"><span className="pot-mark">◯</span>Đàng Xem</Link><nav className="main-nav"><NavLink to="/">{pick(lang, 'Trang chủ', 'Home')}</NavLink><NavLink to="/products">{pick(lang, 'Sản phẩm', 'Products')}</NavLink><NavLink to="/support">{pick(lang, 'Hỗ trợ', 'Support')}</NavLink>{user && <NavLink to="/orders">{pick(lang, 'Đơn hàng', 'Orders')}</NavLink>}</nav><div className="header-tools"><button className="lang-btn" type="button" onClick={() => setLang(lang === 'vi' ? 'en' : 'vi')}>{lang === 'vi' ? 'EN' : 'VI'}</button><Link className="icon-btn cart-btn" to="/checkout">{pick(lang, 'Giỏ', 'Cart')} <span className="cart-count">{cart.items.reduce((sum, item) => sum + item.quantity, 0)}</span></Link>{user ? <Link className="icon-btn" to="/account">{user.fullName}</Link> : <DirectGoogleLogin />}</div></header>{children}<BilingualFooter /><div className="floating-chat"><a className="zalo" href={`https://zalo.me/${contact.phones[0]}`} target="_blank" rel="noreferrer" aria-label={`Zalo ${contact.phones[0]}`} title={`Zalo ${contact.phones[0]}`}>Z</a><a className="facebook" href={contact.facebook} target="_blank" rel="noreferrer" aria-label="Facebook" title="Facebook">f</a></div></>
}

function DirectGoogleLogin() {
  const { lang, setUser, setCart, notify } = useStore()
  const [ready, setReady] = useState(Boolean(window.google?.accounts?.id))
  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID
  const callbackRef = useRef(null)
  callbackRef.current = async credential => {
    try {
      const result = await api.googleLogin(credential)
      setUser(result.user)
      if (result.user.role === 'USER') setCart(await api.cart())
      notify(pick(lang, 'Đăng nhập thành công.', 'Signed in successfully.'))
      if (result.user.role === 'ADMIN') window.location.assign('/admin')
    } catch (error) { notify(error.message) }
  }
  useEffect(() => {
    if (!clientId) return
    const initialize = () => {
      window.google.accounts.id.initialize({ client_id: clientId, callback: response => callbackRef.current(response.credential), auto_select: false, cancel_on_tap_outside: false })
      setReady(true)
    }
    if (window.google?.accounts?.id) initialize()
    else {
      let script = document.querySelector('script[data-google-identity]')
      if (!script) { script = document.createElement('script'); script.src = 'https://accounts.google.com/gsi/client'; script.async = true; script.dataset.googleIdentity = 'true'; document.head.appendChild(script) }
      script.addEventListener('load', initialize)
      return () => script.removeEventListener('load', initialize)
    }
  }, [clientId])
  function openGoogle() {
    if (!clientId) return notify(pick(lang, 'Chưa cấu hình Google Client ID.', 'Google Client ID is not configured.'))
    if (!ready) return notify(pick(lang, 'Google đang tải, vui lòng thử lại.', 'Google is loading, please try again.'))
    window.google.accounts.id.prompt(notification => {
      if (notification.isNotDisplayed?.() || notification.isSkippedMoment?.()) notify(pick(lang, 'Google không thể mở hộp đăng nhập. Hãy cho phép cookie và popup rồi thử lại.', 'Google could not open sign-in. Allow cookies and popups, then try again.'))
    })
  }
  return <button className="icon-btn direct-login" type="button" onClick={openGoogle}>{pick(lang, 'Đăng nhập', 'Sign in')}</button>
}
function BilingualFooter() {
  const { lang } = useStore()
  return <footer className="site-footer"><div className="footer-grid"><div><Link className="brand" to="/">Đàng Xem</Link><p className="footer-slogan">{pick(lang, 'Đất hóa hồn — Tay giữ lửa', 'Earth given soul — Hands guarding fire')}</p><p>{pick(lang, 'Gốm thủ công Chăm Bàu Trúc — Di sản UNESCO 2022.', 'Handcrafted Cham pottery from Bau Truc — UNESCO Heritage 2022.')}</p></div><div className="footer-col"><h3>{pick(lang, 'Liên kết', 'Links')}</h3><Link to="/products">{pick(lang, 'Sản phẩm', 'Products')}</Link><Link to="/orders">{pick(lang, 'Đơn hàng', 'Orders')}</Link><Link to="/support">{pick(lang, 'Hỗ trợ', 'Support')}</Link></div><div className="footer-col"><h3>{pick(lang, 'Liên hệ', 'Contact')}</h3><a href={`mailto:${contact.email}`}>{contact.email}</a>{contact.phones.map(phone => <a key={phone} href={`https://zalo.me/${phone}`} target="_blank" rel="noreferrer">Zalo: {phone}</a>)}<a href={contact.facebook} target="_blank" rel="noreferrer">Facebook</a><a href={contact.map} target="_blank" rel="noreferrer">{contact.address}</a></div></div></footer>
}
function LegacyLayout({ children }) {
  const { cart, user } = useStore()
  return <><header className="site-header"><Link className="brand" to="/"><span className="pot-mark">◯</span>Đàng Xem</Link><nav className="main-nav"><NavLink to="/">Trang chủ</NavLink><NavLink to="/products">Sản phẩm</NavLink><NavLink to="/support">Hỗ trợ</NavLink>{user && <NavLink to="/orders">Đơn hàng</NavLink>}</nav><div className="header-tools"><Link className="icon-btn cart-btn" to="/checkout">Giỏ <span className="cart-count">{cart.items.reduce((sum, item) => sum + item.quantity, 0)}</span></Link><Link className="icon-btn" to="/account">{user ? user.fullName : 'Đăng nhập'}</Link></div></header>{children}<Footer /></>
}
function Footer() { return <footer className="site-footer"><div className="footer-grid"><div><Link className="brand" to="/">Đàng Xem</Link><p className="footer-slogan">Đất hóa hồn — Tay giữ lửa</p><p>Gốm thủ công Chăm Bàu Trúc — Di sản UNESCO 2022.</p></div><div className="footer-col"><h3>Liên kết</h3><Link to="/products">Sản phẩm</Link><Link to="/orders">Đơn hàng</Link><Link to="/support">Hỗ trợ</Link></div><div className="footer-col"><h3>Liên hệ</h3><p>Khánh Hòa, Việt Nam</p><p>7:00 – 17:00 hàng ngày</p></div></div></footer> }
function Loading({ text = 'Đang tải dữ liệu...' }) { return <div className="state-box">{text}</div> }
function Empty({ children }) { return <div className="state-box">{children}</div> }

function ProductCard({ product, onView }) {
  const { add, lang } = useStore()
  const name = lang === 'vi' ? product.nameVi : product.nameEn
  return <article className="product-card"><div className="product-image" onClick={() => onView(product)}><img src={product.thumbnailUrl || fallbackImage} alt={name} onError={event => { event.currentTarget.src = fallbackImage }} /></div><div className="product-info"><div className="product-top"><div><h3>{name}</h3><p>{lang === 'vi' ? product.nameEn : product.nameVi}</p></div><span className="price">{money(product.sellingPrice, lang)}</span></div>{product.sellingPrice < product.basePrice && <p><s>{money(product.basePrice, lang)}</s></p>}<div className="card-actions"><button onClick={() => onView(product)}>{pick(lang, 'Chi tiết', 'Details')}</button><button className="add-cart" onClick={() => add(product.id)}>{pick(lang, 'Thêm vào giỏ', 'Add to cart')}</button></div></div></article>
}
function LegacyProductCard({ product, onView }) {
  const { add } = useStore()
  return <article className="product-card"><div className="product-image" onClick={() => onView(product)}><img src={product.thumbnailUrl || fallbackImage} alt={product.nameVi} onError={event => { event.currentTarget.src = fallbackImage }} /></div><div className="product-info"><div className="product-top"><div><h3>{product.nameVi}</h3><p>{product.nameEn}</p></div><span className="price">{money(product.sellingPrice)}</span></div>{product.sellingPrice < product.basePrice && <p><s>{money(product.basePrice)}</s></p>}<div className="card-actions"><button onClick={() => onView(product)}>Chi tiết</button><button className="add-cart" onClick={() => add(product.id)}>Thêm vào giỏ</button></div></div></article>
}
function ProductModal({ product, onClose }) {
  const { add, lang } = useStore(); if (!product) return null
  const name = lang === 'vi' ? product.nameVi : product.nameEn
  const description = lang === 'vi' ? product.descriptionVi : product.descriptionEn
  return <div className="modal open" onClick={event => event.target === event.currentTarget && onClose()}><div className="modal-card"><button className="close-modal" onClick={onClose}>×</button><div className="zoom-stage"><img src={product.thumbnailUrl || fallbackImage} alt={name} /></div><div className="modal-info"><span className="kicker">{pick(lang, 'Gốm thủ công', 'Handcrafted pottery')}</span><h2>{name}</h2><b>{money(product.sellingPrice, lang)}</b><p>{description || pick(lang, 'Sản phẩm gốm Bàu Trúc được tạo hình hoàn toàn bằng tay.', 'A Bau Truc pottery piece shaped entirely by hand.')}</p><button className="button dark full" onClick={() => add(product.id)}>{pick(lang, 'Thêm vào giỏ hàng', 'Add to cart')}</button></div></div></div>
}
function LegacyProductModal({ product, onClose }) {
  const { add } = useStore(); if (!product) return null
  return <div className="modal open" onClick={event => event.target === event.currentTarget && onClose()}><div className="modal-card"><button className="close-modal" onClick={onClose}>×</button><div className="zoom-stage"><img src={product.thumbnailUrl || fallbackImage} alt={product.nameVi} /></div><div className="modal-info"><span className="kicker">Gốm thủ công</span><h2>{product.nameVi}</h2><b>{money(product.sellingPrice)}</b><p>{product.descriptionVi || 'Sản phẩm gốm Bàu Trúc được tạo hình hoàn toàn bằng tay.'}</p><button className="button dark full" onClick={() => add(product.id)}>Thêm vào giỏ hàng</button></div></div></div>
}
function Home() {
  const { products, catalogLoading, lang } = useStore(); const [view, setView] = useState(null)
  return <Layout><main><section className="hero"><div className="hero-image"><img src="/assets/images/hero-pottery.jpg" alt="Bau Truc pottery artisan" /></div><div className="hero-copy"><span className="kicker">{pick(lang, 'Làng gốm Bàu Trúc · Khánh Hòa', 'Bau Truc Pottery Village · Khanh Hoa')}</span><h1>{pick(lang, <>Đất hóa hồn —<br />Tay giữ lửa</>, <>Earth given soul —<br />Hands guarding fire</>)}</h1><p>{pick(lang, 'Những tác phẩm gốm Chăm độc bản, được tạo hình bằng tay và nung lộ thiên.', 'One-of-a-kind Cham pottery, shaped entirely by hand and fired in the open air.')}</p><Link className="button dark" to="/products">{pick(lang, 'Khám phá sản phẩm →', 'Explore products →')}</Link></div></section><section className="section"><div className="section-head"><div><span className="kicker">{pick(lang, 'Bộ sưu tập', 'Collection')}</span><h2>{pick(lang, 'Sản phẩm nổi bật', 'Featured products')}</h2></div><Link to="/products">{pick(lang, 'Xem tất cả →', 'View all →')}</Link></div>{catalogLoading ? <Loading /> : <div className="product-grid">{products.slice(0, 3).map(product => <ProductCard key={product.id} product={product} onView={setView} />)}</div>}</section></main><ProductModal product={view} onClose={() => setView(null)} /></Layout>
}
function LegacyHome() {
  const { products, catalogLoading } = useStore(); const [view, setView] = useState(null)
  return <Layout><main><section className="hero"><div className="hero-image"><img src="/assets/images/hero-pottery.jpg" alt="Nghệ nhân gốm Bàu Trúc" /></div><div className="hero-copy"><span className="kicker">Làng gốm Bàu Trúc · Khánh Hòa</span><h1>Đất hóa hồn —<br />Tay giữ lửa</h1><p>Những tác phẩm gốm Chăm độc bản, được tạo hình bằng tay và nung lộ thiên.</p><Link className="button dark" to="/products">Khám phá sản phẩm →</Link></div></section><section className="section"><div className="section-head"><div><span className="kicker">Bộ sưu tập</span><h2>Sản phẩm nổi bật</h2></div><Link to="/products">Xem tất cả →</Link></div>{catalogLoading ? <Loading /> : <div className="product-grid">{products.slice(0, 3).map(product => <ProductCard key={product.id} product={product} onView={setView} />)}</div>}</section></main><ProductModal product={view} onClose={() => setView(null)} /></Layout>
}
function Products() {
  const { products, collections, catalogLoading, lang } = useStore(); const [query, setQuery] = useState(''), [collection, setCollection] = useState('all'), [sort, setSort] = useState('new'), [view, setView] = useState(null)
  const list = useMemo(() => products.filter(item => `${item.nameVi} ${item.nameEn}`.toLowerCase().includes(query.toLowerCase()) && (collection === 'all' || String(item.collectionId) === collection)).sort((a, b) => sort === 'low' ? a.sellingPrice - b.sellingPrice : sort === 'high' ? b.sellingPrice - a.sellingPrice : 0), [products, query, collection, sort])
  return <Layout><main className="page"><div className="page-title"><span className="kicker">{pick(lang, 'Cửa hàng', 'Shop')}</span><h1>{pick(lang, 'Tất cả sản phẩm', 'All products')}</h1></div><section className="catalog-layout"><aside className="filters open"><label>{pick(lang, 'Tìm kiếm', 'Search')}</label><input value={query} onChange={event => setQuery(event.target.value)} placeholder={pick(lang, 'Tên sản phẩm...', 'Product name...')} /><label>{pick(lang, 'Bộ sưu tập', 'Collection')}</label><select value={collection} onChange={event => setCollection(event.target.value)}><option value="all">{pick(lang, 'Tất cả', 'All')}</option>{collections.map(item => <option key={item.id} value={item.id}>{lang === 'vi' ? item.nameVi : item.nameEn}</option>)}</select><label>{pick(lang, 'Sắp xếp', 'Sort')}</label><select value={sort} onChange={event => setSort(event.target.value)}><option value="new">{pick(lang, 'Mới nhất', 'Newest')}</option><option value="low">{pick(lang, 'Giá thấp → cao', 'Price low → high')}</option><option value="high">{pick(lang, 'Giá cao → thấp', 'Price high → low')}</option></select></aside><div>{catalogLoading ? <Loading /> : list.length ? <div className="product-grid">{list.map(product => <ProductCard key={product.id} product={product} onView={setView} />)}</div> : <Empty>{pick(lang, 'Không tìm thấy sản phẩm phù hợp.', 'No matching products found.')}</Empty>}</div></section></main><ProductModal product={view} onClose={() => setView(null)} /></Layout>
}
function LegacyProducts() {
  const { products, collections, catalogLoading } = useStore(); const [query, setQuery] = useState(''), [collection, setCollection] = useState('all'), [sort, setSort] = useState('new'), [view, setView] = useState(null)
  const list = useMemo(() => products.filter(item => `${item.nameVi} ${item.nameEn}`.toLowerCase().includes(query.toLowerCase()) && (collection === 'all' || String(item.collectionId) === collection)).sort((a, b) => sort === 'low' ? a.sellingPrice - b.sellingPrice : sort === 'high' ? b.sellingPrice - a.sellingPrice : 0), [products, query, collection, sort])
  return <Layout><main className="page"><div className="page-title"><span className="kicker">Cửa hàng</span><h1>Tất cả sản phẩm</h1></div><section className="catalog-layout"><aside className="filters open"><label>Tìm kiếm</label><input value={query} onChange={event => setQuery(event.target.value)} placeholder="Tên sản phẩm..." /><label>Bộ sưu tập</label><select value={collection} onChange={event => setCollection(event.target.value)}><option value="all">Tất cả</option>{collections.map(item => <option key={item.id} value={item.id}>{item.nameVi}</option>)}</select><label>Sắp xếp</label><select value={sort} onChange={event => setSort(event.target.value)}><option value="new">Mới nhất</option><option value="low">Giá thấp → cao</option><option value="high">Giá cao → thấp</option></select></aside><div>{catalogLoading ? <Loading /> : list.length ? <div className="product-grid">{list.map(product => <ProductCard key={product.id} product={product} onView={setView} />)}</div> : <Empty>Không tìm thấy sản phẩm phù hợp.</Empty>}</div></section></main><ProductModal product={view} onClose={() => setView(null)} /></Layout>
}

function GoogleButton({ onCredential }) {
  const host = useRef(null), callbackRef = useRef(onCredential), clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID
  callbackRef.current = onCredential
  useEffect(() => {
    if (!clientId) return
    const render = () => {
      if (!host.current || !window.google) return
      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: value => callbackRef.current(value.credential),
        auto_select: false,
        cancel_on_tap_outside: false
      })
      window.google.accounts.id.renderButton(host.current, { theme: 'outline', size: 'large', width: 320, text: 'continue_with' })
      window.google.accounts.id.prompt()
    }
    if (window.google) render(); else { const script = document.createElement('script'); script.src = 'https://accounts.google.com/gsi/client'; script.async = true; script.onload = render; document.head.appendChild(script); return () => script.remove() }
    return () => window.google?.accounts.id.cancel()
  }, [clientId])
  if (!clientId) return <div className="auth-error">Thiếu VITE_GOOGLE_CLIENT_ID trong file .env.</div>
  return <div ref={host} />
}
function Account() {
  const { user, setUser, setCart, notify, lang } = useStore(); const [loading, setLoading] = useState(false); const navigate = useNavigate()
  async function login(credential) { setLoading(true); try { const result = await api.googleLogin(credential); setUser(result.user); if (result.user.role === 'USER') setCart(await api.cart()); notify(pick(lang, 'Đăng nhập thành công.', 'Signed in successfully.')); navigate('/products') } catch (error) { notify(error.message) } finally { setLoading(false) } }
  async function logout() { try { await api.logout() } finally { setUser(null); setCart({ items: [], totalAmount: 0 }); navigate('/') } }
  if (!user) return <Navigate to="/" replace />
  return <Layout><main className="auth-page"><section className="auth-card"><span className="account-avatar">{user.fullName?.charAt(0)}</span><span className="kicker">{pick(lang, 'Tài khoản', 'Account')}</span><h1>{pick(lang, 'Xin chào', 'Hello')}, {user.fullName}</h1><p>{user.email}</p><div className="account-actions"><Link className="button dark" to="/orders">{pick(lang, 'Xem đơn hàng', 'View orders')}</Link><button className="button light" onClick={logout}>{pick(lang, 'Đăng xuất', 'Sign out')}</button></div></section></main></Layout>
}
function LegacyAccount() {
  const { user, setUser, setCart, notify } = useStore(); const [loading, setLoading] = useState(false); const navigate = useNavigate()
  async function login(credential) { setLoading(true); try { const result = await api.googleLogin(credential); setUser(result.user); if (result.user.role === 'USER') setCart(await api.cart()); notify('Đăng nhập thành công.'); navigate('/products') } catch (error) { notify(error.message) } finally { setLoading(false) } }
  async function logout() { try { await api.logout() } finally { setUser(null); setCart({ items: [], totalAmount: 0 }); navigate('/') } }
  return <Layout><main className="auth-page"><section className="auth-card">{user ? <><span className="account-avatar">{user.fullName?.charAt(0)}</span><span className="kicker">Tài khoản</span><h1>Xin chào, {user.fullName}</h1><p>{user.email}</p><p>Vai trò: {user.role}</p><div className="account-actions"><Link className="button dark" to="/orders">Xem đơn hàng</Link><button className="button light" onClick={logout}>Đăng xuất</button></div></> : <><span className="kicker">Tài khoản</span><h1>Đăng nhập với Google</h1><p>BE chỉ hỗ trợ Google Identity; tài khoản được tạo tự động ở lần đăng nhập đầu tiên.</p>{loading ? <Loading text="Đang đăng nhập..." /> : <GoogleButton onCredential={login} />}</>}</section></main></Layout>
}
function Checkout() {
  const { cart, setCart, user, notify, lang } = useStore(); const [submitting, setSubmitting] = useState(false); const navigate = useNavigate()
  async function change(item, quantity) { try { setCart(quantity < 1 ? await api.removeCart(item.id) : await api.updateCart(item.id, quantity)) } catch (error) { notify(error.message) } }
  async function submit(event) { event.preventDefault(); if (!user) return navigate('/account'); setSubmitting(true); try { const result = await api.checkout(Object.fromEntries(new FormData(event.currentTarget))); setCart({ items: [], totalAmount: 0 }); if (result.checkoutUrl) window.location.assign(result.checkoutUrl); else navigate(`/orders/${result.orderId}`) } catch (error) { notify(error.message) } finally { setSubmitting(false) } }
  return <Layout><main className="page checkout-page"><div className="page-title"><span className="kicker">{pick(lang, 'Giỏ hàng', 'Cart')}</span><h1>{pick(lang, 'Thông tin đặt hàng', 'Checkout details')}</h1></div>{!user ? <Empty>{pick(lang, 'Bạn cần đăng nhập trước khi thanh toán.', 'Please sign in before checkout.')}</Empty> : !cart.items.length ? <Empty>{pick(lang, 'Giỏ hàng đang trống.', 'Your cart is empty.')} <Link to="/products">{pick(lang, 'Tiếp tục mua sắm', 'Continue shopping')}</Link></Empty> : <div className="checkout-grid"><form className="checkout-form" onSubmit={submit}><h3>{pick(lang, 'Thông tin giao hàng', 'Shipping information')}</h3><div className="form-grid"><label>{pick(lang, 'Họ và tên', 'Full name')}<input name="receiverName" defaultValue={user.fullName} required /></label><label>{pick(lang, 'Số điện thoại', 'Phone')}<input name="phone" minLength="8" maxLength="32" required /></label><label className="wide">Email<input name="email" type="email" defaultValue={user.email} /></label><label className="wide">{pick(lang, 'Địa chỉ', 'Address')}<input name="address" required /></label><label className="wide">{pick(lang, 'Ghi chú', 'Note')}<textarea name="note" /></label></div><button className="button dark full" disabled={submitting}>{submitting ? pick(lang, 'Đang tạo thanh toán...', 'Creating payment...') : `${pick(lang, 'Thanh toán', 'Pay')} ${money(cart.totalAmount, lang)}`}</button></form><aside className="order-summary"><h3>{pick(lang, 'Đơn hàng của bạn', 'Your order')}</h3>{cart.items.map(item => <div className="checkout-item" key={item.id}><img src={item.thumbnailUrl || fallbackImage} alt="" /><div><b>{lang === 'vi' ? item.nameVi : item.nameEn}</b><span><button type="button" onClick={() => change(item, item.quantity - 1)}>−</button> {item.quantity} <button type="button" onClick={() => change(item, item.quantity + 1)}>+</button></span><button type="button" className="link-danger" onClick={() => change(item, 0)}>{pick(lang, 'Xóa', 'Remove')}</button></div><b>{money(item.lineTotal, lang)}</b></div>)}<div className="summary-line total"><span>{pick(lang, 'Tổng cộng', 'Total')}</span><b>{money(cart.totalAmount, lang)}</b></div></aside></div>}</main></Layout>
}
function LegacyCheckout() {
  const { cart, setCart, user, notify } = useStore(); const [submitting, setSubmitting] = useState(false); const navigate = useNavigate()
  async function change(item, quantity) { try { setCart(quantity < 1 ? await api.removeCart(item.id) : await api.updateCart(item.id, quantity)) } catch (error) { notify(error.message) } }
  async function submit(event) { event.preventDefault(); if (!user) return navigate('/account'); setSubmitting(true); try { const values = Object.fromEntries(new FormData(event.currentTarget)); const result = await api.checkout(values); setCart({ items: [], totalAmount: 0 }); sessionStorage.setItem('lastCheckout', JSON.stringify(result)); if (result.checkoutUrl) window.location.assign(result.checkoutUrl); else navigate(`/orders/${result.orderId}`) } catch (error) { notify(error.message) } finally { setSubmitting(false) } }
  return <Layout><main className="page checkout-page"><div className="page-title"><span className="kicker">Giỏ hàng</span><h1>Thông tin đặt hàng</h1></div>{!user ? <Empty>Bạn cần <Link to="/account">đăng nhập</Link> trước khi thanh toán.</Empty> : !cart.items.length ? <Empty>Giỏ hàng đang trống. <Link to="/products">Tiếp tục mua sắm</Link></Empty> : <div className="checkout-grid"><form className="checkout-form" onSubmit={submit}><h3>Thông tin giao hàng</h3><div className="form-grid"><label>Họ và tên<input name="receiverName" defaultValue={user.fullName} maxLength="255" required /></label><label>Số điện thoại<input name="phone" minLength="8" maxLength="32" required /></label><label className="wide">Email<input name="email" type="email" defaultValue={user.email} /></label><label className="wide">Địa chỉ<input name="address" maxLength="500" required /></label><label className="wide">Ghi chú<textarea name="note" maxLength="1000" /></label></div><p>Thanh toán trực tuyến an toàn qua PayOS.</p><button className="button dark full" disabled={submitting}>{submitting ? 'Đang tạo thanh toán...' : 'Thanh toán ' + money(cart.totalAmount)}</button></form><aside className="order-summary"><h3>Đơn hàng của bạn</h3>{cart.items.map(item => <div className="checkout-item" key={item.id}><img src={item.thumbnailUrl || fallbackImage} alt="" /><div><b>{item.nameVi}</b><span><button type="button" onClick={() => change(item, item.quantity - 1)}>−</button> {item.quantity} <button type="button" disabled={item.quantity >= item.availableQuantity} onClick={() => change(item, item.quantity + 1)}>+</button></span><button type="button" className="link-danger" onClick={() => change(item, 0)}>Xóa</button></div><b>{money(item.lineTotal)}</b></div>)}<div className="summary-line total"><span>Tổng cộng</span><b>{money(cart.totalAmount)}</b></div></aside></div>}</main></Layout>
}
function Orders() {
  const { user, lang } = useStore(); const [page, setPage] = useState(null), [loading, setLoading] = useState(true)
  useEffect(() => { if (user) api.orders().then(setPage).finally(() => setLoading(false)); else setLoading(false) }, [user])
  return <Layout><main className="page"><div className="page-title"><span className="kicker">{pick(lang, 'Tài khoản', 'Account')}</span><h1>{pick(lang, 'Đơn hàng của tôi', 'My orders')}</h1></div>{loading ? <Loading /> : !user ? <Empty>{pick(lang, 'Vui lòng đăng nhập.', 'Please sign in.')}</Empty> : !page?.content.length ? <Empty>{pick(lang, 'Bạn chưa có đơn hàng.', 'You have no orders yet.')}</Empty> : <div className="order-list">{page.content.map(order => <Link className="order-row" to={`/orders/${order.id}`} key={order.id}><div><b>{order.orderCode}</b><small>{date(order.createdAt, lang)}</small></div><span>{order.orderStatus}</span><span>{order.paymentStatus}</span><b>{money(order.totalAmount, lang)}</b></Link>)}</div>}</main></Layout>
}
function LegacyOrders() {
  const { user } = useStore(); const [page, setPage] = useState(null), [loading, setLoading] = useState(true)
  useEffect(() => { if (user) api.orders().then(setPage).finally(() => setLoading(false)); else setLoading(false) }, [user])
  return <Layout><main className="page"><div className="page-title"><span className="kicker">Tài khoản</span><h1>Đơn hàng của tôi</h1></div>{loading ? <Loading /> : !user ? <Empty>Vui lòng <Link to="/account">đăng nhập</Link>.</Empty> : !page?.content.length ? <Empty>Bạn chưa có đơn hàng.</Empty> : <div className="order-list">{page.content.map(order => <Link className="order-row" to={`/orders/${order.id}`} key={order.id}><div><b>{order.orderCode}</b><small>{date(order.createdAt)}</small></div><span>{statusText[order.orderStatus] || order.orderStatus}</span><span>{statusText[order.paymentStatus] || order.paymentStatus}</span><b>{money(order.totalAmount)}</b></Link>)}</div>}</main></Layout>
}
function OrderDetail() {
  const { id } = useParams(); const [order, setOrder] = useState(null), [error, setError] = useState('')
  useEffect(() => { api.order(id).then(setOrder).catch(value => setError(value.message)) }, [id])
  return <Layout><main className="page"><div className="page-title"><span className="kicker">Chi tiết đơn hàng</span><h1>{order?.orderCode || 'Đơn hàng'}</h1></div>{error ? <Empty>{error}</Empty> : !order ? <Loading /> : <div className="order-detail"><section><h3>Thông tin nhận hàng</h3><p>{order.receiverName} · {order.phone}</p><p>{order.address}</p>{order.note && <p>Ghi chú: {order.note}</p>}<p>Trạng thái: <b>{statusText[order.orderStatus] || order.orderStatus}</b></p></section><aside className="order-summary">{order.items.map(item => <div className="summary-line" key={item.productId}><span>{item.productNameVi} × {item.quantity}</span><b>{money(item.totalPrice)}</b></div>)}<div className="summary-line total"><span>Tổng cộng</span><b>{money(order.totalAmount)}</b></div></aside></div>}</main></Layout>
}
function Support() { const { lang } = useStore(); return <Layout><main className="page support-page"><div className="page-title"><span className="kicker">{pick(lang, 'Hỗ trợ', 'Support')}</span><h1>{pick(lang, 'Liên hệ với chúng tôi', 'Contact us')}</h1></div><div className="support-layout"><section className="contact-card"><h2>{pick(lang, 'Cơ sở gốm Bàu Trúc Đàng Xem', 'Dang Xem Bau Truc Pottery')}</h2><p><b>Email:</b> <a href={`mailto:${contact.email}`}>{contact.email}</a></p><p><b>Zalo:</b> {contact.phones.map((phone, index) => <span key={phone}>{index > 0 && ' · '}<a href={`https://zalo.me/${phone}`} target="_blank" rel="noreferrer">{phone}</a></span>)}</p><p><b>Facebook:</b> <a href={contact.facebook} target="_blank" rel="noreferrer">{pick(lang, 'Xem trang Facebook', 'Open Facebook page')}</a></p><p><b>{pick(lang, 'Địa chỉ', 'Address')}:</b> <a href={contact.map} target="_blank" rel="noreferrer">{contact.address}</a></p></section><section className="faq-list"><details open><summary>{pick(lang, 'Gốm Bàu Trúc có dùng bàn xoay không?', 'Is Bau Truc pottery made with a wheel?')}</summary><p>{pick(lang, 'Không. Sản phẩm được tạo hình hoàn toàn bằng tay theo kỹ thuật truyền thống của người Chăm.', 'No. Every piece is shaped entirely by hand using traditional Cham techniques.')}</p></details><details><summary>{pick(lang, 'Làm sao theo dõi đơn hàng?', 'How can I track an order?')}</summary><p>{pick(lang, 'Đăng nhập và mở mục Đơn hàng trên thanh điều hướng.', 'Sign in and open Orders in the navigation bar.')}</p></details></section></div></main></Layout> }

function AdminPortal() {
  const store = useStore()
  async function logout() { try { await api.logout() } finally { store.setUser(null); window.location.assign('/') } }
  if (store.authLoading) return <div className="admin-state">Đang xác thực quyền quản trị...</div>
  return <Admin user={store.user} products={store.products} collections={store.collections} notify={store.notify} onLogout={logout} />
}

export default function App() { return <StoreProvider><Routes><Route path="/" element={<Home />} /><Route path="/products" element={<Products />} /><Route path="/login" element={<Account />} /><Route path="/account" element={<Account />} /><Route path="/checkout" element={<Checkout />} /><Route path="/orders" element={<Orders />} /><Route path="/orders/:id" element={<OrderDetail />} /><Route path="/support" element={<Support />} /><Route path="/admin" element={<AdminPortal />} /><Route path="*" element={<Home />} /></Routes></StoreProvider> }
