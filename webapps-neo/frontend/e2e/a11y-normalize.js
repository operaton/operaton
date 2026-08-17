// Determinism layer for the accessibility report.
//
// The report is committed, and its timeline records +/- deltas between runs, so
// an unchanged app must yield unchanged FINDINGS. Raw engine output does not:
// selectors carry live entity ids and row indexes, so a table that grew by one
// row would look like a new accessibility problem.
//
// Everything here is pure — no browser, no clock, no I/O — so it is unit-tested
// directly (a11y-normalize.test.js).

// A process/decision instance id.
const UUID =
  /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/gi;
// A definition id: `orderFulfillment:3:<uuid>` — matched AFTER the uuid pass has
// already reduced the tail to {id}.
const DEFINITION_ID = /[A-Za-z_][\w-]*:\d+:\{id\}/g;
// Positional selectors. `tr:nth-child(37)` tracks how many rows the engine
// happened to hold; collapsing it is what stops row counts leaking into the
// report.
const NTH = /:nth-(child|of-type)\(\s*\d+\s*\)/g;
const ISO_DATE = /\d{4}-\d{2}-\d{2}(?:[T ]\d{2}:\d{2}(?::\d{2})?(?:\.\d+)?Z?)?/g;
// Long digit runs (counts, epoch millis, ports in hrefs).
const LONG_NUMBER = /\d{4,}/g;

/** Sort by codepoint, not locale — immune to ICU differences between machines. */
export const compare = (a, b) => (a < b ? -1 : a > b ? 1 : 0);

export const sorted = (values) => [...values].sort(compare);

export const unique_sorted = (values) => sorted(new Set(values));

/**
 * Strip everything volatile from a CSS selector or a URL path.
 * Order is significant: uuid -> definition id -> positional -> dates -> numbers.
 */
export const normalize_selector = (value) =>
  String(value ?? "")
    .replace(UUID, "{id}")
    .replace(DEFINITION_ID, "{definitionId}")
    .replace(NTH, ":nth-$1(n)")
    .replace(ISO_DATE, "{date}")
    .replace(LONG_NUMBER, "{n}")
    .trim();

/** axe reports `target` as an array of frame-descending selectors. */
export const normalize_target = (target) =>
  (Array.isArray(target) ? target : [target]).map(normalize_selector).join(" ");

const MAX_HTML = 120;

/** Collapse an html snippet to a single stable line. */
export const normalize_html = (html) => {
  const flat = normalize_selector(String(html ?? "").replace(/\s+/g, " "));
  return flat.length > MAX_HTML ? `${flat.slice(0, MAX_HTML)}…` : flat;
};

// Locations are capped so a page with 200 identical failures does not dominate
// the file. The overflow marker is deliberately non-numeric — a count would
// churn every time the seeded data shifted by one row.
export const MAX_LOCATIONS = 10;

export const cap_locations = (locations) => {
  const all = unique_sorted(locations);
  return all.length > MAX_LOCATIONS
    ? { locations: all.slice(0, MAX_LOCATIONS), truncated: true }
    : { locations: all, truncated: false };
};

/**
 * axe tags carry the WCAG criterion: `wcag143` -> 1.4.3, `wcag258` -> 2.5.8,
 * `wcag1412` -> 1.4.12. Principle and guideline are always one digit; whatever
 * remains is the criterion.
 */
export const wcag_from_axe_tags = (tags = []) =>
  unique_sorted(
    tags
      .map((tag) => /^wcag(\d)(\d)(\d+)$/.exec(tag))
      .filter(Boolean)
      .map(([, principle, guideline, criterion]) =>
        [principle, guideline, criterion].join("."),
      ),
  );

/**
 * HTMLCS codes look like
 * `WCAG2AA.Principle1.Guideline1_4.1_4_3.G18.Fail` — the fourth segment is the
 * criterion, the fifth the technique.
 */
export const parse_pa11y_code = (code) => {
  const parts = String(code ?? "").split(".");
  const criterion = parts[3]?.replace(/_/g, ".");
  return {
    wcag: /^\d+(\.\d+)+$/.test(criterion ?? "") ? [criterion] : [],
    technique: parts[4] ?? "",
  };
};

export const IMPACT_ORDER = ["critical", "serious", "moderate", "minor"];

export const impact_rank = (impact) => {
  const index = IMPACT_ORDER.indexOf(impact);
  return index === -1 ? IMPACT_ORDER.length : index;
};

/** pa11y/HTMLCS has no impact grading; map its two levels onto axe's scale. */
const PA11Y_IMPACT = { error: "serious", warning: "moderate", notice: "minor" };

/** Raw axe results -> normalized findings for one (page, state). */
export const findings_from_axe = ({ page, state, results }) =>
  (results?.violations ?? []).map((violation) => {
    const { locations, truncated } = cap_locations(
      violation.nodes.map((node) => normalize_target(node.target)),
    );
    return {
      engine: "axe",
      page,
      state,
      rule: violation.id,
      impact: violation.impact ?? "minor",
      wcag: wcag_from_axe_tags(violation.tags),
      help: violation.help ?? "",
      help_url: violation.helpUrl ?? "",
      locations,
      truncated,
      example_html: normalize_html(violation.nodes[0]?.html),
    };
  });

/** Raw pa11y issues -> normalized findings for one (page, state). */
export const findings_from_pa11y = ({ page, state, issues }) => {
  const by_code = new Map();
  for (const issue of issues ?? []) {
    const existing = by_code.get(issue.code) ?? {
      engine: "pa11y",
      page,
      state,
      rule: issue.code,
      impact: PA11Y_IMPACT[issue.type] ?? "minor",
      ...parse_pa11y_code(issue.code),
      help: issue.message ?? "",
      help_url: "",
      raw_locations: [],
      example_html: normalize_html(issue.context),
    };
    existing.raw_locations.push(normalize_selector(issue.selector));
    by_code.set(issue.code, existing);
  }
  return sorted_findings(
    [...by_code.values()].map(({ raw_locations, ...finding }) => {
      const { locations, truncated } = cap_locations(raw_locations);
      return { ...finding, locations, truncated };
    }),
  );
};

/** Canonical ordering: worst impact first, then rule id, then state. */
export const sorted_findings = (findings) =>
  [...findings].sort(
    (a, b) =>
      impact_rank(a.impact) - impact_rank(b.impact) ||
      compare(a.rule, b.rule) ||
      compare(a.state, b.state),
  );

/** Per-impact tally used by both the summary table and the timeline. */
export const tally = (findings) => {
  const counts = Object.fromEntries(IMPACT_ORDER.map((i) => [i, 0]));
  for (const finding of findings) counts[finding.impact] += 1;
  return { ...counts, total: findings.length };
};

/**
 * The timeline's delta state: how many findings each rule accounts for. Rule ids
 * are stable across runs in a way that raw counts are not, so this is what
 * "new" and "resolved" are computed from.
 */
export const rule_counts = (findings) => {
  const counts = {};
  for (const finding of findings)
    counts[finding.rule] = (counts[finding.rule] ?? 0) + 1;
  return Object.fromEntries(
    sorted(Object.keys(counts)).map((rule) => [rule, counts[rule]]),
  );
};

/** Compare two rule_counts maps -> what appeared and what went away. */
export const diff_rules = (previous, current) => {
  const before = previous ?? {};
  return {
    new_rules: sorted(Object.keys(current).filter((r) => !(r in before))),
    resolved_rules: sorted(Object.keys(before).filter((r) => !(r in current))),
  };
};
