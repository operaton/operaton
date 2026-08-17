// pa11y accessibility runner — a SECOND rule engine (HTML_CodeSniffer) over the
// same shared route manifest the axe spec uses (routes.js). HTMLCS is a WCAG
// techniques-based ruleset distinct from axe-core, so it surfaces issues axe
// deliberately stays silent on.
//
// Prerequisites (same as the axe spec): the Vite dev server on :5173 and the
// Operaton backend on :8084 must be running. Run via `npm run test:a11y:pa11y`.
//
// Auth note: the app logs in by reading sessionStorage["basic_auth"], which
// pa11y actions cannot set — so we drive our own Puppeteer page and seed the
// credential with evaluateOnNewDocument (mirroring fixtures.js) before pa11y
// navigates.

import pa11y from "pa11y";
import puppeteer from "puppeteer";
import { CREDENTIALS } from "./fixtures.js";
import { STATIC_ROUTES, LOGIN_ROUTE, discover_deep_routes } from "./routes.js";

const BASE_URL = process.env.E2E_BASE_URL ?? "http://127.0.0.1:5173";

const PA11Y_OPTS = {
  runners: ["htmlcs"],
  standard: "WCAG2AA",
  wait: 1500, // let the SPA hydrate + fetch before scanning
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

const format = (issues) =>
  issues
    .map((i) => `  [${i.type}] ${i.code}\n      ${i.message}\n      ${i.selector}`)
    .join("\n\n");

const main = async () => {
  const routes = [
    ...STATIC_ROUTES,
    LOGIN_ROUTE,
    ...(await discover_deep_routes({ credentials: CREDENTIALS })),
  ];

  const browser = await puppeteer.launch();
  let error_count = 0;

  for (const route of routes) {
    const url = new URL(route.path, BASE_URL).href;
    const page = await browser.newPage();
    if (route.auth) await seed_auth(page);

    try {
      const result = await pa11y(url, {
        ...PA11Y_OPTS,
        ignore: IGNORE,
        browser,
        page,
      });
      const errors = result.issues.filter((i) => i.type === "error");
      const warnings = result.issues.filter((i) => i.type === "warning");
      error_count += errors.length;

      if (result.issues.length === 0) {
        console.log(`✓ ${route.name} (${route.path}) — clean`);
      } else {
        console.log(
          `\n${errors.length ? "✖" : "!"} ${route.name} (${route.path}) — ` +
            `${errors.length} error(s), ${warnings.length} warning(s)\n\n` +
            `${format(result.issues)}\n`,
        );
      }
    } catch (e) {
      console.error(
        `\n✖ ${route.name} (${route.path}) — scan failed: ${e.message}`,
      );
      error_count += 1;
    } finally {
      await page.close();
    }
  }

  await browser.close();
  console.log(
    `\npa11y/HTMLCS: ${routes.length} pages scanned, ${error_count} error(s).`,
  );
  process.exit(error_count > 0 ? 1 : 0);
};

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
