# Accessibility Requirements

What the web apps owe a keyboard user and a screen-reader user, stated as
requirements a test can fail on.

## Why this document exists

The automated layers described in [Accessibility.md](Accessibility.md) answer
one question well — *does this page contain a WCAG violation?* — and a second
one narrowly: `keyboard.spec.js`, `focus.spec.js` and `arrow-navigation.spec.js`
assert that particular attributes hold on particular elements.

Neither answers the question that actually matters: **can somebody who cannot
see the screen, or cannot use a mouse, get their work done?** A route can pass
every axe rule, expose a correct roving `tabindex`, and still be unusable — the
`/tasks/start` scan was green throughout the period in which choosing a process
announced nothing, starting one announced nothing, and the resulting focus went
to `<body>`. Nothing was *wrong* with the DOM at any instant the scanner looked
at it. The flow was broken between the instants.

So the requirements below are written as **journeys**, in the order a person
performs them, and each one names the assistive-technology outcome rather than
the markup that happens to produce it. A requirement is met when the test that
carries it passes for the right reason, not when a selector matches.

## How to read this

| Field | Meaning |
| --- | --- |
| **ID** | Stable handle. Cite it in a commit or an issue. |
| **WCAG** | The success criterion the requirement serves. `—` means it is a usability requirement with no single criterion behind it. |
| **Verified by** | The spec that fails when the requirement is not met, or `manual` when no automated check is possible. |

Requirements are numbered per journey. Deleting one is a decision; renumbering
the rest is not, so numbers are never reused.

Scope: the requirements cover keyboard operation and what assistive technology
is told. They deliberately say nothing about whether a feature works — that is
the functional suite's job (`npm run test:e2e`), and duplicating it here would
make this layer fail for reasons that have nothing to do with accessibility.

---

## G — Global: the chrome on every page

| ID | Requirement | WCAG | Verified by |
| --- | --- | --- | --- |
| G-1 | The first `Tab` press of any page reaches a skip link that moves focus into `main#content`. | 2.4.1 A | `keyboard.spec.js` |
| G-2 | The primary navigation is a single tab stop; the arrow keys move within it and `Tab` leaves it. | 2.1.1 A | `arrow-navigation.spec.js` |
| G-3 | The link for the current route, and only that link, carries `aria-current="page"`. | 4.1.2 A | `keyboard.spec.js` |
| G-4 | Changing page moves focus to the new page's `h1`; arriving on the first page of a session does not. | 2.4.3 A | `focus.spec.js` |
| G-5 | A keyboard route change shows a focus indicator; a mouse click does not. | 2.4.7 AA | `focus.spec.js` |
| G-6 | Every page exposes exactly one `h1` for focus to land on. | 1.3.1 A | `focus.spec.js` |
| G-7 | Every route is free of WCAG 2.0/2.1 A and AA violations under axe and pa11y. | multiple | `a11y.spec.js`, `a11y-pa11y.mjs` |
| G-8 | An outcome that survives no visible element — a flow ending in a route change — is announced in a polite live region that outlives it. | 4.1.3 AA | `a11y-start-process.spec.js`, `a11y-task-work.spec.js` |

G-8 is served by `src/components/Announcer.jsx`, mounted once outside the
router. Pages whose outcome stays on screen (Admin, Account, Migrations) keep
their own `aria-live` regions instead; those are correct where the element doing
the announcing survives the thing it announces.

---

## SI — Signing in

The only view rendered while unauthenticated, and the one place where a user who
cannot recover is stuck outside the product entirely.

| ID | Requirement | WCAG | Verified by |
| --- | --- | --- | --- |
| SI-1 | The credential form is completable and submittable with the keyboard alone, in the visual order: user name, password, submit. | 2.1.1 A, 2.4.3 A | `a11y-login.spec.js` |
| SI-2 | Both credential fields have a programmatically associated label, and carry the `autocomplete` tokens a password manager needs (`username`, `current-password`). | 1.3.5 AA, 3.3.2 A | `a11y-login.spec.js` |
| SI-3 | A rejected credential keeps the user on the login form. It must never replace the screen with an empty document. | 3.3.1 A | `a11y-login.spec.js` |
| SI-4 | A rejected credential is announced without the user moving focus, and the message says what to do next. | 3.3.1 A, 4.1.3 AA | `a11y-login.spec.js` |
| SI-5 | The failed-login state is itself free of axe violations — an error state is a state the gate must scan, not a state it skips. | multiple | `a11y-login.spec.js` |
| SI-6 | Changing the interface language strands neither focus nor the credentials already typed. | 3.2.2 A | manual |
| SI-7 | The backend selector, when more than one backend is configured, has an accessible name. | 4.1.2 A | `keyboard.spec.js` |

---

## SP — Starting a process

The journey the previous suite covered least and needed most: `/tasks/start` was
scanned, but nothing ever selected a definition or started an instance, so every
requirement below except SP-8 was unverified.

