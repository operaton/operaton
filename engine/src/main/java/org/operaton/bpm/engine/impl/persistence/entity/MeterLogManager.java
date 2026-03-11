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

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.operaton.bpm.engine.impl.Direction;
import org.operaton.bpm.engine.impl.QueryOrderingProperty;
import org.operaton.bpm.engine.impl.QueryPropertyImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.ListQueryParameterObject;
import org.operaton.bpm.engine.impl.db.entitymanager.DbEntityManager;
import org.operaton.bpm.engine.impl.db.entitymanager.operation.DbOperation;
import org.operaton.bpm.engine.impl.metrics.Meter;
import org.operaton.bpm.engine.impl.metrics.MetricsQueryImpl;
import org.operaton.bpm.engine.impl.persistence.AbstractManager;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.management.MetricIntervalValue;

import static java.util.Objects.requireNonNull;

/**
 * @author Daniel Meyer
 *
 */
public class MeterLogManager extends AbstractManager {

  public static final String SELECT_METER_INTERVAL = "selectMeterLogAggregatedByTimeInterval";
  public static final String SELECT_METER_SUM = "selectMeterLogSum";
  public static final String DELETE_ALL_METER = "deleteAllMeterLogEntries";
  public static final String DELETE_ALL_METER_BY_TIMESTAMP_AND_REPORTER = "deleteMeterLogEntriesByTimestampAndReporter";

  public static final String SELECT_UNIQUE_TASK_WORKER = "selectUniqueTaskWorkerCount";
  public static final String SELECT_TASK_METER_FOR_CLEANUP = "selectTaskMetricIdsForCleanup";
  public static final String DELETE_TASK_METER_BY_TIMESTAMP = "deleteTaskMeterLogEntriesByTimestamp";
  public static final String DELETE_TASK_METER_BY_REMOVAL_TIME = "deleteTaskMetricsByRemovalTime";
  public static final String DELETE_TASK_METER_BY_IDS = "deleteTaskMeterLogEntriesByIds";
  private static final String START_TIME = "startTime";
  private static final String END_TIME = "endTime";
  private static final String REPORTER = "reporter";
  private static final String MILLISECONDS = "milliseconds";

  public void insert(MeterLogEntity meterLogEntity) {
    getDbEntityManager()
     .insert(meterLogEntity);
  }

  public Long executeSelectSum(MetricsQueryImpl query) {
    Long result = (Long) getDbEntityManager().selectOne(SELECT_METER_SUM, query);
    result = result != null ? result : 0;

    if(shouldAddCurrentUnloggedCount(query)) {
      // add current unlogged count
      Meter meter = Context.getProcessEngineConfiguration()
        .getMetricsRegistry()
        .getDbMeterByName(query.getName());
      if(meter != null) {
        result += meter.get();
      }
    }

    return result;
  }

  public List<MetricIntervalValue> executeSelectInterval(MetricsQueryImpl query) {
    DbEntityManager dbEntityManager = requireNonNull(getDbEntityManager());
    ProcessEngineConfigurationImpl processEngineConfiguration = requireNonNull(Context.getProcessEngineConfiguration());
    List<MetricIntervalValue> intervalResult = dbEntityManager.selectList(SELECT_METER_INTERVAL, query);

    intervalResult = intervalResult != null ? intervalResult : new ArrayList<>();
    String reporterId = processEngineConfiguration.getDbMetricsReporter().getMetricsCollectionTask().getReporter();
    if (intervalResult.isEmpty() || !isEndTimeAfterLastReportInterval(query) || reporterId == null) {
      return intervalResult;
    }

    Map<String, Meter> metrics = processEngineConfiguration.getMetricsRegistry().getDbMeters();
    String queryName = query.getName();
    //we have to add all unlogged metrics to last interval
    if (queryName != null) {
      MetricIntervalEntity intervalEntity = (MetricIntervalEntity) intervalResult.get(0);
      long entityValue = intervalEntity.getValue();
      if (metrics.get(queryName) != null) {
        entityValue += metrics.get(queryName).get();
      }
      intervalEntity.setValue(entityValue);
    } else {
      Set<String> metricNames = metrics.keySet();
      Date lastIntervalTimestamp = intervalResult.get(0).getTimestamp();
      for (String metricName : metricNames) {
        MetricIntervalEntity entity = new MetricIntervalEntity(lastIntervalTimestamp, metricName, reporterId);
        int idx = intervalResult.indexOf(entity);
        if (idx >= 0) {
          MetricIntervalEntity intervalValue = (MetricIntervalEntity) intervalResult.get(idx);
          intervalValue.setValue(intervalValue.getValue() + metrics.get(metricName).get());
        }
      }
    }
    return intervalResult;
  }

