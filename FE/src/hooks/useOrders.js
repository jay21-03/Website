import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { cartKeys, orderKeys } from '../lib/queryClient'
import { checkoutOrder, getMyOrder, getMyOrders } from '../services/orderService'

export function useCheckout() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: checkoutOrder,
    onSuccess: result => Promise.all([
      client.invalidateQueries({ queryKey: cartKeys.all }),
      client.invalidateQueries({ queryKey: orderKeys.lists() }),
      client.setQueryData(orderKeys.detail(result.orderId), undefined)
    ])
  })
}

export const useMyOrders = (page, enabled) => useQuery({
  queryKey: orderKeys.list(page), queryFn: () => getMyOrders(page), enabled
})

export const useMyOrder = (id, enabled) => useQuery({
  queryKey: orderKeys.detail(id), queryFn: () => getMyOrder(id), enabled
})
