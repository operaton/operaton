import { existsSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

import {
  ALL_ROUTES,
  STATE_LABELS,
  USER_PATHS,
  build_coverage,
  render_coverage,
} from "./a11y-coverage.js";
import {
  INTERACTION_STATES,
  base_states,
  interaction_states_for,
} from "./a11y-states.js";

const e2e_dir = dirname(fileURLToPath(import.meta.url));

// The assertions that make the coverage table load-bearing rather than
// decorative: they fail when routes.js or a11y-states.js grows something the
// manual test plan has not accounted for.
describe("USER_PATHS completeness", () => {
  const { uncovered, orphans } = build_coverage();

  it("claims every route in the manifest", () => {
    // Adding a route to routes.js without placing it in USER_PATHS fails here.
    expect(uncovered).toEqual([]);
  });

  it("claims no route that has been removed from the manifest", () => {
    expect(orphans).toEqual([]);
  });

  it("claims each route exactly once", () => {
    const claimed = USER_PATHS.flatMap((path) => path.routes);
    expect(claimed).toHaveLength(new Set(claimed).size);
    expect(claimed).toHaveLength(ALL_ROUTES.length);
  });

  it("names only specs that exist on disk", () => {
    for (const path of USER_PATHS)
      for (const spec of path.specs)
        expect(
          existsSync(resolve(e2e_dir, spec)),
          `${path.id} names a missing spec: ${spec}`,
        ).toBe(true);
  });

  it("gives every interaction state an English label", () => {
    // a11y-states.js labels are German; this document is English, so a new
    // state must be relabelled here before it can be rendered.
    for (const state of INTERACTION_STATES)
      expect(STATE_LABELS, `no English label for ${state.id}`).toHaveProperty(
        state.id,
      );
  });

  it("reaches every interaction state through some path's routes", () => {
    const { paths } = build_coverage(),
      reached = new Set(paths.flatMap((path) => path.state_ids));
    for (const state of INTERACTION_STATES) expect(reached).toContain(state.id);
  });
});

describe("build_coverage", () => {
  const { paths } = build_coverage();

  it("counts report states as themes x viewports plus interaction states", () => {
    const tasks_route = paths
      .find((path) => path.id === "tasks")
      .routes.find((route) => route.name === "tasks");
    // The same arithmetic a11y-report.mjs performs: every base state, plus the
    // tier B states expanded over their own theme and viewport axes — which is
    // more than one per state id, so it must come from a11y-states.js and not
    // from counting ids.
    expect(tasks_route.report_states).toBe(
      base_states().length + interaction_states_for("tasks").length,
    );
    expect(interaction_states_for("tasks").length).toBeGreaterThan(
      tasks_route.state_ids.length,
    );
  });

  it("attributes interaction states to the pages that declare them", () => {
    const tasks = paths.find((path) => path.id === "tasks"),
      login = paths.find((path) => path.id === "sign-in");
    expect(tasks.state_ids).toContain("global-search");
    expect(tasks.state_ids).toContain("mobile-menu");
    expect(login.state_ids).toEqual(["sso-login"]);
    expect(login.state_ids).not.toContain("global-search");
  });

  it("counts two gate scans per route, one per engine", () => {
    const help = paths.find((path) => path.id === "help-and-errors");
    expect(help.routes).toHaveLength(2);
    expect(help.gate_scans).toBe(4);
  });

  it("reports a route claimed by no path as uncovered", () => {
    const { uncovered } = build_coverage({
      all_routes: [...ALL_ROUTES, { path: "/ghost", name: "ghost" }],
    });
    expect(uncovered).toEqual(["ghost"]);
  });

  it("reports a path claiming a missing route as an orphan", () => {
    const { orphans } = build_coverage({
      user_paths: [
        {
          id: "stale",
          label: "Stale",
          summary: "",
          routes: ["ghost"],
          specs: [],
          manual_checks: [],
        },
      ],
    });
    expect(orphans).toEqual([{ path: "stale", route: "ghost" }]);
  });
});

describe("render_coverage", () => {
  it("is deterministic", () => {
    expect(render_coverage(build_coverage())).toBe(
      render_coverage(build_coverage()),
    );
  });

  it("marks itself generated so nobody hand-edits it", () => {
    expect(render_coverage(build_coverage())).toContain(
      "<!-- GENERATED FILE — do not edit. Regenerate with `npm run a11y:coverage`. -->",
    );
  });

  it("renders paths in manifest order", () => {
    const markdown = render_coverage(build_coverage()),
      positions = USER_PATHS.map((path) =>
        markdown.indexOf(`<a id="${path.id}"></a>`),
      );
    expect(positions).toEqual([...positions].sort((a, b) => a - b));
    expect(positions).not.toContain(-1);
  });

  it("renders a gap rather than throwing when a route is unclaimed", () => {
    const markdown = render_coverage(
      build_coverage({
        all_routes: [...ALL_ROUTES, { path: "/ghost", name: "ghost" }],
      }),
    );
    expect(markdown).toContain("**Gap:**");
    expect(markdown).toContain("`ghost`");
  });

  it("omits the gaps section entirely when nothing is missing", () => {
    expect(render_coverage(build_coverage())).not.toContain("## Gaps");
  });

  it("escapes pipes so a label cannot break the table", () => {
    const markdown = render_coverage({
      paths: [
        {
          id: "pipe",
          label: "A | B",
          summary: "s",
          specs: ["a11y.spec.js"],
          manual_checks: ["c"],
          routes: [
            {
              name: "a | b",
              path: "/x",
              state_ids: [],
              report_states: 4,
            },
          ],
          state_ids: [],
          report_states: 4,
          gate_scans: 2,
          deep_routes: [],
        },
      ],
    });
    // The raw pipe must never appear unescaped inside a table cell.
    expect(markdown).toContain("a \\| b");
  });
});
