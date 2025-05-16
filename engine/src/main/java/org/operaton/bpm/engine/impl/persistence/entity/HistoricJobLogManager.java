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

import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.impl.HistoricJobLogQueryImpl;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.ListQueryParameterObject;
import org.operaton.bpm.engine.impl.db.entitymanager.operation.DbOperation;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.history.event.HistoryEvent;
import org.operaton.bpm.engine.impl.history.event.HistoryEventProcessor;
import org.operaton.bpm.engine.impl.history.event.HistoryEventType;
import org.operaton.bpm.engine.impl.history.event.HistoryEventTypes;
import org.operaton.bpm.engine.impl.history.producer.HistoryEventProducer;
import org.operaton.bpm.engine.impl.persistence.AbstractHistoricManager;
import org.operaton.bpm.engine.impl.util.EnsureUtil;
import org.operaton.bpm.engine.runtime.Job;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Roman Smirnov
 *
 */
public class HistoricJobLogManager extends AbstractHistoricManager {

  // select /////////////////////////////////////////////////////////////////

  public HistoricJobLogEventEntity findHistoricJobLogById(String historicJobLogId) {
    return (HistoricJobLogEventEntity) getDbEntityManager().selectOne("selectHistoricJobLog", historicJobLogId);
  }

  @SuppressWarnings("unchecked")
  public List<HistoricJobLog> findHistoricJobLogsByDeploymentId(String deploymentId) {
    return getDbEntityManager().selectList("selectHistoricJobLogByDeploymentId", deploymentId);
  }

  @SuppressWarnings("unchecked")
  public List<HistoricJobLog> findHistoricJobLogsByQueryCriteria(HistoricJobLogQueryImpl query, Page page) {
    configureQuery(query);
    return getDbEntityManager().selectList("selectHistoricJobLogByQueryCriteria", query, page);
  }

  public long findHistoricJobLogsCountByQueryCriteria(HistoricJobLogQueryImpl query) {
    configureQuery(query);
    return (Long) getDbEntityManager().selectOne("selectHistoricJobLogCountByQueryCriteria", query);
  }

  // update ///////////////////////////////////////////////////////////////////

  public DbOperation addRemovalTimeToJobLogByRootProcessInstanceId(String rootProcessInstanceId, Date removalTime, Integer batchSize) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(ROOT_PROCESS_INSTANCE_ID, rootProcessInstanceId);
    parameters.put(REMOVAL_TIME, removalTime);
    parameters.put(MAX_RESULTS, batchSize);

