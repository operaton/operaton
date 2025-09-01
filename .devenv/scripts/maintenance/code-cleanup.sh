#!/usr/bin/env bash
./mvnw -U org.openrewrite.maven:rewrite-maven-plugin:run
  -Drewrite.activeRecipes=org.openrewrite.java.OrderImports \
  -Drewrite.activeStyles=org.operaton.style.OperatonJavaStyle \
  -Drewrite.exportDatatables=true
./mvnw -U org.openrewrite.maven:rewrite-maven-plugin:run
