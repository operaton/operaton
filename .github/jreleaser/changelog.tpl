# About release 1.0.0-beta-5

## New and Noteworthy

The 1.0.0-beta-5 is a continuation of our ongoing modernization efforts.

### Camunda 7 Compatibility

This release is feature complete and compatible with [Camunda 7.23.0](https://docs.camunda.org/enterprise/announcement/#camunda-platform-7-23).

### Code Modernization

We have continued to modernize the code base by removing deprecated code inherited from Camunda 7. This is part of our
ongoing effort to improve the code quality and maintainability of the Operaton project.

The Sonar findings have been further addressed. Compared with 1.0.0-beta-4 the findings have been reduced by 32 %.
Overall since 1.0.0-beta-1 the findings have been reduced by 83 %.
See the [Sonar report](https://sonarcloud.io/summary/overall?id=io.github.operaton%3Aoperaton) for details.

### Deprecations

We have revised deprecated code inherited from Camunda 7. Since we do not want to break clients we have only marked
deprecated methods and classes.

API that was marked deprecated before has been tagged with
```
@Deprecated(since = "1.0")
```

This is because from perspective of the Operaton code base it is deprecated with the first 1.0 release.

API that has a clear replacement has been marked for removal.

```
@Deprecated(since = "1.0", forRemoval = true)
```

It is strongly encouraged to clear usages of such API. We did not remove it yet, but will remove it from a future version.
It will be announced to when exactly removal is planned on a detailed level.

### Wildfly Upgrade

The Wildfly distribution has been upgraded to Wildfly 36.0.1.

## Versions

Operaton uses the following versions of its dependencies:

| Dependency  | Version |
|-------------|---------|
| Java        | 17      |
| Spring Boot | 3.5.4   |
| Spring      | 6.2.9   |
| Wildfly     | 36.0.1  |
| Tomcat      | 11.0.10 |
| Jakarta EE  | 10.0.0  |
| BPMN        | 2.0     |
| DMN         | 1.3     |
| CMMN        | 1.1     |

{{changelogContributors}}

## Changelog

{{changelogChanges}}
