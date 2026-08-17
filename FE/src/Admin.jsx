import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from './api'
import { businessDateTimeLocal, businessLocalDateTimeToOffset, formatBusinessDateTime, localBusinessDate, monthStartBusinessDate } from './utils/businessDate'
import '../assets/css/admin.css'

const cash = value => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value || 0)
const when = (value, lang = 'vi') => formatBusinessDateTime(value, lang === 'en' ? 'en-US' : 'vi-VN')
const pageContent = value => value?.content || []
const adminText = (lang, vi, en) => lang === 'en' ? en : vi
async function loadAllAdminCollections() {
  const result = []
  let page = 0
  let last = false
  while (!last) {
    const response = await api.adminCollections(new URLSearchParams({ page, size: 100, sort: 'nameVi,asc' }).toString())
    result.push(...pageContent(response))
    last = response?.last ?? true
    page += 1
  }
  return result
}
function useImagePreviews(files) {
  const [previews, setPreviews] = useState([])
  useEffect(() => {
    const next = files.map(file => ({ name: file.name, url: URL.createObjectURL(file) }))
    setPreviews(next)
    return () => next.forEach(item => URL.revokeObjectURL(item.url))
  }, [files])
  return previews
}
const adminSections = [
  ['dashboard', 'Tổng quan', 'Dashboard'], ['products', 'Sản phẩm', 'Products'], ['collections', 'Bộ sưu tập', 'Collections'],
  ['workshop', 'Workshop', 'Workshop'], ['inventory', 'Kho hàng', 'Inventory'], ['orders', 'Đơn hàng', 'Orders'], ['users', 'Người dùng', 'Users'], ['notifications', 'Thông báo', 'Notifications'], ['settings', 'Cấu hình', 'Settings']
]
const adminSectionIds = new Set(adminSections.map(([id]) => id))
const adminSectionTitle = (lang, section) => {
  const match = adminSections.find(([id]) => id === section)
  return match ? adminText(lang, match[1], match[2]) : ''
}

export default function Admin({ user, notify, onLogout, lang = 'vi' }) {
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()
  const requestedSection = searchParams.get('section') || 'dashboard'
  const section = adminSectionIds.has(requestedSection) ? requestedSection : 'dashboard'
  useEffect(() => {
    if (requestedSection !== section) setSearchParams({ section }, { replace: true })
  }, [requestedSection, section, setSearchParams])
  if (!user) return <Navigate to="/" replace />
  if (user.role !== 'ADMIN') return <Navigate to="/" replace />
  return <div className="admin-shell"><aside className="admin-sidebar"><Link className="admin-brand" to="/">Đàng Xem<small>ADMIN CONSOLE</small></Link><nav>{adminSections.map(([id, labelVi, labelEn]) => <button type="button" className={section === id ? 'active' : ''} onClick={() => setSearchParams({ section: id })} key={id}>{adminText(lang, labelVi, labelEn)}</button>)}</nav><div className="admin-profile"><b>{user.fullName}</b><small>{user.email}</small><button type="button" onClick={onLogout}>{adminText(lang, 'Đăng xuất', 'Sign out')}</button></div></aside><main className="admin-main"><header className="admin-topbar"><div><span>{adminText(lang, 'QUẢN TRỊ CỬA HÀNG', 'STORE ADMIN')}</span><h1>{adminSectionTitle(lang, section)}</h1></div><div className="admin-topbar-actions"><NotificationBell notify={notify} onOpenList={() => setSearchParams({ section: 'notifications' })} lang={lang} /><button type="button" onClick={() => navigate(-1)}>{adminText(lang, 'Lùi', 'Back')}</button><Link to="/">{adminText(lang, 'Xem cửa hàng', 'View store')}</Link></div></header>{section === 'dashboard' && <Dashboard lang={lang} />}{section === 'products' && <Products notify={notify} lang={lang} />}{section === 'collections' && <Collections notify={notify} lang={lang} />}{section === 'workshop' && <WorkshopBookings notify={notify} lang={lang} />}{section === 'inventory' && <Inventory notify={notify} lang={lang} />}{section === 'orders' && <Orders notify={notify} lang={lang} />}{section === 'users' && <Users notify={notify} currentId={user.id} lang={lang} />}{section === 'notifications' && <Notifications notify={notify} lang={lang} />}{section === 'settings' && <SupportSettings notify={notify} lang={lang} />}</main></div>
}

function LoadState({ loading, error, empty, children, lang = 'vi' }) {
  if (loading) return <div className="admin-state">{adminText(lang, 'Đang tải dữ liệu...', 'Loading data...')}</div>
  if (error) return <div className="admin-state error">{error}</div>
  if (empty) return <div className="admin-state">{adminText(lang, 'Chưa có dữ liệu.', 'No data yet.')}</div>
  return children
}

function todayIso() { return localBusinessDate() }
function monthStartIso() { return monthStartBusinessDate() }

function Dashboard({ lang = 'vi' }) {
  const [data, setData] = useState(null), [error, setError] = useState('')
  useEffect(() => { api.adminDashboard().then(setData).catch(e => setError(e.message)) }, [])
  return <LoadState loading={!data && !error} error={error} lang={lang}><div className="metric-grid"><Metric label={adminText(lang, 'Tổng đơn hàng', 'Total orders')} value={data?.totalOrders} /><Metric label={adminText(lang, 'Doanh thu', 'Revenue')} value={cash(data?.totalRevenue)} /><Metric label={adminText(lang, 'Đơn hàng mới', 'New orders')} value={data?.newOrders} /><Metric label={adminText(lang, 'Sắp hết hàng', 'Low stock')} value={data?.lowStockProducts?.length || 0} /></div><div className="admin-grid"><Panel title={adminText(lang, 'Đơn hàng gần đây', 'Recent orders')}><SimpleTable heads={[adminText(lang, 'Mã đơn', 'Order code'), adminText(lang, 'Trạng thái', 'Status'), adminText(lang, 'Giá trị', 'Value')]} rows={(data?.recentOrders || []).map(x => [x.orderCode, x.orderStatus, cash(x.totalAmount)])} /></Panel><Panel title={adminText(lang, 'Sản phẩm bán chạy', 'Best selling products')}><SimpleTable heads={[adminText(lang, 'Sản phẩm', 'Product'), adminText(lang, 'Đã bán', 'Sold'), adminText(lang, 'Doanh thu', 'Revenue')]} rows={(data?.bestSellingProducts || []).map(x => [lang === 'vi' ? x.productNameVi : x.productNameEn || x.productNameVi, x.totalQuantity, cash(x.totalRevenue)])} /></Panel><Panel title={adminText(lang, 'Cảnh báo tồn kho', 'Inventory alerts')}><SimpleTable heads={[adminText(lang, 'Sản phẩm', 'Product'), adminText(lang, 'Khả dụng', 'Available')]} rows={(data?.lowStockProducts || []).map(x => [lang === 'vi' ? x.productNameVi : x.productNameEn || x.productNameVi, x.availableQuantity])} /></Panel></div><Reports lang={lang} /></LoadState>
}
function Metric({ label, value }) { return <article className="metric"><span>{label}</span><strong>{value ?? 0}</strong></article> }
function Panel({ title, children, action }) { return <section className="admin-panel"><div className="panel-head"><h2>{title}</h2>{action}</div>{children}</section> }
function SimpleTable({ heads, rows, actions, actionLabel = 'Thao tác' }) { return <div className="table-wrap"><table><thead><tr>{heads.map(x => <th key={x}>{x}</th>)}{actions && <th>{actionLabel}</th>}</tr></thead><tbody>{rows.map((row, i) => <tr key={i}>{row.map((cell, j) => <td key={j}>{cell ?? '—'}</td>)}{actions && <td>{actions(i)}</td>}</tr>)}</tbody></table></div> }

