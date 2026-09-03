import { test, expect } from "./fixtures.js";
import { prepare_page } from "./a11y-scan.js";
import {
  focused,
  focus_is_within,
  tab_until_within,
  wait_for_announcement,
} from "./a11y-keyboard.js";

// Requirements TW-3..TW-9 of docs/Accessibility Requirements.md — working on a
// task with the keyboard, and being told what happened.
//
// TW-1 (the task list as one tab stop) and TW-2 (selection moves the reading
// position) are already carried by arrow-navigation.spec.js and focus.spec.js;
// they are not repeated here.
//
// Needs the engine on :8084 holding at least one user task.

// Not parallel: the completion test consumes a task, and the dialog tests walk
// the same detail pane. `fullyParallel` in the config would otherwise run them
// against each other.
test.describe.configure({ mode: "default" });

const TASK_ROWS = '#task-list tbody a[href^="/tasks/"]';

/** Open the first task's detail pane, or skip when the engine holds no tasks. */
const open_a_task = async (page) => {
  await prepare_page(page, { path: "/tasks" });
  const rows = page.locator(TASK_ROWS);
  await rows
    .first()
    .waitFor({ timeout: 15_000 })
    .catch(() => {});
  test.skip((await rows.count()) === 0, "engine holds no user tasks");

  const name = (await rows.first().textContent())?.trim();
  await rows.first().focus();
  await page.keyboard.press("Enter");
  await page.locator("#task-details").waitFor({ timeout: 15_000 });
  return name;
};

