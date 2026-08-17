import { useEffect, useState } from 'react'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from './api'
import '../assets/css/admin.css'

const cash = value => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value || 0)
const when = value => value ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : '—'
const pageContent = value => value?.content || []
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
  ['dashboard', 'Tổng quan'], ['products', 'Sản phẩm'], ['collections', 'Bộ sưu tập'],
  ['workshop', 'Workshop'], ['inventory', 'Kho hàng'], ['orders', 'Đơn hàng'], ['users', 'Người dùng'], ['notifications', 'Thông báo'], ['settings', 'Cấu hình']
]
const adminSectionIds = new Set(adminSections.map(([id]) => id))

export default function Admin({ user, products, collections, notify, onLogout }) {
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()
  const requestedSection = searchParams.get('section') || 'dashboard'
  const section = adminSectionIds.has(requestedSection) ? requestedSection : 'dashboard'
  useEffect(() => {
    if (requestedSection !== section) setSearchParams({ section }, { replace: true })
  }, [requestedSection, section, setSearchParams])
  if (!user) return <Navigate to="/" replace />
  if (user.role !== 'ADMIN') return <Navigate to="/" replace />
  return <div className="admin-shell"><aside className="admin-sidebar"><Link className="admin-brand" to="/">Đàng Xem<small>ADMIN CONSOLE</small></Link><nav>{adminSections.map(([id, label]) => <button type="button" className={section === id ? 'active' : ''} onClick={() => setSearchParams({ section: id })} key={id}>{label}</button>)}</nav><div className="admin-profile"><b>{user.fullName}</b><small>{user.email}</small><button type="button" onClick={onLogout}>Đăng xuất</button></div></aside><main className="admin-main"><header className="admin-topbar"><div><span>QUẢN TRỊ CỬA HÀNG</span><h1>{adminSections.find(item => item[0] === section)?.[1]}</h1></div><div className="admin-topbar-actions"><button type="button" onClick={() => navigate(-1)}>Lùi</button><Link to="/">Xem cửa hàng</Link></div></header>{section === 'dashboard' && <Dashboard />}{section === 'products' && <Products products={products} collections={collections} notify={notify} />}{section === 'collections' && <Collections collections={collections} notify={notify} />}{section === 'workshop' && <WorkshopBookings notify={notify} />}{section === 'inventory' && <Inventory notify={notify} />}{section === 'orders' && <Orders notify={notify} />}{section === 'users' && <Users notify={notify} currentId={user.id} />}{section === 'notifications' && <Notifications notify={notify} />}{section === 'settings' && <SupportSettings notify={notify} />}</main></div>
}

function LoadState({ loading, error, empty, children }) {
  if (loading) return <div className="admin-state">Đang tải dữ liệu...</div>
  if (error) return <div className="admin-state error">{error}</div>
  if (empty) return <div className="admin-state">Chưa có dữ liệu.</div>
  return children
}

function Dashboard() {
  const [data, setData] = useState(null), [error, setError] = useState('')
  useEffect(() => { api.adminDashboard().then(setData).catch(e => setError(e.message)) }, [])
  return <LoadState loading={!data && !error} error={error}><div className="metric-grid"><Metric label="Tổng đơn hàng" value={data?.totalOrders} /><Metric label="Doanh thu" value={cash(data?.totalRevenue)} /><Metric label="Đơn hàng mới" value={data?.newOrders} /><Metric label="Sắp hết hàng" value={data?.lowStockProducts?.length || 0} /></div><div className="admin-grid"><Panel title="Đơn hàng gần đây"><SimpleTable heads={['Mã đơn', 'Trạng thái', 'Giá trị']} rows={(data?.recentOrders || []).map(x => [x.orderCode, x.orderStatus, cash(x.totalAmount)])} /></Panel><Panel title="Sản phẩm bán chạy"><SimpleTable heads={['Sản phẩm', 'Đã bán', 'Doanh thu']} rows={(data?.bestSellingProducts || []).map(x => [x.productNameVi, x.totalQuantity, cash(x.totalRevenue)])} /></Panel><Panel title="Cảnh báo tồn kho"><SimpleTable heads={['Sản phẩm', 'Khả dụng']} rows={(data?.lowStockProducts || []).map(x => [x.productNameVi, x.availableQuantity])} /></Panel></div></LoadState>
}
function Metric({ label, value }) { return <article className="metric"><span>{label}</span><strong>{value ?? 0}</strong></article> }
function Panel({ title, children, action }) { return <section className="admin-panel"><div className="panel-head"><h2>{title}</h2>{action}</div>{children}</section> }
function SimpleTable({ heads, rows, actions }) { return <div className="table-wrap"><table><thead><tr>{heads.map(x => <th key={x}>{x}</th>)}{actions && <th>Thao tác</th>}</tr></thead><tbody>{rows.map((row, i) => <tr key={i}>{row.map((cell, j) => <td key={j}>{cell ?? '—'}</td>)}{actions && <td>{actions(i)}</td>}</tr>)}</tbody></table></div> }

