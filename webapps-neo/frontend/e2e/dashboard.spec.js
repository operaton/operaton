import { test, expect } from "./fixtures.js";

test.describe("dashboard", () => {
  test("greets the user and shows the overview cards", async ({ page }) => {
    await page.goto("/");
    await expect(page.locator("main#content.dashboard")).toBeVisible();
    await expect(
      page.getByRole("heading", { name: /hello, demo/i }),
    ).toBeVisible();
  });

  test("links from the overview into the task and process apps", async ({
    page,
  }) => {
    await page.goto("/");
    const main = page.locator("main#content");
    await expect(main.locator('a[href="/tasks"]').first()).toBeVisible();
    await expect(main.locator('a[href="/processes"]').first()).toBeVisible();
  });
});
