import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { h } from "preact";
import { render, cleanup, fireEvent } from "@testing-library/preact";

// The global setup stubs useTranslation with a fresh `i18n` per call, which
// makes the instance the component talks to unreachable. This file keeps one
// spy so the choice can be asserted.
const { change_language } = vi.hoisted(() => ({ change_language: vi.fn() }));

vi.mock("react-i18next", () => ({
  useTranslation: () => {
    const t = (key) => key;
    const i18n = {
      changeLanguage: change_language,
      language: "en-US",
      resolvedLanguage: "en-US",
    };
    const result = [t, i18n, true];
    result.t = t;
    result.i18n = i18n;
    return result;
  },
}));

vi.mock("../assets/icons.jsx", () => ({ close: () => null }));

import { LanguageToggle, LanguageSelect } from "./LanguageToggle.jsx";
import { LANGUAGES, language_for } from "../helper/languages.js";

beforeEach(() => change_language.mockClear());
afterEach(cleanup);

const menu_of = (container) => container.querySelector("#language-menu");

describe("LanguageToggle", () => {
  it("shows the current language tag and opens the menu on click", () => {
    const { container, getByRole } = render(<LanguageToggle />);
    const button = getByRole("button", { name: /English/ });

    // The tag, not the full name: the header row has no room for "English".
    expect(button.textContent).toBe("EN");
    expect(button.getAttribute("aria-haspopup")).toBe("dialog");

    const menu = menu_of(container);
    expect(menu.hasAttribute("open")).toBe(false);
    fireEvent.click(button);
    expect(menu.hasAttribute("open")).toBe(true);
  });

  it("lists every offered language and marks the current one", () => {
    const { container } = render(<LanguageToggle />);
    const buttons = Array.from(menu_of(container).querySelectorAll("menu button"));

    expect(buttons.map((b) => b.textContent.replace("✓", "").trim())).toEqual(
      LANGUAGES.map((lng) => lng.label),
    );
    expect(
      buttons.filter((b) => b.getAttribute("aria-current") === "true"),
    ).toHaveLength(1);
    expect(buttons[0].getAttribute("aria-current")).toBe("true");
  });

  it("switches the language and closes the menu", () => {
    const { container, getByRole } = render(<LanguageToggle />);
    fireEvent.click(getByRole("button", { name: /English/ }));

    fireEvent.click(getByRole("button", { name: "Deutsch" }));

    expect(change_language).toHaveBeenCalledWith("de-DE");
    expect(menu_of(container).hasAttribute("open")).toBe(false);
  });

  it("closes without changing the language when dismissed", () => {
    const { container, getByRole } = render(<LanguageToggle />);
    fireEvent.click(getByRole("button", { name: /English/ }));
    fireEvent.click(getByRole("button", { name: "nav.close-menu" }));

    expect(menu_of(container).hasAttribute("open")).toBe(false);
    expect(change_language).not.toHaveBeenCalled();
  });
});

describe("LanguageSelect", () => {
  it("offers the same list and switches on change", () => {
    const { container } = render(<LanguageSelect />);
    const select = container.querySelector("select");

    expect(
      Array.from(select.querySelectorAll("option")).map((o) => o.value),
    ).toEqual(LANGUAGES.map((lng) => lng.code));

    fireEvent.change(select, { target: { value: "zh-TW" } });
    expect(change_language).toHaveBeenCalledWith("zh-TW");
  });
});

describe("language_for", () => {
  it("labels an unoffered locale with its own tag instead of pretending it is English", () => {
    expect(language_for("fr-FR")).toMatchObject({
      code: "fr-FR",
      label: "fr-FR",
      short: "FR",
    });
  });

  it("falls back to the default language when nothing is resolved yet", () => {
    expect(language_for(undefined).code).toBe("en-US");
  });
});
