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
package org.operaton.bpm.engine.test.api.runtime.migration;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionEntity;
import org.operaton.bpm.engine.impl.jobexecutor.TimerCatchIntermediateEventJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerExecuteNestedActivityJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerStartEventSubprocessJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerTaskListenerJobHandler;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TimerEntity;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.util.ActivityInstanceAssert.ActivityInstanceAssertThatClause;
import org.operaton.bpm.engine.test.util.ExecutionAssert;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;

import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Thorben Lindhauer
 *
 */
public class MigrationTestRule extends ProcessEngineTestRule {

  public ProcessInstanceSnapshot snapshotBeforeMigration;
  public ProcessInstanceSnapshot snapshotAfterMigration;

  public MigrationTestRule(ProcessEngineRule processEngineRule) {
    super(processEngineRule);
  }

  public String getSingleExecutionIdForActivity(ActivityInstance activityInstance, String activityId) {
    ActivityInstance singleInstance = getSingleActivityInstance(activityInstance, activityId);

    String[] executionIds = singleInstance.getExecutionIds();
    if (executionIds.length == 1) {
      return executionIds[0];
    }
    else {
      throw new RuntimeException("There is more than one execution assigned to activity instance %s".formatted(singleInstance.getId()));
    }
  }

  public String getSingleExecutionIdForActivityBeforeMigration(String activityId) {
    return getSingleExecutionIdForActivity(snapshotBeforeMigration.getActivityTree(), activityId);
  }

  public String getSingleExecutionIdForActivityAfterMigration(String activityId) {
    return getSingleExecutionIdForActivity(snapshotAfterMigration.getActivityTree(), activityId);
  }

  public ActivityInstance getSingleActivityInstance(ActivityInstance tree, String activityId) {
    ActivityInstance[] activityInstances = tree.getActivityInstances(activityId);
    if (activityInstances.length == 1) {
      return activityInstances[0];
    }
    else {
      throw new RuntimeException("There is not exactly one activity instance for activity %s".formatted(activityId));
    }
  }

  public ActivityInstance getSingleActivityInstanceBeforeMigration(String activityId) {
    return getSingleActivityInstance(snapshotBeforeMigration.getActivityTree(), activityId);
  }

  public ActivityInstance getSingleActivityInstanceAfterMigration(String activityId) {
    return getSingleActivityInstance(snapshotAfterMigration.getActivityTree(), activityId);
  }

  public ProcessInstanceSnapshot takeFullProcessInstanceSnapshot(ProcessInstance processInstance) {
    return takeProcessInstanceSnapshot(processInstance).full();
  }

  public ProcessInstanceSnapshotBuilder takeProcessInstanceSnapshot(ProcessInstance processInstance) {
    return new ProcessInstanceSnapshotBuilder(processInstance, processEngine);
  }

  public ProcessInstance createProcessInstanceAndMigrate(MigrationPlan migrationPlan) {
    ProcessInstance processInstance = processEngine.getRuntimeService()
      .startProcessInstanceById(migrationPlan.getSourceProcessDefinitionId());

    migrateProcessInstance(migrationPlan, processInstance);
    return processInstance;
  }

  public ProcessInstance createProcessInstanceAndMigrate(MigrationPlan migrationPlan, Map<String, Object> variables) {
    ProcessInstance processInstance = processEngine.getRuntimeService()
        .startProcessInstanceById(migrationPlan.getSourceProcessDefinitionId(), variables);

    migrateProcessInstance(migrationPlan, processInstance);
    return processInstance;
  }

  public void migrateProcessInstance(MigrationPlan migrationPlan, ProcessInstance processInstance) {
    snapshotBeforeMigration = takeFullProcessInstanceSnapshot(processInstance);

    RuntimeService runtimeService = processEngine.getRuntimeService();

    runtimeService
      .newMigration(migrationPlan).processInstanceIds(Collections.singletonList(snapshotBeforeMigration.getProcessInstanceId())).execute();

    // fetch updated process instance
    processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();

    snapshotAfterMigration = takeFullProcessInstanceSnapshot(processInstance);
  }

