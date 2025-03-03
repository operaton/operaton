package org.operaton.bpm.engine.test.junit5;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.repository.DeploymentBuilder;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

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
            deploymentBuilder.addModelInstance("testProcess-"+i+".bpmn", bpmnModelInstance);
        }

        return deploymentWithBuilder(deploymentBuilder);
    }

    public  String deploy(DeploymentBuilder deploymentBuilder, String... resources) {
        for (int i = 0; i < resources.length; i++) {
            deploymentBuilder.addClasspathResource(resources[i]);
        }

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
    public void beforeEach(ExtensionContext context) throws Exception {
        ProcessEngine processEngine = (ProcessEngine) context.getStore(ExtensionContext.Namespace.create("Operaton")).get(ProcessEngine.class);
        if (processEngine != null && repositoryService == null) {
            repositoryService = processEngine.getRepositoryService();;
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        deploymentIds.stream().forEach(id -> repositoryService.deleteDeployment(id, true));
    }

}
