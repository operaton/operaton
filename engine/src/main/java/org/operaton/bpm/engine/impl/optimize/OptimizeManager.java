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
package org.operaton.bpm.engine.impl.optimize;

import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.history.HistoricVariableUpdate;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.HistoricDecisionInstanceQueryImpl;
import org.operaton.bpm.engine.impl.db.CompositePermissionCheck;
import org.operaton.bpm.engine.impl.db.PermissionCheckBuilder;
import org.operaton.bpm.engine.impl.db.sql.DbSqlSessionFactory;
import org.operaton.bpm.engine.impl.persistence.AbstractManager;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricIncidentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.optimize.OptimizeHistoricIdentityLinkLogEntity;
import org.operaton.bpm.engine.impl.util.CollectionUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.READ_HISTORY;
import static org.operaton.bpm.engine.authorization.Resources.DECISION_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.TENANT;

public class OptimizeManager extends AbstractManager {

  private static final String CREATED_AFTER = "createdAfter";
  private static final String CREATED_AT = "createdAt";
  private static final String EVALUATED_AFTER = "evaluatedAfter";
  private static final String EVALUATED_AT = "evaluatedAt";
  private static final String FINISHED_AFTER = "finishedAfter";
  private static final String FINISHED_AT = "finishedAt";
  private static final String OCCURRED_AFTER = "occurredAfter";
  private static final String OCCURRED_AT = "occurredAt";
  private static final String OPERATION_TYPES = "operationTypes";
  private static final String STARTED_AFTER = "startedAfter";
  private static final String STARTED_AT = "startedAt";

  /**
   * Loads the byte arrays into the cache; does currently not return a list
   * because it is not needed by the calling code and we can avoid concatenating
   * lists in the implementation that way.
   */
  public void fetchHistoricVariableUpdateByteArrays(List<String> byteArrayIds) {

    List<List<String>> partitions = CollectionUtil.partition(byteArrayIds, DbSqlSessionFactory.MAXIMUM_NUMBER_PARAMS);

    for (List<String> partition : partitions) {
      getDbEntityManager().selectList("selectByteArrays", partition);
    }
  }

  @SuppressWarnings("unchecked")
  public List<HistoricActivityInstance> getCompletedHistoricActivityInstances(Date finishedAfter,
                                                                              Date finishedAt,
                                                                              int maxResults) {
    checkIsAuthorizedToReadHistoryAndTenants();

    Map<String, Object> params = new HashMap<>();
    params.put(FINISHED_AFTER, finishedAfter);
    params.put(FINISHED_AT, finishedAt);
    params.put(MAX_RESULTS, maxResults);

    return getDbEntityManager().selectList("selectCompletedHistoricActivityPage", params);
  }

  @SuppressWarnings("unchecked")
  public List<HistoricActivityInstance> getRunningHistoricActivityInstances(Date startedAfter,
                                                                            Date startedAt,
                                                                            int maxResults) {
    checkIsAuthorizedToReadHistoryAndTenants();

    Map<String, Object> params = new HashMap<>();
    params.put(STARTED_AFTER, startedAfter);
    params.put(STARTED_AT, startedAt);
    params.put(MAX_RESULTS, maxResults);

    return getDbEntityManager().selectList("selectRunningHistoricActivityPage", params);
  }

  @SuppressWarnings("unchecked")
  public List<HistoricTaskInstance> getCompletedHistoricTaskInstances(Date finishedAfter,
                                                                      Date finishedAt,
                                                                      int maxResults) {
    checkIsAuthorizedToReadHistoryAndTenants();

    Map<String, Object> params = new HashMap<>();
    params.put(FINISHED_AFTER, finishedAfter);
    params.put(FINISHED_AT, finishedAt);
    params.put(MAX_RESULTS, maxResults);

    return getDbEntityManager().selectList("selectCompletedHistoricTaskInstancePage", params);
  }

  @SuppressWarnings("unchecked")
  public List<HistoricTaskInstance> getRunningHistoricTaskInstances(Date startedAfter,
                                                                    Date startedAt,
                                                                    int maxResults) {
    checkIsAuthorizedToReadHistoryAndTenants();

    Map<String, Object> params = new HashMap<>();
    params.put(STARTED_AFTER, startedAfter);
    params.put(STARTED_AT, startedAt);
    params.put(MAX_RESULTS, maxResults);

    return getDbEntityManager().selectList("selectRunningHistoricTaskInstancePage", params);
  }

  @SuppressWarnings("unchecked")
  public List<UserOperationLogEntry> getHistoricUserOperationLogs(Date occurredAfter,
                                                                  Date occurredAt,
                                                                  int maxResults) {
    checkIsAuthorizedToReadHistoryAndTenants();

    String[] operationTypes = new String[]{
      UserOperationLogEntry.OPERATION_TYPE_SUSPEND_JOB,
      UserOperationLogEntry.OPERATION_TYPE_ACTIVATE_JOB,
      UserOperationLogEntry.OPERATION_TYPE_SUSPEND_PROCESS_DEFINITION,
      UserOperationLogEntry.OPERATION_TYPE_ACTIVATE_PROCESS_DEFINITION,
      UserOperationLogEntry.OPERATION_TYPE_SUSPEND,
      UserOperationLogEntry.OPERATION_TYPE_ACTIVATE};
    Map<String, Object> params = new HashMap<>();
    params.put(OCCURRED_AFTER, occurredAfter);
    params.put(OCCURRED_AT, occurredAt);
    params.put(OPERATION_TYPES, operationTypes);
    params.put(MAX_RESULTS, maxResults);

    return getDbEntityManager().selectList("selectHistoricUserOperationLogPage", params);
  }

