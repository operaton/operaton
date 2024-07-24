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
import org.operaton.bpm.engine.history.SetRemovalTimeToHistoricBatchesBuilder;
import org.operaton.bpm.engine.history.SetRemovalTimeToHistoricDecisionInstancesBuilder;
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
import org.operaton.bpm.engine.history.SetRemovalTimeToHistoricProcessInstancesBuilder;
import org.operaton.bpm.engine.impl.history.SetRemovalTimeToHistoricBatchesBuilderImpl;
import org.operaton.bpm.engine.impl.history.SetRemovalTimeToHistoricDecisionInstancesBuilderImpl;
import org.operaton.bpm.engine.impl.history.SetRemovalTimeToHistoricProcessInstancesBuilderImpl;
import org.operaton.bpm.engine.runtime.Job;

/**
 * @author Tom Baeyens
 * @author Bernd Ruecker (operaton)
 * @author Christian Stettler
 */
public class HistoryServiceImpl extends ServiceImpl implements HistoryService {

  public HistoricProcessInstanceQuery createHistoricProcessInstanceQuery() {
    return new HistoricProcessInstanceQueryImpl(commandExecutor);
  }

  public HistoricActivityInstanceQuery createHistoricActivityInstanceQuery() {
    return new HistoricActivityInstanceQueryImpl(commandExecutor);
  }

  public HistoricActivityStatisticsQuery createHistoricActivityStatisticsQuery(String processDefinitionId) {
    return new HistoricActivityStatisticsQueryImpl(processDefinitionId, commandExecutor);
  }

  public HistoricCaseActivityStatisticsQuery createHistoricCaseActivityStatisticsQuery(String caseDefinitionId) {
    return new HistoricCaseActivityStatisticsQueryImpl(caseDefinitionId, commandExecutor);
  }

  public HistoricTaskInstanceQuery createHistoricTaskInstanceQuery() {
    return new HistoricTaskInstanceQueryImpl(commandExecutor);
  }

  public HistoricDetailQuery createHistoricDetailQuery() {
    return new HistoricDetailQueryImpl(commandExecutor);
  }

  public UserOperationLogQuery createUserOperationLogQuery() {
    return new UserOperationLogQueryImpl(commandExecutor);
  }

  public HistoricVariableInstanceQuery createHistoricVariableInstanceQuery() {
    return new HistoricVariableInstanceQueryImpl(commandExecutor);
  }

  public HistoricIncidentQuery createHistoricIncidentQuery() {
    return new HistoricIncidentQueryImpl(commandExecutor);
  }

  public HistoricIdentityLinkLogQueryImpl createHistoricIdentityLinkLogQuery() {
    return new HistoricIdentityLinkLogQueryImpl(commandExecutor);
  }

  public HistoricCaseInstanceQuery createHistoricCaseInstanceQuery() {
    return new HistoricCaseInstanceQueryImpl(commandExecutor);
  }

  public HistoricCaseActivityInstanceQuery createHistoricCaseActivityInstanceQuery() {
    return new HistoricCaseActivityInstanceQueryImpl(commandExecutor);
  }

  public HistoricDecisionInstanceQuery createHistoricDecisionInstanceQuery() {
    return new HistoricDecisionInstanceQueryImpl(commandExecutor);
  }

  public void deleteHistoricTaskInstance(String taskId) {
    commandExecutor.execute(new DeleteHistoricTaskInstanceCmd(taskId));
  }

  public void deleteHistoricProcessInstance(String processInstanceId) {
    deleteHistoricProcessInstances(Arrays.asList(processInstanceId));
  }

  public void deleteHistoricProcessInstanceIfExists(String processInstanceId) {
    deleteHistoricProcessInstancesIfExists(Arrays.asList(processInstanceId));
  }

  public void deleteHistoricProcessInstances(List<String> processInstanceIds) {
    commandExecutor.execute(new DeleteHistoricProcessInstancesCmd(processInstanceIds, true));
  }

  public void deleteHistoricProcessInstancesIfExists(List<String> processInstanceIds) {
    commandExecutor.execute(new DeleteHistoricProcessInstancesCmd(processInstanceIds, false));
  }

  public void deleteHistoricProcessInstancesBulk(List<String> processInstanceIds){
    deleteHistoricProcessInstances(processInstanceIds);
  }

  public Job cleanUpHistoryAsync() {
    return cleanUpHistoryAsync(false);
  }

  public Job cleanUpHistoryAsync(boolean immediatelyDue) {
    return commandExecutor.execute(new HistoryCleanupCmd(immediatelyDue));
  }

  @Override
  public Job findHistoryCleanupJob() {
    final List<Job> jobs = commandExecutor.execute(new FindHistoryCleanupJobsCmd());
    if (jobs.size() > 0) {
      return jobs.get(0);
    } else {
      return null;
    }
  }

  @Override
  public List<Job> findHistoryCleanupJobs() {
    return commandExecutor.execute(new FindHistoryCleanupJobsCmd());
  }

  public Batch deleteHistoricProcessInstancesAsync(List<String> processInstanceIds, String deleteReason) {
    return this.deleteHistoricProcessInstancesAsync(processInstanceIds,null,deleteReason);
  }

  public Batch deleteHistoricProcessInstancesAsync(HistoricProcessInstanceQuery query, String deleteReason) {
    return this.deleteHistoricProcessInstancesAsync(null,query,deleteReason);
  }

  public Batch deleteHistoricProcessInstancesAsync(List<String> processInstanceIds, HistoricProcessInstanceQuery query, String deleteReason){
    return commandExecutor.execute(new DeleteHistoricProcessInstancesBatchCmd(processInstanceIds, query, deleteReason));
  }

