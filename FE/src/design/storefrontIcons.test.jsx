import { describe, expect, it } from 'vitest'
import { fireEvent, render } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import Home from './Home'
import { ChatWidget } from '../ChatWidget'

describe('uploaded storefront icons', () => {
  it('shows the uploaded trust icon and leaves the selected icon underneath as fallback', () => {
    const { container } = render(<MemoryRouter><Home media={{ SITE_TRUST_1_ICON: '/trust.png' }} /></MemoryRouter>)
    const image = container.querySelector('img[src="/trust.png"]')
    expect(image).not.toBeNull()
    expect(image.previousElementSibling.tagName.toLowerCase()).toBe('svg')
    fireEvent.error(image)
    expect(image.hidden).toBe(true)
  })

  it('shows the uploaded chat icon and keeps the built-in icon if the image fails', () => {
    const client = new QueryClient()
    const { container } = render(<QueryClientProvider client={client}><ChatWidget iconUrl="/chat.png" /></QueryClientProvider>)
    const image = container.querySelector('.chat-custom-icon')
    expect(image.getAttribute('src')).toBe('/chat.png')
    expect(image.previousElementSibling.tagName.toLowerCase()).toBe('svg')
    fireEvent.error(image)
    expect(image.hidden).toBe(true)
  })
})
