/**
 * Helpers for the shared list-page URL convention used by <ListFilter>:
 *
 *   ?filter=<savedFilterId|"all"|"my">
 *   ?sortBy=<key>&sortOrder=asc|desc
 *   ?q.<criterionKey>=<value>      (namespaced freeform criteria)
 *
 * The `q.` prefix keeps criteria from colliding with route params or reserved
 * keys. parse/write are inverses for round-tripping; criteria_to_params is the
 * shape an API resource wrapper consumes; build_share_link returns the current
 * URL verbatim so it can be handed to a teammate.
 */

const RESERVED_KEYS = new Set(["filter", "sortBy", "sortOrder"]);
const CRITERION_PREFIX = "q.";

const is_empty = (value) =>
  value === "" || value === null || value === undefined;

export const parse_list_query = (query = {}) => {
  const criteria = {};
  for (const [key, value] of Object.entries(query)) {
    if (key.startsWith(CRITERION_PREFIX) && !is_empty(value)) {
      criteria[key.slice(CRITERION_PREFIX.length)] = value;
    }
  }
  return {
    saved_filter_id: query.filter ?? null,
    sortBy: query.sortBy ?? null,
    sortOrder: query.sortOrder ?? null,
    criteria,
  };
};

export const write_list_query = (current_url, patch = {}) => {
  const url = new URL(current_url, "http://_");
  const params = url.searchParams;
  if ("saved_filter_id" in patch) {
    if (is_empty(patch.saved_filter_id) || patch.saved_filter_id === "all") {
      params.delete("filter");
    } else {
      params.set("filter", patch.saved_filter_id);
    }
  }
  if ("sortBy" in patch) {
    if (is_empty(patch.sortBy)) params.delete("sortBy");
    else params.set("sortBy", patch.sortBy);
  }
  if ("sortOrder" in patch) {
    if (is_empty(patch.sortOrder)) params.delete("sortOrder");
    else params.set("sortOrder", patch.sortOrder);
  }
  if ("criteria" in patch) {
    for (const key of Array.from(params.keys())) {
      if (key.startsWith(CRITERION_PREFIX)) params.delete(key);
    }
    for (const [key, value] of Object.entries(patch.criteria ?? {})) {
      if (!is_empty(value))
        params.set(`${CRITERION_PREFIX}${key}`, String(value));
    }
  }
  const search = params.toString();
  return url.pathname + (search ? `?${search}` : "");
};

export const criteria_to_params = (criteria = {}) => {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(criteria)) {
    if (!is_empty(value)) params.set(key, String(value));
  }
  return params;
};

export const build_share_link = (current_url = window.location.href) =>
  new URL(current_url).toString();

const serialize = (url) => {
  const search = url.searchParams.toString();
  return url.pathname + (search ? `?${search}` : "");
};

/** Toggle the list page into the "manage filters" view. */
export const with_manage = (current_url = window.location.href) => {
  const url = new URL(current_url, "http://_");
  url.searchParams.set("filters", "manage");
  return serialize(url);
};

/** Drop the manage flag (i.e. go back to the list). */
export const without_manage = (current_url = window.location.href) => {
  const url = new URL(current_url, "http://_");
  url.searchParams.delete("filters");
  return serialize(url);
};

/**
 * Build a shareable URL that applies a saved filter's resolved query + sort
 * to the current list page. The recipient lands on the same view without
 * needing the saved filter present locally (important for our
 * localStorage-backed non-Task filters).
 */
export const filter_share_link = (current_url, filter) => {
  const url = new URL(current_url, "http://_");
  url.searchParams.delete("filters");
  url.searchParams.delete("filter");
  for (const key of Array.from(url.searchParams.keys())) {
    if (key.startsWith(CRITERION_PREFIX)) url.searchParams.delete(key);
  }
  if (filter.sort?.sortBy) url.searchParams.set("sortBy", filter.sort.sortBy);
  if (filter.sort?.sortOrder)
    url.searchParams.set("sortOrder", filter.sort.sortOrder);
  for (const [k, v] of Object.entries(filter.query ?? {})) {
    if (!is_empty(v))
      url.searchParams.set(`${CRITERION_PREFIX}${k}`, String(v));
  }
  return url.host === "_" ? serialize(url) : url.toString();
};

export const RESERVED_LIST_QUERY_KEYS = RESERVED_KEYS;
export const LIST_QUERY_CRITERION_PREFIX = CRITERION_PREFIX;
