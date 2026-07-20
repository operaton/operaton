import { test, expect } from "./fixtures.js";

test.describe("account", () => {
  test("renders the account area for the signed-in user", async ({ page }) => {
    await page.goto("/account");
    // Account redirects to its first sub-page (profile).
    await expect(page).toHaveURL(/\/account\/profile/);
    // Sub-navigation between account sections is present.
    await expect(page.locator('a[href^="/account/"]').first()).toBeVisible();
  });
});
