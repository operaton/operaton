import { test, expect } from "./fixtures.js";

test.describe("404", () => {
  test("shows the not-found page for an unknown route", async ({ page }) => {
    await page.goto("/this-route-does-not-exist");
    // The 404 page hints at the ALT + K global search.
    await expect(page.getByText(/alt \+ k/i)).toBeVisible();
  });
});
