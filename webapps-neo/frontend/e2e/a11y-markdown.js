// Markdown renderers for the accessibility report.
//
// Pure: a model in, a string out. No browser, no filesystem, no clock — the
// timestamp and commit are passed in, so the renderers stay unit-testable
// (a11y-markdown.test.js).

import { compare, impact_rank, sorted, tally } from "./a11y-normalize.js";

// axe reports impact in English and the normalisation layer keys off those raw
// values, so only the DISPLAY is translated here.
export const IMPACT_LABELS = {
  critical: "kritisch",
  serious: "schwerwiegend",
  moderate: "mittel",
  minor: "gering",
};

export const impact_label = (impact) => IMPACT_LABELS[impact] ?? impact ?? "—";

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

export const THIRD_PARTY = `## Komponenten von Drittanbietern

Die Prozess- und Entscheidungsansichten binden **[bpmn-js](https://github.com/bpmn-io/bpmn-js)**,
**dmn-js** und **@bpmn-io/form-js** von [bpmn.io](https://bpmn.io) ein. Diese stellen
Diagramme als **SVG-Canvas** dar, was für Screenreader standardmäßig nicht
zugänglich ist — Screenreader vermitteln Text, keine Grafiken.

- bpmn-js weist **keine WCAG-Konformitätsstufe** aus, und die README enthält
  keinen Abschnitt zur Barrierefreiheit.
- Es gibt eine frühe Upstream-Initiative,
  [\`@bpmn-io/a11y\`](https://github.com/bpmn-io/a11y) — *"Minimal tool to achieve
  bpmn.io accessibility goals"* (MIT, v0.1.0, bislang wenig Aktivität).
- diagram-js, worauf bpmn-js aufbaut, hat seit bpmn-js 3.0.0 Verbesserungen bei
  der Tastaturnavigation erhalten; Kontextmenü und Popup-Menü sind per Tastatur
  erreichbar.

Jede Diagrammfläche in diesem Bericht gilt als **für Screenreader-Nutzende nicht
zugänglich**, solange es keine Textalternative von Upstream gibt. Befunde zu
diesen Teilbäumen sind eine Untergrenze, keine Obergrenze: axe kann die
umgebenden Bedienelemente prüfen, aber nicht feststellen, dass ein
BPMN-Diagramm ohne Sehvermögen unbenutzbar ist.`;

// ---------------------------------------------------------------------------
// Report
// ---------------------------------------------------------------------------

