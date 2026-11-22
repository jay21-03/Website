// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import Admin from './Admin'
import { api } from './api'

vi.mock('./api', () => ({
  api: {
    adminFaqItems: vi.fn(),
    adminNotifications: vi.fn(),
    adminNotificationStreamUrl: vi.fn(),
    updateFaqItem: vi.fn()
  }
}))

beforeEach(() => {
  vi.clearAllMocks()
  api.adminNotifications.mockResolvedValue({ content: [], totalElements: 0 })
  api.adminNotificationStreamUrl.mockReturnValue('/api/v1/admin/notifications/stream')
})

afterEach(() => cleanup())

function mount(notify) {
  return render(
    <MemoryRouter initialEntries={['/admin?section=faq']}>
      <Admin user={{ id: 1, role: 'ADMIN', fullName: 'Admin', email: 'admin@example.test' }} notify={notify} />
    </MemoryRouter>
  )
}

describe('admin FAQ editor', () => {
  it('edits a bilingual FAQ inline and keeps its visibility settings', async () => {
    const notify = vi.fn()
    const item = {
      id: 17,
      questionVi: 'Gốm được làm như thế nào?',
      answerVi: 'Nặn hoàn toàn bằng tay.',
      questionEn: 'How is the pottery made?',
      answerEn: 'It is shaped entirely by hand.',
      position: 2,
      visible: true
    }
    api.adminFaqItems.mockResolvedValue([item])
    api.updateFaqItem.mockResolvedValue({ ...item, questionVi: 'Gốm Bàu Trúc được làm như thế nào?' })

    mount(notify)
    fireEvent.click(await screen.findByRole('button', { name: 'Sửa' }))

    const editor = screen.getByRole('region', { name: 'Nội dung song ngữ' })
    fireEvent.change(editor.querySelector('[name="questionVi"]'), {
      target: { value: 'Gốm Bàu Trúc được làm như thế nào?' }
    })
    fireEvent.submit(editor.querySelector('form'))

    await waitFor(() => expect(api.updateFaqItem).toHaveBeenCalledWith(
      17,
      expect.objectContaining({
        questionVi: 'Gốm Bàu Trúc được làm như thế nào?',
        questionEn: item.questionEn,
        position: 2,
        visible: true
      })
    ))
    await waitFor(() => expect(notify).toHaveBeenCalledWith('Đã lưu câu hỏi.'))
  })
})
