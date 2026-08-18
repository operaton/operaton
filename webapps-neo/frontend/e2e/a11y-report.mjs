#!/usr/bin/env node
// Accessibility report generator.
//
//   npm run a11y:report
//
// Walks every page in the shared route manifest (routes.js) across a theme x
// viewport axis plus a set of declared interaction states (a11y-states.js),
// scans each with axe-core and (optionally) pa11y, and writes two committed
// markdown files:
//
//   docs/accessibility/REPORT.md    the full per-page, per-state drill-down
//   docs/accessibility/timeline.md  one row per run, with +/- deltas
//
// This is a REPORT, not a test: it ALWAYS exits 0. The gating check is
// a11y.spec.js, which stays deliberately narrower.
//
// Prerequisites: an Operaton engine on :8084. The Vite dev server is started
// automatically in the pinned `a11y` mode (.env.a11y) unless --no-server.

import { spawn } from "node:child_process";
import { execFileSync } from "node:child_process";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { createRequire } from "node:module";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { chromium } from "@playwright/test";

import { analyze_a11y, REPORT_TAGS, REPORT_ENABLED_RULES } from "./a11y.js";
import { prepare_page, freeze_animations, DEFAULT_READY } from "./a11y-scan.js";
import { base_states, interaction_states_for } from "./a11y-states.js";
import { CREDENTIALS } from "./fixtures.js";
import {
  BACKEND,
  LOGIN_ROUTE,
  STATIC_ROUTES,
  discover_deep_routes,
} from "./routes.js";
import {
  findings_from_axe,
  findings_from_pa11y,
  normalize_selector,
  rule_counts,
  sorted_findings,
  tally,
} from "./a11y-normalize.js";
import {
  next_timeline_entry,
  parse_timeline_state,
  render_report,
  render_timeline,
} from "./a11y-markdown.js";

const require = createRequire(import.meta.url);
const here = dirname(fileURLToPath(import.meta.url));
const frontend = resolve(here, "..");

const BASE_URL = process.env.E2E_BASE_URL ?? "http://127.0.0.1:5173";
const OUT_DIR = resolve(frontend, "docs/accessibility");

const args = Object.fromEntries(
  process.argv.slice(2).map((a) => {
    const [k, v] = a.replace(/^--/, "").split("=");
    return [k, v ?? true];
  }),
);

const engines = String(args.engines ?? "axe,pa11y").split(",");
const use_pa11y = engines.includes("pa11y");
// --pages=tasks,login restricts the run while iterating on the generator. The
// committed report is always produced by a full run.
const only_pages = args.pages ? String(args.pages).split(",") : null;

// Human labels for the report's page column. Falls back to the route name.
const PAGE_LABELS = {
  dashboard: "Dashboard",
  tasks: "Aufgaben",
  "start-process": "Prozess starten",
  processes: "Prozesse",
  decisions: "Entscheidungen",
  deployments: "Deployments",
  batches: "Batches",
  migrations: "Migrationen",
  account: "Konto",
  admin: "Administration",
  help: "Hilfe",
  "not-found": "Nicht gefunden (404)",
  login: "Anmeldung (nicht authentifiziert)",
  "process-instance-detail": "Prozessinstanz-Detail",
  "task-detail": "Aufgaben-Detail",
  "decision-detail": "Entscheidungs-Detail",
};

const label_for = (name) => PAGE_LABELS[name] ?? name;

const log = (message) => process.stderr.write(`${message}\n`);

// ---------------------------------------------------------------------------
// Environment
// ---------------------------------------------------------------------------

const server_is_up = async (url) => {
  try {
    const res = await fetch(url, { signal: AbortSignal.timeout(2000) });
    return res.ok || res.status === 404;
  } catch {
    return false;
  }
};

const wait_for = async (url, timeout_ms) => {
  const deadline = Date.now() + timeout_ms;
  while (Date.now() < deadline) {
    if (await server_is_up(url)) return true;
    await new Promise((r) => setTimeout(r, 500));
  }
  return false;
};

/**
 * Start Vite in the pinned `a11y` mode so the report never depends on a
 * developer's gitignored .env.development.local (which changes how many
 * backends the login page offers, and therefore what axe sees).
 */
