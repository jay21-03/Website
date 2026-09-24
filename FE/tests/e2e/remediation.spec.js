import { expect, test } from '@playwright/test'

const product = {
  id: 10,
  nameVi: 'Binh gom do',
  nameEn: 'Red pottery vase',
  descriptionVi: 'Gom Bau Truc tao hinh bang tay.',
  descriptionEn: 'Hand-shaped Bau Truc pottery.',
  basePrice: 250000,
  sellingPrice: 200000,
  collectionId: 1,
  thumbnailUrl: '/assets/images/vase.jpg',
  images: [
    { id: 1, url: '/assets/images/vase.jpg', thumbnail: true },
    { id: 2, url: '/assets/images/tour.jpg', thumbnail: false }
  ]
}

const inactiveProduct = {
  ...product,
  id: 11,
  nameVi: 'Binh gom an',
  nameEn: 'Hidden pottery vase',
  status: 'INACTIVE',
  sellingPrice: 180000,
  discount: {
    discountType: 'PERCENTAGE',
    discountValue: 10,
    startAt: '2026-08-17T08:00:00+07:00',
    endAt: '2026-08-31T23:00:00+07:00',
    isActive: true
  }
}

const featuredProduct2 = {
  ...product,
  id: 12,
  nameVi: 'Binh gom xanh',
  nameEn: 'Blue pottery vase',
  sellingPrice: 220000,
  thumbnailUrl: '/assets/images/plate.jpg',
  status: 'ACTIVE'
}

const featuredProduct3 = {
  ...product,
  id: 13,
  nameVi: 'Bo tra gom',
  nameEn: 'Pottery tea set',
  sellingPrice: 320000,
  thumbnailUrl: '/assets/images/tour.jpg',
  status: 'ACTIVE'
}

const collection = { id: 1, nameVi: 'Bo suu tap do', nameEn: 'Red collection', descriptionVi: 'Gom mau dat nung.', descriptionEn: 'Fired clay tones.', status: 'ACTIVE' }
const inactiveCollection = { id: 2, nameVi: 'Bo suu tap cu', nameEn: 'Archive collection', descriptionVi: 'Dang an.', descriptionEn: 'Hidden.', status: 'INACTIVE' }

const cart = {
  items: [{
    id: 100,
    productId: product.id,
    nameVi: product.nameVi,
    nameEn: product.nameEn,
    thumbnailUrl: product.thumbnailUrl,
    sellingPrice: product.sellingPrice,
    quantity: 1,
    availableQuantity: 5,
    lineTotal: product.sellingPrice
  }],
  totalAmount: product.sellingPrice
}

const order = {
  id: 77,
  orderCode: 'BT-00077',
  orderStatus: 'NEW',
  paymentStatus: 'PENDING',
  totalAmount: product.sellingPrice,
  subtotal: product.sellingPrice,
  paymentId: 88,
  paymentAmount: product.sellingPrice,
  checkoutUrl: null,
  qrCode: null,
  expiresAt: '2026-08-17T10:30:00+07:00',
  receiverName: 'Nguyen Van A',
  phone: '0909000000',
  email: 'a@example.com',
  address: 'Bau Truc',
  note: '',
  createdAt: '2026-08-17T09:00:00+07:00',
  items: [{
    productId: product.id,
    productNameVi: product.nameVi,
    productNameEn: product.nameEn,
    sellingPrice: product.sellingPrice,
    quantity: 1,
    totalPrice: product.sellingPrice
  }]
}

