/**
 * branding.js
 *
 * CI branding from the runtime configuration (see config.js `branding`).
 * White-label per installation: a `branding` block in config.json (or the
 * VITE_BRANDING env fallback) overrides the logo, app name, favicon and the
 * brand colour tokens. Anything omitted or invalid falls back to the Operaton
 * defaults, so a partial or malformed block can never break the UI.
 *
 * Values are validated before they reach the DOM/CSS, so a config file cannot
 * inject arbitrary markup or stylesheet rules.
 */
import { get_config } from "./config.js";

const DEFAULT_APP_NAME = "Operaton";
const DEFAULT_LOGO = "/operaton-logo.svg";

// Overridable brand colours, mapped to the CSS custom property they set.
const COLOR_TOKENS = {
  primary: "--color-primary",
  link: "--color-link",
  danger: "--color-danger",
  warning: "--color-warning",
  success: "--color-success",
};

const is_hex = (v) =>
  typeof v === "string" && /^#([0-9a-f]{3}|[0-9a-f]{6}|[0-9a-f]{8})$/i.test(v);
// Relative path or http(s) URL only — blocks javascript:/data: and stray markup.
const is_url = (v) =>
  typeof v === "string" && /^(https?:\/\/|\/)[^\s"'<>]+$/i.test(v);
const is_finite_number = (v) => typeof v === "number" && Number.isFinite(v);
const non_empty = (v) => typeof v === "string" && v.trim().length > 0;

const branding = () => get_config().branding ?? {};

export const app_name = () =>
  non_empty(branding().appName) ? branding().appName : DEFAULT_APP_NAME;

export const logo_url = () =>
  is_url(branding().logoUrl) ? branding().logoUrl : DEFAULT_LOGO;

export const logo_alt = () =>
  non_empty(branding().logoAlt) ? branding().logoAlt : app_name();

// `--token: value;` declarations for the valid hex colours in `colors`.
const color_decls = (colors) =>
  Object.entries(COLOR_TOKENS)
    .filter(([key]) => is_hex(colors?.[key]))
    .map(([key, token]) => `${token}: ${colors[key]};`);

// Build the :root override stylesheet from a branding block. Only valid values
// are emitted; missing ones keep the stylesheet defaults. Exported for testing.
export const build_css = (b) => {
  const light = color_decls(b.colors);
  if (is_finite_number(b.hue)) light.push(`--hue: ${b.hue};`);
  const dark = color_decls(b.colorsDark);

  const blocks = [];
  if (light.length) blocks.push(`:root { ${light.join(" ")} }`);
  if (dark.length)
    blocks.push(
      `@media (prefers-color-scheme: dark) { :root { ${dark.join(" ")} } }`,
    );
  return blocks.join("\n");
};

const inject_css = (css) => {
  if (!css) return;
  const style = document.createElement("style");
  style.id = "operaton-branding";
  style.textContent = css;
  document.head.appendChild(style);
};

const set_favicon = (url) => {
  if (!is_url(url)) return;
  let link = document.querySelector('link[rel~="icon"]');
  if (!link) {
    link = document.createElement("link");
    link.rel = "icon";
    document.head.appendChild(link);
  }
  link.href = url;
};

/**
 * Apply the configured branding to the document: inject the colour-token
 * overrides (light + dark), swap the favicon and set the document title. Call
 * once at boot after `load_config()`. A missing/empty branding block is a no-op.
 */
export const apply_branding = () => {
  const b = branding();
  inject_css(build_css(b));
  set_favicon(b.faviconUrl);
  if (non_empty(b.appName)) document.title = b.appName;
};
