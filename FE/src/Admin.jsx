import { useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { api } from './api'
import '../assets/css/admin.css'

const cash = value => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value || 0)
const when = value => value ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : '—'
const pageContent = value => value?.content || []

export default function Admin({ user, products, collections, notify, onLogout }) {
  const [section, setSection] = useState('dashboard')
  if (!user) return <Navigate to="/" replace />
  if (user.role !== 'ADMIN') return <Navigate to="/" replace />
  const items = [
    ['dashboard', 'Tổng quan'], ['products', 'Sản phẩm'], ['collections', 'Bộ sưu tập'],
    ['inventory', 'Kho hàng'], ['orders', 'Đơn hàng'], ['users', 'Người dùng'], ['notifications', 'Thông báo']
  ]
  return <div className="admin-shell"><aside className="admin-sidebar"><Link className="admin-brand" to="/">Đàng Xem<small>ADMIN CONSOLE</small></Link><nav>{items.map(([id, label]) => <button className={section === id ? 'active' : ''} onClick={() => setSection(id)} key={id}>{label}</button>)}</nav><div className="admin-profile"><b>{user.fullName}</b><small>{user.email}</small><button onClick={onLogout}>Đăng xuất</button></div></aside><main className="admin-main"><header className="admin-topbar"><div><span>QUẢN TRỊ CỬA HÀNG</span><h1>{items.find(item => item[0] === section)?.[1]}</h1></div><Link to="/">Xem cửa hàng ↗</Link></header>{section === 'dashboard' && <Dashboard />}{section === 'products' && <Products products={products} collections={collections} notify={notify} />}{section === 'collections' && <Collections collections={collections} notify={notify} />}{section === 'inventory' && <Inventory notify={notify} />}{section === 'orders' && <Orders notify={notify} />}{section === 'users' && <Users notify={notify} currentId={user.id} />}{section === 'notifications' && <Notifications notify={notify} />}</main></div>
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
  async function save(e) { e.preventDefault(); const v = Object.fromEntries(new FormData(e.currentTarget)); v.basePrice = Number(v.basePrice); v.collectionId = Number(v.collectionId); try { editing ? await api.updateProduct(editing.id, v) : await api.createProduct(v); notify('Đã lưu sản phẩm.'); setOpen(false); window.location.reload() } catch (x) { notify(x.message) } }
  async function remove(item) { if (!confirm(`Xóa sản phẩm “${item.nameVi}”?`)) return; try { await api.deleteProduct(item.id); notify('Đã xóa sản phẩm.'); window.location.reload() } catch (x) { notify(x.message) } }
  return <Panel title="Danh sách sản phẩm" action={<button className="admin-primary" onClick={() => { setEditing(null); setOpen(true) }}>+ Thêm sản phẩm</button>}><SimpleTable heads={['Tên sản phẩm', 'Giá bán', 'Trạng thái']} rows={products.map(x => [<><b>{x.nameVi}</b><small>{x.nameEn}</small></>, cash(x.sellingPrice), <Status value={x.status} />])} actions={i => <><button onClick={() => { setEditing(products[i]); setOpen(true) }}>Sửa</button><button className="danger" onClick={() => remove(products[i])}>Xóa</button></>} />{open && <Modal title={editing ? 'Cập nhật sản phẩm' : 'Thêm sản phẩm'} close={() => setOpen(false)}><form className="admin-form" onSubmit={save}><label>Tên tiếng Việt<input name="nameVi" defaultValue={editing?.nameVi} required /></label><label>Tên tiếng Anh<input name="nameEn" defaultValue={editing?.nameEn} required /></label><label>Mô tả VI<textarea name="descriptionVi" defaultValue={editing?.descriptionVi} /></label><label>Mô tả EN<textarea name="descriptionEn" defaultValue={editing?.descriptionEn} /></label><label>Giá gốc<input name="basePrice" type="number" min="1" defaultValue={editing?.basePrice} required /></label><label>Bộ sưu tập<select name="collectionId" defaultValue={editing?.collectionId} required>{collections.map(x => <option value={x.id} key={x.id}>{x.nameVi}</option>)}</select></label><label>Trạng thái<select name="status" defaultValue={editing?.status || 'ACTIVE'}><option>ACTIVE</option><option>INACTIVE</option></select></label><button className="admin-primary">Lưu sản phẩm</button></form></Modal>}</Panel>
}

