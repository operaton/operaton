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

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.form.OperatonFormRef;
import org.operaton.bpm.engine.impl.form.entity.OperatonFormDefinitionManager;
import org.operaton.bpm.engine.impl.form.handler.DefaultFormHandler;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.OperatonFormDefinitionEntity;
import org.operaton.bpm.engine.repository.OperatonFormDefinition;

public class GetOperatonFormDefinitionCmd implements Command<OperatonFormDefinition> {

  protected OperatonFormRef operatonFormRef;
  protected String deploymentId;

  public GetOperatonFormDefinitionCmd(OperatonFormRef operatonFormRef, String deploymentId) {
    this.operatonFormRef = operatonFormRef;
    this.deploymentId = deploymentId;
  }

  @Override
  public OperatonFormDefinition execute(CommandContext commandContext) {
    String binding = operatonFormRef.getBinding();
    String key = operatonFormRef.getKey();
    OperatonFormDefinitionEntity definition = null;
    OperatonFormDefinitionManager manager = commandContext.getOperatonFormDefinitionManager();
    if (DefaultFormHandler.FORM_REF_BINDING_DEPLOYMENT.equals(binding)) {
      definition = manager.findDefinitionByDeploymentAndKey(deploymentId, key);
    } else if (DefaultFormHandler.FORM_REF_BINDING_LATEST.equals(binding)) {
      definition = manager.findLatestDefinitionByKey(key);
    } else if (DefaultFormHandler.FORM_REF_BINDING_VERSION.equals(binding)) {
      definition = manager.findDefinitionByKeyVersionAndTenantId(key, operatonFormRef.getVersion(), null);
    } else {
      throw new BadUserRequestException("Unsupported binding type for operatonFormRef. Expected to be one of "
          + DefaultFormHandler.ALLOWED_FORM_REF_BINDINGS + " but was:" + binding);
    }

    return definition;
  }

}
