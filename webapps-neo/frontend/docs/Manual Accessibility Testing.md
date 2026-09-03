# Manual Accessibility Testing

Automated scanning finds a minority of accessibility problems — the figures are
in [Accessibility.md](Accessibility.md). This document is the other part: how to
start the harness, how to watch the automated tests run, and how to walk each
user path yourself with a keyboard and a screen reader.

The tools it assumes — a screen reader, the Firefox accessibility inspector,
full keyboard navigation — are set up in
[Accessibility Tooling.md](Accessibility%20Tooling.md). Do that first.

## Starting the stack

Three things have to be running: an engine, some data in it, and the dev server.

```sh
# 1. the engine, with REST auth off — the same backend the report scans
docker compose -f docker-compose.a11y.yaml up -d    # podman compose -f docker-compose.a11y.yaml up -d

# 2. realistic data, once. An empty engine makes most pages an empty state.
cd dev-fixtures/bot && node deploy.js \
  && node spawn.js --process orderFulfillment --count 3 \
  && node spawn.js --process insuranceClaim   --count 2
cd ../..

# 3. the app
npm run dev
```

Then open **<http://127.0.0.1:5173>** and log in with `demo` / `demo`.

> **Important**: use `127.0.0.1`, not `localhost`. The dev server binds to
> `127.0.0.1` and the engine's CORS allowlist names that origin; `localhost`
> fails with an opaque CORS error.

### Which compose file

| File | REST auth | Use it for |
| --- | --- | --- |
| `docker-compose.a11y.yaml` | off | Everything here. Matches what `npm run a11y:report` scans, so your findings and the report's line up. |
| `docker-compose.yaml` | on | The dev default. Exercises the engine's authorization checks. |

Two consequences worth knowing before you test the login screen:

- With auth **off**, the engine accepts any password, so you cannot test a
  *rejected* login at all.
- With auth **on**, the login form does not currently work in a clean browser
  profile: `verify_credentials` (`src/api/resources/auth.js`) deliberately
  posts the credentials in the request body without an `Authorization` header,
  and the engine's authentication filter rejects it with 401 before the endpoint
  is reached.

So the failed-login checks below cannot be run against either file as things
stand. Record that as the finding it is rather than working around it.

## Watching the automated tests run

The specs worth watching are `e2e/a11y.spec.js` and `e2e/keyboard.spec.js` — the
keyboard one especially, since an axe scan looks like nothing happening. All of
these need the backend up; they start the dev server themselves.

| Command | What you get |
| --- | --- |
| `npm run test:a11y:ui` | Playwright UI mode: pick specs and browsers, watch on change, step back through every action with a DOM snapshot. Start here. |
| `npm run test:a11y:headed` | A real browser window, one test at a time, full speed. |
| `npm run test:a11y:watch` | The same, slowed to 500 ms per action so it can be followed by eye. |
| `npm run test:a11y:debug` | The Playwright Inspector: pause, step, and try selectors against the live page. |
| `npm run test:a11y:firefox` | The same specs in Firefox — the browser you do the manual pass in. |

Reach for `--debug` over `--ui` when you want to interrogate a *selector*; UI
mode is better for everything else, because its time-travel view shows what the
page looked like at each step after the fact.

Firefox needs a one-time download:

```sh
npx playwright install firefox
```

