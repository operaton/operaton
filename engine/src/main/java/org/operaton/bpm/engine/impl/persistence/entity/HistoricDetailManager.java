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
package org.operaton.bpm.engine.impl.persistence.entity;

import org.operaton.bpm.engine.history.HistoricDetail;
import org.operaton.bpm.engine.impl.HistoricDetailQueryImpl;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.db.ListQueryParameterObject;
import org.operaton.bpm.engine.impl.db.entitymanager.operation.DbOperation;
import org.operaton.bpm.engine.impl.history.event.HistoricDetailEventEntity;
import org.operaton.bpm.engine.impl.persistence.AbstractHistoricManager;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Tom Baeyens
 */
public class HistoricDetailManager extends AbstractHistoricManager {

  private static final String VARIABLE_INSTANCE_ID = "variableInstanceId";

  public void deleteHistoricDetailsByProcessInstanceIds(List<String> historicProcessInstanceIds) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(PROCESS_INSTANCE_IDS, historicProcessInstanceIds);
    deleteHistoricDetails(parameters);
  }

  public void deleteHistoricDetailsByTaskProcessInstanceIds(List<String> historicProcessInstanceIds) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(TASK_PROCESS_INSTANCE_IDS, historicProcessInstanceIds);
    deleteHistoricDetails(parameters);
  }

  public void deleteHistoricDetailsByCaseInstanceIds(List<String> historicCaseInstanceIds) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(CASE_INSTANCE_IDS, historicCaseInstanceIds);
    deleteHistoricDetails(parameters);
  }

  public void deleteHistoricDetailsByTaskCaseInstanceIds(List<String> historicCaseInstanceIds) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(TASK_CASE_INSTANCE_IDS, historicCaseInstanceIds);
    deleteHistoricDetails(parameters);
  }

  public void deleteHistoricDetailsByVariableInstanceId(String historicVariableInstanceId) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(VARIABLE_INSTANCE_ID, historicVariableInstanceId);
    deleteHistoricDetails(parameters);
  }

  public void deleteHistoricDetails(Map<String, Object> parameters) {
    getDbEntityManager().deletePreserveOrder(ByteArrayEntity.class, "deleteHistoricDetailByteArraysByIds", parameters);
    getDbEntityManager().deletePreserveOrder(HistoricDetailEventEntity.class, "deleteHistoricDetailsByIds", parameters);
  }


  public long findHistoricDetailCountByQueryCriteria(HistoricDetailQueryImpl historicVariableUpdateQuery) {
    configureQuery(historicVariableUpdateQuery);
    return (Long) getDbEntityManager().selectOne("selectHistoricDetailCountByQueryCriteria", historicVariableUpdateQuery);
  }

  @SuppressWarnings("unchecked")
  public List<HistoricDetail> findHistoricDetailsByQueryCriteria(HistoricDetailQueryImpl historicVariableUpdateQuery, Page page) {
    configureQuery(historicVariableUpdateQuery);
    return getDbEntityManager().selectList("selectHistoricDetailsByQueryCriteria", historicVariableUpdateQuery, page);
  }

  public void deleteHistoricDetailsByTaskId(String taskId) {
    if (isHistoryEnabled()) {
      // delete entries in DB
      List<HistoricDetail> historicDetails = findHistoricDetailsByTaskId(taskId);

      for (HistoricDetail historicDetail : historicDetails) {
        ((HistoricDetailEventEntity) historicDetail).delete();
      }

      //delete entries in Cache
      List<HistoricDetailEventEntity> cachedHistoricDetails = getDbEntityManager().getCachedEntitiesByType(HistoricDetailEventEntity.class);
      for (HistoricDetailEventEntity historicDetail : cachedHistoricDetails) {
        // make sure we only delete the right ones (as we cannot make a proper query in the cache)
        if (taskId.equals(historicDetail.getTaskId())) {
          historicDetail.delete();
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  public List<HistoricDetail> findHistoricDetailsByTaskId(String taskId) {
    return getDbEntityManager().selectList("selectHistoricDetailsByTaskId", taskId);
  }

  protected void configureQuery(HistoricDetailQueryImpl query) {
    getAuthorizationManager().configureHistoricDetailQuery(query);
    getTenantManager().configureQuery(query);
  }

  public DbOperation addRemovalTimeToDetailsByRootProcessInstanceId(String rootProcessInstanceId, Date removalTime, Integer batchSize) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(ROOT_PROCESS_INSTANCE_ID, rootProcessInstanceId);
    parameters.put(REMOVAL_TIME, removalTime);
    parameters.put(MAX_RESULTS, batchSize);

    return getDbEntityManager()
      .updatePreserveOrder(HistoricDetailEventEntity.class, "updateHistoricDetailsByRootProcessInstanceId", parameters);
  }

  public DbOperation addRemovalTimeToDetailsByProcessInstanceId(String processInstanceId, Date removalTime, Integer batchSize) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(PROCESS_INSTANCE_ID, processInstanceId);
    parameters.put(REMOVAL_TIME, removalTime);
    parameters.put(MAX_RESULTS, batchSize);

    return getDbEntityManager()
      .updatePreserveOrder(HistoricDetailEventEntity.class, "updateHistoricDetailsByProcessInstanceId", parameters);
  }

  public DbOperation deleteHistoricDetailsByRemovalTime(Date removalTime, int minuteFrom, int minuteTo, int batchSize) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(REMOVAL_TIME, removalTime);
    if (minuteTo - minuteFrom + 1 < 60) {
      parameters.put(MINUTE_FROM, minuteFrom);
      parameters.put(MINUTE_TO, minuteTo);
    }
    parameters.put(BATCH_SIZE, batchSize);

    return getDbEntityManager()
      .deletePreserveOrder(HistoricDetailEventEntity.class, "deleteHistoricDetailsByRemovalTime",
        new ListQueryParameterObject(parameters, 0, batchSize));
  }

}