function Products({ products, collections, notify }) {
  const [editing, setEditing] = useState(null), [open, setOpen] = useState(false)
  const [selectedFiles, setSelectedFiles] = useState([])
  const previews = useImagePreviews(selectedFiles)
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
    if (!collections.length) return notify('Bạn cần tạo ít nhất một bộ sưu tập trước khi lưu sản phẩm.')
    const form = new FormData(e.currentTarget), files = form.getAll('images').filter(file => file.size > 0)
    const v = Object.fromEntries(form); delete v.images
    v.basePrice = Number(v.basePrice); v.collectionId = Number(v.collectionId)
    if (files.length > 10) return notify('Mỗi sản phẩm được tải tối đa 10 hình ảnh.')
    if (files.some(file => !file.type.startsWith('image/'))) return notify('Chỉ chấp nhận tập tin hình ảnh.')
    try {
      const product = editing ? await api.updateProduct(editing.id, v) : await api.createProduct(v)
      const uploaded = []
      for (const file of files) uploaded.push(await api.uploadProductImage(product.id, file))
      if (uploaded.length && !product.images?.some(image => image.thumbnail)) await api.setProductThumbnail(product.id, uploaded[0].id)
      notify(files.length ? `Đã lưu sản phẩm và tải lên ${files.length} hình ảnh.` : 'Đã lưu sản phẩm.')
      closeForm(); window.location.reload()
    } catch (x) { notify(x.message) }
  }
  async function remove(item) { if (!confirm(`Xóa sản phẩm “${item.nameVi}”?`)) return; try { await api.deleteProduct(item.id); notify('Đã xóa sản phẩm.'); window.location.reload() } catch (x) { notify(x.message) } }
  return <Panel title="Danh sách sản phẩm" action={<button className="admin-primary" onClick={() => openForm()}>+ Thêm sản phẩm</button>}><SimpleTable heads={['Hình', 'Tên sản phẩm', 'Giá bán', 'Trạng thái']} rows={products.map(x => [<img className="admin-product-thumb" src={x.thumbnailUrl || '/assets/images/vase.jpg'} alt="" />, <><b>{x.nameVi}</b><small>{x.nameEn}</small></>, cash(x.sellingPrice), <Status value={x.status} />])} actions={i => <><button onClick={() => openForm(products[i])}>Sửa</button><button className="danger" onClick={() => remove(products[i])}>Xóa</button></>} />{open && <Modal title={editing ? 'Cập nhật sản phẩm' : 'Thêm sản phẩm'} close={closeForm}><form className="admin-form" onSubmit={save}><label>Tên tiếng Việt<input name="nameVi" defaultValue={editing?.nameVi} required /></label><label>Tên tiếng Anh<input name="nameEn" defaultValue={editing?.nameEn} required /></label><label>Mô tả VI<textarea name="descriptionVi" defaultValue={editing?.descriptionVi} /></label><label>Mô tả EN<textarea name="descriptionEn" defaultValue={editing?.descriptionEn} /></label><label>Giá gốc<input name="basePrice" type="number" min="1" defaultValue={editing?.basePrice} required /></label><label>Bộ sưu tập<select name="collectionId" defaultValue={editing?.collectionId || ''} required disabled={!collections.length}>{collections.length ? collections.map(x => <option value={x.id} key={x.id}>{x.nameVi}</option>) : <option value="">Chưa có bộ sưu tập</option>}</select>{!collections.length && <small className="admin-help error">Vui lòng tạo bộ sưu tập trước khi thêm sản phẩm.</small>}</label><label>Trạng thái<select name="status" defaultValue={editing?.status || 'ACTIVE'}><option>ACTIVE</option><option>INACTIVE</option></select></label><label className="image-picker">Hình ảnh sản phẩm<input name="images" type="file" accept="image/jpeg,image/png,image/webp" multiple onChange={e => setSelectedFiles(Array.from(e.target.files || []))} /><small>JPEG, PNG hoặc WebP · tối đa 10 hình · ảnh đầu tiên làm ảnh đại diện</small></label>{previews.length > 0 && <div className="image-preview-block"><b>Ảnh sắp tải lên</b><div className="image-preview-grid">{previews.map(item => <figure key={item.url}><img src={item.url} alt={item.name} /><figcaption>{item.name}</figcaption></figure>)}</div></div>}{editing?.images?.length > 0 && <div className="existing-images"><b>Ảnh hiện có</b>{editing.images.map(image => <img src={image.url} alt="" key={image.id} />)}</div>}<button className="admin-primary" disabled={!collections.length}>Lưu sản phẩm</button></form></Modal>}</Panel>
}

