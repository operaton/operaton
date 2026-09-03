import { test, expect } from "./fixtures.js";
import { prepare_page } from "./a11y-scan.js";
import { focused, active_descendant } from "./a11y-keyboard.js";

// Requirements GS-1..GS-6 of docs/Accessibility Requirements.md — the global
// search dialog, driven entirely from the keyboard.
//
// A combobox is the control least forgiving of a half-implemented ARIA
// contract. The markup can carry every attribute a scanner looks for and still
// say nothing: `aria-activedescendant` announces the active option only while
// it genuinely tracks the arrow keys, and announces nothing at all when it
// names an element that is not in the document. Neither failure is visible to
// axe, because at rest the attribute is simply present.

const TRIGGER = "#go-to";
const DIALOG = "dialog#global-search";
const COMBOBOX = ".goto-input";

const open_search = async (page) => {
  await prepare_page(page, { path: "/tasks" });
  await page.locator(TRIGGER).focus();
  await page.keyboard.press("Enter");
  await expect(page.locator(DIALOG)).toBeVisible();
};

/**
 * Type a query and wait for the results it should produce. `min` is how many
 * options the test needs: arrowing between options needs at least two, while
 * activating one needs only one.
 */
const search_for = async (page, query, { min = 1 } = {}) => {
  await page.keyboard.type(query);
  await expect
    .poll(() => page.locator('[role="option"]').count(), { timeout: 10_000 })
    .toBeGreaterThanOrEqual(min);
};

test.describe("global search — keyboard and combobox semantics", () => {
  test.slow();

  // GS-1
  test("opens from the keyboard and puts focus in the search field", async ({
    page,
  }) => {
    await prepare_page(page, { path: "/tasks" });

    // From its trigger…
    await page.locator(TRIGGER).focus();
    await page.keyboard.press("Enter");
    await expect(page.locator(DIALOG)).toBeVisible();
    await expect(page.locator(COMBOBOX)).toBeFocused();
    await page.keyboard.press("Escape");
    await expect(page.locator(DIALOG)).toBeHidden();

    // …and from the shortcut, which is the only route a user who never sees the
    // header button has.
    await page.keyboard.press("Alt+KeyK");
    await expect(page.locator(DIALOG)).toBeVisible();
    await expect(page.locator(COMBOBOX)).toBeFocused();
  });

  // GS-2
  test("the field is a combobox that owns its listbox and reports expansion", async ({
    page,
  }) => {
    await open_search(page);
    const combobox = page.locator(COMBOBOX);

    await expect(combobox).toHaveRole("combobox");
    await expect(combobox).toHaveAccessibleName(/.+/);
    // Collapsed until there is something to expand into.
    await expect(combobox).toHaveAttribute("aria-expanded", "false");

    const controls = await combobox.getAttribute("aria-controls");
    expect(controls, "the combobox must own a listbox").toBeTruthy();
    await expect(page.locator(`#${controls}`)).toHaveRole("listbox");

    await search_for(page, "a");
    await expect(combobox).toHaveAttribute("aria-expanded", "true");
  });

  // GS-3 — the requirement the whole pattern rests on.
  test("arrow keys move the active option and aria-activedescendant follows them", async ({
    page,
  }) => {
    await open_search(page);
    await search_for(page, "a", { min: 2 });

    const first = await active_descendant(page, COMBOBOX);
    expect(first, "no active option before any arrow key").not.toBeNull();
    expect(
      first.missing,
      `aria-activedescendant names "${first.id}", which is not in the document — a screen reader announces nothing`,
    ).toBe(false);
    expect(first.selected).toBe(true);

    await page.keyboard.press("ArrowDown");
    const second = await active_descendant(page, COMBOBOX);
    expect(second.missing).toBe(false);
    expect(
      second.id,
      "ArrowDown did not move the active option",
    ).not.toBe(first.id);
    expect(
      second.selected,
      "the active option must also be the aria-selected one",
    ).toBe(true);

    // Exactly one option claims selection at a time.
    await expect(page.locator('[role="option"][aria-selected="true"]')).toHaveCount(
      1,
    );

    await page.keyboard.press("ArrowUp");
    expect((await active_descendant(page, COMBOBOX)).id).toBe(first.id);
  });

  // GS-4
  test("Enter navigates to the active option", async ({ page }) => {
    await open_search(page);
    await search_for(page, "process");

    const target = await active_descendant(page, COMBOBOX);
    await page.keyboard.press("Enter");

    await expect(page.locator(DIALOG)).toBeHidden();
    // The active option was a page entry, so the app navigated to it.
    await expect(page).not.toHaveURL(/\/tasks$/);
    expect(target.text.length, "the active option should have had a name").toBeGreaterThan(
      0,
    );
  });

  // GS-5 — the regression that matters: an <input type="search"> swallows the
  // first Escape to clear itself, so before this was handled the dialog closed
  // on Escape only while the field was empty. Anyone who had actually searched
  // was trapped in a modal with no keyboard way out.
  test("Escape closes the search however much has been typed, and returns focus to the trigger", async ({
    page,
  }) => {
    await open_search(page);
    await search_for(page, "process");

    await page.keyboard.press("Escape");
    await expect(page.locator(DIALOG)).toBeHidden();

    const where = await focused(page);
    expect(
      where.id,
      `focus went to ${where.tag} "${where.text}" instead of back to the control that opened the dialog`,
    ).toBe("go-to");
  });

  // GS-6
  test("result groups are labelled by their category heading", async ({
    page,
  }) => {
    await open_search(page);
    await search_for(page, "a");

    const unlabelled = await page.evaluate(() =>
      [...document.querySelectorAll('#goto-listbox [role="group"]')]
        .filter((group) => {
          const id = group.getAttribute("aria-labelledby");
          return !id || !document.getElementById(id)?.textContent?.trim();
        })
        .map((group) => group.outerHTML.slice(0, 60)),
    );
    expect(unlabelled, "result groups with no accessible name").toEqual([]);
  });
});
