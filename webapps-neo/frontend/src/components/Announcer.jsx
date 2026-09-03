import { signal } from "@preact/signals";

// The app's single polite live region — how an outcome reaches a screen-reader
// user when nothing visible stays on screen to carry it.
//
// Why this exists: completing a task or starting a process ends with a route
// change to /tasks. A sighted user sees the list they landed on and infers it
// worked. A screen-reader user is told nothing at all — the old view is simply
// gone, and WCAG 4.1.3 (Status Messages) is not met. The pages that already do
// this well (Admin, Account, Migrations) each own an `aria-live` div, which
// only works while the element announcing the outcome survives the outcome.
// These flows unmount, so the region has to outlive them.
//
// Kept out of the state signal tree on purpose: this is a UI concern with no
// API resource behind it, and `announce()` has to be callable from a promise
// chain that holds no component context.

// Two slots, alternating. A live region only announces when its text CHANGES,
// so completing two tasks in a row would announce once if a single slot held
// the same sentence twice. Writing into the other slot (and clearing the one
// before it) guarantees a real text change every time.
const slots = signal(["", ""]);

let next_slot = 0;

/**
 * Announce a message to assistive technology. Politely: it waits for the
 * current utterance to finish rather than interrupting.
 *
 * @param {string} message text to announce; ignored when empty
 */
export const announce = (message) => {
  if (!message) return;
  const value = ["", ""];
  value[next_slot] = message;
  next_slot = next_slot === 0 ? 1 : 0;
  slots.value = value;
};

/** Test seam — resets the alternation so assertions can be written in order. */
export const reset_announcements = () => {
  slots.value = ["", ""];
  next_slot = 0;
};

/**
 * Rendered once, in the app shell, so it survives every route change. Visually
 * hidden; `aria-atomic` makes the whole sentence read rather than the diff.
 */
export const Announcer = () => (
  <div class="announcer">
    {slots.value.map((text, index) => (
      <div
        key={index}
        class="screen-hidden"
        role="status"
        aria-live="polite"
        aria-atomic="true"
      >
        {text}
      </div>
    ))}
  </div>
);
