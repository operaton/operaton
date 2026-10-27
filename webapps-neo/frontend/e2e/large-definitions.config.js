/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { defineConfig, devices } from '@playwright/test';

// Self-contained regression: every backend call is mocked by the spec. The
// browser gets a fresh profile; this never attaches to an existing browser.
const server = 'http://127.0.0.1:5187';

export default defineConfig({
  testDir: '.',
  // A distinct suffix keeps this mocked suite out of the ordinary backend e2e
  // run, whose fixtures and server are intentionally different.
  testMatch: 'processes-large-definitions.regression.js',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  workers: 1,
  retries: 0,
  timeout: 60_000,
  expect: { timeout: 15_000 },
  reporter: process.env.CI ? 'github' : 'list',
  outputDir: '../test-results/large-definitions',
  use: {
    ...devices['Desktop Chrome'],
    baseURL: server,
    locale: 'en-US',
    // Leave unset for Playwright's pinned Chromium, or use an installed Chrome
    // with NEO_E2E_CHANNEL=chrome when downloading a browser is undesirable.
    ...(process.env.NEO_E2E_CHANNEL
      ? { channel: process.env.NEO_E2E_CHANNEL }
      : {}),
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  webServer: {
    command: 'npm run dev -- --host 127.0.0.1 --port 5187 --strictPort',
    url: server,
    reuseExistingServer: false,
    timeout: 120_000,
  },
});
