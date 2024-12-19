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
package org.operaton.bpm.engine.impl.jobexecutor;

import java.util.Date;

import org.operaton.bpm.engine.impl.cfg.TransactionListener;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.TimerEventJobHandler.TimerJobConfiguration;
import org.operaton.bpm.engine.impl.persistence.entity.TimerEntity;

/**
 * @author Roman Smirnov
 *
 */
public class RepeatingFailedJobListener implements TransactionListener {

  protected CommandExecutor commandExecutor;
  protected String jobId;

  public RepeatingFailedJobListener(CommandExecutor commandExecutor, String jobId) {
    this.commandExecutor = commandExecutor;
    this.jobId = jobId;
  }

  @Override
  public void execute(CommandContext commandContext) {
    CreateNewTimerJobCommand cmd = new CreateNewTimerJobCommand(jobId);
    commandExecutor.execute(cmd);
  }

  protected class CreateNewTimerJobCommand implements Command<Void> {

    protected String jobId;

    public CreateNewTimerJobCommand(String jobId) {
      this.jobId = jobId;
    }

    @Override
    public Void execute(CommandContext commandContext) {

      TimerEntity failedJob = (TimerEntity) commandContext
          .getJobManager()
          .findJobById(jobId);

      Date newDueDate = failedJob.calculateNewDueDate();

      if (newDueDate != null) {
        failedJob.createNewTimerJob(newDueDate);

        // update configuration of failed job
        TimerJobConfiguration config = (TimerJobConfiguration) failedJob.getJobHandlerConfiguration();
        config.setFollowUpJobCreated(true);
        failedJob.setJobHandlerConfiguration(config);
      }

      return null;
    }

  }

}
