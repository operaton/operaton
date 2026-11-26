// Cross-platform entry for the sub-path suite.
//
// The suite is opt-in through the E2E_SUB_PATH environment variable, which
// playwright.config.js reads at load time to select the prefix-serving
// webServer instead of the dev server. `<VAR>=1 command` is a POSIX shell
// construct that cmd.exe cannot parse, so npm runs this file instead: it sets
// the variable and forwards every extra argument to the Playwright CLI.
//
// Usage:
//   npm run test:e2e:sub-path              # whole project
//   npm run test:e2e:sub-path -- --ui      # anything after -- is forwarded
//   npm run test:e2e:sub-path -- -g reload # pick a single test
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const cli = join(root, "node_modules", "@playwright", "test", "cli.js");

const result = spawnSync(
  process.execPath,
  [cli, "test", "--project=sub-path", ...process.argv.slice(2)],
  {
    cwd: root,
    stdio: "inherit",
    env: {
      ...process.env,
      // Selects the sub-path project and its production-build webServer.
      E2E_SUB_PATH: "1",
      // sub-path-server.mjs proxies /<prefix>/api here. Matching the dev
      // server default keeps a plain `npm run test:e2e:sub-path` working
      // without any manual setup; override for the docker stack on :8084.
      E2E_BACKEND_ORIGIN:
        process.env.E2E_BACKEND_ORIGIN ??
        process.env.E2E_BACKEND_TARGET ??
        "http://localhost:8080",
    },
  },
);

if (result.error) {
  console.error(result.error.message);
  process.exit(1);
}

process.exit(result.status ?? 1);
