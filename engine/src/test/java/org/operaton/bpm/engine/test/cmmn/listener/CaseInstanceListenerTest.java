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

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.ScriptEvaluationException;
import org.operaton.bpm.engine.delegate.CaseExecutionListener;
import org.operaton.bpm.engine.runtime.VariableInstanceQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
class CaseInstanceListenerTest extends CmmnTest {

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testCreateListenerByClass.cmmn"})
  @Test
  void testCreateListenerByClass() {
    // given

    // when
    String caseInstanceId = caseService
      .withCaseDefinitionByKey("case")
      .create()
      .getId();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("create").singleResult().getValue()).isTrue();
    assertThat(query.variableName("createEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("createOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testCreateListenerByDelegateExpression.cmmn"})
  @Test
  void testCreateListenerByDelegateExpression() {
    // given

    // when
    String caseInstanceId = caseService
      .withCaseDefinitionByKey("case")
      .setVariable("myListener", new MySpecialCaseExecutionListener())
      .create()
      .getId();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("create").singleResult().getValue()).isTrue();
    assertThat(query.variableName("createEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("createOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testCreateListenerByExpression.cmmn"})
  @Test
  void testCreateListenerByExpression() {
    // given

    // when
    String caseInstanceId = caseService
      .withCaseDefinitionByKey("case")
      .setVariable("myListener", new MyCaseExecutionListener())
      .create()
      .getId();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("create").singleResult().getValue()).isTrue();
    assertThat(query.variableName("createEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("createOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testCreateListenerByScript.cmmn"})
  @Test
  void testCreateListenerByScript() {
    // given

    // when
    String caseInstanceId = caseService
      .withCaseDefinitionByKey("case")
      .create()
      .getId();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("create").singleResult().getValue()).isTrue();
    assertThat(query.variableName("createEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("createOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testCompleteListenerByClass.cmmn"})
  @Test
  void testCompleteListenerByClass() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    // when
    caseService
      .withCaseExecution(caseInstanceId)
      .complete();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("complete").singleResult().getValue()).isTrue();
    assertThat(query.variableName("completeEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("completeOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testCompleteListenerByDelegateExpression.cmmn"})
  @Test
  void testCompleteListenerByDelegateExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MySpecialCaseExecutionListener())
        .create()
        .getId();

    // when
    caseService
      .withCaseExecution(caseInstanceId)
      .complete();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("complete").singleResult().getValue()).isTrue();
    assertThat(query.variableName("completeEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("completeOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testCompleteListenerByExpression.cmmn"})
  @Test
  void testCompleteListenerByExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MyCaseExecutionListener())
        .create()
        .getId();

    // when
    caseService
      .withCaseExecution(caseInstanceId)
      .complete();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("complete").singleResult().getValue()).isTrue();
    assertThat(query.variableName("completeEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("completeOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testCompleteListenerByScript.cmmn"})
  @Test
  void testCompleteListenerByScript() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    // when
    caseService
      .withCaseExecution(caseInstanceId)
      .complete();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("complete").singleResult().getValue()).isTrue();
    assertThat(query.variableName("completeEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("completeOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testTerminateListenerByClass.cmmn"})
  @Test
  void testTerminateListenerByClass() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    // when
    terminate(caseInstanceId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("terminate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("terminateEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("terminateOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testTerminateListenerByDelegateExpression.cmmn"})
  @Test
  void testTerminateListenerByDelegateExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MySpecialCaseExecutionListener())
        .create()
        .getId();

    // when
    terminate(caseInstanceId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("terminate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("terminateEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("terminateOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testTerminateListenerByExpression.cmmn"})
  @Test
  void testTerminateListenerByExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MyCaseExecutionListener())
        .create()
        .getId();

    // when
    terminate(caseInstanceId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("terminate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("terminateEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("terminateOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testTerminateListenerByScript.cmmn"})
  @Test
  void testTerminateListenerByScript() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    // when
    terminate(caseInstanceId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("terminate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("terminateEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("terminateOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testSuspendListenerByClass.cmmn"})
  @Test
  void testSuspendListenerByClass() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    // when
    suspend(caseInstanceId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("suspend").singleResult().getValue()).isTrue();
    assertThat(query.variableName("suspendEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("suspendOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testSuspendListenerByDelegateExpression.cmmn"})
  @Test
  void testSuspendListenerByDelegateExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MySpecialCaseExecutionListener())
        .create()
        .getId();

    // when
    suspend(caseInstanceId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("suspend").singleResult().getValue()).isTrue();
    assertThat(query.variableName("suspendEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("suspendOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testSuspendListenerByExpression.cmmn"})
  @Test
  void testSuspendListenerByExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MyCaseExecutionListener())
        .create()
        .getId();

    // when
    suspend(caseInstanceId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("suspend").singleResult().getValue()).isTrue();
    assertThat(query.variableName("suspendEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("suspendOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testSuspendListenerByScript.cmmn"})
  @Test
  void testSuspendListenerByScript() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    // when
    suspend(caseInstanceId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("suspend").singleResult().getValue()).isTrue();
    assertThat(query.variableName("suspendEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("suspendOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testReActivateListenerByClass.cmmn"})
  @Test
  void testReActivateListenerByClass() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    caseService
      .withCaseExecution(caseInstanceId)
      .complete();

    // when
    reactivate(caseInstanceId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("reactivate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("reactivateEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("reactivateOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testReActivateListenerByDelegateExpression.cmmn"})
  @Test
  void testReActivateListenerByDelegateExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MySpecialCaseExecutionListener())
        .create()
        .getId();

    terminate(caseInstanceId);

    // when
    reactivate(caseInstanceId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("reactivate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("reactivateEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("reactivateOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testReActivateListenerByExpression.cmmn"})
  @Test
  void testReActivateListenerByExpression() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new MyCaseExecutionListener())
        .create()
        .getId();

    suspend(caseInstanceId);

    // when
    reactivate(caseInstanceId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(5);

    assertThat((Boolean) query.variableName("reactivate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("reactivateEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("reactivateOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testReActivateListenerByScript.cmmn"})
  @Test
  void testReActivateListenerByScript() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    caseService
      .withCaseExecution(caseInstanceId)
      .complete();

    // when
    reactivate(caseInstanceId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat((Boolean) query.variableName("reactivate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("reactivateEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("reactivateOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testCloseListenerByClass.cmmn"})
  @Test
  void testCloseListenerByClass() {
    CloseCaseExecutionListener.clear();

    assertThat(CloseCaseExecutionListener.event).isNull();
    assertThat(CloseCaseExecutionListener.counter).isZero();
    assertThat(CloseCaseExecutionListener.onCaseExecutionId).isNull();

    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    caseService
      .withCaseExecution(caseInstanceId)
      .complete();

    // when
    caseService
      .withCaseExecution(caseInstanceId)
      .close();

    // then
    assertThat(CloseCaseExecutionListener.event).isEqualTo("close");
    assertThat(CloseCaseExecutionListener.counter).isEqualTo(1);
    assertThat(CloseCaseExecutionListener.onCaseExecutionId).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testCloseListenerByDelegateExpression.cmmn"})
  @Test
  void testCloseListenerByDelegateExpression() {
    CloseCaseExecutionListener.clear();

    assertThat(CloseCaseExecutionListener.event).isNull();
    assertThat(CloseCaseExecutionListener.counter).isZero();
    assertThat(CloseCaseExecutionListener.onCaseExecutionId).isNull();

    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new CloseCaseExecutionListener())
        .create()
        .getId();

    terminate(caseInstanceId);

    // when
    caseService
      .withCaseExecution(caseInstanceId)
      .close();

    // then
    assertThat(CloseCaseExecutionListener.event).isEqualTo("close");
    assertThat(CloseCaseExecutionListener.counter).isEqualTo(1);
    assertThat(CloseCaseExecutionListener.onCaseExecutionId).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testCloseListenerByExpression.cmmn"})
  @Test
  void testCloseListenerByExpression() {
    CloseCaseExecutionListener.clear();

    assertThat(CloseCaseExecutionListener.event).isNull();
    assertThat(CloseCaseExecutionListener.counter).isZero();
    assertThat(CloseCaseExecutionListener.onCaseExecutionId).isNull();

    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new CloseCaseExecutionListener())
        .create()
        .getId();

    suspend(caseInstanceId);

    // when
    caseService
      .withCaseExecution(caseInstanceId)
      .close();

    // then
    assertThat(CloseCaseExecutionListener.event).isEqualTo("close");
    assertThat(CloseCaseExecutionListener.counter).isEqualTo(1);
    assertThat(CloseCaseExecutionListener.onCaseExecutionId).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testCloseListenerByScript.cmmn"})
  @Test
  void testCloseListenerByScript() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    var caseExecutionCommandBuilder = caseService
        .withCaseExecution(caseInstanceId);

    caseExecutionCommandBuilder.complete();

    // when & then
    assertThatThrownBy(caseExecutionCommandBuilder::close)
        .isInstanceOf(ScriptEvaluationException.class)
        .hasRootCauseInstanceOf(ProcessEngineException.class)
        .hasRootCauseMessage("Intentional exception by close listener");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testAllListenerByClass.cmmn"})
  @Test
  void testAllListenerByClass() {
    // given

    // when
    String caseInstanceId = caseService
      .withCaseDefinitionByKey("case")
      .create()
      .getId();

    caseService
      .withCaseExecution(caseInstanceId)
      .complete();

    reactivate(caseInstanceId);

    terminate(caseInstanceId);

    reactivate(caseInstanceId);

    suspend(caseInstanceId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(16);

    assertThat((Boolean) query.variableName("create").singleResult().getValue()).isTrue();
    assertThat(query.variableName("createEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("createOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

    assertThat((Boolean) query.variableName("complete").singleResult().getValue()).isTrue();
    assertThat(query.variableName("completeEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("completeOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

    assertThat((Boolean) query.variableName("terminate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("terminateEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("terminateOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

    assertThat((Boolean) query.variableName("suspend").singleResult().getValue()).isTrue();
    assertThat(query.variableName("suspendEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("suspendOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

    assertThat((Boolean) query.variableName("reactivate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("reactivateEventCounter").singleResult().getValue()).isEqualTo(2);
    assertThat(query.variableName("reactivateOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(6);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testAllListenerByDelegateExpression.cmmn"})
  @Test
  void testAllListenerByDelegateExpression() {
    // given

    // when
    String caseInstanceId = caseService
      .withCaseDefinitionByKey("case")
      .setVariable("myListener", new MySpecialCaseExecutionListener())
      .create()
      .getId();

    caseService
      .withCaseExecution(caseInstanceId)
      .complete();

    reactivate(caseInstanceId);

    terminate(caseInstanceId);

    reactivate(caseInstanceId);

    suspend(caseInstanceId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(17);

    assertThat((Boolean) query.variableName("create").singleResult().getValue()).isTrue();
    assertThat(query.variableName("createEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("createOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

    assertThat((Boolean) query.variableName("complete").singleResult().getValue()).isTrue();
    assertThat(query.variableName("completeEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("completeOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

    assertThat((Boolean) query.variableName("terminate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("terminateEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("terminateOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

    assertThat((Boolean) query.variableName("suspend").singleResult().getValue()).isTrue();
    assertThat(query.variableName("suspendEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("suspendOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

    assertThat((Boolean) query.variableName("reactivate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("reactivateEventCounter").singleResult().getValue()).isEqualTo(2);
    assertThat(query.variableName("reactivateOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(6);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testAllListenerByExpression.cmmn"})
  @Test
  void testAllListenerByExpression() {
    // given

    // when
    String caseInstanceId = caseService
      .withCaseDefinitionByKey("case")
      .setVariable("myListener", new MyCaseExecutionListener())
      .create()
      .getId();

    caseService
      .withCaseExecution(caseInstanceId)
      .complete();

    reactivate(caseInstanceId);

    terminate(caseInstanceId);

    reactivate(caseInstanceId);

    suspend(caseInstanceId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(17);

    assertThat((Boolean) query.variableName("create").singleResult().getValue()).isTrue();
    assertThat(query.variableName("createEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("createOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

    assertThat((Boolean) query.variableName("complete").singleResult().getValue()).isTrue();
    assertThat(query.variableName("completeEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("completeOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

    assertThat((Boolean) query.variableName("terminate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("terminateEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("terminateOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

    assertThat((Boolean) query.variableName("suspend").singleResult().getValue()).isTrue();
    assertThat(query.variableName("suspendEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("suspendOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

    assertThat((Boolean) query.variableName("reactivate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("reactivateEventCounter").singleResult().getValue()).isEqualTo(2);
    assertThat(query.variableName("reactivateOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(6);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testAllListenerByScript.cmmn"})
  @Test
  void testAllListenerByScript() {
    // given

    // when
    String caseInstanceId = caseService
      .withCaseDefinitionByKey("case")
      .create()
      .getId();

    caseService
      .withCaseExecution(caseInstanceId)
      .complete();

    reactivate(caseInstanceId);

    terminate(caseInstanceId);

    reactivate(caseInstanceId);

    suspend(caseInstanceId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(16);

    assertThat((Boolean) query.variableName("create").singleResult().getValue()).isTrue();
    assertThat(query.variableName("createEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("createOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

    assertThat((Boolean) query.variableName("complete").singleResult().getValue()).isTrue();
    assertThat(query.variableName("completeEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("completeOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

    assertThat((Boolean) query.variableName("terminate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("terminateEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("terminateOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

    assertThat((Boolean) query.variableName("suspend").singleResult().getValue()).isTrue();
    assertThat(query.variableName("suspendEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("suspendOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

    assertThat((Boolean) query.variableName("reactivate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("reactivateEventCounter").singleResult().getValue()).isEqualTo(2);
    assertThat(query.variableName("reactivateOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(6);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testFieldInjectionByClass.cmmn"})
  @Test
  void testFieldInjectionByClass() {
    // given

    // when
    String caseInstanceId = caseService
      .withCaseDefinitionByKey("case")
      .create()
      .getId();

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(4);

    assertThat(query.variableName("greeting").singleResult().getValue()).isEqualTo("Hello from The Case");
    assertThat(query.variableName("helloWorld").singleResult().getValue()).isEqualTo("Hello World");
    assertThat(query.variableName("prefix").singleResult().getValue()).isEqualTo("ope");
    assertThat(query.variableName("suffix").singleResult().getValue()).isEqualTo("rato");

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testFieldInjectionByDelegateExpression.cmmn"})
  @Test
  void testFieldInjectionByDelegateExpression() {
    // given

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
      "org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testListenerByScriptResource.cmmn",
      "org/operaton/bpm/engine/test/cmmn/listener/caseExecutionListener.groovy"
  })
  @Test
  void testListenerByScriptResource() {
    // given

    // when
    String caseInstanceId = caseService
      .withCaseDefinitionByKey("case")
      .create()
      .getId();

    caseService
      .withCaseExecution(caseInstanceId)
      .complete();

    reactivate(caseInstanceId);

    terminate(caseInstanceId);

    // then
    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.count()).isEqualTo(10);

    assertThat((Boolean) query.variableName("create").singleResult().getValue()).isTrue();
    assertThat(query.variableName("createEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("createOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

    assertThat((Boolean) query.variableName("complete").singleResult().getValue()).isTrue();
    assertThat(query.variableName("completeEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("completeOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

    assertThat((Boolean) query.variableName("terminate").singleResult().getValue()).isTrue();
    assertThat(query.variableName("terminateEventCounter").singleResult().getValue()).isEqualTo(1);
    assertThat(query.variableName("terminateOnCaseExecutionId").singleResult().getValue()).isEqualTo(caseInstanceId);

    assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(3);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testDoesNotImplementCaseExecutionListenerInterfaceByClass.cmmn"})
  @Test
  void testDoesNotImplementCaseExecutionListenerInterfaceByClass() {
    // given
    var caseInstanceBuilder = caseService
        .withCaseDefinitionByKey("case");

    // when/then
    assertThatThrownBy(caseInstanceBuilder::create)
      .isInstanceOf(Exception.class)
      .hasMessageContaining("ENGINE-05016 Class 'org.operaton.bpm.engine.test.cmmn.listener.NotCaseExecutionListener' doesn't implement '"+CaseExecutionListener.class.getName() + "'");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testDoesNotImplementCaseExecutionListenerInterfaceByDelegateExpression.cmmn"})
  @Test
  void testDoesNotImplementCaseExecutionListenerInterfaceByDelegateExpression() {
    // given
    var caseInstanceBuilder = caseService
        .withCaseDefinitionByKey("case")
        .setVariable("myListener", new NotCaseExecutionListener());

    // when/then
    assertThatThrownBy(caseInstanceBuilder::create)
      .isInstanceOf(Exception.class)
      .hasMessageContaining("Delegate expression ${myListener} did not resolve to an implementation of interface "+CaseExecutionListener.class.getName());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testListenerDoesNotExist.cmmn"})
  @Test
  void testListenerDoesNotExist() {
    // given
    var caseInstanceBuilder = caseService
        .withCaseDefinitionByKey("case");

    // when/then
    assertThatThrownBy(() -> caseInstanceBuilder.create().getId())
      .isInstanceOf(Exception.class)
      .hasMessageContaining("Exception while instantiating class");
  }

}
