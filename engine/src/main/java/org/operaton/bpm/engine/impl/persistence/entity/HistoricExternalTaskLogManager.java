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

import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.history.HistoricExternalTaskLog;
import org.operaton.bpm.engine.impl.HistoricExternalTaskLogQueryImpl;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.ListQueryParameterObject;
import org.operaton.bpm.engine.impl.db.entitymanager.operation.DbOperation;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.history.event.HistoricExternalTaskLogEntity;
import org.operaton.bpm.engine.impl.history.event.HistoryEvent;
import org.operaton.bpm.engine.impl.history.event.HistoryEventProcessor;
import org.operaton.bpm.engine.impl.history.event.HistoryEventType;
import org.operaton.bpm.engine.impl.history.event.HistoryEventTypes;
import org.operaton.bpm.engine.impl.history.producer.HistoryEventProducer;
import org.operaton.bpm.engine.impl.persistence.AbstractManager;
import org.operaton.bpm.engine.impl.util.EnsureUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoricExternalTaskLogManager extends AbstractManager {

  // select /////////////////////////////////////////////////////////////////

  public HistoricExternalTaskLogEntity findHistoricExternalTaskLogById(String HistoricExternalTaskLogId) {
    return (HistoricExternalTaskLogEntity) getDbEntityManager().selectOne("selectHistoricExternalTaskLog", HistoricExternalTaskLogId);
  }

  @SuppressWarnings("unchecked")
  public List<HistoricExternalTaskLog> findHistoricExternalTaskLogsByQueryCriteria(HistoricExternalTaskLogQueryImpl query, Page page) {
    configureQuery(query);
    return getDbEntityManager().selectList("selectHistoricExternalTaskLogByQueryCriteria", query, page);
  }

  public long findHistoricExternalTaskLogsCountByQueryCriteria(HistoricExternalTaskLogQueryImpl query) {
    configureQuery(query);
    return (Long) getDbEntityManager().selectOne("selectHistoricExternalTaskLogCountByQueryCriteria", query);
  }

  // update ///////////////////////////////////////////////////////////////////

  public DbOperation addRemovalTimeToExternalTaskLogByRootProcessInstanceId(String rootProcessInstanceId, Date removalTime, Integer batchSize) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(ROOT_PROCESS_INSTANCE_ID, rootProcessInstanceId);
    parameters.put(REMOVAL_TIME, removalTime);
    parameters.put(MAX_RESULTS, batchSize);

    return getDbEntityManager()
      .updatePreserveOrder(HistoricExternalTaskLogEntity.class, "updateExternalTaskLogByRootProcessInstanceId", parameters);
  }

  public DbOperation addRemovalTimeToExternalTaskLogByProcessInstanceId(String processInstanceId, Date removalTime, Integer batchSize) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(PROCESS_INSTANCE_ID, processInstanceId);
    parameters.put(REMOVAL_TIME, removalTime);
    parameters.put(MAX_RESULTS, batchSize);

    return getDbEntityManager()
      .updatePreserveOrder(HistoricExternalTaskLogEntity.class, "updateExternalTaskLogByProcessInstanceId", parameters);
  }

  // delete ///////////////////////////////////////////////////////////////////

  public void deleteHistoricExternalTaskLogsByProcessInstanceIds(List<String> processInstanceIds) {
    deleteExceptionByteArrayByParameterMap("processInstanceIdIn", processInstanceIds.toArray());
    getDbEntityManager().deletePreserveOrder(HistoricExternalTaskLogEntity.class, "deleteHistoricExternalTaskLogByProcessInstanceIds", processInstanceIds);
  }

  public DbOperation deleteExternalTaskLogByRemovalTime(Date removalTime, int minuteFrom, int minuteTo, int batchSize) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(REMOVAL_TIME, removalTime);
    if (minuteTo - minuteFrom + 1 < 60) {
      parameters.put(MINUTE_FROM, minuteFrom);
      parameters.put(MINUTE_TO, minuteTo);
    }
    parameters.put(BATCH_SIZE, batchSize);

    return getDbEntityManager()
      .deletePreserveOrder(HistoricExternalTaskLogEntity.class, "deleteExternalTaskLogByRemovalTime",
        new ListQueryParameterObject(parameters, 0, batchSize));
  }

  // byte array delete ////////////////////////////////////////////////////////

  protected void deleteExceptionByteArrayByParameterMap(String key, Object value) {
    EnsureUtil.ensureNotNull(key, value);
    Map<String, Object> parameterMap = new HashMap<>();
    parameterMap.put(key, value);
    getDbEntityManager().delete(ByteArrayEntity.class, "deleteErrorDetailsByteArraysByIds", parameterMap);
  }

  // fire history events ///////////////////////////////////////////////////////

  public void fireExternalTaskCreatedEvent(final ExternalTask externalTask) {
    if (isHistoryEventProduced(HistoryEventTypes.EXTERNAL_TASK_CREATE, externalTask)) {
      HistoryEventProcessor.processHistoryEvents(new HistoryEventProcessor.HistoryEventCreator() {
        @Override
        public HistoryEvent createHistoryEvent(HistoryEventProducer producer) {
          return producer.createHistoricExternalTaskLogCreatedEvt(externalTask);
        }
      });
    }
  }

  public void fireExternalTaskFailedEvent(final ExternalTask externalTask) {
    if (isHistoryEventProduced(HistoryEventTypes.EXTERNAL_TASK_FAIL, externalTask)) {
      HistoryEventProcessor.processHistoryEvents(new HistoryEventProcessor.HistoryEventCreator() {
        @Override
        public HistoryEvent createHistoryEvent(HistoryEventProducer producer) {
          return producer.createHistoricExternalTaskLogFailedEvt(externalTask);
        }

        @Override
        public void postHandleSingleHistoryEventCreated(HistoryEvent event) {
          ((ExternalTaskEntity) externalTask).setLastFailureLogId(event.getId());
        }
      });
    }
  }

  public void fireExternalTaskSuccessfulEvent(final ExternalTask externalTask) {
    if (isHistoryEventProduced(HistoryEventTypes.EXTERNAL_TASK_SUCCESS, externalTask)) {
      HistoryEventProcessor.processHistoryEvents(new HistoryEventProcessor.HistoryEventCreator() {
        @Override
        public HistoryEvent createHistoryEvent(HistoryEventProducer producer) {
          return producer.createHistoricExternalTaskLogSuccessfulEvt(externalTask);
        }
      });
    }
  }

  public void fireExternalTaskDeletedEvent(final ExternalTask externalTask) {
    if (isHistoryEventProduced(HistoryEventTypes.EXTERNAL_TASK_DELETE, externalTask)) {
      HistoryEventProcessor.processHistoryEvents(new HistoryEventProcessor.HistoryEventCreator() {
        @Override
        public HistoryEvent createHistoryEvent(HistoryEventProducer producer) {
          return producer.createHistoricExternalTaskLogDeletedEvt(externalTask);
        }
      });
    }
  }

  // helper /////////////////////////////////////////////////////////

  protected boolean isHistoryEventProduced(HistoryEventType eventType, ExternalTask externalTask) {
    ProcessEngineConfigurationImpl configuration = Context.getProcessEngineConfiguration();
    HistoryLevel historyLevel = configuration.getHistoryLevel();
    return historyLevel.isHistoryEventProduced(eventType, externalTask);
  }

  protected void configureQuery(HistoricExternalTaskLogQueryImpl query) {
    getAuthorizationManager().configureHistoricExternalTaskLogQuery(query);
    getTenantManager().configureQuery(query);
  }
}
