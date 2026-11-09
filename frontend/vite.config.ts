import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/notifications': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/mock-store': {
        target: 'http://localhost:8085',
        changeOrigin: true,
      },
    },
  },
});