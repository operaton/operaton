import { test, expect } from "./fixtures.js";
import { expect_no_a11y_violations } from "./a11y.js";
import { prepare_page, freeze_animations } from "./a11y-scan.js";
import {
  focused,
  tab_stops_within,
  tab_until_within,
  focus_document_start,
  wait_for_announcement,
} from "./a11y-keyboard.js";

// Requirements SP-1..SP-8 of docs/Accessibility Requirements.md — starting a
// process, performed the way a keyboard or screen-reader user performs it.
//
// The route was scanned here long before it was driven, and the scan was green
// the whole time: choosing a definition announced nothing, starting an instance
// announced nothing, and the focus left behind went to <body>. None of that is
// visible to axe, because none of it is a property of the DOM at rest. It only
// appears when something actually starts a process.
//
// Needs the engine on :8084 with at least one startable definition
// (docker compose -f docker-compose.dev-fixtures.yaml up -d, or
// podman compose -f docker-compose.dev-fixtures.yaml up -d).

const DEFINITIONS = "#start-task .definitions tbody";
const definition_links = (page) => page.locator(`${DEFINITIONS} a`);

// Every test starts from a rendered list, so skip once, here, rather than
// repeating the guard in each.
const open_start_page = async (page) => {
  await prepare_page(page, { path: "/tasks/start" });
  const rows = definition_links(page);
  await rows
    .first()
    .waitFor({ timeout: 15_000 })
    .catch(() => {});
  test.skip(
    (await rows.count()) === 0,
    "engine holds no startable process definitions",
  );
  return rows;
};

test.describe("starting a process — keyboard and announcements", () => {
  test.slow();

  // SP-1
  test("the definition list costs one tab stop, not one per definition", async ({
    page,
  }) => {
    const rows = await open_start_page(page);
    const definitions = await rows.count();
    expect(
      definitions,
      "this assertion is only meaningful with several definitions",
    ).toBeGreaterThan(1);

    await focus_document_start(page);
    const stops = await tab_stops_within(page, DEFINITIONS);

    expect(
      stops,
      `Tab should cross the definition list in one stop; it took ${stops} for ${definitions} definitions`,
    ).toBe(1);
  });

  // SP-2
  test("arrow keys move between definitions, Home and End reach the ends, and the page does not scroll", async ({
    page,
  }) => {
    const rows = await open_start_page(page);
    test.skip((await rows.count()) < 2, "needs at least two definitions");

    // Compare hrefs, not names: two versions of a definition share a name.
    const href = () =>
      page.evaluate(() => document.activeElement?.getAttribute("href"));
    const first = await rows.first().getAttribute("href"),
      last = await rows.last().getAttribute("href");

    await rows.first().focus();
    const scroll_before = await page.evaluate(() => window.scrollY);

    await page.keyboard.press("ArrowDown");
    expect(await href()).not.toBe(first);

    await page.keyboard.press("ArrowUp");
    expect(await href()).toBe(first);

    await page.keyboard.press("End");
    expect(await href()).toBe(last);

    await page.keyboard.press("Home");
    expect(await href()).toBe(first);

    // Arrowing a list must not also scroll the document out from under it.
    expect(await page.evaluate(() => window.scrollY)).toBe(scroll_before);
  });

  // SP-3
  test("choosing a definition moves the reading position to the start form heading", async ({
    page,
  }) => {
    const rows = await open_start_page(page);
    const chosen = (await rows.first().textContent())?.trim();

    await rows.first().focus();
    await page.keyboard.press("Enter");

    // The heading is filled from a fetch, so it takes focus on the render that
    // finally has text — wait for that rather than for the navigation.
    await expect(page.locator("#start-task .start-form h2")).toHaveText(
      chosen,
      { timeout: 15_000 },
    );

    const where = await focused(page);
    expect(
      where.tag,
      `focus stayed on ${where.tag} "${where.text}" instead of moving to the start form`,
    ).toBe("H2");
    // It names the process chosen, so the announcement is useful rather than
    // merely present.
    expect(where.text).toBe(chosen);
  });

  // SP-4
  test("every start-form control has an accessible name", async ({ page }) => {
    const rows = await open_start_page(page);
    await rows.first().focus();
    await page.keyboard.press("Enter");
    await page.locator("#start-task .start-form .task-form").waitFor({
      timeout: 15_000,
    });

    const unnamed = await page.evaluate(() => {
      const controls = [
        ...document.querySelectorAll(
          "#start-task .start-form input, #start-task .start-form select, #start-task .start-form textarea, #start-task .start-form button",
        ),
      ];
      return controls
        .filter((control) => {
          const name =
            control.labels?.[0]?.textContent ||
            control.getAttribute("aria-label") ||
            control.textContent;
          return !name?.trim();
        })
        .map((control) => `${control.tagName}[${control.type ?? ""}]`);
    });

    expect(unnamed, "start-form controls with no accessible name").toEqual([]);
  });

  // SP-5, SP-6, SP-7 — one journey, because they are one journey.
  test("the whole flow runs on the keyboard alone, announces the outcome, and leaves focus somewhere", async ({
    page,
  }) => {
    const rows = await open_start_page(page);
    const chosen = (await rows.first().textContent())?.trim();

    // SP-5: reach the list by tabbing, not by focusing a selector.
    await focus_document_start(page);
    const presses = await tab_until_within(page, DEFINITIONS);
    expect(
      presses,
      "the definition list was never reached by pressing Tab",
    ).not.toBeNull();

    await page.keyboard.press("Enter");
    await expect(page.locator("#start-task .start-form h2")).toHaveText(
      chosen,
      { timeout: 15_000 },
    );

    // Tab on from the heading to the start control and activate it. Reaching it
    // this way is the requirement: a button only a mouse can get to would fail
    // here while passing every scan.
    const start = page.getByRole("button", { name: /start process/i });
    await start.waitFor({ timeout: 15_000 });
    const reached = await tab_until_within(page, ".form-buttons button");
    expect(reached, "the start button was never reached by pressing Tab").not.toBeNull();

    await page.keyboard.press("Enter");

    // SP-6: the outcome reaches a live region. This is the whole point — the
    // view that could have shown it is being unmounted by the route change.
    const announced = await wait_for_announcement(page, /started/i);
    expect(
      announced,
      "the announcement should name the process that was started",
    ).toContain(chosen);

    await expect(page).toHaveURL(/\/tasks$/);

    // SP-7: focus is somewhere a screen reader can read and Tab can continue
    // from — not dumped back on <body>.
    const where = await focused(page);
    expect(
      where.tag,
      "focus fell back to <body>; the next Tab press restarts at the top of the document",
    ).not.toBe("BODY");
    expect(where.tag).toBe("H1");
  });

  // SP-8 — the gate scans this route with nothing selected. The state a user
  // actually spends their time in is the one with a form in it.
  test("the start form state has no WCAG A/AA violations", async ({ page }) => {
    const rows = await open_start_page(page);
    await rows.first().focus();
    await page.keyboard.press("Enter");
    await page.locator("#start-task .start-form .task-form").waitFor({
      timeout: 15_000,
    });
    await freeze_animations(page);

    await expect_no_a11y_violations(page);
  });
});
