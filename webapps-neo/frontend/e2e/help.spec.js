import { test, expect } from "./fixtures.js";

test.describe("help", () => {
  test("renders the help/home page with documentation links", async ({
    page,
  }) => {
    await page.goto("/help");
    await expect(
      page.locator('a[href="https://forum.operaton.org"]').first(),
    ).toBeVisible();
    await expect(
      page.locator('a[href="https://github.com/operaton/operaton"]').first(),
    ).toBeVisible();
  });
});
