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
public class TaskServiceImpl extends ServiceImpl implements TaskService {

  @Override
  public Task newTask() {
    return newTask(null);
  }

  @Override
  public Task newTask(String taskId) {
    return commandExecutor.execute(new CreateTaskCmd(taskId));
  }

  @Override
  public void saveTask(Task task) {
    commandExecutor.execute(new SaveTaskCmd(task));
  }

  @Override
  public void deleteTask(String taskId) {
    commandExecutor.execute(new DeleteTaskCmd(taskId, null, false));
  }

  @Override
  public void deleteTasks(Collection<String> taskIds) {
    commandExecutor.execute(new DeleteTaskCmd(taskIds, null, false));
  }

  @Override
  public void deleteTask(String taskId, boolean cascade) {
    commandExecutor.execute(new DeleteTaskCmd(taskId, null, cascade));
  }

  @Override
  public void deleteTasks(Collection<String> taskIds, boolean cascade) {
    commandExecutor.execute(new DeleteTaskCmd(taskIds, null, cascade));
  }

  @Override
  public void deleteTask(String taskId, String deleteReason) {
    commandExecutor.execute(new DeleteTaskCmd(taskId, deleteReason, false));
  }

  @Override
  public void deleteTasks(Collection<String> taskIds, String deleteReason) {
    commandExecutor.execute(new DeleteTaskCmd(taskIds, deleteReason, false));
  }

  @Override
  public void setAssignee(String taskId, String userId) {
    commandExecutor.execute(new AssignTaskCmd(taskId, userId));
  }

  @Override
  public void setOwner(String taskId, String userId) {
    commandExecutor.execute(new SetTaskOwnerCmd(taskId, userId));
  }

  @Override
  public void addCandidateUser(String taskId, String userId) {
    commandExecutor.execute(new AddUserIdentityLinkCmd(taskId, userId, IdentityLinkType.CANDIDATE));
  }

  @Override
  public void addCandidateGroup(String taskId, String groupId) {
    commandExecutor.execute(new AddGroupIdentityLinkCmd(taskId, groupId, IdentityLinkType.CANDIDATE));
  }

  @Override
  public void addUserIdentityLink(String taskId, String userId, String identityLinkType) {
    commandExecutor.execute(new AddUserIdentityLinkCmd(taskId, userId, identityLinkType));
  }

  @Override
  public void addGroupIdentityLink(String taskId, String groupId, String identityLinkType) {
    commandExecutor.execute(new AddGroupIdentityLinkCmd(taskId, groupId, identityLinkType));
  }

  @Override
  public void deleteCandidateGroup(String taskId, String groupId) {
    commandExecutor.execute(new DeleteGroupIdentityLinkCmd(taskId, groupId, IdentityLinkType.CANDIDATE));
  }

  @Override
  public void deleteCandidateUser(String taskId, String userId) {
    commandExecutor.execute(new DeleteUserIdentityLinkCmd(taskId, userId, IdentityLinkType.CANDIDATE));
  }

  @Override
  public void deleteGroupIdentityLink(String taskId, String groupId, String identityLinkType) {
    commandExecutor.execute(new DeleteGroupIdentityLinkCmd(taskId, groupId, identityLinkType));
  }

  @Override
  public void deleteUserIdentityLink(String taskId, String userId, String identityLinkType) {
    commandExecutor.execute(new DeleteUserIdentityLinkCmd(taskId, userId, identityLinkType));
  }

  @Override
  public List<IdentityLink> getIdentityLinksForTask(String taskId) {
    return commandExecutor.execute(new GetIdentityLinksForTaskCmd(taskId));
  }

  @Override
  public void claim(String taskId, String userId) {
    commandExecutor.execute(new ClaimTaskCmd(taskId, userId));
  }

  @Override
  public void complete(String taskId) {
    complete(taskId, null);
  }

  @Override
  public void complete(String taskId, Map<String, Object> variables) {
    commandExecutor.execute(new CompleteTaskCmd(taskId, variables, false, false));
  }

  @Override
  public VariableMap completeWithVariablesInReturn(String taskId, Map<String, Object> variables, boolean deserializeValues) {
    return commandExecutor.execute(new CompleteTaskCmd(taskId, variables, true, deserializeValues));
  }

  @Override
  public void delegateTask(String taskId, String userId) {
    commandExecutor.execute(new DelegateTaskCmd(taskId, userId));
  }

  @Override
  public void resolveTask(String taskId) {
    commandExecutor.execute(new ResolveTaskCmd(taskId, null));
  }

