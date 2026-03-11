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

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.jobexecutor.JobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.JobHandlerConfiguration;
import org.operaton.bpm.engine.impl.jobexecutor.TimerChangeJobDefinitionSuspensionStateJobHandler.JobDefinitionSuspensionStateConfiguration;
import org.operaton.bpm.engine.impl.management.UpdateJobDefinitionSuspensionStateBuilderImpl;
import org.operaton.bpm.engine.impl.management.UpdateJobSuspensionStateBuilderImpl;
import org.operaton.bpm.engine.impl.persistence.entity.*;

/**
 * @author Daniel Meyer
 * @author roman.smirnov
 */
public abstract class AbstractSetJobDefinitionStateCmd extends AbstractSetStateCmd {

  protected String jobDefinitionId;
  protected String processDefinitionId;
  protected String processDefinitionKey;

  protected String processDefinitionTenantId;
  protected boolean isProcessDefinitionTenantIdSet;

  protected AbstractSetJobDefinitionStateCmd(UpdateJobDefinitionSuspensionStateBuilderImpl builder) {
    super(
        builder.isIncludeJobs(),
        builder.getExecutionDate());

    this.jobDefinitionId = builder.getJobDefinitionId();
    this.processDefinitionId = builder.getProcessDefinitionId();
    this.processDefinitionKey = builder.getProcessDefinitionKey();

    this.isProcessDefinitionTenantIdSet = builder.isProcessDefinitionTenantIdSet();
    this.processDefinitionTenantId = builder.getProcessDefinitionTenantId();
  }

  @Override
  protected void checkParameters(CommandContext commandContext) {
    if (jobDefinitionId == null && processDefinitionId == null && processDefinitionKey == null) {
      throw new ProcessEngineException("Job definition id, process definition id nor process definition key cannot be null");
    }
  }

