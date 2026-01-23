# Operaton ${project.version} License Book

## Table of contents

1. [Introduction](#introduction)
2. [Product License](#product_license)
3. [Licenses for Third-Party Libraries](#3rd_party_licenses)
   1. [Operaton Third-Party Java Libraries](#mvn_dependencies)
   2. [Operaton Third-Party Node Libraries](#npm_licenses)
4. [License Texts](#licenses)

This is a license book. It contains licensing information for third-party libraries which are used
in this release of Operaton ${project.version}.

_(Document generated on: ${build.date})_

<a name="introduction"></a>
## Introduction
This Licensing Information document is a part of the program documentation and is intended to help you understand the license terms and copyright for third-party libraries associated with the Operaton software.

<a name="product_license"></a>
## Product License - Operaton
This is a distribution of Operaton ${project.version} (visit https://docs.operaton.org/) a Java-based framework.
The software is mainly published under the Apache License 2.0 and other open source licenses.
Which components are published under an open source license is clearly stated in the license header
of a source code file or a LICENSE file present in the root directory of the software code repository.


<a name="3rd_party_licenses"></a>
## Licenses for Third-Party Libraries
The following sections contain licensing information for third-party libraries that we distribute with the Operaton source code and binaries for your convenience. None of the Libraries are modified or changed and they are solely distributed as is. For practicality, the license text is only referenced once. Each library (whether provided in source or object form) is licensed to you by its copyright holders under the original open source license listed in this License Book. Nothing in this License Book or any license agreement we enter into with you removes or restricts any rights you may have in respect of any component under its original open source license, or makes any of the original authors or copyright holders of the component liable to you in respect of your use or distribution of that component.

We are thankful to all individuals that have created these.

The Libraries used within Operaton ${project.version} are published under the following licenses:

* [Apache-2.0](#apache-2.0) 
* [Apache-2.0-with-LLVM-exception](#apache-2.0-with-llvm-exception) 
* [BlueOak-1.0.0](#blueoak-1.0.0) 
* [bpmn.io](#bpmn.io) 
* [BSD-2-Clause](#bsd-2-clause) 
* [BSD-3-Clause](#bsd-3-clause) 
* [CC-BY-2.5](#cc-by-2.5) 
* [CC0-1.0](#cc0-1.0) 
* [CDDL-1.0](#cddl-1.0) 
* [CDDL-1.1](#cddl-1.1) 
* [EDL-1.0](#edl-1.0) 
* [EPL-1.0](#epl-1.0) 
* [EPL-2.0](#epl-2.0) 
* [FABRIC3](#fabric3) 
* [GPL-2.0-with-classpath-exception](#gpl-2.0-with-classpath-exception) 
* [IndianaUniversity-1.1.1](#indianauniversity-1.1.1) 
* [ISC](#isc) 
* [JQUERY](#jquery) 
* [LGPL-2.1-only](#lgpl-2.1-only) 
* [LGPL-2.1-or-later](#lgpl-2.1-or-later) 
* [MIT](#mit) 
* [MIT-0](#mit-0) 
* [MPL-1.1](#mpl-1.1) 
* [MPL-2.0](#mpl-2.0) 
* [OFL-1.1](#ofl-1.1) 
* [Python-2.0](#python-2.0) 
* [Unicode-3.0](#unicode-3.0) 
* [Unicode-DFS-2016](#unicode-dfs-2016) 
* [UPL-1.0](#upl-1.0) 
* [W3C-19990505](#w3c-19990505) 
* [X11](#x11) 

GNU Lesser/Library General Public License LGPL (any version)

Where the Operaton binaries contain components licensed under any version of LGPL, that licence allows you to extract the object code of the LGPL component from the binary as distributed by us (this is normally distributed as a compressed .tar.gz file which contains a number of files, such as .jar files which are themselves archives containing further components, all of which can be extracted using commonly available tools such as gzip). Using the source code of the library (which we will provide on request, or you can download it from the location specified in this license book), you can make modifications or bug fixes to the library, recompile the library (again, using commonly available tools) and replace the binary of the library you extracted with the recompiled object code. Full details of this process are available on request. This will enable you to modify any LGPL-licensed library and re-integrate that modified library into the Operaton binaries (accepting that this will only work provided you have not broken the library or the interface).

<a name="mvn_dependencies"></a>
### Operaton Third-Party Java Libraries

| Library | Version | License(s) |
|---------|---------|------------|
| [org.apache.tomcat:tomcat-servlet-api](https://central.sonatype.com/artifact/org.apache.tomcat/tomcat-servlet-api/11.0.12) | 11.0.12 | (Apache-2.0 AND CDDL-1.0 AND EPL-2.0) |
| [org.glassfish.extras:glassfish-embedded-all](https://central.sonatype.com/artifact/org.glassfish.extras/glassfish-embedded-all/3.1.1) | 3.1.1 | (CDDL-1.0 OR GPL-2.0-with-classpath-exception) |
| [com.tngtech.archunit:archunit](https://central.sonatype.com/artifact/com.tngtech.archunit/archunit/1.4.1) | 1.4.1 | Apache-2.0, BSD-4-Clause |
| [org.jboss.modules:jboss-modules](https://central.sonatype.com/artifact/org.jboss.modules/jboss-modules/2.1.6.Final) | 2.1.6.Final | Apache-2.0, IndianaUniversity-1.1.1 |
| [com.cronutils:cron-utils](https://central.sonatype.com/artifact/com.cronutils/cron-utils/9.2.1) | 9.2.1 | Apache-2.0 |
| [com.ethlo.time:itu](https://central.sonatype.com/artifact/com.ethlo.time/itu/1.14.0) | 1.14.0 | Apache-2.0 |
| [com.fasterxml.jackson.core:jackson-annotations](https://central.sonatype.com/artifact/com.fasterxml.jackson.core/jackson-annotations/2.20) | 2.20 | Apache-2.0 |
| [com.fasterxml.jackson.core:jackson-core](https://central.sonatype.com/artifact/com.fasterxml.jackson.core/jackson-core/2.20.2) | 2.20.2 | Apache-2.0 |
| [com.fasterxml.jackson.core:jackson-databind](https://central.sonatype.com/artifact/com.fasterxml.jackson.core/jackson-databind/2.20.2) | 2.20.2 | Apache-2.0 |
| [com.fasterxml.jackson.dataformat:jackson-dataformat-yaml](https://central.sonatype.com/artifact/com.fasterxml.jackson.dataformat/jackson-dataformat-yaml/2.20.2) | 2.20.2 | Apache-2.0 |
| [com.fasterxml.jackson.datatype:jackson-datatype-jdk8](https://central.sonatype.com/artifact/com.fasterxml.jackson.datatype/jackson-datatype-jdk8/2.20.2) | 2.20.2 | Apache-2.0 |
| [com.fasterxml.jackson.datatype:jackson-datatype-joda](https://central.sonatype.com/artifact/com.fasterxml.jackson.datatype/jackson-datatype-joda/2.20.2) | 2.20.2 | Apache-2.0 |
| [com.fasterxml.jackson.datatype:jackson-datatype-jsr310](https://central.sonatype.com/artifact/com.fasterxml.jackson.datatype/jackson-datatype-jsr310/2.20.2) | 2.20.2 | Apache-2.0 |
| [com.fasterxml.jackson.jakarta.rs:jackson-jakarta-rs-base](https://central.sonatype.com/artifact/com.fasterxml.jackson.jakarta.rs/jackson-jakarta-rs-base/2.20.2) | 2.20.2 | Apache-2.0 |
| [com.fasterxml.jackson.jakarta.rs:jackson-jakarta-rs-json-provider](https://central.sonatype.com/artifact/com.fasterxml.jackson.jakarta.rs/jackson-jakarta-rs-json-provider/2.20.2) | 2.20.2 | Apache-2.0 |
| [com.fasterxml.jackson.jaxrs:jackson-jaxrs-base](https://central.sonatype.com/artifact/com.fasterxml.jackson.jaxrs/jackson-jaxrs-base/2.20.2) | 2.20.2 | Apache-2.0 |
| [com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider](https://central.sonatype.com/artifact/com.fasterxml.jackson.jaxrs/jackson-jaxrs-json-provider/2.20.2) | 2.20.2 | Apache-2.0 |
| [com.fasterxml.jackson.module:jackson-module-jakarta-xmlbind-annotations](https://central.sonatype.com/artifact/com.fasterxml.jackson.module/jackson-module-jakarta-xmlbind-annotations/2.20.2) | 2.20.2 | Apache-2.0 |
| [com.fasterxml.jackson.module:jackson-module-jaxb-annotations](https://central.sonatype.com/artifact/com.fasterxml.jackson.module/jackson-module-jaxb-annotations/2.20.2) | 2.20.2 | Apache-2.0 |
| [com.fasterxml.jackson.module:jackson-module-parameter-names](https://central.sonatype.com/artifact/com.fasterxml.jackson.module/jackson-module-parameter-names/2.20.2) | 2.20.2 | Apache-2.0 |
| [com.fasterxml.uuid:java-uuid-generator](https://central.sonatype.com/artifact/com.fasterxml.uuid/java-uuid-generator/5.1.1) | 5.1.1 | Apache-2.0 |
| [com.fasterxml:classmate](https://central.sonatype.com/artifact/com.fasterxml/classmate/1.7.3) | 1.7.3 | Apache-2.0 |
| [com.github.ben-manes.caffeine:caffeine](https://central.sonatype.com/artifact/com.github.ben-manes.caffeine/caffeine/3.2.3) | 3.2.3 | Apache-2.0 |
| [com.github.docker-java:docker-java-api](https://central.sonatype.com/artifact/com.github.docker-java/docker-java-api/3.4.2) | 3.4.2 | Apache-2.0 |
| [com.github.docker-java:docker-java-transport](https://central.sonatype.com/artifact/com.github.docker-java/docker-java-transport/3.4.2) | 3.4.2 | Apache-2.0 |
| [com.github.docker-java:docker-java-transport-zerodep](https://central.sonatype.com/artifact/com.github.docker-java/docker-java-transport-zerodep/3.4.2) | 3.4.2 | Apache-2.0 |
| [com.github.stephenc.jcip:jcip-annotations](https://central.sonatype.com/artifact/com.github.stephenc.jcip/jcip-annotations/1.0-1) | 1.0-1 | Apache-2.0 |
| [com.google.auto.service:auto-service-annotations](https://central.sonatype.com/artifact/com.google.auto.service/auto-service-annotations/1.1.1) | 1.1.1 | Apache-2.0 |
| [com.google.code.gson:gson](https://central.sonatype.com/artifact/com.google.code.gson/gson/2.13.2) | 2.13.2 | Apache-2.0 |
| [com.google.errorprone:error_prone_annotations](https://central.sonatype.com/artifact/com.google.errorprone/error_prone_annotations/2.41.0) | 2.41.0 | Apache-2.0 |
| [com.google.guava:failureaccess](https://central.sonatype.com/artifact/com.google.guava/failureaccess/1.0.2) | 1.0.2 | Apache-2.0 |
| [com.google.guava:guava](https://central.sonatype.com/artifact/com.google.guava/guava/33.2.1-jre) | 33.2.1-jre | Apache-2.0 |
| [com.google.inject:guice](https://central.sonatype.com/artifact/com.google.inject/guice/5.1.0) | 5.1.0 | Apache-2.0 |
| [com.googlecode.javaewah:JavaEWAH](https://central.sonatype.com/artifact/com.googlecode.javaewah/JavaEWAH/1.2.3) | 1.2.3 | Apache-2.0 |
| [com.ibm.async:asyncutil](https://central.sonatype.com/artifact/com.ibm.async/asyncutil/0.1.0) | 0.1.0 | Apache-2.0 |
| [com.jayway.jsonpath:json-path](https://central.sonatype.com/artifact/com.jayway.jsonpath/json-path/2.9.0) | 2.9.0 | Apache-2.0 |
| [com.networknt:json-schema-validator](https://central.sonatype.com/artifact/com.networknt/json-schema-validator/1.5.9) | 1.5.9 | Apache-2.0 |
| [com.nimbusds:content-type](https://central.sonatype.com/artifact/com.nimbusds/content-type/2.2) | 2.2 | Apache-2.0 |
| [com.nimbusds:lang-tag](https://central.sonatype.com/artifact/com.nimbusds/lang-tag/1.7) | 1.7 | Apache-2.0 |
| [com.nimbusds:nimbus-jose-jwt](https://central.sonatype.com/artifact/com.nimbusds/nimbus-jose-jwt/10.5) | 10.5 | Apache-2.0 |
| [com.nimbusds:oauth2-oidc-sdk](https://central.sonatype.com/artifact/com.nimbusds/oauth2-oidc-sdk/9.43.6) | 9.43.6 | Apache-2.0 |
| [com.opencsv:opencsv](https://central.sonatype.com/artifact/com.opencsv/opencsv/5.9) | 5.9 | Apache-2.0 |
| [com.tngtech.archunit:archunit-junit4](https://central.sonatype.com/artifact/com.tngtech.archunit/archunit-junit4/1.4.1) | 1.4.1 | Apache-2.0 |
| [com.zaxxer:HikariCP](https://central.sonatype.com/artifact/com.zaxxer/HikariCP/6.3.3) | 6.3.3 | Apache-2.0 |
| [commons-codec:commons-codec](https://central.sonatype.com/artifact/commons-codec/commons-codec/1.19.0) | 1.19.0 | Apache-2.0 |
| [commons-fileupload:commons-fileupload](https://central.sonatype.com/artifact/commons-fileupload/commons-fileupload/1.6.0) | 1.6.0 | Apache-2.0 |
| [commons-io:commons-io](https://central.sonatype.com/artifact/commons-io/commons-io/2.20.0) | 2.20.0 | Apache-2.0 |
| [commons-logging:commons-logging](https://central.sonatype.com/artifact/commons-logging/commons-logging/1.2) | 1.2 | Apache-2.0 |
| [commons-logging:commons-logging](https://central.sonatype.com/artifact/commons-logging/commons-logging/1.3.5) | 1.3.5 | Apache-2.0 |
| [io.agroal:agroal-api](https://central.sonatype.com/artifact/io.agroal/agroal-api/2.8) | 2.8 | Apache-2.0 |
| [io.agroal:agroal-narayana](https://central.sonatype.com/artifact/io.agroal/agroal-narayana/2.8) | 2.8 | Apache-2.0 |
| [io.agroal:agroal-pool](https://central.sonatype.com/artifact/io.agroal/agroal-pool/2.8) | 2.8 | Apache-2.0 |
| [io.micrometer:micrometer-commons](https://central.sonatype.com/artifact/io.micrometer/micrometer-commons/1.15.8) | 1.15.8 | Apache-2.0 |
| [io.micrometer:micrometer-core](https://central.sonatype.com/artifact/io.micrometer/micrometer-core/1.15.8) | 1.15.8 | Apache-2.0 |
| [io.micrometer:micrometer-jakarta9](https://central.sonatype.com/artifact/io.micrometer/micrometer-jakarta9/1.15.8) | 1.15.8 | Apache-2.0 |
| [io.micrometer:micrometer-observation](https://central.sonatype.com/artifact/io.micrometer/micrometer-observation/1.15.8) | 1.15.8 | Apache-2.0 |
| [io.opentelemetry:opentelemetry-api](https://central.sonatype.com/artifact/io.opentelemetry/opentelemetry-api/1.49.0) | 1.49.0 | Apache-2.0 |
| [io.opentelemetry:opentelemetry-context](https://central.sonatype.com/artifact/io.opentelemetry/opentelemetry-context/1.49.0) | 1.49.0 | Apache-2.0 |
| [io.opentelemetry:opentelemetry-exporter-logging](https://central.sonatype.com/artifact/io.opentelemetry/opentelemetry-exporter-logging/1.49.0) | 1.49.0 | Apache-2.0 |
| [io.opentelemetry:opentelemetry-sdk](https://central.sonatype.com/artifact/io.opentelemetry/opentelemetry-sdk/1.49.0) | 1.49.0 | Apache-2.0 |
| [io.opentelemetry:opentelemetry-sdk-common](https://central.sonatype.com/artifact/io.opentelemetry/opentelemetry-sdk-common/1.49.0) | 1.49.0 | Apache-2.0 |
| [io.opentelemetry:opentelemetry-sdk-extension-autoconfigure](https://central.sonatype.com/artifact/io.opentelemetry/opentelemetry-sdk-extension-autoconfigure/1.49.0) | 1.49.0 | Apache-2.0 |
| [io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi](https://central.sonatype.com/artifact/io.opentelemetry/opentelemetry-sdk-extension-autoconfigure-spi/1.49.0) | 1.49.0 | Apache-2.0 |
| [io.opentelemetry:opentelemetry-sdk-logs](https://central.sonatype.com/artifact/io.opentelemetry/opentelemetry-sdk-logs/1.49.0) | 1.49.0 | Apache-2.0 |
| [io.opentelemetry:opentelemetry-sdk-metrics](https://central.sonatype.com/artifact/io.opentelemetry/opentelemetry-sdk-metrics/1.49.0) | 1.49.0 | Apache-2.0 |
| [io.opentelemetry:opentelemetry-sdk-trace](https://central.sonatype.com/artifact/io.opentelemetry/opentelemetry-sdk-trace/1.49.0) | 1.49.0 | Apache-2.0 |
| [io.quarkus.arc:arc](https://central.sonatype.com/artifact/io.quarkus.arc/arc/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus.arc:arc-processor](https://central.sonatype.com/artifact/io.quarkus.arc/arc-processor/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus.gizmo:gizmo](https://central.sonatype.com/artifact/io.quarkus.gizmo/gizmo/1.9.0) | 1.9.0 | Apache-2.0 |
| [io.quarkus.gizmo:gizmo2](https://central.sonatype.com/artifact/io.quarkus.gizmo/gizmo2/2.0.0.Beta6) | 2.0.0.Beta6 | Apache-2.0 |
| [io.quarkus:quarkus-agroal](https://central.sonatype.com/artifact/io.quarkus/quarkus-agroal/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-agroal-deployment](https://central.sonatype.com/artifact/io.quarkus/quarkus-agroal-deployment/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-agroal-dev](https://central.sonatype.com/artifact/io.quarkus/quarkus-agroal-dev/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-agroal-spi](https://central.sonatype.com/artifact/io.quarkus/quarkus-agroal-spi/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-arc](https://central.sonatype.com/artifact/io.quarkus/quarkus-arc/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-arc-deployment](https://central.sonatype.com/artifact/io.quarkus/quarkus-arc-deployment/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-arc-dev](https://central.sonatype.com/artifact/io.quarkus/quarkus-arc-dev/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-assistant-deployment-spi](https://central.sonatype.com/artifact/io.quarkus/quarkus-assistant-deployment-spi/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-assistant-dev](https://central.sonatype.com/artifact/io.quarkus/quarkus-assistant-dev/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-bootstrap-app-model](https://central.sonatype.com/artifact/io.quarkus/quarkus-bootstrap-app-model/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-bootstrap-core](https://central.sonatype.com/artifact/io.quarkus/quarkus-bootstrap-core/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-bootstrap-runner](https://central.sonatype.com/artifact/io.quarkus/quarkus-bootstrap-runner/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-builder](https://central.sonatype.com/artifact/io.quarkus/quarkus-builder/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-class-change-agent](https://central.sonatype.com/artifact/io.quarkus/quarkus-class-change-agent/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-classloader-commons](https://central.sonatype.com/artifact/io.quarkus/quarkus-classloader-commons/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-core](https://central.sonatype.com/artifact/io.quarkus/quarkus-core/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-core-deployment](https://central.sonatype.com/artifact/io.quarkus/quarkus-core-deployment/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-credentials](https://central.sonatype.com/artifact/io.quarkus/quarkus-credentials/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-credentials-deployment](https://central.sonatype.com/artifact/io.quarkus/quarkus-credentials-deployment/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-datasource](https://central.sonatype.com/artifact/io.quarkus/quarkus-datasource/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-datasource-common](https://central.sonatype.com/artifact/io.quarkus/quarkus-datasource-common/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-datasource-deployment](https://central.sonatype.com/artifact/io.quarkus/quarkus-datasource-deployment/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-datasource-deployment-spi](https://central.sonatype.com/artifact/io.quarkus/quarkus-datasource-deployment-spi/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-datasource-dev](https://central.sonatype.com/artifact/io.quarkus/quarkus-datasource-dev/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-development-mode-spi](https://central.sonatype.com/artifact/io.quarkus/quarkus-development-mode-spi/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-devservices-common](https://central.sonatype.com/artifact/io.quarkus/quarkus-devservices-common/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-devservices-deployment](https://central.sonatype.com/artifact/io.quarkus/quarkus-devservices-deployment/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-devui-deployment-spi](https://central.sonatype.com/artifact/io.quarkus/quarkus-devui-deployment-spi/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-fs-util](https://central.sonatype.com/artifact/io.quarkus/quarkus-fs-util/1.2.0) | 1.2.0 | Apache-2.0 |
| [io.quarkus:quarkus-hibernate-validator-spi](https://central.sonatype.com/artifact/io.quarkus/quarkus-hibernate-validator-spi/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-ide-launcher](https://central.sonatype.com/artifact/io.quarkus/quarkus-ide-launcher/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-junit4-mock](https://central.sonatype.com/artifact/io.quarkus/quarkus-junit4-mock/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-kubernetes-service-binding-spi](https://central.sonatype.com/artifact/io.quarkus/quarkus-kubernetes-service-binding-spi/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-mutiny](https://central.sonatype.com/artifact/io.quarkus/quarkus-mutiny/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-mutiny-deployment](https://central.sonatype.com/artifact/io.quarkus/quarkus-mutiny-deployment/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-narayana-jta](https://central.sonatype.com/artifact/io.quarkus/quarkus-narayana-jta/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-narayana-jta-deployment](https://central.sonatype.com/artifact/io.quarkus/quarkus-narayana-jta-deployment/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-smallrye-context-propagation](https://central.sonatype.com/artifact/io.quarkus/quarkus-smallrye-context-propagation/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-smallrye-context-propagation-deployment](https://central.sonatype.com/artifact/io.quarkus/quarkus-smallrye-context-propagation-deployment/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-smallrye-context-propagation-spi](https://central.sonatype.com/artifact/io.quarkus/quarkus-smallrye-context-propagation-spi/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-smallrye-health-spi](https://central.sonatype.com/artifact/io.quarkus/quarkus-smallrye-health-spi/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.quarkus:quarkus-transaction-annotations](https://central.sonatype.com/artifact/io.quarkus/quarkus-transaction-annotations/3.28.5) | 3.28.5 | Apache-2.0 |
| [io.reactivex.rxjava3:rxjava](https://central.sonatype.com/artifact/io.reactivex.rxjava3/rxjava/3.1.12) | 3.1.12 | Apache-2.0 |
| [io.smallrye.common:smallrye-common-annotation](https://central.sonatype.com/artifact/io.smallrye.common/smallrye-common-annotation/2.13.9) | 2.13.9 | Apache-2.0 |
| [io.smallrye.common:smallrye-common-classloader](https://central.sonatype.com/artifact/io.smallrye.common/smallrye-common-classloader/2.13.9) | 2.13.9 | Apache-2.0 |
| [io.smallrye.common:smallrye-common-constraint](https://central.sonatype.com/artifact/io.smallrye.common/smallrye-common-constraint/2.13.9) | 2.13.9 | Apache-2.0 |
| [io.smallrye.common:smallrye-common-constraint](https://central.sonatype.com/artifact/io.smallrye.common/smallrye-common-constraint/2.12.0) | 2.12.0 | Apache-2.0 |
| [io.smallrye.common:smallrye-common-cpu](https://central.sonatype.com/artifact/io.smallrye.common/smallrye-common-cpu/2.13.9) | 2.13.9 | Apache-2.0 |
| [io.smallrye.common:smallrye-common-cpu](https://central.sonatype.com/artifact/io.smallrye.common/smallrye-common-cpu/2.12.0) | 2.12.0 | Apache-2.0 |
| [io.smallrye.common:smallrye-common-expression](https://central.sonatype.com/artifact/io.smallrye.common/smallrye-common-expression/2.13.9) | 2.13.9 | Apache-2.0 |
| [io.smallrye.common:smallrye-common-function](https://central.sonatype.com/artifact/io.smallrye.common/smallrye-common-function/2.13.9) | 2.13.9 | Apache-2.0 |
| [io.smallrye.common:smallrye-common-function](https://central.sonatype.com/artifact/io.smallrye.common/smallrye-common-function/2.12.0) | 2.12.0 | Apache-2.0 |
| [io.smallrye.common:smallrye-common-io](https://central.sonatype.com/artifact/io.smallrye.common/smallrye-common-io/2.13.9) | 2.13.9 | Apache-2.0 |
| [io.smallrye.common:smallrye-common-net](https://central.sonatype.com/artifact/io.smallrye.common/smallrye-common-net/2.13.9) | 2.13.9 | Apache-2.0 |
| [io.smallrye.common:smallrye-common-os](https://central.sonatype.com/artifact/io.smallrye.common/smallrye-common-os/2.13.9) | 2.13.9 | Apache-2.0 |
| [io.smallrye.common:smallrye-common-process](https://central.sonatype.com/artifact/io.smallrye.common/smallrye-common-process/2.13.9) | 2.13.9 | Apache-2.0 |
| [io.smallrye.common:smallrye-common-ref](https://central.sonatype.com/artifact/io.smallrye.common/smallrye-common-ref/2.13.9) | 2.13.9 | Apache-2.0 |
| [io.smallrye.common:smallrye-common-resource](https://central.sonatype.com/artifact/io.smallrye.common/smallrye-common-resource/2.13.9) | 2.13.9 | Apache-2.0 |
| [io.smallrye.config:smallrye-config](https://central.sonatype.com/artifact/io.smallrye.config/smallrye-config/3.13.4) | 3.13.4 | Apache-2.0 |
| [io.smallrye.config:smallrye-config-common](https://central.sonatype.com/artifact/io.smallrye.config/smallrye-config-common/3.13.4) | 3.13.4 | Apache-2.0 |
| [io.smallrye.config:smallrye-config-core](https://central.sonatype.com/artifact/io.smallrye.config/smallrye-config-core/3.13.4) | 3.13.4 | Apache-2.0 |
| [io.smallrye.reactive:mutiny](https://central.sonatype.com/artifact/io.smallrye.reactive/mutiny/2.9.4) | 2.9.4 | Apache-2.0 |
| [io.smallrye.reactive:mutiny-smallrye-context-propagation](https://central.sonatype.com/artifact/io.smallrye.reactive/mutiny-smallrye-context-propagation/2.9.4) | 2.9.4 | Apache-2.0 |
| [io.smallrye.reactive:mutiny-zero-flow-adapters](https://central.sonatype.com/artifact/io.smallrye.reactive/mutiny-zero-flow-adapters/1.1.1) | 1.1.1 | Apache-2.0 |
| [io.smallrye.reactive:smallrye-reactive-converter-api](https://central.sonatype.com/artifact/io.smallrye.reactive/smallrye-reactive-converter-api/3.0.3) | 3.0.3 | Apache-2.0 |
| [io.smallrye.reactive:smallrye-reactive-converter-mutiny](https://central.sonatype.com/artifact/io.smallrye.reactive/smallrye-reactive-converter-mutiny/3.0.3) | 3.0.3 | Apache-2.0 |
| [io.smallrye:jandex](https://central.sonatype.com/artifact/io.smallrye/jandex/3.2.0) | 3.2.0 | Apache-2.0 |
| [io.smallrye:jandex](https://central.sonatype.com/artifact/io.smallrye/jandex/3.5.0) | 3.5.0 | Apache-2.0 |
| [io.smallrye:jandex](https://central.sonatype.com/artifact/io.smallrye/jandex/3.3.2) | 3.3.2 | Apache-2.0 |
| [io.smallrye:jandex-gizmo2](https://central.sonatype.com/artifact/io.smallrye/jandex-gizmo2/3.5.0) | 3.5.0 | Apache-2.0 |
| [io.smallrye:smallrye-context-propagation](https://central.sonatype.com/artifact/io.smallrye/smallrye-context-propagation/2.2.1) | 2.2.1 | Apache-2.0 |
| [io.smallrye:smallrye-context-propagation-api](https://central.sonatype.com/artifact/io.smallrye/smallrye-context-propagation-api/2.2.1) | 2.2.1 | Apache-2.0 |
| [io.smallrye:smallrye-context-propagation-jta](https://central.sonatype.com/artifact/io.smallrye/smallrye-context-propagation-jta/2.2.1) | 2.2.1 | Apache-2.0 |
| [io.smallrye:smallrye-context-propagation-storage](https://central.sonatype.com/artifact/io.smallrye/smallrye-context-propagation-storage/2.2.1) | 2.2.1 | Apache-2.0 |
| [io.undertow:undertow-core](https://central.sonatype.com/artifact/io.undertow/undertow-core/2.3.22.Final) | 2.3.22.Final | Apache-2.0 |
| [jakarta.enterprise:jakarta.enterprise.cdi-api](https://central.sonatype.com/artifact/jakarta.enterprise/jakarta.enterprise.cdi-api/4.0.1) | 4.0.1 | Apache-2.0 |
| [jakarta.enterprise:jakarta.enterprise.cdi-api](https://central.sonatype.com/artifact/jakarta.enterprise/jakarta.enterprise.cdi-api/4.1.0) | 4.1.0 | Apache-2.0 |
| [jakarta.enterprise:jakarta.enterprise.lang-model](https://central.sonatype.com/artifact/jakarta.enterprise/jakarta.enterprise.lang-model/4.0.1) | 4.0.1 | Apache-2.0 |
| [jakarta.inject:jakarta.inject-api](https://central.sonatype.com/artifact/jakarta.inject/jakarta.inject-api/2.0.1) | 2.0.1 | Apache-2.0 |
| [jakarta.validation:jakarta.validation-api](https://central.sonatype.com/artifact/jakarta.validation/jakarta.validation-api/3.0.2) | 3.0.2 | Apache-2.0 |
| [javax.inject:javax.inject](https://central.sonatype.com/artifact/javax.inject/javax.inject/1) | 1 | Apache-2.0 |
| [joda-time:joda-time](https://central.sonatype.com/artifact/joda-time/joda-time/2.12.7) | 2.12.7 | Apache-2.0 |
| [net.bytebuddy:byte-buddy](https://central.sonatype.com/artifact/net.bytebuddy/byte-buddy/1.17.8) | 1.17.8 | Apache-2.0 |
| [net.bytebuddy:byte-buddy](https://central.sonatype.com/artifact/net.bytebuddy/byte-buddy/1.17.6) | 1.17.6 | Apache-2.0 |
| [net.bytebuddy:byte-buddy-agent](https://central.sonatype.com/artifact/net.bytebuddy/byte-buddy-agent/1.17.8) | 1.17.8 | Apache-2.0 |
| [net.minidev:accessors-smart](https://central.sonatype.com/artifact/net.minidev/accessors-smart/2.5.2) | 2.5.2 | Apache-2.0 |
| [net.minidev:accessors-smart](https://central.sonatype.com/artifact/net.minidev/accessors-smart/2.6.0) | 2.6.0 | Apache-2.0 |
| [net.minidev:json-smart](https://central.sonatype.com/artifact/net.minidev/json-smart/2.5.2) | 2.5.2 | Apache-2.0 |
| [net.minidev:json-smart](https://central.sonatype.com/artifact/net.minidev/json-smart/2.6.0) | 2.6.0 | Apache-2.0 |
| [org.aesh:aesh](https://central.sonatype.com/artifact/org.aesh/aesh/2.8.2) | 2.8.2 | Apache-2.0 |
| [org.aesh:readline](https://central.sonatype.com/artifact/org.aesh/readline/2.6) | 2.6 | Apache-2.0 |
| [org.apache.ant:ant](https://central.sonatype.com/artifact/org.apache.ant/ant/1.10.15) | 1.10.15 | Apache-2.0 |
| [org.apache.ant:ant-launcher](https://central.sonatype.com/artifact/org.apache.ant/ant-launcher/1.10.15) | 1.10.15 | Apache-2.0 |
| [org.apache.commons:commons-collections4](https://central.sonatype.com/artifact/org.apache.commons/commons-collections4/4.4) | 4.4 | Apache-2.0 |
| [org.apache.commons:commons-compress](https://central.sonatype.com/artifact/org.apache.commons/commons-compress/1.27.1) | 1.27.1 | Apache-2.0 |
| [org.apache.commons:commons-email2-core](https://central.sonatype.com/artifact/org.apache.commons/commons-email2-core/2.0.0-M1) | 2.0.0-M1 | Apache-2.0 |
| [org.apache.commons:commons-email2-jakarta](https://central.sonatype.com/artifact/org.apache.commons/commons-email2-jakarta/2.0.0-M1) | 2.0.0-M1 | Apache-2.0 |
| [org.apache.commons:commons-exec](https://central.sonatype.com/artifact/org.apache.commons/commons-exec/1.4.0) | 1.4.0 | Apache-2.0 |
| [org.apache.commons:commons-lang3](https://central.sonatype.com/artifact/org.apache.commons/commons-lang3/3.19.0) | 3.19.0 | Apache-2.0 |
| [org.apache.commons:commons-text](https://central.sonatype.com/artifact/org.apache.commons/commons-text/1.13.0) | 1.13.0 | Apache-2.0 |
| [org.apache.groovy:groovy](https://central.sonatype.com/artifact/org.apache.groovy/groovy/4.0.29) | 4.0.29 | Apache-2.0 |
| [org.apache.groovy:groovy](https://central.sonatype.com/artifact/org.apache.groovy/groovy/5.0.4) | 5.0.4 | Apache-2.0 |
| [org.apache.groovy:groovy-datetime](https://central.sonatype.com/artifact/org.apache.groovy/groovy-datetime/4.0.29) | 4.0.29 | Apache-2.0 |
| [org.apache.groovy:groovy-datetime](https://central.sonatype.com/artifact/org.apache.groovy/groovy-datetime/5.0.4) | 5.0.4 | Apache-2.0 |
| [org.apache.groovy:groovy-dateutil](https://central.sonatype.com/artifact/org.apache.groovy/groovy-dateutil/4.0.29) | 4.0.29 | Apache-2.0 |
| [org.apache.groovy:groovy-dateutil](https://central.sonatype.com/artifact/org.apache.groovy/groovy-dateutil/5.0.4) | 5.0.4 | Apache-2.0 |
| [org.apache.groovy:groovy-json](https://central.sonatype.com/artifact/org.apache.groovy/groovy-json/5.0.4) | 5.0.4 | Apache-2.0 |
| [org.apache.groovy:groovy-json](https://central.sonatype.com/artifact/org.apache.groovy/groovy-json/4.0.29) | 4.0.29 | Apache-2.0 |
| [org.apache.groovy:groovy-jsr223](https://central.sonatype.com/artifact/org.apache.groovy/groovy-jsr223/4.0.29) | 4.0.29 | Apache-2.0 |
| [org.apache.groovy:groovy-jsr223](https://central.sonatype.com/artifact/org.apache.groovy/groovy-jsr223/5.0.4) | 5.0.4 | Apache-2.0 |
| [org.apache.groovy:groovy-templates](https://central.sonatype.com/artifact/org.apache.groovy/groovy-templates/4.0.29) | 4.0.29 | Apache-2.0 |
| [org.apache.groovy:groovy-templates](https://central.sonatype.com/artifact/org.apache.groovy/groovy-templates/5.0.4) | 5.0.4 | Apache-2.0 |
| [org.apache.groovy:groovy-xml](https://central.sonatype.com/artifact/org.apache.groovy/groovy-xml/5.0.4) | 5.0.4 | Apache-2.0 |
| [org.apache.groovy:groovy-xml](https://central.sonatype.com/artifact/org.apache.groovy/groovy-xml/4.0.29) | 4.0.29 | Apache-2.0 |
| [org.apache.httpcomponents.client5:httpclient5](https://central.sonatype.com/artifact/org.apache.httpcomponents.client5/httpclient5/5.5.1) | 5.5.1 | Apache-2.0 |
| [org.apache.httpcomponents.core5:httpcore5](https://central.sonatype.com/artifact/org.apache.httpcomponents.core5/httpcore5/5.3.6) | 5.3.6 | Apache-2.0 |
| [org.apache.httpcomponents.core5:httpcore5-h2](https://central.sonatype.com/artifact/org.apache.httpcomponents.core5/httpcore5-h2/5.3.6) | 5.3.6 | Apache-2.0 |
| [org.apache.httpcomponents:httpasyncclient](https://central.sonatype.com/artifact/org.apache.httpcomponents/httpasyncclient/4.1.5) | 4.1.5 | Apache-2.0 |
| [org.apache.httpcomponents:httpclient](https://central.sonatype.com/artifact/org.apache.httpcomponents/httpclient/4.5.14) | 4.5.14 | Apache-2.0 |
| [org.apache.httpcomponents:httpcore](https://central.sonatype.com/artifact/org.apache.httpcomponents/httpcore/4.4.16) | 4.4.16 | Apache-2.0 |
| [org.apache.httpcomponents:httpcore-nio](https://central.sonatype.com/artifact/org.apache.httpcomponents/httpcore-nio/4.4.16) | 4.4.16 | Apache-2.0 |
| [org.apache.httpcomponents:httpmime](https://central.sonatype.com/artifact/org.apache.httpcomponents/httpmime/4.5.13) | 4.5.13 | Apache-2.0 |
| [org.apache.logging.log4j:log4j-api](https://central.sonatype.com/artifact/org.apache.logging.log4j/log4j-api/2.24.3) | 2.24.3 | Apache-2.0 |
| [org.apache.logging.log4j:log4j-to-slf4j](https://central.sonatype.com/artifact/org.apache.logging.log4j/log4j-to-slf4j/2.24.3) | 2.24.3 | Apache-2.0 |
| [org.apache.maven.resolver:maven-resolver-api](https://central.sonatype.com/artifact/org.apache.maven.resolver/maven-resolver-api/1.9.22) | 1.9.22 | Apache-2.0 |
| [org.apache.maven.resolver:maven-resolver-connector-basic](https://central.sonatype.com/artifact/org.apache.maven.resolver/maven-resolver-connector-basic/1.9.22) | 1.9.22 | Apache-2.0 |
| [org.apache.maven.resolver:maven-resolver-impl](https://central.sonatype.com/artifact/org.apache.maven.resolver/maven-resolver-impl/1.9.22) | 1.9.22 | Apache-2.0 |
| [org.apache.maven.resolver:maven-resolver-named-locks](https://central.sonatype.com/artifact/org.apache.maven.resolver/maven-resolver-named-locks/1.9.22) | 1.9.22 | Apache-2.0 |
| [org.apache.maven.resolver:maven-resolver-spi](https://central.sonatype.com/artifact/org.apache.maven.resolver/maven-resolver-spi/1.9.22) | 1.9.22 | Apache-2.0 |
| [org.apache.maven.resolver:maven-resolver-supplier](https://central.sonatype.com/artifact/org.apache.maven.resolver/maven-resolver-supplier/1.9.22) | 1.9.22 | Apache-2.0 |
| [org.apache.maven.resolver:maven-resolver-transport-file](https://central.sonatype.com/artifact/org.apache.maven.resolver/maven-resolver-transport-file/1.9.22) | 1.9.22 | Apache-2.0 |
| [org.apache.maven.resolver:maven-resolver-transport-http](https://central.sonatype.com/artifact/org.apache.maven.resolver/maven-resolver-transport-http/1.9.22) | 1.9.22 | Apache-2.0 |
| [org.apache.maven.resolver:maven-resolver-util](https://central.sonatype.com/artifact/org.apache.maven.resolver/maven-resolver-util/1.9.22) | 1.9.22 | Apache-2.0 |
| [org.apache.maven.shared:maven-invoker](https://central.sonatype.com/artifact/org.apache.maven.shared/maven-invoker/3.3.0) | 3.3.0 | Apache-2.0 |
| [org.apache.maven.shared:maven-shared-utils](https://central.sonatype.com/artifact/org.apache.maven.shared/maven-shared-utils/3.4.2) | 3.4.2 | Apache-2.0 |
| [org.apache.maven:maven-artifact](https://central.sonatype.com/artifact/org.apache.maven/maven-artifact/3.9.9) | 3.9.9 | Apache-2.0 |
| [org.apache.maven:maven-builder-support](https://central.sonatype.com/artifact/org.apache.maven/maven-builder-support/3.9.9) | 3.9.9 | Apache-2.0 |
| [org.apache.maven:maven-model](https://central.sonatype.com/artifact/org.apache.maven/maven-model/3.9.9) | 3.9.9 | Apache-2.0 |
| [org.apache.maven:maven-model-builder](https://central.sonatype.com/artifact/org.apache.maven/maven-model-builder/3.9.9) | 3.9.9 | Apache-2.0 |
| [org.apache.maven:maven-repository-metadata](https://central.sonatype.com/artifact/org.apache.maven/maven-repository-metadata/3.9.9) | 3.9.9 | Apache-2.0 |
| [org.apache.maven:maven-resolver-provider](https://central.sonatype.com/artifact/org.apache.maven/maven-resolver-provider/3.9.9) | 3.9.9 | Apache-2.0 |
| [org.apache.maven:maven-settings](https://central.sonatype.com/artifact/org.apache.maven/maven-settings/3.9.9) | 3.9.9 | Apache-2.0 |
| [org.apache.maven:maven-settings-builder](https://central.sonatype.com/artifact/org.apache.maven/maven-settings-builder/3.9.9) | 3.9.9 | Apache-2.0 |
| [org.apache.sshd:sshd-common](https://central.sonatype.com/artifact/org.apache.sshd/sshd-common/2.15.0) | 2.15.0 | Apache-2.0 |
| [org.apache.sshd:sshd-core](https://central.sonatype.com/artifact/org.apache.sshd/sshd-core/2.15.0) | 2.15.0 | Apache-2.0 |
| [org.apache.tomcat.embed:tomcat-embed-core](https://central.sonatype.com/artifact/org.apache.tomcat.embed/tomcat-embed-core/10.1.50) | 10.1.50 | Apache-2.0 |
| [org.apache.tomcat.embed:tomcat-embed-el](https://central.sonatype.com/artifact/org.apache.tomcat.embed/tomcat-embed-el/10.1.50) | 10.1.50 | Apache-2.0 |
| [org.apache.tomcat.embed:tomcat-embed-websocket](https://central.sonatype.com/artifact/org.apache.tomcat.embed/tomcat-embed-websocket/10.1.50) | 10.1.50 | Apache-2.0 |
| [org.apache.tomcat:tomcat](https://central.sonatype.com/artifact/org.apache.tomcat/tomcat/11.0.12) | 11.0.12 | Apache-2.0 |
| [org.apache.tomcat:tomcat-annotations-api](https://central.sonatype.com/artifact/org.apache.tomcat/tomcat-annotations-api/10.1.50) | 10.1.50 | Apache-2.0 |
| [org.apache.tomcat:tomcat-api](https://central.sonatype.com/artifact/org.apache.tomcat/tomcat-api/11.0.12) | 11.0.12 | Apache-2.0 |
| [org.apache.tomcat:tomcat-catalina](https://central.sonatype.com/artifact/org.apache.tomcat/tomcat-catalina/11.0.12) | 11.0.12 | Apache-2.0 |
| [org.apache.tomcat:tomcat-coyote](https://central.sonatype.com/artifact/org.apache.tomcat/tomcat-coyote/11.0.12) | 11.0.12 | Apache-2.0 |
| [org.apache.tomcat:tomcat-el-api](https://central.sonatype.com/artifact/org.apache.tomcat/tomcat-el-api/10.1.50) | 10.1.50 | Apache-2.0 |
| [org.apache.tomcat:tomcat-jaspic-api](https://central.sonatype.com/artifact/org.apache.tomcat/tomcat-jaspic-api/11.0.12) | 11.0.12 | Apache-2.0 |
| [org.apache.tomcat:tomcat-jdbc](https://central.sonatype.com/artifact/org.apache.tomcat/tomcat-jdbc/11.0.12) | 11.0.12 | Apache-2.0 |
| [org.apache.tomcat:tomcat-jni](https://central.sonatype.com/artifact/org.apache.tomcat/tomcat-jni/11.0.12) | 11.0.12 | Apache-2.0 |
| [org.apache.tomcat:tomcat-jsp-api](https://central.sonatype.com/artifact/org.apache.tomcat/tomcat-jsp-api/10.1.50) | 10.1.50 | Apache-2.0 |
| [org.apache.tomcat:tomcat-juli](https://central.sonatype.com/artifact/org.apache.tomcat/tomcat-juli/11.0.12) | 11.0.12 | Apache-2.0 |
| [org.apache.tomcat:tomcat-util](https://central.sonatype.com/artifact/org.apache.tomcat/tomcat-util/11.0.12) | 11.0.12 | Apache-2.0 |
| [org.apache.tomcat:tomcat-util-scan](https://central.sonatype.com/artifact/org.apache.tomcat/tomcat-util-scan/11.0.12) | 11.0.12 | Apache-2.0 |
| [org.apiguardian:apiguardian-api](https://central.sonatype.com/artifact/org.apiguardian/apiguardian-api/1.1.2) | 1.1.2 | Apache-2.0 |
| [org.assertj:assertj-core](https://central.sonatype.com/artifact/org.assertj/assertj-core/3.27.6) | 3.27.6 | Apache-2.0 |
| [org.awaitility:awaitility](https://central.sonatype.com/artifact/org.awaitility/awaitility/4.3.0) | 4.3.0 | Apache-2.0 |
| [org.camunda.feel:feel-engine](https://central.sonatype.com/artifact/org.camunda.feel/feel-engine/1.19.3) | 1.19.3 | Apache-2.0 |
| [org.codehaus.plexus:plexus-cipher](https://central.sonatype.com/artifact/org.codehaus.plexus/plexus-cipher/2.0) | 2.0 | Apache-2.0 |
| [org.codehaus.plexus:plexus-classworlds](https://central.sonatype.com/artifact/org.codehaus.plexus/plexus-classworlds/2.8.0) | 2.8.0 | Apache-2.0 |
| [org.codehaus.plexus:plexus-compiler-api](https://central.sonatype.com/artifact/org.codehaus.plexus/plexus-compiler-api/2.15.0) | 2.15.0 | Apache-2.0 |
| [org.codehaus.plexus:plexus-compiler-javac](https://central.sonatype.com/artifact/org.codehaus.plexus/plexus-compiler-javac/2.15.0) | 2.15.0 | Apache-2.0 |
| [org.codehaus.plexus:plexus-component-annotations](https://central.sonatype.com/artifact/org.codehaus.plexus/plexus-component-annotations/2.1.0) | 2.1.0 | Apache-2.0 |
| [org.codehaus.plexus:plexus-interpolation](https://central.sonatype.com/artifact/org.codehaus.plexus/plexus-interpolation/1.27) | 1.27 | Apache-2.0 |
| [org.codehaus.plexus:plexus-sec-dispatcher](https://central.sonatype.com/artifact/org.codehaus.plexus/plexus-sec-dispatcher/2.0) | 2.0 | Apache-2.0 |
| [org.codehaus.plexus:plexus-utils](https://central.sonatype.com/artifact/org.codehaus.plexus/plexus-utils/3.5.1) | 3.5.1 | Apache-2.0 |
| [org.codehaus.plexus:plexus-xml](https://central.sonatype.com/artifact/org.codehaus.plexus/plexus-xml/3.0.1) | 3.0.1 | Apache-2.0 |
| [org.eclipse.microprofile.config:microprofile-config-api](https://central.sonatype.com/artifact/org.eclipse.microprofile.config/microprofile-config-api/3.1) | 3.1 | Apache-2.0 |
| [org.eclipse.microprofile.context-propagation:microprofile-context-propagation-api](https://central.sonatype.com/artifact/org.eclipse.microprofile.context-propagation/microprofile-context-propagation-api/1.3) | 1.3 | Apache-2.0 |
| [org.eclipse.microprofile.reactive-streams-operators:microprofile-reactive-streams-operators-api](https://central.sonatype.com/artifact/org.eclipse.microprofile.reactive-streams-operators/microprofile-reactive-streams-operators-api/3.0.1) | 3.0.1 | Apache-2.0 |
| [org.freemarker:freemarker](https://central.sonatype.com/artifact/org.freemarker/freemarker/2.3.34) | 2.3.34 | Apache-2.0 |
| [org.fusesource.jansi:jansi](https://central.sonatype.com/artifact/org.fusesource.jansi/jansi/2.4.1) | 2.4.1 | Apache-2.0 |
| [org.hibernate.common:hibernate-commons-annotations](https://central.sonatype.com/artifact/org.hibernate.common/hibernate-commons-annotations/7.0.3.Final) | 7.0.3.Final | Apache-2.0 |
| [org.hibernate.validator:hibernate-validator](https://central.sonatype.com/artifact/org.hibernate.validator/hibernate-validator/8.0.3.Final) | 8.0.3.Final | Apache-2.0 |
| [org.hibernate.validator:hibernate-validator-cdi](https://central.sonatype.com/artifact/org.hibernate.validator/hibernate-validator-cdi/8.0.2.Final) | 8.0.2.Final | Apache-2.0 |
| [org.infinispan.protostream:protostream](https://central.sonatype.com/artifact/org.infinispan.protostream/protostream/5.0.13.Final) | 5.0.13.Final | Apache-2.0 |
| [org.infinispan.protostream:protostream-processor](https://central.sonatype.com/artifact/org.infinispan.protostream/protostream-processor/5.0.13.Final) | 5.0.13.Final | Apache-2.0 |
| [org.infinispan.protostream:protostream-types](https://central.sonatype.com/artifact/org.infinispan.protostream/protostream-types/5.0.13.Final) | 5.0.13.Final | Apache-2.0 |
| [org.infinispan:infinispan-commons](https://central.sonatype.com/artifact/org.infinispan/infinispan-commons/15.2.6.Final) | 15.2.6.Final | Apache-2.0 |
| [org.infinispan:infinispan-commons-spi](https://central.sonatype.com/artifact/org.infinispan/infinispan-commons-spi/15.2.6.Final) | 15.2.6.Final | Apache-2.0 |
| [org.infinispan:infinispan-core](https://central.sonatype.com/artifact/org.infinispan/infinispan-core/15.2.6.Final) | 15.2.6.Final | Apache-2.0 |
| [org.infinispan:infinispan-counter-api](https://central.sonatype.com/artifact/org.infinispan/infinispan-counter-api/15.2.6.Final) | 15.2.6.Final | Apache-2.0 |
| [org.jboss.arquillian.config:arquillian-config-api](https://central.sonatype.com/artifact/org.jboss.arquillian.config/arquillian-config-api/1.10.0.Final) | 1.10.0.Final | Apache-2.0 |
| [org.jboss.arquillian.config:arquillian-config-impl-base](https://central.sonatype.com/artifact/org.jboss.arquillian.config/arquillian-config-impl-base/1.10.0.Final) | 1.10.0.Final | Apache-2.0 |
| [org.jboss.arquillian.config:arquillian-config-spi](https://central.sonatype.com/artifact/org.jboss.arquillian.config/arquillian-config-spi/1.10.0.Final) | 1.10.0.Final | Apache-2.0 |
| [org.jboss.arquillian.container:arquillian-container-impl-base](https://central.sonatype.com/artifact/org.jboss.arquillian.container/arquillian-container-impl-base/1.10.0.Final) | 1.10.0.Final | Apache-2.0 |
| [org.jboss.arquillian.container:arquillian-container-spi](https://central.sonatype.com/artifact/org.jboss.arquillian.container/arquillian-container-spi/1.10.0.Final) | 1.10.0.Final | Apache-2.0 |
| [org.jboss.arquillian.container:arquillian-container-test-api](https://central.sonatype.com/artifact/org.jboss.arquillian.container/arquillian-container-test-api/1.10.0.Final) | 1.10.0.Final | Apache-2.0 |
| [org.jboss.arquillian.container:arquillian-container-test-impl-base](https://central.sonatype.com/artifact/org.jboss.arquillian.container/arquillian-container-test-impl-base/1.10.0.Final) | 1.10.0.Final | Apache-2.0 |
| [org.jboss.arquillian.container:arquillian-container-test-spi](https://central.sonatype.com/artifact/org.jboss.arquillian.container/arquillian-container-test-spi/1.10.0.Final) | 1.10.0.Final | Apache-2.0 |
| [org.jboss.arquillian.core:arquillian-core-api](https://central.sonatype.com/artifact/org.jboss.arquillian.core/arquillian-core-api/1.10.0.Final) | 1.10.0.Final | Apache-2.0 |
| [org.jboss.arquillian.core:arquillian-core-impl-base](https://central.sonatype.com/artifact/org.jboss.arquillian.core/arquillian-core-impl-base/1.10.0.Final) | 1.10.0.Final | Apache-2.0 |
| [org.jboss.arquillian.core:arquillian-core-spi](https://central.sonatype.com/artifact/org.jboss.arquillian.core/arquillian-core-spi/1.10.0.Final) | 1.10.0.Final | Apache-2.0 |
| [org.jboss.arquillian.junit5:arquillian-junit5-container](https://central.sonatype.com/artifact/org.jboss.arquillian.junit5/arquillian-junit5-container/1.10.0.Final) | 1.10.0.Final | Apache-2.0 |
| [org.jboss.arquillian.junit5:arquillian-junit5-core](https://central.sonatype.com/artifact/org.jboss.arquillian.junit5/arquillian-junit5-core/1.10.0.Final) | 1.10.0.Final | Apache-2.0 |
| [org.jboss.arquillian.test:arquillian-test-api](https://central.sonatype.com/artifact/org.jboss.arquillian.test/arquillian-test-api/1.10.0.Final) | 1.10.0.Final | Apache-2.0 |
| [org.jboss.arquillian.test:arquillian-test-impl-base](https://central.sonatype.com/artifact/org.jboss.arquillian.test/arquillian-test-impl-base/1.10.0.Final) | 1.10.0.Final | Apache-2.0 |
| [org.jboss.arquillian.test:arquillian-test-spi](https://central.sonatype.com/artifact/org.jboss.arquillian.test/arquillian-test-spi/1.10.0.Final) | 1.10.0.Final | Apache-2.0 |
| [org.jboss.classfilewriter:jboss-classfilewriter](https://central.sonatype.com/artifact/org.jboss.classfilewriter/jboss-classfilewriter/1.3.1.Final) | 1.3.1.Final | Apache-2.0 |
| [org.jboss.ejb3:jboss-ejb3-ext-api](https://central.sonatype.com/artifact/org.jboss.ejb3/jboss-ejb3-ext-api/2.4.0.Final) | 2.4.0.Final | Apache-2.0 |
| [org.jboss.invocation:jboss-invocation](https://central.sonatype.com/artifact/org.jboss.invocation/jboss-invocation/2.0.0.Final) | 2.0.0.Final | Apache-2.0 |
| [org.jboss.invocation:jboss-invocation](https://central.sonatype.com/artifact/org.jboss.invocation/jboss-invocation/2.0.1.Final) | 2.0.1.Final | Apache-2.0 |
| [org.jboss.logging:commons-logging-jboss-logging](https://central.sonatype.com/artifact/org.jboss.logging/commons-logging-jboss-logging/1.0.0.Final) | 1.0.0.Final | Apache-2.0 |
| [org.jboss.logging:jboss-logging](https://central.sonatype.com/artifact/org.jboss.logging/jboss-logging/3.6.1.Final) | 3.6.1.Final | Apache-2.0 |
| [org.jboss.logmanager:jboss-logmanager](https://central.sonatype.com/artifact/org.jboss.logmanager/jboss-logmanager/3.1.2.Final) | 3.1.2.Final | Apache-2.0 |
| [org.jboss.logmanager:jboss-logmanager](https://central.sonatype.com/artifact/org.jboss.logmanager/jboss-logmanager/2.1.19.Final) | 2.1.19.Final | Apache-2.0 |
| [org.jboss.marshalling:jboss-marshalling](https://central.sonatype.com/artifact/org.jboss.marshalling/jboss-marshalling/2.2.3.Final) | 2.2.3.Final | Apache-2.0 |
| [org.jboss.marshalling:jboss-marshalling-river](https://central.sonatype.com/artifact/org.jboss.marshalling/jboss-marshalling-river/2.2.3.Final) | 2.2.3.Final | Apache-2.0 |
| [org.jboss.metadata:jboss-metadata-common](https://central.sonatype.com/artifact/org.jboss.metadata/jboss-metadata-common/16.1.0.Final) | 16.1.0.Final | Apache-2.0 |
| [org.jboss.metadata:jboss-metadata-ear](https://central.sonatype.com/artifact/org.jboss.metadata/jboss-metadata-ear/16.1.0.Final) | 16.1.0.Final | Apache-2.0 |
| [org.jboss.metadata:jboss-metadata-ejb](https://central.sonatype.com/artifact/org.jboss.metadata/jboss-metadata-ejb/16.1.0.Final) | 16.1.0.Final | Apache-2.0 |
| [org.jboss.metadata:jboss-metadata-web](https://central.sonatype.com/artifact/org.jboss.metadata/jboss-metadata-web/16.1.0.Final) | 16.1.0.Final | Apache-2.0 |
| [org.jboss.narayana.jta:narayana-jta](https://central.sonatype.com/artifact/org.jboss.narayana.jta/narayana-jta/7.2.2.Final) | 7.2.2.Final | Apache-2.0 |
| [org.jboss.narayana.jts:narayana-jts-integration](https://central.sonatype.com/artifact/org.jboss.narayana.jts/narayana-jts-integration/7.2.2.Final) | 7.2.2.Final | Apache-2.0 |
| [org.jboss.remoting:jboss-remoting](https://central.sonatype.com/artifact/org.jboss.remoting/jboss-remoting/5.0.31.Final) | 5.0.31.Final | Apache-2.0 |
| [org.jboss.resteasy:resteasy-core](https://central.sonatype.com/artifact/org.jboss.resteasy/resteasy-core/6.2.15.Final) | 6.2.15.Final | Apache-2.0 |
| [org.jboss.resteasy:resteasy-core-spi](https://central.sonatype.com/artifact/org.jboss.resteasy/resteasy-core-spi/6.2.15.Final) | 6.2.15.Final | Apache-2.0 |
| [org.jboss.shrinkwrap.descriptors:shrinkwrap-descriptors-api-base](https://central.sonatype.com/artifact/org.jboss.shrinkwrap.descriptors/shrinkwrap-descriptors-api-base/2.0.0) | 2.0.0 | Apache-2.0 |
| [org.jboss.shrinkwrap.descriptors:shrinkwrap-descriptors-spi](https://central.sonatype.com/artifact/org.jboss.shrinkwrap.descriptors/shrinkwrap-descriptors-spi/2.0.0) | 2.0.0 | Apache-2.0 |
| [org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-api](https://central.sonatype.com/artifact/org.jboss.shrinkwrap.resolver/shrinkwrap-resolver-api/3.3.4) | 3.3.4 | Apache-2.0 |
| [org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-api-maven](https://central.sonatype.com/artifact/org.jboss.shrinkwrap.resolver/shrinkwrap-resolver-api-maven/3.3.4) | 3.3.4 | Apache-2.0 |
| [org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-api-maven-archive](https://central.sonatype.com/artifact/org.jboss.shrinkwrap.resolver/shrinkwrap-resolver-api-maven-archive/3.3.4) | 3.3.4 | Apache-2.0 |
| [org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-api-maven-embedded](https://central.sonatype.com/artifact/org.jboss.shrinkwrap.resolver/shrinkwrap-resolver-api-maven-embedded/3.3.4) | 3.3.4 | Apache-2.0 |
| [org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-depchain](https://central.sonatype.com/artifact/org.jboss.shrinkwrap.resolver/shrinkwrap-resolver-depchain/3.3.4) | 3.3.4 | Apache-2.0 |
| [org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-impl-maven](https://central.sonatype.com/artifact/org.jboss.shrinkwrap.resolver/shrinkwrap-resolver-impl-maven/3.3.4) | 3.3.4 | Apache-2.0 |
| [org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-impl-maven-archive](https://central.sonatype.com/artifact/org.jboss.shrinkwrap.resolver/shrinkwrap-resolver-impl-maven-archive/3.3.4) | 3.3.4 | Apache-2.0 |
| [org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-impl-maven-embedded](https://central.sonatype.com/artifact/org.jboss.shrinkwrap.resolver/shrinkwrap-resolver-impl-maven-embedded/3.3.4) | 3.3.4 | Apache-2.0 |
| [org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-spi](https://central.sonatype.com/artifact/org.jboss.shrinkwrap.resolver/shrinkwrap-resolver-spi/3.3.4) | 3.3.4 | Apache-2.0 |
| [org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-spi-maven](https://central.sonatype.com/artifact/org.jboss.shrinkwrap.resolver/shrinkwrap-resolver-spi-maven/3.3.4) | 3.3.4 | Apache-2.0 |
| [org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-spi-maven-archive](https://central.sonatype.com/artifact/org.jboss.shrinkwrap.resolver/shrinkwrap-resolver-spi-maven-archive/3.3.4) | 3.3.4 | Apache-2.0 |
| [org.jboss.shrinkwrap:shrinkwrap-api](https://central.sonatype.com/artifact/org.jboss.shrinkwrap/shrinkwrap-api/1.2.6) | 1.2.6 | Apache-2.0 |
| [org.jboss.shrinkwrap:shrinkwrap-impl-base](https://central.sonatype.com/artifact/org.jboss.shrinkwrap/shrinkwrap-impl-base/1.2.6) | 1.2.6 | Apache-2.0 |
| [org.jboss.shrinkwrap:shrinkwrap-spi](https://central.sonatype.com/artifact/org.jboss.shrinkwrap/shrinkwrap-spi/1.2.6) | 1.2.6 | Apache-2.0 |
| [org.jboss.slf4j:slf4j-jboss-logmanager](https://central.sonatype.com/artifact/org.jboss.slf4j/slf4j-jboss-logmanager/2.0.0.Final) | 2.0.0.Final | Apache-2.0 |
| [org.jboss.threads:jboss-threads](https://central.sonatype.com/artifact/org.jboss.threads/jboss-threads/3.9.1) | 3.9.1 | Apache-2.0 |
| [org.jboss.xnio:xnio-api](https://central.sonatype.com/artifact/org.jboss.xnio/xnio-api/3.8.16.Final) | 3.8.16.Final | Apache-2.0 |
| [org.jboss.xnio:xnio-nio](https://central.sonatype.com/artifact/org.jboss.xnio/xnio-nio/3.8.16.Final) | 3.8.16.Final | Apache-2.0 |
| [org.jboss:jandex](https://central.sonatype.com/artifact/org.jboss/jandex/2.4.5.Final) | 2.4.5.Final | Apache-2.0 |
| [org.jboss:jboss-dmr](https://central.sonatype.com/artifact/org.jboss/jboss-dmr/1.7.0.Final) | 1.7.0.Final | Apache-2.0 |
| [org.jboss:jboss-ejb-client](https://central.sonatype.com/artifact/org.jboss/jboss-ejb-client/5.0.8.Final) | 5.0.8.Final | Apache-2.0 |
| [org.jboss:jboss-iiop-client](https://central.sonatype.com/artifact/org.jboss/jboss-iiop-client/2.0.1.Final) | 2.0.1.Final | Apache-2.0 |
| [org.jboss:jboss-vfs](https://central.sonatype.com/artifact/org.jboss/jboss-vfs/3.3.2.Final) | 3.3.2.Final | Apache-2.0 |
| [org.jboss:staxmapper](https://central.sonatype.com/artifact/org.jboss/staxmapper/1.5.0.Final) | 1.5.0.Final | Apache-2.0 |
| [org.jctools:jctools-core](https://central.sonatype.com/artifact/org.jctools/jctools-core/4.0.5) | 4.0.5 | Apache-2.0 |
| [org.jetbrains:annotations](https://central.sonatype.com/artifact/org.jetbrains/annotations/17.0.0) | 17.0.0 | Apache-2.0 |
| [org.jetbrains:annotations](https://central.sonatype.com/artifact/org.jetbrains/annotations/26.0.2) | 26.0.2 | Apache-2.0 |
| [org.jgroups:jgroups](https://central.sonatype.com/artifact/org.jgroups/jgroups/5.4.12.Final) | 5.4.12.Final | Apache-2.0 |
| [org.jspecify:jspecify](https://central.sonatype.com/artifact/org.jspecify/jspecify/1.0.0) | 1.0.0 | Apache-2.0 |
| [org.liquibase:liquibase-core](https://central.sonatype.com/artifact/org.liquibase/liquibase-core/4.31.1) | 4.31.1 | Apache-2.0 |
| [org.mybatis:mybatis](https://central.sonatype.com/artifact/org.mybatis/mybatis/3.5.19) | 3.5.19 | Apache-2.0 |
| [org.objenesis:objenesis](https://central.sonatype.com/artifact/org.objenesis/objenesis/3.3) | 3.3 | Apache-2.0 |
| [org.openapitools:jackson-databind-nullable](https://central.sonatype.com/artifact/org.openapitools/jackson-databind-nullable/0.2.8) | 0.2.8 | Apache-2.0 |
| [org.opentest4j:opentest4j](https://central.sonatype.com/artifact/org.opentest4j/opentest4j/1.3.0) | 1.3.0 | Apache-2.0 |
| [org.projectodd.vdx:vdx-core](https://central.sonatype.com/artifact/org.projectodd.vdx/vdx-core/1.1.6) | 1.1.6 | Apache-2.0 |
| [org.projectodd.vdx:vdx-wildfly](https://central.sonatype.com/artifact/org.projectodd.vdx/vdx-wildfly/1.1.6) | 1.1.6 | Apache-2.0 |
| [org.scala-lang:scala-library](https://central.sonatype.com/artifact/org.scala-lang/scala-library/2.13.15) | 2.13.15 | Apache-2.0 |
| [org.seleniumhq.selenium:selenium-api](https://central.sonatype.com/artifact/org.seleniumhq.selenium/selenium-api/4.31.0) | 4.31.0 | Apache-2.0 |
| [org.seleniumhq.selenium:selenium-chrome-driver](https://central.sonatype.com/artifact/org.seleniumhq.selenium/selenium-chrome-driver/4.31.0) | 4.31.0 | Apache-2.0 |
| [org.seleniumhq.selenium:selenium-chromium-driver](https://central.sonatype.com/artifact/org.seleniumhq.selenium/selenium-chromium-driver/4.31.0) | 4.31.0 | Apache-2.0 |
| [org.seleniumhq.selenium:selenium-devtools-v138](https://central.sonatype.com/artifact/org.seleniumhq.selenium/selenium-devtools-v138/4.36.0) | 4.36.0 | Apache-2.0 |
| [org.seleniumhq.selenium:selenium-devtools-v139](https://central.sonatype.com/artifact/org.seleniumhq.selenium/selenium-devtools-v139/4.36.0) | 4.36.0 | Apache-2.0 |
| [org.seleniumhq.selenium:selenium-devtools-v140](https://central.sonatype.com/artifact/org.seleniumhq.selenium/selenium-devtools-v140/4.36.0) | 4.36.0 | Apache-2.0 |
| [org.seleniumhq.selenium:selenium-edge-driver](https://central.sonatype.com/artifact/org.seleniumhq.selenium/selenium-edge-driver/4.31.0) | 4.31.0 | Apache-2.0 |
| [org.seleniumhq.selenium:selenium-firefox-driver](https://central.sonatype.com/artifact/org.seleniumhq.selenium/selenium-firefox-driver/4.31.0) | 4.31.0 | Apache-2.0 |
| [org.seleniumhq.selenium:selenium-http](https://central.sonatype.com/artifact/org.seleniumhq.selenium/selenium-http/4.31.0) | 4.31.0 | Apache-2.0 |
| [org.seleniumhq.selenium:selenium-ie-driver](https://central.sonatype.com/artifact/org.seleniumhq.selenium/selenium-ie-driver/4.31.0) | 4.31.0 | Apache-2.0 |
| [org.seleniumhq.selenium:selenium-java](https://central.sonatype.com/artifact/org.seleniumhq.selenium/selenium-java/4.36.0) | 4.36.0 | Apache-2.0 |
| [org.seleniumhq.selenium:selenium-json](https://central.sonatype.com/artifact/org.seleniumhq.selenium/selenium-json/4.31.0) | 4.31.0 | Apache-2.0 |
| [org.seleniumhq.selenium:selenium-manager](https://central.sonatype.com/artifact/org.seleniumhq.selenium/selenium-manager/4.31.0) | 4.31.0 | Apache-2.0 |
| [org.seleniumhq.selenium:selenium-os](https://central.sonatype.com/artifact/org.seleniumhq.selenium/selenium-os/4.31.0) | 4.31.0 | Apache-2.0 |
| [org.seleniumhq.selenium:selenium-remote-driver](https://central.sonatype.com/artifact/org.seleniumhq.selenium/selenium-remote-driver/4.31.0) | 4.31.0 | Apache-2.0 |
| [org.seleniumhq.selenium:selenium-safari-driver](https://central.sonatype.com/artifact/org.seleniumhq.selenium/selenium-safari-driver/4.31.0) | 4.31.0 | Apache-2.0 |
| [org.seleniumhq.selenium:selenium-support](https://central.sonatype.com/artifact/org.seleniumhq.selenium/selenium-support/4.31.0) | 4.31.0 | Apache-2.0 |
| [org.slf4j:jcl-over-slf4j](https://central.sonatype.com/artifact/org.slf4j/jcl-over-slf4j/2.0.17) | 2.0.17 | Apache-2.0 |
| [org.springframework.boot:spring-boot](https://central.sonatype.com/artifact/org.springframework.boot/spring-boot/3.5.10) | 3.5.10 | Apache-2.0 |
| [org.springframework.boot:spring-boot-actuator](https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-actuator/3.5.10) | 3.5.10 | Apache-2.0 |
| [org.springframework.boot:spring-boot-actuator-autoconfigure](https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-actuator-autoconfigure/3.5.10) | 3.5.10 | Apache-2.0 |
| [org.springframework.boot:spring-boot-autoconfigure](https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-autoconfigure/3.5.10) | 3.5.10 | Apache-2.0 |
| [org.springframework.boot:spring-boot-configuration-processor](https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-configuration-processor/3.5.10) | 3.5.10 | Apache-2.0 |
| [org.springframework.boot:spring-boot-devtools](https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-devtools/3.5.10) | 3.5.10 | Apache-2.0 |
| [org.springframework.boot:spring-boot-starter](https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-starter/3.5.10) | 3.5.10 | Apache-2.0 |
| [org.springframework.boot:spring-boot-starter-actuator](https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-starter-actuator/3.5.10) | 3.5.10 | Apache-2.0 |
| [org.springframework.boot:spring-boot-starter-data-jpa](https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-starter-data-jpa/3.5.10) | 3.5.10 | Apache-2.0 |
| [org.springframework.boot:spring-boot-starter-jdbc](https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-starter-jdbc/3.5.10) | 3.5.10 | Apache-2.0 |
| [org.springframework.boot:spring-boot-starter-jersey](https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-starter-jersey/3.5.10) | 3.5.10 | Apache-2.0 |
| [org.springframework.boot:spring-boot-starter-json](https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-starter-json/3.5.10) | 3.5.10 | Apache-2.0 |
| [org.springframework.boot:spring-boot-starter-logging](https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-starter-logging/3.5.10) | 3.5.10 | Apache-2.0 |
| [org.springframework.boot:spring-boot-starter-oauth2-client](https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-starter-oauth2-client/3.5.10) | 3.5.10 | Apache-2.0 |
| [org.springframework.boot:spring-boot-starter-security](https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-starter-security/3.5.10) | 3.5.10 | Apache-2.0 |
| [org.springframework.boot:spring-boot-starter-test](https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-starter-test/3.5.10) | 3.5.10 | Apache-2.0 |
| [org.springframework.boot:spring-boot-starter-tomcat](https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-starter-tomcat/3.5.10) | 3.5.10 | Apache-2.0 |
| [org.springframework.boot:spring-boot-starter-validation](https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-starter-validation/3.5.10) | 3.5.10 | Apache-2.0 |
| [org.springframework.boot:spring-boot-starter-web](https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-starter-web/3.5.10) | 3.5.10 | Apache-2.0 |
| [org.springframework.boot:spring-boot-test](https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-test/3.5.10) | 3.5.10 | Apache-2.0 |
| [org.springframework.boot:spring-boot-test-autoconfigure](https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-test-autoconfigure/3.5.10) | 3.5.10 | Apache-2.0 |
| [org.springframework.data:spring-data-commons](https://central.sonatype.com/artifact/org.springframework.data/spring-data-commons/3.5.8) | 3.5.8 | Apache-2.0 |
| [org.springframework.data:spring-data-jpa](https://central.sonatype.com/artifact/org.springframework.data/spring-data-jpa/3.5.8) | 3.5.8 | Apache-2.0 |
| [org.springframework.security:spring-security-config](https://central.sonatype.com/artifact/org.springframework.security/spring-security-config/6.5.7) | 6.5.7 | Apache-2.0 |
| [org.springframework.security:spring-security-core](https://central.sonatype.com/artifact/org.springframework.security/spring-security-core/6.5.7) | 6.5.7 | Apache-2.0 |
| [org.springframework.security:spring-security-crypto](https://central.sonatype.com/artifact/org.springframework.security/spring-security-crypto/6.5.7) | 6.5.7 | Apache-2.0 |
| [org.springframework.security:spring-security-oauth2-client](https://central.sonatype.com/artifact/org.springframework.security/spring-security-oauth2-client/6.5.7) | 6.5.7 | Apache-2.0 |
| [org.springframework.security:spring-security-oauth2-core](https://central.sonatype.com/artifact/org.springframework.security/spring-security-oauth2-core/6.5.7) | 6.5.7 | Apache-2.0 |
| [org.springframework.security:spring-security-oauth2-jose](https://central.sonatype.com/artifact/org.springframework.security/spring-security-oauth2-jose/6.5.7) | 6.5.7 | Apache-2.0 |
| [org.springframework.security:spring-security-web](https://central.sonatype.com/artifact/org.springframework.security/spring-security-web/6.5.7) | 6.5.7 | Apache-2.0 |
| [org.springframework:spring-aop](https://central.sonatype.com/artifact/org.springframework/spring-aop/6.2.15) | 6.2.15 | Apache-2.0 |
| [org.springframework:spring-aspects](https://central.sonatype.com/artifact/org.springframework/spring-aspects/6.2.15) | 6.2.15 | Apache-2.0 |
| [org.springframework:spring-beans](https://central.sonatype.com/artifact/org.springframework/spring-beans/6.2.15) | 6.2.15 | Apache-2.0 |
| [org.springframework:spring-context](https://central.sonatype.com/artifact/org.springframework/spring-context/6.2.15) | 6.2.15 | Apache-2.0 |
| [org.springframework:spring-context](https://central.sonatype.com/artifact/org.springframework/spring-context/6.2.13) | 6.2.13 | Apache-2.0 |
| [org.springframework:spring-core](https://central.sonatype.com/artifact/org.springframework/spring-core/6.2.15) | 6.2.15 | Apache-2.0 |
| [org.springframework:spring-expression](https://central.sonatype.com/artifact/org.springframework/spring-expression/6.2.15) | 6.2.15 | Apache-2.0 |
| [org.springframework:spring-expression](https://central.sonatype.com/artifact/org.springframework/spring-expression/6.2.13) | 6.2.13 | Apache-2.0 |
| [org.springframework:spring-jcl](https://central.sonatype.com/artifact/org.springframework/spring-jcl/6.2.15) | 6.2.15 | Apache-2.0 |
| [org.springframework:spring-jdbc](https://central.sonatype.com/artifact/org.springframework/spring-jdbc/6.2.15) | 6.2.15 | Apache-2.0 |
| [org.springframework:spring-orm](https://central.sonatype.com/artifact/org.springframework/spring-orm/6.2.15) | 6.2.15 | Apache-2.0 |
| [org.springframework:spring-test](https://central.sonatype.com/artifact/org.springframework/spring-test/6.2.15) | 6.2.15 | Apache-2.0 |
| [org.springframework:spring-tx](https://central.sonatype.com/artifact/org.springframework/spring-tx/6.2.15) | 6.2.15 | Apache-2.0 |
| [org.springframework:spring-web](https://central.sonatype.com/artifact/org.springframework/spring-web/6.2.15) | 6.2.15 | Apache-2.0 |
| [org.springframework:spring-web](https://central.sonatype.com/artifact/org.springframework/spring-web/6.2.13) | 6.2.13 | Apache-2.0 |
| [org.springframework:spring-webmvc](https://central.sonatype.com/artifact/org.springframework/spring-webmvc/6.2.15) | 6.2.15 | Apache-2.0 |
| [org.wildfly.client:wildfly-client-config](https://central.sonatype.com/artifact/org.wildfly.client/wildfly-client-config/1.0.1.Final) | 1.0.1.Final | Apache-2.0 |
| [org.wildfly.clustering:wildfly-clustering-cache-spi](https://central.sonatype.com/artifact/org.wildfly.clustering/wildfly-clustering-cache-spi/7.0.12.Final) | 7.0.12.Final | Apache-2.0 |
| [org.wildfly.clustering:wildfly-clustering-context](https://central.sonatype.com/artifact/org.wildfly.clustering/wildfly-clustering-context/7.0.12.Final) | 7.0.12.Final | Apache-2.0 |
| [org.wildfly.clustering:wildfly-clustering-function](https://central.sonatype.com/artifact/org.wildfly.clustering/wildfly-clustering-function/7.0.12.Final) | 7.0.12.Final | Apache-2.0 |
| [org.wildfly.clustering:wildfly-clustering-marshalling-jboss](https://central.sonatype.com/artifact/org.wildfly.clustering/wildfly-clustering-marshalling-jboss/7.0.12.Final) | 7.0.12.Final | Apache-2.0 |
| [org.wildfly.clustering:wildfly-clustering-marshalling-protostream](https://central.sonatype.com/artifact/org.wildfly.clustering/wildfly-clustering-marshalling-protostream/7.0.12.Final) | 7.0.12.Final | Apache-2.0 |
| [org.wildfly.clustering:wildfly-clustering-marshalling-spi](https://central.sonatype.com/artifact/org.wildfly.clustering/wildfly-clustering-marshalling-spi/7.0.12.Final) | 7.0.12.Final | Apache-2.0 |
| [org.wildfly.clustering:wildfly-clustering-server-api](https://central.sonatype.com/artifact/org.wildfly.clustering/wildfly-clustering-server-api/7.0.12.Final) | 7.0.12.Final | Apache-2.0 |
| [org.wildfly.clustering:wildfly-clustering-server-local](https://central.sonatype.com/artifact/org.wildfly.clustering/wildfly-clustering-server-local/7.0.12.Final) | 7.0.12.Final | Apache-2.0 |
| [org.wildfly.clustering:wildfly-clustering-server-spi](https://central.sonatype.com/artifact/org.wildfly.clustering/wildfly-clustering-server-spi/7.0.12.Final) | 7.0.12.Final | Apache-2.0 |
| [org.wildfly.common:wildfly-common](https://central.sonatype.com/artifact/org.wildfly.common/wildfly-common/2.0.1) | 2.0.1 | Apache-2.0 |
| [org.wildfly.common:wildfly-common](https://central.sonatype.com/artifact/org.wildfly.common/wildfly-common/1.7.0.Final) | 1.7.0.Final | Apache-2.0 |
| [org.wildfly.core:wildfly-controller](https://central.sonatype.com/artifact/org.wildfly.core/wildfly-controller/29.0.1.Final) | 29.0.1.Final | Apache-2.0 |
| [org.wildfly.core:wildfly-controller-client](https://central.sonatype.com/artifact/org.wildfly.core/wildfly-controller-client/30.0.0.Final) | 30.0.0.Final | Apache-2.0 |
| [org.wildfly.core:wildfly-core-management-client](https://central.sonatype.com/artifact/org.wildfly.core/wildfly-core-management-client/29.0.1.Final) | 29.0.1.Final | Apache-2.0 |
| [org.wildfly.core:wildfly-core-security](https://central.sonatype.com/artifact/org.wildfly.core/wildfly-core-security/29.0.1.Final) | 29.0.1.Final | Apache-2.0 |
| [org.wildfly.core:wildfly-deployment-repository](https://central.sonatype.com/artifact/org.wildfly.core/wildfly-deployment-repository/30.0.0.Final) | 30.0.0.Final | Apache-2.0 |
| [org.wildfly.core:wildfly-domain-http-interface](https://central.sonatype.com/artifact/org.wildfly.core/wildfly-domain-http-interface/29.0.1.Final) | 29.0.1.Final | Apache-2.0 |
| [org.wildfly.core:wildfly-domain-management](https://central.sonatype.com/artifact/org.wildfly.core/wildfly-domain-management/29.0.1.Final) | 29.0.1.Final | Apache-2.0 |
| [org.wildfly.core:wildfly-embedded](https://central.sonatype.com/artifact/org.wildfly.core/wildfly-embedded/29.0.1.Final) | 29.0.1.Final | Apache-2.0 |
| [org.wildfly.core:wildfly-io-spi](https://central.sonatype.com/artifact/org.wildfly.core/wildfly-io-spi/29.0.1.Final) | 29.0.1.Final | Apache-2.0 |
| [org.wildfly.core:wildfly-network](https://central.sonatype.com/artifact/org.wildfly.core/wildfly-network/29.0.1.Final) | 29.0.1.Final | Apache-2.0 |
| [org.wildfly.core:wildfly-platform-mbean](https://central.sonatype.com/artifact/org.wildfly.core/wildfly-platform-mbean/29.0.1.Final) | 29.0.1.Final | Apache-2.0 |
| [org.wildfly.core:wildfly-process-controller](https://central.sonatype.com/artifact/org.wildfly.core/wildfly-process-controller/29.0.1.Final) | 29.0.1.Final | Apache-2.0 |
| [org.wildfly.core:wildfly-remoting](https://central.sonatype.com/artifact/org.wildfly.core/wildfly-remoting/29.0.1.Final) | 29.0.1.Final | Apache-2.0 |
| [org.wildfly.core:wildfly-request-controller](https://central.sonatype.com/artifact/org.wildfly.core/wildfly-request-controller/29.0.1.Final) | 29.0.1.Final | Apache-2.0 |
| [org.wildfly.core:wildfly-server](https://central.sonatype.com/artifact/org.wildfly.core/wildfly-server/29.0.1.Final) | 29.0.1.Final | Apache-2.0 |
| [org.wildfly.core:wildfly-service](https://central.sonatype.com/artifact/org.wildfly.core/wildfly-service/29.0.1.Final) | 29.0.1.Final | Apache-2.0 |
| [org.wildfly.core:wildfly-subsystem](https://central.sonatype.com/artifact/org.wildfly.core/wildfly-subsystem/29.0.1.Final) | 29.0.1.Final | Apache-2.0 |
| [org.wildfly.core:wildfly-threads](https://central.sonatype.com/artifact/org.wildfly.core/wildfly-threads/29.0.1.Final) | 29.0.1.Final | Apache-2.0 |
| [org.wildfly.core:wildfly-version](https://central.sonatype.com/artifact/org.wildfly.core/wildfly-version/30.0.0.Final) | 30.0.0.Final | Apache-2.0 |
| [org.wildfly.discovery:wildfly-discovery-client](https://central.sonatype.com/artifact/org.wildfly.discovery/wildfly-discovery-client/1.3.0.Final) | 1.3.0.Final | Apache-2.0 |
| [org.wildfly.security.elytron-web:undertow-server](https://central.sonatype.com/artifact/org.wildfly.security.elytron-web/undertow-server/4.1.2.Final) | 4.1.2.Final | Apache-2.0 |
| [org.wildfly.security.jakarta:jakarta-authorization](https://central.sonatype.com/artifact/org.wildfly.security.jakarta/jakarta-authorization/3.1.4.Final) | 3.1.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-asn1](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-asn1/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-audit](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-audit/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-auth](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-auth/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-auth-server](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-auth-server/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-auth-server-deprecated](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-auth-server-deprecated/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-auth-server-http](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-auth-server-http/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-auth-server-sasl](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-auth-server-sasl/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-auth-util](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-auth-util/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-base](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-base/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-client](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-client/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-credential](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-credential/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-credential-source-impl](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-credential-source-impl/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-credential-store](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-credential-store/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-encryption](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-encryption/2.7.0.Final) | 2.7.0.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-http](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-http/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-http-util](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-http-util/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-keystore](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-keystore/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-mechanism](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-mechanism/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-mechanism-digest](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-mechanism-digest/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-mechanism-gssapi](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-mechanism-gssapi/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-password-impl](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-password-impl/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-permission](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-permission/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-provider-util](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-provider-util/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-realm](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-realm/2.7.0.Final) | 2.7.0.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-sasl](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-sasl/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-sasl-anonymous](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-sasl-anonymous/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-sasl-auth-util](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-sasl-auth-util/2.5.0.Final) | 2.5.0.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-sasl-digest](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-sasl-digest/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-security-manager](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-security-manager/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-security-manager-action](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-security-manager-action/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-ssh-util](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-ssh-util/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-ssl](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-ssl/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-util](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-util/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-x500](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-x500/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-x500-cert](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-x500-cert/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-x500-cert-acme](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-x500-cert-acme/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.security:wildfly-elytron-x500-cert-util](https://central.sonatype.com/artifact/org.wildfly.security/wildfly-elytron-x500-cert-util/2.6.4.Final) | 2.6.4.Final | Apache-2.0 |
| [org.wildfly.transaction:wildfly-transaction-client](https://central.sonatype.com/artifact/org.wildfly.transaction/wildfly-transaction-client/3.0.5.Final) | 3.0.5.Final | Apache-2.0 |
| [org.wildfly.wildfly-http-client:wildfly-http-client-common](https://central.sonatype.com/artifact/org.wildfly.wildfly-http-client/wildfly-http-client-common/2.1.1.Final) | 2.1.1.Final | Apache-2.0 |
| [org.wildfly.wildfly-http-client:wildfly-http-ejb-client](https://central.sonatype.com/artifact/org.wildfly.wildfly-http-client/wildfly-http-ejb-client/2.1.1.Final) | 2.1.1.Final | Apache-2.0 |
| [org.wildfly:wildfly-clustering-common](https://central.sonatype.com/artifact/org.wildfly/wildfly-clustering-common/37.0.1.Final) | 37.0.1.Final | Apache-2.0 |
| [org.wildfly:wildfly-clustering-ejb-spi](https://central.sonatype.com/artifact/org.wildfly/wildfly-clustering-ejb-spi/37.0.1.Final) | 37.0.1.Final | Apache-2.0 |
| [org.wildfly:wildfly-clustering-infinispan-embedded-service](https://central.sonatype.com/artifact/org.wildfly/wildfly-clustering-infinispan-embedded-service/37.0.1.Final) | 37.0.1.Final | Apache-2.0 |
| [org.wildfly:wildfly-clustering-server-api](https://central.sonatype.com/artifact/org.wildfly/wildfly-clustering-server-api/37.0.1.Final) | 37.0.1.Final | Apache-2.0 |
| [org.wildfly:wildfly-clustering-server-service](https://central.sonatype.com/artifact/org.wildfly/wildfly-clustering-server-service/37.0.1.Final) | 37.0.1.Final | Apache-2.0 |
| [org.wildfly:wildfly-clustering-service](https://central.sonatype.com/artifact/org.wildfly/wildfly-clustering-service/37.0.1.Final) | 37.0.1.Final | Apache-2.0 |
| [org.wildfly:wildfly-clustering-singleton-api](https://central.sonatype.com/artifact/org.wildfly/wildfly-clustering-singleton-api/37.0.1.Final) | 37.0.1.Final | Apache-2.0 |
| [org.wildfly:wildfly-common-ee-dependency-management](https://central.sonatype.com/artifact/org.wildfly/wildfly-common-ee-dependency-management/37.0.1.Final) | 37.0.1.Final | Apache-2.0 |
| [org.wildfly:wildfly-common-expansion-dependency-management](https://central.sonatype.com/artifact/org.wildfly/wildfly-common-expansion-dependency-management/37.0.1.Final) | 37.0.1.Final | Apache-2.0 |
| [org.wildfly:wildfly-concurrency-spi](https://central.sonatype.com/artifact/org.wildfly/wildfly-concurrency-spi/37.0.1.Final) | 37.0.1.Final | Apache-2.0 |
| [org.wildfly:wildfly-connector](https://central.sonatype.com/artifact/org.wildfly/wildfly-connector/37.0.1.Final) | 37.0.1.Final | Apache-2.0 |
| [org.wildfly:wildfly-dist](https://central.sonatype.com/artifact/org.wildfly/wildfly-dist/37.0.1.Final) | 37.0.1.Final | Apache-2.0 |
| [org.wildfly:wildfly-ee](https://central.sonatype.com/artifact/org.wildfly/wildfly-ee/37.0.1.Final) | 37.0.1.Final | Apache-2.0 |
| [org.wildfly:wildfly-ejb3](https://central.sonatype.com/artifact/org.wildfly/wildfly-ejb3/37.0.1.Final) | 37.0.1.Final | Apache-2.0 |
| [org.wildfly:wildfly-galleon-pack](https://central.sonatype.com/artifact/org.wildfly/wildfly-galleon-pack/37.0.1.Final) | 37.0.1.Final | Apache-2.0 |
| [org.wildfly:wildfly-iiop-openjdk](https://central.sonatype.com/artifact/org.wildfly/wildfly-iiop-openjdk/37.0.1.Final) | 37.0.1.Final | Apache-2.0 |
| [org.wildfly:wildfly-naming](https://central.sonatype.com/artifact/org.wildfly/wildfly-naming/37.0.1.Final) | 37.0.1.Final | Apache-2.0 |
| [org.wildfly:wildfly-naming-client](https://central.sonatype.com/artifact/org.wildfly/wildfly-naming-client/2.0.1.Final) | 2.0.1.Final | Apache-2.0 |
| [org.wildfly:wildfly-standard-ee-bom](https://central.sonatype.com/artifact/org.wildfly/wildfly-standard-ee-bom/37.0.1.Final) | 37.0.1.Final | Apache-2.0 |
| [org.wildfly:wildfly-standard-expansion-bom](https://central.sonatype.com/artifact/org.wildfly/wildfly-standard-expansion-bom/37.0.1.Final) | 37.0.1.Final | Apache-2.0 |
| [org.wildfly:wildfly-transactions](https://central.sonatype.com/artifact/org.wildfly/wildfly-transactions/37.0.1.Final) | 37.0.1.Final | Apache-2.0 |
| [org.wildfly:wildfly-web-common](https://central.sonatype.com/artifact/org.wildfly/wildfly-web-common/37.0.1.Final) | 37.0.1.Final | Apache-2.0 |
| [org.wildfly:wildfly-weld-common](https://central.sonatype.com/artifact/org.wildfly/wildfly-weld-common/37.0.1.Final) | 37.0.1.Final | Apache-2.0 |
| [org.yaml:snakeyaml](https://central.sonatype.com/artifact/org.yaml/snakeyaml/2.4) | 2.4 | Apache-2.0 |
| [org.crac:crac](https://central.sonatype.com/artifact/org.crac/crac/1.5.0) | 1.5.0 | BSD-2-Clause |
| [com.sun.activation:jakarta.activation](https://central.sonatype.com/artifact/com.sun.activation/jakarta.activation/2.0.1) | 2.0.1 | BSD-3-Clause |
| [com.sun.istack:istack-commons-runtime](https://central.sonatype.com/artifact/com.sun.istack/istack-commons-runtime/4.1.2) | 4.1.2 | BSD-3-Clause |
| [com.sun.istack:istack-commons-runtime](https://central.sonatype.com/artifact/com.sun.istack/istack-commons-runtime/4.2.0) | 4.2.0 | BSD-3-Clause |
| [jakarta.activation:jakarta.activation-api](https://central.sonatype.com/artifact/jakarta.activation/jakarta.activation-api/2.1.0) | 2.1.0 | BSD-3-Clause |
| [jakarta.xml.bind:jakarta.xml.bind-api](https://central.sonatype.com/artifact/jakarta.xml.bind/jakarta.xml.bind-api/4.0.0) | 4.0.0 | BSD-3-Clause |
| [org.antlr:antlr4-runtime](https://central.sonatype.com/artifact/org.antlr/antlr4-runtime/4.13.0) | 4.13.0 | BSD-3-Clause |
| [org.eclipse.angus:angus-activation](https://central.sonatype.com/artifact/org.eclipse.angus/angus-activation/2.0.3) | 2.0.3 | BSD-3-Clause |
| [org.eclipse.jgit:org.eclipse.jgit](https://central.sonatype.com/artifact/org.eclipse.jgit/org.eclipse.jgit/6.10.1.202505221210-r) | 6.10.1.202505221210-r | BSD-3-Clause |
| [org.eclipse.jgit:org.eclipse.jgit.ssh.apache](https://central.sonatype.com/artifact/org.eclipse.jgit/org.eclipse.jgit.ssh.apache/6.10.1.202505221210-r) | 6.10.1.202505221210-r | BSD-3-Clause |
| [org.glassfish.jaxb:jaxb-core](https://central.sonatype.com/artifact/org.glassfish.jaxb/jaxb-core/4.0.6) | 4.0.6 | BSD-3-Clause |
| [org.glassfish.jaxb:jaxb-runtime](https://central.sonatype.com/artifact/org.glassfish.jaxb/jaxb-runtime/4.0.6) | 4.0.6 | BSD-3-Clause |
| [org.glassfish.jaxb:txw2](https://central.sonatype.com/artifact/org.glassfish.jaxb/txw2/4.0.6) | 4.0.6 | BSD-3-Clause |
| [org.hamcrest:hamcrest](https://central.sonatype.com/artifact/org.hamcrest/hamcrest/3.0) | 3.0 | BSD-3-Clause |
| [org.hamcrest:hamcrest-core](https://central.sonatype.com/artifact/org.hamcrest/hamcrest-core/1.3) | 1.3 | BSD-3-Clause |
| [org.hamcrest:hamcrest-core](https://central.sonatype.com/artifact/org.hamcrest/hamcrest-core/3.0) | 3.0 | BSD-3-Clause |
| [org.ow2.asm:asm](https://central.sonatype.com/artifact/org.ow2.asm/asm/7.3.1) | 7.3.1 | BSD-3-Clause |
| [org.ow2.asm:asm](https://central.sonatype.com/artifact/org.ow2.asm/asm/9.7.1) | 9.7.1 | BSD-3-Clause |
| [org.ow2.asm:asm](https://central.sonatype.com/artifact/org.ow2.asm/asm/9.8) | 9.8 | BSD-3-Clause |
| [org.ow2.asm:asm-analysis](https://central.sonatype.com/artifact/org.ow2.asm/asm-analysis/9.8) | 9.8 | BSD-3-Clause |
| [org.ow2.asm:asm-commons](https://central.sonatype.com/artifact/org.ow2.asm/asm-commons/9.6) | 9.6 | BSD-3-Clause |
| [org.ow2.asm:asm-commons](https://central.sonatype.com/artifact/org.ow2.asm/asm-commons/9.8) | 9.8 | BSD-3-Clause |
| [org.ow2.asm:asm-tree](https://central.sonatype.com/artifact/org.ow2.asm/asm-tree/9.8) | 9.8 | BSD-3-Clause |
| [org.ow2.asm:asm-util](https://central.sonatype.com/artifact/org.ow2.asm/asm-util/9.8) | 9.8 | BSD-3-Clause |
| [org.hdrhistogram:HdrHistogram](https://central.sonatype.com/artifact/org.hdrhistogram/HdrHistogram/2.2.2) | 2.2.2 | CC0-1.0, BSD-2-Clause |
| [aopalliance:aopalliance](https://central.sonatype.com/artifact/aopalliance/aopalliance/1.0) | 1.0 | CC0-1.0 |
| [org.jboss:jboss-transaction-spi](https://central.sonatype.com/artifact/org.jboss/jboss-transaction-spi/8.0.0.Final) | 8.0.0.Final | CC0-1.0 |
| [org.latencyutils:LatencyUtils](https://central.sonatype.com/artifact/org.latencyutils/LatencyUtils/2.0.3) | 2.0.3 | CC0-1.0 |
| [org.jboss.spec.javax.transaction:jboss-transaction-api_1.2_spec](https://central.sonatype.com/artifact/org.jboss.spec.javax.transaction/jboss-transaction-api_1.2_spec/1.0.0.Final) | 1.0.0.Final | CDDL-1.0, GPL-2.0-with-classpath-exception |
| [javax.xml.bind:jaxb-api](https://central.sonatype.com/artifact/javax.xml.bind/jaxb-api/2.3.1) | 2.3.1 | CDDL-1.1, GPL-2.0-with-classpath-exception |
| [javax.xml.bind:jaxb-api](https://central.sonatype.com/artifact/javax.xml.bind/jaxb-api/2.2.12) | 2.2.12 | CDDL-1.1, GPL-2.0-with-classpath-exception |
| [ch.qos.logback:logback-classic](https://central.sonatype.com/artifact/ch.qos.logback/logback-classic/1.5.25) | 1.5.25 | EPL-1.0, LGPL-2.1-only |
| [ch.qos.logback:logback-core](https://central.sonatype.com/artifact/ch.qos.logback/logback-core/1.5.25) | 1.5.25 | EPL-1.0, LGPL-2.1-only |
| [junit:junit](https://central.sonatype.com/artifact/junit/junit/4.13.2) | 4.13.2 | EPL-1.0 |
| [jakarta.persistence:jakarta.persistence-api](https://central.sonatype.com/artifact/jakarta.persistence/jakarta.persistence-api/3.1.0) | 3.1.0 | EPL-2.0, BSD-3-Clause |
| [org.glassfish.jersey.core:jersey-server](https://central.sonatype.com/artifact/org.glassfish.jersey.core/jersey-server/3.1.11) | 3.1.11 | EPL-2.0, GPL-2.0-with-classpath-exception, Apache-2.0, BSD-3-Clause |
| [org.glassfish.jersey.core:jersey-common](https://central.sonatype.com/artifact/org.glassfish.jersey.core/jersey-common/3.1.11) | 3.1.11 | EPL-2.0, GPL-2.0-with-classpath-exception, Apache-2.0, CC0-1.0 |
| [org.jboss.spec.jakarta.el:jboss-el-api_5.0_spec](https://central.sonatype.com/artifact/org.jboss.spec.jakarta.el/jboss-el-api_5.0_spec/4.0.1.Final) | 4.0.1.Final | EPL-2.0, GPL-2.0-with-classpath-exception, Apache-2.0, LGPL-2.1-only |
| [org.glassfish.jersey.media:jersey-media-json-jackson](https://central.sonatype.com/artifact/org.glassfish.jersey.media/jersey-media-json-jackson/3.1.11) | 3.1.11 | EPL-2.0, GPL-2.0-with-classpath-exception, Apache-2.0 |
| [org.glassfish.jersey.containers:jersey-container-servlet](https://central.sonatype.com/artifact/org.glassfish.jersey.containers/jersey-container-servlet/3.1.11) | 3.1.11 | EPL-2.0, GPL-2.0-with-classpath-exception, BSD-3-Clause, BSD-2-Clause, Apache-2.0, CC0-1.0, BSD-3-Clause, JQUERY, MIT, W3C-19990505 |
| [org.glassfish.jersey.containers:jersey-container-servlet-core](https://central.sonatype.com/artifact/org.glassfish.jersey.containers/jersey-container-servlet-core/3.1.11) | 3.1.11 | EPL-2.0, GPL-2.0-with-classpath-exception, BSD-3-Clause, BSD-2-Clause, Apache-2.0, CC0-1.0, BSD-3-Clause, JQUERY, MIT, W3C-19990505 |
| [org.glassfish.jersey.core:jersey-client](https://central.sonatype.com/artifact/org.glassfish.jersey.core/jersey-client/3.1.11) | 3.1.11 | EPL-2.0, GPL-2.0-with-classpath-exception, BSD-3-Clause, BSD-2-Clause, Apache-2.0, CC0-1.0, BSD-3-Clause, JQUERY, MIT, W3C-19990505 |
| [org.glassfish.jersey.ext:jersey-bean-validation](https://central.sonatype.com/artifact/org.glassfish.jersey.ext/jersey-bean-validation/3.1.11) | 3.1.11 | EPL-2.0, GPL-2.0-with-classpath-exception, BSD-3-Clause, BSD-2-Clause, Apache-2.0, CC0-1.0, BSD-3-Clause, JQUERY, MIT, W3C-19990505 |
| [org.glassfish.jersey.ext:jersey-entity-filtering](https://central.sonatype.com/artifact/org.glassfish.jersey.ext/jersey-entity-filtering/3.1.11) | 3.1.11 | EPL-2.0, GPL-2.0-with-classpath-exception, BSD-3-Clause, BSD-2-Clause, Apache-2.0, CC0-1.0, BSD-3-Clause, JQUERY, MIT, W3C-19990505 |
| [org.glassfish.jersey.ext:jersey-spring6](https://central.sonatype.com/artifact/org.glassfish.jersey.ext/jersey-spring6/3.1.11) | 3.1.11 | EPL-2.0, GPL-2.0-with-classpath-exception, BSD-3-Clause, BSD-2-Clause, Apache-2.0, CC0-1.0, BSD-3-Clause, JQUERY, MIT, W3C-19990505 |
| [org.glassfish.jersey.inject:jersey-hk2](https://central.sonatype.com/artifact/org.glassfish.jersey.inject/jersey-hk2/3.1.11) | 3.1.11 | EPL-2.0, GPL-2.0-with-classpath-exception, BSD-3-Clause, BSD-2-Clause, Apache-2.0, CC0-1.0, BSD-3-Clause, JQUERY, MIT, W3C-19990505 |
| [com.sun.mail:jakarta.mail](https://central.sonatype.com/artifact/com.sun.mail/jakarta.mail/2.0.1) | 2.0.1 | EPL-2.0, GPL-2.0-with-classpath-exception, BSD-3-Clause |
| [jakarta.annotation:jakarta.annotation-api](https://central.sonatype.com/artifact/jakarta.annotation/jakarta.annotation-api/2.1.1) | 2.1.1 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [jakarta.annotation:jakarta.annotation-api](https://central.sonatype.com/artifact/jakarta.annotation/jakarta.annotation-api/3.0.0) | 3.0.0 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [jakarta.authentication:jakarta.authentication-api](https://central.sonatype.com/artifact/jakarta.authentication/jakarta.authentication-api/3.0.0) | 3.0.0 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [jakarta.authorization:jakarta.authorization-api](https://central.sonatype.com/artifact/jakarta.authorization/jakarta.authorization-api/2.1.0) | 2.1.0 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [jakarta.ejb:jakarta.ejb-api](https://central.sonatype.com/artifact/jakarta.ejb/jakarta.ejb-api/4.0.1) | 4.0.1 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [jakarta.el:jakarta.el-api](https://central.sonatype.com/artifact/jakarta.el/jakarta.el-api/5.0.1) | 5.0.1 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [jakarta.el:jakarta.el-api](https://central.sonatype.com/artifact/jakarta.el/jakarta.el-api/6.0.1) | 6.0.1 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [jakarta.enterprise.concurrent:jakarta.enterprise.concurrent-api](https://central.sonatype.com/artifact/jakarta.enterprise.concurrent/jakarta.enterprise.concurrent-api/3.0.1) | 3.0.1 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [jakarta.faces:jakarta.faces-api](https://central.sonatype.com/artifact/jakarta.faces/jakarta.faces-api/4.0.1) | 4.0.1 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [jakarta.interceptor:jakarta.interceptor-api](https://central.sonatype.com/artifact/jakarta.interceptor/jakarta.interceptor-api/2.1.0) | 2.1.0 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [jakarta.interceptor:jakarta.interceptor-api](https://central.sonatype.com/artifact/jakarta.interceptor/jakarta.interceptor-api/2.2.0) | 2.2.0 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [jakarta.jms:jakarta.jms-api](https://central.sonatype.com/artifact/jakarta.jms/jakarta.jms-api/3.1.0) | 3.1.0 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [jakarta.json:jakarta.json-api](https://central.sonatype.com/artifact/jakarta.json/jakarta.json-api/2.1.0) | 2.1.0 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [jakarta.json:jakarta.json-api](https://central.sonatype.com/artifact/jakarta.json/jakarta.json-api/2.1.3) | 2.1.3 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [jakarta.resource:jakarta.resource-api](https://central.sonatype.com/artifact/jakarta.resource/jakarta.resource-api/2.1.0) | 2.1.0 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [jakarta.servlet.jsp:jakarta.servlet.jsp-api](https://central.sonatype.com/artifact/jakarta.servlet.jsp/jakarta.servlet.jsp-api/3.1.0) | 3.1.0 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [jakarta.servlet:jakarta.servlet-api](https://central.sonatype.com/artifact/jakarta.servlet/jakarta.servlet-api/6.0.0) | 6.0.0 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [jakarta.transaction:jakarta.transaction-api](https://central.sonatype.com/artifact/jakarta.transaction/jakarta.transaction-api/2.0.1) | 2.0.1 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [jakarta.ws.rs:jakarta.ws.rs-api](https://central.sonatype.com/artifact/jakarta.ws.rs/jakarta.ws.rs-api/3.1.0) | 3.1.0 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [org.eclipse.parsson:parsson](https://central.sonatype.com/artifact/org.eclipse.parsson/parsson/1.1.7) | 1.1.7 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [org.glassfish.expressly:expressly](https://central.sonatype.com/artifact/org.glassfish.expressly/expressly/5.0.0) | 5.0.0 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [org.glassfish.hk2.external:aopalliance-repackaged](https://central.sonatype.com/artifact/org.glassfish.hk2.external/aopalliance-repackaged/3.0.6) | 3.0.6 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [org.glassfish.hk2:class-model](https://central.sonatype.com/artifact/org.glassfish.hk2/class-model/3.0.6) | 3.0.6 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [org.glassfish.hk2:hk2](https://central.sonatype.com/artifact/org.glassfish.hk2/hk2/3.0.6) | 3.0.6 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [org.glassfish.hk2:hk2-api](https://central.sonatype.com/artifact/org.glassfish.hk2/hk2-api/3.0.6) | 3.0.6 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [org.glassfish.hk2:hk2-core](https://central.sonatype.com/artifact/org.glassfish.hk2/hk2-core/3.0.6) | 3.0.6 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [org.glassfish.hk2:hk2-locator](https://central.sonatype.com/artifact/org.glassfish.hk2/hk2-locator/3.0.6) | 3.0.6 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [org.glassfish.hk2:hk2-runlevel](https://central.sonatype.com/artifact/org.glassfish.hk2/hk2-runlevel/3.0.6) | 3.0.6 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [org.glassfish.hk2:hk2-utils](https://central.sonatype.com/artifact/org.glassfish.hk2/hk2-utils/3.0.6) | 3.0.6 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [org.glassfish.hk2:osgi-resource-locator](https://central.sonatype.com/artifact/org.glassfish.hk2/osgi-resource-locator/1.0.3) | 1.0.3 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [org.glassfish.hk2:spring-bridge](https://central.sonatype.com/artifact/org.glassfish.hk2/spring-bridge/3.0.6) | 3.0.6 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [org.glassfish:jakarta.enterprise.concurrent](https://central.sonatype.com/artifact/org.glassfish/jakarta.enterprise.concurrent/3.0.2) | 3.0.2 | EPL-2.0, GPL-2.0-with-classpath-exception |
| [eu.maveniverse.maven.mima.runtime:embedded-maven](https://central.sonatype.com/artifact/eu.maveniverse.maven.mima.runtime/embedded-maven/2.4.22) | 2.4.22 | EPL-2.0 |
| [eu.maveniverse.maven.mima.runtime:standalone-shared](https://central.sonatype.com/artifact/eu.maveniverse.maven.mima.runtime/standalone-shared/2.4.22) | 2.4.22 | EPL-2.0 |
| [eu.maveniverse.maven.mima.runtime:standalone-static](https://central.sonatype.com/artifact/eu.maveniverse.maven.mima.runtime/standalone-static/2.4.22) | 2.4.22 | EPL-2.0 |
| [eu.maveniverse.maven.mima:context](https://central.sonatype.com/artifact/eu.maveniverse.maven.mima/context/2.4.22) | 2.4.22 | EPL-2.0 |
| [org.aspectj:aspectjweaver](https://central.sonatype.com/artifact/org.aspectj/aspectjweaver/1.9.25.1) | 1.9.25.1 | EPL-2.0 |
| [org.eclipse.sisu:org.eclipse.sisu.inject](https://central.sonatype.com/artifact/org.eclipse.sisu/org.eclipse.sisu.inject/0.9.0.M3) | 0.9.0.M3 | EPL-2.0 |
| [org.eclipse.sisu:org.eclipse.sisu.plexus](https://central.sonatype.com/artifact/org.eclipse.sisu/org.eclipse.sisu.plexus/0.9.0.M3) | 0.9.0.M3 | EPL-2.0 |
| [org.junit.jupiter:junit-jupiter](https://central.sonatype.com/artifact/org.junit.jupiter/junit-jupiter/5.14.2) | 5.14.2 | EPL-2.0 |
| [org.junit.jupiter:junit-jupiter](https://central.sonatype.com/artifact/org.junit.jupiter/junit-jupiter/5.13.4) | 5.13.4 | EPL-2.0 |
| [org.junit.jupiter:junit-jupiter-api](https://central.sonatype.com/artifact/org.junit.jupiter/junit-jupiter-api/5.14.2) | 5.14.2 | EPL-2.0 |
| [org.junit.jupiter:junit-jupiter-api](https://central.sonatype.com/artifact/org.junit.jupiter/junit-jupiter-api/5.13.4) | 5.13.4 | EPL-2.0 |
| [org.junit.jupiter:junit-jupiter-engine](https://central.sonatype.com/artifact/org.junit.jupiter/junit-jupiter-engine/5.14.2) | 5.14.2 | EPL-2.0 |
| [org.junit.jupiter:junit-jupiter-engine](https://central.sonatype.com/artifact/org.junit.jupiter/junit-jupiter-engine/5.13.4) | 5.13.4 | EPL-2.0 |
| [org.junit.jupiter:junit-jupiter-params](https://central.sonatype.com/artifact/org.junit.jupiter/junit-jupiter-params/5.14.2) | 5.14.2 | EPL-2.0 |
| [org.junit.jupiter:junit-jupiter-params](https://central.sonatype.com/artifact/org.junit.jupiter/junit-jupiter-params/5.13.4) | 5.13.4 | EPL-2.0 |
| [org.junit.platform:junit-platform-commons](https://central.sonatype.com/artifact/org.junit.platform/junit-platform-commons/1.14.2) | 1.14.2 | EPL-2.0 |
| [org.junit.platform:junit-platform-commons](https://central.sonatype.com/artifact/org.junit.platform/junit-platform-commons/1.13.4) | 1.13.4 | EPL-2.0 |
| [org.junit.platform:junit-platform-engine](https://central.sonatype.com/artifact/org.junit.platform/junit-platform-engine/1.14.2) | 1.14.2 | EPL-2.0 |
| [org.junit.platform:junit-platform-engine](https://central.sonatype.com/artifact/org.junit.platform/junit-platform-engine/1.13.4) | 1.13.4 | EPL-2.0 |
| [org.junit.platform:junit-platform-launcher](https://central.sonatype.com/artifact/org.junit.platform/junit-platform-launcher/1.14.2) | 1.14.2 | EPL-2.0 |
| [org.junit.vintage:junit-vintage-engine](https://central.sonatype.com/artifact/org.junit.vintage/junit-vintage-engine/5.14.2) | 5.14.2 | EPL-2.0 |
| [org.codehaus.fabric3.api:commonj](https://central.sonatype.com/artifact/org.codehaus.fabric3.api/commonj/1.1.1) | 1.1.1 | FABRIC3 |
| [io.github.dmlloyd:jdk-classfile-backport](https://central.sonatype.com/artifact/io.github.dmlloyd/jdk-classfile-backport/24.0) | 24.0 | GPL-2.0-with-classpath-exception |
| [org.jboss.ironjacamar:ironjacamar-common-api](https://central.sonatype.com/artifact/org.jboss.ironjacamar/ironjacamar-common-api/3.0.14.Final) | 3.0.14.Final | LGPL-2.1+ |
| [org.jboss.ironjacamar:ironjacamar-common-impl](https://central.sonatype.com/artifact/org.jboss.ironjacamar/ironjacamar-common-impl/3.0.14.Final) | 3.0.14.Final | LGPL-2.1+ |
| [org.jboss.ironjacamar:ironjacamar-common-spi](https://central.sonatype.com/artifact/org.jboss.ironjacamar/ironjacamar-common-spi/3.0.14.Final) | 3.0.14.Final | LGPL-2.1+ |
| [org.jboss.ironjacamar:ironjacamar-core-api](https://central.sonatype.com/artifact/org.jboss.ironjacamar/ironjacamar-core-api/3.0.14.Final) | 3.0.14.Final | LGPL-2.1+ |
| [org.jboss.ironjacamar:ironjacamar-core-impl](https://central.sonatype.com/artifact/org.jboss.ironjacamar/ironjacamar-core-impl/3.0.14.Final) | 3.0.14.Final | LGPL-2.1+ |
| [org.jboss.ironjacamar:ironjacamar-deployers-common](https://central.sonatype.com/artifact/org.jboss.ironjacamar/ironjacamar-deployers-common/3.0.14.Final) | 3.0.14.Final | LGPL-2.1+ |
| [org.jboss.ironjacamar:ironjacamar-jdbc](https://central.sonatype.com/artifact/org.jboss.ironjacamar/ironjacamar-jdbc/3.0.14.Final) | 3.0.14.Final | LGPL-2.1+ |
| [org.jboss.ironjacamar:ironjacamar-validator](https://central.sonatype.com/artifact/org.jboss.ironjacamar/ironjacamar-validator/3.0.14.Final) | 3.0.14.Final | LGPL-2.1+ |
| [org.hibernate.orm:hibernate-core](https://central.sonatype.com/artifact/org.hibernate.orm/hibernate-core/6.6.41.Final) | 6.6.41.Final | LGPL-2.1-only |
| [org.jboss.common:jboss-common-beans](https://central.sonatype.com/artifact/org.jboss.common/jboss-common-beans/2.0.1.Final) | 2.0.1.Final | LGPL-2.1-only |
| [org.jboss.ironjacamar:ironjacamar-spec-api](https://central.sonatype.com/artifact/org.jboss.ironjacamar/ironjacamar-spec-api/1.4.35.Final) | 1.4.35.Final | LGPL-2.1-only |
| [org.jboss.msc:jboss-msc](https://central.sonatype.com/artifact/org.jboss.msc/jboss-msc/1.5.6.Final) | 1.5.6.Final | LGPL-2.1 |
| [org.jboss.stdio:jboss-stdio](https://central.sonatype.com/artifact/org.jboss.stdio/jboss-stdio/1.1.0.Final) | 1.1.0.Final | LGPL-2.1 |
| [org.graalvm.polyglot:js](https://central.sonatype.com/artifact/org.graalvm.polyglot/js/25.0.2) | 25.0.2 | MIT, UPL-1.0 |
| [org.reactivestreams:reactive-streams](https://central.sonatype.com/artifact/org.reactivestreams/reactive-streams/1.0.4) | 1.0.4 | MIT-0 |
| [com.konghq:unirest-java](https://central.sonatype.com/artifact/com.konghq/unirest-java/3.14.5) | 3.14.5 | MIT |
| [com.lihaoyi:fastparse_2.13](https://central.sonatype.com/artifact/com.lihaoyi/fastparse_2.13/3.1.1) | 3.1.1 | MIT |
| [com.lihaoyi:geny_2.13](https://central.sonatype.com/artifact/com.lihaoyi/geny_2.13/1.1.0) | 1.1.0 | MIT |
| [com.lihaoyi:sourcecode_2.13](https://central.sonatype.com/artifact/com.lihaoyi/sourcecode_2.13/0.4.0) | 0.4.0 | MIT |
| [org.mockito:mockito-core](https://central.sonatype.com/artifact/org.mockito/mockito-core/5.17.0) | 5.17.0 | MIT |
| [org.mockito:mockito-core](https://central.sonatype.com/artifact/org.mockito/mockito-core/5.20.0) | 5.20.0 | MIT |
| [org.mockito:mockito-junit-jupiter](https://central.sonatype.com/artifact/org.mockito/mockito-junit-jupiter/5.17.0) | 5.17.0 | MIT |
| [org.rnorth.duct-tape:duct-tape](https://central.sonatype.com/artifact/org.rnorth.duct-tape/duct-tape/1.0.8) | 1.0.8 | MIT |
| [org.slf4j:jul-to-slf4j](https://central.sonatype.com/artifact/org.slf4j/jul-to-slf4j/2.0.17) | 2.0.17 | MIT |
| [org.slf4j:slf4j-api](https://central.sonatype.com/artifact/org.slf4j/slf4j-api/2.0.17) | 2.0.17 | MIT |
| [org.slf4j:slf4j-jdk14](https://central.sonatype.com/artifact/org.slf4j/slf4j-jdk14/2.0.17) | 2.0.17 | MIT |
| [org.testcontainers:database-commons](https://central.sonatype.com/artifact/org.testcontainers/database-commons/1.21.4) | 1.21.4 | MIT |
| [org.testcontainers:db2](https://central.sonatype.com/artifact/org.testcontainers/db2/1.21.4) | 1.21.4 | MIT |
| [org.testcontainers:jdbc](https://central.sonatype.com/artifact/org.testcontainers/jdbc/1.21.4) | 1.21.4 | MIT |
| [org.testcontainers:mariadb](https://central.sonatype.com/artifact/org.testcontainers/mariadb/1.21.4) | 1.21.4 | MIT |
| [org.testcontainers:mssqlserver](https://central.sonatype.com/artifact/org.testcontainers/mssqlserver/1.21.4) | 1.21.4 | MIT |
| [org.testcontainers:mysql](https://central.sonatype.com/artifact/org.testcontainers/mysql/1.21.4) | 1.21.4 | MIT |
| [org.testcontainers:oracle-xe](https://central.sonatype.com/artifact/org.testcontainers/oracle-xe/1.21.4) | 1.21.4 | MIT |
| [org.testcontainers:postgresql](https://central.sonatype.com/artifact/org.testcontainers/postgresql/1.21.4) | 1.21.4 | MIT |
| [org.testcontainers:testcontainers](https://central.sonatype.com/artifact/org.testcontainers/testcontainers/1.21.4) | 1.21.4 | MIT |
| [org.javassist:javassist](https://central.sonatype.com/artifact/org.javassist/javassist/3.30.2-GA) | 3.30.2-GA | MPL-1.1, LGPL-2.1-only, Apache-2.0 |
| [com.h2database:h2](https://central.sonatype.com/artifact/com.h2database/h2/2.4.240) | 2.4.240 | MPL-2.0, EPL-1.0 |
| [com.h2database:h2](https://central.sonatype.com/artifact/com.h2database/h2/2.3.232) | 2.3.232 | MPL-2.0, EPL-1.0 |
| [org.graalvm.js:js](https://central.sonatype.com/artifact/org.graalvm.js/js/25.0.2) | 25.0.2 | UPL-1.0, MIT |
| [org.graalvm.js:js-language](https://central.sonatype.com/artifact/org.graalvm.js/js-language/25.0.2) | 25.0.2 | UPL-1.0, MIT |
| [org.graalvm.js:js-scriptengine](https://central.sonatype.com/artifact/org.graalvm.js/js-scriptengine/25.0.2) | 25.0.2 | UPL-1.0 |
| [org.graalvm.polyglot:polyglot](https://central.sonatype.com/artifact/org.graalvm.polyglot/polyglot/25.0.2) | 25.0.2 | UPL-1.0 |
| [org.graalvm.regex:regex](https://central.sonatype.com/artifact/org.graalvm.regex/regex/25.0.2) | 25.0.2 | UPL-1.0 |
| [org.graalvm.sdk:collections](https://central.sonatype.com/artifact/org.graalvm.sdk/collections/25.0.2) | 25.0.2 | UPL-1.0 |
| [org.graalvm.sdk:jniutils](https://central.sonatype.com/artifact/org.graalvm.sdk/jniutils/25.0.2) | 25.0.2 | UPL-1.0 |
| [org.graalvm.sdk:nativeimage](https://central.sonatype.com/artifact/org.graalvm.sdk/nativeimage/25.0.2) | 25.0.2 | UPL-1.0 |
| [org.graalvm.sdk:nativeimage](https://central.sonatype.com/artifact/org.graalvm.sdk/nativeimage/23.1.2) | 23.1.2 | UPL-1.0 |
| [org.graalvm.sdk:word](https://central.sonatype.com/artifact/org.graalvm.sdk/word/25.0.2) | 25.0.2 | UPL-1.0 |
| [org.graalvm.sdk:word](https://central.sonatype.com/artifact/org.graalvm.sdk/word/23.1.2) | 23.1.2 | UPL-1.0 |
| [org.graalvm.shadowed:xz](https://central.sonatype.com/artifact/org.graalvm.shadowed/xz/25.0.2) | 25.0.2 | UPL-1.0 |
| [org.graalvm.truffle:truffle-api](https://central.sonatype.com/artifact/org.graalvm.truffle/truffle-api/25.0.2) | 25.0.2 | UPL-1.0 |
| [org.graalvm.truffle:truffle-compiler](https://central.sonatype.com/artifact/org.graalvm.truffle/truffle-compiler/25.0.2) | 25.0.2 | UPL-1.0 |
| [org.graalvm.truffle:truffle-runtime](https://central.sonatype.com/artifact/org.graalvm.truffle/truffle-runtime/25.0.2) | 25.0.2 | UPL-1.0 |
| [org.graalvm.shadowed:icu4j](https://central.sonatype.com/artifact/org.graalvm.shadowed/icu4j/25.0.2) | 25.0.2 | Unicode-3.0 |


<a name="npm_licenses"></a>
### Operaton Third-Party Node Libraries

The below table lists the third-party Node libraries used in Operaton along with their versions and associated licenses.

Operaton uses Node libraries for the web applications included in the distribution.

| Library | Version | License(s) |
|---------|---------|------------|
| [mousetrap](https://www.npmjs.com/package/mousetrap/v/1.6.5) | 1.6.5 | Apache-2.0 WITH LLVM-exception |
| [htm](https://www.npmjs.com/package/htm/v/3.1.1) | 3.1.1 | Apache-2.0 |
| [@carbon/grid](https://www.npmjs.com/package/@carbon/grid/v/11.42.0) | 11.42.0 | Apache-2.0 |
| [@carbon/layout](https://www.npmjs.com/package/@carbon/layout/v/11.40.0) | 11.40.0 | Apache-2.0 |
| [@ibm/telemetry-js](https://www.npmjs.com/package/@ibm/telemetry-js/v/1.10.1) | 1.10.1 | Apache-2.0 |
| [qs](https://www.npmjs.com/package/qs/v/6.13.0) | 6.13.0 | BSD-3-Clause |
| [dezalgo](https://www.npmjs.com/package/dezalgo/v/1.0.4) | 1.0.4 | ISC |
| [inherits](https://www.npmjs.com/package/inherits/v/2.0.4) | 2.0.4 | ISC |
| [inherits-browser](https://www.npmjs.com/package/inherits-browser/v/0.1.0) | 0.1.0 | ISC |
| [once](https://www.npmjs.com/package/once/v/1.4.0) | 1.4.0 | ISC |
| [semver](https://www.npmjs.com/package/semver/v/7.7.2) | 7.7.2 | ISC |
| [wrappy](https://www.npmjs.com/package/wrappy/v/1.0.2) | 1.0.2 | ISC |
| [angular](https://www.npmjs.com/package/angular/v/1.8.3) | 1.8.3 | MIT |
| [angular-animate](https://www.npmjs.com/package/angular-animate/v/1.8.3) | 1.8.3 | MIT |
| [angular-cookies](https://www.npmjs.com/package/angular-cookies/v/1.8.3) | 1.8.3 | MIT |
| [angular-loader](https://www.npmjs.com/package/angular-loader/v/1.8.3) | 1.8.3 | MIT |
| [angular-moment](https://www.npmjs.com/package/angular-moment/v/1.3.0) | 1.3.0 | MIT |
| [angular-resource](https://www.npmjs.com/package/angular-resource/v/1.8.3) | 1.8.3 | MIT |
| [angular-route](https://www.npmjs.com/package/angular-route/v/1.8.3) | 1.8.3 | MIT |
| [angular-sanitize](https://www.npmjs.com/package/angular-sanitize/v/1.8.3) | 1.8.3 | MIT |
| [angular-touch](https://www.npmjs.com/package/angular-touch/v/1.8.3) | 1.8.3 | MIT |
| [angular-translate](https://www.npmjs.com/package/angular-translate/v/2.19.1) | 2.19.1 | MIT |
| [angular-ui-bootstrap](https://www.npmjs.com/package/angular-ui-bootstrap/v/2.5.6) | 2.5.6 | MIT |
| [array-move](https://www.npmjs.com/package/array-move/v/4.0.0) | 4.0.0 | MIT |
| [asap](https://www.npmjs.com/package/asap/v/2.0.6) | 2.0.6 | MIT |
| [asynckit](https://www.npmjs.com/package/asynckit/v/0.4.0) | 0.4.0 | MIT |
| [atoa](https://www.npmjs.com/package/atoa/v/1.0.0) | 1.0.0 | MIT |
| [big.js](https://www.npmjs.com/package/big.js/v/7.0.1) | 7.0.1 | MIT |
| [bootstrap](https://www.npmjs.com/package/bootstrap/v/3.4.1) | 3.4.1 | MIT |
| [bpmn-moddle](https://www.npmjs.com/package/bpmn-moddle/v/8.1.0) | 8.1.0 | MIT |
| [call-bind-apply-helpers](https://www.npmjs.com/package/call-bind-apply-helpers/v/1.0.2) | 1.0.2 | MIT |
| [call-bound](https://www.npmjs.com/package/call-bound/v/1.0.4) | 1.0.4 | MIT |
| [camunda-dmn-js](https://www.npmjs.com/package/camunda-dmn-js/v/3.2.0) | 3.2.0 | MIT |
| [camunda-dmn-moddle](https://www.npmjs.com/package/camunda-dmn-moddle/v/1.3.0) | 1.3.0 | MIT |
| [chart.js](https://www.npmjs.com/package/chart.js/v/4.5.1) | 4.5.1 | MIT |
| [classnames](https://www.npmjs.com/package/classnames/v/2.5.1) | 2.5.1 | MIT |
| [clipboard](https://www.npmjs.com/package/clipboard/v/2.0.11) | 2.0.11 | MIT |
| [clsx](https://www.npmjs.com/package/clsx/v/2.1.1) | 2.1.1 | MIT |
| [cmmn-moddle](https://www.npmjs.com/package/cmmn-moddle/v/5.0.0) | 5.0.0 | MIT |
| [codemirror](https://www.npmjs.com/package/codemirror/v/6.0.2) | 6.0.2 | MIT |
| [combined-stream](https://www.npmjs.com/package/combined-stream/v/1.0.8) | 1.0.8 | MIT |
| [component-emitter](https://www.npmjs.com/package/component-emitter/v/1.3.1) | 1.3.1 | MIT |
| [component-event](https://www.npmjs.com/package/component-event/v/0.2.1) | 0.2.1 | MIT |
| [component-props](https://www.npmjs.com/package/component-props/v/1.1.1) | 1.1.1 | MIT |
| [component-xor](https://www.npmjs.com/package/component-xor/v/0.0.4) | 0.0.4 | MIT |
| [contra](https://www.npmjs.com/package/contra/v/1.9.4) | 1.9.4 | MIT |
| [cookiejar](https://www.npmjs.com/package/cookiejar/v/2.1.4) | 2.1.4 | MIT |
| [core-js](https://www.npmjs.com/package/core-js/v/3.35.1) | 3.35.1 | MIT |
| [crelt](https://www.npmjs.com/package/crelt/v/1.0.6) | 1.0.6 | MIT |
| [css.escape](https://www.npmjs.com/package/css.escape/v/1.5.1) | 1.5.1 | MIT |
| [debug](https://www.npmjs.com/package/debug/v/4.4.1) | 4.4.1 | MIT |
| [delayed-stream](https://www.npmjs.com/package/delayed-stream/v/1.0.0) | 1.0.0 | MIT |
| [delegate](https://www.npmjs.com/package/delegate/v/3.2.0) | 3.2.0 | MIT |
| [diagram-js](https://www.npmjs.com/package/diagram-js/v/12.8.1) | 12.8.1 | MIT |
| [diagram-js](https://www.npmjs.com/package/diagram-js/v/13.4.0) | 13.4.0 | MIT |
| [diagram-js](https://www.npmjs.com/package/diagram-js/v/15.3.0) | 15.3.0 | MIT |
| [diagram-js](https://www.npmjs.com/package/diagram-js/v/15.4.0) | 15.4.0 | MIT |
| [diagram-js](https://www.npmjs.com/package/diagram-js/v/4.0.3) | 4.0.3 | MIT |
| [diagram-js-direct-editing](https://www.npmjs.com/package/diagram-js-direct-editing/v/1.8.0) | 1.8.0 | MIT |
| [diagram-js-direct-editing](https://www.npmjs.com/package/diagram-js-direct-editing/v/2.1.2) | 2.1.2 | MIT |
| [diagram-js-direct-editing](https://www.npmjs.com/package/diagram-js-direct-editing/v/3.2.0) | 3.2.0 | MIT |
| [diagram-js-grid](https://www.npmjs.com/package/diagram-js-grid/v/1.1.0) | 1.1.0 | MIT |
| [diagram-js-origin](https://www.npmjs.com/package/diagram-js-origin/v/1.4.0) | 1.4.0 | MIT |
| [didi](https://www.npmjs.com/package/didi/v/10.2.2) | 10.2.2 | MIT |
| [didi](https://www.npmjs.com/package/didi/v/4.0.0) | 4.0.0 | MIT |
| [didi](https://www.npmjs.com/package/didi/v/9.0.2) | 9.0.2 | MIT |
| [dmn-js-properties-panel](https://www.npmjs.com/package/dmn-js-properties-panel/v/3.2.1) | 3.2.1 | MIT |
| [dmn-moddle](https://www.npmjs.com/package/dmn-moddle/v/10.0.0) | 10.0.0 | MIT |
| [dmn-moddle](https://www.npmjs.com/package/dmn-moddle/v/11.0.0) | 11.0.0 | MIT |
| [dom-iterator](https://www.npmjs.com/package/dom-iterator/v/1.0.2) | 1.0.2 | MIT |
| [dom4](https://www.npmjs.com/package/dom4/v/2.1.6) | 2.1.6 | MIT |
| [domify](https://www.npmjs.com/package/domify/v/1.4.2) | 1.4.2 | MIT |
| [domify](https://www.npmjs.com/package/domify/v/2.0.0) | 2.0.0 | MIT |
| [downloadjs](https://www.npmjs.com/package/downloadjs/v/1.4.7) | 1.4.7 | MIT |
| [dunder-proto](https://www.npmjs.com/package/dunder-proto/v/1.0.1) | 1.0.1 | MIT |
| [es-define-property](https://www.npmjs.com/package/es-define-property/v/1.0.1) | 1.0.1 | MIT |
| [es-errors](https://www.npmjs.com/package/es-errors/v/1.3.0) | 1.3.0 | MIT |
| [es-object-atoms](https://www.npmjs.com/package/es-object-atoms/v/1.1.1) | 1.1.1 | MIT |
| [es-set-tostringtag](https://www.npmjs.com/package/es-set-tostringtag/v/2.1.0) | 2.1.0 | MIT |
| [escape-html](https://www.npmjs.com/package/escape-html/v/1.0.3) | 1.0.3 | MIT |
| [events](https://www.npmjs.com/package/events/v/3.3.0) | 3.3.0 | MIT |
| [fast-safe-stringify](https://www.npmjs.com/package/fast-safe-stringify/v/2.1.1) | 2.1.1 | MIT |
| [fast-xml-parser](https://www.npmjs.com/package/fast-xml-parser/v/4.3.6) | 4.3.6 | MIT |
| [feelers](https://www.npmjs.com/package/feelers/v/1.4.0) | 1.4.0 | MIT |
| [feelin](https://www.npmjs.com/package/feelin/v/3.2.0) | 3.2.0 | MIT |
| [feelin](https://www.npmjs.com/package/feelin/v/4.3.0) | 4.3.0 | MIT |
| [file-drops](https://www.npmjs.com/package/file-drops/v/0.6.1) | 0.6.1 | MIT |
| [flatpickr](https://www.npmjs.com/package/flatpickr/v/4.6.13) | 4.6.13 | MIT |
| [focus-trap](https://www.npmjs.com/package/focus-trap/v/7.6.5) | 7.6.5 | MIT |
| [form-data](https://www.npmjs.com/package/form-data/v/4.0.4) | 4.0.4 | MIT |
| [formidable](https://www.npmjs.com/package/formidable/v/2.1.5) | 2.1.5 | MIT |
| [function-bind](https://www.npmjs.com/package/function-bind/v/1.1.2) | 1.1.2 | MIT |
| [get-intrinsic](https://www.npmjs.com/package/get-intrinsic/v/1.3.0) | 1.3.0 | MIT |
| [get-proto](https://www.npmjs.com/package/get-proto/v/1.0.1) | 1.0.1 | MIT |
| [good-listener](https://www.npmjs.com/package/good-listener/v/1.2.2) | 1.2.2 | MIT |
| [gopd](https://www.npmjs.com/package/gopd/v/1.2.0) | 1.2.0 | MIT |
| [hammerjs](https://www.npmjs.com/package/hammerjs/v/2.0.8) | 2.0.8 | MIT |
| [has-symbols](https://www.npmjs.com/package/has-symbols/v/1.1.0) | 1.1.0 | MIT |
| [has-tostringtag](https://www.npmjs.com/package/has-tostringtag/v/1.0.2) | 1.0.2 | MIT |
| [hasown](https://www.npmjs.com/package/hasown/v/2.0.2) | 2.0.2 | MIT |
| [ids](https://www.npmjs.com/package/ids/v/0.2.2) | 0.2.2 | MIT |
| [ids](https://www.npmjs.com/package/ids/v/1.0.5) | 1.0.5 | MIT |
| [indexof](https://www.npmjs.com/package/indexof/v/0.0.1) | 0.0.1 | MIT |
| [inferno](https://www.npmjs.com/package/inferno/v/5.6.3) | 5.6.3 | MIT |
| [inferno-shared](https://www.npmjs.com/package/inferno-shared/v/5.6.3) | 5.6.3 | MIT |
| [inferno-vnode-flags](https://www.npmjs.com/package/inferno-vnode-flags/v/5.6.3) | 5.6.3 | MIT |
| [jquery](https://www.npmjs.com/package/jquery/v/3.7.1) | 3.7.1 | MIT |
| [jquery-ui](https://www.npmjs.com/package/jquery-ui/v/1.13.3) | 1.13.3 | MIT |
| [lezer-feel](https://www.npmjs.com/package/lezer-feel/v/1.8.1) | 1.8.1 | MIT |
| [lodash](https://www.npmjs.com/package/lodash/v/4.17.21) | 4.17.21 | MIT |
| [luxon](https://www.npmjs.com/package/luxon/v/3.7.1) | 3.7.1 | MIT |
| [marked](https://www.npmjs.com/package/marked/v/16.2.1) | 16.2.1 | MIT |
| [matches-selector](https://www.npmjs.com/package/matches-selector/v/1.2.0) | 1.2.0 | MIT |
| [math-intrinsics](https://www.npmjs.com/package/math-intrinsics/v/1.1.0) | 1.1.0 | MIT |
| [methods](https://www.npmjs.com/package/methods/v/1.1.2) | 1.1.2 | MIT |
| [mime](https://www.npmjs.com/package/mime/v/2.6.0) | 2.6.0 | MIT |
| [mime-db](https://www.npmjs.com/package/mime-db/v/1.52.0) | 1.52.0 | MIT |
| [mime-types](https://www.npmjs.com/package/mime-types/v/2.1.35) | 2.1.35 | MIT |
| [min-dash](https://www.npmjs.com/package/min-dash/v/3.8.1) | 3.8.1 | MIT |
| [min-dash](https://www.npmjs.com/package/min-dash/v/4.2.3) | 4.2.3 | MIT |
| [min-dom](https://www.npmjs.com/package/min-dom/v/3.2.1) | 3.2.1 | MIT |
| [min-dom](https://www.npmjs.com/package/min-dom/v/4.2.1) | 4.2.1 | MIT |
| [min-dom](https://www.npmjs.com/package/min-dom/v/5.1.1) | 5.1.1 | MIT |
| [mitt](https://www.npmjs.com/package/mitt/v/3.0.1) | 3.0.1 | MIT |
| [moddle](https://www.npmjs.com/package/moddle/v/4.1.0) | 4.1.0 | MIT |
| [moddle](https://www.npmjs.com/package/moddle/v/5.0.4) | 5.0.4 | MIT |
| [moddle](https://www.npmjs.com/package/moddle/v/6.2.3) | 6.2.3 | MIT |
| [moddle](https://www.npmjs.com/package/moddle/v/7.2.0) | 7.2.0 | MIT |
| [moddle-xml](https://www.npmjs.com/package/moddle-xml/v/10.1.0) | 10.1.0 | MIT |
| [moddle-xml](https://www.npmjs.com/package/moddle-xml/v/11.0.0) | 11.0.0 | MIT |
| [moddle-xml](https://www.npmjs.com/package/moddle-xml/v/7.5.0) | 7.5.0 | MIT |
| [moddle-xml](https://www.npmjs.com/package/moddle-xml/v/9.0.6) | 9.0.6 | MIT |
| [moment](https://www.npmjs.com/package/moment/v/2.30.1) | 2.30.1 | MIT |
| [ms](https://www.npmjs.com/package/ms/v/2.1.3) | 2.1.3 | MIT |
| [object-inspect](https://www.npmjs.com/package/object-inspect/v/1.13.4) | 1.13.4 | MIT |
| [object-refs](https://www.npmjs.com/package/object-refs/v/0.3.0) | 0.3.0 | MIT |
| [object-refs](https://www.npmjs.com/package/object-refs/v/0.4.0) | 0.4.0 | MIT |
| [opencollective-postinstall](https://www.npmjs.com/package/opencollective-postinstall/v/2.0.3) | 2.0.3 | MIT |
| [path-intersection](https://www.npmjs.com/package/path-intersection/v/1.1.1) | 1.1.1 | MIT |
| [path-intersection](https://www.npmjs.com/package/path-intersection/v/2.2.1) | 2.2.1 | MIT |
| [path-intersection](https://www.npmjs.com/package/path-intersection/v/3.1.0) | 3.1.0 | MIT |
| [preact](https://www.npmjs.com/package/preact/v/10.27.1) | 10.27.1 | MIT |
| [q](https://www.npmjs.com/package/q/v/1.5.1) | 1.5.1 | MIT |
| [requirejs](https://www.npmjs.com/package/requirejs/v/2.1.22) | 2.1.22 | MIT |
| [requirejs](https://www.npmjs.com/package/requirejs/v/2.3.8) | 2.3.8 | MIT |
| [requirejs-angular-define](https://www.npmjs.com/package/requirejs-angular-define/v/1.1.0) | 1.1.0 | MIT |
| [saxen](https://www.npmjs.com/package/saxen/v/10.0.0) | 10.0.0 | MIT |
| [saxen](https://www.npmjs.com/package/saxen/v/8.1.2) | 8.1.2 | MIT |
| [select](https://www.npmjs.com/package/select/v/1.1.2) | 1.1.2 | MIT |
| [selection-ranges](https://www.npmjs.com/package/selection-ranges/v/3.0.3) | 3.0.3 | MIT |
| [selection-ranges](https://www.npmjs.com/package/selection-ranges/v/4.0.3) | 4.0.3 | MIT |
| [selection-update](https://www.npmjs.com/package/selection-update/v/0.1.2) | 0.1.2 | MIT |
| [side-channel](https://www.npmjs.com/package/side-channel/v/1.1.0) | 1.1.0 | MIT |
| [side-channel-list](https://www.npmjs.com/package/side-channel-list/v/1.0.0) | 1.0.0 | MIT |
| [side-channel-map](https://www.npmjs.com/package/side-channel-map/v/1.0.1) | 1.0.1 | MIT |
| [side-channel-weakmap](https://www.npmjs.com/package/side-channel-weakmap/v/1.0.2) | 1.0.2 | MIT |
| [strnum](https://www.npmjs.com/package/strnum/v/1.1.2) | 1.1.2 | MIT |
| [style-mod](https://www.npmjs.com/package/style-mod/v/4.1.2) | 4.1.2 | MIT |
| [superagent](https://www.npmjs.com/package/superagent/v/8.1.2) | 8.1.2 | MIT |
| [tabbable](https://www.npmjs.com/package/tabbable/v/6.2.0) | 6.2.0 | MIT |
| [table-js](https://www.npmjs.com/package/table-js/v/9.2.0) | 9.2.0 | MIT |
| [ticky](https://www.npmjs.com/package/ticky/v/1.0.1) | 1.0.1 | MIT |
| [tiny-emitter](https://www.npmjs.com/package/tiny-emitter/v/2.1.0) | 2.1.0 | MIT |
| [tiny-svg](https://www.npmjs.com/package/tiny-svg/v/2.2.4) | 2.2.4 | MIT |
| [tiny-svg](https://www.npmjs.com/package/tiny-svg/v/3.1.3) | 3.1.3 | MIT |
| [w3c-keyname](https://www.npmjs.com/package/w3c-keyname/v/2.2.8) | 2.2.8 | MIT |
| [zeebe-dmn-moddle](https://www.npmjs.com/package/zeebe-dmn-moddle/v/1.0.0) | 1.0.0 | MIT |
| [@bpmn-io/align-to-origin](https://www.npmjs.com/package/@bpmn-io/align-to-origin/v/0.7.0) | 0.7.0 | MIT |
| [@bpmn-io/cm-theme](https://www.npmjs.com/package/@bpmn-io/cm-theme/v/0.1.0-alpha.2) | 0.1.0-alpha.2 | MIT |
| [@bpmn-io/diagram-js-ui](https://www.npmjs.com/package/@bpmn-io/diagram-js-ui/v/0.2.3) | 0.2.3 | MIT |
| [@bpmn-io/dmn-migrate](https://www.npmjs.com/package/@bpmn-io/dmn-migrate/v/0.5.0) | 0.5.0 | MIT |
| [@bpmn-io/dmn-variable-resolver](https://www.npmjs.com/package/@bpmn-io/dmn-variable-resolver/v/0.7.0) | 0.7.0 | MIT |
| [@bpmn-io/draggle](https://www.npmjs.com/package/@bpmn-io/draggle/v/4.1.2) | 4.1.2 | MIT |
| [@bpmn-io/feel-editor](https://www.npmjs.com/package/@bpmn-io/feel-editor/v/1.12.0) | 1.12.0 | MIT |
| [@bpmn-io/feel-lint](https://www.npmjs.com/package/@bpmn-io/feel-lint/v/1.4.0) | 1.4.0 | MIT |
| [@bpmn-io/feel-lint](https://www.npmjs.com/package/@bpmn-io/feel-lint/v/2.1.0) | 2.1.0 | MIT |
| [@bpmn-io/lang-feel](https://www.npmjs.com/package/@bpmn-io/lang-feel/v/2.4.0) | 2.4.0 | MIT |
| [@bpmn-io/lezer-feel](https://www.npmjs.com/package/@bpmn-io/lezer-feel/v/1.9.0) | 1.9.0 | MIT |
| [@bpmn-io/properties-panel](https://www.npmjs.com/package/@bpmn-io/properties-panel/v/3.33.0) | 3.33.0 | MIT |
| [@camunda/feel-builtins](https://www.npmjs.com/package/@camunda/feel-builtins/v/0.2.0) | 0.2.0 | MIT |
| [@codemirror/autocomplete](https://www.npmjs.com/package/@codemirror/autocomplete/v/6.18.6) | 6.18.6 | MIT |
| [@codemirror/commands](https://www.npmjs.com/package/@codemirror/commands/v/6.8.1) | 6.8.1 | MIT |
| [@codemirror/lang-json](https://www.npmjs.com/package/@codemirror/lang-json/v/6.0.2) | 6.0.2 | MIT |
| [@codemirror/language](https://www.npmjs.com/package/@codemirror/language/v/6.11.3) | 6.11.3 | MIT |
| [@codemirror/lint](https://www.npmjs.com/package/@codemirror/lint/v/6.8.5) | 6.8.5 | MIT |
| [@codemirror/search](https://www.npmjs.com/package/@codemirror/search/v/6.5.11) | 6.5.11 | MIT |
| [@codemirror/state](https://www.npmjs.com/package/@codemirror/state/v/6.5.2) | 6.5.2 | MIT |
| [@codemirror/view](https://www.npmjs.com/package/@codemirror/view/v/6.38.1) | 6.38.1 | MIT |
| [@kurkle/color](https://www.npmjs.com/package/@kurkle/color/v/0.3.4) | 0.3.4 | MIT |
| [@lezer/common](https://www.npmjs.com/package/@lezer/common/v/1.2.3) | 1.2.3 | MIT |
| [@lezer/highlight](https://www.npmjs.com/package/@lezer/highlight/v/1.2.1) | 1.2.1 | MIT |
| [@lezer/json](https://www.npmjs.com/package/@lezer/json/v/1.0.3) | 1.0.3 | MIT |
| [@lezer/lr](https://www.npmjs.com/package/@lezer/lr/v/1.4.2) | 1.4.2 | MIT |
| [@lezer/markdown](https://www.npmjs.com/package/@lezer/markdown/v/1.4.3) | 1.4.3 | MIT |
| [@marijn/find-cluster-break](https://www.npmjs.com/package/@marijn/find-cluster-break/v/1.0.2) | 1.0.2 | MIT |
| [@noble/hashes](https://www.npmjs.com/package/@noble/hashes/v/1.8.0) | 1.8.0 | MIT |
| [@paralleldrive/cuid2](https://www.npmjs.com/package/@paralleldrive/cuid2/v/2.2.2) | 2.2.2 | MIT |
| [@types/trusted-types](https://www.npmjs.com/package/@types/trusted-types/v/2.0.7) | 2.0.7 | MIT |
| [dompurify](https://www.npmjs.com/package/dompurify/v/3.2.7) | 3.2.7 | MPL-2.0 OR Apache-2.0 |
| [bpmn-font](https://www.npmjs.com/package/bpmn-font/v/0.12.1) | 0.12.1 | OFL-1.1 |
| [cmmn-font](https://www.npmjs.com/package/cmmn-font/v/0.5.0) | 0.5.0 | OFL-1.1 |
| [dmn-font](https://www.npmjs.com/package/dmn-font/v/0.6.2) | 0.6.2 | OFL-1.1 |
| [@ibm/plex](https://www.npmjs.com/package/@ibm/plex/v/6.4.1) | 6.4.1 | OFL-1.1 |
| [angular-data-depend](https://www.npmjs.com/package/angular-data-depend/v/1.0.3) | 1.0.3 | UNKNOWN |
| [component-event](https://www.npmjs.com/package/component-event/v/0.1.4) | 0.1.4 | UNKNOWN |
| [hat](https://www.npmjs.com/package/hat/v/0.0.3) | 0.0.3 | X11 |
| [bpmn-js](https://www.npmjs.com/package/bpmn-js/v/16.5.0) | 16.5.0 | bpmn.io |
| [cmmn-js](https://www.npmjs.com/package/cmmn-js/v/0.20.0) | 0.20.0 | bpmn.io |
| [dmn-js](https://www.npmjs.com/package/dmn-js/v/17.2.1) | 17.2.1 | bpmn.io |
| [dmn-js-boxed-expression](https://www.npmjs.com/package/dmn-js-boxed-expression/v/17.4.0) | 17.4.0 | bpmn.io |
| [dmn-js-decision-table](https://www.npmjs.com/package/dmn-js-decision-table/v/17.1.0) | 17.1.0 | bpmn.io |
| [dmn-js-decision-table](https://www.npmjs.com/package/dmn-js-decision-table/v/17.4.0) | 17.4.0 | bpmn.io |
| [dmn-js-drd](https://www.npmjs.com/package/dmn-js-drd/v/17.2.1) | 17.2.1 | bpmn.io |
| [dmn-js-literal-expression](https://www.npmjs.com/package/dmn-js-literal-expression/v/17.1.0) | 17.1.0 | bpmn.io |
| [dmn-js-literal-expression](https://www.npmjs.com/package/dmn-js-literal-expression/v/17.4.0) | 17.4.0 | bpmn.io |
| [dmn-js-shared](https://www.npmjs.com/package/dmn-js-shared/v/17.1.0) | 17.1.0 | bpmn.io |
| [dmn-js-shared](https://www.npmjs.com/package/dmn-js-shared/v/17.4.0) | 17.4.0 | bpmn.io |
| [@bpmn-io/form-js](https://www.npmjs.com/package/@bpmn-io/form-js/v/1.8.8) | 1.8.8 | bpmn.io |
| [@bpmn-io/form-js-carbon-styles](https://www.npmjs.com/package/@bpmn-io/form-js-carbon-styles/v/1.17.0) | 1.17.0 | bpmn.io |
| [@bpmn-io/form-js-editor](https://www.npmjs.com/package/@bpmn-io/form-js-editor/v/1.17.0) | 1.17.0 | bpmn.io |
| [@bpmn-io/form-js-playground](https://www.npmjs.com/package/@bpmn-io/form-js-playground/v/1.17.0) | 1.17.0 | bpmn.io |
| [@bpmn-io/form-js-viewer](https://www.npmjs.com/package/@bpmn-io/form-js-viewer/v/1.17.0) | 1.17.0 | bpmn.io |

<a name="licenses"></a>
## License Texts

---
#### <a name="apache-2.0"></a>Apache-2.0
The following license text for the Apache-2.0 license is cited only once.

```

                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      &quot;License&quot; shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      &quot;Licensor&quot; shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      &quot;Legal Entity&quot; shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      &quot;control&quot; means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      &quot;You&quot; (or &quot;Your&quot;) shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      &quot;Source&quot; form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      &quot;Object&quot; form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      &quot;Work&quot; shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      &quot;Derivative Works&quot; shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      &quot;Contribution&quot; shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, &quot;submitted&quot;
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as &quot;Not a Contribution.&quot;

      &quot;Contributor&quot; shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a &quot;NOTICE&quot; text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an &quot;AS IS&quot; BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets &quot;[]&quot;
      replaced with your own identifying information. (Don&#x27;t include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same &quot;printed page&quot; as the copyright notice for easier
      identification within third-party archives.

   Copyright [yyyy] [name of copyright owner]

   Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

```
---
#### <a name="upl-1.0"></a>UPL-1.0
The following license text for the UPL-1.0 license is cited only once.

```
Copyright (c) &lt;year&gt; &lt;copyright holders&gt;

The Universal Permissive License (UPL), Version 1.0

Subject to the condition set forth below, permission is hereby granted to any person obtaining a copy of this software, associated documentation and/or data (collectively the &quot;Software&quot;), free of charge and under any and all copyright rights in the Software, and any and all patent rights owned or freely licensable by each licensor hereunder covering either (i) the unmodified Software as contributed to or provided by such licensor, or (ii) the Larger Works (as defined below), to deal in both

(a) the Software, and

(b) any piece of software and/or hardware listed in the lrgrwrks.txt file if one is included with the Software (each a “Larger Work” to which the Software is contributed by such licensors),

without restriction, including without limitation the rights to copy, create derivative works of, display, perform, and distribute the Software and make, use, sell, offer for sale, import, export, have made, and have sold the Software and the Larger Work(s), and to sublicense the foregoing rights on either these or other terms.

This license is subject to the following condition:

The above copyright notice and either this complete permission notice or at a minimum a reference to the UPL must be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED &quot;AS IS&quot;, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

```
---
#### <a name="bsd-3-clause"></a>BSD-3-Clause
The following license text for the BSD-3-Clause license is cited only once.

```
BSD 3-Clause License

Copyright &lt;YEAR&gt; &lt;COPYRIGHT HOLDER&gt;

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS &quot;AS IS&quot;
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


```
---
#### <a name="epl-2.0"></a>EPL-2.0
The following license text for the EPL-2.0 license is cited only once.

```
Eclipse Public License - v 2.0
THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE PUBLIC LICENSE (“AGREEMENT”). ANY USE, REPRODUCTION OR DISTRIBUTION OF THE PROGRAM CONSTITUTES RECIPIENT&#x27;S ACCEPTANCE OF THIS AGREEMENT.
1. DEFINITIONS
“Contribution” means:
a) in the case of the initial Contributor, the initial content Distributed under this Agreement, and
b) in the case of each subsequent Contributor:
i) changes to the Program, and
ii) additions to the Program;
where such changes and/or additions to the Program originate from and are Distributed by that particular Contributor. A Contribution “originates” from a Contributor if it was added to the Program by such Contributor itself or anyone acting on such Contributor&#x27;s behalf. Contributions do not include changes or additions to the Program that are not Modified Works.
“Contributor” means any person or entity that Distributes the Program.
“Licensed Patents” mean patent claims licensable by a Contributor which are necessarily infringed by the use or sale of its Contribution alone or when combined with the Program.
“Program” means the Contributions Distributed in accordance with this Agreement.
“Recipient” means anyone who receives the Program under this Agreement or any Secondary License (as applicable), including Contributors.
“Derivative Works” shall mean any work, whether in Source Code or other form, that is based on (or derived from) the Program and for which the editorial revisions, annotations, elaborations, or other modifications represent, as a whole, an original work of authorship.
“Modified Works” shall mean any work in Source Code or other form that results from an addition to, deletion from, or modification of the contents of the Program, including, for purposes of clarity any new file in Source Code form that contains any contents of the Program. Modified Works shall not include works that contain only declarations, interfaces, types, classes, structures, or files of the Program solely in each case in order to link to, bind by name, or subclass the Program or Modified Works thereof.
“Distribute” means the acts of a) distributing or b) making available in any manner that enables the transfer of a copy.
“Source Code” means the form of a Program preferred for making modifications, including but not limited to software source code, documentation source, and configuration files.
“Secondary License” means either the GNU General Public License, Version 2.0, or any later versions of that license, including any exceptions or additional permissions as identified by the initial Contributor.
2. GRANT OF RIGHTS
a) Subject to the terms of this Agreement, each Contributor hereby grants Recipient a non-exclusive, worldwide, royalty-free copyright license to reproduce, prepare Derivative Works of, publicly display, publicly perform, Distribute and sublicense the Contribution of such Contributor, if any, and such Derivative Works.
b) Subject to the terms of this Agreement, each Contributor hereby grants Recipient a non-exclusive, worldwide, royalty-free patent license under Licensed Patents to make, use, sell, offer to sell, import and otherwise transfer the Contribution of such Contributor, if any, in Source Code or other form. This patent license shall apply to the combination of the Contribution and the Program if, at the time the Contribution is added by the Contributor, such addition of the Contribution causes such combination to be covered by the Licensed Patents. The patent license shall not apply to any other combinations which include the Contribution. No hardware per se is licensed hereunder.
c) Recipient understands that although each Contributor grants the licenses to its Contributions set forth herein, no assurances are provided by any Contributor that the Program does not infringe the patent or other intellectual property rights of any other entity. Each Contributor disclaims any liability to Recipient for claims brought by any other entity based on infringement of intellectual property rights or otherwise. As a condition to exercising the rights and licenses granted hereunder, each Recipient hereby assumes sole responsibility to secure any other intellectual property rights needed, if any. For example, if a third party patent license is required to allow Recipient to Distribute the Program, it is Recipient&#x27;s responsibility to acquire that license before distributing the Program.
d) Each Contributor represents that to its knowledge it has sufficient copyright rights in its Contribution, if any, to grant the copyright license set forth in this Agreement.
e) Notwithstanding the terms of any Secondary License, no Contributor makes additional grants to any Recipient (other than those set forth in this Agreement) as a result of such Recipient&#x27;s receipt of the Program under the terms of a Secondary License (if permitted under the terms of Section 3).
3. REQUIREMENTS
3.1 If a Contributor Distributes the Program in any form, then:
a) the Program must also be made available as Source Code, in accordance with section 3.2, and the Contributor must accompany the Program with a statement that the Source Code for the Program is available under this Agreement, and informs Recipients how to obtain it in a reasonable manner on or through a medium customarily used for software exchange; and
b) the Contributor may Distribute the Program under a license different than this Agreement, provided that such license:
i) effectively disclaims on behalf of all other Contributors all warranties and conditions, express and implied, including warranties or conditions of title and non-infringement, and implied warranties or conditions of merchantability and fitness for a particular purpose;
ii) effectively excludes on behalf of all other Contributors all liability for damages, including direct, indirect, special, incidental and consequential damages, such as lost profits;
iii) does not attempt to limit or alter the recipients&#x27; rights in the Source Code under section 3.2; and
iv) requires any subsequent distribution of the Program by any party to be under a license that satisfies the requirements of this section 3.
3.2 When the Program is Distributed as Source Code:
a) it must be made available under this Agreement, or if the Program (i) is combined with other material in a separate file or files made available under a Secondary License, and (ii) the initial Contributor attached to the Source Code the notice described in Exhibit A of this Agreement, then the Program may be made available under the terms of such Secondary Licenses, and
b) a copy of this Agreement must be included with each copy of the Program.
3.3 Contributors may not remove or alter any copyright, patent, trademark, attribution notices, disclaimers of warranty, or limitations of liability (‘notices’) contained within the Program from any copy of the Program which they Distribute, provided that Contributors may add their own appropriate notices.
4. COMMERCIAL DISTRIBUTION
Commercial distributors of software may accept certain responsibilities with respect to end users, business partners and the like. While this license is intended to facilitate the commercial use of the Program, the Contributor who includes the Program in a commercial product offering should do so in a manner which does not create potential liability for other Contributors. Therefore, if a Contributor includes the Program in a commercial product offering, such Contributor (“Commercial Contributor”) hereby agrees to defend and indemnify every other Contributor (“Indemnified Contributor”) against any losses, damages and costs (collectively “Losses”) arising from claims, lawsuits and other legal actions brought by a third party against the Indemnified Contributor to the extent caused by the acts or omissions of such Commercial Contributor in connection with its distribution of the Program in a commercial product offering. The obligations in this section do not apply to any claims or Losses relating to any actual or alleged intellectual property infringement. In order to qualify, an Indemnified Contributor must: a) promptly notify the Commercial Contributor in writing of such claim, and b) allow the Commercial Contributor to control, and cooperate with the Commercial Contributor in, the defense and any related settlement negotiations. The Indemnified Contributor may participate in any such claim at its own expense.
For example, a Contributor might include the Program in a commercial product offering, Product X. That Contributor is then a Commercial Contributor. If that Commercial Contributor then makes performance claims, or offers warranties related to Product X, those performance claims and warranties are such Commercial Contributor&#x27;s responsibility alone. Under this section, the Commercial Contributor would have to defend claims against the other Contributors related to those performance claims and warranties, and if a court requires any other Contributor to pay any damages as a result, the Commercial Contributor must pay those damages.
5. NO WARRANTY
EXCEPT AS EXPRESSLY SET FORTH IN THIS AGREEMENT, AND TO THE EXTENT PERMITTED BY APPLICABLE LAW, THE PROGRAM IS PROVIDED ON AN “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED INCLUDING, WITHOUT LIMITATION, ANY WARRANTIES OR CONDITIONS OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Each Recipient is solely responsible for determining the appropriateness of using and distributing the Program and assumes all risks associated with its exercise of rights under this Agreement, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and unavailability or interruption of operations.
6. DISCLAIMER OF LIABILITY
EXCEPT AS EXPRESSLY SET FORTH IN THIS AGREEMENT, AND TO THE EXTENT PERMITTED BY APPLICABLE LAW, NEITHER RECIPIENT NOR ANY CONTRIBUTORS SHALL HAVE ANY LIABILITY FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING WITHOUT LIMITATION LOST PROFITS), HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OR DISTRIBUTION OF THE PROGRAM OR THE EXERCISE OF ANY RIGHTS GRANTED HEREUNDER, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
7. GENERAL
If any provision of this Agreement is invalid or unenforceable under applicable law, it shall not affect the validity or enforceability of the remainder of the terms of this Agreement, and without further action by the parties hereto, such provision shall be reformed to the minimum extent necessary to make such provision valid and enforceable.
If Recipient institutes patent litigation against any entity (including a cross-claim or counterclaim in a lawsuit) alleging that the Program itself (excluding combinations of the Program with other software or hardware) infringes such Recipient&#x27;s patent(s), then such Recipient&#x27;s rights granted under Section 2(b) shall terminate as of the date such litigation is filed.
All Recipient&#x27;s rights under this Agreement shall terminate if it fails to comply with any of the material terms or conditions of this Agreement and does not cure such failure in a reasonable period of time after becoming aware of such noncompliance. If all Recipient&#x27;s rights under this Agreement terminate, Recipient agrees to cease use and distribution of the Program as soon as reasonably practicable. However, Recipient&#x27;s obligations under this Agreement and any licenses granted by Recipient relating to the Program shall continue and survive.
Everyone is permitted to copy and distribute copies of this Agreement, but in order to avoid inconsistency the Agreement is copyrighted and may only be modified in the following manner. The Agreement Steward reserves the right to publish new versions (including revisions) of this Agreement from time to time. No one other than the Agreement Steward has the right to modify this Agreement. The Eclipse Foundation is the initial Agreement Steward. The Eclipse Foundation may assign the responsibility to serve as the Agreement Steward to a suitable separate entity. Each new version of the Agreement will be given a distinguishing version number. The Program (including Contributions) may always be Distributed subject to the version of the Agreement under which it was received. In addition, after a new version of the Agreement is published, Contributor may elect to Distribute the Program (including its Contributions) under the new version.
Except as expressly stated in Sections 2(a) and 2(b) above, Recipient receives no rights or licenses to the intellectual property of any Contributor under this Agreement, whether expressly, by implication, estoppel or otherwise. All rights in the Program not expressly granted under this Agreement are reserved. Nothing in this Agreement is intended to be enforceable by any entity that is not a Contributor or Recipient. No third-party beneficiary rights are created under this Agreement.
Exhibit A – Form of Secondary Licenses Notice
“This Source Code may also be made available under the following Secondary Licenses when the conditions for such availability set forth in the Eclipse Public License, v. 2.0 are satisfied: {name license(s), version(s), and exceptions or additional permissions here}.”
Simply including a copy of this Agreement, including this Exhibit A is not sufficient to license the Source Code under Secondary Licenses.
If it is not possible or desirable to put the notice in a particular file, then You may include the notice in a location (such as a LICENSE file in a relevant directory) where a recipient would be likely to look for such a notice.
You may add additional accurate notices of copyright ownership.

```
---
#### <a name="cc0-1.0"></a>CC0-1.0
The following license text for the CC0-1.0 license is cited only once.

```
Creative Commons Legal Code

CC0 1.0 Universal

    CREATIVE COMMONS CORPORATION IS NOT A LAW FIRM AND DOES NOT PROVIDE
    LEGAL SERVICES. DISTRIBUTION OF THIS DOCUMENT DOES NOT CREATE AN
    ATTORNEY-CLIENT RELATIONSHIP. CREATIVE COMMONS PROVIDES THIS
    INFORMATION ON AN &quot;AS-IS&quot; BASIS. CREATIVE COMMONS MAKES NO WARRANTIES
    REGARDING THE USE OF THIS DOCUMENT OR THE INFORMATION OR WORKS
    PROVIDED HEREUNDER, AND DISCLAIMS LIABILITY FOR DAMAGES RESULTING FROM
    THE USE OF THIS DOCUMENT OR THE INFORMATION OR WORKS PROVIDED
    HEREUNDER.

Statement of Purpose

The laws of most jurisdictions throughout the world automatically confer
exclusive Copyright and Related Rights (defined below) upon the creator
and subsequent owner(s) (each and all, an &quot;owner&quot;) of an original work of
authorship and/or a database (each, a &quot;Work&quot;).

Certain owners wish to permanently relinquish those rights to a Work for
the purpose of contributing to a commons of creative, cultural and
scientific works (&quot;Commons&quot;) that the public can reliably and without fear
of later claims of infringement build upon, modify, incorporate in other
works, reuse and redistribute as freely as possible in any form whatsoever
and for any purposes, including without limitation commercial purposes.
These owners may contribute to the Commons to promote the ideal of a free
culture and the further production of creative, cultural and scientific
works, or to gain reputation or greater distribution for their Work in
part through the use and efforts of others.

For these and/or other purposes and motivations, and without any
expectation of additional consideration or compensation, the person
associating CC0 with a Work (the &quot;Affirmer&quot;), to the extent that he or she
is an owner of Copyright and Related Rights in the Work, voluntarily
elects to apply CC0 to the Work and publicly distribute the Work under its
terms, with knowledge of his or her Copyright and Related Rights in the
Work and the meaning and intended legal effect of CC0 on those rights.

1. Copyright and Related Rights. A Work made available under CC0 may be
protected by copyright and related or neighboring rights (&quot;Copyright and
Related Rights&quot;). Copyright and Related Rights include, but are not
limited to, the following:

  i. the right to reproduce, adapt, distribute, perform, display,
     communicate, and translate a Work;
 ii. moral rights retained by the original author(s) and/or performer(s);
iii. publicity and privacy rights pertaining to a person&#x27;s image or
     likeness depicted in a Work;
 iv. rights protecting against unfair competition in regards to a Work,
     subject to the limitations in paragraph 4(a), below;
  v. rights protecting the extraction, dissemination, use and reuse of data
     in a Work;
 vi. database rights (such as those arising under Directive 96/9/EC of the
     European Parliament and of the Council of 11 March 1996 on the legal
     protection of databases, and under any national implementation
     thereof, including any amended or successor version of such
     directive); and
vii. other similar, equivalent or corresponding rights throughout the
     world based on applicable law or treaty, and any national
     implementations thereof.

2. Waiver. To the greatest extent permitted by, but not in contravention
of, applicable law, Affirmer hereby overtly, fully, permanently,
irrevocably and unconditionally waives, abandons, and surrenders all of
Affirmer&#x27;s Copyright and Related Rights and associated claims and causes
of action, whether now known or unknown (including existing as well as
future claims and causes of action), in the Work (i) in all territories
worldwide, (ii) for the maximum duration provided by applicable law or
treaty (including future time extensions), (iii) in any current or future
medium and for any number of copies, and (iv) for any purpose whatsoever,
including without limitation commercial, advertising or promotional
purposes (the &quot;Waiver&quot;). Affirmer makes the Waiver for the benefit of each
member of the public at large and to the detriment of Affirmer&#x27;s heirs and
successors, fully intending that such Waiver shall not be subject to
revocation, rescission, cancellation, termination, or any other legal or
equitable action to disrupt the quiet enjoyment of the Work by the public
as contemplated by Affirmer&#x27;s express Statement of Purpose.

3. Public License Fallback. Should any part of the Waiver for any reason
be judged legally invalid or ineffective under applicable law, then the
Waiver shall be preserved to the maximum extent permitted taking into
account Affirmer&#x27;s express Statement of Purpose. In addition, to the
extent the Waiver is so judged Affirmer hereby grants to each affected
person a royalty-free, non transferable, non sublicensable, non exclusive,
irrevocable and unconditional license to exercise Affirmer&#x27;s Copyright and
Related Rights in the Work (i) in all territories worldwide, (ii) for the
maximum duration provided by applicable law or treaty (including future
time extensions), (iii) in any current or future medium and for any number
of copies, and (iv) for any purpose whatsoever, including without
limitation commercial, advertising or promotional purposes (the
&quot;License&quot;). The License shall be deemed effective as of the date CC0 was
applied by Affirmer to the Work. Should any part of the License for any
reason be judged legally invalid or ineffective under applicable law, such
partial invalidity or ineffectiveness shall not invalidate the remainder
of the License, and in such case Affirmer hereby affirms that he or she
will not (i) exercise any of his or her remaining Copyright and Related
Rights in the Work or (ii) assert any associated claims and causes of
action with respect to the Work, in either case contrary to Affirmer&#x27;s
express Statement of Purpose.

4. Limitations and Disclaimers.

 a. No trademark or patent rights held by Affirmer are waived, abandoned,
    surrendered, licensed or otherwise affected by this document.
 b. Affirmer offers the Work as-is and makes no representations or
    warranties of any kind concerning the Work, express, implied,
    statutory or otherwise, including without limitation warranties of
    title, merchantability, fitness for a particular purpose, non
    infringement, or the absence of latent or other defects, accuracy, or
    the present or absence of errors, whether or not discoverable, all to
    the greatest extent permissible under applicable law.
 c. Affirmer disclaims responsibility for clearing rights of other persons
    that may apply to the Work or any use thereof, including without
    limitation any person&#x27;s Copyright and Related Rights in the Work.
    Further, Affirmer disclaims responsibility for obtaining any necessary
    consents, permissions or other rights required for any use of the
    Work.
 d. Affirmer understands and acknowledges that Creative Commons is not a
    party to this document and has no duty or obligation with respect to
    this CC0 or use of the Work.

```
---
#### <a name="w3c-19990505"></a>W3C-19990505
The following license text for the W3C-19990505 license is cited only once.

```
W3C® DOCUMENT NOTICE AND LICENSE
Copyright © 1994-2002 World Wide Web Consortium, (Massachusetts Institute of Technology, Institut National de Recherche en Informatique et en Automatique, Keio University). All Rights Reserved.
http://www.w3.org/Consortium/Legal/
Public documents on the W3C site are provided by the copyright holders under the following license. The software or Document Type Definitions (DTDs) associated with W3C specifications are governed by the Software Notice. By using and/or copying this document, or the W3C document from which this statement is linked, you (the licensee) agree that you have read, understood, and will comply with the following terms and conditions:

Permission to use, copy, and distribute the contents of this document, or the W3C document from which this statement is linked, in any medium for any purpose and without fee or royalty is hereby granted, provided that you include the following on ALL copies of the document, or portions thereof, that you use:

A link or URL to the original W3C document.
The pre-existing copyright notice of the original author, or if it doesn&#x27;t exist, a notice of the form: &quot;Copyright © [$date-of-document] World Wide Web Consortium, (Massachusetts Institute of Technology, Institut National de Recherche en Informatique et en Automatique, Keio University). All Rights Reserved. http://www.w3.org/Consortium/Legal/&quot; (Hypertext is preferred, but a textual representation is permitted.)
If it exists, the STATUS of the W3C document.
When space permits, inclusion of the full text of this NOTICE should be provided. We request that authorship attribution be provided in any software, documents, or other items or products that you create pursuant to the implementation of the contents of this document, or any portion thereof.

No right to create modifications or derivatives of W3C documents is granted pursuant to this license. However, if additional requirements (documented in the Copyright FAQ) are satisfied, the right to create modifications or derivatives is sometimes granted by the W3C to individuals complying with those requirements.

THIS DOCUMENT IS PROVIDED &quot;AS IS,&quot; AND COPYRIGHT HOLDERS MAKE NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, OR TITLE; THAT THE CONTENTS OF THE DOCUMENT ARE SUITABLE FOR ANY PURPOSE; NOR THAT THE IMPLEMENTATION OF SUCH CONTENTS WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER RIGHTS.

COPYRIGHT HOLDERS WILL NOT BE LIABLE FOR ANY DIRECT, INDIRECT, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF ANY USE OF THE DOCUMENT OR THE PERFORMANCE OR IMPLEMENTATION OF THE CONTENTS THEREOF.

The name and trademarks of copyright holders may NOT be used in advertising or publicity pertaining to this document or its contents without specific, written prior permission. Title to copyright in this document will at all times remain with copyright holders.

----------------------------------------------------------------------------

This formulation of W3C&#x27;s notice and license became active on April 05 1999 so as to account for the treatment of DTDs, schema&#x27;s and bindings. See the older formulation for the policy prior to this date. Please see our Copyright FAQ for common questions about using materials from our site, including specific terms and conditions for packages like libwww, Amaya, and Jigsaw. Other questions about this notice can be directed to site-policy@w3.org.

webmaster
(last updated by reagle on 1999/04/99.)
```
---
#### <a name="edl-1.0"></a>EDL-1.0
The following license text for the EDL-1.0 license is cited only once.

```
Copyright (c) 2007, Eclipse Foundation, Inc. and its licensors.

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
* Neither the name of the Eclipse Foundation, Inc. nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS &quot;AS IS&quot; AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

```
---
#### <a name="unicode-3.0"></a>Unicode-3.0
The following license text for the Unicode-3.0 license is cited only once.

```
UNICODE LICENSE V3
COPYRIGHT AND PERMISSION NOTICE

Copyright © 1991-2023 Unicode, Inc.
NOTICE TO USER: Carefully read the following legal agreement. BY DOWNLOADING, INSTALLING, COPYING OR OTHERWISE USING DATA FILES, AND/OR SOFTWARE, YOU UNEQUIVOCALLY ACCEPT, AND AGREE TO BE BOUND BY, ALL OF THE TERMS AND CONDITIONS OF THIS AGREEMENT. IF YOU DO NOT AGREE, DO NOT DOWNLOAD, INSTALL, COPY, DISTRIBUTE OR USE THE DATA FILES OR SOFTWARE.

Permission is hereby granted, free of charge, to any person obtaining a copy of data files and any associated documentation (the &quot;Data Files&quot;) or software and any associated documentation (the &quot;Software&quot;) to deal in the Data Files or Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, and/or sell copies of the Data Files or Software, and to permit persons to whom the Data Files or Software are furnished to do so, provided that either (a) this copyright and permission notice appear with all copies of the Data Files or Software, or (b) this copyright and permission notice appear in associated Documentation.

THE DATA FILES AND SOFTWARE ARE PROVIDED &quot;AS IS&quot;, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT OF THIRD PARTY RIGHTS.

IN NO EVENT SHALL THE COPYRIGHT HOLDER OR HOLDERS INCLUDED IN THIS NOTICE BE LIABLE FOR ANY CLAIM, OR ANY SPECIAL INDIRECT OR CONSEQUENTIAL DAMAGES, OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THE DATA FILES OR SOFTWARE.

Except as contained in this notice, the name of a copyright holder shall not be used in advertising or otherwise to promote the sale, use or other dealings in these Data Files or Software without prior written authorization of the copyright holder.
```
---
#### <a name="unicode-dfs-2016"></a>Unicode-DFS-2016
The following license text for the Unicode-DFS-2016 license is cited only once.

```
UNICODE, INC. LICENSE AGREEMENT - DATA FILES AND SOFTWARE

See Terms of Use (https://www.unicode.org/copyright.html) for definitions of Unicode Inc.&#x27;s
Data Files and Software.

NOTICE TO USER: Carefully read the following legal agreement.
BY DOWNLOADING, INSTALLING, COPYING OR OTHERWISE USING UNICODE INC.&#x27;S
DATA FILES (&quot;DATA FILES&quot;), AND/OR SOFTWARE (&quot;SOFTWARE&quot;),
YOU UNEQUIVOCALLY ACCEPT, AND AGREE TO BE BOUND BY, ALL OF THE
TERMS AND CONDITIONS OF THIS AGREEMENT.
IF YOU DO NOT AGREE, DO NOT DOWNLOAD, INSTALL, COPY, DISTRIBUTE OR USE
THE DATA FILES OR SOFTWARE.

COPYRIGHT AND PERMISSION NOTICE

Copyright © 1991-2021 Unicode, Inc. All rights reserved.
Distributed under the Terms of Use in https://www.unicode.org/copyright.html.

Permission is hereby granted, free of charge, to any person obtaining
a copy of the Unicode data files and any associated documentation
(the &quot;Data Files&quot;) or Unicode software and any associated documentation
(the &quot;Software&quot;) to deal in the Data Files or Software
without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, and/or sell copies of
the Data Files or Software, and to permit persons to whom the Data Files
or Software are furnished to do so, provided that either
(a) this copyright and permission notice appear with all copies
of the Data Files or Software, or
(b) this copyright and permission notice appear in associated
Documentation.

THE DATA FILES AND SOFTWARE ARE PROVIDED &quot;AS IS&quot;, WITHOUT WARRANTY OF
ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT OF THIRD PARTY RIGHTS.
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR HOLDERS INCLUDED IN THIS
NOTICE BE LIABLE FOR ANY CLAIM, OR ANY SPECIAL INDIRECT OR CONSEQUENTIAL
DAMAGES, OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE,
DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
PERFORMANCE OF THE DATA FILES OR SOFTWARE.

Except as contained in this notice, the name of a copyright holder
shall not be used in advertising or otherwise to promote the sale,
use or other dealings in these Data Files or Software without prior
written authorization of the copyright holder.

```
---
#### <a name="indianauniversity-1.1.1"></a>IndianaUniversity-1.1.1
The following license text for the IndianaUniversity-1.1.1 license is cited only once.

```
Indiana University Extreme! Lab Software License

Version 1.1.1

Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
   following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
   following disclaimer in the documentation and/or other materials provided with the distribution.

3. The end-user documentation included with the redistribution, if any, must include the following
   acknowledgment:

      &quot;This product includes software developed by the Indiana University Extreme! Lab (http://www.extreme.indiana.edu/).&quot;

   Alternately, this acknowledgment may appear in the software itself, if and wherever such third-party
   acknowledgments normally appear.

4. The names &quot;Indiana Univeristy&quot; and &quot;Indiana Univeristy Extreme! Lab&quot; must not be used to endorse or
   promote products derived from this software without prior written permission. For written permission,
   please contact http://www.extreme.indiana.edu/.

5. Products derived from this software may not use &quot;Indiana Univeristy&quot; name nor may &quot;Indiana Univeristy&quot;
   appear in their name, without prior written permission of the Indiana University.

THIS SOFTWARE IS PROVIDED &quot;AS IS&quot; AND ANY EXPRESSED OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHORS,
COPYRIGHT HOLDERS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
THE POSSIBILITY OF SUCH DAMAGE.
```
---
#### <a name="jquery"></a>JQUERY
The following license text for the JQUERY license is cited only once.

```
License
Note: For the purposes of this document, the term &quot;Project&quot; will refer to any OpenJS Foundation project using the MIT license AND referencing this document in the header of the distributed Project code or Project website source code.

Source Code
Projects referencing this document are released under the terms of the MIT license.

The MIT License is simple and easy to understand and it places almost no restrictions on what you can do with the Project.

You are free to use the Project in any other project (even commercial projects) as long as the copyright header is left intact.

Sample Code
All demos and examples, whether in a Project&#x27;s repository or displayed on a Project site, are released under the terms of the license as specified in the relevant repository. Many Projects choose to release their sample code under the terms of CC0.

CC0 is even more permissive than the MIT license, allowing you to use the code in any manner you want, without any copyright headers, notices, or other attribution.

Web Sites
The content on a Project web site referencing this document in its header is released under the terms of the license specified in the website&#x27;s repository or if not specified, under the MIT license.

The design, layout, and look-and-feel of a Project website is not licensed for use and may not be used on any site, personal or commercial, without prior written consent from the OpenJS Foundation.

For information regarding OpenJS Foundation trademarks, please see Trademark Policy and Trademark List.
```
---
#### <a name="epl-1.0"></a>EPL-1.0
The following license text for the EPL-1.0 license is cited only once.

```
Eclipse Public License - v 1.0
THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE PUBLIC LICENSE (&quot;AGREEMENT&quot;). ANY USE, REPRODUCTION OR DISTRIBUTION OF THE PROGRAM CONSTITUTES RECIPIENT&#x27;S ACCEPTANCE OF THIS AGREEMENT.
1. DEFINITIONS
&quot;Contribution&quot; means:
a) in the case of the initial Contributor, the initial code and documentation distributed under this Agreement, and
b) in the case of each subsequent Contributor:
i) changes to the Program, and
ii) additions to the Program;
where such changes and/or additions to the Program originate from and are distributed by that particular Contributor. A Contribution &#x27;originates&#x27; from a Contributor if it was added to the Program by such Contributor itself or anyone acting on such Contributor&#x27;s behalf. Contributions do not include additions to the Program which: (i) are separate modules of software distributed in conjunction with the Program under their own license agreement, and (ii) are not derivative works of the Program.
&quot;Contributor&quot; means any person or entity that distributes the Program.
&quot;Licensed Patents&quot; mean patent claims licensable by a Contributor which are necessarily infringed by the use or sale of its Contribution alone or when combined with the Program.
&quot;Program&quot; means the Contributions distributed in accordance with this Agreement.
&quot;Recipient&quot; means anyone who receives the Program under this Agreement, including all Contributors.
2. GRANT OF RIGHTS
a) Subject to the terms of this Agreement, each Contributor hereby grants Recipient a non-exclusive, worldwide, royalty-free copyright license to reproduce, prepare derivative works of, publicly display, publicly perform, distribute and sublicense the Contribution of such Contributor, if any, and such derivative works, in source code and object code form.
b) Subject to the terms of this Agreement, each Contributor hereby grants Recipient a non-exclusive, worldwide, royalty-free patent license under Licensed Patents to make, use, sell, offer to sell, import and otherwise transfer the Contribution of such Contributor, if any, in source code and object code form. This patent license shall apply to the combination of the Contribution and the Program if, at the time the Contribution is added by the Contributor, such addition of the Contribution causes such combination to be covered by the Licensed Patents. The patent license shall not apply to any other combinations which include the Contribution. No hardware per se is licensed hereunder.
c) Recipient understands that although each Contributor grants the licenses to its Contributions set forth herein, no assurances are provided by any Contributor that the Program does not infringe the patent or other intellectual property rights of any other entity. Each Contributor disclaims any liability to Recipient for claims brought by any other entity based on infringement of intellectual property rights or otherwise. As a condition to exercising the rights and licenses granted hereunder, each Recipient hereby assumes sole responsibility to secure any other intellectual property rights needed, if any. For example, if a third party patent license is required to allow Recipient to distribute the Program, it is Recipient&#x27;s responsibility to acquire that license before distributing the Program.
d) Each Contributor represents that to its knowledge it has sufficient copyright rights in its Contribution, if any, to grant the copyright license set forth in this Agreement.
3. REQUIREMENTS
A Contributor may choose to distribute the Program in object code form under its own license agreement, provided that:
a) it complies with the terms and conditions of this Agreement; and
b) its license agreement:
i) effectively disclaims on behalf of all Contributors all warranties and conditions, express and implied, including warranties or conditions of title and non-infringement, and implied warranties or conditions of merchantability and fitness for a particular purpose;
ii) effectively excludes on behalf of all Contributors all liability for damages, including direct, indirect, special, incidental and consequential damages, such as lost profits;
iii) states that any provisions which differ from this Agreement are offered by that Contributor alone and not by any other party; and
iv) states that source code for the Program is available from such Contributor, and informs licensees how to obtain it in a reasonable manner on or through a medium customarily used for software exchange.
When the Program is made available in source code form:
a) it must be made available under this Agreement; and
b) a copy of this Agreement must be included with each copy of the Program.
Contributors may not remove or alter any copyright notices contained within the Program.
Each Contributor must identify itself as the originator of its Contribution, if any, in a manner that reasonably allows subsequent Recipients to identify the originator of the Contribution.
4. COMMERCIAL DISTRIBUTION
Commercial distributors of software may accept certain responsibilities with respect to end users, business partners and the like. While this license is intended to facilitate the commercial use of the Program, the Contributor who includes the Program in a commercial product offering should do so in a manner which does not create potential liability for other Contributors. Therefore, if a Contributor includes the Program in a commercial product offering, such Contributor (&quot;Commercial Contributor&quot;) hereby agrees to defend and indemnify every other Contributor (&quot;Indemnified Contributor&quot;) against any losses, damages and costs (collectively &quot;Losses&quot;) arising from claims, lawsuits and other legal actions brought by a third party against the Indemnified Contributor to the extent caused by the acts or omissions of such Commercial Contributor in connection with its distribution of the Program in a commercial product offering. The obligations in this section do not apply to any claims or Losses relating to any actual or alleged intellectual property infringement. In order to qualify, an Indemnified Contributor must: a) promptly notify the Commercial Contributor in writing of such claim, and b) allow the Commercial Contributor to control, and cooperate with the Commercial Contributor in, the defense and any related settlement negotiations. The Indemnified Contributor may participate in any such claim at its own expense.
For example, a Contributor might include the Program in a commercial product offering, Product X. That Contributor is then a Commercial Contributor. If that Commercial Contributor then makes performance claims, or offers warranties related to Product X, those performance claims and warranties are such Commercial Contributor&#x27;s responsibility alone. Under this section, the Commercial Contributor would have to defend claims against the other Contributors related to those performance claims and warranties, and if a court requires any other Contributor to pay any damages as a result, the Commercial Contributor must pay those damages.
5. NO WARRANTY
EXCEPT AS EXPRESSLY SET FORTH IN THIS AGREEMENT, THE PROGRAM IS PROVIDED ON AN &quot;AS IS&quot; BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED INCLUDING, WITHOUT LIMITATION, ANY WARRANTIES OR CONDITIONS OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Each Recipient is solely responsible for determining the appropriateness of using and distributing the Program and assumes all risks associated with its exercise of rights under this Agreement , including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and unavailability or interruption of operations.
6. DISCLAIMER OF LIABILITY
EXCEPT AS EXPRESSLY SET FORTH IN THIS AGREEMENT, NEITHER RECIPIENT NOR ANY CONTRIBUTORS SHALL HAVE ANY LIABILITY FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING WITHOUT LIMITATION LOST PROFITS), HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OR DISTRIBUTION OF THE PROGRAM OR THE EXERCISE OF ANY RIGHTS GRANTED HEREUNDER, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
7. GENERAL
If any provision of this Agreement is invalid or unenforceable under applicable law, it shall not affect the validity or enforceability of the remainder of the terms of this Agreement, and without further action by the parties hereto, such provision shall be reformed to the minimum extent necessary to make such provision valid and enforceable.
If Recipient institutes patent litigation against any entity (including a cross-claim or counterclaim in a lawsuit) alleging that the Program itself (excluding combinations of the Program with other software or hardware) infringes such Recipient&#x27;s patent(s), then such Recipient&#x27;s rights granted under Section 2(b) shall terminate as of the date such litigation is filed.
All Recipient&#x27;s rights under this Agreement shall terminate if it fails to comply with any of the material terms or conditions of this Agreement and does not cure such failure in a reasonable period of time after becoming aware of such noncompliance. If all Recipient&#x27;s rights under this Agreement terminate, Recipient agrees to cease use and distribution of the Program as soon as reasonably practicable. However, Recipient&#x27;s obligations under this Agreement and any licenses granted by Recipient relating to the Program shall continue and survive.
Everyone is permitted to copy and distribute copies of this Agreement, but in order to avoid inconsistency the Agreement is copyrighted and may only be modified in the following manner. The Agreement Steward reserves the right to publish new versions (including revisions) of this Agreement from time to time. No one other than the Agreement Steward has the right to modify this Agreement. The Eclipse Foundation is the initial Agreement Steward. The Eclipse Foundation may assign the responsibility to serve as the Agreement Steward to a suitable separate entity. Each new version of the Agreement will be given a distinguishing version number. The Program (including Contributions) may always be distributed subject to the version of the Agreement under which it was received. In addition, after a new version of the Agreement is published, Contributor may elect to distribute the Program (including its Contributions) under the new version. Except as expressly stated in Sections 2(a) and 2(b) above, Recipient receives no rights or licenses to the intellectual property of any Contributor under this Agreement, whether expressly, by implication, estoppel or otherwise. All rights in the Program not expressly granted under this Agreement are reserved.
This Agreement is governed by the laws of the State of New York and the intellectual property laws of the United States of America. No party to this Agreement will bring a legal action under this Agreement more than one year after the cause of action arose. Each party waives its rights to a jury trial in any resulting litigation.

```
---
#### <a name="python-2.0"></a>Python-2.0
The following license text for the Python-2.0 license is cited only once.

```
A. HISTORY OF THE SOFTWARE
==========================

Python was created in the early 1990s by Guido van Rossum at Stichting
Mathematisch Centrum (CWI, see http://www.cwi.nl) in the Netherlands
as a successor of a language called ABC.  Guido remains Python&#x27;s
principal author, although it includes many contributions from others.

In 1995, Guido continued his work on Python at the Corporation for
National Research Initiatives (CNRI, see http://www.cnri.reston.va.us)
in Reston, Virginia where he released several versions of the
software.

In May 2000, Guido and the Python core development team moved to
BeOpen.com to form the BeOpen PythonLabs team.  In October of the same
year, the PythonLabs team moved to Digital Creations, which became
Zope Corporation.  In 2001, the Python Software Foundation (PSF, see
https://www.python.org/psf/) was formed, a non-profit organization
created specifically to own Python-related Intellectual Property.
Zope Corporation was a sponsoring member of the PSF.

All Python releases are Open Source (see http://www.opensource.org for
the Open Source Definition).  Historically, most, but not all, Python
releases have also been GPL-compatible; the table below summarizes
the various releases.

    Release         Derived     Year        Owner       GPL-
                    from                                compatible? (1)

    0.9.0 thru 1.2              1991-1995   CWI         yes
    1.3 thru 1.5.2  1.2         1995-1999   CNRI        yes
    1.6             1.5.2       2000        CNRI        no
    2.0             1.6         2000        BeOpen.com  no
    1.6.1           1.6         2001        CNRI        yes (2)
    2.1             2.0+1.6.1   2001        PSF         no
    2.0.1           2.0+1.6.1   2001        PSF         yes
    2.1.1           2.1+2.0.1   2001        PSF         yes
    2.1.2           2.1.1       2002        PSF         yes
    2.1.3           2.1.2       2002        PSF         yes
    2.2 and above   2.1.1       2001-now    PSF         yes

Footnotes:

(1) GPL-compatible doesn&#x27;t mean that we&#x27;re distributing Python under
    the GPL.  All Python licenses, unlike the GPL, let you distribute
    a modified version without making your changes open source.  The
    GPL-compatible licenses make it possible to combine Python with
    other software that is released under the GPL; the others don&#x27;t.

(2) According to Richard Stallman, 1.6.1 is not GPL-compatible,
    because its license has a choice of law clause.  According to
    CNRI, however, Stallman&#x27;s lawyer has told CNRI&#x27;s lawyer that 1.6.1
    is &quot;not incompatible&quot; with the GPL.

Thanks to the many outside volunteers who have worked under Guido&#x27;s
direction to make these releases possible.


B. TERMS AND CONDITIONS FOR ACCESSING OR OTHERWISE USING PYTHON
===============================================================

PYTHON SOFTWARE FOUNDATION LICENSE VERSION 2
--------------------------------------------

1. This LICENSE AGREEMENT is between the Python Software Foundation
(&quot;PSF&quot;), and the Individual or Organization (&quot;Licensee&quot;) accessing and
otherwise using this software (&quot;Python&quot;) in source or binary form and
its associated documentation.

2. Subject to the terms and conditions of this License Agreement, PSF hereby
grants Licensee a nonexclusive, royalty-free, world-wide license to reproduce,
analyze, test, perform and/or display publicly, prepare derivative works,
distribute, and otherwise use Python alone or in any derivative version,
provided, however, that PSF&#x27;s License Agreement and PSF&#x27;s notice of copyright,
i.e., &quot;Copyright (c) 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010,
2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 Python Software Foundation;
All Rights Reserved&quot; are retained in Python alone or in any derivative version
prepared by Licensee.

3. In the event Licensee prepares a derivative work that is based on
or incorporates Python or any part thereof, and wants to make
the derivative work available to others as provided herein, then
Licensee hereby agrees to include in any such work a brief summary of
the changes made to Python.

4. PSF is making Python available to Licensee on an &quot;AS IS&quot;
basis.  PSF MAKES NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR
IMPLIED.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, PSF MAKES NO AND
DISCLAIMS ANY REPRESENTATION OR WARRANTY OF MERCHANTABILITY OR FITNESS
FOR ANY PARTICULAR PURPOSE OR THAT THE USE OF PYTHON WILL NOT
INFRINGE ANY THIRD PARTY RIGHTS.

5. PSF SHALL NOT BE LIABLE TO LICENSEE OR ANY OTHER USERS OF PYTHON
FOR ANY INCIDENTAL, SPECIAL, OR CONSEQUENTIAL DAMAGES OR LOSS AS
A RESULT OF MODIFYING, DISTRIBUTING, OR OTHERWISE USING PYTHON,
OR ANY DERIVATIVE THEREOF, EVEN IF ADVISED OF THE POSSIBILITY THEREOF.

6. This License Agreement will automatically terminate upon a material
breach of its terms and conditions.

7. Nothing in this License Agreement shall be deemed to create any
relationship of agency, partnership, or joint venture between PSF and
Licensee.  This License Agreement does not grant permission to use PSF
trademarks or trade name in a trademark sense to endorse or promote
products or services of Licensee, or any third party.

8. By copying, installing or otherwise using Python, Licensee
agrees to be bound by the terms and conditions of this License
Agreement.


BEOPEN.COM LICENSE AGREEMENT FOR PYTHON 2.0
-------------------------------------------

BEOPEN PYTHON OPEN SOURCE LICENSE AGREEMENT VERSION 1

1. This LICENSE AGREEMENT is between BeOpen.com (&quot;BeOpen&quot;), having an
office at 160 Saratoga Avenue, Santa Clara, CA 95051, and the
Individual or Organization (&quot;Licensee&quot;) accessing and otherwise using
this software in source or binary form and its associated
documentation (&quot;the Software&quot;).

2. Subject to the terms and conditions of this BeOpen Python License
Agreement, BeOpen hereby grants Licensee a non-exclusive,
royalty-free, world-wide license to reproduce, analyze, test, perform
and/or display publicly, prepare derivative works, distribute, and
otherwise use the Software alone or in any derivative version,
provided, however, that the BeOpen Python License is retained in the
Software, alone or in any derivative version prepared by Licensee.

3. BeOpen is making the Software available to Licensee on an &quot;AS IS&quot;
basis.  BEOPEN MAKES NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR
IMPLIED.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, BEOPEN MAKES NO AND
DISCLAIMS ANY REPRESENTATION OR WARRANTY OF MERCHANTABILITY OR FITNESS
FOR ANY PARTICULAR PURPOSE OR THAT THE USE OF THE SOFTWARE WILL NOT
INFRINGE ANY THIRD PARTY RIGHTS.

4. BEOPEN SHALL NOT BE LIABLE TO LICENSEE OR ANY OTHER USERS OF THE
SOFTWARE FOR ANY INCIDENTAL, SPECIAL, OR CONSEQUENTIAL DAMAGES OR LOSS
AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THE SOFTWARE, OR ANY
DERIVATIVE THEREOF, EVEN IF ADVISED OF THE POSSIBILITY THEREOF.

5. This License Agreement will automatically terminate upon a material
breach of its terms and conditions.

6. This License Agreement shall be governed by and interpreted in all
respects by the law of the State of California, excluding conflict of
law provisions.  Nothing in this License Agreement shall be deemed to
create any relationship of agency, partnership, or joint venture
between BeOpen and Licensee.  This License Agreement does not grant
permission to use BeOpen trademarks or trade names in a trademark
sense to endorse or promote products or services of Licensee, or any
third party.  As an exception, the &quot;BeOpen Python&quot; logos available at
http://www.pythonlabs.com/logos.html may be used according to the
permissions granted on that web page.

7. By copying, installing or otherwise using the software, Licensee
agrees to be bound by the terms and conditions of this License
Agreement.


CNRI LICENSE AGREEMENT FOR PYTHON 1.6.1
---------------------------------------

1. This LICENSE AGREEMENT is between the Corporation for National
Research Initiatives, having an office at 1895 Preston White Drive,
Reston, VA 20191 (&quot;CNRI&quot;), and the Individual or Organization
(&quot;Licensee&quot;) accessing and otherwise using Python 1.6.1 software in
source or binary form and its associated documentation.

2. Subject to the terms and conditions of this License Agreement, CNRI
hereby grants Licensee a nonexclusive, royalty-free, world-wide
license to reproduce, analyze, test, perform and/or display publicly,
prepare derivative works, distribute, and otherwise use Python 1.6.1
alone or in any derivative version, provided, however, that CNRI&#x27;s
License Agreement and CNRI&#x27;s notice of copyright, i.e., &quot;Copyright (c)
1995-2001 Corporation for National Research Initiatives; All Rights
Reserved&quot; are retained in Python 1.6.1 alone or in any derivative
version prepared by Licensee.  Alternately, in lieu of CNRI&#x27;s License
Agreement, Licensee may substitute the following text (omitting the
quotes): &quot;Python 1.6.1 is made available subject to the terms and
conditions in CNRI&#x27;s License Agreement.  This Agreement together with
Python 1.6.1 may be located on the Internet using the following
unique, persistent identifier (known as a handle): 1895.22/1013.  This
Agreement may also be obtained from a proxy server on the Internet
using the following URL: http://hdl.handle.net/1895.22/1013&quot;.

3. In the event Licensee prepares a derivative work that is based on
or incorporates Python 1.6.1 or any part thereof, and wants to make
the derivative work available to others as provided herein, then
Licensee hereby agrees to include in any such work a brief summary of
the changes made to Python 1.6.1.

4. CNRI is making Python 1.6.1 available to Licensee on an &quot;AS IS&quot;
basis.  CNRI MAKES NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR
IMPLIED.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, CNRI MAKES NO AND
DISCLAIMS ANY REPRESENTATION OR WARRANTY OF MERCHANTABILITY OR FITNESS
FOR ANY PARTICULAR PURPOSE OR THAT THE USE OF PYTHON 1.6.1 WILL NOT
INFRINGE ANY THIRD PARTY RIGHTS.

5. CNRI SHALL NOT BE LIABLE TO LICENSEE OR ANY OTHER USERS OF PYTHON
1.6.1 FOR ANY INCIDENTAL, SPECIAL, OR CONSEQUENTIAL DAMAGES OR LOSS AS
A RESULT OF MODIFYING, DISTRIBUTING, OR OTHERWISE USING PYTHON 1.6.1,
OR ANY DERIVATIVE THEREOF, EVEN IF ADVISED OF THE POSSIBILITY THEREOF.

6. This License Agreement will automatically terminate upon a material
breach of its terms and conditions.

7. This License Agreement shall be governed by the federal
intellectual property law of the United States, including without
limitation the federal copyright law, and, to the extent such
U.S. federal law does not apply, by the law of the Commonwealth of
Virginia, excluding Virginia&#x27;s conflict of law provisions.
Notwithstanding the foregoing, with regard to derivative works based
on Python 1.6.1 that incorporate non-separable material that was
previously distributed under the GNU General Public License (GPL), the
law of the Commonwealth of Virginia shall govern this License
Agreement only as to issues arising under or with respect to
Paragraphs 4, 5, and 7 of this License Agreement.  Nothing in this
License Agreement shall be deemed to create any relationship of
agency, partnership, or joint venture between CNRI and Licensee.  This
License Agreement does not grant permission to use CNRI trademarks or
trade name in a trademark sense to endorse or promote products or
services of Licensee, or any third party.

8. By clicking on the &quot;ACCEPT&quot; button where indicated, or by copying,
installing or otherwise using Python 1.6.1, Licensee agrees to be
bound by the terms and conditions of this License Agreement.

        ACCEPT


CWI LICENSE AGREEMENT FOR PYTHON 0.9.0 THROUGH 1.2
--------------------------------------------------

Copyright (c) 1991 - 1995, Stichting Mathematisch Centrum Amsterdam,
The Netherlands.  All rights reserved.

Permission to use, copy, modify, and distribute this software and its
documentation for any purpose and without fee is hereby granted,
provided that the above copyright notice appear in all copies and that
both that copyright notice and this permission notice appear in
supporting documentation, and that the name of Stichting Mathematisch
Centrum or CWI not be used in advertising or publicity pertaining to
distribution of the software without specific, written prior
permission.

STICHTING MATHEMATISCH CENTRUM DISCLAIMS ALL WARRANTIES WITH REGARD TO
THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS, IN NO EVENT SHALL STICHTING MATHEMATISCH CENTRUM BE LIABLE
FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT
OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

```
---
#### <a name="cc-by-2.5"></a>CC-BY-2.5
The following license text for the CC-BY-2.5 license is cited only once.

```
Creative Commons Attribution 2.5
THE WORK (AS DEFINED BELOW) IS PROVIDED UNDER THE TERMS OF THIS CREATIVE COMMONS PUBLIC LICENSE (&quot;CCPL&quot; OR &quot;LICENSE&quot;). THE WORK IS PROTECTED BY COPYRIGHT AND/OR OTHER APPLICABLE LAW. ANY USE OF THE WORK OTHER THAN AS AUTHORIZED UNDER THIS LICENSE OR COPYRIGHT LAW IS PROHIBITED.
BY EXERCISING ANY RIGHTS TO THE WORK PROVIDED HERE, YOU ACCEPT AND AGREE TO BE BOUND BY THE TERMS OF THIS LICENSE. THE LICENSOR GRANTS YOU THE RIGHTS CONTAINED HERE IN CONSIDERATION OF YOUR ACCEPTANCE OF SUCH TERMS AND CONDITIONS.
1. Definitions
&quot;Collective Work&quot; means a work, such as a periodical issue, anthology or encyclopedia, in which the Work in its entirety in unmodified form, along with a number of other contributions, constituting separate and independent works in themselves, are assembled into a collective whole. A work that constitutes a Collective Work will not be considered a Derivative Work (as defined below) for the purposes of this License.
&quot;Derivative Work&quot; means a work based upon the Work or upon the Work and other pre-existing works, such as a translation, musical arrangement, dramatization, fictionalization, motion picture version, sound recording, art reproduction, abridgment, condensation, or any other form in which the Work may be recast, transformed, or adapted, except that a work that constitutes a Collective Work will not be considered a Derivative Work for the purpose of this License. For the avoidance of doubt, where the Work is a musical composition or sound recording, the synchronization of the Work in timed-relation with a moving image (&quot;synching&quot;) will be considered a Derivative Work for the purpose of this License.
&quot;Licensor&quot; means the individual or entity that offers the Work under the terms of this License.
&quot;Original Author&quot; means the individual or entity who created the Work.
&quot;Work&quot; means the copyrightable work of authorship offered under the terms of this License.
&quot;You&quot; means an individual or entity exercising rights under this License who has not previously violated the terms of this License with respect to the Work, or who has received express permission from the Licensor to exercise rights under this License despite a previous violation.
2. Fair Use Rights. Nothing in this license is intended to reduce, limit, or restrict any rights arising from fair use, first sale or other limitations on the exclusive rights of the copyright owner under copyright law or other applicable laws.
3. License Grant. Subject to the terms and conditions of this License, Licensor hereby grants You a worldwide, royalty-free, non-exclusive, perpetual (for the duration of the applicable copyright) license to exercise the rights in the Work as stated below:
to reproduce the Work, to incorporate the Work into one or more Collective Works, and to reproduce the Work as incorporated in the Collective Works;
to create and reproduce Derivative Works;
to distribute copies or phonorecords of, display publicly, perform publicly, and perform publicly by means of a digital audio transmission the Work including as incorporated in Collective Works;
to distribute copies or phonorecords of, display publicly, perform publicly, and perform publicly by means of a digital audio transmission Derivative Works.
For the avoidance of doubt, where the work is a musical composition:
Performance Royalties Under Blanket Licenses. Licensor waives the exclusive right to collect, whether individually or via a performance rights society (e.g. ASCAP, BMI, SESAC), royalties for the public performance or public digital performance (e.g. webcast) of the Work.
Mechanical Rights and Statutory Royalties. Licensor waives the exclusive right to collect, whether individually or via a music rights agency or designated agent (e.g. Harry Fox Agency), royalties for any phonorecord You create from the Work (&quot;cover version&quot;) and distribute, subject to the compulsory license created by 17 USC Section 115 of the US Copyright Act (or the equivalent in other jurisdictions).
Webcasting Rights and Statutory Royalties. For the avoidance of doubt, where the Work is a sound recording, Licensor waives the exclusive right to collect, whether individually or via a performance-rights society (e.g. SoundExchange), royalties for the public digital performance (e.g. webcast) of the Work, subject to the compulsory license created by 17 USC Section 114 of the US Copyright Act (or the equivalent in other jurisdictions).
The above rights may be exercised in all media and formats whether now known or hereafter devised. The above rights include the right to make such modifications as are technically necessary to exercise the rights in other media and formats. All rights not expressly granted by Licensor are hereby reserved.
4. Restrictions.The license granted in Section 3 above is expressly made subject to and limited by the following restrictions:
You may distribute, publicly display, publicly perform, or publicly digitally perform the Work only under the terms of this License, and You must include a copy of, or the Uniform Resource Identifier for, this License with every copy or phonorecord of the Work You distribute, publicly display, publicly perform, or publicly digitally perform. You may not offer or impose any terms on the Work that alter or restrict the terms of this License or the recipients&#x27; exercise of the rights granted hereunder. You may not sublicense the Work. You must keep intact all notices that refer to this License and to the disclaimer of warranties. You may not distribute, publicly display, publicly perform, or publicly digitally perform the Work with any technological measures that control access or use of the Work in a manner inconsistent with the terms of this License Agreement. The above applies to the Work as incorporated in a Collective Work, but this does not require the Collective Work apart from the Work itself to be made subject to the terms of this License. If You create a Collective Work, upon notice from any Licensor You must, to the extent practicable, remove from the Collective Work any credit as required by clause 4(b), as requested. If You create a Derivative Work, upon notice from any Licensor You must, to the extent practicable, remove from the Derivative Work any credit as required by clause 4(b), as requested.
If you distribute, publicly display, publicly perform, or publicly digitally perform the Work or any Derivative Works or Collective Works, You must keep intact all copyright notices for the Work and provide, reasonable to the medium or means You are utilizing: (i) the name of the Original Author (or pseudonym, if applicable) if supplied, and/or (ii) if the Original Author and/or Licensor designate another party or parties (e.g. a sponsor institute, publishing entity, journal) for attribution in Licensor&#x27;s copyright notice, terms of service or by other reasonable means, the name of such party or parties; the title of the Work if supplied; to the extent reasonably practicable, the Uniform Resource Identifier, if any, that Licensor specifies to be associated with the Work, unless such URI does not refer to the copyright notice or licensing information for the Work; and in the case of a Derivative Work, a credit identifying the use of the Work in the Derivative Work (e.g., &quot;French translation of the Work by Original Author,&quot; or &quot;Screenplay based on original Work by Original Author&quot;). Such credit may be implemented in any reasonable manner; provided, however, that in the case of a Derivative Work or Collective Work, at a minimum such credit will appear where any other comparable authorship credit appears and in a manner at least as prominent as such other comparable authorship credit.
5. Representations, Warranties and Disclaimer
UNLESS OTHERWISE MUTUALLY AGREED TO BY THE PARTIES IN WRITING, LICENSOR OFFERS THE WORK AS-IS AND MAKES NO REPRESENTATIONS OR WARRANTIES OF ANY KIND CONCERNING THE WORK, EXPRESS, IMPLIED, STATUTORY OR OTHERWISE, INCLUDING, WITHOUT LIMITATION, WARRANTIES OF TITLE, MERCHANTIBILITY, FITNESS FOR A PARTICULAR PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, ACCURACY, OR THE PRESENCE OF ABSENCE OF ERRORS, WHETHER OR NOT DISCOVERABLE. SOME JURISDICTIONS DO NOT ALLOW THE EXCLUSION OF IMPLIED WARRANTIES, SO SUCH EXCLUSION MAY NOT APPLY TO YOU.
6. Limitation on Liability. EXCEPT TO THE EXTENT REQUIRED BY APPLICABLE LAW, IN NO EVENT WILL LICENSOR BE LIABLE TO YOU ON ANY LEGAL THEORY FOR ANY SPECIAL, INCIDENTAL, CONSEQUENTIAL, PUNITIVE OR EXEMPLARY DAMAGES ARISING OUT OF THIS LICENSE OR THE USE OF THE WORK, EVEN IF LICENSOR HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
7. Termination
This License and the rights granted hereunder will terminate automatically upon any breach by You of the terms of this License. Individuals or entities who have received Derivative Works or Collective Works from You under this License, however, will not have their licenses terminated provided such individuals or entities remain in full compliance with those licenses. Sections 1, 2, 5, 6, 7, and 8 will survive any termination of this License.
Subject to the above terms and conditions, the license granted here is perpetual (for the duration of the applicable copyright in the Work). Notwithstanding the above, Licensor reserves the right to release the Work under different license terms or to stop distributing the Work at any time; provided, however that any such election will not serve to withdraw this License (or any other license that has been, or is required to be, granted under the terms of this License), and this License will continue in full force and effect unless terminated as stated above.
8. Miscellaneous
Each time You distribute or publicly digitally perform the Work or a Collective Work, the Licensor offers to the recipient a license to the Work on the same terms and conditions as the license granted to You under this License.
Each time You distribute or publicly digitally perform a Derivative Work, Licensor offers to the recipient a license to the original Work on the same terms and conditions as the license granted to You under this License.
If any provision of this License is invalid or unenforceable under applicable law, it shall not affect the validity or enforceability of the remainder of the terms of this License, and without further action by the parties to this agreement, such provision shall be reformed to the minimum extent necessary to make such provision valid and enforceable.
No term or provision of this License shall be deemed waived and no breach consented to unless such waiver or consent shall be in writing and signed by the party to be charged with such waiver or consent.
This License constitutes the entire agreement between the parties with respect to the Work licensed here. There are no understandings, agreements or representations with respect to the Work not specified here. Licensor shall not be bound by any additional provisions that may appear in any communication from You. This License may not be modified without the mutual written agreement of the Licensor and You.

```
---
#### <a name="x11"></a>X11
The following license text for the X11 license is cited only once.

```
X11 License

Copyright (C) 1996 X Consortium

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
documentation files (the &quot;Software&quot;), to deal in the Software without restriction, including without limitation the
rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
Software.

THE SOFTWARE IS PROVIDED &quot;AS IS&quot;, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE X CONSORTIUM
BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of the X Consortium shall not be used in advertising or otherwise to
promote the sale, use or other dealings in this Software without prior written authorization from the X Consortium.

X Window System is a trademark of X Consortium, Inc.
```
---
#### <a name="lgpl-2.1-only"></a>LGPL-2.1-only
The following license text for the LGPL-2.1-only license is cited only once.

```
GNU LESSER GENERAL PUBLIC LICENSE
Version 2.1, February 1999
Copyright (C) 1991, 1999 Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
Everyone is permitted to copy and distribute verbatim copies
of this license document, but changing it is not allowed.

[This is the first released version of the Lesser GPL.  It also counts
 as the successor of the GNU Library Public License, version 2, hence
 the version number 2.1.]
Preamble
The licenses for most software are designed to take away your freedom to share and change it. By contrast, the GNU General Public Licenses are intended to guarantee your freedom to share and change free software--to make sure the software is free for all its users.
This license, the Lesser General Public License, applies to some specially designated software packages--typically libraries--of the Free Software Foundation and other authors who decide to use it. You can use it too, but we suggest you first think carefully about whether this license or the ordinary General Public License is the better strategy to use in any particular case, based on the explanations below.
When we speak of free software, we are referring to freedom of use, not price. Our General Public Licenses are designed to make sure that you have the freedom to distribute copies of free software (and charge for this service if you wish); that you receive source code or can get it if you want it; that you can change the software and use pieces of it in new free programs; and that you are informed that you can do these things.
To protect your rights, we need to make restrictions that forbid distributors to deny you these rights or to ask you to surrender these rights. These restrictions translate to certain responsibilities for you if you distribute copies of the library or if you modify it.
For example, if you distribute copies of the library, whether gratis or for a fee, you must give the recipients all the rights that we gave you. You must make sure that they, too, receive or can get the source code. If you link other code with the library, you must provide complete object files to the recipients, so that they can relink them with the library after making changes to the library and recompiling it. And you must show them these terms so they know their rights.
We protect your rights with a two-step method: (1) we copyright the library, and (2) we offer you this license, which gives you legal permission to copy, distribute and/or modify the library.
To protect each distributor, we want to make it very clear that there is no warranty for the free library. Also, if the library is modified by someone else and passed on, the recipients should know that what they have is not the original version, so that the original author&#x27;s reputation will not be affected by problems that might be introduced by others.
Finally, software patents pose a constant threat to the existence of any free program. We wish to make sure that a company cannot effectively restrict the users of a free program by obtaining a restrictive license from a patent holder. Therefore, we insist that any patent license obtained for a version of the library must be consistent with the full freedom of use specified in this license.
Most GNU software, including some libraries, is covered by the ordinary GNU General Public License. This license, the GNU Lesser General Public License, applies to certain designated libraries, and is quite different from the ordinary General Public License. We use this license for certain libraries in order to permit linking those libraries into non-free programs.
When a program is linked with a library, whether statically or using a shared library, the combination of the two is legally speaking a combined work, a derivative of the original library. The ordinary General Public License therefore permits such linking only if the entire combination fits its criteria of freedom. The Lesser General Public License permits more lax criteria for linking other code with the library.
We call this license the &quot;Lesser&quot; General Public License because it does Less to protect the user&#x27;s freedom than the ordinary General Public License. It also provides other free software developers Less of an advantage over competing non-free programs. These disadvantages are the reason we use the ordinary General Public License for many libraries. However, the Lesser license provides advantages in certain special circumstances.
For example, on rare occasions, there may be a special need to encourage the widest possible use of a certain library, so that it becomes a de-facto standard. To achieve this, non-free programs must be allowed to use the library. A more frequent case is that a free library does the same job as widely used non-free libraries. In this case, there is little to gain by limiting the free library to free software only, so we use the Lesser General Public License.
In other cases, permission to use a particular library in non-free programs enables a greater number of people to use a large body of free software. For example, permission to use the GNU C Library in non-free programs enables many more people to use the whole GNU operating system, as well as its variant, the GNU/Linux operating system.
Although the Lesser General Public License is Less protective of the users&#x27; freedom, it does ensure that the user of a program that is linked with the Library has the freedom and the wherewithal to run that program using a modified version of the Library.
The precise terms and conditions for copying, distribution and modification follow. Pay close attention to the difference between a &quot;work based on the library&quot; and a &quot;work that uses the library&quot;. The former contains code derived from the library, whereas the latter must be combined with the library in order to run.
TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
0. This License Agreement applies to any software library or other program which contains a notice placed by the copyright holder or other authorized party saying it may be distributed under the terms of this Lesser General Public License (also called &quot;this License&quot;). Each licensee is addressed as &quot;you&quot;.
A &quot;library&quot; means a collection of software functions and/or data prepared so as to be conveniently linked with application programs (which use some of those functions and data) to form executables.
The &quot;Library&quot;, below, refers to any such software library or work which has been distributed under these terms. A &quot;work based on the Library&quot; means either the Library or any derivative work under copyright law: that is to say, a work containing the Library or a portion of it, either verbatim or with modifications and/or translated straightforwardly into another language. (Hereinafter, translation is included without limitation in the term &quot;modification&quot;.)
&quot;Source code&quot; for a work means the preferred form of the work for making modifications to it. For a library, complete source code means all the source code for all modules it contains, plus any associated interface definition files, plus the scripts used to control compilation and installation of the library.
Activities other than copying, distribution and modification are not covered by this License; they are outside its scope. The act of running a program using the Library is not restricted, and output from such a program is covered only if its contents constitute a work based on the Library (independent of the use of the Library in a tool for writing it). Whether that is true depends on what the Library does and what the program that uses the Library does.
1. You may copy and distribute verbatim copies of the Library&#x27;s complete source code as you receive it, in any medium, provided that you conspicuously and appropriately publish on each copy an appropriate copyright notice and disclaimer of warranty; keep intact all the notices that refer to this License and to the absence of any warranty; and distribute a copy of this License along with the Library.
You may charge a fee for the physical act of transferring a copy, and you may at your option offer warranty protection in exchange for a fee.
2. You may modify your copy or copies of the Library or any portion of it, thus forming a work based on the Library, and copy and distribute such modifications or work under the terms of Section 1 above, provided that you also meet all of these conditions:
	a) The modified work must itself be a software library.
	b) You must cause the files modified to carry prominent notices stating that you changed the files and the date of any change.
	c) You must cause the whole of the work to be licensed at no charge to all third parties under the terms of this License.
	d) If a facility in the modified Library refers to a function or a table of data to be supplied by an application program that uses the facility, other than as an argument passed when the facility is invoked, then you must make a good faith effort to ensure that, in the event an application does not supply such function or table, the facility still operates, and performs whatever part of its purpose remains meaningful.
(For example, a function in a library to compute square roots has a purpose that is entirely well-defined independent of the application. Therefore, Subsection 2d requires that any application-supplied function or table used by this function must be optional: if the application does not supply it, the square root function must still compute square roots.)
These requirements apply to the modified work as a whole. If identifiable sections of that work are not derived from the Library, and can be reasonably considered independent and separate works in themselves, then this License, and its terms, do not apply to those sections when you distribute them as separate works. But when you distribute the same sections as part of a whole which is a work based on the Library, the distribution of the whole must be on the terms of this License, whose permissions for other licensees extend to the entire whole, and thus to each and every part regardless of who wrote it.
Thus, it is not the intent of this section to claim rights or contest your rights to work written entirely by you; rather, the intent is to exercise the right to control the distribution of derivative or collective works based on the Library.
In addition, mere aggregation of another work not based on the Library with the Library (or with a work based on the Library) on a volume of a storage or distribution medium does not bring the other work under the scope of this License.
3. You may opt to apply the terms of the ordinary GNU General Public License instead of this License to a given copy of the Library. To do this, you must alter all the notices that refer to this License, so that they refer to the ordinary GNU General Public License, version 2, instead of to this License. (If a newer version than version 2 of the ordinary GNU General Public License has appeared, then you can specify that version instead if you wish.) Do not make any other change in these notices.
Once this change is made in a given copy, it is irreversible for that copy, so the ordinary GNU General Public License applies to all subsequent copies and derivative works made from that copy.
This option is useful when you wish to copy part of the code of the Library into a program that is not a library.
4. You may copy and distribute the Library (or a portion or derivative of it, under Section 2) in object code or executable form under the terms of Sections 1 and 2 above provided that you accompany it with the complete corresponding machine-readable source code, which must be distributed under the terms of Sections 1 and 2 above on a medium customarily used for software interchange.
If distribution of object code is made by offering access to copy from a designated place, then offering equivalent access to copy the source code from the same place satisfies the requirement to distribute the source code, even though third parties are not compelled to copy the source along with the object code.
5. A program that contains no derivative of any portion of the Library, but is designed to work with the Library by being compiled or linked with it, is called a &quot;work that uses the Library&quot;. Such a work, in isolation, is not a derivative work of the Library, and therefore falls outside the scope of this License.
However, linking a &quot;work that uses the Library&quot; with the Library creates an executable that is a derivative of the Library (because it contains portions of the Library), rather than a &quot;work that uses the library&quot;. The executable is therefore covered by this License. Section 6 states terms for distribution of such executables.
When a &quot;work that uses the Library&quot; uses material from a header file that is part of the Library, the object code for the work may be a derivative work of the Library even though the source code is not. Whether this is true is especially significant if the work can be linked without the Library, or if the work is itself a library. The threshold for this to be true is not precisely defined by law.
If such an object file uses only numerical parameters, data structure layouts and accessors, and small macros and small inline functions (ten lines or less in length), then the use of the object file is unrestricted, regardless of whether it is legally a derivative work. (Executables containing this object code plus portions of the Library will still fall under Section 6.)
Otherwise, if the work is a derivative of the Library, you may distribute the object code for the work under the terms of Section 6. Any executables containing that work also fall under Section 6, whether or not they are linked directly with the Library itself.
6. As an exception to the Sections above, you may also combine or link a &quot;work that uses the Library&quot; with the Library to produce a work containing portions of the Library, and distribute that work under terms of your choice, provided that the terms permit modification of the work for the customer&#x27;s own use and reverse engineering for debugging such modifications.
You must give prominent notice with each copy of the work that the Library is used in it and that the Library and its use are covered by this License. You must supply a copy of this License. If the work during execution displays copyright notices, you must include the copyright notice for the Library among them, as well as a reference directing the user to the copy of this License. Also, you must do one of these things:
	a) Accompany the work with the complete corresponding machine-readable source code for the Library including whatever changes were used in the work (which must be distributed under Sections 1 and 2 above); and, if the work is an executable linked with the Library, with the complete machine-readable &quot;work that uses the Library&quot;, as object code and/or source code, so that the user can modify the Library and then relink to produce a modified executable containing the modified Library. (It is understood that the user who changes the contents of definitions files in the Library will not necessarily be able to recompile the application to use the modified definitions.)
	b) Use a suitable shared library mechanism for linking with the Library. A suitable mechanism is one that (1) uses at run time a copy of the library already present on the user&#x27;s computer system, rather than copying library functions into the executable, and (2) will operate properly with a modified version of the library, if the user installs one, as long as the modified version is interface-compatible with the version that the work was made with.
	c) Accompany the work with a written offer, valid for at least three years, to give the same user the materials specified in Subsection 6a, above, for a charge no more than the cost of performing this distribution.
	d) If distribution of the work is made by offering access to copy from a designated place, offer equivalent access to copy the above specified materials from the same place.
	e) Verify that the user has already received a copy of these materials or that you have already sent this user a copy.
For an executable, the required form of the &quot;work that uses the Library&quot; must include any data and utility programs needed for reproducing the executable from it. However, as a special exception, the materials to be distributed need not include anything that is normally distributed (in either source or binary form) with the major components (compiler, kernel, and so on) of the operating system on which the executable runs, unless that component itself accompanies the executable.
It may happen that this requirement contradicts the license restrictions of other proprietary libraries that do not normally accompany the operating system. Such a contradiction means you cannot use both them and the Library together in an executable that you distribute.
7. You may place library facilities that are a work based on the Library side-by-side in a single library together with other library facilities not covered by this License, and distribute such a combined library, provided that the separate distribution of the work based on the Library and of the other library facilities is otherwise permitted, and provided that you do these two things:
	a) Accompany the combined library with a copy of the same work based on the Library, uncombined with any other library facilities. This must be distributed under the terms of the Sections above.
	b) Give prominent notice with the combined library of the fact that part of it is a work based on the Library, and explaining where to find the accompanying uncombined form of the same work.
8. You may not copy, modify, sublicense, link with, or distribute the Library except as expressly provided under this License. Any attempt otherwise to copy, modify, sublicense, link with, or distribute the Library is void, and will automatically terminate your rights under this License. However, parties who have received copies, or rights, from you under this License will not have their licenses terminated so long as such parties remain in full compliance.
9. You are not required to accept this License, since you have not signed it. However, nothing else grants you permission to modify or distribute the Library or its derivative works. These actions are prohibited by law if you do not accept this License. Therefore, by modifying or distributing the Library (or any work based on the Library), you indicate your acceptance of this License to do so, and all its terms and conditions for copying, distributing or modifying the Library or works based on it.
10. Each time you redistribute the Library (or any work based on the Library), the recipient automatically receives a license from the original licensor to copy, distribute, link with or modify the Library subject to these terms and conditions. You may not impose any further restrictions on the recipients&#x27; exercise of the rights granted herein. You are not responsible for enforcing compliance by third parties with this License.
11. If, as a consequence of a court judgment or allegation of patent infringement or for any other reason (not limited to patent issues), conditions are imposed on you (whether by court order, agreement or otherwise) that contradict the conditions of this License, they do not excuse you from the conditions of this License. If you cannot distribute so as to satisfy simultaneously your obligations under this License and any other pertinent obligations, then as a consequence you may not distribute the Library at all. For example, if a patent license would not permit royalty-free redistribution of the Library by all those who receive copies directly or indirectly through you, then the only way you could satisfy both it and this License would be to refrain entirely from distribution of the Library.
If any portion of this section is held invalid or unenforceable under any particular circumstance, the balance of the section is intended to apply, and the section as a whole is intended to apply in other circumstances.
It is not the purpose of this section to induce you to infringe any patents or other property right claims or to contest validity of any such claims; this section has the sole purpose of protecting the integrity of the free software distribution system which is implemented by public license practices. Many people have made generous contributions to the wide range of software distributed through that system in reliance on consistent application of that system; it is up to the author/donor to decide if he or she is willing to distribute software through any other system and a licensee cannot impose that choice.
This section is intended to make thoroughly clear what is believed to be a consequence of the rest of this License.
12. If the distribution and/or use of the Library is restricted in certain countries either by patents or by copyrighted interfaces, the original copyright holder who places the Library under this License may add an explicit geographical distribution limitation excluding those countries, so that distribution is permitted only in or among countries not thus excluded. In such case, this License incorporates the limitation as if written in the body of this License.
13. The Free Software Foundation may publish revised and/or new versions of the Lesser General Public License from time to time. Such new versions will be similar in spirit to the present version, but may differ in detail to address new problems or concerns.
Each version is given a distinguishing version number. If the Library specifies a version number of this License which applies to it and &quot;any later version&quot;, you have the option of following the terms and conditions either of that version or of any later version published by the Free Software Foundation. If the Library does not specify a license version number, you may choose any version ever published by the Free Software Foundation.
14. If you wish to incorporate parts of the Library into other free programs whose distribution conditions are incompatible with these, write to the author to ask for permission. For software which is copyrighted by the Free Software Foundation, write to the Free Software Foundation; we sometimes make exceptions for this. Our decision will be guided by the two goals of preserving the free status of all derivatives of our free software and of promoting the sharing and reuse of software generally.
NO WARRANTY
15. BECAUSE THE LIBRARY IS LICENSED FREE OF CHARGE, THERE IS NO WARRANTY FOR THE LIBRARY, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE LIBRARY &quot;AS IS&quot; WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE LIBRARY IS WITH YOU. SHOULD THE LIBRARY PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.
16. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MAY MODIFY AND/OR REDISTRIBUTE THE LIBRARY AS PERMITTED ABOVE, BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR INABILITY TO USE THE LIBRARY (INCLUDING BUT NOT LIMITED TO LOSS OF DATA OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE LIBRARY TO OPERATE WITH ANY OTHER SOFTWARE), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
END OF TERMS AND CONDITIONS

```
---
#### <a name="cddl-1.1"></a>CDDL-1.1
The following license text for the CDDL-1.1 license is cited only once.

```
COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.1

1. Definitions.

    1.1. &quot;Contributor&quot; means each individual or entity that creates or
    contributes to the creation of Modifications.

    1.2. &quot;Contributor Version&quot; means the combination of the Original
    Software, prior Modifications used by a Contributor (if any), and
    the Modifications made by that particular Contributor.

    1.3. &quot;Covered Software&quot; means (a) the Original Software, or (b)
    Modifications, or (c) the combination of files containing Original
    Software with files containing Modifications, in each case including
    portions thereof.

    1.4. &quot;Executable&quot; means the Covered Software in any form other than
    Source Code.

    1.5. &quot;Initial Developer&quot; means the individual or entity that first
    makes Original Software available under this License.

    1.6. &quot;Larger Work&quot; means a work which combines Covered Software or
    portions thereof with code not governed by the terms of this License.

    1.7. &quot;License&quot; means this document.

    1.8. &quot;Licensable&quot; means having the right to grant, to the maximum
    extent possible, whether at the time of the initial grant or
    subsequently acquired, any and all of the rights conveyed herein.

    1.9. &quot;Modifications&quot; means the Source Code and Executable form of
    any of the following:

    A. Any file that results from an addition to, deletion from or
    modification of the contents of a file containing Original Software
    or previous Modifications;

    B. Any new file that contains any part of the Original Software or
    previous Modification; or

    C. Any new file that is contributed or otherwise made available
    under the terms of this License.

    1.10. &quot;Original Software&quot; means the Source Code and Executable form
    of computer software code that is originally released under this
    License.

    1.11. &quot;Patent Claims&quot; means any patent claim(s), now owned or
    hereafter acquired, including without limitation, method, process,
    and apparatus claims, in any patent Licensable by grantor.

    1.12. &quot;Source Code&quot; means (a) the common form of computer software
    code in which modifications are made and (b) associated
    documentation included in or with such code.

    1.13. &quot;You&quot; (or &quot;Your&quot;) means an individual or a legal entity
    exercising rights under, and complying with all of the terms of,
    this License. For legal entities, &quot;You&quot; includes any entity which
    controls, is controlled by, or is under common control with You. For
    purposes of this definition, &quot;control&quot; means (a) the power, direct
    or indirect, to cause the direction or management of such entity,
    whether by contract or otherwise, or (b) ownership of more than
    fifty percent (50%) of the outstanding shares or beneficial
    ownership of such entity.

2. License Grants.

    2.1. The Initial Developer Grant.

    Conditioned upon Your compliance with Section 3.1 below and subject
    to third party intellectual property claims, the Initial Developer
    hereby grants You a world-wide, royalty-free, non-exclusive license:

    (a) under intellectual property rights (other than patent or
    trademark) Licensable by Initial Developer, to use, reproduce,
    modify, display, perform, sublicense and distribute the Original
    Software (or portions thereof), with or without Modifications,
    and/or as part of a Larger Work; and

    (b) under Patent Claims infringed by the making, using or selling of
    Original Software, to make, have made, use, practice, sell, and
    offer for sale, and/or otherwise dispose of the Original Software
    (or portions thereof).

    (c) The licenses granted in Sections 2.1(a) and (b) are effective on
    the date Initial Developer first distributes or otherwise makes the
    Original Software available to a third party under the terms of this
    License.

    (d) Notwithstanding Section 2.1(b) above, no patent license is
    granted: (1) for code that You delete from the Original Software, or
    (2) for infringements caused by: (i) the modification of the
    Original Software, or (ii) the combination of the Original Software
    with other software or devices.

    2.2. Contributor Grant.

    Conditioned upon Your compliance with Section 3.1 below and subject
    to third party intellectual property claims, each Contributor hereby
    grants You a world-wide, royalty-free, non-exclusive license:

    (a) under intellectual property rights (other than patent or
    trademark) Licensable by Contributor to use, reproduce, modify,
    display, perform, sublicense and distribute the Modifications
    created by such Contributor (or portions thereof), either on an
    unmodified basis, with other Modifications, as Covered Software
    and/or as part of a Larger Work; and

    (b) under Patent Claims infringed by the making, using, or selling
    of Modifications made by that Contributor either alone and/or in
    combination with its Contributor Version (or portions of such
    combination), to make, use, sell, offer for sale, have made, and/or
    otherwise dispose of: (1) Modifications made by that Contributor (or
    portions thereof); and (2) the combination of Modifications made by
    that Contributor with its Contributor Version (or portions of such
    combination).

    (c) The licenses granted in Sections 2.2(a) and 2.2(b) are effective
    on the date Contributor first distributes or otherwise makes the
    Modifications available to a third party.

    (d) Notwithstanding Section 2.2(b) above, no patent license is
    granted: (1) for any code that Contributor has deleted from the
    Contributor Version; (2) for infringements caused by: (i) third
    party modifications of Contributor Version, or (ii) the combination
    of Modifications made by that Contributor with other software
    (except as part of the Contributor Version) or other devices; or (3)
    under Patent Claims infringed by Covered Software in the absence of
    Modifications made by that Contributor.

3. Distribution Obligations.

    3.1. Availability of Source Code.

    Any Covered Software that You distribute or otherwise make available
    in Executable form must also be made available in Source Code form
    and that Source Code form must be distributed only under the terms
    of this License. You must include a copy of this License with every
    copy of the Source Code form of the Covered Software You distribute
    or otherwise make available. You must inform recipients of any such
    Covered Software in Executable form as to how they can obtain such
    Covered Software in Source Code form in a reasonable manner on or
    through a medium customarily used for software exchange.

    3.2. Modifications.

    The Modifications that You create or to which You contribute are
    governed by the terms of this License. You represent that You
    believe Your Modifications are Your original creation(s) and/or You
    have sufficient rights to grant the rights conveyed by this License.

    3.3. Required Notices.

    You must include a notice in each of Your Modifications that
    identifies You as the Contributor of the Modification. You may not
    remove or alter any copyright, patent or trademark notices contained
    within the Covered Software, or any notices of licensing or any
    descriptive text giving attribution to any Contributor or the
    Initial Developer.

    3.4. Application of Additional Terms.

    You may not offer or impose any terms on any Covered Software in
    Source Code form that alters or restricts the applicable version of
    this License or the recipients&#x27; rights hereunder. You may choose to
    offer, and to charge a fee for, warranty, support, indemnity or
    liability obligations to one or more recipients of Covered Software.
    However, you may do so only on Your own behalf, and not on behalf of
    the Initial Developer or any Contributor. You must make it
    absolutely clear that any such warranty, support, indemnity or
    liability obligation is offered by You alone, and You hereby agree
    to indemnify the Initial Developer and every Contributor for any
    liability incurred by the Initial Developer or such Contributor as a
    result of warranty, support, indemnity or liability terms You offer.

    3.5. Distribution of Executable Versions.

    You may distribute the Executable form of the Covered Software under
    the terms of this License or under the terms of a license of Your
    choice, which may contain terms different from this License,
    provided that You are in compliance with the terms of this License
    and that the license for the Executable form does not attempt to
    limit or alter the recipient&#x27;s rights in the Source Code form from
    the rights set forth in this License. If You distribute the Covered
    Software in Executable form under a different license, You must make
    it absolutely clear that any terms which differ from this License
    are offered by You alone, not by the Initial Developer or
    Contributor. You hereby agree to indemnify the Initial Developer and
    every Contributor for any liability incurred by the Initial
    Developer or such Contributor as a result of any such terms You offer.

    3.6. Larger Works.

    You may create a Larger Work by combining Covered Software with
    other code not governed by the terms of this License and distribute
    the Larger Work as a single product. In such a case, You must make
    sure the requirements of this License are fulfilled for the Covered
    Software.

4. Versions of the License.

    4.1. New Versions.

    Oracle is the initial license steward and may publish revised and/or
    new versions of this License from time to time. Each version will be
    given a distinguishing version number. Except as provided in Section
    4.3, no one other than the license steward has the right to modify
    this License.

    4.2. Effect of New Versions.

    You may always continue to use, distribute or otherwise make the
    Covered Software available under the terms of the version of the
    License under which You originally received the Covered Software. If
    the Initial Developer includes a notice in the Original Software
    prohibiting it from being distributed or otherwise made available
    under any subsequent version of the License, You must distribute and
    make the Covered Software available under the terms of the version
    of the License under which You originally received the Covered
    Software. Otherwise, You may also choose to use, distribute or
    otherwise make the Covered Software available under the terms of any
    subsequent version of the License published by the license steward.

    4.3. Modified Versions.

    When You are an Initial Developer and You want to create a new
    license for Your Original Software, You may create and use a
    modified version of this License if You: (a) rename the license and
    remove any references to the name of the license steward (except to
    note that the license differs from this License); and (b) otherwise
    make it clear that the license contains terms which differ from this
    License.

5. DISCLAIMER OF WARRANTY.

    COVERED SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN &quot;AS IS&quot; BASIS,
    WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED,
    INCLUDING, WITHOUT LIMITATION, WARRANTIES THAT THE COVERED SOFTWARE
    IS FREE OF DEFECTS, MERCHANTABLE, FIT FOR A PARTICULAR PURPOSE OR
    NON-INFRINGING. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF
    THE COVERED SOFTWARE IS WITH YOU. SHOULD ANY COVERED SOFTWARE PROVE
    DEFECTIVE IN ANY RESPECT, YOU (NOT THE INITIAL DEVELOPER OR ANY
    OTHER CONTRIBUTOR) ASSUME THE COST OF ANY NECESSARY SERVICING,
    REPAIR OR CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN
    ESSENTIAL PART OF THIS LICENSE. NO USE OF ANY COVERED SOFTWARE IS
    AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.

6. TERMINATION.

    6.1. This License and the rights granted hereunder will terminate
    automatically if You fail to comply with terms herein and fail to
    cure such breach within 30 days of becoming aware of the breach.
    Provisions which, by their nature, must remain in effect beyond the
    termination of this License shall survive.

    6.2. If You assert a patent infringement claim (excluding
    declaratory judgment actions) against Initial Developer or a
    Contributor (the Initial Developer or Contributor against whom You
    assert such claim is referred to as &quot;Participant&quot;) alleging that the
    Participant Software (meaning the Contributor Version where the
    Participant is a Contributor or the Original Software where the
    Participant is the Initial Developer) directly or indirectly
    infringes any patent, then any and all rights granted directly or
    indirectly to You by such Participant, the Initial Developer (if the
    Initial Developer is not the Participant) and all Contributors under
    Sections 2.1 and/or 2.2 of this License shall, upon 60 days notice
    from Participant terminate prospectively and automatically at the
    expiration of such 60 day notice period, unless if within such 60
    day period You withdraw Your claim with respect to the Participant
    Software against such Participant either unilaterally or pursuant to
    a written agreement with Participant.

    6.3. If You assert a patent infringement claim against Participant
    alleging that the Participant Software directly or indirectly
    infringes any patent where such claim is resolved (such as by
    license or settlement) prior to the initiation of patent
    infringement litigation, then the reasonable value of the licenses
    granted by such Participant under Sections 2.1 or 2.2 shall be taken
    into account in determining the amount or value of any payment or
    license.

    6.4. In the event of termination under Sections 6.1 or 6.2 above,
    all end user licenses that have been validly granted by You or any
    distributor hereunder prior to termination (excluding licenses
    granted to You by any distributor) shall survive termination.

7. LIMITATION OF LIABILITY.

    UNDER NO CIRCUMSTANCES AND UNDER NO LEGAL THEORY, WHETHER TORT
    (INCLUDING NEGLIGENCE), CONTRACT, OR OTHERWISE, SHALL YOU, THE
    INITIAL DEVELOPER, ANY OTHER CONTRIBUTOR, OR ANY DISTRIBUTOR OF
    COVERED SOFTWARE, OR ANY SUPPLIER OF ANY OF SUCH PARTIES, BE LIABLE
    TO ANY PERSON FOR ANY INDIRECT, SPECIAL, INCIDENTAL, OR
    CONSEQUENTIAL DAMAGES OF ANY CHARACTER INCLUDING, WITHOUT
    LIMITATION, DAMAGES FOR LOSS OF GOODWILL, WORK STOPPAGE, COMPUTER
    FAILURE OR MALFUNCTION, OR ANY AND ALL OTHER COMMERCIAL DAMAGES OR
    LOSSES, EVEN IF SUCH PARTY SHALL HAVE BEEN INFORMED OF THE
    POSSIBILITY OF SUCH DAMAGES. THIS LIMITATION OF LIABILITY SHALL NOT
    APPLY TO LIABILITY FOR DEATH OR PERSONAL INJURY RESULTING FROM SUCH
    PARTY&#x27;S NEGLIGENCE TO THE EXTENT APPLICABLE LAW PROHIBITS SUCH
    LIMITATION. SOME JURISDICTIONS DO NOT ALLOW THE EXCLUSION OR
    LIMITATION OF INCIDENTAL OR CONSEQUENTIAL DAMAGES, SO THIS EXCLUSION
    AND LIMITATION MAY NOT APPLY TO YOU.

8. U.S. GOVERNMENT END USERS.

    The Covered Software is a &quot;commercial item,&quot; as that term is defined
    in 48 C.F.R. 2.101 (Oct. 1995), consisting of &quot;commercial computer
    software&quot; (as that term is defined at 48 C.F.R. ß
    252.227-7014(a)(1)) and &quot;commercial computer software documentation&quot;
    as such terms are used in 48 C.F.R. 12.212 (Sept. 1995). Consistent
    with 48 C.F.R. 12.212 and 48 C.F.R. 227.7202-1 through 227.7202-4
    (June 1995), all U.S. Government End Users acquire Covered Software
    with only those rights set forth herein. This U.S. Government Rights
    clause is in lieu of, and supersedes, any other FAR, DFAR, or other
    clause or provision that addresses Government rights in computer
    software under this License.

9. MISCELLANEOUS.

    This License represents the complete agreement concerning subject
    matter hereof. If any provision of this License is held to be
    unenforceable, such provision shall be reformed only to the extent
    necessary to make it enforceable. This License shall be governed by
    the law of the jurisdiction specified in a notice contained within
    the Original Software (except to the extent applicable law, if any,
    provides otherwise), excluding such jurisdiction&#x27;s conflict-of-law
    provisions. Any litigation relating to this License shall be subject
    to the jurisdiction of the courts located in the jurisdiction and
    venue specified in a notice contained within the Original Software,
    with the losing party responsible for costs, including, without
    limitation, court costs and reasonable attorneys&#x27; fees and expenses.
    The application of the United Nations Convention on Contracts for
    the International Sale of Goods is expressly excluded. Any law or
    regulation which provides that the language of a contract shall be
    construed against the drafter shall not apply to this License. You
    agree that You alone are responsible for compliance with the United
    States export administration regulations (and the export control
    laws and regulation of any other countries) when You use, distribute
    or otherwise make available any Covered Software.

10. RESPONSIBILITY FOR CLAIMS.

    As between Initial Developer and the Contributors, each party is
    responsible for claims and damages arising, directly or indirectly,
    out of its utilization of rights under this License and You agree to
    work with Initial Developer and Contributors to distribute such
    responsibility on an equitable basis. Nothing herein is intended or
    shall be deemed to constitute any admission of liability.

------------------------------------------------------------------------

NOTICE PURSUANT TO SECTION 9 OF THE COMMON DEVELOPMENT AND DISTRIBUTION
LICENSE (CDDL)

The code released under the CDDL shall be governed by the laws of the
State of California (excluding conflict-of-law provisions). Any
litigation relating to this License shall be subject to the jurisdiction
of the Federal Courts of the Northern District of California and the
state courts of the State of California, with venue lying in Santa Clara
County, California.

```
---
#### <a name="gpl-2.0-with-classpath-exception"></a>GPL-2.0-with-classpath-exception
The following license text for the GPL-2.0-with-classpath-exception license is cited only once.

```
                    GNU GENERAL PUBLIC LICENSE
                       Version 2, June 1991

 Copyright (C) 1989, 1991 Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 Everyone is permitted to copy and distribute verbatim copies
 of this license document, but changing it is not allowed.

                            Preamble

  The licenses for most software are designed to take away your
freedom to share and change it.  By contrast, the GNU General Public
License is intended to guarantee your freedom to share and change free
software--to make sure the software is free for all its users.  This
General Public License applies to most of the Free Software
Foundation&#x27;s software and to any other program whose authors commit to
using it.  (Some other Free Software Foundation software is covered by
the GNU Lesser General Public License instead.)  You can apply it to
your programs, too.

  When we speak of free software, we are referring to freedom, not
price.  Our General Public Licenses are designed to make sure that you
have the freedom to distribute copies of free software (and charge for
this service if you wish), that you receive source code or can get it
if you want it, that you can change the software or use pieces of it
in new free programs; and that you know you can do these things.

  To protect your rights, we need to make restrictions that forbid
anyone to deny you these rights or to ask you to surrender the rights.
These restrictions translate to certain responsibilities for you if you
distribute copies of the software, or if you modify it.

  For example, if you distribute copies of such a program, whether
gratis or for a fee, you must give the recipients all the rights that
you have.  You must make sure that they, too, receive or can get the
source code.  And you must show them these terms so they know their
rights.

  We protect your rights with two steps: (1) copyright the software, and
(2) offer you this license which gives you legal permission to copy,
distribute and/or modify the software.

  Also, for each author&#x27;s protection and ours, we want to make certain
that everyone understands that there is no warranty for this free
software.  If the software is modified by someone else and passed on, we
want its recipients to know that what they have is not the original, so
that any problems introduced by others will not reflect on the original
authors&#x27; reputations.

  Finally, any free program is threatened constantly by software
patents.  We wish to avoid the danger that redistributors of a free
program will individually obtain patent licenses, in effect making the
program proprietary.  To prevent this, we have made it clear that any
patent must be licensed for everyone&#x27;s free use or not licensed at all.

  The precise terms and conditions for copying, distribution and
modification follow.

                    GNU GENERAL PUBLIC LICENSE
   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

  0. This License applies to any program or other work which contains
a notice placed by the copyright holder saying it may be distributed
under the terms of this General Public License.  The &quot;Program&quot;, below,
refers to any such program or work, and a &quot;work based on the Program&quot;
means either the Program or any derivative work under copyright law:
that is to say, a work containing the Program or a portion of it,
either verbatim or with modifications and/or translated into another
language.  (Hereinafter, translation is included without limitation in
the term &quot;modification&quot;.)  Each licensee is addressed as &quot;you&quot;.

Activities other than copying, distribution and modification are not
covered by this License; they are outside its scope.  The act of
running the Program is not restricted, and the output from the Program
is covered only if its contents constitute a work based on the
Program (independent of having been made by running the Program).
Whether that is true depends on what the Program does.

  1. You may copy and distribute verbatim copies of the Program&#x27;s
source code as you receive it, in any medium, provided that you
conspicuously and appropriately publish on each copy an appropriate
copyright notice and disclaimer of warranty; keep intact all the
notices that refer to this License and to the absence of any warranty;
and give any other recipients of the Program a copy of this License
along with the Program.

You may charge a fee for the physical act of transferring a copy, and
you may at your option offer warranty protection in exchange for a fee.

  2. You may modify your copy or copies of the Program or any portion
of it, thus forming a work based on the Program, and copy and
distribute such modifications or work under the terms of Section 1
above, provided that you also meet all of these conditions:

    a) You must cause the modified files to carry prominent notices
    stating that you changed the files and the date of any change.

    b) You must cause any work that you distribute or publish, that in
    whole or in part contains or is derived from the Program or any
    part thereof, to be licensed as a whole at no charge to all third
    parties under the terms of this License.

    c) If the modified program normally reads commands interactively
    when run, you must cause it, when started running for such
    interactive use in the most ordinary way, to print or display an
    announcement including an appropriate copyright notice and a
    notice that there is no warranty (or else, saying that you provide
    a warranty) and that users may redistribute the program under
    these conditions, and telling the user how to view a copy of this
    License.  (Exception: if the Program itself is interactive but
    does not normally print such an announcement, your work based on
    the Program is not required to print an announcement.)

These requirements apply to the modified work as a whole.  If
identifiable sections of that work are not derived from the Program,
and can be reasonably considered independent and separate works in
themselves, then this License, and its terms, do not apply to those
sections when you distribute them as separate works.  But when you
distribute the same sections as part of a whole which is a work based
on the Program, the distribution of the whole must be on the terms of
this License, whose permissions for other licensees extend to the
entire whole, and thus to each and every part regardless of who wrote it.

Thus, it is not the intent of this section to claim rights or contest
your rights to work written entirely by you; rather, the intent is to
exercise the right to control the distribution of derivative or
collective works based on the Program.

In addition, mere aggregation of another work not based on the Program
with the Program (or with a work based on the Program) on a volume of
a storage or distribution medium does not bring the other work under
the scope of this License.

  3. You may copy and distribute the Program (or a work based on it,
under Section 2) in object code or executable form under the terms of
Sections 1 and 2 above provided that you also do one of the following:

    a) Accompany it with the complete corresponding machine-readable
    source code, which must be distributed under the terms of Sections
    1 and 2 above on a medium customarily used for software interchange; or,

    b) Accompany it with a written offer, valid for at least three
    years, to give any third party, for a charge no more than your
    cost of physically performing source distribution, a complete
    machine-readable copy of the corresponding source code, to be
    distributed under the terms of Sections 1 and 2 above on a medium
    customarily used for software interchange; or,

    c) Accompany it with the information you received as to the offer
    to distribute corresponding source code.  (This alternative is
    allowed only for noncommercial distribution and only if you
    received the program in object code or executable form with such
    an offer, in accord with Subsection b above.)

The source code for a work means the preferred form of the work for
making modifications to it.  For an executable work, complete source
code means all the source code for all modules it contains, plus any
associated interface definition files, plus the scripts used to
control compilation and installation of the executable.  However, as a
special exception, the source code distributed need not include
anything that is normally distributed (in either source or binary
form) with the major components (compiler, kernel, and so on) of the
operating system on which the executable runs, unless that component
itself accompanies the executable.

If distribution of executable or object code is made by offering
access to copy from a designated place, then offering equivalent
access to copy the source code from the same place counts as
distribution of the source code, even though third parties are not
compelled to copy the source along with the object code.

  4. You may not copy, modify, sublicense, or distribute the Program
except as expressly provided under this License.  Any attempt
otherwise to copy, modify, sublicense or distribute the Program is
void, and will automatically terminate your rights under this License.
However, parties who have received copies, or rights, from you under
this License will not have their licenses terminated so long as such
parties remain in full compliance.

  5. You are not required to accept this License, since you have not
signed it.  However, nothing else grants you permission to modify or
distribute the Program or its derivative works.  These actions are
prohibited by law if you do not accept this License.  Therefore, by
modifying or distributing the Program (or any work based on the
Program), you indicate your acceptance of this License to do so, and
all its terms and conditions for copying, distributing or modifying
the Program or works based on it.

  6. Each time you redistribute the Program (or any work based on the
Program), the recipient automatically receives a license from the
original licensor to copy, distribute or modify the Program subject to
these terms and conditions.  You may not impose any further
restrictions on the recipients&#x27; exercise of the rights granted herein.
You are not responsible for enforcing compliance by third parties to
this License.

  7. If, as a consequence of a court judgment or allegation of patent
infringement or for any other reason (not limited to patent issues),
conditions are imposed on you (whether by court order, agreement or
otherwise) that contradict the conditions of this License, they do not
excuse you from the conditions of this License.  If you cannot
distribute so as to satisfy simultaneously your obligations under this
License and any other pertinent obligations, then as a consequence you
may not distribute the Program at all.  For example, if a patent
license would not permit royalty-free redistribution of the Program by
all those who receive copies directly or indirectly through you, then
the only way you could satisfy both it and this License would be to
refrain entirely from distribution of the Program.

If any portion of this section is held invalid or unenforceable under
any particular circumstance, the balance of the section is intended to
apply and the section as a whole is intended to apply in other
circumstances.

It is not the purpose of this section to induce you to infringe any
patents or other property right claims or to contest validity of any
such claims; this section has the sole purpose of protecting the
integrity of the free software distribution system, which is
implemented by public license practices.  Many people have made
generous contributions to the wide range of software distributed
through that system in reliance on consistent application of that
system; it is up to the author/donor to decide if he or she is willing
to distribute software through any other system and a licensee cannot
impose that choice.

This section is intended to make thoroughly clear what is believed to
be a consequence of the rest of this License.

  8. If the distribution and/or use of the Program is restricted in
certain countries either by patents or by copyrighted interfaces, the
original copyright holder who places the Program under this License
may add an explicit geographical distribution limitation excluding
those countries, so that distribution is permitted only in or among
countries not thus excluded.  In such case, this License incorporates
the limitation as if written in the body of this License.

  9. The Free Software Foundation may publish revised and/or new versions
of the General Public License from time to time.  Such new versions will
be similar in spirit to the present version, but may differ in detail to
address new problems or concerns.

Each version is given a distinguishing version number.  If the Program
specifies a version number of this License which applies to it and &quot;any
later version&quot;, you have the option of following the terms and conditions
either of that version or of any later version published by the Free
Software Foundation.  If the Program does not specify a version number of
this License, you may choose any version ever published by the Free Software
Foundation.

  10. If you wish to incorporate parts of the Program into other free
programs whose distribution conditions are different, write to the author
to ask for permission.  For software which is copyrighted by the Free
Software Foundation, write to the Free Software Foundation; we sometimes
make exceptions for this.  Our decision will be guided by the two goals
of preserving the free status of all derivatives of our free software and
of promoting the sharing and reuse of software generally.

                            NO WARRANTY

  11. BECAUSE THE PROGRAM IS LICENSED FREE OF CHARGE, THERE IS NO WARRANTY
FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW.  EXCEPT WHEN
OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES
PROVIDE THE PROGRAM &quot;AS IS&quot; WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED
OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  THE ENTIRE RISK AS
TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU.  SHOULD THE
PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING,
REPAIR OR CORRECTION.

  12. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING
WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MAY MODIFY AND/OR
REDISTRIBUTE THE PROGRAM AS PERMITTED ABOVE, BE LIABLE TO YOU FOR DAMAGES,
INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING
OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED
TO LOSS OF DATA OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY
YOU OR THIRD PARTIES OR A FAILURE OF THE PROGRAM TO OPERATE WITH ANY OTHER
PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED OF THE
POSSIBILITY OF SUCH DAMAGES.

                     END OF TERMS AND CONDITIONS

            How to Apply These Terms to Your New Programs

  If you develop a new program, and you want it to be of the greatest
possible use to the public, the best way to achieve this is to make it
free software which everyone can redistribute and change under these terms.

  To do so, attach the following notices to the program.  It is safest
to attach them to the start of each source file to most effectively
convey the exclusion of warranty; and each file should have at least
the &quot;copyright&quot; line and a pointer to where the full notice is found.

    &lt;one line to give the program&#x27;s name and a brief idea of what it does.&gt;
    Copyright (C) &lt;year&gt;  &lt;name of author&gt;

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

Also add information on how to contact you by electronic and paper mail.

If the program is interactive, make it output a short notice like this
when it starts in an interactive mode:

    Gnomovision version 69, Copyright (C) year name of author
    Gnomovision comes with ABSOLUTELY NO WARRANTY; for details type `show w&#x27;.
    This is free software, and you are welcome to redistribute it
    under certain conditions; type `show c&#x27; for details.

The hypothetical commands `show w&#x27; and `show c&#x27; should show the appropriate
parts of the General Public License.  Of course, the commands you use may
be called something other than `show w&#x27; and `show c&#x27;; they could even be
mouse-clicks or menu items--whatever suits your program.

You should also get your employer (if you work as a programmer) or your
school, if any, to sign a &quot;copyright disclaimer&quot; for the program, if
necessary.  Here is a sample; alter the names:

  Yoyodyne, Inc., hereby disclaims all copyright interest in the program
  `Gnomovision&#x27; (which makes passes at compilers) written by James Hacker.

  &lt;signature of Ty Coon&gt;, 1 April 1989
  Ty Coon, President of Vice

This General Public License does not permit incorporating your program into
proprietary programs.  If your program is a subroutine library, you may
consider it more useful to permit linking proprietary applications with the
library.  If this is what you want to do, use the GNU Lesser General
Public License instead of this License.


&quot;CLASSPATH&quot; EXCEPTION TO THE GPL

Certain source files distributed by Oracle America and/or its affiliates are
subject to the following clarification and special exception to the GPL, but
only where Oracle has expressly included in the particular source file&#x27;s header
the words &quot;Oracle designates this particular file as subject to the &quot;Classpath&quot;
exception as provided by Oracle in the LICENSE file that accompanied this code.&quot;

    Linking this library statically or dynamically with other modules is making
    a combined work based on this library.  Thus, the terms and conditions of
    the GNU General Public License cover the whole combination.

    As a special exception, the copyright holders of this library give you
    permission to link this library with independent modules to produce an
    executable, regardless of the license terms of these independent modules,
    and to copy and distribute the resulting executable under terms of your
    choice, provided that you also meet, for each linked independent module,
    the terms and conditions of the license of that module.  An independent
    module is a module which is not derived from or based on this library.  If
    you modify this library, you may extend this exception to your version of
    the library, but you are not obligated to do so.  If you do not wish to do
    so, delete this exception statement from your version.

```
---
#### <a name="mpl-1.1"></a>MPL-1.1
The following license text for the MPL-1.1 license is cited only once.

```
Mozilla Public License Version 1.1

1. Definitions.

1.0.1. &quot;Commercial Use&quot;
      means distribution or otherwise making the Covered Code available to a third party.

1.1. &quot;Contributor&quot;
      means each entity that creates or contributes to the creation of Modifications.

1.2. &quot;Contributor Version&quot;
      means the combination of the Original Code, prior Modifications used by a Contributor, and the Modifications made
      by  that particular Contributor.

1.3. &quot;Covered Code&quot;
      means the Original Code or Modifications or the combination of the Original Code and Modifications, in each case
      including portions thereof.

1.4. &quot;Electronic Distribution Mechanism&quot;
      means a mechanism generally accepted in the software development community for the electronic transfer of data.

1.5. &quot;Executable&quot;
      means Covered Code in any form other than Source Code.

1.6. &quot;Initial Developer&quot;
      means the individual or entity identified as the Initial Developer in the Source Code notice required by Exhibit A.

1.7. &quot;Larger Work&quot;
      means a work which combines Covered Code or portions thereof with code not governed by the terms of this License.

1.8. &quot;License&quot;
      means this document.

1.8.1. &quot;Licensable&quot;
      means having the right to grant, to the maximum extent possible, whether at the time of the initial grant or
      subsequently acquired, any and all of the rights conveyed herein.

1.9. &quot;Modifications&quot;
      means any addition to or deletion from the substance or structure of either the Original Code or any previous
      Modifications. When Covered Code is released as a series of files, a Modification is:

      a. Any addition to or deletion from the contents of a file containing Original Code or previous Modifications.
      b. Any new file that contains any part of the Original Code or previous Modifications.

1.10. &quot;Original Code&quot;
      means Source Code of computer software code which is described in the Source Code notice required by Exhibit A as
      Original Code, and which, at the time of its release under this License is not already Covered Code governed by
      this License.

1.10.1. &quot;Patent Claims&quot;
      means any patent claim(s), now owned or hereafter acquired, including without limitation, method, process, and
      apparatus claims, in any patent Licensable by grantor.

1.11. &quot;Source Code&quot;
      means the preferred form of the Covered Code for making modifications to it, including all modules it contains,
      plus any associated interface definition files, scripts used to control compilation and installation of an
      Executable, or source code differential comparisons against either the Original Code or another well known,
      available Covered Code of the Contributor&#x27;s choice. The Source Code can be in a compressed or archival form,
      provided the appropriate decompression or de-archiving software is widely available for no charge.

1.12. &quot;You&quot; (or &quot;Your&quot;)
      means an individual or a legal entity exercising rights under, and complying with all of the terms of, this
      License or a future version of this License issued under Section 6.1. For legal entities, &quot;You&quot; includes any
      entity which controls, is controlled by, or is under common control with You. For purposes of this definition,
      &quot;control&quot; means (a) the power, direct or indirect, to cause the direction or management of such entity, whether by
      contract or otherwise, or (b) ownership of more than fifty percent (50%) of the outstanding shares or beneficial
      ownership of such entity.


2. Source Code License.

2.1. The Initial Developer Grant.

The Initial Developer hereby grants You a world-wide, royalty-free, non-exclusive license, subject to third party
intellectual property claims:

   a. under intellectual property rights (other than patent or trademark) Licensable by Initial Developer to use,
      reproduce, modify, display, perform, sublicense and distribute the Original Code (or portions thereof) with or
      without Modifications, and/or as part of a Larger Work; and
   b. under Patents Claims infringed by the making, using or selling of Original Code, to make, have made, use,
      practice, sell, and offer for sale, and/or otherwise dispose of the Original Code (or portions thereof).
   c. the licenses granted in this Section 2.1 (a) and (b) are effective on the date Initial Developer first distributes
       Original Code under the terms of this License.
   d. Notwithstanding Section 2.1 (b) above, no patent license is granted: 1) for code that You delete from the Original
      Code; 2) separate from the Original Code; or 3) for infringements caused by: i) the modification of the Original
      Code or ii) the combination of the Original Code with other software or devices.

2.2. Contributor Grant.

Subject to third party intellectual property claims, each Contributor hereby grants You a world-wide, royalty-free, non-exclusive license

   a. under intellectual property rights (other than patent or trademark) Licensable by Contributor, to use, reproduce,
      modify, display, perform, sublicense and distribute the Modifications created by such Contributor (or portions
      thereof) either on an unmodified basis, with other Modifications, as Covered Code and/or as part of a Larger Work;
      and
   b. under Patent Claims infringed by the making, using, or selling of Modifications made by that Contributor either
      alone and/or in combination with its Contributor Version (or portions of such combination), to make, use, sell,
      offer for sale, have made, and/or otherwise dispose of: 1) Modifications made by that Contributor (or portions
      thereof); and 2) the combination of Modifications made by that Contributor with its Contributor Version (or
      portions of such combination).
   c. the licenses granted in Sections 2.2 (a) and 2.2 (b) are effective on the date Contributor first makes Commercial
      Use of the Covered Code.
   d. Notwithstanding Section 2.2 (b) above, no patent license is granted: 1) for any code that Contributor has deleted
      from the Contributor Version; 2) separate from the Contributor Version; 3) for infringements caused by: i) third
      party modifications of Contributor Version or ii) the combination of Modifications made by that Contributor with
      other software (except as part of the Contributor Version) or other devices; or 4) under Patent Claims infringed
      by Covered Code in the absence of Modifications made by that Contributor.

3. Distribution Obligations.

3.1. Application of License.
The Modifications which You create or to which You contribute are governed by the terms of this License, including
without limitation Section 2.2. The Source Code version of Covered Code may be distributed only under the terms of this
License or a future version of this License released under Section 6.1, and You must include a copy of this License with
every copy of the Source Code You distribute. You may not offer or impose any terms on any Source Code version that
alters or restricts the applicable version of this License or the recipients&#x27; rights hereunder. However, You may include
an additional document offering the additional rights described in Section 3.5.

3.2. Availability of Source Code.
Any Modification which You create or to which You contribute must be made available in Source Code form under the terms
of this License either on the same media as an Executable version or via an accepted Electronic Distribution Mechanism
to anyone to whom you made an Executable version available; and if made available via Electronic Distribution Mechanism,
must remain available for at least twelve (12) months after the date it initially became available, or at least six (6)
months after a subsequent version of that particular Modification has been made available to such recipients. You are
responsible for ensuring that the Source Code version remains available even if the Electronic Distribution Mechanism is
maintained by a third party.

3.3. Description of Modifications.
You must cause all Covered Code to which You contribute to contain a file documenting the changes You made to create
that Covered Code and the date of any change. You must include a prominent statement that the Modification is derived,
directly or indirectly, from Original Code provided by the Initial Developer and including the name of the Initial
Developer in (a) the Source Code, and (b) in any notice in an Executable version or related documentation in which You
describe the origin or ownership of the Covered Code.

3.4. Intellectual Property Matters
(a) Third Party Claims
If Contributor has knowledge that a license under a third party&#x27;s intellectual property rights is required to exercise
the rights granted by such Contributor under Sections 2.1 or 2.2, Contributor must include a text file with the Source
Code distribution titled &quot;LEGAL&quot; which describes the claim and the party making the claim in sufficient detail that a
recipient will know whom to contact. If Contributor obtains such knowledge after the Modification is made available as
described in Section 3.2, Contributor shall promptly modify the LEGAL file in all copies Contributor makes available
thereafter and shall take other steps (such as notifying appropriate mailing lists or newsgroups) reasonably calculated
to inform those who received the Covered Code that new knowledge has been obtained.

(b) Contributor APIs
If Contributor&#x27;s Modifications include an application programming interface and Contributor has knowledge of patent
licenses which are reasonably necessary to implement that API, Contributor must also include this information in the
LEGAL file.

(c) Representations.
Contributor represents that, except as disclosed pursuant to Section 3.4 (a) above, Contributor believes that
Contributor&#x27;s Modifications are Contributor&#x27;s original creation(s) and/or Contributor has sufficient rights to grant the
rights conveyed by this License.

3.5. Required Notices.
You must duplicate the notice in Exhibit A in each file of the Source Code. If it is not possible to put such notice in
a particular Source Code file due to its structure, then You must include such notice in a location (such as a relevant
directory) where a user would be likely to look for such a notice. If You created one or more Modification(s) You may
add your name as a Contributor to the notice described in Exhibit A. You must also duplicate this License in any
documentation for the Source Code where You describe recipients&#x27; rights or ownership rights relating to Covered Code.
You may choose to offer, and to charge a fee for, warranty, support, indemnity or liability obligations to one or more
recipients of Covered Code. However, You may do so only on Your own behalf, and not on behalf of the Initial Developer
or any Contributor. You must make it absolutely clear than any such warranty, support, indemnity or liability obligation
is offered by You alone, and You hereby agree to indemnify the Initial Developer and every Contributor for any liability
incurred by the Initial Developer or such Contributor as a result of warranty, support, indemnity or liability terms
You offer.

3.6. Distribution of Executable Versions.
You may distribute Covered Code in Executable form only if the requirements of Sections 3.1, 3.2, 3.3, 3.4 and 3.5 have
been met for that Covered Code, and if You include a notice stating that the Source Code version of the Covered Code is
available under the terms of this License, including a description of how and where You have fulfilled the obligations
of Section 3.2. The notice must be conspicuously included in any notice in an Executable version, related documentation
or collateral in which You describe recipients&#x27; rights relating to the Covered Code. You may distribute the Executable
version of Covered Code or ownership rights under a license of Your choice, which may contain terms different from this
License, provided that You are in compliance with the terms of this License and that the license for the Executable
version does not attempt to limit or alter the recipient&#x27;s rights in the Source Code version from the rights set forth
in this License. If You distribute the Executable version under a different license You must make it absolutely clear
that any terms which differ from this License are offered by You alone, not by the Initial Developer or any Contributor.
You hereby agree to indemnify the Initial Developer and every Contributor for any liability incurred by the Initial
Developer or such Contributor as a result of any such terms You offer.

3.7. Larger Works.
You may create a Larger Work by combining Covered Code with other code not governed by the terms of this License and
distribute the Larger Work as a single product. In such a case, You must make sure the requirements of this License are
fulfilled for the Covered Code.

4. Inability to Comply Due to Statute or Regulation.
If it is impossible for You to comply with any of the terms of this License with respect to some or all of the Covered
Code due to statute, judicial order, or regulation then You must: (a) comply with the terms of this License to the
maximum extent possible; and (b) describe the limitations and the code they affect. Such description must be included in
the LEGAL file described in Section 3.4 and must be included with all distributions of the Source Code. Except to the
extent prohibited by statute or regulation, such description must be sufficiently detailed for a recipient of ordinary
skill to be able to understand it.

5. Application of this License.
This License applies to code to which the Initial Developer has attached the notice in Exhibit A and to related
Covered Code.

6. Versions of the License.
6.1. New Versions
Netscape Communications Corporation (&quot;Netscape&quot;) may publish revised and/or new versions of the License from time to
time. Each version will be given a distinguishing version number.

6.2. Effect of New Versions
Once Covered Code has been published under a particular version of the License, You may always continue to use it under
the terms of that version. You may also choose to use such Covered Code under the terms of any subsequent version of the
License published by Netscape. No one other than Netscape has the right to modify the terms applicable to Covered Code
created under this License.

6.3. Derivative Works
If You create or use a modified version of this License (which you may only do in order to apply it to code which is not
already Covered Code governed by this License), You must (a) rename Your license so that the phrases &quot;Mozilla&quot;,
&quot;MOZILLAPL&quot;, &quot;MOZPL&quot;, &quot;Netscape&quot;, &quot;MPL&quot;, &quot;NPL&quot; or any confusingly similar phrase do not appear in your license (except
to note that your license differs from this License) and (b) otherwise make it clear that Your version of the license
contains terms which differ from the Mozilla Public License and Netscape Public License. (Filling in the name of the
Initial Developer, Original Code or Contributor in the notice described in Exhibit A shall not of themselves be deemed
to be modifications of this License.)

7. DISCLAIMER OF WARRANTY
COVERED CODE IS PROVIDED UNDER THIS LICENSE ON AN &quot;AS IS&quot; BASIS, WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
IMPLIED, INCLUDING, WITHOUT LIMITATION, WARRANTIES THAT THE COVERED CODE IS FREE OF DEFECTS, MERCHANTABLE, FIT FOR A
PARTICULAR PURPOSE OR NON-INFRINGING. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE COVERED CODE IS WITH YOU.
SHOULD ANY COVERED CODE PROVE DEFECTIVE IN ANY RESPECT, YOU (NOT THE INITIAL DEVELOPER OR ANY OTHER CONTRIBUTOR) ASSUME
THE COST OF ANY NECESSARY SERVICING, REPAIR OR CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF
THIS LICENSE. NO USE OF ANY COVERED CODE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.

8. Termination
8.1. This License and the rights granted hereunder will terminate automatically if You fail to comply with terms herein
and fail to cure such breach within 30 days of becoming aware of the breach. All sublicenses to the Covered Code which
are properly granted shall survive any termination of this License. Provisions which, by their nature, must remain in
effect beyond the termination of this License shall survive.

8.2. If You initiate litigation by asserting a patent infringement claim (excluding declatory judgment actions) against
Initial Developer or a Contributor (the Initial Developer or Contributor against whom You file such action is referred
to as &quot;Participant&quot;) alleging that:

   a. such Participant&#x27;s Contributor Version directly or indirectly infringes any patent, then any and all rights
      granted by such Participant to You under Sections 2.1 and/or 2.2 of this License shall, upon 60 days notice from
      Participant terminate prospectively, unless if within 60 days after receipt of notice You either: (i) agree in
      writing to pay Participant a mutually agreeable reasonable royalty for Your past and future use of Modifications
      made by such Participant, or (ii) withdraw Your litigation claim with respect to the Contributor Version against
      such Participant. If within 60 days of notice, a reasonable royalty and payment arrangement are not mutually
      agreed upon in writing by the parties or the litigation claim is not withdrawn, the rights granted by Participant
      to You under Sections 2.1 and/or 2.2 automatically terminate at the expiration of the 60 day notice period
      specified above.
    b. any software, hardware, or device, other than such Participant&#x27;s Contributor Version, directly or indirectly
       infringes any patent, then any rights granted to You by such Participant under Sections 2.1(b) and 2.2(b) are
       revoked effective as of the date You first made, used, sold, distributed, or had made, Modifications made by that
       Participant.

8.3. If You assert a patent infringement claim against Participant alleging that such Participant&#x27;s Contributor Version
directly or indirectly infringes any patent where such claim is resolved (such as by license or settlement) prior to the
initiation of patent infringement litigation, then the reasonable value of the licenses granted by such Participant
under Sections 2.1 or 2.2 shall be taken into account in determining the amount or value of any payment or license.

8.4. In the event of termination under Sections 8.1 or 8.2 above, all end user license agreements (excluding
distributors and resellers) which have been validly granted by You or any distributor hereunder prior to termination
shall survive termination.

9. LIMITATION OF LIABILITY
UNDER NO CIRCUMSTANCES AND UNDER NO LEGAL THEORY, WHETHER TORT (INCLUDING NEGLIGENCE), CONTRACT, OR OTHERWISE, SHALL
YOU, THE INITIAL DEVELOPER, ANY OTHER CONTRIBUTOR, OR ANY DISTRIBUTOR OF COVERED CODE, OR ANY SUPPLIER OF ANY OF SUCH
PARTIES, BE LIABLE TO ANY PERSON FOR ANY INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES OF ANY CHARACTER
INCLUDING, WITHOUT LIMITATION, DAMAGES FOR LOSS OF GOODWILL, WORK STOPPAGE, COMPUTER FAILURE OR MALFUNCTION, OR ANY AND
ALL OTHER COMMERCIAL DAMAGES OR LOSSES, EVEN IF SUCH PARTY SHALL HAVE BEEN INFORMED OF THE POSSIBILITY OF SUCH DAMAGES.
THIS LIMITATION OF LIABILITY SHALL NOT APPLY TO LIABILITY FOR DEATH OR PERSONAL INJURY RESULTING FROM SUCH PARTY&#x27;S
NEGLIGENCE TO THE EXTENT APPLICABLE LAW PROHIBITS SUCH LIMITATION. SOME JURISDICTIONS DO NOT ALLOW THE EXCLUSION OR
LIMITATION OF INCIDENTAL OR CONSEQUENTIAL DAMAGES, SO THIS EXCLUSION AND LIMITATION MAY NOT APPLY TO YOU.

10. U.S. government end users
The Covered Code is a &quot;commercial item,&quot; as that term is defined in 48 C.F.R. 2.101 (Oct. 1995), consisting of
&quot;commercial computer software&quot; and &quot;commercial computer software documentation,&quot; as such terms are used in
48 C.F.R. 12.212 (Sept. 1995). Consistent with 48 C.F.R. 12.212 and 48 C.F.R. 227.7202-1 through 227.7202-4 (June 1995),
all U.S. Government End Users acquire Covered Code with only those rights set forth herein.

11. Miscellaneous
This License represents the complete agreement concerning subject matter hereof. If any provision of this License is
held to be unenforceable, such provision shall be reformed only to the extent necessary to make it enforceable. This
License shall be governed by California law provisions (except to the extent applicable law, if any, provides
otherwise), excluding its conflict-of-law provisions. With respect to disputes in which at least one party is a citizen
of, or an entity chartered or registered to do business in the United States of America, any litigation relating to this
License shall be subject to the jurisdiction of the Federal Courts of the Northern District of California, with venue
lying in Santa Clara County, California, with the losing party responsible for costs, including without limitation,
court costs and reasonable attorneys&#x27; fees and expenses. The application of the United Nations Convention on Contracts
for the International Sale of Goods is expressly excluded. Any law or regulation which provides that the language of a
contract shall be construed against the drafter shall not apply to this License.

12. Responsibility for claims
As between Initial Developer and the Contributors, each party is responsible for claims and damages arising, directly
or indirectly, out of its utilization of rights under this License and You agree to work with Initial Developer and
Contributors to distribute such responsibility on an equitable basis. Nothing herein is intended or shall be deemed to
constitute any admission of liability.

13. Multiple-licensed code
Initial Developer may designate portions of the Covered Code as &quot;Multiple-Licensed&quot;. &quot;Multiple-Licensed&quot; means that the
Initial Developer permits you to utilize portions of the Covered Code under Your choice of the MPL or the alternative
licenses, if any, specified by the Initial Developer in the file described in Exhibit A.



Exhibit A - Mozilla Public License.

&quot;The contents of this file are subject to the Mozilla Public License
Version 1.1 (the &quot;License&quot;); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
https://www.mozilla.org/MPL/

Software distributed under the License is distributed on an &quot;AS IS&quot;
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
License for the specific language governing rights and limitations
under the License.

The Original Code is ______________________________________.

The Initial Developer of the Original Code is ________________________.
Portions created by ______________________ are Copyright (C) ______
_______________________. All Rights Reserved.

Contributor(s): ______________________________________.

Alternatively, the contents of this file may be used under the terms
of the _____ license (the  &quot;[___] License&quot;), in which case the
provisions of [______] License are applicable instead of those
above. If you wish to allow use of your version of this file only
under the terms of the [____] License and not to allow others to use
your version of this file under the MPL, indicate your decision by
deleting the provisions above and replace them with the notice and
other provisions required by the [___] License. If you do not delete
the provisions above, a recipient may use your version of this file
under either the MPL or the [___] License.&quot;

NOTE: The text of this Exhibit A may differ slightly from the text of the notices in the Source Code files of the
Original Code. You should use the text of this Exhibit A rather than the text found in the Original Code Source Code for
Your Modifications.
```
---
#### <a name="ofl-1.1"></a>OFL-1.1
The following license text for the OFL-1.1 license is cited only once.

```
SIL OPEN FONT LICENSE
Version 1.1 - 26 February 2007
PREAMBLE
The goals of the Open Font License (OFL) are to stimulate worldwide
development of collaborative font projects, to support the font creation
efforts of academic and linguistic communities, and to provide a free and
open framework in which fonts may be shared and improved in partnership
with others.
The OFL allows the licensed fonts to be used, studied, modified and
redistributed freely as long as they are not sold by themselves. The
fonts, including any derivative works, can be bundled, embedded,
redistributed and/or sold with any software provided that any reserved
names are not used by derivative works. The fonts and derivatives,
however, cannot be released under any other type of license. The
requirement for fonts to remain under this license does not apply
to any document created using the fonts or their derivatives.
DEFINITIONS
&quot;Font Software&quot; refers to the set of files released by the Copyright
Holder(s) under this license and clearly marked as such. This may
include source files, build scripts and documentation.
&quot;Reserved Font Name&quot; refers to any names specified as such after the
copyright statement(s).
&quot;Original Version&quot; refers to the collection of Font Software components as
distributed by the Copyright Holder(s).
&quot;Modified Version&quot; refers to any derivative made by adding to, deleting,
or substituting - in part or in whole - any of the components of the
Original Version, by changing formats or by porting the Font Software to a
new environment.
&quot;Author&quot; refers to any designer, engineer, programmer, technical
writer or other person who contributed to the Font Software.
PERMISSION &amp; CONDITIONS
Permission is hereby granted, free of charge, to any person obtaining
a copy of the Font Software, to use, study, copy, merge, embed, modify,
redistribute, and sell modified and unmodified copies of the Font
Software, subject to the following conditions:
1) Neither the Font Software nor any of its individual components,
in Original or Modified Versions, may be sold by itself.
2) Original or Modified Versions of the Font Software may be bundled,
redistributed and/or sold with any software, provided that each copy
contains the above copyright notice and this license. These can be
included either as stand-alone text files, human-readable headers or
in the appropriate machine-readable metadata fields within text or
binary files as long as those fields can be easily viewed by the user.
3) No Modified Version of the Font Software may use the Reserved Font
Name(s) unless explicit written permission is granted by the corresponding
Copyright Holder. This restriction only applies to the primary font name as
presented to the users.
4) The name(s) of the Copyright Holder(s) or the Author(s) of the Font
Software shall not be used to promote, endorse or advertise any
Modified Version, except to acknowledge the contribution(s) of the
Copyright Holder(s) and the Author(s) or with their explicit written
permission.
5) The Font Software, modified or unmodified, in part or in whole,
must be distributed entirely under this license, and must not be
distributed under any other license. The requirement for fonts to
remain under this license does not apply to any document created
using the Font Software.
TERMINATION
This license becomes null and void if any of the above conditions are
not met.
DISCLAIMER
THE FONT SOFTWARE IS PROVIDED &quot;AS IS&quot;, WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO ANY WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT
OF COPYRIGHT, PATENT, TRADEMARK, OR OTHER RIGHT. IN NO EVENT SHALL THE
COPYRIGHT HOLDER BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
INCLUDING ANY GENERAL, SPECIAL, INDIRECT, INCIDENTAL, OR CONSEQUENTIAL
DAMAGES, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF THE USE OR INABILITY TO USE THE FONT SOFTWARE OR FROM
OTHER DEALINGS IN THE FONT SOFTWARE.

```
---
#### <a name="cddl-1.0"></a>CDDL-1.0
The following license text for the CDDL-1.0 license is cited only once.

```
COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.0


      1. Definitions.

            1.1. &quot;Contributor&quot; means each individual or entity that
            creates or contributes to the creation of Modifications.

            1.2. &quot;Contributor Version&quot; means the combination of the
            Original Software, prior Modifications used by a
            Contributor (if any), and the Modifications made by that
            particular Contributor.

            1.3. &quot;Covered Software&quot; means (a) the Original Software, or
            (b) Modifications, or (c) the combination of files
            containing Original Software with files containing
            Modifications, in each case including portions thereof.

            1.4. &quot;Executable&quot; means the Covered Software in any form
            other than Source Code.

            1.5. &quot;Initial Developer&quot; means the individual or entity
            that first makes Original Software available under this
            License.

            1.6. &quot;Larger Work&quot; means a work which combines Covered
            Software or portions thereof with code not governed by the
            terms of this License.

            1.7. &quot;License&quot; means this document.

            1.8. &quot;Licensable&quot; means having the right to grant, to the
            maximum extent possible, whether at the time of the initial
            grant or subsequently acquired, any and all of the rights
            conveyed herein.

            1.9. &quot;Modifications&quot; means the Source Code and Executable
            form of any of the following:

                  A. Any file that results from an addition to,
                  deletion from or modification of the contents of a
                  file containing Original Software or previous
                  Modifications;

                  B. Any new file that contains any part of the
                  Original Software or previous Modification; or

                  C. Any new file that is contributed or otherwise made
                  available under the terms of this License.

            1.10. &quot;Original Software&quot; means the Source Code and
            Executable form of computer software code that is
            originally released under this License.

            1.11. &quot;Patent Claims&quot; means any patent claim(s), now owned
            or hereafter acquired, including without limitation,
            method, process, and apparatus claims, in any patent
            Licensable by grantor.

            1.12. &quot;Source Code&quot; means (a) the common form of computer
            software code in which modifications are made and (b)
            associated documentation included in or with such code.

            1.13. &quot;You&quot; (or &quot;Your&quot;) means an individual or a legal
            entity exercising rights under, and complying with all of
            the terms of, this License. For legal entities, &quot;You&quot;
            includes any entity which controls, is controlled by, or is
            under common control with You. For purposes of this
            definition, &quot;control&quot; means (a) the power, direct or
            indirect, to cause the direction or management of such
            entity, whether by contract or otherwise, or (b) ownership
            of more than fifty percent (50%) of the outstanding shares
            or beneficial ownership of such entity.

      2. License Grants.

            2.1. The Initial Developer Grant.

            Conditioned upon Your compliance with Section 3.1 below and
            subject to third party intellectual property claims, the
            Initial Developer hereby grants You a world-wide,
            royalty-free, non-exclusive license:

                  (a) under intellectual property rights (other than
                  patent or trademark) Licensable by Initial Developer,
                  to use, reproduce, modify, display, perform,
                  sublicense and distribute the Original Software (or
                  portions thereof), with or without Modifications,
                  and/or as part of a Larger Work; and

                  (b) under Patent Claims infringed by the making,
                  using or selling of Original Software, to make, have
                  made, use, practice, sell, and offer for sale, and/or
                  otherwise dispose of the Original Software (or
                  portions thereof).

                  (c) The licenses granted in Sections 2.1(a) and (b)
                  are effective on the date Initial Developer first
                  distributes or otherwise makes the Original Software
                  available to a third party under the terms of this
                  License.

                  (d) Notwithstanding Section 2.1(b) above, no patent
                  license is granted: (1) for code that You delete from
                  the Original Software, or (2) for infringements
                  caused by: (i) the modification of the Original
                  Software, or (ii) the combination of the Original
                  Software with other software or devices.

            2.2. Contributor Grant.

            Conditioned upon Your compliance with Section 3.1 below and
            subject to third party intellectual property claims, each
            Contributor hereby grants You a world-wide, royalty-free,
            non-exclusive license:

                  (a) under intellectual property rights (other than
                  patent or trademark) Licensable by Contributor to
                  use, reproduce, modify, display, perform, sublicense
                  and distribute the Modifications created by such
                  Contributor (or portions thereof), either on an
                  unmodified basis, with other Modifications, as
                  Covered Software and/or as part of a Larger Work; and


                  (b) under Patent Claims infringed by the making,
                  using, or selling of Modifications made by that
                  Contributor either alone and/or in combination with
                  its Contributor Version (or portions of such
                  combination), to make, use, sell, offer for sale,
                  have made, and/or otherwise dispose of: (1)
                  Modifications made by that Contributor (or portions
                  thereof); and (2) the combination of Modifications
                  made by that Contributor with its Contributor Version
                  (or portions of such combination).

                  (c) The licenses granted in Sections 2.2(a) and
                  2.2(b) are effective on the date Contributor first
                  distributes or otherwise makes the Modifications
                  available to a third party.

                  (d) Notwithstanding Section 2.2(b) above, no patent
                  license is granted: (1) for any code that Contributor
                  has deleted from the Contributor Version; (2) for
                  infringements caused by: (i) third party
                  modifications of Contributor Version, or (ii) the
                  combination of Modifications made by that Contributor
                  with other software (except as part of the
                  Contributor Version) or other devices; or (3) under
                  Patent Claims infringed by Covered Software in the
                  absence of Modifications made by that Contributor.

      3. Distribution Obligations.

            3.1. Availability of Source Code.

            Any Covered Software that You distribute or otherwise make
            available in Executable form must also be made available in
            Source Code form and that Source Code form must be
            distributed only under the terms of this License. You must
            include a copy of this License with every copy of the
            Source Code form of the Covered Software You distribute or
            otherwise make available. You must inform recipients of any
            such Covered Software in Executable form as to how they can
            obtain such Covered Software in Source Code form in a
            reasonable manner on or through a medium customarily used
            for software exchange.

            3.2. Modifications.

            The Modifications that You create or to which You
            contribute are governed by the terms of this License. You
            represent that You believe Your Modifications are Your
            original creation(s) and/or You have sufficient rights to
            grant the rights conveyed by this License.

            3.3. Required Notices.

            You must include a notice in each of Your Modifications
            that identifies You as the Contributor of the Modification.
            You may not remove or alter any copyright, patent or
            trademark notices contained within the Covered Software, or
            any notices of licensing or any descriptive text giving
            attribution to any Contributor or the Initial Developer.

            3.4. Application of Additional Terms.

            You may not offer or impose any terms on any Covered
            Software in Source Code form that alters or restricts the
            applicable version of this License or the recipients&quot;
            rights hereunder. You may choose to offer, and to charge a
            fee for, warranty, support, indemnity or liability
            obligations to one or more recipients of Covered Software.
            However, you may do so only on Your own behalf, and not on
            behalf of the Initial Developer or any Contributor. You
            must make it absolutely clear that any such warranty,
            support, indemnity or liability obligation is offered by
            You alone, and You hereby agree to indemnify the Initial
            Developer and every Contributor for any liability incurred
            by the Initial Developer or such Contributor as a result of
            warranty, support, indemnity or liability terms You offer.


            3.5. Distribution of Executable Versions.

            You may distribute the Executable form of the Covered
            Software under the terms of this License or under the terms
            of a license of Your choice, which may contain terms
            different from this License, provided that You are in
            compliance with the terms of this License and that the
            license for the Executable form does not attempt to limit
            or alter the recipient&quot;s rights in the Source Code form
            from the rights set forth in this License. If You
            distribute the Covered Software in Executable form under a
            different license, You must make it absolutely clear that
            any terms which differ from this License are offered by You
            alone, not by the Initial Developer or Contributor. You
            hereby agree to indemnify the Initial Developer and every
            Contributor for any liability incurred by the Initial
            Developer or such Contributor as a result of any such terms
            You offer.

            3.6. Larger Works.

            You may create a Larger Work by combining Covered Software
            with other code not governed by the terms of this License
            and distribute the Larger Work as a single product. In such
            a case, You must make sure the requirements of this License
            are fulfilled for the Covered Software.

      4. Versions of the License.

            4.1. New Versions.

            Sun Microsystems, Inc. is the initial license steward and
            may publish revised and/or new versions of this License
            from time to time. Each version will be given a
            distinguishing version number. Except as provided in
            Section 4.3, no one other than the license steward has the
            right to modify this License.

            4.2. Effect of New Versions.

            You may always continue to use, distribute or otherwise
            make the Covered Software available under the terms of the
            version of the License under which You originally received
            the Covered Software. If the Initial Developer includes a
            notice in the Original Software prohibiting it from being
            distributed or otherwise made available under any
            subsequent version of the License, You must distribute and
            make the Covered Software available under the terms of the
            version of the License under which You originally received
            the Covered Software. Otherwise, You may also choose to
            use, distribute or otherwise make the Covered Software
            available under the terms of any subsequent version of the
            License published by the license steward.

            4.3. Modified Versions.

            When You are an Initial Developer and You want to create a
            new license for Your Original Software, You may create and
            use a modified version of this License if You: (a) rename
            the license and remove any references to the name of the
            license steward (except to note that the license differs
            from this License); and (b) otherwise make it clear that
            the license contains terms which differ from this License.


      5. DISCLAIMER OF WARRANTY.

      COVERED SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN &quot;AS IS&quot;
      BASIS, WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED,
      INCLUDING, WITHOUT LIMITATION, WARRANTIES THAT THE COVERED
      SOFTWARE IS FREE OF DEFECTS, MERCHANTABLE, FIT FOR A PARTICULAR
      PURPOSE OR NON-INFRINGING. THE ENTIRE RISK AS TO THE QUALITY AND
      PERFORMANCE OF THE COVERED SOFTWARE IS WITH YOU. SHOULD ANY
      COVERED SOFTWARE PROVE DEFECTIVE IN ANY RESPECT, YOU (NOT THE
      INITIAL DEVELOPER OR ANY OTHER CONTRIBUTOR) ASSUME THE COST OF
      ANY NECESSARY SERVICING, REPAIR OR CORRECTION. THIS DISCLAIMER OF
      WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE. NO USE OF
      ANY COVERED SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS
      DISCLAIMER.

      6. TERMINATION.

            6.1. This License and the rights granted hereunder will
            terminate automatically if You fail to comply with terms
            herein and fail to cure such breach within 30 days of
            becoming aware of the breach. Provisions which, by their
            nature, must remain in effect beyond the termination of
            this License shall survive.

            6.2. If You assert a patent infringement claim (excluding
            declaratory judgment actions) against Initial Developer or
            a Contributor (the Initial Developer or Contributor against
            whom You assert such claim is referred to as &quot;Participant&quot;)
            alleging that the Participant Software (meaning the
            Contributor Version where the Participant is a Contributor
            or the Original Software where the Participant is the
            Initial Developer) directly or indirectly infringes any
            patent, then any and all rights granted directly or
            indirectly to You by such Participant, the Initial
            Developer (if the Initial Developer is not the Participant)
            and all Contributors under Sections 2.1 and/or 2.2 of this
            License shall, upon 60 days notice from Participant
            terminate prospectively and automatically at the expiration
            of such 60 day notice period, unless if within such 60 day
            period You withdraw Your claim with respect to the
            Participant Software against such Participant either
            unilaterally or pursuant to a written agreement with
            Participant.

            6.3. In the event of termination under Sections 6.1 or 6.2
            above, all end user licenses that have been validly granted
            by You or any distributor hereunder prior to termination
            (excluding licenses granted to You by any distributor)
            shall survive termination.

      7. LIMITATION OF LIABILITY.

      UNDER NO CIRCUMSTANCES AND UNDER NO LEGAL THEORY, WHETHER TORT
      (INCLUDING NEGLIGENCE), CONTRACT, OR OTHERWISE, SHALL YOU, THE
      INITIAL DEVELOPER, ANY OTHER CONTRIBUTOR, OR ANY DISTRIBUTOR OF
      COVERED SOFTWARE, OR ANY SUPPLIER OF ANY OF SUCH PARTIES, BE
      LIABLE TO ANY PERSON FOR ANY INDIRECT, SPECIAL, INCIDENTAL, OR
      CONSEQUENTIAL DAMAGES OF ANY CHARACTER INCLUDING, WITHOUT
      LIMITATION, DAMAGES FOR LOST PROFITS, LOSS OF GOODWILL, WORK
      STOPPAGE, COMPUTER FAILURE OR MALFUNCTION, OR ANY AND ALL OTHER
      COMMERCIAL DAMAGES OR LOSSES, EVEN IF SUCH PARTY SHALL HAVE BEEN
      INFORMED OF THE POSSIBILITY OF SUCH DAMAGES. THIS LIMITATION OF
      LIABILITY SHALL NOT APPLY TO LIABILITY FOR DEATH OR PERSONAL
      INJURY RESULTING FROM SUCH PARTY&quot;S NEGLIGENCE TO THE EXTENT
      APPLICABLE LAW PROHIBITS SUCH LIMITATION. SOME JURISDICTIONS DO
      NOT ALLOW THE EXCLUSION OR LIMITATION OF INCIDENTAL OR
      CONSEQUENTIAL DAMAGES, SO THIS EXCLUSION AND LIMITATION MAY NOT
      APPLY TO YOU.

      8. U.S. GOVERNMENT END USERS.

      The Covered Software is a &quot;commercial item,&quot; as that term is
      defined in 48 C.F.R. 2.101 (Oct. 1995), consisting of &quot;commercial
      computer software&quot; (as that term is defined at 48 C.F.R. &quot;
      252.227-7014(a)(1)) and &quot;commercial computer software
      documentation&quot; as such terms are used in 48 C.F.R. 12.212 (Sept.
      1995). Consistent with 48 C.F.R. 12.212 and 48 C.F.R. 227.7202-1
      through 227.7202-4 (June 1995), all U.S. Government End Users
      acquire Covered Software with only those rights set forth herein.
      This U.S. Government Rights clause is in lieu of, and supersedes,
      any other FAR, DFAR, or other clause or provision that addresses
      Government rights in computer software under this License.

      9. MISCELLANEOUS.

      This License represents the complete agreement concerning subject
      matter hereof. If any provision of this License is held to be
      unenforceable, such provision shall be reformed only to the
      extent necessary to make it enforceable. This License shall be
      governed by the law of the jurisdiction specified in a notice
      contained within the Original Software (except to the extent
      applicable law, if any, provides otherwise), excluding such
      jurisdiction&quot;s conflict-of-law provisions. Any litigation
      relating to this License shall be subject to the jurisdiction of
      the courts located in the jurisdiction and venue specified in a
      notice contained within the Original Software, with the losing
      party responsible for costs, including, without limitation, court
      costs and reasonable attorneys&quot; fees and expenses. The
      application of the United Nations Convention on Contracts for the
      International Sale of Goods is expressly excluded. Any law or
      regulation which provides that the language of a contract shall
      be construed against the drafter shall not apply to this License.
      You agree that You alone are responsible for compliance with the
      United States export administration regulations (and the export
      control laws and regulation of any other countries) when You use,
      distribute or otherwise make available any Covered Software.

      10. RESPONSIBILITY FOR CLAIMS.

      As between Initial Developer and the Contributors, each party is
      responsible for claims and damages arising, directly or
      indirectly, out of its utilization of rights under this License
      and You agree to work with Initial Developer and Contributors to
      distribute such responsibility on an equitable basis. Nothing
      herein is intended or shall be deemed to constitute any admission
      of liability.

```
---
#### <a name="mit"></a>MIT
The following license text for the MIT license is cited only once.

```
The MIT License
SPDX short identifier: MIT
Copyright &lt;YEAR&gt; &lt;COPYRIGHT HOLDER&gt;
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the &quot;Software&quot;), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED &quot;AS IS&quot;, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

```
---
#### <a name="mit-0"></a>MIT-0
The following license text for the MIT-0 license is cited only once.

```
MIT No Attribution

Copyright &lt;YEAR&gt; &lt;COPYRIGHT HOLDER&gt;

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
documentation files (the &quot;Software&quot;), to deal in the Software without restriction, including without limitation the
rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
persons to whom the Software is furnished to do so.

THE SOFTWARE IS PROVIDED &quot;AS IS&quot;, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```
---
#### <a name="lgpl-2.1-or-later"></a>LGPL-2.1-or-later
The following license text for the LGPL-2.1-or-later license is cited only once.

```
GNU Lesser General Public License version 2.1
Version 2.1
SPDX short identifier: LGPL-2.1
Steward:Free Software Foundation

Copyright (C) 1991, 1999 Free Software Foundation, Inc. 59 Temple Place, Suite 330, Boston, MA
02111-1307 USA Everyone is permitted to copy and distribute verbatim copies of this license
document, but changing it is not allowed.

[This is the first released version of the Lesser GPL. It also counts as the successor of the GNU
Library Public License, version 2, hence the version number 2.1.]


Preamble

The licenses for most software are designed to take away your freedom to share and change it.
By contrast, the GNU General Public Licenses are intended to guarantee your freedom to share
and change free software–to make sure the software is free for all its users.

This license, the Lesser General Public License, applies to some specially designated software
packages–typically libraries–of the Free Software Foundation and other authors who decide to
use it. You can use it too, but we suggest you first think carefully about whether this license or
the ordinary General Public License is the better strategy to use in any particular case, based on
the explanations below.

When we speak of free software, we are referring to freedom of use, not price. Our General
Public Licenses are designed to make sure that you have the freedom to distribute copies of free
software (and charge for this service if you wish); that you receive source code or can get it if you
want it; that you can change the software and use pieces of it in new free programs; and that you
are informed that you can do these things.

To protect your rights, we need to make restrictions that forbid distributors to deny you these
rights or to ask you to surrender these rights. These restrictions translate to certain
responsibilities for you if you distribute copies of the library or if you modify it.

For example, if you distribute copies of the library, whether gratis or for a fee, you must give the
recipients all the rights that we gave you. You must make sure that they, too, receive or can get
the source code. If you link other code with the library, you must provide complete object files to
the recipients, so that they can relink them with the library after making changes to the library
and recompiling it. And you must show them these terms so they know their rights.

We protect your rights with a two-step method: (1) we copyright the library, and (2) we offer you
this license, which gives you legal permission to copy, distribute and/or modify the library.

To protect each distributor, we want to make it very clear that there is no warranty for the free
library. Also, if the library is modified by someone else and passed on, the recipients should know
that what they have is not the original version, so that the original author’s reputation will not be
affected by problems that might be introduced by others.

Finally, software patents pose a constant threat to the existence of any free program. We wish to
make sure that a company cannot effectively restrict the users of a free program by obtaining a
restrictive license from a patent holder. Therefore, we insist that any patent license obtained for a
version of the library must be consistent with the full freedom of use specified in this license.

Most GNU software, including some libraries, is covered by the ordinary GNU General Public
License. This license, the GNU Lesser General Public License, applies to certain designated
libraries, and is quite different from the ordinary General Public License. We use this license for
certain libraries in order to permit linking those libraries into non-free programs.

When a program is linked with a library, whether statically or using a shared library, the
combination of the two is legally speaking a combined work, a derivative of the original library.
The ordinary General Public License therefore permits such linking only if the entire combination
fits its criteria of freedom. The Lesser General Public License permits more lax criteria for linking
other code with the library.

We call this license the “Lesser” General Public License because it does Less to protect the
user’s freedom than the ordinary General Public License. It also provides other free software
developers Less of an advantage over competing non-free programs. These disadvantages are
the reason we use the ordinary General Public License for many libraries. However, the Lesser
license provides advantages in certain special circumstances.

For example, on rare occasions, there may be a special need to encourage the widest possible
use of a certain library, so that it becomes a de-facto standard. To achieve this, non-free
programs must be allowed to use the library. A more frequent case is that a free library does the
same job as widely used non-free libraries. In this case, there is little to gain by limiting the free
library to free software only, so we use the Lesser General Public License.

In other cases, permission to use a particular library in non-free programs enables a greater
number of people to use a large body of free software. For example, permission to use the GNU C
Library in non-free programs enables many more people to use the whole GNU operating
system, as well as its variant, the GNU/Linux operating system.

Although the Lesser General Public License is Less protective of the users’ freedom, it does
ensure that the user of a program that is linked with the Library has the freedom and the
wherewithal to run that program using a modified version of the Library.

The precise terms and conditions for copying, distribution and modification follow. Pay close
attention to the difference between a “work based on the library” and a “work that uses the
library”. The former contains code derived from the library, whereas the latter must be combined
with the library in order to run.


TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

0. This License Agreement applies to any software library or other program which contains a
   notice placed by the copyright holder or other authorized party saying it may be distributed
   under the terms of this Lesser General Public License (also called “this License”). Each licensee
   is addressed as “you”.

A “library” means a collection of software functions and/or data prepared so as to be
conveniently linked with application programs (which use some of those functions and data) to
form executables.

The “Library”, below, refers to any such software library or work which has been distributed
under these terms. A “work based on the Library” means either the Library or any derivative work
under copyright law: that is to say, a work containing the Library or a portion of it, either verbatim
or with modifications and/or translated straightforwardly into another language. (Hereinafter,
translation is included without limitation in the term “modification”.)

“Source code” for a work means the preferred form of the work for making modifications to it.
For a library, complete source code means all the source code for all modules it contains, plus any
associated interface definition files, plus the scripts used to control compilation and installation
of the library.

Activities other than copying, distribution and modification are not covered by this License; they
are outside its scope. The act of running a program using the Library is not restricted, and output
from such a program is covered only if its contents constitute a work based on the Library
(independent of the use of the Library in a tool for writing it). Whether that is true depends on
what the Library does and what the program that uses the Library does.

1. You may copy and distribute verbatim copies of the Library’s complete source code as you
   receive it, in any medium, provided that you conspicuously and appropriately publish on each
   copy an appropriate copyright notice and disclaimer of warranty; keep intact all the notices that
   refer to this License and to the absence of any warranty; and distribute a copy of this License
   along with the Library.

   You may charge a fee for the physical act of transferring a copy, and you may at your option offer
   warranty protection in exchange for a fee.

2. You may modify your copy or copies of the Library or any portion of it, thus forming a work
   based on the Library, and copy and distribute such modifications or work under the terms of
   Section 1 above, provided that you also meet all of these conditions:

   a) The modified work must itself be a software library.

   b) You must cause the files modified to carry prominent notices stating that you changed the
      files and the date of any change.

   c) You must cause the whole of the work to be licensed at no charge to all third parties under the
      terms of this License.

   d) If a facility in the modified Library refers to a function or a table of data to be supplied by an
      application program that uses the facility, other than as an argument passed when the facility is
      invoked, then you must make a good faith effort to ensure that, in the event an application does
      not supply such function or table, the facility still operates, and performs whatever part of its
      purpose remains meaningful.

      (For example, a function in a library to compute square roots has a purpose that is entirely well-
      defined independent of the application. Therefore, Subsection 2d requires that any application-
      supplied function or table used by this function must be optional: if the application does not
      supply it, the square root function must still compute square roots.)

      These requirements apply to the modified work as a whole. If identifiable sections of that work
      are not derived from the Library, and can be reasonably considered independent and separate
      works in themselves, then this License, and its terms, do not apply to those sections when you
      distribute them as separate works. But when you distribute the same sections as part of a whole
      which is a work based on the Library, the distribution of the whole must be on the terms of this
      License, whose permissions for other licensees extend to the entire whole, and thus to each and
      every part regardless of who wrote it.

      Thus, it is not the intent of this section to claim rights or contest your rights to work written
      entirely by you; rather, the intent is to exercise the right to control the distribution of derivative
      or collective works based on the Library.

      In addition, mere aggregation of another work not based on the Library with the Library (or with a
      work based on the Library) on a volume of a storage or distribution medium does not bring the
      other work under the scope of this License.

3. You may opt to apply the terms of the ordinary GNU General Public License instead of this
   License to a given copy of the Library. To do this, you must alter all the notices that refer to this
   License, so that they refer to the ordinary GNU General Public License, version 2, instead of to
   this License. (If a newer version than version 2 of the ordinary GNU General Public License has
   appeared, then you can specify that version instead if you wish.) Do not make any other change in
   these notices.

   Once this change is made in a given copy, it is irreversible for that copy, so the ordinary GNU
   General Public License applies to all subsequent copies and derivative works made from that copy.

   This option is useful when you wish to copy part of the code of the Library into a program that is
   not a library.

4. You may copy and distribute the Library (or a portion or derivative of it, under Section 2) in
   object code or executable form under the terms of Sections 1 and 2 above provided that you
   accompany it with the complete corresponding machine-readable source code, which must be
   distributed under the terms of Sections 1 and 2 above on a medium customarily used for
   software interchange.

   If distribution of object code is made by offering access to copy from a designated place, then
   offering equivalent access to copy the source code from the same place satisfies the
   requirement to distribute the source code, even though third parties are not compelled to copy
   the source along with the object code.

5. A program that contains no derivative of any portion of the Library, but is designed to work
   with the Library by being compiled or linked with it, is called a “work that uses the Library”. Such a
   work, in isolation, is not a derivative work of the Library, and therefore falls outside the scope of
   this License.

   However, linking a “work that uses the Library” with the Library creates an executable that is a
   derivative of the Library (because it contains portions of the Library), rather than a “work that
   uses the library”. The executable is therefore covered by this License. Section 6 states terms
   for distribution of such executables.

   When a “work that uses the Library” uses material from a header file that is part of the Library,
   the object code for the work may be a derivative work of the Library even though the source
   code is not. Whether this is true is especially significant if the work can be linked without the
   Library, or if the work is itself a library. The threshold for this to be true is not precisely defined by
   law.

   If such an object file uses only numerical parameters, data structure layouts and accessors, and
   small macros and small inline functions (ten lines or less in length), then the use of the object file
   is unrestricted, regardless of whether it is legally a derivative work. (Executables containing this
   object code plus portions of the Library will still fall under Section 6.)

   Otherwise, if the work is a derivative of the Library, you may distribute the object code for the
   work under the terms of Section 6. Any executables containing that work also fall under Section
   6, whether or not they are linked directly with the Library itself.

6. As an exception to the Sections above, you may also combine or link a “work that uses the
   Library” with the Library to produce a work containing portions of the Library, and distribute that
   work under terms of your choice, provided that the terms permit modification of the work for the
   customer’s own use and reverse engineering for debugging such modifications.

   You must give prominent notice with each copy of the work that the Library is used in it and that
   the Library and its use are covered by this License. You must supply a copy of this License. If the
   work during execution displays copyright notices, you must include the copyright notice for the
   Library among them, as well as a reference directing the user to the copy of this License. Also, you must do one of these things:

   a) Accompany the work with the complete corresponding machine-readable source code for the
      Library including whatever changes were used in the work (which must be distributed under
      Sections 1 and 2 above); and, if the work is an executable linked with the
      Library, with the complete machine-readable “work that uses the Library”, as object code and/or source code, so
      that the user can modify the Library and then relink to produce a modified executable containing
      the modified Library. (It is understood that the user who changes the contents of definitions files
      in the Library will not necessarily be able to recompile the application to use the modified definitions.)

   b) Use a suitable shared library mechanism for linking with the Library. A suitable mechanism is
      one that (1) uses at run time a copy of the library already present on the user’s computer system,
      rather than copying library functions into the executable, and (2) will operate properly with a
      modified version of the library, if the user installs one, as long as the modified version is
      interface-compatible with the version that the work was made with.

   c) Accompany the work with a written offer, valid for at least three years, to give the same user
      the materials specified in Subsection 6a, above, for a charge no more than the cost of performing
      this distribution.

   d) If distribution of the work is made by offering access to copy from a designated place, offer
      equivalent access to copy the above specified materials from the same place.

   e) Verify that the user has already received a copy of these materials or that you have already
      sent this user a copy.

   For an executable, the required form of the “work that uses the Library” must include any data
   and utility programs needed for reproducing the executable from it. However, as a special
   exception, the materials to be distributed need not include anything that is normally distributed
   (in either source or binary form) with the major components (compiler, kernel, and so on) of the
   operating system on which the executable runs, unless that component itself accompanies the
   executable.

   It may happen that this requirement contradicts the license restrictions of other proprietary
   libraries that do not normally accompany the operating system. Such a contradiction means you
   cannot use both them and the Library together in an executable that you distribute.

7. You may place library facilities that are a work based on the Library side-by-side in a single
   library together with other library facilities not covered by this License, and distribute such a
   combined library, provided that the separate distribution of the work based on the Library and of
   the other library facilities is otherwise permitted, and provided that you do these two things:

   a) Accompany the combined library with a copy of the same work based on the Library,
      uncombined with any other library facilities. This must be distributed under the terms of the
      Sections above.

   b) Give prominent notice with the combined library of the fact that part of it is a work based on
      the Library, and explaining where to find the accompanying uncombined form of the same work.

8. You may not copy, modify, sublicense, link with, or distribute the Library except as expressly
   provided under this License. Any attempt otherwise to copy, modify, sublicense, link with, or
   distribute the Library is void, and will automatically terminate your rights under this License.
   However, parties who have received copies, or rights, from you under this License will not have
   their licenses terminated so long as such parties remain in full compliance.

9. You are not required to accept this License, since you have not signed it. However, nothing else
   grants you permission to modify or distribute the Library or its derivative works. These actions
   are prohibited by law if you do not accept this License. Therefore, by modifying or distributing the
   Library (or any work based on the Library), you indicate your acceptance of this License to do so,
   and all its terms and conditions for copying, distributing or modifying the Library or works based
   on it.

10. Each time you redistribute the Library (or any work based on the Library), the recipient
   automatically receives a license from the original licensor to copy, distribute, link with or modify
   the Library subject to these terms and conditions. You may not impose any further restrictions
   on the recipients’ exercise of the rights granted herein. You are not responsible for enforcing
   compliance by third parties with this License.

11. If, as a consequence of a court judgment or allegation of patent infringement or for any other
   reason (not limited to patent issues), conditions are imposed on you (whether by court order,
   agreement or otherwise) that contradict the conditions of this License, they do not excuse you
   from the conditions of this License. If you cannot distribute so as to satisfy simultaneously your
   obligations under this License and any other pertinent obligations, then as a consequence you
   may not distribute the Library at all. For example, if a patent license would not permit royalty-free
   redistribution of the Library by all those who receive copies directly or indirectly through you,
   then the only way you could satisfy both it and this License would be to refrain entirely from
   distribution of the Library.

   If any portion of this section is held invalid or unenforceable under any particular circumstance,
   the balance of the section is intended to apply, and the section as a whole is intended to apply in
   other circumstances.

   It is not the purpose of this section to induce you to infringe any patents or other property right
   claims or to contest validity of any such claims; this section has the sole purpose of protecting
   the integrity of the free software distribution system which is implemented by public license
   practices. Many people have made generous contributions to the wide range of software
   distributed through that system in reliance on consistent application of that system; it is up to the
   author/donor to decide if he or she is willing to distribute software through any other system and
   a licensee cannot impose that choice.

   This section is intended to make thoroughly clear what is believed to be a consequence of the
   rest of this License.

12. If the distribution and/or use of the Library is restricted in certain countries either by patents
   or by copyrighted interfaces, the original copyright holder who places the Library under this
   License may add an explicit geographical distribution limitation excluding those countries, so
   that distribution is permitted only in or among countries not thus excluded. In such case, this
   License incorporates the limitation as if written in the body of this License.

13. The Free Software Foundation may publish revised and/or new versions of the Lesser
   General Public License from time to time. Such new versions will be similar in spirit to the
   present version, but may differ in detail to address new problems or concerns.

   Each version is given a distinguishing version number. If the Library specifies a version number of
   this License which applies to it and “any later version”, you have the option of following the terms
   and conditions either of that version or of any later version published by the Free Software
   Foundation. If the Library does not specify a license version number, you may choose any version
   ever published by the Free Software Foundation.

14. If you wish to incorporate parts of the Library into other free programs whose distribution
   conditions are incompatible with these, write to the author to ask for permission. For software
   which is copyrighted by the Free Software Foundation, write to the Free Software Foundation; we
   sometimes make exceptions for this. Our decision will be guided by the two goals of preserving
   the free status of all derivatives of our free software and of promoting the sharing and reuse of
   software generally.

NO WARRANTY

15. BECAUSE THE LIBRARY IS LICENSED FREE OF CHARGE, THERE IS NO WARRANTY FOR
THE LIBRARY, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN
OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES
PROVIDE THE LIBRARY “AS IS” WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO
THE QUALITY AND PERFORMANCE OF THE LIBRARY IS WITH YOU. SHOULD THE LIBRARY
PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR
CORRECTION.

16. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL
ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MAY MODIFY AND/OR
REDISTRIBUTE THE LIBRARY AS PERMITTED ABOVE, BE LIABLE TO YOU FOR DAMAGES,
INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING
OUT OF THE USE OR INABILITY TO USE THE LIBRARY (INCLUDING BUT NOT LIMITED TO
LOSS OF DATA OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR
THIRD PARTIES OR A FAILURE OF THE LIBRARY TO OPERATE WITH ANY OTHER SOFTWARE),
EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH
DAMAGES.

END OF TERMS AND CONDITIONS


How to Apply These Terms to Your New Libraries

If you develop a new library, and you want it to be of the greatest possible use to the public, we
recommend making it free software that everyone can redistribute and change. You can do so by
permitting redistribution under these terms (or, alternatively, under the terms of the ordinary
General Public License).

To apply these terms, attach the following notices to the library. It is safest to attach them to the
start of each source file to most effectively convey the exclusion of warranty; and each file
should have at least the “copyright” line and a pointer to where the full notice is found.

&lt;one line to give the library’s name and an idea of what it does.&gt; Copyright (C) &lt;year&gt; &lt;name of
author&gt;

This library is free software; you can redistribute it and/or modify it under the terms of the GNU
Lesser General Public License as published by the Free Software Foundation; either version 2.1
of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
PURPOSE. See the GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License along with this
library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
MA 02111-1307 USA

Also add information on how to contact you by electronic and paper mail.

You should also get your employer (if you work as a programmer) or your school, if any, to sign a
“copyright disclaimer” for the library, if necessary. Here is a sample; alter the names:

Yoyodyne, Inc., hereby disclaims all copyright interest in the library `Frob’ (a library for tweaking
knobs) written by James Random Hacker.

signature of Ty Coon, 1 April 1990
Ty Coon, President of Vice

That’s all there is to it!


```
---
#### <a name="bsd-2-clause"></a>BSD-2-Clause
The following license text for the BSD-2-Clause license is cited only once.

```
Note: This license has also been called the “Simplified BSD License” and the “FreeBSD License”. See also the 3-clause BSD License.

Copyright &lt;YEAR&gt; &lt;COPYRIGHT HOLDER&gt;

Redistribution and use in source and binary forms, with or without modification, are permitted
provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and
   the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions
   and the following disclaimer in the documentation and/or other materials provided with the
   distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS”
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.
```
---
#### <a name="isc"></a>ISC
The following license text for the ISC license is cited only once.

```
ISC License (ISC)
Copyright &lt;YEAR&gt; &lt;OWNER&gt;
Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted, provided that the above copyright notice and this permission notice appear in all copies.
THE SOFTWARE IS PROVIDED &quot;AS IS&quot; AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

```
---
#### <a name="bpmn.io"></a>bpmn.io
The following license text for the bpmn.io license is cited only once.

```
bpmn.io License
Copyright (c) 2014-present Camunda Services GmbH
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the &quot;Software&quot;), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
The source code responsible for displaying the bpmn.io logo (two green cogwheels in a box) that links back to http://bpmn.io as part of rendered diagrams MUST NOT be removed or changed. When this software is being used in a website or application, the logo must stay fully visible and not visually overlapped by other elements.
THE SOFTWARE IS PROVIDED &quot;AS IS&quot;, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

```
---
#### <a name="apache-2.0-with-llvm-exception"></a>Apache-2.0-with-LLVM-exception
The following license text for the Apache-2.0-with-LLVM-exception license is cited only once.

```

                              Apache License
                        Version 2.0, January 2004
                     http://www.apache.org/licenses/

TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

1. Definitions.

   &quot;License&quot; shall mean the terms and conditions for use, reproduction,
   and distribution as defined by Sections 1 through 9 of this document.

   &quot;Licensor&quot; shall mean the copyright owner or entity authorized by
   the copyright owner that is granting the License.

   &quot;Legal Entity&quot; shall mean the union of the acting entity and all
   other entities that control, are controlled by, or are under common
   control with that entity. For the purposes of this definition,
   &quot;control&quot; means (i) the power, direct or indirect, to cause the
   direction or management of such entity, whether by contract or
   otherwise, or (ii) ownership of fifty percent (50%) or more of the
   outstanding shares, or (iii) beneficial ownership of such entity.

   &quot;You&quot; (or &quot;Your&quot;) shall mean an individual or Legal Entity
   exercising permissions granted by this License.

   &quot;Source&quot; form shall mean the preferred form for making modifications,
   including but not limited to software source code, documentation
   source, and configuration files.

   &quot;Object&quot; form shall mean any form resulting from mechanical
   transformation or translation of a Source form, including but
   not limited to compiled object code, generated documentation,
   and conversions to other media types.

   &quot;Work&quot; shall mean the work of authorship, whether in Source or
   Object form, made available under the License, as indicated by a
   copyright notice that is included in or attached to the work
   (an example is provided in the Appendix below).

   &quot;Derivative Works&quot; shall mean any work, whether in Source or Object
   form, that is based on (or derived from) the Work and for which the
   editorial revisions, annotations, elaborations, or other modifications
   represent, as a whole, an original work of authorship. For the purposes
   of this License, Derivative Works shall not include works that remain
   separable from, or merely link (or bind by name) to the interfaces of,
   the Work and Derivative Works thereof.

   &quot;Contribution&quot; shall mean any work of authorship, including
   the original version of the Work and any modifications or additions
   to that Work or Derivative Works thereof, that is intentionally
   submitted to Licensor for inclusion in the Work by the copyright owner
   or by an individual or Legal Entity authorized to submit on behalf of
   the copyright owner. For the purposes of this definition, &quot;submitted&quot;
   means any form of electronic, verbal, or written communication sent
   to the Licensor or its representatives, including but not limited to
   communication on electronic mailing lists, source code control systems,
   and issue tracking systems that are managed by, or on behalf of, the
   Licensor for the purpose of discussing and improving the Work, but
   excluding communication that is conspicuously marked or otherwise
   designated in writing by the copyright owner as &quot;Not a Contribution.&quot;

   &quot;Contributor&quot; shall mean Licensor and any individual or Legal Entity
   on behalf of whom a Contribution has been received by Licensor and
   subsequently incorporated within the Work.

2. Grant of Copyright License. Subject to the terms and conditions of
   this License, each Contributor hereby grants to You a perpetual,
   worldwide, non-exclusive, no-charge, royalty-free, irrevocable
   copyright license to reproduce, prepare Derivative Works of,
   publicly display, publicly perform, sublicense, and distribute the
   Work and such Derivative Works in Source or Object form.

3. Grant of Patent License. Subject to the terms and conditions of
   this License, each Contributor hereby grants to You a perpetual,
   worldwide, non-exclusive, no-charge, royalty-free, irrevocable
   (except as stated in this section) patent license to make, have made,
   use, offer to sell, sell, import, and otherwise transfer the Work,
   where such license applies only to those patent claims licensable
   by such Contributor that are necessarily infringed by their
   Contribution(s) alone or by combination of their Contribution(s)
   with the Work to which such Contribution(s) was submitted. If You
   institute patent litigation against any entity (including a
   cross-claim or counterclaim in a lawsuit) alleging that the Work
   or a Contribution incorporated within the Work constitutes direct
   or contributory patent infringement, then any patent licenses
   granted to You under this License for that Work shall terminate
   as of the date such litigation is filed.

4. Redistribution. You may reproduce and distribute copies of the
   Work or Derivative Works thereof in any medium, with or without
   modifications, and in Source or Object form, provided that You
   meet the following conditions:

   (a) You must give any other recipients of the Work or
       Derivative Works a copy of this License; and

   (b) You must cause any modified files to carry prominent notices
       stating that You changed the files; and

   (c) You must retain, in the Source form of any Derivative Works
       that You distribute, all copyright, patent, trademark, and
       attribution notices from the Source form of the Work,
       excluding those notices that do not pertain to any part of
       the Derivative Works; and

   (d) If the Work includes a &quot;NOTICE&quot; text file as part of its
       distribution, then any Derivative Works that You distribute must
       include a readable copy of the attribution notices contained
       within such NOTICE file, excluding those notices that do not
       pertain to any part of the Derivative Works, in at least one
       of the following places: within a NOTICE text file distributed
       as part of the Derivative Works; within the Source form or
       documentation, if provided along with the Derivative Works; or,
       within a display generated by the Derivative Works, if and
       wherever such third-party notices normally appear. The contents
       of the NOTICE file are for informational purposes only and
       do not modify the License. You may add Your own attribution
       notices within Derivative Works that You distribute, alongside
       or as an addendum to the NOTICE text from the Work, provided
       that such additional attribution notices cannot be construed
       as modifying the License.

   You may add Your own copyright statement to Your modifications and
   may provide additional or different license terms and conditions
   for use, reproduction, or distribution of Your modifications, or
   for any such Derivative Works as a whole, provided Your use,
   reproduction, and distribution of the Work otherwise complies with
   the conditions stated in this License.

5. Submission of Contributions. Unless You explicitly state otherwise,
   any Contribution intentionally submitted for inclusion in the Work
   by You to the Licensor shall be under the terms and conditions of
   this License, without any additional terms or conditions.
   Notwithstanding the above, nothing herein shall supersede or modify
   the terms of any separate license agreement you may have executed
   with Licensor regarding such Contributions.

6. Trademarks. This License does not grant permission to use the trade
   names, trademarks, service marks, or product names of the Licensor,
   except as required for reasonable and customary use in describing the
   origin of the Work and reproducing the content of the NOTICE file.

7. Disclaimer of Warranty. Unless required by applicable law or
   agreed to in writing, Licensor provides the Work (and each
   Contributor provides its Contributions) on an &quot;AS IS&quot; BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
   implied, including, without limitation, any warranties or conditions
   of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
   PARTICULAR PURPOSE. You are solely responsible for determining the
   appropriateness of using or redistributing the Work and assume any
   risks associated with Your exercise of permissions under this License.

8. Limitation of Liability. In no event and under no legal theory,
   whether in tort (including negligence), contract, or otherwise,
   unless required by applicable law (such as deliberate and grossly
   negligent acts) or agreed to in writing, shall any Contributor be
   liable to You for damages, including any direct, indirect, special,
   incidental, or consequential damages of any character arising as a
   result of this License or out of the use or inability to use the
   Work (including but not limited to damages for loss of goodwill,
   work stoppage, computer failure or malfunction, or any and all
   other commercial damages or losses), even if such Contributor
   has been advised of the possibility of such damages.

9. Accepting Warranty or Additional Liability. While redistributing
   the Work or Derivative Works thereof, You may choose to offer,
   and charge a fee for, acceptance of support, warranty, indemnity,
   or other liability obligations and/or rights consistent with this
   License. However, in accepting such obligations, You may act only
   on Your own behalf and on Your sole responsibility, not on behalf
   of any other Contributor, and only if You agree to indemnify,
   defend, and hold each Contributor harmless for any liability
   incurred by, or claims asserted against, such Contributor by reason
   of your accepting any such warranty or additional liability.

END OF TERMS AND CONDITIONS

--- Exceptions to the Apache 2.0 License ----

As an exception, if, as a result of your compiling your source code, portions
of this Software are embedded into an Object form of such source code, you
may redistribute such embedded portions in such Object form without complying
with the conditions of Sections 4(a), 4(b) and 4(d) of the License.

In addition, if you combine or link compiled forms of this Software with
software that is licensed under the GPLv2 (&quot;Combined Software&quot;) and if a
court of competent jurisdiction determines that the patent provision (Section
3), the indemnity provision (Section 9) or other Section of the License
conflicts with the conditions of the GPLv2, you may retroactively and
prospectively choose to deem waived or otherwise exclude such Section(s) of
the License, but only in their entirety and only with respect to the Combined
Software.

```
---
#### <a name="fabric3"></a>FABRIC3
The following license text for the FABRIC3 license is cited only once.

```
Copyright (c) 2020 Jeff Forcier.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS &quot;AS IS&quot; AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```
---
#### <a name="blueoak-1.0.0"></a>BlueOak-1.0.0
The following license text for the BlueOak-1.0.0 license is cited only once.

```
# Blue Oak Model License

Version 1.0.0

## Purpose

This license gives everyone as much permission to work with
this software as possible, while protecting contributors
from liability.

## Acceptance

In order to receive this license, you must agree to its
rules.  The rules of this license are both obligations
under that agreement and conditions to your license.
You must not do anything with this software that triggers
a rule that you cannot or will not follow.

## Copyright

Each contributor licenses you to do everything with this
software that would otherwise infringe that contributor&#x27;s
copyright in it.

## Notices

You must ensure that everyone who gets a copy of
any part of this software from you, with or without
changes, also gets the text of this license or a link to
&lt;https://blueoakcouncil.org/license/1.0.0&gt;.

## Excuse

If anyone notifies you in writing that you have not
complied with [Notices](#notices), you can keep your
license by taking all practical steps to comply within 30
days after the notice.  If you do not do so, your license
ends immediately.

## Patent

Each contributor licenses you to do everything with this
software that would otherwise infringe any patent claims
they can license or become able to license.

## Reliability

No contributor can revoke this license.

## No Liability

***As far as the law allows, this software comes as is,
without any warranty or condition, and no contributor
will be liable to anyone for any damages related to this
software or this license, under any kind of legal claim.***

```
---
#### <a name="mpl-2.0"></a>MPL-2.0
The following license text for the MPL-2.0 license is cited only once.

```
Mozilla Public License, version 2.0

1. Definitions

1.1. “Contributor”

     means each individual or legal entity that creates, contributes to the
     creation of, or owns Covered Software.

1.2. “Contributor Version”

     means the combination of the Contributions of others (if any) used by a
     Contributor and that particular Contributor’s Contribution.

1.3. “Contribution”

     means Covered Software of a particular Contributor.

1.4. “Covered Software”

     means Source Code Form to which the initial Contributor has attached the
     notice in Exhibit A, the Executable Form of such Source Code Form, and
     Modifications of such Source Code Form, in each case including portions
     thereof.

1.5. “Incompatible With Secondary Licenses”
     means

     a. that the initial Contributor has attached the notice described in
        Exhibit B to the Covered Software; or

     b. that the Covered Software was made available under the terms of version
        1.1 or earlier of the License, but not also under the terms of a
        Secondary License.

1.6. “Executable Form”

     means any form of the work other than Source Code Form.

1.7. “Larger Work”

     means a work that combines Covered Software with other material, in a separate
     file or files, that is not Covered Software.

1.8. “License”

     means this document.

1.9. “Licensable”

     means having the right to grant, to the maximum extent possible, whether at the
     time of the initial grant or subsequently, any and all of the rights conveyed by
     this License.

1.10. “Modifications”

     means any of the following:

     a. any file in Source Code Form that results from an addition to, deletion
        from, or modification of the contents of Covered Software; or

     b. any new file in Source Code Form that contains any Covered Software.

1.11. “Patent Claims” of a Contributor

      means any patent claim(s), including without limitation, method, process,
      and apparatus claims, in any patent Licensable by such Contributor that
      would be infringed, but for the grant of the License, by the making,
      using, selling, offering for sale, having made, import, or transfer of
      either its Contributions or its Contributor Version.

1.12. “Secondary License”

      means either the GNU General Public License, Version 2.0, the GNU Lesser
      General Public License, Version 2.1, the GNU Affero General Public
      License, Version 3.0, or any later versions of those licenses.

1.13. “Source Code Form”

      means the form of the work preferred for making modifications.

1.14. “You” (or “Your”)

      means an individual or a legal entity exercising rights under this
      License. For legal entities, “You” includes any entity that controls, is
      controlled by, or is under common control with You. For purposes of this
      definition, “control” means (a) the power, direct or indirect, to cause
      the direction or management of such entity, whether by contract or
      otherwise, or (b) ownership of more than fifty percent (50%) of the
      outstanding shares or beneficial ownership of such entity.


2. License Grants and Conditions

2.1. Grants

     Each Contributor hereby grants You a world-wide, royalty-free,
     non-exclusive license:

     a. under intellectual property rights (other than patent or trademark)
        Licensable by such Contributor to use, reproduce, make available,
        modify, display, perform, distribute, and otherwise exploit its
        Contributions, either on an unmodified basis, with Modifications, or as
        part of a Larger Work; and

     b. under Patent Claims of such Contributor to make, use, sell, offer for
        sale, have made, import, and otherwise transfer either its Contributions
        or its Contributor Version.

2.2. Effective Date

     The licenses granted in Section 2.1 with respect to any Contribution become
     effective for each Contribution on the date the Contributor first distributes
     such Contribution.

2.3. Limitations on Grant Scope

     The licenses granted in this Section 2 are the only rights granted under this
     License. No additional rights or licenses will be implied from the distribution
     or licensing of Covered Software under this License. Notwithstanding Section
     2.1(b) above, no patent license is granted by a Contributor:

     a. for any code that a Contributor has removed from Covered Software; or

     b. for infringements caused by: (i) Your and any other third party’s
        modifications of Covered Software, or (ii) the combination of its
        Contributions with other software (except as part of its Contributor
        Version); or

     c. under Patent Claims infringed by Covered Software in the absence of its
        Contributions.

     This License does not grant any rights in the trademarks, service marks, or
     logos of any Contributor (except as may be necessary to comply with the
     notice requirements in Section 3.4).

2.4. Subsequent Licenses

     No Contributor makes additional grants as a result of Your choice to
     distribute the Covered Software under a subsequent version of this License
     (see Section 10.2) or under the terms of a Secondary License (if permitted
     under the terms of Section 3.3).

2.5. Representation

     Each Contributor represents that the Contributor believes its Contributions
     are its original creation(s) or it has sufficient rights to grant the
     rights to its Contributions conveyed by this License.

2.6. Fair Use

     This License is not intended to limit any rights You have under applicable
     copyright doctrines of fair use, fair dealing, or other equivalents.

2.7. Conditions

     Sections 3.1, 3.2, 3.3, and 3.4 are conditions of the licenses granted in
     Section 2.1.


3. Responsibilities

3.1. Distribution of Source Form

     All distribution of Covered Software in Source Code Form, including any
     Modifications that You create or to which You contribute, must be under the
     terms of this License. You must inform recipients that the Source Code Form
     of the Covered Software is governed by the terms of this License, and how
     they can obtain a copy of this License. You may not attempt to alter or
     restrict the recipients’ rights in the Source Code Form.

3.2. Distribution of Executable Form

     If You distribute Covered Software in Executable Form then:

     a. such Covered Software must also be made available in Source Code Form,
        as described in Section 3.1, and You must inform recipients of the
        Executable Form how they can obtain a copy of such Source Code Form by
        reasonable means in a timely manner, at a charge no more than the cost
        of distribution to the recipient; and

     b. You may distribute such Executable Form under the terms of this License,
        or sublicense it under different terms, provided that the license for
        the Executable Form does not attempt to limit or alter the recipients’
        rights in the Source Code Form under this License.

3.3. Distribution of a Larger Work

     You may create and distribute a Larger Work under terms of Your choice,
     provided that You also comply with the requirements of this License for the
     Covered Software. If the Larger Work is a combination of Covered Software
     with a work governed by one or more Secondary Licenses, and the Covered
     Software is not Incompatible With Secondary Licenses, this License permits
     You to additionally distribute such Covered Software under the terms of
     such Secondary License(s), so that the recipient of the Larger Work may, at
     their option, further distribute the Covered Software under the terms of
     either this License or such Secondary License(s).

3.4. Notices

     You may not remove or alter the substance of any license notices (including
     copyright notices, patent notices, disclaimers of warranty, or limitations
     of liability) contained within the Source Code Form of the Covered
     Software, except that You may alter any license notices to the extent
     required to remedy known factual inaccuracies.

3.5. Application of Additional Terms

     You may choose to offer, and to charge a fee for, warranty, support,
     indemnity or liability obligations to one or more recipients of Covered
     Software. However, You may do so only on Your own behalf, and not on behalf
     of any Contributor. You must make it absolutely clear that any such
     warranty, support, indemnity, or liability obligation is offered by You
     alone, and You hereby agree to indemnify every Contributor for any
     liability incurred by such Contributor as a result of warranty, support,
     indemnity or liability terms You offer. You may include additional
     disclaimers of warranty and limitations of liability specific to any
     jurisdiction.

4. Inability to Comply Due to Statute or Regulation

   If it is impossible for You to comply with any of the terms of this License
   with respect to some or all of the Covered Software due to statute, judicial
   order, or regulation then You must: (a) comply with the terms of this License
   to the maximum extent possible; and (b) describe the limitations and the code
   they affect. Such description must be placed in a text file included with all
   distributions of the Covered Software under this License. Except to the
   extent prohibited by statute or regulation, such description must be
   sufficiently detailed for a recipient of ordinary skill to be able to
   understand it.

5. Termination

5.1. The rights granted under this License will terminate automatically if You
     fail to comply with any of its terms. However, if You become compliant,
     then the rights granted under this License from a particular Contributor
     are reinstated (a) provisionally, unless and until such Contributor
     explicitly and finally terminates Your grants, and (b) on an ongoing basis,
     if such Contributor fails to notify You of the non-compliance by some
     reasonable means prior to 60 days after You have come back into compliance.
     Moreover, Your grants from a particular Contributor are reinstated on an
     ongoing basis if such Contributor notifies You of the non-compliance by
     some reasonable means, this is the first time You have received notice of
     non-compliance with this License from such Contributor, and You become
     compliant prior to 30 days after Your receipt of the notice.

5.2. If You initiate litigation against any entity by asserting a patent
     infringement claim (excluding declaratory judgment actions, counter-claims,
     and cross-claims) alleging that a Contributor Version directly or
     indirectly infringes any patent, then the rights granted to You by any and
     all Contributors for the Covered Software under Section 2.1 of this License
     shall terminate.

5.3. In the event of termination under Sections 5.1 or 5.2 above, all end user
     license agreements (excluding distributors and resellers) which have been
     validly granted by You or Your distributors under this License prior to
     termination shall survive termination.

6. Disclaimer of Warranty

   Covered Software is provided under this License on an “as is” basis, without
   warranty of any kind, either expressed, implied, or statutory, including,
   without limitation, warranties that the Covered Software is free of defects,
   merchantable, fit for a particular purpose or non-infringing. The entire
   risk as to the quality and performance of the Covered Software is with You.
   Should any Covered Software prove defective in any respect, You (not any
   Contributor) assume the cost of any necessary servicing, repair, or
   correction. This disclaimer of warranty constitutes an essential part of this
   License. No use of  any Covered Software is authorized under this License
   except under this disclaimer.

7. Limitation of Liability

   Under no circumstances and under no legal theory, whether tort (including
   negligence), contract, or otherwise, shall any Contributor, or anyone who
   distributes Covered Software as permitted above, be liable to You for any
   direct, indirect, special, incidental, or consequential damages of any
   character including, without limitation, damages for lost profits, loss of
   goodwill, work stoppage, computer failure or malfunction, or any and all
   other commercial damages or losses, even if such party shall have been
   informed of the possibility of such damages. This limitation of liability
   shall not apply to liability for death or personal injury resulting from such
   party’s negligence to the extent applicable law prohibits such limitation.
   Some jurisdictions do not allow the exclusion or limitation of incidental or
   consequential damages, so this exclusion and limitation may not apply to You.

8. Litigation

   Any litigation relating to this License may be brought only in the courts of
   a jurisdiction where the defendant maintains its principal place of business
   and such litigation shall be governed by laws of that jurisdiction, without
   reference to its conflict-of-law provisions. Nothing in this Section shall
   prevent a party’s ability to bring cross-claims or counter-claims.

9. Miscellaneous

   This License represents the complete agreement concerning the subject matter
   hereof. If any provision of this License is held to be unenforceable, such
   provision shall be reformed only to the extent necessary to make it
   enforceable. Any law or regulation which provides that the language of a
   contract shall be construed against the drafter shall not be used to construe
   this License against a Contributor.


10. Versions of the License

10.1. New Versions

      Mozilla Foundation is the license steward. Except as provided in Section
      10.3, no one other than the license steward has the right to modify or
      publish new versions of this License. Each version will be given a
      distinguishing version number.

10.2. Effect of New Versions

      You may distribute the Covered Software under the terms of the version of
      the License under which You originally received the Covered Software, or
      under the terms of any subsequent version published by the license
      steward.

10.3. Modified Versions

      If you create software not governed by this License, and you want to
      create a new license for such software, you may create and use a modified
      version of this License if you rename the license and remove any
      references to the name of the license steward (except to note that such
      modified license differs from this License).

10.4. Distributing Source Code Form that is Incompatible With Secondary Licenses
      If You choose to distribute Source Code Form that is Incompatible With
      Secondary Licenses under the terms of this version of the License, the
      notice described in Exhibit B of this License must be attached.

Exhibit A - Source Code Form License Notice

      This Source Code Form is subject to the
      terms of the Mozilla Public License, v.
      2.0. If a copy of the MPL was not
      distributed with this file, You can
      obtain one at
      http://mozilla.org/MPL/2.0/.

If it is not possible or desirable to put the notice in a particular file, then
You may include the notice in a location (such as a LICENSE file in a relevant
directory) where a recipient would be likely to look for such a notice.

You may add additional accurate notices of copyright ownership.

Exhibit B - “Incompatible With Secondary Licenses” Notice

      This Source Code Form is “Incompatible
      With Secondary Licenses”, as defined by
      the Mozilla Public License, v. 2.0.

```
