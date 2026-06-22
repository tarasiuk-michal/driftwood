import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/events': 'http://localhost:8080',
      '/scenarios': 'http://localhost:8080',
      '/workflows': 'http://localhost:8080',
    },
  },
})
