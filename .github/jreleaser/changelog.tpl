# About release 1.0.0-beta-5

## New and Noteworthy

The 1.0.0-beta-5 is a continuation of our ongoing modernization efforts.

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


{{changelogContributors}}

## Changelog

{{changelogChanges}}
