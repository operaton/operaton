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
package org.operaton.bpm.engine.impl;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.operaton.bpm.application.ProcessApplicationReference;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.exception.DeploymentResourceNotFoundException;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.exception.cmmn.CaseDefinitionNotFoundException;
import org.operaton.bpm.engine.exception.cmmn.CmmnModelInstanceNotFoundException;
import org.operaton.bpm.engine.exception.dmn.DecisionDefinitionNotFoundException;
import org.operaton.bpm.engine.exception.dmn.DmnModelInstanceNotFoundException;
import org.operaton.bpm.engine.impl.cmd.*;
import org.operaton.bpm.engine.impl.cmmn.cmd.GetDeploymentCaseDefinitionCmd;
import org.operaton.bpm.engine.impl.cmmn.cmd.GetDeploymentCaseDiagramCmd;
import org.operaton.bpm.engine.impl.cmmn.cmd.GetDeploymentCaseModelCmd;
import org.operaton.bpm.engine.impl.cmmn.cmd.GetDeploymentCmmnModelInstanceCmd;
import org.operaton.bpm.engine.impl.cmmn.cmd.UpdateCaseDefinitionHistoryTimeToLiveCmd;
import org.operaton.bpm.engine.impl.cmmn.entity.repository.CaseDefinitionQueryImpl;
import org.operaton.bpm.engine.impl.dmn.cmd.GetDeploymentDecisionDefinitionCmd;
import org.operaton.bpm.engine.impl.dmn.cmd.GetDeploymentDecisionDiagramCmd;
import org.operaton.bpm.engine.impl.dmn.cmd.GetDeploymentDecisionModelCmd;
import org.operaton.bpm.engine.impl.dmn.cmd.GetDeploymentDecisionRequirementsDefinitionCmd;
import org.operaton.bpm.engine.impl.dmn.cmd.GetDeploymentDecisionRequirementsDiagramCmd;
import org.operaton.bpm.engine.impl.dmn.cmd.GetDeploymentDecisionRequirementsModelCmd;
import org.operaton.bpm.engine.impl.dmn.cmd.GetDeploymentDmnModelInstanceCmd;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionDefinitionQueryImpl;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionRequirementsDefinitionQueryImpl;
import org.operaton.bpm.engine.impl.pvm.ReadOnlyProcessDefinition;
import org.operaton.bpm.engine.impl.repository.DeleteProcessDefinitionsBuilderImpl;
import org.operaton.bpm.engine.impl.repository.DeploymentBuilderImpl;
import org.operaton.bpm.engine.impl.repository.ProcessApplicationDeploymentBuilderImpl;
import org.operaton.bpm.engine.impl.repository.UpdateProcessDefinitionSuspensionStateBuilderImpl;
import org.operaton.bpm.engine.repository.*;
import org.operaton.bpm.engine.task.IdentityLink;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.cmmn.CmmnModelInstance;
import org.operaton.bpm.model.dmn.DmnModelInstance;

/**
 * @author Tom Baeyens
 * @author Falko Menge
 * @author Joram Barrez
 */
