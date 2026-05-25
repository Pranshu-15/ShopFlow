import { useState, useEffect, useRef } from 'react'
import { BrowserRouter, Routes, Route, Link, NavLink } from 'react-router-dom'
import { ShoppingCart, User, ShoppingBag, Sun, Moon, LayoutDashboard } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { AuthProvider } from '@/context/AuthContext'
import { useAuth } from '@/hooks/useAuth'
import { HomePage }         from '@/pages/HomePage'
import { ShopPage }         from '@/pages/ShopPage'
import { CartPage }         from '@/pages/CartPage'
import { CheckoutPage }     from '@/pages/CheckoutPage'
import { OrderHistoryPage } from '@/pages/OrderHistoryPage'
import { AccountPage }      from '@/pages/AccountPage'
import { AdminPage }        from '@/pages/admin/AdminPage'
import gsap from 'gsap'

function Navbar() {
  const { isAuthenticated, isAdmin, login, logout, register } = useAuth()
  const navRef = useRef<HTMLElement>(null)
  const [isDark, setIsDark] = useState<boolean>(() =>
    localStorage.getItem('theme') === 'dark' ||
    (!localStorage.getItem('theme') && window.matchMedia('(prefers-color-scheme: dark)').matches),
  )

  useEffect(() => {
    document.documentElement.classList.toggle('dark', isDark)
    localStorage.setItem('theme', isDark ? 'dark' : 'light')
  }, [isDark])

  useEffect(() => {
    gsap.fromTo(navRef.current,
      { y: -70, opacity: 0 },
      { y: 0, opacity: 1, duration: 0.55, ease: 'power3.out' }
    )
  }, [])

  const navLinkClass = ({ isActive }: { isActive: boolean }) =>
    `text-sm font-medium transition-colors hover:text-primary px-1 py-0.5 ${
      isActive
        ? 'text-primary border-b-2 border-primary'
        : 'text-muted-foreground border-b-2 border-transparent'
    }`

  return (
    <header ref={navRef} className="sticky top-0 z-50 w-full border-b border-border/60 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80 shadow-sm">
      <div className="container mx-auto px-4 h-16 flex items-center justify-between gap-4">
        <Link to="/" className="flex items-center gap-2 shrink-0">
          <div className="h-7 w-7 rounded-lg bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center">
            <ShoppingBag className="h-4 w-4 text-white" />
          </div>
          <span className="font-bold text-xl bg-gradient-to-r from-indigo-600 to-violet-600 bg-clip-text text-transparent">
            ShopFlow
          </span>
        </Link>

        <nav className="hidden md:flex items-center gap-6">
          <NavLink to="/shop"   className={navLinkClass}>Shop</NavLink>
          <NavLink to="/orders" className={navLinkClass}>Orders</NavLink>
          {isAdmin && <NavLink to="/admin" className={navLinkClass}>Admin</NavLink>}
        </nav>

        <div className="flex items-center gap-1.5">
          <Button variant="ghost" size="icon" onClick={() => setIsDark((d) => !d)} aria-label="Toggle theme" className="rounded-full">
            {isDark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
          </Button>

          <Button variant="ghost" size="icon" asChild className="rounded-full">
            <Link to="/cart" aria-label="Cart">
              <ShoppingCart className="h-4 w-4" />
            </Link>
          </Button>

          {isAuthenticated ? (
            <>
              {isAdmin && (
                <Button variant="ghost" size="icon" asChild className="rounded-full">
                  <Link to="/admin" aria-label="Admin Dashboard">
                    <LayoutDashboard className="h-4 w-4" />
                  </Link>
                </Button>
              )}
              <Button variant="ghost" size="icon" asChild className="rounded-full">
                <Link to="/account" aria-label="Account">
                  <User className="h-4 w-4" />
                </Link>
              </Button>
              <Button variant="outline" size="sm" onClick={logout} className="ml-1 rounded-full">
                Sign Out
              </Button>
            </>
          ) : (
            <div className="flex items-center gap-2 ml-1">
              <Button variant="ghost" size="sm" onClick={register} className="rounded-full">
                Sign Up
              </Button>
              <Button size="sm" onClick={login} className="rounded-full shadow-sm shadow-primary/30 hover:shadow-primary/50 transition-shadow">
                <ShoppingBag className="mr-1.5 h-3.5 w-3.5" />
                Sign In
              </Button>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Navbar />
        <main>
          <Routes>
            <Route path="/"         element={<HomePage />} />
            <Route path="/shop"     element={<ShopPage />} />
            <Route path="/cart"     element={<CartPage />} />
            <Route path="/checkout" element={<CheckoutPage />} />
            <Route path="/orders"   element={<OrderHistoryPage />} />
            <Route path="/account"  element={<AccountPage />} />
            <Route path="/admin"    element={<AdminPage />} />
          </Routes>
        </main>
      </BrowserRouter>
    </AuthProvider>
  )
}
