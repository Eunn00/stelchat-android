import { defineConfig } from 'vite';
export default defineConfig({
  server: { proxy: { '/api': 'https://stelchat.xyz', '/emoji-assets': 'https://stelchat.xyz' } },
  build: { target: 'es2022' }
});