function Reports({ lang = 'vi' }) {
  const [filters, setFilters] = useState({ fromDate: monthStartIso(), toDate: todayIso(), groupBy: 'DAY', limit: 10 })
  const [revenue, setRevenue] = useState(null), [bestSelling, setBestSelling] = useState(null), [error, setError] = useState('')
  const load = useCallback(() => { setError(''); Promise.all([api.revenueReport(new URLSearchParams(filters).toString()), api.bestSellingReport(new URLSearchParams({ fromDate: filters.fromDate, toDate: filters.toDate, limit: filters.limit }).toString())]).then(([r, b]) => { setRevenue(r); setBestSelling(b) }).catch(e => setError(e.message)) }, [filters])
  useEffect(() => { load() }, [load])
  function submit(event) { event.preventDefault(); if (filters.fromDate > filters.toDate) return setError(adminText(lang, 'Ngày bắt đầu không được sau ngày kết thúc.', 'Start date cannot be after end date.')); load() }
  return <Panel title={adminText(lang, 'Báo cáo', 'Reports')}><form className="admin-search report-filters" onSubmit={submit}><input type="date" aria-label={adminText(lang, 'Từ ngày', 'From date')} value={filters.fromDate} onChange={e => setFilters({ ...filters, fromDate: e.target.value })} /><input type="date" aria-label={adminText(lang, 'Đến ngày', 'To date')} value={filters.toDate} onChange={e => setFilters({ ...filters, toDate: e.target.value })} /><select aria-label={adminText(lang, 'Nhóm theo', 'Group by')} value={filters.groupBy} onChange={e => setFilters({ ...filters, groupBy: e.target.value })}><option>DAY</option><option>WEEK</option><option>MONTH</option></select><input aria-label={adminText(lang, 'Giới hạn', 'Limit')} type="number" min="1" max="50" value={filters.limit} onChange={e => setFilters({ ...filters, limit: e.target.value })} /><button>{adminText(lang, 'Tải báo cáo', 'Load reports')}</button></form>{error && <div className="admin-state error">{error}</div>}<div className="admin-grid"><section><h3>{adminText(lang, 'Doanh thu', 'Revenue')}</h3><p className="report-total">{cash(revenue?.totalRevenue)}</p><SimpleTable heads={[adminText(lang, 'Kỳ', 'Period'), adminText(lang, 'Doanh thu', 'Revenue')]} rows={(revenue?.points || []).map(x => [x.periodStart, cash(x.revenue)])} /></section><section><h3>{adminText(lang, 'Bán chạy', 'Best selling')}</h3><SimpleTable heads={[adminText(lang, 'Sản phẩm', 'Product'), adminText(lang, 'Số lượng', 'Quantity')]} rows={(bestSelling || []).map(x => [lang === 'vi' ? x.productNameVi : x.productNameEn || x.productNameVi, x.soldQuantity])} /></section></div></Panel>
}

function NotificationBell({ notify, onOpenList, lang = 'vi' }) {
  const [open, setOpen] = useState(false), [page, setPage] = useState(null), [sseDown, setSseDown] = useState(false)
  const load = useCallback(() => api.adminNotifications('isRead=false&size=10&sort=createdAt,desc').then(setPage).catch(e => notify(e.message)), [notify])
  useEffect(() => {
    load()
    let fallback
    let source
    const clearFallback = () => {
      if (fallback) {
        window.clearInterval(fallback)
        fallback = undefined
      }
    }
    try {
      source = new EventSource(api.adminNotificationStreamUrl(), { withCredentials: true })
      source.onopen = () => { setSseDown(false); clearFallback() }
      source.onmessage = () => load()
      source.addEventListener('notification', () => load())
      source.onerror = () => { setSseDown(true); fallback = fallback || window.setInterval(load, 30000) }
    } catch {
      setSseDown(true); fallback = window.setInterval(load, 30000)
    }
    return () => { source?.close(); clearFallback() }
  }, [load])
  const rows = pageContent(page)
  async function read(item) { try { await api.adminReadNotification(item.id); load(); notify(adminText(lang, 'Đã đánh dấu thông báo đã đọc.', 'Notification marked as read.')) } catch (e) { notify(e.message) } }
  const unreadTotal = page?.totalElements || 0
  return <div className="notification-bell"><button type="button" onClick={() => setOpen(value => !value)} aria-label={adminText(lang, 'Thông báo', 'Notifications')}><span className="bell-mark" aria-hidden="true" />{unreadTotal > 0 && <span>{unreadTotal}</span>}</button>{open && <div className="notification-popover"><header><b>{adminText(lang, 'Thông báo mới', 'New notifications')}</b>{sseDown && <small>{adminText(lang, 'SSE gián đoạn, đang tự làm mới định kỳ.', 'SSE is interrupted; polling fallback is active.')}</small>}</header>{rows.length ? rows.map(item => <article key={item.id}><b>{item.title}</b><p>{item.message}</p><small>{when(item.createdAt, lang)}</small><button onClick={() => read(item)}>{adminText(lang, 'Đã đọc', 'Mark read')}</button></article>) : <p className="empty-copy">{adminText(lang, 'Không có thông báo chưa đọc.', 'No unread notifications.')}</p>}<button className="text-action" onClick={onOpenList}>{adminText(lang, 'Xem tất cả', 'View all')}</button></div>}</div>
}

