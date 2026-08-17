// Shared route manifest for the accessibility scans — consumed by both the axe
// Playwright spec (a11y.spec.js) and the pa11y/HTMLCS runner (a11y-pa11y.mjs),
// so "scan all pages" stays a single source of truth.

// Top-level authenticated pages. `auth: true` → the basic-auth credential must
// be seeded before navigating (see fixtures.js / the pa11y runner).
export const STATIC_ROUTES = [
  { path: "/", name: "dashboard", auth: true },
  { path: "/tasks", name: "tasks", auth: true },
  { path: "/tasks/start", name: "start-process", auth: true },
  { path: "/processes", name: "processes", auth: true },
  { path: "/decisions", name: "decisions", auth: true },
  { path: "/deployments", name: "deployments", auth: true },
  { path: "/batches", name: "batches", auth: true },
  { path: "/migrations", name: "migrations", auth: true },
  { path: "/account", name: "account", auth: true },
  { path: "/admin", name: "admin", auth: true },
  { path: "/help", name: "help", auth: true },
  { path: "/does-not-exist", name: "not-found", auth: true },
];

// The login screen renders only when NOT authenticated (src/index.jsx), so it
// is scanned without seeding the credential.
export const LOGIN_ROUTE = { path: "/", name: "login", auth: false };

// Default dev backend (Operaton engine REST); override with E2E_BACKEND.
export const BACKEND =
  process.env.E2E_BACKEND ?? "http://localhost:8084/engine-rest";

const auth_header = ({ username, password }) =>
  `Basic ${Buffer.from(`${username}:${password}`).toString("base64")}`;

// Ask the engine for real ids so deep-route scans land on populated pages.
// Missing data degrades gracefully (that route is simply skipped) rather than
// failing the whole run — mirrors the discovery in processes-instance-detail.spec.js.
//
// `include_unavailable` (used by the report generator) keeps a placeholder entry
// for routes the engine had no data for, so the report's set of sections is
// fixed by this manifest rather than by whatever the engine happened to hold —
// a missing page then reads as "not scanned", instead of silently vanishing.
export const discover_deep_routes = async ({
  backend = BACKEND,
  credentials,
  include_unavailable = false,
} = {}) => {
  const headers = { Authorization: auth_header(credentials) };
  const first = async (path) => {
    try {
      const res = await fetch(`${backend}${path}`, { headers });
      if (!res.ok) return null;
      const list = await res.json();
      return Array.isArray(list) && list.length ? list[0] : null;
    } catch {
      return null;
    }
  };

  const routes = [];
  const add = (name, entity, to_path, reason) =>
    entity
      ? routes.push({ path: to_path(entity), name, auth: true, available: true })
      : include_unavailable &&
        routes.push({ path: null, name, auth: true, available: false, reason });

  add(
    "process-instance-detail",
    await first(
      "/history/process-instance?sortBy=startTime&sortOrder=desc&maxResults=1",
    ),
    (i) => `/processes/${i.processDefinitionId}/instances/${i.id}/vars`,
    "engine holds no process instances",
  );

  add(
    "task-detail",
    await first("/task?maxResults=1"),
    (t) => `/tasks/${t.id}`,
    "engine holds no user tasks",
  );

  add(
    "decision-detail",
    await first("/decision-definition?latestVersion=true&maxResults=1"),
    (d) => `/decisions/${d.id}`,
    "engine holds no decision definitions",
  );

  return routes;
};
