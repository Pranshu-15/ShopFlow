import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { ShoppingBag, Loader2, ShoppingCart, ArrowRight, Trash2 } from 'lucide-react'
import { CartItem } from '@/components/CartItem'
import { Button } from '@/components/ui/button'
import apiClient from '@/lib/apiClient'
import type { Cart } from '@/types/cart'
import { useAuth } from '@/hooks/useAuth'
import gsap from 'gsap'

export function CartPage() {
  const { isLoading: authLoading, isAuthenticated, login } = useAuth()
  const [cart, setCart]           = useState<Cart | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError]         = useState<string | null>(null)
  const pageRef = useRef<HTMLDivElement>(null)

  const fetchCart = async () => {
    setIsLoading(true)
    try {
      const { data } = await apiClient.get<Cart>('/api/v1/cart')
      setCart(data)
    } catch {
      setError('Failed to load cart.')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    if (authLoading) return
    if (!isAuthenticated) { setIsLoading(false); return }
    fetchCart()
  }, [authLoading, isAuthenticated])

  // Animate items in after load
  useEffect(() => {
    if (!isLoading && cart && cart.items.length > 0) {
      const rows = document.querySelectorAll('.cart-item-row')
      const summary = document.querySelector('.cart-summary')
      gsap.set(rows, { opacity: 0, x: -24 })
      gsap.to(rows, { opacity: 1, x: 0, duration: 0.4, stagger: 0.08, ease: 'power2.out' })
      if (summary) {
        gsap.set(summary, { opacity: 0, y: 20 })
        gsap.to(summary, { opacity: 1, y: 0, duration: 0.5, delay: 0.3 })
      }
    }
  }, [isLoading, cart])

  useEffect(() => {
    if (pageRef.current) {
      gsap.fromTo(pageRef.current, { opacity: 0 }, { opacity: 1, duration: 0.4 })
    }
  }, [])

  const handleUpdateQuantity = async (productId: number, quantity: number) => {
    try {
      if (quantity <= 0) {
        await apiClient.delete(`/api/v1/cart/items/${productId}`)
      } else {
        await apiClient.put(`/api/v1/cart/items/${productId}`, { quantity })
      }
      await fetchCart()
    } catch {
      alert('Failed to update item.')
    }
  }

  const handleRemove = async (productId: number) => {
    try {
      await apiClient.delete(`/api/v1/cart/items/${productId}`)
      await fetchCart()
    } catch {
      alert('Failed to remove item.')
    }
  }

  const handleClearCart = async () => {
    try {
      await apiClient.delete('/api/v1/cart')
      await fetchCart()
    } catch {
      alert('Failed to clear cart.')
    }
  }

  if (isLoading || authLoading) {
    return (
      <div className="flex flex-col items-center justify-center py-32 gap-3">
        <Loader2 className="h-10 w-10 animate-spin text-primary" />
        <p className="text-sm text-muted-foreground">Loading your cart...</p>
      </div>
    )
  }

  if (!isAuthenticated) {
    return (
      <div className="container mx-auto px-4 py-24 max-w-md text-center">
        <div className="h-20 w-20 rounded-2xl bg-primary/10 flex items-center justify-center mx-auto mb-6">
          <ShoppingCart className="h-10 w-10 text-primary" />
        </div>
        <h2 className="text-2xl font-bold mb-2">Sign in to view your cart</h2>
        <p className="text-muted-foreground mb-8">Your cart items are saved for when you return.</p>
        <Button onClick={login} className="rounded-full px-8 shadow-sm shadow-primary/30">Sign In</Button>
      </div>
    )
  }

  if (error) {
    return <p className="text-destructive text-center py-20">{error}</p>
  }

  const isEmpty = !cart || cart.items.length === 0

  return (
    <div ref={pageRef} className="min-h-screen bg-background">
      <div className="border-b border-border/50">
        <div className="container mx-auto px-4 py-6">
          <h1 className="text-3xl font-bold">Your Cart</h1>
          {!isEmpty && (
            <p className="text-muted-foreground text-sm mt-1">{cart.totalQuantity} item{cart.totalQuantity !== 1 ? 's' : ''}</p>
          )}
        </div>
      </div>

      <div className="container mx-auto px-4 py-8 max-w-2xl">
        {isEmpty ? (
          <div className="flex flex-col items-center justify-center py-24 gap-4 text-center">
            <div className="h-24 w-24 rounded-3xl bg-muted flex items-center justify-center">
              <ShoppingBag className="h-12 w-12 text-muted-foreground/30" />
            </div>
            <div>
              <h3 className="font-semibold text-lg mb-1">Your cart is empty</h3>
              <p className="text-muted-foreground text-sm">Add some products to get started.</p>
            </div>
            <Button asChild className="rounded-full px-8 mt-2 shadow-sm shadow-primary/25">
              <Link to="/shop">
                <ShoppingBag className="mr-2 h-4 w-4" />
                Browse Products
              </Link>
            </Button>
          </div>
        ) : (
          <>
            <div className="rounded-2xl border border-border/60 bg-card shadow-sm overflow-hidden">
              <div className="px-6 py-2">
                {cart.items.map((item) => (
                  <div key={item.productId} className="cart-item-row">
                    <CartItem
                      item={item}
                      onUpdateQuantity={handleUpdateQuantity}
                      onRemove={handleRemove}
                    />
                  </div>
                ))}
              </div>
              <div className="cart-summary px-6 py-4 bg-muted/30 border-t border-border/50">
                <div className="flex justify-between items-center">
                  <div>
                    <p className="text-sm text-muted-foreground">Total ({cart.totalQuantity} items)</p>
                    <p className="text-2xl font-bold text-primary mt-0.5">₹{cart.totalPrice.toFixed(2)}</p>
                  </div>
                </div>
              </div>
            </div>

            <div className="flex gap-3 mt-5">
              <Button
                variant="outline"
                onClick={handleClearCart}
                className="rounded-full gap-2 hover:bg-destructive/5 hover:text-destructive hover:border-destructive/30"
              >
                <Trash2 className="h-4 w-4" />
                Clear Cart
              </Button>
              <Button asChild className="flex-1 rounded-full shadow-sm shadow-primary/30 hover:shadow-primary/50 transition-all">
                <Link to="/checkout">
                  Proceed to Checkout
                  <ArrowRight className="ml-2 h-4 w-4" />
                </Link>
              </Button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
