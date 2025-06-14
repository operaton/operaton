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
package org.operaton.bpm.integrationtest.deployment.war;

import java.util.Set;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.operaton.bpm.BpmPlatform;
import org.operaton.bpm.integrationtest.deployment.war.apps.CustomNameServletPA;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thorben Lindhauer
 *
 */
@RunWith(Arquillian.class)
public class TestWarDeploymentCustomPAName extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive processArchive() {
    return ShrinkWrap.create(WebArchive.class, "pa1.war")
        .addAsLibraries(DeploymentHelper.getTestingLibs())
        .addAsResource("META-INF/processes.xml")
        .addClass(AbstractFoxPlatformIntegrationTest.class)
        .addClass(CustomNameServletPA.class)
        .addAsResource("org/operaton/bpm/integrationtest/testDeployProcessArchive.bpmn20.xml");
  }

  @Test
  public void testProcessApplicationName() {
    Set<String> paNames = BpmPlatform.getProcessApplicationService().getProcessApplicationNames();

    assertThat(paNames).hasSize(1).contains(CustomNameServletPA.NAME);
  }
}
