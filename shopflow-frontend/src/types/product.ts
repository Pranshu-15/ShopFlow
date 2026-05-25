export interface Product {
  id: number
  name: string
  description: string
  price: number
  stockQuantity: number
  sku: string
  slug: string
  imageUrl?: string
  categoryId?: number
  categoryName?: string
  active: boolean
}

export interface ProductSearchParams {
  query?: string
  categoryId?: number
  minPrice?: number
  maxPrice?: number
  page?: number
  size?: number
}

export interface ProductPage {
  content: Product[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
