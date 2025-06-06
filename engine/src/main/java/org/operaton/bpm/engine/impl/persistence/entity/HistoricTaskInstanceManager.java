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

import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.impl.HistoricTaskInstanceQueryImpl;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.ListQueryParameterObject;
import org.operaton.bpm.engine.impl.db.entitymanager.operation.DbOperation;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.history.event.HistoricTaskInstanceEventEntity;
import org.operaton.bpm.engine.impl.history.event.HistoryEvent;
import org.operaton.bpm.engine.impl.history.event.HistoryEventProcessor;
import org.operaton.bpm.engine.impl.history.event.HistoryEventTypes;
import org.operaton.bpm.engine.impl.history.producer.HistoryEventProducer;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.AbstractHistoricManager;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;


/**
 * @author  Tom Baeyens
 */
public class HistoricTaskInstanceManager extends AbstractHistoricManager {

  /**
   * Deletes all data related with tasks, which belongs to specified process instance ids.
   * @param processInstanceIds
   * @param deleteVariableInstances when true, will also delete variable instances. Can be false when variable instances were deleted separately.
   */
  public void deleteHistoricTaskInstancesByProcessInstanceIds(List<String> processInstanceIds, boolean deleteVariableInstances) {

    CommandContext commandContext = Context.getCommandContext();

    if (deleteVariableInstances) {
      getHistoricVariableInstanceManager().deleteHistoricVariableInstancesByTaskProcessInstanceIds(processInstanceIds);
    }

    getHistoricDetailManager()
        .deleteHistoricDetailsByTaskProcessInstanceIds(processInstanceIds);

    commandContext
        .getCommentManager()
        .deleteCommentsByTaskProcessInstanceIds(processInstanceIds);

    getAttachmentManager()
        .deleteAttachmentsByTaskProcessInstanceIds(processInstanceIds);

    getHistoricIdentityLinkManager()
        .deleteHistoricIdentityLinksLogByTaskProcessInstanceIds(processInstanceIds);

    getDbEntityManager().deletePreserveOrder(HistoricTaskInstanceEntity.class, "deleteHistoricTaskInstanceByProcessInstanceIds", processInstanceIds);
  }

  public void deleteHistoricTaskInstancesByCaseInstanceIds(List<String> caseInstanceIds) {

    CommandContext commandContext = Context.getCommandContext();

    getHistoricDetailManager()
        .deleteHistoricDetailsByTaskCaseInstanceIds(caseInstanceIds);

    commandContext
        .getCommentManager()
        .deleteCommentsByTaskCaseInstanceIds(caseInstanceIds);

    getAttachmentManager()
        .deleteAttachmentsByTaskCaseInstanceIds(caseInstanceIds);

    getHistoricIdentityLinkManager()
        .deleteHistoricIdentityLinksLogByTaskCaseInstanceIds(caseInstanceIds);

    getDbEntityManager().deletePreserveOrder(HistoricTaskInstanceEntity.class, "deleteHistoricTaskInstanceByCaseInstanceIds", caseInstanceIds);
  }

  public long findHistoricTaskInstanceCountByQueryCriteria(final HistoricTaskInstanceQueryImpl historicTaskInstanceQuery) {
    if (isHistoryEnabled()) {
      configureQuery(historicTaskInstanceQuery);
      return (Long) getDbEntityManager().selectOne("selectHistoricTaskInstanceCountByQueryCriteria",historicTaskInstanceQuery);
    }

    return 0;
  }

  @SuppressWarnings("unchecked")
  public List<HistoricTaskInstance> findHistoricTaskInstancesByQueryCriteria(final HistoricTaskInstanceQueryImpl historicTaskInstanceQuery, final Page page) {
    if (isHistoryEnabled()) {
      configureQuery(historicTaskInstanceQuery);
      return getDbEntityManager().selectList("selectHistoricTaskInstancesByQueryCriteria", historicTaskInstanceQuery, page);
    }

    return Collections.emptyList();
  }

  public HistoricTaskInstanceEntity findHistoricTaskInstanceById(final String taskId) {
    ensureNotNull("Invalid historic task id", "taskId", taskId);

    if (isHistoryEnabled()) {
      return (HistoricTaskInstanceEntity) getDbEntityManager().selectOne("selectHistoricTaskInstance", taskId);
    }

    return null;
  }