function Products({ notify, lang = 'vi' }) {
  const [page, setPage] = useState(null), [collections, setCollections] = useState([])
  const [filters, setFilters] = useState({ keyword: '', status: '', collectionId: '', page: 0, size: 20, sort: 'createdAt,desc' })
  const [editing, setEditing] = useState(null), [open, setOpen] = useState(false), [error, setError] = useState('')
  const [selectedFiles, setSelectedFiles] = useState([])
  const previews = useImagePreviews(selectedFiles)
  const products = pageContent(page)
  const params = value => new URLSearchParams(Object.fromEntries(Object.entries(value).filter(([, v]) => v !== ''))).toString()
  const load = useCallback(() => {
    setError('')
    Promise.all([api.adminProducts(params(filters)), loadAllAdminCollections()])
      .then(([productPage, collectionOptions]) => { setPage(productPage); setCollections(collectionOptions) })
      .catch(e => setError(e.message))
  }, [filters])
  useEffect(() => { load() }, [load])
  const updateFilter = changes => setFilters(value => ({ ...value, ...changes, page: changes.page ?? 0 }))
  function openForm(product = null) {
    setEditing(product)
    setSelectedFiles([])
    setOpen(true)
  }
  function closeForm() {
    setOpen(false)
    setSelectedFiles([])
  }
  async function save(e) {
    e.preventDefault()
    if (!collections.length) return notify(adminText(lang, 'Bạn cần tạo ít nhất một bộ sưu tập trước khi lưu sản phẩm.', 'Create at least one collection before saving a product.'))
    const form = new FormData(e.currentTarget), files = form.getAll('images').filter(file => file.size > 0)
    const v = Object.fromEntries(form); delete v.images
    v.basePrice = Number(v.basePrice); v.collectionId = Number(v.collectionId)
    const discountType = v.discountType; const discountValue = v.discountValue; const startAt = v.startAt; const endAt = v.endAt; const isActive = Boolean(v.isActive)
    delete v.discountType; delete v.discountValue; delete v.startAt; delete v.endAt; delete v.isActive
    if (files.length > 10) return notify(adminText(lang, 'Mỗi sản phẩm được tải tối đa 10 hình ảnh.', 'Each product can upload at most 10 images.'))
    if (files.some(file => !file.type.startsWith('image/'))) return notify(adminText(lang, 'Chỉ chấp nhận tập tin hình ảnh.', 'Only image files are accepted.'))
    try {
      const product = editing ? await api.updateProduct(editing.id, v) : await api.createProduct(v)
      if (discountType && discountValue && startAt && endAt) await api.updateDiscount(product.id, { discountType, discountValue: Number(discountValue), startAt: businessLocalDateTimeToOffset(startAt), endAt: businessLocalDateTimeToOffset(endAt), isActive })
      const uploaded = []
      for (const file of files) uploaded.push(await api.uploadProductImage(product.id, file))
      if (uploaded.length && !product.images?.some(image => image.thumbnail)) await api.setProductThumbnail(product.id, uploaded[0].id)
      notify(files.length ? adminText(lang, `Đã lưu sản phẩm và tải lên ${files.length} hình ảnh.`, `Product saved and ${files.length} images uploaded.`) : adminText(lang, 'Đã lưu sản phẩm.', 'Product saved.'))
      closeForm(); load()
    } catch (x) { notify(x.message) }
  }
  async function imageAction(action) { try { await action(); notify(adminText(lang, 'Đã cập nhật hình ảnh.', 'Images updated.')); load() } catch (x) { notify(x.message) } }
  async function discountAction(action) { try { await action(); notify(adminText(lang, 'Đã cập nhật giảm giá.', 'Discount updated.')); load() } catch (x) { notify(x.message) } }
  async function remove(item) { if (!confirm(adminText(lang, `Xóa sản phẩm “${item.nameVi}”?`, `Delete product "${item.nameEn || item.nameVi}"?`))) return; try { await api.deleteProduct(item.id); notify(adminText(lang, 'Đã xóa sản phẩm.', 'Product deleted.')); load() } catch (x) { notify(x.message) } }
  const discount = editing?.discount
  return <Panel title={adminText(lang, 'Danh sách sản phẩm', 'Product list')} action={<button className="admin-primary" onClick={() => openForm()}>+ {adminText(lang, 'Thêm sản phẩm', 'Add product')}</button>}><form className="admin-search" onSubmit={e => { e.preventDefault(); load() }}><input value={filters.keyword} onChange={e => updateFilter({ keyword: e.target.value })} placeholder={adminText(lang, 'Tìm sản phẩm', 'Search products')} /><select value={filters.status} onChange={e => updateFilter({ status: e.target.value })}><option value="">{adminText(lang, 'Mọi trạng thái', 'All statuses')}</option><option value="ACTIVE">ACTIVE</option><option value="INACTIVE">INACTIVE</option></select><select value={filters.collectionId} onChange={e => updateFilter({ collectionId: e.target.value })}><option value="">{adminText(lang, 'Mọi bộ sưu tập', 'All collections')}</option>{collections.map(x => <option key={x.id} value={x.id}>{lang === 'vi' ? x.nameVi : x.nameEn || x.nameVi}</option>)}</select><select value={filters.sort} onChange={e => updateFilter({ sort: e.target.value })}><option value="createdAt,desc">{adminText(lang, 'Mới nhất', 'Newest')}</option><option value="nameVi,asc">{adminText(lang, 'Tên A-Z', 'Name A-Z')}</option><option value="basePrice,asc">{adminText(lang, 'Giá thấp', 'Lowest price')}</option><option value="basePrice,desc">{adminText(lang, 'Giá cao', 'Highest price')}</option></select><button>{adminText(lang, 'Tải', 'Load')}</button></form><LoadState loading={!page && !error} error={error} empty={page && !products.length} lang={lang}><SimpleTable heads={[adminText(lang, 'Hình', 'Image'), adminText(lang, 'Tên sản phẩm', 'Product name'), adminText(lang, 'Giá bán', 'Selling price'), adminText(lang, 'Trạng thái', 'Status')]} rows={products.map(x => [<img className="admin-product-thumb" src={x.thumbnailUrl || '/assets/images/vase.jpg'} alt="" />, <><b>{lang === 'vi' ? x.nameVi : x.nameEn || x.nameVi}</b><small>{lang === 'vi' ? x.nameEn : x.nameVi}</small></>, cash(x.sellingPrice), <Status value={x.status} />])} actionLabel={adminText(lang, 'Thao tác', 'Actions')} actions={i => <><button onClick={() => openForm(products[i])}>{adminText(lang, 'Sửa', 'Edit')}</button><button className="danger" onClick={() => remove(products[i])}>{adminText(lang, 'Xóa', 'Delete')}</button></>} />{page && <nav className="pagination"><button disabled={page.first} onClick={() => updateFilter({ page: page.page - 1 })}>{adminText(lang, 'Trước', 'Previous')}</button><span>{adminText(lang, 'Trang', 'Page')} {page.page + 1}/{Math.max(page.totalPages, 1)} · {page.totalElements} {adminText(lang, 'sản phẩm', 'products')}</span><button disabled={page.last} onClick={() => updateFilter({ page: page.page + 1 })}>{adminText(lang, 'Sau', 'Next')}</button></nav>}</LoadState>{open && <Modal title={editing ? adminText(lang, 'Cập nhật sản phẩm', 'Update product') : adminText(lang, 'Thêm sản phẩm', 'Add product')} close={closeForm}><form className="admin-form" onSubmit={save}><label>{adminText(lang, 'Tên tiếng Việt', 'Vietnamese name')}<input name="nameVi" defaultValue={editing?.nameVi} required /></label><label>{adminText(lang, 'Tên tiếng Anh', 'English name')}<input name="nameEn" defaultValue={editing?.nameEn} required /></label><label>{adminText(lang, 'Mô tả VI', 'Vietnamese description')}<textarea name="descriptionVi" defaultValue={editing?.descriptionVi} /></label><label>{adminText(lang, 'Mô tả EN', 'English description')}<textarea name="descriptionEn" defaultValue={editing?.descriptionEn} /></label><label>{adminText(lang, 'Giá gốc', 'Base price')}<input name="basePrice" type="number" min="1" defaultValue={editing?.basePrice} required /></label><label>{adminText(lang, 'Bộ sưu tập', 'Collection')}<select name="collectionId" defaultValue={editing?.collectionId || ''} required disabled={!collections.length}>{collections.length ? collections.map(x => <option value={x.id} key={x.id}>{lang === 'vi' ? x.nameVi : x.nameEn || x.nameVi}</option>) : <option value="">{adminText(lang, 'Chưa có bộ sưu tập', 'No collections yet')}</option>}</select>{!collections.length && <small className="admin-help error">{adminText(lang, 'Vui lòng tạo bộ sưu tập trước khi thêm sản phẩm.', 'Create a collection before adding a product.')}</small>}</label><label>{adminText(lang, 'Trạng thái', 'Status')}<select name="status" defaultValue={editing?.status || 'ACTIVE'}><option>ACTIVE</option><option>INACTIVE</option></select></label><fieldset className="admin-fieldset"><legend>{adminText(lang, 'Giảm giá', 'Discount')}</legend>{discount && <p className="admin-help">{adminText(lang, 'Hiện tại', 'Current')}: {discount.discountType} · {discount.discountValue} · {discount.isActive ? adminText(lang, 'đang bật', 'enabled') : adminText(lang, 'đang tắt', 'disabled')}</p>}<label>{adminText(lang, 'Loại', 'Type')}<select name="discountType" defaultValue={discount?.discountType || ''}><option value="">{adminText(lang, 'Không thay đổi', 'No change')}</option><option>PERCENTAGE</option><option>FIXED_PRICE</option></select></label><label>{adminText(lang, 'Giá trị', 'Value')}<input name="discountValue" type="number" min="1" step="0.01" defaultValue={discount?.discountValue || ''} /></label><label>{adminText(lang, 'Bắt đầu', 'Start')}<input name="startAt" type="datetime-local" defaultValue={businessDateTimeLocal(discount?.startAt)} /></label><label>{adminText(lang, 'Kết thúc', 'End')}<input name="endAt" type="datetime-local" defaultValue={businessDateTimeLocal(discount?.endAt)} /></label><label className="check-row"><input name="isActive" type="checkbox" defaultChecked={discount?.isActive ?? true} /> {adminText(lang, 'Đang bật', 'Enabled')}</label>{editing && <div className="row-actions wide"><button type="button" onClick={() => discountAction(() => api.toggleDiscount(editing.id, false))}>{adminText(lang, 'Tắt discount', 'Disable discount')}</button><button type="button" onClick={() => discountAction(() => api.toggleDiscount(editing.id, true))}>{adminText(lang, 'Bật discount', 'Enable discount')}</button><button type="button" className="danger" onClick={() => discountAction(() => api.deleteDiscount(editing.id))}>{adminText(lang, 'Xóa discount', 'Delete discount')}</button></div>}</fieldset><label className="image-picker">{adminText(lang, 'Hình ảnh sản phẩm', 'Product images')}<input name="images" type="file" accept="image/jpeg,image/png,image/webp" multiple onChange={e => setSelectedFiles(Array.from(e.target.files || []))} /><small>{adminText(lang, 'JPEG, PNG hoặc WebP · tối đa 10 hình · ảnh đầu tiên làm ảnh đại diện', 'JPEG, PNG, or WebP · up to 10 images · first image becomes thumbnail')}</small></label>{previews.length > 0 && <div className="image-preview-block"><b>{adminText(lang, 'Ảnh sắp tải lên', 'Images to upload')}</b><div className="image-preview-grid">{previews.map(item => <figure key={item.url}><img src={item.url} alt={item.name} /><figcaption>{item.name}</figcaption></figure>)}</div></div>}{editing?.images?.length > 0 && <div className="existing-images"><b>{adminText(lang, 'Ảnh hiện có', 'Existing images')}</b>{editing.images.map((image, index) => <figure key={image.id}><img src={image.url} alt="" /><figcaption>{image.thumbnail ? adminText(lang, 'Ảnh đại diện', 'Thumbnail') : `${adminText(lang, 'Ảnh', 'Image')} ${index + 1}`}</figcaption><div className="row-actions"><button type="button" onClick={() => imageAction(() => api.setProductThumbnail(editing.id, image.id))}>{adminText(lang, 'Đại diện', 'Thumbnail')}</button><button type="button" disabled={index === 0} onClick={() => { const ids = editing.images.map(x => x.id); [ids[index - 1], ids[index]] = [ids[index], ids[index - 1]]; imageAction(() => api.reorderProductImages(editing.id, ids)) }}>{adminText(lang, 'Lên', 'Up')}</button><button type="button" disabled={index === editing.images.length - 1} onClick={() => { const ids = editing.images.map(x => x.id); [ids[index + 1], ids[index]] = [ids[index], ids[index + 1]]; imageAction(() => api.reorderProductImages(editing.id, ids)) }}>{adminText(lang, 'Xuống', 'Down')}</button><button type="button" className="danger" onClick={() => imageAction(() => api.deleteProductImage(editing.id, image.id))}>{adminText(lang, 'Xóa', 'Delete')}</button></div></figure>)}</div>}<button className="admin-primary" disabled={!collections.length}>{adminText(lang, 'Lưu sản phẩm', 'Save product')}</button></form></Modal>}</Panel>
}

