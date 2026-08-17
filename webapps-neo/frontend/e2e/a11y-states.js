// The state matrix for the accessibility report.
//
// A full cartesian product of pages x themes x viewports x locales x dialogs is
// thousands of scans for almost no extra rule coverage. Two tiers instead:
//
//   Tier A  a global axis applied to every page   (theme x viewport)
//   Tier B  interaction states declared per page  (dialogs, empty, error, sso)
//
// Tier B is where the genuinely unscanned DOM lives: every dialog in the app is
// invisible to the current scans, and dialogs are a classic failure point
// (focus trapping, aria-modal, accessible names).

// The app has no theme toggle — `prefers-color-scheme` is the only driver
// (src/css/style.css) — so the theme axis is a browser context option.
export const THEMES = [
  { id: "light", color_scheme: "light" },
  { id: "dark", color_scheme: "dark" },
];

// The mobile breakpoint is `@media (width <= 70em)` = 1120px
// (src/css/style.css), so the mobile viewport must sit below it and the desktop
// one above. Below the breakpoint the header swaps a <menu> for a <dialog>,
// which is genuinely different DOM rather than a reflow.
export const VIEWPORTS = [
  { id: "desktop", width: 1440, height: 900 },
  { id: "mobile", width: 390, height: 844 },
];

// ---------------------------------------------------------------------------
// Request mocks. These make a state MORE deterministic than the default one:
// the DOM comes from a fixed fixture rather than from whatever the engine holds.
// ---------------------------------------------------------------------------

const ENGINE_GLOB = "**/engine-rest/**";

// Session endpoints must keep working even when we are faking data failures.
// Breaking `identity/verify` logs the app out, so instead of scanning the empty
// or error state we would scan the login screen — and report it as clean.
const SESSION_PATH = /\/engine-rest\/(identity\/verify|engine|version)\b/;

const mock_data = (page, respond) =>
  page.route(ENGINE_GLOB, (route) =>
    SESSION_PATH.test(route.request().url())
      ? route.continue()
      : respond(route),
  );

/** Every list endpoint returns nothing -> the app's empty states. */
export const mock_empty = (page) =>
  mock_data(page, (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: "[]",
    }),
  );

/** Every engine call fails -> RequestState's ERROR branch (src/api/helper.jsx). */
export const mock_error = (page) =>
  mock_data(page, (route) =>
    route.fulfill({
      status: 500,
      contentType: "application/json",
      body: JSON.stringify({ type: "ProcessEngineException", message: "boom" }),
    }),
  );

/**
 * Serve a runtime config that puts the app in OAuth2 mode, so the login card
 * renders its SSO button instead of the username/password form. Cheaper and
 * more reliable than standing up Keycloak just to scan one screen.
 */
export const mock_oauth_config = (page) =>
  page.route("**/config.json", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        backends: [{ name: "Operaton", url: "http://localhost:8084" }],
        authMode: "oauth2",
        oauth: {
          flow: "pkce",
          authority: "http://localhost:8888/realms/operaton",
          clientId: "operaton-web-apps",
          redirectUri: "http://127.0.0.1:5173/",
        },
        hideReleaseWarning: true,
      }),
    }),
  );

// ---------------------------------------------------------------------------
// Tier B — interaction states
//
// `pages`     route names (from routes.js) this state applies to
// `viewports` / `themes`  which axis values it makes sense in; themes defaults
//             to light only, because a dialog's contrast is already covered by
//             the Tier A dark pass of the page behind it
// `mock`      applied before navigation
// `prepare`   run after the page is ready, before the scan
// `ready`     what to wait for once `prepare` has run
// ---------------------------------------------------------------------------

export const INTERACTION_STATES = [
  {
    id: "global-search",
    label: "Global search dialog (open)",
    pages: ["tasks"],
    viewports: ["desktop"],
    themes: ["light", "dark"],
    prepare: (page) => page.locator("#go-to").click(),
    ready: "dialog#global-search[open]",
  },
  {
    id: "mobile-menu",
    label: "Mobile navigation dialog (open)",
    pages: ["tasks"],
    viewports: ["mobile"],
    themes: ["light", "dark"],
    prepare: (page) => page.locator("#mobile-menu-toggle").click(),
    ready: "dialog#mobile-menu[open]",
  },
  {
    id: "upload-dialog",
    label: "Deployment upload dialog (open)",
    pages: ["deployments"],
    viewports: ["desktop"],
    prepare: (page) =>
      page.locator(".button-group button.primary").first().click(),
    ready: "dialog[open]",
  },
  {
    id: "empty-data",
    label: "Empty result set",
    pages: ["tasks", "processes", "batches", "decisions", "deployments"],
    viewports: ["desktop"],
    mock: mock_empty,
  },
  {
    id: "backend-error",
    label: "Backend error (RequestState ERROR)",
    pages: ["tasks", "processes"],
    viewports: ["desktop"],
    mock: mock_error,
  },
  {
    id: "sso-login",
    label: "SSO login card (OAuth2 mode)",
    pages: ["login"],
    viewports: ["desktop"],
    themes: ["light", "dark"],
    mock: mock_oauth_config,
    ready: "section.login-page",
  },
];

// The LOADING state is deliberately absent: sampling a transient state is racy
// by construction and would churn the report on every run. It needs a human
// with the network throttled, not a scanner.

/** Tier A states for one page: every theme x viewport combination. */
export const base_states = () =>
  THEMES.flatMap((theme) =>
    VIEWPORTS.map((viewport) => ({
      id: `${theme.id}-${viewport.id}`,
      label: `default · ${theme.id} · ${viewport.id}`,
      theme,
      viewport,
    })),
  );

/** Tier B states that apply to `page_name`, expanded over their axis values. */
export const interaction_states_for = (page_name) =>
  INTERACTION_STATES.filter((state) => state.pages.includes(page_name)).flatMap(
    (state) =>
      (state.themes ?? ["light"]).flatMap((theme_id) =>
        (state.viewports ?? ["desktop"]).map((viewport_id) => {
          const theme = THEMES.find((t) => t.id === theme_id),
            viewport = VIEWPORTS.find((v) => v.id === viewport_id);
          return {
            id: `${state.id}-${theme_id}-${viewport_id}`,
            label: `${state.label} · ${theme_id} · ${viewport_id}`,
            theme,
            viewport,
            mock: state.mock,
            prepare: state.prepare,
            ready: state.ready,
          };
        }),
      ),
  );