function Collections({ collections, notify }) {
  const [editing, setEditing] = useState(null), [open, setOpen] = useState(false)
  async function save(e) { e.preventDefault(); const v = Object.fromEntries(new FormData(e.currentTarget)); try { editing ? await api.updateCollection(editing.id, v) : await api.createCollection(v); notify('Đã lưu bộ sưu tập.'); window.location.reload() } catch (x) { notify(x.message) } }
  return <Panel title="Bộ sưu tập" action={<button className="admin-primary" onClick={() => { setEditing(null); setOpen(true) }}>+ Thêm bộ sưu tập</button>}><SimpleTable heads={['Tên VI', 'Tên EN', 'Trạng thái']} rows={collections.map(x => [x.nameVi, x.nameEn, <Status value={x.status} />])} actions={i => <button onClick={() => { setEditing(collections[i]); setOpen(true) }}>Sửa</button>} />{open && <Modal title="Bộ sưu tập" close={() => setOpen(false)}><form className="admin-form" onSubmit={save}><label>Tên tiếng Việt<input name="nameVi" defaultValue={editing?.nameVi} required /></label><label>Tên tiếng Anh<input name="nameEn" defaultValue={editing?.nameEn} required /></label><label>Mô tả VI<textarea name="descriptionVi" defaultValue={editing?.descriptionVi} /></label><label>Mô tả EN<textarea name="descriptionEn" defaultValue={editing?.descriptionEn} /></label><label>Trạng thái<select name="status" defaultValue={editing?.status || 'ACTIVE'}><option>ACTIVE</option><option>INACTIVE</option></select></label><button className="admin-primary">Lưu</button></form></Modal>}</Panel>
}

