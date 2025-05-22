/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.integrationtest.util;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenDependencies;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenDependency;

public class DeploymentHelper extends AbstractDeploymentHelper {

  protected static final String OPERATON_EJB_CLIENT = "org.operaton.bpm.jakartaee:operaton-ejb-client";
  protected static final String OPERATON_ENGINE_CDI = "org.operaton.bpm:operaton-engine-cdi";
  protected static final String OPERATON_ENGINE_SPRING = "org.operaton.bpm:operaton-engine-spring";
  protected static final String OPERATON_ENGINE = "org.operaton.bpm:operaton-engine";
  protected static JavaArchive[] cachedTestArtifacts;

  public static JavaArchive getEjbClient() {
    return getEjbClient(OPERATON_EJB_CLIENT);
  }

  public static JavaArchive getEngineCdi() {
    return getEngineCdi(OPERATON_ENGINE_CDI);
  }

  public static JavaArchive[] getWeld() {
    return getWeld(OPERATON_ENGINE_CDI);
  }

  public static JavaArchive[] getEngineSpring() {
    return getEngineSpring(OPERATON_ENGINE_SPRING);
  }

  public static JavaArchive[] getAssertJ() {
    if (cachedTestArtifacts != null) {
      return cachedTestArtifacts;
    } else {
      JavaArchive[] archives = Maven.configureResolver()
        .workOffline()
        .loadPomFromFile("pom.xml")
        .addDependencies(
          MavenDependencies.createDependency("org.assertj:assertj-core", ScopeType.TEST, false),
          MavenDependencies.createDependency("org.awaitility:awaitility", ScopeType.TEST, false)
        )
        .resolve()
        .withTransitivity()
        .as(JavaArchive.class);

      if(archives.length < 3) {
        throw new RuntimeException("Could not resolve testing artifacts. Resolved: " + Stream.of(archives).map(JavaArchive::getId).collect(Collectors.joining()));
      } else {
        cachedTestArtifacts = archives;
        return cachedTestArtifacts;
      }
    }
  }

  protected static JavaArchive[] getWeld(String engineCdiArtifactName) {
    if (cachedWeldAssets != null) {
      return cachedWeldAssets;
    } else {

      JavaArchive[] archives = resolveDependenciesFromPomXml(engineCdiArtifactName,
              "org.jboss.weld.servlet:weld-servlet-shaded"
      );

      if(archives.length == 0) {
        throw new RuntimeException("Could not resolve the Weld implementation and JakartaEE API dependencies");
      } else {
        cachedWeldAssets = archives;
        return cachedWeldAssets;
      }
    }
  }

  protected static JavaArchive[] resolveDependenciesFromPomXml(String engineCdiArtifactName, String dependencyName) {
      return Maven.configureResolver()
              .workOffline()
              .loadPomFromFile("pom.xml")
              .resolve(engineCdiArtifactName, dependencyName)
              .withoutTransitivity()
              .as(JavaArchive.class);
  }

}
