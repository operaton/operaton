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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.integrationtest.deployment.war.beans.GroovyProcessEnginePlugin;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;


/**
 * Assert that we can deploy a WAR with a process engine plugin
 * which ships and requires groovy as a dependency for scripting purposes.
 * <p>
 * Does not work on JBoss, see <a href="https://app.camunda.com/jira/browse/CAM-1778">CAM-1778</a>
 * </p>
 */
@ExtendWith(ArquillianExtension.class)
public class TestWarDeploymentWithProcessEnginePlugin extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive processArchive() {
    return initWebArchiveDeployment("test.war", "singleEngineWithProcessEnginePlugin.xml")
        .addClass(GroovyProcessEnginePlugin.class)
        .addAsResource("org/operaton/bpm/integrationtest/deployment/war/groovy.bpmn20.xml")
        .addAsResource("org/operaton/bpm/integrationtest/deployment/war/groovyAsync.bpmn20.xml")
        .addAsLibraries(Maven.configureResolver()
            .workOffline()
            .loadPomFromFile("pom.xml")
            .resolve("org.apache.groovy:groovy-jsr223")
            .withoutTransitivity()
            .as(JavaArchive.class));
  }

  @Test
  void testPAGroovyProcessEnginePlugin() {
    ProcessEngine groovyEngine = processEngineService.getProcessEngine("groovy");
    assertThat(groovyEngine).isNotNull();

    ProcessInstance pi = groovyEngine.getRuntimeService().startProcessInstanceByKey("groovy");
    HistoricProcessInstance hpi = groovyEngine.getHistoryService()
        .createHistoricProcessInstanceQuery().processDefinitionKey("groovy").finished().singleResult();
    assertEquals(pi.getId(), hpi.getId());
  }

  @Test
  void testPAGroovyAsyncProcessEnginePlugin() {
    ProcessEngine groovyEngine = processEngineService.getProcessEngine("groovy");
    assertThat(groovyEngine).isNotNull();

    ProcessInstance pi = groovyEngine.getRuntimeService().startProcessInstanceByKey("groovyAsync");

    waitForJobExecutorToProcessAllJobs();

    HistoricProcessInstance hpi = groovyEngine.getHistoryService()
        .createHistoricProcessInstanceQuery().processDefinitionKey("groovyAsync").finished().singleResult();
    assertEquals(pi.getId(), hpi.getId());
  }

}
