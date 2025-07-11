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

import org.operaton.bpm.engine.history.HistoricIdentityLinkLog;
import org.operaton.bpm.engine.impl.HistoricIdentityLinkLogQueryImpl;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.ListQueryParameterObject;
import org.operaton.bpm.engine.impl.db.entitymanager.operation.DbOperation;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.history.event.HistoricIdentityLinkLogEventEntity;
import org.operaton.bpm.engine.impl.history.event.HistoryEventTypes;
import org.operaton.bpm.engine.impl.persistence.AbstractHistoricManager;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Deivarayan Azhagappan
 *
 */
public class HistoricIdentityLinkLogManager extends AbstractHistoricManager {

  public long findHistoricIdentityLinkLogCountByQueryCriteria(HistoricIdentityLinkLogQueryImpl query) {
    configureQuery(query);
    return (Long) getDbEntityManager().selectOne("selectHistoricIdentityLinkCountByQueryCriteria", query);
  }

  @SuppressWarnings("unchecked")
  public List<HistoricIdentityLinkLog> findHistoricIdentityLinkLogByQueryCriteria(HistoricIdentityLinkLogQueryImpl query, Page page) {
    configureQuery(query);
    return getDbEntityManager().selectList("selectHistoricIdentityLinkByQueryCriteria", query, page);
  }

  public DbOperation addRemovalTimeToIdentityLinkLogByRootProcessInstanceId(String rootProcessInstanceId, Date removalTime, Integer batchSize) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(ROOT_PROCESS_INSTANCE_ID, rootProcessInstanceId);
    parameters.put(REMOVAL_TIME, removalTime);
    parameters.put(MAX_RESULTS, batchSize);

    return getDbEntityManager()
      .updatePreserveOrder(HistoricIdentityLinkLogEventEntity.class, "updateIdentityLinkLogByRootProcessInstanceId", parameters);
  }

  public DbOperation addRemovalTimeToIdentityLinkLogByProcessInstanceId(String processInstanceId, Date removalTime, Integer batchSize) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(PROCESS_INSTANCE_ID, processInstanceId);
    parameters.put(REMOVAL_TIME, removalTime);
    parameters.put(MAX_RESULTS, batchSize);

    return getDbEntityManager()
      .updatePreserveOrder(HistoricIdentityLinkLogEventEntity.class, "updateIdentityLinkLogByProcessInstanceId", parameters);
  }

  public void deleteHistoricIdentityLinksLogByProcessDefinitionId(String processDefId) {
    if (isHistoryEventProduced()) {
      getDbEntityManager().delete(HistoricIdentityLinkLogEntity.class, "deleteHistoricIdentityLinksByProcessDefinitionId", processDefId);
    }
  }

  public void deleteHistoricIdentityLinksLogByTaskId(String taskId) {
    if (isHistoryEventProduced()) {
      getDbEntityManager().delete(HistoricIdentityLinkLogEntity.class, "deleteHistoricIdentityLinksByTaskId", taskId);
    }
  }

  public void deleteHistoricIdentityLinksLogByTaskProcessInstanceIds(List<String> processInstanceIds) {
    getDbEntityManager().deletePreserveOrder(HistoricIdentityLinkLogEntity.class, "deleteHistoricIdentityLinksByTaskProcessInstanceIds", processInstanceIds);
  }

  public void deleteHistoricIdentityLinksLogByTaskCaseInstanceIds(List<String> caseInstanceIds) {
    getDbEntityManager().deletePreserveOrder(HistoricIdentityLinkLogEntity.class, "deleteHistoricIdentityLinksByTaskCaseInstanceIds", caseInstanceIds);
  }

  public DbOperation deleteHistoricIdentityLinkLogByRemovalTime(Date removalTime, int minuteFrom, int minuteTo, int batchSize) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(REMOVAL_TIME, removalTime);
    if (minuteTo - minuteFrom + 1 < 60) {
      parameters.put(MINUTE_FROM, minuteFrom);
      parameters.put(MINUTE_TO, minuteTo);
    }
    parameters.put(BATCH_SIZE, batchSize);

    return getDbEntityManager()
      .deletePreserveOrder(HistoricIdentityLinkLogEntity.class, "deleteHistoricIdentityLinkLogByRemovalTime",
        new ListQueryParameterObject(parameters, 0, batchSize));
  }

  protected void configureQuery(HistoricIdentityLinkLogQueryImpl query) {
    getAuthorizationManager().configureHistoricIdentityLinkQuery(query);
    getTenantManager().configureQuery(query);
  }

  protected boolean isHistoryEventProduced() {
    HistoryLevel historyLevel = Context.getProcessEngineConfiguration().getHistoryLevel();
    return historyLevel.isHistoryEventProduced(HistoryEventTypes.IDENTITY_LINK_ADD, null) ||
           historyLevel.isHistoryEventProduced(HistoryEventTypes.IDENTITY_LINK_DELETE, null);
  }

}
