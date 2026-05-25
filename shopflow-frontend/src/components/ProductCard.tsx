import { ShoppingCart, Plus, Minus, ImageOff } from 'lucide-react'
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import type { Product } from '@/types/product'

interface ProductCardProps {
  product: Product
  cartQuantity: number
  onAddToCart: (product: Product) => void
  onIncrement: (product: Product) => void
  onDecrement: (product: Product) => void
}

export function ProductCard({ product, cartQuantity, onAddToCart, onIncrement, onDecrement }: ProductCardProps) {
  const inCart = cartQuantity > 0

  return (
    <Card className="flex flex-col h-full group hover:shadow-xl hover:-translate-y-1 transition-all duration-300 overflow-hidden border-border/60">
      <div className="aspect-square overflow-hidden relative bg-muted">
        {product.imageUrl ? (
          <img
            src={product.imageUrl}
            alt={product.name}
            className="h-full w-full object-cover group-hover:scale-105 transition-transform duration-500"
          />
        ) : (
          <div className="h-full w-full bg-gradient-to-br from-secondary to-muted flex items-center justify-center">
            <ImageOff className="h-10 w-10 text-muted-foreground/30" />
          </div>
        )}
        {product.stockQuantity === 0 && (
          <div className="absolute inset-0 bg-background/70 flex items-center justify-center">
            <span className="text-sm font-semibold text-destructive bg-background px-3 py-1 rounded-full border border-destructive/20">
              Out of stock
            </span>
          </div>
        )}
        {inCart && (
          <div className="absolute top-2 right-2 h-6 w-6 rounded-full bg-primary flex items-center justify-center shadow-md">
            <span className="text-[10px] font-bold text-primary-foreground">{cartQuantity}</span>
          </div>
        )}
      </div>

      <CardHeader className="pb-1 pt-3">
        {product.categoryName && (
          <Badge variant="secondary" className="w-fit text-xs mb-1.5 rounded-full">
            {product.categoryName}
          </Badge>
        )}
        <CardTitle className="text-sm font-semibold line-clamp-2 leading-snug group-hover:text-primary transition-colors">
          {product.name}
        </CardTitle>
      </CardHeader>

      <CardContent className="flex-1 pb-2 pt-0">
        <p className="text-xs text-muted-foreground line-clamp-2 leading-relaxed">{product.description}</p>
        <div className="mt-3 flex items-center justify-between">
          <p className="text-lg font-bold text-primary">
            ₹{typeof product.price === 'number' ? product.price.toFixed(2) : product.price}
          </p>
          {product.stockQuantity > 0 && (
            <span className="text-xs text-emerald-600 dark:text-emerald-400 font-medium bg-emerald-50 dark:bg-emerald-950/40 px-2 py-0.5 rounded-full">
              {product.stockQuantity} left
            </span>
          )}
        </div>
      </CardContent>

      <CardFooter className="pt-2">
        {cartQuantity === 0 ? (
          <Button
            className="w-full rounded-full shadow-sm hover:shadow-md hover:shadow-primary/20 transition-all"
            onClick={() => onAddToCart(product)}
            disabled={product.stockQuantity === 0}
          >
            <ShoppingCart className="mr-2 h-4 w-4" />
            Add to Cart
          </Button>
        ) : (
          <div className="flex items-center w-full gap-2">
            <Button
              variant="outline"
              size="icon"
              onClick={() => onDecrement(product)}
              className="rounded-full h-9 w-9 border-primary/30 hover:border-primary/60 shrink-0"
            >
              <Minus className="h-3.5 w-3.5" />
            </Button>
            <div className="flex-1 text-center py-1.5 rounded-full bg-primary/8 border border-primary/20">
              <span className="text-sm font-bold text-primary">{cartQuantity}</span>
              <span className="text-xs text-muted-foreground"> in cart</span>
            </div>
            <Button
              variant="outline"
              size="icon"
              onClick={() => onIncrement(product)}
              className="rounded-full h-9 w-9 border-primary/30 hover:border-primary/60 shrink-0"
            >
              <Plus className="h-3.5 w-3.5" />
            </Button>
          </div>
        )}
      </CardFooter>
    </Card>
  )
}
