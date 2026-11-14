import { test, expect, login_via_form } from "./fixtures.js";

// Three different mechanisms paint colours while rendering, so none of them can
// be verified by reading a token:
//
//   BPMN  bpmn-js writes fill/stroke/label inline. BPMNViewer reads the
//         --canvas-* tokens and rebuilds the viewer on a theme change.
//   DMN   dmn-js's DRD renderer also writes "fill: white" inline; only the
//         !important override in components.css reaches it.
//   Logo  the bundled SVG recolours itself from prefers-color-scheme, so
//         Logo.jsx paints it through a mask with --text instead.
//
// Each case therefore reads the colour the browser actually painted, and each
// one flips the OS preference against the chosen theme so that "it only works
// because my machine is dark" cannot pass.

const rgb_of = (page, selector) =>
  page.evaluate((sel) => {
    const el = document.querySelector(sel);
    if (!el) return null;
    const raw = getComputedStyle(el).fill;
    // Chromium resolves modern colour syntax (oklch(…)) verbatim, so round-trip
    // through a 1x1 canvas to always get a comparable rgb() string.
    const ctx = document.createElement("canvas").getContext("2d");
    ctx.fillStyle = raw;
    ctx.fillRect(0, 0, 1, 1);
    const [r, g, b] = ctx.getImageData(0, 0, 1, 1).data;
    return `rgb(${r}, ${g}, ${b})`;
  }, selector);

// Perceived brightness of an "rgb(r, g, b)" string, 0-255.
const brightness = (rgb) => {
  const [r, g, b] = rgb.match(/\d+/g).map(Number);
  return 0.299 * r + 0.587 * g + 0.114 * b;
};

const BPMN_SHAPE = ".djs-shape .djs-visual > rect";
const DRD_SHAPE = ".dmn-drd-container .djs-shape .djs-visual > rect";

// An explicit preference, so what is asserted is the chosen theme rather than
// the auto-mode guess.
const choose_theme = (page, theme) =>
  page.evaluate((t) => localStorage.setItem("theme", t), theme);

const wait_for = (page, selector, ms = 20_000) =>
  page
    .waitForSelector(selector, { timeout: ms })
    .then(() => true)
    .catch(() => false);

// Navigates, logging in first if the session has not been established yet.
const goto_signed_in = async (page, path) => {
  await page.goto(path);
  if (await page.getByLabel(/user name/i).count()) {
    await login_via_form(page);
    await page.goto(path);
  }
};

const logo_paint = (page) =>
  page.evaluate(() => {
    const el = document.querySelector("#logo .logo-default");
    if (!el) return null;
    const cs = getComputedStyle(el);
    return {
      // A mask keeps the glyphs and paints them with this colour, so the SVG's
      // own prefers-color-scheme query no longer decides anything.
      mask: cs.maskImage !== "none" ? cs.maskImage : cs.webkitMaskImage,
      background: cs.backgroundColor,
    };
  });

const body_brightness = async (page) =>
  brightness(
    await page.evaluate(() => getComputedStyle(document.body).backgroundColor),
  );