  @SuppressWarnings("unchecked")
  public List<OptimizeHistoricIdentityLinkLogEntity> getHistoricIdentityLinkLogs(Date occurredAfter,
                                                                                 Date occurredAt,
                                                                                 int maxResults) {
    checkIsAuthorizedToReadHistoryAndTenants();

    Map<String, Object> params = new HashMap<>();
    params.put(OCCURRED_AFTER, occurredAfter);
    params.put(OCCURRED_AT, occurredAt);
    params.put(MAX_RESULTS, maxResults);

    return getDbEntityManager().selectList("selectHistoricIdentityLinkPage", params);
  }

  @SuppressWarnings("unchecked")
  public List<HistoricProcessInstance> getCompletedHistoricProcessInstances(Date finishedAfter,
                                                                            Date finishedAt,
                                                                            int maxResults) {
    checkIsAuthorizedToReadHistoryAndTenants();

    Map<String, Object> params = new HashMap<>();
    params.put(FINISHED_AFTER, finishedAfter);
    params.put(FINISHED_AT, finishedAt);
    params.put(MAX_RESULTS, maxResults);

    return getDbEntityManager().selectList("selectCompletedHistoricProcessInstancePage", params);
  }

  @SuppressWarnings("unchecked")
  public List<HistoricProcessInstance> getRunningHistoricProcessInstances(Date startedAfter,
                                                                          Date startedAt,
                                                                          int maxResults) {
    checkIsAuthorizedToReadHistoryAndTenants();

    Map<String, Object> params = new HashMap<>();
    params.put(STARTED_AFTER, startedAfter);
    params.put(STARTED_AT, startedAt);
    params.put(MAX_RESULTS, maxResults);

    return getDbEntityManager().selectList("selectRunningHistoricProcessInstancePage", params);
  }

  @SuppressWarnings("unchecked")
  public List<HistoricVariableUpdate> getHistoricVariableUpdates(Date occurredAfter,
                                                                 Date occurredAt,
                                                                 int maxResults) {
    checkIsAuthorizedToReadHistoryAndTenants();

    Map<String, Object> params = new HashMap<>();
    params.put(OCCURRED_AFTER, occurredAfter);
    params.put(OCCURRED_AT, occurredAt);
    params.put(MAX_RESULTS, maxResults);

    return getDbEntityManager().selectList("selectHistoricVariableUpdatePage", params);
  }

  @SuppressWarnings("unchecked")
  public List<HistoricIncidentEntity> getCompletedHistoricIncidents(Date finishedAfter,
                                                                    Date finishedAt,
                                                                    int maxResults) {
    checkIsAuthorizedToReadHistoryAndTenants();

    Map<String, Object> params = new HashMap<>();
    params.put(FINISHED_AFTER, finishedAfter);
    params.put(FINISHED_AT, finishedAt);
    params.put(MAX_RESULTS, maxResults);

    return getDbEntityManager().selectList("selectCompletedHistoricIncidentsPage", params);
  }

  @SuppressWarnings("unchecked")
  public List<HistoricIncidentEntity> getOpenHistoricIncidents(Date createdAfter,
                                                               Date createdAt,
                                                               int maxResults) {
    checkIsAuthorizedToReadHistoryAndTenants();

    Map<String, Object> params = new HashMap<>();
    params.put(CREATED_AFTER, createdAfter);
    params.put(CREATED_AT, createdAt);
    params.put(MAX_RESULTS, maxResults);

    return getDbEntityManager().selectList("selectOpenHistoricIncidentsPage", params);
  }

  @SuppressWarnings("unchecked")
  public List<HistoricDecisionInstance> getHistoricDecisionInstances(Date evaluatedAfter,
                                                                     Date evaluatedAt,
                                                                     int maxResults) {
    checkIsAuthorizedToReadHistoryAndTenants();

    Map<String, Object> params = new HashMap<>();
    params.put(EVALUATED_AFTER, evaluatedAfter);
    params.put(EVALUATED_AT, evaluatedAt);
    params.put(MAX_RESULTS, maxResults);

    List<HistoricDecisionInstance> decisionInstances =
      getDbEntityManager().selectList("selectHistoricDecisionInstancePage", params);

    HistoricDecisionInstanceQueryImpl query =
      (HistoricDecisionInstanceQueryImpl) new HistoricDecisionInstanceQueryImpl()
        .disableBinaryFetching()
        .disableCustomObjectDeserialization()
        .includeInputs()
        .includeOutputs();

    List<List<HistoricDecisionInstance>> partitions = CollectionUtil.partition(decisionInstances, DbSqlSessionFactory.MAXIMUM_NUMBER_PARAMS);

    for (List<HistoricDecisionInstance> partition : partitions) {
      getHistoricDecisionInstanceManager()
        .enrichHistoricDecisionsWithInputsAndOutputs(query, partition);
    }

    return decisionInstances;
  }

  private void checkIsAuthorizedToReadHistoryAndTenants() {
    CompositePermissionCheck necessaryPermissionsForOptimize = new PermissionCheckBuilder()
      .conjunctive()
      .atomicCheckForResourceId(PROCESS_DEFINITION, ANY, READ_HISTORY)
      .atomicCheckForResourceId(DECISION_DEFINITION, ANY, READ_HISTORY)
      .atomicCheckForResourceId(TENANT, ANY, READ)
      .build();
    getAuthorizationManager().checkAuthorization(necessaryPermissionsForOptimize);
  }
}
