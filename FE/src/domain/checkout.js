import { z } from 'zod'

export const checkoutSchema = z.object({
  receiverName: z.string().trim().min(1, 'Vui lòng nhập họ và tên.').max(255, 'Họ và tên tối đa 255 ký tự.'),
  phone: z.string().trim().min(8, 'Số điện thoại phải có ít nhất 8 ký tự.').max(32, 'Số điện thoại tối đa 32 ký tự.'),
  email: z.union([z.literal(''), z.string().trim().email('Email không hợp lệ.').max(320, 'Email tối đa 320 ký tự.')]),
  address: z.string().trim().min(1, 'Vui lòng nhập địa chỉ.').max(500, 'Địa chỉ tối đa 500 ký tự.'),
  note: z.string().trim().max(1000, 'Ghi chú tối đa 1000 ký tự.')
})

export function validateCheckout(values) {
  const result = checkoutSchema.safeParse(values)
  if (result.success) return { data: result.data, errors: {} }
  return {
    data: null,
    errors: Object.fromEntries(result.error.issues.map(issue => [String(issue.path[0]), issue.message]))
  }
}
