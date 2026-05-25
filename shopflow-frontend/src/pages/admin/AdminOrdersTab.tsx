import { useState, useEffect } from 'react'
import { Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import apiClient from '@/lib/apiClient'
import type { Order, OrderStatus } from '@/types/order'

const STATUS_COLORS: Record<OrderStatus, string> = {
  PENDING:   'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400',
  CONFIRMED: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400',
  SHIPPED:   'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
  CANCELLED: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
  FAILED:    'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400',
}

const VALID_TRANSITIONS: Record<OrderStatus, OrderStatus[]> = {
  PENDING:   ['CONFIRMED', 'CANCELLED', 'FAILED'],
  CONFIRMED: ['SHIPPED', 'CANCELLED', 'FAILED'],
  SHIPPED:   [],
  CANCELLED: [],
  FAILED:    [],
}

export function AdminOrdersTab() {
  const [orders, setOrders]       = useState<Order[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [updating, setUpdating]   = useState<string | null>(null)
  const [selected, setSelected]   = useState<Record<string, OrderStatus>>({})

  useEffect(() => {
    apiClient.get<Order[]>('/api/v1/orders/admin/all')
      .then(({ data }) => {
        const sorted = [...data].sort(
          (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
        )
        setOrders(sorted)
        const init: Record<string, OrderStatus> = {}
        sorted.forEach((o) => { init[o.orderId] = o.status })
        setSelected(init)
      })
      .catch(() => alert('Failed to load orders.'))
      .finally(() => setIsLoading(false))
  }, [])

  const handleUpdate = async (order: Order) => {
    const newStatus = selected[order.orderId]
    if (!newStatus || newStatus === order.status) return
    setUpdating(order.orderId)
    try {
      const { data } = await apiClient.put<Order>(`/api/v1/orders/${order.orderId}/status`, {
        status: newStatus,
      })
      setOrders((prev) => prev.map((o) => (o.orderId === order.orderId ? data : o)))
    } catch {
      alert('Failed to update order status.')
      setSelected((prev) => ({ ...prev, [order.orderId]: order.status }))
    } finally {
      setUpdating(null)
    }
  }

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center py-20 gap-3">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
        <p className="text-sm text-muted-foreground">Loading orders...</p>
      </div>
    )
  }

  return (
    <>
      <p className="text-sm text-muted-foreground mb-4">{orders.length} total orders</p>

      <div className="overflow-x-auto rounded-xl border border-border/60 shadow-sm">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border/60 bg-muted/40">
              <th className="text-left px-4 py-3 font-semibold text-xs uppercase tracking-wide text-muted-foreground">Order ID</th>
              <th className="text-left px-4 py-3 font-semibold text-xs uppercase tracking-wide text-muted-foreground">User</th>
              <th className="text-right px-4 py-3 font-semibold text-xs uppercase tracking-wide text-muted-foreground">Total</th>
              <th className="text-left px-4 py-3 font-semibold text-xs uppercase tracking-wide text-muted-foreground">Date</th>
              <th className="text-center px-4 py-3 font-semibold text-xs uppercase tracking-wide text-muted-foreground">Status</th>
              <th className="text-left px-4 py-3 font-semibold text-xs uppercase tracking-wide text-muted-foreground">Update</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody className="divide-y divide-border/40">
            {orders.map((order) => {
              const transitions = VALID_TRANSITIONS[order.status]
              const isTerminal  = transitions.length === 0
              const isDirty     = selected[order.orderId] !== order.status
              return (
                <tr key={order.orderId} className="hover:bg-muted/20 transition-colors">
                  <td className="px-4 py-3 font-mono text-xs text-muted-foreground font-medium">
                    #{order.orderId.slice(0, 8).toUpperCase()}
                  </td>
                  <td className="px-4 py-3 max-w-[140px] truncate text-muted-foreground text-xs">
                    {order.userId.slice(0, 8)}…
                  </td>
                  <td className="px-4 py-3 text-right font-bold text-primary">
                    ₹{Number(order.totalAmount).toFixed(2)}
                  </td>
                  <td className="px-4 py-3 text-muted-foreground whitespace-nowrap text-xs">
                    {new Date(order.createdAt).toLocaleDateString('en-IN', {
                      day: '2-digit', month: 'short', year: 'numeric',
                    })}
                  </td>
                  <td className="px-4 py-3 text-center">
                    <span className={`inline-flex px-2.5 py-0.5 rounded-full text-xs font-semibold ${STATUS_COLORS[order.status]}`}>
                      {order.status}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    {isTerminal ? (
                      <span className="text-xs text-muted-foreground italic">Terminal</span>
                    ) : (
                      <select
                        className="rounded-lg border border-border/70 bg-background px-2 py-1.5 text-xs focus:outline-none focus:ring-2 focus:ring-ring"
                        value={selected[order.orderId] ?? order.status}
                        onChange={(e) =>
                          setSelected((prev) => ({
                            ...prev,
                            [order.orderId]: e.target.value as OrderStatus,
                          }))
                        }
                      >
                        <option value={order.status}>{order.status}</option>
                        {transitions.map((s) => (
                          <option key={s} value={s}>{s}</option>
                        ))}
                      </select>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    {!isTerminal && (
                      <Button
                        size="sm"
                        variant={isDirty ? 'default' : 'outline'}
                        className="h-7 text-xs rounded-lg px-3"
                        disabled={!isDirty || updating === order.orderId}
                        onClick={() => handleUpdate(order)}
                      >
                        {updating === order.orderId
                          ? <Loader2 className="h-3 w-3 animate-spin" />
                          : 'Apply'}
                      </Button>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </>
  )
}
