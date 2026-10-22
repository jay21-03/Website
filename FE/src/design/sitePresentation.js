import { createElement } from 'react'

const pair = (vi, en) => ({ vi, en })

export const presentationFields = {
  brand: {
    name: pair('Đàng Xem', 'Đàng Xem'),
    footerDescription: pair('Gốm thủ công Chăm Bàu Trúc — Di sản UNESCO 2022.', 'Handcrafted Cham pottery from Bau Truc — UNESCO Heritage 2022.'),
    navHome: pair('Trang Chủ', 'Home'),
    navProducts: pair('Sản Phẩm', 'Products'),
    navExperience: pair('Trải Nghiệm', 'Experience'),
    navAbout: pair('Về Chúng Tôi', 'About Us'),
    navContact: pair('Liên Hệ', 'Contact'),
    footerLinksTitle: pair('Liên kết', 'Links'),
    footerContactTitle: pair('Liên hệ', 'Contact'),
    footerCart: pair('Giỏ hàng', 'Cart'),
    footerOrders: pair('Đơn hàng', 'Orders'),
    footerSupport: pair('Hỗ trợ', 'Support'),
    footerPolicy: pair('Chính sách', 'Policy'),
    mobileProducts: pair('Sản phẩm', 'Shop'),
    mobileBooking: pair('Đặt lịch', 'Book'),
    mobileCall: pair('Gọi', 'Call'),
    mobileCart: pair('Giỏ hàng', 'Cart'),
    menuBooking: pair('Đặt Lịch Trải Nghiệm', 'Book an Experience'),
    menuCall: pair('Gọi nghệ nhân', 'Call the artisan')
  },
  home: {
    eyebrow: pair('Làng gốm Bàu Trúc · Khánh Hòa', 'Bàu Trúc pottery village · Khánh Hòa'),
    heroTagline: pair('Gốm thủ công Chăm Bàu Trúc | Di sản UNESCO 2022', 'Handmade Cham pottery from Bàu Trúc | UNESCO Heritage 2022'),
    heroBody: pair('Mỗi sản phẩm là một phiên bản duy nhất — được tạo ra hoàn toàn bằng đôi bàn tay của nghệ nhân Chăm, từ đất sét tự nhiên làng Bàu Trúc, Khánh Hòa.', 'Every piece is one of a kind — shaped entirely by the hands of Cham artisans from natural clay of Bàu Trúc village, Khánh Hòa.'),
    productsAction: pair('Khám Phá Sản Phẩm', 'Explore Products'),
    bookingAction: pair('Đặt Lịch Trải Nghiệm', 'Book an Experience'),
    storyAction: pair('Đọc thêm câu chuyện của chúng tôi →', 'Read more of our story →'),
    detailsAction: pair('Xem Chi Tiết', 'View Details'),
    bookNowAction: pair('Đặt Lịch Ngay', 'Book Now'),
    trust1Title: pair('100% Thủ Công', '100% Handmade'),
    trust1Body: pair('Không khuôn, không bàn xoay máy — tạo hình hoàn toàn bằng tay.', 'No molds, no wheel — every piece is shaped entirely by hand.'),
    trust2Title: pair('Nguyên Liệu Tự Nhiên', 'Natural Materials'),
    trust2Body: pair('Đất sét lấy từ cánh đồng Nu Lanh, pha cát mịn sông Quao.', 'Clay from the Nu Lanh fields blended with fine Quao river sand.'),
    trust3Title: pair('Di Sản UNESCO', 'UNESCO Heritage'),
    trust3Body: pair('Nghệ thuật gốm Chăm Bàu Trúc được ghi danh năm 2022.', 'Cham pottery of Bàu Trúc was inscribed by UNESCO in 2022.'),
    storyEyebrow: pair('Câu chuyện', 'Our story'),
    storyTitle: pair('Hơn 15 năm giữ lửa nghề Chăm', 'Over 15 years keeping the Cham craft alive'),
    storyBody: pair('Từ năm 2009, Nghệ nhân Đàng Xem đã gìn giữ và phát triển nghề gốm truyền thống của người Chăm tại làng Bàu Trúc. Mỗi sản phẩm mang trong mình hàng trăm năm lịch sử và dấu ấn riêng của đôi bàn tay tạo ra nó.', 'Since 2009, artisan Đàng Xem has preserved and developed the traditional Cham pottery craft in Bàu Trúc village. Each piece carries centuries of history and the mark of the hands that made it.'),
    featuredEyebrow: pair('Bộ sưu tập', 'Collection'),
    featuredTitle: pair('Sản Phẩm Nổi Bật', 'Featured Products'),
    experienceTitle: pair('Trải Nghiệm Làm Gốm Cùng Nghệ Nhân', 'Make Pottery with the Artisan'),
    experienceBody: pair('Đặt tay vào đất sét — cảm nhận hàng trăm năm văn hóa Chăm qua đôi bàn tay của chính bạn. Workshop phù hợp cho cá nhân, gia đình và đoàn tour.', 'Put your hands in the clay and feel centuries of Cham culture. Workshops for individuals, families and tour groups.'),
    partnersTitle: pair('Đối Tác Tin Cậy', 'Trusted Partners'),
    partner1: pair('Amanoi Resort', 'Amanoi Resort'),
    partner2: pair('Tour lữ hành địa phương', 'Local tour operators'),
    partner3: pair('UNESCO', 'UNESCO'),
    socialTitle: pair('Theo Dõi Hành Trình Gốm', 'Follow the Pottery Journey'),
    socialHandle: pair('@dangxem.baoutruc', '@dangxem.baoutruc')
  },
  workshop: {
    heroTitle: pair('Chạm Tay Vào Lịch Sử', 'Touch History with Your Hands'),
    heroSubtitle: pair('Trải nghiệm làm gốm Chăm cùng nghệ nhân — tự tay tạo nên một tác phẩm của riêng bạn.', 'Make Cham pottery with the artisan — shape a piece of your own.'),
    introEyebrow: pair('Trải nghiệm', 'Experience'),
    introTitle: pair('Một Buổi Làm Nghệ Nhân', 'Be an Artisan for a Day'),
    step1Title: pair('Tìm hiểu lịch sử gốm Chăm', 'Learn about Cham pottery heritage'),
    step1Body: pair('Nghe nghệ nhân kể về làng Bàu Trúc, đất Nu Lanh và kỹ thuật nung lộ thiên.', 'Hear the artisan tell of Bàu Trúc village, Nu Lanh clay and open-air firing.'),
    step2Title: pair('Tự tay nặn và tạo hình', 'Shape your own piece'),
    step2Body: pair('Không bàn xoay — bạn đi vòng quanh khối đất theo đúng cách người Chăm làm.', 'No wheel — you walk around the clay exactly as the Cham people do.'),
    step3Title: pair('Mang về kỷ niệm độc bản', 'Take home your creation'),
    step3Body: pair('Sản phẩm được hong khô, hoàn thiện và gửi về tận tay bạn.', 'Your piece is dried, finished and delivered to you.'),
    bookingTitle: pair('Thông tin đặt lịch', 'Booking information'),
    informationTitle: pair('Thông tin trải nghiệm', 'Experience information')
  },
  support: {
    heroTitle: pair('Kết Nối Với Chúng Tôi', 'Get in Touch'),
    heroSubtitle: pair('Xưởng mở cửa mỗi ngày — ghé thăm, đặt hàng hoặc hẹn lịch trải nghiệm.', 'Our studio is open daily — visit us, place an order or book a workshop.'),
    contactTitle: pair('Cơ sở gốm Bàu Trúc Đàng Xem', 'Dang Xem Bau Truc Pottery'),
    question1: pair('Gốm Bàu Trúc có dùng bàn xoay không?', 'Is Bau Truc pottery made with a wheel?'),
    answer1: pair('Không. Sản phẩm được tạo hình hoàn toàn bằng tay theo kỹ thuật truyền thống của người Chăm.', 'No. Every piece is shaped entirely by hand using traditional Cham techniques.'),
    question2: pair('Làm sao theo dõi đơn hàng?', 'How can I track an order?'),
    answer2: pair('Đăng nhập và mở mục Đơn hàng trên thanh điều hướng.', 'Sign in and open Orders in the navigation bar.')
  },
  catalog: {
    title: pair('Từng Sản Phẩm — Một Câu Chuyện', 'Every Piece — A Story'),
    subtitle: pair('Gốm nặn tay, nung lộ thiên bằng rơm và củi. Không sản phẩm nào giống sản phẩm nào.', 'Hand-shaped pottery, fired in the open air with straw and wood. No two pieces are alike.'),
    contactBody: pair('Cần sản phẩm theo yêu cầu? Chúng tôi nhận đặt hàng độc bản.', 'Looking for a custom piece? We welcome bespoke orders.'),
    contactAction: pair('Liên Hệ Đặt Hàng →', 'Contact us →'),
    addToCart: pair('Thêm vào giỏ hàng', 'Add to cart'),
    buyNow: pair('Mua ngay', 'Buy Now'),
    descriptionTab: pair('Mô tả sản phẩm', 'Description'),
    specificationsTab: pair('Thông số kỹ thuật', 'Specifications')
  },
  cart: {
    title: pair('Giỏ hàng của bạn', 'Your cart'),
    empty: pair('Giỏ hàng đang trống.', 'Your cart is empty.'),
    checkoutAction: pair('Tiếp tục thanh toán', 'Continue to checkout')
  },
  orders: {
    title: pair('Đơn hàng của tôi', 'My orders')
  },
  faq: {
    title: pair('Câu hỏi thường gặp', 'Frequently asked questions'),
    question1: pair('Gốm Bàu Trúc có dùng bàn xoay không?', 'Is Bau Truc pottery made with a wheel?'),
    answer1: pair('Không. Sản phẩm được tạo hình bằng tay theo kỹ thuật truyền thống của người Chăm.', 'No. Products are shaped by hand using traditional Cham techniques.'),
    question2: pair('Tôi có thể đặt lịch workshop không?', 'Can I book a workshop?'),
    answer2: pair('Có. Mở trang Workshop, chọn gói phù hợp và gửi yêu cầu đặt lịch để cửa hàng xác nhận.', 'Yes. Open Workshop, choose an offering, and submit a booking request for store confirmation.'),
    question3: pair('Thanh toán online hoạt động thế nào?', 'How does online payment work?'),
    answer3: pair('Hệ thống tạo Payment trạng thái PENDING và hiển thị thông tin payOS nếu cổng thanh toán đã được cấu hình.', 'The system creates a PENDING payment and shows payOS details when the gateway is configured.')
  },
  policy: {
    title: pair('Chính sách mua hàng', 'Purchase policy'),
    paymentTitle: pair('Thanh toán', 'Payment'),
    paymentBody: pair('Đơn hàng sử dụng thanh toán payOS. Ứng dụng không tự đánh dấu thanh toán thành công; trạng thái được cập nhật từ hệ thống backend.', 'Orders use payOS payment. The app does not mark payment success by itself; status is updated by the backend system.'),
    refundTitle: pair('Hoàn tiền thủ công', 'Manual refund'),
    refundBody: pair('Hoàn tiền tự động ngoài phạm vi. Khi cần, admin chỉ ghi nhận khoản hoàn tiền đã thực hiện bên ngoài hệ thống.', 'Automatic refund is out of scope. When needed, admins only record refunds already completed outside the system.'),
    shippingTitle: pair('Vận chuyển', 'Shipping'),
    shippingBody: pair('Phí vận chuyển và theo dõi vận chuyển chưa thuộc phạm vi hệ thống hiện tại.', 'Shipping fee and shipment tracking are outside the current system scope.')
  },
  checkout: {
    title: pair('Thanh toán', 'Checkout'),
    formTitle: pair('Thông tin nhận hàng', 'Recipient information'),
    summaryTitle: pair('Đơn hàng của bạn', 'Your order'),
    buyNowTitle: pair('Mua ngay', 'Buy Now'),
    receiverName: pair('Họ và tên', 'Full name'),
    phone: pair('Số điện thoại', 'Phone number'),
    email: pair('Email', 'Email'),
    address: pair('Địa chỉ', 'Address'),
    note: pair('Ghi chú', 'Note'),
    submit: pair('Đặt hàng', 'Place order'),
    submitting: pair('Đang tạo đơn hàng...', 'Creating order...'),
    quantity: pair('Số lượng', 'Quantity'),
    total: pair('Tổng cộng', 'Total'),
    remove: pair('Xóa', 'Remove'),
    buyNowNotice: pair('Mua ngay không thay đổi giỏ hàng hiện có. Giá và tồn kho được kiểm tra lại khi đặt hàng.', 'Buy Now does not change your cart. Price and stock are revalidated when placing the order.')
  }
}

