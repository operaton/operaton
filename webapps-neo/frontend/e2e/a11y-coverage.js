// What the accessibility scanners actually reach, and what they cannot.
//
// The point of this module is to keep the manual test plan honest. The route
// manifest (routes.js) and the state matrix (a11y-states.js) change as the app
// grows; a hand-written "here is what we cover" table would quietly go stale
// and nobody would notice. So the coverage numbers are DERIVED from those two
// modules, and the only thing maintained by hand is the mapping from a user
// path to the routes and specs that belong to it — which a11y-coverage.test.js
// then forces to stay complete.
//
// Pure: no browser, no filesystem, no clock. The runner (a11y-coverage.mjs)
// writes the result to docs/accessibility/COVERAGE.md.

import { cell, code, code_list, section, table } from "./a11y-markdown.js";
import { LOGIN_ROUTE, STATIC_ROUTES } from "./routes.js";
import {
  INTERACTION_STATES,
  base_states,
  interaction_states_for,
} from "./a11y-states.js";

export const ALL_ROUTES = [...STATIC_ROUTES, LOGIN_ROUTE];

// a11y-states.js labels its states in German, because they are rendered into
// the German REPORT.md. This document is English, so the ids are relabelled
// here rather than reusing `state.label`.
export const STATE_LABELS = {
  "global-search": "Global search dialog open",
  "mobile-menu": "Mobile navigation dialog open",
  "upload-dialog": "Deployment upload dialog open",
  "empty-data": "Empty result set",
  "backend-error": "Backend error (request state ERROR)",
  "sso-login": "SSO login (OAuth2 mode)",
};

// Checks that apply to every user path. No scanner performs any of them: they
// are all judgements about meaning, order or behaviour over time.
export const SHARED_MANUAL_CHECKS = [
  "Tab order follows the visual and logical order, and the focus ring is visible on every stop. Menus and lists are one stop each — arrow within them.",
  "Focus returns to the trigger when a dialog closes. (Route-change focus is handled by components/Heading.jsx and covered by focus.spec.js; dialog restore is not.)",
  "Names announced by a screen reader are meaningful, not merely present.",
  "The page is usable at 400% zoom without horizontal scrolling.",
  "No information is carried by colour alone.",
];

/**
 * User paths: the journeys a person actually takes through the app, each
 * claiming the routes it covers and naming the specs that exercise it.
 *
 * `routes` holds route names from routes.js. Every route must be claimed by
 * exactly one path — the unit test enforces it, so adding a route to the
 * manifest fails the build until it is placed here.
 *
 * Interaction states are NOT listed: they are derived from the routes via
 * a11y-states.js, so a new state in the matrix shows up here on its own.
 */
