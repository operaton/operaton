// Markdown renderers for the accessibility report.
//
// Pure: a model in, a string out. No browser, no filesystem, no clock — the
// timestamp and commit are passed in, so the renderers stay unit-testable
// (a11y-markdown.test.js).

import { compare, impact_rank, sorted, tally } from "./a11y-normalize.js";

/** Table cells must not contain a raw pipe or a newline. */
const cell = (value) =>
  String(value ?? "")
    .replace(/\r?\n/g, " ")
    .replace(/\|/g, "\\|")
    .trim();

const code = (value) => `\`${cell(value)}\``;

const code_list = (values) =>
  values.length ? values.map(code).join(", ") : "—";

const row = (cells) => `| ${cells.join(" | ")} |`;

const table = (headers, rows) =>
  [
    row(headers),
    row(headers.map(() => "---")),
    ...rows.map((cells) => row(cells)),
  ].join("\n");

const section = (parts) => parts.filter(Boolean).join("\n\n");

const all_findings = (pages) =>
  pages.flatMap((page) => page.states.flatMap((state) => state.findings));

// ---------------------------------------------------------------------------
// Fixed prose. Deliberately stated before any number, so the report can never
// be read as a clean bill of health.
// ---------------------------------------------------------------------------

const SCOPE = `## What this report does and does not tell you

**Automated testing finds a minority of accessibility problems.** Deque's study of
~13,000 pages puts axe-core's coverage at roughly **57% of issues by volume**;
measured against WCAG success criteria the commonly cited figure is **30–40%**.
A page reported as clean below is *not* a page known to be accessible — it is a
page where these particular machine-checkable rules found nothing.

**Checked automatically**

| Area | Examples |
| --- | --- |
| Colour | Text contrast against its background |
| Names | Buttons, links, form fields and dialogs having accessible names |
| ARIA | Valid roles, valid attributes, required parent/child relationships |
| Structure | Landmark regions, heading order, list nesting, table headers |
| Images | Presence of \`alt\` text |
| Keyboard | \`tabindex\` misuse, focusable elements hidden from assistive tech |
| Documents | \`lang\` attribute, unique \`id\`s, page title |
| Targets | Minimum pointer target size (WCAG 2.2) |

**NOT checked — needs a human and assistive technology**

| Area | Why a machine cannot decide |
| --- | --- |
| Meaningful alt text | \`alt="image"\` passes every automated rule and tells a user nothing |
| Reading and tab order | Requires knowing what order the content is *meant* to be in |
| Focus management | Whether focus lands somewhere sensible after a route change or dialog close |
| Screen-reader output | Whether announcements are timely, correct and not overwhelming |
| Error recovery | Whether a user can understand and correct a mistake |
| Plain language | Reading level, jargon, cognitive load |
| Colour as meaning | Whether colour is the *only* thing conveying a distinction |
| Reflow | Usability at 400% zoom and at narrow widths |
| Motion | Whether animation can trigger vestibular symptoms |
| Real AT testing | NVDA, JAWS and VoiceOver behaviour on real hardware |

The project's manual testing commitments are in [Accessibility.md](../Accessibility.md).`;

const THIRD_PARTY = `## Third-party components

The process and decision views embed **[bpmn-js](https://github.com/bpmn-io/bpmn-js)**,
**dmn-js** and **@bpmn-io/form-js** from [bpmn.io](https://bpmn.io). These render
diagrams as an **SVG canvas**, which is not screen-reader accessible by default —
screen readers convey text, not graphics.

- bpmn-js publishes **no WCAG conformance level**, and its README has no
  accessibility section.
- There is an early upstream initiative,
  [\`@bpmn-io/a11y\`](https://github.com/bpmn-io/a11y) — *"Minimal tool to achieve
  bpmn.io accessibility goals"* (MIT, v0.1.0, minimal activity so far).
- diagram-js, which bpmn-js builds on, has had keyboard-navigation improvements
  since bpmn-js 3.0.0, and its context pad and popup menu are keyboard reachable.

Treat every diagram surface in this report as **known-inaccessible to screen
reader users** until an upstream text alternative exists. Findings reported
against those subtrees are a floor, not a ceiling: axe can check the surrounding
controls, but it cannot tell you that a BPMN diagram is unusable without sight.`;

