/**
 * Stand-in for a sub-path deployment.
 *
 * Serves the built bundle (`target/`) behind a prefix, doing the two things a
 * real deployment does and a plain static server does not:
 *
 *   1. rewrite `<base href>` in the shell to the application root, the way
 *      SpaIndexTransformer (Spring Boot) and env.sh (the nginx image) do;
 *   2. answer client-side routes with the shell, while still 404ing a missing
 *      asset — masking those is what hides a broken deployment.
 *
 *   3. forwards `{prefix}/api` to the backend, the way a reverse proxy in front
 *      of a real deployment does. Without it the app has no backend of its own:
 *      an absolute `config.json` URL makes every call cross-origin, and the
 *      backend's CORS whitelist omits `authorization`, so the login preflight is
 *      rejected before a single request is made.
 *
 * It also serves `config.json`, so the bundle can be built with no environment
 * at all and still find a backend. Used by e2e/sub-path.spec.js; run it by hand
 * with `node e2e/sub-path-server.mjs` after `npm run build`.
 */
import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { extname, join, normalize, sep } from "node:path";
import { fileURLToPath } from "node:url";

const HERE = fileURLToPath(new URL(".", import.meta.url));

const PORT = Number(process.env.PORT ?? 5173);
const ROOT = process.env.E2E_ASSET_DIR ?? join(HERE, "..", "target");
const BACKEND =
  process.env.E2E_BACKEND_ORIGIN ??
  process.env.E2E_BACKEND_TARGET ??
  "http://localhost:8080";
// "" is the server root; "/app-neo" a sub-path. Normalised the way env.sh does.
const trimmed = (process.env.E2E_APP_PATH ?? "/app-neo").replace(
  /^\/*|\/*$/g,
  "",
);
const APP_PATH = trimmed ? `/${trimmed}` : "";

const CONFIG = JSON.stringify({
  backends: [{ name: "Operaton", url: "" }],
  // Session auth against our own /api: an explicit backend would be cross-origin.
  hideReleaseWarning: true,
});

const TYPES = {
  ".css": "text/css",
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript",
  ".json": "application/json",
  ".png": "image/png",
  ".svg": "image/svg+xml",
  ".ttf": "font/ttf",
  ".woff": "font/woff",
  ".woff2": "font/woff2",
};

const send = (response, status, body, type = "text/plain") => {
  response.writeHead(status, { "Content-Type": type });
  response.end(body);
};

/** The shell, with its base href pointing at the application root. */
const shell = async () => {
  const html = await readFile(join(ROOT, "index.html"), "utf8");
  return html.replace(
    /<base\s+href="[^"]*"\s*\/?>/i,
    `<base href="${APP_PATH}/">`,
  );
};

const serve_file = async (response, relative) => {
  // Keep the resolved path inside ROOT.
  const file = join(ROOT, normalize(relative).replace(/^(\.\.(\/|\\|$))+/, ""));
  if (!file.startsWith(ROOT + sep) && file !== ROOT) return false;
  try {
    const body = await readFile(file);
    send(
      response,
      200,
      body,
      TYPES[extname(file)] ?? "application/octet-stream",
    );
    return true;
  } catch {
    return false;
  }
};

/**
 * Forward `{prefix}/api/...` to the backend's `/api/...`, as the reverse proxy
 * in front of a real deployment does. Cookies, CSRF headers and the body pass
 * through untouched, and the upstream status is preserved.
 */
const read_body = (request) =>
  new Promise((resolve, reject) => {
    const chunks = [];
    request.on("data", (chunk) => chunks.push(chunk));
    request.on("end", () => resolve(Buffer.concat(chunks)));
    request.on("error", reject);
  });

// Transport-level headers the proxy resolves itself; forwarding them would
// describe the wrong hop (or, for content-encoding, the wrong body).
const HOP_BY_HOP = new Set([
  "host",
  "origin",
  "referer",
  "connection",
  "content-length",
  "accept-encoding",
  "transfer-encoding",
]);

const proxy_api = async (request, response, pathname, search) => {
  const target = `${BACKEND}${pathname.slice(APP_PATH.length)}${search}`;
  const headers = Object.fromEntries(
    Object.entries(request.headers).filter(([key]) => !HOP_BY_HOP.has(key)),
  );
  const body =
    request.method === "GET" || request.method === "HEAD"
      ? undefined
      : await read_body(request);

  try {
    const upstream = await fetch(target, {
      method: request.method,
      headers,
      body,
      redirect: "manual",
    });
    const out = {};
    for (const [key, value] of upstream.headers) {
      if (key === "set-cookie" || HOP_BY_HOP.has(key)) continue;
      out[key] = value;
    }
    // Several Set-Cookie headers collapse into one when read as a plain entry.
    const cookies = upstream.headers.getSetCookie?.() ?? [];
    if (cookies.length) out["set-cookie"] = cookies;
    response.writeHead(upstream.status, out);
    response.end(Buffer.from(await upstream.arrayBuffer()));
  } catch (error) {
    console.log(`proxy ${target}: ${error.message}`);
    send(response, 502, `backend unavailable: ${error.message}`);
  }
};

createServer(async (request, response) => {
  const { pathname, search } = new URL(request.url, `http://127.0.0.1:${PORT}`);

  if (APP_PATH && pathname === APP_PATH) {
    response.writeHead(301, { Location: `${APP_PATH}/` });
    return response.end();
  }
  if (APP_PATH && !pathname.startsWith(`${APP_PATH}/`)) {
    return send(response, 404, "not found");
  }

  // The app's own API, proxied to the backend the way a real deployment does.
  if (pathname.startsWith(`${APP_PATH}/api/`)) {
    return proxy_api(request, response, pathname, search);
  }

  const relative = pathname.slice(APP_PATH.length + 1);

  if (relative === "config.json") {
    return send(response, 200, CONFIG, "application/json");
  }
  if (relative === "" || relative === "index.html") {
    return send(response, 200, await shell(), TYPES[".html"]);
  }
  if (await serve_file(response, relative)) return;

  // A last segment with an extension is a (missing) asset, not a route — the
  // same rule SpaResourceResolver applies on the server.
  const last = relative.split("/").pop();
  if (last.includes(".")) return send(response, 404, "not found");

  return send(response, 200, await shell(), TYPES[".html"]);
}).listen(PORT, "127.0.0.1", () => {
  console.log(
    `sub-path server: http://127.0.0.1:${PORT}${APP_PATH}/ (root: ${ROOT})`,
  );
});
