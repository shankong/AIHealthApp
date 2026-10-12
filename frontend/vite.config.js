import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'node:path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    proxy: {
      '/api': {
        // 本地 Spring Boot 后端（端口在 backend/src/main/resources/application.yml 里配的 8080）。
        // 原来指向远程 http://159.75.169.224:1235/，那是别人写的完整后端。
        //
        // 这里不需要 rewrite：本地 Controller 自己就带 /api 前缀
        // （controller/User.java 上是 @RequestMapping("/api/user")），
        // 所以 /api/user/login 原样转发到 http://localhost:8080/api/user/login 正好对上。
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
})

