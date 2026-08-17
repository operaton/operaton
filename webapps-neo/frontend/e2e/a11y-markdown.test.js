import {
  next_timeline_entry,
  parse_timeline_state,
  render_report,
  render_timeline,
} from "./a11y-markdown.js";

const finding = (over = {}) => ({
  engine: "axe",
  page: "tasks",
  state: "default · light · desktop",
  rule: "color-contrast",
  impact: "serious",
  wcag: ["1.4.3"],
  help: "Elements must meet contrast thresholds",
  help_url: "https://example.test/color-contrast",
  locations: ["main tr:nth-child(n) td"],
  truncated: false,
  example_html: "<td>x</td>",
  ...over,
});

const meta = {
  generated_at: "2026-08-17 14:32 UTC",
  commit: "a1b2c3d",
  tags: ["wcag2a", "wcag22aa", "best-practice"],
  enabled_rules: ["target-size"],
  rule_count: 100,
  axe_version: "4.12.1",
  pa11y_version: "9.1.1",
  axis: "light / dark × desktop / mobile",
  page_count: 16,
  scan_count: "93 axe",
  backend: "Operaton 1.0.0",
  data_state: "seeded",
  locale: "en-US",
  notes: "",
};

const pages = [
  {
    name: "tasks",
    label: "Tasks",
    path: "/tasks",
    scanned: true,
    states: [
      {
        id: "light-desktop",
        label: "default · light · desktop",
        findings: [finding()],
        incomplete: [{ rule: "color-contrast", help: "check the gradient" }],
        failed: null,
      },
      {
        id: "dark-desktop",
        label: "default · dark · desktop",
        findings: [finding({ state: "default · dark · desktop" })],
        incomplete: [],
        failed: null,
      },
    ],
  },
  {
    name: "migrations",
    label: "Migrations",
    path: null,
    scanned: false,
    reason: "engine holds no migratable definitions",
    states: [],
  },
];

describe("render_report", () => {
  const md = render_report({ meta, pages });

  it("states the limits of automated testing before any numbers", () => {
    const scope = md.indexOf("What this report does and does not tell you");
    const summary = md.indexOf("## Summary");
    expect(scope).toBeGreaterThan(-1);
    expect(scope).toBeLessThan(summary);
  });

  it("names what automated testing cannot cover", () => {
    expect(md).toContain("NOT checked");
    expect(md).toContain("Meaningful alt text");
    expect(md).toContain("NVDA");
  });

  it("documents the bpmn-js diagram dependency and its a11y status", () => {
    expect(md).toContain("bpmn-js");
    expect(md).toContain("https://github.com/bpmn-io/bpmn-js");
    expect(md).toContain("https://github.com/bpmn-io/a11y");
    expect(md).toContain("no WCAG conformance level");
  });

  it("collapses a rule seen in several states into one block", () => {
    // color-contrast appears in both states; it should be reported once, with
    // both state labels listed.
    const blocks = md.match(/#### `color-contrast`/g) ?? [];
    expect(blocks).toHaveLength(1);
    expect(md).toContain("default · light · desktop");
    expect(md).toContain("default · dark · desktop");
  });

  it("keeps unscanned pages visible rather than dropping them", () => {
    expect(md).toContain("Migrations");
    expect(md).toContain("engine holds no migratable definitions");
  });

  it("surfaces axe incomplete results as manual review", () => {
    expect(md).toContain("Needs manual review");
    expect(md).toContain("check the gradient");
  });

  it("ends with exactly one newline", () => {
    expect(md.endsWith("\n")).toBe(true);
    expect(md.endsWith("\n\n")).toBe(false);
  });

  it("escapes pipes so a selector cannot break the table", () => {
    const piped = render_report({
      meta,
      pages: [
        {
          ...pages[0],
          states: [
            {
              ...pages[0].states[0],
              findings: [finding({ help: "a | b" })],
              incomplete: [],
            },
          ],
        },
      ],
    });
    expect(piped).toContain("a \\| b");
  });
});

describe("timeline", () => {
  it("writes a baseline row on the first run", () => {
    const entry = next_timeline_entry({
      generated_at: "2026-08-17 14:32",
      commit: "a1b2c3d",
      counts: { critical: 0, serious: 3, moderate: 1, minor: 0, total: 4 },
      rules: { "color-contrast": 3, region: 1 },
      previous: { history: [], rules: null },
    });
    expect(entry.baseline).toBe(true);
    expect(entry.new_rules).toEqual([]);
    expect(render_timeline({ history: [entry] })).toContain("*(baseline)*");
  });

  it("computes the delta and names new and resolved rules", () => {
    const first = next_timeline_entry({
      generated_at: "2026-08-14 09:11",
      commit: "9f8e7d6",
      counts: { critical: 0, serious: 3, moderate: 1, minor: 0, total: 4 },
      rules: { "color-contrast": 3, label: 1 },
      previous: { history: [], rules: null },
    });
    const second = next_timeline_entry({
      generated_at: "2026-08-17 14:32",
      commit: "a1b2c3d",
      counts: { critical: 0, serious: 1, moderate: 1, minor: 0, total: 2 },
      rules: { "color-contrast": 1, "target-size": 1 },
      previous: { history: [first], rules: first.rules },
    });
    expect(second.delta).toBe(-2);
    expect(second.new_rules).toEqual(["target-size"]);
    expect(second.resolved_rules).toEqual(["label"]);

    const md = render_timeline({ history: [second, first] });
    expect(md).toContain("**-2**");
    expect(md).toContain("`target-size`");
    expect(md).toContain("`label`");
  });

  it("round-trips its embedded state so the next run can diff against it", () => {
    const entry = next_timeline_entry({
      generated_at: "2026-08-17 14:32",
      commit: "a1b2c3d",
      counts: { critical: 0, serious: 1, moderate: 0, minor: 0, total: 1 },
      rules: { region: 1 },
      previous: { history: [], rules: null },
    });
    const parsed = parse_timeline_state(render_timeline({ history: [entry] }));
    expect(parsed.rules).toEqual({ region: 1 });
    expect(parsed.history).toHaveLength(1);
  });

  it("treats a missing or corrupt file as a first run", () => {
    expect(parse_timeline_state("")).toEqual({ history: [], rules: null });
    expect(parse_timeline_state("<!-- a11y-state {oops -->")).toEqual({
      history: [],
      rules: null,
    });
  });
});