async function mockApi(page, role = 'USER') {
  let orderPolls = 0
  let adminProducts = [product, inactiveProduct]
  let adminCollections = [collection, inactiveCollection]
  let adminHome = {
    sloganVi: 'Tinh hoa gốm Chăm – Gìn giữ hồn di sản',
    sloganEn: 'The essence of Cham pottery – preserving the soul of heritage',
    media: [
      { slot: 'HOME_HERO', url: 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==', updatedAt: order.createdAt },
      { slot: 'HOME_STORY', url: null, updatedAt: order.createdAt },
      { slot: 'HOME_SOCIAL_1', url: null, updatedAt: order.createdAt },
      { slot: 'HOME_SOCIAL_2', url: null, updatedAt: order.createdAt },
      { slot: 'HOME_SOCIAL_3', url: null, updatedAt: order.createdAt },
      { slot: 'HOME_SOCIAL_4', url: null, updatedAt: order.createdAt },
      { slot: 'HOME_SOCIAL_5', url: null, updatedAt: order.createdAt },
      { slot: 'HOME_SOCIAL_6', url: null, updatedAt: order.createdAt }
    ],
    featuredProducts: []
  }
  const publicHome = {
    sloganVi: 'Tinh hoa gốm Chăm – Gìn giữ hồn di sản',
    sloganEn: 'The essence of Cham pottery – preserving the soul of heritage',
    heroImageUrl: 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==',
    storyImageUrl: null,
    socialImages: [
      { slot: 'HOME_SOCIAL_1', url: 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==', updatedAt: order.createdAt },
      { slot: 'HOME_SOCIAL_2', url: null, updatedAt: order.createdAt },
      { slot: 'HOME_SOCIAL_3', url: null, updatedAt: order.createdAt },
      { slot: 'HOME_SOCIAL_4', url: null, updatedAt: order.createdAt },
      { slot: 'HOME_SOCIAL_5', url: null, updatedAt: order.createdAt },
      { slot: 'HOME_SOCIAL_6', url: null, updatedAt: order.createdAt }
    ],
    featuredProducts: [
      { ...product, slot: 1 },
      { ...featuredProduct2, slot: 2 },
      { ...featuredProduct3, slot: 3 }
    ]
  }
  const adminUsers = [
    { id: 1, fullName: 'Admin User', email: 'admin@example.com', role: 'ADMIN', status: 'ACTIVE', createdAt: order.createdAt },
    { id: 2, fullName: 'Customer User', email: 'customer@example.com', role: 'USER', status: 'ACTIVE', createdAt: order.createdAt }
  ]
  await page.route('**/api/v1/**', async route => {
    const url = new URL(route.request().url())
    const path = url.pathname.replace('/api/v1', '')
    const envelope = data => JSON.stringify({ success: true, data, timestamp: '2026-08-17T10:00:00+07:00', correlationId: 'e2e' })
    const ok = data => route.fulfill({ status: 200, contentType: 'application/json', body: envelope(data) })
    const created = data => route.fulfill({ status: 201, contentType: 'application/json', body: envelope(data) })

    if (path === '/auth/csrf') return ok({ token: 'csrf-e2e' })
    if (path === '/me') {
      if (!role) return route.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify({ success: false, error: { code: 'UNAUTHORIZED', message: 'Authentication required.' } }) })
      return ok({ id: 1, fullName: 'Admin User', email: 'admin@example.com', role, status: 'ACTIVE' })
    }
    if (path === '/home') return ok(publicHome)
    if (path === '/products') return ok({ content: [product], page: Number(url.searchParams.get('page') || 0), size: 20, totalElements: 1, totalPages: 1, first: true, last: true })
    if (path === '/products/10') return ok(product)
    if (path === '/collections') return ok([collection])
    if (path === '/support/settings' || path === '/admin/support/settings') return ok({ email: 'support@example.com', zaloPhone: '0909000000', secondaryPhone: '0909000001', address: 'Bau Truc', openingHours: '7:00 - 17:00' })
    if (path === '/cart' || path === '/cart/items') return ok(cart)
    if (path === '/checkout') return ok({ checkoutOperationId: 1, orderId: order.id, orderCode: order.orderCode, paymentId: order.paymentId, paymentStatus: 'PENDING', totalAmount: order.totalAmount, checkoutUrl: null, qrCode: null, expiresAt: order.expiresAt })
    if (path === '/me/orders') return ok({ content: [order], page: 0, size: 20, totalElements: 1, totalPages: 1, first: true, last: true })
    if (path === '/me/orders/77') { orderPolls += 1; return ok({ ...order, paymentStatus: orderPolls >= 2 ? 'PAID' : 'PENDING' }) }
    if (path === '/workshops') return ok([{ id: 5, title: 'Lam gom co ban', description: 'Trai nghiem tao hinh gom.', priceAmount: 150000, durationMinutes: 120, maxParticipants: 10, imageUrl: '/assets/images/tour.jpg', status: 'ACTIVE' }])
    if (path === '/workshop/bookings') return created({ id: 9, status: 'NEW' })
    if (path === '/admin/dashboard') return ok({ totalOrders: 1, totalRevenue: 200000, newOrders: 1, lowStockProducts: [], recentOrders: [order], bestSellingProducts: [{ productNameVi: product.nameVi, totalQuantity: 2, totalRevenue: 400000 }] })
    if (path === '/admin/home' && route.request().method() === 'GET') return ok(adminHome)
    if (path === '/admin/home/slogan' && route.request().method() === 'PUT') {
      adminHome = { ...adminHome, ...(await route.request().postDataJSON()) }
      publicHome.sloganVi = adminHome.sloganVi
      publicHome.sloganEn = adminHome.sloganEn
      return ok(adminHome)
    }
    if (path === '/admin/home/featured-products' && route.request().method() === 'PUT') {
      const { productIds } = await route.request().postDataJSON()
      const candidates = [{ ...product, status: 'ACTIVE' }, featuredProduct2, featuredProduct3]
      adminHome = {
        ...adminHome,
        featuredProducts: productIds.map((id, index) => {
          const selected = candidates.find(item => item.id === id)
          return { slot: index + 1, id: selected.id, nameVi: selected.nameVi, nameEn: selected.nameEn, descriptionVi: selected.descriptionVi, descriptionEn: selected.descriptionEn, basePrice: selected.basePrice, sellingPrice: selected.sellingPrice, thumbnailUrl: selected.thumbnailUrl }
        })
      }
      return ok(adminHome)
    }
    if (path.startsWith('/admin/home/media/') && route.request().method() === 'PUT') {
      const slot = decodeURIComponent(path.split('/').pop())
      const updated = { slot, url: 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==', updatedAt: order.createdAt }
      adminHome = { ...adminHome, media: adminHome.media.map(item => item.slot === slot ? updated : item) }
      return ok(updated)
    }
    if (path.startsWith('/admin/home/media/') && route.request().method() === 'DELETE') {
      const slot = decodeURIComponent(path.split('/').pop())
      const updated = { slot, url: null, updatedAt: order.createdAt }
      adminHome = { ...adminHome, media: adminHome.media.map(item => item.slot === slot ? updated : item) }
      return ok(updated)
    }
    if (path === '/admin/reports/revenue') return ok({ totalRevenue: 200000, points: [{ periodStart: '2026-08-17', revenue: 200000 }] })
    if (path === '/admin/reports/best-selling') return ok([{ productNameVi: product.nameVi, soldQuantity: 2 }])
    if (path === '/admin/notifications') return ok({ content: [{ id: 1, title: 'Don moi', message: 'Co don hang moi', createdAt: order.createdAt, isRead: false }], page: 0, size: 10, totalElements: 37, totalPages: 4, first: true, last: false })
    if (path === '/admin/products' && route.request().method() === 'GET') {
      let rows = adminProducts
      if (url.searchParams.get('status') === 'ACTIVE') rows = [{ ...product, status: 'ACTIVE' }, featuredProduct2, featuredProduct3]
      else if (url.searchParams.get('status')) rows = rows.filter(item => item.status === url.searchParams.get('status'))
      return ok({ content: rows, page: Number(url.searchParams.get('page') || 0), size: Number(url.searchParams.get('size') || 20), totalElements: rows.length, totalPages: 1, first: true, last: true })
    }
    if (path === '/admin/products/11' && route.request().method() === 'PUT') {
      const body = await route.request().postDataJSON()
      adminProducts = adminProducts.map(item => item.id === 11 ? { ...item, ...body, sellingPrice: item.sellingPrice } : item)
      return ok(adminProducts.find(item => item.id === 11))
    }
    if (path === '/admin/products/11/discount' && route.request().method() === 'PUT') return ok({ ...inactiveProduct.discount, ...(await route.request().postDataJSON()) })
    if (path === '/admin/collections' && route.request().method() === 'GET') {
      let rows = adminCollections
      if (url.searchParams.get('status')) rows = rows.filter(item => item.status === url.searchParams.get('status'))
      return ok({ content: rows, page: Number(url.searchParams.get('page') || 0), size: Number(url.searchParams.get('size') || 20), totalElements: rows.length, totalPages: 1, first: true, last: true })
    }
    if (path === '/admin/collections/2' && route.request().method() === 'DELETE') {
      adminCollections = adminCollections.filter(item => item.id !== 2)
      return ok(null)
    }
    if (path === '/admin/users') return ok({ content: adminUsers, page: Number(url.searchParams.get('page') || 0), size: Number(url.searchParams.get('size') || 20), totalElements: 42, totalPages: 3, first: true, last: false })
    if (path === '/admin/workshops') return ok([{ id: 5, title: 'Lam gom co ban', description: 'Trai nghiem tao hinh gom.', priceAmount: 150000, durationMinutes: 120, maxParticipants: 10, status: 'ACTIVE' }])
    if (path === '/admin/workshop/bookings') return ok({ content: [{ id: 9, workshopId: 5, fullName: 'Nguyen Van A', email: 'a@example.com', phone: '0909000000', preferredAt: order.createdAt, participants: 2, status: 'NEW' }], page: 0, size: 20, totalElements: 1, totalPages: 1, first: true, last: true })
    if (path === '/admin/inventory') return ok({ content: [{ productId: product.id, productNameVi: product.nameVi, quantity: 10, reservedQuantity: 1, availableQuantity: 9, status: 'IN_STOCK' }], page: 0, size: 20, totalElements: 1, totalPages: 1, first: true, last: true })
    if (path === '/admin/orders') return ok({ content: [order], page: 0, size: 20, totalElements: 1, totalPages: 1, first: true, last: true })
    if (path === '/admin/orders/77') return ok(order)
    return ok(null)
  })
}

async function mockGoogleIdentity(page) {
  await page.addInitScript(() => {
    window.google = { accounts: { id: {
      initialize: config => { window.__googleCredentialCallback = config.callback },
      prompt: callback => {
        window.__googlePrompted = true
        callback?.({ isNotDisplayed: () => false, isSkippedMoment: () => false })
      },
      renderButton: (element, config) => {
        const button = document.createElement('button')
        button.type = 'button'
        button.textContent = 'Continue with Google'
        button.addEventListener('click', () => config.click_listener?.())
        element.appendChild(button)
      }
    } } }
  })
}

test('public homepage renders managed media and backend featured products', async ({ page }) => {
  await mockApi(page)
  await page.goto('/')

  const hero = page.getByRole('img', { name: 'Nghệ nhân Chăm tạo hình gốm bằng tay' })
  await expect(hero).toHaveAttribute('src', 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==')

  const featured = page.locator('article').filter({ hasText: 'Binh gom xanh' })
  await expect(featured.getByRole('heading', { name: 'Binh gom xanh' })).toBeVisible()
  await expect(featured.getByRole('link', { name: /Xem Chi Tiết|View Details/i })).toHaveAttribute('href', '/products/12')

  const journeyImage = page.getByRole('img', { name: 'Ảnh xưởng gốm 1' })
  await expect(journeyImage).toHaveAttribute('src', 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==')
})

test('public catalog delegates search/filter/sort to backend and opens product detail', async ({ page }) => {
  await mockApi(page)
  await page.goto('/products')
  await expect(page.getByRole('heading', { name: /Từng Sản Phẩm|Every Piece/i })).toBeVisible()
  await page.getByText(/Tìm kiếm & bộ lọc|Search & filters/i).click()
  await page.getByPlaceholder(/Tên sản phẩm|Product name/i).fill('binh')
  await page.getByText('Binh gom do').click()
  await expect(page.getByRole('heading', { name: 'Binh gom do' })).toBeVisible()
  await expect(page.getByText(/200.000|200,000/)).toBeVisible()
})

test('public collections route filters products and product detail gallery supports zoom', async ({ page, isMobile }) => {
  await mockApi(page)
  await page.goto('/collections')
  await expect(page.getByRole('heading', { name: /Khám phá theo bộ sưu tập|Explore by collection/i })).toBeVisible()
  await page.getByRole('link', { name: /Xem sản phẩm|View products/i }).click()
  await expect(page).toHaveURL(/collectionId=1/)
  await page.goto('/products/10')
  await page.getByRole('button', { name: /Phóng to ảnh|Zoom in/i }).click()
  await expect(page.getByText('125%')).toBeVisible()
  if (isMobile) {
    const image = page.locator('.product-detail-image')
    await image.dispatchEvent('touchstart', { changedTouches: [{ identifier: 1, clientX: 320, clientY: 200 }] })
    await image.dispatchEvent('touchend', { changedTouches: [{ identifier: 1, clientX: 120, clientY: 200 }] })
  } else {
    await page.getByRole('button', { name: /Ảnh sau|Next image/i }).click()
  }
  await expect(page.locator('.product-thumbs button.active')).toHaveCount(1)
})

test('cart checkout uses pending payment UI without shipping fee or fake payment success', async ({ page }) => {
  await mockApi(page)
  await page.goto('/cart')
  await expect(page.getByRole('heading', { name: /Giỏ hàng|Cart/i })).toBeVisible()
  await page.getByRole('link', { name: /Thanh toán|Checkout/i }).click()
  await page.getByLabel('Số điện thoại').fill('0909000000')
  await page.getByLabel('Địa chỉ').fill('Bau Truc')
  await page.getByRole('button', { name: /Đặt hàng/i }).click()
  await expect(page).toHaveURL(/\/orders\/77/)
  await expect(page.getByText(/Thanh toán payOS/i)).toBeVisible()
  await expect(page.getByText(/Cổng thanh toán đang chờ cấu hình/i)).toBeVisible()
  await expect(page.getByText(/phí vận chuyển/i)).toHaveCount(0)
})

test('admin dashboard, notifications, workshop, support and reporting render with mocked backend', async ({ page }) => {
  await mockApi(page, 'ADMIN')
  await page.addInitScript(() => {
    window.EventSource = class {
      addEventListener() {}
      close() {}
    }
  })
  await page.goto('/admin')
  await expect(page.getByRole('heading', { name: 'Tổng quan' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Báo cáo' })).toBeVisible()
  await expect(page.locator('.notification-bell > button > span').last()).toHaveText('37')
  await page.getByLabel('Thông báo').click()
  await expect(page.getByText('Don moi')).toBeVisible()
  await page.getByRole('button', { name: 'Workshop' }).click()
  await expect(page.getByText('Gói workshop')).toBeVisible()
  await expect(page.getByText('Lịch hẹn workshop')).toBeVisible()
  await page.getByRole('button', { name: 'Cấu hình' }).click()
  await expect(page.locator('input[name="email"]')).toHaveValue('support@example.com')
})


test('admin homepage media supports preview, upload and reset to default', async ({ page }) => {
  await mockApi(page, 'ADMIN')
  page.on('dialog', dialog => dialog.accept())
  await page.goto('/admin?section=homepage')
  await expect(page.getByRole('heading', { name: 'Trang chủ', exact: true })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Nội dung hình ảnh trang chủ' })).toBeVisible()

  const hero = page.locator('.homepage-media-card').filter({ hasText: 'Ảnh Hero' })
  await expect(hero.getByText('Ảnh quản lý')).toBeVisible()
  await hero.locator('input[type="file"]').setInputFiles({ name: 'hero.png', mimeType: 'image/png', buffer: Buffer.from('mock-home-image') })
  await hero.getByRole('button', { name: 'Tải ảnh đã chọn' }).click()
  await expect(page.getByText('Đã cập nhật Ảnh Hero.')).toBeVisible()

  await hero.getByRole('button', { name: 'Dùng ảnh mặc định' }).click()
  await expect(page.getByText('Đã đặt lại Ảnh Hero về mặc định.')).toBeVisible()
  await expect(hero.getByText('Ảnh mặc định', { exact: true })).toBeVisible()
})

test('admin homepage configures three distinct ACTIVE featured products in slot order', async ({ page }) => {
  await mockApi(page, 'ADMIN')
  await page.goto('/admin?section=homepage')

  const slot1 = page.getByLabel('Sản phẩm nổi bật vị trí 1')
  const slot2 = page.getByLabel('Sản phẩm nổi bật vị trí 2')
  const slot3 = page.getByLabel('Sản phẩm nổi bật vị trí 3')

  await slot1.selectOption('10')
  await expect(slot2.locator('option[value="10"]')).toHaveAttribute('disabled', '')
  await slot2.selectOption('12')
  await slot3.selectOption('13')

  await page.getByRole('button', { name: 'Lưu sản phẩm nổi bật' }).click()
  await expect(page.getByText('Đã cập nhật 3 sản phẩm nổi bật.')).toBeVisible()

  const slots = page.locator('.homepage-featured-slot')
  await expect(slots.nth(0)).toContainText('Binh gom do')
  await expect(slots.nth(1)).toContainText('Binh gom xanh')
  await expect(slots.nth(2)).toContainText('Bo tra gom')
})

test('admin catalog uses admin APIs for inactive products, discount prefill and collection delete', async ({ page }) => {
  await mockApi(page, 'ADMIN')
  page.on('dialog', dialog => dialog.accept())
  await page.goto('/admin')
  await page.getByRole('button', { name: 'Sản phẩm' }).click()
  await expect(page.getByText('Binh gom an')).toBeVisible()
  await page.getByRole('row', { name: /Binh gom an/i }).getByRole('button', { name: 'Sửa' }).click()
  await expect(page.getByText(/Hiện tại: PERCENTAGE/)).toBeVisible()
  await page.getByLabel('Trạng thái').selectOption('ACTIVE')
  await page.getByRole('button', { name: 'Lưu sản phẩm' }).click()
  await expect(page.getByText('Đã lưu sản phẩm.')).toBeVisible()
  await page.getByRole('button', { name: 'Bộ sưu tập' }).click()
  await expect(page.getByText('Bo suu tap cu')).toBeVisible()
  await page.getByRole('row', { name: /Bo suu tap cu/i }).getByRole('button', { name: 'Xóa' }).click()
  await expect(page.getByText('Đã xóa bộ sưu tập.')).toBeVisible()
})

test('admin users and orders use backend pagination and filters', async ({ page }) => {
  const requests = []
  await mockApi(page, 'ADMIN')
  page.on('request', request => {
    if (request.url().includes('/api/v1/admin/users') || request.url().includes('/api/v1/admin/orders')) requests.push(request.url())
  })
  await page.goto('/admin')
  await page.getByRole('button', { name: 'Người dùng' }).click()
  await page.getByPlaceholder('Tìm email hoặc tên').fill('customer')
  await page.getByRole('button', { name: 'Tải' }).click()
  await expect(page.getByText(/42 người dùng/)).toBeVisible()
  await page.getByRole('button', { name: 'Đơn hàng' }).click()
  await page.getByPlaceholder('Tìm mã đơn, tên, SĐT').fill('BT-00077')
  await page.getByRole('button', { name: 'Tải' }).click()
  expect(requests.some(url => url.includes('keyword=customer') && !url.includes('size=100'))).toBeTruthy()
  expect(requests.some(url => url.includes('keyword=BT-00077') && !url.includes('size=100'))).toBeTruthy()
})

test('checkout clears idempotency key only for definitive terminal failure', async ({ page }) => {
  await mockApi(page)
  await page.route('**/api/v1/checkout', async route => {
    const body = JSON.stringify({ success: false, error: { code: 'PAYOS_REQUEST_FAILED', message: 'payOS failed.', fieldErrors: [] }, timestamp: '2026-08-17T10:00:00+07:00', correlationId: 'e2e' })
    return route.fulfill({ status: 502, contentType: 'application/json', body })
  })
  await page.goto('/checkout')
  await page.getByLabel('Số điện thoại').fill('0909000000')
  await page.getByLabel('Địa chỉ').fill('Bau Truc')
  await page.getByRole('button', { name: /Đặt hàng/i }).click()
  await expect(page.getByRole('alert')).toContainText(/payOS|thanh toán/i)
  await expect.poll(() => page.evaluate(() => sessionStorage.getItem('bautruc.checkout.idempotency-key'))).toBeNull()
})

test('public workshop booking and support settings render on mobile', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await mockApi(page)
  await page.goto('/workshop')
  await expect(page.getByRole('heading', { name: /Chạm Tay Vào Lịch Sử/i })).toBeVisible()
  await page.getByLabel('Họ và tên').fill('Nguyen Van A')
  await page.getByLabel('Email').fill('a@example.com')
  await page.getByLabel('Số điện thoại').fill('0909000000')
  await page.getByLabel('Ngày giờ mong muốn').fill('2026-08-18T09:00')
  await page.getByRole('button', { name: /Gửi yêu cầu đặt lịch/i }).click()
  await expect(page.getByRole('status')).toContainText('#9')
  await page.goto('/support')
  await expect(page.getByRole('main').getByRole('link', { name: 'support@example.com' })).toBeVisible()
  await expect(page.getByRole('main').getByText('Bau Truc')).toBeVisible()
})

test('admin updates bilingual slogan and homepage plus footer refresh from the API', async ({ page, isMobile }) => {
  await mockApi(page, 'ADMIN')
  await page.goto('/admin?section=homepage')
  await page.getByLabel('Slogan tiếng Việt').fill('Đất kể chuyện qua bàn tay Chăm')
  await page.getByLabel('Slogan tiếng Anh').fill('Clay tells stories through Cham hands')
  await page.getByRole('button', { name: 'Lưu thay đổi' }).click()
  await expect(page.getByText('Đã cập nhật slogan trang chủ.')).toBeVisible()

  const homepageResponse = page.waitForResponse(response => new URL(response.url()).pathname === '/api/v1/home')
  await page.goto('/')
  const homepagePayload = await (await homepageResponse).json()
  expect(homepagePayload.data.sloganVi).toBe('Đất kể chuyện qua bàn tay Chăm')
  await expect(page.getByRole('heading', { name: 'Đất kể chuyện qua bàn tay Chăm' })).toBeVisible()
  await expect(page.locator('.site-footer')).toContainText('Đất kể chuyện qua bàn tay Chăm')
  if (isMobile) await page.getByRole('button', { name: 'Mở menu' }).click()
  await page.getByRole('button', { name: 'ENG' }).first().click()
  await expect(page.getByRole('heading', { name: 'Clay tells stories through Cham hands' })).toBeVisible()
  await expect(page.locator('.site-footer')).toContainText('Clay tells stories through Cham hands')
  await page.reload()
  await expect(page.getByRole('heading', { name: 'Clay tells stories through Cham hands' })).toBeVisible()
})

test('guest cart persists then Google login merges it and continues to checkout', async ({ page }) => {
  await mockGoogleIdentity(page)
  await mockApi(page, null)
  let accountCart = { items: [], totalAmount: 0 }
  await page.route('**/api/v1/auth/google', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { user: { id: 2, fullName: 'Customer', email: 'customer@example.com', role: 'USER' } } }) }))
  await page.route('**/api/v1/cart', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: accountCart }) }))
  await page.route('**/api/v1/cart/items', async route => {
    const { productId, quantity } = await route.request().postDataJSON()
    accountCart = { items: [{ ...cart.items[0], productId, quantity, lineTotal: product.sellingPrice * quantity }], totalAmount: product.sellingPrice * quantity }
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: accountCart }) })
  })
  await page.goto('/products/10')
  await page.getByRole('button', { name: 'Thêm vào giỏ hàng' }).click()
  await page.goto('/cart')
  await expect(page.getByRole('heading', { name: 'Giỏ hàng của bạn' })).toBeVisible()
  await expect(page.getByText('Binh gom do')).toBeVisible()
  await page.getByRole('button', { name: 'Tăng số lượng' }).click()
  await page.reload()
  await expect(page.locator('.checkout-item')).toContainText('2')
  await page.getByRole('button', { name: 'Đăng nhập để thanh toán' }).click()
  await expect.poll(() => page.evaluate(() => window.__googlePrompted)).toBeTruthy()
  await page.evaluate(() => window.__googleCredentialCallback({ credential: 'valid-google-credential' }))
  await expect(page).toHaveURL(/\/checkout$/)
  await expect(page.getByText('Binh gom do')).toBeVisible()
  await expect.poll(() => page.evaluate(() => localStorage.getItem('bautruc.guest-cart.v1'))).toBeNull()
  expect(accountCart.items[0].quantity).toBe(2)
})