function Collections({ notify, lang = 'vi' }) {
  const [page, setPage] = useState(null), [filters, setFilters] = useState({ keyword: '', status: '', page: 0, size: 20, sort: 'createdAt,desc' })
  const [editing, setEditing] = useState(null), [open, setOpen] = useState(false), [error, setError] = useState('')
  const rows = pageContent(page)
  const params = value => new URLSearchParams(Object.fromEntries(Object.entries(value).filter(([, v]) => v !== ''))).toString()
  const load = useCallback(() => api.adminCollections(params(filters)).then(setPage).catch(e => setError(e.message)), [filters])
  useEffect(() => { load() }, [load])
  const updateFilter = changes => setFilters(value => ({ ...value, ...changes, page: changes.page ?? 0 }))
  async function save(e) { e.preventDefault(); const v = Object.fromEntries(new FormData(e.currentTarget)); try { editing ? await api.updateCollection(editing.id, v) : await api.createCollection(v); notify(adminText(lang, 'Đã lưu bộ sưu tập.', 'Collection saved.')); setOpen(false); load() } catch (x) { notify(x.message) } }
  async function remove(item) { if (!confirm(adminText(lang, `Xóa bộ sưu tập “${item.nameVi}”?`, `Delete collection "${item.nameEn || item.nameVi}"?`))) return; try { await api.deleteCollection(item.id); notify(adminText(lang, 'Đã xóa bộ sưu tập.', 'Collection deleted.')); load() } catch (x) { notify(x.message) } }
  return <Panel title={adminText(lang, 'Bộ sưu tập', 'Collections')} action={<button className="admin-primary" onClick={() => { setEditing(null); setOpen(true) }}>+ {adminText(lang, 'Thêm bộ sưu tập', 'Add collection')}</button>}><form className="admin-search" onSubmit={e => { e.preventDefault(); load() }}><input value={filters.keyword} onChange={e => updateFilter({ keyword: e.target.value })} placeholder={adminText(lang, 'Tìm bộ sưu tập', 'Search collections')} /><select value={filters.status} onChange={e => updateFilter({ status: e.target.value })}><option value="">{adminText(lang, 'Mọi trạng thái', 'All statuses')}</option><option value="ACTIVE">ACTIVE</option><option value="INACTIVE">INACTIVE</option></select><select value={filters.sort} onChange={e => updateFilter({ sort: e.target.value })}><option value="createdAt,desc">{adminText(lang, 'Mới nhất', 'Newest')}</option><option value="nameVi,asc">{adminText(lang, 'Tên A-Z', 'Name A-Z')}</option><option value="status,asc">{adminText(lang, 'Trạng thái', 'Status')}</option></select><button>{adminText(lang, 'Tải', 'Load')}</button></form><LoadState loading={!page && !error} error={error} empty={page && !rows.length} lang={lang}><SimpleTable heads={[adminText(lang, 'Tên VI', 'Vietnamese name'), adminText(lang, 'Tên EN', 'English name'), adminText(lang, 'Trạng thái', 'Status')]} rows={rows.map(x => [x.nameVi, x.nameEn, <Status value={x.status} />])} actionLabel={adminText(lang, 'Thao tác', 'Actions')} actions={i => <><button onClick={() => { setEditing(rows[i]); setOpen(true) }}>{adminText(lang, 'Sửa', 'Edit')}</button><button className="danger" onClick={() => remove(rows[i])}>{adminText(lang, 'Xóa', 'Delete')}</button></>} />{page && <nav className="pagination"><button disabled={page.first} onClick={() => updateFilter({ page: page.page - 1 })}>{adminText(lang, 'Trước', 'Previous')}</button><span>{adminText(lang, 'Trang', 'Page')} {page.page + 1}/{Math.max(page.totalPages, 1)} · {page.totalElements} {adminText(lang, 'bộ sưu tập', 'collections')}</span><button disabled={page.last} onClick={() => updateFilter({ page: page.page + 1 })}>{adminText(lang, 'Sau', 'Next')}</button></nav>}</LoadState>{open && <Modal title={adminText(lang, 'Bộ sưu tập', 'Collection')} close={() => setOpen(false)}><form className="admin-form" onSubmit={save}><label>{adminText(lang, 'Tên tiếng Việt', 'Vietnamese name')}<input name="nameVi" defaultValue={editing?.nameVi} required /></label><label>{adminText(lang, 'Tên tiếng Anh', 'English name')}<input name="nameEn" defaultValue={editing?.nameEn} required /></label><label>{adminText(lang, 'Mô tả VI', 'Vietnamese description')}<textarea name="descriptionVi" defaultValue={editing?.descriptionVi} /></label><label>{adminText(lang, 'Mô tả EN', 'English description')}<textarea name="descriptionEn" defaultValue={editing?.descriptionEn} /></label><label>{adminText(lang, 'Trạng thái', 'Status')}<select name="status" defaultValue={editing?.status || 'ACTIVE'}><option>ACTIVE</option><option>INACTIVE</option></select></label><button className="admin-primary">{adminText(lang, 'Lưu', 'Save')}</button></form></Modal>}</Panel>
}