  @Override
  public void resolveTask(String taskId, Map<String, Object> variables) {
    commandExecutor.execute(new ResolveTaskCmd(taskId, variables));
  }

  @Override
  public void setPriority(String taskId, int priority) {
    commandExecutor.execute(new SetTaskPriorityCmd(taskId, priority) );
  }

  @Override
  public void setName(String taskId, String name) {
    commandExecutor.execute(new SetTaskNameCmd(taskId, name));
  }

  @Override
  public void setDescription(String taskId, String description) {
    commandExecutor.execute(new SetTaskDescriptionCmd(taskId, description));
  }

  @Override
  public void setDueDate(String taskId, Date dueDate) {
    commandExecutor.execute(new SetTaskDueDateCmd(taskId, dueDate));
  }

  @Override
  public void setFollowUpDate(String taskId, Date followUpDate) {
    commandExecutor.execute(new SetTaskFollowUpDateCmd(taskId, followUpDate));
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
    return commandExecutor.execute(new GetTaskVariablesCmd(taskId, null, false, deserializeValues));
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
    return commandExecutor.execute(new GetTaskVariablesCmd(taskId, null, true, deserializeValues));
  }

  @Override
  public VariableMap getVariables(String taskId, Collection<String> variableNames) {
    return getVariablesTyped(taskId, variableNames, true);
  }

  @Override
  public VariableMap getVariablesTyped(String taskId, Collection<String> variableNames, boolean deserializeValues) {
    return commandExecutor.execute(new GetTaskVariablesCmd(taskId, variableNames, false, deserializeValues));
  }

  @Override
  public VariableMap getVariablesLocal(String taskId, Collection<String> variableNames) {
    return getVariablesLocalTyped(taskId, variableNames, true);
  }

  @Override
  public VariableMap getVariablesLocalTyped(String taskId, Collection<String> variableNames, boolean deserializeValues) {
    return commandExecutor.execute(new GetTaskVariablesCmd(taskId, variableNames, true, deserializeValues));
  }

  @Override
  public Object getVariable(String taskId, String variableName) {
    return commandExecutor.execute(new GetTaskVariableCmd(taskId, variableName, false));
  }

  @Override
  public Object getVariableLocal(String taskId, String variableName) {
    return commandExecutor.execute(new GetTaskVariableCmd(taskId, variableName, true));
  }

  @Override
  public <T extends TypedValue> T getVariableTyped(String taskId, String variableName) {
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
    return (T) commandExecutor.execute(new GetTaskVariableCmdTyped(taskId, variableName, isLocal, deserializeValue));
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
      commandExecutor.execute(new SetTaskVariablesCmd(taskId, variables, local));
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
      commandExecutor.execute(new PatchTaskVariablesCmd(taskId, modifications, deletions, local));
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
    commandExecutor.execute(new RemoveTaskVariablesCmd(taskId, variableNames, false));
  }

  @Override
  public void removeVariableLocal(String taskId, String variableName) {
    Collection<String> variableNames = new ArrayList<>(1);
    variableNames.add(variableName);
    commandExecutor.execute(new RemoveTaskVariablesCmd(taskId, variableNames, true));
  }

  @Override
  public void removeVariables(String taskId, Collection<String> variableNames) {
    commandExecutor.execute(new RemoveTaskVariablesCmd(taskId, variableNames, false));
  }

  @Override
  public void removeVariablesLocal(String taskId, Collection<String> variableNames) {
    commandExecutor.execute(new RemoveTaskVariablesCmd(taskId, variableNames, true));
  }

  @Override
  public void addComment(String taskId, String processInstance, String message) {
    createComment(taskId, processInstance, message);
  }

  @Override
  public Comment createComment(String taskId, String processInstance, String message) {
    return commandExecutor.execute(new AddCommentCmd(taskId, processInstance, message));
  }

  @Override
  public void deleteTaskComment(String taskId, String commentId) {
    commandExecutor.execute(new DeleteTaskCommentCmd(taskId, commentId));
  }

  @Override
  public void deleteProcessInstanceComment(String processInstanceId, String commentId) {
    commandExecutor.execute(new DeleteProcessInstanceCommentCmd(processInstanceId, commentId));
  }

  @Override
  public void deleteTaskComments(String taskId) {
    commandExecutor.execute(new DeleteTaskCommentCmd(taskId));
  }

  @Override
  public void deleteProcessInstanceComments(String processInstanceId) {
    commandExecutor.execute(new DeleteProcessInstanceCommentCmd(processInstanceId));
  }

