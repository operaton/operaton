// Primitives for driving the app the way somebody with no pointing device
// does, and for reading back what a screen reader would have been told.
//
// The distinction that matters: `page.click()` and `locator.fill()` are not
// keyboard operation. They dispatch the event a mouse or a script would, and
// pass on controls no keyboard user can reach. Everything here goes through
// `page.keyboard`, so a test written with it fails when a step becomes
// mouse-only — which is exactly the failure the specs are looking for.
//
// Used by the journey specs (a11y-start-process, a11y-task-work,
// a11y-global-search, a11y-login). The attribute-level specs (keyboard,
// focus, arrow-navigation) predate it and assert narrower things directly.

/** A short description of whatever currently has focus, for assertions and messages. */
export const focused = (page) =>
  page.evaluate(() => {
    const element = document.activeElement;
    if (!element || element === document.body)
      return { tag: "BODY", text: "", id: null, role: null };
    return {
      tag: element.tagName,
      id: element.id || null,
      role: element.getAttribute("role"),
      text: (
        element.textContent ||
        element.value ||
        element.getAttribute("aria-label") ||
        ""
      )
        .trim()
        .slice(0, 80),
    };
  });

/** Whether focus is currently inside `selector`. */
export const focus_is_within = (page, selector) =>
  page.evaluate(
    (target) => Boolean(document.activeElement?.closest(target)),
    selector,
  );

/**
 * Press Tab until focus lands inside `selector`, and report how many presses it
 * took. Returns `null` if it never got there within `max`.
 *
 * The count is the point: "one Tab press reaches the list" and "seven Tab
 * presses get past it" are the same DOM to a scanner and a different product to
 * a keyboard user.
 */
export const tab_until_within = async (page, selector, { max = 40 } = {}) => {
  for (let presses = 1; presses <= max; presses++) {
    await page.keyboard.press("Tab");
    if (await focus_is_within(page, selector)) return presses;
  }
  return null;
};

/**
 * How many Tab presses land inside `selector` while crossing it.
 *
 * Starts from wherever focus already is, walks forward until it has entered and
 * then left the region, and counts the stops inside. A roving tabindex is
 * working when this is 1, however many entries the region holds.
 */
export const tab_stops_within = async (page, selector, { max = 60 } = {}) => {
  let entered = false,
    stops = 0;
  for (let i = 0; i < max; i++) {
    await page.keyboard.press("Tab");
    const inside = await focus_is_within(page, selector);
    if (inside) {
      entered = true;
      stops++;
    } else if (entered) {
      return stops;
    }
  }
  return entered ? stops : null;
};

/** Move focus to the very start of the document, so a walk is reproducible. */
export const focus_document_start = async (page) => {
  await page.evaluate(() => {
    document.activeElement?.blur();
    // Chromium resumes tabbing from the last focused element even after blur;
    // focusing the body resets the sequential navigation starting point.
    document.body.focus();
  });
};

/**
 * Everything a polite/assertive live region currently holds, plus anything with
 * an alert role — i.e. the text a screen reader has been handed without the
 * user asking for it.
 */
export const announcements = (page) =>
  page.evaluate(() =>
    [
      ...document.querySelectorAll("[aria-live], [role=status], [role=alert]"),
    ]
      .map((element) => element.textContent.trim())
      .filter(Boolean),
  );

/**
 * Wait until a live region says something matching `pattern`, and return it.
 *
 * Polled rather than awaited on a locator: the app's announcer alternates
 * between two regions (so a repeated message still counts as a change), and
 * which of the two holds the text is an implementation detail no spec should
 * encode.
 */
export const wait_for_announcement = async (
  page,
  pattern,
  { timeout = 10_000 } = {},
) => {
  const deadline = Date.now() + timeout;
  let seen = [];
  while (Date.now() < deadline) {
    seen = await announcements(page);
    const match = seen.find((text) => pattern.test(text));
    if (match) return match;
    await page.waitForTimeout(100);
  }
  throw new Error(
    `No announcement matched ${pattern}. Live regions held: ${
      seen.length ? JSON.stringify(seen) : "nothing"
    }`,
  );
};

/**
 * The element `aria-activedescendant` points at, resolved. Returns `null` when
 * the attribute is absent and `{ missing: true }` when it names an id that is
 * not in the document — the failure mode that leaves a combobox silent.
 */
export const active_descendant = (page, combobox_selector) =>
  page.evaluate((selector) => {
    const combobox = document.querySelector(selector);
    const id = combobox?.getAttribute("aria-activedescendant");
    if (!id) return null;
    const option = document.getElementById(id);
    if (!option) return { id, missing: true };
    return {
      id,
      missing: false,
      selected: option.getAttribute("aria-selected") === "true",
      text: option.textContent.trim(),
    };
  }, combobox_selector);
