/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.cmmn.listener;

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.delegate.CaseExecutionListener;
import org.operaton.bpm.engine.runtime.VariableInstanceQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;
import org.junit.Test;

/**
 * @author Roman Smirnov
 *
 */
public class CaseInstanceListenerTest extends CmmnTest {

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testCreateListenerByClass.cmmn"})
  @Test
  public void testCreateListenerByClass() {
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
  public void testCreateListenerByDelegateExpression() {
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
  public void testCreateListenerByExpression() {
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
  public void testCreateListenerByScript() {
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
  public void testCompleteListenerByClass() {
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
  public void testCompleteListenerByDelegateExpression() {
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
  public void testCompleteListenerByExpression() {
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
  public void testCompleteListenerByScript() {
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
  public void testTerminateListenerByClass() {
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
  public void testTerminateListenerByDelegateExpression() {
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
  public void testTerminateListenerByExpression() {
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
  public void testTerminateListenerByScript() {
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
  public void testSuspendListenerByClass() {
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
  public void testSuspendListenerByDelegateExpression() {
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
  public void testSuspendListenerByExpression() {
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
  public void testSuspendListenerByScript() {
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
  public void testReActivateListenerByClass() {
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
  public void testReActivateListenerByDelegateExpression() {
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
  public void testReActivateListenerByExpression() {
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
  public void testReActivateListenerByScript() {
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
  public void testCloseListenerByClass() {
    CloseCaseExecutionListener.clear();

    assertThat(CloseCaseExecutionListener.EVENT).isNull();
    assertThat(CloseCaseExecutionListener.COUNTER).isEqualTo(0);
    assertThat(CloseCaseExecutionListener.ON_CASE_EXECUTION_ID).isNull();

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
    assertThat(CloseCaseExecutionListener.EVENT).isEqualTo("close");
    assertThat(CloseCaseExecutionListener.COUNTER).isEqualTo(1);
    assertThat(CloseCaseExecutionListener.ON_CASE_EXECUTION_ID).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testCloseListenerByDelegateExpression.cmmn"})
  @Test
  public void testCloseListenerByDelegateExpression() {
    CloseCaseExecutionListener.clear();

    assertThat(CloseCaseExecutionListener.EVENT).isNull();
    assertThat(CloseCaseExecutionListener.COUNTER).isEqualTo(0);
    assertThat(CloseCaseExecutionListener.ON_CASE_EXECUTION_ID).isNull();

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
    assertThat(CloseCaseExecutionListener.EVENT).isEqualTo("close");
    assertThat(CloseCaseExecutionListener.COUNTER).isEqualTo(1);
    assertThat(CloseCaseExecutionListener.ON_CASE_EXECUTION_ID).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testCloseListenerByExpression.cmmn"})
  @Test
  public void testCloseListenerByExpression() {
    CloseCaseExecutionListener.clear();

    assertThat(CloseCaseExecutionListener.EVENT).isNull();
    assertThat(CloseCaseExecutionListener.COUNTER).isEqualTo(0);
    assertThat(CloseCaseExecutionListener.ON_CASE_EXECUTION_ID).isNull();

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
    assertThat(CloseCaseExecutionListener.EVENT).isEqualTo("close");
    assertThat(CloseCaseExecutionListener.COUNTER).isEqualTo(1);
    assertThat(CloseCaseExecutionListener.ON_CASE_EXECUTION_ID).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testCloseListenerByScript.cmmn"})
  @Test
  public void testCloseListenerByScript() {
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
    // TODO: if history is provided, the historic variables have to be checked!

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testAllListenerByClass.cmmn"})
  @Test
  public void testAllListenerByClass() {
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
  public void testAllListenerByDelegateExpression() {
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
  public void testAllListenerByExpression() {
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
  public void testAllListenerByScript() {
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
  public void testFieldInjectionByClass() {
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
  public void testFieldInjectionByDelegateExpression() {
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
  public void testListenerByScriptResource() {
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
  public void testDoesNotImplementCaseExecutionListenerInterfaceByClass() {
    // given


    try {
      // when
      caseService
        .withCaseDefinitionByKey("case")
        .create();
    } catch (Exception e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent("ENGINE-05016 Class 'org.operaton.bpm.engine.test.cmmn.listener.NotCaseExecutionListener' doesn't implement '"+CaseExecutionListener.class.getName() + "'", message);
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testDoesNotImplementCaseExecutionListenerInterfaceByDelegateExpression.cmmn"})
  @Test
  public void testDoesNotImplementCaseExecutionListenerInterfaceByDelegateExpression() {
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

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/listener/CaseInstanceListenerTest.testListenerDoesNotExist.cmmn"})
  @Test
  public void testListenerDoesNotExist() {
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
      testRule.assertTextPresent("Exception while instantiating class", message);
    }

  }

}
