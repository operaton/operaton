import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import {
  THEME_KEY,
  apply_theme,
  cycle_theme,
  next_pref,
  read_pref,
  resolve_theme,
  resolved_theme,
  start_theme,
  stop_theme,
  theme_pref,
} from "./theme.js";

const root = () => document.documentElement;

// happy-dom does not emulate an OS colour preference, so the media query is
// stubbed: `handlers` lets a test fire the same event a browser would.
const stub_system = (dark) => {
  const handlers = new Set();
  const mql = {
    media: "(prefers-color-scheme: dark)",
    matches: dark,
    addEventListener: (_type, handler) => handlers.add(handler),
    removeEventListener: (_type, handler) => handlers.delete(handler),
    addListener: (handler) => handlers.add(handler),
    removeListener: (handler) => handlers.delete(handler),
  };
  vi.stubGlobal(
    "matchMedia",
    vi.fn(() => mql),
  );
  return {
    set(next) {
      mql.matches = next;
      handlers.forEach((handler) => handler({ matches: next }));
    },
    listener_count: () => handlers.size,
  };
};

beforeEach(() => {
  window.localStorage.clear();
  root().removeAttribute("data-theme");
  root().style.colorScheme = "";
  theme_pref.value = "auto";
  resolved_theme.value = "light";
});

afterEach(() => {
  stop_theme();
  vi.unstubAllGlobals();
});

describe("resolve_theme", () => {
  it("follows the system for auto", () => {
    stub_system(true);
    expect(resolve_theme("auto")).toBe("dark");
    stub_system(false);
    expect(resolve_theme("auto")).toBe("light");
  });

  it("honours an explicit choice even against the system", () => {
    stub_system(true);
    expect(resolve_theme("light")).toBe("light");
    stub_system(false);
    expect(resolve_theme("dark")).toBe("dark");
  });

  it("falls back to auto for an unknown preference", () => {
    stub_system(true);
    expect(resolve_theme("sepia")).toBe("dark");
    expect(resolve_theme(undefined)).toBe("dark");
    expect(resolve_theme(null)).toBe("dark");
  });
});

describe("apply_theme", () => {
  it("publishes the resolved theme on the root element", () => {
    stub_system(false);
    apply_theme("dark");

    expect(root().getAttribute("data-theme")).toBe("dark");
    expect(root().style.colorScheme).toBe("dark");
    expect(theme_pref.value).toBe("dark");
    expect(resolved_theme.value).toBe("dark");
  });

  it("persists an explicit choice", () => {
    stub_system(false);
    apply_theme("dark");
    expect(window.localStorage.getItem(THEME_KEY)).toBe("dark");
  });

  it("clears the stored key for auto", () => {
    window.localStorage.setItem(THEME_KEY, "dark");
    stub_system(false);
    apply_theme("auto");
    expect(window.localStorage.getItem(THEME_KEY)).toBeNull();
  });

  it("validates the argument", () => {
    stub_system(false);
    apply_theme("nonsense");
    expect(theme_pref.value).toBe("auto");
    expect(root().getAttribute("data-theme")).toBe("light");
  });
});

describe("read_pref", () => {
  it("returns the stored preference", () => {
    window.localStorage.setItem(THEME_KEY, "light");
    expect(read_pref()).toBe("light");
  });

  it("ignores an unknown stored value", () => {
    window.localStorage.setItem(THEME_KEY, "purple");
    expect(read_pref()).toBe("auto");
  });
});

describe("cycling", () => {
  it("walks auto → light → dark → auto", () => {
    expect(next_pref("auto")).toBe("light");
    expect(next_pref("light")).toBe("dark");
    expect(next_pref("dark")).toBe("auto");
  });

  it("cycles from the current preference and persists it", () => {
    const system = stub_system(true);
    expect(cycle_theme()).toBe("light");
    expect(theme_pref.value).toBe("light");
    expect(root().getAttribute("data-theme")).toBe("light");
    expect(system.listener_count()).toBe(0); // no listener before start_theme
  });
});

describe("start_theme", () => {
  it("applies the stored preference at boot", () => {
    window.localStorage.setItem(THEME_KEY, "dark");
    stub_system(false);
    start_theme();
    expect(theme_pref.value).toBe("dark");
    expect(root().getAttribute("data-theme")).toBe("dark");
  });

  it("follows the system while auto is selected", () => {
    const system = stub_system(false);
    start_theme();
    expect(root().getAttribute("data-theme")).toBe("light");

    system.set(true);
    expect(resolved_theme.value).toBe("dark");
    expect(root().getAttribute("data-theme")).toBe("dark");

    system.set(false);
    expect(root().getAttribute("data-theme")).toBe("light");
    expect(system.listener_count()).toBe(1);
  });

  it("does not follow the system once a theme is chosen explicitly", () => {
    const system = stub_system(false);
    start_theme();
    apply_theme("light");

    system.set(true);
    expect(root().getAttribute("data-theme")).toBe("light");
    expect(theme_pref.value).toBe("light");
  });

  it("is idempotent", () => {
    const system = stub_system(false);
    start_theme();
    start_theme();
    expect(system.listener_count()).toBe(1);
  });

  it("removes its listener on stop_theme", () => {
    const system = stub_system(false);
    start_theme();
    stop_theme();
    expect(system.listener_count()).toBe(0);
  });
});
