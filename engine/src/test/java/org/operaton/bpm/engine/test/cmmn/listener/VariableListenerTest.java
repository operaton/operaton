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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.delegate.CaseVariableListener;
import org.operaton.bpm.engine.delegate.DelegateCaseVariableInstance;
import org.operaton.bpm.engine.delegate.VariableListener;
import org.operaton.bpm.engine.history.HistoricDetail;
import org.operaton.bpm.engine.history.HistoricVariableUpdate;
import org.operaton.bpm.engine.impl.context.CaseExecutionContext;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Thorben Lindhauer
 *
 */
class VariableListenerTest extends CmmnTest {

  protected Map<Object, Object> beans;

  @BeforeEach
  void setUp() {
    LogVariableListener.reset();
    beans = processEngineConfiguration.getBeans();
  }

  @Deployment
  @Test
  void testAnyEventListenerByClass() {
    CaseInstance caseInstance = caseService
      .withCaseDefinitionByKey("case")
      .create();

    CaseExecution taskExecution =
        caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();

    // when i set a variable on a higher scope
    caseService.withCaseExecution(caseInstance.getId()).setVariable("anInstanceVariable", "anInstanceValue").execute();

    // then the listener is not invoked
    assertThat(LogVariableListener.getInvocations()).isEmpty();

    // when i set a variable on the human task (ie the source execution matters although the variable ends up in the same place)
    caseService.withCaseExecution(taskExecution.getId()).setVariableLocal("aTaskVariable", "aTaskValue").execute();

    // then the listener is invoked
    assertThat(LogVariableListener.getInvocations()).hasSize(1);

    DelegateVariableInstanceSpec
      .fromCaseExecution(taskExecution)
      .event(VariableListener.CREATE)
      .name("aTaskVariable")
      .value("aTaskValue")
      .matches(LogVariableListener.getInvocations().get(0));

    LogVariableListener.reset();

    // when i update the variable on the human task
    caseService.withCaseExecution(taskExecution.getId()).setVariable("aTaskVariable", "aNewTaskValue").execute();

    // then the listener is invoked
    assertThat(LogVariableListener.getInvocations()).hasSize(1);
    DelegateVariableInstanceSpec
      .fromCaseExecution(taskExecution)
      .event(VariableListener.UPDATE)
      .name("aTaskVariable")
      .value("aNewTaskValue")
      .activityInstanceId(taskExecution.getId())
      .matches(LogVariableListener.getInvocations().get(0));
    LogVariableListener.reset();

    // when i remove the variable from the human task
    caseService.withCaseExecution(taskExecution.getId()).removeVariable("aTaskVariable").execute();

    // then the listener is invoked
    assertThat(LogVariableListener.getInvocations()).hasSize(1);
    DelegateVariableInstanceSpec
      .fromCaseExecution(taskExecution)
      .event(VariableListener.DELETE)
      .name("aTaskVariable")
      .value(null)
      .activityInstanceId(taskExecution.getId())
      .matches(LogVariableListener.getInvocations().get(0));

    LogVariableListener.reset();
  }

  @Deployment
  @Test
  void testCreateEventListenerByClass() {
    caseService
      .withCaseDefinitionByKey("case")
      .create();

    CaseExecution taskExecution =
        caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();

    // when i create a variable on the human task
    caseService.withCaseExecution(taskExecution.getId()).setVariableLocal("aTaskVariable", "aTaskValue").execute();

    // then the listener is invoked
    assertThat(LogVariableListener.getInvocations()).hasSize(1);

    DelegateVariableInstanceSpec
      .fromCaseExecution(taskExecution)
      .event(VariableListener.CREATE)
      .name("aTaskVariable")
      .value("aTaskValue")
      .matches(LogVariableListener.getInvocations().get(0));

    LogVariableListener.reset();

    // when i update the variable on the human task
    caseService.withCaseExecution(taskExecution.getId()).setVariable("aTaskVariable", "aNewTaskValue").execute();

    // then the listener is not invoked
    assertThat(LogVariableListener.getInvocations()).isEmpty();

    // when i remove the variable from the human task
    caseService.withCaseExecution(taskExecution.getId()).removeVariable("aTaskVariable").execute();

    // then the listener is not invoked
    assertThat(LogVariableListener.getInvocations()).isEmpty();
  }