function WorkshopBookings({ notify, lang = 'vi' }) {
  const [offerings, setOfferings] = useState([]), [editing, setEditing] = useState(null), [open, setOpen] = useState(false)
  const [imagePreview, setImagePreview] = useState('')
  const [data, setData] = useState(null), [status, setStatus] = useState('')
  const load = useCallback(() => api.adminWorkshopBookings(new URLSearchParams({ size: 100, sort: 'createdAt,desc', ...(status ? { status } : {}) }).toString()).then(setData).catch(e => notify(e.message)), [notify, status])
  const loadOfferings = useCallback(() => api.adminWorkshops().then(setOfferings).catch(e => notify(e.message)), [notify])
  useEffect(() => { load(); loadOfferings() }, [load, loadOfferings])
  const rows = pageContent(data)
  function openOfferingForm(item = null) {
    setEditing(item)
    setImagePreview(item?.imageUrl || '')
    setOpen(true)
  }
  function closeOfferingForm() {
    setOpen(false)
    setEditing(null)
    setImagePreview('')
  }
  async function saveOffering(e) {
    e.preventDefault()
    const v = Object.fromEntries(new FormData(e.currentTarget))
    v.priceAmount = Number(v.priceAmount); v.durationMinutes = Number(v.durationMinutes); v.maxParticipants = Number(v.maxParticipants)
    try {
      editing ? await api.updateWorkshop(editing.id, v) : await api.createWorkshop(v)
      closeOfferingForm(); loadOfferings(); notify(adminText(lang, 'Đã lưu workshop.', 'Workshop saved.'))
    } catch (x) { notify(x.message) }
  }
  async function removeOffering(item) { if (!confirm(adminText(lang, `Xóa workshop “${item.title}”?`, `Delete workshop "${item.title}"?`))) return; try { await api.deleteWorkshop(item.id); loadOfferings(); notify(adminText(lang, 'Đã xóa workshop.', 'Workshop deleted.')) } catch (x) { notify(x.message) } }
  async function change(item, nextStatus) {
    try {
      await api.adminWorkshopStatus(item.id, nextStatus)
      load()
      notify(adminText(lang, 'Đã cập nhật lịch workshop.', 'Workshop booking updated.'))
    } catch (x) { notify(x.message) }
  }
  const byId = new Map(offerings.map(x => [x.id, x.title]))
  return <><Panel title={adminText(lang, 'Gói workshop', 'Workshop offerings')} action={<button className="admin-primary" onClick={() => openOfferingForm()}>+ {adminText(lang, 'Thêm workshop', 'Add workshop')}</button>}><SimpleTable heads={['Workshop', adminText(lang, 'Giá', 'Price'), adminText(lang, 'Thời lượng', 'Duration'), adminText(lang, 'Sức chứa', 'Capacity'), adminText(lang, 'Trạng thái', 'Status')]} rows={offerings.map(x => [<><b>{x.title}</b><small>{x.description}</small></>, cash(x.priceAmount), `${x.durationMinutes} ${adminText(lang, 'phút', 'minutes')}`, `${x.maxParticipants} ${adminText(lang, 'người', 'people')}`, <Status value={x.status} />])} actionLabel={adminText(lang, 'Thao tác', 'Actions')} actions={i => <><button onClick={() => openOfferingForm(offerings[i])}>{adminText(lang, 'Sửa', 'Edit')}</button><button className="danger" onClick={() => removeOffering(offerings[i])}>{adminText(lang, 'Xóa', 'Delete')}</button></>} />{open && <Modal title={editing ? adminText(lang, 'Cập nhật workshop', 'Update workshop') : adminText(lang, 'Thêm workshop', 'Add workshop')} close={closeOfferingForm}><form className="admin-form" onSubmit={saveOffering}><label>{adminText(lang, 'Tên workshop', 'Workshop name')}<input name="title" defaultValue={editing?.title} maxLength="255" required /></label><label>{adminText(lang, 'Giá', 'Price')}<input name="priceAmount" type="number" min="0" defaultValue={editing?.priceAmount ?? 0} required /></label><label className="wide">{adminText(lang, 'Mô tả', 'Description')}<textarea name="description" defaultValue={editing?.description} maxLength="2000" required /></label><label>{adminText(lang, 'Thời lượng phút', 'Duration in minutes')}<input name="durationMinutes" type="number" min="1" max="1440" defaultValue={editing?.durationMinutes ?? 120} required /></label><label>{adminText(lang, 'Sức chứa tối đa', 'Maximum capacity')}<input name="maxParticipants" type="number" min="1" max="100" defaultValue={editing?.maxParticipants ?? 10} required /></label><label className="wide">{adminText(lang, 'URL hình ảnh', 'Image URL')}<input name="imageUrl" defaultValue={editing?.imageUrl} maxLength="1024" placeholder="/assets/images/artisan.jpg" onChange={e => setImagePreview(e.target.value.trim())} /></label>{imagePreview && <div className="image-preview-block"><b>{adminText(lang, 'Ảnh hiển thị trên trang workshop', 'Image shown on workshop page')}</b><div className="single-image-preview"><img src={imagePreview} alt={adminText(lang, 'Xem trước workshop', 'Workshop preview')} /></div></div>}<label>{adminText(lang, 'Trạng thái', 'Status')}<select name="status" defaultValue={editing?.status || 'ACTIVE'}><option>ACTIVE</option><option>INACTIVE</option></select></label><button className="admin-primary">{adminText(lang, 'Lưu workshop', 'Save workshop')}</button></form></Modal>}</Panel><Panel title={adminText(lang, 'Lịch hẹn workshop', 'Workshop bookings')} action={<form className="admin-search" onSubmit={e => { e.preventDefault(); load() }}><select value={status} onChange={e => setStatus(e.target.value)}><option value="">{adminText(lang, 'Tất cả', 'All')}</option><option value="NEW">{adminText(lang, 'Mới', 'New')}</option><option value="CONFIRMED">{adminText(lang, 'Đã xác nhận', 'Confirmed')}</option><option value="CANCELLED">{adminText(lang, 'Đã hủy', 'Cancelled')}</option><option value="COMPLETED">{adminText(lang, 'Hoàn thành', 'Completed')}</option></select><button>{adminText(lang, 'Lọc', 'Filter')}</button></form>}><SimpleTable heads={[adminText(lang, 'Khách', 'Customer'), 'Workshop', adminText(lang, 'Liên hệ', 'Contact'), adminText(lang, 'Thời gian', 'Time'), adminText(lang, 'Số người', 'Participants'), adminText(lang, 'Trạng thái', 'Status')]} rows={rows.map(x => [<><b>{x.fullName}</b><small>{x.note}</small></>, x.workshopId ? byId.get(x.workshopId) || `#${x.workshopId}` : adminText(lang, 'Tư vấn chung', 'General consultation'), <><span>{x.phone}</span><small>{x.email}</small></>, when(x.preferredAt, lang), x.participants, <Status value={x.status} />])} actionLabel={adminText(lang, 'Thao tác', 'Actions')} actions={i => { const x = rows[i]; return <div className="row-actions"><button disabled={x.status === 'CONFIRMED'} onClick={() => change(x, 'CONFIRMED')}>{adminText(lang, 'Xác nhận', 'Confirm')}</button><button disabled={x.status === 'COMPLETED'} onClick={() => change(x, 'COMPLETED')}>{adminText(lang, 'Hoàn thành', 'Complete')}</button><button className="danger" disabled={x.status === 'CANCELLED'} onClick={() => change(x, 'CANCELLED')}>{adminText(lang, 'Hủy', 'Cancel')}</button></div> }} /></Panel></>
}

