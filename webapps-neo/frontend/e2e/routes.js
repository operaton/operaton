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
export const discover_deep_routes = async ({ backend = BACKEND, credentials }) => {
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

  const inst = await first(
    "/history/process-instance?sortBy=startTime&sortOrder=desc&maxResults=1",
  );
  if (inst)
    routes.push({
      path: `/processes/${inst.processDefinitionId}/instances/${inst.id}/vars`,
      name: "process-instance-detail",
      auth: true,
    });

  const task = await first("/task?maxResults=1");
  if (task)
    routes.push({ path: `/tasks/${task.id}`, name: "task-detail", auth: true });

  const decision = await first(
    "/decision-definition?latestVersion=true&maxResults=1",
  );
  if (decision)
    routes.push({
      path: `/decisions/${decision.id}`,
      name: "decision-detail",
      auth: true,
    });

  return routes;
};
