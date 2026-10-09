import { test as base, expect } from "@playwright/test";

export const CREDENTIALS = { username: "demo", password: "demo" };

/**
 * `test` with an already-authenticated page.
 *
 * This used to seed sessionStorage with the credentials, because the app
 * restored them from there on load. It no longer keeps a password anywhere on
 * the client, so authentication has to happen the way a user does it.
 *
 * Against the webapp's own backend the session cookie survives navigation, so
 * one login here covers the whole test. Against a backend configured elsewhere
 * (which is what .env.development points at) credentials live in memory for the
 * page's lifetime, so a spec that calls page.goto() again has to log in again —
 * use `login_via_form` after such a navigation.
 */
export const test = base.extend({
  page: async ({ page }, use) => {
    await page.goto("/");
    await login_via_form(page);
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
