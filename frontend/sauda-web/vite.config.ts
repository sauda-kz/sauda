import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

const backendProxy = {
  target: "http://localhost:8080",
  changeOrigin: true,
};

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 3000,
    proxy: {
      "/api": backendProxy,
      "/actuator": backendProxy,
      "/swagger-ui.html": backendProxy,
      "/swagger-ui": backendProxy,
      "/v3/api-docs": backendProxy,
    },
  },
  test: {
    environment: "jsdom",
    globals: true,
  },
});