test.describe("diagram canvas", () => {
  test("bpmn shapes follow the resolved theme", async ({ page }) => {
    await page.emulateMedia({ colorScheme: "light" });

    // A fresh page has no stored preference, so the theme tracks the OS and
    // emulateMedia() alone is enough to switch it — no reload, no re-login.
    await page.evaluate(() => localStorage.removeItem("theme"));
    await page.goto("/processes");

    if (await page.getByLabel(/user name/i).count()) await login_via_form(page);

    const first = page.locator('main.processes a[href^="/processes/"]').first();
    // The table is rendered only after the statistics call resolves, so counting
    // straight after goto() races against it and skips the whole test.
    await first.waitFor({ state: "visible", timeout: 20_000 }).catch(() => {});
    test.skip(!(await first.count()), "no process definition available");

    await first.click();
    await page.waitForSelector(BPMN_SHAPE, { timeout: 20_000 });

    const light = await rgb_of(page, BPMN_SHAPE);
    expect(light).toMatch(/^rgb/);
    expect(brightness(light)).toBeGreaterThan(200);

    await page.emulateMedia({ colorScheme: "dark" });
    await expect.poll(() => rgb_of(page, BPMN_SHAPE)).not.toBe(light);

    const dark = await rgb_of(page, BPMN_SHAPE);
    expect(brightness(dark)).toBeLessThan(120);
  });

  test("dmn drd shapes follow the resolved theme", async ({ page }) => {
    // OS light, chosen theme light: the light half below must come from the
    // chosen theme, not from the machine.
    await page.emulateMedia({ colorScheme: "light" });
    await choose_theme(page, "light");
    await goto_signed_in(page, "/decisions");

    const first = page.locator('main.decisions a[href*="/decisions/"]').first();
    await first.waitFor({ state: "visible", timeout: 20_000 }).catch(() => {});
    test.skip(!(await first.count()), "no decision definition available");

    await first.click();
    const definition_url = page.url();
    // A decision that only has a decision table renders no DRD; that is not a
    // theme failure, so the case documents itself as skipped instead.
    test.skip(!(await wait_for(page, DRD_SHAPE)), "decision renders no drd");

    const light = await rgb_of(page, DRD_SHAPE);
    expect(brightness(light)).toBeGreaterThan(200);

    // OS still light while the theme is dark: the SVG keeps its inline
    // "fill: white", so a dark computed fill can only come from the override.
    await choose_theme(page, "dark");
    await page.goto(definition_url);
    if (await page.getByLabel(/user name/i).count()) {
      await login_via_form(page);
      await page.goto(definition_url);
    }
    test.skip(!(await wait_for(page, DRD_SHAPE)), "drd did not re-render");

    await expect.poll(() => rgb_of(page, DRD_SHAPE)).not.toBe(light);

    const dark = await rgb_of(page, DRD_SHAPE);
    expect(brightness(dark)).toBeLessThan(120);
    // The page really is dark, so the shapes are not dark-on-dark by accident.
    expect(await body_brightness(page)).toBeLessThan(120);
  });
});

test.describe("theme-painted chrome", () => {
  test("the header mark is painted from the theme, not the OS", async ({
    page,
  }) => {
    // OS dark, chosen theme light: an SVG recolouring itself from
    // prefers-color-scheme would paint light-on-light here.
    await page.emulateMedia({ colorScheme: "dark" });
    await choose_theme(page, "light");
    await goto_signed_in(page, "/processes");
    await page.waitForSelector("#logo .logo-default", { timeout: 15_000 });

    const light = await logo_paint(page);
    expect(light.mask).toContain("operaton-logo.svg");
    expect(light.background).toBe("rgb(0, 0, 0)");

    await choose_theme(page, "dark");
    await page.reload();
    if (await page.getByLabel(/user name/i).count()) await login_via_form(page);
    await page.waitForSelector("#logo .logo-default", { timeout: 15_000 });

    const dark = await logo_paint(page);
    expect(dark.background).toBe("rgb(255, 255, 255)");
    expect(await body_brightness(page)).toBeLessThan(120);
  });

  test("the server name is set at the pill's own type size", async ({
    page,
  }) => {
    await goto_signed_in(page, "/processes");
    await page.waitForSelector("#options-toggle", { timeout: 15_000 });

    // A base-select list rendered the server name as a real option, which
    // inherited the global `option { font-size: 1rem }` and grew — the native
    // popup this replaces used the select's own size. The name is a span now, so
    // it must sit at the pill's own size, and a long name must stay on one line.
    const sizes = await page.evaluate(() => {
      const pill = document.querySelector("#options-toggle");
      const name = document.querySelector("#options-toggle-server");
      return {
        pill: getComputedStyle(pill).fontSize,
        name: getComputedStyle(name).fontSize,
        wrap: getComputedStyle(name).whiteSpace,
      };
    });
    expect(sizes.name).toBe(sizes.pill);
    expect(sizes.wrap).toBe("nowrap");
  });
});
