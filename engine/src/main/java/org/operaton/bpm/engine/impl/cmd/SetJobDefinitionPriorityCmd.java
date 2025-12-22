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

import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.oplog.UserOperationLogContext;
import org.operaton.bpm.engine.impl.oplog.UserOperationLogContextEntry;
import org.operaton.bpm.engine.impl.oplog.UserOperationLogContextEntryBuilder;
import org.operaton.bpm.engine.impl.persistence.entity.JobDefinitionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyChange;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * @author Thorben Lindhauer
 *
 */
public class SetJobDefinitionPriorityCmd implements Command<Void> {

  public static final String JOB_DEFINITION_OVERRIDING_PRIORITY = "overridingPriority";

  protected String jobDefinitionId;
  protected Long priority;
  protected boolean cascade;

  public SetJobDefinitionPriorityCmd(String jobDefinitionId, Long priority, boolean cascade) {
    this.jobDefinitionId = jobDefinitionId;
    this.priority = priority;
    this.cascade = cascade;
  }

  @Override
  public Void execute(CommandContext commandContext) {
    ensureNotNull(NotValidException.class, "jobDefinitionId", jobDefinitionId);

    JobDefinitionEntity jobDefinition = commandContext.getJobDefinitionManager().findById(jobDefinitionId);

    ensureNotNull(NotFoundException.class,
        "Job definition with id '" + jobDefinitionId + "' does not exist",
        "jobDefinition",
        jobDefinition);

    checkUpdateProcess(commandContext, jobDefinition);

    Long currentPriority = jobDefinition.getOverridingJobPriority();
    jobDefinition.setJobPriority(priority);

    UserOperationLogContext opLogContext = new UserOperationLogContext();
    createJobDefinitionOperationLogEntry(opLogContext, currentPriority, jobDefinition);

    if (cascade && priority != null) {
      commandContext.getJobManager().updateJobPriorityByDefinitionId(jobDefinitionId, priority);
      createCascadeJobsOperationLogEntry(opLogContext, jobDefinition);
    }

    commandContext.getOperationLogManager().logUserOperations(opLogContext);

    return null;
  }

  protected void checkUpdateProcess(CommandContext commandContext, JobDefinitionEntity jobDefinition) {

    String processDefinitionId = jobDefinition.getProcessDefinitionId();

    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkUpdateProcessDefinitionById(processDefinitionId);

      if (cascade) {
        checker.checkUpdateProcessInstanceByProcessDefinitionId(processDefinitionId);
      }
    }
  }

  protected void createJobDefinitionOperationLogEntry(UserOperationLogContext opLogContext, Long previousPriority,
      JobDefinitionEntity jobDefinition) {

    PropertyChange propertyChange = new PropertyChange(
        JOB_DEFINITION_OVERRIDING_PRIORITY, previousPriority, jobDefinition.getOverridingJobPriority());

    UserOperationLogContextEntry entry = UserOperationLogContextEntryBuilder
        .entry(UserOperationLogEntry.OPERATION_TYPE_SET_PRIORITY, EntityTypes.JOB_DEFINITION)
        .inContextOf(jobDefinition)
        .propertyChanges(propertyChange)
        .category(UserOperationLogEntry.CATEGORY_OPERATOR)
        .create();

    opLogContext.addEntry(entry);
  }

  protected void createCascadeJobsOperationLogEntry(UserOperationLogContext opLogContext, JobDefinitionEntity jobDefinition) {
    // old value is unknown
    PropertyChange propertyChange = new PropertyChange(
        SetJobPriorityCmd.JOB_PRIORITY_PROPERTY, null, jobDefinition.getOverridingJobPriority());

    UserOperationLogContextEntry entry = UserOperationLogContextEntryBuilder
        .entry(UserOperationLogEntry.OPERATION_TYPE_SET_PRIORITY, EntityTypes.JOB)
        .inContextOf(jobDefinition)
        .propertyChanges(propertyChange)
        .category(UserOperationLogEntry.CATEGORY_OPERATOR)
        .create();

    opLogContext.addEntry(entry);
  }

}