test('failed checkout login preserves the guest cart and stays on cart', async ({ page }) => {
  await mockGoogleIdentity(page)
  await mockApi(page, null)
  await page.route('**/api/v1/auth/google', route => route.fulfill({
    status: 401,
    contentType: 'application/json',
    body: JSON.stringify({ success: false, error: { code: 'INVALID_GOOGLE_TOKEN', message: 'Google login failed.', fieldErrors: [] } })
  }))
  await page.goto('/products/10')
  await page.getByRole('button', { name: 'Thêm vào giỏ hàng' }).click()
  await page.goto('/cart')
  await page.getByRole('button', { name: 'Continue with Google' }).click()
  await page.evaluate(() => window.__googleCredentialCallback({ credential: 'invalid-google-credential' }))
  await expect(page).toHaveURL(/\/cart$/)
  await expect(page.getByText('Google login failed.')).toBeVisible()
  await expect.poll(() => page.evaluate(() => JSON.parse(localStorage.getItem('bautruc.guest-cart.v1')))).toEqual([{ productId: 10, quantity: 1 }])
})

test('normal header Google login does not create checkout redirect', async ({ page, isMobile }) => {
  await mockGoogleIdentity(page)
  await mockApi(page, null)
  await page.route('**/api/v1/auth/google', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { user: { id: 2, fullName: 'Customer', email: 'customer@example.com', role: 'USER' } } }) }))
  await page.goto('/')
  if (isMobile) await page.getByRole('button', { name: 'Mở menu' }).click()
  await page.locator('header').getByRole('button', { name: 'Đăng nhập' }).first().click()
  await page.evaluate(() => window.__googleCredentialCallback({ credential: 'valid-google-credential' }))
  await expect(page).toHaveURL('/')
})

