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
package org.operaton.bpm.engine.impl.migration.instance.parser;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.impl.ActivityExecutionTreeMapping;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.migration.instance.MigratingActivityInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingEventScopeInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingExternalTaskInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingJobInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingProcessElementInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingProcessInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingScopeInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingTransitionInstance;
import org.operaton.bpm.engine.impl.migration.validation.instance.MigratingProcessInstanceValidationReportImpl;
import org.operaton.bpm.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ExternalTaskEntity;
import org.operaton.bpm.engine.impl.persistence.entity.IncidentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobDefinitionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.impl.persistence.entity.VariableInstanceEntity;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ProcessDefinitionImpl;
import org.operaton.bpm.engine.impl.util.StringUtil;
import org.operaton.bpm.engine.migration.MigrationInstruction;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.runtime.TransitionInstance;
import org.operaton.commons.utils.CollectionUtil;

/**
 * @author Thorben Lindhauer
 *
 */
public class MigratingInstanceParseContext {

  protected MigratingProcessInstance migratingProcessInstance;

  protected Map<String, MigratingActivityInstance> activityInstances = new HashMap<>();
  protected Map<String, MigratingEventScopeInstance> compensationInstances = new HashMap<>();
  protected Map<String, MigratingJobInstance> migratingJobs = new HashMap<>();
  protected Map<String, MigratingExternalTaskInstance> migratingExternalTasks = new HashMap<>();

  protected Collection<EventSubscriptionEntity> eventSubscriptions;
  protected Collection<IncidentEntity> incidents;
  protected Collection<JobEntity> jobs;
  protected Collection<TaskEntity> tasks;
  protected Collection<ExternalTaskEntity> externalTasks;
  protected Collection<VariableInstanceEntity> variables;

  protected ProcessDefinitionEntity sourceProcessDefinition;
  protected ProcessDefinitionEntity targetProcessDefinition;
  protected Map<String, List<JobDefinitionEntity>> targetJobDefinitions;
  protected ActivityExecutionTreeMapping mapping;
  protected Map<String, List<MigrationInstruction>> instructionsBySourceScope;

  protected MigratingInstanceParser parser;

  public MigratingInstanceParseContext(
      MigratingInstanceParser parser,
      MigrationPlan migrationPlan,
      ExecutionEntity processInstance,
      ProcessDefinitionEntity targetProcessDefinition) {
    this.parser = parser;
    this.sourceProcessDefinition = processInstance.getProcessDefinition();
    this.targetProcessDefinition = targetProcessDefinition;
    this.migratingProcessInstance = new MigratingProcessInstance(processInstance.getId(), sourceProcessDefinition, targetProcessDefinition);
    this.mapping = new ActivityExecutionTreeMapping(Context.getCommandContext(), processInstance.getId());
    this.instructionsBySourceScope = organizeInstructionsBySourceScope(migrationPlan);
  }

  public MigratingInstanceParseContext jobs(Collection<JobEntity> jobs) {
    this.jobs = new HashSet<>(jobs);
    return this;
  }

  public MigratingInstanceParseContext incidents(Collection<IncidentEntity> incidents) {
    this.incidents = new HashSet<>(incidents);
    return this;
  }

  public MigratingInstanceParseContext tasks(Collection<TaskEntity> tasks) {
    this.tasks = new HashSet<>(tasks);
    return this;
  }

  public MigratingInstanceParseContext externalTasks(Collection<ExternalTaskEntity> externalTasks) {
    this.externalTasks = new HashSet<>(externalTasks);
    return this;
  }

  public MigratingInstanceParseContext eventSubscriptions(Collection<EventSubscriptionEntity> eventSubscriptions) {
    this.eventSubscriptions = new HashSet<>(eventSubscriptions);
    return this;
  }

  public MigratingInstanceParseContext targetJobDefinitions(Collection<JobDefinitionEntity> jobDefinitions) {
    this.targetJobDefinitions = new HashMap<>();

    for (JobDefinitionEntity jobDefinition : jobDefinitions) {
      CollectionUtil.addToMapOfLists(this.targetJobDefinitions, jobDefinition.getActivityId(), jobDefinition);
    }
    return this;
  }

  public MigratingInstanceParseContext variables(Collection<VariableInstanceEntity> variables) {
    this.variables = new HashSet<>(variables);
    return this;
  }

  public void submit(MigratingActivityInstance activityInstance) {
    activityInstances.put(activityInstance.getActivityInstance().getId(), activityInstance);
  }

  public void submit(MigratingEventScopeInstance compensationInstance) {
    ExecutionEntity scopeExecution = compensationInstance.resolveRepresentativeExecution();
    if (scopeExecution != null) {
      compensationInstances.put(scopeExecution.getId(), compensationInstance);
    }
  }

  public void submit(MigratingJobInstance job) {
    migratingJobs.put(job.getJobEntity().getId(), job);
  }

  public void submit(MigratingExternalTaskInstance externalTask) {
    migratingExternalTasks.put(externalTask.getId(), externalTask);
  }

  public void consume(TaskEntity task) {
    tasks.remove(task);
  }

  public void consume(ExternalTaskEntity externalTask) {
    externalTasks.remove(externalTask);
  }

  public void consume(IncidentEntity incident) {
    incidents.remove(incident);
  }

  public void consume(JobEntity job) {
    jobs.remove(job);
  }