  @Override
  public void updateTaskComment(String taskId, String commentId, String message) {
    commandExecutor.execute(new UpdateCommentCmd(taskId, null, commentId, message));
  }

  @Override
  public void updateProcessInstanceComment(String processInstanceId, String commentId, String message) {
    commandExecutor.execute(new UpdateCommentCmd(null, processInstanceId, commentId, message));
  }

  @Override
  public List<Comment> getTaskComments(String taskId) {
    return commandExecutor.execute(new GetTaskCommentsCmd(taskId));
  }

  @Override
  public Comment getTaskComment(String taskId, String commentId) {
    return commandExecutor.execute(new GetTaskCommentCmd(taskId, commentId));
  }

  @Override
  @SuppressWarnings("java:S5738")
  public List<Event> getTaskEvents(String taskId) {
    return commandExecutor.execute(new GetTaskEventsCmd(taskId));
  }

  @Override
  public List<Comment> getProcessInstanceComments(String processInstanceId) {
    return commandExecutor.execute(new GetProcessInstanceCommentsCmd(processInstanceId));
  }

  @Override
  public Attachment createAttachment(String attachmentType, String taskId, String processInstanceId, String attachmentName, String attachmentDescription, InputStream content) {
    return commandExecutor.execute(new CreateAttachmentCmd(attachmentType, taskId, processInstanceId, attachmentName, attachmentDescription, content, null));
  }

  @Override
  public Attachment createAttachment(String attachmentType, String taskId, String processInstanceId, String attachmentName, String attachmentDescription, String url) {
    return commandExecutor.execute(new CreateAttachmentCmd(attachmentType, taskId, processInstanceId, attachmentName, attachmentDescription, null, url));
  }

  @Override
  public InputStream getAttachmentContent(String attachmentId) {
    return commandExecutor.execute(new GetAttachmentContentCmd(attachmentId));
  }

  @Override
  public InputStream getTaskAttachmentContent(String taskId, String attachmentId) {
    return commandExecutor.execute(new GetTaskAttachmentContentCmd(taskId, attachmentId));
  }

  @Override
  public void deleteAttachment(String attachmentId) {
    commandExecutor.execute(new DeleteAttachmentCmd(attachmentId));
  }

  @Override
  public void deleteTaskAttachment(String taskId, String attachmentId) {
    commandExecutor.execute(new DeleteAttachmentCmd(taskId, attachmentId));
  }

  @Override
  public Attachment getAttachment(String attachmentId) {
    return commandExecutor.execute(new GetAttachmentCmd(attachmentId));
  }

  @Override
  public Attachment getTaskAttachment(String taskId, String attachmentId) {
    return commandExecutor.execute(new GetTaskAttachmentCmd(taskId, attachmentId));
  }

  @Override
  public List<Attachment> getTaskAttachments(String taskId) {
    return commandExecutor.execute(new GetTaskAttachmentsCmd(taskId));
  }

  @Override
  public List<Attachment> getProcessInstanceAttachments(String processInstanceId) {
    return commandExecutor.execute(new GetProcessInstanceAttachmentsCmd(processInstanceId));
  }

  @Override
  public void saveAttachment(Attachment attachment) {
    commandExecutor.execute(new SaveAttachmentCmd(attachment));
  }

  @Override
  public List<Task> getSubTasks(String parentTaskId) {
    return commandExecutor.execute(new GetSubTasksCmd(parentTaskId));
  }

  @Override
  public TaskReport createTaskReport() {
    return new TaskReportImpl(commandExecutor);
  }

  @Override
  public void handleBpmnError(String taskId, String errorCode) {
    commandExecutor.execute(new HandleTaskBpmnErrorCmd(taskId, errorCode));
  }

  @Override
  public void handleBpmnError(String taskId, String errorCode, String errorMessage) {
    commandExecutor.execute(new HandleTaskBpmnErrorCmd(taskId, errorCode, errorMessage));
  }

  @Override
  public void handleBpmnError(String taskId, String errorCode, String errorMessage, Map<String, Object> variables) {
    commandExecutor.execute(new HandleTaskBpmnErrorCmd(taskId, errorCode, errorMessage, variables));
  }

  @Override
  public void handleEscalation(String taskId, String escalationCode) {
    commandExecutor.execute(new HandleTaskEscalationCmd(taskId, escalationCode));
  }

  @Override
  public void handleEscalation(String taskId, String escalationCode, Map<String, Object> variables) {
    commandExecutor.execute(new HandleTaskEscalationCmd(taskId, escalationCode, variables));
  }
}
