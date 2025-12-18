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

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.delegate.Expression;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.form.FormDefinition;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.deploy.cache.DeploymentCache;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.task.TaskDefinition;


/**
 * Command for retrieving start or task form keys.
 *
 * @author Falko Menge (operaton)
 */
public class GetFormKeyCmd implements Command<String> {

  protected String taskDefinitionKey;
  protected String processDefinitionId;

  /**
   * Retrieves a start form key.
   */
  public GetFormKeyCmd(String processDefinitionId) {
    setProcessDefinitionId(processDefinitionId);
  }

  /**
   * Retrieves a task form key.
   */
  public GetFormKeyCmd(String processDefinitionId, String taskDefinitionKey) {
    setProcessDefinitionId(processDefinitionId);
    if (taskDefinitionKey == null || taskDefinitionKey.isEmpty()) {
      throw new ProcessEngineException("The task definition key is mandatory, but '%s' has been provided.".formatted(taskDefinitionKey));
    }
    this.taskDefinitionKey = taskDefinitionKey;
  }

  protected void setProcessDefinitionId(String processDefinitionId) {
    if (processDefinitionId == null || processDefinitionId.isEmpty()) {
      throw new ProcessEngineException("The process definition id is mandatory, but '" + processDefinitionId + "' has been provided.");
    }
    this.processDefinitionId = processDefinitionId;
  }

  @Override
  public String execute(CommandContext commandContext) {
    ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
    DeploymentCache deploymentCache = processEngineConfiguration.getDeploymentCache();
    ProcessDefinitionEntity processDefinition = deploymentCache.findDeployedProcessDefinitionById(processDefinitionId);

    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkReadProcessDefinition(processDefinition);
    }

    Expression formKeyExpression = null;

    if (taskDefinitionKey == null) {

      FormDefinition formDefinition = processDefinition.getStartFormDefinition();
      formKeyExpression = formDefinition.getFormKey();

    } else {
      TaskDefinition taskDefinition = processDefinition.getTaskDefinitions().get(taskDefinitionKey);
      formKeyExpression = taskDefinition.getFormKey();
    }

    String formKey = null;
    if (formKeyExpression != null) {
      formKey = formKeyExpression.getExpressionText();
    }
    return formKey;
  }

}
