import { test, expect } from "@playwright/test";

// The theme contract, verified without a backend: the inline script in
// index.html resolves the stored preference before the stylesheets paint, and
// every stylesheet keys off html[data-theme]. These assertions therefore do not
// depend on the app having booted or on a signed-in session.

const theme_of = (page) => page.locator("html").getAttribute("data-theme");

const open = (page) => page.goto("/", { waitUntil: "domcontentloaded" });

test.describe("theme", () => {
  test("auto follows the system preference", async ({ page }) => {
    await page.emulateMedia({ colorScheme: "dark" });
    await open(page);
    expect(await theme_of(page)).toBe("dark");

    await page.emulateMedia({ colorScheme: "light" });
    await open(page);
    expect(await theme_of(page)).toBe("light");
  });

  test("an explicit choice overrides the system and survives a reload", async ({
    page,
  }) => {
    await page.emulateMedia({ colorScheme: "dark" });
    await open(page);
    await page.evaluate(() => localStorage.setItem("theme", "light"));

    await open(page);
    expect(await theme_of(page)).toBe("light");

    await page.evaluate(() => localStorage.setItem("theme", "dark"));
    await page.emulateMedia({ colorScheme: "light" });
    await open(page);
    expect(await theme_of(page)).toBe("dark");
  });

  test("color-scheme follows the resolved theme, not the OS", async ({ page }) => {
    await page.emulateMedia({ colorScheme: "dark" });
    await open(page);
    await page.evaluate(() => localStorage.setItem("theme", "light"));
    await open(page);

    const scheme = await page.evaluate(
      () => document.documentElement.style.colorScheme,
    );
    expect(scheme).toBe("light");
  });

  test("the dark tokens reach the root element", async ({ page }) => {
    await page.emulateMedia({ colorScheme: "light" });
    await open(page);
    await page.evaluate(() => localStorage.setItem("theme", "dark"));
    await open(page);

    // Wait for the stylesheet to be in place: in dev it is injected by the
    // module graph, and only then is the token readable at all.
    await expect
      .poll(
        () =>
          page.evaluate(() =>
            getComputedStyle(document.documentElement).getPropertyValue(
              "--background-1",
            ),
          ),
        { timeout: 5000 },
      )
      .toContain("0.22");

    const dark = await page.evaluate(() => {
      const root = document.documentElement;
      return {
        background: getComputedStyle(root)
          .getPropertyValue("--background-1")
          .trim(),
        text: getComputedStyle(root).getPropertyValue("--text").trim(),
      };
    });
    expect(dark.background).toContain("0.22");
    expect(dark.text).toBe("#fff");
  });

  test("the bundled mark is painted from the theme, not the OS", async ({
    page,
  }) => {
    await page.emulateMedia({ colorScheme: "dark" });
    await open(page);
    await page.evaluate(() => localStorage.setItem("theme", "light"));
    await open(page);

    // The login screen mounts only after the config request resolves, so the
    // lookup has to wait for it instead of racing an empty body.
    const read_logo = async (theme) => {
      await page.waitForSelector(".login-logo", { timeout: 15_000 });
      const logo = await page.evaluate(() => {
        const el = document.querySelector(".login-logo");
        const cs = getComputedStyle(el);
        return {
          // A mask keeps the glyphs and paints them with this colour, so the
          // SVG's own prefers-color-scheme query no longer decides anything.
          filter: cs.filter,
          mask: cs.maskImage !== "none" ? cs.maskImage : cs.webkitMaskImage,
          background: cs.backgroundColor,
        };
      });
      return { ...logo, theme };
    };

    const light = await read_logo("light");
    expect(light.mask).toContain("operaton-logo.svg");
    expect(light.filter).toBe("none");
    // OS dark, chosen theme light: the mark paints with --text, which is black
    // here. Left to the SVG's own media query it would paint light-on-light.
    expect(light.background).toBe("rgb(0, 0, 0)");

    await page.evaluate(() => localStorage.setItem("theme", "dark"));
    await open(page);
    const dark = await read_logo("dark");
    expect(dark.background).toBe("rgb(255, 255, 255)");
  });
});
