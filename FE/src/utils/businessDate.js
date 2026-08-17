const businessDateFormatter = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Asia/Ho_Chi_Minh',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit'
})

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