const start_dev_server = async () => {
  if (await server_is_up(BASE_URL)) {
    // Refuse rather than reuse. A foreign server answering on this port serves
    // an unknown codebase in an unknown mode, and the report it produces looks
    // completely normal — there is no marker in the output saying it scanned
    // the wrong application. Pass --no-server to point deliberately at a server
    // you control (with E2E_BASE_URL).
    throw new Error(
      `something is already listening on ${BASE_URL}, so vite cannot start in ` +
        `--mode a11y. Stop it and re-run, or pass --no-server if that server is ` +
        `deliberately yours.`,
    );
  }
  log(`> starting vite --mode a11y on ${BASE_URL}`);
  const child = spawn(
    "npx",
    ["vite", "--mode", "a11y", "--port", "5173", "--host", "127.0.0.1"],
    { cwd: frontend, stdio: "ignore", detached: false },
  );
  if (!(await wait_for(BASE_URL, 120_000)))
    throw new Error("dev server did not come up within 120s");
  return child;
};

const engine_version = async () => {
  // The engine runs with REST auth enabled, so even /version needs credentials.
  const authorization = `Basic ${Buffer.from(
    `${CREDENTIALS.username}:${CREDENTIALS.password}`,
  ).toString("base64")}`;
  try {
    const res = await fetch(`${BACKEND}/version`, {
      headers: { Authorization: authorization },
      signal: AbortSignal.timeout(5000),
    });
    if (!res.ok) return "unreachable";
    return (await res.json()).version ?? "unknown";
  } catch {
    return "unreachable";
  }
};

const git_commit = () => {
  try {
    return execFileSync("git", ["rev-parse", "--short", "HEAD"], {
      cwd: frontend,
      encoding: "utf8",
    }).trim();
  } catch {
    return "unknown";
  }
};

// ---------------------------------------------------------------------------
// Scanning
// ---------------------------------------------------------------------------

const scan_one = async (context, { route, state }) => {
  const expects_login = route.name === "login";
  // newPage() is inside the try: a browser hiccup on one scan must not abort
  // the whole run and lose every other result.
  let page = null;
  try {
    page = await context.newPage();
    if (state.mock) await state.mock(page);
    // Wait for either the app landmark or the login screen, so a failed login
    // surfaces as a clear diagnostic instead of a 60s timeout on `main`.
    //
    // `state.ready` describes the DOM *after* `state.prepare` has run — a dialog
    // that only exists once its trigger is clicked. Waiting for it here would
    // block on the very thing `prepare` is about to produce, so a prepare-state
    // waits for the ordinary landmark first and for `state.ready` further down.
    const initial_ready = state.prepare
      ? `${DEFAULT_READY}, section.login-page`
      : (state.ready ??
        (expects_login ? "section.login-page" : `${DEFAULT_READY}, section.login-page`));
    await prepare_page(page, {
      path: route.path,
      ready: initial_ready,
      timeout: 60_000,
    });

    // An authenticated route that rendered the login screen means the session
    // was never established. Scanning it anyway would quietly fill the report
    // with clean login pages and read as "no accessibility problems". Only
    // meaningful when the wait above could actually have matched the login
    // screen, which is exactly when it used the generic landmark.
    if (!expects_login && initial_ready.includes("section.login-page")) {
      const landed_on_login = await page
        .locator("section.login-page")
        .first()
        .isVisible()
        .catch(() => false);
      if (landed_on_login)
        return {
          findings: [],
          incomplete: [],
          failed: "NotAuthenticated",
        };
    }
    if (state.prepare) {
      await state.prepare(page);
      if (state.ready)
        await page.locator(state.ready).first().waitFor({ timeout: 15_000 });
      // New DOM may have arrived with its own entry animation.
      await freeze_animations(page);
    }
    const results = await analyze_a11y(page, {
      tags: REPORT_TAGS,
      enableRules: REPORT_ENABLED_RULES,
    });
    return {
      findings: findings_from_axe({
        page: route.name,
        state: state.label,
        results,
      }),
      // axe could not decide these; they need a human.
      incomplete: (results.incomplete ?? []).map((i) => ({
        rule: i.id,
        help: i.help ?? "",
      })),
      failed: null,
    };
  } catch (error) {
    // Only the error CLASS — messages carry timings and selectors that churn.
    return { findings: [], incomplete: [], failed: error.constructor.name };
  } finally {
    await page?.close().catch(() => {});
  }
};

