import express from 'express'
import bcrypt from 'bcryptjs'
import jwt from 'jsonwebtoken'
import { mkdir, readFile, rename, writeFile } from 'node:fs/promises'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const app = express()
const here = dirname(fileURLToPath(import.meta.url))
const dataDir = join(here, 'data')
const usersFile = join(dataDir, 'users.json')
const JWT_SECRET = process.env.JWT_SECRET || 'dev-only-change-this-secret-before-production'
const PORT = Number(process.env.PORT || 3001)

app.use(express.json({ limit: '32kb' }))

async function readUsers() {
  try { return JSON.parse(await readFile(usersFile, 'utf8')) }
  catch (error) {
    if (error.code !== 'ENOENT') throw error
    await mkdir(dataDir, { recursive: true })
    await writeFile(usersFile, '[]', 'utf8')
    return []
  }
}

async function saveUsers(users) {
  await mkdir(dataDir, { recursive: true })
  const temp = `${usersFile}.tmp`
  await writeFile(temp, JSON.stringify(users, null, 2), 'utf8')
  await rename(temp, usersFile)
}

function publicUser(user) { return { id: user.id, name: user.name, email: user.email, createdAt: user.createdAt } }
function tokenFor(user) { return jwt.sign({ sub: user.id, email: user.email }, JWT_SECRET, { expiresIn: '7d' }) }
function validEmail(email) { return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) }

app.post('/api/auth/register', async (req, res, next) => {
  try {
    const name = String(req.body.name || '').trim()
    const email = String(req.body.email || '').trim().toLowerCase()
    const password = String(req.body.password || '')
    if (name.length < 2) return res.status(400).json({ message: 'Họ tên phải có ít nhất 2 ký tự.' })
    if (!validEmail(email)) return res.status(400).json({ message: 'Email không hợp lệ.' })
    if (password.length < 8) return res.status(400).json({ message: 'Mật khẩu phải có ít nhất 8 ký tự.' })
    const users = await readUsers()
    if (users.some(user => user.email === email)) return res.status(409).json({ message: 'Email này đã được đăng ký.' })
    const user = { id: crypto.randomUUID(), name, email, passwordHash: await bcrypt.hash(password, 12), createdAt: new Date().toISOString() }
    users.push(user)
    await saveUsers(users)
    res.status(201).json({ token: tokenFor(user), user: publicUser(user) })
  } catch (error) { next(error) }
})

app.post('/api/auth/login', async (req, res, next) => {
  try {
    const email = String(req.body.email || '').trim().toLowerCase()
    const password = String(req.body.password || '')
    const user = (await readUsers()).find(item => item.email === email)
    if (!user || !(await bcrypt.compare(password, user.passwordHash))) return res.status(401).json({ message: 'Email hoặc mật khẩu không đúng.' })
    res.json({ token: tokenFor(user), user: publicUser(user) })
  } catch (error) { next(error) }
})

app.get('/api/auth/me', async (req, res) => {
  const token = req.headers.authorization?.replace(/^Bearer\s+/i, '')
  if (!token) return res.status(401).json({ message: 'Bạn chưa đăng nhập.' })
  try {
    const payload = jwt.verify(token, JWT_SECRET)
    const user = (await readUsers()).find(item => item.id === payload.sub)
    if (!user) return res.status(401).json({ message: 'Phiên đăng nhập không hợp lệ.' })
    res.json({ user: publicUser(user) })
  } catch { res.status(401).json({ message: 'Phiên đăng nhập đã hết hạn.' }) }
})

app.get('/api/health', (_req, res) => res.json({ ok: true }))
app.use((error, _req, res, _next) => { console.error(error); res.status(500).json({ message: 'Máy chủ gặp lỗi. Vui lòng thử lại.' }) })
app.listen(PORT, '127.0.0.1', () => console.log(`Auth API running at http://127.0.0.1:${PORT}`))
