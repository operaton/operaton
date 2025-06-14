/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.integrationtest.util;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenDependencies;
import org.jboss.shrinkwrap.resolver.api.maven.strategy.RejectDependenciesStrategy;


public abstract class AbstractDeploymentHelper {

  protected static JavaArchive cachedClientAsset;
  protected static JavaArchive cachedEngineCdiAsset;
  protected static JavaArchive[] cachedWeldAssets;
  protected static JavaArchive[] cachedSpringAssets;

  protected static JavaArchive getEjbClient(String ejbClientArtifactName) {
    if(cachedClientAsset != null) {
      return cachedClientAsset;
    } else {

      JavaArchive[] resolvedArchives = Maven.configureResolver()
          .workOffline()
          .loadPomFromFile("pom.xml")
          .resolve(ejbClientArtifactName)
          .withTransitivity()
          .as(JavaArchive.class);

      if(resolvedArchives.length == 0) {
        throw new RuntimeException("could not resolve "+ ejbClientArtifactName);
      } else {
        cachedClientAsset = resolvedArchives[0];
        return cachedClientAsset;
      }
    }

  }

  protected static JavaArchive getEngineCdi(String engineCdiArtifactName) {
    if(cachedEngineCdiAsset != null) {
      return cachedEngineCdiAsset;
    } else {

      JavaArchive[] resolvedArchives = Maven.configureResolver()
          .workOffline()
          .loadPomFromFile("pom.xml")
          .resolve(engineCdiArtifactName)
          .withTransitivity()
          .as(JavaArchive.class);

      if(resolvedArchives.length == 0) {
        throw new RuntimeException("could not resolve "+ engineCdiArtifactName);
      } else {
        cachedEngineCdiAsset = resolvedArchives[0];
        return cachedEngineCdiAsset;
      }
    }
  }

  protected static JavaArchive[] getWeld(String engineCdiArtifactName) {
    if(cachedWeldAssets != null) {
      return cachedWeldAssets;
    } else {

      JavaArchive[] resolvedArchives = Maven.configureResolver()
          .workOffline()
          .loadPomFromFile("pom.xml")
          .resolve(engineCdiArtifactName, "org.jboss.weld.servlet:weld-servlet")
          .withTransitivity()
          .as(JavaArchive.class);

      if(resolvedArchives.length == 0) {
        throw new RuntimeException("could not resolve org.jboss.weld.servlet:weld-servlet");
      } else {
        cachedWeldAssets = resolvedArchives;
        return cachedWeldAssets;
      }
    }

  }

  protected static JavaArchive[] getEngineSpring(String engineSpringArtifactName) {
    if(cachedSpringAssets != null) {
      return cachedSpringAssets;
    } else {

      JavaArchive[] resolvedArchives = Maven.configureResolver()
          .workOffline()
          .loadPomFromFile("pom.xml")
          .addDependencies(
              MavenDependencies.createDependency(engineSpringArtifactName, ScopeType.COMPILE, false,
                  MavenDependencies.createExclusion("org.operaton.bpm:operaton-engine")),
                  MavenDependencies.createDependency("org.springframework:spring-context", ScopeType.COMPILE, false),
                  MavenDependencies.createDependency("org.springframework:spring-jdbc", ScopeType.COMPILE, false),
                  MavenDependencies.createDependency("org.springframework:spring-tx", ScopeType.COMPILE, false),
                  MavenDependencies.createDependency("org.springframework:spring-orm", ScopeType.COMPILE, false),
                  MavenDependencies.createDependency("org.springframework:spring-web", ScopeType.COMPILE, false))
          .resolve()
          .withTransitivity()
          .as(JavaArchive.class);

      if(resolvedArchives.length == 0) {
        throw new RuntimeException("could not resolve " + engineSpringArtifactName);
      } else {
        cachedSpringAssets = resolvedArchives;
        return cachedSpringAssets;
      }
    }

  }

  public static JavaArchive[] getJodaTimeModuleForServer(String server) {
    if (server.equals("tomcat") ||
        server.equals("websphere9") ||
        server.equals("weblogic") ||
        server.equals("glassfish")) {
      return Maven.configureResolver()
          .workOffline()
          .loadPomFromFile("pom.xml")
          .resolve("com.fasterxml.jackson.datatype:jackson-datatype-joda")
          .using(new RejectDependenciesStrategy(false,
              "joda-time:joda-time"))
          .as(JavaArchive.class);
    } else if (server.equals("jboss")) {
      return Maven.configureResolver()
          .workOffline()
          .loadPomFromFile("pom.xml")
          .resolve("com.fasterxml.jackson.datatype:jackson-datatype-joda")
          .using(new RejectDependenciesStrategy(false,
              "com.fasterxml.jackson.core:jackson-annotations",
              "com.fasterxml.jackson.core:jackson-core",
              "com.fasterxml.jackson.core:jackson-databind"))
          .as(JavaArchive.class);
    } else {
      throw new RuntimeException("Unable to determine dependencies for jodaTimeModule: " + server);
    }
  }

  public static JavaArchive[] getSpinJacksonJsonDataFormatForServer(String server) {
    if (server.equals("tomcat") ||
        server.equals("websphere9") ||
        server.equals("weblogic") ||
        server.equals("glassfish")) {
      return Maven.configureResolver()
          .workOffline()
          .loadPomFromFile("pom.xml")
          .resolve("org.operaton.spin:operaton-spin-dataformat-json-jackson")
          .using(new RejectDependenciesStrategy(false,
              "org.operaton.spin:operaton-spin-core",
              "org.operaton.commons:operaton-commons-logging",
              "org.operaton.commons:operaton-commons-utils"))
          .as(JavaArchive.class);
    } else {
      throw new RuntimeException("Unable to determine dependencies for spinJacksonJsonDataFormat: " + server);
    }
  }

}
