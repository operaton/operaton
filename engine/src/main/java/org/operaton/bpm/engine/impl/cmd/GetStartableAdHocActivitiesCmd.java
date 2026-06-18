/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.impl.bpmn.behavior.AdHocStartability;
import org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.runtime.AdHocActivityImpl;
import org.operaton.bpm.engine.runtime.AdHocActivity;

/**
 * Discovers activities that can currently be triggered in an active ad-hoc subprocess.
 */
public class GetStartableAdHocActivitiesCmd implements Command<List<AdHocActivity>>, Serializable {

  private static final long serialVersionUID = 1L;

  protected final String executionId;
  protected final AdHocStartability startability = AdHocStartability.INSTANCE;

  public GetStartableAdHocActivitiesCmd(String executionId) {
    this.executionId = executionId;
  }

  @Override
  public List<AdHocActivity> execute(CommandContext commandContext) {
    ensureNotNull(BadUserRequestException.class, "executionId is null", "executionId", executionId);

    ExecutionEntity execution = commandContext.getExecutionManager().findExecutionById(executionId);
    ensureNotNull(BadUserRequestException.class, "execution " + executionId + " doesn't exist", "execution", execution);

    for (CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkReadProcessInstance(execution);
    }

    ActivityImpl adHocActivity = execution.getActivity();
    ensureNotNull(BadUserRequestException.class, "execution " + executionId + " has no current activity", "activity", adHocActivity);

    if (!(adHocActivity.getActivityBehavior() instanceof AdHocSubProcessActivityBehavior)) {
      throw new BadUserRequestException("execution " + executionId + " is not waiting in an adHocSubProcess");
    }

    return startability.getStartableActivities(execution).stream()
        .map(AdHocActivityImpl::fromActivity)
        .collect(Collectors.toList());
  }
}
