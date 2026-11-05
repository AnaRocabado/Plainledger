import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

// In development, the API runs on the host (./mvnw spring-boot:run or the IDE).
// In Docker, Nginx serves the build and proxies the same paths (see nginx.conf).
const backend = "http://localhost:8080";

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/api": backend,
      "/actuator/health": backend,
    },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/test/setup.ts"],
    // JUnit XML is the traceability evidence for frontend tests (ADR-0009).
    reporters: ["default", "junit"],
    outputFile: { junit: "./test-results/junit.xml" },
  },
});
