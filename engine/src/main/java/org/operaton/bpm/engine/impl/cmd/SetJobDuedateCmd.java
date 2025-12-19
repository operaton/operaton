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
import java.util.Collections;
import java.util.Date;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyChange;
import org.operaton.bpm.engine.impl.persistence.entity.TimerEntity;


/**
 * @author Kristin Polenz
 */
public class SetJobDuedateCmd implements Command<Void>, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final String jobId;
  private final Date newDuedate;
  private final boolean cascade;

  public SetJobDuedateCmd(String jobId, Date newDuedate, boolean cascade) {
    if (jobId == null || jobId.isEmpty()) {
      throw new ProcessEngineException("The job id is mandatory, but '%s' has been provided.".formatted(jobId));
    }
    this.jobId = jobId;
    this.newDuedate = newDuedate;
    this.cascade = cascade;
  }

  @Override
  public Void execute(CommandContext commandContext) {
    JobEntity job = commandContext
            .getJobManager()
            .findJobById(jobId);
    if (job != null) {

      for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
        checker.checkUpdateJob(job);
      }

      commandContext.getOperationLogManager().logJobOperation(UserOperationLogEntry.OPERATION_TYPE_SET_DUEDATE, jobId,
          job.getJobDefinitionId(), job.getProcessInstanceId(), job.getProcessDefinitionId(), job.getProcessDefinitionKey(),
          Collections.singletonList(new PropertyChange("duedate", job.getDuedate(), newDuedate)));

      // for timer jobs cascade due date changes
      if (cascade && newDuedate != null && job instanceof TimerEntity timerEntity) {
        long offset = newDuedate.getTime() - job.getDuedate().getTime();
        timerEntity.setRepeatOffset(timerEntity.getRepeatOffset() + offset);
      }

      job.setDuedate(newDuedate);
    } else {
      throw new ProcessEngineException("No job found with id '%s'.".formatted(jobId));
    }
    return null;
  }
}
