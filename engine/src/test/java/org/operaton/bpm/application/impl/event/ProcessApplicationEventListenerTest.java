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
package org.operaton.bpm.application.impl.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.application.impl.EmbeddedProcessApplication;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.core.instance.CoreExecution;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

/**
 * @author Daniel Meyer
 *
 */
public class ProcessApplicationEventListenerTest {

  @RegisterExtension
  ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
       .configurationResource("org/operaton/bpm/application/impl/event/pa.event.listener.operaton.cfg.xml")
       .build();

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  RepositoryService repositoryService;
  TaskService taskService;
  ManagementService managementService;

  @BeforeEach
  public void setUp() {
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
    runtimeService = engineRule.getRuntimeService();
    repositoryService = engineRule.getRepositoryService();
    taskService = engineRule.getTaskService();
    managementService = engineRule.getManagementService();
  }

  @AfterEach
  public void closeDownProcessEngine() {
    managementService.unregisterProcessApplication(engineRule.getDeploymentId(), false);
  }

  @Test
  @Deployment(resources = { "org/operaton/bpm/application/impl/event/ProcessApplicationEventListenerTest.testExecutionListener.bpmn20.xml" })
  public void testExecutionListenerNull() {
    // this test verifies that the process application can return a 'null'
    // execution listener
    EmbeddedProcessApplication processApplication = new EmbeddedProcessApplication();

    // register app so that it receives events
    managementService.registerProcessApplication(engineRule.getDeploymentId(), processApplication.getReference());
    // I can start a process event though the process app does not provide an
    // event listener.
    ProcessInstance process = runtimeService.startProcessInstanceByKey("startToEnd");

    assertThat(process.isEnded()).isTrue();

  }

  @Test
  @Deployment(resources = { "org/operaton/bpm/application/impl/event/ProcessApplicationEventListenerTest.testExecutionListener.bpmn20.xml" })
  public void testShouldInvokeExecutionListenerOnStartAndEndOfProcessInstance() {
    final AtomicInteger processDefinitionEventCount = new AtomicInteger();

    EmbeddedProcessApplication processApplication = new EmbeddedProcessApplication() {
      @Override
      public ExecutionListener getExecutionListener() {
        // this process application returns an execution listener
        return execution -> {
          if (((CoreExecution) execution).getEventSource() instanceof ProcessDefinitionEntity)
            processDefinitionEventCount.incrementAndGet();
        };
      }
    };

    // register app so that it receives events
    managementService.registerProcessApplication(engineRule.getDeploymentId(), processApplication.getReference());

    // Start process instance.
    runtimeService.startProcessInstanceByKey("startToEnd");

    // Start and end of the process 
    assertThat(processDefinitionEventCount.get()).isEqualTo(2);
  }

  @Test
  @Deployment(resources = { "org/operaton/bpm/application/impl/event/ProcessApplicationEventListenerTest.testExecutionListener.bpmn20.xml" })
  public void testShouldNotIncrementExecutionListenerCountOnStartAndEndOfProcessInstance() {
    final AtomicInteger eventCount = new AtomicInteger();

    EmbeddedProcessApplication processApplication = new EmbeddedProcessApplication() {
      @Override
      public ExecutionListener getExecutionListener() {
        // this process application returns an execution listener
        return execution -> {
          if (!(((CoreExecution) execution).getEventSource() instanceof ProcessDefinitionEntity))
            eventCount.incrementAndGet();
        };
      }
    };

    // register app so that it receives events
    managementService.registerProcessApplication(engineRule.getDeploymentId(), processApplication.getReference());

    // Start process instance.
    runtimeService.startProcessInstanceByKey("startToEnd");

    assertThat(eventCount.get()).isEqualTo(5);
  }

  @Test
  @Deployment
  public void testExecutionListener() {
    final AtomicInteger eventCount = new AtomicInteger();
    
    EmbeddedProcessApplication processApplication = new EmbeddedProcessApplication() {
      @Override
      public ExecutionListener getExecutionListener() {
        // this process application returns an execution listener
        return execution -> eventCount.incrementAndGet();
      }
    };

    // register app so that it is notified about events
    managementService.registerProcessApplication(engineRule.getDeploymentId(), processApplication.getReference());

    // start process instance
    runtimeService.startProcessInstanceByKey("startToEnd");

    // 7 events received
    assertThat(eventCount.get()).isEqualTo(7);
   }

