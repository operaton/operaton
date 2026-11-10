/**
 * theme.js
 *
 * Theme preference for the whole application: `auto` follows the operating
 * system, `light` and `dark` are explicit choices that survive reloads.
 *
 * The resolved value is written to <html data-theme="light|dark">, which is the
 * only thing the stylesheets look at (see style.css and branding.js). That way
 * each theme is declared once instead of once as a media query and once as an
 * attribute selector — the two cannot drift apart.
 *
 * The first paint is handled by the inline script in index.html; keep its
 * storage key and resolution order in sync with this file.
 */
import { signal } from "@preact/signals";

export const THEME_KEY = "theme";
export const THEME_PREFS = ["auto", "light", "dark"];

const DARK_QUERY = "(prefers-color-scheme: dark)";

const is_pref = (value) => THEME_PREFS.includes(value);

/** The preference as chosen by the user — not the theme actually rendered. */
export const theme_pref = signal("auto");
/** What that preference resolves to right now: "light" or "dark". */
export const resolved_theme = signal("light");

// localStorage throws in some privacy modes; the preference then just does not
// survive the session instead of breaking the page.
export const read_pref = () => {
  try {
    const stored = window.localStorage.getItem(THEME_KEY);
    return is_pref(stored) ? stored : "auto";
  } catch {
    return "auto";
  }
};

const write_pref = (pref) => {
  try {
    if (pref === "auto") window.localStorage.removeItem(THEME_KEY);
    else window.localStorage.setItem(THEME_KEY, pref);
  } catch {
    /* storage unavailable — the in-memory preference still applies */
  }
};

const system_prefers_dark = () => {
  try {
    return window.matchMedia(DARK_QUERY).matches === true;
  } catch {
    return false;
  }
};

/** Map a stored preference to the theme that should be rendered. */
export const resolve_theme = (pref) => {
  const next = is_pref(pref) ? pref : "auto";
  if (next !== "auto") return next;
  return system_prefers_dark() ? "dark" : "light";
};

/**
 * Apply a preference: remember it, resolve it, and publish the result on
 * <html>. `persist: false` keeps the stored value untouched — used when the
 * system theme changes while `auto` is active.
 */
export const apply_theme = (pref, { persist = true } = {}) => {
  const next = is_pref(pref) ? pref : "auto";
  if (persist) write_pref(next);

  const resolved = resolve_theme(next);

  const root = document.documentElement;
  root.dataset.theme = resolved;
  // Native controls and scrollbars follow the explicit choice as well; without
  // this a dark page keeps light form widgets when the OS disagrees.
  root.style.colorScheme = resolved;

  // Publish only after <html> carries the new value: subscribers (e.g. the
  // BPMN viewer, which reads --canvas-* from the computed root style) must
  // never observe the signal ahead of the attribute it is supposed to match.
  theme_pref.value = next;
  resolved_theme.value = resolved;
  return resolved;
};

/** auto → light → dark → auto */
export const next_pref = (pref) => {
  const index = THEME_PREFS.indexOf(is_pref(pref) ? pref : "auto");
  return THEME_PREFS[(index + 1) % THEME_PREFS.length];
};

export const cycle_theme = () => apply_theme(next_pref(theme_pref.value));

let media = null;
let media_listener = null;

/**
 * Read the stored preference, publish it, and start following the system while
 * `auto` is selected. Idempotent; called once at boot, before the first render.
 */
export const start_theme = () => {
  apply_theme(read_pref());

  if (media || typeof window.matchMedia !== "function") return;
  media = window.matchMedia(DARK_QUERY);
  media_listener = () => {
    // An explicit choice must not be overridden by the OS; only `auto` follows.
    if (theme_pref.peek() === "auto") apply_theme("auto", { persist: false });
  };
  if (media.addEventListener) media.addEventListener("change", media_listener);
  else media.addListener(media_listener);
};

/** Stop following the system theme. Used by tests (and safe on hot reload). */
export const stop_theme = () => {
  if (!media) return;
  if (media.removeEventListener)
    media.removeEventListener("change", media_listener);
  else media.removeListener(media_listener);
  media = null;
  media_listener = null;
};
