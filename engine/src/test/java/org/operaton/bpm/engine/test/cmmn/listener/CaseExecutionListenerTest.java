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
package org.operaton.bpm.engine.test.cmmn.listener;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.operaton.bpm.engine.delegate.CaseExecutionListener;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.runtime.VariableInstanceQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Smirnov
 *
 */
class CaseExecutionListenerTest extends CmmnTest {

  @ParameterizedTest
  @CsvSource({
    "org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testCreateListenerByClass.cmmn,create",
    "org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testCreateListenerByScript.cmmn,create",
    "org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testEnableListenerByClass.cmmn,enable",
    "org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testEnableListenerByScript.cmmn,enable",
    "org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testStartListenerByClass.cmmn,start"
  })
  void testListenerByResourceAndEvent(String resource, String eventName) {
    // deploy the resource under test
    processEngineRule.manageDeployment(
      repositoryService.createDeployment()
      .addClasspathResource(resource)
      .deploy());

    // when
    String caseInstanceId = caseService
      .withCaseDefinitionByKey("case")
      .create()
      .getId();

    // then
    String humanTaskId = caseService
      .createCaseExecutionQuery()
      .activityId("PI_HumanTask_1")
      .singleResult()
      .getId();

    VariableInstanceQuery query = runtimeService
      .createVariableInstanceQuery()
      .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName(eventName).singleResult().getValue()).isTrue();
    assertThat(query.variableName(eventName + "EventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName(eventName + "OnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);
  }

  @ParameterizedTest
  @CsvSource({
    "org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testCreateListenerByDelegateExpression.cmmn,create",
    "org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testEnableListenerByDelegateExpression.cmmn,enable",
    "org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testStartListenerByDelegateExpression.cmmn,start"
  })
  void testListenerByDelegateExpression(String resource, String eventName) {
    // deploy the resource under test
    processEngineRule.manageDeployment(
      repositoryService.createDeployment()
        .addClasspathResource(resource)
        .deploy());

    // when
    String caseInstanceId = caseService
      .withCaseDefinitionByKey("case")
      .setVariable("myListener", new MySpecialCaseExecutionListener())
      .create()
      .getId();

    // then
    String humanTaskId = caseService
      .createCaseExecutionQuery()
      .activityId("PI_HumanTask_1")
      .singleResult()
      .getId();

    VariableInstanceQuery query = runtimeService
      .createVariableInstanceQuery()
      .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName(eventName).singleResult().getValue()).isTrue();
    assertThat(query.variableName(eventName + "EventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName(eventName + "OnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);
  }

