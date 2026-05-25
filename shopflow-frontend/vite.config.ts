import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api/v1/users':    { target: 'http://localhost:8081', changeOrigin: true },
      '/api/v1/catalog':  { target: 'http://localhost:8082', changeOrigin: true },
      '/api/v1/cart':     { target: 'http://localhost:8083', changeOrigin: true },
      '/api/v1/payments': { target: 'http://localhost:8084', changeOrigin: true },
      '/api/v1/orders':   { target: 'http://localhost:8085', changeOrigin: true },
    },
  },
})
