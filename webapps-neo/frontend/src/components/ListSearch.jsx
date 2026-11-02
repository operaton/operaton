import { useSignal } from "@preact/signals";
import { useRef } from "preact/hooks";
import * as Icons from "../assets/icons.jsx";

/**
 * A single-field search for a list page that no longer carries saved filters
 * (those are a Tasklist feature). Searches as you type (debounced); an empty
 * term clears the search. Wrapped in a <search> landmark.
 *
 * @param label    already-translated field label (also the placeholder)
 * @param value    the current term (e.g. read back from the URL) to prefill
 * @param on_search {(term: string) => void}
 */
export const ListSearch = ({ label, value = "", on_search }) => {
  const term = useSignal(value),
    timer = useRef(null);

  const run = (next) => {
    term.value = next;
    clearTimeout(timer.current);
    timer.current = setTimeout(() => on_search(next.trim()), 250);
  };

  return (
    <search class="list-search">
      <span class="search-field">
        <Icons.search />
        <input
          type="search"
          aria-label={label}
          placeholder={label}
          value={term.value}
          onInput={(e) => run(e.currentTarget.value)}
        />
      </span>
    </search>
  );
};