  public void deleteHistoricTaskInstanceById(final String taskId) {
    if (isHistoryEnabled()) {
      HistoricTaskInstanceEntity historicTaskInstance = findHistoricTaskInstanceById(taskId);
      if (historicTaskInstance != null) {
        CommandContext commandContext = Context.getCommandContext();

        commandContext
          .getHistoricDetailManager()
          .deleteHistoricDetailsByTaskId(taskId);

        commandContext
          .getHistoricVariableInstanceManager()
          .deleteHistoricVariableInstancesByTaskId(taskId);

        commandContext
          .getCommentManager()
          .deleteCommentsByTaskId(taskId);

        commandContext
          .getAttachmentManager()
          .deleteAttachmentsByTaskId(taskId);

        commandContext
          .getHistoricIdentityLinkManager()
          .deleteHistoricIdentityLinksLogByTaskId(taskId);

        deleteAuthorizations(Resources.HISTORIC_TASK, taskId);

        getDbEntityManager().delete(historicTaskInstance);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public List<HistoricTaskInstance> findHistoricTaskInstancesByNativeQuery(final Map<String, Object> parameterMap,
          final int firstResult, final int maxResults) {
    return getDbEntityManager().selectListWithRawParameter("selectHistoricTaskInstanceByNativeQuery", parameterMap,
            firstResult, maxResults);
  }

  public long findHistoricTaskInstanceCountByNativeQuery(final Map<String, Object> parameterMap) {
    return (Long) getDbEntityManager().selectOne("selectHistoricTaskInstanceCountByNativeQuery", parameterMap);
  }

  public void updateHistoricTaskInstance(final TaskEntity taskEntity) {
    ProcessEngineConfigurationImpl configuration = Context.getProcessEngineConfiguration();

    HistoryLevel historyLevel = configuration.getHistoryLevel();
    if(historyLevel.isHistoryEventProduced(HistoryEventTypes.TASK_INSTANCE_UPDATE, taskEntity)) {

      HistoryEventProcessor.processHistoryEvents(new HistoryEventProcessor.HistoryEventCreator() {
        @Override
        public HistoryEvent createHistoryEvent(HistoryEventProducer producer) {
          return producer.createTaskInstanceUpdateEvt(taskEntity);
        }
      });
    }
  }

  public DbOperation addRemovalTimeToTaskInstancesByRootProcessInstanceId(String rootProcessInstanceId, Date removalTime, Integer batchSize) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(ROOT_PROCESS_INSTANCE_ID, rootProcessInstanceId);
    parameters.put(REMOVAL_TIME, removalTime);
    parameters.put(MAX_RESULTS, batchSize);

    return getDbEntityManager()
      .updatePreserveOrder(HistoricTaskInstanceEventEntity.class, "updateHistoricTaskInstancesByRootProcessInstanceId", parameters);
  }

  public DbOperation addRemovalTimeToTaskInstancesByProcessInstanceId(String processInstanceId, Date removalTime, Integer batchSize) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(PROCESS_INSTANCE_ID, processInstanceId);
    parameters.put(REMOVAL_TIME, removalTime);
    parameters.put(MAX_RESULTS, batchSize);

    return getDbEntityManager()
      .updatePreserveOrder(HistoricTaskInstanceEventEntity.class, "updateHistoricTaskInstancesByProcessInstanceId", parameters);
  }

  public void markTaskInstanceEnded(String taskId, final String deleteReason) {
    ProcessEngineConfigurationImpl configuration = Context.getProcessEngineConfiguration();

    final TaskEntity taskEntity = Context.getCommandContext()
        .getDbEntityManager()
        .selectById(TaskEntity.class, taskId);

    HistoryLevel historyLevel = configuration.getHistoryLevel();
    if(historyLevel.isHistoryEventProduced(HistoryEventTypes.TASK_INSTANCE_COMPLETE, taskEntity)) {

      HistoryEventProcessor.processHistoryEvents(new HistoryEventProcessor.HistoryEventCreator() {
        @Override
        public HistoryEvent createHistoryEvent(HistoryEventProducer producer) {
          return producer.createTaskInstanceCompleteEvt(taskEntity, deleteReason);
        }
      });
    }
  }


  public void createHistoricTask(final TaskEntity task) {
    ProcessEngineConfigurationImpl configuration = Context.getProcessEngineConfiguration();

    HistoryLevel historyLevel = configuration.getHistoryLevel();
    if(historyLevel.isHistoryEventProduced(HistoryEventTypes.TASK_INSTANCE_CREATE, task)) {

      HistoryEventProcessor.processHistoryEvents(new HistoryEventProcessor.HistoryEventCreator() {
        @Override
        public HistoryEvent createHistoryEvent(HistoryEventProducer producer) {
          return producer.createTaskInstanceCreateEvt(task);
        }
      });

    }
  }

  protected void configureQuery(final HistoricTaskInstanceQueryImpl query) {
    getAuthorizationManager().configureHistoricTaskInstanceQuery(query);
    getTenantManager().configureQuery(query);
  }

  public DbOperation deleteHistoricTaskInstancesByRemovalTime(Date removalTime, int minuteFrom, int minuteTo, int batchSize) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(REMOVAL_TIME, removalTime);
    if (minuteTo - minuteFrom + 1 < 60) {
      parameters.put(MINUTE_FROM, minuteFrom);
      parameters.put(MINUTE_TO, minuteTo);
    }
    parameters.put(BATCH_SIZE, batchSize);

    return getDbEntityManager()
      .deletePreserveOrder(HistoricTaskInstanceEntity.class, "deleteHistoricTaskInstancesByRemovalTime",
        new ListQueryParameterObject(parameters, 0, batchSize));
  }

}