function Inventory({ notify, lang = 'vi' }) {
  const [data, setData] = useState(null)
  const [selected, setSelected] = useState(null)
  const [history, setHistory] = useState(null)
  const [searchKeyword, setSearchKeyword] = useState('')

  const [filters, setFilters] = useState({
    keyword: '',
    status: '',
    page: 0,
    size: 20,
    sort: 'productId,asc'
  })

  const load = useCallback(() => {
    const query = new URLSearchParams(
      Object.fromEntries(
        Object.entries(filters).filter(([, value]) => value !== '')
      )
    ).toString()

    api.adminInventory(query)
      .then(setData)
      .catch(e => notify(e.message))
  }, [filters, notify])

  const updateFilter = changes => {
    setFilters(value => ({
      ...value,
      ...changes,
      page: changes.page ?? 0
    }))
  }

  function submitSearch(event) {
    event.preventDefault()

    setFilters(value => ({
      ...value,
      keyword: searchKeyword.trim(),
      page: 0
    }))
  }

  useEffect(() => {
    load()
  }, [load])

  function openAdjust(item) {
    setSelected(item)

    api.adminInventoryHistory(item.productId, 'size=20')
      .then(setHistory)
      .catch(e => notify(e.message))
  }

  async function adjust(e) {
    e.preventDefault()

    const value = Object.fromEntries(
      new FormData(e.currentTarget)
    )

    value.quantityChange = Number(value.quantityChange)

    try {
      await api.adminAdjustInventory(
        selected.productId,
        value
      )

      setSelected(null)
      load()
      notify(adminText(lang, 'Đã cập nhật tồn kho.', 'Inventory updated.'))
    } catch (e) {
      notify(e.message)
    }
  }

  const rows = pageContent(data)

  return (
    <Panel
      title={adminText(lang, 'Tồn kho', 'Inventory')}
      action={
        <form
          className="admin-search"
          onSubmit={submitSearch}
        >
          <input
            value={searchKeyword}
            onChange={e => setSearchKeyword(e.target.value)}
            placeholder={adminText(lang, 'Tìm sản phẩm', 'Search products')}
          />

          <select
            value={filters.status}
            onChange={e =>
              updateFilter({
                status: e.target.value
              })
            }
          >
            <option value="">{adminText(lang, 'Mọi trạng thái', 'All statuses')}</option>
            <option value="IN_STOCK">
              IN_STOCK
            </option>
            <option value="LOW_STOCK">
              LOW_STOCK
            </option>
            <option value="OUT_OF_STOCK">
              OUT_OF_STOCK
            </option>
          </select>

          <select
            value={filters.sort}
            onChange={e =>
              updateFilter({
                sort: e.target.value
              })
            }
          >
            <option value="productId,asc">
              {adminText(lang, 'Product ID tăng', 'Product ID ascending')}
            </option>

            <option value="productId,desc">
              {adminText(lang, 'Product ID giảm', 'Product ID descending')}
            </option>

            <option value="quantity,asc">
              {adminText(lang, 'Tổng kho tăng', 'Quantity ascending')}
            </option>

            <option value="quantity,desc">
              {adminText(lang, 'Tổng kho giảm', 'Quantity descending')}
            </option>

            <option value="availableQuantity,asc">
              {adminText(lang, 'Khả dụng tăng', 'Available ascending')}
            </option>

            <option value="availableQuantity,desc">
              {adminText(lang, 'Khả dụng giảm', 'Available descending')}
            </option>
          </select>

          <button type="submit">
            {adminText(lang, 'Tìm', 'Search')}
          </button>
        </form>
      }
    >
      <SimpleTable
        heads={[
          adminText(lang, 'Sản phẩm', 'Product'),
          adminText(lang, 'Tổng', 'Quantity'),
          adminText(lang, 'Đang giữ', 'Reserved'),
          adminText(lang, 'Khả dụng', 'Available'),
          adminText(lang, 'Trạng thái', 'Status')
        ]}
        rows={rows.map(x => [
          x.productNameVi,
          x.quantity,
          x.reservedQuantity,
          x.availableQuantity,
          <Status value={x.status} />
        ])}
        actions={i => (
          <button
            onClick={() => openAdjust(rows[i])}
          >
            {adminText(lang, 'Điều chỉnh', 'Adjust')}
          </button>
        )}
        actionLabel={adminText(lang, 'Thao tác', 'Actions')}
      />

      {data && (
        <nav className="pagination">
          <button
            disabled={data.first}
            onClick={() =>
              updateFilter({
                page: Math.max(0, data.page - 1)
              })
            }
          >
            {adminText(lang, 'Trước', 'Previous')}
          </button>

          <span>
            {adminText(lang, 'Trang', 'Page')} {data.page + 1}/
            {Math.max(data.totalPages, 1)}
            {' · '}
            {data.totalElements} {adminText(lang, 'sản phẩm', 'products')}
          </span>

          <button
            disabled={data.last}
            onClick={() =>
              updateFilter({
                page: data.page + 1
              })
            }
          >
            {adminText(lang, 'Sau', 'Next')}
          </button>
        </nav>
      )}

      {selected && (
        <Modal
          title={`${adminText(lang, 'Điều chỉnh', 'Adjust')}: ${selected.productNameVi}`}
          close={() => {
            setSelected(null)
            setHistory(null)
          }}
        >
          <form
            className="admin-form"
            onSubmit={adjust}
          >
            <label>
              {adminText(lang, 'Loại', 'Type')}

              <select name="type">
                <option>IMPORT</option>
                <option>ADJUSTMENT</option>
              </select>
            </label>

            <label>
              {adminText(lang, 'Số lượng thay đổi', 'Quantity change')}

              <input
                name="quantityChange"
                type="number"
                required
              />
            </label>

            <label>
              {adminText(lang, 'Lý do', 'Reason')}

              <textarea
                name="reason"
                required
              />
            </label>

            <button className="admin-primary">
              {adminText(lang, 'Xác nhận', 'Confirm')}
            </button>
          </form>

          <section className="history-block">
            <h3>{adminText(lang, 'Lịch sử tồn kho', 'Inventory history')}</h3>

            <SimpleTable
              heads={[
                adminText(lang, 'Loại', 'Type'),
                adminText(lang, 'Tổng', 'Quantity'),
                adminText(lang, 'Đang giữ', 'Reserved'),
                adminText(lang, 'Tham chiếu', 'Reference'),
                adminText(lang, 'Lý do', 'Reason'),
                adminText(lang, 'Ngày', 'Date')
              ]}
              rows={pageContent(history).map(x => [
                x.type,
                `${x.beforeQuantity} → ${x.afterQuantity} (${x.quantityDelta})`,
                `${x.beforeReservedQuantity} → ${x.afterReservedQuantity} (${x.reservedQuantityDelta})`,
                x.referenceType
                  ? `${x.referenceType} #${x.referenceId || ''}`
                  : '—',
                x.reason,
                when(x.createdAt, lang)
              ])}
            />
          </section>
        </Modal>
      )}
    </Panel>
  )
}

