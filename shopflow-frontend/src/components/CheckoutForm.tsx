import { useState } from 'react'
import { CreditCard, Truck, Loader2, ArrowLeft, Lock, ShieldCheck } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import type { Cart } from '@/types/cart'

type PaymentMethod = 'cod' | 'online'
type Step = 'summary' | 'payment'

interface CheckoutFormProps {
  cart: Cart
  onSubmit: () => Promise<void>
}

function formatCardNumber(value: string) {
  return value.replace(/\D/g, '').slice(0, 16).replace(/(.{4})/g, '$1 ').trim()
}

function formatExpiry(value: string) {
  const digits = value.replace(/\D/g, '').slice(0, 4)
  return digits.length > 2 ? `${digits.slice(0, 2)}/${digits.slice(2)}` : digits
}

export function CheckoutForm({ cart, onSubmit }: CheckoutFormProps) {
  const [step, setStep]                 = useState<Step>('summary')
  const [method, setMethod]             = useState<PaymentMethod>('cod')
  const [cardName, setCardName]         = useState('')
  const [cardNumber, setCardNumber]     = useState('')
  const [expiry, setExpiry]             = useState('')
  const [cvv, setCvv]                   = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError]               = useState<string | null>(null)

  const total = cart.totalPrice

  const handleContinue = () => {
    if (method === 'online') {
      setStep('payment')
    } else {
      handlePlaceOrder()
    }
  }

  const handlePlaceOrder = async () => {
    setError(null)
    setIsSubmitting(true)
    try {
      await onSubmit()
    } catch {
      setError('Failed to place order. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handlePay = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!cardName || cardNumber.replace(/\s/g, '').length < 16 || expiry.length < 5 || cvv.length < 3) {
      setError('Please fill in all card details correctly.')
      return
    }
    await handlePlaceOrder()
  }

  if (step === 'payment') {
    return (
      <form onSubmit={handlePay} className="space-y-5">
        <button
          type="button"
          onClick={() => { setStep('summary'); setError(null) }}
          className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors"
        >
          <ArrowLeft className="h-3.5 w-3.5" /> Back to summary
        </button>

        <div className="rounded-xl border border-primary/20 bg-primary/5 p-4">
          <p className="text-xs text-muted-foreground mb-1">Amount to pay</p>
          <p className="text-2xl font-bold text-primary">₹{total.toFixed(2)}</p>
        </div>

        <div className="space-y-3">
          <div>
            <label className="block text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-1.5" htmlFor="cardName">
              Cardholder Name
            </label>
            <Input
              id="cardName"
              placeholder="Name on card"
              value={cardName}
              onChange={(e) => setCardName(e.target.value)}
              autoComplete="cc-name"
              className="rounded-xl border-border/70"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-1.5" htmlFor="cardNumber">
              Card Number
            </label>
            <Input
              id="cardNumber"
              placeholder="1234 5678 9012 3456"
              value={cardNumber}
              onChange={(e) => setCardNumber(formatCardNumber(e.target.value))}
              inputMode="numeric"
              autoComplete="cc-number"
              className="rounded-xl border-border/70 font-mono tracking-wider"
            />
          </div>

          <div className="flex gap-3">
            <div className="flex-1">
              <label className="block text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-1.5" htmlFor="expiry">
                Expiry
              </label>
              <Input
                id="expiry"
                placeholder="MM/YY"
                value={expiry}
                onChange={(e) => setExpiry(formatExpiry(e.target.value))}
                inputMode="numeric"
                autoComplete="cc-exp"
                maxLength={5}
                className="rounded-xl border-border/70 font-mono"
              />
            </div>
            <div className="w-28">
              <label className="block text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-1.5" htmlFor="cvv">
                CVV
              </label>
              <Input
                id="cvv"
                placeholder="123"
                value={cvv}
                onChange={(e) => setCvv(e.target.value.replace(/\D/g, '').slice(0, 4))}
                inputMode="numeric"
                autoComplete="cc-csc"
                maxLength={4}
                className="rounded-xl border-border/70 font-mono"
              />
            </div>
          </div>
        </div>

        {error && (
          <p className="text-sm text-destructive bg-destructive/5 px-3 py-2 rounded-lg">{error}</p>
        )}

        <Button type="submit" className="w-full rounded-full h-11 shadow-sm shadow-primary/30" disabled={isSubmitting}>
          {isSubmitting ? (
            <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> Processing...</>
          ) : (
            <><Lock className="mr-2 h-4 w-4" /> Pay ₹{total.toFixed(2)}</>
          )}
        </Button>

        <div className="flex items-center justify-center gap-2 text-xs text-muted-foreground">
          <ShieldCheck className="h-3.5 w-3.5 text-green-500" />
          This is a simulated payment. No real charge will be made.
        </div>
      </form>
    )
  }

  return (
    <div className="space-y-6">
      {/* Order summary */}
      <div>
        <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">
          Order Summary
        </h3>
        <div className="rounded-xl border border-border/50 overflow-hidden">
          {cart.items.map((item, idx) => (
            <div
              key={item.productId}
              className={`flex justify-between items-center px-4 py-3 text-sm ${
                idx !== cart.items.length - 1 ? 'border-b border-border/40' : ''
              }`}
            >
              <span className="text-muted-foreground">
                {item.productName}
                <span className="ml-1 text-xs opacity-60">× {item.quantity}</span>
              </span>
              <span className="font-medium">₹{item.subtotal.toFixed(2)}</span>
            </div>
          ))}
          <div className="flex justify-between items-center px-4 py-3 bg-muted/30 border-t border-border/50">
            <span className="font-semibold">Total</span>
            <span className="font-bold text-lg text-primary">₹{total.toFixed(2)}</span>
          </div>
        </div>
      </div>

      {/* Payment method */}
      <div>
        <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">
          Payment Method
        </h3>
        <div className="grid grid-cols-2 gap-3">
          {([
            { id: 'cod' as PaymentMethod,    icon: Truck,       label: 'Cash on Delivery', sublabel: 'Pay at doorstep' },
            { id: 'online' as PaymentMethod, icon: CreditCard,  label: 'Pay Online',       sublabel: 'Card / UPI' },
          ]).map(({ id, icon: Icon, label, sublabel }) => (
            <button
              key={id}
              type="button"
              onClick={() => setMethod(id)}
              className={`flex flex-col items-center gap-2 rounded-xl border-2 p-4 transition-all cursor-pointer text-left ${
                method === id
                  ? 'border-primary bg-primary/5 shadow-sm shadow-primary/10'
                  : 'border-border/50 hover:border-border bg-card'
              }`}
            >
              <div className={`h-10 w-10 rounded-xl flex items-center justify-center ${
                method === id ? 'bg-primary/15' : 'bg-muted'
              }`}>
                <Icon className={`h-5 w-5 ${method === id ? 'text-primary' : 'text-muted-foreground'}`} />
              </div>
              <div className="text-center">
                <p className={`text-sm font-semibold ${method === id ? 'text-primary' : 'text-foreground'}`}>
                  {label}
                </p>
                <p className="text-xs text-muted-foreground">{sublabel}</p>
              </div>
            </button>
          ))}
        </div>
      </div>

      {error && (
        <p className="text-sm text-destructive bg-destructive/5 px-3 py-2 rounded-lg">{error}</p>
      )}

      <Button
        className="w-full rounded-full h-11 shadow-sm shadow-primary/30 hover:shadow-primary/50 transition-all"
        onClick={handleContinue}
        disabled={isSubmitting || cart.items.length === 0}
      >
        {isSubmitting ? (
          <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> Processing...</>
        ) : method === 'cod' ? (
          <><Truck className="mr-2 h-4 w-4" /> Confirm Order</>
        ) : (
          <><CreditCard className="mr-2 h-4 w-4" /> Continue to Payment</>
        )}
      </Button>
    </div>
  )
}
