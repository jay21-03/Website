// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import Admin from './Admin'
import { api } from './api'

vi.mock('./api', () => ({
  api: {
    adminCollections: vi.fn(),
    adminNotifications: vi.fn(),
    adminNotificationStreamUrl: vi.fn(),
    adminSeoMetadata: vi.fn(),
    adminSitePresentation: vi.fn(),
    adminSupportSettings: vi.fn(),
    uploadSiteMedia: vi.fn(),
    updateSeoMetadata: vi.fn()
  }
}))

beforeEach(() => {
  vi.clearAllMocks()
  api.adminCollections.mockResolvedValue({ content: [], last: true })
  api.adminNotifications.mockResolvedValue({ content: [], totalElements: 0 })
  api.adminNotificationStreamUrl.mockReturnValue('/api/v1/admin/notifications/stream')
  api.adminSitePresentation.mockResolvedValue({ media: {} })
  api.uploadSiteMedia.mockResolvedValue({ url: 'https://cdn.example.test/seo-share.png' })
  api.adminSupportSettings.mockResolvedValue({
    email: 'hello@example.test',
    zaloPhone: '0900000000',
    address: 'Bau Truc'
  })
})

afterEach(() => cleanup())

function mount(notify) {
  const client = new QueryClient()
  vi.spyOn(client, 'invalidateQueries')
  const view = render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={['/admin?section=settings']}>
        <Admin user={{ id: 1, role: 'ADMIN', fullName: 'Admin', email: 'admin@example.test' }} notify={notify} />
      </MemoryRouter>
    </QueryClientProvider>
  )
  return { ...view, client }
}

describe('admin SEO settings', () => {
  it('lets an admin upload a shared social image while editing a specific page', async () => {
    const notify = vi.fn()
    const file = new File(['image'], 'share.png', { type: 'image/png' })
    api.adminSeoMetadata.mockResolvedValue({
      '/blog': { path: '/blog', title: 'Journal', description: 'Stories' }
    })
    api.updateSeoMetadata.mockResolvedValue({
      path: '/blog',
      title: 'Journal',
      description: 'Stories',
      imageSlot: 'SEO_SHARE'
    })

    const { client } = mount(notify)
    const pageSelect = await screen.findByLabelText('Trang')
    fireEvent.change(pageSelect, { target: { value: '/blog' } })
    await screen.findByDisplayValue('Journal')

    fireEvent.change(await screen.findByLabelText(/Tải ảnh chia sẻ lên/), {
      target: { files: [file] }
    })

    await waitFor(() => expect(api.uploadSiteMedia).toHaveBeenCalledWith('SEO_SHARE', file))
    await waitFor(() => expect(client.invalidateQueries).toHaveBeenCalledWith({ queryKey: ['site-presentation'] }))
    expect(screen.getByText('share.png')).toBeInTheDocument()
    expect(screen.getByLabelText('Ảnh chia sẻ')).toHaveValue('SEO_SHARE')
    fireEvent.submit(pageSelect.closest('form'))

    await waitFor(() => expect(api.updateSeoMetadata).toHaveBeenCalledWith(
      expect.objectContaining({ path: '/blog', imageSlot: 'SEO_SHARE' })
    ))
    await waitFor(() => expect(client.invalidateQueries).toHaveBeenCalledWith({ queryKey: ['seo-metadata'] }))
  })

  it('updates metadata for the selected page from the redesigned settings form', async () => {
    const notify = vi.fn()
    api.adminSeoMetadata.mockResolvedValue({
      '/': { path: '/', title: 'Home title', description: 'Home description' },
      '/blog': { path: '/blog', title: 'Blog title', description: 'Blog description' }
    })
    api.updateSeoMetadata.mockResolvedValue({
      path: '/blog',
      title: 'Updated Blog title',
      description: 'Blog description'
    })

    mount(notify)
    const pageSelect = await screen.findByLabelText('Trang')
    fireEvent.change(pageSelect, { target: { value: '/blog' } })

    const title = await screen.findByDisplayValue('Blog title')
    fireEvent.change(title, { target: { value: 'Updated Blog title' } })
    fireEvent.submit(pageSelect.closest('form'))

    await waitFor(() => expect(api.updateSeoMetadata).toHaveBeenCalledWith(
      expect.objectContaining({ path: '/blog', title: 'Updated Blog title' })
    ))
    await waitFor(() => expect(notify).toHaveBeenCalledWith('Đã lưu SEO và chia sẻ.'))
  })

  it('shows the saved sharing image and social cache refresh links', async () => {
    api.adminSeoMetadata.mockResolvedValue({
      default: { path: 'default', imageSlot: 'SEO_SHARE' },
      '/': { path: '/', imageSlot: 'WORKSHOP_HERO', title: 'Home title' }
    })
    api.adminSitePresentation.mockResolvedValue({ media: {
      SEO_SHARE: 'https://cdn.example.test/share.jpg',
      WORKSHOP_HERO: 'https://cdn.example.test/workshop.jpg'
    } })

    const { container } = mount(vi.fn())
    await screen.findByDisplayValue('Home title')
    expect(screen.getByText(/Ảnh đã lưu: WORKSHOP_HERO/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Zalo' })).toHaveAttribute('href', 'https://developers.zalo.me/tools/debug-sharing')
    expect(screen.getByRole('link', { name: 'Facebook' })).toHaveAttribute('href', 'https://developers.facebook.com/tools/debug/')
    expect(container.querySelector('.seo-social-preview img')).toHaveAttribute('src', 'https://cdn.example.test/workshop.jpg')

    fireEvent.change(screen.getByLabelText('Ảnh chia sẻ'), { target: { value: '' } })
    expect(container.querySelector('.seo-social-preview img')).toHaveAttribute('src', 'https://cdn.example.test/share.jpg')
  })
})
