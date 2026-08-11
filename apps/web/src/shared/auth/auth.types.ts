export interface CurrentUser {
  id: string
  email: string
  displayName: string
  emailVerified: boolean
  timeZone: string
  theme: 'INDIGO' | 'FOREST' | 'APRICOT' | 'GRAPHITE_CORAL'
}

export type AuthStatus = 'unknown' | 'loading' | 'authenticated' | 'guest'

export interface LoginCredentials { email: string; password: string }
