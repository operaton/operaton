import { test, CREDENTIALS } from "./fixtures.js";
import { test as base } from "@playwright/test";
import { STATIC_ROUTES, LOGIN_ROUTE, discover_deep_routes } from "./routes.js";
import { expect_no_a11y_violations } from "./a11y.js";

// Automated WCAG 2.x A/AA scans (axe-core) over every page in the shared route
// manifest (routes.js). The `test` fixture is pre-authenticated (fixtures.js)
// and the Vite dev server is started by Playwright; the Operaton backend must be
// up on :8084 for authed pages and deep-route discovery to render real content
// (docker compose -f docker-compose.dev-fixtures.yaml up -d).

// Navigate, wait for the page's readiness landmark, then assert no violations.
// `ready` defaults to the <main> landmark every authed page renders; the login
// screen has none, so it overrides.
const scan = async (page, path, ready = "main") => {
  await page.goto(path, { waitUntil: "domcontentloaded" });
  await page.locator(ready).first().waitFor({ timeout: 30_000 });
  // Freeze fade-in animations/transitions to their end state so axe never
  // samples a mid-animation (reduced-opacity) frame as a false contrast failure.
  await page.addStyleTag({
    content:
      "*,*::before,*::after{animation-duration:0s!important;animation-delay:0s!important;transition:none!important}",
  });
  await expect_no_a11y_violations(page);
};

test.describe("accessibility (axe)", () => {
  for (const { path, name } of STATIC_ROUTES) {
    test(`${name} (${path}) has no WCAG A/AA violations`, async ({ page }) => {
      // Heavy routes compile on demand under parallel cold-start; give headroom.
      test.setTimeout(60_000);
      await scan(page, path);
    });
  }

  // Deep routes (instance/task/decision detail) are discovered from live engine
  // data at run time; a single looping test keeps Playwright's sync collection
  // happy and skips cleanly when the engine has no such data.
  test("deep routes have no WCAG A/AA violations", async ({ page }) => {
    test.setTimeout(90_000);
    const routes = await discover_deep_routes({ credentials: CREDENTIALS });
    test.skip(routes.length === 0, "engine has no instances/tasks/decisions");
    for (const { path } of routes) await scan(page, path);
  });
});

// The login screen renders only when unauthenticated, so scan it with the
// plain (non-auth-seeding) fixture.
base.describe("accessibility (axe) — login", () => {
  base(`${LOGIN_ROUTE.name} has no WCAG A/AA violations`, async ({ page }) => {
    base.setTimeout(60_000);
    await scan(page, LOGIN_ROUTE.path, "section.login-page");
  });
});
