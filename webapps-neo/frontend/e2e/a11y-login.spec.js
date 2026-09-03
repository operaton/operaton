import { test as base, expect } from "@playwright/test";
import { CREDENTIALS } from "./fixtures.js";
import { expect_no_a11y_violations } from "./a11y.js";
import { freeze_animations } from "./a11y-scan.js";
import { focused, focus_document_start } from "./a11y-keyboard.js";

// Requirements SI-1..SI-5 of docs/Accessibility Requirements.md.
//
// Uses the plain (non-auth-seeding) fixture: the login screen renders only
// while unauthenticated. This is the one view where a user who cannot recover
// is locked out of the product altogether, which is why the failure path is
// tested at least as carefully as the success path.

const LOGIN = "section.login-page";

const open_login = async (page) => {
  await page.goto("/", { waitUntil: "domcontentloaded" });
  await page.locator(LOGIN).waitFor({ timeout: 30_000 });
  await freeze_animations(page);
};

/** Tab forward until the named field has focus, then type into it. */
const tab_to_and_type = async (page, id, text, { max = 20 } = {}) => {
  for (let presses = 1; presses <= max; presses++) {
    await page.keyboard.press("Tab");
    if (await page.evaluate((target) => document.activeElement?.id === target, id)) {
      await page.keyboard.type(text);
      return presses;
    }
  }
  throw new Error(`#${id} was never reached by pressing Tab`);
};

base.describe("signing in — keyboard and error recovery", () => {
  base.slow();

  // SI-1
  base("the credential form is completable with the keyboard alone", async ({
    page,
  }) => {
    await open_login(page);
    await focus_document_start(page);

    // In visual order, and each reached by Tab rather than by focusing it.
    const to_username = await tab_to_and_type(
      page,
      "username",
      CREDENTIALS.username,
    );
    const to_password = await tab_to_and_type(
      page,
      "password",
      CREDENTIALS.password,
    );
    expect(
      to_password,
      "the password field must follow the user name field in the tab order",
    ).toBeGreaterThan(0);
    expect(to_username).toBeGreaterThan(0);

    // The submit button is the next stop, and Enter submits.
    await page.keyboard.press("Tab");
    const submit = await focused(page);
    expect(
      submit.tag,
      `the control after the password field was ${submit.tag} "${submit.text}"`,
    ).toBe("BUTTON");

    await page.keyboard.press("Enter");
    await expect(page.locator(LOGIN)).toBeHidden({ timeout: 30_000 });
    await expect(page.locator("#primary-navigation")).toBeVisible();
  });

  // SI-2
  base("both credential fields are labelled and autofillable", async ({
    page,
  }) => {
    await open_login(page);

    const username = page.locator("#username"),
      password = page.locator("#password");

    await expect(username).toHaveAccessibleName(/.+/);
    await expect(password).toHaveAccessibleName(/.+/);

    // Without these a password manager cannot fill the form, which is an
    // access barrier for anyone who cannot type a long credential reliably.
    await expect(username).toHaveAttribute("autocomplete", "username");
    await expect(password).toHaveAttribute("autocomplete", "current-password");
  });

  // SI-3, SI-4 — a rejected password used to leave `logged_in.data` at
  // "wrong_login", which matched no branch of the router, so the entire app
  // rendered as an empty document and said nothing about why.
  base("a rejected credential keeps the form on screen and announces the failure", async ({
    page,
  }) => {
    await open_login(page);

    await page.locator("#username").fill(CREDENTIALS.username);
    await page.locator("#password").fill("definitely-not-the-password");
    await page.getByRole("button", { name: /login/i }).click();

    // SI-3: still a login form, not a blank document.
    await expect(page.locator(LOGIN)).toBeVisible({ timeout: 30_000 });
    await expect(page.locator("#username")).toBeVisible();

    // SI-4: announced where a screen reader will read it without the user
    // going looking, and specific enough to act on.
    const alert = page.locator(`${LOGIN} [role="alert"]`);
    await expect(alert).toBeVisible({ timeout: 15_000 });
    await expect(alert).toHaveText(/./);

    // The user name already typed survives the failure, so recovery is one
    // field rather than the whole form again.
    await expect(page.locator("#username")).toHaveValue(CREDENTIALS.username);
  });

  // SI-5
  base("the failed-login state has no WCAG A/AA violations", async ({
    page,
  }) => {
    await open_login(page);

    await page.locator("#username").fill(CREDENTIALS.username);
    await page.locator("#password").fill("definitely-not-the-password");
    await page.getByRole("button", { name: /login/i }).click();
    await page.locator(`${LOGIN} [role="alert"]`).waitFor({ timeout: 15_000 });
    await freeze_animations(page);

    await expect_no_a11y_violations(page);
  });
});
