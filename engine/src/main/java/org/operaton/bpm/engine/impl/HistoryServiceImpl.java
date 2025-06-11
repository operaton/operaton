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

import java.util.Arrays;
import java.util.List;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatchQuery;
import org.operaton.bpm.engine.history.CleanableHistoricBatchReport;
import org.operaton.bpm.engine.history.CleanableHistoricCaseInstanceReport;
import org.operaton.bpm.engine.history.CleanableHistoricDecisionInstanceReport;
import org.operaton.bpm.engine.history.CleanableHistoricProcessInstanceReport;
import org.operaton.bpm.engine.history.HistoricActivityInstanceQuery;
import org.operaton.bpm.engine.history.HistoricActivityStatisticsQuery;
import org.operaton.bpm.engine.history.HistoricCaseActivityInstanceQuery;
import org.operaton.bpm.engine.history.HistoricCaseActivityStatisticsQuery;
import org.operaton.bpm.engine.history.HistoricCaseInstanceQuery;
import org.operaton.bpm.engine.history.HistoricDecisionInstanceQuery;
import org.operaton.bpm.engine.history.HistoricDecisionInstanceStatisticsQuery;
import org.operaton.bpm.engine.history.HistoricDetailQuery;
import org.operaton.bpm.engine.history.HistoricExternalTaskLogQuery;
import org.operaton.bpm.engine.history.HistoricIncidentQuery;
import org.operaton.bpm.engine.history.HistoricJobLogQuery;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.history.HistoricProcessInstanceReport;
import org.operaton.bpm.engine.history.HistoricTaskInstanceQuery;
import org.operaton.bpm.engine.history.HistoricTaskInstanceReport;
import org.operaton.bpm.engine.history.HistoricVariableInstanceQuery;
import org.operaton.bpm.engine.history.NativeHistoricActivityInstanceQuery;
import org.operaton.bpm.engine.history.NativeHistoricCaseActivityInstanceQuery;
import org.operaton.bpm.engine.history.NativeHistoricCaseInstanceQuery;
import org.operaton.bpm.engine.history.NativeHistoricDecisionInstanceQuery;
import org.operaton.bpm.engine.history.NativeHistoricProcessInstanceQuery;
import org.operaton.bpm.engine.history.NativeHistoricTaskInstanceQuery;
import org.operaton.bpm.engine.history.NativeHistoricVariableInstanceQuery;
import org.operaton.bpm.engine.history.SetRemovalTimeSelectModeForHistoricBatchesBuilder;
import org.operaton.bpm.engine.history.SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder;
import org.operaton.bpm.engine.history.SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder;
import org.operaton.bpm.engine.history.UserOperationLogQuery;
import org.operaton.bpm.engine.impl.batch.history.DeleteHistoricBatchCmd;
import org.operaton.bpm.engine.impl.batch.history.HistoricBatchQueryImpl;
import org.operaton.bpm.engine.impl.cmd.DeleteHistoricCaseInstanceCmd;
import org.operaton.bpm.engine.impl.cmd.DeleteHistoricCaseInstancesBulkCmd;
import org.operaton.bpm.engine.impl.cmd.DeleteHistoricProcessInstancesCmd;
import org.operaton.bpm.engine.impl.cmd.DeleteHistoricTaskInstanceCmd;
import org.operaton.bpm.engine.impl.cmd.DeleteHistoricVariableInstanceCmd;
import org.operaton.bpm.engine.impl.cmd.DeleteHistoricVariableInstancesByProcessInstanceIdCmd;
import org.operaton.bpm.engine.impl.cmd.DeleteUserOperationLogEntryCmd;
import org.operaton.bpm.engine.impl.cmd.FindHistoryCleanupJobsCmd;
import org.operaton.bpm.engine.impl.cmd.GetHistoricExternalTaskLogErrorDetailsCmd;
import org.operaton.bpm.engine.impl.cmd.GetHistoricJobLogExceptionStacktraceCmd;
import org.operaton.bpm.engine.impl.cmd.HistoryCleanupCmd;
import org.operaton.bpm.engine.impl.cmd.batch.DeleteHistoricProcessInstancesBatchCmd;
import org.operaton.bpm.engine.impl.dmn.cmd.DeleteHistoricDecisionInstanceByDefinitionIdCmd;
import org.operaton.bpm.engine.impl.dmn.cmd.DeleteHistoricDecisionInstanceByInstanceIdCmd;
import org.operaton.bpm.engine.impl.dmn.cmd.DeleteHistoricDecisionInstancesBatchCmd;
import org.operaton.bpm.engine.impl.dmn.cmd.DeleteHistoricDecisionInstancesBulkCmd;
import org.operaton.bpm.engine.impl.history.SetRemovalTimeToHistoricBatchesBuilderImpl;
import org.operaton.bpm.engine.impl.history.SetRemovalTimeToHistoricDecisionInstancesBuilderImpl;
import org.operaton.bpm.engine.impl.history.SetRemovalTimeToHistoricProcessInstancesBuilderImpl;
import org.operaton.bpm.engine.runtime.Job;