Firefox is a second pair of eyes, **not** a second gate. Its accessibility tree
differs from Chromium's in places, so a Firefox-only axe result is information
rather than a regression — and on macOS the system's "Keyboard navigation"
setting changes what `Tab` reaches, which would make `keyboard.spec.js` fail for
reasons that belong to the tester's machine. `npm run test:a11y`, `npm run
test:e2e` and CI all pin Chromium.

> `npm run test:a11y:watch` sets `PW_SLOW_MO` with POSIX inline-environment
> syntax, which `cmd.exe` does not understand. On Windows use Git Bash or
> PowerShell (`$env:PW_SLOW_MO=500; npx playwright test a11y keyboard --headed`).

## What the scanners already cover

[`accessibility/COVERAGE.md`](./accessibility/COVERAGE.md) maps every user path
below to the routes, states and specs that exercise it. It is generated from
`e2e/routes.js` and `e2e/a11y-states.js` by `npm run a11y:coverage`, so it
cannot quietly disagree with what the scanners do — add a route to the manifest
and `e2e/a11y-coverage.test.js` fails until the route is placed in a path.

Read it as *a scanner visited this state*, never as *this state is accessible*.

## Running a walkthrough

Every path gets the same four passes. Do them in this order — each one finds
things the next would otherwise drown out.

1. **Keyboard only.** Hands off the mouse. Tab through the whole path. Every
   stop must be visible, reachable and operable, and the order must match what
   you see. Use Firefox's *Show Tabbing Order* overlay to check the order
   without counting keystrokes. Remember that menus and lists are a *single*
   stop each — use the arrow keys inside them, and `Home`/`End` for the ends.
2. **Screen reader.** Same path, eyes closed where you can manage it. You are
   judging whether what is announced is *enough to act on* — not whether a name
   exists, but whether it identifies the thing.
3. **Zoom to 400%** in a 1280 px-wide window. Nothing may require horizontal
   scrolling, and nothing may be cut off. This is also how you reach the mobile
   navigation on a desktop.
4. **Colour.** Simulate a colour-vision deficiency in Firefox. Anything that
   was only distinguishable by hue is a finding.

The checks that apply to every path — tab order, focus after navigation,
meaningful names, reflow, colour — are listed once in `COVERAGE.md` rather than
repeated below. What follows is what is *specific* to each path.

## The walkthroughs

### Signing in

Open the app logged out. Tab from the top: language selector, backend selector
(only when more than one backend is configured), username, password, submit.
Submit with `Enter` from inside the password field.

- **Already automated**: `e2e/login.spec.js` (form renders, sign-in succeeds,
  server selector present), `e2e/a11y.spec.js` scans the logged-out screen.
- **Only you can decide**: whether a failed login is *announced* — it currently
  is not, see the known findings; whether the backend selector reads as a
  meaningful choice or as noise to someone who has never seen it.

### Dashboard and primary navigation

Load the app fresh and press `Tab` once. The skip link must appear; `Enter` must
put focus inside `<main id="content">`. Then walk the nine primary nav links and
the overview cards.

- **Already automated**: `e2e/dashboard.spec.js`, `e2e/navigation.spec.js`,
  and the skip-link and `aria-current="page"` tests in `e2e/keyboard.spec.js`.
- **Only you can decide**: whether the nav links are distinguishable when read
  out of context; whether `Alt` + `Shift` + `0`…`7` moves *focus* and not just
  the route; whether the skip link actually saves keystrokes.

### Working on tasks

The densest path. Select a task from the list, move through the
form / history / attachments / diagram tabs with the arrow keys, open one of the
task dialogs (due date, follow-up, groups, comment, assignee) and close it with
`Escape`.

- **Already automated**: `e2e/tasks.spec.js`, the roving-tab and dialog tests in
  `e2e/keyboard.spec.js`, `e2e/goto.spec.js` for global search.
- **Only you can decide**: whether the task name announced when focus lands on
  the detail heading is enough to orient you; whether each dialog returns focus
  to the control that opened it — none of them do. **Claiming, assigning and
  completing a task have no automated coverage at all** — walk them.

### Global search

`Alt` + `K` from anywhere. Type, arrow through the results, `Enter` to
navigate, `Escape` to close.

- **Already automated**: `e2e/goto.spec.js`, the dialog test in
  `e2e/keyboard.spec.js`, and the `global-search` report state.
- **Only you can decide**: whether the number of results is announced, and
  whether the active option is announced as the arrow keys move — the combobox
  in `src/components/GoTo.jsx` sets `aria-activedescendant`, which is exactly
  the pattern that fails silently.

### Inspecting processes

Definitions list → a definition → its instances → one instance and its tabs.
Then flip the live/history toggle and repeat.

- **Already automated**: `e2e/processes.spec.js`,
  `e2e/processes-instance-detail.spec.js`.
- **Only you can decide**: everything about the BPMN diagram — it is an SVG
  canvas with no text alternative, so the question is whether the surrounding
  controls carry what a non-sighted user needs; whether the live/history toggle
  announces which mode is active; whether bulk selection announces a count.

### Inspecting decisions

Definitions list → a decision → its evaluated instances with inputs and outputs.

- **Already automated**: `e2e/decisions.spec.js`.
- **Only you can decide**: whether the decision table's data is available in an
  accessible form anywhere, given dmn-js renders it as SVG; whether input and
  output columns are associated with their headers when read cell by cell.

### Browsing deployments

Deployments → resources → resource preview, then open the upload dialog.

- **Already automated**: `e2e/deployments.spec.js`, and the `upload-dialog`
  report state.
- **Only you can decide**: whether you can tell which of the three columns you
  are in; whether the upload dialog announces its purpose on open, exposes the
  file input with a real label, and reports the outcome. **Deploying a resource
  is not automated.**

### Monitoring batches

The batch list with its running/history toggle, then a batch detail.

- **Already automated**: `e2e/batches.spec.js`.
- **Only you can decide**: whether each `<progress>` has an accessible name and
  announces its value rather than only drawing it; whether the "select a batch"
  prompt is announced when nothing is selected.

### Running a migration

The three-step wizard: select definitions, map activities, configure and
execute.

- **Already automated**: `e2e/migrations.spec.js` — it asserts the three step
  headings render, and nothing more.
- **Only you can decide**: whether moving between steps announces the new step
  and moves focus; whether the mapping controls identify *which* activity they
  map; whether validation errors are announced and move focus to the first
  invalid control. **No test executes a migration.**

### Administration and account

Admin sub-navigation (users, groups, tenants, authorizations, system), then your
own profile, password and memberships.

- **Already automated**: `e2e/admin.spec.js`, `e2e/account.spec.js` — both only
  assert that navigation and redirects work.
- **Only you can decide**: whether the authorization matrix is navigable cell by
  cell with its headers announced; whether the `aria-live` regions in Admin,
  Account and Migrations announce once rather than three times; whether
  destructive actions are distinguishable without colour. **No CRUD flow and no
  password change is automated.**

### Help and error pages

`/help`, then any unknown URL.

- **Already automated**: `e2e/help.spec.js`, `e2e/not-found.spec.js`.
- **Only you can decide**: whether the 404 announces that the route was not
  found rather than presenting an empty shell, and whether the `ALT + K` hint is
  discoverable by someone who cannot see it.

## Keyboard navigation

Menus and selection lists take one tab stop and are arrowed through internally
(`src/helper/roving_focus.js`). Tabbing into a nav lands on the page you are
on; tabbing into a list lands on the row you have open. Tables move by row, so
a row holding a checkbox and a link is one stop rather than two.

`e2e/arrow-navigation.spec.js` asserts the mechanics. What it cannot tell you:
whether the grouping *makes sense* — whether arrowing through this particular
list feels like one thing, and whether a screen reader announces enough position
information ("row 3 of 40") to make it navigable without sight.

## Focus management

Changing page moves focus to the new page's `<h1>`, and selecting an entry from
a list moves it to the detail pane's heading — see `src/components/Heading.jsx`
for why those are two separate events. Focus is deliberately left alone on the
initial page load so the skip link stays the first Tab stop.

Outlines are drawn by `:focus-visible` rather than `:focus`, which is what makes
this bearable: a heading focused after a mouse click shows no ring, while the
same heading focused after keyboard navigation does. A visually hidden `<h1>`
reveals itself while focused, so focus never appears to vanish.

`e2e/focus.spec.js` asserts all of the above. What it cannot judge, and you can:
whether the announcement that follows is *useful* — whether the heading names
the thing you just selected well enough to orient someone who cannot see it.

## Two checks that belong to no single path

- **Reduced motion.** There is no `prefers-reduced-motion` handling anywhere in
  `src/css/`, while most pages carry `fade-in` animations. No automated layer
  will ever catch this: `e2e/a11y-scan.js` freezes those very animations before
  scanning, precisely so axe does not sample a mid-fade frame. Set the OS
  preference and walk any path.
- **Language switching.** Five locales ship, and switching them is not covered
  by any spec. Check that `<html lang>` follows the choice, and that a screen
  reader switches voice with it.

## Known findings to expect

Recorded so they are not rediscovered as new every time:

- **A failed login blanks the app.** `src/api/resources/auth.js` sets
  `logged_in.data` to `"wrong_login"`, which no branch in `src/index.jsx`
  handles, so the component tree unmounts: no error message, no focus target,
  nothing announced.
- **Diagrams are unreachable.** bpmn-js, dmn-js and form-js render SVG canvases
  with no text alternative — see the third-party section of
  [Accessibility.md](Accessibility.md).
- **`<html lang>` is static.** It stays `en` regardless of the chosen locale.
- **Dialogs do not restore focus** to the control that opened them; the native
  `<dialog>` traps focus while open but drops it on close.
- The scanner findings themselves are in
  [`accessibility/REPORT.md`](./accessibility/REPORT.md); check there before
  filing a duplicate.
