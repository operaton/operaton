import { test, expect } from "@playwright/test";
import { login_via_form } from "./fixtures.js";

// Uses the un-seeded base `test`, so we land on the login screen.
test.describe("login", () => {
  test("shows the login screen when unauthenticated", async ({ page }) => {
    await page.goto("/");
    await expect(
      page.getByRole("heading", { name: /operaton web apps login/i }),
    ).toBeVisible();
    await expect(page.getByRole("button", { name: /login/i })).toBeVisible();
  });

  test("lets a user sign in with demo credentials and reach the app", async ({
    page,
  }) => {
    await page.goto("/");
    await login_via_form(page);

    // The login heading is gone and the app chrome (main navigation) appears.
    await expect(
      page.getByRole("heading", { name: /operaton web apps login/i }),
    ).toBeHidden();
    await expect(page.locator("nav").first()).toBeVisible();
  });

  test("offers a server selector on the login screen", async ({ page }) => {
    await page.goto("/");
    await expect(page.locator("select")).toBeVisible();
  });
});
