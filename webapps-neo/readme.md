# Webapps Neo

The new Operaton web apps.

## Building

The web apps can be build by using Maven:

`mvn clean install`

For development you can use the build options provided by npm and vite.
See [frontend/README.md](frontend/README.md) for details.

### Going Fast

`mvnd clean install -DskipTests -PskipFrontendBuild`

- `mvnd`: mvn with parrallel threads
- `-DskipTests`: Skip tests
- `-PskipFrontendBuild`: Skip building old web apps

Roughly reduces build times on MacPro with M1 chip from 3 minutes to 1 minute.

## Running the web apps

The new web apps can run in several ways. **In every embedded variant they are
disabled by default** and have to be switched on explicitly. Once enabled they
are served at the **application root** (`http://localhost:8080/`). The legacy
web apps, if present, stay at `http://localhost:8080/operaton/`.

| Option | Enable with | URL (defaults) |
| --- | --- | --- |
| [Spring Boot starter](#spring-boot-starter) | `operaton.bpm.webapp.neo.enabled=true` | `http://localhost:8080/` |
| [Operaton Run](#operaton-run) | `--webapps-neo` or `OPERATON_BPM_RUN_ENABLE_NEW_WEB_APPS=true` | `http://localhost:8080/` |
| [Tomcat / WildFly distribution](#tomcat-and-wildfly-distributions) | `-Doperaton.bpm.webapp.neo.enabled=true` or `OPERATON_BPM_WEBAPP_NEO_ENABLED=true` | `http://localhost:8080/` |
| [Standalone Docker image](#standalone-docker-image) | always on | `http://localhost:{port}/` |
| [Vite dev server](#vite-dev-server) | always on | `http://127.0.0.1:5173/` |

If you only see a 404 or end up on the legacy web apps, the new web apps are not
enabled yet.

### Spring Boot starter

1. Add the starter to your application, next to the regular Operaton starter:

   ```xml
   <dependency>
     <groupId>org.operaton.bpm.springboot</groupId>
     <artifactId>operaton-bpm-spring-boot-starter-webapp-neo</artifactId>
     <version>${operaton.version}</version>
   </dependency>
   ```

   The starter brings its own engine API (served at `{applicationPath}/api/engine`),
   so the REST starter is not required.

2. Enable the web apps and create a user to log in with, e.g. in
   `application.yaml`:

   ```yaml
   operaton:
     bpm:
       admin-user:
         id: demo
         password: demo
       webapp:
         neo:
           enabled: true
   ```

3. Start the application (`mvn spring-boot:run`) and open
   `http://localhost:8080/`.

The URL is composed of `server.servlet.context-path` and
`operaton.bpm.webapp.neo.application-path`, both empty by default. To move the
web apps to a sub-path, set for example:

```yaml
operaton:
  bpm:
    webapp:
      neo:
        application-path: /app-neo   # -> http://localhost:8080/app-neo/
```

With a sub-path, `/` redirects to it unless
`operaton.bpm.webapp.neo.index-redirect-enabled` is `false`. If the legacy
starter (`operaton-bpm-spring-boot-starter-webapp`) is on the classpath as well,
it gives up its own `/` redirect while the new web apps are enabled and remains
reachable at `/operaton/app/`.

A complete example project is available at
[operaton-spring-boot-example](https://github.com/javahippie/operaton-spring-boot-example).
To try a locally built version, set the version you built in its `pom.xml`.

#### Configuration properties

All properties live under `operaton.bpm.webapp.neo`:

| Property | Default | Description |
| --- | --- | --- |
| `enabled` | `false` | Turns the web apps on. |
| `application-path` | `""` (root) | Path the web apps are served from. `app-neo`, `/app-neo` and `/app-neo/` are equivalent. |
| `index-redirect-enabled` | `true` | Redirect `/` to the application path when it is not the root. |
| `auth-mode` | `auto` | `basic`, `oauth2` or `auto`. `auto` uses `oauth2` as soon as a Spring Security OAuth2 client registration (`spring.security.oauth2.client.registration.*`) is configured, `basic` otherwise. |
| `plugins-url` | `""` | URL of the runtime plugin manifest. Empty means `/plugins/plugins.json` on the application's own origin. |

#### Setting them via environment variables

Spring Boot maps environment variables onto these properties by
[relaxed binding](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties.relaxed-binding.environment-variables):
upper-case the name, replace `.` with `_` and **drop the dashes**:

| Property | Environment variable |
| --- | --- |
| `operaton.bpm.webapp.neo.enabled` | `OPERATON_BPM_WEBAPP_NEO_ENABLED` |
| `operaton.bpm.webapp.neo.application-path` | `OPERATON_BPM_WEBAPP_NEO_APPLICATIONPATH` |
| `operaton.bpm.webapp.neo.index-redirect-enabled` | `OPERATON_BPM_WEBAPP_NEO_INDEXREDIRECTENABLED` |
| `operaton.bpm.webapp.neo.auth-mode` | `OPERATON_BPM_WEBAPP_NEO_AUTHMODE` |
| `operaton.bpm.webapp.neo.plugins-url` | `OPERATON_BPM_WEBAPP_NEO_PLUGINSURL` |

```bash
OPERATON_BPM_WEBAPP_NEO_ENABLED=true \
OPERATON_BPM_WEBAPP_NEO_APPLICATIONPATH=/app-neo \
  java -jar my-operaton-app.jar
```

These values are not baked into the frontend: the backend serves them to the
browser at runtime from `{applicationPath}/config.json`, so changing them only
needs a restart, not a rebuild.

### Operaton Run

Start Run with the `--webapps-neo` flag:

```bash
./start.sh --webapps-neo
```

This enables the new web apps and the REST API, but not the legacy web apps.
Alternatively, set `OPERATON_BPM_RUN_ENABLE_NEW_WEB_APPS=true` before starting
Run without flags to get the new web apps in addition to the legacy ones and
the REST API:

```bash
OPERATON_BPM_RUN_ENABLE_NEW_WEB_APPS=true ./start.sh
```

The same variable works for the `operaton/operaton` container image:

```bash
docker run -p 8080:8080 -e OPERATON_BPM_RUN_ENABLE_NEW_WEB_APPS=true operaton/operaton:latest
podman run -p 8080:8080 -e OPERATON_BPM_RUN_ENABLE_NEW_WEB_APPS=true operaton/operaton:latest
```

Open `http://localhost:8080/` and log in with `demo`/`demo`.

`configuration/default.yml` maps a few Run-specific environment variables onto
the properties above:

| Environment variable | Property |
| --- | --- |
| `OPERATON_BPM_RUN_ENABLE_NEW_WEB_APPS` | `operaton.bpm.webapp.neo.enabled` (also puts the web apps on the classpath) |
| `OPERATON_BPM_RUN_WEBAPPS_NEO_AUTH_MODE` | `operaton.bpm.webapp.neo.auth-mode` |
| `OPERATON_BPM_RUN_WEBAPPS_NEO_PLUGINS_URL` | `operaton.bpm.webapp.neo.plugins-url` |

Run is a Spring Boot application, so every other property can be set with the
relaxed-binding names from the [Spring Boot section](#setting-them-via-environment-variables),
e.g. `OPERATON_BPM_WEBAPP_NEO_APPLICATIONPATH=/app-neo`.

### Tomcat and WildFly distributions

The distributions ship the new web apps as a WAR deployed at the context root
`/`. Enable them with the system property `operaton.bpm.webapp.neo.enabled=true`
(see the commented line in `bin/setenv.sh`/`setenv.bat` for Tomcat and
`bin/standalone.conf`/`standalone.conf.bat` for WildFly) or the environment
variable `OPERATON_BPM_WEBAPP_NEO_ENABLED=true`:

```bash
OPERATON_BPM_WEBAPP_NEO_ENABLED=true ./server/apache-tomcat-*/bin/startup.sh
```

While disabled, `http://localhost:8080/` redirects to the legacy web apps at
`/operaton/`. The properties from the Spring Boot section are not available
here: the web apps use basic authentication against the server they are served
from.

### Standalone Docker image

The image in [`frontend/`](frontend/) serves the web apps with nginx and connects
to any Operaton or Camunda 7 backend that exposes the REST API at
`{url}/engine-rest`. The backend must allow the web apps' origin via CORS.
Settings are passed as `DOCKER_RUN_PLACEHOLDER_*` environment variables:

```bash
docker build -t operaton-webapps-standalone frontend
docker run -p 8088:80 \
  -e ASSET_DIR=/var/www/html \
  -e APP_PREFIX=DOCKER_RUN_PLACEHOLDER_ \
  -e 'DOCKER_RUN_PLACEHOLDER_BACKEND=[{"name": "Operaton", "url": "http://localhost:8080"}]' \
  -e DOCKER_RUN_PLACEHOLDER_AUTH_MODE=basic \
  operaton-webapps-standalone

podman build -t operaton-webapps-standalone frontend
podman run -p 8088:80 \
  -e ASSET_DIR=/var/www/html \
  -e APP_PREFIX=DOCKER_RUN_PLACEHOLDER_ \
  -e 'DOCKER_RUN_PLACEHOLDER_BACKEND=[{"name": "Operaton", "url": "http://localhost:8080"}]' \
  -e DOCKER_RUN_PLACEHOLDER_AUTH_MODE=basic \
  operaton-webapps-standalone
```

The web apps are then available at `http://localhost:8088/`. Quote the backend
JSON so the shell does not mangle it. See
[Docker Build.md](frontend/docs/Docker%20Build.md) for all variables, including
OAuth2 (PKCE) and `DOCKER_RUN_PLACEHOLDER_APPLICATION_PATH` for serving from a
sub-path.

### Vite dev server

For frontend development, run `npm run dev` in [`frontend/`](frontend/) against
a running backend and open `http://127.0.0.1:5173/` (use `127.0.0.1`, not
`localhost`, to avoid CORS issues). The backend is configured with `VITE_*`
variables in `frontend/.env.development.local`. These are compiled into the
bundle and only affect the dev server, not any of the deployments above. See
[frontend/README.md](frontend/README.md) and
[Environment Variables.md](frontend/docs/Environment%20Variables.md).

## Additional Information

The compiled contents of the `webapps-neo` package are packaged as the
`operaton-webapp-neo-webjar` and used as a dependency in
`spring-boot-starter/starter-webapp-neo(-core)`, which serves them as described
above.

## Security filter rules

`assembly/src/main/webapp/WEB-INF/securityFilterRules.json` is read by
`SecurityFilter` at startup and moved to the webjar root during packaging. JSON
takes no comments, so the reasoning lives here.

How the rules are evaluated:

1. A request matching a **denied** path is marked as secured.
2. It is then decided by the first matching **allowed** path. An allowed path
   without an `authorizer` grants anonymous access; one with an authorizer
   delegates the decision to it.
3. A request matching nothing at all is **granted**.

The consequence worth knowing: these rules can require authentication for a
namespace, but they cannot express deny-by-default. A denied path with no
matching allow falls through to granted.

webapps-neo serves no API of its own. The SPA shell and its assets are public by
definition, and all engine access goes through `/engine-rest`, which is guarded
separately — by `ProcessEngineAuthenticationFilter` when
`operaton.bpm.run.auth.enabled` is set, or by the Spring Security OAuth2 filter
chain. So the file keeps exactly one rule: it reserves the engine API namespace
under the application path, so that if webapps-neo ever exposes it, it requires
an authenticated user instead of being open. Everything the legacy webapp needed
for cockpit, tasklist, admin and welcome has been removed, because none of those
paths exist here.