  public void deleteUserOperationLogEntry(String entryId) {
    commandExecutor.execute(new DeleteUserOperationLogEntryCmd(entryId));
  }

  public void deleteHistoricCaseInstance(String caseInstanceId) {
    commandExecutor.execute(new DeleteHistoricCaseInstanceCmd(caseInstanceId));
  }

  public void deleteHistoricCaseInstancesBulk(List<String> caseInstanceIds) {
    commandExecutor.execute(new DeleteHistoricCaseInstancesBulkCmd(caseInstanceIds));
  }

  public void deleteHistoricDecisionInstance(String decisionDefinitionId) {
    deleteHistoricDecisionInstanceByDefinitionId(decisionDefinitionId);
  }

  @Override
  public void deleteHistoricDecisionInstancesBulk(List<String> decisionInstanceIds) {
    commandExecutor.execute(new DeleteHistoricDecisionInstancesBulkCmd(decisionInstanceIds));
  }

  public void deleteHistoricDecisionInstanceByDefinitionId(String decisionDefinitionId) {
    commandExecutor.execute(new DeleteHistoricDecisionInstanceByDefinitionIdCmd(decisionDefinitionId));
  }

  public void deleteHistoricDecisionInstanceByInstanceId(String historicDecisionInstanceId){
    commandExecutor.execute(new DeleteHistoricDecisionInstanceByInstanceIdCmd(historicDecisionInstanceId));
  }

  public Batch deleteHistoricDecisionInstancesAsync(List<String> decisionInstanceIds, String deleteReason) {
    return deleteHistoricDecisionInstancesAsync(decisionInstanceIds, null, deleteReason);
  }

  public Batch deleteHistoricDecisionInstancesAsync(HistoricDecisionInstanceQuery query, String deleteReason) {
    return deleteHistoricDecisionInstancesAsync(null, query, deleteReason);
  }

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

  public NativeHistoricProcessInstanceQuery createNativeHistoricProcessInstanceQuery() {
    return new NativeHistoricProcessInstanceQueryImpl(commandExecutor);
  }

  public NativeHistoricTaskInstanceQuery createNativeHistoricTaskInstanceQuery() {
    return new NativeHistoricTaskInstanceQueryImpl(commandExecutor);
  }

  public NativeHistoricActivityInstanceQuery createNativeHistoricActivityInstanceQuery() {
    return new NativeHistoricActivityInstanceQueryImpl(commandExecutor);
  }

  public NativeHistoricCaseInstanceQuery createNativeHistoricCaseInstanceQuery() {
    return new NativeHistoricCaseInstanceQueryImpl(commandExecutor);
  }

  public NativeHistoricCaseActivityInstanceQuery createNativeHistoricCaseActivityInstanceQuery() {
    return new NativeHistoricCaseActivityInstanceQueryImpl(commandExecutor);
  }

  public NativeHistoricDecisionInstanceQuery createNativeHistoricDecisionInstanceQuery() {
    return new NativeHistoryDecisionInstanceQueryImpl(commandExecutor);
  }

  public NativeHistoricVariableInstanceQuery createNativeHistoricVariableInstanceQuery() {
    return new NativeHistoricVariableInstanceQueryImpl(commandExecutor);
  }

  public HistoricJobLogQuery createHistoricJobLogQuery() {
    return new HistoricJobLogQueryImpl(commandExecutor);
  }

  public String getHistoricJobLogExceptionStacktrace(String historicJobLogId) {
    return commandExecutor.execute(new GetHistoricJobLogExceptionStacktraceCmd(historicJobLogId));
  }

  public HistoricProcessInstanceReport createHistoricProcessInstanceReport() {
    return new HistoricProcessInstanceReportImpl(commandExecutor);
  }

  public HistoricTaskInstanceReport createHistoricTaskInstanceReport() {
    return new HistoricTaskInstanceReportImpl(commandExecutor);
  }

  public CleanableHistoricProcessInstanceReport createCleanableHistoricProcessInstanceReport() {
    return new CleanableHistoricProcessInstanceReportImpl(commandExecutor);
  }

  public CleanableHistoricDecisionInstanceReport createCleanableHistoricDecisionInstanceReport() {
    return new CleanableHistoricDecisionInstanceReportImpl(commandExecutor);
  }

  public CleanableHistoricCaseInstanceReport createCleanableHistoricCaseInstanceReport() {
    return new CleanableHistoricCaseInstanceReportImpl(commandExecutor);
  }

  public CleanableHistoricBatchReport createCleanableHistoricBatchReport() {
    return new CleanableHistoricBatchReportImpl(commandExecutor);
  }

  public HistoricBatchQuery createHistoricBatchQuery() {
    return new HistoricBatchQueryImpl(commandExecutor);
  }

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

  public SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder setRemovalTimeToHistoricProcessInstances() {
    return new SetRemovalTimeToHistoricProcessInstancesBuilderImpl(commandExecutor);
  }

  public SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder setRemovalTimeToHistoricDecisionInstances() {
    return new SetRemovalTimeToHistoricDecisionInstancesBuilderImpl(commandExecutor);
  }

  public SetRemovalTimeSelectModeForHistoricBatchesBuilder setRemovalTimeToHistoricBatches() {
    return new SetRemovalTimeToHistoricBatchesBuilderImpl(commandExecutor);
  }

  public void setAnnotationForOperationLogById(String operationId, String annotation) {
    commandExecutor.execute(new SetAnnotationForOperationLog(operationId, annotation));
  }

  public void clearAnnotationForOperationLogById(String operationId) {
    commandExecutor.execute(new SetAnnotationForOperationLog(operationId, null));
  }

}
