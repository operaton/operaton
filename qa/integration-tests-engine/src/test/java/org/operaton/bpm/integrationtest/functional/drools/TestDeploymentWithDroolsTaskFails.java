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
package org.operaton.bpm.integrationtest.functional.drools;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.integrationtest.util.DeploymentHelper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * In this test we make sure that if a user deploys a WAR file with a broken
 * .bpmn-XML file, the deployment fails.
 *
 * @author Daniel Meyer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class TestDeploymentWithDroolsTaskFails {

  @ArquillianResource
  private Deployer deployer;

  @Deployment(managed=false, name="deployment")
  public static WebArchive processArchive() {

    return  ShrinkWrap.create(WebArchive.class, "test.war")
      .addAsWebInfResource("org/operaton/bpm/integrationtest/beans.xml", "beans.xml")
      .addAsLibraries(DeploymentHelper.getEjbClient())
      .addAsResource("META-INF/processes.xml", "META-INF/processes.xml")
      .addAsResource("org/operaton/bpm/integrationtest/functional/drools/TestDeploymentWithDroolsTaskFails.testDeployDroolsFails.bpmn20.xml");
  }

  @Test
  @RunAsClient
  void testDeployDroolsFails() {
    assertThatThrownBy(() -> deployer.deploy("deployment")).isInstanceOf(Exception.class);
  }

}
