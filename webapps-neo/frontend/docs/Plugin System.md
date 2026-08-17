# Plugin System

The web app ships a runtime **plugin system** so third parties (and our own
optional features) can extend the UI without forking the app. It is modeled on
the Camunda 7 / Operaton webapp plugin mechanism, adapted to this standalone
Preact SPA. A plugin can add a whole page, drop a widget on the dashboard, and
bring its own API calls, state, translations, and styles — all described by a
single **descriptor** object.

> **Note**: Plugins extend the app at the **root** — a top-level page, a
> dashboard card, or a headless API/state namespace. The core application's own
> sub-navigations (the Processes definition tabs, the Task detail tabs) are
> intentionally **closed**; there is no plugin point for injecting into them.

## Concept

Everything is driven by one idea: a **descriptor** registered against a named
**plugin point**. At boot the [loader](../src/plugins/loader.js) collects every
descriptor into a single in-memory [registry](../src/plugins/registry.js), then
the app renders. Because the registry is populated **before the first
`render()` and never mutated afterwards**, every host seam can read it
synchronously — no reactivity, no re-render churn.

Plugins come from two sources that share the exact same descriptor contract:

- **Bundled** first-party plugins that ship inside the app bundle
  (`src/plugins/bundled/*/plugin.jsx`, discovered via `import.meta.glob`).
- **Remote** plugins discovered at runtime from a JSON manifest and dynamically
  imported — the standalone-deployment path, where an operator drops plugins
  next to the served app without rebuilding it.

A broken plugin can never brick the app: each one registers/loads in isolation,
and a slow or missing remote manifest is guarded by a timeout.

## Plugin Points

The three extension points live in [`points.js`](../src/plugins/points.js). A
descriptor's `point` must be one of these:

| Constant                         | Value              | What it does                                                         |
| -------------------------------- | ------------------ | -------------------------------------------------------------------- |
| `PLUGIN_POINTS.PAGE`             | `app.page`         | A whole app section: route + primary-nav entry + hotkey + GoTo entry |
| `PLUGIN_POINTS.DASHBOARD_WIDGET` | `dashboard.widget` | A card rendered on the dashboard                                     |
| `PLUGIN_POINTS.API`              | `app.api`          | A pure API/state namespace, no UI                                    |

## The Descriptor Contract

A plugin module's `default` export is a descriptor, or an array of them. The
canonical shape (documented in the [`registry.js`](../src/plugins/registry.js)
header):

```js
{
  id: string,                // unique across all plugins
  point: string,             // one of PLUGIN_POINTS
  priority?: number,         // sort order within a point (higher = earlier), default 0
  properties?: object,       // point-specific config (see below)
  Component?: ComponentType, // Preact component for PAGE / DASHBOARD_WIDGET
  api?: object,              // fn leaves (state, ...args) => Promise → engine_rest.plugins[id]
  signals?: () => object,    // factory → state.api.plugins[id]
  translations?: object,     // { "en-US": {…}, "de-DE": {…} } deep-merged into the i18n namespace
}
```

`properties` is point-specific:

- **PAGE** — `path` and `href` (the route), `nameKey` (i18n key for the nav
  label), optional `hotkey` (e.g. `"alt+shift+8"`).
