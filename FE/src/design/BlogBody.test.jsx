// @vitest-environment jsdom
import { render } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { BlogBody } from './BlogBody'

describe('BlogBody image placeholders', () => {
  it('omits trimmed Vietnamese and English placeholders regardless of case', () => {
    const { container } = render(
      <BlogBody markdown={'# Story\n  [Image: cover.jpg]\n[Ảnh: ảnh bìa]\n[image: another.jpg]\nStory text'} />
    )

    expect(container.textContent).toBe('Story text')
  })

  it('formats the Tet article headings, list, bold labels and internal links', () => {
    const { container, getByRole, queryByText } = render(<BlogBody markdown={'# Quà Tết\n\n## Cách nhận biết gốm Bàu Trúc làm tay thật\n\n- **Không có vết xoay tròn đều:** gốm nặn tay\n- **Màu không đều:** dấu vết nung\n\nXem [Chính sách mua hàng](/chinh-sach).\n\n---\n(Link /chinh-sach: đổi theo đường dẫn thật của trang Chính sách.)'} />)
    expect(getByRole('heading', { level: 2 }).textContent).toBe('Cách nhận biết gốm Bàu Trúc làm tay thật')
    expect(container.querySelectorAll('li')).toHaveLength(2)
    expect(container.querySelector('strong').textContent).toBe('Không có vết xoay tròn đều:')
    expect(getByRole('link', { name: 'Chính sách mua hàng' }).getAttribute('href')).toBe('/chinh-sach')
    expect(container.querySelector('hr')).not.toBeNull()
    expect(queryByText(/Link \/chinh-sach:/)).toBeNull()
    expect(container.textContent).not.toContain('**')
  })

  it('formats the village article and rejects unsafe links', () => {
    const { container, getByRole } = render(<BlogBody markdown={'# Làng gốm Bàu Trúc ở đâu?\n\n### Từ sân bay Cam Ranh\n\nĐi từ [trang Trải nghiệm](/trai-nghiem).\n\n- **Giờ ghé:** buổi sáng\n\n[Ảnh: cổng làng]\n[Bad](javascript:alert(1))'} />)
    expect(getByRole('heading', { level: 3 }).textContent).toBe('Từ sân bay Cam Ranh')
    expect(getByRole('link', { name: 'trang Trải nghiệm' }).getAttribute('href')).toBe('/trai-nghiem')
    expect(container.querySelector('li strong').textContent).toBe('Giờ ghé:')
    expect(container.textContent).not.toContain('[Ảnh:')
    expect(container.querySelectorAll('a')).toHaveLength(1)
    expect(container.textContent).toContain('Bad')
  })
})