export const USER_PATHS = [
  {
    id: "sign-in",
    label: "Signing in",
    summary:
      "The login screen, the only view rendered while unauthenticated. Backend selector, credential form, language selector.",
    routes: ["login"],
    specs: ["login.spec.js", "a11y.spec.js"],
    manual_checks: [
      "The form is completable with the keyboard alone, and the submit button is reachable without a mouse.",
      "A failed login is announced and leaves focus somewhere recoverable. It currently does not — see the known findings.",
      "Changing the language does not strand focus or lose entered credentials.",
    ],
  },
  {
    id: "dashboard",
    label: "Landing on the dashboard",
    summary:
      "The post-login landing page and the surrounding app chrome: header, primary navigation, skip link.",
    routes: ["dashboard"],
    specs: [
      "dashboard.spec.js",
      "navigation.spec.js",
      "keyboard.spec.js",
      "focus.spec.js",
      "arrow-navigation.spec.js",
      "a11y.spec.js",
    ],
    manual_checks: [
      'The first Tab press reveals the skip link, and activating it moves focus into <main id="content">.',
      "The overview cards are reachable and their link text makes sense out of context.",
      "The Alt+Shift+0..7 navigation shortcuts move focus, not just the route.",
    ],
  },
  {
    id: "tasks",
    label: "Working on tasks",
    summary:
      "The tasklist, task detail with its form/history/attachments/diagram tabs, the task dialogs, starting a process, and the global search dialog.",
    routes: ["tasks", "start-process"],
    specs: [
      "tasks.spec.js",
      "goto.spec.js",
      "keyboard.spec.js",
      "focus.spec.js",
      "arrow-navigation.spec.js",
      "a11y.spec.js",
    ],
    manual_checks: [
      "Selecting a task moves focus to the detail heading (focus.spec.js asserts it) — confirm the announcement that follows is actually useful.",
      "The tab strip follows the APG pattern: arrow keys move, Tab leaves, and the selected panel is announced.",
      "Each task dialog traps focus, closes on Escape, and returns focus to the control that opened it.",
      "Claiming, assigning and completing a task announce their outcome — no automated test covers task completion at all.",
      "The global search combobox announces the active option as the arrow keys move through results.",
    ],
  },
  {
    id: "processes",
    label: "Inspecting processes",
    summary:
      "Deployed definitions, definition detail with its instance/incident/job navigation, and instance detail with its variable and incident tabs, in both live and history mode.",
    routes: ["processes"],
    specs: [
      "processes.spec.js",
      "processes-instance-detail.spec.js",
      "a11y.spec.js",
    ],
    manual_checks: [
      "The BPMN diagram is an SVG canvas with no text alternative — confirm the surrounding controls carry everything a non-sighted user needs.",
      "The live/history toggle announces which mode is active.",
      "Bulk selection announces how many rows are selected, and select-all has an accessible name.",
      "Deep instance routes are only scanned when the engine holds data; walk one by hand even when the scan skipped it.",
    ],
  },
  {
    id: "decisions",
    label: "Inspecting decisions",
    summary:
      "The DMN definition list and decision detail: diagram, definition details, and evaluated instances with their inputs and outputs.",
    routes: ["decisions"],
    specs: ["decisions.spec.js", "a11y.spec.js"],
    manual_checks: [
      "The DMN diagram and decision table are SVG-rendered; check the tabular data is available in an accessible form elsewhere.",
      "Input and output columns of an evaluated instance are associated with their headers when read cell by cell.",
    ],
  },
  {
    id: "deployments",
    label: "Browsing deployments",
    summary:
      "The three-column deployment browser — deployments, their resources, and the resource preview — plus the upload dialog.",
    routes: ["deployments"],
    specs: ["deployments.spec.js", "a11y.spec.js"],
    manual_checks: [
      "Moving between the three columns is possible with the keyboard and the current column is discoverable.",
      "The upload dialog exposes the file input with a real label, and reports success or failure audibly.",
    ],
  },
  {
    id: "batches",
    label: "Monitoring batches",
    summary:
      "The batch list with its running/history toggle and per-batch progress, and batch detail.",
    routes: ["batches"],
    specs: ["batches.spec.js", "a11y.spec.js"],
    manual_checks: [
      "Each <progress> element has an accessible name and its value is announced, not just drawn.",
      "The 'select a batch' empty prompt is announced when no batch is chosen.",
    ],
  },
  {
    id: "migrations",
    label: "Running a migration",
    summary:
      "The three-step migration wizard: select definitions, map activities, configure and execute.",
    routes: ["migrations"],
    specs: ["migrations.spec.js", "a11y.spec.js"],
    manual_checks: [
      "Moving between steps announces which step is now active and where focus went.",
      "The activity mapping controls have names that identify which activity they map — no automated test executes a migration.",
      "Validation errors are announced and focus moves to the first invalid control.",
    ],
  },
  {
    id: "administration",
    label: "Administering users and authorizations",
    summary:
      "Admin sub-navigation over users, groups, tenants, authorizations and system settings, including the authorization resource matrix.",
    routes: ["admin"],
    specs: ["admin.spec.js", "arrow-navigation.spec.js", "a11y.spec.js"],
    manual_checks: [
      "The authorization matrix is navigable cell by cell with its row and column headers announced.",
      "Create, edit and delete flows announce their outcome through the existing aria-live regions.",
      "Destructive actions are distinguishable without relying on colour.",
    ],
  },
  {
    id: "account",
    label: "Managing your own account",
    summary:
      "Profile, password change, and the user's own group and tenant memberships.",
    routes: ["account"],
    specs: ["account.spec.js", "a11y.spec.js"],
    manual_checks: [
      "The password change result is announced by its assertive live region without stealing focus.",
      "Password fields carry the right autocomplete tokens so a password manager can fill them.",
    ],
  },
  {
    id: "help-and-errors",
    label: "Help and error pages",
    summary: "The static help page and the 404 fallback.",
    routes: ["help", "not-found"],
    specs: ["help.spec.js", "not-found.spec.js", "a11y.spec.js"],
    manual_checks: [
      "The 404 page announces that the route was not found rather than presenting an empty shell.",
      "The ALT + K hint is discoverable by a screen reader user who cannot see it.",
    ],
  },
];

