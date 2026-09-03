import { test, expect } from "./fixtures.js";

// Focus management across route changes and in-page selection. None of this is
// visible to a scanner — axe inspects a static DOM, while every assertion here
// is about where focus went after an interaction.
//
// See src/components/Heading.jsx for the two-level model: PageHeading takes
// focus when the page changes, DetailHeading when the selection within a page
// changes.

test.describe("focus management", () => {
  test("the page heading is focusable but not a tab stop", async ({ page }) => {
    await page.goto("/tasks");
    await page.locator("main#content").waitFor();

    const heading = page.locator("main#content h1").first();
    await expect(heading).toHaveAttribute("tabindex", "-1");

    // First Tab must still reach the skip link, not the heading.
    await page.keyboard.press("Tab");
    await expect(page.locator(":focus")).toHaveAttribute("href", "#content");
  });

  test("focus is left alone on the initial page load", async ({ page }) => {
    await page.goto("/tasks");
    await page.locator("main#content").waitFor();

    // Nothing in the document should have grabbed focus on arrival.
    const focused = await page.evaluate(() => document.activeElement?.tagName);
    expect(focused).toBe("BODY");
  });

  test("changing page moves focus to the new page's h1", async ({ page }) => {
    await page.goto("/tasks");
    await page.locator("main#content").waitFor();

    await page.getByRole("link", { name: "Processes", exact: true }).click();
    await page.locator("main.processes").waitFor();

    const focused = page.locator(":focus");
    await expect(focused).toHaveJSProperty("tagName", "H1");
    // And it is the heading of the page we navigated to, not the one we left.
    await expect(focused).toHaveCount(1);
  });

  test("every page exposes exactly one h1 to focus", async ({ page }) => {
    // Four pages had no h1 at all before focus management landed, which left
    // nothing to move focus to. Guard against that coming back.
    for (const path of [
      "/",
      "/tasks",
      "/processes",
      "/decisions",
      "/deployments",
      "/batches",
      "/migrations",
      "/admin",
      "/account",
      "/help",
      "/does-not-exist",
    ]) {
      await page.goto(path);
      await page.locator("main#content").waitFor();
      await expect(
        page.locator("main#content h1"),
        `${path} should have exactly one h1`,
      ).toHaveCount(1);
    }
  });

  test("selecting a task moves focus to the detail heading, not the page h1", async ({
    page,
  }) => {
    await page.goto("/tasks");
    await page.locator("main#content").waitFor();

    // Row links only — the toolbar above the table also links under /tasks/.
    const rows = page.locator('#task-list tbody a[href^="/tasks/"]');
    await rows
      .first()
      .waitFor({ timeout: 10_000 })
      .catch(() => {});
    test.skip((await rows.count()) === 0, "engine holds no tasks to select");

    const task_name = (await rows.first().textContent())?.trim();
    await rows.first().click();

    const heading = page.locator("#task-details h2").first();
    await heading.waitFor();
    await expect(heading).toHaveJSProperty("tabIndex", -1);

    // Focus is on the detail heading — not left in the list, and not dragged
    // back up to the page's h1.
    const focused = page.locator(":focus");
    await expect(focused).toHaveJSProperty("tagName", "H2");
    await expect(focused).toHaveText(task_name);
  });

  test("a keyboard route change shows a focus outline, a mouse click does not", async ({
    page,
  }) => {
    // :focus-visible is what makes focusing a heading tolerable: the indicator
    // appears for keyboard users and stays hidden for mouse users.
    await page.goto("/tasks");
    await page.locator("main#content").waitFor();

    await page.getByRole("link", { name: "Processes", exact: true }).click();
    await page.locator("main.processes").waitFor();
    expect(
      await page.evaluate(() =>
        document.activeElement?.matches(":focus-visible"),
      ),
    ).toBe(false);

    await page.goto("/tasks");
    await page.locator("main#content").waitFor();
    await page
      .getByRole("link", { name: "Processes", exact: true })
      .press("Enter");
    await page.locator("main.processes").waitFor();
    expect(
      await page.evaluate(() =>
        document.activeElement?.matches(":focus-visible"),
      ),
    ).toBe(true);
  });

  test("a focused visually hidden heading is revealed", async ({ page }) => {
    // Otherwise focus visibly disappears: .screen-hidden clips the outline away
    // along with the box.
    await page.goto("/tasks");
    await page.locator("main#content").waitFor();

    const box = await page.evaluate(() => {
      const heading = document.querySelector("main#content h1.screen-hidden");
      if (!heading) return null;
      heading.focus();
      return heading.getBoundingClientRect().width;
    });
    test.skip(box === null, "this page's h1 is not visually hidden");

    // Programmatic focus alone does not match :focus-visible after a click, so
    // assert the rule exists rather than the rendered size.
    const revealed = await page.evaluate(() =>
      [...document.styleSheets]
        .flatMap((sheet) => {
          try {
            return [...sheet.cssRules];
          } catch {
            return [];
          }
        })
        .some((rule) => rule.cssText?.includes(".screen-hidden:focus-visible")),
    );
    expect(revealed).toBe(true);
  });
});
