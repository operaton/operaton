import { test, expect } from "./fixtures.js";

test.describe("deployments", () => {
  test("redirects to the first deployment and renders the deployments view", async ({
    page,
  }) => {
    await page.goto("/deployments");
    await expect(page.locator("main#content.deployments")).toBeVisible();
    // The page selects the most recent deployment, reflected in the URL.
    await expect(page).toHaveURL(/\/deployments(\/.+)?/);
  });
});