// ---------------------------------------------------------------------------
// Report
// ---------------------------------------------------------------------------

const environment_table = (meta) =>
  table(
    ["Setting", "Value"],
    [
      ["Generated", cell(meta.generated_at)],
      ["Commit", code(meta.commit)],
      ["Ruleset", "WCAG 2.0 / 2.1 / 2.2 Level A + AA, plus axe best-practice"],
      ["axe-core tags", meta.tags.map(code).join(" ")],
      [
        "Rules enabled",
        `${cell(meta.rule_count)} axe rules${
          meta.enabled_rules?.length
            ? ` (incl. ${code_list(meta.enabled_rules)}, off by default)`
            : ""
        }`,
      ],
      [
        "Engines",
        `axe-core ${cell(meta.axe_version)}${
          meta.pa11y_version
            ? ` · pa11y ${cell(meta.pa11y_version)} (HTML_CodeSniffer, WCAG2AA)`
            : ""
        }`,
      ],
      ["Global axis", cell(meta.axis)],
      ["Pages", cell(meta.page_count)],
      ["Scans", cell(meta.scan_count)],
      ["Backend", cell(meta.backend)],
      ["Data state", cell(meta.data_state)],
      ["Locale", cell(meta.locale)],
    ],
  );

const summary_table = (pages) => {
  const rows = pages.map((page) => {
    if (!page.scanned)
      return [
        cell(page.label),
        code(page.path ?? "—"),
        "—",
        "—",
        "—",
        "—",
        "0",
        cell(page.reason ?? "not scanned"),
      ];
    const findings = page.states.flatMap((state) => state.findings);
    const counts = tally(findings);
    const worst = [...page.states]
      .filter((state) => state.findings.length)
      .sort(
        (a, b) =>
          impact_rank(a.findings[0]?.impact) -
            impact_rank(b.findings[0]?.impact) ||
          b.findings.length - a.findings.length,
      )[0];
    return [
      cell(page.label),
      code(page.path),
      String(counts.critical),
      String(counts.serious),
      String(counts.moderate),
      String(counts.minor),
      String(page.states.length),
      worst ? cell(worst.label) : "—",
    ];
  });

  const totals = tally(all_findings(pages));
  rows.push([
    "**Total**",
    "",
    `**${totals.critical}**`,
    `**${totals.serious}**`,
    `**${totals.moderate}**`,
    `**${totals.minor}**`,
    `**${pages.reduce((n, p) => n + p.states.length, 0)}**`,
    "",
  ]);

  return table(
    [
      "Page",
      "Route",
      "Critical",
      "Serious",
      "Moderate",
      "Minor",
      "States",
      "Worst state",
    ],
    rows,
  );
};

const by_rule_table = (pages) => {
  const findings = all_findings(pages);
  if (!findings.length) return "No violations found by either engine.";

  const rules = new Map();
  for (const finding of findings) {
    const existing = rules.get(finding.rule) ?? {
      rule: finding.rule,
      impact: finding.impact,
      wcag: new Set(),
      engines: new Set(),
      pages: new Set(),
      help_url: finding.help_url,
    };
    finding.wcag.forEach((w) => existing.wcag.add(w));
    existing.engines.add(finding.engine);
    existing.pages.add(finding.page);
    if (!existing.help_url) existing.help_url = finding.help_url;
    rules.set(finding.rule, existing);
  }

  const rows = [...rules.values()]
    .sort(
      (a, b) =>
        impact_rank(a.impact) - impact_rank(b.impact) || compare(a.rule, b.rule),
    )
    .map((entry) => [
      code(entry.rule),
      cell(entry.impact),
      String(entry.pages.size),
      sorted(entry.wcag).join(", ") || "—",
      sorted(entry.engines).join(", "),
      entry.help_url ? `[How to fix](${entry.help_url})` : "—",
    ]);

  return table(
    ["Rule", "Impact", "Pages", "WCAG", "Engines", "Reference"],
    rows,
  );
};

