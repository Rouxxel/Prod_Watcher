import { defineConfig } from "vite";
import { tanstackStart } from "@tanstack/react-start/plugin/vite";
import { cloudflare } from "@cloudflare/vite-plugin";
import { nitro } from "nitro/vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import tsconfigPaths from "vite-tsconfig-paths";

// Vercel: nitro/vite (SSR → Vercel Functions). Cloudflare: @cloudflare/vite-plugin.
// Custom SSR entry: src/server.ts wraps @tanstack/react-start/server-entry with error handling.
const isVercel = process.env.VERCEL === "1";

export default defineConfig({
  server: {
    port: 8000,
    strictPort: true,
    host: true,
  },
  plugins: [
    ...(isVercel ? [nitro()] : [cloudflare({ viteEnvironment: { name: "ssr" } })]),
    tanstackStart({
      server: { entry: "server" },
    }),
    react(),
    tailwindcss(),
    tsconfigPaths(),
  ],
  resolve: {
    dedupe: ["react", "react-dom", "@tanstack/react-router", "@tanstack/react-query"],
  },
});
