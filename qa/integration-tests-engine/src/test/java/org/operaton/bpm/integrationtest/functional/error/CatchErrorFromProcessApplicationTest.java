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
package org.operaton.bpm.integrationtest.functional.error;

import java.util.Collections;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ArquillianExtension.class)
public class CatchErrorFromProcessApplicationTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive createProcessArchiveDeployment() {
    return initWebArchiveDeployment()
      .addClass(ThrowErrorDelegate.class)
      .addClass(MyBusinessException.class)
      .addAsResource("org/operaton/bpm/integrationtest/functional/error/CatchErrorFromProcessApplicationTest.bpmn20.xml")
      .addAsResource("org/operaton/bpm/integrationtest/functional/error/CatchErrorFromProcessApplicationTest.delegateExpression.bpmn20.xml")
      .addAsResource("org/operaton/bpm/integrationtest/functional/error/CatchErrorFromProcessApplicationTest.sequentialMultiInstance.bpmn20.xml")
      .addAsResource("org/operaton/bpm/integrationtest/functional/error/CatchErrorFromProcessApplicationTest.delegateExpression.sequentialMultiInstance.bpmn20.xml")
      .addAsResource("org/operaton/bpm/integrationtest/functional/error/CatchErrorFromProcessApplicationTest.parallelMultiInstance.bpmn20.xml")
      .addAsResource("org/operaton/bpm/integrationtest/functional/error/CatchErrorFromProcessApplicationTest.delegateExpression.parallelMultiInstance.bpmn20.xml");
  }

  @Deployment(name="clientDeployment")
  public static WebArchive clientDeployment() {
    WebArchive deployment = ShrinkWrap.create(WebArchive.class, "client.war")
      .addAsWebInfResource("org/operaton/bpm/integrationtest/beans.xml", "beans.xml")
      .addClass(AbstractFoxPlatformIntegrationTest.class)
      .addAsLibraries(DeploymentHelper.getEngineCdi())
      .addAsLibraries(DeploymentHelper.getTestingLibs());

    TestContainer.addContainerSpecificResourcesForNonPa(deployment);

    return deployment;
  }

  @OperateOnDeployment("clientDeployment")
  @ParameterizedTest(name = "{0} thrown in process '{1}'")
  @CsvSource({
    "Exception, testProcess",
    "Exception, testProcessSequentialMI",
    "Exception, testProcessParallelMI",
    "Error, testProcess",
    "Error, testProcessSequentialMI",
    "Error, testProcessParallelMI",
  })
  void testThrowInUserTask(String thrownType, String processDefinitionKey) {
    // given
    Map<String, Object> variables;
    String expectedUserTaskKey;
    switch (thrownType) {
      case "Exception" -> {
        variables = throwException();
        expectedUserTaskKey = "userTaskException";
      }
      case "Error" -> {
        variables = throwError();
        expectedUserTaskKey = "userTaskError";
      }
      default -> throw new IllegalArgumentException("Unknown thrown type: " + thrownType);
    }
    String pi = runtimeService.startProcessInstanceByKey(processDefinitionKey, variables).getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    // when
    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();

    // then
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo(expectedUserTaskKey);

    taskService.complete(userTask.getId());
  }

  @OperateOnDeployment("clientDeployment")
  @ParameterizedTest(name = "{0} thrown in process '{1}'")
  @CsvSource({
    "Exception, testProcess",
    "Error, testProcess",
    "Exception, testProcessParallelMI",
    "Error, testProcessParallelMI",
    "Exception, testProcessSequentialMI",
    "Error, testProcessSequentialMI",
  })
  void testThrowInSignal(String thrownType, String processDefinitionKey) {
    // given
    Map<String, Object> variables;
    String expectedUserTaskKey;
    switch (thrownType) {
      case "Exception" -> {
        variables = throwException();
        expectedUserTaskKey = "userTaskException";
      }
      case "Error" -> {
        variables = throwError();
        expectedUserTaskKey = "userTaskError";
      }
      default -> throw new IllegalArgumentException("Unknown thrown type: " + thrownType);
    }

    String pi = runtimeService.startProcessInstanceByKey(processDefinitionKey).getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    Execution serviceTask;
    switch (processDefinitionKey) {
      case "testProcess" -> serviceTask = runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").singleResult();
      case "testProcessParallelMI" -> serviceTask = runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").list().get(3);
      case "testProcessSequentialMI" -> {
        // signal 2 times to execute first sequential behaviors
        runtimeService.setVariables(pi, leaveExecution());
        runtimeService.signal(runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").singleResult().getId());
        runtimeService.setVariables(pi, leaveExecution());

        serviceTask = runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").singleResult();
      }
      default -> throw new IllegalArgumentException("Unknown processDefinitionKey: " + processDefinitionKey);
    }
    assertThat(serviceTask).isNotNull();

    runtimeService.setVariables(pi, variables);
    runtimeService.signal(serviceTask.getId());

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat((Boolean) runtimeService.getVariable(pi, "signaled")).isTrue();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo(expectedUserTaskKey);

    taskService.complete(userTask.getId());
  }

  public Map<String, Object> throwError() {
    return Collections.singletonMap("type", (Object) "error");
  }

  public Map<String, Object> throwException() {
    return Collections.singletonMap("type", (Object) "exception");
  }

  public Map<String, Object> leaveExecution() {
    return Collections.singletonMap("type", (Object) "leave");
  }

}