function WorkshopBookings({ notify }) {
  const [offerings, setOfferings] = useState([]), [editing, setEditing] = useState(null), [open, setOpen] = useState(false)
  const [imagePreview, setImagePreview] = useState('')
  const [data, setData] = useState(null), [status, setStatus] = useState('')
  const params = () => new URLSearchParams({ size: 100, sort: 'createdAt,desc', ...(status ? { status } : {}) }).toString()
  const load = () => api.adminWorkshopBookings(params()).then(setData).catch(e => notify(e.message))
  const loadOfferings = () => api.adminWorkshops().then(setOfferings).catch(e => notify(e.message))
  useEffect(() => { load(); loadOfferings() }, [])
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
      closeOfferingForm(); loadOfferings(); notify('Đã lưu workshop.')
    } catch (x) { notify(x.message) }
  }
  async function removeOffering(item) { if (!confirm(`Xóa workshop “${item.title}”?`)) return; try { await api.deleteWorkshop(item.id); loadOfferings(); notify('Đã xóa workshop.') } catch (x) { notify(x.message) } }
  async function change(item, nextStatus) {
    try {
      await api.adminWorkshopStatus(item.id, nextStatus)
      load()
      notify('Đã cập nhật lịch workshop.')
    } catch (x) { notify(x.message) }
  }
  const byId = new Map(offerings.map(x => [x.id, x.title]))
  return <><Panel title="Gói workshop" action={<button className="admin-primary" onClick={() => openOfferingForm()}>+ Thêm workshop</button>}><SimpleTable heads={['Workshop', 'Giá', 'Thời lượng', 'Sức chứa', 'Trạng thái']} rows={offerings.map(x => [<><b>{x.title}</b><small>{x.description}</small></>, cash(x.priceAmount), `${x.durationMinutes} phút`, `${x.maxParticipants} người`, <Status value={x.status} />])} actions={i => <><button onClick={() => openOfferingForm(offerings[i])}>Sửa</button><button className="danger" onClick={() => removeOffering(offerings[i])}>Xóa</button></>} />{open && <Modal title={editing ? 'Cập nhật workshop' : 'Thêm workshop'} close={closeOfferingForm}><form className="admin-form" onSubmit={saveOffering}><label>Tên workshop<input name="title" defaultValue={editing?.title} maxLength="255" required /></label><label>Giá<input name="priceAmount" type="number" min="0" defaultValue={editing?.priceAmount ?? 0} required /></label><label className="wide">Mô tả<textarea name="description" defaultValue={editing?.description} maxLength="2000" required /></label><label>Thời lượng phút<input name="durationMinutes" type="number" min="1" max="1440" defaultValue={editing?.durationMinutes ?? 120} required /></label><label>Sức chứa tối đa<input name="maxParticipants" type="number" min="1" max="100" defaultValue={editing?.maxParticipants ?? 10} required /></label><label className="wide">URL hình ảnh<input name="imageUrl" defaultValue={editing?.imageUrl} maxLength="1024" placeholder="/assets/images/artisan.jpg" onChange={e => setImagePreview(e.target.value.trim())} /></label>{imagePreview && <div className="image-preview-block"><b>Ảnh hiển thị trên trang workshop</b><div className="single-image-preview"><img src={imagePreview} alt="Xem trước workshop" /></div></div>}<label>Trạng thái<select name="status" defaultValue={editing?.status || 'ACTIVE'}><option>ACTIVE</option><option>INACTIVE</option></select></label><button className="admin-primary">Lưu workshop</button></form></Modal>}</Panel><Panel title="Lịch hẹn workshop" action={<form className="admin-search" onSubmit={e => { e.preventDefault(); load() }}><select value={status} onChange={e => setStatus(e.target.value)}><option value="">Tất cả</option><option value="NEW">Mới</option><option value="CONFIRMED">Đã xác nhận</option><option value="CANCELLED">Đã hủy</option><option value="COMPLETED">Hoàn thành</option></select><button>Lọc</button></form>}><SimpleTable heads={['Khách', 'Workshop', 'Liên hệ', 'Thời gian', 'Số người', 'Trạng thái']} rows={rows.map(x => [<><b>{x.fullName}</b><small>{x.note}</small></>, x.workshopId ? byId.get(x.workshopId) || `#${x.workshopId}` : 'Tư vấn chung', <><span>{x.phone}</span><small>{x.email}</small></>, when(x.preferredAt), x.participants, <Status value={x.status} />])} actions={i => { const x = rows[i]; return <div className="row-actions"><button disabled={x.status === 'CONFIRMED'} onClick={() => change(x, 'CONFIRMED')}>Xác nhận</button><button disabled={x.status === 'COMPLETED'} onClick={() => change(x, 'COMPLETED')}>Hoàn thành</button><button className="danger" disabled={x.status === 'CANCELLED'} onClick={() => change(x, 'CANCELLED')}>Hủy</button></div> }} /></Panel></>
}