  protected boolean isEndTimeAfterLastReportInterval(MetricsQueryImpl query) {
    long reportingIntervalInSeconds = Context.getProcessEngineConfiguration()
      .getDbMetricsReporter()
      .getReportingIntervalInSeconds();

    return query.getEndDate() == null
        || query.getEndDateMilliseconds()>= ClockUtil.getCurrentTime().getTime() - (1000 * reportingIntervalInSeconds);
  }

  protected boolean shouldAddCurrentUnloggedCount(MetricsQueryImpl query) {
    return query.getName() != null
        && isEndTimeAfterLastReportInterval(query);

  }

  public void deleteAll() {
    getDbEntityManager().delete(MeterLogEntity.class, DELETE_ALL_METER, null);
  }

  public void deleteByTimestampAndReporter(Date timestamp, String reporter) {
    Map<String, Object> parameters = new HashMap<>();
    if (timestamp != null) {
      parameters.put(MILLISECONDS, timestamp.getTime());
    }
    parameters.put(REPORTER, reporter);
    getDbEntityManager().delete(MeterLogEntity.class, DELETE_ALL_METER_BY_TIMESTAMP_AND_REPORTER, parameters);
  }

  // TASK METER LOG

  public long findUniqueTaskWorkerCount(Date startTime, Date endTime) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(START_TIME, startTime);
    parameters.put(END_TIME, endTime);

    return (Long) getDbEntityManager().selectOne(SELECT_UNIQUE_TASK_WORKER, parameters);
  }

  public void deleteTaskMetricsByTimestamp(Date timestamp) {
    Map<String, Object> parameters = Collections.singletonMap("timestamp", timestamp);
    getDbEntityManager().delete(TaskMeterLogEntity.class, DELETE_TASK_METER_BY_TIMESTAMP, parameters);
  }

  public void deleteTaskMetricsById(List<String> taskMetricIds) {
    getDbEntityManager().deletePreserveOrder(TaskMeterLogEntity.class, DELETE_TASK_METER_BY_IDS, taskMetricIds);
  }

  public DbOperation deleteTaskMetricsByRemovalTime(Date currentTimestamp, Integer timeToLive, int minuteFrom, int minuteTo, int batchSize) {
    Map<String, Object> parameters = new HashMap<>();
    // data inserted prior to now minus timeToLive-days can be removed
    Date removalTime = Date.from(currentTimestamp.toInstant().minus(timeToLive, ChronoUnit.DAYS));
    parameters.put(REMOVAL_TIME, removalTime);
    if (minuteTo - minuteFrom + 1 < 60) {
      parameters.put(MINUTE_FROM, minuteFrom);
      parameters.put(MINUTE_TO, minuteTo);
    }
    parameters.put(BATCH_SIZE, batchSize);

    return getDbEntityManager()
      .deletePreserveOrder(TaskMeterLogEntity.class, DELETE_TASK_METER_BY_REMOVAL_TIME,
        new ListQueryParameterObject(parameters, 0, batchSize));
  }

  @SuppressWarnings("unchecked")
  public List<String> findTaskMetricsForCleanup(int batchSize, Integer timeToLive, int minuteFrom, int minuteTo) {
    Map<String, Object> queryParameters = new HashMap<>();
    queryParameters.put(CURRENT_TIMESTAMP, ClockUtil.getCurrentTime());
    queryParameters.put("timeToLive", timeToLive);
    if (minuteTo - minuteFrom + 1 < 60) {
      queryParameters.put(MINUTE_FROM, minuteFrom);
      queryParameters.put(MINUTE_TO, minuteTo);
    }
    ListQueryParameterObject parameterObject = new ListQueryParameterObject(queryParameters, 0, batchSize);
    parameterObject.getOrderingProperties().add(new QueryOrderingProperty(new QueryPropertyImpl("TIMESTAMP_"), Direction.ASCENDING));

    return getDbEntityManager().selectList(SELECT_TASK_METER_FOR_CLEANUP, parameterObject);
  }

}
