import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { loadEnv } from "vite";
import { defineConfig } from "vitest/config";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const proxyTarget = env.DEV_PROXY_TARGET ?? "http://localhost:8080";

  const backendProxy = {
    target: proxyTarget,
    changeOrigin: true,
  };

  return {
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
  };
});
