/**
 * Which application each built-in page belongs to.
 *
 * The engine authorizes on the three applications the previous web apps knew,
 * so Neo's finer-grained pages map onto those. A page that is not listed is
 * always available — `/`, `/account` and `/help` belong to no application.
 */
export const PAGE_APPS = {
  "/tasks": "tasklist",
  "/processes": "cockpit",
  "/decisions": "cockpit",
  "/deployments": "cockpit",
  "/batches": "cockpit",
  "/migrations": "cockpit",
  "/admin": "admin",
};

/**
 * Whether the signed-in user may use `app`, given the `authorizedApps` the
 * server reported at sign-in.
 *
 * Anything but a list means the server never told us — a backend configured
 * elsewhere has no session to ask. Nothing is hidden then: the engine still
 * filters every query, and hiding pages on a guess is worse than showing them.
 * With engine authorization switched off the server reports every application,
 * so this needs no separate case.
 */
export const app_allowed = (authorized_apps, app) =>
  !app || !Array.isArray(authorized_apps) || authorized_apps.includes(app);

/** Whether the page at `href` is available to the signed-in user. */
export const page_allowed = (authorized_apps, href) =>
  app_allowed(authorized_apps, PAGE_APPS[href]);