  @Deployment
  @Test
  void testUpdateEventListenerByClass() {
    caseService
      .withCaseDefinitionByKey("case")
      .create();

    CaseExecution taskExecution =
        caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();

    // when i create a variable on the human task
    caseService.withCaseExecution(taskExecution.getId()).setVariableLocal("aTaskVariable", "aTaskValue").execute();

    // then the listener is not invoked
    assertThat(LogVariableListener.getInvocations()).isEmpty();

    // when i update the variable on the human task
    caseService.withCaseExecution(taskExecution.getId()).setVariable("aTaskVariable", "aNewTaskValue").execute();

    // then the listener is invoked
    assertThat(LogVariableListener.getInvocations()).hasSize(1);

    DelegateVariableInstanceSpec
      .fromCaseExecution(taskExecution)
      .event(VariableListener.UPDATE)
      .name("aTaskVariable")
      .value("aNewTaskValue")
      .activityInstanceId(taskExecution.getId())
      .matches(LogVariableListener.getInvocations().get(0));

    LogVariableListener.reset();


    // when i remove the variable from the human task
    caseService.withCaseExecution(taskExecution.getId()).removeVariable("aTaskVariable").execute();

    // then the listener is not invoked
    assertThat(LogVariableListener.getInvocations()).isEmpty();
  }


  @Deployment
  @Test
  void testVariableListenerInvokedFromSourceScope() {
    CaseInstance caseInstance = caseService
      .withCaseDefinitionByKey("case")
      .create();

    CaseExecution taskExecution =
        caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();

    // when i create a variable on the case instance
    caseService.withCaseExecution(caseInstance.getId()).setVariable("aTaskVariable", "aTaskValue").execute();

    // then the listener is not invoked
    assertThat(LogVariableListener.getInvocations()).isEmpty();

    // when i update the variable from the task execution
    caseService.withCaseExecution(taskExecution.getId()).setVariable("aTaskVariable", "aTaskValue").execute();

    // then the listener is invoked
    assertThat(LogVariableListener.getInvocations()).hasSize(1);

    DelegateVariableInstanceSpec
      .fromCaseExecution(caseInstance)
      .sourceExecution(taskExecution)
      .event(VariableListener.UPDATE)
      .name("aTaskVariable")
      .value("aTaskValue")
      .activityInstanceId(caseInstance.getId())
      .matches(LogVariableListener.getInvocations().get(0));

    LogVariableListener.reset();
  }

  @Deployment
  @Test
  void testDeleteEventListenerByClass() {
    caseService
      .withCaseDefinitionByKey("case")
      .create();

    CaseExecution taskExecution =
        caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();

    // when i create a variable on the human task
    caseService.withCaseExecution(taskExecution.getId()).setVariableLocal("aTaskVariable", "aTaskValue").execute();

    // then the listener is not invoked
    assertThat(LogVariableListener.getInvocations()).isEmpty();

    // when i update the variable on the human task
    caseService.withCaseExecution(taskExecution.getId()).setVariable("aTaskVariable", "aNewTaskValue").execute();

    // then the listener is not invoked
    assertThat(LogVariableListener.getInvocations()).isEmpty();

    // when i remove the variable from the human task
    caseService.withCaseExecution(taskExecution.getId()).removeVariable("aTaskVariable").execute();

    // then the listener is invoked
    assertThat(LogVariableListener.getInvocations()).hasSize(1);

    DelegateVariableInstanceSpec
      .fromCaseExecution(taskExecution)
      .event(VariableListener.DELETE)
      .name("aTaskVariable")
      .value(null)
      .activityInstanceId(taskExecution.getId())
      .matches(LogVariableListener.getInvocations().get(0));

    LogVariableListener.reset();
  }

