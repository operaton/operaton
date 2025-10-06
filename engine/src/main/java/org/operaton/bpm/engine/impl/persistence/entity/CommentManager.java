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

import org.operaton.bpm.engine.impl.Direction;
import org.operaton.bpm.engine.impl.QueryOrderingProperty;
import org.operaton.bpm.engine.impl.QueryPropertyImpl;
import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.db.ListQueryParameterObject;
import org.operaton.bpm.engine.impl.db.entitymanager.operation.DbOperation;
import org.operaton.bpm.engine.impl.persistence.AbstractHistoricManager;
import org.operaton.bpm.engine.task.Comment;
import org.operaton.bpm.engine.task.Event;

/**
 * @author Tom Baeyens
 */
public class CommentManager extends AbstractHistoricManager {


  @Override
  public void delete(DbEntity dbEntity) {
    checkHistoryEnabled();
    super.delete(dbEntity);
  }

  @Override
  public void insert(DbEntity dbEntity) {
    checkHistoryEnabled();
    super.insert(dbEntity);
  }

  @SuppressWarnings("unchecked")
  public List<Comment> findCommentsByTaskId(String taskId) {
    checkHistoryEnabled();
    return getDbEntityManager().selectList("selectCommentsByTaskId", taskId);
  }

  @SuppressWarnings("unchecked")
  public List<Event> findEventsByTaskId(String taskId) {
    checkHistoryEnabled();

    ListQueryParameterObject query = new ListQueryParameterObject();
    query.setParameter(taskId);
    query.getOrderingProperties().add(new QueryOrderingProperty(new QueryPropertyImpl("TIME_"), Direction.DESCENDING));

    return getDbEntityManager().selectList("selectEventsByTaskId", query);
  }

  public void deleteCommentsByTaskId(String taskId) {
    checkHistoryEnabled();
    getDbEntityManager().delete(CommentEntity.class, "deleteCommentsByTaskId", taskId);
  }

  public void deleteCommentsByProcessInstanceIds(List<String> processInstanceIds) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(PROCESS_INSTANCE_IDS, processInstanceIds);
    deleteComments(parameters);
  }

  public void deleteCommentsByTaskProcessInstanceIds(List<String> processInstanceIds) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(TASK_PROCESS_INSTANCE_IDS, processInstanceIds);
    deleteComments(parameters);
  }

  public void deleteCommentsByTaskCaseInstanceIds(List<String> caseInstanceIds) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(TASK_CASE_INSTANCE_IDS, caseInstanceIds);
    deleteComments(parameters);
  }

  protected void deleteComments(Map<String, Object> parameters) {
    getDbEntityManager().deletePreserveOrder(CommentEntity.class, "deleteCommentsByIds", parameters);
  }

  @SuppressWarnings("unchecked")
  public List<Comment> findCommentsByProcessInstanceId(String processInstanceId) {
    checkHistoryEnabled();
    return getDbEntityManager().selectList("selectCommentsByProcessInstanceId", processInstanceId);
  }

  public CommentEntity findCommentByTaskIdAndCommentId(String taskId, String commentId) {
    checkHistoryEnabled();

    Map<String, String> parameters = new HashMap<>();
    parameters.put(TASK_ID, taskId);
    parameters.put(ID, commentId);

    return (CommentEntity) getDbEntityManager().selectOne("selectCommentByTaskIdAndCommentId", parameters);
  }

  public CommentEntity findCommentByProcessInstanceIdAndCommentId(String processInstanceId, String commentId) {
    checkHistoryEnabled();

    Map<String, String> parameters = new HashMap<>();
    parameters.put(PROCESS_INSTANCE_ID, processInstanceId);
    parameters.put(ID, commentId);

    return (CommentEntity) getDbEntityManager().selectOne("selectCommentByProcessInstanceIdAndCommentId", parameters);
  }

  public DbOperation addRemovalTimeToCommentsByRootProcessInstanceId(String rootProcessInstanceId, Date removalTime, Integer batchSize) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(ROOT_PROCESS_INSTANCE_ID, rootProcessInstanceId);
    parameters.put(REMOVAL_TIME, removalTime);
    parameters.put(MAX_RESULTS, batchSize);

    return getDbEntityManager()
      .updatePreserveOrder(CommentEntity.class, "updateCommentsByRootProcessInstanceId", parameters);
  }

  public DbOperation addRemovalTimeToCommentsByProcessInstanceId(String processInstanceId, Date removalTime, Integer batchSize) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(PROCESS_INSTANCE_ID, processInstanceId);
    parameters.put(REMOVAL_TIME, removalTime);
    parameters.put(MAX_RESULTS, batchSize);

    return getDbEntityManager()
      .updatePreserveOrder(CommentEntity.class, "updateCommentsByProcessInstanceId", parameters);
  }

  public DbOperation deleteCommentsByRemovalTime(Date removalTime, int minuteFrom, int minuteTo, int batchSize) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(REMOVAL_TIME, removalTime);
    if (minuteTo - minuteFrom + 1 < 60) {
      parameters.put(MINUTE_FROM, minuteFrom);
      parameters.put(MINUTE_TO, minuteTo);
    }
    parameters.put(BATCH_SIZE, batchSize);

    return getDbEntityManager()
      .deletePreserveOrder(CommentEntity.class, "deleteCommentsByRemovalTime",
        new ListQueryParameterObject(parameters, 0, batchSize));
  }
}
