import { useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import { ShoppingBag, Package, ShieldCheck, Zap, ArrowRight } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useAuth } from '@/hooks/useAuth'
import gsap from 'gsap'

export function HomePage() {
  const { isAuthenticated, user, login } = useAuth()
  const heroRef    = useRef<HTMLDivElement>(null)
  const featRef    = useRef<HTMLDivElement>(null)
  const ctaRef     = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const ctx = gsap.context(() => {
      const tl = gsap.timeline({ defaults: { ease: 'power3.out' } })
      tl.fromTo('.hero-badge',  { opacity: 0, y: -24 }, { opacity: 1, y: 0, duration: 0.5 })
        .fromTo('.hero-title',  { opacity: 0, y: 56  }, { opacity: 1, y: 0, duration: 0.75 }, '-=0.2')
        .fromTo('.hero-sub',    { opacity: 0, y: 32  }, { opacity: 1, y: 0, duration: 0.6  }, '-=0.4')
        .fromTo('.hero-cta',    { opacity: 0, y: 24  }, { opacity: 1, y: 0, duration: 0.5  }, '-=0.35')
        .fromTo('.hero-stat',   { opacity: 0, y: 20, scale: 0.9 },
          { opacity: 1, y: 0, scale: 1, duration: 0.45, stagger: 0.12 }, '-=0.25')
        .fromTo('.feat-card',   { opacity: 0, y: 50, scale: 0.95 },
          { opacity: 1, y: 0, scale: 1, duration: 0.55, stagger: 0.14 }, '-=0.15')
        .fromTo('.cta-banner',  { opacity: 0, y: 30 },
          { opacity: 1, y: 0, duration: 0.6 }, '-=0.2')
    }, heroRef)

    return () => ctx.revert()
  }, [])

  return (
    <div className="min-h-screen bg-background" ref={heroRef}>
      {/* Hero */}
      <section className="relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-br from-indigo-50 via-white to-violet-50 dark:from-indigo-950/30 dark:via-background dark:to-violet-950/20" />
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top_right,rgba(99,102,241,0.12),transparent_60%)] dark:bg-[radial-gradient(ellipse_at_top_right,rgba(99,102,241,0.08),transparent_60%)]" />

        <div className="relative container mx-auto px-4 py-24 md:py-32 text-center">
          <div className="hero-badge inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-primary/10 text-primary text-sm font-medium mb-8 border border-primary/20">
            <Zap className="h-3.5 w-3.5" />
            Fast delivery · Secure payments · Best prices
          </div>

          <h1 className="hero-title text-5xl md:text-7xl font-bold tracking-tight mb-6 leading-tight">
            Shop smarter with{' '}
            <span className="bg-gradient-to-r from-indigo-600 via-violet-600 to-purple-600 bg-clip-text text-transparent">
              ShopFlow
            </span>
          </h1>

          <p className="hero-sub text-xl text-muted-foreground mb-10 max-w-2xl mx-auto leading-relaxed">
            Discover thousands of products with seamless checkout and lightning-fast delivery right to your doorstep.
          </p>

          <div className="hero-cta flex flex-wrap gap-4 justify-center">
            <Button asChild size="lg" className="h-12 px-8 rounded-full shadow-lg shadow-primary/30 hover:shadow-primary/50 transition-all hover:-translate-y-0.5">
              <Link to="/shop">
                <ShoppingBag className="mr-2 h-5 w-5" />
                Browse Products
                <ArrowRight className="ml-2 h-4 w-4" />
              </Link>
            </Button>
            {!isAuthenticated && (
              <Button variant="outline" size="lg" onClick={login} className="h-12 px-8 rounded-full hover:bg-primary/5 hover:border-primary/40 transition-all">
                Sign In
              </Button>
            )}
          </div>

          {isAuthenticated && user && (
            <p className="hero-cta mt-8 text-sm text-muted-foreground">
              Welcome back,{' '}
              <strong className="text-foreground font-semibold">{user.name || user.email}</strong>! 🎉
            </p>
          )}

          {/* Stats */}
          <div className="mt-16 flex flex-wrap justify-center gap-8 text-center">
            {[
              { label: 'Products', value: '10,000+' },
              { label: 'Happy Customers', value: '50,000+' },
              { label: 'Delivery Cities', value: '500+' },
            ].map(({ label, value }) => (
              <div key={label} className="hero-stat px-6 py-3 rounded-2xl bg-white/70 dark:bg-card/50 border border-border/50 backdrop-blur-sm shadow-sm">
                <p className="text-2xl font-bold text-primary">{value}</p>
                <p className="text-xs text-muted-foreground mt-0.5">{label}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="container mx-auto px-4 py-20" ref={featRef}>
        <div className="text-center mb-12">
          <h2 className="text-3xl font-bold mb-3">Why choose ShopFlow?</h2>
          <p className="text-muted-foreground max-w-lg mx-auto">Everything you need for a smooth shopping experience, built with you in mind.</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {[
            {
              icon: ShoppingBag,
              color: 'indigo',
              title: 'Wide Selection',
              description: 'Browse thousands of products across multiple categories with real-time search and smart filters.',
            },
            {
              icon: Package,
              color: 'violet',
              title: 'Fast Shipping',
              description: 'Track your orders in real-time from placement to your doorstep. Same-day delivery available.',
            },
            {
              icon: ShieldCheck,
              color: 'purple',
              title: 'Secure Checkout',
              description: 'Bank-grade encryption and multiple payment options including COD and online payments.',
            },
          ].map(({ icon: Icon, color, title, description }) => (
            <div
              key={title}
              className="feat-card group p-6 rounded-2xl border border-border/50 bg-card hover:shadow-xl hover:-translate-y-1 transition-all duration-300 cursor-default"
             
            >
              <div className={`h-12 w-12 rounded-xl mb-4 flex items-center justify-center group-hover:scale-110 transition-transform ${
                color === 'indigo' ? 'bg-indigo-100 dark:bg-indigo-950/60' :
                color === 'violet' ? 'bg-violet-100 dark:bg-violet-950/60' :
                'bg-purple-100 dark:bg-purple-950/60'
              }`}>
                <Icon className={`h-6 w-6 ${
                  color === 'indigo' ? 'text-indigo-600 dark:text-indigo-400' :
                  color === 'violet' ? 'text-violet-600 dark:text-violet-400' :
                  'text-purple-600 dark:text-purple-400'
                }`} />
              </div>
              <h3 className="font-bold text-lg mb-2">{title}</h3>
              <p className="text-muted-foreground text-sm leading-relaxed">{description}</p>
            </div>
          ))}
        </div>
      </section>

      {/* CTA Banner */}
      <section className="container mx-auto px-4 pb-20" ref={ctaRef}>
        <div className="cta-banner relative rounded-3xl overflow-hidden bg-gradient-to-br from-indigo-600 via-violet-600 to-purple-700 p-10 text-center text-white shadow-xl shadow-primary/25">
          <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_bottom_left,rgba(255,255,255,0.12),transparent_50%)]" />
          <div className="relative">
            <h2 className="text-3xl font-bold mb-3">Ready to start shopping?</h2>
            <p className="text-indigo-100 mb-8 text-lg">Join thousands of happy customers today.</p>
            <Button asChild size="lg" variant="secondary" className="rounded-full h-12 px-8 hover:-translate-y-0.5 transition-transform">
              <Link to="/shop">
                <ShoppingBag className="mr-2 h-5 w-5" />
                Shop Now
              </Link>
            </Button>
          </div>
        </div>
      </section>
    </div>
  )
}
