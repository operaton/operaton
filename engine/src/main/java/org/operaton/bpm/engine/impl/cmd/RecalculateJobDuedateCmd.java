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
package org.operaton.bpm.engine.impl.cmd;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.context.ProcessApplicationContextUtil;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.jobexecutor.JobHandlerConfiguration;
import org.operaton.bpm.engine.impl.jobexecutor.TimerCatchIntermediateEventJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerDeclarationImpl;
import org.operaton.bpm.engine.impl.jobexecutor.TimerEventJobHandler.TimerJobConfiguration;
import org.operaton.bpm.engine.impl.jobexecutor.TimerExecuteNestedActivityJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerStartEventJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerStartEventSubprocessJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerTaskListenerJobHandler;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyChange;
import org.operaton.bpm.engine.impl.persistence.entity.TimerEntity;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotEmpty;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;


/**
 * @author Tobias Metzke
 */
public class RecalculateJobDuedateCmd implements Command<Void>, Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private final String jobId;
  private final boolean creationDateBased;

  public RecalculateJobDuedateCmd(String jobId, boolean creationDateBased) {
    ensureNotEmpty("The job id is mandatory", "jobId", jobId);
    this.jobId = jobId;
    this.creationDateBased = creationDateBased;
  }

  @Override
  public Void execute(final CommandContext commandContext) {
    final JobEntity job = commandContext.getJobManager().findJobById(jobId);
    ensureNotNull(NotFoundException.class, "No job found with id '%s'".formatted(jobId), "job", job);

    // allow timer jobs only
    checkJobType(job);

    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkUpdateJob(job);
    }

    // prepare recalculation
    final TimerDeclarationImpl timerDeclaration = findTimerDeclaration(commandContext, job);
    final TimerEntity timer = (TimerEntity) job;
    Date oldDuedate = job.getDuedate();
    Runnable runnable = () ->
        timerDeclaration.resolveAndSetDuedate(timer.getExecution(), timer, creationDateBased);

    // run recalculation in correct context
    ProcessDefinitionEntity contextDefinition = commandContext
        .getProcessEngineConfiguration()
        .getDeploymentCache()
        .findDeployedProcessDefinitionById(job.getProcessDefinitionId());
    ProcessApplicationContextUtil.doContextSwitch(runnable, contextDefinition);

    // log operation
    List<PropertyChange> propertyChanges = new ArrayList<>();
    propertyChanges.add(new PropertyChange("duedate", oldDuedate, job.getDuedate()));
    propertyChanges.add(new PropertyChange("creationDateBased", null, creationDateBased));
    commandContext.getOperationLogManager().logJobOperation(UserOperationLogEntry.OPERATION_TYPE_RECALC_DUEDATE, jobId,
        job.getJobDefinitionId(), job.getProcessInstanceId(), job.getProcessDefinitionId(), job.getProcessDefinitionKey(),
        propertyChanges);

    return null;
  }

  protected void checkJobType(JobEntity job) {
    String type = job.getJobHandlerType();
    if (!(TimerExecuteNestedActivityJobHandler.TYPE.equals(type) ||
        TimerCatchIntermediateEventJobHandler.TYPE.equals(type) ||
        TimerStartEventJobHandler.TYPE.equals(type) ||
        TimerStartEventSubprocessJobHandler.TYPE.equals(type) ||
        TimerTaskListenerJobHandler.TYPE.equals(type)) ||
        !(job instanceof TimerEntity)) {
      throw new ProcessEngineException("Only timer jobs can be recalculated, but the job with id '%s' is of type '%s'.".formatted(jobId, type));
    }
  }

  protected TimerDeclarationImpl findTimerDeclaration(CommandContext commandContext, JobEntity job) {
    TimerDeclarationImpl timerDeclaration = null;
    if (job.getExecutionId() != null) {
      // timeout listener or boundary / intermediate / subprocess start event
      timerDeclaration = findTimerDeclarationForActivity(commandContext, job);
    } else {
      // process instance start event
      timerDeclaration = findTimerDeclarationForProcessStartEvent(commandContext, job);
    }

    if (timerDeclaration == null) {
      throw new ProcessEngineException("No timer declaration found for job id '%s'.".formatted(jobId));
    }
    return timerDeclaration;
  }

  protected TimerDeclarationImpl findTimerDeclarationForActivity(CommandContext commandContext, JobEntity job) {
    ExecutionEntity execution = commandContext.getExecutionManager().findExecutionById(job.getExecutionId());
    if (execution == null) {
      throw new ProcessEngineException("No execution found with id '%s' for job id '%s'".formatted(job.getExecutionId(), jobId));
    }
    ActivityImpl activity = execution.getProcessDefinition().findActivity(job.getActivityId());
    if (activity != null) {
      if (TimerTaskListenerJobHandler.TYPE.equals(job.getJobHandlerType())) {
        return findTimeoutListenerDeclaration(job, activity);
      }
      Map<String, TimerDeclarationImpl> timerDeclarations = TimerDeclarationImpl.getDeclarationsForScope(activity.getEventScope());
      if (!timerDeclarations.isEmpty() && timerDeclarations.containsKey(job.getActivityId())) {
        return  timerDeclarations.get(job.getActivityId());
      }
    }
    return null;
  }

  protected TimerDeclarationImpl findTimeoutListenerDeclaration(JobEntity job, ActivityImpl activity) {
    Map<String, Map<String, TimerDeclarationImpl>> timeoutDeclarations = TimerDeclarationImpl.getTimeoutListenerDeclarationsForScope(activity.getEventScope());
    if (!timeoutDeclarations.isEmpty()) {
      Map<String, TimerDeclarationImpl> activityTimeouts = timeoutDeclarations.get(job.getActivityId());
      if (activityTimeouts != null && !activityTimeouts.isEmpty()) {
        JobHandlerConfiguration jobHandlerConfiguration = job.getJobHandlerConfiguration();
        if (jobHandlerConfiguration instanceof TimerJobConfiguration timerJobConfiguration) {
          return activityTimeouts.get(timerJobConfiguration.getTimerElementSecondaryKey());
        }
      }
    }
    return null;
  }

  protected TimerDeclarationImpl findTimerDeclarationForProcessStartEvent(CommandContext commandContext, JobEntity job) {
    ProcessDefinitionEntity processDefinition = commandContext.getProcessEngineConfiguration().getDeploymentCache().findDeployedProcessDefinitionById(job.getProcessDefinitionId());
    @SuppressWarnings("unchecked")
    List<TimerDeclarationImpl> timerDeclarations = (List<TimerDeclarationImpl>) processDefinition.getProperty(BpmnParse.PROPERTYNAME_START_TIMER);
    for (TimerDeclarationImpl timerDeclarationCandidate : timerDeclarations) {
      if (timerDeclarationCandidate.getJobDefinitionId().equals(job.getJobDefinitionId())) {
        return timerDeclarationCandidate;
      }
    }
    return null;
  }
}
