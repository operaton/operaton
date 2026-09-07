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
package org.operaton.bpm.engine.impl.persistence.deploy.cache;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cmmn.entity.repository.CaseDefinitionEntity;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionDefinitionEntity;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionRequirementsDefinitionEntity;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionRequirementsDefinitionQueryImpl;
import org.operaton.bpm.engine.impl.persistence.deploy.Deployer;
import org.operaton.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.OperatonFormDefinitionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.repository.DecisionRequirementsDefinition;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.cmmn.CmmnModelInstance;
import org.operaton.bpm.model.dmn.DmnModelInstance;
import org.operaton.commons.utils.cache.Cache;


/**
 * @author Tom Baeyens
 * @author Falko Menge
 */
public @NullMarked class DeploymentCache {

  protected ProcessDefinitionCache processDefinitionEntityCache;
  protected CaseDefinitionCache caseDefinitionCache;
  protected DecisionDefinitionCache decisionDefinitionCache;
  protected DecisionRequirementsDefinitionCache decisionRequirementsDefinitionCache;
  protected OperatonFormDefinitionCache operatonFormDefinitionCache;


  protected BpmnModelInstanceCache bpmnModelInstanceCache;
  protected CmmnModelInstanceCache cmmnModelInstanceCache;
  protected DmnModelInstanceCache dmnModelInstanceCache;
  protected CacheDeployer cacheDeployer = new CacheDeployer();

  public DeploymentCache(CacheFactory factory, int cacheCapacity) {
    processDefinitionEntityCache = new ProcessDefinitionCache(factory, cacheCapacity, cacheDeployer);
    caseDefinitionCache = new CaseDefinitionCache(factory, cacheCapacity, cacheDeployer);
    decisionDefinitionCache = new DecisionDefinitionCache(factory, cacheCapacity, cacheDeployer);
    decisionRequirementsDefinitionCache = new DecisionRequirementsDefinitionCache(factory, cacheCapacity, cacheDeployer);
    operatonFormDefinitionCache = new OperatonFormDefinitionCache(factory, cacheCapacity, cacheDeployer);

    bpmnModelInstanceCache = new BpmnModelInstanceCache(factory, cacheCapacity, processDefinitionEntityCache);
    cmmnModelInstanceCache = new CmmnModelInstanceCache(factory, cacheCapacity, caseDefinitionCache);
    dmnModelInstanceCache = new DmnModelInstanceCache(factory, cacheCapacity, decisionDefinitionCache);
  }

  public void deploy(final DeploymentEntity deployment) {
    cacheDeployer.deploy(deployment);
  }

  // PROCESS DEFINITION ////////////////////////////////////////////////////////////////////////////////

  public @Nullable ProcessDefinitionEntity findProcessDefinitionFromCache(String processDefinitionId) {
    return processDefinitionEntityCache.findDefinitionFromCache(processDefinitionId);
  }

  public @Nullable ProcessDefinitionEntity findDeployedProcessDefinitionById(String processDefinitionId) {
    return processDefinitionEntityCache.findDeployedDefinitionById(processDefinitionId);
  }

  /**
   * @return the latest version of the process definition with the given key (from any tenant)
   * @throws ProcessEngineException if more than one tenant has a process definition with the given key
   * @see #findDeployedLatestProcessDefinitionByKeyAndTenantId(String, String)
   */
  public @Nullable ProcessDefinitionEntity findDeployedLatestProcessDefinitionByKey(String processDefinitionKey) {
    return processDefinitionEntityCache.findDeployedLatestDefinitionByKey(processDefinitionKey);
  }

  /**
   * @return the latest version of the process definition with the given key and tenant id
   */
  public @Nullable ProcessDefinitionEntity findDeployedLatestProcessDefinitionByKeyAndTenantId(String processDefinitionKey, String tenantId) {
    return processDefinitionEntityCache.findDeployedLatestDefinitionByKeyAndTenantId(processDefinitionKey, tenantId);
  }

  public @Nullable ProcessDefinitionEntity findDeployedProcessDefinitionByKeyVersionAndTenantId(final String processDefinitionKey, final Integer processDefinitionVersion, final String tenantId) {
    return processDefinitionEntityCache.findDeployedDefinitionByKeyVersionAndTenantId(processDefinitionKey, processDefinitionVersion, tenantId);
  }

  public @Nullable ProcessDefinitionEntity findDeployedProcessDefinitionByKeyVersionTagAndTenantId(String processDefinitionKey, String processDefinitionVersionTag, String tenantId) {
    return processDefinitionEntityCache.findDeployedDefinitionByKeyVersionTagAndTenantId(processDefinitionKey, processDefinitionVersionTag, tenantId);
  }

  public @Nullable ProcessDefinitionEntity findDeployedProcessDefinitionByDeploymentAndKey(String deploymentId, String processDefinitionKey) {
    return processDefinitionEntityCache.findDeployedDefinitionByDeploymentAndKey(deploymentId, processDefinitionKey);
  }

  public @Nullable ProcessDefinitionEntity resolveProcessDefinition(ProcessDefinitionEntity processDefinition) {
    return processDefinitionEntityCache.resolveDefinition(processDefinition);
  }

  public @Nullable BpmnModelInstance findBpmnModelInstanceForProcessDefinition(ProcessDefinitionEntity processDefinitionEntity) {
    return bpmnModelInstanceCache.findBpmnModelInstanceForDefinition(processDefinitionEntity);
  }

  public @Nullable BpmnModelInstance findBpmnModelInstanceForProcessDefinition(String processDefinitionId) {
    return bpmnModelInstanceCache.findBpmnModelInstanceForDefinition(processDefinitionId);
  }

  public void addProcessDefinition(ProcessDefinitionEntity processDefinition) {
    processDefinitionEntityCache.addDefinition(processDefinition);
  }

  public void removeProcessDefinition(String processDefinitionId) {
    processDefinitionEntityCache.removeDefinitionFromCache(processDefinitionId);
    bpmnModelInstanceCache.remove(processDefinitionId);
  }

  public void discardProcessDefinitionCache() {
    processDefinitionEntityCache.clear();
    bpmnModelInstanceCache.clear();
  }

  // CAMUNDA FORM DEFINITION ////////////////////////////////////////////////////////////////////////

  public void addOperatonFormDefinition(OperatonFormDefinitionEntity operatonFormDefinition) {
    operatonFormDefinitionCache.addDefinition(operatonFormDefinition);
  }

  public void removeOperatonFormDefinition(String operatonFormDefinitionId) {
    operatonFormDefinitionCache.removeDefinitionFromCache(operatonFormDefinitionId);
  }

  /** @deprecated unused internal API */
  @Deprecated(forRemoval = true, since = "2.2")
  @SuppressWarnings("java:S1133")
  public void discardOperatonFormDefinitionCache() {
    operatonFormDefinitionCache.clear();
  }

  // CASE DEFINITION ////////////////////////////////////////////////////////////////////////////////

  public @Nullable CaseDefinitionEntity findCaseDefinitionFromCache(String caseDefinitionId) {
    return caseDefinitionCache.findDefinitionFromCache(caseDefinitionId);
  }

  public @Nullable CaseDefinitionEntity findDeployedCaseDefinitionById(String caseDefinitionId) {
    return caseDefinitionCache.findDeployedDefinitionById(caseDefinitionId);
  }

  /**
   * @return the latest version of the case definition with the given key (from any tenant)
   * @throws ProcessEngineException if more than one tenant has a case definition with the given key
   * @see #findDeployedLatestCaseDefinitionByKeyAndTenantId(String, String)
   */
  public @Nullable CaseDefinitionEntity findDeployedLatestCaseDefinitionByKey(String caseDefinitionKey) {
    return caseDefinitionCache.findDeployedLatestDefinitionByKey(caseDefinitionKey);
  }

  /**
   * @return the latest version of the case definition with the given key and tenant id
   */
  public @Nullable CaseDefinitionEntity findDeployedLatestCaseDefinitionByKeyAndTenantId(String caseDefinitionKey, String tenantId) {
    return caseDefinitionCache.findDeployedLatestDefinitionByKeyAndTenantId(caseDefinitionKey, tenantId);
  }

  public @Nullable CaseDefinitionEntity findDeployedCaseDefinitionByKeyVersionAndTenantId(String caseDefinitionKey, Integer caseDefinitionVersion, String tenantId) {
    return caseDefinitionCache.findDeployedDefinitionByKeyVersionAndTenantId(caseDefinitionKey, caseDefinitionVersion, tenantId);
  }

  public @Nullable CaseDefinitionEntity findDeployedCaseDefinitionByDeploymentAndKey(String deploymentId, String caseDefinitionKey) {
    return caseDefinitionCache.findDeployedDefinitionByDeploymentAndKey(deploymentId, caseDefinitionKey);
  }

  public @Nullable CaseDefinitionEntity getCaseDefinitionById(String caseDefinitionId) {
    return caseDefinitionCache.getCaseDefinitionById(caseDefinitionId);
  }

  public @Nullable CaseDefinitionEntity resolveCaseDefinition(CaseDefinitionEntity caseDefinition) {
    return caseDefinitionCache.resolveDefinition(caseDefinition);
  }

  public @Nullable CmmnModelInstance findCmmnModelInstanceForCaseDefinition(String caseDefinitionId) {
    return cmmnModelInstanceCache.findBpmnModelInstanceForDefinition(caseDefinitionId);
  }

  public void addCaseDefinition(CaseDefinitionEntity caseDefinition) {
    caseDefinitionCache.addDefinition(caseDefinition);
  }

  public void removeCaseDefinition(String caseDefinitionId) {
    caseDefinitionCache.removeDefinitionFromCache(caseDefinitionId);
    cmmnModelInstanceCache.remove(caseDefinitionId);
  }

  public void discardCaseDefinitionCache() {
    caseDefinitionCache.clear();
    cmmnModelInstanceCache.clear();
  }

  // DECISION DEFINITION ////////////////////////////////////////////////////////////////////////////

  public @Nullable DecisionDefinitionEntity findDecisionDefinitionFromCache(String decisionDefinitionId) {
    return decisionDefinitionCache.findDefinitionFromCache(decisionDefinitionId);
  }

  public @Nullable DecisionDefinitionEntity findDeployedDecisionDefinitionById(String decisionDefinitionId) {
    return decisionDefinitionCache.findDeployedDefinitionById(decisionDefinitionId);
  }

  public @Nullable DecisionDefinition findDeployedLatestDecisionDefinitionByKey(String decisionDefinitionKey) {
    return decisionDefinitionCache.findDeployedLatestDefinitionByKey(decisionDefinitionKey);
  }

  public @Nullable DecisionDefinition findDeployedLatestDecisionDefinitionByKeyAndTenantId(String decisionDefinitionKey, String tenantId) {
    return decisionDefinitionCache.findDeployedLatestDefinitionByKeyAndTenantId(decisionDefinitionKey, tenantId);
  }

  public @Nullable DecisionDefinition findDeployedDecisionDefinitionByDeploymentAndKey(String deploymentId, String decisionDefinitionKey) {
    return decisionDefinitionCache.findDeployedDefinitionByDeploymentAndKey(deploymentId, decisionDefinitionKey);
  }

  public @Nullable DecisionDefinition findDeployedDecisionDefinitionByKeyAndVersion(String decisionDefinitionKey, Integer decisionDefinitionVersion) {
    return decisionDefinitionCache.findDeployedDefinitionByKeyAndVersion(decisionDefinitionKey, decisionDefinitionVersion);
  }

  public @Nullable DecisionDefinition findDeployedDecisionDefinitionByKeyVersionAndTenantId(String decisionDefinitionKey, Integer decisionDefinitionVersion, String tenantId) {
    return decisionDefinitionCache.findDeployedDefinitionByKeyVersionAndTenantId(decisionDefinitionKey, decisionDefinitionVersion, tenantId);
  }

  public @Nullable DecisionDefinition findDeployedDecisionDefinitionByKeyVersionTagAndTenantId(String decisionDefinitionKey, String decisionDefinitionVersionTag, String tenantId) {
    return decisionDefinitionCache.findDeployedDefinitionByKeyVersionTagAndTenantId(decisionDefinitionKey, decisionDefinitionVersionTag, tenantId);
  }

  public @Nullable DecisionDefinitionEntity resolveDecisionDefinition(DecisionDefinitionEntity decisionDefinition) {
    return decisionDefinitionCache.resolveDefinition(decisionDefinition);
  }

  public @Nullable DmnModelInstance findDmnModelInstanceForDecisionDefinition(String decisionDefinitionId) {
    return dmnModelInstanceCache.findBpmnModelInstanceForDefinition(decisionDefinitionId);
  }

  public void addDecisionDefinition(DecisionDefinitionEntity decisionDefinition) {
    decisionDefinitionCache.addDefinition(decisionDefinition);
  }

  public void removeDecisionDefinition(String decisionDefinitionId) {
    decisionDefinitionCache.removeDefinitionFromCache(decisionDefinitionId);
    dmnModelInstanceCache.remove(decisionDefinitionId);
  }

  public void discardDecisionDefinitionCache() {
    decisionDefinitionCache.clear();
    dmnModelInstanceCache.clear();
  }

  //DECISION REQUIREMENT DEFINITION ////////////////////////////////////////////////////////////////////////////

  public void addDecisionRequirementsDefinition(DecisionRequirementsDefinitionEntity decisionRequirementsDefinition) {
    decisionRequirementsDefinitionCache.addDefinition(decisionRequirementsDefinition);
  }

  public @Nullable DecisionRequirementsDefinitionEntity findDecisionRequirementsDefinitionFromCache(String decisionRequirementsDefinitionId) {
    return decisionRequirementsDefinitionCache.findDefinitionFromCache(decisionRequirementsDefinitionId);
  }

  public @Nullable DecisionRequirementsDefinitionEntity findDeployedDecisionRequirementsDefinitionById(String decisionRequirementsDefinitionId) {
    return decisionRequirementsDefinitionCache.findDeployedDefinitionById(decisionRequirementsDefinitionId);
  }

  public @Nullable DecisionRequirementsDefinitionEntity resolveDecisionRequirementsDefinition(DecisionRequirementsDefinitionEntity decisionRequirementsDefinition) {
    return decisionRequirementsDefinitionCache.resolveDefinition(decisionRequirementsDefinition);
  }

  public void discardDecisionRequirementsDefinitionCache() {
    decisionRequirementsDefinitionCache.clear();
  }

  public void removeDecisionRequirementsDefinition(String decisionRequirementsDefinitionId) {
    decisionRequirementsDefinitionCache.removeDefinitionFromCache(decisionRequirementsDefinitionId);
  }

  // getters and setters //////////////////////////////////////////////////////

  public Cache<String, BpmnModelInstance> getBpmnModelInstanceCache() {
    return bpmnModelInstanceCache.getCache();
  }

  public Cache<String, CmmnModelInstance> getCmmnModelInstanceCache() {
    return cmmnModelInstanceCache.getCache();
  }

  public Cache<String, DmnModelInstance> getDmnDefinitionCache() {
    return dmnModelInstanceCache.getCache();
  }

  public Cache<String, DecisionDefinitionEntity> getDecisionDefinitionCache() {
    return decisionDefinitionCache.getCache();
  }

  public Cache<String, DecisionRequirementsDefinitionEntity> getDecisionRequirementsDefinitionCache() {
    return decisionRequirementsDefinitionCache.getCache();
  }

  public Cache<String, ProcessDefinitionEntity> getProcessDefinitionCache() {
    return processDefinitionEntityCache.getCache();
  }

  public Cache<String, CaseDefinitionEntity> getCaseDefinitionCache() {
    return caseDefinitionCache.getCache();
  }

  public void setDeployers(List<Deployer> deployers) {
    this.cacheDeployer.setDeployers(deployers);
  }

  public void removeDeployment(String deploymentId) {
    bpmnModelInstanceCache.removeAllDefinitionsByDeploymentId(deploymentId);
    if (Context.getProcessEngineConfiguration().isCmmnEnabled()) {
      cmmnModelInstanceCache.removeAllDefinitionsByDeploymentId(deploymentId);
    }
    if (Context.getProcessEngineConfiguration().isDmnEnabled()) {
      dmnModelInstanceCache.removeAllDefinitionsByDeploymentId(deploymentId);
      removeAllDecisionRequirementsDefinitionsByDeploymentId(deploymentId);
    }
  }

  protected void removeAllDecisionRequirementsDefinitionsByDeploymentId(String deploymentId) {
    // remove all decision requirements definitions for a specific deployment

    List<DecisionRequirementsDefinition> allDefinitionsForDeployment = new DecisionRequirementsDefinitionQueryImpl()
        .deploymentId(deploymentId)
        .list();

    for (DecisionRequirementsDefinition decisionRequirementsDefinition : allDefinitionsForDeployment) {
      try {
        removeDecisionDefinition(decisionRequirementsDefinition.getId());
      } catch (Exception e) {
        ProcessEngineLogger.PERSISTENCE_LOGGER
            .removeEntryFromDeploymentCacheFailure("decision requirement", decisionRequirementsDefinition.getId(), e);
      }
    }
  }

  public CachePurgeReport purgeCache() {

    CachePurgeReport result = new CachePurgeReport();
    Cache<String, ProcessDefinitionEntity> processDefinitionCache = getProcessDefinitionCache();
    if (!processDefinitionCache.isEmpty()) {
      result.addPurgeInformation(CachePurgeReport.PROCESS_DEF_CACHE, processDefinitionCache.keySet());
      processDefinitionCache.clear();
    }

    Cache<String, BpmnModelInstance> theBpmnModelInstanceCache = getBpmnModelInstanceCache();
    if (!theBpmnModelInstanceCache.isEmpty()) {
      result.addPurgeInformation(CachePurgeReport.BPMN_MODEL_INST_CACHE, theBpmnModelInstanceCache.keySet());
      theBpmnModelInstanceCache.clear();
    }

    Cache<String, CaseDefinitionEntity> theCaseDefinitionCache = getCaseDefinitionCache();
    if (!theCaseDefinitionCache.isEmpty()) {
      result.addPurgeInformation(CachePurgeReport.CASE_DEF_CACHE, theCaseDefinitionCache.keySet());
      theCaseDefinitionCache.clear();
    }

    Cache<String, CmmnModelInstance> theCmmnModelInstanceCache = getCmmnModelInstanceCache();
    if (!theCmmnModelInstanceCache.isEmpty()) {
      result.addPurgeInformation(CachePurgeReport.CASE_MODEL_INST_CACHE, theCmmnModelInstanceCache.keySet());
      theCmmnModelInstanceCache.clear();
    }

    Cache<String, DecisionDefinitionEntity> theDecisionDefinitionCache = getDecisionDefinitionCache();
    if (!theDecisionDefinitionCache.isEmpty()) {
      result.addPurgeInformation(CachePurgeReport.DMN_DEF_CACHE, theDecisionDefinitionCache.keySet());
      theDecisionDefinitionCache.clear();
    }

    Cache<String, DmnModelInstance> theDmnModelInstanceCache = getDmnDefinitionCache();
    if (!theDmnModelInstanceCache.isEmpty()) {
      result.addPurgeInformation(CachePurgeReport.DMN_MODEL_INST_CACHE, theDmnModelInstanceCache.keySet());
      theDmnModelInstanceCache.clear();
    }

    Cache<String, DecisionRequirementsDefinitionEntity> theDecisionRequirementsDefinitionCache = getDecisionRequirementsDefinitionCache();
    if (!theDecisionRequirementsDefinitionCache.isEmpty()) {
      result.addPurgeInformation(CachePurgeReport.DMN_REQ_DEF_CACHE, theDecisionRequirementsDefinitionCache.keySet());
      theDecisionRequirementsDefinitionCache.clear();
    }

    return result;
  }

}