function Collections({ collections, notify }) {
  const [editing, setEditing] = useState(null), [open, setOpen] = useState(false)
  async function save(e) { e.preventDefault(); const v = Object.fromEntries(new FormData(e.currentTarget)); try { editing ? await api.updateCollection(editing.id, v) : await api.createCollection(v); notify('Đã lưu bộ sưu tập.'); window.location.reload() } catch (x) { notify(x.message) } }
  return <Panel title="Bộ sưu tập" action={<button className="admin-primary" onClick={() => { setEditing(null); setOpen(true) }}>+ Thêm bộ sưu tập</button>}><SimpleTable heads={['Tên VI', 'Tên EN', 'Trạng thái']} rows={collections.map(x => [x.nameVi, x.nameEn, <Status value={x.status} />])} actions={i => <button onClick={() => { setEditing(collections[i]); setOpen(true) }}>Sửa</button>} />{open && <Modal title="Bộ sưu tập" close={() => setOpen(false)}><form className="admin-form" onSubmit={save}><label>Tên tiếng Việt<input name="nameVi" defaultValue={editing?.nameVi} required /></label><label>Tên tiếng Anh<input name="nameEn" defaultValue={editing?.nameEn} required /></label><label>Mô tả VI<textarea name="descriptionVi" defaultValue={editing?.descriptionVi} /></label><label>Mô tả EN<textarea name="descriptionEn" defaultValue={editing?.descriptionEn} /></label><label>Trạng thái<select name="status" defaultValue={editing?.status || 'ACTIVE'}><option>ACTIVE</option><option>INACTIVE</option></select></label><button className="admin-primary">Lưu</button></form></Modal>}</Panel>
}

function Inventory({ notify }) {
  const [data, setData] = useState(null), [selected, setSelected] = useState(null), [query, setQuery] = useState('')
  const load = () => api.adminInventory(new URLSearchParams({ keyword: query, size: 100 })).then(setData).catch(e => notify(e.message))
  useEffect(load, [])
  async function adjust(e) { e.preventDefault(); const v = Object.fromEntries(new FormData(e.currentTarget)); v.quantityChange = Number(v.quantityChange); try { await api.adminAdjustInventory(selected.productId, v); setSelected(null); load(); notify('Đã cập nhật tồn kho.') } catch (x) { notify(x.message) } }
  const rows = pageContent(data)
  return <Panel title="Tồn kho" action={<form className="admin-search" onSubmit={e => { e.preventDefault(); load() }}><input value={query} onChange={e => setQuery(e.target.value)} placeholder="Tìm sản phẩm" /><button>Tìm</button></form>}><SimpleTable heads={['Sản phẩm', 'Tổng', 'Đang giữ', 'Khả dụng', 'Trạng thái']} rows={rows.map(x => [x.productNameVi, x.quantity, x.reservedQuantity, x.availableQuantity, <Status value={x.status} />])} actions={i => <button onClick={() => setSelected(rows[i])}>Điều chỉnh</button>} />{selected && <Modal title={`Điều chỉnh: ${selected.productNameVi}`} close={() => setSelected(null)}><form className="admin-form" onSubmit={adjust}><label>Loại<select name="type"><option>IMPORT</option><option>ADJUSTMENT</option></select></label><label>Số lượng thay đổi<input name="quantityChange" type="number" required /></label><label>Lý do<textarea name="reason" required /></label><button className="admin-primary">Xác nhận</button></form></Modal>}</Panel>
}