  public void consume(EventSubscriptionEntity eventSubscription) {
    eventSubscriptions.remove(eventSubscription);
  }

  public void consume(VariableInstanceEntity variableInstance) {
    variables.remove(variableInstance);
  }

  public MigratingProcessInstance getMigratingProcessInstance() {
    return migratingProcessInstance;
  }

  public Collection<MigratingActivityInstance> getMigratingActivityInstances() {
    return activityInstances.values();
  }

  public ProcessDefinitionImpl getSourceProcessDefinition() {
    return sourceProcessDefinition;
  }

  public ProcessDefinitionImpl getTargetProcessDefinition() {
    return targetProcessDefinition;
  }

  public ActivityImpl getTargetActivity(MigrationInstruction instruction) {
    if (instruction != null) {
      return targetProcessDefinition.findActivity(instruction.getTargetActivityId());
    }
    else {
      return null;
    }
  }

  public JobDefinitionEntity getTargetJobDefinition(String activityId, String jobHandlerType) {
    List<JobDefinitionEntity> jobDefinitionsForActivity = targetJobDefinitions.get(activityId);

    if (jobDefinitionsForActivity != null) {
      for (JobDefinitionEntity jobDefinition : jobDefinitionsForActivity) {
        if (jobHandlerType.equals(jobDefinition.getJobType())) {
          // assuming there is no more than one job definition per pair of activity and type
          return jobDefinition;
        }
      }
    }

    return null;
  }

  public ActivityExecutionTreeMapping getMapping() {
    return mapping;
  }

  // TODO: conditions would go here
  public MigrationInstruction getInstructionFor(String scopeId) {
    List<MigrationInstruction> instructions = instructionsBySourceScope.get(scopeId);

    if (instructions == null || instructions.isEmpty()) {
      return null;
    }
    else {
      return instructions.get(0);
    }
  }

  public MigratingActivityInstance getMigratingActivityInstanceById(String activityInstanceId) {
    return activityInstances.get(activityInstanceId);
  }

  public MigratingScopeInstance getMigratingCompensationInstanceByExecutionId(String id) {
    return compensationInstances.get(id);
  }

  public MigratingJobInstance getMigratingJobInstanceById(String jobId) {
    return migratingJobs.get(jobId);
  }

  public MigratingExternalTaskInstance getMigratingExternalTaskInstanceById(String externalTaskId) {
    return migratingExternalTasks.get(externalTaskId);
  }

  public MigrationInstruction findSingleMigrationInstruction(String sourceScopeId) {
    List<MigrationInstruction> instructions = instructionsBySourceScope.get(sourceScopeId);

    if (instructions != null && !instructions.isEmpty()) {
      return instructions.get(0);
    }
    else {
      return null;
    }

  }

  protected Map<String, List<MigrationInstruction>> organizeInstructionsBySourceScope(MigrationPlan migrationPlan) {
    Map<String, List<MigrationInstruction>> organizedInstructions = new HashMap<>();

    for (MigrationInstruction instruction : migrationPlan.getInstructions()) {
      CollectionUtil.addToMapOfLists(organizedInstructions, instruction.getSourceActivityId(), instruction);
    }

    return organizedInstructions;
  }

  public void handleDependentActivityInstanceJobs(MigratingActivityInstance migratingInstance, List<JobEntity> jobs) {
    parser.getDependentActivityInstanceJobHandler().handle(this, migratingInstance, jobs);
  }

  public void handleDependentTransitionInstanceJobs(MigratingTransitionInstance migratingInstance, List<JobEntity> jobs) {
    parser.getDependentTransitionInstanceJobHandler().handle(this, migratingInstance, jobs);
  }

  public void handleDependentEventSubscriptions(MigratingActivityInstance migratingInstance, List<EventSubscriptionEntity> eventSubscriptions) {
    parser.getDependentEventSubscriptionHandler().handle(this, migratingInstance, eventSubscriptions);
  }

  public void handleDependentVariables(MigratingProcessElementInstance migratingInstance, List<VariableInstanceEntity> variables) {
    parser.getDependentVariablesHandler().handle(this, migratingInstance, variables);
  }

  public void handleTransitionInstance(TransitionInstance transitionInstance) {
    parser.getTransitionInstanceHandler().handle(this, transitionInstance);
  }

  public void validateNoEntitiesLeft(MigratingProcessInstanceValidationReportImpl processInstanceReport) {
    processInstanceReport.setProcessInstanceId(migratingProcessInstance.getProcessInstanceId());

    ensureNoEntitiesAreLeft("tasks", tasks, processInstanceReport);
    ensureNoEntitiesAreLeft("externalTask", externalTasks, processInstanceReport);
    ensureNoEntitiesAreLeft("incidents", incidents, processInstanceReport);
    ensureNoEntitiesAreLeft("jobs", jobs, processInstanceReport);
    ensureNoEntitiesAreLeft("event subscriptions", eventSubscriptions, processInstanceReport);
    ensureNoEntitiesAreLeft("variables", variables, processInstanceReport);
  }

  public void ensureNoEntitiesAreLeft(String entityName, Collection<? extends DbEntity> dbEntities, MigratingProcessInstanceValidationReportImpl processInstanceReport) {
    if (!dbEntities.isEmpty()) {
      processInstanceReport.addFailure("Process instance contains not migrated " + entityName + ": [" + StringUtil.joinDbEntityIds(dbEntities) + "]");
    }
  }



}