const environment_table = (meta) =>
  table(
    ["Einstellung", "Wert"],
    [
      ["Erstellt", cell(meta.generated_at)],
      ["Commit", code(meta.commit)],
      ["Regelsatz", "WCAG 2.0 / 2.1 / 2.2 Stufe A + AA, zzgl. axe Best Practice"],
      ["axe-core-Tags", meta.tags.map(code).join(" ")],
      [
        "Aktive Regeln",
        `${cell(meta.rule_count)} axe-Regeln${
          meta.enabled_rules?.length
            ? ` (inkl. ${code_list(meta.enabled_rules)}, standardmäßig deaktiviert)`
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
      ["Globale Achse", cell(meta.axis)],
      ["Seiten", cell(meta.page_count)],
      ["Scans", cell(meta.scan_count)],
      ["Backend", cell(meta.backend)],
      ["Datenstand", cell(meta.data_state)],
      ["Sprache", cell(meta.locale)],
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
        cell(page.reason ?? "nicht geprüft"),
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
    "**Gesamt**",
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
      "Seite",
      "Route",
      "Kritisch",
      "Schwerwiegend",
      "Mittel",
      "Gering",
      "Zustände",
      "Schlechtester Zustand",
    ],
    rows,
  );
};

const by_rule_table = (pages) => {
  const findings = all_findings(pages);
  if (!findings.length) return "Keine Verstöße durch eine der Engines gefunden.";

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
      cell(impact_label(entry.impact)),
      String(entry.pages.size),
      sorted(entry.wcag).join(", ") || "—",
      sorted(entry.engines).join(", "),
      entry.help_url ? `[Behebung](${entry.help_url})` : "—",
    ]);

  return table(
    ["Regel", "Auswirkung", "Seiten", "WCAG", "Engines", "Referenz"],
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
  return `## Übereinstimmung der Engines

**${both.length}** WCAG-Kriterien von beiden Engines gefunden · **${
    axe.size - both.length
  }** nur von axe-core · **${pa11y.size - both.length}** nur von pa11y.

Zwei Engines werden mitgeführt, weil HTML_CodeSniffer technikbasiert arbeitet,
axe-core dagegen heuristisch — ihre Überschneidung ist daher bauartbedingt nur
teilweise. Fällt die pa11y-Spalte dauerhaft auf null, lohnt die zweite Engine
ihre Laufzeit nicht mehr.`;
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

  return `## Manuelle Prüfung erforderlich

axe konnte diese nicht automatisch entscheiden — meist, weil eine Farbe hinter
einem Bild oder Verlauf nicht auslesbar ist. Jeder Punkt muss von einer Person
bestätigt oder verworfen werden.

${table(
  ["Regel", "Seiten", "Zu prüfen"],
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
  return `#### \`${finding.rule}\` — ${impact_label(finding.impact)}

${cell(finding.help)}${
    finding.wcag.length ? ` · WCAG ${finding.wcag.join(", ")}` : ""
  }${finding.help_url ? ` · [How to fix](${finding.help_url})` : ""}

- Zustände: ${code_list(states)}
- Fundstellen${finding.truncated ? " (Auszug)" : ""}:
${locations}${finding.truncated ? "\n  - …und weitere" : ""}
- Beispiel: ${code(finding.example_html)}`;
};

const page_section = (page) => {
  if (!page.scanned)
    return `### ${page.label} — \`${page.path ?? "—"}\`

Nicht geprüft: ${cell(page.reason ?? "nicht verfügbar")}.`;

  const state_rows = page.states.map((state) => {
    if (state.failed)
      return [cell(state.label), "—", "—", "—", "—", `Scan fehlgeschlagen (${cell(state.failed)})`];
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
      ["Zustand", "Kritisch", "Schwerwiegend", "Mittel", "Gering", "Hinweis"],
      state_rows,
    ),
    blocks.length ? blocks.join("\n\n") : "_Keine Verstöße in einem geprüften Zustand._",
  ]);
};

// The logo lives in the app's own public/ directory; this path is relative to
// docs/accessibility/REPORT.md so it resolves both on GitHub and for pandoc.
const LOGO = "![Operaton](../../public/operaton-logo.svg)";

/**
 * GitHub's heading-anchor slug: lowercased, punctuation dropped, spaces to
 * hyphens. Pandoc derives its own identifiers the same way, so one table of
 * contents works for the rendered markdown and for the PDF alike.
 */
export const slug = (text) =>
  String(text ?? "")
    .toLowerCase()
    // Unicode-aware: \w would drop the umlauts out of German headings, and
    // GitHub keeps them ("prüfung", not "prfung"). Pandoc keeps them too.
    .replace(/[^\p{L}\p{N}\s-]/gu, "")
    .trim()
    .replace(/\s+/g, "-");

/**
 * Build the contents list from the body that was actually rendered, rather than
 * from a hardcoded list — sections like "Engine agreement" and "Needs manual
 * review" only appear when they have something to say.
 */
export const render_toc = (body) => {
  const entries = [...body.matchAll(/^## (.+)$/gm)].map((match) => match[1]);
  if (!entries.length) return null;
  return `## Inhalt

${entries.map((title) => `- [${title}](#${slug(title)})`).join("\n")}`;
};

export const render_report = ({ meta, pages }) => {
  const body = section([
    "## Scan-Umgebung",
    environment_table(meta),
    meta.notes,
    "## Zusammenfassung",
    summary_table(pages),
    "### Nach Regel",
    by_rule_table(pages),
    agreement_line(pages),
    manual_review_section(pages),
    "## Seiten",
    pages.map(page_section).join("\n\n"),
    // Trails the findings: it is a standing caveat about a dependency, not
    // something the reader needs before the tables.
    THIRD_PARTY,
  ]);

  return `${section([
    LOGO,
    "# Barrierefreiheitsbericht — Operaton Web Apps",
    "<!-- GENERIERTE DATEI — nicht bearbeiten. Neu erzeugen mit `npm run a11y:report`. -->",
    "Nur zur Information: Dieser Bericht lässt niemals einen Build fehlschlagen.",
    render_toc(body),
    body,
  ])}\n`;
};

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
    entry.baseline ? "*(Ausgangswert)*" : `**${signed(entry.delta)}**`,
    entry.baseline ? "—" : code_list(entry.new_rules),
    entry.baseline ? "—" : code_list(entry.resolved_rules),
  ]);

  const state = JSON.stringify(
    { rules: history[0]?.rules ?? {}, history },
    null,
    2,
  );

  return `${section([
    "# Barrierefreiheits-Verlauf — Operaton Web Apps",
    "<!-- GENERIERTE DATEI — nicht bearbeiten. Neu erzeugen mit `npm run a11y:report`. -->",
    `Eine Zeile je erzeugtem Bericht, neueste zuerst. \`Δ\` ist die Änderung der
Gesamtzahl der Verstöße; **Neue Regeln** und **Behobene Regeln** benennen die
axe-/pa11y-Regeln, die hinzugekommen oder entfallen sind — das ist der
handlungsrelevante Teil. Eine Änderung von \`Δ\` allein kann auch nur bedeuten,
dass eine Seite eine Zeile mehr enthält.

Die Zahlen stammen aus denselben normalisierten Befunden wie
[REPORT.md](./REPORT.md) und bewegen sich daher nicht, wenn sich Engine-Daten ändern.`,
    table(
      [
        "Erstellt (UTC)",
        "Commit",
        "Kritisch",
        "Schwerwiegend",
        "Mittel",
        "Gering",
        "Gesamt",
        "Δ",
        "Neue Regeln",
        "Behobene Regeln",
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
