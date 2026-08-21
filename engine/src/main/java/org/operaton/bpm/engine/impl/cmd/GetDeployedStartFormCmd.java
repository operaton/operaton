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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.form.FormData;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.persistence.deploy.cache.DeploymentCache;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.util.EnsureUtil;

/**
 *
 * @author Anna Pazola
 *
 */
public @NullMarked class GetDeployedStartFormCmd extends AbstractGetDeployedFormCmd {

  protected String processDefinitionId;

  public GetDeployedStartFormCmd(String processDefinitionId) {
    EnsureUtil.ensureNotNull(BadUserRequestException.class, "Process definition id cannot be null", "processDefinitionId", processDefinitionId);
    this.processDefinitionId = processDefinitionId;
  }

  @Override
  protected @Nullable FormData getFormData() {
    return getCommandContext().runWithoutAuthorization(new GetStartFormCmd(processDefinitionId));
  }

  @Override
  protected void checkAuthorization() {
    ProcessEngineConfigurationImpl processEngineConfiguration = getCommandContext().getProcessEngineConfiguration();
    DeploymentCache deploymentCache = processEngineConfiguration.getDeploymentCache();
    ProcessDefinitionEntity processDefinition = deploymentCache.findDeployedProcessDefinitionById(processDefinitionId);
    for (CommandChecker checker : getCommandContext().getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkReadProcessDefinition(processDefinition);
    }
  }
}
