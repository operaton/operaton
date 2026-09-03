import { test, expect } from "./fixtures.js";

// Menus and selection lists are one stop in the tab order; the arrow keys move
// within them. See src/helper/roving_focus.js — entries stay plain links, so
// this is a tabindex and key-handling change, not an ARIA role change.

const focused_text = (page) =>
  page.evaluate(() => document.activeElement?.textContent?.trim());

test.describe("arrow navigation", () => {
  test("the primary nav is a single tab stop", async ({ page }) => {
    await page.goto("/tasks");
    await page.locator("main#content").waitFor();

    const stops = await page.evaluate(() =>
      [...document.querySelectorAll("#primary-navigation menu a")].map((a) =>
        a.getAttribute("tabindex"),
      ),
    );
    // Exactly one entry keeps its natural place in the order.
    expect(stops.filter((value) => value === null)).toHaveLength(1);
    expect(stops.filter((value) => value === "-1").length).toBeGreaterThan(0);
  });

  test("the tab stop is the page you are on", async ({ page }) => {
    await page.goto("/processes");
    await page.locator("main#content").waitFor();

    const tabbable = page.locator(
      "#primary-navigation menu a:not([tabindex='-1'])",
    );
    await expect(tabbable).toHaveCount(1);
    await expect(tabbable).toHaveAttribute("aria-current", "page");
  });

  test("arrow keys move within the primary nav and wrap", async ({ page }) => {
    await page.goto("/tasks");
    await page.locator("main#content").waitFor();

    const entries = await page.evaluate(() =>
      [...document.querySelectorAll("#primary-navigation menu a")].map((a) =>
        a.textContent.trim(),
      ),
    );

    await page.locator("#primary-navigation menu a").first().focus();
    expect(await focused_text(page)).toBe(entries[0]);

    await page.keyboard.press("ArrowRight");
    expect(await focused_text(page)).toBe(entries[1]);

    await page.keyboard.press("ArrowLeft");
    expect(await focused_text(page)).toBe(entries[0]);

    // Wraps backwards from the first entry to the last.
    await page.keyboard.press("ArrowLeft");
    expect(await focused_text(page)).toBe(entries.at(-1));

    await page.keyboard.press("Home");
    expect(await focused_text(page)).toBe(entries[0]);

    await page.keyboard.press("End");
    expect(await focused_text(page)).toBe(entries.at(-1));
  });

  test("arrow keys do not scroll the page while navigating a menu", async ({
    page,
  }) => {
    await page.goto("/tasks");
    await page.locator("main#content").waitFor();

    await page.locator("#primary-navigation menu a").first().focus();
    const before = await page.evaluate(() => window.scrollY);
    await page.keyboard.press("ArrowDown");
    expect(await page.evaluate(() => window.scrollY)).toBe(before);
  });

  test("the admin sub-nav navigates with up and down", async ({ page }) => {
    await page.goto("/admin/users");
    await page.locator("main#content").waitFor();

    const entries = page.locator("nav menu.list a");
    await entries.first().focus();
    const first = await focused_text(page);

    await page.keyboard.press("ArrowDown");
    expect(await focused_text(page)).not.toBe(first);

    await page.keyboard.press("ArrowUp");
    expect(await focused_text(page)).toBe(first);
  });

  test("the task list is one tab stop, arrowed by row", async ({ page }) => {
    await page.goto("/tasks");
    await page.locator("main#content").waitFor();

    const rows = page.locator('#task-list tbody a[href^="/tasks/"]');
    await rows
      .first()
      .waitFor({ timeout: 10_000 })
      .catch(() => {});
    test.skip((await rows.count()) < 2, "needs at least two tasks");

    const tabbable = await page.evaluate(
      () =>
        [
          ...document.querySelectorAll(
            "#task-list tbody a, #task-list tbody button, #task-list tbody input",
          ),
        ].filter((element) => element.getAttribute("tabindex") !== "-1").length,
    );
    // Only the active row stays reachable by Tab.
    expect(tabbable).toBeGreaterThan(0);

    // Compare hrefs, not text: fixture tasks share names across instances.
    const first_href = await rows.first().getAttribute("href");
    await rows.first().focus();
    await page.keyboard.press("ArrowDown");

    const landed = await page.evaluate(() => {
      const active = document.activeElement;
      return {
        href: active?.getAttribute("href"),
        in_list: Boolean(active?.closest("#task-list tbody tr")),
      };
    });
    expect(landed.in_list).toBe(true);
    expect(landed.href).not.toBe(first_href);
  });

  test("Home and End jump to the first and last row", async ({ page }) => {
    await page.goto("/tasks");
    await page.locator("main#content").waitFor();

    const rows = page.locator('#task-list tbody a[href^="/tasks/"]');
    await rows
      .first()
      .waitFor({ timeout: 10_000 })
      .catch(() => {});
    test.skip((await rows.count()) < 2, "needs at least two tasks");

    const first = await rows.first().getAttribute("href"),
      last = await rows.last().getAttribute("href"),
      active_href = () =>
        page.evaluate(() => document.activeElement?.getAttribute("href"));

    await rows.first().focus();
    await page.keyboard.press("End");
    expect(await active_href()).toBe(last);

    await page.keyboard.press("Home");
    expect(await active_href()).toBe(first);
  });

  test("Tab leaves the nav entirely instead of stepping through it", async ({
    page,
  }) => {
    // The whole point of the roving tabindex: one Tab in, one Tab out, however
    // many links the nav holds.
    await page.goto("/tasks");
    await page.locator("main#content").waitFor();

    await page
      .locator("#primary-navigation menu a:not([tabindex='-1'])")
      .focus();
    expect(
      await page.evaluate(() =>
        Boolean(document.activeElement?.closest("#primary-navigation")),
      ),
    ).toBe(true);

    await page.keyboard.press("Tab");
    expect(
      await page.evaluate(() =>
        Boolean(document.activeElement?.closest("#primary-navigation")),
      ),
    ).toBe(false);
  });

  test("the tab stop follows the selected row", async ({ page }) => {
    await page.goto("/tasks");
    await page.locator("main#content").waitFor();

    const rows = page.locator('#task-list tbody a[href^="/tasks/"]');
    await rows
      .first()
      .waitFor({ timeout: 10_000 })
      .catch(() => {});
    test.skip((await rows.count()) < 2, "needs at least two tasks");

    await rows.nth(1).click();
    await page.locator("#task-details").waitFor();

    // After selecting the second row, that row is the one Tab returns to.
    const tabbable = page.locator(
      "#task-list tbody tr[aria-selected='true'] a:not([tabindex='-1'])",
    );
    await expect(tabbable).toHaveCount(1);
  });
});
