# About release 1.0.0-beta-4

## New and Noteworthy

The 1.0.0-beta-4 release marks a major milestone on our road to a modernized code base.

### Drop of Legacy JavaEE Support

With this release, we are excited to announce that we have finally dropped support of legacy JavaEE support and API usages.
We have dropped support for Wildfly 26 and Tomcat 9, which were the last JavaEE based versions. Furthermore, we have
dropped the dependency on Spring 5, which was the last remaining dependency on JavaEE.

The code base has been modernized to use Jakarta EE 10 APIs, which are the latest standards in the Java ecosystem.

We have upgraded all mein dependencies to their latest versions, including Spring 6, SpringBoot 3.4.4 and Jakarta EE 10.

### Dependency Upgrades

The fact that we have dropped support for JavaEE has allowed us to upgrade all dependencies to their latest versions,
and drop dependencies that was still using JavaEE APIs.

We have upgraded the following dependencies:

- SpringBoot 3.4.1 -> 3.4.4


### Distributions

We have optimized the Tomcat distribution in matter of size. The distributions are now smaller: From 130 MB for to Zip
distribution to 81 MB.

The layering of the Operaton docker images have been improved for the Wildfly and Tomcat distributions.
The images are now packaging the application server in a separate layer, which allows for faster builds and smaller images.
Now, the final layer only adds Operaton itself to a more stable layer stack that adds the application server.

As a result, downloading the images for updates are pulling less layers, especially when using nightly distributions.


### Integration tests

We have added integration tests for all supported application servers. The integration tests are run on a nightly basis.

The integration builds are building Operaton against Java 17 and Java 21.

We are especially proud of the fact that we have enabled the integration tests to use Testcontainers for running
all databases besides h2. On nightly basis we are running the integration tests against PostgreSQL, and added support
for MSSQL, MySQL and Oracle.



{{changelogContributors}}

## Changelog

{{changelogChanges}}
