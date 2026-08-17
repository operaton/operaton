// pa11y accessibility runner — a SECOND rule engine (HTML_CodeSniffer) over the
// same shared route manifest the axe spec uses (routes.js). HTMLCS is a WCAG
// techniques-based ruleset distinct from axe-core, so it surfaces issues axe
// deliberately stays silent on.
//
// Two entry points:
//   * `npm run test:a11y:pa11y` — CLI, prints a summary, exits non-zero on errors
//   * `run_pa11y()`             — imported by the report generator, returns data
//
// Prerequisites (same as the axe spec): the Vite dev server on :5173 and the
// Operaton backend on :8084 must be running.
//
// Auth note: the app logs in by reading sessionStorage["basic_auth"], which
// pa11y actions cannot set — so we drive our own Puppeteer page and seed the
// credential with evaluateOnNewDocument (mirroring fixtures.js) before pa11y
// navigates.

import { pathToFileURL } from "node:url";
import pa11y from "pa11y";
import puppeteer from "puppeteer";
import { CREDENTIALS } from "./fixtures.js";
import { STATIC_ROUTES, LOGIN_ROUTE, discover_deep_routes } from "./routes.js";

const BASE_URL = process.env.E2E_BASE_URL ?? "http://127.0.0.1:5173";

const PA11Y_OPTS = {
  runners: ["htmlcs"],
  standard: "WCAG2AA",
  timeout: 30_000,
};

// Triaged HTMLCS noise. `notice` drops the purely informational bucket; add
// specific rule codes below with a one-line reason as they are reviewed.
const IGNORE = [
  "notice",
  // "WCAG2AA.Principle1.Guideline1_4.1_4_3.G18.Fail", // tracked in #NNN
];

const seed_auth = (page) =>
  page.evaluateOnNewDocument((creds) => {
    window.sessionStorage.setItem("basic_auth", JSON.stringify(creds));
  }, CREDENTIALS);

// Wait for the page's readiness landmark rather than sleeping a fixed interval.
// A fixed wait against an async SPA is a race: it intermittently scans a
// pre-hydration DOM and reports findings that do not reproduce.
const ready_action = (route) => [
  `wait for element ${
    route.name === "login" ? "section.login-page" : "main"
  } to be visible`,
];

const format = (issues) =>
  issues
    .map((i) => `  [${i.type}] ${i.code}\n      ${i.message}\n      ${i.selector}`)
    .join("\n\n");

/**
 * Scan `routes` with pa11y and return structured results.
 * @returns Array<{route, issues, failed}>
 */
export const run_pa11y = async ({
  routes,
  base_url = BASE_URL,
  ignore = IGNORE,
} = {}) => {
  const browser = await puppeteer.launch();
  const out = [];

  try {
    for (const route of routes) {
      if (!route.path) continue;
      const url = new URL(route.path, base_url).href;
      const page = await browser.newPage();
      if (route.auth) await seed_auth(page);

      try {
        const result = await pa11y(url, {
          ...PA11Y_OPTS,
          actions: ready_action(route),
          ignore,
          browser,
          page,
        });
        out.push({ route, issues: result.issues, failed: null });
      } catch (error) {
        out.push({ route, issues: [], failed: error.constructor.name });
      } finally {
        await page.close();
      }
    }
  } finally {
    await browser.close();
  }

  return out;
};

const main = async () => {
  const routes = [
    ...STATIC_ROUTES,
    LOGIN_ROUTE,
    ...(await discover_deep_routes({ credentials: CREDENTIALS })),
  ];

  const results = await run_pa11y({ routes });
  let error_count = 0;

  for (const { route, issues, failed } of results) {
    if (failed) {
      console.error(`\n✖ ${route.name} (${route.path}) — scan failed: ${failed}`);
      error_count += 1;
      continue;
    }
    const errors = issues.filter((i) => i.type === "error");
    const warnings = issues.filter((i) => i.type === "warning");
    error_count += errors.length;

    if (issues.length === 0) {
      console.log(`✓ ${route.name} (${route.path}) — clean`);
    } else {
      console.log(
        `\n${errors.length ? "✖" : "!"} ${route.name} (${route.path}) — ` +
          `${errors.length} error(s), ${warnings.length} warning(s)\n\n` +
          `${format(issues)}\n`,
      );
    }
  }

  console.log(
    `\npa11y/HTMLCS: ${routes.length} pages scanned, ${error_count} error(s).`,
  );
  process.exit(error_count > 0 ? 1 : 0);
};

// Only run the CLI when invoked directly — the report generator imports this.
if (import.meta.url === pathToFileURL(process.argv[1] ?? "").href)
  main().catch((e) => {
    console.error(e);
    process.exit(1);
  });
