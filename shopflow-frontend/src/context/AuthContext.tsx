import { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import keycloak from '@/lib/keycloak'

export interface AuthUser {
  id: string
  email: string
  name: string
}

interface AuthContextValue {
  isAuthenticated: boolean
  isAdmin: boolean
  user: AuthUser | null
  isLoading: boolean
  token: string | undefined
  login: () => void
  logout: () => void
  register: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

let keycloakInitialized = false

function extractIsAdmin(parsed: Record<string, unknown> | undefined): boolean {
  const realmAccess = parsed?.['realm_access'] as { roles?: string[] } | undefined
  return realmAccess?.roles?.includes('ADMIN') ?? false
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [isAdmin, setIsAdmin]                 = useState(false)
  const [user, setUser]                       = useState<AuthUser | null>(null)
  const [isLoading, setIsLoading]             = useState(true)

  useEffect(() => {
    if (keycloakInitialized) {
      setIsLoading(false)
      setIsAuthenticated(keycloak.authenticated ?? false)
      if (keycloak.authenticated && keycloak.tokenParsed) {
        setUser({
          id:    keycloak.subject ?? '',
          email: (keycloak.tokenParsed['email'] as string) ?? '',
          name:  (keycloak.tokenParsed['name']  as string) ?? '',
        })
        setIsAdmin(extractIsAdmin(keycloak.tokenParsed))
      }
      return
    }

    keycloakInitialized = true

    keycloak
      .init({ onLoad: 'check-sso', checkLoginIframe: false })
      .then((authenticated) => {
        setIsAuthenticated(authenticated)
        if (authenticated && keycloak.tokenParsed) {
          setUser({
            id:    keycloak.subject ?? '',
            email: (keycloak.tokenParsed['email'] as string) ?? '',
            name:  (keycloak.tokenParsed['name']  as string) ?? '',
          })
          setIsAdmin(extractIsAdmin(keycloak.tokenParsed))
        }
      })
      .finally(() => setIsLoading(false))
  }, [])

  return (
    <AuthContext.Provider value={{
      isAuthenticated,
      isAdmin,
      user,
      isLoading,
      token:    keycloak.token,
      login:    () => keycloak.login(),
      logout:   () => keycloak.logout(),
      register: () => keycloak.register(),
    }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
