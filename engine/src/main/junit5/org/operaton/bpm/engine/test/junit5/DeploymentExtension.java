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
package org.operaton.bpm.engine.test.junit5;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.repository.DeploymentBuilder;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static java.util.Objects.requireNonNull;

public class DeploymentExtension implements AfterEachCallback, BeforeEachCallback {
    protected RepositoryService repositoryService;
    protected String deploymentId;
    protected Set<String> deploymentIds = new HashSet<>();

    public DeploymentExtension() {}
    public DeploymentExtension(RepositoryService repositoryService) {
        requireNonNull(repositoryService);
        this.repositoryService = repositoryService;
    }
    public  String deploymentForTenant(String tenantId, BpmnModelInstance... bpmnModelInstances) {
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment().tenantId(tenantId);

        return deploy(deploymentBuilder, bpmnModelInstances);
    }

    public  String deploymentForTenant(String tenantId, String... resources) {
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment().tenantId(tenantId);

        return deploy(deploymentBuilder, resources);
    }

    public  String deploymentForTenant(String tenantId, String classpathResource, BpmnModelInstance modelInstance) {
        return deploy(repositoryService.createDeployment()
                .tenantId(tenantId)
                .addClasspathResource(classpathResource), modelInstance);
    }

    public  String deploy(DeploymentBuilder deploymentBuilder, BpmnModelInstance... bpmnModelInstances) {
        for (int i = 0; i < bpmnModelInstances.length; i++) {
            BpmnModelInstance bpmnModelInstance = bpmnModelInstances[i];
            deploymentBuilder.addModelInstance("testProcess-%s.bpmn".formatted(i), bpmnModelInstance);
        }

        return deploymentWithBuilder(deploymentBuilder);
    }

    public  String deploy(DeploymentBuilder deploymentBuilder, String... resources) {
        Stream.of(resources).forEach(deploymentBuilder::addClasspathResource);

        return deploymentWithBuilder(deploymentBuilder);
    }

    public String deploy(BpmnModelInstance... bpmnModelInstances) {
        Objects.requireNonNull(repositoryService, "RepositoryService not initialized. Either pass one at construction time or make sure ProcessEngineExtension is registered");
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();

        return deploy(deploymentBuilder, bpmnModelInstances);
    }

    public String deploy(String... resources) {
        Objects.requireNonNull(repositoryService, "RepositoryService not initialized. Either pass one at construction time or make sure ProcessEngineExtension is registered");
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();

        return deploy(deploymentBuilder, resources);
    }

    public String deploymentWithBuilder(DeploymentBuilder builder) {
        deploymentId = builder.deploy().getId();
        deploymentIds.add(deploymentId);

        return deploymentId;
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        ProcessEngine processEngine = (ProcessEngine) context.getStore(ExtensionContext.Namespace.create("Operaton")).get(ProcessEngine.class);
        if (processEngine != null && repositoryService == null) {
            repositoryService = processEngine.getRepositoryService();
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        deploymentIds.forEach(id -> repositoryService.deleteDeployment(id, true));
    }

}
