import { defineConfig, devices } from "@playwright/test";

/**
 * Playwright e2e config for the Operaton Web Apps.
 *
 * Prerequisite: a backend must be running. By default the dev server proxies
 * /api to http://localhost:8080. For the docker compose stack on :8084 set
 * E2E_BACKEND_TARGET=http://localhost:8084 and start it with:
 *   docker compose up        (or:  podman compose up)
 * The dev server started below via `webServer` is handed its backend contract
 * explicitly (see `backend_env`), overriding .env.development[.local].
 *
 * Run:  npm run test:e2e        (headless)
 *       npx playwright test --ui
 *
 * The sub-path suite (the app served from a prefix rather than the server root)
 * needs a production build instead of the dev server, so it is opt-in:
 *       npm run test:e2e:sub-path
 * It takes over the dev server's port, because that is the origin the backend's
 * CORS configuration allows.
 */
const SERVER = "http://127.0.0.1:5173";

// Handed to every webServer below. Vite's loadEnv() lets process.env win over
// the .env files, so this pins the contract regardless of what a developer has
// in .env.development.local (which may target an external engine on another
// port, the combination that makes the session login fail and the canvas spec
// skip itself).
const backend_env = {
  // An empty `url` means "same origin as the app": the app then authenticates
  // through the proxied session cookie instead of engine-rest + CORS.
  VITE_BACKEND: JSON.stringify([{ name: "Operaton", url: "" }]),
  // A Spring Boot host serves these web apps under /api, so the proxy must
  // keep the prefix or the login call never reaches the backend.
  VITE_PROXY_TARGET: process.env.E2E_BACKEND_TARGET ?? "http://localhost:8080",
  VITE_PROXY_KEEP_API_PREFIX: "true",
};

const SUB_PATH = process.env.E2E_APP_PATH ?? "/app-neo";
const sub_path_enabled = !!process.env.E2E_SUB_PATH;

const dev_server = {
  command: "npm run dev",
  url: SERVER,
  reuseExistingServer: !process.env.CI,
  timeout: 120_000,
  env: backend_env,
};

const sub_path_server = {
  command: "npm run build && node e2e/sub-path-server.mjs",
  url: `${SERVER}${SUB_PATH}/`,
  // Always a fresh build: a stale bundle would test the wrong thing.
  reuseExistingServer: false,
  timeout: 180_000,
  env: { ...backend_env, PORT: "5173", E2E_APP_PATH: SUB_PATH },
};

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  reporter: process.env.CI ? "github" : "list",
  use: {
    baseURL: SERVER,
    trace: "on-first-retry",
    screenshot: "only-on-failure",
  },
  projects: [
    {
      name: "chromium",
      // The sub-path suite runs against its own server, in its own project.
      testIgnore: /sub-path\.spec\.js/,
      use: { ...devices["Desktop Chrome"] },
    },
    ...(sub_path_enabled
      ? [
          {
            name: "sub-path",
            testMatch: /sub-path\.spec\.js/,
            use: { ...devices["Desktop Chrome"] },
          },
        ]
      : []),
  ],
  // One or the other: both want :5173, and only one of them is being tested.
  webServer: sub_path_enabled ? [sub_path_server] : [dev_server],
});
