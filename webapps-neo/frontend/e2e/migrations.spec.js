import { test, expect } from "./fixtures.js";

test.describe("migrations", () => {
  test("renders the migration wizard with source/target selection", async ({
    page,
  }) => {
    await page.goto("/migrations");
    // The wizard renders its numbered steps as visible headings.
    await expect(
      page.getByRole("heading", { name: /select process definitions/i }),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", { name: /map activities/i }),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", { name: /configure & execute/i }),
    ).toBeVisible();
  });
});