  @Test
  @Deployment
  public void testExecutionListenerWithErrorBoundaryEvent() {
    final AtomicInteger eventCount = new AtomicInteger();
    
    EmbeddedProcessApplication processApplication = new EmbeddedProcessApplication() {
      @Override
      public ExecutionListener getExecutionListener() {
        return execution -> eventCount.incrementAndGet();
      }
    };

    // register app so that it is notified about events
    managementService.registerProcessApplication(engineRule.getDeploymentId(), processApplication.getReference());

    // 1. (start)startEvent(end) -(take)-> (start)serviceTask(end) -(take)-> (start)endEvent(end) (8 Events)

    // start process instance
    runtimeService.startProcessInstanceByKey("executionListener");

    assertThat(eventCount.get()).isEqualTo(10);
    
    // reset counter
    eventCount.set(0);
    
    // 2. (start)startEvent(end) -(take)-> (start)serviceTask(end)/(start)errorBoundaryEvent(end) -(take)-> (start)endEvent(end) (10 Events)

    // start process instance
    runtimeService.startProcessInstanceByKey("executionListener", Collections.singletonMap("shouldThrowError", true));

    assertThat(eventCount.get()).isEqualTo(12);
  }

  @Test
  @Deployment
  public void testExecutionListenerWithTimerBoundaryEvent() {
    final AtomicInteger eventCount = new AtomicInteger();
    
    EmbeddedProcessApplication processApplication = new EmbeddedProcessApplication() {
      @Override
      public ExecutionListener getExecutionListener() {
        return execution -> eventCount.incrementAndGet();
      }
    };

    // register app so that it is notified about events
    managementService.registerProcessApplication(engineRule.getDeploymentId(), processApplication.getReference());

    // 1. (start)startEvent(end) -(take)-> (start)userTask(end) -(take)-> (start)endEvent(end) (8 Events)

    // start process instance
    runtimeService.startProcessInstanceByKey("executionListener");

    // complete task
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    assertThat(eventCount.get()).isEqualTo(10);
    
    // reset counter
    eventCount.set(0);
    
    // 2. (start)startEvent(end) -(take)-> (start)userTask(end)/(start)timerBoundaryEvent(end) -(take)-> (start)endEvent(end) (10 Events)

    // start process instance
    runtimeService.startProcessInstanceByKey("executionListener");

    // fire timer event
    Job job = managementService.createJobQuery().singleResult();
    managementService.executeJob(job.getId());

    assertThat(eventCount.get()).isEqualTo(12);
  }

  @Test
  @Deployment
  public void testExecutionListenerWithSignalBoundaryEvent() {
    final AtomicInteger eventCount = new AtomicInteger();
    
    EmbeddedProcessApplication processApplication = new EmbeddedProcessApplication() {
      @Override
      public ExecutionListener getExecutionListener() {
        return execution -> eventCount.incrementAndGet();
      }
    };

    // register app so that it is notified about events
    managementService.registerProcessApplication(engineRule.getDeploymentId(), processApplication.getReference());

    // 1. (start)startEvent(end) -(take)-> (start)userTask(end) -(take)-> (start)endEvent(end) (8 Events)

    // start process instance
    runtimeService.startProcessInstanceByKey("executionListener");

    // complete task
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    assertThat(eventCount.get()).isEqualTo(10);
    
    // reset counter
    eventCount.set(0);
    
    // 2. (start)startEvent(end) -(take)-> (start)userTask(end)/(start)signalBoundaryEvent(end) -(take)-> (start)endEvent(end) (10 Events)

    // start process instance
    runtimeService.startProcessInstanceByKey("executionListener");

    // signal event
    runtimeService.signalEventReceived("signal");

    assertThat(eventCount.get()).isEqualTo(12);
  }

  @Test
  @Deployment
  public void testExecutionListenerWithMultiInstanceBody() {
    final AtomicInteger eventCountForMultiInstanceBody = new AtomicInteger();

    EmbeddedProcessApplication processApplication = new EmbeddedProcessApplication() {
      @Override
      public ExecutionListener getExecutionListener() {
        return execution -> {
          if ("miTasks#multiInstanceBody".equals(execution.getCurrentActivityId())
              && (ExecutionListener.EVENTNAME_START.equals(execution.getEventName())
                  || ExecutionListener.EVENTNAME_END.equals(execution.getEventName()))) {
            eventCountForMultiInstanceBody.incrementAndGet();
          }
        };
      }
    };

    // register app so that it is notified about events
    managementService.registerProcessApplication(engineRule.getDeploymentId(), processApplication.getReference());


    // start process instance
    runtimeService.startProcessInstanceByKey("executionListener");

    // complete task
    List<Task> miTasks = taskService.createTaskQuery().list();
    for (Task task : miTasks) {
      taskService.complete(task.getId());
    }

    // 2 events are expected: one for mi body start; one for mi body end
    assertThat(eventCountForMultiInstanceBody.get()).isEqualTo(2);
  }

