import { defineConfig, configDefaults } from "vitest/config";
import preact from "@preact/preset-vite";

export default defineConfig({
  plugins: [preact()],
  test: {
    globals: true,
    environment: "happy-dom",
    setupFiles: ["src/test/setup.js"],
    // Playwright specs under e2e/ are run by `npm run test:e2e`, not vitest.
    exclude: [...configDefaults.exclude, "e2e/**"],
    coverage: {
      reporter: ["text", "json", "html"],
      exclude: [
        "node_modules/",
        "dist/",
        "**/*.config.js",
        "**/*.spec.js",
        "**/*.test.js",
        "**/*.test.jsx",
        "src/test/",
        "e2e/",
      ],
    },
  },
});
