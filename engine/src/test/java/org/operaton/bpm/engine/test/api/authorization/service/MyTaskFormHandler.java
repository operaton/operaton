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
package org.operaton.bpm.engine.test.api.authorization.service;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.form.TaskFormData;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.form.TaskFormDataImpl;
import org.operaton.bpm.engine.impl.form.handler.TaskFormHandler;
import org.operaton.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.impl.util.xml.Element;
import org.operaton.bpm.engine.variable.VariableMap;

/**
 * @author Roman Smirnov
 *
 */
public class MyTaskFormHandler extends MyDelegationService implements TaskFormHandler {

  @Override
  public void parseConfiguration(Element activityElement, DeploymentEntity deployment, ProcessDefinitionEntity processDefinition, BpmnParse bpmnParse) {
    // do nothing
  }

  @Override
  public void submitFormVariables(VariableMap properties, VariableScope variableScope) {
    ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
    IdentityService identityService = processEngineConfiguration.getIdentityService();
    RuntimeService runtimeService = processEngineConfiguration.getRuntimeService();

    logAuthentication(identityService);
    logInstancesCount(runtimeService);
  }

  @Override
  public TaskFormData createTaskForm(TaskEntity task) {
    ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
    IdentityService identityService = processEngineConfiguration.getIdentityService();
    RuntimeService runtimeService = processEngineConfiguration.getRuntimeService();

    logAuthentication(identityService);
    logInstancesCount(runtimeService);

    TaskFormDataImpl result = new TaskFormDataImpl();
    result.setTask(task);
    return result;
  }

}