function Orders({ notify }) {
  const [data, setData] = useState(null), [detail, setDetail] = useState(null)
  const load = () => api.adminOrders('size=100&sort=createdAt,desc').then(setData).catch(e => notify(e.message)); useEffect(load, [])
  const rows = pageContent(data)
  async function transition(status) { try { setDetail(await api.adminOrderStatus(detail.id, status)); load(); notify('Đã cập nhật đơn hàng.') } catch (x) { notify(x.message) } }
  return <Panel title="Đơn hàng"><SimpleTable heads={['Mã đơn', 'Ngày tạo', 'Đơn hàng', 'Thanh toán', 'Tổng tiền']} rows={rows.map(x => [x.orderCode, when(x.createdAt), <Status value={x.orderStatus} />, <Status value={x.paymentStatus} />, cash(x.totalAmount)])} actions={i => <button onClick={() => api.adminOrder(rows[i].id).then(setDetail).catch(e => notify(e.message))}>Chi tiết</button>} />{detail && <Modal title={`Đơn hàng ${detail.orderCode}`} close={() => setDetail(null)}><div className="order-admin-detail"><p><b>Người nhận:</b> {detail.receiverName} · {detail.phone}</p><p><b>Địa chỉ:</b> {detail.address}</p>{detail.items.map(x => <p key={x.productId}>{x.productNameVi} × {x.quantity} <b>{cash(x.totalPrice)}</b></p>)}<h3>Tổng cộng: {cash(detail.totalAmount)}</h3><div className="admin-actions"><button onClick={() => transition('CONFIRMED')}>Xác nhận</button><button onClick={() => transition('COMPLETED')}>Hoàn thành</button><button className="danger" onClick={() => api.adminCancelOrder(detail.id).then(setDetail).then(load).catch(e => notify(e.message))}>Hủy đơn</button></div></div></Modal>}</Panel>
}

function Users({ notify, currentId }) {
  const [data, setData] = useState(null); const load = () => api.adminUsers('size=100&sort=createdAt,desc').then(setData).catch(e => notify(e.message)); useEffect(load, [])
  const rows = pageContent(data)
  async function action(item, name) { if (item.id === currentId && ['demote', 'block'].includes(name) && !confirm('Thao tác trên chính tài khoản hiện tại. Tiếp tục?')) return; try { await api.adminUserAction(item.id, name); load(); notify('Đã cập nhật người dùng.') } catch (x) { notify(x.message) } }
  return <Panel title="Người dùng"><SimpleTable heads={['Người dùng', 'Email', 'Vai trò', 'Trạng thái', 'Ngày tạo']} rows={rows.map(x => [x.fullName, x.email, x.role, <Status value={x.status} />, when(x.createdAt)])} actions={i => { const x = rows[i]; return <div className="row-actions">{x.role === 'USER' ? <button onClick={() => action(x, 'promote')}>Cấp Admin</button> : <button onClick={() => action(x, 'demote')}>Hạ quyền</button>}{x.status === 'ACTIVE' ? <button className="danger" onClick={() => action(x, 'block')}>Chặn</button> : <button onClick={() => action(x, 'unblock')}>Bỏ chặn</button>}</div> }} /></Panel>
}

function Notifications({ notify }) {
  const [data, setData] = useState(null); const load = () => api.adminNotifications('size=100&sort=createdAt,desc').then(setData).catch(e => notify(e.message)); useEffect(load, [])
  const rows = pageContent(data)
  return <Panel title={`Thông báo (${rows.filter(x => !x.isRead).length} chưa đọc)`}><div className="notification-list">{rows.map(x => <article className={x.isRead ? '' : 'unread'} key={x.id}><div><b>{x.title}</b><p>{x.message}</p><small>{when(x.createdAt)}</small></div>{!x.isRead && <button onClick={() => api.adminReadNotification(x.id).then(load).catch(e => notify(e.message))}>Đánh dấu đã đọc</button>}</article>)}</div></Panel>
}

function Status({ value }) { return <span className={`admin-status ${String(value).toLowerCase()}`}>{value}</span> }
function Modal({ title, close, children }) { return <div className="admin-modal" onMouseDown={e => e.target === e.currentTarget && close()}><div><header><h2>{title}</h2><button onClick={close}>×</button></header>{children}</div></div> }
