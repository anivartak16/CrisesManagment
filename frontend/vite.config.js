import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// The Spring Boot backend's CorsConfig allows http://localhost:5173 and
// http://localhost:3000. Vite's default port is 5173, so no proxy is
// required as long as VITE_API_BASE_URL points at the backend directly.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
  },
});
