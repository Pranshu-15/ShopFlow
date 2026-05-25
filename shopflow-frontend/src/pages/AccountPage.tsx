import { useEffect, useRef } from 'react'
import { User, Mail, LogOut, ShieldCheck, Lock } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { useAuth } from '@/hooks/useAuth'
import gsap from 'gsap'

export function AccountPage() {
  const { isAuthenticated, user, login, logout } = useAuth()
  const cardRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (cardRef.current) {
      gsap.fromTo(cardRef.current,
        { opacity: 0, y: 40, scale: 0.97 },
        { opacity: 1, y: 0, scale: 1, duration: 0.55, ease: 'power3.out' }
      )
      gsap.fromTo(
        cardRef.current.querySelectorAll('.info-row'),
        { opacity: 0, x: -20 },
        { opacity: 1, x: 0, duration: 0.4, stagger: 0.1, delay: 0.3, ease: 'power2.out' }
      )
    }
  }, [isAuthenticated])

  if (!isAuthenticated) {
    return (
      <div className="container mx-auto px-4 py-24 max-w-md text-center">
        <div className="h-20 w-20 rounded-2xl bg-primary/10 flex items-center justify-center mx-auto mb-6">
          <Lock className="h-10 w-10 text-primary" />
        </div>
        <h2 className="text-2xl font-bold mb-2">Sign In Required</h2>
        <p className="text-muted-foreground mb-8">Please sign in to view your account details.</p>
        <Button onClick={login} className="rounded-full px-8 shadow-sm shadow-primary/30">
          Sign In with Keycloak
        </Button>
      </div>
    )
  }

  const initials = (user?.name ?? user?.email ?? '?')[0].toUpperCase()

  return (
    <div className="min-h-screen bg-background">
      <div className="border-b border-border/50">
        <div className="container mx-auto px-4 py-6">
          <h1 className="text-3xl font-bold">My Account</h1>
        </div>
      </div>

      <div className="container mx-auto px-4 py-8 max-w-lg">
        <div ref={cardRef} className="rounded-2xl border border-border/60 bg-card shadow-sm overflow-hidden">
          {/* Gradient banner */}
          <div className="h-24 bg-gradient-to-br from-indigo-500 via-violet-500 to-purple-600" />

          <div className="px-6 pb-6">
            <div className="-mt-10 mb-4 flex items-end gap-4">
              <div className="h-20 w-20 rounded-2xl bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center text-white font-bold text-3xl shadow-lg border-4 border-card">
                {initials}
              </div>
              <div className="pb-1">
                <h2 className="text-xl font-bold">{user?.name || 'User'}</h2>
                <Badge variant="secondary" className="mt-1 gap-1 rounded-full text-xs">
                  <ShieldCheck className="h-3 w-3 text-green-500" />
                  Verified Account
                </Badge>
              </div>
            </div>

            <div className="space-y-3 mt-5">
              <div className="info-row flex items-center gap-3 p-3 rounded-xl bg-muted/40 border border-border/40">
                <div className="h-8 w-8 rounded-lg bg-background flex items-center justify-center shrink-0 shadow-sm">
                  <Mail className="h-4 w-4 text-primary" />
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">Email address</p>
                  <p className="text-sm font-medium">{user?.email || 'No email on file'}</p>
                </div>
              </div>

              <div className="info-row flex items-center gap-3 p-3 rounded-xl bg-muted/40 border border-border/40">
                <div className="h-8 w-8 rounded-lg bg-background flex items-center justify-center shrink-0 shadow-sm">
                  <User className="h-4 w-4 text-primary" />
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">Full name</p>
                  <p className="text-sm font-medium">{user?.name || 'No name on file'}</p>
                </div>
              </div>
            </div>

            <div className="mt-6 pt-5 border-t border-border/40">
              <Button
                variant="destructive"
                onClick={logout}
                className="w-full rounded-full shadow-sm"
              >
                <LogOut className="mr-2 h-4 w-4" />
                Sign Out
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
