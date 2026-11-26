import { test, expect } from "./fixtures.js";

// The header language switch, end to end: choosing a language must relabel the
// running UI (no reload, no re-login), write the choice where
// i18next-browser-languagedetector reads it on the next visit, and be still in
// effect after a reload. Switching back proves the menu is not one-way and that
// a second change overwrites the same key rather than piling up state.

// The key LanguageDetector's default `caches: ["localStorage"]` writes;
// `lookupLocalStorage` is the same option, so a stored value is picked up again.
const LANG_KEY = "i18nextLng";

const toggle = (page) => page.locator("#language-toggle");
const menu = (page) => page.locator("#language-menu");

// The nav entry is the visible proof that the whole UI follows the choice, not
// just the button label.
const nav_link = (page, name) =>
  page
    .locator("nav#primary-navigation")
    .getByRole("link", { name, exact: true })
    .first();

// The app keeps no password on the client, so a reload can land on the login
// form again. Fill it by id rather than by label: once this spec has switched
// to German, the labels are German too and `/user name/i` would miss them.
const ensure_signed_in = async (page) => {
  // Right after reload() neither shell is painted yet, so counting `#username`
  // straight away always reads zero and skips the login that is still needed.
  await page.waitForSelector("#language-toggle, #username", {
    timeout: 30_000,
  });
  const user = page.locator("#username");
  if (!(await user.count())) return;
  await user.fill("demo");
  await page.locator("#password").fill("demo");
  await page.locator('section.login-page form button[type="submit"]').click();
  await expect(toggle(page)).toBeVisible({ timeout: 20_000 });
};

test.describe("language switch", () => {
  test("switches in place, survives a reload, and switches back", async ({
    page,
  }) => {
    // A known starting point: the developer's browser language, or a value left
    // behind by an earlier run, must not decide what "before" means.
    await page.evaluate((key) => localStorage.setItem(key, "en-US"), LANG_KEY);
    await page.reload();
    await ensure_signed_in(page);

    await expect(toggle(page)).toContainText("EN");
    await expect(nav_link(page, "Tasks")).toBeVisible();

    // The menu opens on the language in use, checked.
    await toggle(page).click();
    await expect(menu(page)).toBeVisible();
    await expect(menu(page).locator('button[lang="en-US"]')).toHaveAttribute(
      "aria-current",
      "true",
    );

    await menu(page).locator('button[lang="de-DE"]').click();
    await expect(menu(page)).toBeHidden();

    // Relabelled in place — the point of the feature is that the session and
    // the current page survive the switch.
    await expect(toggle(page)).toContainText("DE");
    await expect(toggle(page)).toHaveAttribute("title", "Sprache: Deutsch");
    await expect(nav_link(page, "Aufgaben")).toBeVisible();
    await expect(nav_link(page, "Tasks")).toHaveCount(0);
    // <html lang> follows, so font fallback and line breaking (CJK punctuation
    // in particular) are decided by the language actually rendered.
    await expect(page.locator("html")).toHaveAttribute("lang", "de-DE");
    expect(
      await page.evaluate((key) => localStorage.getItem(key), LANG_KEY),
    ).toBe("de-DE");
    // Signing out and back in is exactly what this feature exists to avoid.
    await expect(page.locator("#username")).toHaveCount(0);

    // Reload: the choice is read back from storage before the first render, so
    // there is no frame in English either.
    await page.reload();
    await ensure_signed_in(page);
    await expect(toggle(page)).toContainText("DE");
    await expect(page.locator("html")).toHaveAttribute("lang", "de-DE");
    await expect(nav_link(page, "Aufgaben")).toBeVisible();
    expect(
      await page.evaluate((key) => localStorage.getItem(key), LANG_KEY),
    ).toBe("de-DE");

    // And back, through the same menu: one key, overwritten.
    await toggle(page).click();
    await expect(menu(page)).toBeVisible();
    await expect(menu(page).locator('button[lang="de-DE"]')).toHaveAttribute(
      "aria-current",
      "true",
    );
    await menu(page).locator('button[lang="en-US"]').click();
    await expect(menu(page)).toBeHidden();

    await expect(toggle(page)).toContainText("EN");
    await expect(nav_link(page, "Tasks")).toBeVisible();
    await expect(page.locator("html")).toHaveAttribute("lang", "en-US");
    expect(
      await page.evaluate((key) => localStorage.getItem(key), LANG_KEY),
    ).toBe("en-US");
  });
});
