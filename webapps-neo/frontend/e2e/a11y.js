import AxeBuilder from "@axe-core/playwright";
import { expect } from "@playwright/test";

// WCAG 2.0 / 2.1 level A + AA — the levels the web apps target.
const DEFAULT_TAGS = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"];

/**
 * Run an axe-core scan against the current page (optionally scoped) and return
 * the raw results.
 *
 * @param page Playwright page
 * @param opts.include  CSS selector to scope the scan to
 * @param opts.exclude  CSS selector(s) to skip
 * @param opts.disableRules  axe rule ids to skip (with a comment explaining why)
 * @param opts.tags  override the default WCAG tag set
 */
export const analyze_a11y = async (
  page,
  { include, exclude, disableRules, tags } = {},
) => {
  let builder = new AxeBuilder({ page }).withTags(tags ?? DEFAULT_TAGS);
  if (include) builder = builder.include(include);
  if (exclude) builder = builder.exclude(exclude);
  if (disableRules?.length) builder = builder.disableRules(disableRules);
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
