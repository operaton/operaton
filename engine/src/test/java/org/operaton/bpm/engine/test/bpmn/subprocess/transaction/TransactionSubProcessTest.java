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
package org.operaton.bpm.engine.test.bpmn.subprocess.transaction;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.ActivityInstanceAssert;
import org.operaton.bpm.engine.variable.Variables;

import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * @author Daniel Meyer
 */
class TransactionSubProcessTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  RepositoryService repositoryService;
  TaskService taskService;
  HistoryService historyService;

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/subprocess/transaction/TransactionSubProcessTest.testSimpleCase.bpmn20.xml"})
  @Test
  void testSimpleCaseTxSuccessful() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transactionProcess");

    // after the process is started, we have compensate event subscriptions:
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").activityId("undoBookHotel").count()).isEqualTo(5);
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").activityId("undoBookFlight").count()).isOne();

    // the task is present:
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    // making the tx succeed:
    taskService.setVariable(task.getId(), "confirmed", true);
    taskService.complete(task.getId());

    // now the process instance execution is sitting in the 'afterSuccess' task
    // -> has left the transaction using the "normal" sequence flow
    List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstance.getId());
    assertThat(activeActivityIds).contains("afterSuccess");

    // there is a compensate event subscription for the transaction under the process instance
    EventSubscriptionEntity eventSubscriptionEntity = (EventSubscriptionEntity) runtimeService.createEventSubscriptionQuery()
        .eventType("compensate").activityId("tx").executionId(processInstance.getId()).singleResult();

    // there is an event-scope execution associated with the event-subscription:
    assertThat(eventSubscriptionEntity.getConfiguration()).isNotNull();
    Execution eventScopeExecution = runtimeService.createExecutionQuery().executionId(eventSubscriptionEntity.getConfiguration()).singleResult();
    assertThat(eventScopeExecution).isNotNull();

    // there is a compensate event subscription for the miBody of 'bookHotel' activity
    EventSubscriptionEntity miBodyEventSubscriptionEntity = (EventSubscriptionEntity) runtimeService.createEventSubscriptionQuery()
        .eventType("compensate").activityId("bookHotel" + BpmnParse.MULTI_INSTANCE_BODY_ID_SUFFIX).executionId(eventScopeExecution.getId()).singleResult();
    assertThat(miBodyEventSubscriptionEntity).isNotNull();
    String miBodyEventScopeExecutionId = miBodyEventSubscriptionEntity.getConfiguration();

    // we still have compensate event subscriptions for the compensation handlers, only now they are part of the event scope
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").activityId("undoBookHotel").executionId(miBodyEventScopeExecutionId).count()).isEqualTo(5);
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").activityId("undoBookFlight").executionId(eventScopeExecution.getId()).count()).isOne();
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").activityId("undoChargeCard").executionId(eventScopeExecution.getId()).count()).isOne();

    // assert that the compensation handlers have not been invoked:
    assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookHotel")).isNull();
    assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookFlight")).isNull();
    assertThat(runtimeService.getVariable(processInstance.getId(), "undoChargeCard")).isNull();

    // end the process instance
    runtimeService.signal(processInstance.getId());
    testRule.assertProcessEnded(processInstance.getId());
    assertThat(runtimeService.createExecutionQuery().count()).isZero();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/subprocess/transaction/TransactionSubProcessTest.testSimpleCase.bpmn20.xml"})
  @Test
  void testActivityInstanceTreeAfterSuccessfulCompletion() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transactionProcess");

    // the tx task is present
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    // making the tx succeed
    taskService.setVariable(task.getId(), "confirmed", true);
    taskService.complete(task.getId());

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    ActivityInstanceAssert.assertThat(tree)
      .hasStructure(
        ActivityInstanceAssert.describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .activity("afterSuccess")
        .done());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/subprocess/transaction/TransactionSubProcessTest.testWaitstateCompensationHandler.bpmn20.xml"})
  @Test
  void testWaitstateCompensationHandler() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transactionProcess");

    // after the process is started, we have compensate event subscriptions:
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").activityId("undoBookHotel").count()).isEqualTo(5);
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").activityId("undoBookFlight").count()).isOne();

    // the task is present:
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    // making the tx fail:
    taskService.setVariable(task.getId(), "confirmed", false);
    taskService.complete(task.getId());

    // now there are two user task instances (the compensation handlers):

    List<Task> undoBookHotel = taskService.createTaskQuery().taskDefinitionKey("undoBookHotel").list();
    List<Task> undoBookFlight = taskService.createTaskQuery().taskDefinitionKey("undoBookFlight").list();

    assertThat(undoBookHotel).hasSize(5);
    assertThat(undoBookFlight).hasSize(1);

    ActivityInstance rootActivityInstance = runtimeService.getActivityInstance(processInstance.getId());
    List<ActivityInstance> undoBookHotelInstances = testRule.getInstancesForActivityId(rootActivityInstance, "undoBookHotel");
    List<ActivityInstance> undoBookFlightInstances = testRule.getInstancesForActivityId(rootActivityInstance, "undoBookFlight");
    assertThat(undoBookHotelInstances).hasSize(5);
    assertThat(undoBookFlightInstances).hasSize(1);

    assertThat(
        describeActivityInstanceTree(processInstance.getId())
          .beginScope("tx")
            .activity("failure")
            .activity("undoBookHotel")
            .activity("undoBookHotel")
            .activity("undoBookHotel")
            .activity("undoBookHotel")
            .activity("undoBookHotel")
            .activity("undoBookFlight")
          .done()
          );

    for (Task t : undoBookHotel) {
      taskService.complete(t.getId());
    }
    taskService.complete(undoBookFlight.get(0).getId());

    // now the process instance execution is sitting in the 'afterCancellation' task
    // -> has left the transaction using the cancel boundary event
    List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstance.getId());
    assertThat(activeActivityIds).contains("afterCancellation");

    // we have no more compensate event subscriptions
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").count()).isZero();

    // end the process instance
    runtimeService.signal(processInstance.getId());
    testRule.assertProcessEnded(processInstance.getId());
    assertThat(runtimeService.createExecutionQuery().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/subprocess/transaction/TransactionSubProcessTest.testSimpleCase.bpmn20.xml"})
  @Test
  void testSimpleCaseTxCancelled() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transactionProcess");

    // after the process is started, we have compensate event subscriptions:
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").activityId("undoBookHotel").count()).isEqualTo(5);
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").activityId("undoBookFlight").count()).isOne();

    // the task is present:
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    // making the tx fail:
    taskService.setVariable(task.getId(), "confirmed", false);
    taskService.complete(task.getId());

    // we have no more compensate event subscriptions
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").count()).isZero();

    // assert that the compensation handlers have been invoked:
    assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookHotel")).isEqualTo(5);
    assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookFlight")).isEqualTo(1);
    assertThat(runtimeService.getVariable(processInstance.getId(), "undoChargeCard")).isEqualTo(1);

    // signal compensation handler completion
    List<Execution> compensationHandlerExecutions = collectExecutionsFor("undoBookHotel", "undoBookFlight", "undoChargeCard");
    for (Execution execution : compensationHandlerExecutions) {
      runtimeService.signal(execution.getId());
    }

    // now the process instance execution is sitting in the 'afterCancellation' task
    // -> has left the transaction using the cancel boundary event
    List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstance.getId());
    assertThat(activeActivityIds).contains("afterCancellation");

    // if we have history, we check that the invocation of the compensation handlers is recorded in history.
    if(!ProcessEngineConfiguration.HISTORY_NONE.equals(processEngineConfiguration.getHistory())) {
      assertThat(historyService.createHistoricActivityInstanceQuery()
        .activityId("undoBookFlight")
        .count()).isOne();

      assertThat(historyService.createHistoricActivityInstanceQuery()
          .activityId("undoBookHotel")
          .count()).isEqualTo(5);

      assertThat(historyService.createHistoricActivityInstanceQuery()
        .activityId("undoChargeCard")
        .count()).isOne();
    }

    // end the process instance
    runtimeService.signal(processInstance.getId());
    testRule.assertProcessEnded(processInstance.getId());
    assertThat(runtimeService.createExecutionQuery().count()).isZero();
  }


  @Deployment
  @Test
  void testCancelEndConcurrent() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transactionProcess");

    // after the process is started, we have compensate event subscriptions:
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").activityId("undoBookHotel").count()).isEqualTo(5);
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").activityId("undoBookFlight").count()).isOne();

    // the task is present:
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    // making the tx fail:
    taskService.setVariable(task.getId(), "confirmed", false);
    taskService.complete(task.getId());

    // we have no more compensate event subscriptions
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").count()).isZero();

    // assert that the compensation handlers have been invoked:
    assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookHotel")).isEqualTo(5);
    assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookFlight")).isEqualTo(1);

    // signal compensation handler completion
    List<Execution> compensationHandlerExecutions = collectExecutionsFor("undoBookHotel", "undoBookFlight");
    for (Execution execution : compensationHandlerExecutions) {
      runtimeService.signal(execution.getId());
    }

    // now the process instance execution is sitting in the 'afterCancellation' task
    // -> has left the transaction using the cancel boundary event
    List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstance.getId());
    assertThat(activeActivityIds).contains("afterCancellation");

    // if we have history, we check that the invocation of the compensation handlers is recorded in history.
    if(!ProcessEngineConfiguration.HISTORY_NONE.equals(processEngineConfiguration.getHistory())) {
      assertThat(historyService.createHistoricActivityInstanceQuery()
        .activityId("undoBookFlight")
        .count()).isOne();

      assertThat(historyService.createHistoricActivityInstanceQuery()
          .activityId("undoBookHotel")
          .count()).isEqualTo(5);
    }

    // end the process instance
    runtimeService.signal(processInstance.getId());
    testRule.assertProcessEnded(processInstance.getId());
    assertThat(runtimeService.createExecutionQuery().count()).isZero();
  }

  @Deployment
  @Test
  void testNestedCancelInner() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transactionProcess");

    // after the process is started, we have compensate event subscriptions:
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").activityId("undoBookFlight").count()).isZero();
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").activityId("innerTxundoBookHotel").count()).isEqualTo(5);
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").activityId("innerTxundoBookFlight").count()).isOne();

    // the tasks are present:
    Task taskInner = taskService.createTaskQuery().taskDefinitionKey("innerTxaskCustomer").singleResult();
    Task taskOuter = taskService.createTaskQuery().taskDefinitionKey("bookFlight").singleResult();
    assertThat(taskInner).isNotNull();
    assertThat(taskOuter).isNotNull();

    // making the tx fail:
    taskService.setVariable(taskInner.getId(), "confirmed", false);
    taskService.complete(taskInner.getId());

    // we have no more compensate event subscriptions for the inner tx
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").activityId("innerTxundoBookHotel").count()).isZero();
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").activityId("innerTxundoBookFlight").count()).isZero();

    // we do not have a subscription or the outer tx yet
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").activityId("undoBookFlight").count()).isZero();

    // assert that the compensation handlers have been invoked:
    assertThat(runtimeService.getVariable(processInstance.getId(), "innerTxundoBookHotel")).isEqualTo(5);
    assertThat(runtimeService.getVariable(processInstance.getId(), "innerTxundoBookFlight")).isEqualTo(1);

    // signal compensation handler completion
    List<Execution> compensationHandlerExecutions = collectExecutionsFor("innerTxundoBookFlight", "innerTxundoBookHotel");
    for (Execution execution : compensationHandlerExecutions) {
      runtimeService.signal(execution.getId());
    }

    // now the process instance execution is sitting in the 'afterInnerCancellation' task
    // -> has left the transaction using the cancel boundary event
    List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstance.getId());
    assertThat(activeActivityIds).contains("afterInnerCancellation");

    // if we have history, we check that the invocation of the compensation handlers is recorded in history.
    if(!ProcessEngineConfiguration.HISTORY_NONE.equals(processEngineConfiguration.getHistory())) {
      assertThat(historyService.createHistoricActivityInstanceQuery()
          .activityId("innerTxundoBookHotel")
          .count()).isEqualTo(5);

      assertThat(historyService.createHistoricActivityInstanceQuery()
        .activityId("innerTxundoBookFlight")
        .count()).isOne();
    }

    // complete the task in the outer tx
    taskService.complete(taskOuter.getId());

    // end the process instance (signal the execution still sitting in afterInnerCancellation)
    runtimeService.signal(runtimeService.createExecutionQuery().activityId("afterInnerCancellation").singleResult().getId());

    testRule.assertProcessEnded(processInstance.getId());
    assertThat(runtimeService.createExecutionQuery().count()).isZero();
  }

  @Deployment
  @Test
  void testNestedCancelOuter() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transactionProcess");

    // after the process is started, we have compensate event subscriptions:
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").activityId("undoBookFlight").count()).isZero();
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").activityId("innerTxundoBookHotel").count()).isEqualTo(5);
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").activityId("innerTxundoBookFlight").count()).isOne();

    // the tasks are present:
    Task taskInner = taskService.createTaskQuery().taskDefinitionKey("innerTxaskCustomer").singleResult();
    Task taskOuter = taskService.createTaskQuery().taskDefinitionKey("bookFlight").singleResult();
    assertThat(taskInner).isNotNull();
    assertThat(taskOuter).isNotNull();

    // making the outer tx fail (invokes cancel end event)
    taskService.complete(taskOuter.getId());

    // now the process instance is sitting in 'afterOuterCancellation'
    List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstance.getId());
    assertThat(activeActivityIds).contains("afterOuterCancellation");

    // we have no more compensate event subscriptions
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").activityId("innerTxundoBookHotel").count()).isZero();
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").activityId("innerTxundoBookFlight").count()).isZero();
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("compensate").activityId("undoBookFlight").count()).isZero();

    // the compensation handlers of the inner tx have not been invoked
    assertThat(runtimeService.getVariable(processInstance.getId(), "innerTxundoBookHotel")).isNull();
    assertThat(runtimeService.getVariable(processInstance.getId(), "innerTxundoBookFlight")).isNull();

    // the compensation handler in the outer tx has been invoked
    assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookFlight")).isEqualTo(1);

    // end the process instance (signal the execution still sitting in afterOuterCancellation)
    runtimeService.signal(runtimeService.createExecutionQuery().activityId("afterOuterCancellation").singleResult().getId());

    testRule.assertProcessEnded(processInstance.getId());
    assertThat(runtimeService.createExecutionQuery().count()).isZero();

  }

  /*
   * The cancel end event cancels all instances, compensation is performed for all instances
   *
   * see spec page 470:
   * "If the cancelActivity attribute is set, the Activity the Event is attached to is then
   * cancelled (in case of a multi-instance, all its instances are cancelled);"
   */
  @Deployment
  @Test
  void testMultiInstanceTx() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transactionProcess");

    // there are now 5 instances of the transaction:

    List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery()
      .eventType("compensate")
      .list();

    // there are 10 compensation event subscriptions
    assertThat(eventSubscriptions).hasSize(10);

    Task task = taskService.createTaskQuery().listPage(0, 1).get(0);

    // canceling one instance triggers compensation for all other instances:
    taskService.setVariable(task.getId(), "confirmed", false);
    taskService.complete(task.getId());

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isZero();

    assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookHotel")).isEqualTo(1);
    assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookFlight")).isEqualTo(1);

    runtimeService.signal(runtimeService.createExecutionQuery().activityId("afterCancellation").singleResult().getId());

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/subprocess/transaction/TransactionSubProcessTest.testMultiInstanceTx.bpmn20.xml"})
  @Test
  void testMultiInstanceTxSuccessful() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transactionProcess");

    // there are now 5 instances of the transaction:

    List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery()
      .eventType("compensate")
      .list();

    // there are 10 compensation event subscriptions
    assertThat(eventSubscriptions).hasSize(10);

    // first complete the inner user-tasks
    List<Task> tasks = taskService.createTaskQuery().list();
    for (Task task : tasks) {
      taskService.setVariable(task.getId(), "confirmed", true);
      taskService.complete(task.getId());
    }

    // now complete the inner receive tasks
    List<Execution> executions = runtimeService.createExecutionQuery().activityId("receive").list();
    for (Execution execution : executions) {
      runtimeService.signal(execution.getId());
    }

    runtimeService.signal(runtimeService.createExecutionQuery().activityId("afterSuccess").singleResult().getId());

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isZero();
    testRule.assertProcessEnded(processInstance.getId());

  }

  @Deployment
  @Test
  void testCompensateSubprocess() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("txProcess");

    Task innerTask = taskService.createTaskQuery().singleResult();
    taskService.complete(innerTask.getId());

    // when the transaction is cancelled
    runtimeService.setVariable(instance.getId(), "cancelTx", true);
    runtimeService.setVariable(instance.getId(), "compensate", false);
    Task beforeCancelTask = taskService.createTaskQuery().singleResult();
    taskService.complete(beforeCancelTask.getId());

    // then compensation is triggered
    Task compensationTask = taskService.createTaskQuery().singleResult();
    assertThat(compensationTask).isNotNull();
    assertThat(compensationTask.getTaskDefinitionKey()).isEqualTo("undoInnerTask");
    taskService.complete(compensationTask.getId());

    // and the process instance ends successfully
    Task afterBoundaryTask = taskService.createTaskQuery().singleResult();
    assertThat(afterBoundaryTask.getTaskDefinitionKey()).isEqualTo("afterCancel");
    taskService.complete(afterBoundaryTask.getId());
    testRule.assertProcessEnded(instance.getId());
  }

  @Deployment
  @Test
  void testCompensateTransactionWithEventSubprocess() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("txProcess");
    Task beforeCancelTask = taskService.createTaskQuery().singleResult();

    // when the transaction is cancelled and handled by an event subprocess
    taskService.complete(beforeCancelTask.getId());

    // then completing compensation works
    Task compensationHandler = taskService.createTaskQuery().singleResult();
    assertThat(compensationHandler).isNotNull();
    assertThat(compensationHandler.getTaskDefinitionKey()).isEqualTo("blackBoxCompensationHandler");

    taskService.complete(compensationHandler.getId());

    testRule.assertProcessEnded(processInstance.getId());

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/transaction/TransactionSubProcessTest.testCompensateTransactionWithEventSubprocess.bpmn20.xml")
  @Test
  void testCompensateTransactionWithEventSubprocessActivityInstanceTree() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("txProcess");
    Task beforeCancelTask = taskService.createTaskQuery().singleResult();

    // when the transaction is cancelled and handled by an event subprocess
    taskService.complete(beforeCancelTask.getId());

    // then the activity instance tree is correct
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .beginScope("tx")
            .activity("cancelEnd")
            .beginScope("innerSubProcess")
              .activity("blackBoxCompensationHandler")
              .beginScope("eventSubProcess")
                .activity("eventSubProcessThrowCompensation")
       .done());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/transaction/TransactionSubProcessTest.testCompensateSubprocess.bpmn20.xml")
  @Test
  void testCompensateSubprocessNotTriggered() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("txProcess");

    Task innerTask = taskService.createTaskQuery().singleResult();
    taskService.complete(innerTask.getId());

    // when the transaction is not cancelled
    runtimeService.setVariable(instance.getId(), "cancelTx", false);
    runtimeService.setVariable(instance.getId(), "compensate", false);
    Task beforeEndTask = taskService.createTaskQuery().singleResult();
    taskService.complete(beforeEndTask.getId());

    // then
    Task afterTxTask = taskService.createTaskQuery().singleResult();
    assertThat(afterTxTask.getTaskDefinitionKey()).isEqualTo("afterTx");

    // and the process has ended
    taskService.complete(afterTxTask.getId());
    testRule.assertProcessEnded(instance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/transaction/TransactionSubProcessTest.testCompensateSubprocess.bpmn20.xml")
  @Test
  void testCompensateSubprocessAfterTxCompletion() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("txProcess");

    Task innerTask = taskService.createTaskQuery().singleResult();
    taskService.complete(innerTask.getId());

    // when the transaction is not cancelled
    runtimeService.setVariable(instance.getId(), "cancelTx", false);
    runtimeService.setVariable(instance.getId(), "compensate", true);
    Task beforeTxEndTask = taskService.createTaskQuery().singleResult();
    taskService.complete(beforeTxEndTask.getId());

    // but when compensation is thrown after the tx has completed successfully
    Task afterTxTask = taskService.createTaskQuery().singleResult();
    taskService.complete(afterTxTask.getId());

    // then compensation for the subprocess is triggered
    Task compensationTask = taskService.createTaskQuery().singleResult();
    assertThat(compensationTask).isNotNull();
    assertThat(compensationTask.getTaskDefinitionKey()).isEqualTo("undoInnerTask");
    taskService.complete(compensationTask.getId());

    // and the process has ended
    testRule.assertProcessEnded(instance.getId());
  }

  @Deployment
  public void FAILURE_testMultipleCompensationOfCancellationOfMultipleTx() {
    // when
    List<String> devices = new ArrayList<>();
	  devices.add("device1");
    devices.add("device2");
    devices.add("fail");
    runtimeService.startProcessInstanceByKey(
	      "order", //
	      Variables.putValue("devices", devices));

    // then the compensation should be triggered three times
    int expected = 3;
    int actual = historyService
      .createHistoricActivityInstanceQuery()
      .activityId("ServiceTask_CompensateConfiguration")
      .list()
      .size();
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void testMultipleCancelBoundaryFails() {
    // given
    var deploymentBuilder = repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/subprocess/transaction/TransactionSubProcessTest.testMultipleCancelBoundaryFails.bpmn20.xml");

    // when/then
    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(ParseException.class)
      .hasMessageContaining("multiple boundary events with cancelEventDefinition not supported on same transaction")
      .satisfies(e -> {
        var exception = (ParseException) e;
        assertThat(exception.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("catchCancelTx2");
      });
  }

  @Test
  void testCancelBoundaryNoTransactionFails() {
    // given
    var deploymentBuilder = repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/subprocess/transaction/TransactionSubProcessTest.testCancelBoundaryNoTransactionFails.bpmn20.xml");

    // when/then
    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(ParseException.class)
      .hasMessageContaining("boundary event with cancelEventDefinition only supported on transaction subprocesses")
      .satisfies(e -> {
        var exception = (ParseException) e;
        assertThat(exception.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("catchCancelTx");
      });
  }

  @Test
  void testCancelEndNoTransactionFails() {
    // given
    var deploymentBuilder = repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/subprocess/transaction/TransactionSubProcessTest.testCancelEndNoTransactionFails.bpmn20.xml");

    // when/then
    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(ParseException.class)
      .hasMessageContaining("end event with cancelEventDefinition only supported inside transaction subprocess")
      .satisfies(e -> {
        var exception = (ParseException) e;
        assertThat(exception.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("failure");
      });
  }

  @Deployment
  @Test
  void testParseWithDI() {

    // this test simply makes sure we can parse a transaction subprocess with DI information
    // the actual transaction behavior is tested by other testcases

    // // failing case

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TransactionSubProcessTest");

    Task task = taskService.createTaskQuery().singleResult();
    taskService.setVariable(task.getId(), "confirmed", false);

    taskService.complete(task.getId());

    testRule.assertProcessEnded(processInstance.getId());


    // //// success case

    processInstance = runtimeService.startProcessInstanceByKey("TransactionSubProcessTest");

    task = taskService.createTaskQuery().singleResult();
    taskService.setVariable(task.getId(), "confirmed", true);

    taskService.complete(task.getId());

    testRule.assertProcessEnded(processInstance.getId());
  }

  protected List<Execution> collectExecutionsFor(String... activityIds) {
    List<Execution> executions = new ArrayList<>();

    for (String activityId : activityIds) {
      executions.addAll(runtimeService.createExecutionQuery().activityId(activityId).list());
    }

    return executions;
  }
}