/**
 * @author Tom Baeyens
 * @author Bernd Ruecker (Camunda)
 * @author Christian Stettler
 */
public class HistoryServiceImpl extends ServiceImpl implements HistoryService {

  @Override
  public HistoricProcessInstanceQuery createHistoricProcessInstanceQuery() {
    return new HistoricProcessInstanceQueryImpl(commandExecutor);
  }

  @Override
  public HistoricActivityInstanceQuery createHistoricActivityInstanceQuery() {
    return new HistoricActivityInstanceQueryImpl(commandExecutor);
  }

  @Override
  public HistoricActivityStatisticsQuery createHistoricActivityStatisticsQuery(String processDefinitionId) {
    return new HistoricActivityStatisticsQueryImpl(processDefinitionId, commandExecutor);
  }

  @Override
  public HistoricCaseActivityStatisticsQuery createHistoricCaseActivityStatisticsQuery(String caseDefinitionId) {
    return new HistoricCaseActivityStatisticsQueryImpl(caseDefinitionId, commandExecutor);
  }

  @Override
  public HistoricTaskInstanceQuery createHistoricTaskInstanceQuery() {
    return new HistoricTaskInstanceQueryImpl(commandExecutor);
  }

  @Override
  public HistoricDetailQuery createHistoricDetailQuery() {
    return new HistoricDetailQueryImpl(commandExecutor);
  }

  @Override
  public UserOperationLogQuery createUserOperationLogQuery() {
    return new UserOperationLogQueryImpl(commandExecutor);
  }

  @Override
  public HistoricVariableInstanceQuery createHistoricVariableInstanceQuery() {
    return new HistoricVariableInstanceQueryImpl(commandExecutor);
  }

  @Override
  public HistoricIncidentQuery createHistoricIncidentQuery() {
    return new HistoricIncidentQueryImpl(commandExecutor);
  }

  @Override
  public HistoricIdentityLinkLogQueryImpl createHistoricIdentityLinkLogQuery() {
    return new HistoricIdentityLinkLogQueryImpl(commandExecutor);
  }

  @Override
  public HistoricCaseInstanceQuery createHistoricCaseInstanceQuery() {
    return new HistoricCaseInstanceQueryImpl(commandExecutor);
  }

  @Override
  public HistoricCaseActivityInstanceQuery createHistoricCaseActivityInstanceQuery() {
    return new HistoricCaseActivityInstanceQueryImpl(commandExecutor);
  }

  @Override
  public HistoricDecisionInstanceQuery createHistoricDecisionInstanceQuery() {
    return new HistoricDecisionInstanceQueryImpl(commandExecutor);
  }

  @Override
  public void deleteHistoricTaskInstance(String taskId) {
    commandExecutor.execute(new DeleteHistoricTaskInstanceCmd(taskId));
  }

  @Override
  public void deleteHistoricProcessInstance(String processInstanceId) {
    deleteHistoricProcessInstances(Arrays.asList(processInstanceId));
  }

  @Override
  public void deleteHistoricProcessInstanceIfExists(String processInstanceId) {
    deleteHistoricProcessInstancesIfExists(Arrays.asList(processInstanceId));
  }

  @Override
  public void deleteHistoricProcessInstances(List<String> processInstanceIds) {
    commandExecutor.execute(new DeleteHistoricProcessInstancesCmd(processInstanceIds, true));
  }

  @Override
  public void deleteHistoricProcessInstancesIfExists(List<String> processInstanceIds) {
    commandExecutor.execute(new DeleteHistoricProcessInstancesCmd(processInstanceIds, false));
  }

  @Override
  public void deleteHistoricProcessInstancesBulk(List<String> processInstanceIds) {
    deleteHistoricProcessInstances(processInstanceIds);
  }

  @Override
  public Job cleanUpHistoryAsync() {
    return cleanUpHistoryAsync(false);
  }

