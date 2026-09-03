# Accessibility Tooling

The tools the manual accessibility pass assumes you have: a screen reader, a
way to see the accessibility tree, and a browser that lets the keyboard reach
everything. Set them up once here; the procedure itself is in
[Manual Accessibility Testing.md](Manual%20Accessibility%20Testing.md).

We keep this separate from the procedure on purpose. Setting up NVDA is a
one-off; walking a user path is not, and nobody should have to scroll past a
hundred lines of installation notes to reach step one.

## Screen readers

A screen reader is the only tool that answers the question the scanners cannot:
*does this page make sense when it is read aloud instead of looked at?* Use the
one that matches your platform — testing all three is a nice-to-have, testing
none is the gap.

Pair the screen reader with the browser it is most used with in the wild, since
each combination has its own quirks: **Orca with Firefox**, **NVDA with Firefox
or Chrome**, **VoiceOver with Safari**.

> **Important**: a screen reader takes over large parts of your keyboard. Learn
> how to stop it *before* you start it — the "stop" row in each table below.

### Orca (Linux)

Orca ships with GNOME, so on Fedora there is nothing to install.

| Action | How |
| --- | --- |
| Start / stop | `Super` + `Alt` + `S` |
| Start from a terminal | `orca` (or `orca --replace` if one is already running) |
| Toggle without a shortcut | `gsettings set org.gnome.desktop.a11y.applications screen-reader-enabled true` |
| Preferences | `orca --setup` |
| Orca modifier ("Orca") | `Insert` on a desktop layout, `Caps Lock` on a laptop layout |
| Stop speech | `Ctrl` |
| Read from here | `Orca` + `Semicolon` |
| Next / previous heading | `H` / `Shift` + `H` |
| Headings by level | `1`…`6` |
| Next landmark | `M` |
| Next link | `K` |
| Next form field | `Tab`, or `E` for entries |
| Next table, then cells | `T`, then `Alt` + arrow keys |
| List headings / links / landmarks | `Alt` + `Shift` + `H` / `A` / `M` |
| Where am I | `Orca` + `Enter` |

Two things to know before you draw conclusions from an Orca session:

- **Orca's shortcuts and ours collide.** The app binds `Alt` + `Shift` + `0`…`7`
  for navigation (`src/components/Header.jsx`), and Orca binds `Alt` + `Shift`
  with letters for its list dialogs. They do not overlap today, but if a
  shortcut appears not to work, check it is reaching the browser at all before
  filing it as a bug.
- Orca speaks through `speech-dispatcher`. If it starts but says nothing,
  that is the thing to check first, not the app.

### NVDA (Windows)