| ID | Requirement | WCAG | Verified by |
| --- | --- | --- | --- |
| SP-1 | The list of startable definitions costs **one** tab stop, not one per definition. | 2.4.3 A | `a11y-start-process.spec.js` |
| SP-2 | The arrow keys move between definitions, `Home`/`End` reach the ends, and neither scrolls the page. | 2.1.1 A | `a11y-start-process.spec.js` |
| SP-3 | Choosing a definition moves the reading position to the start form's heading, which names the process chosen. | 2.4.3 A, 4.1.3 AA | `a11y-start-process.spec.js` |
| SP-4 | Every control of the start form has an accessible name, the business key included. | 4.1.2 A, 3.3.2 A | `a11y-start-process.spec.js` |
| SP-5 | The whole journey — reach the list, choose a definition, fill the form, start the instance — is possible with the keyboard alone, with no pointer event at any step. | 2.1.1 A | `a11y-start-process.spec.js` |
| SP-6 | Starting an instance announces that it started, and names the process. | 4.1.3 AA | `a11y-start-process.spec.js` |
| SP-7 | Starting an instance leaves focus on the heading of the view it lands on. Focus must not fall back to `<body>`. | 2.4.3 A | `a11y-start-process.spec.js` |
| SP-8 | The page is free of axe violations both with no definition chosen and with a start form rendered. | multiple | `a11y-start-process.spec.js`, `a11y.spec.js` |
| SP-9 | A start form that fails validation says so in an alert and leaves the entered values in place. | 3.3.1 A | manual — the fixture definitions carry no required start-form field |

---

## TW — Working on a task

| ID | Requirement | WCAG | Verified by |
| --- | --- | --- | --- |
| TW-1 | The task list costs one tab stop; arrow keys move by row and the stop follows the selected row. | 2.4.3 A | `arrow-navigation.spec.js` |
| TW-2 | Selecting a task moves the reading position to the detail heading, not to the page `h1` and not nowhere. | 2.4.3 A | `focus.spec.js` |
| TW-3 | The detail tab strip follows the APG pattern: one tab stop, arrow keys move between tabs, and the selected tab labels its panel. | 4.1.2 A | `a11y-task-work.spec.js`, `keyboard.spec.js` |
| TW-4 | The tab panel is reachable by `Tab` and is named by the tab that selected it. | 2.4.3 A, 4.1.2 A | `a11y-task-work.spec.js` |
| TW-5 | Every task dialog opens from the keyboard, and opening it moves focus inside. | 2.1.1 A | `a11y-task-work.spec.js` |
| TW-6 | Every task dialog closes on `Escape` and returns focus to the control that opened it. | 2.1.2 A, 2.4.3 A | `a11y-task-work.spec.js` |
| TW-7 | Every task dialog has an accessible name. | 4.1.2 A | `a11y-task-work.spec.js` |
| TW-8 | Completing a task announces that it completed. | 4.1.3 AA | `a11y-task-work.spec.js` |
| TW-9 | Completing a task leaves focus on the heading of the view it lands on. | 2.4.3 A | `a11y-task-work.spec.js` |
| TW-10 | Claiming, assigning and unclaiming announce their outcome. | 4.1.3 AA | manual — see [Known gaps](#known-gaps) |

---

## GS — Finding something with the global search

A combobox is the control least forgiving of a partial ARIA implementation: a
screen reader reads the active option only if `aria-activedescendant` genuinely
tracks the arrow keys, and reads nothing at all if it points at an element that
does not exist.

| ID | Requirement | WCAG | Verified by |
| --- | --- | --- | --- |
| GS-1 | The search opens from the keyboard, by `Alt+K` and by activating its trigger, and opening it moves focus to the search field. | 2.1.1 A | `a11y-global-search.spec.js` |
| GS-2 | The field is a combobox that owns its listbox and reports whether it is expanded. | 4.1.2 A | `a11y-global-search.spec.js` |
| GS-3 | The arrow keys move the active option, `aria-activedescendant` follows them, and it always names an element that exists and is `aria-selected`. | 4.1.2 A | `a11y-global-search.spec.js` |
| GS-4 | `Enter` navigates to the active option. | 2.1.1 A | `a11y-global-search.spec.js` |
| GS-5 | `Escape` closes the search **however much has been typed**, and returns focus to the trigger. | 2.1.2 A, 2.4.3 A | `a11y-global-search.spec.js` |
| GS-6 | Result groups are labelled by their category heading. | 1.3.1 A | `a11y-global-search.spec.js` |

---

## Known gaps

Requirements above marked `manual`, plus the flows still driven by no spec, are
tracked in [`docs/accessibility/COVERAGE.md`](./accessibility/COVERAGE.md) — it
is generated from `e2e/routes.js` and `e2e/a11y-coverage.js`, so it cannot drift
from what the suite actually does. Regenerate it with `npm run a11y:coverage`.

The diagram surfaces (bpmn-js, dmn-js) remain a documented third-party
limitation; see the last section of [Accessibility.md](Accessibility.md).

## Adding a requirement

1. Write it here first, in the journey it belongs to, with a WCAG criterion and
   a next free number.
2. Write the test that fails without it.
3. Only then change the app.

A requirement with no test is a wish, and this document has room for neither.
