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
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Date;
import java.util.List;

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
import org.operaton.bpm.engine.impl.cmd.AddIdentityLinkForProcessDefinitionCmd;
import org.operaton.bpm.engine.impl.cmd.DeleteDeploymentCmd;
import org.operaton.bpm.engine.impl.cmd.DeleteIdentityLinkForProcessDefinitionCmd;
import org.operaton.bpm.engine.impl.cmd.DeployCmd;
import org.operaton.bpm.engine.impl.cmd.GetDeployedProcessDefinitionCmd;
import org.operaton.bpm.engine.impl.cmd.GetDeploymentBpmnModelInstanceCmd;
import org.operaton.bpm.engine.impl.cmd.GetDeploymentProcessDiagramCmd;
import org.operaton.bpm.engine.impl.cmd.GetDeploymentProcessDiagramLayoutCmd;
import org.operaton.bpm.engine.impl.cmd.GetDeploymentProcessModelCmd;
import org.operaton.bpm.engine.impl.cmd.GetDeploymentResourceCmd;
import org.operaton.bpm.engine.impl.cmd.GetDeploymentResourceForIdCmd;
import org.operaton.bpm.engine.impl.cmd.GetDeploymentResourceNamesCmd;
import org.operaton.bpm.engine.impl.cmd.GetDeploymentResourcesCmd;
import org.operaton.bpm.engine.impl.cmd.GetIdentityLinksForProcessDefinitionCmd;
import org.operaton.bpm.engine.impl.cmd.GetStaticCalledProcessDefinitionCmd;
import org.operaton.bpm.engine.impl.cmd.UpdateDecisionDefinitionHistoryTimeToLiveCmd;
import org.operaton.bpm.engine.impl.cmd.UpdateProcessDefinitionHistoryTimeToLiveCmd;
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
public class RepositoryServiceImpl extends ServiceImpl implements RepositoryService {

  protected Charset deploymentCharset;

  public Charset getDeploymentCharset() {
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
    return commandExecutor.execute(new DeployCmd(deploymentBuilder));
  }

  @Override
  public void deleteDeployment(String deploymentId) {
    commandExecutor.execute(new DeleteDeploymentCmd(deploymentId, false, false, false));
  }

  @Override
  public void deleteDeployment(String deploymentId, boolean cascade) {
    commandExecutor.execute(new DeleteDeploymentCmd(deploymentId, cascade, false, false));
  }

  @Override
  public void deleteDeployment(String deploymentId, boolean cascade, boolean skipCustomListeners) {
    commandExecutor.execute(new DeleteDeploymentCmd(deploymentId, cascade, skipCustomListeners, false));
  }

