export function BlogBody({ markdown = '' }) {
  const lines = markdown.split(/\r?\n/)
  const start = lines.findIndex(line => line.startsWith('# '))
  return lines.slice(start + 1).filter(line => line.trim() && !line.startsWith('[Ảnh:')).map((line, index) =>
    line.startsWith('## ') ? <h2 key={index}>{line.slice(3)}</h2> :
      line.startsWith('### ') ? <h3 key={index}>{line.slice(4)}</h3> : <p key={index}>{line}</p>)
}
