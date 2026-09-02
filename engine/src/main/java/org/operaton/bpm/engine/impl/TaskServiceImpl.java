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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.cmd.*;
import org.operaton.bpm.engine.impl.util.ExceptionUtil;
import org.operaton.bpm.engine.task.Attachment;
import org.operaton.bpm.engine.task.Comment;
import org.operaton.bpm.engine.task.Event;
import org.operaton.bpm.engine.task.IdentityLink;
import org.operaton.bpm.engine.task.IdentityLinkType;
import org.operaton.bpm.engine.task.NativeTaskQuery;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.task.TaskReport;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.value.TypedValue;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;


/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public @NullMarked class TaskServiceImpl extends ServiceImpl implements TaskService {

  @Override
  public Task newTask() {
    return getCommandExecutor().execute(new CreateTaskCmd(null));
  }

  @Override
  public Task newTask(String taskId) {
    return getCommandExecutor().execute(new CreateTaskCmd(taskId));
  }

  @Override
  public void saveTask(Task task) {
    getCommandExecutor().execute(new SaveTaskCmd(task));
  }

  @Override
  public void deleteTask(String taskId) {
    getCommandExecutor().execute(new DeleteTaskCmd(taskId, null, false));
  }

  @Override
  public void deleteTasks(Collection<String> taskIds) {
    getCommandExecutor().execute(new DeleteTaskCmd(taskIds, null, false));
  }

  @Override
  public void deleteTask(String taskId, boolean cascade) {
    getCommandExecutor().execute(new DeleteTaskCmd(taskId, null, cascade));
  }

  @Override
  public void deleteTasks(Collection<String> taskIds, boolean cascade) {
    getCommandExecutor().execute(new DeleteTaskCmd(taskIds, null, cascade));
  }

  @Override
  public void deleteTask(String taskId, String deleteReason) {
    getCommandExecutor().execute(new DeleteTaskCmd(taskId, deleteReason, false));
  }

  @Override
  public void deleteTasks(Collection<String> taskIds, String deleteReason) {
    getCommandExecutor().execute(new DeleteTaskCmd(taskIds, deleteReason, false));
  }

  @Override
  public void setAssignee(String taskId, @Nullable String userId) {
    getCommandExecutor().execute(new AssignTaskCmd(taskId, userId));
  }

  @Override
  public void setOwner(String taskId, String userId) {
    getCommandExecutor().execute(new SetTaskOwnerCmd(taskId, userId));
  }

  @Override
  public void addCandidateUser(String taskId, String userId) {
    getCommandExecutor().execute(new AddUserIdentityLinkCmd(taskId, userId, IdentityLinkType.CANDIDATE));
  }

  @Override
  public void addCandidateGroup(String taskId, String groupId) {
    getCommandExecutor().execute(new AddGroupIdentityLinkCmd(taskId, groupId, IdentityLinkType.CANDIDATE));
  }

  @Override
  public void addUserIdentityLink(String taskId, String userId, String identityLinkType) {
    getCommandExecutor().execute(new AddUserIdentityLinkCmd(taskId, userId, identityLinkType));
  }

  @Override
  public void addGroupIdentityLink(String taskId, String groupId, String identityLinkType) {
    getCommandExecutor().execute(new AddGroupIdentityLinkCmd(taskId, groupId, identityLinkType));
  }

  @Override
  public void deleteCandidateGroup(String taskId, String groupId) {
    getCommandExecutor().execute(new DeleteGroupIdentityLinkCmd(taskId, groupId, IdentityLinkType.CANDIDATE));
  }

  @Override
  public void deleteCandidateUser(String taskId, String userId) {
    getCommandExecutor().execute(new DeleteUserIdentityLinkCmd(taskId, userId, IdentityLinkType.CANDIDATE));
  }

  @Override
  public void deleteGroupIdentityLink(String taskId, String groupId, String identityLinkType) {
    getCommandExecutor().execute(new DeleteGroupIdentityLinkCmd(taskId, groupId, identityLinkType));
  }

  @Override
  public void deleteUserIdentityLink(String taskId, String userId, String identityLinkType) {
    getCommandExecutor().execute(new DeleteUserIdentityLinkCmd(taskId, userId, identityLinkType));
  }

  @Override
  public List<IdentityLink> getIdentityLinksForTask(String taskId) {
    return getCommandExecutor().execute(new GetIdentityLinksForTaskCmd(taskId));
  }

  @Override
  public void claim(String taskId, @Nullable String userId) {
    getCommandExecutor().execute(new ClaimTaskCmd(taskId, userId));
  }

  @Override
  public void complete(String taskId) {
    complete(taskId, null);
  }

  @Override
  public void complete(String taskId, @Nullable Map<String, Object> variables) {
    getCommandExecutor().execute(new CompleteTaskCmd(taskId, variables, false, false));
  }

  @Override
  public VariableMap completeWithVariablesInReturn(String taskId, @Nullable Map<String, Object> variables, boolean deserializeValues) {
    return getCommandExecutor().execute(new CompleteTaskCmd(taskId, variables, true, deserializeValues));
  }

  @Override
  public void delegateTask(String taskId, String userId) {
    getCommandExecutor().execute(new DelegateTaskCmd(taskId, userId));
  }

  @Override
  public void resolveTask(String taskId) {
    getCommandExecutor().execute(new ResolveTaskCmd(taskId, null));
  }

  @Override
  public void resolveTask(String taskId, @Nullable Map<String, Object> variables) {
    getCommandExecutor().execute(new ResolveTaskCmd(taskId, variables));
  }

  @Override
  public void setPriority(String taskId, int priority) {
    getCommandExecutor().execute(new SetTaskPriorityCmd(taskId, priority) );
  }

  @Override
  public void setName(String taskId, String name) {
    getCommandExecutor().execute(new SetTaskNameCmd(taskId, name));
  }

  @Override
  public void setDescription(String taskId, String description) {
    getCommandExecutor().execute(new SetTaskDescriptionCmd(taskId, description));
  }

  @Override
  public void setDueDate(String taskId, Date dueDate) {
    getCommandExecutor().execute(new SetTaskDueDateCmd(taskId, dueDate));
  }

  @Override
  public void setFollowUpDate(String taskId, Date followUpDate) {
    getCommandExecutor().execute(new SetTaskFollowUpDateCmd(taskId, followUpDate));
  }

  @Override
  public TaskQuery createTaskQuery() {
    return new TaskQueryImpl(commandExecutor);
  }

  @Override
  public NativeTaskQuery createNativeTaskQuery() {
    return new NativeTaskQueryImpl(commandExecutor);
  }

  @Override
  public VariableMap getVariables(String taskId) {
    return getVariablesTyped(taskId);
  }

  @Override
  public VariableMap getVariablesTyped(String taskId) {
    return getVariablesTyped(taskId, true);
  }

  @Override
  public VariableMap getVariablesTyped(String taskId, boolean deserializeValues) {
    return getCommandExecutor().execute(new GetTaskVariablesCmd(taskId, null, false, deserializeValues));
  }

  @Override
  public VariableMap getVariablesLocal(String taskId) {
    return getVariablesLocalTyped(taskId);
  }

  @Override
  public VariableMap getVariablesLocalTyped(String taskId) {
    return getVariablesLocalTyped(taskId, true);
  }

  @Override
  public VariableMap getVariablesLocalTyped(String taskId, boolean deserializeValues) {
    return getCommandExecutor().execute(new GetTaskVariablesCmd(taskId, null, true, deserializeValues));
  }

  @Override
  public VariableMap getVariables(String taskId, Collection<String> variableNames) {
    return getVariablesTyped(taskId, variableNames, true);
  }

  @Override
  public VariableMap getVariablesTyped(String taskId, Collection<String> variableNames, boolean deserializeValues) {
    return getCommandExecutor().execute(new GetTaskVariablesCmd(taskId, variableNames, false, deserializeValues));
  }

  @Override
  public VariableMap getVariablesLocal(String taskId, Collection<String> variableNames) {
    return getVariablesLocalTyped(taskId, variableNames, true);
  }

  @Override
  public VariableMap getVariablesLocalTyped(String taskId, Collection<String> variableNames, boolean deserializeValues) {
    return getCommandExecutor().execute(new GetTaskVariablesCmd(taskId, variableNames, true, deserializeValues));
  }

  @Override
  public Object getVariable(String taskId, String variableName) {
    return getCommandExecutor().execute(new GetTaskVariableCmd(taskId, variableName, false));
  }

  @Override
  public Object getVariableLocal(String taskId, String variableName) {
    return getCommandExecutor().execute(new GetTaskVariableCmd(taskId, variableName, true));
  }

  @Override
  public <T extends TypedValue> @Nullable T getVariableTyped(String taskId, String variableName) {
    return getVariableTyped(taskId, variableName, false, true);
  }

  @Override
  public <T extends TypedValue> T getVariableTyped(String taskId, String variableName, boolean deserializeValue) {
    return getVariableTyped(taskId, variableName, false, deserializeValue);
  }

  @Override
  public <T extends TypedValue> T getVariableLocalTyped(String taskId, String variableName) {
    return getVariableTyped(taskId, variableName, true, true);
  }

  @Override
  public <T extends TypedValue> T getVariableLocalTyped(String taskId, String variableName, boolean deserializeValue) {
    return getVariableTyped(taskId, variableName, true, deserializeValue);
  }

  @SuppressWarnings("unchecked")
  protected <T extends TypedValue> T getVariableTyped(String taskId, String variableName, boolean isLocal, boolean deserializeValue) {
    return (T) getCommandExecutor().execute(new GetTaskVariableCmdTyped(taskId, variableName, isLocal, deserializeValue));
  }

  @Override
  public void setVariable(String taskId, String variableName, Object value) {
    ensureNotNull("variableName", variableName);
    Map<String, Object> variables = new HashMap<>();
    variables.put(variableName, value);
    setVariables(taskId, variables, false);
  }

  @Override
  public void setVariableLocal(String taskId, String variableName, Object value) {
    ensureNotNull("variableName", variableName);
    Map<String, Object> variables = new HashMap<>();
    variables.put(variableName, value);
    setVariables(taskId, variables, true);
  }

  @Override
  public void setVariables(String taskId, Map<String, ? extends Object> variables) {
    setVariables(taskId, variables, false);
  }

  @Override
  public void setVariablesLocal(String taskId, Map<String, ? extends Object> variables) {
    setVariables(taskId, variables, true);
  }

  protected void setVariables(String taskId, Map<String, ? extends Object> variables, boolean local) {
    try {
      getCommandExecutor().execute(new SetTaskVariablesCmd(taskId, variables, local));
    } catch (ProcessEngineException ex) {
      if (ExceptionUtil.checkValueTooLongException(ex)) {
        throw new BadUserRequestException("Variable value is too long", ex);
      }
      throw ex;
    }
  }

  public void updateVariablesLocal(String taskId, Map<String, ? extends Object> modifications, Collection<String> deletions) {
    updateVariables(taskId, modifications, deletions, true);
  }

  public void updateVariables(String taskId, Map<String, ? extends Object> modifications, Collection<String> deletions) {
    updateVariables(taskId, modifications, deletions, false);
  }

  protected void updateVariables(String taskId, Map<String, ? extends Object> modifications, Collection<String> deletions, boolean local) {
    try {
      getCommandExecutor().execute(new PatchTaskVariablesCmd(taskId, modifications, deletions, local));
    } catch (ProcessEngineException ex) {
      if (ExceptionUtil.checkValueTooLongException(ex)) {
        throw new BadUserRequestException("Variable value is too long", ex);
      }
      throw ex;
    }
  }

  @Override
  public void removeVariable(String taskId, String variableName) {
    Collection<String> variableNames = new ArrayList<>();
    variableNames.add(variableName);
    getCommandExecutor().execute(new RemoveTaskVariablesCmd(taskId, variableNames, false));
  }

  @Override
  public void removeVariableLocal(String taskId, String variableName) {
    Collection<String> variableNames = new ArrayList<>(1);
    variableNames.add(variableName);
    getCommandExecutor().execute(new RemoveTaskVariablesCmd(taskId, variableNames, true));
  }

  @Override
  public void removeVariables(String taskId, Collection<String> variableNames) {
    getCommandExecutor().execute(new RemoveTaskVariablesCmd(taskId, variableNames, false));
  }

  @Override
  public void removeVariablesLocal(String taskId, Collection<String> variableNames) {
    getCommandExecutor().execute(new RemoveTaskVariablesCmd(taskId, variableNames, true));
  }

  @Override
  public void addComment(@Nullable String taskId, String processInstance, String message) {
    createComment(taskId, processInstance, message);
  }

  @Override
  public Comment createComment(@Nullable String taskId, String processInstance, String message) {
    return getCommandExecutor().execute(new AddCommentCmd(taskId, processInstance, message));
  }

  @Override
  public void deleteTaskComment(String taskId, String commentId) {
    getCommandExecutor().execute(new DeleteTaskCommentCmd(taskId, commentId));
  }

  @Override
  public void deleteProcessInstanceComment(String processInstanceId, String commentId) {
    getCommandExecutor().execute(new DeleteProcessInstanceCommentCmd(processInstanceId, commentId));
  }

  @Override
  public void deleteTaskComments(String taskId) {
    getCommandExecutor().execute(new DeleteTaskCommentCmd(taskId));
  }

  @Override
  public void deleteProcessInstanceComments(String processInstanceId) {
    getCommandExecutor().execute(new DeleteProcessInstanceCommentCmd(processInstanceId));
  }

  @Override
  public void updateTaskComment(String taskId, String commentId, String message) {
    getCommandExecutor().execute(new UpdateCommentCmd(taskId, null, commentId, message));
  }

  @Override
  public void updateProcessInstanceComment(String processInstanceId, String commentId, String message) {
    getCommandExecutor().execute(new UpdateCommentCmd(null, processInstanceId, commentId, message));
  }

  @Override
  public List<Comment> getTaskComments(String taskId) {
    return getCommandExecutor().execute(new GetTaskCommentsCmd(taskId));
  }

  @Override
  public Comment getTaskComment(String taskId, String commentId) {
    return getCommandExecutor().execute(new GetTaskCommentCmd(taskId, commentId));
  }

  @Override
  @SuppressWarnings("java:S5738")
  public List<Event> getTaskEvents(String taskId) {
    return getCommandExecutor().execute(new GetTaskEventsCmd(taskId));
  }

  @Override
  public List<Comment> getProcessInstanceComments(String processInstanceId) {
    return getCommandExecutor().execute(new GetProcessInstanceCommentsCmd(processInstanceId));
  }

  @Override
  public Attachment createAttachment(@Nullable String attachmentType, @Nullable String taskId, @Nullable String processInstanceId, @Nullable String attachmentName, @Nullable String attachmentDescription, @Nullable InputStream content) {
    return getCommandExecutor().execute(new CreateAttachmentCmd(attachmentType, taskId, processInstanceId, attachmentName, attachmentDescription, content, null));
  }

  @Override
  public Attachment createAttachment(@Nullable String attachmentType, @Nullable String taskId, @Nullable String processInstanceId, @Nullable String attachmentName, @Nullable String attachmentDescription, @Nullable String url) {
    return getCommandExecutor().execute(new CreateAttachmentCmd(attachmentType, taskId, processInstanceId, attachmentName, attachmentDescription, null, url));
  }

  @Override
  public InputStream getAttachmentContent(String attachmentId) {
    return getCommandExecutor().execute(new GetAttachmentContentCmd(attachmentId));
  }

  @Override
  public InputStream getTaskAttachmentContent(String taskId, String attachmentId) {
    return getCommandExecutor().execute(new GetTaskAttachmentContentCmd(taskId, attachmentId));
  }

  @Override
  public void deleteAttachment(String attachmentId) {
    getCommandExecutor().execute(new DeleteAttachmentCmd(attachmentId));
  }

  @Override
  public void deleteTaskAttachment(@Nullable String taskId, String attachmentId) {
    getCommandExecutor().execute(new DeleteAttachmentCmd(taskId, attachmentId));
  }

  @Override
  public Attachment getAttachment(String attachmentId) {
    return getCommandExecutor().execute(new GetAttachmentCmd(attachmentId));
  }

  @Override
  public Attachment getTaskAttachment(String taskId, String attachmentId) {
    return getCommandExecutor().execute(new GetTaskAttachmentCmd(taskId, attachmentId));
  }

  @Override
  public List<Attachment> getTaskAttachments(String taskId) {
    return getCommandExecutor().execute(new GetTaskAttachmentsCmd(taskId));
  }

  @Override
  public List<Attachment> getProcessInstanceAttachments(String processInstanceId) {
    return getCommandExecutor().execute(new GetProcessInstanceAttachmentsCmd(processInstanceId));
  }

  @Override
  public void saveAttachment(Attachment attachment) {
    getCommandExecutor().execute(new SaveAttachmentCmd(attachment));
  }

  @Override
  public List<Task> getSubTasks(String parentTaskId) {
    return getCommandExecutor().execute(new GetSubTasksCmd(parentTaskId));
  }

  @Override
  public TaskReport createTaskReport() {
    return new TaskReportImpl(commandExecutor);
  }

  @Override
  public void handleBpmnError(String taskId, String errorCode) {
    getCommandExecutor().execute(new HandleTaskBpmnErrorCmd(taskId, errorCode));
  }

  @Override
  public void handleBpmnError(String taskId, String errorCode, String errorMessage) {
    getCommandExecutor().execute(new HandleTaskBpmnErrorCmd(taskId, errorCode, errorMessage));
  }

  @Override
  public void handleBpmnError(String taskId, String errorCode, String errorMessage, Map<String, Object> variables) {
    getCommandExecutor().execute(new HandleTaskBpmnErrorCmd(taskId, errorCode, errorMessage, variables));
  }

  @Override
  public void handleEscalation(String taskId, String escalationCode) {
    getCommandExecutor().execute(new HandleTaskEscalationCmd(taskId, escalationCode));
  }

  @Override
  public void handleEscalation(String taskId, String escalationCode, Map<String, Object> variables) {
    getCommandExecutor().execute(new HandleTaskEscalationCmd(taskId, escalationCode, variables));
  }
}
