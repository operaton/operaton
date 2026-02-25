# Webapp Integration Tests

This module contains integration tests for the operaton webapps.
Tests are executed with managed arqullian container.
To run tests individually add -Darquillian.launch=%container% to your run configuration.
Currently, you can select between wildfly and tomcat.
You also need to run a build with wished container profile first so that your runtime is available to you

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