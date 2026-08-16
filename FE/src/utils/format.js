export function formatCurrencyVnd(value, lang = 'vi') {
  return new Intl.NumberFormat(lang === 'vi' ? 'vi-VN' : 'en-US', {
    style: 'currency', currency: 'VND', maximumFractionDigits: 0
  }).format(value ?? 0)
}

export function formatBusinessDateTime(value, lang = 'vi') {
  if (!value) return '—'
  return new Intl.DateTimeFormat(lang === 'vi' ? 'vi-VN' : 'en-US', {
    dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh'
  }).format(new Date(value))
}