  @ParameterizedTest
  @CsvSource({
    "org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testCreateListenerByExpression.cmmn,create",
    "org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testEnableListenerByExpression.cmmn,enable",
    "org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testStartListenerByExpression.cmmn,start"
  })
  void testListenerByExpression(String resource, String eventName) {
    // deploy the resource under test
    processEngineRule.manageDeployment(
      repositoryService.createDeployment()
        .addClasspathResource(resource)
        .deploy());

    // when
    String caseInstanceId = caseService
      .withCaseDefinitionByKey("case")
      .setVariable("myListener", new MyCaseExecutionListener())
      .create()
      .getId();

    // then
    String humanTaskId = caseService
      .createCaseExecutionQuery()
      .activityId("PI_HumanTask_1")
      .singleResult()
      .getId();

    VariableInstanceQuery query = runtimeService
      .createVariableInstanceQuery()
      .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName(eventName).singleResult().getValue()).isTrue();
    assertThat(query.variableName(eventName + "EventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName(eventName + "OnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testDisableListenerByClass.cmmn"})
  @Test
  void testDisableListenerByClass() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    caseService
      .withCaseExecution(humanTaskId)
      .disable();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("disable").singleResult().getValue()).isTrue();
    assertThat(query.variableName("disableEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("disableOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testDisableListenerByDelegateExpression.cmmn"})
  @Test
  void testDisableListenerByDelegateExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MySpecialCaseExecutionListener())
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    caseService
      .withCaseExecution(humanTaskId)
      .disable();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("disable").singleResult().getValue()).isTrue();
    assertThat(query.variableName("disableEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("disableOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testDisableListenerByExpression.cmmn"})
  @Test
  void testDisableListenerByExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MyCaseExecutionListener())
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    caseService
      .withCaseExecution(humanTaskId)
      .disable();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("disable").singleResult().getValue()).isTrue();
    assertThat(query.variableName("disableEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("disableOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testDisableListenerByScript.cmmn"})
  @Test
  void testDisableListenerByScript() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    caseService
      .withCaseExecution(humanTaskId)
      .disable();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("disable").singleResult().getValue()).isTrue();
    assertThat(query.variableName("disableEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("disableOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testReEnableListenerByClass.cmmn"})
  @Test
  void testReEnableListenerByClass() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    caseService
      .withCaseExecution(humanTaskId)
      .disable();

    // when
    caseService
      .withCaseExecution(humanTaskId)
      .reenable();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("reenable").singleResult().getValue()).isTrue();
    assertThat(query.variableName("reenableEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("reenableOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testReEnableListenerByDelegateExpression.cmmn"})
  @Test
  void testReEnableListenerByDelegateExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MySpecialCaseExecutionListener())
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    caseService
      .withCaseExecution(humanTaskId)
      .disable();

    // when
    caseService
      .withCaseExecution(humanTaskId)
      .reenable();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("reenable").singleResult().getValue()).isTrue();
    assertThat(query.variableName("reenableEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("reenableOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testReEnableListenerByExpression.cmmn"})
  @Test
  void testReEnableListenerByExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MyCaseExecutionListener())
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    caseService
      .withCaseExecution(humanTaskId)
      .disable();

    // when
    caseService
      .withCaseExecution(humanTaskId)
      .reenable();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("reenable").singleResult().getValue()).isTrue();
    assertThat(query.variableName("reenableEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("reenableOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testReEnableListenerByScript.cmmn"})
  @Test
  void testReEnableListenerByScript() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    caseService
      .withCaseExecution(humanTaskId)
      .disable();

    // when
    caseService
      .withCaseExecution(humanTaskId)
      .reenable();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("reenable").singleResult().getValue()).isTrue();
    assertThat(query.variableName("reenableEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("reenableOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testManualStartListenerByClass.cmmn"})
  @Test
  void testManualStartListenerByClass() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    caseService
      .withCaseExecution(humanTaskId)
      .manualStart();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("manualStart").singleResult().getValue()).isTrue();
    assertThat(query.variableName("manualStartEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("manualStartOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testManualStartListenerByDelegateExpression.cmmn"})
  @Test
  void testManualStartListenerByDelegateExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MySpecialCaseExecutionListener())
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    caseService
      .withCaseExecution(humanTaskId)
      .manualStart();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("manualStart").singleResult().getValue()).isTrue();
    assertThat(query.variableName("manualStartEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("manualStartOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testManualStartListenerByExpression.cmmn"})
  @Test
  void testManualStartListenerByExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MyCaseExecutionListener())
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    caseService
      .withCaseExecution(humanTaskId)
      .manualStart();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("manualStart").singleResult().getValue()).isTrue();
    assertThat(query.variableName("manualStartEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("manualStartOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testManualStartListenerByScript.cmmn"})
  @Test
  void testManualStartListenerByScript() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    caseService
      .withCaseExecution(humanTaskId)
      .manualStart();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("manualStart").singleResult().getValue()).isTrue();
    assertThat(query.variableName("manualStartEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("manualStartOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testCompleteListenerByClass.cmmn"})
  @Test
  void testCompleteListenerByClass() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    caseService
      .withCaseExecution(humanTaskId)
      .complete();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("complete").singleResult().getValue()).isTrue();
    assertThat(query.variableName("completeEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("completeOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testCompleteListenerByDelegateExpression.cmmn"})
  @Test
  void testCompleteListenerByDelegateExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MySpecialCaseExecutionListener())
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    caseService
      .withCaseExecution(humanTaskId)
      .complete();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("complete").singleResult().getValue()).isTrue();
    assertThat(query.variableName("completeEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("completeOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testCompleteListenerByExpression.cmmn"})
  @Test
  void testCompleteListenerByExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MyCaseExecutionListener())
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    caseService
      .withCaseExecution(humanTaskId)
      .complete();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("complete").singleResult().getValue()).isTrue();
    assertThat(query.variableName("completeEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("completeOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testCompleteListenerByScript.cmmn"})
  @Test
  void testCompleteListenerByScript() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    caseService
      .withCaseExecution(humanTaskId)
      .complete();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("complete").singleResult().getValue()).isTrue();
    assertThat(query.variableName("completeEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("completeOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testTerminateListenerByClass.cmmn"})
  @Test
  void testTerminateListenerByClass() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    terminate(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("terminate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("terminateEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("terminateOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testTerminateListenerByDelegateExpression.cmmn"})
  @Test
  void testTerminateListenerByDelegateExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MySpecialCaseExecutionListener())
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    terminate(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("terminate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("terminateEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("terminateOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testTerminateListenerByExpression.cmmn"})
  @Test
  void testTerminateListenerByExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MyCaseExecutionListener())
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    terminate(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("terminate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("terminateEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("terminateOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testTerminateListenerByScript.cmmn"})
  @Test
  void testTerminateListenerByScript() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    terminate(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("terminate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("terminateEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("terminateOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testExitListenerByClass.cmmn"})
  @Test
  void testExitListenerByClass() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    exit(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("exit").singleResult().getValue()).isTrue();
    assertThat(query.variableName("exitEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("exitOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testExitListenerByDelegateExpression.cmmn"})
  @Test
  void testExitListenerByDelegateExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MySpecialCaseExecutionListener())
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    exit(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("exit").singleResult().getValue()).isTrue();
    assertThat(query.variableName("exitEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("exitOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testExitListenerByExpression.cmmn"})
  @Test
  void testExitListenerByExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MyCaseExecutionListener())
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    exit(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("exit").singleResult().getValue()).isTrue();
    assertThat(query.variableName("exitEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("exitOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testExitListenerByScript.cmmn"})
  @Test
  void testExitListenerByScript() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    exit(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("exit").singleResult().getValue()).isTrue();
    assertThat(query.variableName("exitEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("exitOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testParentTerminateListenerByClass.cmmn"})
  @Test
  void testParentTerminateListenerByClass() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String milestoneId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Milestone_1")
        .singleResult()
        .getId();

    // when
    parentTerminate(milestoneId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("parentTerminate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("parentTerminateEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("parentTerminateOnCaseExecutionId").singleResult().getValue()).isEqualTo(milestoneId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testParentTerminateListenerByDelegateExpression.cmmn"})
  @Test
  void testParentTerminateListenerByDelegateExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MySpecialCaseExecutionListener())
        .create()
        .getId();

    String milestoneId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Milestone_1")
        .singleResult()
        .getId();

    // when
    parentTerminate(milestoneId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("parentTerminate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("parentTerminateEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("parentTerminateOnCaseExecutionId").singleResult().getValue()).isEqualTo(milestoneId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testParentTerminateListenerByExpression.cmmn"})
  @Test
  void testParentTerminateListenerByExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MyCaseExecutionListener())
        .create()
        .getId();

    String milestoneId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Milestone_1")
        .singleResult()
        .getId();

    // when
    parentTerminate(milestoneId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("parentTerminate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("parentTerminateEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("parentTerminateOnCaseExecutionId").singleResult().getValue()).isEqualTo(milestoneId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testParentTerminateListenerByScript.cmmn"})
  @Test
  void testParentTerminateListenerByScript() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String milestoneId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Milestone_1")
        .singleResult()
        .getId();

    // when
    parentTerminate(milestoneId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("parentTerminate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("parentTerminateEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("parentTerminateOnCaseExecutionId").singleResult().getValue()).isEqualTo(milestoneId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testSuspendListenerByClass.cmmn"})
  @Test
  void testSuspendListenerByClass() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    suspend(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("suspend").singleResult().getValue()).isTrue();
    assertThat(query.variableName("suspendEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("suspendOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testSuspendListenerByDelegateExpression.cmmn"})
  @Test
  void testSuspendListenerByDelegateExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MySpecialCaseExecutionListener())
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    suspend(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("suspend").singleResult().getValue()).isTrue();
    assertThat(query.variableName("suspendEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("suspendOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testSuspendListenerByExpression.cmmn"})
  @Test
  void testSuspendListenerByExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MyCaseExecutionListener())
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    suspend(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("suspend").singleResult().getValue()).isTrue();
    assertThat(query.variableName("suspendEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("suspendOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testSuspendListenerByScript.cmmn"})
  @Test
  void testSuspendListenerByScript() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    suspend(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("suspend").singleResult().getValue()).isTrue();
    assertThat(query.variableName("suspendEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("suspendOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testParentSuspendListenerByClass.cmmn"})
  @Test
  void testParentSuspendListenerByClass() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    parentSuspend(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("parentSuspend").singleResult().getValue()).isTrue();
    assertThat(query.variableName("parentSuspendEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("parentSuspendOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testParentSuspendListenerByDelegateExpression.cmmn"})
  @Test
  void testParentSuspendListenerByDelegateExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MySpecialCaseExecutionListener())
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    parentSuspend(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("parentSuspend").singleResult().getValue()).isTrue();
    assertThat(query.variableName("parentSuspendEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("parentSuspendOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testParentSuspendListenerByExpression.cmmn"})
  @Test
  void testParentSuspendListenerByExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MyCaseExecutionListener())
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    parentSuspend(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("parentSuspend").singleResult().getValue()).isTrue();
    assertThat(query.variableName("parentSuspendEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("parentSuspendOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testParentSuspendListenerByScript.cmmn"})
  @Test
  void testParentSuspendListenerByScript() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    parentSuspend(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("parentSuspend").singleResult().getValue()).isTrue();
    assertThat(query.variableName("parentSuspendEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("parentSuspendOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testResumeListenerByClass.cmmn"})
  @Test
  void testResumeListenerByClass() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    suspend(humanTaskId);

    // when
    resume(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("resume").singleResult().getValue()).isTrue();
    assertThat(query.variableName("resumeEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("resumeOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testResumeListenerByDelegateExpression.cmmn"})
  @Test
  void testResumeListenerByDelegateExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MySpecialCaseExecutionListener())
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    suspend(humanTaskId);

    // when
    resume(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("resume").singleResult().getValue()).isTrue();
    assertThat(query.variableName("resumeEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("resumeOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testResumeListenerByExpression.cmmn"})
  @Test
  void testResumeListenerByExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MyCaseExecutionListener())
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    suspend(humanTaskId);

    // when
    resume(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("resume").singleResult().getValue()).isTrue();
    assertThat(query.variableName("resumeEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("resumeOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testResumeListenerByScript.cmmn"})
  @Test
  void testResumeListenerByScript() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    suspend(humanTaskId);

    // when
    resume(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("resume").singleResult().getValue()).isTrue();
    assertThat(query.variableName("resumeEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("resumeOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testParentResumeListenerByClass.cmmn"})
  @Test
  void testParentResumeListenerByClass() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    suspend(humanTaskId);

    // when
    parentResume(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("parentResume").singleResult().getValue()).isTrue();
    assertThat(query.variableName("parentResumeEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("parentResumeOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testParentResumeListenerByDelegateExpression.cmmn"})
  @Test
  void testParentResumeListenerByDelegateExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MySpecialCaseExecutionListener())
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    suspend(humanTaskId);

    // when
    parentResume(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("parentResume").singleResult().getValue()).isTrue();
    assertThat(query.variableName("parentResumeEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("parentResumeOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testParentResumeListenerByExpression.cmmn"})
  @Test
  void testParentResumeListenerByExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MyCaseExecutionListener())
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    suspend(humanTaskId);

    // when
    parentResume(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("parentResume").singleResult().getValue()).isTrue();
    assertThat(query.variableName("parentResumeEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("parentResumeOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testParentResumeListenerByScript.cmmn"})
  @Test
  void testParentResumeListenerByScript() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    suspend(humanTaskId);

    // when
    parentResume(humanTaskId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("parentResume").singleResult().getValue()).isTrue();
    assertThat(query.variableName("parentResumeEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("parentResumeOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testOccurListenerByClass.cmmn"})
  @Test
  void testOccurListenerByClass() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String milestoneId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Milestone_1")
        .singleResult()
        .getId();

    // when
    occur(milestoneId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("occur").singleResult().getValue()).isTrue();
    assertThat(query.variableName("occurEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("occurOnCaseExecutionId").singleResult().getValue()).isEqualTo(milestoneId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testOccurListenerByDelegateExpression.cmmn"})
  @Test
  void testOccurListenerByDelegateExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MySpecialCaseExecutionListener())
        .create()
        .getId();

    String milestoneId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Milestone_1")
        .singleResult()
        .getId();

    // when
    occur(milestoneId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("occur").singleResult().getValue()).isTrue();
    assertThat(query.variableName("occurEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("occurOnCaseExecutionId").singleResult().getValue()).isEqualTo(milestoneId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testOccurListenerByExpression.cmmn"})
  @Test
  void testOccurListenerByExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MyCaseExecutionListener())
        .create()
        .getId();

    String milestoneId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Milestone_1")
        .singleResult()
        .getId();

    // when
    occur(milestoneId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("occur").singleResult().getValue()).isTrue();
    assertThat(query.variableName("occurEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("occurOnCaseExecutionId").singleResult().getValue()).isEqualTo(milestoneId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testOccurListenerByScript.cmmn"})
  @Test
  void testOccurListenerByScript() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String milestoneId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Milestone_1")
        .singleResult()
        .getId();

    // when
    occur(milestoneId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("occur").singleResult().getValue()).isTrue();
    assertThat(query.variableName("occurEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("occurOnCaseExecutionId").singleResult().getValue()).isEqualTo(milestoneId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

  }

  @ParameterizedTest
  @CsvSource({
    "org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testAllListenerByClass.cmmn",
    "org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testAllListenerByDelegateExpression.cmmn",
    "org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testAllListenerByExpression.cmmn",
    "org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testAllListenerByScript.cmmn"
  })
  void testAllListener(String resource) {
    processEngineRule.manageDeployment(
      repositoryService.createDeployment()
        .addClasspathResource(resource)
        .deploy()
    );

    // given
    String caseInstanceId = caseService
      .withCaseDefinitionByKey("case")
      .setVariable("myListener", new MySpecialCaseExecutionListener())
      .create()
      .getId();

    String humanTaskId = caseService
      .createCaseExecutionQuery()
      .activityId("PI_HumanTask_1")
      .singleResult()
      .getId();

    // when

    caseService
      .withCaseExecution(humanTaskId)
      .disable();

    caseService
      .withCaseExecution(humanTaskId)
      .reenable();

    caseService
      .withCaseExecution(humanTaskId)
      .manualStart();

    suspend(humanTaskId);

    resume(humanTaskId);

    caseService
      .withCaseExecution(humanTaskId)
      .complete();

    // then
    VariableInstanceQuery query = runtimeService
      .createVariableInstanceQuery()
      .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(26);

    Stream.of("create", "enable", "disable", "reenable", "manualStart", "suspend", "resume", "complete")
      .forEach(event -> {
        assertThat((Boolean) query.variableName(event).singleResult().getValue()).isTrue();
        assertThat(query.variableName(event + "EventCounter").singleResult().getValue()).isEqualTo(1);
        assertThat(query.variableName(event + "OnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);
      });

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(8);
  }

  @ParameterizedTest
  @CsvSource({
    "org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testFieldInjectionByClass.cmmn",
    "org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testFieldInjectionByDelegateExpression.cmmn"
  })
  void testFieldInjection(String resource) {
    // given
    processEngineRule.manageDeployment(
      repositoryService.createDeployment()
        .addClasspathResource(resource)
        .deploy()
    );

    // when
    String caseInstanceId = caseService
      .withCaseDefinitionByKey("case")
      .setVariable("myListener", new FieldInjectionCaseExecutionListener())
      .create()
      .getId();

    // then
    VariableInstanceQuery query = runtimeService
      .createVariableInstanceQuery()
      .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat(query.variableName("greeting").singleResult().getValue()).isEqualTo("Hello from The Case");
    assertThat(query.variableName("helloWorld").singleResult().getValue()).isEqualTo("Hello World");
    assertThat(query.variableName("prefix").singleResult().getValue()).isEqualTo("ope");
    assertThat(query.variableName("suffix").singleResult().getValue()).isEqualTo("rato");

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testListenerByScriptResource.cmmn",
      "org/operaton/bpm/engine/test/cmmn/listener/caseExecutionListener.groovy"
  })
  @Test
  void testListenerByScriptResource() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    caseService
      .withCaseExecution(humanTaskId)
      .disable();

    caseService
      .withCaseExecution(humanTaskId)
      .reenable();

    caseService
      .withCaseExecution(humanTaskId)
      .manualStart();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(10);

    assertThat((Boolean) query.variableName("disable").singleResult().getValue()).isTrue();
    assertThat(query.variableName("disableEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("disableOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat((Boolean) query.variableName("reenable").singleResult().getValue()).isTrue();
    assertThat(query.variableName("reenableEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("reenableOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat((Boolean) query.variableName("manualStart").singleResult().getValue()).isTrue();
    assertThat(query.variableName("manualStartEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("manualStartOnCaseExecutionId").singleResult().getValue()).isEqualTo(humanTaskId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(3);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testDoesNotImplementCaseExecutionListenerInterfaceByClass.cmmn"})
  @Test
  void testDoesNotImplementCaseExecutionListenerInterfaceByClass() {
    // given


    try {
      // when
      caseService
        .withCaseDefinitionByKey("case")
        .create();
    } catch (Exception e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent("ENGINE-05016 Class 'org.operaton.bpm.engine.test.cmmn.listener.NotCaseExecutionListener' doesn't implement '%s'".formatted(CaseExecutionListener.class.getName()), message);
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testDoesNotImplementCaseExecutionListenerInterfaceByDelegateExpression.cmmn"})
  @Test
  void testDoesNotImplementCaseExecutionListenerInterfaceByDelegateExpression() {
    // given

    try {
      // when
      caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new NotCaseExecutionListener())
        .create();
    } catch (Exception e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent("Delegate expression ${myListener} did not resolve to an implementation of interface "+CaseExecutionListener.class.getName(), message);
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testListenerDoesNotExist.cmmn"})
  @Test
  void testListenerDoesNotExist() {
    // given

    try {
      // when
      caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();
    } catch (Exception e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent("Exception while instantiating class 'org.operaton.bpm.engine.test.cmmn.listener.NotExistingCaseExecutionListener'", message);
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseExecutionListenerTest.testBusinessKeyAsCaseBusinessKey.cmmn"})
  @Test
  void testBusinessKeyAsCaseBusinessKey() {
    // given

    // when
    caseService.withCaseDefinitionByKey("case")
      .businessKey("myBusinessKey")
      .create()
      .getId();

    // then
    VariableInstance v1 = runtimeService.createVariableInstanceQuery().variableName("businessKey").singleResult();
    VariableInstance v2 = runtimeService.createVariableInstanceQuery().variableName("caseBusinessKey").singleResult();
    assertThat(v1).isNotNull();
    assertThat(v2).isNotNull();
    assertThat(v1.getValue()).isEqualTo("myBusinessKey");
    assertThat(v2.getValue()).isEqualTo(v1.getValue());
  }

}