- Any point may set `apiBase` to route the plugin's `get`/`post` calls to its
  own backend instead of the engine REST API (see
  [`use_plugin_api`](#the-use_plugin_api-hook)).

> **Important**: `id` must be unique and `point` must be a known value.
> Invalid or duplicate descriptors are logged and skipped — they never throw.

### Contributing an API namespace and state

- `api` is deep-mounted at `engine_rest.plugins.<id>`. Each function should
  follow the host convention `(state, ...args) => VERB(url, state, signal)`, so
  auth headers, error shapes, and `RESPONSE_STATE` all come for free from
  [`helper.jsx`](../src/api/helper.jsx).
- `signals` is a factory whose returned object is mounted at
  `state.api.plugins.<id>`. Use it for the signals your `api` writes into.
- `translations` is deep-merged into the `translation` namespace and **never
  overwrites host keys**, so namespace them under `plugins.<id>.*`.

> **Important**: The registry defers each plugin's `addResourceBundle` until
> i18next has loaded the app's own `translation.json` for the active language.
> This is deliberate — adding a partial bundle for a language _before_ its http
> backend load makes i18next treat that language as already present and skip the
> fetch, which would wipe out every base key (`missingKey` for all built-in
> strings). Plugin authors don't need to do anything; just be aware plugin
> strings appear a tick after the base UI on first load.

## Authoring a Plugin

### The `use_plugin_api` hook

Inside a plugin `Component`, call
[`use_plugin_api(id)`](../src/plugins/plugin_api.jsx) to get the render-time
context (this is the idiomatic Preact replacement for Camunda's
`render(container, data)` argument):

```jsx
const { params, query, state, engine, signals, api, resolve_url, get, post } =
  use_plugin_api("metrics");
```

- `params` / `query` — the current route's params and query string.
- `signals` — the plugin's own state branch (`state.api.plugins.metrics`).
- `api` — the plugin's mounted API namespace (`engine_rest.plugins.metrics`).
- `get(path, signal)` / `post(path, body, signal)` — scoped verbs. When the
  descriptor sets `properties.apiBase`, they fetch the plugin's own backend and
  write the same `{ status, data }` shape `RequestState` renders; otherwise they
  delegate to the standard engine-rest `GET`/`POST`. **The same plugin code thus
  works unchanged whether embedded or talking to a separate service.**

### Worked example — Engine Metrics

[`src/plugins/bundled/metrics/plugin.jsx`](../src/plugins/bundled/metrics/plugin.jsx)
is the reference bundled plugin — a PAGE that owns its API, state, translations,
and CSS, sharing no host signals, which proves namespace isolation. Its default
export is an array of one descriptor (an array so a plugin can ship several):

```jsx
export default [
  {
    id: "metrics",
    point: PLUGIN_POINTS.PAGE,
    properties: {
      path: "/plugin/metrics",
      href: "/plugin/metrics",
      nameKey: "plugins.metrics.nav",
      hotkey: "alt+shift+8",
    },
    Component: MetricsPage,
    api, // → engine_rest.plugins.metrics.*
    signals: make_signals, // → state.api.plugins.metrics.*
    translations, // en-US + de-DE under plugins.metrics.*
  },
];
```

Bundled plugins import host primitives directly. The blessed one-import surface
is [`src/plugins/index.js`](../src/plugins/index.js):

```jsx
import {
  PLUGIN_POINTS,
  use_plugin_api,
  RequestState,
  GET,
  POST,
} from "../../index.js";
```

### Remote (no-build) plugins

A remote plugin is plain JS served next to the app — it cannot `import` from the
app bundle. Instead the host exposes its own framework instances on
`window.__OPERATON_PLUGIN_HOST__` (see [`host.js`](../src/plugins/host.js)) so
the plugin shares the app's **single** Preact instance (bundling its own copy
would break `useContext(AppState)`). Such a plugin reads `h`, `hooks`,
`signals`, `register`, `use_plugin_api`, `PLUGIN_POINTS`, etc. from that object
and authors markup with `html` (htm bound to the host's `h`).

## Implementation

### Modules (`src/plugins/`)

| File                | Responsibility                                                                                                                                  |
| ------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| `points.js`         | The `PLUGIN_POINTS` enum.                                                                                                                       |
| `registry.js`       | The in-memory registry. `register()` validates + mounts i18n/API; read seams `plugins_for()`, `plugin_descriptor()`, `plugin_state_branches()`. |
| `loader.js`         | `load_plugins()` — registers bundled plugins, then discovers and imports remote ones under a timeout.                                           |
| `plugin_api.jsx`    | `use_plugin_api(id)` — the render-time context hook.                                                                                            |
| `host.js`           | `install_plugin_host()` — exposes `window.__OPERATON_PLUGIN_HOST__` for remote plugins.                                                         |
| `index.js`          | The public import surface for bundled plugin authors.                                                                                           |
| `../api/plugins.js` | Dependency-free container for mounted API namespaces (also exposed as `engine_rest.plugins`).                                                   |

`api/plugins.js` is deliberately dependency-free so importing it from the
registry — which `state.js` pulls in — never drags in the HTTP helpers that
tests mock.

### Boot sequence

[`index.jsx`](../src/index.jsx) wires it all together:

```jsx
install_plugin_host();
load_plugins().finally(() => render(<App />, document.getElementById("app")));
```

Host primitives are exposed, then every plugin (bundled + remote) loads **before
the first render**, so the frozen registry is stable for every seam. If the
remote plugin server hangs, the loader's timeout still lets the app boot.

### Host seams

Each seam simply reads the frozen registry:

- **Routes** — `index.jsx` injects `plugins_for(PLUGIN_POINTS.PAGE)` as `<Route>`s.
- **Navigation** — [`Header.jsx`](../src/components/Header.jsx) merges PAGE
  plugins into the primary nav (rendered once for both desktop and mobile) and
  binds their hotkeys.
- **Global search** — [`GoTo.jsx`](../src/components/GoTo.jsx) adds PAGE plugins
  to the command palette.
- **Dashboard** — [`Dashboard.jsx`](../src/pages/Dashboard.jsx) renders
  `plugins_for(PLUGIN_POINTS.DASHBOARD_WIDGET)`.
- **State / API** — `state.js` mounts `state.api.plugins.<id>` and
  `engine_rest.jsx` mounts `engine_rest.plugins.<id>`.

## Configuration

### VITE_PLUGINS_URL

URL of the remote plugin **manifest**, a JSON array of plugin packages. Defaults
to `/plugins/plugins.json` when unset (see
[`public/plugins/plugins.json`](../public/plugins/plugins.json), which ships
empty). Empty values and unreplaced Docker placeholders are ignored.

```properties
# .env
VITE_PLUGINS_URL=/plugins/plugins.json
```

Each manifest entry describes one remote package:

```JSON
[
  {
    "name": "example",
    "location": "/plugins/example",
    "main": "plugin.js",
    "css": "plugin.css"
  }
]
```

- `location` — base URL of the package (required).
- `main` — entry module, defaults to `plugin.js`.
- `css` — optional stylesheet injected as a `<link>`.

`main` and `css` are loaded with a `?bust=<VITE_APP_VERSION>` cache-busting
query. At runtime, a `window.PLUGIN_PACKAGES` array (future servlet injection)
takes precedence over the fetched manifest.

See [Environment Variables.md](Environment%20Variables.md) for the full env-var
list.

## Testing

Plugin modules are unit-tested in `src/plugins/*.test.js(x)` and
`src/api/plugins.test.js`; the host seams have integration coverage in
`src/components/Header.test.jsx`, `src/components/GoTo.test.jsx`, and
`src/pages/Dashboard.test.jsx`. Reset the shared registry between tests with
`_reset_registry()` (and clear `plugin_apis`) so registrations don't leak across
cases — see the existing tests for the pattern.
