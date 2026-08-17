# Environment Variables

We use Vite as a build tool: https://vite.dev/guide/env-and-mode

## Where configuration comes from

The `VITE_*` variables below are baked into the bundle at build time, so they
only configure the **development server**. A deployed build reads its settings at
startup from `config.json` on its own origin, which means the same bundle can be
reconfigured without rebuilding it:

| Deployment | `config.json` comes from | Configured with |
| --- | --- | --- |
| Embedded in Operaton (Run) | Served by the backend | `operaton.bpm.webapp.neo.*`, i.e. `OPERATON_BPM_RUN_*` environment variables — see `distro/run/assembly/resources/default.yml` |
| Standalone Docker image | A static file rewritten by the container entrypoint | `DOCKER_RUN_PLACEHOLDER_*` environment variables — see `Docker Build.md` |
| `npm run dev` | Not served; falls back to `VITE_*` | `.env.development.local` |

The document looks like this — everything except `authMode` is optional:

```json
{
  "backends": [{ "name": "Operaton", "url": "" }],
  "authMode": "basic",
  "oauth": { "flow": "session", "login": "/oauth2/authorization/operaton", "logout": "/logout" },
  "pluginsUrl": "/plugins/plugins.json",
  "hideReleaseWarning": false,
  "user": { "id": "demo" }
}
```

`authMode` is `basic` (username and password against the REST API) or `oauth2`.
Embedded, the backend decides: it defaults to `basic` and switches to `oauth2`
by itself once Spring Security OAuth2 client registrations are configured, which
is what happens when you integrate an identity provider such as
[operaton-keycloak](https://github.com/operaton/operaton-keycloak). Override it
with `operaton.bpm.webapp.neo.auth-mode`.

`oauth.flow` selects who performs the handshake. `session` means the server does
it and the app rides the resulting session cookie — the embedded case. `pkce`
means the browser talks to the identity provider directly, which is what the
standalone image does, and then `authority`, `clientId` and `redirectUri` are
required.

`user` is present only when the caller already has a session, letting the app
skip the login screen on reload.

## Overview on our Env Vars

### VITE_BACKEND

A list of possible backends a user can switch between when using the
application.
The data is a JSON list with objects consisting of a `name` and `url` string. If you use Camunda 7 (C7) as a backend, you need to additionally set `c7_mode` to true.

```JSON
[
  {
    "name": "Operaton Local Dev",
    "url": "http://localhost:8080"
  },
  {
    "name": "C7 Prod",
    "url": "https://processes.example.com",
    "c7_mode": true
  }
]
```

The URL string has the following structure:  
`{http|https}` + `://` + `{your.domain}` + `{port|_}`

E.g.:

- `http://localhost:8080`
- `https://operaton.example.com`

The resulting entry in the `.env`-files can look like the following

```properties
# .env.development
VITE_BACKEND=[{"name": "Dev Operaton", "url": "http://localhost:8084"}, {"name": "Dev c7", "url": "http://localhost:8088", "c7_mode": true}]
```

> **Important**: This config works with the `docker-compose.yaml` setup and is also
> already set in `.env.development`. You can use the `.env.development.local` file
> to change this config to you preferences. For IntelliJ users this is also provided
> as run configuration.

### VITE_PLUGINS_URL

URL of the runtime plugin manifest — a JSON array of remote plugin packages the
app loads at boot. Optional; defaults to `/plugins/plugins.json` when unset.

```properties
# .env
VITE_PLUGINS_URL=/plugins/plugins.json
```

See [Plugin System.md](Plugin%20System.md) for the manifest shape and the full
plugin mechanism.
