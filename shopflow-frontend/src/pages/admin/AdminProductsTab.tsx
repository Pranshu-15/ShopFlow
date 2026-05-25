import { useState, useEffect, useRef } from 'react'
import { Plus, Pencil, Trash2, Loader2, X, Upload, Link, ImageOff } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import apiClient from '@/lib/apiClient'
import type { Product, ProductPage } from '@/types/product'

interface Category { id: number; name: string }

interface ProductFormData {
  name: string
  description: string
  price: string
  stockQuantity: string
  sku: string
  slug: string
  imageUrl: string
  active: boolean
  categoryId: string
}

const empty: ProductFormData = {
  name: '', description: '', price: '', stockQuantity: '',
  sku: '', slug: '', imageUrl: '', active: true, categoryId: '',
}

function toSlug(name: string) {
  return name.toLowerCase().replace(/\s+/g, '-').replace(/[^a-z0-9-]/g, '')
}

function extractError(err: unknown): string {
  const data = (err as { response?: { data?: Record<string, unknown> } })?.response?.data
  if (!data) return 'Failed to save product.'
  if (data.errors && typeof data.errors === 'object') {
    const msgs = Object.values(data.errors as Record<string, string>)
    if (msgs.length) return msgs.join(' • ')
  }
  if (typeof data.error === 'string' && data.error) return data.error
  if (typeof data.message === 'string' && data.message) return data.message
  return 'Failed to save product.'
}

async function resizeToDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onerror = reject
    reader.onload = (e) => {
      const img = new Image()
      img.onerror = reject
      img.onload = () => {
        const MAX = 800
        let { width, height } = img
        if (width > MAX || height > MAX) {
          if (width > height) { height = Math.round(height * MAX / width); width = MAX }
          else { width = Math.round(width * MAX / height); height = MAX }
        }
        const canvas = document.createElement('canvas')
        canvas.width = width; canvas.height = height
        canvas.getContext('2d')?.drawImage(img, 0, 0, width, height)
        resolve(canvas.toDataURL('image/jpeg', 0.78))
      }
      img.src = e.target?.result as string
    }
    reader.readAsDataURL(file)
  })
}

interface ProductModalProps {
  product: Product | null
  categories: Category[]
  onClose: () => void
  onSaved: () => void
}