  public void triggerTimer() {
    Job job = assertTimerJobExists(snapshotAfterMigration);
    processEngine.getManagementService().executeJob(job.getId());
  }

  public ExecutionAssert assertExecutionTreeAfterMigration() {
    return ExecutionAssert.assertThat(snapshotAfterMigration.getExecutionTree());
  }

  public ActivityInstanceAssertThatClause assertActivityTreeAfterMigration() {
    return assertThat(snapshotAfterMigration.getActivityTree());
  }

  public void assertEventSubscriptionsMigrated(String activityIdBefore, String activityIdAfter, String eventName) {
    List<EventSubscription> eventSubscriptionsBefore = snapshotBeforeMigration.getEventSubscriptionsForActivityIdAndEventName(activityIdAfter, eventName);

    for (EventSubscription eventSubscription : eventSubscriptionsBefore) {
      assertEventSubscriptionMigrated(eventSubscription, activityIdAfter, eventName);
    }
  }

  protected void assertEventSubscriptionMigrated(EventSubscription eventSubscriptionBefore, String activityIdAfter, String eventName) {
    EventSubscription eventSubscriptionAfter = snapshotAfterMigration.getEventSubscriptionById(eventSubscriptionBefore.getId());
    assertThat(eventSubscriptionAfter)
        .as("Expected that an event subscription with id '%s' exists after migration".formatted(eventSubscriptionBefore.getId()))
        .isNotNull();

    assertThat(eventSubscriptionAfter.getEventType()).isEqualTo(eventSubscriptionBefore.getEventType());
    assertThat(eventSubscriptionAfter.getActivityId()).isEqualTo(activityIdAfter);
    assertThat(eventSubscriptionAfter.getEventName()).isEqualTo(eventName);
  }


  public void assertEventSubscriptionMigrated(String activityIdBefore, String activityIdAfter, String eventName) {
    EventSubscription eventSubscriptionBefore = snapshotBeforeMigration.getEventSubscriptionForActivityIdAndEventName(activityIdBefore, eventName);
    assertThat(eventSubscriptionBefore)
        .as("Expected that an event subscription for activity '%s' exists before migration".formatted(activityIdBefore))
        .isNotNull();

    assertEventSubscriptionMigrated(eventSubscriptionBefore, activityIdAfter, eventName);
  }

  public void assertEventSubscriptionMigrated(String activityIdBefore, String eventNameBefore, String activityIdAfter, String eventNameAfter) {
    EventSubscription eventSubscriptionBefore = snapshotBeforeMigration.getEventSubscriptionForActivityIdAndEventName(activityIdBefore, eventNameBefore);
    assertThat(eventSubscriptionBefore)
        .as("Expected that an event subscription for activity '%s' exists before migration".formatted(activityIdBefore))
        .isNotNull();

    assertEventSubscriptionMigrated(eventSubscriptionBefore, activityIdAfter, eventNameAfter);
  }

  public void assertEventSubscriptionRemoved(String activityId, String eventName) {
    EventSubscription eventSubscriptionBefore = snapshotBeforeMigration.getEventSubscriptionForActivityIdAndEventName(activityId, eventName);
    assertThat(eventSubscriptionBefore)
        .as("Expected an event subscription for activity '%s' before the migration".formatted(activityId))
        .isNotNull();

    for (EventSubscription eventSubscription : snapshotAfterMigration.getEventSubscriptions()) {
      if (eventSubscriptionBefore.getId().equals(eventSubscription.getId())) {
        fail("Expected event subscription '%s' to be removed after migration".formatted(eventSubscriptionBefore.getId()));
      }
    }
  }

  public void assertEventSubscriptionCreated(String activityId, String eventName) {
    EventSubscription eventSubscriptionAfter = snapshotAfterMigration.getEventSubscriptionForActivityIdAndEventName(activityId, eventName);
    assertThat(eventSubscriptionAfter)
        .as("Expected an event subscription for activity '%s' after the migration".formatted(activityId))
        .isNotNull();

    for (EventSubscription eventSubscription : snapshotBeforeMigration.getEventSubscriptions()) {
      if (eventSubscriptionAfter.getId().equals(eventSubscription.getId())) {
        fail("Expected event subscription '%s' to be created after migration".formatted(eventSubscriptionAfter.getId()));
      }
    }
  }