test.describe("working on a task — keyboard and announcements", () => {
  test.slow();

  // TW-3, TW-4
  test("the detail tab strip is one tab stop whose panel is named by the selected tab", async ({
    page,
  }) => {
    await open_a_task(page);

    const tablist = page.getByRole("tablist");
    const appeared = await tablist
      .waitFor({ state: "visible", timeout: 15_000 })
      .then(() => true)
      .catch(() => false);
    test.skip(!appeared, "this task renders no tabbed detail");

    // One stop: every tab but the selected one is out of the tab order.
    const tabbable = await page.evaluate(
      () =>
        [...document.querySelectorAll('[role="tablist"] [role="tab"]')].filter(
          (tab) => tab.getAttribute("tabindex") !== "-1",
        ).length,
    );
    expect(tabbable, "exactly one tab should keep its tab stop").toBe(1);

    // Arrow keys move the selection with the focus, per the APG tab pattern.
    const selected = tablist.getByRole("tab", { selected: true });
    await selected.focus();
    const before = await focused(page);
    await page.keyboard.press("ArrowRight");
    const after = await focused(page);

    expect(after.role).toBe("tab");
    expect(after.text, "ArrowRight should move to another tab").not.toBe(
      before.text,
    );
    await expect(page.locator(":focus")).toHaveAttribute(
      "aria-selected",
      "true",
    );

    // TW-4: the panel is reachable and carries the name of the tab that
    // selected it, so a screen reader reads the two as a pair.
    const panel = page.getByRole("tabpanel");
    await expect(panel).toBeVisible();
    const labelled_by = await panel.getAttribute("aria-labelledby");
    expect(labelled_by, "the tab panel must be labelled").toBeTruthy();
    // An attribute selector, not `#id`: tab ids are built from a route
    // parameter and routinely hold characters a CSS id selector cannot carry.
    await expect(
      page.locator(`[id="${labelled_by}"]`),
    ).toHaveAttribute("aria-selected", "true");
  });

  // TW-5, TW-6, TW-7 — every dialog the detail pane offers, walked in turn.
  test("every task dialog opens from the keyboard, is named, and Escape returns focus to its trigger", async ({
    page,
  }) => {
    await open_a_task(page);

    const triggers = page.locator("#task-details button.task-card");
    const count = await triggers.count();
    test.skip(count === 0, "this task detail offers no dialogs");

    for (let index = 0; index < count; index++) {
      const trigger = triggers.nth(index);
      const label = (await trigger.textContent())?.trim().slice(0, 40);

      // TW-5: reached and activated with the keyboard, never clicked.
      await trigger.focus();
      const trigger_before = await focused(page);
      await page.keyboard.press("Enter");

      const dialog = page.locator("dialog[open]");
      const opened = await dialog
        .first()
        .waitFor({ state: "visible", timeout: 5_000 })
        .then(() => true)
        .catch(() => false);
      if (!opened) continue; // Not every card opens a dialog.

      // TW-7: a modal with no accessible name is an unnamed region a screen
      // reader drops the user into.
      const name = await dialog.first().evaluate((element) => {
        const labelled = element.getAttribute("aria-labelledby");
        return (
          element.getAttribute("aria-label") ||
          (labelled && document.getElementById(labelled)?.textContent) ||
          ""
        ).trim();
      });
      expect(name, `dialog opened by "${label}" has no accessible name`).not.toBe(
        "",
      );

      // Opening a modal must move the reading position into it.
      expect(
        await focus_is_within(page, "dialog[open]"),
        `focus stayed outside the dialog opened by "${label}"`,
      ).toBe(true);

      // TW-6: Escape closes it, and focus comes back where it started.
      await page.keyboard.press("Escape");
      await expect(page.locator("dialog[open]")).toHaveCount(0);

      const trigger_after = await focused(page);
      expect(
        trigger_after.text,
        `Escape from the dialog opened by "${label}" left focus on ${trigger_after.tag} "${trigger_after.text}" instead of its trigger`,
      ).toBe(trigger_before.text);
    }
  });

  // TW-8, TW-9 — the outcome of the one action the whole page exists for.
  test("completing a task announces the outcome and leaves focus on the task list heading", async ({
    page,
  }) => {
    await prepare_page(page, { path: "/tasks" });
    const rows = page.locator(TASK_ROWS);
    await rows
      .first()
      .waitFor({ timeout: 15_000 })
      .catch(() => {});
    const hrefs = await rows.evaluateAll((links) =>
      links.map((link) => link.getAttribute("href")),
    );
    test.skip(hrefs.length === 0, "engine holds no user tasks");

    // Not every task can be completed: one carrying a legacy AngularJS form
    // renders a migration notice and no controls at all. Walk the list until a
    // task offers a completion control, rather than testing whichever task
    // happens to sort first.
    //
    // This search navigates by URL rather than by keyboard on purpose — it is
    // setup, and TW-2 already covers selecting a task from the list. From the
    // point a completable task is open, every step below is keyboard-only,
    // because that is what this requirement is about.
    const complete = page.getByRole("button", {
      name: /complete( task| without form)?/i,
    });
    let task_name = null;
    for (const href of hrefs) {
      await prepare_page(page, { path: href, ready: "#task-details" });
      const offered = await complete
        .first()
        .waitFor({ state: "visible", timeout: 5_000 })
        .then(() => true)
        .catch(() => false);
      if (offered) {
        task_name = (await page.locator("#task-details h2").first().textContent())?.trim();
        break;
      }
    }
    test.skip(
      task_name === null,
      `none of the ${hrefs.length} tasks offers a completion control`,
    );

    // Reached by tabbing, so a control only a mouse can get to fails here.
    const reached = await tab_until_within(page, ".form-buttons button");
    expect(
      reached,
      "the completion control was never reached by pressing Tab",
    ).not.toBeNull();
    await page.keyboard.press("Enter");

    // A form with required fields rejects a bare submit; that is requirement
    // SP-9's territory, not a failure of this one.
    const validation = page.locator('.task-form [role="alert"]');
    if (await validation.count()) {
      test.skip(true, `task "${task_name}" needs form input to complete`);
    }

    // TW-8
    await wait_for_announcement(page, /completed/i);
    await expect(page).toHaveURL(/\/tasks$/);

    // TW-9
    const where = await focused(page);
    expect(
      where.tag,
      "focus fell back to <body>; the next Tab press restarts at the top of the document",
    ).not.toBe("BODY");
    expect(where.tag).toBe("H1");
  });
});
