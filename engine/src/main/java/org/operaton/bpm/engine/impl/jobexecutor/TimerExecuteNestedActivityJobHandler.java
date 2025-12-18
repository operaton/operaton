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

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;


/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class TimerExecuteNestedActivityJobHandler extends TimerEventJobHandler {

  public static final String TYPE = "timer-transition";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public void execute(TimerJobConfiguration configuration, ExecutionEntity execution, CommandContext commandContext, String tenantId) {
    String activityId = configuration.getTimerElementKey();
    ActivityImpl activity = execution.getProcessDefinition().findActivity(activityId);

    ensureNotNull("Error while firing timer: boundary event activity %s not found".formatted(configuration), "boundary event activity", activity);

    try {

      execution.executeEventHandlerActivity(activity);

    } catch (RuntimeException e) {
      throw e;

    } catch (Exception e) {
      throw new ProcessEngineException("exception during timer execution: " + e.getMessage(), e);
    }
  }
}
