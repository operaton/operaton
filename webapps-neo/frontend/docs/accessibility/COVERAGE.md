# Accessibility Coverage

<!-- GENERATED FILE — do not edit. Regenerate with `npm run a11y:coverage`. -->

What the automated accessibility layers reach, per user path, and what they
leave for a human. Generated from the route manifest (`e2e/routes.js`) and the
state matrix (`e2e/a11y-states.js`), so it cannot drift away from what the
scanners actually do.

Read it alongside [Manual Accessibility Testing.md](../Manual%20Accessibility%20Testing.md),
which holds the walkthrough itself. Coverage here means *a scanner visited this
state*, never *this state is accessible* — the gate's own findings live in
[REPORT.md](./REPORT.md).

## How to read this

- **Gate scans** — visits by `npm run test:a11y`: each route once by axe and
  once by pa11y, in one theme and one viewport. The command fails on a
  violation, but no CI job runs it today, so it only bites locally.
- **Report scans** — visits by `npm run a11y:report`: every theme × viewport
  combination, plus the interaction states that apply to the route. This layer
  is informational and never fails a build.
- **Manual only** — checks no scanner performs, in addition to the shared list
  below that applies to every path.

## Summary

Across 11 user paths and 13 routes: **26** gate scans and **66** report scans.

| User path | Routes | Gate scans | Report scans | Specs |
| --- | --- | --- | --- | --- |
| [Signing in](#sign-in) | `login` | 2 | 6 | 2 |
| [Landing on the dashboard](#dashboard) | `dashboard` | 2 | 4 | 6 |
| [Working on tasks](#tasks) | `tasks`, `start-process` | 4 | 14 | 6 |
| [Inspecting processes](#processes) | `processes` | 2 | 6 | 3 |
| [Inspecting decisions](#decisions) | `decisions` | 2 | 5 | 2 |
| [Browsing deployments](#deployments) | `deployments` | 2 | 6 | 2 |
| [Monitoring batches](#batches) | `batches` | 2 | 5 | 2 |
| [Running a migration](#migrations) | `migrations` | 2 | 4 | 2 |
| [Administering users and authorizations](#administration) | `admin` | 2 | 4 | 3 |
| [Managing your own account](#account) | `account` | 2 | 4 | 2 |
| [Help and error pages](#help-and-errors) | `help`, `not-found` | 4 | 8 | 3 |

## Checks that apply everywhere

- Tab order follows the visual and logical order, and the focus ring is visible on every stop. Menus and lists are one stop each — arrow within them.
- Focus returns to the trigger when a dialog closes. (Route-change focus is handled by components/Heading.jsx and covered by focus.spec.js; dialog restore is not.)
- Names announced by a screen reader are meaningful, not merely present.
- The page is usable at 400% zoom without horizontal scrolling.
- No information is carried by colour alone.

## User paths

<a id="sign-in"></a>

### Signing in

The login screen, the only view rendered while unauthenticated. Backend selector, credential form, language selector.

| Route | Path | Gate | Report states | Interaction states |
| --- | --- | --- | --- | --- |
| `login` | `/` | axe + pa11y | 6 | SSO login (OAuth2 mode) |

Automated by `login.spec.js`, `a11y.spec.js`.

**Manual only:**

- The form is completable with the keyboard alone, and the submit button is reachable without a mouse.
- A failed login is announced and leaves focus somewhere recoverable. It currently does not — see the known findings.
- Changing the language does not strand focus or lose entered credentials.

<a id="dashboard"></a>

### Landing on the dashboard

The post-login landing page and the surrounding app chrome: header, primary navigation, skip link.

| Route | Path | Gate | Report states | Interaction states |
| --- | --- | --- | --- | --- |
| `dashboard` | `/` | axe + pa11y | 4 | — |

Automated by `dashboard.spec.js`, `navigation.spec.js`, `keyboard.spec.js`, `focus.spec.js`, `arrow-navigation.spec.js`, `a11y.spec.js`.

**Manual only:**

- The first Tab press reveals the skip link, and activating it moves focus into <main id="content">.
- The overview cards are reachable and their link text makes sense out of context.
- The Alt+Shift+0..7 navigation shortcuts move focus, not just the route.

<a id="tasks"></a>

### Working on tasks

The tasklist, task detail with its form/history/attachments/diagram tabs, the task dialogs, starting a process, and the global search dialog.

| Route | Path | Gate | Report states | Interaction states |
| --- | --- | --- | --- | --- |
| `tasks` | `/tasks` | axe + pa11y | 10 | Global search dialog open, Mobile navigation dialog open, Empty result set, Backend error (request state ERROR) |
| `start-process` | `/tasks/start` | axe + pa11y | 4 | — |

Deep routes, scanned only when the engine holds matching data: `/tasks/{id}`.

Automated by `tasks.spec.js`, `goto.spec.js`, `keyboard.spec.js`, `focus.spec.js`, `arrow-navigation.spec.js`, `a11y.spec.js`.

**Manual only:**

- Selecting a task moves focus to the detail heading (focus.spec.js asserts it) — confirm the announcement that follows is actually useful.
- The tab strip follows the APG pattern: arrow keys move, Tab leaves, and the selected panel is announced.
- Each task dialog traps focus, closes on Escape, and returns focus to the control that opened it.
- Claiming, assigning and completing a task announce their outcome — no automated test covers task completion at all.
- The global search combobox announces the active option as the arrow keys move through results.

<a id="processes"></a>

### Inspecting processes

Deployed definitions, definition detail with its instance/incident/job navigation, and instance detail with its variable and incident tabs, in both live and history mode.

| Route | Path | Gate | Report states | Interaction states |
| --- | --- | --- | --- | --- |
| `processes` | `/processes` | axe + pa11y | 6 | Empty result set, Backend error (request state ERROR) |

Deep routes, scanned only when the engine holds matching data: `/processes/{definitionId}/instances/{id}/vars`.

Automated by `processes.spec.js`, `processes-instance-detail.spec.js`, `a11y.spec.js`.

**Manual only:**

- The BPMN diagram is an SVG canvas with no text alternative — confirm the surrounding controls carry everything a non-sighted user needs.
- The live/history toggle announces which mode is active.
- Bulk selection announces how many rows are selected, and select-all has an accessible name.
- Deep instance routes are only scanned when the engine holds data; walk one by hand even when the scan skipped it.

<a id="decisions"></a>

### Inspecting decisions

The DMN definition list and decision detail: diagram, definition details, and evaluated instances with their inputs and outputs.

| Route | Path | Gate | Report states | Interaction states |
| --- | --- | --- | --- | --- |
| `decisions` | `/decisions` | axe + pa11y | 5 | Empty result set |

Deep routes, scanned only when the engine holds matching data: `/decisions/{id}`.

Automated by `decisions.spec.js`, `a11y.spec.js`.

**Manual only:**

- The DMN diagram and decision table are SVG-rendered; check the tabular data is available in an accessible form elsewhere.
- Input and output columns of an evaluated instance are associated with their headers when read cell by cell.

<a id="deployments"></a>

### Browsing deployments

The three-column deployment browser — deployments, their resources, and the resource preview — plus the upload dialog.

| Route | Path | Gate | Report states | Interaction states |
| --- | --- | --- | --- | --- |
| `deployments` | `/deployments` | axe + pa11y | 6 | Deployment upload dialog open, Empty result set |

Automated by `deployments.spec.js`, `a11y.spec.js`.

**Manual only:**

- Moving between the three columns is possible with the keyboard and the current column is discoverable.
- The upload dialog exposes the file input with a real label, and reports success or failure audibly.

<a id="batches"></a>

### Monitoring batches

The batch list with its running/history toggle and per-batch progress, and batch detail.

| Route | Path | Gate | Report states | Interaction states |
| --- | --- | --- | --- | --- |
| `batches` | `/batches` | axe + pa11y | 5 | Empty result set |

Automated by `batches.spec.js`, `a11y.spec.js`.

**Manual only:**

- Each <progress> element has an accessible name and its value is announced, not just drawn.
- The 'select a batch' empty prompt is announced when no batch is chosen.

<a id="migrations"></a>

### Running a migration

The three-step migration wizard: select definitions, map activities, configure and execute.

| Route | Path | Gate | Report states | Interaction states |
| --- | --- | --- | --- | --- |
| `migrations` | `/migrations` | axe + pa11y | 4 | — |

Automated by `migrations.spec.js`, `a11y.spec.js`.

**Manual only:**

- Moving between steps announces which step is now active and where focus went.
- The activity mapping controls have names that identify which activity they map — no automated test executes a migration.
- Validation errors are announced and focus moves to the first invalid control.

<a id="administration"></a>

### Administering users and authorizations

Admin sub-navigation over users, groups, tenants, authorizations and system settings, including the authorization resource matrix.

| Route | Path | Gate | Report states | Interaction states |
| --- | --- | --- | --- | --- |
| `admin` | `/admin` | axe + pa11y | 4 | — |

Automated by `admin.spec.js`, `arrow-navigation.spec.js`, `a11y.spec.js`.

**Manual only:**

- The authorization matrix is navigable cell by cell with its row and column headers announced.
- Create, edit and delete flows announce their outcome through the existing aria-live regions.
- Destructive actions are distinguishable without relying on colour.

<a id="account"></a>

### Managing your own account

Profile, password change, and the user's own group and tenant memberships.

| Route | Path | Gate | Report states | Interaction states |
| --- | --- | --- | --- | --- |
| `account` | `/account` | axe + pa11y | 4 | — |

Automated by `account.spec.js`, `a11y.spec.js`.

**Manual only:**

- The password change result is announced by its assertive live region without stealing focus.
- Password fields carry the right autocomplete tokens so a password manager can fill them.

<a id="help-and-errors"></a>

### Help and error pages

The static help page and the 404 fallback.

| Route | Path | Gate | Report states | Interaction states |
| --- | --- | --- | --- | --- |
| `help` | `/help` | axe + pa11y | 4 | — |
| `not-found` | `/does-not-exist` | axe + pa11y | 4 | — |

Automated by `help.spec.js`, `not-found.spec.js`, `a11y.spec.js`.

**Manual only:**

- The 404 page announces that the route was not found rather than presenting an empty shell.
- The ALT + K hint is discoverable by a screen reader user who cannot see it.

## Flows with no automated coverage at all

No spec drives any of these, so a scanner has never seen the states they produce. They are manual-only by default.

- Claiming, assigning or completing a task, and submitting a task form
- Starting a process instance through to completion
- Creating, editing or deleting users, groups, tenants and authorizations
- Changing a password or editing the profile
- Executing a migration
- Deploying a resource through the upload dialog
- Creating and editing saved filters
- Switching the interface language
