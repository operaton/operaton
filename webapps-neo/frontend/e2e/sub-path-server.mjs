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
const BACKEND = process.env.E2E_BACKEND_ORIGIN ?? "http://localhost:8084";
// "" is the server root; "/app-neo" a sub-path. Normalised the way env.sh does.
const trimmed = (process.env.E2E_APP_PATH ?? "/app-neo").replace(
  /^\/*|\/*$/g,
  "",
);
const APP_PATH = trimmed ? `/${trimmed}` : "";

const CONFIG = JSON.stringify({
  backends: [{ name: "Operaton", url: BACKEND }],
  authMode: "basic",
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

createServer(async (request, response) => {
  const { pathname } = new URL(request.url, `http://127.0.0.1:${PORT}`);

  if (APP_PATH && pathname === APP_PATH) {
    response.writeHead(301, { Location: `${APP_PATH}/` });
    return response.end();
  }
  if (APP_PATH && !pathname.startsWith(`${APP_PATH}/`)) {
    return send(response, 404, "not found");
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
