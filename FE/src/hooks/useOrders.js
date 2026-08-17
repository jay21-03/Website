import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { cartKeys, orderKeys } from '../lib/queryClient'
import { checkoutOrder, completeCheckoutAttempt, getMyOrder, getMyOrders } from '../services/orderService'
import { clearCheckoutIdempotencyKey, shouldClearCheckoutIdempotencyKey } from '../services/checkoutIdempotency'

export function useCheckout() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: checkoutOrder,
    onSuccess: result => {
      completeCheckoutAttempt()
      return Promise.all([
        client.invalidateQueries({ queryKey: cartKeys.all }),
        client.invalidateQueries({ queryKey: orderKeys.lists() }),
        client.setQueryData(orderKeys.detail(result.orderId), undefined)
      ])
    },
    onError: error => {
      if (shouldClearCheckoutIdempotencyKey(error)) clearCheckoutIdempotencyKey()
    }
  })
}

export const useMyOrders = (page, enabled) => useQuery({
  queryKey: orderKeys.list(page), queryFn: () => getMyOrders(page), enabled
})

const terminalPaymentStatuses = new Set(['PAID', 'FAILED', 'CANCELLED', 'EXPIRED', 'REFUNDED'])

export const useMyOrder = (id, enabled) => useQuery({
  queryKey: orderKeys.detail(id),
  queryFn: () => getMyOrder(id),
  enabled,
  refetchInterval: query => {
    const status = query.state.data?.paymentStatus
    return status === 'PENDING' && !terminalPaymentStatuses.has(status) ? 5000 : false
  }
})
