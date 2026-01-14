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
package org.operaton.bpm.engine.test.cmmn.processtask;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.exception.NotAllowedException;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.runtime.*;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.TypedValue;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Roman Smirnov
 *
 */
class ProcessTaskTest extends CmmnTest {

  protected static final String PROCESS_TASK = "PI_ProcessTask_1";
  protected static final String ONE_PROCESS_TASK_CASE = "oneProcessTaskCase";

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testCallProcessAsConstant() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK).getId();

    // then

    // there exists a process instance
    ExecutionEntity processInstance = (ExecutionEntity) queryProcessInstance();
    assertThat(processInstance).isNotNull();

    // the case instance id is set on called process instance
    assertThat(processInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);
    // the super case execution id is equals the processTaskId
    assertThat(processInstance.getSuperCaseExecutionId()).isEqualTo(processTaskId);

    TaskEntity task = (TaskEntity) queryTask();

    // the case instance id has been also set on the task
    assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);
    // the case execution id should be null
    assertThat(task.getCaseExecutionId()).isNull();

    // complete ////////////////////////////////////////////////////////

    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstance.getId());

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testCallProcessAsExpressionStartsWithDollar.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testCallProcessAsExpressionStartsWithDollar() {
    // given
    // a deployed case definition
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE, Variables.createVariables().putValue("process", "oneTaskProcess")).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK).getId();

    // then

    // there exists a process instance
    ExecutionEntity processInstance = (ExecutionEntity) queryProcessInstance();
    assertThat(processInstance).isNotNull();

    // the case instance id is set on called process instance
    assertThat(processInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);
    // the super case execution id is equals the processTaskId
    assertThat(processInstance.getSuperCaseExecutionId()).isEqualTo(processTaskId);

    TaskEntity task = (TaskEntity) queryTask();

    // the case instance id has been also set on the task
    assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);
    // the case execution id should be null
    assertThat(task.getCaseExecutionId()).isNull();

    // complete ////////////////////////////////////////////////////////

    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstance.getId());

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testCallProcessAsExpressionStartsWithHash.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testCallProcessAsExpressionStartsWithHash() {
    // given
    // a deployed case definition
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE, Variables.createVariables().putValue("process", "oneTaskProcess")).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK).getId();

    // then

    // there exists a process instance
    ExecutionEntity processInstance = (ExecutionEntity) queryProcessInstance();
    assertThat(processInstance).isNotNull();

    // the case instance id is set on called process instance
    assertThat(processInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);
    // the super case execution id is equals the processTaskId
    assertThat(processInstance.getSuperCaseExecutionId()).isEqualTo(processTaskId);

    TaskEntity task = (TaskEntity) queryTask();

    // the case instance id has been also set on the task
    assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);
    // the case execution id should be null
    assertThat(task.getCaseExecutionId()).isNull();

    // complete ////////////////////////////////////////////////////////

    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstance.getId());

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testCallLatestProcess.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testCallLatestProcess() {
    // given
    String bpmnResourceName = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml";

    String deploymentId = repositoryService.createDeployment()
        .addClasspathResource(bpmnResourceName)
        .deploy()
        .getId();

    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(2);

    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK).getId();

    // latest process definition
    String latestProcessDefinitionId = repositoryService
      .createProcessDefinitionQuery()
      .latestVersion()
      .singleResult()
      .getId();

    // then

    // there exists a process instance
    ExecutionEntity processInstance = (ExecutionEntity) queryProcessInstance();

    // the case instance id is set on called process instance
    assertThat(processInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);
    // the super case execution id is equals the processTaskId
    assertThat(processInstance.getSuperCaseExecutionId()).isEqualTo(processTaskId);
    // it is associated with the latest process definition
    assertThat(processInstance.getProcessDefinitionId()).isEqualTo(latestProcessDefinitionId);

    TaskEntity task = (TaskEntity) queryTask();

    // the case instance id has been also set on the task
    assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);
    // the case execution id should be null
    assertThat(task.getCaseExecutionId()).isNull();

    // complete ////////////////////////////////////////////////////////

    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstance.getId());

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

    repositoryService.deleteDeployment(deploymentId, true);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testCallProcessByDeployment.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testCallProcessByDeployment() {
    // given

    String firstDeploymentId = repositoryService
      .createDeploymentQuery()
      .singleResult()
      .getId();

    String bpmnResourceName = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml";
    String deploymentId = repositoryService.createDeployment()
            .addClasspathResource(bpmnResourceName)
            .deploy()
            .getId();

    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(2);

    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK).getId();

    // latest process definition
    String processDefinitionIdInSameDeployment = repositoryService
      .createProcessDefinitionQuery()
      .deploymentId(firstDeploymentId)
      .singleResult()
      .getId();

    // then

    // there exists a process instance
    ExecutionEntity processInstance = (ExecutionEntity) queryProcessInstance();
    assertThat(processInstance).isNotNull();

    // the case instance id is set on called process instance
    assertThat(processInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);
    // the super case execution id is equals the processTaskId
    assertThat(processInstance.getSuperCaseExecutionId()).isEqualTo(processTaskId);
    // it is associated with the correct process definition
    assertThat(processInstance.getProcessDefinitionId()).isEqualTo(processDefinitionIdInSameDeployment);

    TaskEntity task = (TaskEntity) queryTask();

    // the case instance id has been also set on the task
    assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);
    // the case execution id should be null
    assertThat(task.getCaseExecutionId()).isNull();

    // complete ////////////////////////////////////////////////////////

    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstance.getId());

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

    repositoryService.deleteDeployment(deploymentId, true);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testCallProcessByVersion.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testCallProcessByVersion() {
    // given

    String bpmnResourceName = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml";
    String secondDeploymentId = repositoryService.createDeployment()
            .addClasspathResource(bpmnResourceName)
            .deploy()
            .getId();

    String thirdDeploymentId = repositoryService.createDeployment()
          .addClasspathResource(bpmnResourceName)
          .deploy()
          .getId();

    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(3);

    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK).getId();

    // latest process definition
    String processDefinitionIdInSecondDeployment = repositoryService
      .createProcessDefinitionQuery()
      .deploymentId(secondDeploymentId)
      .singleResult()
      .getId();

    // then

    // there exists a process instance
    ExecutionEntity processInstance = (ExecutionEntity) queryProcessInstance();
    assertThat(processInstance).isNotNull();

    // the case instance id is set on called process instance
    assertThat(processInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);
    // the super case execution id is equals the processTaskId
    assertThat(processInstance.getSuperCaseExecutionId()).isEqualTo(processTaskId);
    // it is associated with the correct process definition
    assertThat(processInstance.getProcessDefinitionId()).isEqualTo(processDefinitionIdInSecondDeployment);

    TaskEntity task = (TaskEntity) queryTask();

    // the case instance id has been also set on the task
    assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);
    // the case execution id should be null
    assertThat(task.getCaseExecutionId()).isNull();

    // complete ////////////////////////////////////////////////////////

    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstance.getId());

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

    repositoryService.deleteDeployment(secondDeploymentId, true);
    repositoryService.deleteDeployment(thirdDeploymentId, true);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testCallProcessByVersionAsExpressionStartsWithDollar.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testCallProcessByVersionAsExpressionStartsWithDollar() {
    // given

    String bpmnResourceName = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml";

    String secondDeploymentId = repositoryService.createDeployment()
            .addClasspathResource(bpmnResourceName)
            .deploy()
            .getId();

    String thirdDeploymentId = repositoryService.createDeployment()
          .addClasspathResource(bpmnResourceName)
          .deploy()
          .getId();

    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(3);

    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE,
        Variables.createVariables().putValue("myVersion", 2)).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK).getId();

    // latest process definition
    String processDefinitionIdInSecondDeployment = repositoryService
      .createProcessDefinitionQuery()
      .deploymentId(secondDeploymentId)
      .singleResult()
      .getId();

    // then

    // there exists a process instance
    ExecutionEntity processInstance = (ExecutionEntity) queryProcessInstance();
    assertThat(processInstance).isNotNull();

    // the case instance id is set on called process instance
    assertThat(processInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);
    // the super case execution id is equals the processTaskId
    assertThat(processInstance.getSuperCaseExecutionId()).isEqualTo(processTaskId);
    // it is associated with the correct process definition
    assertThat(processInstance.getProcessDefinitionId()).isEqualTo(processDefinitionIdInSecondDeployment);

    TaskEntity task = (TaskEntity) queryTask();

    // the case instance id has been also set on the task
    assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);
    // the case execution id should be null
    assertThat(task.getCaseExecutionId()).isNull();

    // complete ////////////////////////////////////////////////////////

    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstance.getId());

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

    repositoryService.deleteDeployment(secondDeploymentId, true);
    repositoryService.deleteDeployment(thirdDeploymentId, true);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testCallProcessByVersionAsExpressionStartsWithHash.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testCallProcessByVersionAsExpressionStartsWithHash() {
    // given

    String bpmnResourceName = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml";

    String secondDeploymentId = repositoryService.createDeployment()
            .addClasspathResource(bpmnResourceName)
            .deploy()
            .getId();

    String thirdDeploymentId = repositoryService.createDeployment()
          .addClasspathResource(bpmnResourceName)
          .deploy()
          .getId();

    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(3);

    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE,
        Variables.createVariables().putValue("myVersion", 2)).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK).getId();

    // latest process definition
    String processDefinitionIdInSecondDeployment = repositoryService
      .createProcessDefinitionQuery()
      .deploymentId(secondDeploymentId)
      .singleResult()
      .getId();

    // then

    // there exists a process instance
    ExecutionEntity processInstance = (ExecutionEntity) queryProcessInstance();
    assertThat(processInstance).isNotNull();

    // the case instance id is set on called process instance
    assertThat(processInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);
    // the super case execution id is equals the processTaskId
    assertThat(processInstance.getSuperCaseExecutionId()).isEqualTo(processTaskId);
    // it is associated with the correct process definition
    assertThat(processInstance.getProcessDefinitionId()).isEqualTo(processDefinitionIdInSecondDeployment);

    TaskEntity task = (TaskEntity) queryTask();

    // the case instance id has been also set on the task
    assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);
    // the case execution id should be null
    assertThat(task.getCaseExecutionId()).isNull();

    // complete ////////////////////////////////////////////////////////

    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstance.getId());

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

    repositoryService.deleteDeployment(secondDeploymentId, true);
    repositoryService.deleteDeployment(thirdDeploymentId, true);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testInputBusinessKey.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testInputBusinessKey() {
    // given
    String businessKey = "myBusinessKey";
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE, businessKey).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK).getId();

    // then

    // there exists a process instance
    ExecutionEntity processInstance = (ExecutionEntity) queryProcessInstance();
    assertThat(processInstance).isNotNull();

    // the case instance id is set on called process instance
    assertThat(processInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);
    // the super case execution id is equals the processTaskId
    assertThat(processInstance.getSuperCaseExecutionId()).isEqualTo(processTaskId);
    // the business key has been set
    assertThat(processInstance.getBusinessKey()).isEqualTo(businessKey);

    TaskEntity task = (TaskEntity) queryTask();

    // the case instance id has been also set on the task
    assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);
    // the case execution id should be null
    assertThat(task.getCaseExecutionId()).isNull();

    // complete ////////////////////////////////////////////////////////

    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstance.getId());

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testInputDifferentBusinessKey.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testInputDifferentBusinessKey() {
    // given
    String businessKey = "myBusinessKey";
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE, businessKey).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK).getId();

    // when
    caseService
      .withCaseExecution(processTaskId)
      .setVariable("myOwnBusinessKey", "myOwnBusinessKey")
      .manualStart();

    // then

    // there exists a process instance
    ExecutionEntity processInstance = (ExecutionEntity) queryProcessInstance();
    assertThat(processInstance).isNotNull();

    // the case instance id is set on called process instance
    assertThat(processInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);
    // the super case execution id is equals the processTaskId
    assertThat(processInstance.getSuperCaseExecutionId()).isEqualTo(processTaskId);
    // the business key has been set
    assertThat(processInstance.getBusinessKey()).isEqualTo("myOwnBusinessKey");
    assertThat(processInstance.getBusinessKey()).isNotEqualTo(businessKey);

    TaskEntity task = (TaskEntity) queryTask();

    // the case instance id has been also set on the task
    assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);
    // the case execution id should be null
    assertThat(task.getCaseExecutionId()).isNull();

    // complete ////////////////////////////////////////////////////////

    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstance.getId());

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testInputSource.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testInputSource() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE,
        Variables.createVariables()
            .putValue("aVariable", "abc")
            .putValue("anotherVariable", 999)
            .putValue("aThirdVariable", "def"))
        .getId();
    var processTask = queryCaseExecutionByActivityId(PROCESS_TASK);
    assertThat(processTask).isNotNull();

    // then

    // there exists a process instance
    ProcessInstance processInstance = queryProcessInstance();
    assertThat(processInstance).isNotNull();

    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(processInstance.getId())
        .list();

    assertThat(variables).hasSize(2);
    assertThat(variables.get(1).getName()).isNotEqualTo(variables.get(0).getName());

    for (VariableInstance variable : variables) {
      String name = variable.getName();
      if ("aVariable".equals(name)) {
        assertThat(name).isEqualTo("aVariable");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if ("anotherVariable".equals(name)) {
        assertThat(name).isEqualTo("anotherVariable");
        assertThat(variable.getValue()).isEqualTo(999);
      } else {
        fail("Found an unexpected variable: '%s'".formatted(name));
      }
    }

    // complete ////////////////////////////////////////////////////////

    taskService.complete(queryTask().getId());
    testRule.assertProcessEnded(processInstance.getId());

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testInputSourceDifferentTarget.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testInputSourceDifferentTarget() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK).getId();

    // when
    caseService
      .withCaseExecution(processTaskId)
      .setVariable("aVariable", "abc")
      .setVariable("anotherVariable", 999)
      .setVariable("aThirdVariable", "def")
      .manualStart();

    // then

    // there exists a process instance
    ProcessInstance processInstance = queryProcessInstance();
    assertThat(processInstance).isNotNull();

    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(processInstance.getId())
        .list();

    assertThat(variables).hasSize(2);
    assertThat(variables.get(1).getName()).isNotEqualTo(variables.get(0).getName());

    for (VariableInstance variable : variables) {
      String name = variable.getName();
      if ("myVariable".equals(name)) {
        assertThat(name).isEqualTo("myVariable");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if ("myAnotherVariable".equals(name)) {
        assertThat(name).isEqualTo("myAnotherVariable");
        assertThat(variable.getValue()).isEqualTo(999);
      } else {
        fail("Found an unexpected variable: '%s'".formatted(name));
      }
    }

    // complete ////////////////////////////////////////////////////////

    taskService.complete(queryTask().getId());
    testRule.assertProcessEnded(processInstance.getId());

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testInputSource.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testInputSourceNullValue() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();
    var processTask = queryCaseExecutionByActivityId(PROCESS_TASK);
    assertThat(processTask).isNotNull();

    // then

    // there exists a process instance
    ProcessInstance processInstance = queryProcessInstance();
    assertThat(processInstance).isNotNull();

    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(processInstance.getId())
        .list();

    assertThat(variables).hasSize(2);
    assertThat(variables.get(1).getName()).isNotEqualTo(variables.get(0).getName());

    for (VariableInstance variable : variables) {
      String name = variable.getName();

      if (!"aVariable".equals(name) && !"anotherVariable".equals(name)) {
        fail("Found an unexpected variable: '%s'".formatted(name));
      }

      assertThat(variable.getValue()).isNull();
    }

    // complete ////////////////////////////////////////////////////////

    taskService.complete(queryTask().getId());
    testRule.assertProcessEnded(processInstance.getId());

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testInputSourceExpression.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testInputSourceExpression() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE,
        Variables.createVariables().putValue("aVariable", "abc")
    .putValue("anotherVariable", 999)).getId();
    var processTask = queryCaseExecutionByActivityId(PROCESS_TASK);
    assertThat(processTask).isNotNull();

    // then

    // there exists a process instance
    ProcessInstance processInstance = queryProcessInstance();
    assertThat(processInstance).isNotNull();

    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(processInstance.getId())
        .list();

    assertThat(variables).hasSize(2);
    assertThat(variables.get(1).getName()).isNotEqualTo(variables.get(0).getName());

    for (VariableInstance variable : variables) {
      String name = variable.getName();
      if ("aVariable".equals(name)) {
        assertThat(name).isEqualTo("aVariable");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if ("anotherVariable".equals(name)) {
        assertThat(name).isEqualTo("anotherVariable");
        assertThat(variable.getValue()).isEqualTo((long) 1000);
      } else {
        fail("Found an unexpected variable: '%s'".formatted(name));
      }
    }

    // complete ////////////////////////////////////////////////////////

    taskService.complete(queryTask().getId());
    testRule.assertProcessEnded(processInstance.getId());

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testInputAll.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testInputAll() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE,
        Variables.createVariables().putValue("aVariable", "abc").putValue("anotherVariable", 999)).getId();
    var processTask = queryCaseExecutionByActivityId(PROCESS_TASK);
    assertThat(processTask).isNotNull();

    // then

    // there exists a process instance
    ProcessInstance processInstance = queryProcessInstance();
    assertThat(processInstance).isNotNull();

    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(processInstance.getId())
        .list();

    assertThat(variables).hasSize(2);
    assertThat(variables.get(1).getName()).isNotEqualTo(variables.get(0).getName());

    for (VariableInstance variable : variables) {
      String name = variable.getName();
      if ("aVariable".equals(name)) {
        assertThat(name).isEqualTo("aVariable");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if ("anotherVariable".equals(name)) {
        assertThat(name).isEqualTo("anotherVariable");
        assertThat(variable.getValue()).isEqualTo(999);
      } else {
        fail("Found an unexpected variable: '%s'".formatted(name));
      }
    }

    // complete ////////////////////////////////////////////////////////

    taskService.complete(queryTask().getId());
    testRule.assertProcessEnded(processInstance.getId());

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testInputAllLocal.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testInputAllLocal() {
    // given
    createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();
    String caseTaskId = queryCaseExecutionByActivityId(PROCESS_TASK).getId();

    // when
    caseService
      .withCaseExecution(caseTaskId)
      .setVariable("aVariable", "abc")
      .setVariableLocal("aLocalVariable", "def")
      .manualStart();

    // then only the local variable is mapped to the sub process instance
    ProcessInstance subProcessInstance = queryProcessInstance();

    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(subProcessInstance.getId())
        .list();

    assertThat(variables).hasSize(1);
    assertThat(variables.get(0).getName()).isEqualTo("aLocalVariable");
  }


  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testInputOverlapping.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testInputOverlapping() {
    // specifics should override "all"
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK).getId();

    // when
    caseService
      .withCaseExecution(processTaskId)
      .setVariable("aVariable", "abc")
      .setVariable("anotherVariable", 999)
      .manualStart();

    // then

    // there exists a process instance
    ProcessInstance processInstance = queryProcessInstance();
    assertThat(processInstance).isNotNull();

    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(processInstance.getId())
        .list();

    assertThat(variables).hasSize(2);
    assertThat(variables.get(1).getName()).isNotEqualTo(variables.get(0).getName());

    for (VariableInstance variable : variables) {
      String name = variable.getName();
      if ("aVariable".equals(name)) {
        assertThat(name).isEqualTo("aVariable");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if ("anotherVariable".equals(name)) {
        assertThat(name).isEqualTo("anotherVariable");
        assertThat(variable.getValue()).isEqualTo((long) 1000);
      } else {
        fail("Found an unexpected variable: '%s'".formatted(name));
      }
    }

    // complete ////////////////////////////////////////////////////////

    taskService.complete(queryTask().getId());
    testRule.assertProcessEnded(processInstance.getId());

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCaseWithManualActivation.cmmn"
  })
  @Test
  void testProcessNotFound() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK).getId();
    var caseExecutionCommandBuilder = caseService
        .withCaseExecution(processTaskId);

    assertThatThrownBy(caseExecutionCommandBuilder::manualStart).isInstanceOf(ProcessEngineException.class);

    // complete //////////////////////////////////////////////////////////

    terminate(caseInstanceId);
    close(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testCompleteSimpleProcess() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();

    Task task = queryTask();

    // when
    taskService.complete(task.getId());

    // then

    // the process instance has been completed
    ProcessInstance processInstance = queryProcessInstance();
    assertThat(processInstance).isNull();

    // the case execution associated with the
    // process task has been completed
    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK);
    assertThat(processTask).isNull();

    // complete ////////////////////////////////////////////////////////

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testOutputSource.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testOutputSource() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();

    String processInstanceId = queryProcessInstance().getId();

    runtimeService.setVariable(processInstanceId, "aVariable", "abc");
    runtimeService.setVariable(processInstanceId, "anotherVariable", 999);
    runtimeService.setVariable(processInstanceId, "aThirdVariable", "def");

    String taskId = queryTask().getId();

    // when
    // should also complete process instance
    taskService.complete(taskId);

    // then
    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertThat(variables).hasSize(2);
    assertThat(variables.get(1).getName()).isNotEqualTo(variables.get(0).getName());

    for (VariableInstance variable : variables) {
      String name = variable.getName();
      if ("aVariable".equals(name)) {
        assertThat(name).isEqualTo("aVariable");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if ("anotherVariable".equals(name)) {
        assertThat(name).isEqualTo("anotherVariable");
        assertThat(variable.getValue()).isEqualTo(999);
      } else {
        fail("Found an unexpected variable: '%s'".formatted(name));
      }
    }

    // complete ////////////////////////////////////////////////////////

    testRule.assertProcessEnded(processInstanceId);

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testOutputSourceDifferentTarget.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testOutputSourceDifferentTarget() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();

    String processInstanceId = queryProcessInstance().getId();

    runtimeService.setVariable(processInstanceId, "aVariable", "abc");
    runtimeService.setVariable(processInstanceId, "anotherVariable", 999);

    String taskId = queryTask().getId();

    // when
    // should also complete process instance
    taskService.complete(taskId);

    // then
    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertThat(variables).hasSize(2);
    assertThat(variables.get(1).getName()).isNotEqualTo(variables.get(0).getName());

    for (VariableInstance variable : variables) {
      String name = variable.getName();
      if ("myVariable".equals(name)) {
        assertThat(name).isEqualTo("myVariable");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if ("myAnotherVariable".equals(name)) {
        assertThat(name).isEqualTo("myAnotherVariable");
        assertThat(variable.getValue()).isEqualTo(999);
      } else {
        fail("Found an unexpected variable: '%s'".formatted(name));
      }
    }

    // complete ////////////////////////////////////////////////////////

    testRule.assertProcessEnded(processInstanceId);

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testOutputSource.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testOutputSourceNullValue() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();

    String processInstanceId = queryProcessInstance().getId();

    String taskId = queryTask().getId();

    // when
    // should also complete process instance
    taskService.complete(taskId);

    // then
    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertThat(variables).hasSize(2);
    assertThat(variables.get(1).getName()).isNotEqualTo(variables.get(0).getName());

    for (VariableInstance variable : variables) {
      String name = variable.getName();
      if (!"aVariable".equals(name) && !"anotherVariable".equals(name)) {
        fail("Found an unexpected variable: '%s'".formatted(name));
      }

      assertThat(variable.getValue()).isNull();
    }

    // complete ////////////////////////////////////////////////////////

    testRule.assertProcessEnded(processInstanceId);

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testOutputSourceExpression.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testOutputSourceExpression() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();

    String processInstanceId = queryProcessInstance().getId();

    String taskId = queryTask().getId();

    runtimeService.setVariable(processInstanceId, "aVariable", "abc");
    runtimeService.setVariable(processInstanceId, "anotherVariable", 999);

    // when
    // should also complete process instance
    taskService.complete(taskId);

    // then
    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertThat(variables).hasSize(2);
    assertThat(variables.get(1).getName()).isNotEqualTo(variables.get(0).getName());

    for (VariableInstance variable : variables) {
      String name = variable.getName();
      if ("aVariable".equals(name)) {
        assertThat(name).isEqualTo("aVariable");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if ("anotherVariable".equals(name)) {
        assertThat(name).isEqualTo("anotherVariable");
        assertThat(variable.getValue()).isEqualTo((long) 1000);
      } else {
        fail("Found an unexpected variable: '%s'".formatted(name));
      }
    }

    // complete ////////////////////////////////////////////////////////

    testRule.assertProcessEnded(processInstanceId);

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testOutputAll.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testOutputAll() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();
    var processTask = queryCaseExecutionByActivityId(PROCESS_TASK);
    assertThat(processTask).isNotNull();

    String processInstanceId = queryProcessInstance().getId();

    String taskId = queryTask().getId();

    runtimeService.setVariable(processInstanceId, "aVariable", "abc");
    runtimeService.setVariable(processInstanceId, "anotherVariable", 999);

    // when
    // should also complete process instance
    taskService.complete(taskId);

    // then
    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertThat(variables).hasSize(2);
    assertThat(variables.get(1).getName()).isNotEqualTo(variables.get(0).getName());

    for (VariableInstance variable : variables) {
      String name = variable.getName();
      if ("aVariable".equals(name)) {
        assertThat(name).isEqualTo("aVariable");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if ("anotherVariable".equals(name)) {
        assertThat(name).isEqualTo("anotherVariable");
        assertThat(variable.getValue()).isEqualTo(999);
      } else {
        fail("Found an unexpected variable: '%s'".formatted(name));
      }
    }

    // complete ////////////////////////////////////////////////////////

    testRule.assertProcessEnded(processInstanceId);

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testOutputOverlapping.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testOutputOverlapping() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();
    var processTask = queryCaseExecutionByActivityId(PROCESS_TASK);
    assertThat(processTask).isNotNull();

    String processInstanceId = queryProcessInstance().getId();

    String taskId = queryTask().getId();

    runtimeService.setVariable(processInstanceId, "aVariable", "abc");
    runtimeService.setVariable(processInstanceId, "anotherVariable", 999);

    // when
    // should also complete process instance
    taskService.complete(taskId);

    // then
    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertThat(variables).hasSize(2);
    assertThat(variables.get(1).getName()).isNotEqualTo(variables.get(0).getName());

    for (VariableInstance variable : variables) {
      String name = variable.getName();
      if ("aVariable".equals(name)) {
        assertThat(name).isEqualTo("aVariable");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if ("anotherVariable".equals(name)) {
        assertThat(name).isEqualTo("anotherVariable");
        assertThat(variable.getValue()).isEqualTo((long) 1000);
      } else {
        fail("Found an unexpected variable: '%s'".formatted(name));
      }
    }

    // complete ////////////////////////////////////////////////////////

    testRule.assertProcessEnded(processInstanceId);

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testOutputAllWithManualActivation.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testOutputVariablesShouldNotExistAnymore() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK).getId();

    caseService
      .withCaseExecution(processTaskId)
      // set variables local
      .setVariableLocal("aVariable", "xyz")
      .setVariableLocal("anotherVariable", 123)
      .manualStart();

    String processInstanceId = queryProcessInstance().getId();

    String taskId = queryTask().getId();

    runtimeService.setVariable(processInstanceId, "aVariable", "abc");
    runtimeService.setVariable(processInstanceId, "anotherVariable", 999);

    // when
    // should also complete process instance
    taskService.complete(taskId);

    // then

    // the variables have been deleted
    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertThat(variables).isEmpty();

    // complete ////////////////////////////////////////////////////////

    testRule.assertProcessEnded(processInstanceId);

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testVariablesRoundtrip.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testVariablesRoundtrip() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK).getId();

    caseService
      .withCaseExecution(processTaskId)
      .setVariable("aVariable", "xyz")
      .setVariable("anotherVariable", 123);

    String processInstanceId = queryProcessInstance().getId();

    String taskId = queryTask().getId();

    runtimeService.setVariable(processInstanceId, "aVariable", "abc");
    runtimeService.setVariable(processInstanceId, "anotherVariable", 999);

    // when
    // should also complete process instance
    taskService.complete(taskId);

    // then

    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertThat(variables).hasSize(2);
    assertThat(variables.get(1).getName()).isNotEqualTo(variables.get(0).getName());

    for (VariableInstance variable : variables) {
      String name = variable.getName();
      if ("aVariable".equals(name)) {
        assertThat(name).isEqualTo("aVariable");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if ("anotherVariable".equals(name)) {
        assertThat(name).isEqualTo("anotherVariable");
        assertThat(variable.getValue()).isEqualTo(999);
      } else {
        fail("Found an unexpected variable: '%s'".formatted(name));
      }
    }

    // complete ////////////////////////////////////////////////////////

    testRule.assertProcessEnded(processInstanceId);

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testInputOutputAll.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testInputOutputAllTypedVariables() {
    String variableName = "aVariable";
    String variableName2 = "anotherVariable";
    String variableName3 = "theThirdVariable";
    TypedValue variableValue = Variables.stringValue("abc");
    TypedValue variableValue2 = Variables.longValue(null);

    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE,
        Variables.createVariables()
        .putValue(variableName, variableValue)
        .putValue(variableName2, variableValue2))
        .getId();
    var processTask = queryCaseExecutionByActivityId(PROCESS_TASK);
    assertThat(processTask).isNotNull();

    String processInstanceId = queryProcessInstance().getId();

    TypedValue value = runtimeService.getVariableTyped(processInstanceId, variableName);
    assertThat(value).isEqualTo(variableValue);
    value = runtimeService.getVariableTyped(processInstanceId, variableName2);
    assertThat(value).isEqualTo(variableValue2);

    String taskId = queryTask().getId();

    TypedValue variableValue3 = Variables.integerValue(1);
    runtimeService.setVariable(processInstanceId, variableName3, variableValue3);

    // should also complete process instance
    taskService.complete(taskId);

    value = caseService.getVariableTyped(caseInstanceId, variableName3);

    assertThat(value).isEqualTo(variableValue3);

    // complete ////////////////////////////////////////////////////////

    testRule.assertProcessEnded(processInstanceId);

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testVariablesRoundtrip.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testInputOutputLimitedTypedVariables() {
    String variableName = "aVariable";
    String variableName2 = "anotherVariable";
    TypedValue caseVariableValue = Variables.stringValue("abc");
    TypedValue caseVariableValue2 = Variables.integerValue(null);

    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE,
        Variables.createVariables().putValue(variableName, caseVariableValue)
        .putValue(variableName2, caseVariableValue2)).getId();

    String processInstanceId = queryProcessInstance().getId();
    TypedValue value = runtimeService.getVariableTyped(processInstanceId, variableName);
    assertThat(value).isEqualTo(caseVariableValue);
    value = runtimeService.getVariableTyped(processInstanceId, variableName2);
    assertThat(value).isEqualTo(caseVariableValue2);


    TypedValue processVariableValue = Variables.stringValue("cba");
    TypedValue processVariableValue2 = Variables.booleanValue(null);
    runtimeService.setVariable(processInstanceId, variableName, processVariableValue);
    runtimeService.setVariable(processInstanceId, variableName2, processVariableValue2);

    // should also complete process instance
    taskService.complete(queryTask().getId());

    value = caseService.getVariableTyped(caseInstanceId, variableName);
    assertThat(value).isEqualTo(processVariableValue);
    value = caseService.getVariableTyped(caseInstanceId, variableName2);
    assertThat(value).isEqualTo(processVariableValue2);
    // complete ////////////////////////////////////////////////////////

    testRule.assertProcessEnded(processInstanceId);

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testCompleteProcessTask() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK).getId();
    var caseExecutionCommandBuilder = caseService
        .withCaseExecution(processTaskId);

    assertThatThrownBy(caseExecutionCommandBuilder::complete).isInstanceOf(NotAllowedException.class);

    // complete ////////////////////////////////////////////////////////

    String processInstanceId = queryProcessInstance().getId();

    String taskId = queryTask().getId();

    taskService.complete(taskId);
    testRule.assertProcessEnded(processInstanceId);

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testCompleteProcessTaskAfterTerminateSubProcessInstance() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK).getId();

    String processInstanceId = queryProcessInstance().getId();

    runtimeService.deleteProcessInstance(processInstanceId, null);

    ProcessInstance processInstance = queryProcessInstance();
    assertThat(processInstance).isNull();
    var caseExecutionCommandBuilder = caseService
        .withCaseExecution(processTaskId);

    assertThatThrownBy(caseExecutionCommandBuilder::complete).isInstanceOf(Exception.class);

    // complete ////////////////////////////////////////////////////////

    terminate(caseInstanceId);
    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testTerminateProcessTask() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK).getId();

    // when
    // terminate process task
    terminate(processTaskId);

    // then

    // the process instance is still running
    ProcessInstance processInstance = queryProcessInstance();
    assertThat(processInstance).isNotNull();

    // complete ////////////////////////////////////////////////////////

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(processInstance.getId());

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testTerminateSubProcessInstance() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();

    String processInstanceId = queryProcessInstance().getId();

    // when
    runtimeService.deleteProcessInstance(processInstanceId, null);

    // then
    ProcessInstance processInstance = queryProcessInstance();
    assertThat(processInstance).isNull();

    // the case execution associated with the process task
    // does still exist and is active.
    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK);

    assertThat(processTask).isNotNull();

    assertThat(processTask.isActive()).isTrue();

    // complete ////////////////////////////////////////////////////////

    terminate(caseInstanceId);
    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testSuspendProcessTask() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK).getId();

    // when
    // suspend process task
    suspend(processTaskId);

    // then
    ProcessInstance processInstance = queryProcessInstance();
    assertThat(processInstance).isNotNull();
    assertThat(processInstance.isSuspended()).isFalse();

    Task task = queryTask();
    assertThat(task).isNotNull();
    assertThat(task.isSuspended()).isFalse();

    // complete ////////////////////////////////////////////////////////

    resume(processTaskId);
    terminate(caseInstanceId);
    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

    taskService.complete(task.getId());

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testSuspendSubProcessInstance() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();
    var processTask = queryCaseExecutionByActivityId(PROCESS_TASK);
    assertThat(processTask).isNotNull();

    String processInstanceId = queryProcessInstance().getId();

    // when
    // suspend sub process instance
    runtimeService.suspendProcessInstanceById(processInstanceId);

    // then
    ProcessInstance processInstance = queryProcessInstance();
    assertThat(processInstance.isSuspended()).isTrue();

    // the case execution associated with the process task
    // is still active
    processTask = queryCaseExecutionByActivityId(PROCESS_TASK);
    assertThat(processTask.isActive()).isTrue();

    // complete ////////////////////////////////////////////////////////

    runtimeService.activateProcessInstanceById(processInstanceId);

    String taskId = queryTask().getId();
    taskService.complete(taskId);
    testRule.assertProcessEnded(processInstanceId);

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testResumeProcessTask() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK).getId();

    suspend(processTaskId);

    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK);
    assertThat(processTask.isActive()).isFalse();

    ProcessInstance processInstance = queryProcessInstance();
    assertThat(processInstance.isSuspended()).isFalse();

    // when
    resume(processTaskId);

    // then
    processInstance = queryProcessInstance();
    assertThat(processInstance.isSuspended()).isFalse();

    processTask = queryCaseExecutionByActivityId(PROCESS_TASK);
    assertThat(processTask.isActive()).isTrue();

    // complete ////////////////////////////////////////////////////////

    String taskId = queryTask().getId();
    taskService.complete(taskId);
    testRule.assertProcessEnded(processInstance.getId());

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testNonBlockingProcessTask.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testNonBlockingProcessTask() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();

    // then
    ProcessInstance processInstance = queryProcessInstance();
    assertThat(processInstance).isNotNull();

    Task task = queryTask();
    assertThat(task).isNotNull();

    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK);
    assertThat(processTask).isNull();

    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .singleResult();
    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isCompleted()).isTrue();

    // complete ////////////////////////////////////////////////////////

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);

    String taskId = queryTask().getId();
    taskService.complete(taskId);
    testRule.assertProcessEnded(processInstance.getId());

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testProcessInstanceCompletesInOneGo.cmmn",
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testProcessInstanceCompletesInOneGo.bpmn20.xml"
  })
  @Test
  void testProcessInstanceCompletesInOneGo() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();

    // then
    ProcessInstance processInstance = queryProcessInstance();
    assertThat(processInstance).isNull();

    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK);
    assertThat(processTask).isNull();

    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .singleResult();
    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isCompleted()).isTrue();

    // complete ////////////////////////////////////////////////////////

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testNonBlockingProcessTaskAndProcessInstanceCompletesInOneGo.cmmn",
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testProcessInstanceCompletesInOneGo.bpmn20.xml"
  })
  @Test
  void testNonBlockingProcessTaskAndProcessInstanceCompletesInOneGo() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();

    // then
    ProcessInstance processInstance = queryProcessInstance();
    assertThat(processInstance).isNull();

    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK);
    assertThat(processTask).isNull();

    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .singleResult();
    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isCompleted()).isTrue();

    // complete ////////////////////////////////////////////////////////

    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testStartProcessInstanceAsync.cmmn",
      "org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testStartProcessInstanceAsync.bpmn20.xml"
  })
  @Test
  void testStartProcessInstanceAsync() {
    // given
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();
    var processTask = queryCaseExecutionByActivityId(PROCESS_TASK);
    assertThat(processTask).isNotNull();

    // then
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    ProcessInstance processInstance = queryProcessInstance();
    assertThat(processInstance).isNotNull();

    // complete ////////////////////////////////////////////////////////

    managementService.executeJob(job.getId());
    close(caseInstanceId);
    testRule.assertCaseEnded(caseInstanceId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCaseWithManualActivation.cmmn"})
  @Test
  void testActivityType() {
    // given
    createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();

    // when
    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK);

    // then
    assertThat(processTask.getActivityType()).isEqualTo("processTask");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testOutputAll.cmmn",
      "org/operaton/bpm/engine/test/cmmn/processtask/subProcessWithError.bpmn"})
  @Test
  void testOutputWhenErrorOccurs() {
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();
    Task task = queryTask();
    assertThat(task.getName()).isEqualTo("SubTask");
    String variableName = "foo";
    Object variableValue = "bar";
    runtimeService.setVariable(task.getProcessInstanceId(), variableName, variableValue);
    taskService.complete(task.getId());

    Object variable = caseService.getVariable(caseInstanceId, variableName);
    assertThat(variable).isEqualTo(variableValue);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/processtask/ProcessTaskTest.testOutputAll.cmmn",
      "org/operaton/bpm/engine/test/cmmn/processtask/subProcessWithThrownError.bpmn"})
  @Test
  void testOutputWhenThrownBpmnErrorOccurs() {
    String caseInstanceId = createCaseInstanceByKey(ONE_PROCESS_TASK_CASE).getId();
    Task task = queryTask();
    assertThat(task.getName()).isEqualTo("SubTask");
    String variableName = "foo";
    Object variableValue = "bar";
    runtimeService.setVariable(task.getProcessInstanceId(), variableName, variableValue);
    taskService.complete(task.getId());

    Object variable = caseService.getVariable(caseInstanceId, variableName);
    assertThat(variable).isEqualTo(variableValue);
  }

  protected ProcessInstance queryProcessInstance() {
    return runtimeService
        .createProcessInstanceQuery()
        .singleResult();
  }

  protected Task queryTask() {
    return taskService
        .createTaskQuery()
        .singleResult();
  }


}
