import { test, expect } from "./fixtures.js";

test.describe("decisions", () => {
  test("shows the decisions list view", async ({ page }) => {
    await page.goto("/decisions");
    await expect(page.locator("main#content.decisions")).toBeVisible();
    await expect(
      page.getByRole("heading", { name: /queried decisions/i }),
    ).toHaveCount(1);
  });
});