function Inventory({ notify }) {
  const [data, setData] = useState(null), [selected, setSelected] = useState(null), [query, setQuery] = useState('')
  const load = () => api.adminInventory(new URLSearchParams({ keyword: query, size: 100 })).then(setData).catch(e => notify(e.message))
  useEffect(() => { load() }, [])
  async function adjust(e) { e.preventDefault(); const v = Object.fromEntries(new FormData(e.currentTarget)); v.quantityChange = Number(v.quantityChange); try { await api.adminAdjustInventory(selected.productId, v); setSelected(null); load(); notify('Đã cập nhật tồn kho.') } catch (x) { notify(x.message) } }
  const rows = pageContent(data)
  return <Panel title="Tồn kho" action={<form className="admin-search" onSubmit={e => { e.preventDefault(); load() }}><input value={query} onChange={e => setQuery(e.target.value)} placeholder="Tìm sản phẩm" /><button>Tìm</button></form>}><SimpleTable heads={['Sản phẩm', 'Tổng', 'Đang giữ', 'Khả dụng', 'Trạng thái']} rows={rows.map(x => [x.productNameVi, x.quantity, x.reservedQuantity, x.availableQuantity, <Status value={x.status} />])} actions={i => <button onClick={() => setSelected(rows[i])}>Điều chỉnh</button>} />{selected && <Modal title={`Điều chỉnh: ${selected.productNameVi}`} close={() => setSelected(null)}><form className="admin-form" onSubmit={adjust}><label>Loại<select name="type"><option>IMPORT</option><option>ADJUSTMENT</option></select></label><label>Số lượng thay đổi<input name="quantityChange" type="number" required /></label><label>Lý do<textarea name="reason" required /></label><button className="admin-primary">Xác nhận</button></form></Modal>}</Panel>
}

function Orders({ notify }) {
  const [data, setData] = useState(null), [detail, setDetail] = useState(null)
  const load = () => api.adminOrders('size=100&sort=createdAt,desc').then(setData).catch(e => notify(e.message)); useEffect(() => { load() }, [])
  const rows = pageContent(data)
  async function transition(status) { try { setDetail(await api.adminOrderStatus(detail.id, status)); load(); notify('Đã cập nhật đơn hàng.') } catch (x) { notify(x.message) } }
  return <Panel title="Đơn hàng"><SimpleTable heads={['Mã đơn', 'Ngày tạo', 'Đơn hàng', 'Thanh toán', 'Tổng tiền']} rows={rows.map(x => [x.orderCode, when(x.createdAt), <Status value={x.orderStatus} />, <Status value={x.paymentStatus} />, cash(x.totalAmount)])} actions={i => <button onClick={() => api.adminOrder(rows[i].id).then(setDetail).catch(e => notify(e.message))}>Chi tiết</button>} />{detail && <Modal title={`Đơn hàng ${detail.orderCode}`} close={() => setDetail(null)}><div className="order-admin-detail"><p><b>Người nhận:</b> {detail.receiverName} · {detail.phone}</p><p><b>Địa chỉ:</b> {detail.address}</p>{detail.items.map(x => <p key={x.productId}>{x.productNameVi} × {x.quantity} <b>{cash(x.totalPrice)}</b></p>)}<h3>Tổng cộng: {cash(detail.totalAmount)}</h3><div className="admin-actions"><button onClick={() => transition('CONFIRMED')}>Xác nhận</button><button onClick={() => transition('COMPLETED')}>Hoàn thành</button><button className="danger" onClick={() => api.adminCancelOrder(detail.id).then(setDetail).then(load).catch(e => notify(e.message))}>Hủy đơn</button></div></div></Modal>}</Panel>
}

