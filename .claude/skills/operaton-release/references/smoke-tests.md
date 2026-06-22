# Smoke tests (hybrid)

Two parts: **automated** browser-driven tests against the Docker images, and a **guided manual** checklist for the OS distributions (Tomcat/Wildfly `start.sh`), whose startup is too environment-dependent to automate reliably.

Credentials: `demo` / `demo`.

## Part 1 — Docker images (automate)

Use the reproducible scripts in `.devenv/scripts/smoketest/`. Each image runs on a dedicated port to avoid conflicts with locally-running Operaton instances or other services on 8080:

| Image | Container name | Port |
|-------|---------------|------|
| `operaton/operaton` | `operaton-smoke-operaton` | **18080** |
| `operaton/wildfly` | `operaton-smoke-wildfly` | **18081** |
| `operaton/tomcat` | `operaton-smoke-tomcat` | **18082** |

**Run all three at once:**
```bash
.devenv/scripts/smoketest/smoke-all.sh
# or with a specific tag:
.devenv/scripts/smoketest/smoke-all.sh --tag=2.1.2
```

**Run a single image:**
```bash
.devenv/scripts/smoketest/smoke-test.sh --image=operaton   # port 18080
.devenv/scripts/smoketest/smoke-test.sh --image=wildfly    # port 18081
.devenv/scripts/smoketest/smoke-test.sh --image=tomcat     # port 18082
```

Use `--tag=SNAPSHOT` (default) for PREPARE dry checks, or `--tag=X.Y.Z` during PERFORM verification.

The scripts pull the image, start the container, poll `http://localhost:<PORT>/operaton/app/` for readiness, run `browser-flows.mjs` via Playwright, then tear down automatically. Screenshots land in `.devenv/scripts/smoketest/screenshots/<image>/`.

### Browser flows

**Tasklist** — `http://localhost:<PORT>/operaton/app/tasklist`
1. Login page appears.
2. Login `demo`/`demo` → Tasklist opens with **2** "Assign Reviewer" tasks.
3. Click a task → task form shown.
4. Enter reviewer `John`, click **Complete** → task removed from "My Tasks" (**1** remaining).
5. Click "John's Tasks" → **1** "Review Invoice" task open.

**Cockpit** — `http://localhost:<PORT>/operaton/app/cockpit`
1. Click *Running Process Instances*.
2. Open one process instance.
3. Edit a variable and save → confirmation `The variable '<VAR>' has been changed successfully.` appears.

Any failure → 🔴 blocker for that distribution.

## Part 2 — OS distributions (guided manual)

Generate a checklist for the human and ask them to confirm results (the agent cannot reliably run `start.sh` across OSes):

**Tomcat** — download distro from the release, then:
- `./start.sh` → record Tomcat version
- Browser opens `http://localhost:8080/operaton-welcome/index.html`
- `ps -ef | grep catalina` shows the process
- `grep "ENGINE-08048" -r --include=catalina.out` → "Operaton successfully started at 'Apache Tomcat/<ver>'"
- `grep "ENGINE-08050" -r --include=catalina.out` → "InvoiceProcessApplication successfully deployed"
- Run the Tasklist + Cockpit flows above
- `./shutdown.sh`

**Wildfly** — download distro, `./start.sh` → record Wildfly version, browser opens welcome page, run the webapp flows.

For a PREPARE run, you may note "manual distribution smoke test pending — checklist provided" as a non-blocking 🟡 item if the human hasn't run it yet, unless the user wants it blocking.
