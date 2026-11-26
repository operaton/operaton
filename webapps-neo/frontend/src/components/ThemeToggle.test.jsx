import { describe, it, expect, beforeEach, afterEach } from "vitest";
import { h } from "preact";
import { render, cleanup, fireEvent } from "@testing-library/preact";

import { ThemeToggle } from "./ThemeToggle.jsx";
import { THEME_KEY, theme_pref } from "../theme.js";

beforeEach(() => {
  window.localStorage.clear();
  theme_pref.value = "auto";
  document.documentElement.removeAttribute("data-theme");
});

afterEach(cleanup);

const icon_of = (button) => button.querySelector("span").textContent;

describe("ThemeToggle", () => {
  it("cycles auto → light → dark → auto and persists each explicit choice", () => {
    const { getByRole } = render(<ThemeToggle id="toggle" />);
    const button = getByRole("button");

    expect(icon_of(button)).toBe("◐");
    expect(window.localStorage.getItem(THEME_KEY)).toBe(null);

    fireEvent.click(button);
    expect(theme_pref.value).toBe("light");
    expect(window.localStorage.getItem(THEME_KEY)).toBe("light");
    expect(icon_of(button)).toBe("☀");

    fireEvent.click(button);
    expect(theme_pref.value).toBe("dark");
    expect(window.localStorage.getItem(THEME_KEY)).toBe("dark");
    expect(icon_of(button)).toBe("☾");

    // Back to auto: the stored override is removed rather than written.
    fireEvent.click(button);
    expect(theme_pref.value).toBe("auto");
    expect(window.localStorage.getItem(THEME_KEY)).toBe(null);
    expect(icon_of(button)).toBe("◐");
  });

  it("describes the current state, and spells it out when asked", () => {
    const { getByRole } = render(<ThemeToggle id="toggle" show_label />);
    const button = getByRole("button");

    expect(button.getAttribute("aria-label")).toContain(":");
    expect(button.getAttribute("title")).toBe(button.getAttribute("aria-label"));
    // The mobile variant carries the text next to the icon.
    expect(button.textContent).toContain(":");

    fireEvent.click(button);
    expect(button.textContent).toContain(": ");
  });
});
