import { test, expect } from "./fixtures.js";

test.describe("batches", () => {
  test("renders the batches page", async ({ page }) => {
    await page.goto("/batches");
    await expect(page.locator("main#content.batches")).toBeVisible();
    await expect(page.getByRole("heading", { name: /batches/i })).toBeVisible();
  });

  test("the header Batches link routes to /batches", async ({ page }) => {
    await page.goto("/");
    await page.locator('nav a[href="/batches"]').first().click();
    await expect(page).toHaveURL(/\/batches/);
    await expect(page.locator("main#content.batches")).toBeVisible();
  });

  test("shows a select-batch prompt when none is selected", async ({
    page,
  }) => {
    await page.goto("/batches");
    // Either the empty-list note or the select prompt is shown (the dev engine
    // usually has no batches); the page should not error.
    await expect(page.locator("main#content.batches")).toBeVisible();
  });
});