  @Deployment
  @Test
  void testVariableListenerByDelegateExpression() {
    beans.put("listener", new LogVariableListener());

    caseService
      .withCaseDefinitionByKey("case")
      .create();

    CaseExecution taskExecution =
        caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();

    // when i create a variable on the human task
    caseService.withCaseExecution(taskExecution.getId()).setVariableLocal("aTaskVariable", "aTaskValue").execute();

    // then the listener is invoked
    assertThat(LogVariableListener.getInvocations()).hasSize(1);

    DelegateVariableInstanceSpec
      .fromCaseExecution(taskExecution)
      .event(VariableListener.CREATE)
      .name("aTaskVariable")
      .value("aTaskValue")
      .matches(LogVariableListener.getInvocations().get(0));

    LogVariableListener.reset();
  }

  @Deployment
  @Test
  void testVariableListenerByExpression() {
    SimpleBean simpleBean = new SimpleBean();
    beans.put("bean", simpleBean);

    caseService
      .withCaseDefinitionByKey("case")
      .create();

    CaseExecution taskExecution =
        caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();

    // when i create a variable on the human task
    caseService.withCaseExecution(taskExecution.getId()).setVariableLocal("aTaskVariable", "aTaskValue").execute();

    // then the listener is invoked
    assertThat(simpleBean.wasInvoked()).isTrue();
  }