  @Override
  public Job cleanUpHistoryAsync(boolean immediatelyDue) {
    return commandExecutor.execute(new HistoryCleanupCmd(immediatelyDue));
  }

  @Override
  public Job findHistoryCleanupJob() {
    final List<Job> jobs = commandExecutor.execute(new FindHistoryCleanupJobsCmd());
    if (!jobs.isEmpty()) {
      return jobs.get(0);
    } else {
      return null;
    }
  }

  @Override
  public List<Job> findHistoryCleanupJobs() {
    return commandExecutor.execute(new FindHistoryCleanupJobsCmd());
  }

  @Override
  public Batch deleteHistoricProcessInstancesAsync(List<String> processInstanceIds, String deleteReason) {
    return this.deleteHistoricProcessInstancesAsync(processInstanceIds,null,deleteReason);
  }

  @Override
  public Batch deleteHistoricProcessInstancesAsync(HistoricProcessInstanceQuery query, String deleteReason) {
    return this.deleteHistoricProcessInstancesAsync(null,query,deleteReason);
  }

  @Override
  public Batch deleteHistoricProcessInstancesAsync(List<String> processInstanceIds, HistoricProcessInstanceQuery query, String deleteReason) {
    return commandExecutor.execute(new DeleteHistoricProcessInstancesBatchCmd(processInstanceIds, query, deleteReason));
  }

  @Override
  public void deleteUserOperationLogEntry(String entryId) {
    commandExecutor.execute(new DeleteUserOperationLogEntryCmd(entryId));
  }

  @Override
  public void deleteHistoricCaseInstance(String caseInstanceId) {
    commandExecutor.execute(new DeleteHistoricCaseInstanceCmd(caseInstanceId));
  }

  @Override
  public void deleteHistoricCaseInstancesBulk(List<String> caseInstanceIds) {
    commandExecutor.execute(new DeleteHistoricCaseInstancesBulkCmd(caseInstanceIds));
  }

  @Override
  public void deleteHistoricDecisionInstance(String decisionDefinitionId) {
    deleteHistoricDecisionInstanceByDefinitionId(decisionDefinitionId);
  }

  @Override
  public void deleteHistoricDecisionInstancesBulk(List<String> decisionInstanceIds) {
    commandExecutor.execute(new DeleteHistoricDecisionInstancesBulkCmd(decisionInstanceIds));
  }

  @Override
  public void deleteHistoricDecisionInstanceByDefinitionId(String decisionDefinitionId) {
    commandExecutor.execute(new DeleteHistoricDecisionInstanceByDefinitionIdCmd(decisionDefinitionId));
  }

  @Override
  public void deleteHistoricDecisionInstanceByInstanceId(String historicDecisionInstanceId) {
    commandExecutor.execute(new DeleteHistoricDecisionInstanceByInstanceIdCmd(historicDecisionInstanceId));
  }

  @Override
  public Batch deleteHistoricDecisionInstancesAsync(List<String> decisionInstanceIds, String deleteReason) {
    return deleteHistoricDecisionInstancesAsync(decisionInstanceIds, null, deleteReason);
  }

  @Override
  public Batch deleteHistoricDecisionInstancesAsync(HistoricDecisionInstanceQuery query, String deleteReason) {
    return deleteHistoricDecisionInstancesAsync(null, query, deleteReason);
  }

  @Override
  public Batch deleteHistoricDecisionInstancesAsync(List<String> decisionInstanceIds, HistoricDecisionInstanceQuery query, String deleteReason) {
    return commandExecutor.execute(new DeleteHistoricDecisionInstancesBatchCmd(decisionInstanceIds, query, deleteReason));
  }

  @Override
  public void deleteHistoricVariableInstance(String variableInstanceId) {
    commandExecutor.execute(new DeleteHistoricVariableInstanceCmd(variableInstanceId));
  }

  @Override
  public void deleteHistoricVariableInstancesByProcessInstanceId(String processInstanceId) {
    commandExecutor.execute(new DeleteHistoricVariableInstancesByProcessInstanceIdCmd(processInstanceId));
  }

  @Override
  public NativeHistoricProcessInstanceQuery createNativeHistoricProcessInstanceQuery() {
    return new NativeHistoricProcessInstanceQueryImpl(commandExecutor);
  }

  @Override
  public NativeHistoricTaskInstanceQuery createNativeHistoricTaskInstanceQuery() {
    return new NativeHistoricTaskInstanceQueryImpl(commandExecutor);
  }

