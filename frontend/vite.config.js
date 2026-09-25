import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// In development the React app calls /api on its own origin and Vite forwards it to Spring Boot,
// so no CORS setup is needed locally. Set VITE_PROXY_TARGET if the API is not on :8080.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const target = env.VITE_PROXY_TARGET || 'http://localhost:8080'
  return {
    plugins: [react(), tailwindcss()],
    server: {
      port: 5173,
      proxy: {
        '/api': { target, changeOrigin: true },
      },
    },
    build: {
      chunkSizeWarningLimit: 1500,
    },
  }
})
