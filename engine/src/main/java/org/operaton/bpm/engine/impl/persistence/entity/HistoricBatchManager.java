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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.history.CleanableHistoricBatchReportResult;
import org.operaton.bpm.engine.impl.CleanableHistoricBatchReportImpl;
import org.operaton.bpm.engine.impl.Direction;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.QueryOrderingProperty;
import org.operaton.bpm.engine.impl.QueryPropertyImpl;
import org.operaton.bpm.engine.impl.batch.BatchEntity;
import org.operaton.bpm.engine.impl.batch.history.HistoricBatchEntity;
import org.operaton.bpm.engine.impl.batch.history.HistoricBatchQueryImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.ListQueryParameterObject;
import org.operaton.bpm.engine.impl.db.entitymanager.operation.DbOperation;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.history.event.HistoryEvent;
import org.operaton.bpm.engine.impl.history.event.HistoryEventProcessor;
import org.operaton.bpm.engine.impl.history.event.HistoryEventTypes;
import org.operaton.bpm.engine.impl.history.producer.HistoryEventProducer;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.AbstractManager;
import org.operaton.bpm.engine.impl.util.ClockUtil;

public class HistoricBatchManager extends AbstractManager {
  private static final String MAP = "map";

  public long findBatchCountByQueryCriteria(HistoricBatchQueryImpl historicBatchQuery) {
    configureQuery(historicBatchQuery);
    return (Long) getDbEntityManager().selectOne("selectHistoricBatchCountByQueryCriteria", historicBatchQuery);
  }

  @SuppressWarnings("unchecked")
  public List<HistoricBatch> findBatchesByQueryCriteria(HistoricBatchQueryImpl historicBatchQuery, Page page) {
    configureQuery(historicBatchQuery);
    return getDbEntityManager().selectList("selectHistoricBatchesByQueryCriteria", historicBatchQuery, page);
  }

  public HistoricBatchEntity findHistoricBatchById(String batchId) {
    return getDbEntityManager().selectById(HistoricBatchEntity.class, batchId);
  }

  public HistoricBatchEntity findHistoricBatchByJobId(String jobId) {
    return (HistoricBatchEntity) getDbEntityManager().selectOne("selectHistoricBatchByJobId", jobId);
  }

  @SuppressWarnings("unchecked")
  public List<String> findHistoricBatchIdsForCleanup(Integer batchSize, Map<String, Integer> batchOperationsForHistoryCleanup, int minuteFrom, int minuteTo) {
    Map<String, Object> queryParameters = new HashMap<>();
    queryParameters.put(CURRENT_TIMESTAMP, ClockUtil.getCurrentTime());
    queryParameters.put(MAP, batchOperationsForHistoryCleanup);
    if (minuteTo - minuteFrom + 1 < 60) {
      queryParameters.put(MINUTE_FROM, minuteFrom);
      queryParameters.put(MINUTE_TO, minuteTo);
    }
    ListQueryParameterObject parameterObject = new ListQueryParameterObject(queryParameters, 0, batchSize);
    parameterObject.getOrderingProperties().add(new QueryOrderingProperty(new QueryPropertyImpl("END_TIME_"), Direction.ASCENDING));

    return getDbEntityManager().selectList("selectHistoricBatchIdsForCleanup", parameterObject);
  }

  public void deleteHistoricBatchById(String id) {
    getDbEntityManager().delete(HistoricBatchEntity.class, "deleteHistoricBatchById", id);
  }

  public void deleteHistoricBatchesByIds(List<String> historicBatchIds) {
    CommandContext commandContext = Context.getCommandContext();

    commandContext.getHistoricIncidentManager().deleteHistoricIncidentsByBatchId(historicBatchIds);
    commandContext.getHistoricJobLogManager().deleteHistoricJobLogByBatchIds(historicBatchIds);

    getDbEntityManager().deletePreserveOrder(HistoricBatchEntity.class, "deleteHistoricBatchByIds", historicBatchIds);

  }

  public void createHistoricBatch(final BatchEntity batch) {
    ProcessEngineConfigurationImpl configuration = Context.getProcessEngineConfiguration();

    HistoryLevel historyLevel = configuration.getHistoryLevel();
    if(historyLevel.isHistoryEventProduced(HistoryEventTypes.BATCH_START, batch)) {

      HistoryEventProcessor.processHistoryEvents(new HistoryEventProcessor.HistoryEventCreator() {
        @Override
        public HistoryEvent createHistoryEvent(HistoryEventProducer producer) {
          return producer.createBatchStartEvent(batch);
        }
      });
    }
  }