  @Test
  @Deployment
  public void testTaskListener() {
    final List<String> events = new ArrayList<>();

    EmbeddedProcessApplication processApplication = new EmbeddedProcessApplication() {
      @Override
      public TaskListener getTaskListener() {
        return delegateTask -> events.add(delegateTask.getEventName());
      }
    };

    // register app so that it is notified about events
    managementService.registerProcessApplication(engineRule.getDeploymentId(), processApplication.getReference());

    // start process instance
    ProcessInstance taskListenerProcess = runtimeService.startProcessInstanceByKey("taskListenerProcess");

    // create event received
    assertThat(events).hasSize(1);
    assertThat(events.get(0)).isEqualTo(TaskListener.EVENTNAME_CREATE);

    Task task = taskService.createTaskQuery().singleResult();
    //assign task:
    taskService.setAssignee(task.getId(), "jonny");
    assertThat(events).hasSize(3);
    assertThat(events.get(1)).isEqualTo(TaskListener.EVENTNAME_UPDATE);
    assertThat(events.get(2)).isEqualTo(TaskListener.EVENTNAME_ASSIGNMENT);

    // complete task
    taskService.complete(task.getId());
    assertThat(events).hasSize(5);
    assertThat(events.get(3)).isEqualTo(TaskListener.EVENTNAME_COMPLETE);
    // next task was created
    assertThat(events.get(4)).isEqualTo(TaskListener.EVENTNAME_CREATE);

    // delete process instance so last task will be deleted
    runtimeService.deleteProcessInstance(taskListenerProcess.getProcessInstanceId(), "test delete event");
    assertThat(events).hasSize(6);
    assertThat(events.get(5)).isEqualTo(TaskListener.EVENTNAME_DELETE);

  }

  @Test
  @Deployment
  public void testIntermediateTimerEvent() {
    // given
    final List<String> timerEvents = new ArrayList<>();

    EmbeddedProcessApplication processApplication = new EmbeddedProcessApplication() {
      @Override
      public ExecutionListener getExecutionListener() {
        return delegateExecution -> {
          String currentActivityId = delegateExecution.getCurrentActivityId();
          String eventName = delegateExecution.getEventName();
          if ("timer".equals(currentActivityId) &&
              (ExecutionListener.EVENTNAME_START.equals(eventName) || ExecutionListener.EVENTNAME_END.equals(eventName))) {
            timerEvents.add(delegateExecution.getEventName());
          }
        };
      }
    };

    // register app so that it is notified about events
    managementService.registerProcessApplication(engineRule.getDeploymentId(), processApplication.getReference());

    // when
    runtimeService.startProcessInstanceByKey("process");
    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.executeJob(jobId);

    // then
    assertThat(timerEvents).hasSize(2);

    // "start" event listener
    assertThat(timerEvents.get(0)).isEqualTo(ExecutionListener.EVENTNAME_START);

    // "end" event listener
    assertThat(timerEvents.get(1)).isEqualTo(ExecutionListener.EVENTNAME_END);
  }

  @Test
  @Deployment
  public void testIntermediateSignalEvent() {
    // given
    final List<String> timerEvents = new ArrayList<>();

    EmbeddedProcessApplication processApplication = new EmbeddedProcessApplication() {
      @Override
      public ExecutionListener getExecutionListener() {
        return delegateExecution -> {
          String currentActivityId = delegateExecution.getCurrentActivityId();
          String eventName = delegateExecution.getEventName();
          if ("signal".equals(currentActivityId) &&
              (ExecutionListener.EVENTNAME_START.equals(eventName) || ExecutionListener.EVENTNAME_END.equals(eventName))) {
            timerEvents.add(delegateExecution.getEventName());
          }
        };
      }
    };

    // register app so that it is notified about events
    managementService.registerProcessApplication(engineRule.getDeploymentId(), processApplication.getReference());

    // when
    runtimeService.startProcessInstanceByKey("process");
    runtimeService.signalEventReceived("abort");

    // then
    assertThat(timerEvents).hasSize(2);

    // "start" event listener
    assertThat(timerEvents.get(0)).isEqualTo(ExecutionListener.EVENTNAME_START);

    // "end" event listener
    assertThat(timerEvents.get(1)).isEqualTo(ExecutionListener.EVENTNAME_END);
  }

}
