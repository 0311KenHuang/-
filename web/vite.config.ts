import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({ resolvers: [ElementPlusResolver()] }),
    Components({ resolvers: [ElementPlusResolver()] }),
  ],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  build: {
    chunkSizeWarningLimit: 800,
    rollupOptions: {
      output: {
        // 大依赖拆独立 chunk：主包显著缩小，且依赖升级前用户端缓存仍有效
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined;
          if (id.includes('element-plus') || id.includes('@element-plus')) return 'vendor-element';
          if (id.includes('/vue/') || id.includes('@vue') || id.includes('vue-router') || id.includes('pinia')) {
            return 'vendor-vue';
          }
          return 'vendor-misc';
        },
      },
    },
  },
  server: {
    port: 5173,
    proxy: {
      // 开发期直连本地服务，生产由 nginx 反代
      '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true },
    },
  },
});