const run_axe = async (browser, routes) => {
  // One task per (route, state); grouped by theme x viewport so each browser
  // context is created once and reused.
  const tasks = routes.flatMap((route) =>
    route.available === false
      ? []
      : [...base_states(), ...interaction_states_for(route.name)].map(
          (state) => ({ route, state }),
        ),
  );

  const groups = new Map();
  for (const task of tasks) {
    const key = `${task.state.theme.id}|${task.state.viewport.id}|${
      task.route.auth ? "auth" : "anon"
    }`;
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(task);
  }

  const results = new Map();
  log(`> ${tasks.length} axe scans across ${groups.size} browser contexts`);

  await Promise.all(
    [...groups.entries()].map(async ([key, group]) => {
      const { state, route } = group[0];
      const context = await browser.newContext({
        colorScheme: state.theme.color_scheme,
        viewport: {
          width: state.viewport.width,
          height: state.viewport.height,
        },
        baseURL: BASE_URL,
      });
      // The app restores a basic-auth session from sessionStorage on load, so
      // seeding it logs us in without driving the form (mirrors fixtures.js).
      if (route.auth)
        await context.addInitScript((creds) => {
          window.sessionStorage.setItem("basic_auth", JSON.stringify(creds));
        }, CREDENTIALS);

      for (const task of group) {
        const outcome = await scan_one(context, task);
        results.set(`${task.route.name}|${task.state.id}`, {
          ...task,
          ...outcome,
        });
      }
      await context.close();
      log(`  · ${key} done (${group.length} scans)`);
    }),
  );

  return results;
};

// ---------------------------------------------------------------------------
// Assembly
// ---------------------------------------------------------------------------

const build_pages = (routes, axe_results, pa11y_by_page) =>
  routes.map((route) => {
    if (route.available === false)
      return {
        name: route.name,
        label: label_for(route.name),
        path: null,
        scanned: false,
        reason: route.reason,
        states: [],
      };

    const states = [
      ...base_states(),
      ...interaction_states_for(route.name),
    ].map((state) => {
      const outcome = axe_results.get(`${route.name}|${state.id}`);
      return {
        id: state.id,
        label: state.label,
        findings: sorted_findings(outcome?.findings ?? []),
        incomplete: outcome?.incomplete ?? [],
        failed: outcome?.failed ?? null,
      };
    });

    // pa11y runs on a reduced axis, so its findings attach to a synthetic state
    // rather than pretending to cover every theme/viewport combination.
    const pa11y = pa11y_by_page.get(route.name);
    if (pa11y?.length)
      states.push({
        id: "pa11y",
        label: "HTML_CodeSniffer (pa11y) · hell · Desktop",
        findings: sorted_findings(pa11y),
        incomplete: [],
        failed: null,
      });

    return {
      name: route.name,
      label: label_for(route.name),
      path: normalize_selector(route.path),
      scanned: true,
      states,
    };
  });

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

