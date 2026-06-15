# Webapp Integration Tests

This module contains integration tests for the operaton webapps.

Tests are executed with managed arquillian container. To run tests individually add -Darquillian.launch=tomcat/wildfly and -Dtomcat/wildfly.runtime.location to your run configuration.
You can find suitable values it qa/pom.xml.

Important: You a build with wished container must be run first so that server runtime is preconfigured and available to you.

## Running protractor JS Integration Tests

[Basic Getting Started with `protractor`](https://github.com/angular/protractor/blob/master/docs/getting-started.md)


### Initial Setup

```
npm install -g protractor
```

```
webdriver-manager update
```


### Running Test Cases

```
webdriver-manager start
```


### Protractor API

[API](https://github.com/angular/protractor/blob/master/docs/api.md)