function ProductModal({ product, categories, onClose, onSaved }: ProductModalProps) {
  const [form, setForm]           = useState<ProductFormData>(
    product
      ? {
          name: product.name, description: product.description ?? '',
          price: String(product.price), stockQuantity: String(product.stockQuantity),
          sku: product.sku, slug: product.slug, imageUrl: product.imageUrl ?? '',
          active: product.active, categoryId: product.categoryId ? String(product.categoryId) : '',
        }
      : empty,
  )
  const [imageMode, setImageMode] = useState<'url' | 'upload'>('url')
  const [saving, setSaving]       = useState(false)
  const [uploading, setUploading] = useState(false)
  const [error, setError]         = useState<string | null>(null)
  const fileRef                   = useRef<HTMLInputElement>(null)

  const set = (k: keyof ProductFormData, v: string | boolean) =>
    setForm((f) => ({ ...f, [k]: v }))

  const handleFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true)
    try {
      const dataUrl = await resizeToDataUrl(file)
      set('imageUrl', dataUrl)
      setImageMode('url')
    } catch {
      setError('Failed to process image.')
    } finally {
      setUploading(false)
      if (fileRef.current) fileRef.current.value = ''
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setError(null)
    const body = {
      name:          form.name,
      description:   form.description,
      price:         parseFloat(form.price),
      stockQuantity: parseInt(form.stockQuantity, 10),
      sku:           form.sku,
      slug:          form.slug,
      imageUrl:      form.imageUrl.trim() || null,
      active:        form.active,
      categoryId:    form.categoryId ? parseInt(form.categoryId, 10) : null,
    }
    try {
      if (product) {
        await apiClient.put(`/api/v1/catalog/products/${product.id}`, body)
      } else {
        await apiClient.post('/api/v1/catalog/products', body)
      }
      onSaved()
    } catch (err) {
      setError(extractError(err))
    } finally {
      setSaving(false)
    }
  }

  const isDataUrl = form.imageUrl.startsWith('data:')

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
      <div className="bg-background border border-border/60 rounded-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto shadow-2xl">
        {/* Modal header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-border/50">
          <h2 className="text-lg font-bold">{product ? 'Edit Product' : 'Add Product'}</h2>
          <button
            onClick={onClose}
            className="h-8 w-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-colors"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div className="col-span-2">
              <label className="block text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-1.5">Name *</label>
              <Input
                value={form.name}
                onChange={(e) => {
                  set('name', e.target.value)
                  if (!product) set('slug', toSlug(e.target.value))
                }}
                className="rounded-xl border-border/70"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-1.5">SKU *</label>
              <Input value={form.sku} onChange={(e) => set('sku', e.target.value)} className="rounded-xl border-border/70" required />
            </div>
            <div>
              <label className="block text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-1.5">Slug *</label>
              <Input value={form.slug} onChange={(e) => set('slug', e.target.value)} className="rounded-xl border-border/70" required />
            </div>

            <div>
              <label className="block text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-1.5">Price (₹) *</label>
              <Input type="number" min="0.01" step="0.01" value={form.price}
                onChange={(e) => set('price', e.target.value)} className="rounded-xl border-border/70" required />
            </div>
            <div>
              <label className="block text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-1.5">Stock *</label>
              <Input type="number" min="0" value={form.stockQuantity}
                onChange={(e) => set('stockQuantity', e.target.value)} className="rounded-xl border-border/70" required />
            </div>

            <div className="col-span-2">
              <label className="block text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-1.5">Description</label>
              <textarea
                className="w-full min-h-[70px] rounded-xl border border-border/70 bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring resize-none"
                value={form.description}
                onChange={(e) => set('description', e.target.value)}
              />
            </div>

            {/* Image section */}
            <div className="col-span-2">
              <label className="block text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-2">Product Image</label>

              {form.imageUrl && (
                <div className="relative mb-3 h-36 w-full rounded-xl overflow-hidden bg-muted border border-border/50">
                  <img
                    src={form.imageUrl}
                    alt="preview"
                    className="h-full w-full object-cover"
                    onError={(e) => {
                      (e.target as HTMLImageElement).style.display = 'none'
                    }}
                  />
                  <button
                    type="button"
                    onClick={() => set('imageUrl', '')}
                    className="absolute top-2 right-2 rounded-full bg-black/60 p-1 text-white hover:bg-black/80 transition-colors"
                  >
                    <X className="h-3.5 w-3.5" />
                  </button>
                  {isDataUrl && (
                    <span className="absolute bottom-2 left-2 rounded-full bg-black/60 px-2.5 py-0.5 text-xs text-white">
                      Uploaded
                    </span>
                  )}
                </div>
              )}

              <div className="flex gap-1.5 mb-2">
                {(['url', 'upload'] as const).map((m) => (
                  <button
                    key={m}
                    type="button"
                    onClick={() => setImageMode(m)}
                    className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium transition-all border ${
                      imageMode === m
                        ? 'bg-primary text-primary-foreground border-primary shadow-sm'
                        : 'border-border/60 text-muted-foreground hover:text-foreground bg-background'
                    }`}
                  >
                    {m === 'url' ? <Link className="h-3 w-3" /> : <Upload className="h-3 w-3" />}
                    {m === 'url' ? 'Image URL' : 'Upload File'}
                  </button>
                ))}
              </div>

              {imageMode === 'url' ? (
                <Input
                  value={isDataUrl ? '' : form.imageUrl}
                  onChange={(e) => set('imageUrl', e.target.value)}
                  placeholder="https://example.com/image.jpg"
                  disabled={isDataUrl}
                  className="rounded-xl border-border/70"
                />
              ) : (
                <div>
                  <input
                    ref={fileRef}
                    type="file"
                    accept="image/*"
                    className="hidden"
                    onChange={handleFile}
                  />
                  <Button
                    type="button"
                    variant="outline"
                    className="w-full rounded-xl border-dashed"
                    onClick={() => fileRef.current?.click()}
                    disabled={uploading}
                  >
                    {uploading
                      ? <><Loader2 className="h-4 w-4 mr-2 animate-spin" /> Processing…</>
                      : <><Upload className="h-4 w-4 mr-2" /> Choose Image from Device</>
                    }
                  </Button>
                  <p className="text-xs text-muted-foreground mt-1.5">
                    Resized to max 800px. JPG, PNG, WebP supported.
                  </p>
                </div>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-1.5">Category</label>
              <select
                className="w-full rounded-xl border border-border/70 bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                value={form.categoryId}
                onChange={(e) => set('categoryId', e.target.value)}
              >
                <option value="">None</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
            </div>
            <div className="flex items-center gap-2 pt-5">
              <input
                type="checkbox"
                id="active"
                checked={form.active}
                onChange={(e) => set('active', e.target.checked)}
                className="h-4 w-4 rounded border"
              />
              <label htmlFor="active" className="text-sm font-medium">Active</label>
            </div>
          </div>

          {error && (
            <p className="text-sm text-destructive bg-destructive/5 px-3 py-2 rounded-lg">{error}</p>
          )}

          <div className="flex gap-2 pt-1">
            <Button type="button" variant="outline" className="flex-1 rounded-full" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" className="flex-1 rounded-full shadow-sm shadow-primary/25" disabled={saving || uploading}>
              {saving
                ? <Loader2 className="h-4 w-4 animate-spin" />
                : product ? 'Save Changes' : 'Add Product'
              }
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}

export function AdminProductsTab() {
  const [products, setProducts]     = useState<Product[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [isLoading, setIsLoading]   = useState(true)
  const [editTarget, setEditTarget] = useState<Product | null | 'new'>(null)
  const [deleting, setDeleting]     = useState<number | null>(null)

  const load = async () => {
    setIsLoading(true)
    try {
      const [prod, cat] = await Promise.all([
        apiClient.get<ProductPage>('/api/v1/catalog/products/admin/all?size=100'),
        apiClient.get<Category[]>('/api/v1/catalog/categories'),
      ])
      setProducts(prod.data.content)
      setCategories(cat.data)
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const handleDelete = async (id: number) => {
    if (!confirm('Delete this product? This cannot be undone.')) return
    setDeleting(id)
    try {
      await apiClient.delete(`/api/v1/catalog/products/${id}`)
      setProducts((p) => p.filter((x) => x.id !== id))
    } catch {
      alert('Failed to delete product.')
    } finally {
      setDeleting(null)
    }
  }

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center py-20 gap-3">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
        <p className="text-sm text-muted-foreground">Loading products...</p>
      </div>
    )
  }

  return (
    <>
      <div className="flex justify-between items-center mb-4">
        <p className="text-sm text-muted-foreground">{products.length} products</p>
        <Button size="sm" onClick={() => setEditTarget('new')} className="rounded-full shadow-sm shadow-primary/20 gap-1.5">
          <Plus className="h-4 w-4" /> Add Product
        </Button>
      </div>

      <div className="overflow-x-auto rounded-xl border border-border/60 shadow-sm">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border/60 bg-muted/40">
              <th className="text-left px-4 py-3 font-semibold text-xs uppercase tracking-wide text-muted-foreground">Product</th>
              <th className="text-left px-4 py-3 font-semibold text-xs uppercase tracking-wide text-muted-foreground">SKU</th>
              <th className="text-right px-4 py-3 font-semibold text-xs uppercase tracking-wide text-muted-foreground">Price</th>
              <th className="text-right px-4 py-3 font-semibold text-xs uppercase tracking-wide text-muted-foreground">Stock</th>
              <th className="text-left px-4 py-3 font-semibold text-xs uppercase tracking-wide text-muted-foreground">Category</th>
              <th className="text-center px-4 py-3 font-semibold text-xs uppercase tracking-wide text-muted-foreground">Status</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody className="divide-y divide-border/40">
            {products.map((p) => (
              <tr key={p.id} className="hover:bg-muted/20 transition-colors">
                <td className="px-4 py-3">
                  <div className="flex items-center gap-3">
                    <div className="h-9 w-9 rounded-lg overflow-hidden bg-muted border border-border/40 shrink-0">
                      {p.imageUrl ? (
                        <img src={p.imageUrl} alt={p.name} className="h-full w-full object-cover" />
                      ) : (
                        <div className="h-full w-full flex items-center justify-center">
                          <ImageOff className="h-4 w-4 text-muted-foreground/30" />
                        </div>
                      )}
                    </div>
                    <span className="font-medium max-w-[160px] truncate">{p.name}</span>
                  </div>
                </td>
                <td className="px-4 py-3 text-muted-foreground font-mono text-xs">{p.sku}</td>
                <td className="px-4 py-3 text-right font-bold text-primary">₹{Number(p.price).toFixed(2)}</td>
                <td className="px-4 py-3 text-right text-muted-foreground">{p.stockQuantity}</td>
                <td className="px-4 py-3 text-muted-foreground text-xs">{p.categoryName ?? '—'}</td>
                <td className="px-4 py-3 text-center">
                  <Badge variant={p.active ? 'default' : 'secondary'} className="rounded-full text-xs">
                    {p.active ? 'Active' : 'Inactive'}
                  </Badge>
                </td>
                <td className="px-4 py-3">
                  <div className="flex gap-1 justify-end">
                    <Button
                      variant="ghost" size="icon"
                      className="h-8 w-8 rounded-lg hover:bg-primary/10 hover:text-primary"
                      onClick={() => setEditTarget(p)}
                    >
                      <Pencil className="h-3.5 w-3.5" />
                    </Button>
                    <Button
                      variant="ghost" size="icon"
                      className="h-8 w-8 rounded-lg text-muted-foreground hover:text-destructive hover:bg-destructive/10"
                      onClick={() => handleDelete(p.id)}
                      disabled={deleting === p.id}
                    >
                      {deleting === p.id
                        ? <Loader2 className="h-3.5 w-3.5 animate-spin" />
                        : <Trash2 className="h-3.5 w-3.5" />}
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {editTarget !== null && (
        <ProductModal
          product={editTarget === 'new' ? null : editTarget}
          categories={categories}
          onClose={() => setEditTarget(null)}
          onSaved={() => { setEditTarget(null); load() }}
        />
      )}
    </>
  )
}
