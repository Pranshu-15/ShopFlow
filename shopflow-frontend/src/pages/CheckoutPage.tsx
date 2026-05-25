import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { CheckCircle, Loader2, Package } from 'lucide-react'
import { CheckoutForm } from '@/components/CheckoutForm'
import { Button } from '@/components/ui/button'
import apiClient from '@/lib/apiClient'
import type { Cart } from '@/types/cart'
import type { CreateOrderRequest } from '@/types/order'

export function CheckoutPage() {
  const navigate                  = useNavigate()
  const [cart, setCart]           = useState<Cart | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [success, setSuccess]     = useState(false)
  const [orderId, setOrderId]     = useState<string | null>(null)

  useEffect(() => {
    apiClient.get<Cart>('/api/v1/cart')
      .then(({ data }) => setCart(data))
      .catch(() => navigate('/cart'))
      .finally(() => setIsLoading(false))
  }, [navigate])

  const handleSubmit = async () => {
    if (!cart) return

    const orderRequest: CreateOrderRequest = {
      currency: 'INR',
      items: cart.items.map((item) => ({
        productId:   String(item.productId),
        productName: item.productName,
        unitPrice:   item.unitPrice,
        quantity:    item.quantity,
      })),
    }

    const { data } = await apiClient.post<{ orderId: string }>('/api/v1/orders', orderRequest)
    await apiClient.delete('/api/v1/cart')
    setOrderId(data.orderId)
    setSuccess(true)
  }

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center py-32 gap-3">
        <Loader2 className="h-10 w-10 animate-spin text-primary" />
        <p className="text-sm text-muted-foreground">Loading checkout...</p>
      </div>
    )
  }

  if (success) {
    return (
      <div className="container mx-auto px-4 py-24 max-w-md text-center">
        <div className="relative inline-flex mb-8">
          <div className="h-24 w-24 rounded-3xl bg-green-100 dark:bg-green-950/40 flex items-center justify-center">
            <CheckCircle className="h-12 w-12 text-green-500" />
          </div>
          <div className="absolute -top-2 -right-2 h-8 w-8 rounded-full bg-primary flex items-center justify-center shadow-md">
            <Package className="h-4 w-4 text-primary-foreground" />
          </div>
        </div>
        <h2 className="text-3xl font-bold mb-3">Order Placed!</h2>
        <p className="text-muted-foreground mb-2">
          Your order is confirmed and being processed.
        </p>
        {orderId && (
          <p className="text-sm font-mono text-muted-foreground bg-muted px-4 py-2 rounded-full inline-block mb-8">
            Order #{orderId.slice(0, 8).toUpperCase()}
          </p>
        )}
        <div className="mt-4">
          <Button onClick={() => navigate('/orders')} className="rounded-full px-8 shadow-sm shadow-primary/30">
            View My Orders
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="border-b border-border/50">
        <div className="container mx-auto px-4 py-6">
          <h1 className="text-3xl font-bold">Checkout</h1>
        </div>
      </div>

      <div className="container mx-auto px-4 py-8 max-w-lg">
        <div className="rounded-2xl border border-border/60 bg-card shadow-sm overflow-hidden">
          <div className="px-6 py-5 border-b border-border/40 bg-muted/20">
            <h2 className="font-semibold text-lg">Review & Confirm</h2>
            <p className="text-xs text-muted-foreground mt-0.5">Complete your purchase below</p>
          </div>
          <div className="p-6">
            {cart && <CheckoutForm cart={cart} onSubmit={handleSubmit} />}
          </div>
        </div>
      </div>
    </div>
  )
}
