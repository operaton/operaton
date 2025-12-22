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
package org.operaton.bpm.engine.impl.cfg.multitenancy;

import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.history.HistoricCaseInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.batch.BatchEntity;
import org.operaton.bpm.engine.impl.batch.history.HistoricBatchEntity;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.cmd.CommandLogger;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionDefinitionEntity;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionRequirementsDefinitionEntity;
import org.operaton.bpm.engine.impl.history.event.HistoricExternalTaskLogEntity;
import org.operaton.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricJobLogEventEntity;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TenantManager;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.CaseExecution;

/**
 * {@link CommandChecker} to ensure that commands are only executed for
 * entities which belongs to one of the authenticated tenants.
 */
public class TenantCommandChecker implements CommandChecker {

  protected static final CommandLogger LOG = ProcessEngineLogger.CMD_LOGGER;

  @Override
  public void checkEvaluateDecision(DecisionDefinition decisionDefinition) {
    if (!getTenantManager().isAuthenticatedTenant(decisionDefinition.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("evaluate the decision '%s'".formatted(decisionDefinition.getId()));
    }
  }

  @Override
  public void checkCreateProcessInstance(ProcessDefinition processDefinition) {
    if (!getTenantManager().isAuthenticatedTenant(processDefinition.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("create an instance of the process definition '%s'".formatted(processDefinition.getId()));
    }
  }

  @Override
  public void checkReadProcessDefinition(ProcessDefinition processDefinition) {
    if (!getTenantManager().isAuthenticatedTenant(processDefinition.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("get the process definition '%s'".formatted(processDefinition.getId()));
    }
  }

  @Override
  public void checkCreateCaseInstance(CaseDefinition caseDefinition) {
    if (!getTenantManager().isAuthenticatedTenant(caseDefinition.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("create an instance of the case definition '%s'".formatted(caseDefinition.getId()));
    }
  }

  @Override
  public void checkUpdateProcessDefinitionById(String processDefinitionId) {
    if (getTenantManager().isTenantCheckEnabled()) {
      ProcessDefinitionEntity processDefinition = findLatestProcessDefinitionById(processDefinitionId);
      if (processDefinition != null && !getTenantManager().isAuthenticatedTenant(processDefinition.getTenantId())) {
        throw LOG.exceptionCommandWithUnauthorizedTenant("update the process definition '%s'".formatted(processDefinitionId));
      }
    }
  }

  @Override
  public void checkUpdateProcessDefinitionSuspensionStateById(String processDefinitionId) {
    checkUpdateProcessDefinitionById(processDefinitionId);
  }

  @Override
  public void checkUpdateProcessDefinitionByKey(String processDefinitionKey) {
    // nothing to do
  }

  @Override
  public void checkUpdateProcessDefinitionSuspensionStateByKey(String processDefinitionKey) {
    // nothing to do
  }

  @Override
  public void checkDeleteProcessDefinitionById(String processDefinitionId) {
    if (getTenantManager().isTenantCheckEnabled()) {
      ProcessDefinitionEntity processDefinition = findLatestProcessDefinitionById(processDefinitionId);
      if (processDefinition != null && !getTenantManager().isAuthenticatedTenant(processDefinition.getTenantId())) {
        throw LOG.exceptionCommandWithUnauthorizedTenant("delete the process definition '%s'".formatted(processDefinitionId));
      }
    }
  }

  @Override
  public void checkDeleteProcessDefinitionByKey(String processDefinitionKey) {
    // nothing to do
  }

  @Override
  public void checkUpdateProcessInstanceByProcessDefinitionId(String processDefinitionId) {
    if (getTenantManager().isTenantCheckEnabled()) {
      ProcessDefinitionEntity processDefinition = findLatestProcessDefinitionById(processDefinitionId);
      if (processDefinition != null && !getTenantManager().isAuthenticatedTenant(processDefinition.getTenantId())) {
        throw LOG.exceptionCommandWithUnauthorizedTenant("update the process definition '%s'".formatted(processDefinitionId));
      }
    }
  }

  @Override
  public void checkUpdateRetriesProcessInstanceByProcessDefinitionId(String processDefinitionId) {
    checkUpdateProcessInstanceByProcessDefinitionId(processDefinitionId);
  }

  @Override
  public void checkUpdateProcessInstanceSuspensionStateByProcessDefinitionId(String processDefinitionId) {
    checkUpdateProcessInstanceByProcessDefinitionId(processDefinitionId);
  }

  @Override
  public void checkUpdateProcessInstance(ExecutionEntity execution) {
    if (execution != null && !getTenantManager().isAuthenticatedTenant(execution.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("update the process instance '%s'".formatted(execution.getId()));
    }
  }

  @Override
  public void checkUpdateProcessInstanceVariables(ExecutionEntity execution) {
    checkUpdateProcessInstance(execution);
  }

  @Override
  public void checkUpdateJob(JobEntity job) {
    if (job != null && !getTenantManager().isAuthenticatedTenant(job.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("update the job '%s'".formatted(job.getId()));
    }
  }

  @Override
  public void checkUpdateRetriesJob(JobEntity job) {
    checkUpdateJob(job);
  }

  @Override
  public void checkUpdateProcessInstanceByProcessDefinitionKey(String processDefinitionKey) {
    // nothing to do
  }

  @Override
  public void checkUpdateProcessInstanceSuspensionStateByProcessDefinitionKey(String processDefinitionKey) {
    // nothing to do
  }

  @Override
  public void checkUpdateProcessInstanceById(String processInstanceId) {
    if (getTenantManager().isTenantCheckEnabled()) {
      ExecutionEntity execution = findExecutionById(processInstanceId);
      if (execution != null && !getTenantManager().isAuthenticatedTenant(execution.getTenantId())) {
        throw LOG.exceptionCommandWithUnauthorizedTenant("update the process instance '%s'".formatted(processInstanceId));
      }
    }
  }

  @Override
  public void checkUpdateProcessInstanceSuspensionStateById(String processInstanceId) {
    checkUpdateProcessInstanceById(processInstanceId);
  }

  @Override
  public void checkCreateMigrationPlan(ProcessDefinition sourceProcessDefinition, ProcessDefinition targetProcessDefinition) {
    String sourceTenant = sourceProcessDefinition.getTenantId();
    String targetTenant = targetProcessDefinition.getTenantId();

    if (!getTenantManager().isAuthenticatedTenant(sourceTenant)) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("get process definition '%s'".formatted(sourceProcessDefinition.getId()));
    }
    if (!getTenantManager().isAuthenticatedTenant(targetTenant)) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("get process definition '%s'".formatted(targetProcessDefinition.getId()));
    }

    if (sourceTenant != null && targetTenant != null && !sourceTenant.equals(targetTenant)) {
      throw ProcessEngineLogger.MIGRATION_LOGGER
        .cannotMigrateBetweenTenants(sourceTenant, targetTenant);
    }
  }

  @Override
  public void checkReadProcessInstance(String processInstanceId) {
    if (getTenantManager().isTenantCheckEnabled()) {
      ExecutionEntity execution = findExecutionById(processInstanceId);
      if (execution != null && !getTenantManager().isAuthenticatedTenant(execution.getTenantId())) {
        throw LOG.exceptionCommandWithUnauthorizedTenant("read the process instance '%s'".formatted(processInstanceId));
      }
    }
  }

  @Override
  public void checkReadJob(JobEntity job) {
    if (job != null && !getTenantManager().isAuthenticatedTenant(job.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("read the job '%s'".formatted(job.getId()));
    }
  }

  @Override
  public void checkReadProcessInstance(ExecutionEntity execution) {
    if (execution != null && !getTenantManager().isAuthenticatedTenant(execution.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("read the process instance '%s'".formatted(execution.getId()));
    }
  }

  @Override
  public void checkReadProcessInstanceVariable(ExecutionEntity execution) {
    checkReadProcessInstance(execution);
  }

  @Override
  public void checkDeleteProcessInstance(ExecutionEntity execution) {
    if (execution != null && !getTenantManager().isAuthenticatedTenant(execution.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("delete the process instance '%s'".formatted(execution.getId()));
    }
  }

  @Override
  public void checkMigrateProcessInstance(ExecutionEntity processInstance, ProcessDefinition targetProcessDefinition) {
    String sourceTenant = processInstance.getTenantId();
    String targetTenant = targetProcessDefinition.getTenantId();

    if (getTenantManager().isTenantCheckEnabled() && !getTenantManager().isAuthenticatedTenant(processInstance.getTenantId())) {
       throw LOG.exceptionCommandWithUnauthorizedTenant("migrate process instance '%s'".formatted(processInstance.getId()));
    }

    if (targetTenant != null && (sourceTenant == null || !sourceTenant.equals(targetTenant))) {
      throw ProcessEngineLogger.MIGRATION_LOGGER
        .cannotMigrateInstanceBetweenTenants(processInstance.getId(), sourceTenant, targetTenant);
    }
  }

  @Override
  public void checkReadTask(TaskEntity task) {
    if (task != null && !getTenantManager().isAuthenticatedTenant(task.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("read the task '%s'".formatted(task.getId()));
    }
  }

  @Override
  public void checkReadTaskVariable(TaskEntity task) {
    checkReadTask(task);
  }

  @Override
  public void checkUpdateTaskVariable(TaskEntity task) {
    if (task != null && !getTenantManager().isAuthenticatedTenant(task.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("update the task '%s'".formatted(task.getId()));
    }
  }

  @Override
  public void checkCreateBatch(Permission permission) {
    // nothing to do
  }

  @Override
  public void checkDeleteBatch(BatchEntity batch) {
    if (batch != null && !getTenantManager().isAuthenticatedTenant(batch.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("delete batch '%s'".formatted(batch.getId()));
    }
  }

  @Override
  public void checkDeleteHistoricBatch(HistoricBatchEntity batch) {
    if (batch != null && !getTenantManager().isAuthenticatedTenant(batch.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("delete historic batch '%s'".formatted(batch.getId()));
    }
  }

  @Override
  public void checkSuspendBatch(BatchEntity batch) {
    if (batch != null && !getTenantManager().isAuthenticatedTenant(batch.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("suspend batch '%s'".formatted(batch.getId()));
    }
  }

  @Override
  public void checkActivateBatch(BatchEntity batch) {
    if (batch != null && !getTenantManager().isAuthenticatedTenant(batch.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("activate batch '%s'".formatted(batch.getId()));
    }
  }

  @Override
  public void checkReadHistoricBatch() {
    // nothing to do
  }

  @Override
  public void checkCreateDeployment() {
    // nothing to do
  }

  @Override
  public void checkReadDeployment(String deploymentId) {
    if (getTenantManager().isTenantCheckEnabled()) {
      DeploymentEntity deployment = findDeploymentById(deploymentId);
      if (deployment != null && !getTenantManager().isAuthenticatedTenant(deployment.getTenantId())) {
        throw LOG.exceptionCommandWithUnauthorizedTenant("get the deployment '%s'".formatted(deploymentId));
      }
    }
  }

  @Override
  public void checkDeleteDeployment(String deploymentId) {
    if (getTenantManager().isTenantCheckEnabled()) {
      DeploymentEntity deployment = findDeploymentById(deploymentId);
      if (deployment != null && !getTenantManager().isAuthenticatedTenant(deployment.getTenantId())) {
        throw LOG.exceptionCommandWithUnauthorizedTenant("delete the deployment '%s'".formatted(deploymentId));
      }
    }
  }

  @Override
  public void checkDeleteTask(TaskEntity task) {
    if (task != null && !getTenantManager().isAuthenticatedTenant(task.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("delete the task '%s'".formatted(task.getId()));
    }
  }

  @Override
  public void checkTaskAssign(TaskEntity task) {
    if (task != null && !getTenantManager().isAuthenticatedTenant(task.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("assign the task '%s'".formatted(task.getId()));
    }
  }

  @Override
  public void checkCreateTask(TaskEntity task) {
    if (task != null && !getTenantManager().isAuthenticatedTenant(task.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("create the task '%s'".formatted(task.getId()));
    }
  }

  @Override
  public void checkCreateTask() {
    // nothing to do
  }

  @Override
  public void checkTaskWork(TaskEntity task) {
    if (task != null && !getTenantManager().isAuthenticatedTenant(task.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("work on task '%s'".formatted(task.getId()));
    }
  }

  @Override
  public void checkReadDecisionDefinition(DecisionDefinitionEntity decisionDefinition) {
    if (decisionDefinition != null && !getTenantManager().isAuthenticatedTenant(decisionDefinition.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("get the decision definition '%s'".formatted(decisionDefinition.getId()));
    }
  }

  @Override
  public void checkUpdateDecisionDefinitionById(String decisionDefinitionId) {
    if (getTenantManager().isTenantCheckEnabled()) {
      DecisionDefinitionEntity decisionDefinition = findLatestDecisionDefinitionById(decisionDefinitionId);
      if (decisionDefinition != null && !getTenantManager().isAuthenticatedTenant(decisionDefinition.getTenantId())) {
        throw LOG.exceptionCommandWithUnauthorizedTenant("update the decision definition '%s'".formatted(decisionDefinitionId));
      }
    }
  }

  @Override
  public void checkReadDecisionRequirementsDefinition(DecisionRequirementsDefinitionEntity decisionRequirementsDefinition) {
    if (decisionRequirementsDefinition != null && !getTenantManager().isAuthenticatedTenant(decisionRequirementsDefinition.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("get the decision requirements definition '%s'".formatted(decisionRequirementsDefinition.getId()));
    }
  }

  @Override
  public void checkReadCaseDefinition(CaseDefinition caseDefinition) {
    if (caseDefinition != null && !getTenantManager().isAuthenticatedTenant(caseDefinition.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("get the case definition '%s'".formatted(caseDefinition.getId()));
    }
  }

  @Override
  public void checkUpdateCaseDefinition(CaseDefinition caseDefinition) {
    if (caseDefinition != null && !getTenantManager().isAuthenticatedTenant(caseDefinition.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("update the case definition '%s'".formatted(caseDefinition.getId()));
    }
  }

  @Override
  public void checkDeleteHistoricTaskInstance(HistoricTaskInstanceEntity task) {
    if (task != null && !getTenantManager().isAuthenticatedTenant(task.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("delete the historic task instance '%s'".formatted(task.getId()));
    }
  }

  @Override
  public void checkDeleteHistoricProcessInstance(HistoricProcessInstance instance) {
    if (instance != null && !getTenantManager().isAuthenticatedTenant(instance.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("delete the historic process instance '%s'".formatted(instance.getId()));
    }
  }

  @Override
  public void checkDeleteHistoricCaseInstance(HistoricCaseInstance instance) {
    if (instance != null && !getTenantManager().isAuthenticatedTenant(instance.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("delete the historic case instance '%s'".formatted(instance.getId()));
    }
  }

  @Override
  public void checkDeleteHistoricDecisionInstance(String decisionDefinitionKey) {
    // No tenant check here because it is called in the SQL query:
    // HistoricDecisionInstance.selectHistoricDecisionInstancesByDecisionDefinitionId
    // It is necessary to make the check there because of performance issues. If the check
    // is done here then the we get all history decision instances (also from possibly
    // other tenants) and then filter them. If there are a lot instances this can cause
    // latency. Therefore does the SQL query only return the
    // historic decision instances which belong to the authenticated tenant.
  }

  @Override
  public void checkDeleteHistoricDecisionInstance(HistoricDecisionInstance decisionInstance) {
    if (decisionInstance != null && !getTenantManager().isAuthenticatedTenant(decisionInstance.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant(
          "delete the historic decision instance '%s'".formatted(decisionInstance.getId())
      );
    }
  }

  @Override
  public void checkDeleteHistoricVariableInstance(HistoricVariableInstanceEntity variable) {
    if (variable != null && !getTenantManager().isAuthenticatedTenant(variable.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("delete the historic variable instance '%s'".formatted(variable.getId()));
    }
  }

  @Override
  public void checkDeleteHistoricVariableInstancesByProcessInstance(HistoricProcessInstanceEntity instance) {
    if (instance != null && !getTenantManager().isAuthenticatedTenant(instance.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("delete the historic variable instances of process instance '%s'".formatted(instance.getId()));
    }
  }

  @Override
  public void checkReadHistoricJobLog(HistoricJobLogEventEntity historicJobLog) {
    if (historicJobLog != null && !getTenantManager().isAuthenticatedTenant(historicJobLog.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("get the historic job log '%s'".formatted(historicJobLog.getId()));
    }
  }

  @Override
  public void checkReadHistoryAnyProcessDefinition() {
    // No tenant check here because it is called in the SQL query:
    // Report.selectHistoricProcessInstanceDurationReport
    // It is necessary to make the check there because the query may be return only the
    // historic process instances which belong to the authenticated tenant.
  }

  @Override
  public void checkReadHistoryProcessDefinition(String processDefinitionId) {
    // No tenant check here because it is called in the SQL query:
    // Report.selectHistoricProcessInstanceDurationReport
    // It is necessary to make the check there because the query may be return only the
    // historic process instances which belong to the authenticated tenant.
  }

  @Override
  public void checkUpdateCaseInstance(CaseExecution caseExecution) {
    if (caseExecution != null && !getTenantManager().isAuthenticatedTenant(caseExecution.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("update the case execution '%s'".formatted(caseExecution.getId()));
    }
  }

  @Override
  public void checkReadCaseInstance(CaseExecution caseExecution) {
    if (caseExecution != null && !getTenantManager().isAuthenticatedTenant(caseExecution.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("get the case execution '%s'".formatted(caseExecution.getId()));
    }
  }

  @Override
  public void checkDeleteUserOperationLog(UserOperationLogEntry entry) {
    if (entry != null && !getTenantManager().isAuthenticatedTenant(entry.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("delete the user operation log entry '%s'".formatted(entry.getId()));
    }
  }

  @Override
  public void checkUpdateUserOperationLog(UserOperationLogEntry entry) {
    if (entry != null && !getTenantManager().isAuthenticatedTenant(entry.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("update the user operation log entry '%s'".formatted(entry.getId()));
    }
  }

  @Override
  public void checkReadHistoricExternalTaskLog(HistoricExternalTaskLogEntity historicExternalTaskLog) {
    if (historicExternalTaskLog != null && !getTenantManager().isAuthenticatedTenant(historicExternalTaskLog.getTenantId())) {
      throw LOG.exceptionCommandWithUnauthorizedTenant("get the historic external task log '%s'".formatted(historicExternalTaskLog.getId()));
    }
  }

  @Override
  public void checkReadDiagnosticsData() {
    // nothing to do
  }

  @Override
  public void checkReadHistoryLevel() {
    // nothing to do
  }

  @Override
  public void checkReadTableCount() {
    // nothing to do
  }

  @Override
  public void checkReadTableName() {
    // nothing to do
  }

  @Override
  public void checkReadTableMetaData() {
    // nothing to do
  }

  @Override
  public void checkReadProperties() {
    // nothing to do
  }

  @Override
  public void checkSetProperty() {
    // nothing to do
  }

  @Override
  public void checkDeleteProperty() {
    // nothing to do
  }

  @Override
  public void checkRegisterProcessApplication() {
    // nothing to do
  }

  @Override
  public void checkUnregisterProcessApplication() {
    // nothing to do
  }

  @Override
  public void checkReadRegisteredDeployments() {
    // nothing to do
  }

  @Override
  public void checkReadProcessApplicationForDeployment() {
    // nothing to do
  }

  @Override
  public void checkRegisterDeployment() {
    // nothing to do
  }

  @Override
  public void checkUnregisterDeployment() {
    // nothing to do
  }

  @Override
  public void checkDeleteMetrics() {
    // nothing to do
  }

  @Override
  public void checkDeleteTaskMetrics() {
    // nothing to do
  }

  @Override
  public void checkReadSchemaLog() {
    // nothing to do
  }

  // helper //////////////////////////////////////////////////

  protected TenantManager getTenantManager() {
    return Context.getCommandContext().getTenantManager();
  }

  protected ProcessDefinitionEntity findLatestProcessDefinitionById(String processDefinitionId) {
    return Context.getCommandContext().getProcessDefinitionManager().findLatestProcessDefinitionById(processDefinitionId);
  }

  protected DecisionDefinitionEntity findLatestDecisionDefinitionById(String decisionDefinitionId) {
    return Context.getCommandContext().getDecisionDefinitionManager().findDecisionDefinitionById(decisionDefinitionId);
  }

  protected ExecutionEntity findExecutionById(String processInstanceId) {
    return Context.getCommandContext().getExecutionManager().findExecutionById(processInstanceId);
  }

  protected DeploymentEntity findDeploymentById(String deploymentId) {
    return Context.getCommandContext().getDeploymentManager().findDeploymentById(deploymentId);
  }

}
