/**
 * loader.js
 *
 * Populates the registry at boot from two sources that share one descriptor
 * contract:
 *   1. Bundled first-party plugins — glob-imported from `./bundled/`.
 *   2. Remote plugins — discovered from a JSON manifest and dynamically
 *      imported at runtime (the standalone deployment path; see
 *      `docs/Plugin System.md`).
 *
 * Every plugin loads in isolation: one broken plugin logs and is skipped, and
 * a slow/broken manifest server can never brick the app (see `with_timeout`).
 */
import { register } from "./registry.js";
import { get_config } from "../config.js";

const VERSION = import.meta.env.VITE_APP_VERSION || "dev";

const register_descriptors = (module) => {
  for (const descriptor of [].concat(module?.default ?? []))
    register(descriptor);
};

/** Register plugins that ship inside the app bundle. */
const register_bundled = () => {
  const modules = import.meta.glob("./bundled/*/plugin.jsx", { eager: true });
  for (const path in modules) {
    try {
      register_descriptors(modules[path]);
    } catch (error) {
      console.error(
        `[plugins] bundled plugin "${path}" failed to register`,
        error,
      );
    }
  }
};

/**
 * Where remote plugins are listed. `window.PLUGIN_PACKAGES` (future servlet
 * injection) wins; otherwise fetch the static manifest. A missing manifest is
 * not an error — it just means no remote plugins.
 */
const manifest_url = () => get_config().plugins_url ?? "/plugins/plugins.json";

const discover_packages = async () => {
  if (Array.isArray(window.PLUGIN_PACKAGES)) return window.PLUGIN_PACKAGES;
  const url = manifest_url();
  try {
    const response = await fetch(url);
    if (!response.ok) return [];
    const json = await response.json();
    return Array.isArray(json) ? json : [];
  } catch {
    return [];
  }
};

const inject_css = (href) => {
  const link = document.createElement("link");
  link.rel = "stylesheet";
  link.href = href;
  document.head.appendChild(link);
};

/**
 * Whether `location` may be loaded from.
 *
 * A remote plugin is imported as a module and runs with the full privileges of the
 * app — same DOM, same session, same access to the API helpers. Loading one is
 * therefore a decision for whoever deploys the webapp, not something to infer from
 * a manifest. Same-origin locations are allowed once remote plugins are switched on;
 * any other origin has to be listed explicitly.
 */
export const is_allowed_location = (location, allow_origins = []) => {
  // Guard explicitly: `new URL(undefined, base)` resolves to "<base>/undefined",
  // which would pass the same-origin check below for a manifest entry that simply
  // has no location.
  if (typeof location !== "string" || !location.trim()) return false;
  let origin;
  try {
    origin = new URL(location, document.baseURI).origin;
  } catch {
    return false;
  }
  if (origin === window.location.origin) return true;
  return allow_origins.some((allowed) => {
    try {
      return new URL(allowed).origin === origin;
    } catch {
      return false;
    }
  });
};

const load_remote = async (packages, importer, allow_origins) => {
  for (const pkg of packages) {
    if (!is_allowed_location(pkg.location, allow_origins)) {
      console.error(
        `[plugins] remote plugin "${pkg.name ?? pkg.location}" refused: ` +
          `"${pkg.location}" is not same-origin and is not in remotePluginsAllowOrigins`,
      );
      continue;
    }
    try {
      if (pkg.css) inject_css(`${pkg.location}/${pkg.css}?bust=${VERSION}`);
      const module = await importer(
        `${pkg.location}/${pkg.main ?? "plugin.js"}?bust=${VERSION}`,
      );
      register_descriptors(module);
    } catch (error) {
      console.error(
        `[plugins] remote plugin "${pkg.name ?? pkg.location}" failed to load, skipping`,
        error,
      );
    }
  }
};

// Resolve when `promise` settles or `ms` elapses — whichever comes first — so a
// hung network request can't block boot.
const with_timeout = (promise, ms) =>
  Promise.race([
    promise.catch(() => undefined),
    new Promise((resolve) => setTimeout(resolve, ms)),
  ]);

/**
 * Load every plugin. `importer` is injectable for tests; `timeout` guards both
 * the manifest fetch and the remote imports.
 */
export const load_plugins = async ({
  importer = (url) => import(/* @vite-ignore */ url),
  timeout = 3000,
} = {}) => {
  register_bundled();

  // Bundled plugins ship with the app and are always registered. Remote ones are
  // third-party code and stay off until a deployment opts in.
  const config = get_config();
  if (!config.remote_plugins_enabled) return;

  const packages = (await with_timeout(discover_packages(), timeout)) ?? [];
  if (packages.length)
    await with_timeout(
      load_remote(packages, importer, config.remote_plugins_allow_origins ?? []),
      timeout,
    );
};
