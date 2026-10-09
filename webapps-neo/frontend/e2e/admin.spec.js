import { test, expect } from "./fixtures.js";

test.describe("admin", () => {
  test("redirects to the users sub-page", async ({ page }) => {
    await page.goto("/admin");
    await expect(page).toHaveURL(/\/admin\/users/);
  });

  test("provides sub-navigation to groups, tenants and authorizations", async ({
    page,
  }) => {
    await page.goto("/admin/users");
    // Sub-nav links are routed under /admin/*
    for (const sub of ["groups", "tenants", "authorizations"]) {
      await expect(
        page.locator(`a[href^="/admin/${sub}"]`).first(),
      ).toBeVisible();
    }
  });

  test("navigates to the groups sub-page", async ({ page }) => {
    await page.goto("/admin/users");
    await page.locator('a[href^="/admin/groups"]').first().click();
    await expect(page).toHaveURL(/\/admin\/groups/);
  });
});
