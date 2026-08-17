/**
 * oauth.js
 *
 * OAuth 2.0 Authorization Code Flow with PKCE.
 * No external dependencies — uses Web Crypto API.
 */

import { RESPONSE_STATE } from "./helper.jsx"

const AUTH_MODE = import.meta.env.VITE_AUTH_MODE || "basic",
  AUTHORITY = import.meta.env.VITE_OAUTH_AUTHORITY,
  CLIENT_ID = import.meta.env.VITE_OAUTH_CLIENT_ID,
  REDIRECT_URI = import.meta.env.VITE_OAUTH_REDIRECT_URI

export const is_oauth = AUTH_MODE === "oauth"

// --- PKCE helpers ---

const generate_random = (length) => {
  const array = new Uint8Array(length)
  crypto.getRandomValues(array)
  return array
}

const base64url_encode = (buffer) =>
  btoa(String.fromCharCode(...new Uint8Array(buffer)))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "")

const generate_pkce = async () => {
  const verifier = base64url_encode(generate_random(32))
  const encoder = new TextEncoder()
  const digest = await crypto.subtle.digest("SHA-256", encoder.encode(verifier))
  const challenge = base64url_encode(digest)
  return { verifier, challenge }
}

// --- OAuth flow ---

export const start_oauth_login = async () => {
  const { verifier, challenge } = await generate_pkce()
  const oauth_state = base64url_encode(generate_random(16))

  sessionStorage.setItem("oauth_code_verifier", verifier)
  sessionStorage.setItem("oauth_state", oauth_state)
  sessionStorage.setItem("oauth_redirect_path", window.location.pathname)

  const params = new URLSearchParams({
    response_type: "code",
    client_id: CLIENT_ID,
    redirect_uri: REDIRECT_URI,
    scope: "openid profile email",
    state: oauth_state,
    code_challenge: challenge,
    code_challenge_method: "S256",
  })

  window.location.href = `${AUTHORITY}/protocol/openid-connect/auth?${params}`
}

export const handle_oauth_callback = async (state) => {
  const params = new URLSearchParams(window.location.search),
    code = params.get("code"),
    returned_state = params.get("state"),
    stored_state = sessionStorage.getItem("oauth_state"),
    verifier = sessionStorage.getItem("oauth_code_verifier")

  if (!code || !verifier) return false

  if (returned_state !== stored_state) {
    console.error("OAuth state mismatch")
    state.auth.logged_in.value = {
      status: RESPONSE_STATE.ERROR,
      data: "oauth_state_mismatch",
    }
    return false
  }

  // Clean URL
  window.history.replaceState({}, "", window.location.pathname)

  const body = new URLSearchParams({
    grant_type: "authorization_code",
    client_id: CLIENT_ID,
    redirect_uri: REDIRECT_URI,
    code,
    code_verifier: verifier,
  })

  try {
    const response = await fetch(
      `${AUTHORITY}/protocol/openid-connect/token`,
      {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body,
      },
    )

    if (!response.ok) throw new Error("Token exchange failed")

    const tokens = await response.json()
    store_tokens(state, tokens)

    sessionStorage.removeItem("oauth_code_verifier")
    sessionStorage.removeItem("oauth_state")

    // Restore original path
    const redirect_path = sessionStorage.getItem("oauth_redirect_path") || "/"
    sessionStorage.removeItem("oauth_redirect_path")
    if (redirect_path !== "/") window.history.replaceState({}, "", redirect_path)

    return true
  } catch (error) {
    console.error("OAuth token exchange failed:", error)
    state.auth.logged_in.value = {
      status: RESPONSE_STATE.ERROR,
      data: "oauth_token_error",
    }
    return false
  }
}

export const refresh_oauth_token = async (state) => {
  const refresh_token = sessionStorage.getItem("oauth_refresh_token")
  if (!refresh_token) return false

  const body = new URLSearchParams({
    grant_type: "refresh_token",
    client_id: CLIENT_ID,
    refresh_token,
  })

  try {
    const response = await fetch(
      `${AUTHORITY}/protocol/openid-connect/token`,
      {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body,
      },
    )

    if (!response.ok) throw new Error("Token refresh failed")

    const tokens = await response.json()
    store_tokens(state, tokens)
    return true
  } catch (error) {
    console.error("OAuth token refresh failed:", error)
    clear_tokens(state)
    return false
  }
}

export const oauth_logout = (state) => {
  const id_token = sessionStorage.getItem("oauth_id_token")
  clear_tokens(state)

  const params = new URLSearchParams({
    post_logout_redirect_uri: REDIRECT_URI,
  })
  if (id_token) params.set("id_token_hint", id_token)

  window.location.href = `${AUTHORITY}/protocol/openid-connect/logout?${params}`
}

export const restore_oauth_session = async (state) => {
  const access_token = sessionStorage.getItem("oauth_access_token")
  if (!access_token) return false

  // Check if token is expired
  if (is_token_expired(access_token)) {
    return refresh_oauth_token(state)
  }

  state.auth.token.value = access_token
  state.auth.logged_in.value = {
    status: RESPONSE_STATE.SUCCESS,
    data: "authenticated",
  }
  set_user_from_token(state, access_token)
  return true
}

// --- Internal helpers ---

const store_tokens = (state, tokens) => {
  sessionStorage.setItem("oauth_access_token", tokens.access_token)
  if (tokens.refresh_token)
    sessionStorage.setItem("oauth_refresh_token", tokens.refresh_token)
  if (tokens.id_token)
    sessionStorage.setItem("oauth_id_token", tokens.id_token)

  state.auth.token.value = tokens.access_token
  state.auth.logged_in.value = {
    status: RESPONSE_STATE.SUCCESS,
    data: "authenticated",
  }
  set_user_from_token(state, tokens.access_token)
}

const clear_tokens = (state) => {
  sessionStorage.removeItem("oauth_access_token")
  sessionStorage.removeItem("oauth_refresh_token")
  sessionStorage.removeItem("oauth_id_token")
  state.auth.token.value = null
  state.auth.logged_in.value = {
    status: RESPONSE_STATE.ERROR,
    data: "unauthenticated",
  }
}

const parse_jwt = (token) => {
  const payload = token.split(".")[1]
  return JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")))
}

const is_token_expired = (token) => {
  try {
    const { exp } = parse_jwt(token)
    return Date.now() >= exp * 1000
  } catch {
    return true
  }
}

const set_user_from_token = (state, token) => {
  try {
    const claims = parse_jwt(token)
    state.auth.user.id.value = claims.preferred_username || claims.sub
  } catch {
    // ignore parse errors
  }
}
