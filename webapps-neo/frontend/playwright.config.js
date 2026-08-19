import { defineConfig, devices } from "@playwright/test";

/**
 * Playwright e2e config for the Operaton Web Apps.
 *
 * Prerequisite: the Operaton backend must be running on :8084
 *   docker compose up        (or:  podman compose up)
 * The dev server (started automatically below via `webServer`) reads
 * VITE_BACKEND from .env.development, which points at http://localhost:8084.
 *
 * Run:  npm run test:e2e        (headless)
 *       npx playwright test --ui
 */
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  reporter: process.env.CI ? "github" : "list",
  use: {
    baseURL: "http://127.0.0.1:5173",
    trace: "on-first-retry",
    screenshot: "only-on-failure",
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
  webServer: {
    command: "npm run dev",
    url: "http://127.0.0.1:5173",
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
});