  public void assertTimerJob(Job job) {
    assertThat(((JobEntity) job).getType()).as("Expected job to be a timer job").isEqualTo(TimerEntity.TYPE);
  }

  public Job assertTimerJobExists(ProcessInstanceSnapshot snapshot) {
    List<Job> jobs = snapshot.getJobs();
    assertThat(jobs).hasSize(1);
    Job job = jobs.get(0);
    assertTimerJob(job);
    return job;
  }

  public void assertJobCreated(String activityId, String handlerType) {
    JobDefinition jobDefinitionAfter = snapshotAfterMigration.getJobDefinitionForActivityIdAndType(activityId, handlerType);
    assertThat(jobDefinitionAfter)
        .as("Expected that a job definition for activity '%s' exists after migration".formatted(activityId))
        .isNotNull();

    Job jobAfter = snapshotAfterMigration.getJobForDefinitionId(jobDefinitionAfter.getId());
    assertThat(jobAfter)
        .as("Expected that a job for activity '%s' exists after migration".formatted(activityId))
        .isNotNull();
    assertTimerJob(jobAfter);
    assertThat(jobAfter.getProcessDefinitionId()).isEqualTo(jobDefinitionAfter.getProcessDefinitionId());
    assertThat(jobAfter.getProcessDefinitionKey()).isEqualTo(jobDefinitionAfter.getProcessDefinitionKey());

    for (Job job : snapshotBeforeMigration.getJobs()) {
      if (jobAfter.getId().equals(job.getId())) {
        fail("Expected job '%s' to be created first after migration".formatted(jobAfter.getId()));
      }
    }
  }

  public void assertJobsCreated(String activityId, String handlerType, int countJobs) {
    List<JobDefinition> jobDefinitionsAfter = snapshotAfterMigration.getJobDefinitionsForActivityIdAndType(activityId, handlerType);
    assertThat(jobDefinitionsAfter)
        .as("Expected that %sjob definitions for activity '%s' exist after migration, but found ".formatted(countJobs, activityId, jobDefinitionsAfter.size()))
        .hasSize(countJobs);

    for (JobDefinition jobDefinitionAfter : jobDefinitionsAfter) {
      Job jobAfter = snapshotAfterMigration.getJobForDefinitionId(jobDefinitionAfter.getId());
      assertThat(jobAfter)
          .as("Expected that a job for activity '%s' exists after migration".formatted(activityId))
          .isNotNull();
      assertTimerJob(jobAfter);
      assertThat(jobAfter.getProcessDefinitionId()).isEqualTo(jobDefinitionAfter.getProcessDefinitionId());
      assertThat(jobAfter.getProcessDefinitionKey()).isEqualTo(jobDefinitionAfter.getProcessDefinitionKey());

      for (Job job : snapshotBeforeMigration.getJobs()) {
        if (jobAfter.getId().equals(job.getId())) {
          fail("Expected job '%s' to be created first after migration".formatted(jobAfter.getId()));
        }
      }
    }
  }

  public void assertJobRemoved(String activityId, String handlerType) {
    JobDefinition jobDefinitionBefore = snapshotBeforeMigration.getJobDefinitionForActivityIdAndType(activityId, handlerType);
    assertThat(jobDefinitionBefore)
        .as("Expected that a job definition for activity '%s' exists before migration".formatted(activityId))
        .isNotNull();

    Job jobBefore = snapshotBeforeMigration.getJobForDefinitionId(jobDefinitionBefore.getId());
    assertThat(jobBefore)
        .as("Expected that a job for activity '%s' exists before migration".formatted(activityId))
        .isNotNull();
    assertTimerJob(jobBefore);

    for (Job job : snapshotAfterMigration.getJobs()) {
      if (jobBefore.getId().equals(job.getId())) {
        fail("Expected job '%s' to be removed after migration".formatted(jobBefore.getId()));
      }
    }
  }

