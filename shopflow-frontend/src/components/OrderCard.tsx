import { Package, Clock, CheckCircle2, Truck, XCircle, AlertCircle } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import type { Order, OrderStatus } from '@/types/order'

interface OrderCardProps {
  order: Order
}

const statusConfig: Record<OrderStatus, {
  label: string
  badge: 'default' | 'secondary' | 'success' | 'warning' | 'destructive' | 'outline'
  icon: React.ElementType
  color: string
}> = {
  PENDING:   { label: 'Pending',   badge: 'warning',     icon: Clock,         color: 'text-yellow-500' },
  CONFIRMED: { label: 'Confirmed', badge: 'success',     icon: CheckCircle2,  color: 'text-green-500'  },
  SHIPPED:   { label: 'Shipped',   badge: 'default',     icon: Truck,         color: 'text-primary'    },
  CANCELLED: { label: 'Cancelled', badge: 'secondary',   icon: XCircle,       color: 'text-muted-foreground' },
  FAILED:    { label: 'Failed',    badge: 'destructive', icon: AlertCircle,   color: 'text-destructive' },
}

export function OrderCard({ order }: OrderCardProps) {
  const createdDate = new Date(order.createdAt).toLocaleDateString('en-IN', {
    year: 'numeric', month: 'short', day: 'numeric',
  })
  const cfg = statusConfig[order.status]
  const StatusIcon = cfg.icon

  return (
    <div className="rounded-2xl border border-border/60 bg-card shadow-sm hover:shadow-md transition-shadow overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between px-5 py-4 bg-muted/20 border-b border-border/40">
        <div className="flex items-center gap-3">
          <div className="h-9 w-9 rounded-xl bg-primary/10 flex items-center justify-center">
            <Package className="h-4.5 w-4.5 text-primary" />
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Order ID</p>
            <p className="text-sm font-mono font-semibold tracking-wider">
              #{order.orderId.slice(0, 8).toUpperCase()}
            </p>
          </div>
        </div>
        <div className="flex flex-col items-end gap-1.5">
          <Badge variant={cfg.badge} className="gap-1 text-xs">
            <StatusIcon className={`h-3 w-3 ${cfg.color}`} />
            {cfg.label}
          </Badge>
          <p className="text-xs text-muted-foreground">{createdDate}</p>
        </div>
      </div>

      {/* Items */}
      <div className="px-5 py-4">
        <ul className="space-y-2">
          {order.items.map((item) => (
            <li key={item.id} className="flex justify-between items-center text-sm">
              <div className="flex items-center gap-2">
                <span className="h-1.5 w-1.5 rounded-full bg-muted-foreground/40 shrink-0" />
                <span className="text-muted-foreground">
                  {item.productName}
                </span>
                <span className="text-xs text-muted-foreground/60">× {item.quantity}</span>
              </div>
              <span className="font-medium text-xs">₹{(item.unitPrice * item.quantity).toFixed(2)}</span>
            </li>
          ))}
        </ul>

        <div className="flex justify-between items-center mt-4 pt-3 border-t border-border/40">
          <span className="text-sm text-muted-foreground">Total</span>
          <span className="font-bold text-primary text-lg">₹{order.totalAmount.toFixed(2)}</span>
        </div>
      </div>
    </div>
  )
}