const agreement_line = (pages) => {
  const findings = all_findings(pages);
  const criteria = (engine) =>
    new Set(
      findings
        .filter((f) => f.engine === engine)
        .flatMap((f) => f.wcag)
        .filter(Boolean),
    );
  const axe = criteria("axe");
  const pa11y = criteria("pa11y");
  if (!pa11y.size) return null;
  const both = [...axe].filter((c) => pa11y.has(c));
  return `## Engine agreement

**${both.length}** WCAG criteria found by both engines · **${
    axe.size - both.length
  }** by axe-core only · **${pa11y.size - both.length}** by pa11y only.

Two engines are carried because HTML_CodeSniffer is techniques-based where
axe-core is heuristic-based, so their overlap is partial by construction. If the
pa11y-only column reaches zero and stays there, the second engine has stopped
earning its runtime.`;
};

const manual_review_section = (pages) => {
  const rows = pages
    .filter((page) => page.scanned)
    .flatMap((page) =>
      page.states.flatMap((state) =>
        (state.incomplete ?? []).map((item) => ({
          page: page.label,
          state: state.label,
          rule: item.rule,
          help: item.help,
        })),
      ),
    );
  if (!rows.length) return null;

  const grouped = new Map();
  for (const item of rows) {
    const existing = grouped.get(item.rule) ?? {
      rule: item.rule,
      help: item.help,
      pages: new Set(),
    };
    existing.pages.add(item.page);
    grouped.set(item.rule, existing);
  }

  return `## Needs manual review

axe could not decide these automatically — usually because it cannot read a
colour behind an image or gradient. Each needs a human to confirm or dismiss.

${table(
  ["Rule", "Pages", "What to check"],
  [...grouped.values()]
    .sort((a, b) => compare(a.rule, b.rule))
    .map((entry) => [
      code(entry.rule),
      String(entry.pages.size),
      cell(entry.help),
    ]),
)}`;
};

const finding_block = (finding, states) => {
  const locations = finding.locations
    .map((location) => `  - ${code(location)}`)
    .join("\n");
  return `#### \`${finding.rule}\` — ${finding.impact}

${cell(finding.help)}${
    finding.wcag.length ? ` · WCAG ${finding.wcag.join(", ")}` : ""
  }${finding.help_url ? ` · [How to fix](${finding.help_url})` : ""}

- States: ${code_list(states)}
- Locations${finding.truncated ? " (first few)" : ""}:
${locations}${finding.truncated ? "\n  - …and more" : ""}
- Example: ${code(finding.example_html)}`;
};

const page_section = (page) => {
  if (!page.scanned)
    return `### ${page.label} — \`${page.path ?? "—"}\`

Not scanned: ${cell(page.reason ?? "unavailable")}.`;

  const state_rows = page.states.map((state) => {
    if (state.failed)
      return [cell(state.label), "—", "—", "—", "—", `scan failed (${cell(state.failed)})`];
    const counts = tally(state.findings);
    return [
      cell(state.label),
      String(counts.critical),
      String(counts.serious),
      String(counts.moderate),
      String(counts.minor),
      "",
    ];
  });

  // One block per rule, listing which states exhibit it — the same violation
  // repeated across four theme/viewport combinations should read as one item.
  const by_rule = new Map();
  for (const state of page.states)
    for (const finding of state.findings) {
      const existing = by_rule.get(finding.rule) ?? {
        finding,
        states: [],
        locations: new Set(),
      };
      existing.states.push(state.label);
      finding.locations.forEach((l) => existing.locations.add(l));
      existing.truncated = existing.truncated || finding.truncated;
      by_rule.set(finding.rule, existing);
    }

  const blocks = [...by_rule.values()]
    .sort(
      (a, b) =>
        impact_rank(a.finding.impact) - impact_rank(b.finding.impact) ||
        compare(a.finding.rule, b.finding.rule),
    )
    .map((entry) =>
      finding_block(
        {
          ...entry.finding,
          locations: sorted(entry.locations),
          truncated: entry.truncated,
        },
        sorted(entry.states),
      ),
    );

  return section([
    `### ${page.label} — \`${page.path}\``,
    table(
      ["State", "Critical", "Serious", "Moderate", "Minor", "Note"],
      state_rows,
    ),
    blocks.length ? blocks.join("\n\n") : "_No violations in any scanned state._",
  ]);
};