function Orders({ notify, lang = 'vi' }) {
  const [data, setData] = useState(null), [detail, setDetail] = useState(null)
  const [filters, setFilters] = useState({ keyword: '', orderStatus: '', paymentStatus: '', fromDate: '', toDate: '', page: 0, size: 20, sort: 'createdAt,desc' })
  const params = value => new URLSearchParams(Object.fromEntries(Object.entries(value).filter(([, v]) => v !== ''))).toString()
  const load = useCallback(() => api.adminOrders(params(filters)).then(setData).catch(e => notify(e.message)), [filters, notify])
  useEffect(() => { load() }, [load])
  const updateFilter = changes => setFilters(value => ({ ...value, ...changes, page: changes.page ?? 0 }))
  const rows = pageContent(data)
  async function transition(status) { try { setDetail(await api.adminOrderStatus(detail.id, status)); load(); notify(adminText(lang, 'Đã cập nhật đơn hàng.', 'Order updated.')) } catch (x) { notify(x.message) } }
  async function refund() { if (!confirm(adminText(lang, 'Chức năng này chỉ ghi nhận refund đã được thực hiện bên ngoài hệ thống. Ứng dụng không gửi refund tới payOS.', 'This only records a refund already completed outside the system. The application does not send a refund to payOS.'))) return; const note = prompt(adminText(lang, 'Ghi chú refund', 'Refund note')) || ''; try { const payment = await api.adminManualRefund(detail.paymentId, note); setDetail({ ...detail, paymentStatus: payment.status }); load(); notify(adminText(lang, 'Đã ghi nhận hoàn tiền thủ công.', 'Manual refund recorded.')) } catch (x) { notify(x.message) } }
  return <Panel title={adminText(lang, 'Đơn hàng', 'Orders')}><form className="admin-search" onSubmit={e => { e.preventDefault(); load() }}><input value={filters.keyword} onChange={e => updateFilter({ keyword: e.target.value })} placeholder={adminText(lang, 'Tìm mã đơn, tên, SĐT', 'Search order code, name, phone')} /><select value={filters.orderStatus} onChange={e => updateFilter({ orderStatus: e.target.value })}><option value="">{adminText(lang, 'Mọi trạng thái đơn', 'All order statuses')}</option><option>NEW</option><option>CONFIRMED</option><option>COMPLETED</option><option>CANCELLED</option></select><select value={filters.paymentStatus} onChange={e => updateFilter({ paymentStatus: e.target.value })}><option value="">{adminText(lang, 'Mọi thanh toán', 'All payment statuses')}</option><option>PENDING</option><option>PAID</option><option>FAILED</option><option>CANCELLED</option><option>EXPIRED</option><option>REFUNDED</option></select><input type="date" aria-label={adminText(lang, 'Từ ngày', 'From date')} value={filters.fromDate} onChange={e => updateFilter({ fromDate: e.target.value })} /><input type="date" aria-label={adminText(lang, 'Đến ngày', 'To date')} value={filters.toDate} onChange={e => updateFilter({ toDate: e.target.value })} /><select value={filters.sort} onChange={e => updateFilter({ sort: e.target.value })}><option value="createdAt,desc">{adminText(lang, 'Mới nhất', 'Newest')}</option><option value="createdAt,asc">{adminText(lang, 'Cũ nhất', 'Oldest')}</option><option value="totalAmount,desc">{adminText(lang, 'Giá trị cao', 'Highest value')}</option><option value="totalAmount,asc">{adminText(lang, 'Giá trị thấp', 'Lowest value')}</option></select><button>{adminText(lang, 'Tải', 'Load')}</button></form><SimpleTable heads={[adminText(lang, 'Mã đơn', 'Order code'), adminText(lang, 'Ngày tạo', 'Created at'), adminText(lang, 'Đơn hàng', 'Order'), adminText(lang, 'Thanh toán', 'Payment'), adminText(lang, 'Tổng tiền', 'Total')]} rows={rows.map(x => [x.orderCode, when(x.createdAt, lang), <Status value={x.orderStatus} />, <Status value={x.paymentStatus} />, cash(x.totalAmount)])} actionLabel={adminText(lang, 'Thao tác', 'Actions')} actions={i => <button onClick={() => api.adminOrder(rows[i].id).then(setDetail).catch(e => notify(e.message))}>{adminText(lang, 'Chi tiết', 'Detail')}</button>} />{data && <nav className="pagination"><button disabled={data.first} onClick={() => updateFilter({ page: data.page - 1 })}>{adminText(lang, 'Trước', 'Previous')}</button><span>{adminText(lang, 'Trang', 'Page')} {data.page + 1}/{Math.max(data.totalPages, 1)} · {data.totalElements} {adminText(lang, 'đơn hàng', 'orders')}</span><button disabled={data.last} onClick={() => updateFilter({ page: data.page + 1 })}>{adminText(lang, 'Sau', 'Next')}</button></nav>}{detail && <Modal title={`${adminText(lang, 'Đơn hàng', 'Order')} ${detail.orderCode}`} close={() => setDetail(null)}><div className="order-admin-detail"><p><b>{adminText(lang, 'Người nhận', 'Receiver')}:</b> {detail.receiverName} · {detail.phone}</p><p><b>{adminText(lang, 'Địa chỉ', 'Address')}:</b> {detail.address}</p><p><b>{adminText(lang, 'Đơn hàng', 'Order')}:</b> <Status value={detail.orderStatus} /> <b>{adminText(lang, 'Thanh toán', 'Payment')}:</b> <Status value={detail.paymentStatus} /></p>{detail.items.map(x => <p key={x.productId}>{lang === 'vi' ? x.productNameVi : x.productNameEn || x.productNameVi} × {x.quantity} <b>{cash(x.totalPrice)}</b></p>)}<h3>{adminText(lang, 'Tổng cộng', 'Total')}: {cash(detail.totalAmount)}</h3><div className="admin-actions"><button onClick={() => transition('CONFIRMED')}>{adminText(lang, 'Xác nhận', 'Confirm')}</button><button onClick={() => transition('COMPLETED')}>{adminText(lang, 'Hoàn thành', 'Complete')}</button><button className="danger" onClick={() => api.adminCancelOrder(detail.id).then(setDetail).then(load).catch(e => notify(e.message))}>{adminText(lang, 'Hủy đơn', 'Cancel order')}</button>{detail.orderStatus === 'CANCELLED' && detail.paymentStatus === 'PAID' && <button className="danger" onClick={refund}>{adminText(lang, 'Ghi nhận refund', 'Record refund')}</button>}</div></div></Modal>}</Panel>
}

function Users({ notify, currentId, lang = 'vi' }) {
  const [data, setData] = useState(null)
  const [filters, setFilters] = useState({ keyword: '', role: '', status: '', page: 0, size: 20, sort: 'createdAt,desc' })
  const params = value => new URLSearchParams(Object.fromEntries(Object.entries(value).filter(([, v]) => v !== ''))).toString()
  const load = useCallback(() => api.adminUsers(params(filters)).then(setData).catch(e => notify(e.message)), [filters, notify])
  useEffect(() => { load() }, [load])
  const updateFilter = changes => setFilters(value => ({ ...value, ...changes, page: changes.page ?? 0 }))
  const rows = pageContent(data)
  async function action(item, name) { if (item.id === currentId && ['demote', 'block'].includes(name) && !confirm(adminText(lang, 'Thao tác trên chính tài khoản hiện tại. Tiếp tục?', 'This changes your current account. Continue?'))) return; try { await api.adminUserAction(item.id, name); load(); notify(adminText(lang, 'Đã cập nhật người dùng.', 'User updated.')) } catch (x) { notify(x.message) } }
  return <Panel title={adminText(lang, 'Người dùng', 'Users')}><form className="admin-search" onSubmit={e => { e.preventDefault(); load() }}><input value={filters.keyword} onChange={e => updateFilter({ keyword: e.target.value })} placeholder={adminText(lang, 'Tìm email hoặc tên', 'Search email or name')} /><select value={filters.role} onChange={e => updateFilter({ role: e.target.value })}><option value="">{adminText(lang, 'Mọi vai trò', 'All roles')}</option><option>USER</option><option>ADMIN</option></select><select value={filters.status} onChange={e => updateFilter({ status: e.target.value })}><option value="">{adminText(lang, 'Mọi trạng thái', 'All statuses')}</option><option>ACTIVE</option><option>BLOCKED</option></select><select value={filters.sort} onChange={e => updateFilter({ sort: e.target.value })}><option value="createdAt,desc">{adminText(lang, 'Mới nhất', 'Newest')}</option><option value="email,asc">Email A-Z</option><option value="email,desc">Email Z-A</option></select><button>{adminText(lang, 'Tải', 'Load')}</button></form><SimpleTable heads={[adminText(lang, 'Người dùng', 'User'), 'Email', adminText(lang, 'Vai trò', 'Role'), adminText(lang, 'Trạng thái', 'Status'), adminText(lang, 'Ngày tạo', 'Created at')]} rows={rows.map(x => [x.fullName, x.email, x.role, <Status value={x.status} />, when(x.createdAt, lang)])} actionLabel={adminText(lang, 'Thao tác', 'Actions')} actions={i => { const x = rows[i]; return <div className="row-actions">{x.role === 'USER' ? <button onClick={() => action(x, 'promote')}>{adminText(lang, 'Cấp Admin', 'Promote')}</button> : <button onClick={() => action(x, 'demote')}>{adminText(lang, 'Hạ quyền', 'Demote')}</button>}{x.status === 'ACTIVE' ? <button className="danger" onClick={() => action(x, 'block')}>{adminText(lang, 'Chặn', 'Block')}</button> : <button onClick={() => action(x, 'unblock')}>{adminText(lang, 'Bỏ chặn', 'Unblock')}</button>}</div> }} />{data && <nav className="pagination"><button disabled={data.first} onClick={() => updateFilter({ page: data.page - 1 })}>{adminText(lang, 'Trước', 'Previous')}</button><span>{adminText(lang, 'Trang', 'Page')} {data.page + 1}/{Math.max(data.totalPages, 1)} · {data.totalElements} {adminText(lang, 'người dùng', 'users')}</span><button disabled={data.last} onClick={() => updateFilter({ page: data.page + 1 })}>{adminText(lang, 'Sau', 'Next')}</button></nav>}</Panel>
}

