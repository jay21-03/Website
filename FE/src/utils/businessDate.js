const businessDateFormatter = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Asia/Ho_Chi_Minh',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit'
})

export const BUSINESS_TIME_ZONE = 'Asia/Ho_Chi_Minh'
const BUSINESS_OFFSET = '+07:00'

function parts(date) {
  return Object.fromEntries(businessDateFormatter.formatToParts(date).map(part => [part.type, part.value]))
}

export function localBusinessDate(value = new Date()) {
  const date = value instanceof Date ? value : new Date(value)
  const { year, month, day } = parts(date)
  return `${year}-${month}-${day}`
}

export function monthStartBusinessDate(value = new Date()) {
  const date = value instanceof Date ? value : new Date(value)
  const { year, month } = parts(date)
  return `${year}-${month}-01`
}

export function formatBusinessDateTime(value, locale = 'vi-VN') {
  if (!value) return '—'
  return new Intl.DateTimeFormat(locale, {
    dateStyle: 'short',
    timeStyle: 'short',
    timeZone: BUSINESS_TIME_ZONE
  }).format(new Date(value))
}

export function businessDateTimeLocal(value) {
  if (!value) return ''
  const date = value instanceof Date ? value : new Date(value)
  const dateParts = Object.fromEntries(new Intl.DateTimeFormat('en-CA', {
    timeZone: BUSINESS_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23'
  }).formatToParts(date).map(part => [part.type, part.value]))
  return `${dateParts.year}-${dateParts.month}-${dateParts.day}T${dateParts.hour}:${dateParts.minute}`
}

export function businessLocalDateTimeToOffset(value) {
  return value ? `${value}:00${BUSINESS_OFFSET}` : ''
}
