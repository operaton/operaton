import {
  cap_locations,
  compare,
  diff_rules,
  findings_from_axe,
  findings_from_pa11y,
  normalize_html,
  normalize_selector,
  normalize_target,
  parse_pa11y_code,
  rule_counts,
  sorted_findings,
  tally,
  wcag_from_axe_tags,
} from "./a11y-normalize.js";

describe("normalize_selector", () => {
  it("replaces process instance uuids", () => {
    expect(
      normalize_selector("/tasks/6f1a2b3c-4d5e-6f70-8192-a3b4c5d6e7f8"),
    ).toBe("/tasks/{id}");
  });

  it("replaces a definition id after the uuid pass", () => {
    expect(
      normalize_selector(
        "/processes/orderFulfillment:3:6f1a2b3c-4d5e-6f70-8192-a3b4c5d6e7f8/instances",
      ),
    ).toBe("/processes/{definitionId}/instances");
  });

  it("collapses positional selectors so row counts do not leak", () => {
    // This is the load-bearing one: 37 failing table rows must not read as a
    // different finding from 12 failing table rows.
    expect(normalize_selector("tbody > tr:nth-child(37) > td")).toBe(
      "tbody > tr:nth-child(n) > td",
    );
    expect(normalize_selector("li:nth-of-type(4)")).toBe("li:nth-of-type(n)");
    expect(normalize_selector("tr:nth-child(37)")).toBe(
      normalize_selector("tr:nth-child(12)"),
    );
  });

  it("replaces dates and long digit runs", () => {
    expect(normalize_selector("[data-start='2026-08-17T09:11:00Z']")).toBe(
      "[data-start='{date}']",
    );
    expect(normalize_selector("#item-948213")).toBe("#item-{n}");
  });

  it("leaves short numbers alone", () => {
    expect(normalize_selector(".col-3")).toBe(".col-3");
  });
});

describe("normalize_target", () => {
  it("joins axe's frame-descending selector array", () => {
    expect(normalize_target(["#app", "tr:nth-child(9) td"])).toBe(
      "#app tr:nth-child(n) td",
    );
  });
});

describe("normalize_html", () => {
  it("collapses whitespace and truncates", () => {
    expect(normalize_html("<a\n   href='#'>  hi </a>")).toBe(
      "<a href='#'> hi </a>",
    );
    expect(normalize_html("x".repeat(200))).toHaveLength(121); // 120 + ellipsis
  });
});

describe("wcag_from_axe_tags", () => {
  it("parses criteria of varying length", () => {
    expect(
      wcag_from_axe_tags(["wcag2aa", "wcag143", "wcag258", "wcag1412"]),
    ).toEqual(["1.4.12", "1.4.3", "2.5.8"]);
  });

  it("ignores non-criterion tags", () => {
    expect(wcag_from_axe_tags(["best-practice", "cat.forms"])).toEqual([]);
  });
});

describe("parse_pa11y_code", () => {
  it("extracts the criterion and technique", () => {
    expect(
      parse_pa11y_code("WCAG2AA.Principle1.Guideline1_4.1_4_3.G18.Fail"),
    ).toEqual({ wcag: ["1.4.3"], technique: "G18" });
  });

  it("degrades on an unexpected shape", () => {
    expect(parse_pa11y_code("nonsense")).toEqual({ wcag: [], technique: "" });
  });
});

describe("cap_locations", () => {
  it("dedupes and sorts", () => {
    expect(cap_locations(["b", "a", "b"])).toEqual({
      locations: ["a", "b"],
      truncated: false,
    });
  });

  it("caps without emitting a churning count", () => {
    const many = Array.from({ length: 25 }, (_, i) => `sel-${String(i)}`);
    const result = cap_locations(many);
    expect(result.locations).toHaveLength(10);
    expect(result.truncated).toBe(true);
  });
});

describe("findings_from_axe", () => {
  const results = {
    violations: [
      {
        id: "color-contrast",
        impact: "serious",
        tags: ["wcag2aa", "wcag143"],
        help: "Elements must meet contrast thresholds",
        helpUrl: "https://example.test/color-contrast",
        nodes: [
          { target: ["tr:nth-child(3) td"], html: "<td>a</td>" },
          { target: ["tr:nth-child(9) td"], html: "<td>b</td>" },
        ],
      },
    ],
  };

  it("normalises nodes into deduped locations", () => {
    const [finding] = findings_from_axe({
      page: "tasks",
      state: "light · desktop",
      results,
    });
    // Both nodes normalise to the same location.
    expect(finding.locations).toEqual(["tr:nth-child(n) td"]);
    expect(finding.wcag).toEqual(["1.4.3"]);
    expect(finding.engine).toBe("axe");
  });
});

describe("findings_from_pa11y", () => {
  it("groups issues by code and maps type onto an impact", () => {
    const findings = findings_from_pa11y({
      page: "tasks",
      state: "pa11y",
      issues: [
        {
          code: "WCAG2AA.Principle1.Guideline1_4.1_4_3.G18.Fail",
          type: "error",
          message: "contrast",
          selector: "#a",
          context: "<b>x</b>",
        },
        {
          code: "WCAG2AA.Principle1.Guideline1_4.1_4_3.G18.Fail",
          type: "error",
          message: "contrast",
          selector: "#b",
          context: "<b>y</b>",
        },
      ],
    });
    expect(findings).toHaveLength(1);
    expect(findings[0].impact).toBe("serious");
    expect(findings[0].locations).toEqual(["#a", "#b"]);
  });
});

describe("sorted_findings", () => {
  it("orders by impact, then rule id", () => {
    const order = sorted_findings([
      { impact: "minor", rule: "a", state: "" },
      { impact: "critical", rule: "z", state: "" },
      { impact: "serious", rule: "b", state: "" },
    ]).map((f) => f.rule);
    expect(order).toEqual(["z", "b", "a"]);
  });
});

describe("tally", () => {
  it("counts every impact bucket, including empty ones", () => {
    expect(tally([{ impact: "serious" }, { impact: "serious" }])).toEqual({
      critical: 0,
      serious: 2,
      moderate: 0,
      minor: 0,
      total: 2,
    });
  });
});

describe("rule_counts / diff_rules", () => {
  it("reports rules that appeared and disappeared", () => {
    const before = rule_counts([{ rule: "label" }, { rule: "region" }]);
    const after = rule_counts([{ rule: "region" }, { rule: "target-size" }]);
    expect(diff_rules(before, after)).toEqual({
      new_rules: ["target-size"],
      resolved_rules: ["label"],
    });
  });

  it("treats a missing previous state as an all-new baseline", () => {
    expect(diff_rules(null, { label: 1 })).toEqual({
      new_rules: ["label"],
      resolved_rules: [],
    });
  });
});

describe("compare", () => {
  it("sorts by codepoint, not locale", () => {
    // localeCompare would place "a" before "B"; codepoint order must not.
    expect(["a", "B"].sort(compare)).toEqual(["B", "a"]);
  });
});
