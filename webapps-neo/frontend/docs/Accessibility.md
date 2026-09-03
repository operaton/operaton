# Accessibility

We want the front-end of Operaton, its web app, be as accessible as possible.
There is no perfect accessibility, yet we strive to implement as many
accessibility features as possible and a solid baseline of accessibility.

This file presents a collection of references for implementing accessibility on
the web and specific issues concerning the Operaton front-end applications.

## Knowledge Sources

Official site for web standards: https://www.w3.org/WAI/ARIA/apg/
Mozilla MDN: https://developer.mozilla.org/en-US/docs/Web/Accessibility

Getting started & basics:

- https://www.w3.org/WAI/ARIA/apg/practices/landmark-regions/
- https://www.w3.org/WAI/ARIA/apg/practices/names-and-descriptions/

### Keyboard Navigation:

- https://www.w3.org/WAI/ARIA/apg/practices/keyboard-interface/
- https://developer.mozilla.org/en-US/docs/Learn/Tools_and_testing/Client-side_JavaScript_frameworks/React_accessibility

## Issues

### Split Layout

The split layout pattern can be used when presenting the user with the processes
or tasks page.
An issue is the correct implementation, which isn't properly defined (see GitHub
issues for more information).

https://www.w3.org/WAI/ARIA/apg/patterns/windowsplitter/
https://github.com/w3c/aria-practices/issues/130
https://github.com/w3c/aria-practices/issues/129

An alternative would be a layout, which is controlled by buttons presenting only
a select amount of states (hidden, min, max). This leads to less user
adjustment, but can circumvent the accessibility and some UX issues.

## Testing

There are three layers, and they do different jobs.

| Command | Does | Lives in |
| --- | --- | --- |
| `npm run test:a11y` | The gate: both engines, fails on a violation | `e2e/a11y.spec.js`, `e2e/a11y-pa11y.mjs` |
| `npm run test:a11y:ui` | Watch the gate run in Playwright's UI mode | — |
| `npm run test:a11y:watch` | Watch it in a real, slowed-down browser window | — |
| `npm run test:a11y:firefox` | The same specs in Firefox | — |
| `npm run a11y:report` | The report: wider ruleset, more states, never fails | `e2e/a11y-report.mjs` |
| `npm run a11y:coverage` | Regenerate the manual/automated coverage table | `e2e/a11y-coverage.mjs` |
| `npm run a11y:coverage:check` | Fail if that table is out of date | `e2e/a11y-coverage.mjs` |
| `npm run test:e2e` | The functional e2e suite | `e2e/*.spec.js` |

The procedure for the manual layer is in
[Manual Accessibility Testing.md](Manual%20Accessibility%20Testing.md), and the
screen readers and browser tooling it needs are in
[Accessibility Tooling.md](Accessibility%20Tooling.md).

### 1. The gate — `npm run test:a11y`

Runs on the same route manifest with two engines and **fails** on a violation:

- `npm run test:a11y:axe` — axe-core via Playwright (`e2e/a11y.spec.js`), WCAG
  2.0/2.1 level A + AA.
- `npm run test:a11y:pa11y` — pa11y / HTML_CodeSniffer (`e2e/a11y-pa11y.mjs`), a
  techniques-based ruleset that catches things axe deliberately stays silent on.

Three specs cover what no scanner can assert, because all of it is runtime
behaviour rather than a property of the DOM:

- `e2e/keyboard.spec.js` — skip-link ordering, `aria-current="page"`, the tab
  strip's roving tabindex, Escape handling.
- `e2e/focus.spec.js` — focus on route change and on list selection, and that a
  mouse click draws no focus ring where the keyboard does.
- `e2e/arrow-navigation.spec.js` — menus and lists as a single tab stop, arrowed
  internally.

Keep this layer narrow. It only earns its place while it stays green.

#### Watching a run

An axe scan looks like nothing happening; `keyboard.spec.js` does not, and is
the one worth watching.

```sh
npm run test:a11y:ui        # UI mode: pick specs, watch on change, step back through actions
npm run test:a11y:watch     # headed, one worker, 500 ms between actions
npm run test:a11y:firefox   # the same specs in Firefox
npx playwright install firefox   # one-off, for the line above
```

Firefox is a second pair of eyes, not a second gate — its accessibility tree
differs from Chromium's, and on macOS the system keyboard-navigation setting
changes what `Tab` reaches. The gate, the e2e suite and CI all pin Chromium.

### 2. The report — `npm run a11y:report`

Generates [`docs/accessibility/REPORT.md`](./accessibility/REPORT.md) and
[`docs/accessibility/timeline.md`](./accessibility/timeline.md). Wider than the
gate (adds WCAG 2.2 AA and axe's best-practice rules) and scans states the gate
never sees — dark mode, mobile viewport, open dialogs, empty and error states.
It is informational and **always exits 0**; it never fails a build.

```sh
# engine only, REST auth off, no load-generating bot
docker compose -f docker-compose.a11y.yaml up -d      # podman compose -f docker-compose.a11y.yaml up -d
cd dev-fixtures/bot && node deploy.js \
  && node spawn.js --process orderFulfillment --count 3 \
  && node spawn.js --process insuranceClaim   --count 2
cd ../.. && npm run a11y:report
```

The report is committed and refreshed by the `Accessibility Report` workflow, so
its git history is the trend line.

Alongside it, `npm run a11y:coverage` generates
[`docs/accessibility/COVERAGE.md`](./accessibility/COVERAGE.md) — which routes
and states the scanners reach, per user path, and what is left for a human. It
is derived from `e2e/routes.js` and `e2e/a11y-states.js`, needs no engine, and
`e2e/a11y-coverage.test.js` fails if a route is added to the manifest without
being placed in a user path.

### 3. Manual testing — the part that matters most

**Automated tooling finds a minority of accessibility problems.** Deque's own
study puts axe-core at roughly 57% of issues by volume, and the commonly cited
figure against WCAG success criteria is 30–40%. Everything below is invisible to
layers 1 and 2:

- whether alt text and labels are *meaningful*, not merely present
- reading and tab order actually matching the visual and logical order
- focus management on dialog open/close (route-change focus is now handled —
  see `src/components/Heading.jsx`)
- screen-reader announcement quality (NVDA, JAWS, VoiceOver)
- error recovery, plain language, cognitive load
- colour used as the only carrier of meaning
- reflow at 400% zoom, and motion sensitivity

So manual testing with assistive technology stays essential. The procedure — one
walkthrough per user path, with the coverage table that maps each to the
automated specs — is in
[Manual Accessibility Testing.md](Manual%20Accessibility%20Testing.md). The
screen readers and browser tooling it assumes are set up in
[Accessibility Tooling.md](Accessibility%20Tooling.md).

Where possible we want a test group with personal experience of assistive tools
who can give feedback as users.

### Known third-party limitation: diagrams

The process and decision views embed [bpmn-js](https://github.com/bpmn-io/bpmn-js),
dmn-js and form-js, which render an **SVG canvas** — not screen-reader
accessible by default. bpmn-js publishes no WCAG conformance level. There is an
early upstream effort at [bpmn-io/a11y](https://github.com/bpmn-io/a11y), and
diagram-js has had keyboard improvements since bpmn-js 3.0.0. Until an upstream
text alternative exists, treat every diagram surface as unusable without sight
and make sure the surrounding controls carry the information a user needs.