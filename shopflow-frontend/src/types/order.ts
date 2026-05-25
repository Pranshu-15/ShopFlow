export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'CANCELLED' | 'FAILED'

export interface OrderItemType {
  id: number
  productId: string
  productName: string
  unitPrice: number
  quantity: number
}

export interface Order {
  id: number
  orderId: string
  userId: string
  totalAmount: number
  currency: string
  status: OrderStatus
  items: OrderItemType[]
  createdAt: string
  updatedAt: string
}

export interface CreateOrderRequest {
  currency: string
  items: { productId: string; productName: string; unitPrice: number; quantity: number }[]
}
