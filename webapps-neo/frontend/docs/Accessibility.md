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

### 1. The gate — `npm run test:a11y`

Runs on the same route manifest with two engines and **fails** on a violation:

- `npm run test:a11y:axe` — axe-core via Playwright (`e2e/a11y.spec.js`), WCAG
  2.0/2.1 level A + AA.
- `npm run test:a11y:pa11y` — pa11y / HTML_CodeSniffer (`e2e/a11y-pa11y.mjs`), a
  techniques-based ruleset that catches things axe deliberately stays silent on.

`e2e/keyboard.spec.js` covers keyboard operability that no scanner can assert:
skip-link ordering, `aria-current="page"`, roving tabindex, Escape handling.

Keep this layer narrow. It only earns its place while it stays green.

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

### 3. Manual testing — the part that matters most

**Automated tooling finds a minority of accessibility problems.** Deque's own
study puts axe-core at roughly 57% of issues by volume, and the commonly cited
figure against WCAG success criteria is 30–40%. Everything below is invisible to
layers 1 and 2:

- whether alt text and labels are *meaningful*, not merely present
- reading and tab order actually matching the visual and logical order
- focus management across route changes and dialog open/close
- screen-reader announcement quality (NVDA, JAWS, VoiceOver)
- error recovery, plain language, cognitive load
- colour used as the only carrier of meaning
- reflow at 400% zoom, and motion sensitivity

So manual testing with assistive technology stays essential. Where possible we
want a test group with personal experience of assistive tools who can give
feedback as users.

### Known third-party limitation: diagrams

The process and decision views embed [bpmn-js](https://github.com/bpmn-io/bpmn-js),
dmn-js and form-js, which render an **SVG canvas** — not screen-reader
accessible by default. bpmn-js publishes no WCAG conformance level. There is an
early upstream effort at [bpmn-io/a11y](https://github.com/bpmn-io/a11y), and
diagram-js has had keyboard improvements since bpmn-js 3.0.0. Until an upstream
text alternative exists, treat every diagram surface as unusable without sight
and make sure the surrounding controls carry the information a user needs.