export const presentationDefaults = {
  pages: presentationFields,
  checkout: {
    fieldOrder: ['receiverName', 'phone', 'email', 'address', 'note'],
    sectionOrder: ['form', 'summary'],
    showNote: true,
    showEmail: true
  },
  colorPreset: 'clay',
  fontPreset: 'heritage'
}

export const colorPresets = {
  clay: { name: 'Heritage Clay', colors: ['#FBF6EC', '#F1E4D2', '#9D4B2F', '#2C1A0E', '#704126', '#C69A56'] },
  royal: { name: 'Royal Pottery', colors: ['#F9F4EC', '#EDE2D5', '#563820', '#25170F', '#826044', '#B1844C'] },
  minimal: { name: 'Ceramic Minimal', colors: ['#FAF9F6', '#F0EDE7', '#484340', '#242424', '#786C64', '#AA8064'] },
  nature: { name: 'Artisan Nature', colors: ['#F7F4E9', '#EBE6D6', '#315641', '#233025', '#6A7052', '#B18C53'] },
  sunset: { name: 'Sunset Terracotta', colors: ['#FFF7F0', '#F9E4D6', '#AA4D3C', '#3A231B', '#865741', '#D69B64'] },
  night: { name: 'Night Kiln', colors: ['#1D1713', '#30251E', '#D18B5A', '#F5E9DB', '#AE8A72', '#E2B976'] }
}

