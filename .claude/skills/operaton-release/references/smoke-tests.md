# Smoke tests (hybrid)

Two parts: **automated** browser-driven tests against the Docker images, and a **guided manual** checklist for the OS distributions (Tomcat/Wildfly `start.sh`), whose startup is too environment-dependent to automate reliably.

All webapp flows target `http://localhost:8080`. Credentials: `demo` / `demo`.

## Part 1 — Docker images (automate)

For each image, pull + run, drive the browser flows, then tear down. Use the SNAPSHOT tag for a PREPARE dry check (or the release tag once published, during PERFORM verification).

```bash
# Standalone
docker rm -f operaton 2>/dev/null || true
docker pull operaton/operaton:SNAPSHOT
docker run -d --name operaton -p 8080:8080 operaton/operaton:SNAPSHOT
# wait until http://localhost:8080/operaton/app/ responds, then run the browser flows
# ... teardown:
docker rm -f operaton
```
Repeat for `operaton/wildfly:SNAPSHOT` (container `operaton-wildfly`) and `operaton/tomcat:SNAPSHOT` (container `operaton-tomcat`), one at a time (shared port 8080).

Poll readiness before driving the browser:
```bash
until curl -sf http://localhost:8080/operaton/app/ >/dev/null; do sleep 3; done
```

### Browser flows (use the Claude-in-Chrome / browser MCP)

**Tasklist** — `http://localhost:8080/operaton/app/tasklist`
1. Login page appears.
2. Login `demo`/`demo` → Tasklist opens with **2** "Assign Reviewer" tasks.
3. Click a task → task form shown.
4. Enter reviewer `John`, click **Complete** → task removed from "My Tasks" (**1** remaining).
5. Click "John's Tasks" → **1** "Review Invoice" task open.

**Cockpit** — `http://localhost:8080/operaton/app/cockpit`
1. Click *Running Process Instances*.
2. Open one process instance.
3. Edit a variable and save → confirmation `The variable '<VAR>' has been changed successfully.` appears.

Record pass/fail per image with a screenshot of the key states. Any failure → 🔴 blocker for that distribution.

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
