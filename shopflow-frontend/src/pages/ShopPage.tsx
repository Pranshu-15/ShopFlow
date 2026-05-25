import { useState, useEffect, useRef } from 'react'
import { Search, Loader2, AlertCircle, RotateCcw } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { ProductCard } from '@/components/ProductCard'
import apiClient from '@/lib/apiClient'
import type { Product, ProductPage } from '@/types/product'
import type { AddItemRequest, Cart } from '@/types/cart'
import { useAuth } from '@/hooks/useAuth'
import gsap from 'gsap'

export function ShopPage() {
  const { isLoading: authLoading, isAuthenticated, login } = useAuth()
  const [allProducts, setAllProducts] = useState<Product[]>([])
  const [query, setQuery]             = useState('')
  const [page, setPage]               = useState(0)
  const [totalPages, setTotalPages]   = useState(0)
  const [isLoading, setIsLoading]     = useState(true)
  const [error, setError]             = useState<string | null>(null)
  const [retryKey, setRetryKey]       = useState(0)
  const [cartItems, setCartItems]     = useState<Record<number, number>>({})
  const headerRef = useRef<HTMLDivElement>(null)
  const gridRef   = useRef<HTMLDivElement>(null)

  useEffect(() => {
    gsap.fromTo(headerRef.current,
      { opacity: 0, y: -20 },
      { opacity: 1, y: 0, duration: 0.5, ease: 'power2.out' }
    )
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    const fetchProducts = async (attempt: number) => {
      setIsLoading(true)
      setError(null)
      try {
        const params = new URLSearchParams({ page: String(page), size: '12' })
        const { data } = await apiClient.get<ProductPage>(
          `/api/v1/catalog/products?${params}`,
          { signal: controller.signal },
        )
        if (!controller.signal.aborted) {
          setAllProducts(data.content)
          setTotalPages(data.totalPages)
          setIsLoading(false)
        }
      } catch (err: unknown) {
        if (controller.signal.aborted) return
        if (attempt === 0) {
          setTimeout(() => { if (!controller.signal.aborted) fetchProducts(1) }, 600)
        } else {
          console.error('Products fetch error:', err)
          setError('Failed to load products. Please try again.')
          setIsLoading(false)
        }
      }
    }
    fetchProducts(0)
    return () => controller.abort()
  }, [page, retryKey])

  // Animate product cards in after loading
  useEffect(() => {
    if (!isLoading && allProducts.length > 0 && gridRef.current) {
      const cards = gridRef.current.querySelectorAll('.product-card')
      gsap.set(cards, { opacity: 0, y: 36, scale: 0.94 })
      gsap.to(cards, { opacity: 1, y: 0, scale: 1, duration: 0.45, stagger: 0.06, ease: 'power2.out' })
    }
  }, [isLoading, allProducts, query])

  useEffect(() => {
    if (authLoading || !isAuthenticated) return
    apiClient.get<Cart>('/api/v1/cart')
      .then(({ data }) => {
        const map: Record<number, number> = {}
        data.items.forEach((item) => { map[item.productId] = item.quantity })
        setCartItems(map)
      })
      .catch(() => {})
  }, [authLoading, isAuthenticated])

  const products = query.trim()
    ? allProducts.filter(
        (p) =>
          p.name.toLowerCase().includes(query.toLowerCase()) ||
          p.description.toLowerCase().includes(query.toLowerCase()),
      )
    : allProducts

  const handleAddToCart = async (product: Product) => {
    if (!isAuthenticated) { login(); return }
    const item: AddItemRequest = {
      productId:   product.id,
      productName: product.name,
      sku:         product.sku,
      unitPrice:   product.price,
      quantity:    1,
      imageUrl:    product.imageUrl,
    }
    try {
      await apiClient.post('/api/v1/cart/items', item)
      setCartItems((prev) => ({ ...prev, [product.id]: 1 }))
    } catch {
      alert('Failed to add item to cart.')
    }
  }

  const handleIncrement = async (product: Product) => {
    const newQty = (cartItems[product.id] ?? 0) + 1
    try {
      await apiClient.put(`/api/v1/cart/items/${product.id}`, { quantity: newQty })
      setCartItems((prev) => ({ ...prev, [product.id]: newQty }))
    } catch {
      alert('Failed to update cart.')
    }
  }

  const handleDecrement = async (product: Product) => {
    const current = cartItems[product.id] ?? 0
    if (current <= 0) return
    if (current === 1) {
      try {
        await apiClient.delete(`/api/v1/cart/items/${product.id}`)
        setCartItems((prev) => { const next = { ...prev }; delete next[product.id]; return next })
      } catch {
        alert('Failed to update cart.')
      }
    } else {
      const newQty = current - 1
      try {
        await apiClient.put(`/api/v1/cart/items/${product.id}`, { quantity: newQty })
        setCartItems((prev) => ({ ...prev, [product.id]: newQty }))
      } catch {
        alert('Failed to update cart.')
      }
    }
  }

  return (
    <div className="min-h-screen bg-background">
      {/* Page header */}
      <div ref={headerRef} className="border-b border-border/50 bg-gradient-to-r from-background via-accent/30 to-background" style={{ opacity: 0 }}>
        <div className="container mx-auto px-4 py-8">
          <h1 className="text-4xl font-bold mb-1">Shop</h1>
          <p className="text-muted-foreground text-sm">
            {allProducts.length > 0 ? `${allProducts.length} products available` : 'Discover our collection'}
          </p>
          <div className="mt-5 flex gap-2 max-w-lg">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search products..."
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                className="pl-9 rounded-full border-border/70 focus:border-primary/60 bg-background shadow-sm"
              />
            </div>
          </div>
        </div>
      </div>

      <div className="container mx-auto px-4 py-8">
        {isLoading && (
          <div className="flex flex-col items-center justify-center py-32 gap-3">
            <Loader2 className="h-10 w-10 animate-spin text-primary" />
            <p className="text-sm text-muted-foreground">Loading products...</p>
          </div>
        )}

        {error && !isLoading && (
          <div className="flex flex-col items-center justify-center py-32 gap-4">
            <div className="h-16 w-16 rounded-2xl bg-destructive/10 flex items-center justify-center">
              <AlertCircle className="h-8 w-8 text-destructive" />
            </div>
            <p className="text-muted-foreground">{error}</p>
            <Button variant="outline" onClick={() => setRetryKey((k) => k + 1)} className="rounded-full gap-2">
              <RotateCcw className="h-4 w-4" />
              Try again
            </Button>
          </div>
        )}

        {!isLoading && !error && (
          <>
            {products.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-32 gap-3">
                <div className="h-16 w-16 rounded-2xl bg-muted flex items-center justify-center">
                  <Search className="h-8 w-8 text-muted-foreground/40" />
                </div>
                <p className="text-muted-foreground">No products found for "{query}"</p>
                <Button variant="ghost" onClick={() => setQuery('')} className="rounded-full text-sm">
                  Clear search
                </Button>
              </div>
            ) : (
              <div ref={gridRef} className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-5">
                {products.map((product) => (
                  <div key={product.id} className="product-card">
                    <ProductCard
                      product={product}
                      cartQuantity={cartItems[product.id] ?? 0}
                      onAddToCart={handleAddToCart}
                      onIncrement={handleIncrement}
                      onDecrement={handleDecrement}
                    />
                  </div>
                ))}
              </div>
            )}

            {totalPages > 1 && (
              <div className="flex justify-center items-center gap-3 mt-12">
                <Button
                  variant="outline"
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="rounded-full px-6"
                >
                  ← Previous
                </Button>
                <span className="px-4 py-2 text-sm text-muted-foreground bg-muted rounded-full">
                  Page {page + 1} of {totalPages}
                </span>
                <Button
                  variant="outline"
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page === totalPages - 1}
                  className="rounded-full px-6"
                >
                  Next →
                </Button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