  public void assertJobMigrated(String activityIdBefore, String activityIdAfter, String handlerType) {
    assertJobMigrated(activityIdBefore, activityIdAfter, handlerType, null);
  }

  public void assertJobMigrated(String activityIdBefore, String activityIdAfter, String handlerType, Date dueDateAfter) {
    JobDefinition jobDefinitionBefore = snapshotBeforeMigration.getJobDefinitionForActivityIdAndType(activityIdBefore, handlerType);
    assertThat(jobDefinitionBefore)
        .as("Expected that a job definition for activity '%s' exists before migration".formatted(activityIdBefore))
        .isNotNull();

    Job jobBefore = snapshotBeforeMigration.getJobForDefinitionId(jobDefinitionBefore.getId());
    assertThat(jobBefore)
        .as("Expected that a timer job for activity '%s' exists before migration".formatted(activityIdBefore))
        .isNotNull();

    assertJobMigrated(jobBefore, activityIdAfter, dueDateAfter == null ? jobBefore.getDuedate() : dueDateAfter);
  }

  public void assertJobMigrated(Job jobBefore, String activityIdAfter) {
    assertJobMigrated(jobBefore, activityIdAfter, jobBefore.getDuedate());
  }

  public void assertJobMigrated(Job jobBefore, String activityIdAfter, Date dueDateAfter) {

    Job jobAfter = snapshotAfterMigration.getJobById(jobBefore.getId());
    assertThat(jobAfter)
        .as("Expected that a job with id '%s' exists after migration".formatted(jobBefore.getId()))
        .isNotNull();

    JobDefinition jobDefinitionAfter = snapshotAfterMigration.getJobDefinitionForActivityIdAndType(activityIdAfter, ((JobEntity) jobBefore).getJobHandlerType());
    assertThat(jobDefinitionAfter)
        .as("Expected that a job definition for activity '%s' exists after migration".formatted(activityIdAfter))
        .isNotNull();

    assertThat(jobAfter.getId()).isEqualTo(jobBefore.getId());
    assertThat(jobAfter.getJobDefinitionId())
        .as("Expected that job is assigned to job definition '%s' after migration".formatted(jobDefinitionAfter.getId()))
        .isEqualTo(jobDefinitionAfter.getId());
    assertThat(jobAfter.getDeploymentId())
        .as("Expected that job is assigned to deployment '%s' after migration".formatted(snapshotAfterMigration.getDeploymentId()))
        .isEqualTo(snapshotAfterMigration.getDeploymentId());
    assertThat(jobAfter.getDuedate()).isEqualTo(dueDateAfter);
    assertThat(((JobEntity) jobAfter).getType()).isEqualTo(((JobEntity) jobBefore).getType());
    assertThat(jobAfter.getPriority()).isEqualTo(jobBefore.getPriority());
    assertThat(jobAfter.getProcessDefinitionId()).isEqualTo(jobDefinitionAfter.getProcessDefinitionId());
    assertThat(jobAfter.getProcessDefinitionKey()).isEqualTo(jobDefinitionAfter.getProcessDefinitionKey());
  }

  public void assertBoundaryTimerJobCreated(String activityId) {
    assertJobCreated(activityId, TimerExecuteNestedActivityJobHandler.TYPE);
  }

  public void assertBoundaryTimerJobRemoved(String activityId) {
    assertJobRemoved(activityId, TimerExecuteNestedActivityJobHandler.TYPE);
  }

  public void assertBoundaryTimerJobMigrated(String activityIdBefore, String activityIdAfter) {
    assertJobMigrated(activityIdBefore, activityIdAfter, TimerExecuteNestedActivityJobHandler.TYPE);
  }

  public void assertIntermediateTimerJobCreated(String activityId) {
    assertJobCreated(activityId, TimerCatchIntermediateEventJobHandler.TYPE);
  }

  public void assertIntermediateTimerJobRemoved(String activityId) {
    assertJobRemoved(activityId, TimerCatchIntermediateEventJobHandler.TYPE);
  }

