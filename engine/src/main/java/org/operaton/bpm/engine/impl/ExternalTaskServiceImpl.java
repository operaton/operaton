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

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.externaltask.ExternalTaskQuery;
import org.operaton.bpm.engine.externaltask.ExternalTaskQueryBuilder;
import org.operaton.bpm.engine.externaltask.FetchAndLockBuilder;
import org.operaton.bpm.engine.externaltask.UpdateExternalTaskRetriesSelectBuilder;
import org.operaton.bpm.engine.impl.cmd.CompleteExternalTaskCmd;
import org.operaton.bpm.engine.impl.cmd.ExtendLockOnExternalTaskCmd;
import org.operaton.bpm.engine.impl.cmd.GetExternalTaskErrorDetailsCmd;
import org.operaton.bpm.engine.impl.cmd.GetTopicNamesCmd;
import org.operaton.bpm.engine.impl.cmd.HandleExternalTaskBpmnErrorCmd;
import org.operaton.bpm.engine.impl.cmd.HandleExternalTaskFailureCmd;
import org.operaton.bpm.engine.impl.cmd.LockExternalTaskCmd;
import org.operaton.bpm.engine.impl.cmd.SetExternalTaskPriorityCmd;
import org.operaton.bpm.engine.impl.cmd.SetExternalTaskRetriesCmd;
import org.operaton.bpm.engine.impl.cmd.UnlockExternalTaskCmd;
import org.operaton.bpm.engine.impl.cmd.UpdateExternalTaskRetriesBuilderImpl;
import org.operaton.bpm.engine.impl.externaltask.ExternalTaskQueryTopicBuilderImpl;
import org.operaton.bpm.engine.impl.externaltask.FetchAndLockBuilderImpl;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNullElse;

/**
 * @author Thorben Lindhauer
 * @author Christopher Zell
 * @author Askar Akhmerov
 */
public @NullMarked class ExternalTaskServiceImpl extends ServiceImpl implements ExternalTaskService {

  @Override
  public ExternalTaskQueryBuilder fetchAndLock(int maxTasks, String workerId) {
    return fetchAndLock(maxTasks, workerId, false);
  }

  @Override
  public ExternalTaskQueryBuilder fetchAndLock(int maxTasks, String workerId, boolean usePriority) {
    return new ExternalTaskQueryTopicBuilderImpl(commandExecutor, workerId, maxTasks, usePriority);
  }

  @Override
  public FetchAndLockBuilder fetchAndLock() {
    return new FetchAndLockBuilderImpl(commandExecutor);
  }

  @Override
  public void lock(String externalTaskId, String workerId, long lockDuration) {
    getCommandExecutor().execute(new LockExternalTaskCmd(externalTaskId, workerId, lockDuration));
  }

  @Override
  public void complete(String externalTaskId, String workerId) {
    complete(externalTaskId, workerId, emptyMap(), emptyMap());
  }

  @Override
  public void complete(String externalTaskId, String workerId, @Nullable Map<String, Object> variables) {
    complete(externalTaskId, workerId, variables, emptyMap());
  }

  @Override
  public void complete(String externalTaskId, String workerId, @Nullable Map<String, Object> variables, @Nullable Map<String, Object> localVariables) {
    getCommandExecutor().execute(new CompleteExternalTaskCmd(externalTaskId, workerId, requireNonNullElse(variables, emptyMap()), requireNonNullElse(localVariables, emptyMap())));
  }

  @Override
  public void handleFailure(String externalTaskId, String workerId, @Nullable String errorMessage, int retries, long retryDuration) {
    this.handleFailure(externalTaskId,workerId,errorMessage,null,retries,retryDuration);
  }

  @Override
  public void handleFailure(String externalTaskId, String workerId, @Nullable String errorMessage, @Nullable String errorDetails, int retries, long retryDuration) {
    this.handleFailure(externalTaskId, workerId, errorMessage, errorDetails, retries, retryDuration, emptyMap(), emptyMap());
  }

  @Override
  public void handleFailure(String externalTaskId, String workerId, @Nullable String errorMessage, @Nullable String errorDetails, int retries, long retryDuration, @Nullable Map<String, Object> variables, @Nullable Map<String, Object> localVariables) {
    getCommandExecutor().execute(new HandleExternalTaskFailureCmd(externalTaskId, workerId, errorMessage, errorDetails, retries, retryDuration,
            requireNonNullElse(variables, emptyMap()), requireNonNullElse(localVariables, emptyMap())));
  }

  @Override
  public void handleBpmnError(String externalTaskId, String workerId, String errorCode) {
    handleBpmnError(externalTaskId, workerId, errorCode, null, null);
  }

  @Override
  public void handleBpmnError(String externalTaskId, String workerId, String errorCode, @Nullable String errorMessage) {
    handleBpmnError(externalTaskId, workerId, errorCode, errorMessage, null);
  }

  @Override
  public void handleBpmnError(String externalTaskId, String workerId, String errorCode, @Nullable String errorMessage, @Nullable Map<String, Object> variables) {
    getCommandExecutor().execute(new HandleExternalTaskBpmnErrorCmd(externalTaskId, workerId, errorCode, errorMessage, requireNonNullElse(variables, emptyMap())));
  }

  @Override
  public void unlock(String externalTaskId) {
    getCommandExecutor().execute(new UnlockExternalTaskCmd(externalTaskId));
  }

  public void setRetries(String externalTaskId, int retries, boolean writeUserOperationLog) {
    getCommandExecutor().execute(new SetExternalTaskRetriesCmd(externalTaskId, retries, writeUserOperationLog));
  }

  @Override
  public void setPriority(String externalTaskId, long priority) {
    getCommandExecutor().execute(new SetExternalTaskPriorityCmd(externalTaskId, priority));
  }

  @Override
  public ExternalTaskQuery createExternalTaskQuery() {
    return new ExternalTaskQueryImpl(commandExecutor);
  }

  @Override
  public List<String> getTopicNames() {
    return getCommandExecutor().execute(new GetTopicNamesCmd(false,false,false));
  }

  @Override
  public List<String> getTopicNames(boolean withLockedTasks, boolean withUnlockedTasks, boolean withRetriesLeft) {
    return getCommandExecutor().execute(new GetTopicNamesCmd(withLockedTasks, withUnlockedTasks, withRetriesLeft));
  }

  @Override
  public String getExternalTaskErrorDetails(String externalTaskId) {
    return getCommandExecutor().execute(new GetExternalTaskErrorDetailsCmd(externalTaskId));
  }

  @Override
  public void setRetries(String externalTaskId, int retries) {
    setRetries(externalTaskId, retries, true);
  }

  @Override
  public void setRetries(List<String> externalTaskIds, int retries) {
    updateRetries()
      .externalTaskIds(externalTaskIds)
      .set(retries);
  }

  @Override
  public Batch setRetriesAsync(List<String> externalTaskIds, ExternalTaskQuery externalTaskQuery, int retries) {
    return updateRetries()
        .externalTaskIds(externalTaskIds)
        .externalTaskQuery(externalTaskQuery)
        .setAsync(retries);
  }

  @Override
  public UpdateExternalTaskRetriesSelectBuilder updateRetries() {
    return new UpdateExternalTaskRetriesBuilderImpl(getCommandExecutor());
  }

  @Override
  public void extendLock(String externalTaskId, String workerId, long lockDuration) {
    getCommandExecutor().execute(new ExtendLockOnExternalTaskCmd(externalTaskId, workerId, lockDuration));
  }

}