    return getDbEntityManager()
      .updatePreserveOrder(HistoricJobLogEventEntity.class, "updateJobLogByRootProcessInstanceId", parameters);
  }

  public DbOperation addRemovalTimeToJobLogByProcessInstanceId(String processInstanceId, Date removalTime, Integer batchSize) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(PROCESS_INSTANCE_ID, processInstanceId);
    parameters.put(REMOVAL_TIME, removalTime);
    parameters.put(MAX_RESULTS, batchSize);

    return getDbEntityManager()
      .updatePreserveOrder(HistoricJobLogEventEntity.class, "updateJobLogByProcessInstanceId", parameters);
  }

  public void addRemovalTimeToJobLogByBatchId(String batchId, Date removalTime) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(BATCH_ID, batchId);
    parameters.put(REMOVAL_TIME, removalTime);

    getDbEntityManager()
      .updatePreserveOrder(HistoricJobLogEventEntity.class, "updateJobLogByBatchId", parameters);

    getDbEntityManager()
      .updatePreserveOrder(ByteArrayEntity.class, "updateByteArraysByBatchId", parameters);
  }

  // delete ///////////////////////////////////////////////////////////////////

  public void deleteHistoricJobLogById(String id) {
    if (isHistoryEnabled()) {
      deleteExceptionByteArrayByParameterMap("id", id);
      getDbEntityManager().delete(HistoricJobLogEventEntity.class, "deleteHistoricJobLogById", id);
    }
  }

  public void deleteHistoricJobLogByJobId(String jobId) {
    if (isHistoryEnabled()) {
      deleteExceptionByteArrayByParameterMap("jobId", jobId);
      getDbEntityManager().delete(HistoricJobLogEventEntity.class, "deleteHistoricJobLogByJobId", jobId);
    }
  }

  public void deleteHistoricJobLogsByProcessInstanceIds(List<String> processInstanceIds) {
    deleteExceptionByteArrayByParameterMap("processInstanceIdIn", processInstanceIds.toArray());
    getDbEntityManager().deletePreserveOrder(HistoricJobLogEventEntity.class, "deleteHistoricJobLogByProcessInstanceIds", processInstanceIds);
  }

  public void deleteHistoricJobLogsByProcessDefinitionId(String processDefinitionId) {
    if (isHistoryEnabled()) {
      deleteExceptionByteArrayByParameterMap("processDefinitionId", processDefinitionId);
      getDbEntityManager().delete(HistoricJobLogEventEntity.class, "deleteHistoricJobLogByProcessDefinitionId", processDefinitionId);
    }
  }

  public void deleteHistoricJobLogsByDeploymentId(String deploymentId) {
    if (isHistoryEnabled()) {
      deleteExceptionByteArrayByParameterMap("deploymentId", deploymentId);
      getDbEntityManager().delete(HistoricJobLogEventEntity.class, "deleteHistoricJobLogByDeploymentId", deploymentId);
    }
  }

  public void deleteHistoricJobLogsByHandlerType(String handlerType) {
    if (isHistoryEnabled()) {
      deleteExceptionByteArrayByParameterMap("handlerType", handlerType);
      getDbEntityManager().delete(HistoricJobLogEventEntity.class, "deleteHistoricJobLogByHandlerType", handlerType);
    }
  }

  public void deleteHistoricJobLogsByJobDefinitionId(String jobDefinitionId) {
    if (isHistoryEnabled()) {
      deleteExceptionByteArrayByParameterMap("jobDefinitionId", jobDefinitionId);
      getDbEntityManager().delete(HistoricJobLogEventEntity.class, "deleteHistoricJobLogByJobDefinitionId", jobDefinitionId);
    }
  }

  public void deleteHistoricJobLogByBatchIds(List<String> historicBatchIds) {
    if (isHistoryEnabled()) {
      deleteExceptionByteArrayByParameterMap("historicBatchIdIn", historicBatchIds);
      getDbEntityManager().delete(HistoricJobLogEventEntity.class, "deleteHistoricJobLogByBatchIds", historicBatchIds);
    }
  }

  public DbOperation deleteJobLogByRemovalTime(Date removalTime, int minuteFrom, int minuteTo, int batchSize) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(REMOVAL_TIME, removalTime);
    if (minuteTo - minuteFrom + 1 < 60) {
      parameters.put(MINUTE_FROM, minuteFrom);
      parameters.put(MINUTE_TO, minuteTo);
    }
    parameters.put(BATCH_SIZE, batchSize);

    return getDbEntityManager()
      .deletePreserveOrder(HistoricJobLogEventEntity.class, "deleteJobLogByRemovalTime",
        new ListQueryParameterObject(parameters, 0, batchSize));
  }

  // byte array delete ////////////////////////////////////////////////////////

  protected void deleteExceptionByteArrayByParameterMap(String key, Object value) {
    EnsureUtil.ensureNotNull(key, value);
    Map<String, Object> parameterMap = new HashMap<>();
    parameterMap.put(key, value);
    getDbEntityManager().delete(ByteArrayEntity.class, "deleteExceptionByteArraysByIds", parameterMap);
  }

  // fire history events ///////////////////////////////////////////////////////

  public void fireJobCreatedEvent(final Job job) {
    if (isHistoryEventProduced(HistoryEventTypes.JOB_CREATE, job)) {
      HistoryEventProcessor.processHistoryEvents(new HistoryEventProcessor.HistoryEventCreator() {
        @Override
        public HistoryEvent createHistoryEvent(HistoryEventProducer producer) {
          return producer.createHistoricJobLogCreateEvt(job);
        }
      });
    }
  }

  public void fireJobFailedEvent(final Job job, final Throwable exception) {
    if (isHistoryEventProduced(HistoryEventTypes.JOB_FAIL, job)) {
      HistoryEventProcessor.processHistoryEvents(new HistoryEventProcessor.HistoryEventCreator() {
        @Override
        public HistoryEvent createHistoryEvent(HistoryEventProducer producer) {
          return producer.createHistoricJobLogFailedEvt(job, exception);
        }

        @Override
        public void postHandleSingleHistoryEventCreated(HistoryEvent event) {
          ((JobEntity) job).setLastFailureLogId(event.getId());
        }
      });
    }
  }

  public void fireJobSuccessfulEvent(final Job job) {
    if (isHistoryEventProduced(HistoryEventTypes.JOB_SUCCESS, job)) {
      HistoryEventProcessor.processHistoryEvents(new HistoryEventProcessor.HistoryEventCreator() {
        @Override
        public HistoryEvent createHistoryEvent(HistoryEventProducer producer) {
          return producer.createHistoricJobLogSuccessfulEvt(job);
        }
      });
    }
  }

  public void fireJobDeletedEvent(final Job job) {
    if (isHistoryEventProduced(HistoryEventTypes.JOB_DELETE, job)) {
      HistoryEventProcessor.processHistoryEvents(new HistoryEventProcessor.HistoryEventCreator() {
        @Override
        public HistoryEvent createHistoryEvent(HistoryEventProducer producer) {
          return producer.createHistoricJobLogDeleteEvt(job);
        }
      });
    }
  }


  // helper /////////////////////////////////////////////////////////

  protected boolean isHistoryEventProduced(HistoryEventType eventType, Job job) {
    ProcessEngineConfigurationImpl configuration = Context.getProcessEngineConfiguration();
    HistoryLevel historyLevel = configuration.getHistoryLevel();
    return historyLevel.isHistoryEventProduced(eventType, job);
  }

  protected void configureQuery(HistoricJobLogQueryImpl query) {
    getAuthorizationManager().configureHistoricJobLogQuery(query);
    getTenantManager().configureQuery(query);
  }

}
