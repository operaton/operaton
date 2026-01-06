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
package org.operaton.bpm.engine.impl.persistence.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.TaskQueryImpl;
import org.operaton.bpm.engine.impl.cfg.auth.ResourceAuthorizationProvider;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.ListQueryParameterObject;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.AbstractManager;
import org.operaton.bpm.engine.task.Task;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;


/**
 * @author Tom Baeyens
 */
public class TaskManager extends AbstractManager {

  private static final String CASE_EXECUTION_ID = "caseExecutionId";

  public void insertTask(TaskEntity task) {
    getDbEntityManager().insert(task);
    createDefaultAuthorizations(task);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void deleteTasksByProcessInstanceId(String processInstanceId, String deleteReason, boolean cascade, boolean skipCustomListeners) {
    List<TaskEntity> tasks = (List) getDbEntityManager()
      .createTaskQuery()
      .processInstanceId(processInstanceId)
      .list();

    String reason = deleteReason == null || deleteReason.isEmpty() ? TaskEntity.DELETE_REASON_DELETED : deleteReason;

    for (TaskEntity task: tasks) {
      task.delete(reason, cascade, skipCustomListeners);
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void deleteTasksByCaseInstanceId(String caseInstanceId, String deleteReason, boolean cascade) {
    List<TaskEntity> tasks = (List) getDbEntityManager()
        .createTaskQuery()
        .caseInstanceId(caseInstanceId)
        .list();

      String reason = deleteReason == null || deleteReason.isEmpty() ? TaskEntity.DELETE_REASON_DELETED : deleteReason;

      for (TaskEntity task: tasks) {
        task.delete(reason, cascade, false);
      }
  }

  public void deleteTask(TaskEntity task, String deleteReason, boolean cascade, boolean skipCustomListeners) {
    if (!task.isDeleted()) {
      task.setDeleted(true);

      CommandContext commandContext = Context.getCommandContext();
      String taskId = task.getId();

      List<Task> subTasks = findTasksByParentTaskId(taskId);
      for (Task subTask: subTasks) {
        ((TaskEntity) subTask).delete(deleteReason, cascade, skipCustomListeners);
      }

      task.deleteIdentityLinks();

      commandContext
        .getVariableInstanceManager()
        .deleteVariableInstanceByTask(task);

      if (cascade) {
        commandContext
          .getHistoricTaskInstanceManager()
          .deleteHistoricTaskInstanceById(taskId);
      } else {
        commandContext
          .getHistoricTaskInstanceManager()
          .markTaskInstanceEnded(taskId, deleteReason);
      }

      deleteAuthorizations(Resources.TASK, taskId);
      getDbEntityManager().delete(task);
    }
  }

  public TaskEntity findTaskById(String id) {
    ensureNotNull("Invalid task id", "id", id);
    return getDbEntityManager().selectById(TaskEntity.class, id);
  }

  @SuppressWarnings("unchecked")
  public List<TaskEntity> findTasksByExecutionId(String executionId) {
    return getDbEntityManager().selectList("selectTasksByExecutionId", executionId);
  }

  public TaskEntity findTaskByCaseExecutionId(String caseExecutionId) {
    return (TaskEntity) getDbEntityManager().selectOne("selectTaskByCaseExecutionId", caseExecutionId);
  }

  @SuppressWarnings("unchecked")
  public List<TaskEntity> findTasksByProcessInstanceId(String processInstanceId) {
    return getDbEntityManager().selectList("selectTasksByProcessInstanceId", processInstanceId);
  }

  /**
   * @deprecated since 1.0, use {@link #findTasksByQueryCriteria(TaskQueryImpl)} instead,
   *             which allows pagination to be set directly on the query object.
   */
  @Deprecated(since = "1.0")
  public List<Task> findTasksByQueryCriteria(TaskQueryImpl taskQuery, Page page) {
    taskQuery.setFirstResult(page.getFirstResult());
    taskQuery.setMaxResults(page.getMaxResults());
    return findTasksByQueryCriteria(taskQuery);
  }

  @SuppressWarnings("unchecked")
  public List<Task> findTasksByQueryCriteria(TaskQueryImpl taskQuery) {
    configureQuery(taskQuery);
    return getDbEntityManager().selectList("selectTaskByQueryCriteria", taskQuery);
  }

  public long findTaskCountByQueryCriteria(TaskQueryImpl taskQuery) {
    configureQuery(taskQuery);
    return (Long) getDbEntityManager().selectOne("selectTaskCountByQueryCriteria", taskQuery);
  }

  @SuppressWarnings("unchecked")
  public List<Task> findTasksByNativeQuery(Map<String, Object> parameterMap, int firstResult, int maxResults) {
    return getDbEntityManager().selectListWithRawParameter("selectTaskByNativeQuery", parameterMap, firstResult, maxResults);
  }

  public long findTaskCountByNativeQuery(Map<String, Object> parameterMap) {
    return (Long) getDbEntityManager().selectOne("selectTaskCountByNativeQuery", parameterMap);
  }

  @SuppressWarnings("unchecked")
  public List<Task> findTasksByParentTaskId(String parentTaskId) {
    return getDbEntityManager().selectList("selectTasksByParentTaskId", parentTaskId);
  }

  public void updateTaskSuspensionStateByProcessDefinitionId(String processDefinitionId, SuspensionState suspensionState) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(PROCESS_DEFINITION_ID, processDefinitionId);
    parameters.put(SUSPENSION_STATE, suspensionState.getStateCode());
    getDbEntityManager().update(TaskEntity.class, "updateTaskSuspensionStateByParameters", configureParameterizedQuery(parameters));
  }

  public void updateTaskSuspensionStateByProcessInstanceId(String processInstanceId, SuspensionState suspensionState) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(PROCESS_INSTANCE_ID, processInstanceId);
    parameters.put(SUSPENSION_STATE, suspensionState.getStateCode());
    getDbEntityManager().update(TaskEntity.class, "updateTaskSuspensionStateByParameters", configureParameterizedQuery(parameters));
  }

  public void updateTaskSuspensionStateByProcessDefinitionKey(String processDefinitionKey, SuspensionState suspensionState) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(PROCESS_DEFINITION_KEY, processDefinitionKey);
    parameters.put(IS_PROCESS_DEFINITION_TENANT_ID_SET, false);
    parameters.put(SUSPENSION_STATE, suspensionState.getStateCode());
    getDbEntityManager().update(TaskEntity.class, "updateTaskSuspensionStateByParameters", configureParameterizedQuery(parameters));
  }