  public void assertIntermediateTimerJobMigrated(String activityIdBefore, String activityIdAfter) {
    assertJobMigrated(activityIdBefore, activityIdAfter, TimerCatchIntermediateEventJobHandler.TYPE);
  }

  public void assertEventSubProcessTimerJobCreated(String activityId) {
    assertJobCreated(activityId, TimerStartEventSubprocessJobHandler.TYPE);
  }

  public void assertEventSubProcessTimerJobRemoved(String activityId) {
    assertJobRemoved(activityId, TimerStartEventSubprocessJobHandler.TYPE);
  }

  public void assertTaskListenerTimerJobCreated(String activityId) {
    assertJobCreated(activityId, TimerTaskListenerJobHandler.TYPE);
  }

  public void assertTaskListenerTimerJobsCreated(String activityId, int countJobs) {
    assertJobsCreated(activityId, TimerTaskListenerJobHandler.TYPE, countJobs);
  }

  public void assertTaskListenerTimerJobRemoved(String activityId) {
    assertJobRemoved(activityId, TimerTaskListenerJobHandler.TYPE);
  }

  public void assertTaskListenerTimerJobMigrated(String activityIdBefore, String activityIdAfter) {
    assertJobMigrated(activityIdBefore, activityIdAfter, TimerTaskListenerJobHandler.TYPE);
  }

  public void assertTaskListenerTimerJobMigrated(String activityIdBefore, String activityIdAfter, Date dueDateAfter) {
    assertJobMigrated(activityIdBefore, activityIdAfter, TimerTaskListenerJobHandler.TYPE, dueDateAfter);
  }

  public void assertVariableMigratedToExecution(VariableInstance variableBefore, String executionId) {
    assertVariableMigratedToExecution(variableBefore, executionId, variableBefore.getActivityInstanceId());
  }

  public void assertVariableMigratedToExecution(VariableInstance variableBefore, String executionId, String activityInstanceId) {
    VariableInstance variableAfter = snapshotAfterMigration.getVariable(variableBefore.getId());

    assertThat(variableAfter)
        .as("Variable with id %s does not exist".formatted(variableBefore.getId()))
        .isNotNull();

    assertThat(variableAfter.getActivityInstanceId()).isEqualTo(activityInstanceId);
    assertThat(variableAfter.getCaseExecutionId()).isEqualTo(variableBefore.getCaseExecutionId());
    assertThat(variableAfter.getCaseInstanceId()).isEqualTo(variableBefore.getCaseInstanceId());
    assertThat(variableAfter.getErrorMessage()).isEqualTo(variableBefore.getErrorMessage());
    assertThat(variableAfter.getExecutionId()).isEqualTo(executionId);
    assertThat(variableAfter.getId()).isEqualTo(variableBefore.getId());
    assertThat(variableAfter.getName()).isEqualTo(variableBefore.getName());
    assertThat(variableAfter.getProcessInstanceId()).isEqualTo(variableBefore.getProcessInstanceId());
    assertThat(variableAfter.getTaskId()).isEqualTo(variableBefore.getTaskId());
    assertThat(variableAfter.getTenantId()).isEqualTo(variableBefore.getTenantId());
    assertThat(variableAfter.getTypeName()).isEqualTo(variableBefore.getTypeName());
    assertThat(variableAfter.getValue()).isEqualTo(variableBefore.getValue());
  }

  public void assertSuperExecutionOfCaseInstance(String caseInstanceId, String expectedSuperExecutionId) {
    CaseExecutionEntity calledInstance = (CaseExecutionEntity) processEngine.getCaseService()
        .createCaseInstanceQuery()
        .caseInstanceId(caseInstanceId)
        .singleResult();

    assertThat(calledInstance.getSuperExecutionId()).isEqualTo(expectedSuperExecutionId);
  }

  public void assertSuperExecutionOfProcessInstance(String processInstance, String expectedSuperExecutionId) {
    ExecutionEntity calledInstance = (ExecutionEntity) processEngine.getRuntimeService()
        .createProcessInstanceQuery()
        .processInstanceId(processInstance)
        .singleResult();

    assertThat(calledInstance.getSuperExecutionId()).isEqualTo(expectedSuperExecutionId);
  }

}