  @Override
  protected void checkAuthorization(CommandContext commandContext) {
    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      if (jobDefinitionId != null) {
        checkAuthorizationByJobDefinitionId(commandContext, checker);
      } else if (processDefinitionId != null) {
        checkAuthorizationByProcessDefinitionId(checker);
      } else if (processDefinitionKey != null) {
        checkAuthorizationByProcessDefinitionKey(checker);
      }
    }
  }

  private void checkAuthorizationByProcessDefinitionKey(CommandChecker checker) {
    checker.checkUpdateProcessDefinitionByKey(processDefinitionKey);
    if (includeSubResources) {
      checker.checkUpdateProcessInstanceByProcessDefinitionKey(processDefinitionKey);
    }
  }

  private void checkAuthorizationByProcessDefinitionId(CommandChecker checker) {
    checker.checkUpdateProcessDefinitionById(processDefinitionId);
    if (includeSubResources) {
      checker.checkUpdateProcessInstanceByProcessDefinitionId(processDefinitionId);
    }
  }

  private void checkAuthorizationByJobDefinitionId(CommandContext commandContext, CommandChecker checker) {
    JobDefinitionManager jobDefinitionManager = commandContext.getJobDefinitionManager();
    JobDefinitionEntity jobDefinition = jobDefinitionManager.findById(jobDefinitionId);

    if (jobDefinition != null && jobDefinition.getProcessDefinitionKey() != null) {
      String procDefKey = jobDefinition.getProcessDefinitionKey();
      checker.checkUpdateProcessDefinitionByKey(procDefKey);

      if (includeSubResources) {
        checker.checkUpdateProcessInstanceByProcessDefinitionKey(procDefKey);
      }
    }
  }

  @Override
  protected void updateSuspensionState(CommandContext commandContext, SuspensionState suspensionState) {
    JobDefinitionManager jobDefinitionManager = commandContext.getJobDefinitionManager();
    JobManager jobManager = commandContext.getJobManager();

    if (jobDefinitionId != null) {
      jobDefinitionManager.updateJobDefinitionSuspensionStateById(jobDefinitionId, suspensionState);

    } else if (processDefinitionId != null) {
      jobDefinitionManager.updateJobDefinitionSuspensionStateByProcessDefinitionId(processDefinitionId, suspensionState);
      jobManager.updateStartTimerJobSuspensionStateByProcessDefinitionId(processDefinitionId, suspensionState);

    } else if (processDefinitionKey != null) {

      if (!isProcessDefinitionTenantIdSet) {
        jobDefinitionManager.updateJobDefinitionSuspensionStateByProcessDefinitionKey(processDefinitionKey, suspensionState);
        jobManager.updateStartTimerJobSuspensionStateByProcessDefinitionKey(processDefinitionKey, suspensionState);

      } else {
        jobDefinitionManager.updateJobDefinitionSuspensionStateByProcessDefinitionKeyAndTenantId(processDefinitionKey, processDefinitionTenantId, suspensionState);
        jobManager.updateStartTimerJobSuspensionStateByProcessDefinitionKeyAndTenantId(processDefinitionKey, processDefinitionTenantId, suspensionState);
      }
    }
  }

  @Override
  protected JobHandlerConfiguration getJobHandlerConfiguration() {

    if (jobDefinitionId != null) {
      return JobDefinitionSuspensionStateConfiguration.byJobDefinitionId(jobDefinitionId, isIncludeSubResources());

    } else if (processDefinitionId != null) {
      return JobDefinitionSuspensionStateConfiguration.byProcessDefinitionId(processDefinitionId, isIncludeSubResources());

    } else {

      if (!isProcessDefinitionTenantIdSet) {
        return JobDefinitionSuspensionStateConfiguration.byProcessDefinitionKey(processDefinitionKey, isIncludeSubResources());

      } else {
        return JobDefinitionSuspensionStateConfiguration.byProcessDefinitionKeyAndTenantId(processDefinitionKey, processDefinitionTenantId, isIncludeSubResources());
      }
    }

  }

  @Override
  protected void logUserOperation(CommandContext commandContext) {
    PropertyChange propertyChange = new PropertyChange(SUSPENSION_STATE_PROPERTY, null, getNewSuspensionState().getName());
    commandContext.getOperationLogManager().logJobDefinitionOperation(getLogEntryOperation(), jobDefinitionId,
      processDefinitionId, processDefinitionKey, propertyChange);
  }

  protected UpdateJobSuspensionStateBuilderImpl createJobCommandBuilder() {
    UpdateJobSuspensionStateBuilderImpl builder = new UpdateJobSuspensionStateBuilderImpl();

    if (jobDefinitionId != null) {
      builder.byJobDefinitionId(jobDefinitionId);

    } else if (processDefinitionId != null) {
      builder.byProcessDefinitionId(processDefinitionId);

    } else if (processDefinitionKey != null) {
      builder.byProcessDefinitionKey(processDefinitionKey);

      if (isProcessDefinitionTenantIdSet && processDefinitionTenantId != null) {
        builder.processDefinitionTenantId(processDefinitionTenantId);

      } else if (isProcessDefinitionTenantIdSet) {
        builder.processDefinitionWithoutTenantId();
      }
    }
    return builder;
  }

  /**
   * Subclasses should return the type of the {@link JobHandler} here. it will be used when
   * the user provides an execution date on which the actual state change will happen.
   */
  @Override
  protected abstract String getDelayedExecutionJobHandlerType();

  @Override
  protected AbstractSetStateCmd getNextCommand() {
    UpdateJobSuspensionStateBuilderImpl jobCommandBuilder = createJobCommandBuilder();

    return getNextCommand(jobCommandBuilder);
  }

  @Override
  protected String getDeploymentId(CommandContext commandContext) {
    if (jobDefinitionId != null) {
      return getDeploymentIdByJobDefinition(commandContext, jobDefinitionId);
    } else if (processDefinitionId != null) {
      return getDeploymentIdByProcessDefinition(commandContext, processDefinitionId);
    } else if (processDefinitionKey != null) {
      return getDeploymentIdByProcessDefinitionKey(commandContext, processDefinitionKey, isProcessDefinitionTenantIdSet, processDefinitionTenantId);
    }
    return null;
  }

  protected abstract AbstractSetJobStateCmd getNextCommand(UpdateJobSuspensionStateBuilderImpl jobCommandBuilder);
}
