import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ShieldAlert, LayoutDashboard, Package, ClipboardList } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useAuth } from '@/hooks/useAuth'
import { AdminProductsTab } from './AdminProductsTab'
import { AdminOrdersTab } from './AdminOrdersTab'

type Tab = 'products' | 'orders'

const tabs: { id: Tab; label: string; icon: React.ElementType }[] = [
  { id: 'products', label: 'Products', icon: Package },
  { id: 'orders',   label: 'Orders',   icon: ClipboardList },
]

export function AdminPage() {
  const { isAdmin, isLoading } = useAuth()
  const navigate               = useNavigate()
  const [tab, setTab]          = useState<Tab>('products')

  if (isLoading) return null

  if (!isAdmin) {
    return (
      <div className="container mx-auto px-4 py-24 max-w-md text-center">
        <div className="h-20 w-20 rounded-2xl bg-destructive/10 flex items-center justify-center mx-auto mb-6">
          <ShieldAlert className="h-10 w-10 text-destructive" />
        </div>
        <h1 className="text-2xl font-bold mb-2">Access Denied</h1>
        <p className="text-muted-foreground mb-8">You don't have permission to view this page.</p>
        <Button onClick={() => navigate('/')} className="rounded-full px-8">Go Home</Button>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <div className="border-b border-border/50 bg-gradient-to-r from-background via-accent/20 to-background">
        <div className="container mx-auto px-4 py-6">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center shadow-sm">
              <LayoutDashboard className="h-5 w-5 text-white" />
            </div>
            <div>
              <h1 className="text-2xl font-bold">Admin Dashboard</h1>
              <p className="text-xs text-muted-foreground mt-0.5">Manage your store</p>
            </div>
          </div>
        </div>
      </div>

      <div className="container mx-auto px-4 py-6">
        {/* Tabs */}
        <div className="flex gap-1 p-1 bg-muted/50 rounded-xl w-fit mb-6 border border-border/40">
          {tabs.map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              onClick={() => setTab(id)}
              className={`flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-lg transition-all ${
                tab === id
                  ? 'bg-background text-primary shadow-sm border border-border/60'
                  : 'text-muted-foreground hover:text-foreground'
              }`}
            >
              <Icon className="h-4 w-4" />
              {label}
            </button>
          ))}
        </div>

        {tab === 'products' && <AdminProductsTab />}
        {tab === 'orders'   && <AdminOrdersTab />}
      </div>
    </div>
  )
}
