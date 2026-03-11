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
package org.operaton.bpm.engine.test.cmmn.casetask;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.exception.NotAllowedException;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionEntity;
import org.operaton.bpm.engine.impl.cmmn.execution.CmmnExecution;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.impl.VariableMapImpl;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Roman Smirnov
 *
 */
class CaseTaskTest extends CmmnTest {

  static final String CASE_TASK = "PI_CaseTask_1";
  static final String ONE_CASE_TASK_CASE = "oneCaseTaskCase";
  static final String ONE_TASK_CASE = "oneTaskCase";

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testCallCaseAsConstant() {
    // given
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();

    // then
    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    String superCaseExecutionId = subCaseInstance.getSuperCaseExecutionId();
    CaseExecution superCaseExecution = queryCaseExecutionById(superCaseExecutionId);

    assertThat(superCaseExecutionId).isEqualTo(caseTaskId);
    assertThat(superCaseExecution.getCaseInstanceId()).isEqualTo(superCaseInstanceId);

    // complete ////////////////////////////////////////////////////////

    terminate(subCaseInstance.getId());
    close(subCaseInstance.getId());
    testRule.assertCaseEnded(subCaseInstance.getId());

    terminate(caseTaskId);
    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/casetask/CaseTaskTest.testCallCaseAsExpressionStartsWithDollar.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testCallCaseAsExpressionStartsWithDollar() {
    // given
    // a deployed case definition
    VariableMap vars = new VariableMapImpl();
    vars.putValue("oneTaskCase", ONE_TASK_CASE);
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE, vars).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();

    // then

    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    String superCaseExecutionId = subCaseInstance.getSuperCaseExecutionId();
    CaseExecution superCaseExecution = queryCaseExecutionById(superCaseExecutionId);

    assertThat(superCaseExecutionId).isEqualTo(caseTaskId);
    assertThat(superCaseExecution.getCaseInstanceId()).isEqualTo(superCaseInstanceId);

    // complete ////////////////////////////////////////////////////////

    terminate(subCaseInstance.getId());
    close(subCaseInstance.getId());
    testRule.assertCaseEnded(subCaseInstance.getId());

    terminate(caseTaskId);
    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/casetask/CaseTaskTest.testCallCaseAsExpressionStartsWithHash.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testCallCaseAsExpressionStartsWithHash() {
    // given
    // a deployed case definition
    VariableMap vars = new VariableMapImpl();
    vars.putValue("oneTaskCase", ONE_TASK_CASE);
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE, vars).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();

    // then

    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    String superCaseExecutionId = subCaseInstance.getSuperCaseExecutionId();
    CaseExecution superCaseExecution = queryCaseExecutionById(superCaseExecutionId);

    assertThat(superCaseExecutionId).isEqualTo(caseTaskId);
    assertThat(superCaseExecution.getCaseInstanceId()).isEqualTo(superCaseInstanceId);

    // complete ////////////////////////////////////////////////////////

    terminate(subCaseInstance.getId());
    close(subCaseInstance.getId());
    testRule.assertCaseEnded(subCaseInstance.getId());

    terminate(caseTaskId);
    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);
  }

  /**
   * assert on default behaviour - remove manual activation
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/casetask/CaseTaskTest.testCallLatestCase.cmmn",
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

    assertThat(repositoryService.createCaseDefinitionQuery().count()).isEqualTo(3);

    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();

    String latestCaseDefinitionId = repositoryService
      .createCaseDefinitionQuery()
      .caseDefinitionKey(ONE_TASK_CASE)
      .latestVersion()
      .singleResult()
      .getId();

    // then

    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    String superCaseExecutionId = subCaseInstance.getSuperCaseExecutionId();
    CaseExecution superCaseExecution = queryCaseExecutionById(superCaseExecutionId);

    assertThat(superCaseExecutionId).isEqualTo(caseTaskId);
    assertThat(superCaseExecution.getCaseInstanceId()).isEqualTo(superCaseInstanceId);
    assertThat(subCaseInstance.getCaseDefinitionId()).isEqualTo(latestCaseDefinitionId);

    // complete ////////////////////////////////////////////////////////

    terminate(subCaseInstance.getId());
    close(subCaseInstance.getId());
    testRule.assertCaseEnded(subCaseInstance.getId());

    terminate(caseTaskId);
    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

    repositoryService.deleteDeployment(deploymentId, true);
  }

  /**
   * default behaviour of manual activation changed - remove manual activation
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/casetask/CaseTaskTest.testCallCaseByDeployment.cmmn",
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

    assertThat(repositoryService.createCaseDefinitionQuery().count()).isEqualTo(3);

    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();

    String caseDefinitionIdInSameDeployment = repositoryService
      .createCaseDefinitionQuery()
      .caseDefinitionKey(ONE_TASK_CASE)
      .deploymentId(firstDeploymentId)
      .singleResult()
      .getId();

    // then

    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    String superCaseExecutionId = subCaseInstance.getSuperCaseExecutionId();
    CaseExecution superCaseExecution = queryCaseExecutionById(superCaseExecutionId);

    assertThat(superCaseExecutionId).isEqualTo(caseTaskId);
    assertThat(superCaseExecution.getCaseInstanceId()).isEqualTo(superCaseInstanceId);
    assertThat(subCaseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionIdInSameDeployment);

    // complete ////////////////////////////////////////////////////////

    terminate(subCaseInstance.getId());
    close(subCaseInstance.getId());
    testRule.assertCaseEnded(subCaseInstance.getId());

    terminate(caseTaskId);
    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

    repositoryService.deleteDeployment(deploymentId, true);
  }

  /**
   * assertions on completion - take manual activation out
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/casetask/CaseTaskTest.testCallCaseByVersion.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testCallCaseByVersion() {
    // given

    String bpmnResourceName = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn";
    String secondDeploymentId = repositoryService.createDeployment()
            .addClasspathResource(bpmnResourceName)
            .deploy()
            .getId();

    String thirdDeploymentId = repositoryService.createDeployment()
          .addClasspathResource(bpmnResourceName)
          .deploy()
          .getId();

    assertThat(repositoryService.createCaseDefinitionQuery().count()).isEqualTo(4);

    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();

    String caseDefinitionIdInSecondDeployment = repositoryService
      .createCaseDefinitionQuery()
      .caseDefinitionKey(ONE_TASK_CASE)
      .deploymentId(secondDeploymentId)
      .singleResult()
      .getId();

    // then

    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    String superCaseExecutionId = subCaseInstance.getSuperCaseExecutionId();
    CaseExecution superCaseExecution = queryCaseExecutionById(superCaseExecutionId);

    assertThat(superCaseExecutionId).isEqualTo(caseTaskId);
    assertThat(superCaseExecution.getCaseInstanceId()).isEqualTo(superCaseInstanceId);
    assertThat(subCaseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionIdInSecondDeployment);

    // complete ////////////////////////////////////////////////////////

    terminate(subCaseInstance.getId());
    close(subCaseInstance.getId());
    testRule.assertCaseEnded(subCaseInstance.getId());

    terminate(caseTaskId);
    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

    repositoryService.deleteDeployment(secondDeploymentId, true);
    repositoryService.deleteDeployment(thirdDeploymentId, true);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/casetask/CaseTaskTest.testCallCaseByVersionAsExpressionStartsWithDollar.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testCallCaseByVersionAsExpressionStartsWithDollar() {
    // given

    String bpmnResourceName = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn";

    String secondDeploymentId = repositoryService.createDeployment()
            .addClasspathResource(bpmnResourceName)
            .deploy()
            .getId();

    String thirdDeploymentId = repositoryService.createDeployment()
          .addClasspathResource(bpmnResourceName)
          .deploy()
          .getId();

    assertThat(repositoryService.createCaseDefinitionQuery().count()).isEqualTo(4);

    VariableMap vars = new VariableMapImpl();
    vars.putValue("myVersion", 2);
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE,vars).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();

    String caseDefinitionIdInSecondDeployment = repositoryService
      .createCaseDefinitionQuery()
      .caseDefinitionKey(ONE_TASK_CASE)
      .deploymentId(secondDeploymentId)
      .singleResult()
      .getId();

    // then

    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    String superCaseExecutionId = subCaseInstance.getSuperCaseExecutionId();
    CaseExecution superCaseExecution = queryCaseExecutionById(superCaseExecutionId);

    assertThat(superCaseExecutionId).isEqualTo(caseTaskId);
    assertThat(superCaseExecution.getCaseInstanceId()).isEqualTo(superCaseInstanceId);
    assertThat(subCaseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionIdInSecondDeployment);

    // complete ////////////////////////////////////////////////////////

    terminate(subCaseInstance.getId());
    close(subCaseInstance.getId());
    testRule.assertCaseEnded(subCaseInstance.getId());

    terminate(caseTaskId);
    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

    repositoryService.deleteDeployment(secondDeploymentId, true);
    repositoryService.deleteDeployment(thirdDeploymentId, true);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/casetask/CaseTaskTest.testCallCaseByVersionAsExpressionStartsWithHash.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testCallCaseByVersionAsExpressionStartsWithHash() {
    // given

    String bpmnResourceName = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn";

    String secondDeploymentId = repositoryService.createDeployment()
            .addClasspathResource(bpmnResourceName)
            .deploy()
            .getId();

    String thirdDeploymentId = repositoryService.createDeployment()
          .addClasspathResource(bpmnResourceName)
          .deploy()
          .getId();

    assertThat(repositoryService.createCaseDefinitionQuery().count()).isEqualTo(4);

    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();

    String caseDefinitionIdInSecondDeployment = repositoryService
      .createCaseDefinitionQuery()
      .caseDefinitionKey(ONE_TASK_CASE)
      .deploymentId(secondDeploymentId)
      .singleResult()
      .getId();

    // when
    caseService
      .withCaseExecution(caseTaskId)
      .setVariable("myVersion", 2)
      .manualStart();

    // then

    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    String superCaseExecutionId = subCaseInstance.getSuperCaseExecutionId();
    CaseExecution superCaseExecution = queryCaseExecutionById(superCaseExecutionId);

    assertThat(superCaseExecutionId).isEqualTo(caseTaskId);
    assertThat(superCaseExecution.getCaseInstanceId()).isEqualTo(superCaseInstanceId);
    assertThat(subCaseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionIdInSecondDeployment);

    // complete ////////////////////////////////////////////////////////

    terminate(subCaseInstance.getId());
    close(subCaseInstance.getId());
    testRule.assertCaseEnded(subCaseInstance.getId());

    terminate(caseTaskId);
    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

    repositoryService.deleteDeployment(secondDeploymentId, true);
    repositoryService.deleteDeployment(thirdDeploymentId, true);
  }

  /**
   * assertion on default behaviour - remove manual activation
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/casetask/CaseTaskTest.testInputBusinessKey.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testInputBusinessKey() {
    // given
    String businessKey = "myBusinessKey";
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE, businessKey).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();

    // then

    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    String superCaseExecutionId = subCaseInstance.getSuperCaseExecutionId();
    CaseExecution superCaseExecution = queryCaseExecutionById(superCaseExecutionId);

    assertThat(superCaseExecutionId).isEqualTo(caseTaskId);
    assertThat(superCaseExecution.getCaseInstanceId()).isEqualTo(superCaseInstanceId);
    assertThat(subCaseInstance.getBusinessKey()).isEqualTo(businessKey);

    // complete ////////////////////////////////////////////////////////

    terminate(subCaseInstance.getId());
    close(subCaseInstance.getId());
    testRule.assertCaseEnded(subCaseInstance.getId());

    terminate(caseTaskId);
    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

  }

  /**
   * variable passed in manual activation - change process definition
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/casetask/CaseTaskTest.testInputDifferentBusinessKey.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testInputDifferentBusinessKey() {
    // given
    String businessKey = "myBusinessKey";
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE, businessKey).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();

    // when
    caseService
      .withCaseExecution(caseTaskId)
      .setVariable("myOwnBusinessKey", "myOwnBusinessKey")
      .manualStart();

    // then

    CaseExecutionEntity subCaseInstance = (CaseExecutionEntity) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    String superCaseExecutionId = subCaseInstance.getSuperCaseExecutionId();
    CaseExecution superCaseExecution = queryCaseExecutionById(superCaseExecutionId);

    assertThat(superCaseExecutionId).isEqualTo(caseTaskId);
    assertThat(superCaseExecution.getCaseInstanceId()).isEqualTo(superCaseInstanceId);
    assertThat(subCaseInstance.getBusinessKey()).isEqualTo("myOwnBusinessKey");

    // complete ////////////////////////////////////////////////////////

    terminate(subCaseInstance.getId());
    close(subCaseInstance.getId());
    testRule.assertCaseEnded(subCaseInstance.getId());

    terminate(caseTaskId);
    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

  }

  /**
   * assertion on variables which are set on manual start - change process definition
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/casetask/CaseTaskTest.testInputSourceWithManualActivation.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testInputSource() {
    // given
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();

    // when
    caseService
      .withCaseExecution(caseTaskId)
      .setVariable("aVariable", "abc")
      .setVariable("anotherVariable", 999)
      .setVariable("aThirdVariable", "def")
      .manualStart();

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

    terminate(subCaseInstance.getId());
    close(subCaseInstance.getId());
    testRule.assertCaseEnded(subCaseInstance.getId());

    terminate(caseTaskId);
    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

  }

  /**
   * default manual activation behaviour changed - remove manual activation statement
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/casetask/CaseTaskTest.testInputSourceDifferentTarget.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testInputSourceDifferentTarget() {
    // given
    VariableMap vars = new VariableMapImpl();
    vars.putValue("aVariable", "abc");
    vars.putValue("anotherVariable", 999);
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE, vars).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();

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

    terminate(subCaseInstance.getId());
    close(subCaseInstance.getId());
    testRule.assertCaseEnded(subCaseInstance.getId());

    terminate(caseTaskId);
    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

  }

  /**
   * assertion on default execution - take manual start out
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/casetask/CaseTaskTest.testInputSource.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testInputSourceNullValue() {
    // given
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();

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

    terminate(subCaseInstance.getId());
    close(subCaseInstance.getId());
    testRule.assertCaseEnded(subCaseInstance.getId());

    terminate(caseTaskId);
    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);
  }

  /**
   * Default manual activation changed - add variables to case instantiation, remove manual activation
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/casetask/CaseTaskTest.testInputSourceExpression.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testInputSourceExpression() {
    // given
    VariableMap vars = new VariableMapImpl();
    vars.putValue("aVariable", "abc");
    vars.putValue("anotherVariable", 999);
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE,vars).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();

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

    terminate(subCaseInstance.getId());
    close(subCaseInstance.getId());
    testRule.assertCaseEnded(subCaseInstance.getId());

    terminate(caseTaskId);
    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/casetask/CaseTaskTest.testInputAll.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testInputAll() {
    // given
    VariableMap vars = new VariableMapImpl();
    vars.putValue("aVariable", "abc");
    vars.putValue("anotherVariable", 999);
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE, vars).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();

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

    terminate(subCaseInstance.getId());
    close(subCaseInstance.getId());
    testRule.assertCaseEnded(subCaseInstance.getId());

    terminate(caseTaskId);
    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

  }

  /**
   * assert on variable defined during manual start - change process definition
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/casetask/CaseTaskTest.testInputAllLocal.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testInputAllLocal() {
    // given
    createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();

    // when
    caseService
      .withCaseExecution(caseTaskId)
      .setVariable("aVariable", "abc")
      .setVariableLocal("aLocalVariable", "def")
      .manualStart();

    // then only the local variable is mapped to the subCaseInstance
    CaseInstance subCaseInstance = queryOneTaskCaseInstance();

    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(subCaseInstance.getId())
        .list();

    assertThat(variables).hasSize(1);
    assertThat(variables.get(0).getName()).isEqualTo("aLocalVariable");
  }

  /**
   * assertion on manual activation operation - change process definition
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCaseWithManualActivation.cmmn"
  })
  @Test
  void testCaseNotFound() {
    // given
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();
    var caseExecutionCommandBuilder = caseService
        .withCaseExecution(caseTaskId);

    assertThatThrownBy(caseExecutionCommandBuilder::manualStart).isInstanceOf(NotFoundException.class);

    // complete //////////////////////////////////////////////////////////

    caseService
      .withCaseExecution(caseTaskId)
      .disable();
    close(superCaseInstanceId);

  }

  /**
   * assertion on completion - remove manual start
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testCompleteSimpleCase() {
    // given
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();

    String subCaseInstanceId = queryOneTaskCaseInstance().getId();
    String humanTaskId = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    // when
    caseService
      .withCaseExecution(humanTaskId)
      .complete();

    // then

    CaseExecution caseTask = queryCaseExecutionByActivityId("PI_CaseTask_1");
    assertThat(caseTask).isNull();

    // complete ////////////////////////////////////////////////////////

    close(subCaseInstanceId);
    testRule.assertCaseEnded(subCaseInstanceId);

    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

  }

  /**
   * subprocess manual start with variables - change process definition
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/casetask/CaseTaskTest.testOutputSource.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"
  })
  @Test
  void testOutputSource() {
    // given
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();

    String subCaseInstanceId = queryOneTaskCaseInstance().getId();

    caseService
      .withCaseExecution(subCaseInstanceId)
      .setVariable("aVariable", "abc")
      .setVariable("anotherVariable", 999)
      .setVariable("aThirdVariable", "def")
      .execute();

    String humanTaskId = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    caseService
      .withCaseExecution(humanTaskId)
      .manualStart();

    caseService
      .withCaseExecution(humanTaskId)
      .complete();

    // when
    caseService
      .withCaseExecution(subCaseInstanceId)
      .close();

    // then
    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(superCaseInstanceId)
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

    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

  }

  /**
   * default behaviour of manual activation changed - remove manual activation
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/casetask/CaseTaskTest.testOutputSourceDifferentTarget.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testOutputSourceDifferentTarget() {
    // given
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();

    String subCaseInstanceId = queryOneTaskCaseInstance().getId();

    caseService
      .withCaseExecution(subCaseInstanceId)
      .setVariable("aVariable", "abc")
      .setVariable("anotherVariable", 999)
      .execute();

    String humanTaskId = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    caseService
      .withCaseExecution(humanTaskId)
      .complete();

    // when
    caseService
      .withCaseExecution(subCaseInstanceId)
      .close();

    // then
    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(superCaseInstanceId)
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

    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

  }

  /**
   * assertion on default behaviour - remove manual activations
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/casetask/CaseTaskTest.testOutputSource.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testOutputSourceNullValue() {
    // given
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();

    String subCaseInstanceId = queryOneTaskCaseInstance().getId();
    String humanTaskId = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    caseService
      .withCaseExecution(humanTaskId)
      .complete();

    // when
    caseService
      .withCaseExecution(subCaseInstanceId)
      .close();

    // then
    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(superCaseInstanceId)
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

    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

  }

  /**
   * assertion on variables - change process definition
   * manual start on case not needed enaymore and therefore removed
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/casetask/CaseTaskTest.testOutputSourceExpression.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"
  })
  @Test
  void testOutputSourceExpression() {
    // given
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();

    String subCaseInstanceId = queryOneTaskCaseInstance().getId();

    caseService
      .withCaseExecution(subCaseInstanceId)
      .setVariable("aVariable", "abc")
      .setVariable("anotherVariable", 999)
      .execute();

    String humanTaskId = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    caseService
      .withCaseExecution(humanTaskId)
      .manualStart();

    caseService
      .withCaseExecution(humanTaskId)
      .complete();

    // when
    caseService
      .withCaseExecution(subCaseInstanceId)
      .close();

    // then
    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(superCaseInstanceId)
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

    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

  }

  /**
   * since assertion happens on variables, changing oneTaskCase definition to have manual activation,
   * case task behaviour changed, so manual activation is taken out
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/casetask/CaseTaskTest.testOutputAll.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"
  })
  @Test
  void testOutputAll() {
    // given
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();

    String subCaseInstanceId = queryOneTaskCaseInstance().getId();

    caseService
      .withCaseExecution(subCaseInstanceId)
      .setVariable("aVariable", "abc")
      .setVariable("anotherVariable", 999)
      .execute();

    String humanTaskId = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    caseService
      .withCaseExecution(humanTaskId)
      .manualStart();

    caseService
      .withCaseExecution(humanTaskId)
      .complete();

    // when
    caseService
      .withCaseExecution(subCaseInstanceId)
      .close();

    // then
    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(superCaseInstanceId)
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

    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/casetask/CaseTaskTest.testOutputVariablesShouldNotExistAnymore.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"
  })
  @Test
  void testOutputVariablesShouldNotExistAnymore() {
    // given
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();

    caseService
      .withCaseExecution(caseTaskId)
      // set variables local
      .setVariableLocal("aVariable", "xyz")
      .setVariableLocal("anotherVariable", 123)
      .manualStart();

    String subCaseInstanceId = queryOneTaskCaseInstance().getId();

    caseService
      .withCaseExecution(subCaseInstanceId)
      .setVariable("aVariable", "abc")
      .setVariable("anotherVariable", 999)
      .execute();

    String humanTaskId = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    caseService
      .withCaseExecution(humanTaskId)
      .manualStart();

    caseService
      .withCaseExecution(humanTaskId)
      .complete();

    // when
    caseService
      .withCaseExecution(subCaseInstanceId)
      .close();

    // then

    // the variables has been deleted
    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(superCaseInstanceId)
        .list();

    assertThat(variables).isEmpty();

    // complete ////////////////////////////////////////////////////////

    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

  }

  /**
   * assertion on variables - change subprocess definition
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/casetask/CaseTaskTest.testVariablesRoundtrip.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"
  })
  @Test
  void testVariablesRoundtrip() {
    // given

    VariableMap vars = new VariableMapImpl();
    vars.putValue("aVariable", "xyz");
    vars.putValue("anotherVariable", 123);
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE, vars).getId();

    String subCaseInstanceId = queryOneTaskCaseInstance().getId();

    caseService
      .withCaseExecution(subCaseInstanceId)
      .setVariable("aVariable", "abc")
      .setVariable("anotherVariable", 999)
      .execute();

    String humanTaskId = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    caseService
      .withCaseExecution(humanTaskId)
      .manualStart();

    caseService
      .withCaseExecution(humanTaskId)
      .complete();

    // when
    caseService
      .withCaseExecution(subCaseInstanceId)
      .close();

    // then

    List<VariableInstance> variables = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(superCaseInstanceId)
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

    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

  }

  /**
   * Default behaviour changed, so manual start is taken out
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testCompleteCaseTask() {
    // given
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();
    var caseExecutionCommandBuilder = caseService
        .withCaseExecution(caseTaskId);

    assertThatThrownBy(caseExecutionCommandBuilder::complete).isInstanceOf(NotAllowedException.class);


    // complete ////////////////////////////////////////////////////////

    String subCaseInstanceId = queryOneTaskCaseInstance().getId();
    terminate(subCaseInstanceId);
    close(subCaseInstanceId);
    testRule.assertCaseEnded(subCaseInstanceId);

    terminate(caseTaskId);
    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

  }

  /**
   * assert on default behaviour - remove manual activation
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testTerminateCaseTask() {
    // given
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();

    CaseInstance subCaseInstance = queryOneTaskCaseInstance();
    assertThat(subCaseInstance.isActive()).isTrue();

    // when
    terminate(caseTaskId);

    subCaseInstance = queryOneTaskCaseInstance();
    assertThat(subCaseInstance.isActive()).isTrue();

    // complete ////////////////////////////////////////////////////////

    String subCaseInstanceId = queryOneTaskCaseInstance().getId();
    terminate(subCaseInstanceId);
    close(subCaseInstanceId);
    testRule.assertCaseEnded(subCaseInstanceId);

    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

  }

  /**
   * removed manual start as it is handled by default behaviour
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testTerminateSubCaseInstance() {
    // given
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();

    String subCaseInstanceId = queryOneTaskCaseInstance().getId();

    // when
    terminate(subCaseInstanceId);

    // then
    CmmnExecution subCaseInstance = (CmmnExecution) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();
    assertThat(subCaseInstance.isTerminated()).isTrue();

    CaseExecution caseTask = queryCaseExecutionByActivityId(CASE_TASK);
    assertThat(caseTask).isNotNull();
    assertThat(caseTask.isActive()).isTrue();

    // complete ////////////////////////////////////////////////////////

    close(subCaseInstanceId);
    testRule.assertCaseEnded(subCaseInstanceId);

    terminate(caseTaskId);
    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

  }

  /**
   * assertion on completion - remove manual start
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testSuspendCaseTask() {
    // given
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();

    CaseInstance subCaseInstance = queryOneTaskCaseInstance();
    assertThat(subCaseInstance.isActive()).isTrue();

    // when
    suspend(caseTaskId);

    subCaseInstance = queryOneTaskCaseInstance();
    assertThat(subCaseInstance.isActive()).isTrue();

    // complete ////////////////////////////////////////////////////////

    String subCaseInstanceId = queryOneTaskCaseInstance().getId();
    terminate(subCaseInstanceId);
    close(subCaseInstanceId);
    testRule.assertCaseEnded(subCaseInstanceId);

    terminate(superCaseInstanceId);
    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

  }

  /**
   * default behaviour of manual activation changed - remove manual activation
   * change definition of oneTaskCase in order to allow suspension state
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"
  })
  @Test
  void testSuspendSubCaseInstance() {
    // given
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();

    String subCaseInstanceId = queryOneTaskCaseInstance().getId();

    // when
    suspend(subCaseInstanceId);

    // then
    CmmnExecution subCaseInstance = (CmmnExecution) queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();
    assertThat(subCaseInstance.isSuspended()).isTrue();

    CaseExecution caseTask = queryCaseExecutionByActivityId(CASE_TASK);
    assertThat(caseTask).isNotNull();
    assertThat(caseTask.isActive()).isTrue();

    // complete ////////////////////////////////////////////////////////

    close(subCaseInstanceId);
    testRule.assertCaseEnded(subCaseInstanceId);

    terminate(caseTaskId);
    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testResumeCaseTask() {
    // given
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK).getId();

    suspend(caseTaskId);

    CaseInstance subCaseInstance = queryOneTaskCaseInstance();
    assertThat(subCaseInstance.isActive()).isTrue();

    // when
    resume(caseTaskId);

    // then
    CaseExecution caseTask = queryCaseExecutionByActivityId(CASE_TASK);
    assertThat(caseTask.isActive()).isTrue();

    subCaseInstance = queryOneTaskCaseInstance();
    assertThat(subCaseInstance.isActive()).isTrue();

    // complete ////////////////////////////////////////////////////////

    String subCaseInstanceId = queryOneTaskCaseInstance().getId();
    terminate(subCaseInstanceId);
    close(subCaseInstanceId);
    testRule.assertCaseEnded(subCaseInstanceId);

    terminate(caseTaskId);
    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

  }

  /**
   * assert on default behaviour - remove manual activation
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/casetask/CaseTaskTest.testNotBlockingCaseTask.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testNotBlockingCaseTask() {
    // given
    String superCaseInstanceId = createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();

    // then
    CaseInstance subCaseInstance = queryOneTaskCaseInstance();
    assertThat(subCaseInstance).isNotNull();

    CaseExecution caseTask = queryCaseExecutionByActivityId(CASE_TASK);
    assertThat(caseTask).isNull();

    CaseInstance superCaseInstance = caseService
        .createCaseInstanceQuery()
        .caseDefinitionKey(ONE_CASE_TASK_CASE)
        .singleResult();
    assertThat(superCaseInstance).isNotNull();
    assertThat(superCaseInstance.isCompleted()).isTrue();

    // complete ////////////////////////////////////////////////////////

    close(superCaseInstanceId);
    testRule.assertCaseEnded(superCaseInstanceId);

    terminate(subCaseInstance.getId());
    close(subCaseInstance.getId());
    testRule.assertProcessEnded(subCaseInstance.getId());

  }

  /**
   * Changed process definition as we prove activity type
   */
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCaseWithManualActivation.cmmn"})
  @Test
  void testActivityType() {
    // given
    createCaseInstanceByKey(ONE_CASE_TASK_CASE).getId();

    // when
    CaseExecution caseTask = queryCaseExecutionByActivityId(CASE_TASK);

    // then
    assertThat(caseTask.getActivityType()).isEqualTo("caseTask");
  }

  @Override
  protected CaseInstance createCaseInstanceByKey(String caseDefinitionKey) {
    return createCaseInstanceByKey(caseDefinitionKey, null, null);
  }

  @Override
  protected CaseInstance createCaseInstanceByKey(String caseDefinitionKey, String businessKey) {
    return caseService
        .withCaseDefinitionByKey(caseDefinitionKey)
        .businessKey(businessKey)
        .create();
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

  protected Task queryTask() {
    return taskService
        .createTaskQuery()
        .singleResult();
  }

}
