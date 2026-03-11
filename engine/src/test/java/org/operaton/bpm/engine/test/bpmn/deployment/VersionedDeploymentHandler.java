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
package org.operaton.bpm.engine.test.bpmn.deployment;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.impl.util.StringUtil;
import org.operaton.bpm.engine.repository.CandidateDeployment;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.DeploymentHandler;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.repository.Resource;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.Process;

import static org.operaton.bpm.engine.impl.ResourceSuffixes.BPMN_RESOURCE_SUFFIXES;
import static java.util.Collections.emptySet;

public class VersionedDeploymentHandler implements DeploymentHandler {

  protected ProcessEngine processEngine;
  protected RepositoryService repositoryService;
  protected String candidateVersionTag;
  protected String candidateProcessDefinitionKey;

  public VersionedDeploymentHandler(ProcessEngine processEngine) {
    this.processEngine = processEngine;
    this.repositoryService = processEngine.getRepositoryService();
  }

  @Override
  public boolean shouldDeployResource(Resource newResource, Resource existingResource) {

    if (isBpmnResource(newResource)) {

      Integer existingVersion = parseOperatonVersionTag(existingResource);
      Integer newVersion = parseOperatonVersionTag(newResource);
      if (this.candidateVersionTag == null) {
        this.candidateProcessDefinitionKey = parseProcessDefinitionKey(newResource);
        this.candidateVersionTag = String.valueOf(newVersion);
      }

      return newVersion > existingVersion;
    }

    return false;
  }

  @Override
  public String determineDuplicateDeployment(CandidateDeployment candidateDeployment) {

    String deploymentId = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey(candidateProcessDefinitionKey)
        .versionTag(candidateVersionTag)
        .orderByProcessDefinitionVersion()
        .desc()
        .singleResult()
        .getDeploymentId();

    return repositoryService.createDeploymentQuery()
        .deploymentId(deploymentId).singleResult().getId();
  }

  @Override
  public Set<String> determineDeploymentsToResumeByProcessDefinitionKey(
      String[] processDefinitionKeys) {
    // If all the resources are new, the candidateVersionTag
    // property will be null since there's nothing to compare it to.
    if (candidateVersionTag == null) {
      return emptySet();
    }

    return repositoryService.createProcessDefinitionQuery().processDefinitionKeyIn(processDefinitionKeys).list()
            .stream()
            .filter(processDefinition -> candidateVersionTag.equals(processDefinition.getVersionTag()))
            .map(ProcessDefinition::getDeploymentId)
            .collect(Collectors.toSet());
  }

  @Override
  public Set<String> determineDeploymentsToResumeByDeploymentName(CandidateDeployment candidateDeployment) {
    // If all the resources are new, the candidateVersionTag
    // property will be null since there's nothing to compare it to.
    if (candidateVersionTag == null) {
      return emptySet();
    }

    Set<String> deploymentIds = new HashSet<>();

    List<Deployment> previousDeployments = processEngine.getRepositoryService()
        .createDeploymentQuery().deploymentName(candidateDeployment.getName()).list();

    for (Deployment deployment : previousDeployments) {
      // find the Process Definitions included in this deployment
      List<ProcessDefinition> deploymentPDs = repositoryService.createProcessDefinitionQuery()
          .deploymentId(deployment.getId())
          .list();

      for (ProcessDefinition processDefinition : deploymentPDs) {
        // only deploy Deployments of the same name that contain a Process Definition with the
        // correct Operaton Version Tag.
        if (candidateVersionTag.equals(processDefinition.getVersionTag())) {
          deploymentIds.add(deployment.getId());
          break;
        }
      }
    }

    return deploymentIds;
  }

  protected Integer parseOperatonVersionTag(Resource resource) {
    BpmnModelInstance model = Bpmn
        .readModelFromStream(new ByteArrayInputStream(resource.getBytes()));

    Process process = model.getDefinitions().getChildElementsByType(Process.class)
        .iterator().next();

    return process.getOperatonVersionTag() != null ?
        Integer.parseInt(process.getOperatonVersionTag()) :
        0;
  }

  protected String parseProcessDefinitionKey(Resource resource) {
    BpmnModelInstance model = Bpmn
        .readModelFromStream(new ByteArrayInputStream(resource.getBytes()));

    Process process = model.getDefinitions().getChildElementsByType(Process.class)
        .iterator().next();

    return process.getId();
  }

  protected boolean isBpmnResource(Resource resource) {
    return StringUtil.hasAnySuffix(resource.getName(), BPMN_RESOURCE_SUFFIXES);
  }
}
