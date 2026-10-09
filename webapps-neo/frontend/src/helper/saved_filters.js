/**
 * localStorage-backed CRUD for saved list filters. The Camunda 7 `/filter`
 * REST resource only accepts resourceType=Task, so non-Task lists (Process
 * Definition, Decision, Deployment, Batch) persist client-side under
 *   operaton.saved_filters.<resource_type>
 * Saved filters share the same shape the server uses for Task filters so the
 * <ListFilter> UI can consume either source.
 *
 * SavedFilter shape:
 *   { id, name, query: { ... }, sort?: { sortBy, sortOrder }, properties?: { ... } }
 */

import { RESPONSE_STATE } from "../api/helper.jsx";

const STORAGE_PREFIX = "operaton.saved_filters.";
const storage_key = (resource_type) => `${STORAGE_PREFIX}${resource_type}`;

const read = (resource_type) => {
  try {
    const raw = localStorage.getItem(storage_key(resource_type));
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
};

const write = (resource_type, filters) => {
  localStorage.setItem(storage_key(resource_type), JSON.stringify(filters));
  return filters;
};

export const list_saved_filters = (resource_type) => read(resource_type);

export const create_saved_filter = (resource_type, filter) => {
  const id =
    typeof crypto !== "undefined" && crypto.randomUUID
      ? crypto.randomUUID()
      : `f_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
  return write(resource_type, [...read(resource_type), { ...filter, id }]);
};

export const update_saved_filter = (resource_type, id, patch) =>
  write(
    resource_type,
    read(resource_type).map((f) => (f.id === id ? { ...f, ...patch, id } : f)),
  );

export const delete_saved_filter = (resource_type, id) =>
  write(
    resource_type,
    read(resource_type).filter((f) => f.id !== id),
  );

/**
 * Mirror the wire shape the API helpers write into signals so <ListFilter>
 * can treat localStorage and server-backed filter sources identically.
 */
export const hydrate_signal = (resource_type, signal) => {
  signal.value = {
    status: RESPONSE_STATE.SUCCESS,
    data: list_saved_filters(resource_type),
  };
};
