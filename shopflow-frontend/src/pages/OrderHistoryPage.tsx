import { useEffect, useRef, useState } from 'react'
import { Package, Loader2, ShoppingBag } from 'lucide-react'
import { Link } from 'react-router-dom'
import { OrderCard } from '@/components/OrderCard'
import { Button } from '@/components/ui/button'
import apiClient from '@/lib/apiClient'
import type { Order } from '@/types/order'
import { useAuth } from '@/hooks/useAuth'
import gsap from 'gsap'

export function OrderHistoryPage() {
  const { isLoading: authLoading, isAuthenticated, login } = useAuth()
  const [orders, setOrders]       = useState<Order[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError]         = useState<string | null>(null)
  const listRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (authLoading) return
    if (!isAuthenticated) { setIsLoading(false); return }

    const controller = new AbortController()
    apiClient
      .get<Order[]>('/api/v1/orders', { signal: controller.signal })
      .then(({ data }) => { if (!controller.signal.aborted) setOrders(data) })
      .catch((err) => { if (!controller.signal.aborted) { console.error(err); setError('Failed to load orders.') } })
      .finally(() => { if (!controller.signal.aborted) setIsLoading(false) })

    return () => controller.abort()
  }, [authLoading, isAuthenticated])

  useEffect(() => {
    if (!isLoading && orders.length > 0 && listRef.current) {
      const cards = listRef.current.querySelectorAll('.order-card')
      gsap.set(cards, { opacity: 0, y: 32, scale: 0.97 })
      gsap.to(cards, { opacity: 1, y: 0, scale: 1, duration: 0.45, stagger: 0.1, ease: 'power2.out' })
    }
  }, [isLoading, orders])

  if (isLoading || authLoading) {
    return (
      <div className="flex flex-col items-center justify-center py-32 gap-3">
        <Loader2 className="h-10 w-10 animate-spin text-primary" />
        <p className="text-sm text-muted-foreground">Loading your orders...</p>
      </div>
    )
  }

  if (!isAuthenticated) {
    return (
      <div className="container mx-auto px-4 py-24 max-w-md text-center">
        <div className="h-20 w-20 rounded-2xl bg-primary/10 flex items-center justify-center mx-auto mb-6">
          <Package className="h-10 w-10 text-primary" />
        </div>
        <h2 className="text-2xl font-bold mb-2">Sign in to view orders</h2>
        <p className="text-muted-foreground mb-8">Track all your past and current orders in one place.</p>
        <Button onClick={login} className="rounded-full px-8 shadow-sm shadow-primary/30">Sign In</Button>
      </div>
    )
  }

  if (error) {
    return (
      <div className="container mx-auto px-4 py-24 max-w-md text-center">
        <p className="text-destructive">{error}</p>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="border-b border-border/50">
        <div className="container mx-auto px-4 py-6">
          <h1 className="text-3xl font-bold">Order History</h1>
          {orders.length > 0 && (
            <p className="text-muted-foreground text-sm mt-1">{orders.length} order{orders.length !== 1 ? 's' : ''} placed</p>
          )}
        </div>
      </div>

      <div className="container mx-auto px-4 py-8 max-w-2xl">
        {orders.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-24 gap-4 text-center">
            <div className="h-24 w-24 rounded-3xl bg-muted flex items-center justify-center">
              <Package className="h-12 w-12 text-muted-foreground/30" />
            </div>
            <div>
              <h3 className="font-semibold text-lg mb-1">No orders yet</h3>
              <p className="text-muted-foreground text-sm">Your order history will appear here.</p>
            </div>
            <Button asChild className="rounded-full px-8 mt-2 shadow-sm shadow-primary/25">
              <Link to="/shop">
                <ShoppingBag className="mr-2 h-4 w-4" />
                Start Shopping
              </Link>
            </Button>
          </div>
        ) : (
          <div ref={listRef} className="space-y-4">
            {orders
              .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
              .map((order) => (
                <div key={order.id} className="order-card">
                  <OrderCard order={order} />
                </div>
              ))}
          </div>
        )}
      </div>
    </div>
  )
}
