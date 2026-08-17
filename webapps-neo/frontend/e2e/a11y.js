import AxeBuilder from "@axe-core/playwright";
import { expect } from "@playwright/test";

// WCAG 2.0 / 2.1 level A + AA — the levels the web apps target. Used by the
// gating spec (a11y.spec.js), which must stay narrow enough to keep passing.
const DEFAULT_TAGS = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"];

// The wider set used by the informational report (a11y-report.mjs): adds WCAG
// 2.2 AA and axe's best-practice rules.
export const REPORT_TAGS = [
  "wcag2a",
  "wcag2aa",
  "wcag21a",
  "wcag21aa",
  "wcag22aa",
  "best-practice",
];

// `target-size` is the ONLY wcag22aa rule in axe-core, and axe ships it
// disabled by default — so `withTags(["wcag22aa"])` on its own contributes
// nothing at all. It has to be turned on explicitly or "WCAG 2.2 AA" is a
// label with no rules behind it.
export const REPORT_ENABLED_RULES = ["target-size"];

/**
 * Run an axe-core scan against the current page (optionally scoped) and return
 * the raw results.
 *
 * @param page Playwright page
 * @param opts.include  CSS selector to scope the scan to
 * @param opts.exclude  CSS selector(s) to skip
 * @param opts.disableRules  axe rule ids to skip (with a comment explaining why)
 * @param opts.enableRules  axe rule ids to force on (for default-off rules)
 * @param opts.tags  override the default WCAG tag set
 */
export const analyze_a11y = async (
  page,
  { include, exclude, disableRules, enableRules, tags } = {},
) => {
  // AxeBuilder#options() REPLACES the whole option object and
  // AxeBuilder#disableRules() REPLACES option.rules, so enabling and disabling
  // rules cannot be chained — build one merged map and apply it first, then
  // withTags(), which only sets option.runOnly on what is already there.
  const rules = {
    ...Object.fromEntries((enableRules ?? []).map((r) => [r, { enabled: true }])),
    ...Object.fromEntries(
      (disableRules ?? []).map((r) => [r, { enabled: false }]),
    ),
  };

  let builder = new AxeBuilder({ page });
  if (Object.keys(rules).length) builder = builder.options({ rules });
  builder = builder.withTags(tags ?? DEFAULT_TAGS);
  if (include) builder = builder.include(include);
  if (exclude) builder = builder.exclude(exclude);
  return builder.analyze();
};

// Render violations as a readable block so a failing test points straight at
// the rule, the impact, the offending nodes, and the fix docs.
const format_violations = (violations) =>
  violations
    .map((v) => {
      const nodes = v.nodes
        .map((n) => `      ${n.target.join(" ")}`)
        .join("\n");
      return `  [${v.impact ?? "n/a"}] ${v.id} — ${v.help}\n${nodes}\n      ${v.helpUrl}`;
    })
    .join("\n\n");

/**
 * Assert the current page (or a scoped part of it) has no axe violations.
 * Returns the results so callers can make extra assertions if needed.
 */
export const expect_no_a11y_violations = async (page, opts = {}) => {
  const results = await analyze_a11y(page, opts);
  expect(
    results.violations,
    results.violations.length
      ? `\nAccessibility violations found:\n\n${format_violations(results.violations)}\n`
      : "",
  ).toEqual([]);
  return results;
};
