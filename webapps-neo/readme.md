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
