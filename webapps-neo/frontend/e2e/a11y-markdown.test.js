import {
  next_timeline_entry,
  parse_timeline_state,
  render_report,
  render_timeline,
  render_toc,
  slug,
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

  it("opens with the logo, then the title, then the contents", () => {
    const logo = md.indexOf("![Operaton](../../public/operaton-logo.svg)");
    const title = md.indexOf("# Barrierefreiheitsbericht");
    const toc = md.indexOf("## Inhalt");
    const first = md.indexOf("## Scan-Umgebung");
    expect(logo).toBeGreaterThan(-1);
    expect(logo).toBeLessThan(title);
    expect(title).toBeLessThan(toc);
    // The contents sit after the title but still ahead of the body.
    expect(toc).toBeLessThan(first);
  });

  it("lists every rendered section in the contents", () => {
    const toc_block = md.slice(md.indexOf("## Inhalt"), md.indexOf("## Scan-Umgebung"));
    for (const title of ["Scan-Umgebung", "Zusammenfassung", "Seiten"])
      expect(toc_block).toContain(`- [${title}](#${slug(title)})`);
    // "## Contents" is emitted before the body it describes, so it must not
    // list itself.
    expect(toc_block).not.toContain("- [Inhalt]");
  });

  it("leads with the tables rather than a prose preamble", () => {
    // The scope section was removed on request; the first section after the
    // contents is now the third-party note, then straight into the tables.
    expect(md).not.toContain("What this report does and does not tell you");
    expect(md).not.toContain("NOT checked");
    expect(md.indexOf("## Scan-Umgebung")).toBeLessThan(md.indexOf("## Seiten"));
    // The bpmn-js caveat trails the findings rather than preceding them.
    expect(md.indexOf("## Seiten")).toBeLessThan(
      md.indexOf("## Komponenten von Drittanbietern"),
    );
  });

  it("does not link out to the timeline", () => {
    expect(md).not.toContain("timeline.md");
  });

  it("documents the bpmn-js diagram dependency and its a11y status", () => {
    expect(md).toContain("bpmn-js");
    expect(md).toContain("https://github.com/bpmn-io/bpmn-js");
    expect(md).toContain("https://github.com/bpmn-io/a11y");
    expect(md).toContain("keine WCAG-Konformitätsstufe");
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
    expect(md).toContain("Manuelle Prüfung erforderlich");
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
    expect(render_timeline({ history: [entry] })).toContain("*(Ausgangswert)*");
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

describe("slug", () => {
  it("matches GitHub's anchor derivation", () => {
    expect(slug("Scan environment")).toBe("scan-environment");
    expect(slug("What this report does and does not tell you")).toBe(
      "what-this-report-does-and-does-not-tell-you",
    );
    // Punctuation is dropped, not hyphenated.
    expect(slug("Needs manual review!")).toBe("needs-manual-review");
    // Umlauts survive: GitHub and pandoc both keep them in the anchor.
    expect(slug("Manuelle Prüfung erforderlich")).toBe(
      "manuelle-prüfung-erforderlich",
    );
    expect(slug("Scan-Umgebung")).toBe("scan-umgebung");
  });
});

describe("render_toc", () => {
  it("lists level-two headings only", () => {
    const toc = render_toc("## One\n\n### Nested\n\ntext\n\n## Two");
    expect(toc).toContain("- [One](#one)");
    expect(toc).toContain("- [Two](#two)");
    expect(toc).not.toContain("Nested");
  });

  it("returns null when there is nothing to list", () => {
    expect(render_toc("just prose")).toBeNull();
  });
});
