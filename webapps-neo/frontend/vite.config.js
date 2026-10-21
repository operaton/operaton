import { defineConfig, loadEnv } from "vite";
import preact from "@preact/preset-vite";

// The app resolves config.json and its API base against document.baseURI. On a
// deep link the dev server serves index.html unchanged, so that base is the
// route itself and every such URL is looked up under /tasks/<id>/… instead of
// the app root. A deployment injects the tag; in dev nobody does.
const devBaseHref = () => ({
  name: "dev-base-href",
  apply: "serve",
  transformIndexHtml: (html) =>
    /<base\s/i.test(html)
      ? html
      : html.replace(/<head>/i, '<head>\n        <base href="/">'),
});

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  return {
    plugins: [preact(), devBaseHref()],
    // Emit relative asset URLs. Together with the <base href> in index.html — which
    // the server rewrites to the application root — the bundle works both from the
    // server root and from a sub-path (operaton.bpm.webapp.neo.application-path).
    // The dev server ignores this and serves from '/', which is what index.html
    // declares verbatim.
    base: "./",
    // The Maven assembly (webapps-neo/assembly) packages the build from
    // frontend/target, and the root pom's clean plugin cleans that directory.
    build: {
      outDir: "target",
      emptyOutDir: true,
    },
    server: {
      host: "127.0.0.1",
      proxy: {
        // '/api': 'http://localhost:8084',
        "/api": {
          // Override with VITE_PROXY_TARGET to develop against another engine.
          target: env.VITE_PROXY_TARGET || "http://localhost:8084",
          changeOrigin: true,
          // Operaton Run serves the web-app API under /operaton; a Spring Boot
          // app that hosts these web apps serves it at /api. Keep the prefix
          // for the latter, or the session login never reaches the backend.
          ...(env.VITE_PROXY_KEEP_API_PREFIX === "true"
            ? {}
            : { rewrite: (path) => path.replace(/^\/api/, "") }),
        },
      },
    },
  };
});
