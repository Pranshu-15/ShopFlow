export interface CartItemType {
  productId: number
  productName: string
  sku: string
  unitPrice: number
  quantity: number
  subtotal: number
  imageUrl?: string
}

export interface Cart {
  items: CartItemType[]
  totalPrice: number
  totalQuantity: number
}

export interface AddItemRequest {
  productId: number
  productName: string
  sku: string
  unitPrice: number
  quantity: number
  imageUrl?: string
}
