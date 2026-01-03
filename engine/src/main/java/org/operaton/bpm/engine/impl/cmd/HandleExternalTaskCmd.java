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

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ExternalTaskEntity;
import org.operaton.bpm.engine.impl.util.EnsureUtil;

/**
 * Represents an abstract class for the handle of external task commands.
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
public abstract class HandleExternalTaskCmd extends ExternalTaskCmd {

  /**
   * The reported worker id.
   */
  protected String workerId;

  protected HandleExternalTaskCmd(String externalTaskId, String workerId) {
    super(externalTaskId);
    this.workerId = workerId;
  }

  @Override
  public Void execute(CommandContext commandContext) {
    validateInput();

    ExternalTaskEntity externalTask = commandContext.getExternalTaskManager().findExternalTaskById(externalTaskId);
    EnsureUtil.ensureNotNull(NotFoundException.class,
        "Cannot find external task with id %s".formatted(externalTaskId), "externalTask", externalTask);

    if (validateWorkerViolation(externalTask)) {
      throw new BadUserRequestException("%s. It is locked by worker '%s'.".formatted(getErrorMessageOnWrongWorkerAccess(), externalTask.getWorkerId()));
    }

    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkUpdateProcessInstanceById(externalTask.getProcessInstanceId());
    }

    try {
      execute(externalTask);
    } catch (NotFoundException e) {
      // wrap up NotFoundExceptions reported for entities different than external tasks
      throw new ProcessEngineException(e.getMessage(), e);
    }

    return null;
  }

  /**
   * Returns the error message. Which is used to create an specific message
   *  for the BadUserRequestException if an worker has no rights to execute commands of the external task.
   *
   * @return the specific error message
   */
  public abstract String getErrorMessageOnWrongWorkerAccess();

  /**
   * Validates the current input of the command.
   */
  @Override
  protected void validateInput() {
    EnsureUtil.ensureNotNull("workerId", workerId);
  }

  /**
   * Validates the caller's workerId against the workerId of the external task.
   */
  protected boolean validateWorkerViolation(ExternalTaskEntity externalTask) {
    return !workerId.equals(externalTask.getWorkerId());
  }
}