function Users({ notify, currentId }) {
  const [data, setData] = useState(null); const load = () => api.adminUsers('size=100&sort=createdAt,desc').then(setData).catch(e => notify(e.message)); useEffect(() => { load() }, [])
  const rows = pageContent(data)
  async function action(item, name) { if (item.id === currentId && ['demote', 'block'].includes(name) && !confirm('Thao tác trên chính tài khoản hiện tại. Tiếp tục?')) return; try { await api.adminUserAction(item.id, name); load(); notify('Đã cập nhật người dùng.') } catch (x) { notify(x.message) } }
  return <Panel title="Người dùng"><SimpleTable heads={['Người dùng', 'Email', 'Vai trò', 'Trạng thái', 'Ngày tạo']} rows={rows.map(x => [x.fullName, x.email, x.role, <Status value={x.status} />, when(x.createdAt)])} actions={i => { const x = rows[i]; return <div className="row-actions">{x.role === 'USER' ? <button onClick={() => action(x, 'promote')}>Cấp Admin</button> : <button onClick={() => action(x, 'demote')}>Hạ quyền</button>}{x.status === 'ACTIVE' ? <button className="danger" onClick={() => action(x, 'block')}>Chặn</button> : <button onClick={() => action(x, 'unblock')}>Bỏ chặn</button>}</div> }} /></Panel>
}

function Notifications({ notify }) {
  const [data, setData] = useState(null); const load = () => api.adminNotifications('size=100&sort=createdAt,desc').then(setData).catch(e => notify(e.message)); useEffect(() => { load() }, [])
  const rows = pageContent(data)
  return <Panel title={`Thông báo (${rows.filter(x => !x.isRead).length} chưa đọc)`}><div className="notification-list">{rows.map(x => <article className={x.isRead ? '' : 'unread'} key={x.id}><div><b>{x.title}</b><p>{x.message}</p><small>{when(x.createdAt)}</small></div>{!x.isRead && <button onClick={() => api.adminReadNotification(x.id).then(load).catch(e => notify(e.message))}>Đánh dấu đã đọc</button>}</article>)}</div></Panel>
}

function SupportSettings({ notify }) {
  const [settings, setSettings] = useState(null)
  useEffect(() => { api.adminSupportSettings().then(setSettings).catch(e => notify(e.message)) }, [])
  async function save(e) {
    e.preventDefault()
    const values = Object.fromEntries(new FormData(e.currentTarget))
    try {
      setSettings(await api.updateSupportSettings(values))
      notify('Đã lưu thông tin hỗ trợ.')
    } catch (x) { notify(x.message) }
  }
  return <Panel title="Thông tin hỗ trợ">{!settings ? <div className="admin-state">Đang tải dữ liệu...</div> : <form className="admin-form" onSubmit={save}><label>Email<input name="email" type="email" defaultValue={settings.email} maxLength="320" required /></label><label>Zalo chính<input name="zaloPhone" defaultValue={settings.zaloPhone} maxLength="32" required /></label><label>Số điện thoại phụ<input name="secondaryPhone" defaultValue={settings.secondaryPhone || ''} maxLength="32" /></label><label>Giờ mở cửa<input name="openingHours" defaultValue={settings.openingHours || ''} maxLength="255" /></label><label className="wide">Facebook URL<input name="facebookUrl" defaultValue={settings.facebookUrl || ''} maxLength="1024" /></label><label className="wide">Địa chỉ<input name="address" defaultValue={settings.address} maxLength="500" required /></label><label className="wide">Google Map URL<input name="mapUrl" defaultValue={settings.mapUrl || ''} maxLength="1024" /></label><button className="admin-primary">Lưu thông tin hỗ trợ</button></form>}</Panel>
}

function Status({ value }) { return <span className={`admin-status ${String(value).toLowerCase()}`}>{value}</span> }
function Modal({ title, close, children }) { return <div className="admin-modal" onMouseDown={e => e.target === e.currentTarget && close()}><div><header><h2>{title}</h2><button onClick={close}>×</button></header>{children}</div></div> }