test('mobile typography has Vietnamese glyphs without clipping, overlap or horizontal overflow', async ({ page }) => {
  test.setTimeout(90000)
  await mockApi(page, null)
  const viewports = [
    { width: 390, height: 844 },
    { width: 412, height: 915 },
    { width: 430, height: 932 }
  ]

  for (const viewport of viewports) {
    await page.setViewportSize(viewport)
    for (const path of ['/', '/products/10', '/cart']) {
      await page.goto(path)
      await expect(page.locator('header')).toBeVisible()
      await page.evaluate(() => document.fonts.ready)
      const audit = await page.evaluate(() => {
        const visible = element => {
          const style = getComputedStyle(element)
          return style.display !== 'none' && style.visibility !== 'hidden' && element.getClientRects().length > 0
        }
        const clipped = [...document.querySelectorAll('h1,h2,h3,.button,.icon-btn')]
          .filter(visible)
          .filter(element => {
            const style = getComputedStyle(element)
            const clipsVertically = ['hidden', 'clip'].includes(style.overflowY) && element.scrollHeight > element.clientHeight + 2
            return element.scrollWidth > element.clientWidth + 2 || clipsVertically
          })
          .map(element => element.textContent.trim())
        const brand = document.querySelector('header > div > a[href="/"]')?.getBoundingClientRect()
        const tools = [...document.querySelectorAll('header > div > div')].find(visible)?.getBoundingClientRect()
        const overlaps = brand && tools && brand.right > tools.left && brand.left < tools.right && brand.bottom > tools.top && brand.top < tools.bottom
        return {
          overflow: document.documentElement.scrollWidth - document.documentElement.clientWidth,
          clipped,
          overlaps,
          bodyFont: getComputedStyle(document.body).fontFamily,
          fontLoaded: document.fonts.check('400 16px "Be Vietnam Pro"', 'Gìn giữ hồn di sản'),
          replacementGlyph: document.body.innerText.includes('\uFFFD')
        }
      })
      expect(audit.overflow, `${path} at ${viewport.width}px`).toBeLessThanOrEqual(1)
      expect(audit.clipped, `${path} at ${viewport.width}px`).toEqual([])
      expect(audit.overlaps, `${path} at ${viewport.width}px`).toBeFalsy()
      await expect.poll(() => page.evaluate(() => getComputedStyle(document.body).fontFamily)).toContain('Be Vietnam Pro')
      expect(audit.fontLoaded).toBeTruthy()
      expect(audit.replacementGlyph).toBeFalsy()
    }
  }

  await page.goto('/')
  await expect(page.getByText('Tinh hoa gốm Chăm – Gìn giữ hồn di sản').first()).toBeVisible()
  await page.goto('/products/10')
  await expect(page.getByRole('button', { name: 'Thêm vào giỏ hàng' })).toBeVisible()
  await page.goto('/cart')
  await expect(page.getByRole('heading', { name: 'Giỏ hàng của bạn' })).toBeVisible()
})

