import { Minus, Plus, Trash2, ImageOff } from 'lucide-react'
import { Button } from '@/components/ui/button'
import type { CartItemType } from '@/types/cart'

interface CartItemProps {
  item: CartItemType
  onUpdateQuantity: (productId: number, quantity: number) => void
  onRemove: (productId: number) => void
}

export function CartItem({ item, onUpdateQuantity, onRemove }: CartItemProps) {
  return (
    <div className="flex items-center gap-4 py-4 border-b border-border/50 last:border-0 group">
      {/* Product thumbnail */}
      <div className="h-16 w-16 rounded-xl overflow-hidden bg-muted shrink-0 border border-border/50">
        {item.imageUrl ? (
          <img
            src={item.imageUrl}
            alt={item.productName}
            className="h-full w-full object-cover"
          />
        ) : (
          <div className="h-full w-full flex items-center justify-center bg-gradient-to-br from-secondary to-muted">
            <ImageOff className="h-5 w-5 text-muted-foreground/30" />
          </div>
        )}
      </div>

      {/* Product info */}
      <div className="flex-1 min-w-0">
        <p className="font-semibold text-sm leading-tight truncate">{item.productName}</p>
        <p className="text-xs text-muted-foreground mt-0.5">
          ₹{item.unitPrice.toFixed(2)} each
        </p>
      </div>

      {/* Quantity controls */}
      <div className="flex items-center gap-1.5 shrink-0">
        <Button
          variant="outline"
          size="icon"
          className="h-8 w-8 rounded-full border-border/70 hover:border-primary/50"
          onClick={() => onUpdateQuantity(item.productId, item.quantity - 1)}
          aria-label="Decrease quantity"
        >
          <Minus className="h-3 w-3" />
        </Button>
        <span className="w-8 text-center text-sm font-bold text-primary">{item.quantity}</span>
        <Button
          variant="outline"
          size="icon"
          className="h-8 w-8 rounded-full border-border/70 hover:border-primary/50"
          onClick={() => onUpdateQuantity(item.productId, item.quantity + 1)}
          aria-label="Increase quantity"
        >
          <Plus className="h-3 w-3" />
        </Button>
      </div>

      {/* Subtotal */}
      <p className="w-20 text-right font-bold text-sm text-primary shrink-0">
        ₹{item.subtotal.toFixed(2)}
      </p>

      {/* Remove */}
      <Button
        variant="ghost"
        size="icon"
        className="h-8 w-8 rounded-full text-muted-foreground hover:text-destructive hover:bg-destructive/10 opacity-0 group-hover:opacity-100 transition-all shrink-0"
        onClick={() => onRemove(item.productId)}
        aria-label="Remove item"
      >
        <Trash2 className="h-3.5 w-3.5" />
      </Button>
    </div>
  )
}