export const fontPresets = {
  heritage: { name: 'Heritage Classic', heading: 'Playfair Display', body: 'Be Vietnam Pro' },
  vietnamese: { name: 'Vietnamese Heritage', heading: 'Noto Serif', body: 'Be Vietnam Pro' },
  editorial: { name: 'Warm Editorial', heading: 'Lora', body: 'Be Vietnam Pro' },
  commerce: { name: 'Modern Commerce', heading: 'Montserrat', body: 'Be Vietnam Pro' },
  minimal: { name: 'Minimal Vietnamese', heading: 'Be Vietnam Pro', body: 'Be Vietnam Pro' },
  modern: { name: 'Modern', heading: 'Be Vietnam Pro', body: 'DM Sans' }
}

export const presentationStyle = (content, page, key) => {
  const field = content?.pages?.[page]?.[key]
  return field?.font || field?.color ? {
    ...(field.font ? { fontFamily: `"${fontPresets[field.font]?.heading || fontPresets.heritage.heading}", "Be Vietnam Pro", sans-serif` } : {}),
    ...(field.font === 'minimal' ? { fontWeight: 700 } : {}),
    ...(field.color && /^#[0-9a-fA-F]{6}$/.test(field.color) ? { color: field.color } : {})
  } : undefined
}

export const presentationText = (content, page, key, lang) => {
  const value = content?.pages?.[page]?.[key] || presentationFields[page]?.[key]
  return value?.[lang] || value?.vi || ''
}

export const presentationNode = (content, page, key, lang) =>
  createElement('span', { style: presentationStyle(content, page, key) }, presentationText(content, page, key, lang))

export const visibleCheckoutFields = layout =>
  layout.fieldOrder.filter(name =>
    (name !== 'note' || layout.showNote) && (name !== 'email' || layout.showEmail !== false))
