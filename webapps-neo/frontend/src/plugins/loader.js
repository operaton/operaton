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
const manifest_url = () => {
  const configured = import.meta.env.VITE_PLUGINS_URL;
  // Ignore an empty value or an unreplaced Docker runtime placeholder
  // (env.sh only substitutes DOCKER_RUN_PLACEHOLDER_* when the operator sets it).
  return configured && !configured.includes("PLACEHOLDER")
    ? configured
    : "/plugins/plugins.json";
};

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

const load_remote = async (packages, importer) => {
  for (const pkg of packages) {
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
  const packages = (await with_timeout(discover_packages(), timeout)) ?? [];
  if (packages.length)
    await with_timeout(load_remote(packages, importer), timeout);
};