  @Override
  public void deleteDeployment(String deploymentId, boolean cascade, boolean skipCustomListeners, boolean skipIoMappings) {
    commandExecutor.execute(new DeleteDeploymentCmd(deploymentId, cascade, skipCustomListeners, skipIoMappings));
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
    return commandExecutor.execute(new GetDeploymentResourceNamesCmd(deploymentId));
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Resource> getDeploymentResources(String deploymentId) {
    return commandExecutor.execute(new GetDeploymentResourcesCmd(deploymentId));
  }

  @Override
  public InputStream getResourceAsStream(String deploymentId, String resourceName) {
    return commandExecutor.execute(new GetDeploymentResourceCmd(deploymentId, resourceName));
  }

  @Override
  public InputStream getResourceAsStreamById(String deploymentId, String resourceId) {
    return commandExecutor.execute(new GetDeploymentResourceForIdCmd(deploymentId, resourceId));
  }

  @Override
  public DeploymentQuery createDeploymentQuery() {
    return new DeploymentQueryImpl(commandExecutor);
  }

  @Override
  public ProcessDefinition getProcessDefinition(String processDefinitionId) {
    return commandExecutor.execute(new GetDeployedProcessDefinitionCmd(processDefinitionId, true));
  }

  public ReadOnlyProcessDefinition getDeployedProcessDefinition(String processDefinitionId) {
    return commandExecutor.execute(new GetDeployedProcessDefinitionCmd(processDefinitionId, true));
  }

  @Override
  public void suspendProcessDefinitionById(String processDefinitionId) {
    updateProcessDefinitionSuspensionState()
      .byProcessDefinitionId(processDefinitionId)
      .suspend();
  }

  @Override
  public void suspendProcessDefinitionById(String processDefinitionId, boolean suspendProcessInstances, Date suspensionDate) {
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
  public void suspendProcessDefinitionByKey(String processDefinitionKey, boolean suspendProcessInstances, Date suspensionDate) {
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
  public void activateProcessDefinitionById(String processDefinitionId, boolean activateProcessInstances, Date activationDate) {
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
  public void activateProcessDefinitionByKey(String processDefinitionKey, boolean activateProcessInstances, Date activationDate) {
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
  public void updateProcessDefinitionHistoryTimeToLive(String processDefinitionId, Integer historyTimeToLive) {
    commandExecutor.execute(new UpdateProcessDefinitionHistoryTimeToLiveCmd(processDefinitionId, historyTimeToLive));
  }

  @Override
  public void updateDecisionDefinitionHistoryTimeToLive(String decisionDefinitionId, Integer historyTimeToLive) {
    commandExecutor.execute(new UpdateDecisionDefinitionHistoryTimeToLiveCmd(decisionDefinitionId, historyTimeToLive));
  }

  @Override
  public void updateCaseDefinitionHistoryTimeToLive(String caseDefinitionId, Integer historyTimeToLive) {
    commandExecutor.execute(new UpdateCaseDefinitionHistoryTimeToLiveCmd(caseDefinitionId, historyTimeToLive));
  }

  @Override
  public InputStream getProcessModel(String processDefinitionId) {
    return commandExecutor.execute(new GetDeploymentProcessModelCmd(processDefinitionId));
  }

  @Override
  public InputStream getProcessDiagram(String processDefinitionId) {
    return commandExecutor.execute(new GetDeploymentProcessDiagramCmd(processDefinitionId));
  }

  @Override
  public InputStream getCaseDiagram(String caseDefinitionId) {
    return commandExecutor.execute(new GetDeploymentCaseDiagramCmd(caseDefinitionId));
  }

  @Override
  public DiagramLayout getProcessDiagramLayout(String processDefinitionId) {
    return commandExecutor.execute(new GetDeploymentProcessDiagramLayoutCmd(processDefinitionId));
  }

  @Override
  public BpmnModelInstance getBpmnModelInstance(String processDefinitionId) {
    return commandExecutor.execute(new GetDeploymentBpmnModelInstanceCmd(processDefinitionId));
  }

  @Override
  public CmmnModelInstance getCmmnModelInstance(String caseDefinitionId) {
    try {
      return commandExecutor.execute(new GetDeploymentCmmnModelInstanceCmd(caseDefinitionId));
    } catch (NullValueException e) {
      throw new NotValidException(e.getMessage(), e);
    } catch (CmmnModelInstanceNotFoundException | DeploymentResourceNotFoundException e) {
      throw new NotFoundException(e.getMessage(), e);
    }
  }

  @Override
  public DmnModelInstance getDmnModelInstance(String decisionDefinitionId) {
    try {
      return commandExecutor.execute(new GetDeploymentDmnModelInstanceCmd(decisionDefinitionId));
    } catch (NullValueException e) {
      throw new NotValidException(e.getMessage(), e);
    } catch (DmnModelInstanceNotFoundException | DeploymentResourceNotFoundException e) {
      throw new NotFoundException(e.getMessage(), e);
    }
  }

  @Override
  public void addCandidateStarterUser(String processDefinitionId, String userId) {
    commandExecutor.execute(new AddIdentityLinkForProcessDefinitionCmd(processDefinitionId, userId, null));
  }

  @Override
  public void addCandidateStarterGroup(String processDefinitionId, String groupId) {
    commandExecutor.execute(new AddIdentityLinkForProcessDefinitionCmd(processDefinitionId, null, groupId));
  }

  @Override
  public void deleteCandidateStarterGroup(String processDefinitionId, String groupId) {
    commandExecutor.execute(new DeleteIdentityLinkForProcessDefinitionCmd(processDefinitionId, null, groupId));
  }

  @Override
  public void deleteCandidateStarterUser(String processDefinitionId, String userId) {
    commandExecutor.execute(new DeleteIdentityLinkForProcessDefinitionCmd(processDefinitionId, userId, null));
  }

  @Override
  public List<IdentityLink> getIdentityLinksForProcessDefinition(String processDefinitionId) {
    return commandExecutor.execute(new GetIdentityLinksForProcessDefinitionCmd(processDefinitionId));
  }

  @Override
  public CaseDefinition getCaseDefinition(String caseDefinitionId) {
    try {
      return commandExecutor.execute(new GetDeploymentCaseDefinitionCmd(caseDefinitionId));

    } catch (NullValueException e) {
      throw new NotValidException(e.getMessage(), e);

    } catch (CaseDefinitionNotFoundException e) {
      throw new NotFoundException(e.getMessage(), e);

    }
  }

  @Override
  public InputStream getCaseModel(String caseDefinitionId) {
    try {
      return commandExecutor.execute(new GetDeploymentCaseModelCmd(caseDefinitionId));
    } catch (NullValueException e) {
      throw new NotValidException(e.getMessage(), e);
    } catch (CaseDefinitionNotFoundException | DeploymentResourceNotFoundException e) {
      throw new NotFoundException(e.getMessage(), e);
    }
  }

  @Override
  public DecisionDefinition getDecisionDefinition(String decisionDefinitionId) {
    try {
      return commandExecutor.execute(new GetDeploymentDecisionDefinitionCmd(decisionDefinitionId));
    } catch (NullValueException e) {
      throw new NotValidException(e.getMessage(), e);
    } catch (DecisionDefinitionNotFoundException e) {
      throw new NotFoundException(e.getMessage(), e);
    }
  }

  @Override
  public DecisionRequirementsDefinition getDecisionRequirementsDefinition(String decisionRequirementsDefinitionId) {
    try {
      return commandExecutor.execute(new GetDeploymentDecisionRequirementsDefinitionCmd(decisionRequirementsDefinitionId));
    } catch (NullValueException e) {
      throw new NotValidException(e.getMessage(), e);
    } catch (DecisionDefinitionNotFoundException e) {
      throw new NotFoundException(e.getMessage(), e);
    }
  }

  @Override
  public InputStream getDecisionModel(String decisionDefinitionId) {
    try {
      return commandExecutor.execute(new GetDeploymentDecisionModelCmd(decisionDefinitionId));
    } catch (NullValueException e) {
      throw new NotValidException(e.getMessage(), e);
    } catch (DecisionDefinitionNotFoundException | DeploymentResourceNotFoundException e) {
      throw new NotFoundException(e.getMessage(), e);
    }
  }

  @Override
  public InputStream getDecisionRequirementsModel(String decisionRequirementsDefinitionId) {
    try {
      return commandExecutor.execute(new GetDeploymentDecisionRequirementsModelCmd(decisionRequirementsDefinitionId));
    } catch (NullValueException e) {
      throw new NotValidException(e.getMessage(), e);
    } catch (DecisionDefinitionNotFoundException | DeploymentResourceNotFoundException e) {
      throw new NotFoundException(e.getMessage(), e);
    }
  }

  @Override
  public InputStream getDecisionDiagram(String decisionDefinitionId) {
    return commandExecutor.execute(new GetDeploymentDecisionDiagramCmd(decisionDefinitionId));
  }

  @Override
  public InputStream getDecisionRequirementsDiagram(String decisionRequirementsDefinitionId) {
    return commandExecutor.execute(new GetDeploymentDecisionRequirementsDiagramCmd(decisionRequirementsDefinitionId));
  }

  @Override
  public Collection<CalledProcessDefinition> getStaticCalledProcessDefinitions(String processDefinitionId) {
    return commandExecutor.execute(new GetStaticCalledProcessDefinitionCmd(processDefinitionId));
  }

}