function Notifications({ notify, lang = 'vi' }) {
  const [data, setData] = useState(null)
  const [unreadTotal, setUnreadTotal] = useState(0)
  const [filters, setFilters] = useState({ isRead: '', type: '', page: 0, size: 20, sort: 'createdAt,desc' })

  const load = useCallback(() => {
    const listQuery = new URLSearchParams(
      Object.fromEntries(Object.entries(filters).filter(([, value]) => value !== ''))
    ).toString()
    const unreadQuery = new URLSearchParams({ isRead: 'false', page: 0, size: 1, sort: 'createdAt,desc' }).toString()

    Promise.all([
      api.adminNotifications(listQuery),
      api.adminNotifications(unreadQuery)
    ])
      .then(([listPage, unreadPage]) => {
        setData(listPage)
        setUnreadTotal(unreadPage?.totalElements || 0)
      })
      .catch(e => notify(e.message))
  }, [filters, notify])

  useEffect(() => { load() }, [load])

  const updateFilter = changes => setFilters(value => ({
    ...value,
    ...changes,
    page: changes.page ?? 0
  }))

  async function read(item) {
    try {
      await api.adminReadNotification(item.id)
      load()
      notify(adminText(lang, 'Đã đánh dấu thông báo đã đọc.', 'Notification marked as read.'))
    } catch (e) {
      notify(e.message)
    }
  }

  const rows = pageContent(data)
  return <Panel title={`${adminText(lang, 'Thông báo', 'Notifications')} (${unreadTotal} ${adminText(lang, 'chưa đọc', 'unread')})`}><form className="admin-search" onSubmit={e => { e.preventDefault(); load() }}><select value={filters.isRead} onChange={e => updateFilter({ isRead: e.target.value })}><option value="">{adminText(lang, 'Mọi trạng thái đọc', 'All read states')}</option><option value="false">{adminText(lang, 'Chưa đọc', 'Unread')}</option><option value="true">{adminText(lang, 'Đã đọc', 'Read')}</option></select><select value={filters.type} onChange={e => updateFilter({ type: e.target.value })}><option value="">{adminText(lang, 'Mọi loại', 'All types')}</option><option>NEW_ORDER</option><option>PAYMENT_SUCCESS</option><option>PAYMENT_FAILED</option><option>LOW_STOCK</option><option>OUT_OF_STOCK</option></select><select value={filters.sort} onChange={e => updateFilter({ sort: e.target.value })}><option value="createdAt,desc">{adminText(lang, 'Mới nhất', 'Newest')}</option><option value="createdAt,asc">{adminText(lang, 'Cũ nhất', 'Oldest')}</option></select><button>{adminText(lang, 'Tải', 'Load')}</button></form><div className="notification-list">{rows.map(x => <article className={x.isRead ? '' : 'unread'} key={x.id}><div><b>{x.title}</b><p>{x.message}</p><small>{when(x.createdAt, lang)}</small></div>{!x.isRead && <button onClick={() => read(x)}>{adminText(lang, 'Đánh dấu đã đọc', 'Mark as read')}</button>}</article>)}</div>{data && <nav className="pagination"><button disabled={data.first} onClick={() => updateFilter({ page: Math.max(0, data.page - 1) })}>{adminText(lang, 'Trước', 'Previous')}</button><span>{adminText(lang, 'Trang', 'Page')} {data.page + 1}/{Math.max(data.totalPages, 1)} · {data.totalElements} {adminText(lang, 'thông báo', 'notifications')}</span><button disabled={data.last} onClick={() => updateFilter({ page: data.page + 1 })}>{adminText(lang, 'Sau', 'Next')}</button></nav>}</Panel>
}

function SupportSettings({ notify, lang = 'vi' }) {
  const [settings, setSettings] = useState(null)
  useEffect(() => { api.adminSupportSettings().then(setSettings).catch(e => notify(e.message)) }, [notify])
  async function save(e) {
    e.preventDefault()
    const values = Object.fromEntries(new FormData(e.currentTarget))
    try {
      setSettings(await api.updateSupportSettings(values))
      notify(adminText(lang, 'Đã lưu thông tin hỗ trợ.', 'Support information saved.'))
    } catch (x) { notify(x.message) }
  }
  return <Panel title={adminText(lang, 'Thông tin hỗ trợ', 'Support information')}>{!settings ? <div className="admin-state">{adminText(lang, 'Đang tải dữ liệu...', 'Loading data...')}</div> : <form className="admin-form" onSubmit={save}><label>Email<input name="email" type="email" defaultValue={settings.email} maxLength="320" required /></label><label>{adminText(lang, 'Zalo chính', 'Primary Zalo')}<input name="zaloPhone" defaultValue={settings.zaloPhone} maxLength="32" required /></label><label>{adminText(lang, 'Số điện thoại phụ', 'Secondary phone')}<input name="secondaryPhone" defaultValue={settings.secondaryPhone || ''} maxLength="32" /></label><label>{adminText(lang, 'Giờ mở cửa', 'Opening hours')}<input name="openingHours" defaultValue={settings.openingHours || ''} maxLength="255" /></label><label className="wide">Facebook URL<input name="facebookUrl" defaultValue={settings.facebookUrl || ''} maxLength="1024" /></label><label className="wide">{adminText(lang, 'Địa chỉ', 'Address')}<input name="address" defaultValue={settings.address} maxLength="500" required /></label><label className="wide">Google Map URL<input name="mapUrl" defaultValue={settings.mapUrl || ''} maxLength="1024" /></label><button className="admin-primary">{adminText(lang, 'Lưu thông tin hỗ trợ', 'Save support information')}</button></form>}</Panel>
}

function Status({ value }) { return <span className={`admin-status ${String(value).toLowerCase()}`}>{value}</span> }
function Modal({ title, close, children }) { return <div className="admin-modal" onMouseDown={e => e.target === e.currentTarget && close()}><div><header><h2>{title}</h2><button onClick={close}>×</button></header>{children}</div></div> }
