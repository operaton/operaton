import { test, expect } from "@playwright/test";
import { login_via_form } from "./fixtures.js";

/**
 * The SPA served from a sub-path (`operaton.bpm.webapp.neo.application-path`).
 *
 * Runs against e2e/sub-path-server.mjs, which serves the built bundle behind
 * `/app-neo` the way a real deployment does. This is the only level that catches
 * the routing half of sub-path support: a unit test can assert `app_path()`
 * returns the right string, but only a browser proves the router still matches.
 *
 * Enabled with E2E_SUB_PATH=1 (see playwright.config.js) because it needs a
 * production build rather than the dev server.
 */
const APP_PATH = process.env.E2E_APP_PATH ?? "/app-neo";
const ORIGIN = process.env.E2E_SUB_PATH_URL ?? "http://127.0.0.1:5173";

/**
 * Same-origin responses that failed — a blank page is always one of these.
 *
 * `/locales/` is exempt: i18next asks for the detected language ("en") before
 * falling back to the one we ship ("en-US"), so a 404 there is expected and
 * happens at the server root just the same.
 */
const collect_failures = (page) => {
  const failures = [];
  page.on("response", (response) => {
    const url = new URL(response.url());
    if (
      url.origin === ORIGIN &&
      response.status() >= 400 &&
      !url.pathname.startsWith(`${APP_PATH}/locales/`)
    ) {
      failures.push(`${response.status()} ${url.pathname}`);
    }
  });
  return failures;
};

test.describe("sub-path deployment", () => {
  test("boots, routes and survives a reload on a deep route", async ({
    page,
  }) => {
    const failures = collect_failures(page);

    await page.goto(`${APP_PATH}/`);

    // The shell states the application root; everything else hangs off it.
    await expect(page.locator("base")).toHaveAttribute("href", `${APP_PATH}/`);

    await login_via_form(page);
    await expect(page).toHaveURL(new RegExp(`${APP_PATH}/$`));

    // Navigation: the router matches prefixed routes against prefixed links.
    await page.locator(`nav a[href="${APP_PATH}/processes"]`).first().click();
    await expect(page).toHaveURL(new RegExp(`${APP_PATH}/processes`));
    await expect(page.locator("main.processes")).toBeVisible();

    // A hard reload on a deep route: the server answers with the shell and the
    // router has to land on the same page rather than the 404 component.
    await page.reload();
    await login_via_form(page);
    await expect(page.locator("main.processes")).toBeVisible();

    expect(failures, "no same-origin request may fail").toEqual([]);
  });

  test("does not answer a missing asset with the shell", async ({ page }) => {
    const response = await page.goto(`${APP_PATH}/assets/does-not-exist.js`);

    expect(response.status()).toBe(404);
  });
});
