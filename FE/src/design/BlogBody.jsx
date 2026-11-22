const inlinePattern = /(\*\*[^*]+\*\*|\[[^\]]+\]\([^)]+\))/g

function inline(text) {
  return text.split(inlinePattern).map((part, index) => {
    if (part.startsWith('**') && part.endsWith('**')) return <strong key={index}>{part.slice(2, -2)}</strong>
    const link = /^\[([^\]]+)\]\(([^)]+)\)$/.exec(part)
    if (link) {
      const href = link[2]
      if ((href.startsWith('/') && !href.startsWith('//')) || /^https:\/\//i.test(href)) {
        return <a key={index} href={href}>{link[1]}</a>
      }
      return link[1]
    }
    return part
  })
}

export function BlogBody({ markdown = '' }) {
  const lines = markdown.split(/\r?\n/)
  const blocks = []
  let list = []
  let paragraph = []
  let hasContent = false

  const flush = () => {
    if (list.length) blocks.push(<ul key={blocks.length}>{list.map((item, index) => <li key={index}>{inline(item)}</li>)}</ul>)
    if (paragraph.length) blocks.push(<p key={blocks.length}>{inline(paragraph.join(' '))}</p>)
    list = []
    paragraph = []
  }

  for (const raw of lines) {
    const line = raw.trim()
    if (!hasContent && /^(?:Title|Meta|URL|Đăng):\s/i.test(line)) continue
    if (!line || /^\[(?:Ảnh|Image):/i.test(line) || /^\(Link \/[^)]+\)$/i.test(line)) {
      flush()
    } else if (line.startsWith('# ')) {
      if (hasContent) {
        flush()
        blocks.push(<h2 key={blocks.length}>{inline(line.slice(2))}</h2>)
      }
      hasContent = true
    } else if (line === '---') {
      flush()
      blocks.push(<hr key={blocks.length} />)
      hasContent = true
    } else if (line.startsWith('### ')) {
      flush()
      blocks.push(<h3 key={blocks.length}>{inline(line.slice(4))}</h3>)
      hasContent = true
    } else if (line.startsWith('## ')) {
      flush()
      blocks.push(<h2 key={blocks.length}>{inline(line.slice(3))}</h2>)
      hasContent = true
    } else if (line.startsWith('- ')) {
      if (paragraph.length) flush()
      list.push(line.slice(2))
      hasContent = true
    } else {
      if (list.length) flush()
      paragraph.push(line)
      hasContent = true
    }
  }
  flush()
  return blocks
}
