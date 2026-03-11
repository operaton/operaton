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
package org.operaton.bpm.engine.impl.form.deployer;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.operaton.bpm.engine.impl.AbstractDefinitionDeployer;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.core.model.Properties;
import org.operaton.bpm.engine.impl.persistence.deploy.cache.DeploymentCache;
import org.operaton.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.OperatonFormDefinitionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ResourceEntity;
import org.operaton.bpm.engine.impl.util.EngineUtilLogger;
import org.operaton.bpm.engine.impl.util.JsonUtil;

import static org.operaton.bpm.engine.impl.ResourceSuffixes.FORM_RESOURCE_SUFFIXES;

public class OperatonFormDefinitionDeployer extends AbstractDefinitionDeployer<OperatonFormDefinitionEntity> {

  protected static final EngineUtilLogger LOG = ProcessEngineLogger.UTIL_LOGGER;

  @Override
  protected String[] getResourcesSuffixes() {
    return FORM_RESOURCE_SUFFIXES;
  }

  @Override
  protected List<OperatonFormDefinitionEntity> transformDefinitions(DeploymentEntity deployment, ResourceEntity resource,
      Properties properties) {
    String formContent = new String(resource.getBytes(), StandardCharsets.UTF_8);

    try {
      JsonObject formJsonObject = new Gson().fromJson(formContent, JsonObject.class);
      String operatonFormDefinitionKey = JsonUtil.getString(formJsonObject, "id");
      OperatonFormDefinitionEntity definition = new OperatonFormDefinitionEntity(operatonFormDefinitionKey, deployment.getId(), resource.getName(), deployment.getTenantId());
      return Collections.singletonList(definition);
    } catch (Exception e) {
      // form could not be parsed, throw exception if strict parsing is not disabled
      if (!getCommandContext().getProcessEngineConfiguration().isDisableStrictOperatonFormParsing()) {
        throw LOG.exceptionDuringFormParsing(e.getMessage(), resource.getName());
      }
      return Collections.emptyList();
    }
  }

  @Override
  protected OperatonFormDefinitionEntity findDefinitionByDeploymentAndKey(String deploymentId, String definitionKey) {
    return getCommandContext().getOperatonFormDefinitionManager().findDefinitionByDeploymentAndKey(deploymentId,
        definitionKey);
  }

  @Override
  protected OperatonFormDefinitionEntity findLatestDefinitionByKeyAndTenantId(String definitionKey, String tenantId) {
    return getCommandContext().getOperatonFormDefinitionManager().findLatestDefinitionByKeyAndTenantId(definitionKey,
        tenantId);
  }

  @Override
  protected void persistDefinition(OperatonFormDefinitionEntity definition) {
    getCommandContext().getOperatonFormDefinitionManager().insert(definition);
  }

  @Override
  protected void addDefinitionToDeploymentCache(DeploymentCache deploymentCache,
      OperatonFormDefinitionEntity definition) {
    deploymentCache.addOperatonFormDefinition(definition);
  }

}
