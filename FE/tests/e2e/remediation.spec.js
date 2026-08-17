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
    if (path === '/me') return ok({ id: 1, fullName: 'Admin User', email: 'admin@example.com', role, status: 'ACTIVE' })
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
    if (path === '/admin/reports/revenue') return ok({ totalRevenue: 200000, points: [{ periodStart: '2026-08-17', revenue: 200000 }] })
    if (path === '/admin/reports/best-selling') return ok([{ productNameVi: product.nameVi, soldQuantity: 2 }])
    if (path === '/admin/notifications') return ok({ content: [{ id: 1, title: 'Don moi', message: 'Co don hang moi', createdAt: order.createdAt, isRead: false }], page: 0, size: 10, totalElements: 37, totalPages: 4, first: true, last: false })
    if (path === '/admin/products' && route.request().method() === 'GET') {
      let rows = adminProducts
      if (url.searchParams.get('status')) rows = rows.filter(item => item.status === url.searchParams.get('status'))
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
    if (path === '/admin/inventory') return ok({ content: [{ productId: product.id, productNameVi: product.nameVi, quantity: 10, reservedQuantity: 1, availableQuantity: 9, status: 'AVAILABLE' }], page: 0, size: 20, totalElements: 1, totalPages: 1, first: true, last: true })
    if (path === '/admin/orders') return ok({ content: [order], page: 0, size: 20, totalElements: 1, totalPages: 1, first: true, last: true })
    if (path === '/admin/orders/77') return ok(order)
    return ok(null)
  })
}

test('public catalog delegates search/filter/sort to backend and opens product detail', async ({ page }) => {
  await mockApi(page)
  await page.goto('/products')
  await expect(page.getByRole('heading', { name: /Tất cả sản phẩm|All products/i })).toBeVisible()
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
  await expect(page.getByRole('heading', { name: /Đặt lịch trải nghiệm/i })).toBeVisible()
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
