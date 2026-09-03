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
 *
 * See docs/Manual Accessibility Testing.md for the headed and UI-mode scripts
 * used to watch the accessibility specs run.
 */
export default defineConfig({
  testDir: "./e2e",
  // Playwright's default testMatch also picks up `*.test.js`, and the pure
  // modules under e2e/ are unit-tested with vitest under exactly that suffix.
  // Collecting them here throws `describe is not defined` during collection,
  // which aborts the whole run — the suite reported "0 tests in 0 files" until
  // this was pinned. The suffix is the split: *.spec.js is Playwright's,
  // *.test.js is vitest's (see vitest.config.js).
  testMatch: "**/*.spec.js",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  reporter: process.env.CI ? "github" : "list",
  use: {
    baseURL: "http://127.0.0.1:5173",
    trace: "on-first-retry",
    screenshot: "only-on-failure",
    // Slows every browser operation so a headed run can be followed by eye.
    // Playwright has no CLI flag for this — slowMo is a launch option only.
    // 0 (the default) leaves headless runs and CI at full speed; `npm run
    // test:a11y:watch` sets it.
    launchOptions: { slowMo: Number(process.env.PW_SLOW_MO ?? 0) },
  },
  // Chromium is the default everywhere: the gate, the e2e suite and CI all pass
  // --project=chromium explicitly (see package.json), so adding firefox here
  // cannot silently double a run. Firefox exists to watch the same specs in the
  // browser we do manual screen-reader testing in, and needs a one-time
  // `npx playwright install firefox`.
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
    {
      name: "firefox",
      use: { ...devices["Desktop Firefox"] },
    },
  ],
  webServer: {
    command: "npm run dev",
    url: "http://127.0.0.1:5173",
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
});