test('dark product title remains readable and mobile UNESCO badge does not overlap', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.addInitScript(() => localStorage.setItem('dangxem-theme', 'dark'))
  await mockApi(page, null)

  await page.goto('/products/10')
  const contrast = await page.locator('.product-detail-info h1').evaluate(element => {
    const parse = value => value.match(/[\d.]+/g).slice(0, 3).map(Number)
    const luminance = value => {
      const channels = parse(value).map(channel => {
        const normalized = channel / 255
        return normalized <= 0.04045 ? normalized / 12.92 : ((normalized + 0.055) / 1.055) ** 2.4
      })
      return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2]
    }
    const style = getComputedStyle(element)
    const foreground = luminance(style.color)
    const background = luminance(getComputedStyle(element.closest('.product-detail-info')).backgroundColor)
    return (Math.max(foreground, background) + 0.05) / (Math.min(foreground, background) + 0.05)
  })
  expect(contrast).toBeGreaterThanOrEqual(4.5)

  await page.goto('/')
  const badge = await page.getByText('UNESCO Heritage 2022', { exact: true }).boundingBox()
  const location = await page.getByText('Làng gốm Bàu Trúc · Khánh Hòa', { exact: true }).boundingBox()
  expect(badge).not.toBeNull()
  expect(location).not.toBeNull()
  expect(badge.y + badge.height).toBeLessThanOrEqual(location.y)
})

test('brand favicon assets are served as images instead of the SPA fallback', async ({ request }) => {
  const svg = await request.get('/favicon.svg?v=1')
  const ico = await request.get('/favicon.ico?v=1')
  const apple = await request.get('/apple-touch-icon.png?v=1')
  expect(svg.ok()).toBeTruthy()
  expect(await svg.text()).toContain('<svg')
  expect([...((await ico.body()).subarray(0, 6))]).toEqual([0, 0, 1, 0, 1, 0])
  expect([...((await apple.body()).subarray(0, 8))]).toEqual([137, 80, 78, 71, 13, 10, 26, 10])
})
