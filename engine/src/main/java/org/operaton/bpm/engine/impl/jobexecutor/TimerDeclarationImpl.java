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
package org.operaton.bpm.engine.impl.jobexecutor;

import java.io.Serial;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.delegate.Expression;
import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.impl.bpmn.helper.BpmnProperties;
import org.operaton.bpm.engine.impl.calendar.BusinessCalendar;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.el.StartProcessVariableScope;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TimerEntity;
import org.operaton.bpm.engine.impl.pvm.PvmScope;

/**
 * @author Tom Baeyens
 * @author Daniel Meyer
 */
public class TimerDeclarationImpl extends JobDeclaration<ExecutionEntity, TimerEntity> {

  @Serial private static final long serialVersionUID = 1L;

  protected Expression description;
  protected TimerDeclarationType type;

  protected String repeat;
  protected boolean isInterruptingTimer; // For boundary timers
  protected String eventScopeActivityId;

  protected String rawJobHandlerConfiguration;

  public TimerDeclarationImpl(Expression expression, TimerDeclarationType type, String jobHandlerType) {
    super(jobHandlerType);
    this.description = expression;
    this.type= type;
  }

  public boolean isInterruptingTimer() {
    return isInterruptingTimer;
  }

  public void setInterruptingTimer(boolean isInterruptingTimer) {
    this.isInterruptingTimer = isInterruptingTimer;
  }

  public String getRepeat() {
    return repeat;
  }

  public void setEventScopeActivityId(String eventScopeActivityId) {
    this.eventScopeActivityId = eventScopeActivityId;
  }

  public String getEventScopeActivityId() {
    return eventScopeActivityId;
  }

  @Override
  protected TimerEntity newJobInstance(ExecutionEntity execution) {

    TimerEntity timer = new TimerEntity(this);
    if (execution != null) {
      timer.setExecution(execution);
    }

    return timer;
  }

  public void setRawJobHandlerConfiguration(String rawJobHandlerConfiguration) {
    this.rawJobHandlerConfiguration = rawJobHandlerConfiguration;
  }

  public void updateJob(TimerEntity timer) {
    initializeConfiguration(timer.getExecution(), timer);
  }

  protected void initializeConfiguration(ExecutionEntity context, TimerEntity job) {
    String dueDateString = resolveAndSetDuedate(context, job, false);

    if ((type == TimerDeclarationType.CYCLE
            && !Objects.equals(jobHandlerType, TimerCatchIntermediateEventJobHandler.TYPE))
            && !isInterruptingTimer) {

      // See ACT-1427: A boundary timer with a cancelActivity='true', doesn't need to repeat itself
      String prepared = prepareRepeat(dueDateString);
      job.setRepeat(prepared);
    }
  }

  public String resolveAndSetDuedate(ExecutionEntity context, TimerEntity job, boolean creationDateBased) {
    BusinessCalendar businessCalendar = Context
        .getProcessEngineConfiguration()
        .getBusinessCalendarManager()
        .getBusinessCalendar(type.calendarName);

    if (description==null) {
      throw new ProcessEngineException("Timer '%s' was not configured with a valid duration/time".formatted(context.getActivityId()));
    }

    String dueDateString = null;
    Date duedate = null;

    // ACT-1415: timer-declaration on start-event may contain expressions NOT
    // evaluating variables but other context, evaluating should happen nevertheless
    VariableScope scopeForExpression = context;
    if(scopeForExpression == null) {
      scopeForExpression = StartProcessVariableScope.getSharedInstance();
    }

    Object dueDateValue = description.getValue(scopeForExpression);
    if (dueDateValue instanceof String string) {
      dueDateString = string;
    }
    else if (dueDateValue instanceof Date date) {
      duedate = date;
    }
    else {
      throw new ProcessEngineException("Timer '%s' was not configured with a valid duration/time, either hand in a java.util.Date or a String in format 'yyyy-MM-dd'T'hh:mm:ss'"
          .formatted(context.getActivityId()));
    }

    if (duedate==null) {
      if (creationDateBased) {
        if (job.getCreateTime() == null) {
          throw new ProcessEngineException("Timer '%s' has no creation time and cannot be recalculated based on creation date. Either recalculate on your own or trigger recalculation with creationDateBased set to false."
              .formatted(context.getActivityId()));
        }
        duedate = businessCalendar.resolveDuedate(dueDateString, job.getCreateTime());
      } else {
        duedate = businessCalendar.resolveDuedate(dueDateString);
      }
    }

    job.setDuedate(duedate);
    return dueDateString;
  }

  @Override
  protected void postInitialize(ExecutionEntity execution, TimerEntity timer) {
    initializeConfiguration(execution, timer);
  }

  protected String prepareRepeat(String dueDate) {
    if (dueDate.startsWith("R")) {
      return TimerEntity.replaceRepeatCycleAndDate(dueDate);
    }
    return dueDate;
  }

  public TimerEntity createTimerInstance(ExecutionEntity execution) {
    return createTimer(execution);
  }

  public TimerEntity createStartTimerInstance(String deploymentId) {
    return createTimer(deploymentId);
  }

  public TimerEntity createTimer(String deploymentId) {
    TimerEntity timer = super.createJobInstance((ExecutionEntity) null);
    timer.setDeploymentId(deploymentId);
    scheduleTimer(timer);
    return timer;
  }

  public TimerEntity createTimer(ExecutionEntity execution) {
    TimerEntity timer = super.createJobInstance(execution);
    scheduleTimer(timer);
    return timer;
  }

  protected void scheduleTimer(TimerEntity timer) {
    Context
      .getCommandContext()
      .getJobManager()
      .schedule(timer);
  }

  @Override
  protected ExecutionEntity resolveExecution(ExecutionEntity context) {
    return context;
  }

  @Override
  protected JobHandlerConfiguration resolveJobHandlerConfiguration(ExecutionEntity context) {
    return resolveJobHandler().newConfiguration(rawJobHandlerConfiguration);
  }

  /**
   * @return all timers declared in the given scope
   */
  public static Map<String, TimerDeclarationImpl> getDeclarationsForScope(PvmScope scope) {
    if (scope == null) {
      return Collections.emptyMap();
    }

    Map<String, TimerDeclarationImpl> result = scope.getProperties().get(BpmnProperties.TIMER_DECLARATIONS);
    if (result != null) {
      return result;
    }
    else {
      return Collections.emptyMap();
    }
  }

  /**
   * @return all timeout listeners declared in the given scope
   */
  public static Map<String, Map<String, TimerDeclarationImpl>> getTimeoutListenerDeclarationsForScope(PvmScope scope) {
    if (scope == null) {
      return Collections.emptyMap();
    }

    Map<String, Map<String, TimerDeclarationImpl>> result = scope.getProperties().get(BpmnProperties.TIMEOUT_LISTENER_DECLARATIONS);
    if (result != null) {
      return result;
    }
    else {
      return Collections.emptyMap();
    }
  }

}