Free and open source, from [nvaccess.org](https://www.nvaccess.org/download/).
The portable copy needs no administrator rights, which makes it the easiest
screen reader to get onto a locked-down work machine.

| Action | How |
| --- | --- |
| Start | `Ctrl` + `Alt` + `N` |
| Stop | `NVDA` + `Q` |
| NVDA modifier ("NVDA") | `Insert` or `Caps Lock` (chosen at install) |
| Stop speech | `Ctrl` |
| Read from here | `NVDA` + `Down arrow` |
| Toggle browse / focus mode | `NVDA` + `Space` |
| Elements list (headings, links, landmarks, form fields) | `NVDA` + `F7` |
| Next / previous heading | `H` / `Shift` + `H` |
| Headings by level | `1`…`6` |
| Next landmark | `D` |
| Next link | `K` |
| Next form field | `F` |
| Next table, then cells | `T`, then `Ctrl` + `Alt` + arrow keys |
| Speech viewer (read output on screen) | NVDA menu → Tools → Speech Viewer |

**Browse mode versus focus mode is the concept to internalise.** In browse mode
single letters navigate the document; in focus mode keystrokes go to the
control. NVDA switches automatically when focus enters a text field or a
composite widget, and a great many "the keyboard is broken" reports are really
"NVDA was in the other mode". Our tab strips and the global-search combobox are
exactly the widgets where this matters. Turn on the Speech Viewer when you want
to quote what was announced into a bug report.

### VoiceOver (macOS)

Built in, nothing to install.

| Action | How |
| --- | --- |
| Start / stop | `Cmd` + `F5` |
| VoiceOver modifier ("VO") | `Ctrl` + `Option` |
| Stop speech | `Ctrl` |
| Read from here | `VO` + `A` |
| Next / previous item | `VO` + `Right arrow` / `Left arrow` |
| Rotor (headings, links, landmarks, form controls) | `VO` + `U` |
| Next heading | `VO` + `Cmd` + `H` |
| Next table | `VO` + `Cmd` + `T` |
| Interact with a group or table | `VO` + `Shift` + `Down arrow` |
| Stop interacting | `VO` + `Shift` + `Up arrow` |
| Preferences | VoiceOver Utility (`VO` + `F8`) |

**Interaction has no equivalent on Windows or Linux.** VoiceOver treats groups
and tables as containers you step *into*, so a table that reads fine in NVDA can
feel unreachable in VoiceOver until you interact with it. Judge the app on
whether interacting is *possible and sensible*, not on whether the model matches
what you are used to.

## Seeing ARIA in the browser

### Firefox's Accessibility Inspector

Built in, and the fastest way to answer "what is this element's accessible name
and role?" — no extension required.

1. Open the developer tools (`F12`) and pick the **Accessibility** panel.
2. Click **Turn on accessibility features** the first time.
3. Select any node to see its computed **name**, **role**, **description**,
   **keyboard shortcut** and states, which is exactly what a screen reader will
   announce.

Three things in that panel earn their keep:

- **Check for issues → All issues** — a quick pass for contrast, keyboard and
  text-label problems on the current state of the page. Unlike our scanners it
  sees whatever is on screen right now, including a dialog you just opened.
- **Show Tabbing Order** — overlays a numbered badge on every tab stop. This is
  the single best way to see tab order diverging from visual order without
  pressing Tab fifty times.
- **Simulate** — colour-vision deficiency and contrast loss, for the
  "is colour the only carrier of meaning?" check.

### Extensions worth adding

The harness already runs axe over every route in `e2e/routes.js`, so an
extension's value is on the states it *cannot* reach: a dialog you opened by
hand, a form mid-validation, a list filtered down to one row.

| Extension | Adds |
| --- | --- |
| [axe DevTools](https://addons.mozilla.org/firefox/addon/axe-devtools/) | The same engine the gate uses, on demand, against the current DOM |
| [WAVE Evaluation Tool](https://addons.mozilla.org/firefox/addon/wave-accessibility-tool/) | Icons drawn onto the page itself — good for spotting a missing label in context |
| [Landmark Navigation via Keyboard](https://addons.mozilla.org/firefox/addon/landmarks/) | Jump between landmarks without a screen reader running |

Two well-known tools are **Chrome/Edge only** and have no Firefox build:
Accessibility Insights for Web and ARC Toolkit. If you rely on either, do that
pass in Chrome and keep the screen-reader pass in Firefox.

## Keyboard navigation

Everything must be reachable and operable with the keyboard alone. That is the
whole test: unplug the mouse, and try.

| Key | Expected |
| --- | --- |
| `Tab` / `Shift` + `Tab` | Move forward and back through focusable elements, with a visible focus ring at every stop |
| `Enter` | Follow a link, submit a form, activate a button |
| `Space` | Activate a button, toggle a checkbox |
| `Escape` | Close the open dialog and return focus to whatever opened it |
| Arrow keys | Move *within* a group: menus, selection lists, tab strips, the global-search listbox |
| `Home` / `End` | First and last entry in a group |

**Menus and selection lists are one tab stop each.** `Tab` enters a nav or a
list once and the next `Tab` leaves it entirely; the arrow keys move between
entries, and `Home`/`End` jump to the ends. Entries are still plain links — a
screen reader announces "link" and the target URL, because
`src/helper/roving_focus.js` changes only the tabindex and the key handling, not
the ARIA roles. In a table the arrow keys move by *row*, so a row that pairs a
checkbox with a link is one stop, not two.

The app adds its own shortcuts, defined in `src/components/Header.jsx` and
`src/components/GoTo.jsx`:

| Shortcut | Does |
| --- | --- |
| `Tab` from a fresh page load | Reveals the skip link; `Enter` jumps to `<main id="content">` |
| `Alt` + `Shift` + `0`…`7` | Jump to dashboard, tasks, processes, decisions, deployments, batches, migrations, admin |
| `Alt` + `K` | Open the global search dialog |

### Making Firefox reach everything

By default Firefox on Linux and Windows tabs to links and form controls
already. Two settings are still worth knowing:

- **`accessibility.tabfocus`** — in `about:config`, set it to `7` to force `Tab`
  to reach every focusable element including links. Useful when you want to be
  certain the browser is not the thing hiding a stop.
- **Caret browsing (`F7`)** — puts a text cursor in the page so you can walk the
  content with the arrow keys. This is how you check *reading* order, which is
  not always the same as tab order.

**On macOS, Firefox defers to the system.** With System Settings → Keyboard →
**Keyboard navigation** switched off, `Tab` skips links entirely, no matter what
`accessibility.tabfocus` says. Turn it on before testing, or you will report
keyboard bugs that only exist in your settings.

That same macOS behaviour is why the `firefox` Playwright project is a *watch*
target and not part of the gate — see
[Manual Accessibility Testing.md](Manual%20Accessibility%20Testing.md).

### Zoom and reflow

WCAG 1.4.10 asks for no horizontal scrolling at a 320 px-wide viewport. In
practice: `Ctrl` + `+` (`Cmd` + `+` on macOS) to 400% in a 1280 px-wide window,
which is equivalent. Our header swaps its `<menu>` for a `<dialog>` below
`70em`, so this is also how you reach the mobile navigation on a desktop.
