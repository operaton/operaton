import { useEffect, useRef } from "preact/hooks";
import { useLocation } from "preact-iso";

// Focus management for route changes and in-page selection.
//
// A single-page app replaces the document without telling anyone: the browser
// moves neither focus nor the screen reader's reading position, so after
// following a link a keyboard user is still on the link they just left and a
// screen-reader user hears nothing at all. The fix is to move focus to the new
// view's heading, which announces it and puts the next Tab press in the right
// place.
//
// Two levels of "new view" exist here, and they are not the same event:
//
//   PageHeading    the whole page changed (/tasks -> /processes)
//   DetailHeading  the page stayed, the thing being shown changed
//                  (/tasks -> /tasks/:id, selecting a row from the list)
//
// Keying both off the full path would make selecting a task yank focus back up
// to the page's <h1>, which is why PageHeading compares only the first path
// segment and leaves everything below it to DetailHeading.

// Module scope on purpose. A page's <h1> unmounts along with its page, so a
// per-component ref cannot tell "first paint of the app" from "first paint of
// the page we just navigated to" — both look like a fresh mount. These two
// facts have to outlive any single heading.
let initial_navigation_done = false,
  last_page = null;

/**
 * The page a path belongs to: its first segment, ignoring deeper params.
 * Tolerates a missing path so a heading rendered outside a LocationProvider
 * (as unit tests do) degrades to "no page" rather than throwing.
 */
export const page_of = (path) => (path ?? "").split("/")[1] ?? "";

/**
 * Arms focus movement. The first PageHeading to mount calls this itself, so the
 * page a session opens on never steals focus while every page after it does.
 *
 * Doing it here rather than from the app root matters: the root mounts while
 * the login screen is showing, long before any page exists, so a root-level
 * flag would already be set by the time the first real page arrived — and the
 * first page load would grab focus, pushing the skip link out of first place.
 */
export const mark_initial_navigation_done = () => {
  initial_navigation_done = true;
};

/** Test seam — the module state above is deliberately not reachable otherwise. */
export const reset_focus_tracking = () => {
  initial_navigation_done = false;
  last_page = null;
};

/**
 * Move focus to the heading of the view we just landed on.
 *
 * For flows that finish with a route change WITHIN the same page — starting a
 * process or completing a task both end at /tasks, having begun at /tasks/start
 * or /tasks/:id — where `PageHeading` deliberately stays put because the first
 * path segment did not change. The control that was focused is unmounted by
 * that route change, so without this focus falls back to `<body>` and the next
 * Tab press starts again from the top of the document.
 *
 * A frame is given to the router to render the view before it is looked for.
 */
export const focus_page_heading = () => {
  requestAnimationFrame(() =>
    document.querySelector("main#content h1")?.focus(),
  );
};

/**
 * The page's `<h1>`. Takes focus when the page changes, never on first load.
 *
 * `tabIndex={-1}` makes it focusable programmatically without adding a stop to
 * the tab order. The outline it draws is governed by `:focus-visible`, so a
 * mouse user who clicked a nav link sees nothing while a keyboard user gets a
 * visible indicator — see the focus rules in `css/style.css`.
 */
export const PageHeading = ({ children, ...props }) => {
  const heading = useRef(null),
    location = useLocation(),
    page = page_of(location?.path);

  useEffect(() => {
    if (!initial_navigation_done) {
      // The page this session opened on. Record it and arm the mechanism.
      last_page = page;
      mark_initial_navigation_done();
      return;
    }
    if (last_page === page) return;
    last_page = page;
    heading.current?.focus();
  }, [page]);

  return (
    <h1 ref={heading} tabIndex={-1} {...props}>
      {children}
    </h1>
  );
};

/**
 * A detail pane's heading. Takes focus when `focus_key` changes — the id of
 * whatever the pane is showing — so selecting an entry from a list moves the
 * reading position into the detail rather than leaving it in the list.
 *
 * Deliberately has no dependency array: detail headings are filled from an
 * async fetch, and focusing an empty heading announces nothing. Running on
 * every render lets it wait for the render that actually has text, which is
 * cheap (two ref reads) and the same approach `TaskRowEntry` already takes for
 * scrolling the selected row into view.
 */
export const DetailHeading = ({ focus_key, level = 2, children, ...props }) => {
  const heading = useRef(null),
    focused = useRef(null),
    armed = useRef(null),
    Tag = `h${level}`;

  // Captured during the first render, before any effect has run. On the initial
  // page load that is `false` even for a deep link straight to a detail pane,
  // so arriving at /tasks/:id does not yank focus; a pane that appears later,
  // because someone picked a row, sees `true` and focuses right away.
  if (armed.current === null) armed.current = initial_navigation_done;

  useEffect(() => {
    if (!armed.current) {
      armed.current = true;
      focused.current = focus_key;
      return;
    }
    if (focused.current === focus_key) return;
    if (focus_key === undefined || focus_key === null) return;
    if (!heading.current?.textContent?.trim()) return;
    focused.current = focus_key;
    heading.current.focus();
  });

  return (
    <Tag ref={heading} tabIndex={-1} {...props}>
      {children}
    </Tag>
  );
};
