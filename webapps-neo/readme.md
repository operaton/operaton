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

## Additional Information

The complied contents of the `webapps-neo` package are used as a depenency in
`spring-boot-starter/starter-webapp-neo(-core)`.

There the spring boot applications takes care of providing an URL.
