// Putting a page into a scannable state — shared by the gating spec
// (a11y.spec.js) and the report generator (a11y-report.mjs) so the readiness
// wait and the animation freeze exist in exactly one place.

// Freeze fade-in animations/transitions at their end state. Without this, axe
// can sample a mid-animation (reduced-opacity) frame and report a phantom
// contrast failure — the single most common source of flaky a11y results.
const FREEZE_ANIMATIONS =
  "*,*::before,*::after{animation-duration:0s!important;animation-delay:0s!important;transition:none!important}";

/** The landmark every authenticated page renders. The login screen has none. */
export const DEFAULT_READY = "main";

/**
 * Navigate to `path`, wait for the page to be ready, and freeze animations.
 *
 * @param page   Playwright page
 * @param path   app-relative path
 * @param ready  CSS selector that signals the page has rendered
 * @param timeout ms to wait for `ready` (heavy routes compile on demand)
 */
export const prepare_page = async (
  page,
  { path, ready = DEFAULT_READY, timeout = 30_000 } = {},
) => {
  await page.goto(path, { waitUntil: "domcontentloaded" });
  await page.locator(ready).first().waitFor({ timeout });
  await page.addStyleTag({ content: FREEZE_ANIMATIONS });
};

/** Re-apply the freeze after an interaction has injected new DOM. */
export const freeze_animations = (page) =>
  page.addStyleTag({ content: FREEZE_ANIMATIONS });
