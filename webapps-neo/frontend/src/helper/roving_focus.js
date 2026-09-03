import { useLayoutEffect, useRef } from "preact/hooks";

/** Prefix every branch of a selector list, not just the first. */
const scoped = (prefix, selector) =>
  selector
    .split(",")
    .map((part) => `${prefix} ${part.trim()}`)
    .join(", ");

// Roving tabindex: make a menu or a list one stop in the tab order, and move
// between its entries with the arrow keys.
//
// Why: a nav with nine links, or a task list with forty rows, otherwise costs a
// keyboard user nine or forty Tab presses to get past. The APG answer is to
// give the group a single tab stop and hand the arrow keys the job of moving
// within it.
//
// What this deliberately does NOT do is add `role="menubar"`/`menuitem` or
// `role="listbox"`/`option`. Those roles replace link semantics — a screen
// reader stops saying "link", the target URL disappears, and "open in new tab"
// stops making sense. APG itself reserves menu roles for application menus
// rather than site navigation. Entries here stay plain `<a href>`; only the
// tabindex and the key handling change.
//
// Named camelCase, unlike the rest of the codebase, because the react-hooks
// lint rule only recognises hooks by that spelling — same as the existing
// helper/keyPressHook.jsx.
//
// The existing components/Tabs.jsx implements the same idea by hand for the
// APG tab pattern, where the roles ARE correct. This hook is for everything
// else.

const FOCUSABLE = "a[href], button:not([disabled]), input:not([disabled])";

const HORIZONTAL = { next: "ArrowRight", previous: "ArrowLeft" },
  VERTICAL = { next: "ArrowDown", previous: "ArrowUp" };

// Input types that do NOT consume the arrow keys, so navigation may pass
// through them. Everything else — text entry, radio groups, selects — keeps
// its own behaviour.
const PASSIVE_INPUTS = new Set([
  "checkbox",
  "button",
  "submit",
  "reset",
  "file",
]);

const uses_arrow_keys = (element) => {
  const tag = element.tagName;
  if (tag === "SELECT" || tag === "TEXTAREA") return true;
  if (tag !== "INPUT") return false;
  return !PASSIVE_INPUTS.has(element.type);
};

/** Entries of a plain list container, in DOM order. */
const list_items = (container, selector) => [
  ...container.querySelectorAll(selector),
];

/**
 * Rows of a table reduced to one entry each: the row's first focusable element.
 * A row can hold more than one control — the process list pairs a checkbox with
 * a link — and arrowing between rows should land on the row, not step through
 * everything inside it.
 */
const row_items = (container) =>
  [...container.querySelectorAll("tbody tr")]
    .map((row) => row.querySelector(FOCUSABLE))
    .filter(Boolean);

/**
 * Everything the roving tabindex has to take out of the tab order.
 *
 * `FOCUSABLE` is a selector LIST, so it has to be prefixed branch by branch —
 * interpolating it whole would scope only `a[href]` to the row and leave
 * `button`/`input` matching anywhere in the container, including the header.
 */
const controlled = (container, mode, selector) =>
  mode === "rows"
    ? [...container.querySelectorAll(scoped("tbody tr", FOCUSABLE))]
    : list_items(container, selector);

/**
 * The entry that keeps its place in the tab order. Tabbing into a nav should
 * land on the page you are on, and into a list on the row you have open —
 * not always on the first entry.
 */
const active_index = (items, container) => {
  const marked = items.findIndex(
    (item) =>
      item.getAttribute("aria-current") === "page" ||
      item.closest('[aria-selected="true"]'),
  );
  if (marked !== -1) return marked;
  // Keep whatever already has focus, so a re-render mid-navigation does not
  // drag the tab stop back to the top.
  const focused = items.indexOf(container.ownerDocument.activeElement);
  return focused !== -1 ? focused : 0;
};

/**
 * @param {object} [options]
 * @param {"list"|"rows"} [options.mode] `rows` treats a table body as the list.
 * @param {"horizontal"|"vertical"|"both"} [options.orientation]
 * @param {string} [options.selector] which descendants are entries, list mode.
 * @param {boolean} [options.wrap] whether the ends join up.
 * @returns {{ref: object, onKeyDown: Function}} spread onto the container.
 */
export const useRovingFocus = ({
  mode = "list",
  orientation = "both",
  selector = "a[href]",
  wrap = true,
} = {}) => {
  const container = useRef(null);

  const apply = () => {
    const node = container.current;
    if (!node) return;

    const items =
      mode === "rows" ? row_items(node) : list_items(node, selector);
    if (!items.length) return;

    const active = items[active_index(items, node)],
      active_row = mode === "rows" ? active?.closest("tr") : null;

    for (const element of controlled(node, mode, selector)) {
      const keeps_tab_stop =
        element === active || (active_row && active_row.contains(element));
      // Removing the attribute rather than setting 0 leaves links and buttons
      // at their natural place in the order.
      if (keeps_tab_stop) element.removeAttribute("tabindex");
      else element.setAttribute("tabindex", "-1");
    }
  };

  // No dependency array on purpose: entries change with every filter or route
  // change, so the tab stop has to be recomputed after each render rather than
  // on a dependency we would have to remember to declare.
  useLayoutEffect(apply);

  // A render of THIS component is not the only way entries arrive. Where the
  // list is filled by a `<RequestState>` child subscribed to its own signal,
  // the rows appear without the container re-rendering, so the effect above
  // never runs again and every row keeps its natural tab stop — the whole list
  // becomes N tab stops instead of one. Watching the subtree covers that case
  // as well, and cannot loop: only `childList` is observed, while `apply` only
  // writes attributes.
  useLayoutEffect(() => {
    const node = container.current;
    if (!node || typeof MutationObserver === "undefined") return;

    const observer = new MutationObserver(apply);
    observer.observe(node, { childList: true, subtree: true });
    return () => observer.disconnect();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const on_keydown = (event) => {
    const node = container.current;
    if (!node) return;

    // Controls that use the arrow keys themselves keep them. A checkbox does
    // not, so arrowing out of the process list's select column still moves to
    // the next row; a select, a text field or a radio group does.
    if (uses_arrow_keys(event.target)) return;

    const items =
      mode === "rows" ? row_items(node) : list_items(node, selector);
    if (!items.length) return;

    const axes =
        orientation === "horizontal"
          ? [HORIZONTAL]
          : orientation === "vertical"
            ? [VERTICAL]
            : [HORIZONTAL, VERTICAL],
      forward = axes.some((axis) => axis.next === event.key),
      backward = axes.some((axis) => axis.previous === event.key);

    let target = null;
    if (forward || backward) {
      const from =
        mode === "rows"
          ? items.findIndex((item) =>
              item.closest("tr")?.contains(event.target),
            )
          : items.indexOf(event.target);
      if (from === -1) return;
      const step = forward ? 1 : -1,
        next = from + step;
      target = wrap
        ? items[(next + items.length) % items.length]
        : items[Math.min(Math.max(next, 0), items.length - 1)];
    } else if (event.key === "Home") {
      target = items[0];
    } else if (event.key === "End") {
      target = items[items.length - 1];
    }

    if (!target || target === event.target) {
      // Home/End on an already-first entry still counts as handled, otherwise
      // the browser scrolls the page instead.
      if (event.key === "Home" || event.key === "End") event.preventDefault();
      return;
    }

    // Arrow keys would otherwise scroll the list out from under the focus.
    event.preventDefault();
    target.focus();
  };

  return { ref: container, onKeyDown: on_keydown };
};