  @Override
  public NativeHistoricActivityInstanceQuery createNativeHistoricActivityInstanceQuery() {
    return new NativeHistoricActivityInstanceQueryImpl(commandExecutor);
  }

  @Override
  public NativeHistoricCaseInstanceQuery createNativeHistoricCaseInstanceQuery() {
    return new NativeHistoricCaseInstanceQueryImpl(commandExecutor);
  }

  @Override
  public NativeHistoricCaseActivityInstanceQuery createNativeHistoricCaseActivityInstanceQuery() {
    return new NativeHistoricCaseActivityInstanceQueryImpl(commandExecutor);
  }

  @Override
  public NativeHistoricDecisionInstanceQuery createNativeHistoricDecisionInstanceQuery() {
    return new NativeHistoryDecisionInstanceQueryImpl(commandExecutor);
  }

  @Override
  public NativeHistoricVariableInstanceQuery createNativeHistoricVariableInstanceQuery() {
    return new NativeHistoricVariableInstanceQueryImpl(commandExecutor);
  }

  @Override
  public HistoricJobLogQuery createHistoricJobLogQuery() {
    return new HistoricJobLogQueryImpl(commandExecutor);
  }

  @Override
  public String getHistoricJobLogExceptionStacktrace(String historicJobLogId) {
    return commandExecutor.execute(new GetHistoricJobLogExceptionStacktraceCmd(historicJobLogId));
  }

  @Override
  public HistoricProcessInstanceReport createHistoricProcessInstanceReport() {
    return new HistoricProcessInstanceReportImpl(commandExecutor);
  }

  @Override
  public HistoricTaskInstanceReport createHistoricTaskInstanceReport() {
    return new HistoricTaskInstanceReportImpl(commandExecutor);
  }

  @Override
  public CleanableHistoricProcessInstanceReport createCleanableHistoricProcessInstanceReport() {
    return new CleanableHistoricProcessInstanceReportImpl(commandExecutor);
  }

  @Override
  public CleanableHistoricDecisionInstanceReport createCleanableHistoricDecisionInstanceReport() {
    return new CleanableHistoricDecisionInstanceReportImpl(commandExecutor);
  }

  @Override
  public CleanableHistoricCaseInstanceReport createCleanableHistoricCaseInstanceReport() {
    return new CleanableHistoricCaseInstanceReportImpl(commandExecutor);
  }

  @Override
  public CleanableHistoricBatchReport createCleanableHistoricBatchReport() {
    return new CleanableHistoricBatchReportImpl(commandExecutor);
  }

  @Override
  public HistoricBatchQuery createHistoricBatchQuery() {
    return new HistoricBatchQueryImpl(commandExecutor);
  }

  @Override
  public void deleteHistoricBatch(String batchId) {
    commandExecutor.execute(new DeleteHistoricBatchCmd(batchId));
  }

  @Override
  public HistoricDecisionInstanceStatisticsQuery createHistoricDecisionInstanceStatisticsQuery(String decisionRequirementsDefinitionId) {
    return new HistoricDecisionInstanceStatisticsQueryImpl(decisionRequirementsDefinitionId, commandExecutor);
  }

  @Override
  public HistoricExternalTaskLogQuery createHistoricExternalTaskLogQuery() {
    return new HistoricExternalTaskLogQueryImpl(commandExecutor);
  }

  @Override
  public String getHistoricExternalTaskLogErrorDetails(String historicExternalTaskLogId) {
    return commandExecutor.execute(new GetHistoricExternalTaskLogErrorDetailsCmd(historicExternalTaskLogId));
  }

  @Override
  public SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder setRemovalTimeToHistoricProcessInstances() {
    return new SetRemovalTimeToHistoricProcessInstancesBuilderImpl(commandExecutor);
  }

  @Override
  public SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder setRemovalTimeToHistoricDecisionInstances() {
    return new SetRemovalTimeToHistoricDecisionInstancesBuilderImpl(commandExecutor);
  }

  @Override
  public SetRemovalTimeSelectModeForHistoricBatchesBuilder setRemovalTimeToHistoricBatches() {
    return new SetRemovalTimeToHistoricBatchesBuilderImpl(commandExecutor);
  }

  @Override
  public void setAnnotationForOperationLogById(String operationId, String annotation) {
    commandExecutor.execute(new SetAnnotationForOperationLog(operationId, annotation));
  }

  @Override
  public void clearAnnotationForOperationLogById(String operationId) {
    commandExecutor.execute(new SetAnnotationForOperationLog(operationId, null));
  }

}
