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
package org.operaton.bpm.engine.impl.dmn.deployer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.operaton.bpm.dmn.engine.DmnDecision;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnTransformer;
import org.operaton.bpm.engine.impl.AbstractDefinitionDeployer;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.core.model.Properties;
import org.operaton.bpm.engine.impl.dmn.DecisionLogger;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionDefinitionEntity;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionDefinitionManager;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionRequirementsDefinitionEntity;
import org.operaton.bpm.engine.impl.persistence.deploy.Deployer;
import org.operaton.bpm.engine.impl.persistence.deploy.cache.DeploymentCache;
import org.operaton.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ResourceEntity;

import static org.operaton.bpm.engine.impl.ResourceSuffixes.DMN_RESOURCE_SUFFIXES;

/**
 * {@link Deployer} responsible to parse DMN 1.1 XML files and create the proper
 * {@link DecisionDefinitionEntity}s. Since it uses the result of the
 * {@link DecisionRequirementsDefinitionDeployer} to avoid duplicated parsing, the DecisionRequirementsDefinitionDeployer must
 * process the deployment before this cacheDeployer.
 */
public class DecisionDefinitionDeployer extends AbstractDefinitionDeployer<DecisionDefinitionEntity> {

  protected static final DecisionLogger LOG = ProcessEngineLogger.DECISION_LOGGER;

  protected DmnTransformer transformer;

  @Override
  protected String[] getResourcesSuffixes() {
    return DMN_RESOURCE_SUFFIXES;
  }

  @Override
  protected List<DecisionDefinitionEntity> transformDefinitions(DeploymentEntity deployment, ResourceEntity resource, Properties properties) {
    List<DecisionDefinitionEntity> decisions = new ArrayList<>();

    // get the decisions from the deployed drd instead of parse the DMN again
    DecisionRequirementsDefinitionEntity deployedDrd = findDeployedDrdForResource(deployment, resource.getName());

    if (deployedDrd == null) {
      throw LOG.exceptionNoDrdForResource(resource.getName());
    }

    Collection<DmnDecision> decisionsOfDrd = deployedDrd.getDecisions();
    for (DmnDecision decisionOfDrd : decisionsOfDrd) {

      DecisionDefinitionEntity decisionEntity = (DecisionDefinitionEntity) decisionOfDrd;
      if (DecisionRequirementsDefinitionDeployer.isDecisionRequirementsDefinitionPersistable(deployedDrd)) {
        decisionEntity.setDecisionRequirementsDefinitionId(deployedDrd.getId());
        decisionEntity.setDecisionRequirementsDefinitionKey(deployedDrd.getKey());
      }

      decisions.add(decisionEntity);
    }

    if (!DecisionRequirementsDefinitionDeployer.isDecisionRequirementsDefinitionPersistable(deployedDrd)) {
      deployment.removeArtifact(deployedDrd);
    }

    return decisions;
  }

  protected DecisionRequirementsDefinitionEntity findDeployedDrdForResource(DeploymentEntity deployment, String resourceName) {
    List<DecisionRequirementsDefinitionEntity> deployedDrds = deployment.getDeployedArtifacts(DecisionRequirementsDefinitionEntity.class);
    for (DecisionRequirementsDefinitionEntity deployedDrd : deployedDrds) {
      if (deployedDrd.getResourceName().equals(resourceName)) {
        return deployedDrd;
      }
    }
    return null;
  }

  @Override
  protected DecisionDefinitionEntity findDefinitionByDeploymentAndKey(String deploymentId, String definitionKey) {
    return getDecisionDefinitionManager().findDecisionDefinitionByDeploymentAndKey(deploymentId, definitionKey);
  }

  @Override
  protected DecisionDefinitionEntity findLatestDefinitionByKeyAndTenantId(String definitionKey, String tenantId) {
    return getDecisionDefinitionManager().findLatestDecisionDefinitionByKeyAndTenantId(definitionKey, tenantId);
  }

  @Override
  protected void persistDefinition(DecisionDefinitionEntity definition) {
    getDecisionDefinitionManager().insertDecisionDefinition(definition);
  }

  @Override
  protected void addDefinitionToDeploymentCache(DeploymentCache deploymentCache, DecisionDefinitionEntity definition) {
    deploymentCache.addDecisionDefinition(definition);
  }

  // context ///////////////////////////////////////////////////////////////////////////////////////////

  protected DecisionDefinitionManager getDecisionDefinitionManager() {
    return getCommandContext().getDecisionDefinitionManager();
  }

  // getters/setters ///////////////////////////////////////////////////////////////////////////////////

  public DmnTransformer getTransformer() {
    return transformer;
  }

  public void setTransformer(DmnTransformer transformer) {
    this.transformer = transformer;
  }

}
