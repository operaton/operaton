/*
 * Copyright and/or licensed under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. This file is licensed to you under the Apache License,
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
package org.operaton.bpm.engine.test.junit5;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimerTask;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.opentest4j.AssertionFailedError;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.delegate.Expression;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmmn.behavior.CaseControlRuleImpl;
import org.operaton.bpm.engine.impl.el.FixedValue;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobManager;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.DeploymentBuilder;
import org.operaton.bpm.engine.repository.DeploymentWithDefinitions;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.util.JobExecutorHelper;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 * JUnit 5 Extension for managing a ProcessEngine during tests.
 * <p>
 * This extension provides many of the utility methods from your former JUnit 4
 * rule. It now has a default no-args constructor that creates a ProcessEngine
 * using the default configuration. This allows you to register it via:
 * 
 * <pre>
 * &#64;RegisterExtension
 * protected ProcessEngineTestExtension testRule = new ProcessEngineTestExtension();
 * </pre>
 * </p>
 */
public class ProcessEngineTestExtension
		implements BeforeEachCallback, AfterEachCallback {

  public static final String DEFAULT_BPMN_RESOURCE_NAME = "process.bpmn20.xml";

  private ProcessEngineExtension processEngineRule;
  private ProcessEngine processEngine;

  public ProcessEngineTestExtension() {
  }
  
  public ProcessEngineTestExtension(ProcessEngineExtension processEngineExtension) {
    this.processEngineRule = processEngineExtension;
    this.processEngine = processEngineRule.getProcessEngine();
  }

  public ProcessEngine getProcessEngine() {
    return processEngineRule.getProcessEngine();
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    if (processEngineRule != null)
      this.processEngine = processEngineRule.getProcessEngine();
    else
      this.processEngine = (ProcessEngine) context.getStore(ExtensionContext.Namespace.create("Operaton")).get(ProcessEngine.class);
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    this.processEngine = null;
  }

  public void assertProcessEnded(String processInstanceId) {
    ProcessInstance processInstance = processEngine
      .getRuntimeService()
      .createProcessInstanceQuery()
      .processInstanceId(processInstanceId)
      .singleResult();

    assertThat(processInstance).describedAs("Process instance with id " + processInstanceId + " is not finished").isNull();
  }

  public void assertProcessNotEnded(final String processInstanceId) {
    ProcessInstance processInstance = processEngine
      .getRuntimeService()
      .createProcessInstanceQuery()
      .processInstanceId(processInstanceId)
      .singleResult();

    if (processInstance==null) {
      throw new AssertionFailedError("Expected process instance '"+processInstanceId+"' to be still active but it was not in the db");
    }
  }


  public void assertCaseEnded(String caseInstanceId) {
    CaseInstance caseInstance = processEngine
      .getCaseService()
      .createCaseInstanceQuery()
      .caseInstanceId(caseInstanceId)
      .singleResult();

    assertThat(caseInstance).describedAs("Case instance with id " + caseInstanceId + " is not finished").isNull();
  }

  public DeploymentWithDefinitions deploy(BpmnModelInstance... bpmnModelInstances) {
    return deploy(createDeploymentBuilder(), Arrays.asList(bpmnModelInstances), Collections.<String> emptyList());
  }

  public DeploymentWithDefinitions deploy(String... resources) {
    return deploy(createDeploymentBuilder(), Collections.<BpmnModelInstance> emptyList(), Arrays.asList(resources));
  }

  public <T extends DeploymentWithDefinitions> T deploy(DeploymentBuilder deploymentBuilder) {
    T deployment = (T) deploymentBuilder.deployWithResult();

    processEngineRule.manageDeployment(deployment);

    return deployment;
  }

  public Deployment deploy(BpmnModelInstance bpmnModelInstance, String resource) {
    return deploy(createDeploymentBuilder(), Collections.singletonList(bpmnModelInstance), Collections.singletonList(resource));
  }

  public Deployment deployForTenant(String tenantId, BpmnModelInstance... bpmnModelInstances) {
    return deploy(createDeploymentBuilder().tenantId(tenantId), Arrays.asList(bpmnModelInstances), Collections.<String> emptyList());
  }

  public Deployment deployForTenant(String tenantId, String... resources) {
    return deploy(createDeploymentBuilder().tenantId(tenantId), Collections.<BpmnModelInstance> emptyList(), Arrays.asList(resources));
  }

  public Deployment deployForTenant(String tenant, BpmnModelInstance bpmnModelInstance, String resource) {
    return deploy(createDeploymentBuilder().tenantId(tenant), Collections.singletonList(bpmnModelInstance), Collections.singletonList(resource));
  }

  public ProcessDefinition deployAndGetDefinition(BpmnModelInstance bpmnModel) {
    return deployForTenantAndGetDefinition(null, bpmnModel);
  }

  public ProcessDefinition deployAndGetDefinition(String classpathResource) {
    return deployForTenantAndGetDefinition(null, classpathResource);
  }

  public ProcessDefinition deployForTenantAndGetDefinition(String tenant, String classpathResource) {
    Deployment deployment = deploy(createDeploymentBuilder().tenantId(tenant), Collections.<BpmnModelInstance>emptyList(), Collections.singletonList(classpathResource));

    return processEngine.getRepositoryService()
      .createProcessDefinitionQuery()
      .deploymentId(deployment.getId())
      .singleResult();
  }

  public ProcessDefinition deployForTenantAndGetDefinition(String tenant, BpmnModelInstance bpmnModel) {
    Deployment deployment = deploy(createDeploymentBuilder().tenantId(tenant), Collections.singletonList(bpmnModel), Collections.<String>emptyList());

    return processEngine.getRepositoryService()
      .createProcessDefinitionQuery()
      .deploymentId(deployment.getId())
      .singleResult();
  }

  protected DeploymentWithDefinitions deploy(DeploymentBuilder deploymentBuilder, List<BpmnModelInstance> bpmnModelInstances, List<String> resources) {
    int i = 0;
    for (BpmnModelInstance bpmnModelInstance : bpmnModelInstances) {
      deploymentBuilder.addModelInstance(i + "_" + DEFAULT_BPMN_RESOURCE_NAME, bpmnModelInstance);
      i++;
    }

    for (String resource : resources) {
      deploymentBuilder.addClasspathResource(resource);
    }

    return deploy(deploymentBuilder);
  }

  protected DeploymentBuilder createDeploymentBuilder() {
    return processEngine.getRepositoryService().createDeployment();
  }

  public void waitForJobExecutorToProcessAllJobs() {
    JobExecutorHelper.waitForJobExecutorToProcessAllJobs((ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration(), JobExecutorHelper.JOBS_WAIT_TIMEOUT_MS, 0L);
  }

  public void waitForJobExecutorToProcessAllJobs(long maxMillisToWait) {
    JobExecutorHelper.waitForJobExecutorToProcessAllJobs((ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration(), maxMillisToWait, JobExecutorHelper.CHECK_INTERVAL_MS);
  }

  protected List<Job> availableJobs() {
    return processEngine.getManagementService().createJobQuery().list().stream()
            .filter(job -> !job.isSuspended() && job.getRetries() > 0 && (job.getDuedate() == null || ClockUtil.getCurrentTime().after(job.getDuedate())))
            .toList();
  }

  public boolean areJobsAvailable() {
    return !availableJobs().isEmpty();
  }

  /**
   * Execute all available jobs recursively till no more jobs found.
   */
  public void executeAvailableJobs() {
    executeAvailableJobs(0, Integer.MAX_VALUE, true);
  }

  public void executeAvailableJobs(Boolean recursive) {
    executeAvailableJobs(0, Integer.MAX_VALUE, recursive);
  }

  /**
   * Execute all available jobs recursively till no more jobs found or the number of executions is higher than expected.
   *
   * @param expectedExecutions number of expected job executions
   *
   * @throws AssertionFailedError when execute less or more jobs than expected
   *
   * @see #executeAvailableJobs()
   */
  public void executeAvailableJobs(int expectedExecutions) {
    executeAvailableJobs(0, expectedExecutions, true);
  }

  public void executeAvailableJobs(int expectedExecutions, Boolean recursive) {
    executeAvailableJobs(0, expectedExecutions, recursive);
  }

  private void executeAvailableJobs(int jobsExecuted, int expectedExecutions, Boolean recursive) {
    List<Job> jobs = processEngine.getManagementService().createJobQuery().withRetriesLeft().list();

    if (jobs.isEmpty()) {
      if (expectedExecutions != Integer.MAX_VALUE) {
        assertThat(jobsExecuted).describedAs("executed less jobs than expected.").isEqualTo(expectedExecutions);
      }
      return;
    }

    for (Job job : jobs) {
      try {
        processEngine.getManagementService().executeJob(job.getId());
        jobsExecuted += 1;
      } catch (Exception e) {}
    }

    assertThat(jobsExecuted).describedAs("executed more jobs than expected.").isLessThanOrEqualTo(expectedExecutions);

    if (recursive) {
      executeAvailableJobs(jobsExecuted, expectedExecutions, recursive);
    }
  }

  public void completeTask(String taskKey) {
    TaskService taskService = processEngine.getTaskService();
    Task task = taskService.createTaskQuery().taskDefinitionKey(taskKey).singleResult();
    assertThat(task).as("Expected a task with key '" + taskKey + "' to exist").isNotNull();
    taskService.complete(task.getId());
  }

  public void completeAnyTask(String taskKey) {
    TaskService taskService = processEngine.getTaskService();
    List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey(taskKey).list();
    assertThat(!tasks.isEmpty()).isTrue();
    taskService.complete(tasks.get(0).getId());
  }

  public void setAnyVariable(String executionId) {
    setVariable(executionId, "any", "any");
  }

  public void setVariable(String executionId, String varName, Object varValue) {
    processEngine.getRuntimeService().setVariable(executionId, varName, varValue);
  }

  public void correlateMessage(String messageName) {
    processEngine.getRuntimeService().createMessageCorrelation(messageName).correlate();
  }

  public void sendSignal(String signalName) {
    processEngine.getRuntimeService().signalEventReceived(signalName);
  }

  public boolean isHistoryLevelNone() {
    HistoryLevel historyLevel = ((ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration()).getHistoryLevel();
    return HistoryLevel.HISTORY_LEVEL_NONE.equals(historyLevel);
  }

  public boolean isHistoryLevelActivity() {
    HistoryLevel historyLevel = ((ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration()).getHistoryLevel();
    return HistoryLevel.HISTORY_LEVEL_ACTIVITY.equals(historyLevel);
  }

  public boolean isHistoryLevelAudit() {
    HistoryLevel historyLevel = ((ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration()).getHistoryLevel();
    return HistoryLevel.HISTORY_LEVEL_AUDIT.equals(historyLevel);
  }

  public boolean isHistoryLevelFull() {
    HistoryLevel historyLevel = ((ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration()).getHistoryLevel();
    return HistoryLevel.HISTORY_LEVEL_FULL.equals(historyLevel);
  }

  /**
   * Asserts if the provided text is part of some text.
   */
  public void assertTextPresent(String expected, String actual) {
    if ( (actual==null)
      || (actual.indexOf(expected)==-1)
      ) {
      throw new AssertionFailedError("expected presence of ["+expected+"], but was ["+actual+"]");
    }
  }

  /**
   * Asserts if the provided text is part of some text, ignoring any uppercase characters
   */
  public void assertTextPresentIgnoreCase(String expected, String actual) {
    assertTextPresent(expected.toLowerCase(), actual.toLowerCase());
  }

  public Object defaultManualActivation() {
    Expression expression = new FixedValue(true);
    return new CaseControlRuleImpl(expression);
  }


  public void deleteHistoryCleanupJobs() {
    HistoryService historyService = processEngine.getHistoryService();
    final List<Job> jobs = historyService.findHistoryCleanupJobs();
    for (final Job job : jobs) {
      final String jobId = job.getId();

      ((ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration())
        .getCommandExecutorTxRequired()
        .execute((Command<Void>) commandContext -> {
          JobManager jobManager = commandContext.getJobManager();

          JobEntity jobEntity = jobManager.findJobById(jobId);

          jobEntity.delete();
          commandContext.getHistoricJobLogManager().deleteHistoricJobLogByJobId(job.getId());
          return null;
        });
    }
  }

  public CaseInstance createCaseInstanceByKey(String caseDefinitionKey) {
    return createCaseInstanceByKey(caseDefinitionKey, null, null);
  }

  public CaseInstance createCaseInstanceByKey(String caseDefinitionKey, String businessKey) {
    return createCaseInstanceByKey(caseDefinitionKey, businessKey, null);
  }

  public CaseInstance createCaseInstanceByKey(String caseDefinitionKey, VariableMap variables) {
    return createCaseInstanceByKey(caseDefinitionKey, null, variables);
  }

  public CaseInstance createCaseInstanceByKey(String caseDefinitionKey, String businessKey, VariableMap variables) {
    return processEngine.getCaseService()
        .withCaseDefinitionByKey(caseDefinitionKey)
        .businessKey(businessKey)
        .setVariables(variables)
        .create();
  }

  public String getDatabaseType() {
    return ((ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration())
        .getDbSqlSessionFactory()
        .getDatabaseType();
  }

  public void deleteAllAuthorizations() {
    AuthorizationService authorizationService = processEngine.getAuthorizationService();

    authorizationService.createAuthorizationQuery()
      .list()
      .stream()
      .map(Authorization::getId)
      .forEach(authorizationService::deleteAuthorization);
  }


  public void deleteAllStandaloneTasks() {
    TaskService taskService = processEngine.getTaskService();

    taskService.createTaskQuery()
      .list()
      .stream()
      .filter(t -> t.getProcessInstanceId() == null && t.getCaseInstanceId() == null)
      .forEach(t -> taskService.deleteTask(t.getId(), true));
  }

  public void createGrantAuthorization(String userId, Resource resource, String resourceId, Permission... permissions) {
    AuthorizationService authorizationService = processEngine.getAuthorizationService();

    Authorization processInstanceAuthorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
    processInstanceAuthorization.setResource(resource);
    processInstanceAuthorization.setResourceId(resourceId);
    processInstanceAuthorization.setPermissions(permissions);
    processInstanceAuthorization.setUserId(userId);
    authorizationService.saveAuthorization(processInstanceAuthorization);
  }

  protected static class InterruptTask extends TimerTask {
    protected boolean timeLimitExceeded = false;
    protected Thread thread;
    public InterruptTask(Thread thread) {
      this.thread = thread;
    }
    public boolean isTimeLimitExceeded() {
      return timeLimitExceeded;
    }
    @Override
    public void run() {
      timeLimitExceeded = true;
      thread.interrupt();
    }
  }

}
