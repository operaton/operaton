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
package org.operaton.bpm.engine.impl.form.handler;

import org.operaton.bpm.engine.delegate.Expression;
import org.operaton.bpm.engine.form.TaskFormData;
import org.operaton.bpm.engine.impl.form.FormDefinition;
import org.operaton.bpm.engine.impl.form.OperatonFormRefImpl;
import org.operaton.bpm.engine.impl.form.TaskFormDataImpl;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.impl.task.TaskDefinition;


/**
 * @author Tom Baeyens
 */
public class DefaultTaskFormHandler extends DefaultFormHandler implements TaskFormHandler {

  @Override
  public TaskFormData createTaskForm(TaskEntity task) {
    TaskFormDataImpl taskFormData = new TaskFormDataImpl();

    TaskDefinition taskDefinition = task.getTaskDefinition();

    FormDefinition formDefinition = taskDefinition.getFormDefinition();
    Expression formKey = formDefinition.getFormKey();
    Expression operatonFormDefinitionKey = formDefinition.getOperatonFormDefinitionKey();
    String operatonFormDefinitionBinding = formDefinition.getOperatonFormDefinitionBinding();
    Expression operatonFormDefinitionVersion = formDefinition.getOperatonFormDefinitionVersion();

    if (formKey != null) {
      Object formValue = formKey.getValue(task);
      if (formValue != null) {
        taskFormData.setFormKey(formValue.toString());
      }
    } else if (operatonFormDefinitionKey != null && operatonFormDefinitionBinding != null) {
      Object formRefKeyValue = operatonFormDefinitionKey.getValue(task);
      if(formRefKeyValue != null) {
        OperatonFormRefImpl ref = new OperatonFormRefImpl(formRefKeyValue.toString(), operatonFormDefinitionBinding);
        if(FORM_REF_BINDING_VERSION.equals(operatonFormDefinitionBinding) && operatonFormDefinitionVersion != null) {
          Object formRefVersionValue = operatonFormDefinitionVersion.getValue(task);
          if(formRefVersionValue != null) {
            ref.setVersion(Integer.parseInt((String)formRefVersionValue));
          }
        }
        taskFormData.setOperatonFormRef(ref);
      }
    }

    taskFormData.setDeploymentId(deploymentId);
    taskFormData.setTask(task);
    initializeFormProperties(taskFormData, task.getExecution());
    initializeFormFields(taskFormData, task.getExecution());
    return taskFormData;
  }

}
