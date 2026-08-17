/**
 * config.js
 *
 * Runtime configuration for the web apps.
 *
 * These settings used to be compiled into the bundle by Vite, which meant a
 * distribution could only be reconfigured by rebuilding the frontend. They are
 * now fetched from `config.json` on our own origin at boot:
 *
 *   - embedded in Operaton (Run), Spring serves it from the application
 *     configuration, so `OPERATON_BPM_RUN_*` environment variables reach us
 *   - standalone (Docker image), it is a static file the container entrypoint
 *     rewrites from the environment
 *
 * The `VITE_*` variables remain as the fallback so `npm run dev` keeps working
 * from `.env.development` with no server involved.
 */

const AUTH_MODE_BASIC = "basic",
  AUTH_MODE_OAUTH2 = "oauth2"

// Where config.json lives, relative to the document. Honours a sub-path
// deployment (`operaton.bpm.webapp.neo.application-path`) because index.html is
// served from that path too.
const config_url = () => new URL("config.json", document.baseURI).href

// A DOCKER_RUN_PLACEHOLDER_* name that was never substituted must be treated as
// unset, otherwise it is taken for a real value. See env.sh.
const clean = (value) =>
  typeof value === "string" && value && !value.includes("PLACEHOLDER")
    ? value
    : undefined

const parse_backends = (raw) => {
  const value = clean(raw)
  if (!value) return undefined
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) && parsed.length ? parsed : undefined
  } catch {
    return undefined
  }
}

/**
 * Configuration from the build-time environment. Used by the dev server, and as
 * the fallback when no config.json is served.
 */
const from_env = () => {
  const mode = clean(import.meta.env.VITE_AUTH_MODE),
    authority = clean(import.meta.env.VITE_OAUTH_AUTHORITY)

  return {
    backends: parse_backends(import.meta.env.VITE_BACKEND) ?? [
      { name: "Operaton", url: "" },
    ],
    // "oauth" was the historical spelling of the mode
    auth_mode: mode === "oauth" || mode === AUTH_MODE_OAUTH2 ? AUTH_MODE_OAUTH2 : AUTH_MODE_BASIC,
    // Without an authority there is nothing to talk PKCE to, so the handshake
    // belongs to the server we are served from.
    oauth: authority
      ? {
          flow: "pkce",
          authority,
          client_id: clean(import.meta.env.VITE_OAUTH_CLIENT_ID),
          redirect_uri: clean(import.meta.env.VITE_OAUTH_REDIRECT_URI),
        }
      : undefined,
    plugins_url: clean(import.meta.env.VITE_PLUGINS_URL),
    hide_release_warning: clean(import.meta.env.VITE_HIDE_RELEASE_WARNING) === "true",
    user: undefined,
  }
}

/**
 * Map a config document onto our shape. Tolerant on purpose: the same shape is
 * produced by the server (real JSON types) and by the Docker entrypoint
 * substituting strings into a static file, so booleans may arrive as "true".
 */
const from_document = (json) => {
  const mode = clean(json.authMode),
    is_oauth = mode === "oauth" || mode === AUTH_MODE_OAUTH2,
    authority = clean(json.oauth?.authority)

  // A pkce flow without an authority has nothing to talk to; treat it as absent
  // rather than sending the user to "undefined/...".
  const oauth =
    is_oauth && json.oauth && (json.oauth.flow !== "pkce" || authority)
      ? {
          flow: json.oauth.flow ?? "session",
          login: clean(json.oauth.login),
          logout: clean(json.oauth.logout),
          authority,
          client_id: clean(json.oauth.clientId),
          redirect_uri: clean(json.oauth.redirectUri),
        }
      : undefined

  return {
    backends:
      Array.isArray(json.backends) && json.backends.length
        ? json.backends
        : [{ name: "Operaton", url: "" }],
    // Without a usable oauth configuration, fall back to asking for credentials
    // instead of showing a login button that cannot work.
    auth_mode: is_oauth && oauth ? AUTH_MODE_OAUTH2 : AUTH_MODE_BASIC,
    oauth,
    plugins_url: clean(json.pluginsUrl),
    hide_release_warning:
      json.hideReleaseWarning === true || clean(json.hideReleaseWarning) === "true",
    user: json.user?.id ? { id: json.user.id } : undefined,
  }
}

let config = null

/**
 * Fetch the runtime configuration. Falls back to the build-time environment when
 * no config.json is served — a plain `vite build` deployment behaves as before.
 * Never rejects: the app must still render a login screen if this request fails.
 */
export const load_config = async () => {
  try {
    const response = await fetch(config_url(), {
      headers: { Accept: "application/json" },
      credentials: "include",
    })
    config = response.ok ? from_document(await response.json()) : from_env()
  } catch {
    config = from_env()
  }
  return config
}

/**
 * The loaded configuration. Falls back to the environment when read before
 * `load_config()` resolves, so tests and stray early reads cannot crash the app.
 */
export const get_config = () => config ?? (config = from_env())

/** Test seam. */
export const set_config = (value) => (config = value ? from_document(value) : null)
