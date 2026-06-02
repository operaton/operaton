import { test, expect } from "./fixtures.js";

test.describe("global search (GoTo)", () => {
  test("opens with the ALT+K hotkey", async ({ page }) => {
    await page.goto("/");
    await page.locator("body").click();
    await page.keyboard.press("Alt+KeyK");
    await expect(page.locator('input[type="search"]')).toBeVisible();
  });

  test("opens from the header button and exposes a focused search box", async ({
    page,
  }) => {
    await page.goto("/");
    await page.getByRole("button", { name: /go to/i }).click();

    const search = page.locator('input[type="search"]');
    await expect(search).toBeVisible();
    await expect(search).toBeFocused();
  });

  test("filtering by a page name and selecting it navigates there", async ({
    page,
  }) => {
    await page.goto("/");
    await page.getByRole("button", { name: /go to/i }).click();

    const search = page.locator('input[type="search"]');
    await search.fill("processes");

    // The "pages" category lists the Processes web app; selecting it navigates.
    const option = page
      .locator('[role="option"]', { hasText: /^processes$/i })
      .first();
    await expect(option).toBeVisible();
    await option.click();

    await expect(page).toHaveURL(/\/processes/);
  });
});