// Deep routes are discovered at scan time from live engine data
// (routes.js discover_deep_routes), so they cannot be enumerated statically.
export const DEEP_ROUTES = [
  {
    pattern: "/processes/{definitionId}/instances/{id}/vars",
    path: "processes",
  },
  { pattern: "/tasks/{id}", path: "tasks" },
  { pattern: "/decisions/{id}", path: "decisions" },
];

// Flows no automated test drives at all. Listed so the walkthrough can say
// which steps have no safety net behind them whatsoever.
export const UNAUTOMATED_FLOWS = [
  "Claiming, assigning or completing a task, and submitting a task form",
  "Starting a process instance through to completion",
  "Creating, editing or deleting users, groups, tenants and authorizations",
  "Changing a password or editing the profile",
  "Executing a migration",
  "Deploying a resource through the upload dialog",
  "Creating and editing saved filters",
  "Switching the interface language",
];

const state_label = (id) => STATE_LABELS[id] ?? id;

/** The interaction-state ids a11y-states.js expands for a given page name. */
const state_ids_for = (route_name) =>
  INTERACTION_STATES.filter((state) => state.pages.includes(route_name)).map(
    (state) => state.id,
  );

/**
 * Join the hand-maintained USER_PATHS against the derived route and state data.
 * Everything numeric here comes from routes.js and a11y-states.js, so it tracks
 * those files without anyone remembering to update this one.
 */
export const build_coverage = ({
  all_routes = ALL_ROUTES,
  user_paths = USER_PATHS,
} = {}) => {
  const by_name = new Map(all_routes.map((route) => [route.name, route])),
    // Named by a path but absent from the manifest: a walkthrough left behind
    // after a route was deleted or renamed.
    orphans = [],
    claimed = new Set();

  const paths = user_paths.map((path) => {
    const routes = path.routes.flatMap((name) => {
      const route = by_name.get(name);
      if (!route) {
        orphans.push({ path: path.id, route: name });
        return [];
      }
      claimed.add(name);
      const state_ids = state_ids_for(name);
      return [
        {
          ...route,
          state_ids,
          // Tier A (theme x viewport) plus the expanded tier B states — the
          // same arithmetic the report performs when it scans.
          report_states:
            base_states().length + interaction_states_for(name).length,
        },
      ];
    });

    return {
      ...path,
      routes,
      state_ids: [...new Set(routes.flatMap((route) => route.state_ids))],
      report_states: routes.reduce(
        (total, route) => total + route.report_states,
        0,
      ),
      // The gate visits each route once per engine, in one theme and viewport.
      gate_scans: routes.length * 2,
      deep_routes: DEEP_ROUTES.filter((deep) => deep.path === path.id),
    };
  });

  return {
    paths,
    // Scanned automatically but claimed by no walkthrough — the drift that
    // matters, because the scan keeps passing while nobody looks at the page.
    uncovered: all_routes
      .filter((route) => !claimed.has(route.name))
      .map((route) => route.name),
    orphans,
  };
};

const HEADER = `# Accessibility Coverage

<!-- GENERATED FILE — do not edit. Regenerate with \`npm run a11y:coverage\`. -->

What the automated accessibility layers reach, per user path, and what they
leave for a human. Generated from the route manifest (\`e2e/routes.js\`) and the
state matrix (\`e2e/a11y-states.js\`), so it cannot drift away from what the
scanners actually do.

Read it alongside [Manual Accessibility Testing.md](../Manual%20Accessibility%20Testing.md),
which holds the walkthrough itself. Coverage here means *a scanner visited this
state*, never *this state is accessible* — the gate's own findings live in
[REPORT.md](./REPORT.md).`;