export const render_report = ({ meta, pages }) =>
  `${section([
    "# Accessibility Report — Operaton Web Apps",
    "<!-- GENERATED FILE — do not edit. Regenerate with `npm run a11y:report`. -->",
    `Informational only: this report never fails a build. Trend over time:
[timeline.md](./timeline.md).`,
    SCOPE,
    THIRD_PARTY,
    "## Scan environment",
    environment_table(meta),
    meta.notes,
    "## Summary",
    summary_table(pages),
    "### By rule",
    by_rule_table(pages),
    agreement_line(pages),
    manual_review_section(pages),
    "## Pages",
    pages.map(page_section).join("\n\n"),
  ])}\n`;

// ---------------------------------------------------------------------------
// Timeline
// ---------------------------------------------------------------------------

const STATE_OPEN = "<!-- a11y-state";
const STATE_RE = /<!--\s*a11y-state\s*([\s\S]*?)-->/;

/**
 * The timeline carries its own history as JSON in a trailing HTML comment, so
 * the table is re-rendered from structured data rather than parsed back out of
 * markdown. Pretty-printed so each run adds its own lines to the diff.
 */
export const parse_timeline_state = (markdown) => {
  const match = STATE_RE.exec(markdown ?? "");
  if (!match) return { history: [], rules: null };
  try {
    const parsed = JSON.parse(match[1]);
    return { history: parsed.history ?? [], rules: parsed.rules ?? null };
  } catch {
    return { history: [], rules: null };
  }
};

const signed = (value) =>
  value > 0 ? `+${value}` : value < 0 ? String(value) : "0";

export const render_timeline = ({ history }) => {
  const rows = history.map((entry) => [
    cell(entry.generated_at),
    code(entry.commit),
    String(entry.counts.critical),
    String(entry.counts.serious),
    String(entry.counts.moderate),
    String(entry.counts.minor),
    String(entry.counts.total),
    entry.baseline ? "*(baseline)*" : `**${signed(entry.delta)}**`,
    entry.baseline ? "—" : code_list(entry.new_rules),
    entry.baseline ? "—" : code_list(entry.resolved_rules),
  ]);

  const state = JSON.stringify(
    { rules: history[0]?.rules ?? {}, history },
    null,
    2,
  );

  return `${section([
    "# Accessibility Timeline — Operaton Web Apps",
    "<!-- GENERATED FILE — do not edit. Regenerate with `npm run a11y:report`. -->",
    `One row per generated report, newest first. \`Δ\` is the change in total
violations; **New rules** and **Resolved rules** name the axe/pa11y rules that
appeared or disappeared, which is the actionable part — a change in \`Δ\` alone
can just mean a page gained a row.

Counts come from the same normalised findings as
[REPORT.md](./REPORT.md), so they do not move when engine data does.`,
    table(
      [
        "Generated (UTC)",
        "Commit",
        "Critical",
        "Serious",
        "Moderate",
        "Minor",
        "Total",
        "Δ",
        "New rules",
        "Resolved rules",
      ],
      rows,
    ),
    `${STATE_OPEN}\n${state}\n-->`,
  ])}\n`;
};

/** Build the next history entry from this run's findings and the prior state. */
export const next_timeline_entry = ({
  generated_at,
  commit,
  counts,
  rules,
  previous,
}) => {
  const baseline = previous.history.length === 0;
  const before = previous.history[0];
  const new_rules = sorted(
    Object.keys(rules).filter((r) => !(r in (previous.rules ?? {}))),
  );
  const resolved_rules = sorted(
    Object.keys(previous.rules ?? {}).filter((r) => !(r in rules)),
  );
  return {
    generated_at,
    commit,
    counts,
    rules,
    baseline,
    delta: baseline ? counts.total : counts.total - (before?.counts.total ?? 0),
    new_rules: baseline ? [] : new_rules,
    resolved_rules: baseline ? [] : resolved_rules,
  };
};
