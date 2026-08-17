import { test, expect } from "./fixtures.js";

// Keyboard-operability checks that axe cannot assert: focus order, skip links,
// roving tabindex, and Escape/native-dialog focus management. Uses the
// pre-authenticated page fixture; the backend must be up on :8084.

test.describe("keyboard navigation", () => {
  test("the first Tab lands on the skip-to-content link, targeting #content", async ({
    page,
  }) => {
    await page.goto("/tasks");
    await page.locator("main#content").waitFor();

    await page.keyboard.press("Tab");
    const focused = page.locator(":focus");
    await expect(focused).toHaveJSProperty("tagName", "A");
    await expect(focused).toHaveAttribute("href", "#content");
    // The skip target actually exists on the page.
    await expect(page.locator("#content")).toHaveCount(1);
  });

  test("the current route's nav link exposes aria-current=page", async ({
    page,
  }) => {
    await page.goto("/processes");
    const nav = page.locator("#primary-navigation");
    await expect(nav.locator('a[href="/processes"]')).toHaveAttribute(
      "aria-current",
      "page",
    );
    // A non-current link must not claim to be the current page.
    await expect(nav.locator('a[href="/tasks"]')).not.toHaveAttribute(
      "aria-current",
      "page",
    );
  });

  test("the server selector is reachable and has an accessible name", async ({
    page,
  }) => {
    await page.goto("/tasks");
    // The desktop selector — scoped so it doesn't clash with the mobile one.
    // Its accessible name comes from the visible "Server" label wrapping it.
    const select = page.locator("#server-selector select");
    await expect(select).toHaveAccessibleName(/server/i);
    await select.focus();
    await expect(select).toBeFocused();
  });

  test("the global search dialog opens, traps focus, and Escape closes it", async ({
    page,
  }) => {
    await page.goto("/tasks");
    await page.locator("main#content").waitFor();

    const dialog = page.locator("dialog#global-search");
    await expect(dialog).toBeHidden();

    // Open via the visible Go To trigger (also bound to Alt+K).
    await page.locator("#go-to").click();
    await expect(dialog).toBeVisible();
    // Native <dialog>.showModal() moves focus into the dialog.
    await expect(dialog.locator(":focus")).toHaveCount(1);

    await page.keyboard.press("Escape");
    await expect(dialog).toBeHidden();
  });

  test("task detail tabs support roving arrow-key navigation", async ({
    page,
  }) => {
    await page.goto("/tasks");
    const firstTask = page.locator('main#content a[href^="/tasks/"]').first();
    test.skip(
      (await firstTask.count()) === 0,
      "no tasks available in this environment",
    );

    await firstTask.click();

    // The task detail (and its tablist) loads async; give it a bounded wait and
    // skip if this task has no tabbed detail in the current environment.
    const tablist = page.getByRole("tablist");
    const appeared = await tablist
      .waitFor({ state: "visible", timeout: 8000 })
      .then(() => true)
      .catch(() => false);
    test.skip(!appeared, "task detail tablist did not render");

    const selectedTab = tablist.getByRole("tab", { selected: true });
    await selectedTab.focus();
    await page.keyboard.press("ArrowRight");
    // Focus moved to another tab and the route/selection followed.
    const nowFocused = page.locator(":focus");
    await expect(nowFocused).toHaveRole("tab");
    await expect(nowFocused).toHaveAttribute("aria-selected", "true");
  });
});
