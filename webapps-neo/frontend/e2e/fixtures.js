import { test as base, expect } from "@playwright/test";

export const CREDENTIALS = { username: "demo", password: "demo" };

/**
 * `test` with an already-authenticated page. The app's basic-auth flow restores
 * credentials from sessionStorage on load (see auth.is_authenticated /
 * restore_basic_session), so seeding the key before navigation logs us in
 * without driving the login form every time.
 */
export const test = base.extend({
  page: async ({ page }, use) => {
    await page.addInitScript((creds) => {
      window.sessionStorage.setItem("basic_auth", JSON.stringify(creds));
    }, CREDENTIALS);
    await use(page);
  },
});

export { expect };

/**
 * Drive the actual login form (used by the login spec). Assumes the page is on
 * the login screen (unauthenticated).
 */
export const login_via_form = async (page) => {
  await page.getByLabel(/user name/i).fill(CREDENTIALS.username);
  await page.getByLabel(/password/i).fill(CREDENTIALS.password);
  await page.getByRole("button", { name: /login/i }).click();
};
