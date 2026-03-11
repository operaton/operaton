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
package org.operaton.bpm.engine.test.api.externaltask;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * @author Thorben Lindhauer
 *
 */
@Parameterized
@ExtendWith(ProcessEngineExtension.class)
public class ExternalTaskSupportTest {

  protected RepositoryService repositoryService;
  protected RuntimeService runtimeService;
  protected ExternalTaskService externalTaskService;

  @Parameters
  public static Collection<Object[]> processResources() {
    return List.of(new Object[][] {
      {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskSupportTest.businessRuleTask.bpmn20.xml"},
      {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskSupportTest.messageEndEvent.bpmn20.xml"},
      {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskSupportTest.messageIntermediateEvent.bpmn20.xml"},
      {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskSupportTest.sendTask.bpmn20.xml"}
    });
  }

  @Parameter
  public String processDefinitionResource;

  protected String deploymentId;

  @BeforeEach
  void setUp() {
    deploymentId = repositoryService
        .createDeployment()
        .addClasspathResource(processDefinitionResource)
        .deploy()
        .getId();
  }

  @AfterEach
  void tearDown() {
    if (deploymentId != null) {
      repositoryService.deleteDeployment(deploymentId, true);
    }
  }

  @TestTemplate
  void testExternalTaskSupport() {
    // given
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // when
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());

    // then
    List<LockedExternalTask> externalTasks = externalTaskService
        .fetchAndLock(1, "aWorker")
        .topic("externalTaskTopic", 5000L)
        .execute();

    assertThat(externalTasks).hasSize(1);
    assertThat(externalTasks.get(0).getProcessInstanceId()).isEqualTo(processInstance.getId());

    // and it is possible to complete the external task successfully and end the process instance
    externalTaskService.complete(externalTasks.get(0).getId(), "aWorker");

    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @TestTemplate
  void testExternalTaskProperties() {
    // given
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceById(processDefinition.getId());

    // when
    List<LockedExternalTask> externalTasks = externalTaskService
        .fetchAndLock(1, "aWorker")
        .topic("externalTaskTopic", 5000L)
        .includeExtensionProperties()
        .execute();

    // then
    LockedExternalTask task = externalTasks.get(0);
    Map<String, String> properties = task.getExtensionProperties();
    assertThat(properties).containsOnly(
        entry("key1", "val1"),
        entry("key2", "val2"));
  }
}
