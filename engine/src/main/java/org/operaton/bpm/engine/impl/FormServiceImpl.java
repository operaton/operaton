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
package org.operaton.bpm.engine.impl;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.form.StartFormData;
import org.operaton.bpm.engine.form.TaskFormData;
import org.operaton.bpm.engine.impl.cmd.GetDeployedStartFormCmd;
import org.operaton.bpm.engine.impl.cmd.GetFormKeyCmd;
import org.operaton.bpm.engine.impl.cmd.GetRenderedStartFormCmd;
import org.operaton.bpm.engine.impl.cmd.GetRenderedTaskFormCmd;
import org.operaton.bpm.engine.impl.cmd.GetStartFormCmd;
import org.operaton.bpm.engine.impl.cmd.GetStartFormVariablesCmd;
import org.operaton.bpm.engine.impl.cmd.GetTaskFormCmd;
import org.operaton.bpm.engine.impl.cmd.GetTaskFormVariablesCmd;
import org.operaton.bpm.engine.impl.cmd.SubmitStartFormCmd;
import org.operaton.bpm.engine.impl.cmd.SubmitTaskFormCmd;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.variable.VariableMap;


/**
 * @author Tom Baeyens
 * @author Falko Menge (operaton)
 */
public class FormServiceImpl extends ServiceImpl implements FormService {

  @Override
  public Object getRenderedStartForm(String processDefinitionId) {
    return commandExecutor.execute(new GetRenderedStartFormCmd(processDefinitionId, null));
  }

  @Override
  public Object getRenderedStartForm(String processDefinitionId, String engineName) {
    return commandExecutor.execute(new GetRenderedStartFormCmd(processDefinitionId, engineName));
  }

  @Override
  public Object getRenderedTaskForm(String taskId) {
    return commandExecutor.execute(new GetRenderedTaskFormCmd(taskId, null));
  }

  @Override
  public Object getRenderedTaskForm(String taskId, String engineName) {
    return commandExecutor.execute(new GetRenderedTaskFormCmd(taskId, engineName));
  }

  @Override
  public StartFormData getStartFormData(String processDefinitionId) {
    return commandExecutor.execute(new GetStartFormCmd(processDefinitionId));
  }

  @Override
  public TaskFormData getTaskFormData(String taskId) {
    return commandExecutor.execute(new GetTaskFormCmd(taskId));
  }

  @Override
  public ProcessInstance submitStartFormData(String processDefinitionId, Map<String, String> properties) {
    return commandExecutor.execute(new SubmitStartFormCmd(processDefinitionId, null, (Map) properties));
  }

  @Override
  public ProcessInstance submitStartFormData(String processDefinitionId, String businessKey, Map<String, String> properties) {
	  return commandExecutor.execute(new SubmitStartFormCmd(processDefinitionId, businessKey, (Map) properties));
  }

  @Override
  public ProcessInstance submitStartForm(String processDefinitionId, Map<String, Object> properties) {
    return commandExecutor.execute(new SubmitStartFormCmd(processDefinitionId, null, properties));
  }

  @Override
  public ProcessInstance submitStartForm(String processDefinitionId, String businessKey, Map<String, Object> properties) {
    return commandExecutor.execute(new SubmitStartFormCmd(processDefinitionId, businessKey, properties));
  }

  @Override
  public void submitTaskFormData(String taskId, Map<String, String> properties) {
    submitTaskForm(taskId, (Map) properties);
  }

  @Override
  public void submitTaskForm(String taskId, Map<String, Object> properties) {
    commandExecutor.execute(new SubmitTaskFormCmd(taskId, properties, false, false));
  }

  @Override
  public VariableMap submitTaskFormWithVariablesInReturn(String taskId, Map<String, Object> properties, boolean deserializeValues) {
    return commandExecutor.execute(new SubmitTaskFormCmd(taskId, properties, true, deserializeValues));
  }

  @Override
  public String getStartFormKey(String processDefinitionId) {
    return commandExecutor.execute(new GetFormKeyCmd(processDefinitionId));
  }

  @Override
  public String getTaskFormKey(String processDefinitionId, String taskDefinitionKey) {
    return commandExecutor.execute(new GetFormKeyCmd(processDefinitionId, taskDefinitionKey));
  }

  @Override
  public VariableMap getStartFormVariables(String processDefinitionId) {
    return getStartFormVariables(processDefinitionId, null, true);
  }

  @Override
  public VariableMap getStartFormVariables(String processDefinitionId, Collection<String> formVariables, boolean deserializeObjectValues) {
    return commandExecutor.execute(new GetStartFormVariablesCmd(processDefinitionId, formVariables, deserializeObjectValues));
  }

  @Override
  public VariableMap getTaskFormVariables(String taskId) {
    return getTaskFormVariables(taskId, null, true);
  }

  @Override
  public VariableMap getTaskFormVariables(String taskId, Collection<String> formVariables, boolean deserializeObjectValues) {
    return commandExecutor.execute(new GetTaskFormVariablesCmd(taskId, formVariables, deserializeObjectValues));
  }

  @Override
  public InputStream getDeployedStartForm(String processDefinitionId) {
    return commandExecutor.execute(new GetDeployedStartFormCmd(processDefinitionId));
  }

  @Override
  public InputStream getDeployedTaskForm(String taskId) {
    return commandExecutor.execute(new GetDeployedTaskFormCmd(taskId));
  }

}
