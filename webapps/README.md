# Operaton Webapp

This is the Operaton web application source.
Clean, package and install it via [Maven](https://maven.apache.org/).

## Structure of this project

The structure is as follows:

* `assembly` - Java sources and tests for the Operaton web application.
* `frontend` - HTML, CSS and Javascript sources as well as Plugins and tests for the Operaton webapplications Cockpit, Tasklist and Admin.

## FRONTEND

### UI

There are 3 web applications available for the Operaton :

* __cockpit__: an administration interface for processes and decisions
* __tasklist__: provides an interface to process user tasks
* __admin__: is used to administer users, groups and their authorizations

The webapps above are relying on 2 libraries:

* __operaton-bpm-sdk-js__: provides tools for developers who want interact with the platform using Javascript
* __operaton-commons-ui__: is a set of shared scripts, templates and assets, used in the different webapps

#### Plugins

Parts of the web applications can be extended using plugins.

See [plugin development guide](https://docs.operaton.org/latest/real-life/how-to/#cockpit-how-to-develop-a-cockpit-plugin) for details.

#### Translations

English and german translations are located in the `ui/<app>/client/locales` folders.  
Translations for other languages are available in the [operaton-7-webapp-translations](https://github.com/camunda-community-hub/camunda-7-webapp-translations) repository.

### Libraries

#### [operaton-bpm-sdk-js](https://github.com/operaton/operaton/tree/master/webapps/frontend/operaton-bpm-sdk-js)

Has tools to work with the REST API and forms (included transitively via operaton-commons-ui).

#### [operaton-commons-ui](https://github.com/operaton/operaton/tree/master/webapps/frontend/operaton-commons-ui)

Contains resources like images, [`.less`](http://lesscss.org) stylesheets as well as some [angular.js](http://angularjs.org) modules.

### Prerequisite

You need [node.js](http://nodejs.org) >= 17 and npm.

### Setup

#### Adjusting Maven Settings

See https://github.com/operaton/operaton/blob/main/CONTRIBUTING.md#build-from-source

#### Using Webpack

Build the web apps using Webpack:

```sh
# cd <path to your workspace>
git clone git@github.com:operaton/operaton.git
cd operaton/webapps/frontend
npm install
npm start
```

To start the server in development mode, call

```sh
cd operaton/webapps/assembly
mvn jetty:run -Pdevelop
npm run start
```

The webapps are then available pointing a browser at [http://localhost:8080](http://localhost:8080). To login as an admin user, use `jonny1` as User ID and password.

You can now start developing using the `npm run start` command in the frontend directory.

## Browsers support

The supported browsers are:

- Chrome Latest
- Firefox Latest
- Edge Latest

## Contributing

Have a look at our [contribution guide](https://github.com/operaton/operaton/blob/main/CONTRIBUTING.md) for how to contribute to this repository.

## Help and support

* [Documentation](https://docs.operaton.org/manual/latest/)
* [Forum](https://forum.operaton.org)
* [Stackoverflow](https://stackoverflow.com/questions/tagged/operaton)

## License

The source files in this repository are made available under the [Apache License Version 2.0](./LICENSE).
