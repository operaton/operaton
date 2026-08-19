# Webapps Neo

The new Operaton web apps.

## Building

The web apps can be build by using Maven:

`mvn clean install`

For development you can use the build options provided by npm and vite.
See [ui/README.md](ui/README.md) for details.

### Going Fast

`mvnd clean install -DskipTests -PskipFrontendBuild`

- `mvnd`: mvn with parrallel threads
- `-DskipTests`: Skip tests
- `-PskipFrontendBuild`: Skip building old web apps

Roughly reduces build times on MacPro with M1 chip from 3 minutes to 1 minute.

## Using the web apps locally

1. Clone [Example Repo](https://github.com/javahippie/operaton-spring-boot-example)
2. Add the version to which you built the application in the `pom.xml`
3. Run `mvn spring-boot:run` in the root of the repository

## Additional Information

The complied contents of the `webapps-neo` package are used as a depenency in
`spring-boot-starter/starter-webapp-neo(-core)`.

There the spring boot applications takes care of providing an URL.

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
