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


import java.util.ArrayList;
import java.util.List;

import org.operaton.bpm.engine.impl.ConditionEvaluationBuilderImpl;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.runtime.ConditionHandler;
import org.operaton.bpm.engine.impl.runtime.ConditionHandlerResult;
import org.operaton.bpm.engine.impl.runtime.ConditionSet;
import org.operaton.bpm.engine.runtime.ProcessInstance;

/**
 * Evaluates the conditions to start processes by conditional start events
 *
 * @author Yana Vasileva
 *
 */
public class EvaluateStartConditionCmd implements Command<List<ProcessInstance>> {

  protected ConditionEvaluationBuilderImpl builder;

  public EvaluateStartConditionCmd(ConditionEvaluationBuilderImpl builder) {
    this.builder = builder;
  }

  @Override
  public List<ProcessInstance> execute(final CommandContext commandContext) {
    final ConditionHandler conditionHandler = commandContext.getProcessEngineConfiguration().getConditionHandler();
    final ConditionSet conditionSet = new ConditionSet(builder);

    List<ConditionHandlerResult> results = conditionHandler.evaluateStartCondition(commandContext, conditionSet);

    for (ConditionHandlerResult ConditionHandlerResult : results) {
      checkAuthorization(commandContext, ConditionHandlerResult);
    }

    List<ProcessInstance> processInstances = new ArrayList<>();
    for (ConditionHandlerResult ConditionHandlerResult : results) {
      processInstances.add(instantiateProcess(commandContext, ConditionHandlerResult));
    }

    return processInstances;
  }

  protected void checkAuthorization(CommandContext commandContext, ConditionHandlerResult result) {
    for (CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      ProcessDefinitionEntity definition = result.getProcessDefinition();
      checker.checkCreateProcessInstance(definition);
    }
  }

  @SuppressWarnings("unused")
  protected ProcessInstance instantiateProcess(CommandContext commandContext, ConditionHandlerResult result) {
    ProcessDefinitionEntity processDefinitionEntity = result.getProcessDefinition();

    ActivityImpl startEvent = processDefinitionEntity.findActivity(result.getActivity().getActivityId());
    ExecutionEntity processInstance = processDefinitionEntity.createProcessInstance(builder.getBusinessKey(), startEvent);
    processInstance.start(builder.getVariables());

    return processInstance;
  }

}
