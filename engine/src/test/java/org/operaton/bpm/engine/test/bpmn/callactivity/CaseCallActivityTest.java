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
package org.operaton.bpm.engine.test.bpmn.callactivity;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.exception.cmmn.CaseDefinitionNotFoundException;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionEntity;
import org.operaton.bpm.engine.impl.cmmn.execution.CmmnExecution;
import org.operaton.bpm.engine.runtime.*;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.TypedValue;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Roman Smirnov
 *
 */
class CaseCallActivityTest extends CmmnTest {

  static final String PROCESS_DEFINITION_KEY= "process";
  static final String ONE_TASK_CASE = "oneTaskCase";
  static final String CALL_ACTIVITY_ID = "callActivity";
  static final String USER_TASK_ID = "userTask";
  static final String HUMAN_TASK_ID = "PI_HumanTask_1";

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testCallCaseAsConstant.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testCallCaseAsConstant() {
    // given
    // a deployed process definition and case definition

    // when
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();

    // then
    String callActivityId = queryExecutionByActivityId(CALL_ACTIVITY_ID).getId();

    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    assertThat(subCaseInstance.getSuperExecutionId()).isEqualTo(callActivityId);

    // complete
    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    complete(humanTaskId);
    close(subCaseInstance.getId());
    testRule.assertProcessEnded(superProcessInstanceId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testCallCaseAsExpressionStartsWithDollar.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testCallCaseAsExpressionStartsWithDollar() {
    // given
    // a deployed process definition and case definition

    // when
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY, Variables.createVariables().putValue(ONE_TASK_CASE, ONE_TASK_CASE)).getId();

    // then
    String callActivityId = queryExecutionByActivityId(CALL_ACTIVITY_ID).getId();

    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    assertThat(subCaseInstance.getSuperExecutionId()).isEqualTo(callActivityId);

    // complete
    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    complete(humanTaskId);
    close(subCaseInstance.getId());
    testRule.assertProcessEnded(superProcessInstanceId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testCallCaseAsExpressionStartsWithHash.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testCallCaseAsExpressionStartsWithHash() {
    // given
    // a deployed process definition and case definition

    // when
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY, Variables.createVariables().putValue(ONE_TASK_CASE, ONE_TASK_CASE)).getId();

    // then
    String callActivityId = queryExecutionByActivityId(CALL_ACTIVITY_ID).getId();

    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    assertThat(subCaseInstance.getSuperExecutionId()).isEqualTo(callActivityId);

    // complete
    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    complete(humanTaskId);
    close(subCaseInstance.getId());
    testRule.assertProcessEnded(superProcessInstanceId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testCallCaseWithCompositeExpression.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testCallCaseWithCompositeExpression() {
    // given
    // a deployed process definition and case definition

    // when
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();

    // then
    String callActivityId = queryExecutionByActivityId(CALL_ACTIVITY_ID).getId();

    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    assertThat(subCaseInstance.getSuperExecutionId()).isEqualTo(callActivityId);

    // complete
    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    complete(humanTaskId);
    close(subCaseInstance.getId());
    testRule.assertProcessEnded(superProcessInstanceId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testCallLatestCase.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testCallLatestCase() {
    // given
    String cmmnResourceName = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn";

    String deploymentId = repositoryService.createDeployment()
        .addClasspathResource(cmmnResourceName)
        .deploy()
        .getId();

    assertThat(repositoryService.createCaseDefinitionQuery().count()).isEqualTo(2);

    String latestCaseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionKey(ONE_TASK_CASE)
        .latestVersion()
        .singleResult()
        .getId();

    // when
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();

    // then
    String callActivityId = queryExecutionByActivityId(CALL_ACTIVITY_ID).getId();

    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    assertThat(subCaseInstance.getSuperExecutionId()).isEqualTo(callActivityId);
    assertThat(subCaseInstance.getCaseDefinitionId()).isEqualTo(latestCaseDefinitionId);

    // complete ////////////////////////////////////////////////////////
    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    complete(humanTaskId);
    close(subCaseInstance.getId());
    testRule.assertProcessEnded(superProcessInstanceId);

    repositoryService.deleteDeployment(deploymentId, true);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testCallCaseByDeployment.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testCallCaseByDeployment() {
    // given

    String firstDeploymentId = repositoryService
      .createDeploymentQuery()
      .singleResult()
      .getId();

    String cmmnResourceName = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn";
    String deploymentId = repositoryService.createDeployment()
            .addClasspathResource(cmmnResourceName)
            .deploy()
            .getId();

    assertThat(repositoryService.createCaseDefinitionQuery().count()).isEqualTo(2);

    String caseDefinitionIdInSameDeployment = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionKey(ONE_TASK_CASE)
        .deploymentId(firstDeploymentId)
        .singleResult()
        .getId();

    // when
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();

    // then
    String callActivityId = queryExecutionByActivityId(CALL_ACTIVITY_ID).getId();

    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    assertThat(subCaseInstance.getSuperExecutionId()).isEqualTo(callActivityId);
    assertThat(subCaseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionIdInSameDeployment);

    // complete ////////////////////////////////////////////////////////
    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    complete(humanTaskId);
    close(subCaseInstance.getId());
    testRule.assertProcessEnded(superProcessInstanceId);

    repositoryService.deleteDeployment(deploymentId, true);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testCallCaseByVersion.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testCallCaseByVersion() {
    // given

    String cmmnResourceName = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn";
    String secondDeploymentId = repositoryService.createDeployment()
            .addClasspathResource(cmmnResourceName)
            .deploy()
            .getId();

    String thirdDeploymentId = repositoryService.createDeployment()
          .addClasspathResource(cmmnResourceName)
          .deploy()
          .getId();

    assertThat(repositoryService.createCaseDefinitionQuery().count()).isEqualTo(3);

    String caseDefinitionIdInSecondDeployment = repositoryService
      .createCaseDefinitionQuery()
      .caseDefinitionKey(ONE_TASK_CASE)
      .deploymentId(secondDeploymentId)
      .singleResult()
      .getId();

    // when
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();

    // then
    String callActivityId = queryExecutionByActivityId(CALL_ACTIVITY_ID).getId();

    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    assertThat(subCaseInstance.getSuperExecutionId()).isEqualTo(callActivityId);
    assertThat(subCaseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionIdInSecondDeployment);

    // complete ////////////////////////////////////////////////////////
    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    complete(humanTaskId);
    close(subCaseInstance.getId());
    testRule.assertProcessEnded(superProcessInstanceId);

    repositoryService.deleteDeployment(secondDeploymentId, true);
    repositoryService.deleteDeployment(thirdDeploymentId, true);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testCallCaseByVersionAsExpression.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testCallCaseByVersionAsExpression() {
    // given

    String cmmnResourceName = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn";

    String secondDeploymentId = repositoryService.createDeployment()
            .addClasspathResource(cmmnResourceName)
            .deploy()
            .getId();

    String thirdDeploymentId = repositoryService.createDeployment()
          .addClasspathResource(cmmnResourceName)
          .deploy()
          .getId();

    assertThat(repositoryService.createCaseDefinitionQuery().count()).isEqualTo(3);

    String caseDefinitionIdInSecondDeployment = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionKey(ONE_TASK_CASE)
        .deploymentId(secondDeploymentId)
        .singleResult()
        .getId();

    VariableMap variables = Variables.createVariables().putValue("myVersion", 2);

    // when
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY, variables).getId();

    // then
    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    assertThat(subCaseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionIdInSecondDeployment);

    // complete ////////////////////////////////////////////////////////
    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    complete(humanTaskId);
    close(subCaseInstance.getId());
    testRule.assertProcessEnded(superProcessInstanceId);

    repositoryService.deleteDeployment(secondDeploymentId, true);
    repositoryService.deleteDeployment(thirdDeploymentId, true);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testCallCaseAsConstant.bpmn20.xml"})
  @Test
  void testCaseNotFound() {
    // given

    assertThatThrownBy(() -> startProcessInstanceByKey(PROCESS_DEFINITION_KEY)).isInstanceOf(CaseDefinitionNotFoundException.class);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testInputBusinessKey.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testInputBusinessKey() {
    // given
    String businessKey = "myBusinessKey";

    // when
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY, null, businessKey).getId();

    // then
    String callActivityId = queryExecutionByActivityId(CALL_ACTIVITY_ID).getId();

    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    assertThat(subCaseInstance.getSuperExecutionId()).isEqualTo(callActivityId);
    assertThat(subCaseInstance.getBusinessKey()).isEqualTo(businessKey);

    // complete ////////////////////////////////////////////////////////
    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    complete(humanTaskId);
    close(subCaseInstance.getId());
    testRule.assertProcessEnded(superProcessInstanceId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testInputDifferentBusinessKey.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testInputDifferentBusinessKey() {
    // given
    String myBusinessKey = "myBusinessKey";
    String myOwnBusinessKey = "myOwnBusinessKey";

    VariableMap variables = Variables.createVariables().putValue(myOwnBusinessKey, myOwnBusinessKey);

    // when
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY, variables, myBusinessKey).getId();

    // then
    String callActivityId = queryExecutionByActivityId(CALL_ACTIVITY_ID).getId();

    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    assertThat(subCaseInstance.getSuperExecutionId()).isEqualTo(callActivityId);
    assertThat(subCaseInstance.getBusinessKey()).isEqualTo(myOwnBusinessKey);

    // complete ////////////////////////////////////////////////////////
    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    complete(humanTaskId);
    close(subCaseInstance.getId());
    testRule.assertProcessEnded(superProcessInstanceId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testInputSource.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testInputSource() {
    // given

    VariableMap parameters = Variables.createVariables()
      .putValue("aVariable", "abc")
      .putValue("anotherVariable", 999)
      .putValue("aThirdVariable", "def");

    // when
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY, parameters).getId();

    // then
    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(subCaseInstance.getId())
        .list();

    assertThat(variables)
            .isNotEmpty()
            .hasSize(2);

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
    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    complete(humanTaskId);
    close(subCaseInstance.getId());
    testRule.assertProcessEnded(superProcessInstanceId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testInputSourceDifferentTarget.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testInputSourceDifferentTarget() {
    // given

    VariableMap parameters = Variables.createVariables()
      .putValue("aVariable", "abc")
      .putValue("anotherVariable", 999)
      .putValue("aThirdVariable", "def");

    // when
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY, parameters).getId();

    // then
    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(subCaseInstance.getId())
        .list();

    assertThat(variables)
            .isNotEmpty()
            .hasSize(2);

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
    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    complete(humanTaskId);
    close(subCaseInstance.getId());
    testRule.assertProcessEnded(superProcessInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testInputSource.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testInputSourceNullValue() {
    // given

    // when
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();

    // then
    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(subCaseInstance.getId())
        .list();

    assertThat(variables)
            .isNotEmpty()
            .hasSize(2);

    for (VariableInstance variable : variables) {
      String name = variable.getName();

      if ("aVariable".equals(name)) {
        assertThat(name).isEqualTo("aVariable");
      } else if ("anotherVariable".equals(name)) {
        assertThat(name).isEqualTo("anotherVariable");
      } else {
        fail("Found an unexpected variable: '%s'".formatted(name));
      }

      assertThat(variable.getValue()).isNull();
    }

    // complete ////////////////////////////////////////////////////////
    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    complete(humanTaskId);
    close(subCaseInstance.getId());
    testRule.assertProcessEnded(superProcessInstanceId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testInputSourceExpression.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testInputSourceExpression() {
    // given
    VariableMap parameters = Variables.createVariables()
        .putValue("aVariable", "abc")
        .putValue("anotherVariable", 999);

    // when
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY, parameters).getId();

    // then
    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(subCaseInstance.getId())
        .list();

    assertThat(variables)
            .isNotEmpty()
            .hasSize(2);

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
    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    complete(humanTaskId);
    close(subCaseInstance.getId());
    testRule.assertProcessEnded(superProcessInstanceId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testInputSourceAsCompositeExpression.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testInputSourceAsCompositeExpression() {
    // given
    VariableMap parameters = Variables.createVariables()
      .putValue("aVariable", "abc")
      .putValue("anotherVariable", 999);

    // when
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY, parameters).getId();

    // then
    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    List<VariableInstance> variables = runtimeService
      .createVariableInstanceQuery()
      .caseInstanceIdIn(subCaseInstance.getId())
      .list();

    assertThat(variables)
            .isNotEmpty()
            .hasSize(2);

    for (VariableInstance variable : variables) {
      String name = variable.getName();
      if ("aVariable".equals(name)) {
        assertThat(name).isEqualTo("aVariable");
        assertThat(variable.getValue()).isEqualTo("Prefixabc");

      } else if ("anotherVariable".equals(name)) {
        assertThat(name).isEqualTo("anotherVariable");
        assertThat(variable.getValue()).isEqualTo("Prefix" + (long) 1000);

      } else {
        fail("Found an unexpected variable: '%s'".formatted(name));
      }
    }

    // complete ////////////////////////////////////////////////////////
    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    complete(humanTaskId);
    close(subCaseInstance.getId());
    testRule.assertProcessEnded(superProcessInstanceId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testInputAll.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testInputAll() {
    // given
    VariableMap parameters = Variables.createVariables()
        .putValue("aVariable", "abc")
        .putValue("anotherVariable", 999);

    // when
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY, parameters).getId();

    // then
    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(subCaseInstance.getId())
        .list();

    assertThat(variables)
            .isNotEmpty()
            .hasSize(2);

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
    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    complete(humanTaskId);
    close(subCaseInstance.getId());
    testRule.assertProcessEnded(superProcessInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testCompleteCase.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testCompleteCase() {
    // given
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();
    String subCaseInstanceId = queryOneTaskCaseInstance().getId();

    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    // when
    complete(humanTaskId);

    // then
    Task userTask = queryTaskByActivityId(USER_TASK_ID);
    assertThat(userTask).isNotNull();

    Execution callActivity = queryExecutionByActivityId(CALL_ACTIVITY_ID);
    assertThat(callActivity).isNull();

    // complete ////////////////////////////////////////////////////////

    close(subCaseInstanceId);
    testRule.assertCaseEnded(subCaseInstanceId);

    taskService.complete(userTask.getId());
    testRule.assertCaseEnded(superProcessInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testOutputSource.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testOutputSource() {
    // given
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();
    String subCaseInstanceId = queryOneTaskCaseInstance().getId();

    caseService
      .withCaseExecution(subCaseInstanceId)
      .setVariable("aVariable", "abc")
      .setVariable("anotherVariable", 999)
      .setVariable("aThirdVariable", "def")
      .execute();

    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    // when
    complete(humanTaskId);

    // then
    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(superProcessInstanceId)
        .list();

    assertThat(variables)
            .isNotEmpty()
            .hasSize(2);

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
    close(subCaseInstanceId);
    testRule.assertCaseEnded(subCaseInstanceId);

    String taskId = queryTaskByActivityId(USER_TASK_ID).getId();
    taskService.complete(taskId);
    testRule.assertProcessEnded(superProcessInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testOutputSourceDifferentTarget.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testOutputSourceDifferentTarget() {
    // given
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();
    String subCaseInstanceId = queryOneTaskCaseInstance().getId();

    caseService
      .withCaseExecution(subCaseInstanceId)
      .setVariable("aVariable", "abc")
      .setVariable("anotherVariable", 999)
      .execute();

    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    // when
    complete(humanTaskId);

    // then
    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(superProcessInstanceId)
        .list();

    assertThat(variables)
            .isNotEmpty()
            .hasSize(2);

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
    close(subCaseInstanceId);
    testRule.assertCaseEnded(subCaseInstanceId);

    String taskId = queryTaskByActivityId(USER_TASK_ID).getId();
    taskService.complete(taskId);
    testRule.assertProcessEnded(superProcessInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testOutputSource.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testOutputSourceNullValue() {
    // given
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();
    String subCaseInstanceId = queryOneTaskCaseInstance().getId();

    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    // when
    complete(humanTaskId);

    // then
    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(superProcessInstanceId)
        .list();

    assertThat(variables)
            .isNotEmpty()
            .hasSize(2);

    for (VariableInstance variable : variables) {
      String name = variable.getName();
      if ("aVariable".equals(name)) {
        assertThat(name).isEqualTo("aVariable");
      } else if ("anotherVariable".equals(name)) {
        assertThat(name).isEqualTo("anotherVariable");
      } else {
        fail("Found an unexpected variable: '%s'".formatted(name));
      }

      assertThat(variable.getValue()).isNull();
    }

    // complete ////////////////////////////////////////////////////////
    close(subCaseInstanceId);
    testRule.assertCaseEnded(subCaseInstanceId);

    String taskId = queryTaskByActivityId(USER_TASK_ID).getId();
    taskService.complete(taskId);
    testRule.assertProcessEnded(superProcessInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testOutputSourceExpression.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testOutputSourceExpression() {
    // given
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();
    String subCaseInstanceId = queryOneTaskCaseInstance().getId();

    caseService
      .withCaseExecution(subCaseInstanceId)
      .setVariable("aVariable", "abc")
      .setVariable("anotherVariable", 999)
      .execute();

    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    // when
    complete(humanTaskId);

    // then
    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(superProcessInstanceId)
        .list();

    assertThat(variables)
            .isNotEmpty()
            .hasSize(2);

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
    close(subCaseInstanceId);
    testRule.assertCaseEnded(subCaseInstanceId);

    String taskId = queryTaskByActivityId(USER_TASK_ID).getId();
    taskService.complete(taskId);
    testRule.assertProcessEnded(superProcessInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testOutputSourceAsCompositeExpression.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testOutputSourceAsCompositeExpression() {
    // given
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();
    String subCaseInstanceId = queryOneTaskCaseInstance().getId();

    caseService
      .withCaseExecution(subCaseInstanceId)
      .setVariable("aVariable", "abc")
      .setVariable("anotherVariable", 999)
      .execute();

    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    // when
    complete(humanTaskId);

    // then
    List<VariableInstance> variables = runtimeService
      .createVariableInstanceQuery()
      .processInstanceIdIn(superProcessInstanceId)
      .list();

    assertThat(variables)
            .isNotEmpty()
            .hasSize(2);

    for (VariableInstance variable : variables) {
      String name = variable.getName();
      if ("aVariable".equals(name)) {
        assertThat(name).isEqualTo("aVariable");
        assertThat(variable.getValue()).isEqualTo("Prefixabc");
      } else if ("anotherVariable".equals(name)) {
        assertThat(name).isEqualTo("anotherVariable");
        assertThat(variable.getValue()).isEqualTo("Prefix" + (long) 1000);
      } else {
        fail("Found an unexpected variable: '%s'".formatted(name));
      }
    }

    // complete ////////////////////////////////////////////////////////
    close(subCaseInstanceId);
    testRule.assertCaseEnded(subCaseInstanceId);

    String taskId = queryTaskByActivityId(USER_TASK_ID).getId();
    taskService.complete(taskId);
    testRule.assertProcessEnded(superProcessInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testOutputAll.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testOutputAll() {
    // given
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();
    String subCaseInstanceId = queryOneTaskCaseInstance().getId();

    caseService
      .withCaseExecution(subCaseInstanceId)
      .setVariable("aVariable", "abc")
      .setVariable("anotherVariable", 999)
      .execute();

    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    // when
    complete(humanTaskId);

    // then
    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(superProcessInstanceId)
        .list();

    assertThat(variables)
            .isNotEmpty()
            .hasSize(2);

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
    close(subCaseInstanceId);
    testRule.assertCaseEnded(subCaseInstanceId);

    String taskId = queryTaskByActivityId(USER_TASK_ID).getId();
    taskService.complete(taskId);
    testRule.assertProcessEnded(superProcessInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testOutputAll.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testOutputVariablesShouldNotExistAnymore() {
    // given
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();

    String callActivityId = queryExecutionByActivityId(CALL_ACTIVITY_ID).getId();

    VariableMap parameters = Variables.createVariables()
      .putValue("aVariable", "xyz")
      .putValue("anotherVariable", 123);

    runtimeService.setVariablesLocal(callActivityId, parameters);

    String subCaseInstanceId = queryOneTaskCaseInstance().getId();

    caseService
      .withCaseExecution(subCaseInstanceId)
      .setVariable("aVariable", "abc")
      .setVariable("anotherVariable", 999)
      .execute();

    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    // when
    complete(humanTaskId);

    // then

    // the variables has been deleted
    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(superProcessInstanceId)
        .list();

    assertThat(variables).isEmpty();

    // complete ////////////////////////////////////////////////////////
    close(subCaseInstanceId);
    testRule.assertCaseEnded(subCaseInstanceId);

    String taskId = queryTaskByActivityId(USER_TASK_ID).getId();
    taskService.complete(taskId);
    testRule.assertProcessEnded(superProcessInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testVariablesRoundtrip.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testVariablesRoundtrip() {
    // given
    VariableMap parameters = Variables.createVariables()
      .putValue("aVariable", "xyz")
      .putValue("anotherVariable", 999);

    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY, parameters).getId();

    String subCaseInstanceId = queryOneTaskCaseInstance().getId();

    caseService
      .withCaseExecution(subCaseInstanceId)
      .setVariable("aVariable", "abc")
      .setVariable("anotherVariable", 1000)
      .execute();

    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    // when
    complete(humanTaskId);

    // then
    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(superProcessInstanceId)
        .list();

    assertThat(variables)
            .isNotEmpty()
            .hasSize(2);

    for (VariableInstance variable : variables) {
      String name = variable.getName();
      if ("aVariable".equals(name)) {
        assertThat(name).isEqualTo("aVariable");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if ("anotherVariable".equals(name)) {
        assertThat(name).isEqualTo("anotherVariable");
        assertThat(variable.getValue()).isEqualTo(1000);
      } else {
        fail("Found an unexpected variable: '%s'".formatted(name));
      }
    }

    // complete ////////////////////////////////////////////////////////
    close(subCaseInstanceId);
    testRule.assertCaseEnded(subCaseInstanceId);

    String taskId = queryTaskByActivityId(USER_TASK_ID).getId();
    taskService.complete(taskId);
    testRule.assertProcessEnded(superProcessInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testOutputAll.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"
  })
  @Test
  void testCallCaseOutputAllVariablesTypedToProcess(){
    startProcessInstanceByKey("process");
    CaseInstance caseInstance = queryOneTaskCaseInstance();
    String variableName = "foo";
    String variableName2 = "null";
    TypedValue variableValue = Variables.stringValue("bar");
    TypedValue variableValue2 = Variables.integerValue(null);
    caseService.withCaseExecution(caseInstance.getId())
      .setVariable(variableName, variableValue)
      .setVariable(variableName2, variableValue2)
      .execute();
    complete(caseInstance.getId());

    Task task = taskService.createTaskQuery().singleResult();
    TypedValue value = runtimeService.getVariableTyped(task.getProcessInstanceId(), variableName);
    assertThat(value).isEqualTo(variableValue);
    value = runtimeService.getVariableTyped(task.getProcessInstanceId(), variableName2);
    assertThat(value).isEqualTo(variableValue2);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testCallCaseAsConstant.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testDeleteProcessInstance() {
    // given
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();
    String subCaseInstanceId = queryOneTaskCaseInstance().getId();

    // when
    runtimeService.deleteProcessInstance(superProcessInstanceId, null);

    // then
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

    CaseInstance subCaseInstance = queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();
    assertThat(subCaseInstance.isActive()).isTrue();

    // complete ////////////////////////////////////////////////////////
    terminate(subCaseInstanceId);
    close(subCaseInstanceId);
    testRule.assertCaseEnded(subCaseInstanceId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testCallCaseAsConstant.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testSuspendProcessInstance() {
    // given
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();
    String subCaseInstanceId = queryOneTaskCaseInstance().getId();

    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();

    // when (1)
    runtimeService.suspendProcessInstanceById(superProcessInstanceId);

    // then
    Execution superProcessInstance = queryExecutionById(superProcessInstanceId);
    assertThat(superProcessInstance).isNotNull();
    assertThat(superProcessInstance.isSuspended()).isTrue();

    CaseInstance subCaseInstance = queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();
    assertThat(subCaseInstance.isActive()).isTrue();

    assertThatThrownBy(() -> complete(humanTaskId)).isInstanceOf(Exception.class);

    // complete ////////////////////////////////////////////////////////
    runtimeService.activateProcessInstanceById(superProcessInstanceId);

    complete(humanTaskId);
    close(subCaseInstance.getId());
    testRule.assertCaseEnded(subCaseInstanceId);
    testRule.assertProcessEnded(superProcessInstanceId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testCallCaseAsConstant.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testTerminateSubCaseInstance() {
    // given
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();
    String subCaseInstanceId = queryOneTaskCaseInstance().getId();

    // when
    terminate(subCaseInstanceId);

    // then
    CmmnExecution subCaseInstance = (CmmnExecution) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();
    assertThat(subCaseInstance.isTerminated()).isTrue();

    Execution callActivity = queryExecutionByActivityId(CALL_ACTIVITY_ID);
    assertThat(callActivity).isNotNull();

    // complete ////////////////////////////////////////////////////////

    close(subCaseInstanceId);
    testRule.assertCaseEnded(subCaseInstanceId);

    runtimeService.deleteProcessInstance(superProcessInstanceId, null);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testCallCaseAsConstant.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"
  })
  @Test
  void testSuspendSubCaseInstance() {
    // given
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();
    String subCaseInstanceId = queryOneTaskCaseInstance().getId();

    // when
    suspend(subCaseInstanceId);

    // then
    CmmnExecution subCaseInstance = (CmmnExecution) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();
    assertThat(subCaseInstance.isSuspended()).isTrue();

    Execution callActivity = queryExecutionByActivityId(CALL_ACTIVITY_ID);
    assertThat(callActivity).isNotNull();

    // complete ////////////////////////////////////////////////////////

    close(subCaseInstanceId);
    testRule.assertCaseEnded(subCaseInstanceId);

    runtimeService.deleteProcessInstance(superProcessInstanceId, null);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivityTest.testCompletionOfCaseWithTwoTasks.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"
  })
  @Test
  void testCompletionOfTwoHumanTasks() {
    // given
    String superProcessInstanceId = startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();

    // when (1)
    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();
    manualStart(humanTaskId);
    complete(humanTaskId);

    // then (1)

    assertThat(taskService.createTaskQuery().count()).isZero();

    // when (2)
    humanTaskId = queryCaseExecutionByActivityId("PI_HumanTask_2").getId();
    manualStart(humanTaskId);
    complete(humanTaskId);

    // then (2)
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    assertThat(task.getProcessInstanceId()).isEqualTo(superProcessInstanceId);
    assertThat(task.getTaskDefinitionKey()).isEqualTo("userTask");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivity.testSubProcessLocalInputAllVariables.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testSubProcessLocalInputAllVariables() {
    ProcessInstance processInstance = startProcessInstanceByKey("subProcessLocalInputAllVariables");
    Task beforeCallActivityTask = taskService.createTaskQuery().singleResult();

    // when setting a variable in a process instance
    runtimeService.setVariable(processInstance.getId(), "callingProcessVar1", "val1");

    // and executing the call activity
    taskService.complete(beforeCallActivityTask.getId());

    // then only the local variable specified in the io mapping is passed to the called instance
    CaseExecutionEntity calledInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(calledInstance).isNotNull();

    Map<String, Object> calledInstanceVariables = caseService.getVariables(calledInstance.getId());
    assertThat(calledInstanceVariables)
            .hasSize(1)
            .containsEntry("inputParameter", "val2");

    // when setting a variable in the called instance
    caseService.setVariable(calledInstance.getId(), "calledCaseVar1", 42L);

    // and completing it
    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();
    complete(humanTaskId);

    // then the call activity output variable has been mapped to the process instance execution
    // and the output mapping variable as well
    Map<String, Object> callingInstanceVariables = runtimeService.getVariables(processInstance.getId());
    assertThat(callingInstanceVariables)
            .hasSize(3)
            .containsEntry("callingProcessVar1", "val1")
            .containsEntry("calledCaseVar1", 42L)
            .containsEntry("outputParameter", 43L);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivity.testSubProcessLocalInputSingleVariable.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testSubProcessLocalInputSingleVariable() {
    ProcessInstance processInstance = startProcessInstanceByKey("subProcessLocalInputSingleVariable");
    Task beforeCallActivityTask = taskService.createTaskQuery().singleResult();

    // when setting a variable in a process instance
    runtimeService.setVariable(processInstance.getId(), "callingProcessVar1", "val1");

    // and executing the call activity
    taskService.complete(beforeCallActivityTask.getId());

    // then the local variable specified in the io mapping is passed to the called instance
    CaseExecutionEntity calledInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(calledInstance).isNotNull();

    Map<String, Object> calledInstanceVariables = caseService.getVariables(calledInstance.getId());
    assertThat(calledInstanceVariables)
            .hasSize(1)
            .containsEntry("mappedInputParameter", "val2");

    // when setting a variable in the called instance
    caseService.setVariable(calledInstance.getId(), "calledCaseVar1", 42L);

    // and completing it
    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();
    complete(humanTaskId);

    // then the call activity output variable has been mapped to the process instance execution
    // and the output mapping variable as well
    Map<String, Object> callingInstanceVariables = runtimeService.getVariables(processInstance.getId());
    assertThat(callingInstanceVariables)
            .hasSize(4)
            .containsEntry("callingProcessVar1", "val1")
            .containsEntry("mappedInputParameter", "val2")
            .containsEntry("calledCaseVar1", 42L)
            .containsEntry("outputParameter", 43L);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivity.testSubProcessLocalInputSingleVariableExpression.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testSubProcessLocalInputSingleVariableExpression() {
    ProcessInstance processInstance = startProcessInstanceByKey("subProcessLocalInputSingleVariableExpression");
    Task beforeCallActivityTask = taskService.createTaskQuery().singleResult();

    // when executing the call activity
    taskService.complete(beforeCallActivityTask.getId());

    // then the local input parameter can be resolved because its source expression variable
    // is defined in the call activity's input mapping
    CaseExecutionEntity calledInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(calledInstance).isNotNull();

    Map<String, Object> calledInstanceVariables = caseService.getVariables(calledInstance.getId());
    assertThat(calledInstanceVariables)
            .hasSize(1)
            .containsEntry("mappedInputParameter", 43L);

    // and completing it
    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();
    complete(humanTaskId);

    // and executing a call activity in parameter where the source variable is not mapped by an activity
    // input parameter fails

    runtimeService.setVariable(processInstance.getId(), "globalVariable", "42");
    var beforeSecondCallActivityTaskId = taskService.createTaskQuery().singleResult().getId();

    // when/then
    assertThatThrownBy(() -> taskService.complete(beforeSecondCallActivityTaskId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot resolve identifier 'globalVariable'");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivity.testSubProcessLocalOutputAllVariables.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  void testSubProcessLocalOutputAllVariables() {
    ProcessInstance processInstance = startProcessInstanceByKey("subProcessLocalOutputAllVariables");
    Task beforeCallActivityTask = taskService.createTaskQuery().singleResult();

    // when setting a variable in a process instance
    runtimeService.setVariable(processInstance.getId(), "callingProcessVar1", "val1");

    // and executing the call activity
    taskService.complete(beforeCallActivityTask.getId());

    // then all variables have been mapped into the called instance
    CaseExecutionEntity calledInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(calledInstance).isNotNull();

    Map<String, Object> calledInstanceVariables = caseService.getVariables(calledInstance.getId());
    assertThat(calledInstanceVariables)
            .hasSize(2)
            .containsEntry("callingProcessVar1", "val1")
            .containsEntry("inputParameter", "val2");

    // when setting a variable in the called instance
    caseService.setVariable(calledInstance.getId(), "calledCaseVar1", 42L);

    // and completing it
    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();
    manualStart(humanTaskId);
    complete(humanTaskId);

    // then only the output mapping variable has been mapped into the calling process instance
    Map<String, Object> callingInstanceVariables = runtimeService.getVariables(processInstance.getId());
    assertThat(callingInstanceVariables)
            .hasSize(2)
            .containsEntry("callingProcessVar1", "val1")
            .containsEntry("outputParameter", 43L);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CaseCallActivity.testSubProcessLocalOutputSingleVariable.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testSubProcessLocalOutputSingleVariable() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessLocalOutputSingleVariable");
    Task beforeCallActivityTask = taskService.createTaskQuery().singleResult();

    // when setting a variable in a process instance
    runtimeService.setVariable(processInstance.getId(), "callingProcessVar1", "val1");

    // and executing the call activity
    taskService.complete(beforeCallActivityTask.getId());

    // then all variables have been mapped into the called instance
    CaseExecutionEntity calledInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(calledInstance).isNotNull();

    Map<String, Object> calledInstanceVariables = caseService.getVariables(calledInstance.getId());
    assertThat(calledInstanceVariables)
            .hasSize(2)
            .containsEntry("callingProcessVar1", "val1")
            .containsEntry("inputParameter", "val2");

    // when setting a variable in the called instance
    caseService.setVariable(calledInstance.getId(), "calledCaseVar1", 42L);

    // and completing it
    String humanTaskId = queryCaseExecutionByActivityId(HUMAN_TASK_ID).getId();
    complete(humanTaskId);

    // then only the output mapping variable has been mapped into the calling process instance
    Map<String, Object> callingInstanceVariables = runtimeService.getVariables(processInstance.getId());
    assertThat(callingInstanceVariables)
            .hasSize(2)
            .containsEntry("callingProcessVar1", "val1")
            .containsEntry("outputParameter", 43L);
  }

  protected ProcessInstance startProcessInstanceByKey(String processDefinitionKey) {
    return startProcessInstanceByKey(processDefinitionKey, null);
  }

  protected ProcessInstance startProcessInstanceByKey(String processDefinitionKey, Map<String, Object> variables) {
    return startProcessInstanceByKey(processDefinitionKey, variables, null);
  }

  protected ProcessInstance startProcessInstanceByKey(String processDefinitionKey, Map<String, Object> variables, String businessKey) {
    return runtimeService.startProcessInstanceByKey(processDefinitionKey, businessKey, variables);
  }

  @Override
  protected CaseExecution queryCaseExecutionById(String id) {
    return caseService
        .createCaseExecutionQuery()
        .caseExecutionId(id)
        .singleResult();
  }

  @Override
  protected CaseExecution queryCaseExecutionByActivityId(String activityId) {
    return caseService
        .createCaseExecutionQuery()
        .activityId(activityId)
        .singleResult();
  }

  protected CaseInstance queryOneTaskCaseInstance() {
    return caseService
        .createCaseInstanceQuery()
        .caseDefinitionKey(ONE_TASK_CASE)
        .singleResult();
  }

  protected Execution queryExecutionById(String id) {
    return runtimeService
        .createExecutionQuery()
        .executionId(id)
        .singleResult();
  }

  protected Execution queryExecutionByActivityId(String activityId) {
    return runtimeService
        .createExecutionQuery()
        .activityId(activityId)
        .singleResult();
  }

  protected Task queryTaskByActivityId(String activityId) {
    return taskService
        .createTaskQuery()
        .taskDefinitionKey(activityId)
        .singleResult();
  }

}
