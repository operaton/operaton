#!/usr/bin/env bash
./mvnw -U org.openrewrite.maven:rewrite-maven-plugin:run
  -Drewrite.activeRecipes=org.openrewrite.java.OrderImports \
  -Drewrite.activeStyles=org.operaton.style.OperatonJavaStyle \
  -Drewrite.exportDatatables=true \
  -Dskip.frontend.build=true \
  -Pdistro,istro-run,distro-tomcat,distro-wildfly,distro-webjar,distro-starter,distro-serverless
./mvnw -U org.openrewrite.maven:rewrite-maven-plugin:run
