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
package org.operaton.bpm.engine.impl.form.handler;

import org.operaton.bpm.engine.form.StartFormData;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;

/**
 * @author Roman Smirnov
 *
 */
public class DelegateStartFormHandler extends DelegateFormHandler implements StartFormHandler {

  public DelegateStartFormHandler(StartFormHandler formHandler, DeploymentEntity deployment) {
    super(formHandler, deployment.getId());
  }

  @Override
  public StartFormData createStartFormData(final ProcessDefinitionEntity processDefinition) {
    return performContextSwitch(() -> {
      CreateStartFormInvocation invocation = new CreateStartFormInvocation((StartFormHandler) formHandler, processDefinition);
      Context.getProcessEngineConfiguration()
          .getDelegateInterceptor()
          .handleInvocation(invocation);
      return (StartFormData) invocation.getInvocationResult();
    });
  }

  @Override
  public StartFormHandler getFormHandler() {
    return (StartFormHandler) formHandler;
  }

}
