import axios from 'axios'
import keycloak from './keycloak'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
})

apiClient.interceptors.request.use((config) => {
  if (keycloak.token) {
    config.headers.Authorization = `Bearer ${keycloak.token}`
  }
  return config
})

// Refresh token on 401 and retry once
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401 && keycloak.isTokenExpired()) {
      await keycloak.updateToken(30)
      return apiClient(error.config)
    }
    return Promise.reject(error)
  },
)

export default apiClient