public @NullMarked class RepositoryServiceImpl extends ServiceImpl implements RepositoryService {

  protected @Nullable Charset deploymentCharset;

  public @Nullable Charset getDeploymentCharset() {
    return deploymentCharset;
  }

  public void setDeploymentCharset(Charset deploymentCharset) {
    this.deploymentCharset = deploymentCharset;
  }

  @Override
  public DeploymentBuilder createDeployment() {
    return new DeploymentBuilderImpl(this);
  }

  @Override
  public ProcessApplicationDeploymentBuilder createDeployment(ProcessApplicationReference processApplication) {
    return new ProcessApplicationDeploymentBuilderImpl(this, processApplication);
  }

  public DeploymentWithDefinitions deployWithResult(DeploymentBuilderImpl deploymentBuilder) {
    return getCommandExecutor().execute(new DeployCmd(deploymentBuilder));
  }

  @Override
  public void deleteDeployment(String deploymentId) {
    getCommandExecutor().execute(new DeleteDeploymentCmd(deploymentId, false, false, false));
  }

  @Override
  public void deleteDeployment(String deploymentId, boolean cascade) {
    getCommandExecutor().execute(new DeleteDeploymentCmd(deploymentId, cascade, false, false));
  }

  @Override
  public void deleteDeployment(String deploymentId, boolean cascade, boolean skipCustomListeners) {
    getCommandExecutor().execute(new DeleteDeploymentCmd(deploymentId, cascade, skipCustomListeners, false));
  }

  @Override
  public void deleteDeployment(String deploymentId, boolean cascade, boolean skipCustomListeners, boolean skipIoMappings) {
    getCommandExecutor().execute(new DeleteDeploymentCmd(deploymentId, cascade, skipCustomListeners, skipIoMappings));
  }

  @Override
  public void deleteProcessDefinition(String processDefinitionId) {
    deleteProcessDefinition(processDefinitionId, false);
  }

  @Override
  public void deleteProcessDefinition(String processDefinitionId, boolean cascade) {
    deleteProcessDefinition(processDefinitionId, cascade, false);
  }

  @Override
  public void deleteProcessDefinition(String processDefinitionId, boolean cascade, boolean skipCustomListeners) {
    deleteProcessDefinition(processDefinitionId, cascade, skipCustomListeners, false);
  }

  @Override
  public void deleteProcessDefinition(String processDefinitionId, boolean cascade, boolean skipCustomListeners, boolean skipIoMappings) {
    DeleteProcessDefinitionsBuilder builder = deleteProcessDefinitions().byIds(processDefinitionId);

    if (cascade) {
      builder.cascade();
    }

    if (skipCustomListeners) {
      builder.skipCustomListeners();
    }

    if (skipIoMappings) {
      builder.skipIoMappings();
    }

    builder.delete();
  }

  @Override
  public DeleteProcessDefinitionsSelectBuilder deleteProcessDefinitions() {
    return new DeleteProcessDefinitionsBuilderImpl(commandExecutor);
  }

  @Override
  public ProcessDefinitionQuery createProcessDefinitionQuery() {
    return new ProcessDefinitionQueryImpl(commandExecutor);
  }

  @Override
  public CaseDefinitionQuery createCaseDefinitionQuery() {
    return new CaseDefinitionQueryImpl(commandExecutor);
  }

  @Override
  public DecisionDefinitionQuery createDecisionDefinitionQuery() {
    return new DecisionDefinitionQueryImpl(commandExecutor);
  }

  @Override
  public DecisionRequirementsDefinitionQuery createDecisionRequirementsDefinitionQuery() {
    return new DecisionRequirementsDefinitionQueryImpl(commandExecutor);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<String> getDeploymentResourceNames(String deploymentId) {
    return getCommandExecutor().execute(new GetDeploymentResourceNamesCmd(deploymentId));
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Resource> getDeploymentResources(String deploymentId) {
    return getCommandExecutor().execute(new GetDeploymentResourcesCmd(deploymentId));
  }

  @Override
  public InputStream getResourceAsStream(String deploymentId, String resourceName) {
    return getCommandExecutor().execute(new GetDeploymentResourceCmd(deploymentId, resourceName));
  }

  @Override
  public InputStream getResourceAsStreamById(String deploymentId, String resourceId) {
    return getCommandExecutor().execute(new GetDeploymentResourceForIdCmd(deploymentId, resourceId));
  }

  @Override
  public DeploymentQuery createDeploymentQuery() {
    return new DeploymentQueryImpl(commandExecutor);
  }

  @Override
  public ProcessDefinition getProcessDefinition(String processDefinitionId) {
    return getCommandExecutor().execute(new GetDeployedProcessDefinitionCmd(processDefinitionId, true));
  }

  public ReadOnlyProcessDefinition getDeployedProcessDefinition(String processDefinitionId) {
    return getCommandExecutor().execute(new GetDeployedProcessDefinitionCmd(processDefinitionId, true));
  }

  @Override
  public void suspendProcessDefinitionById(String processDefinitionId) {
    updateProcessDefinitionSuspensionState()
      .byProcessDefinitionId(processDefinitionId)
      .suspend();
  }

  @Override
  public void suspendProcessDefinitionById(String processDefinitionId, boolean suspendProcessInstances, @Nullable Date suspensionDate) {
    updateProcessDefinitionSuspensionState()
      .byProcessDefinitionId(processDefinitionId)
      .includeProcessInstances(suspendProcessInstances)
      .executionDate(suspensionDate)
      .suspend();
  }

  @Override
  public void suspendProcessDefinitionByKey(String processDefinitionKey) {
    updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(processDefinitionKey)
      .suspend();
  }

  @Override
  public void suspendProcessDefinitionByKey(String processDefinitionKey, boolean suspendProcessInstances, @Nullable Date suspensionDate) {
    updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(processDefinitionKey)
      .includeProcessInstances(suspendProcessInstances)
      .executionDate(suspensionDate)
      .suspend();
  }

  @Override
  public void activateProcessDefinitionById(String processDefinitionId) {
    updateProcessDefinitionSuspensionState()
      .byProcessDefinitionId(processDefinitionId)
      .activate();
  }

  @Override
  public void activateProcessDefinitionById(String processDefinitionId, boolean activateProcessInstances, @Nullable Date activationDate) {
    updateProcessDefinitionSuspensionState()
      .byProcessDefinitionId(processDefinitionId)
      .includeProcessInstances(activateProcessInstances)
      .executionDate(activationDate)
      .activate();
  }

  @Override
  public void activateProcessDefinitionByKey(String processDefinitionKey) {
    updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(processDefinitionKey)
      .activate();
  }

  @Override
  public void activateProcessDefinitionByKey(String processDefinitionKey, boolean activateProcessInstances, @Nullable Date activationDate) {
    updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(processDefinitionKey)
      .includeProcessInstances(activateProcessInstances)
      .executionDate(activationDate)
      .activate();
  }

  @Override
  public UpdateProcessDefinitionSuspensionStateSelectBuilder updateProcessDefinitionSuspensionState() {
    return new UpdateProcessDefinitionSuspensionStateBuilderImpl(commandExecutor);
  }

  @Override
  public void updateProcessDefinitionHistoryTimeToLive(String processDefinitionId, @Nullable Integer historyTimeToLive) {
    getCommandExecutor().execute(new UpdateProcessDefinitionHistoryTimeToLiveCmd(processDefinitionId, historyTimeToLive));
  }

  @Override
  public void updateDecisionDefinitionHistoryTimeToLive(String decisionDefinitionId, @Nullable Integer historyTimeToLive) {
    getCommandExecutor().execute(new UpdateDecisionDefinitionHistoryTimeToLiveCmd(decisionDefinitionId, historyTimeToLive));
  }

  @Override
  public void updateCaseDefinitionHistoryTimeToLive(String caseDefinitionId, @Nullable Integer historyTimeToLive) {
    getCommandExecutor().execute(new UpdateCaseDefinitionHistoryTimeToLiveCmd(caseDefinitionId, historyTimeToLive));
  }

  @Override
  public @Nullable InputStream getProcessModel(String processDefinitionId) {
    return getCommandExecutor().execute(new GetDeploymentProcessModelCmd(processDefinitionId));
  }

  @Override
  public @Nullable InputStream getProcessDiagram(String processDefinitionId) {
    return getCommandExecutor().execute(new GetDeploymentProcessDiagramCmd(processDefinitionId));
  }

  @Override
  public @Nullable InputStream getCaseDiagram(String caseDefinitionId) {
    return getCommandExecutor().execute(new GetDeploymentCaseDiagramCmd(caseDefinitionId));
  }

  @Override
  public @Nullable DiagramLayout getProcessDiagramLayout(String processDefinitionId) {
    return getCommandExecutor().execute(new GetDeploymentProcessDiagramLayoutCmd(processDefinitionId));
  }

  @Override
  public BpmnModelInstance getBpmnModelInstance(String processDefinitionId) {
    return getCommandExecutor().execute(new GetDeploymentBpmnModelInstanceCmd(processDefinitionId));
  }

  @Override
  public CmmnModelInstance getCmmnModelInstance(String caseDefinitionId) {
    try {
      return getCommandExecutor().execute(new GetDeploymentCmmnModelInstanceCmd(caseDefinitionId));
    } catch (NullValueException e) {
      throw new NotValidException(e.getMessage(), e);
    } catch (CmmnModelInstanceNotFoundException | DeploymentResourceNotFoundException e) {
      throw new NotFoundException(e.getMessage(), e);
    }
  }

  @Override
  public DmnModelInstance getDmnModelInstance(String decisionDefinitionId) {
    try {
      return getCommandExecutor().execute(new GetDeploymentDmnModelInstanceCmd(decisionDefinitionId));
    } catch (NullValueException e) {
      throw new NotValidException(e.getMessage(), e);
    } catch (DmnModelInstanceNotFoundException | DeploymentResourceNotFoundException e) {
      throw new NotFoundException(e.getMessage(), e);
    }
  }

  @Override
  public void addCandidateStarterUser(String processDefinitionId, String userId) {
    getCommandExecutor().execute(new AddIdentityLinkForProcessDefinitionCmd(processDefinitionId, userId, null));
  }

  @Override
  public void addCandidateStarterGroup(String processDefinitionId, String groupId) {
    getCommandExecutor().execute(new AddIdentityLinkForProcessDefinitionCmd(processDefinitionId, null, groupId));
  }

  @Override
  public void deleteCandidateStarterGroup(String processDefinitionId, String groupId) {
    getCommandExecutor().execute(new DeleteIdentityLinkForProcessDefinitionCmd(processDefinitionId, null, groupId));
  }

  @Override
  public void deleteCandidateStarterUser(String processDefinitionId, String userId) {
    getCommandExecutor().execute(new DeleteIdentityLinkForProcessDefinitionCmd(processDefinitionId, userId, null));
  }

  @Override
  public List<IdentityLink> getIdentityLinksForProcessDefinition(String processDefinitionId) {
    return getCommandExecutor().execute(new GetIdentityLinksForProcessDefinitionCmd(processDefinitionId));
  }

  @Override
  public CaseDefinition getCaseDefinition(String caseDefinitionId) {
    try {
      return getCommandExecutor().execute(new GetDeploymentCaseDefinitionCmd(caseDefinitionId));
    } catch (NullValueException e) {
      throw new NotValidException(e.getMessage(), e);
    } catch (CaseDefinitionNotFoundException e) {
      throw new NotFoundException(e.getMessage(), e);
    }
  }

  @Override
  public @Nullable InputStream getCaseModel(String caseDefinitionId) {
    try {
      return getCommandExecutor().execute(new GetDeploymentCaseModelCmd(caseDefinitionId));
    } catch (NullValueException e) {
      throw new NotValidException(e.getMessage(), e);
    } catch (CaseDefinitionNotFoundException | DeploymentResourceNotFoundException e) {
      throw new NotFoundException(e.getMessage(), e);
    }
  }

  @Override
  public DecisionDefinition getDecisionDefinition(String decisionDefinitionId) {
    try {
      return getCommandExecutor().execute(new GetDeploymentDecisionDefinitionCmd(decisionDefinitionId));
    } catch (NullValueException e) {
      throw new NotValidException(e.getMessage(), e);
    } catch (DecisionDefinitionNotFoundException e) {
      throw new NotFoundException(e.getMessage(), e);
    }
  }

  @Override
  public DecisionRequirementsDefinition getDecisionRequirementsDefinition(String decisionRequirementsDefinitionId) {
    try {
      return getCommandExecutor().execute(new GetDeploymentDecisionRequirementsDefinitionCmd(decisionRequirementsDefinitionId));
    } catch (NullValueException e) {
      throw new NotValidException(e.getMessage(), e);
    } catch (DecisionDefinitionNotFoundException e) {
      throw new NotFoundException(e.getMessage(), e);
    }
  }

  @Override
  public InputStream getDecisionModel(String decisionDefinitionId) {
    try {
      return getCommandExecutor().execute(new GetDeploymentDecisionModelCmd(decisionDefinitionId));
    } catch (NullValueException e) {
      throw new NotValidException(e.getMessage(), e);
    } catch (DecisionDefinitionNotFoundException | DeploymentResourceNotFoundException e) {
      throw new NotFoundException(e.getMessage(), e);
    }
  }

  @Override
  public InputStream getDecisionRequirementsModel(String decisionRequirementsDefinitionId) {
    try {
      return getCommandExecutor().execute(new GetDeploymentDecisionRequirementsModelCmd(decisionRequirementsDefinitionId));
    } catch (NullValueException e) {
      throw new NotValidException(e.getMessage(), e);
    } catch (DecisionDefinitionNotFoundException | DeploymentResourceNotFoundException e) {
      throw new NotFoundException(e.getMessage(), e);
    }
  }

  @Override
  @Nullable public InputStream getDecisionDiagram(String decisionDefinitionId) {
    return getCommandExecutor().execute(new GetDeploymentDecisionDiagramCmd(decisionDefinitionId));
  }

  @Override
  public @Nullable InputStream getDecisionRequirementsDiagram(String decisionRequirementsDefinitionId) {
    return getCommandExecutor().execute(new GetDeploymentDecisionRequirementsDiagramCmd(decisionRequirementsDefinitionId));
  }

  @Override
  public Collection<CalledProcessDefinition> getStaticCalledProcessDefinitions(String processDefinitionId) {
    return getCommandExecutor().execute(new GetStaticCalledProcessDefinitionCmd(processDefinitionId));
  }

}
