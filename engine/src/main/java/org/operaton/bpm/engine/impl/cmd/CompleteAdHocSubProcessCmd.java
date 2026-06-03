/*
 * Copyright 2026 FINOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.model.bpmn.instance.AdHocSubProcess;
import org.operaton.bpm.model.bpmn.instance.FlowElement;

/**
 * Completes an active ad-hoc subprocess execution.
 */
public class CompleteAdHocSubProcessCmd implements Command<Void>, Serializable {

  private static final long serialVersionUID = 1L;

  protected final String executionId;
  protected final Map<String, Object> variables;

  public CompleteAdHocSubProcessCmd(String executionId) {
    this(executionId, null);
  }

  public CompleteAdHocSubProcessCmd(String executionId, Map<String, Object> variables) {
    this.executionId = executionId;
    this.variables = variables;
  }

  @Override
  public Void execute(CommandContext commandContext) {
    ensureNotNull(BadUserRequestException.class, "executionId is null", "executionId", executionId);

    ExecutionEntity execution = commandContext.getExecutionManager().findExecutionById(executionId);
    ensureNotNull(BadUserRequestException.class, "execution " + executionId + " doesn't exist", "execution", execution);

    for (CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkUpdateProcessInstance(execution);
    }

    ActivityImpl adHocActivity = execution.getActivity();
    ensureNotNull(BadUserRequestException.class, "execution " + executionId + " has no current activity", "activity", adHocActivity);

    if (!(adHocActivity.getActivityBehavior() instanceof AdHocSubProcessActivityBehavior)) {
      throw new BadUserRequestException("execution " + executionId + " is not waiting in an adHocSubProcess");
    }

    boolean cancelRemainingInstances = resolveCancelRemainingInstances(execution, adHocActivity);

    boolean hasActiveChildren = execution.getExecutions().stream().anyMatch(ActivityExecution::isActive);
    if (hasActiveChildren && !cancelRemainingInstances) {
      throw new BadUserRequestException(
          "adHocSubProcess " + adHocActivity.getId() + " has active child activities and cannot be completed");
    }

    if (variables != null && !variables.isEmpty()) {
      execution.setVariables(variables);
    }

    AdHocSubProcessActivityBehavior behavior =
      (AdHocSubProcessActivityBehavior) adHocActivity.getActivityBehavior();

    if (hasActiveChildren) {
      for (ActivityExecution child : new ArrayList<>(execution.getExecutions())) {
        if (child.isActive()) {
          child.interrupt("adHocSubProcessManuallyCompleted");
        }
      }

      for (ActivityExecution child : new ArrayList<>(execution.getExecutions())) {
        child.remove();
      }

      execution.forceUpdate();
    }

    behavior.leave(execution);

    return null;
  }

  protected boolean resolveCancelRemainingInstances(ExecutionEntity execution, ActivityImpl adHocActivity) {
    FlowElement flowElement = execution.getBpmnModelElementInstance();
    if (flowElement instanceof AdHocSubProcess) {
      return ((AdHocSubProcess) flowElement).isCancelRemainingInstances();
    }

    Object cancelRemainingProperty = adHocActivity.getProperty(BpmnParse.PROPERTYNAME_AD_HOC_CANCEL_REMAINING);
    if (cancelRemainingProperty instanceof Boolean) {
      return (Boolean) cancelRemainingProperty;
    }

    if (cancelRemainingProperty instanceof String) {
      return Boolean.parseBoolean((String) cancelRemainingProperty);
    }

    return true;
  }
}
