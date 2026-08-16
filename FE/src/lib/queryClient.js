import { QueryClient } from '@tanstack/react-query'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: (count, error) => error?.status >= 500 && count < 1, refetchOnWindowFocus: false },
    mutations: { retry: false }
  }
})

export const cartKeys = { all: ['cart'] }
export const orderKeys = {
  all: ['orders'],
  lists: () => [...orderKeys.all, 'list'],
  list: page => [...orderKeys.lists(), { page }],
  details: () => [...orderKeys.all, 'detail'],
  detail: id => [...orderKeys.details(), String(id)]
}
