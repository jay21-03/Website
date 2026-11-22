// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import Admin from './Admin'
import { api } from './api'

vi.mock('./api', () => ({
  api: {
    home: vi.fn(),
    adminBlogPosts: vi.fn(),
    adminNotifications: vi.fn(),
    adminNotificationStreamUrl: vi.fn(),
    createBlogPost: vi.fn(),
    updateBlogPost: vi.fn(),
    uploadBlogImage: vi.fn()
  }
}))

beforeEach(() => {
  vi.clearAllMocks()
  api.home.mockResolvedValue({ heroImageUrl: 'https://cdn.example/home-hero.jpg' })
  api.adminNotifications.mockResolvedValue({ content: [], totalElements: 0 })
  api.adminNotificationStreamUrl.mockReturnValue('/api/v1/admin/notifications/stream')
})

afterEach(() => cleanup())

function mount(notify) {
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter initialEntries={['/admin?section=blog']}>
        <Admin user={{ id: 1, role: 'ADMIN', fullName: 'Admin', email: 'admin@example.test' }} notify={notify} />
      </MemoryRouter>
    </QueryClientProvider>
  )
}

describe('admin blog cover upload', () => {
  it('sends PUBLISHED when changing an existing draft to publish immediately', async () => {
    const user = userEvent.setup()
    const notify = vi.fn()
    const draft = {
      id: 303,
      slug: 'test-story',
      title: 'Test story',
      bodyMarkdown: '# Test story',
      status: 'DRAFT',
      publishAt: null,
      imageUrl: null
    }
    api.adminBlogPosts.mockResolvedValue([draft])
    api.updateBlogPost.mockResolvedValue({ ...draft, status: 'PUBLISHED' })

    mount(notify)
    fireEvent.click(await screen.findByRole('button', { name: 'Chỉnh sửa' }))
    await user.selectOptions(screen.getByLabelText('Hiển thị bài viết'), 'PUBLISHED')
    expect(screen.getByLabelText('Hiển thị bài viết').value).toBe('PUBLISHED')
    fireEvent.submit(document.querySelector('.blog-admin-form'))

    await waitFor(() => expect(api.updateBlogPost).toHaveBeenCalledWith(
      303,
      expect.objectContaining({ status: 'PUBLISHED', publishAt: null })
    ))
    await waitFor(() => expect(notify).toHaveBeenCalledWith('Đã lưu bài viết.'))
  })

  it('does not report failure when the upload response is lost after the image was saved', async () => {
    const notify = vi.fn()
    const savedPost = {
      id: 303,
      slug: 'test-story',
      title: 'Test story',
      bodyMarkdown: '# Test story',
      status: 'DRAFT',
      imageUrl: null
    }
    api.adminBlogPosts
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([{ ...savedPost, imageUrl: 'https://cdn.example/site/blog/303/image.png' }])
    api.createBlogPost.mockResolvedValue(savedPost)
    api.uploadBlogImage.mockRejectedValue(new TypeError('Failed to fetch'))

    mount(notify)
    fireEvent.click(await screen.findByRole('button', { name: /Viết bài mới/ }))
    fireEvent.change(screen.getByLabelText('Tiêu đề (VI)'), { target: { value: 'Test story' } })
    fireEvent.change(screen.getByLabelText('Nội dung (VI) — Markdown'), { target: { value: '# Test story' } })
    await userEvent.upload(
      screen.getByLabelText('Ảnh bìa'),
      new File(['image'], 'cover.png', { type: 'image/png' })
    )
    expect(screen.getByLabelText('Ảnh bìa').files[0].size).toBeGreaterThan(0)
    fireEvent.submit(document.querySelector('.blog-admin-form'))

    await waitFor(() => expect(api.uploadBlogImage).toHaveBeenCalledTimes(1))
    await waitFor(() => expect(notify).toHaveBeenCalledWith(expect.stringContaining('ảnh bìa đã được lưu')))
    expect(notify).not.toHaveBeenCalledWith(expect.stringContaining('chưa xác nhận ảnh bìa'))
    expect(api.adminBlogPosts).toHaveBeenCalledTimes(2)
  })

  it('saves English fields and previews them independently', async () => {
    const notify = vi.fn()
    const story = {
      id: 304,
      slug: 'english-story',
      title: 'Tiêu đề',
      bodyMarkdown: '# Tiêu đề\nNội dung tiếng Việt',
      titleEn: 'English title',
      bodyMarkdownEn: '# English title\nEnglish content',
      status: 'DRAFT',
      publishAt: null,
      imageUrl: null
    }
    api.adminBlogPosts.mockResolvedValue([story])
    api.updateBlogPost.mockResolvedValue(story)

    mount(notify)
    fireEvent.click(await screen.findByRole('button', { name: 'Chỉnh sửa' }))
    fireEvent.input(document.querySelector('[name="titleEn"]'), { target: { value: 'Updated English title' } })
    fireEvent.input(document.querySelector('[name="bodyMarkdownEn"]'), { target: { value: '# Updated English title\nUpdated English content' } })
    fireEvent.click(screen.getByRole('button', { name: 'English' }))

    const preview = document.querySelector('.blog-admin-preview')
    expect(preview.querySelector('h2').textContent).toBe('Updated English title')
    expect(preview.textContent).toContain('Updated English content')

    fireEvent.submit(document.querySelector('.blog-admin-form'))
    await waitFor(() => expect(api.updateBlogPost).toHaveBeenCalledWith(
      304,
      expect.objectContaining({
        titleEn: 'Updated English title',
        bodyMarkdownEn: '# Updated English title\nUpdated English content'
      })
    ))
  })

  it('leaves posts without a cover image empty in the list and preview', async () => {
    const story = {
      id: 305,
      slug: 'no-cover-story',
      title: 'Story without cover',
      bodyMarkdown: '# Story without cover',
      status: 'DRAFT',
      imageUrl: null
    }
    api.adminBlogPosts.mockResolvedValue([story])

    mount(vi.fn())
    await screen.findByText('Story without cover')
    expect(document.querySelector('.blog-admin-row img')).toBeNull()

    fireEvent.click(screen.getByRole('button', { name: 'Chỉnh sửa' }))
    expect(document.querySelector('.blog-admin-preview img')).toBeNull()
  })
})
