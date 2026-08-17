import { z } from 'zod'

const text = (lang, vi, en) =>
  lang === 'en' ? en : vi

function createCheckoutSchema(lang = 'vi') {
  return z.object({
    receiverName: z.string()
      .trim()
      .min(
        1,
        text(
          lang,
          'Vui lòng nhập họ và tên.',
          'Please enter your full name.'
        )
      )
      .max(
        255,
        text(
          lang,
          'Họ và tên tối đa 255 ký tự.',
          'Full name must not exceed 255 characters.'
        )
      ),

    phone: z.string()
      .trim()
      .min(
        8,
        text(
          lang,
          'Số điện thoại phải có ít nhất 8 ký tự.',
          'Phone number must contain at least 8 characters.'
        )
      )
      .max(
        32,
        text(
          lang,
          'Số điện thoại tối đa 32 ký tự.',
          'Phone number must not exceed 32 characters.'
        )
      ),

    email: z.union([
      z.literal(''),
      z.string()
        .trim()
        .email(
          text(
            lang,
            'Email không hợp lệ.',
            'Invalid email address.'
          )
        )
        .max(
          320,
          text(
            lang,
            'Email tối đa 320 ký tự.',
            'Email must not exceed 320 characters.'
          )
        )
    ]),

    address: z.string()
      .trim()
      .min(
        1,
        text(
          lang,
          'Vui lòng nhập địa chỉ.',
          'Please enter your address.'
        )
      )
      .max(
        500,
        text(
          lang,
          'Địa chỉ tối đa 500 ký tự.',
          'Address must not exceed 500 characters.'
        )
      ),

    note: z.string()
      .trim()
      .max(
        1000,
        text(
          lang,
          'Ghi chú tối đa 1000 ký tự.',
          'Note must not exceed 1000 characters.'
        )
      )
  })
}

export function validateCheckout(values, lang = 'vi') {
  const result =
    createCheckoutSchema(lang).safeParse(values)

  if (result.success) {
    return {
      data: result.data,
      errors: {}
    }
  }

  return {
    data: null,
    errors: Object.fromEntries(
      result.error.issues.map(issue => [
        String(issue.path[0]),
        issue.message
      ])
    )
  }
}