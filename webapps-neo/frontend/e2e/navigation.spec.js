import { test, expect } from "./fixtures.js";

// Authenticated via the seeded sessionStorage in fixtures.js.
test.describe("navigation", () => {
  test("the main nav links to every web app", async ({ page }) => {
    await page.goto("/");

    // Nav links are duplicated for responsive layouts, so assert presence
    // (the navigate-on-click behavior is covered by the next test).
    for (const href of [
      "/tasks",
      "/processes",
      "/decisions",
      "/deployments",
      "/batches",
      "/migrations",
      "/admin",
      "/account",
      "/help",
    ]) {
      await expect(page.locator(`nav a[href="${href}"]`)).not.toHaveCount(0);
    }
  });

  test("clicking a nav link navigates to that web app", async ({ page }) => {
    await page.goto("/");
    await page.locator('nav a[href="/processes"]').first().click();
    await expect(page).toHaveURL(/\/processes/);
    await expect(page.locator("main#processes")).toBeVisible();
  });
});