const main = async () => {
  const server = args["no-server"] ? null : await start_dev_server();
  const backend_version = await engine_version();
  if (backend_version === "unreachable")
    log(
      `! engine at ${BACKEND} is unreachable — authenticated pages will render ` +
        `error shells and appear as scan failures`,
    );

  const deep = await discover_deep_routes({
    credentials: CREDENTIALS,
    include_unavailable: true,
  });
  const routes = [...STATIC_ROUTES, LOGIN_ROUTE, ...deep].filter(
    (route) => !only_pages || only_pages.includes(route.name),
  );
  if (only_pages)
    log(`! --pages is set: this is a PARTIAL report over ${routes.length} page(s)`);

  const browser = await chromium.launch();
  let axe_results = new Map();
  try {
    // Warm the dev server before fanning out: Vite's first navigation triggers
    // dependency optimisation, which would otherwise time out parallel contexts.
    const warm = await browser.newPage();
    await warm.goto(BASE_URL, { waitUntil: "domcontentloaded", timeout: 120_000 });
    await warm.close();

    axe_results = await run_axe(browser, routes);
  } finally {
    await browser.close();
  }

  const pa11y_by_page = new Map();
  if (use_pa11y) {
    try {
      const { run_pa11y } = await import("./a11y-pa11y.mjs");
      log("> running pa11y (HTML_CodeSniffer)");
      const pa11y_failures = [];
      for (const { route, issues, failed } of await run_pa11y({
        routes: routes.filter((r) => r.available !== false),
        base_url: BASE_URL,
      })) {
        // A failed pa11y scan yields an empty issue list, which is
        // indistinguishable from a clean page once it reaches the report. Say so
        // out loud rather than letting a broken scan read as "no problems".
        if (failed) {
          pa11y_failures.push(`${route.name} (${failed})`);
          continue;
        }
        pa11y_by_page.set(
          route.name,
          findings_from_pa11y({
            page: route.name,
            state: "HTML_CodeSniffer (pa11y) · hell · Desktop",
            issues,
          }),
        );
      }
      if (pa11y_failures.length)
        log(
          `! pa11y could not scan ${pa11y_failures.length} page(s): ` +
            `${pa11y_failures.join(", ")} — they are absent from the pa11y ` +
            `column, NOT clean`,
        );
    } catch (error) {
      log(`! pa11y stage failed (${error.constructor.name}) — continuing`);
    }
  }

  const pages = build_pages(routes, axe_results, pa11y_by_page);
  const findings = pages.flatMap((p) => p.states.flatMap((s) => s.findings));
  const scan_count = [...axe_results.values()].length;

  const generated_at = new Date().toISOString().replace("T", " ").slice(0, 16);
  const commit = git_commit();

  const meta = {
    generated_at: `${generated_at} UTC`,
    commit,
    tags: REPORT_TAGS,
    enabled_rules: REPORT_ENABLED_RULES,
    rule_count: require("axe-core").getRules(REPORT_TAGS).length,
    axe_version: require("axe-core/package.json").version,
    pa11y_version: use_pa11y ? require("pa11y/package.json").version : null,
    axis: "hell / dunkel × Desktop 1440×900 / Mobil 390×844",
    page_count: routes.length,
    scan_count: `${scan_count} axe${use_pa11y ? ` · ${pa11y_by_page.size} pa11y` : ""}`,
    backend: `Operaton ${backend_version}`,
    data_state:
      "über dev-fixtures befüllt (Deployment + feste Instanzzahlen, kein Last-Bot)",
    locale: "en-US",
    notes: `Der Anfragezustand \`LOADING\` wird bewusst nicht geprüft: Einen
flüchtigen Zustand zu erfassen ist zeitkritisch und würde diesen Bericht bei
jedem Lauf verändern. Das \`<html lang="en">\` des Dokuments ist statisch
(\`index.html\`); eine nicht-englische Darstellung ist daher ein echter Verstoß
gegen WCAG 3.1.1, den **keine der hier eingesetzten Engines erkennen kann**.`,
  };

  await mkdir(OUT_DIR, { recursive: true });
  await writeFile(
    resolve(OUT_DIR, "REPORT.md"),
    render_report({ meta, pages }),
    "utf8",
  );

  const timeline_path = resolve(OUT_DIR, "timeline.md");
  const previous = parse_timeline_state(
    await readFile(timeline_path, "utf8").catch(() => ""),
  );
  const entry = next_timeline_entry({
    generated_at: `${generated_at}`,
    commit,
    counts: tally(findings),
    rules: rule_counts(findings),
    previous,
  });
  await writeFile(
    timeline_path,
    render_timeline({ history: [entry, ...previous.history] }),
    "utf8",
  );

  const counts = tally(findings);
  log(
    `> ${counts.total} violations ` +
      `(${counts.critical} critical, ${counts.serious} serious, ` +
      `${counts.moderate} moderate, ${counts.minor} minor) across ${scan_count} scans`,
  );
  log(`> wrote docs/accessibility/REPORT.md and timeline.md`);

  if (server) server.kill();
};

// A report must never fail a build: every path exits 0, including this one.
main()
  .catch((error) => {
    log(`! report generation failed: ${error.stack ?? error}`);
  })
  .finally(() => process.exit(0));
