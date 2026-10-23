import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080'
    }
  },
  build: {
    chunkSizeWarningLimit: 3000
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['tests/setup.js'],
    // Element Plus를 Vite로 변환해야 async-validator의 ESM 진입점이 쓰인다.
    // (외부 모듈로 두면 Node가 CJS 버전을 불러와 폼 검증이 조용히 통과되어 버린다)
    server: { deps: { inline: ['element-plus'] } }
  }
})
