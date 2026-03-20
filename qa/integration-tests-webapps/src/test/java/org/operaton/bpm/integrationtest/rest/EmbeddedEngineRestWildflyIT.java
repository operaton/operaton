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
package org.operaton.bpm.integrationtest.rest;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.integrationtest.rest.beans.CustomProcessEngineProvider;
import org.operaton.bpm.integrationtest.rest.beans.CustomRestApplication;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(ArquillianExtension.class)
public class EmbeddedEngineRestWildflyIT {

  private static final String EMBEDDED_ENGINE_REST = "embedded-engine-rest";

  @SuppressWarnings("unused")
  @ArquillianResource
  private Deployer deployer;

  @Deployment(name = EMBEDDED_ENGINE_REST, managed = false)
  public static WebArchive createRestEngineDeployment() {
    return ShrinkWrap.createFromZipFile(WebArchive.class, new File("target/operaton-engine-rest.war"))
            .addAsWebInfResource(new File("src/test/resources/jboss-deployment-structure.xml"), "jboss-deployment-structure.xml")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsManifestResource(new File("src/test/resources/META-INF/services/org.operaton.bpm.engine.rest.spi.ProcessEngineProvider"), "META-INF/services/org.operaton.bpm.engine.rest.spi.ProcessEngineProvider")
            .addClasses(CustomRestApplication.class, CustomProcessEngineProvider.class);
  }

  @Test
  @RunAsClient
  void testDeploymentWorks() {
    assertThatCode(() -> {
      deployer.deploy(EMBEDDED_ENGINE_REST);
      deployer.undeploy(EMBEDDED_ENGINE_REST);
    }).doesNotThrowAnyException();
  }
}