  public void completeHistoricBatch(final BatchEntity batch) {
    ProcessEngineConfigurationImpl configuration = Context.getProcessEngineConfiguration();

    HistoryLevel historyLevel = configuration.getHistoryLevel();
    if(historyLevel.isHistoryEventProduced(HistoryEventTypes.BATCH_END, batch)) {

      HistoryEventProcessor.processHistoryEvents(new HistoryEventProcessor.HistoryEventCreator() {
        @Override
        public HistoryEvent createHistoryEvent(HistoryEventProducer producer) {
          return producer.createBatchEndEvent(batch);
        }
      });
    }
  }

  public void updateHistoricBatch(final BatchEntity batch) {
    ProcessEngineConfigurationImpl configuration = Context.getProcessEngineConfiguration();

    HistoryLevel historyLevel = configuration.getHistoryLevel();
    if (historyLevel.isHistoryEventProduced(HistoryEventTypes.BATCH_UPDATE, batch)) {

      HistoryEventProcessor.processHistoryEvents(new HistoryEventProcessor.HistoryEventCreator() {
        @Override
        public HistoryEvent createHistoryEvent(HistoryEventProducer producer) {
          return producer.createBatchUpdateEvent(batch);
        }
      });
    }
  }

  protected void configureQuery(HistoricBatchQueryImpl query) {
    getAuthorizationManager().configureHistoricBatchQuery(query);
    getTenantManager().configureQuery(query);
  }

  @SuppressWarnings("unchecked")
  public List<CleanableHistoricBatchReportResult> findCleanableHistoricBatchesReportByCriteria(CleanableHistoricBatchReportImpl query, Page page, Map<String, Integer> batchOperationsForHistoryCleanup) {
    query.setCurrentTimestamp(ClockUtil.getCurrentTime());
    query.setParameter(batchOperationsForHistoryCleanup);
    query.getOrderingProperties().add(new QueryOrderingProperty(new QueryPropertyImpl("TYPE_"), Direction.ASCENDING));
    if (batchOperationsForHistoryCleanup.isEmpty()) {
      return getDbEntityManager().selectList("selectOnlyFinishedBatchesReportEntities", query, page);
    } else {
      return getDbEntityManager().selectList("selectFinishedBatchesReportEntities", query, page);
    }
  }

  public long findCleanableHistoricBatchesReportCountByCriteria(CleanableHistoricBatchReportImpl query, Map<String, Integer> batchOperationsForHistoryCleanup) {
    query.setCurrentTimestamp(ClockUtil.getCurrentTime());
    query.setParameter(batchOperationsForHistoryCleanup);
    if (batchOperationsForHistoryCleanup.isEmpty()) {
      return (Long) getDbEntityManager().selectOne("selectOnlyFinishedBatchesReportEntitiesCount", query);
    } else {
      return (Long) getDbEntityManager().selectOne("selectFinishedBatchesReportEntitiesCount", query);
    }
  }

  public DbOperation deleteHistoricBatchesByRemovalTime(Date removalTime, int minuteFrom, int minuteTo, int batchSize) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(REMOVAL_TIME, removalTime);
    if (minuteTo - minuteFrom + 1 < 60) {
      parameters.put(MINUTE_FROM, minuteFrom);
      parameters.put(MINUTE_TO, minuteTo);
    }
    parameters.put(BATCH_SIZE, batchSize);

    return getDbEntityManager()
      .deletePreserveOrder(HistoricBatchEntity.class, "deleteHistoricBatchesByRemovalTime",
        new ListQueryParameterObject(parameters, 0, batchSize));
  }

  public void addRemovalTimeById(String id, Date removalTime) {
    CommandContext commandContext = Context.getCommandContext();

    commandContext.getHistoricIncidentManager()
      .addRemovalTimeToHistoricIncidentsByBatchId(id, removalTime);

    commandContext.getHistoricJobLogManager()
      .addRemovalTimeToJobLogByBatchId(id, removalTime);

    Map<String, Object> parameters = new HashMap<>();
    parameters.put(ID, id);
    parameters.put(REMOVAL_TIME, removalTime);

    getDbEntityManager()
      .updatePreserveOrder(HistoricBatchEntity.class, "updateHistoricBatchRemovalTimeById", parameters);
  }

}