  public void updateTaskSuspensionStateByProcessDefinitionKeyAndTenantId(String processDefinitionKey, String processDefinitionTenantId, SuspensionState suspensionState) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(PROCESS_DEFINITION_KEY, processDefinitionKey);
    parameters.put(IS_PROCESS_DEFINITION_TENANT_ID_SET, true);
    parameters.put(PROCESS_DEFINITION_TENANT_ID, processDefinitionTenantId);
    parameters.put(SUSPENSION_STATE, suspensionState.getStateCode());
    getDbEntityManager().update(TaskEntity.class, "updateTaskSuspensionStateByParameters", configureParameterizedQuery(parameters));
  }

  public void updateTaskSuspensionStateByCaseExecutionId(String caseExecutionId, SuspensionState suspensionState) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(CASE_EXECUTION_ID, caseExecutionId);
    parameters.put(SUSPENSION_STATE, suspensionState.getStateCode());
    getDbEntityManager().update(TaskEntity.class, "updateTaskSuspensionStateByParameters", configureParameterizedQuery(parameters));

  }

  // helper ///////////////////////////////////////////////////////////

  protected void createDefaultAuthorizations(TaskEntity task) {
    if(isAuthorizationEnabled()) {
      ResourceAuthorizationProvider provider = getResourceAuthorizationProvider();
      AuthorizationEntity[] authorizations = provider.newTask(task);
      saveDefaultAuthorizations(authorizations);
    }
  }

  protected void configureQuery(TaskQueryImpl query) {
    getAuthorizationManager().configureTaskQuery(query);
    getTenantManager().configureQuery(query);
  }

  protected ListQueryParameterObject configureParameterizedQuery(Object parameter) {
    return getTenantManager().configureQuery(parameter);
  }

}
