# About this release

## New and Noteworthy

{{changelogNewAndNoteworthy}}

### Breaking Changes

- Operaton removed the compatibility layer for Activiti. If you need to use Activiti models you will have to convert them (see the following [blog post](https://camunda.com/blog/2016/10/migrate-from-activiti-to-camunda/) for details).
- The support for the `javax.el` expression language has been removed. Application servers that ship this library are
incompatible with Operaton. If you are running Operaton on an application server, make sure that it supports
`jakarta-el` in version 4.0.0 or newer (e.g. Wildfly 21 or newer).

{{changelogContributors}}

## Changelog

{{changelogChanges}}