const LEGEND = `## How to read this

- **Gate scans** — visits by \`npm run test:a11y\`: each route once by axe and
  once by pa11y, in one theme and one viewport. The command fails on a
  violation, but no CI job runs it today, so it only bites locally.
- **Report scans** — visits by \`npm run a11y:report\`: every theme × viewport
  combination, plus the interaction states that apply to the route. This layer
  is informational and never fails a build.
- **Manual only** — checks no scanner performs, in addition to the shared list
  below that applies to every path.`;

const shared_checks_section = () =>
  section([
    "## Checks that apply everywhere",
    SHARED_MANUAL_CHECKS.map((check) => `- ${check}`).join("\n"),
  ]);

const summary_table = (paths) =>
  table(
    ["User path", "Routes", "Gate scans", "Report scans", "Specs"],
    paths.map((path) =>
      [
        `[${cell(path.label)}](#${path.id})`,
        // By name, not by path: the login screen and the dashboard both live at
        // `/` and would otherwise render as the same cell.
        code_list(path.routes.map((route) => route.name)),
        String(path.gate_scans),
        String(path.report_states),
        String(path.specs.length),
      ].map(String),
    ),
  );

const path_section = (path) =>
  section([
    `<a id="${path.id}"></a>`,
    `### ${path.label}`,
    path.summary,
    table(
      ["Route", "Path", "Gate", "Report states", "Interaction states"],
      path.routes.map((route) => [
        code(route.name),
        code(route.path),
        "axe + pa11y",
        String(route.report_states),
        route.state_ids.length
          ? route.state_ids.map(state_label).join(", ")
          : "—",
      ]),
    ),
    path.deep_routes.length
      ? `Deep routes, scanned only when the engine holds matching data: ${code_list(
          path.deep_routes.map((deep) => deep.pattern),
        )}.`
      : null,
    `Automated by ${code_list(path.specs)}.`,
    "**Manual only:**",
    path.manual_checks.map((check) => `- ${check}`).join("\n"),
  ]);

const unautomated_section = () =>
  section([
    "## Flows with no automated coverage at all",
    "No spec drives any of these, so a scanner has never seen the states they produce. They are manual-only by default.",
    UNAUTOMATED_FLOWS.map((flow) => `- ${flow}`).join("\n"),
  ]);

const totals_line = (paths) => {
  const gate = paths.reduce((total, path) => total + path.gate_scans, 0),
    report = paths.reduce((total, path) => total + path.report_states, 0);
  return `Across ${paths.length} user paths and ${ALL_ROUTES.length} routes: **${gate}** gate scans and **${report}** report scans.`;
};

// Rendered rather than thrown: an unfinished mapping should be visible in the
// document, not a crash that stops the document being produced at all.
const gaps_section = ({ uncovered, orphans }) => {
  if (!uncovered.length && !orphans.length) return null;
  return section([
    "## Gaps",
    uncovered.length
      ? `> **Gap:** ${code_list(uncovered)} ${uncovered.length === 1 ? "is" : "are"} scanned automatically but appear${uncovered.length === 1 ? "s" : ""} in no walkthrough.`
      : null,
    orphans.length
      ? `> **Gap:** ${orphans.map(({ path, route }) => `\`${route}\` (claimed by *${path}*)`).join(", ")} no longer exist${orphans.length === 1 ? "s" : ""} in the route manifest.`
      : null,
  ]);
};

export const render_coverage = ({ paths, uncovered = [], orphans = [] }) =>
  `${section([
    HEADER,
    LEGEND,
    "## Summary",
    totals_line(paths),
    summary_table(paths),
    gaps_section({ uncovered, orphans }),
    shared_checks_section(),
    "## User paths",
    paths.map(path_section).join("\n\n"),
    unautomated_section(),
  ])}\n`;
