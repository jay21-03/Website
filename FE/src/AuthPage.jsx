import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

async function api(path, options = {}) {
  const response = await fetch(`/api/auth/${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) }
  })
  const data = await response.json()
  if (!response.ok) throw new Error(data.message || 'Không thể xử lý yêu cầu.')
  return data
}

export default function AuthPage() {
  const navigate = useNavigate()
  const [mode, setMode] = useState('login')
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(false)
  const [checking, setChecking] = useState(true)
  const [error, setError] = useState('')
  const [showPassword, setShowPassword] = useState(false)

  useEffect(() => {
    const token = localStorage.getItem('dxToken')
    if (!token) { setChecking(false); return }
    fetch('/api/auth/me', { headers: { Authorization: `Bearer ${token}` } })
      .then(async response => {
        if (!response.ok) throw new Error()
        return response.json()
      })
      .then(data => setUser(data.user))
      .catch(() => localStorage.removeItem('dxToken'))
      .finally(() => setChecking(false))
  }, [])

  async function submit(event) {
    event.preventDefault()
    setError('')
    setLoading(true)
    const form = Object.fromEntries(new FormData(event.currentTarget))
    try {
      const data = await api(mode, { method: 'POST', body: JSON.stringify(form) })
      localStorage.setItem('dxToken', data.token)
      localStorage.setItem('dxUser', JSON.stringify(data.user))
      setUser(data.user)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }

  function logout() {
    localStorage.removeItem('dxToken')
    localStorage.removeItem('dxUser')
    setUser(null)
  }

  if (checking) return <main className="auth-page"><p>Đang kiểm tra phiên đăng nhập...</p></main>

  if (user) return <main className="auth-page"><section className="auth-card account-card"><span className="account-avatar">{user.name.charAt(0).toUpperCase()}</span><span className="kicker">Tài khoản của bạn</span><h1>Xin chào, {user.name}</h1><p>{user.email}</p><div className="account-actions"><button className="button dark" onClick={() => navigate('/')}>Tiếp tục mua sắm</button><button className="button light" onClick={logout}>Đăng xuất</button></div></section></main>

  const registering = mode === 'register'
  return <main className="auth-shell">
    <section className="auth-visual"><Link className="auth-brand" to="/">Đàng Xem<small>BAU TRUC POTTERY</small></Link><div><span className="kicker light-kicker">Gốm Bàu Trúc</span><h2>Đất hóa hồn.<br/>Tay giữ lửa.</h2><p>Mỗi tài khoản mở ra một hành trình khám phá những tác phẩm gốm Chăm độc bản.</p></div></section>
    <section className="auth-form-side"><div className="auth-card auth-card-clean"><Link className="back-home" to="/">← Về trang chủ</Link><span className="kicker">Tài khoản</span><h1>{registering ? 'Tạo tài khoản' : 'Chào mừng trở lại'}</h1><p>{registering ? 'Đăng ký để lưu sản phẩm và theo dõi đơn hàng.' : 'Đăng nhập để tiếp tục với Đàng Xem.'}</p>{error && <div className="auth-error" role="alert">{error}</div>}<form className="stack-form" onSubmit={submit}>{registering && <label>Họ và tên<input name="name" autoComplete="name" minLength="2" required placeholder="Nguyễn Văn An" /></label>}<label>Email<input name="email" type="email" autoComplete="email" required placeholder="name@email.com" /></label><label>Mật khẩu<div className="password-field"><input name="password" type={showPassword ? 'text' : 'password'} autoComplete={registering ? 'new-password' : 'current-password'} minLength="8" required placeholder="Tối thiểu 8 ký tự" /><button type="button" onClick={() => setShowPassword(!showPassword)}>{showPassword ? 'Ẩn' : 'Hiện'}</button></div></label><button className="button dark auth-submit" disabled={loading}>{loading ? 'Đang xử lý...' : registering ? 'Tạo tài khoản' : 'Đăng nhập'}</button></form><p className="auth-switch">{registering ? 'Đã có tài khoản?' : 'Chưa có tài khoản?'} <button onClick={() => { setMode(registering ? 'login' : 'register'); setError('') }}>{registering ? 'Đăng nhập' : 'Đăng ký ngay'}</button></p><p className="auth-terms">Bằng cách tiếp tục, bạn đồng ý với Điều khoản sử dụng và Chính sách bảo mật.</p></div></section>
  </main>
}
