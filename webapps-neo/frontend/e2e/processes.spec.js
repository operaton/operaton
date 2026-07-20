import { test, expect } from "./fixtures.js";

test.describe("processes", () => {
  test("lists deployed process definitions", async ({ page }) => {
    await page.goto("/processes");
    await expect(page.locator("main#processes")).toBeVisible();
    await expect(
      page.getByRole("heading", { name: /deployed process definitions/i }),
    ).toBeVisible();
  });

  test("opens a process definition overview when one exists", async ({
    page,
  }) => {
    await page.goto("/processes");
    const firstDefinition = page
      .locator('main#processes a[href^="/processes/"]')
      .first();

    // The seeded engine ships demo processes, but guard in case it doesn't.
    if (await firstDefinition.count()) {
      await firstDefinition.click();
      await expect(page).toHaveURL(/\/processes\/.+/);
    }
  });
});
