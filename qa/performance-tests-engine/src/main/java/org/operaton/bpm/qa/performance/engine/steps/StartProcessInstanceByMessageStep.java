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
package org.operaton.bpm.qa.performance.engine.steps;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.qa.performance.engine.framework.PerfTestRunContext;
import static org.operaton.bpm.qa.performance.engine.steps.PerfTestConstants.PROCESS_INSTANCE_ID;
import static org.operaton.bpm.qa.performance.engine.steps.PerfTestConstants.RUN_ID;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Meyer
 *
 */
public class StartProcessInstanceByMessageStep extends ProcessEngineAwareStep {

  protected final String message;
  protected final Map<String, Object> processVariables;

  public StartProcessInstanceByMessageStep(ProcessEngine processEngine, String message) {
    this(processEngine, message, null);
  }

  public StartProcessInstanceByMessageStep(ProcessEngine processEngine, String message, Map<String, Object> processVariables) {
    super(processEngine);
    this.message = message;
    this.processVariables = processVariables;
  }

  @Override
  public void execute(PerfTestRunContext context) {
    Map<String, Object> variables = new HashMap<>();
    if (processVariables != null) {
      variables.putAll(processVariables);
    }
    // unique run id as variable
    variables.put(RUN_ID, context.getVariable(RUN_ID));

    ProcessInstance processInstance = runtimeService.startProcessInstanceByMessage(message, variables);
    context.setVariable(PROCESS_INSTANCE_ID, processInstance.getId());
  }

}