  @Deployment
  @Test
  void testVariableListenerByScript() {
    caseService
      .withCaseDefinitionByKey("case")
      .create();

    CaseExecution taskExecution =
        caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();

    // when i create a variable on the human task
    caseService.withCaseExecution(taskExecution.getId()).setVariableLocal("aTaskVariable", "aTaskValue").execute();

    // then the listener is invoked
    assertThat(SimpleBean.wasStaticallyInvoked()).isTrue();

    SimpleBean.reset();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/listener/VariableListenerTest.testListenerOnParentScope.cmmn")
  @Test
  void testListenerSourceExecution() {
    CaseInstance caseInstance = caseService
      .withCaseDefinitionByKey("case")
      .create();

    CaseExecution taskExecution =
        caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();

    // when i set a variable on a deeper scope execution but actually on the parent
    caseService.withCaseExecution(taskExecution.getId()).setVariable("aTaskVariable", "aTaskValue").execute();

    // then the listener is invoked
    assertThat(LogVariableListener.getInvocations()).hasSize(1);

    // and the source execution is the execution the variable was set on
    DelegateVariableInstanceSpec
      .fromCaseExecution(caseInstance)
      .sourceExecution(taskExecution)
      .event(VariableListener.CREATE)
      .name("aTaskVariable")
      .value("aTaskValue")
      .matches(LogVariableListener.getInvocations().get(0));

    LogVariableListener.reset();
  }

  @Deployment
  @Test
  void testListenerOnParentScope() {
    caseService
      .withCaseDefinitionByKey("case")
      .create();

    CaseExecution taskExecution =
        caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();

    // when i set a variable on a deeper scope
    caseService.withCaseExecution(taskExecution.getId()).setVariableLocal("aTaskVariable", "aTaskValue").execute();

    // then the listener is invoked
    assertThat(LogVariableListener.getInvocations()).hasSize(1);

    DelegateVariableInstanceSpec
      .fromCaseExecution(taskExecution)
      .event(VariableListener.CREATE)
      .name("aTaskVariable")
      .value("aTaskValue")
      .matches(LogVariableListener.getInvocations().get(0));

    LogVariableListener.reset();
  }

  @Deployment
  @Test
  void testChildListenersNotInvoked() {
    CaseInstance caseInstance = caseService
      .withCaseDefinitionByKey("case")
      .create();

    // when i set a variable on the parent scope
    caseService.withCaseExecution(caseInstance.getId()).setVariableLocal("aTaskVariable", "aTaskValue").execute();

    // then the listener is not invoked
    assertThat(LogVariableListener.getInvocations()).isEmpty();

    LogVariableListener.reset();
  }

  @Deployment
  @Test
  void testListenerOnAncestorScope() {
    caseService
      .withCaseDefinitionByKey("case")
      .create();

    CaseExecution stageExecution =
        caseService.createCaseExecutionQuery().activityId("PI_Stage_1").singleResult();
    assertThat(stageExecution).isNotNull();

    CaseExecution taskExecution =
        caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();

    // when i set a variable on a deeper scope
    caseService.withCaseExecution(taskExecution.getId()).setVariableLocal("aTaskVariable", "aTaskValue").execute();

    // then the listener is invoked
    assertThat(LogVariableListener.getInvocations()).hasSize(1);

    DelegateVariableInstanceSpec
      .fromCaseExecution(taskExecution)
      .event(VariableListener.CREATE)
      .name("aTaskVariable")
      .value("aTaskValue")
      .matches(LogVariableListener.getInvocations().get(0));

    LogVariableListener.reset();
  }

  @Deployment
  @Test
  void testInvalidListenerClassName() {
    caseService
      .withCaseDefinitionByKey("case")
      .create();

    CaseExecution taskExecution =
        caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();
    var caseExecutionCommandBuilder = caseService
        .withCaseExecution(taskExecution.getId())
        .setVariableLocal("aTaskVariable", "aTaskValue");

    assertThatThrownBy(caseExecutionCommandBuilder::execute).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment
  @Test
  void testListenerDoesNotImplementInterface() {
    caseService
      .withCaseDefinitionByKey("case")
      .create();

    CaseExecution taskExecution =
        caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();
    var caseExecutionCommandBuilder = caseService
        .withCaseExecution(taskExecution.getId())
        .setVariableLocal("aTaskVariable", "aTaskValue");

    assertThatThrownBy(caseExecutionCommandBuilder::execute).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment
  @Test
  void testDelegateInstanceIsProcessEngineAware() {
    CaseInstance caseInstance = caseService
      .withCaseDefinitionByKey("case")
      .create();
    assertThat(ProcessEngineAwareListener.hasFoundValidRuntimeService()).isFalse();

    // when i set a variable that causes the listener to be notified
    caseService.withCaseExecution(caseInstance.getId()).setVariableLocal("aTaskVariable", "aTaskValue").execute();

    // then the listener is invoked and has found process engine services
    assertThat(ProcessEngineAwareListener.hasFoundValidRuntimeService()).isTrue();

    ProcessEngineAwareListener.reset();
  }

  @Deployment
  @Test
  void testListenerDoesNotInterfereWithHistory() {
    CaseInstance caseInstance = caseService
      .withCaseDefinitionByKey("case")
      .create();

    // when i set a variable that causes the listener to be notified
    // and that listener sets the same variable to another value (here "value2")
    caseService.withCaseExecution(caseInstance.getId()).setVariableLocal("variable", "value1").execute();

    // then there should be two historic variable updates for both values
    if (processEngineConfiguration.getHistoryLevel().getId() >= HistoryLevel.HISTORY_LEVEL_FULL.getId()) {
      List<HistoricDetail> variableUpdates = historyService.createHistoricDetailQuery().variableUpdates().list();

      assertThat(variableUpdates).hasSize(2);

      for (HistoricDetail detail : variableUpdates) {
        HistoricVariableUpdate update = (HistoricVariableUpdate) detail;
        boolean update1Processed = false;
        boolean update2Processed = false;

        if (!update1Processed && "value1".equals(update.getValue())) {
          update1Processed = true;
        } else if (!update2Processed && "value2".equals(update.getValue())) {
          update2Processed = true;
        } else {
          fail("unexpected variable update");
        }

        assertThat(update1Processed || update2Processed).isTrue();
      }
    }
  }

  @Deployment
  @Test
  void testListenerInvocationFinishesBeforeSubsequentInvocations() {
    caseService
      .withCaseDefinitionByKey("case")
      .create();

    CaseExecution taskExecution =
        caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();

    // when i set a variable and the listener itself sets another variable
    caseService.withCaseExecution(taskExecution.getId()).setVariableLocal("variable", "value1").execute();

    // then all listeners for the first variable update are invoked first
    // and then the listeners for the second update are invoked
    List<DelegateCaseVariableInstance> invocations = LogAndUpdateVariableListener.getInvocations();
    assertThat(invocations).hasSize(6);

    // the first invocations should regard the first value
    DelegateVariableInstanceSpec
      .fromCaseExecution(taskExecution)
      .event(VariableListener.CREATE)
      .name("variable")
      .value("value1")
      .matches(LogAndUpdateVariableListener.getInvocations().get(0));

    DelegateVariableInstanceSpec
      .fromCaseExecution(taskExecution)
      .event(VariableListener.CREATE)
      .name("variable")
      .value("value1")
      .matches(LogAndUpdateVariableListener.getInvocations().get(1));

    // the second invocations should regard the updated value
    // there are four invocations since both listeners have set "value2" and both were again executed, i.e. 2*2 = 4

    for (int i = 2; i < 6; i++) {
      DelegateVariableInstanceSpec
      .fromCaseExecution(taskExecution)
      .event(VariableListener.UPDATE)
      .name("variable")
      .value("value2")
      .matches(LogAndUpdateVariableListener.getInvocations().get(i));
    }

    LogAndUpdateVariableListener.reset();
  }

  @Deployment
  @Test
  void testTwoListenersOnSameScope() {
    caseService
      .withCaseDefinitionByKey("case")
      .create();

    CaseExecution taskExecution =
        caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();

    // when i set a variable
    caseService.withCaseExecution(taskExecution.getId()).setVariableLocal("testVariable", "value1").execute();

    // then both listeners are invoked
    assertThat(LogVariableListener.getInvocations()).hasSize(1);

    DelegateVariableInstanceSpec
      .fromCaseExecution(taskExecution)
      .event(VariableListener.CREATE)
      .name("testVariable")
      .value("value1")
      .matches(LogVariableListener.getInvocations().get(0));

    assertThat(LogAndUpdateVariableListener.getInvocations()).hasSize(1);

    DelegateVariableInstanceSpec
      .fromCaseExecution(taskExecution)
      .event(VariableListener.CREATE)
      .name("testVariable")
      .value("value1")
      .matches(LogAndUpdateVariableListener.getInvocations().get(0));

    LogVariableListener.reset();
    LogAndUpdateVariableListener.reset();

  }

  @Deployment
  @Test
  void testVariableListenerByClassWithFieldExpressions() {
    caseService
      .withCaseDefinitionByKey("case")
      .create();

    CaseExecution taskExecution =
        caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();

    // when i set a variable
    caseService.withCaseExecution(taskExecution.getId()).setVariableLocal("testVariable", "value1").execute();

    // then the field expressions are resolved
    assertThat(LogInjectedValuesListener.getResolvedStringValueExpressions()).hasSize(1);
    assertThat(LogInjectedValuesListener.getResolvedStringValueExpressions().get(0)).isEqualTo("injectedValue");

    assertThat(LogInjectedValuesListener.getResolvedJuelExpressions()).hasSize(1);
    assertThat(LogInjectedValuesListener.getResolvedJuelExpressions().get(0)).isEqualTo("ope");

    LogInjectedValuesListener.reset();
  }

  @Deployment
  @Test
  void testVariableListenerByDelegateExpressionWithFieldExpressions() {
    beans.put("listener", new LogInjectedValuesListener());

    caseService
      .withCaseDefinitionByKey("case")
      .create();

    CaseExecution taskExecution =
        caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();

    // when i set a variable
    caseService.withCaseExecution(taskExecution.getId()).setVariableLocal("testVariable", "value1").execute();

    // then the field expressions are resolved
    assertThat(LogInjectedValuesListener.getResolvedStringValueExpressions()).hasSize(1);
    assertThat(LogInjectedValuesListener.getResolvedStringValueExpressions().get(0)).isEqualTo("injectedValue");

    assertThat(LogInjectedValuesListener.getResolvedJuelExpressions()).hasSize(1);
    assertThat(LogInjectedValuesListener.getResolvedJuelExpressions().get(0)).isEqualTo("ope");

    LogInjectedValuesListener.reset();
  }

  @Deployment
  @Test
  void testVariableListenerExecutionContext() {
    CaseInstance caseInstance = caseService
      .withCaseDefinitionByKey("case")
      .create();

    CaseExecution taskExecution =
        caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();

    // when i set a variable
    caseService.withCaseExecution(taskExecution.getId()).setVariableLocal("testVariable", "value1").execute();

    // then the listener is invoked
    assertThat(LogExecutionContextListener.getCaseExecutionContexts()).hasSize(1);
    CaseExecutionContext executionContext = LogExecutionContextListener.getCaseExecutionContexts().get(0);

    assertThat(executionContext).isNotNull();

    // although this is not inside a command, checking for IDs should be ok
    assertThat(executionContext.getCaseInstance().getId()).isEqualTo(caseInstance.getId());
    assertThat(executionContext.getExecution().getId()).isEqualTo(taskExecution.getId());

    LogExecutionContextListener.reset();
  }

  @Deployment
  @Test
  void testInvokeBuiltinListenersOnly() {
    // disable custom variable listener invocation
    processEngineConfiguration.setInvokeCustomVariableListeners(false);

    // add a builtin variable listener the hard way
    CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().singleResult();
    processEngineConfiguration
      .getDeploymentCache()
      .getCaseDefinitionById(caseDefinition.getId())
      .findActivity("PI_HumanTask_1")
      .addBuiltInVariableListener(CaseVariableListener.CREATE, new LogVariableListener());

    caseService
      .withCaseDefinitionByKey("case")
      .create();

    CaseExecution taskExecution =
        caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();

    // when i set a variable
    caseService.withCaseExecution(taskExecution.getId()).setVariableLocal("testVariable", "value1").execute();

    // then the builtin listener is invoked
    assertThat(LogVariableListener.getInvocations()).hasSize(1);

    // but the custom listener is not invoked
    assertThat(LogExecutionContextListener.getCaseExecutionContexts()).isEmpty();

    LogVariableListener.reset();
    LogExecutionContextListener.reset();

    // restore configuration
    processEngineConfiguration.setInvokeCustomVariableListeners(true);
  }

  @Test
  void testDefaultCustomListenerInvocationSetting() {
    assertThat(processEngineConfiguration.isInvokeCustomVariableListeners()).isTrue();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/listener/VariableListenerTest.testVariableListenerWithProcessTask.cmmn",
      "org/operaton/bpm/engine/test/cmmn/listener/VariableListenerTest.testVariableListenerWithProcessTask.bpmn20.xml"
  })
  @Test
  void testVariableListenerWithProcessTask() {
    CaseInstance caseInstance = caseService.createCaseInstanceByKey("case");

    CaseExecution processTask = caseService
        .createCaseExecutionQuery()
        .activityId("PI_ProcessTask_1")
        .singleResult();

    String processTaskId = processTask.getId();

    caseService
      .withCaseExecution(processTaskId)
      .manualStart();

    // then the listener is invoked
    assertThat(LogVariableListener.getInvocations()).hasSize(1);

    DelegateVariableInstanceSpec
      .fromCaseExecution(caseInstance)
      .sourceExecution(processTask)
      .event(VariableListener.CREATE)
      .name("aVariable")
      .value("aValue")
      .matches(LogVariableListener.getInvocations().get(0));

    LogVariableListener.reset();
  }

  @AfterEach
  void tearDown() {
    beans.clear();
  }

}
