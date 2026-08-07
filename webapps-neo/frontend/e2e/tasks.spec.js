import { test, expect } from "./fixtures.js";

test.describe("tasks", () => {
  test("shows the task list", async ({ page }) => {
    await page.goto("/tasks");
    await expect(page.locator("main#content.tasks")).toBeVisible();
    // The list heading is visually hidden (a11y), so assert it is present.
    await expect(page.getByRole("heading", { name: /task list/i })).toHaveCount(
      1,
    );
  });

  test("opens a task detail when a task exists", async ({ page }) => {
    await page.goto("/tasks");
    const firstTask = page.locator('main#content a[href^="/tasks/"]').first();
    if (await firstTask.count()) {
      await firstTask.click();
      await expect(page).toHaveURL(/\/tasks\/.+/);
    }
  });
});
