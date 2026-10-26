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
package org.operaton.bpm.engine.impl.db.sql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.operaton.bpm.engine.impl.ProcessEngineLogger;

import org.jspecify.annotations.Nullable;
import org.operaton.bpm.engine.impl.HistoricProcessInstanceQueryImpl;
import org.operaton.bpm.engine.impl.HistoricTaskInstanceQueryImpl;
import org.operaton.bpm.engine.impl.ProcessInstanceQueryImpl;
import org.operaton.bpm.engine.impl.QueryOrderingProperty;
import org.operaton.bpm.engine.impl.QueryVariableValue;
import org.operaton.bpm.engine.impl.TaskQueryImpl;
import org.operaton.bpm.engine.impl.db.EnginePersistenceLogger;
import org.operaton.bpm.engine.query.QueryProperty;

/**
 * @author Thorben Lindhauer
 */
public final class MybatisJoinHelper {
  protected static final EnginePersistenceLogger LOG = ProcessEngineLogger.PERSISTENCE_LOGGER;

  protected static final String DEFAULT_ORDER = "RES.ID_ asc";
  private static final Map<String, MyBatisTableMapping> MAPPINGS = new HashMap<>();

  static {
    MAPPINGS.put(QueryOrderingProperty.RELATION_VARIABLE, new VariableTableMapping());
    MAPPINGS.put(QueryOrderingProperty.RELATION_PROCESS_DEFINITION, new ProcessDefinitionTableMapping());
    MAPPINGS.put(QueryOrderingProperty.RELATION_CASE_DEFINITION, new CaseDefinitionTableMapping());
    MAPPINGS.put(QueryOrderingProperty.RELATION_DEPLOYMENT, new DeploymentTableMapping());
  }

  private MybatisJoinHelper() {
  }

  public static String tableAlias(@Nullable String relation, int index) {
    if (relation == null) {
      return "RES";
    } else {
      MyBatisTableMapping mapping = getTableMapping(relation);

      if (mapping.isOneToOneRelation()) {
        return mapping.getTableAlias();
      } else {
        return mapping.getTableAlias() + index;
      }
    }
  }

  public static String tableMapping(String relation) {
    MyBatisTableMapping mapping = getTableMapping(relation);

    return mapping.getTableName();
  }

  public static String orderBySelection(QueryOrderingProperty orderingProperty, int index) {
    QueryProperty queryProperty = orderingProperty.getQueryProperty();

    StringBuilder sb = new StringBuilder();

    if (queryProperty.getFunction() != null) {
      sb.append(queryProperty.getFunction());
      sb.append("(");
    }

    sb.append(tableAlias(orderingProperty.getRelation(), index));
    sb.append(".");
    sb.append(queryProperty.getName());

    if (queryProperty.getFunction() != null) {
      sb.append(")");
    }

    return sb.toString();
  }

  public static String orderBy(QueryOrderingProperty orderingProperty, int index) {
    QueryProperty queryProperty = orderingProperty.getQueryProperty();

    StringBuilder sb = new StringBuilder();

    sb.append(tableAlias(orderingProperty.getRelation(), index));
    if (orderingProperty.isContainedProperty()) {
      sb.append(".");
    } else {
      sb.append("_");
    }
    sb.append(queryProperty.getName());

    sb.append(" ");

    sb.append(orderingProperty.getDirection().getName());

    return sb.toString();

  }

  public static String buildOrderBy(List<QueryOrderingProperty> orderingProperties) {
    if (orderingProperties == null || orderingProperties.isEmpty()) {
      return DEFAULT_ORDER;
    }

    StringBuilder sb = new StringBuilder();

    for (int index = 0; index < orderingProperties.size(); index++) {
      if (index > 0) {
        sb.append(", ");
      }
      sb.append(orderBy(orderingProperties.get(index), index));
    }

    return sb.toString();
  }

  /**
   * Computes which optional joins are needed for a {@code TaskQueryImpl} union/or query.
   * <p>
   * This is computed once in Java rather than accumulated via nested {@code <bind>} elements
   * inside a mapper {@code <foreach>}, since MyBatis's {@code ForEachSqlNode} copies the parent
   * bindings into a per-iteration snapshot instead of delegating writes back to the parent,
   * so a {@code <bind>} written during one iteration is no longer visible after the loop ends.
   */
  public static TaskJoinFlags computeTaskJoinFlags(List<TaskQueryImpl> queries) {
    boolean identityLinkJoin = false;
    boolean processDefinitionJoin = false;
    boolean executionJoin = false;
    boolean caseDefinitionJoin = false;
    boolean caseExecutionJoin = false;
    boolean subProcessExecutionJoin = false;
    String joinType = "inner join";

    for (TaskQueryImpl query : queries) {
      if (query == null) {
        continue;
      }

      if (query.isOrQueryActive()) {
        joinType = "left join";
      }

      if (query.getCandidateUser() != null || query.getCandidateGroups() != null || query.getCandidateGroupLike() != null
          || query.getInvolvedUser() != null || Boolean.TRUE.equals(query.isWithCandidateGroups())
          || Boolean.TRUE.equals(query.isWithCandidateUsers())) {
        identityLinkJoin = true;
      }

      if (query.getProcessDefinitionKey() != null || query.getProcessDefinitionName() != null
          || query.getProcessDefinitionNameLike() != null
          || (query.getProcessDefinitionKeys() != null && query.getProcessDefinitionKeys().length > 0)) {
        processDefinitionJoin = true;
      }

      if (query.getProcessInstanceBusinessKey() != null || query.getProcessInstanceBusinessKeyLike() != null
          || (query.getProcessInstanceBusinessKeys() != null && query.getProcessInstanceBusinessKeys().length > 0)) {
        executionJoin = true;
      }

      if (query.getCaseDefinitionKey() != null || query.getCaseDefinitionName() != null
          || query.getCaseDefinitionNameLike() != null) {
        caseDefinitionJoin = true;
      }

      if (query.getCaseInstanceBusinessKey() != null || query.getCaseInstanceBusinessKeyLike() != null) {
        caseExecutionJoin = true;
      }

      if (query.getActivityInstanceIdIn() != null && query.getActivityInstanceIdIn().length > 0) {
        subProcessExecutionJoin = true;
      }
    }

    return new TaskJoinFlags(identityLinkJoin, processDefinitionJoin, executionJoin, caseDefinitionJoin,
        caseExecutionJoin, subProcessExecutionJoin, joinType);
  }

  public record TaskJoinFlags(boolean identityLinkJoin, boolean processDefinitionJoin, boolean executionJoin,
      boolean caseDefinitionJoin, boolean caseExecutionJoin, boolean subProcessExecutionJoin, String joinType) {
  }

  /**
   * Computes which optional joins are needed for a {@code ProcessInstanceQueryImpl} union/or query.
   * See {@link #computeTaskJoinFlags(List)} for why this is precomputed in Java rather than
   * accumulated via nested {@code <bind>} elements inside a mapper {@code <foreach>}.
   */
  public static ProcessInstanceJoinFlags computeProcessInstanceJoinFlags(List<ProcessInstanceQueryImpl> queries) {
    boolean incidentJoin = false;
    boolean executionJoin = false;
    String joinType = "inner join";

    for (ProcessInstanceQueryImpl query : queries) {
      if (query == null) {
        continue;
      }

      if (query.isOrQueryActive()) {
        joinType = "left join";
      }

      if (query.isWithIncident() || query.getIncidentType() != null || query.getIncidentId() != null
          || query.getIncidentMessage() != null || query.getIncidentMessageLike() != null) {
        incidentJoin = true;
      }

      if (query.getActivityIds() != null && query.getActivityIds().length > 0) {
        executionJoin = true;
      }
    }

    return new ProcessInstanceJoinFlags(incidentJoin, executionJoin, joinType);
  }

  public record ProcessInstanceJoinFlags(boolean incidentJoin, boolean executionJoin, String joinType) {
  }

  /**
   * Computes which optional joins are needed for a {@code HistoricProcessInstanceQueryImpl} union/or query.
   * See {@link #computeTaskJoinFlags(List)} for why this is precomputed in Java rather than
   * accumulated via nested {@code <bind>} elements inside a mapper {@code <foreach>}.
   */
  public static HistoricProcessInstanceJoinFlags computeHistoricProcessInstanceJoinFlags(
      List<HistoricProcessInstanceQueryImpl> queries) {
    boolean incidentJoin = false;
    boolean historicActivityInstanceJoin = false;
    boolean executionJoin = false;
    String joinType = "inner join";

    for (HistoricProcessInstanceQueryImpl query : queries) {
      if (query == null) {
        continue;
      }

      boolean queryIncidentJoin = query.isWithIncidents() || query.isWithRootIncidents()
          || query.getIncidentStatus() != null || query.getIncidentMessage() != null
          || query.getIncidentMessageLike() != null || query.getIncidentType() != null
          || (query.getIncidentIds() != null && query.getIncidentIds().length > 0);

      if (queryIncidentJoin) {
        incidentJoin = true;
        if (query.isOrQueryActive()) {
          joinType = "left join";
        }
      }

      if ((query.getExecutedActivityIds() != null && query.getExecutedActivityIds().length > 0)
          || (query.getActiveActivityIds() != null && query.getActiveActivityIds().length > 0)) {
        historicActivityInstanceJoin = true;
      }

      if (query.getActivityIds() != null && query.getActivityIds().length > 0) {
        executionJoin = true;
      }
    }

    return new HistoricProcessInstanceJoinFlags(incidentJoin, historicActivityInstanceJoin, executionJoin, joinType);
  }

  public record HistoricProcessInstanceJoinFlags(boolean incidentJoin, boolean historicActivityInstanceJoin,
      boolean executionJoin, String joinType) {
  }

  /**
   * Computes which optional joins are needed for a {@code HistoricTaskInstanceQueryImpl} union/or query.
   * See {@link #computeTaskJoinFlags(List)} for why this is precomputed in Java rather than
   * accumulated via nested {@code <bind>} elements inside a mapper {@code <foreach>}.
   */
  public static HistoricTaskInstanceJoinFlags computeHistoricTaskInstanceJoinFlags(
      List<HistoricTaskInstanceQueryImpl> queries) {
    boolean historicProcessInstanceJoin = false;
    boolean processDefinitionJoin = false;
    boolean caseDefinitionJoin = false;
    String joinType = "inner join";

    for (HistoricTaskInstanceQueryImpl query : queries) {
      if (query == null) {
        continue;
      }

      if (query.isOrQueryActive()) {
        joinType = "left join";
      }

      if (query.isProcessFinished() || query.isProcessUnfinished() || query.getProcessInstanceBusinessKey() != null
          || query.getProcessInstanceBusinessKeyLike() != null
          || (query.getProcessInstanceBusinessKeys() != null && query.getProcessInstanceBusinessKeys().length > 0)) {
        historicProcessInstanceJoin = true;
      }

      if (query.getProcessDefinitionKey() != null || query.getProcessDefinitionName() != null) {
        processDefinitionJoin = true;
      }

      if (query.getCaseDefinitionKey() != null || query.getCaseDefinitionName() != null) {
        caseDefinitionJoin = true;
      }
    }

    return new HistoricTaskInstanceJoinFlags(historicProcessInstanceJoin, processDefinitionJoin, caseDefinitionJoin,
        joinType);
  }

  public record HistoricTaskInstanceJoinFlags(boolean historicProcessInstanceJoin, boolean processDefinitionJoin,
      boolean caseDefinitionJoin, String joinType) {
  }

  /**
   * One group of variable value conditions sharing a variable name, joined to a single
   * {@code VI<index>} aliased instance table. The index is stable across the whole set of
   * union/or {@code queries} so it can be used both when generating the joins and when
   * generating the matching WHERE clause conditions.
   * <p>
   * This is precomputed in Java rather than accumulated via a {@code <bind>}-based counter
   * inside nested {@code <foreach>} elements, since MyBatis's {@code ForEachSqlNode} copies the
   * parent bindings into a per-iteration snapshot instead of delegating writes back to the
   * parent, so such a counter no longer survives past a single iteration.
   */
  public record VariableConditionGroup(Object query, int index, Set<QueryVariableValue> values) {
  }

  public static List<VariableConditionGroup> computeVariableConditionGroups(
      List<HistoricProcessInstanceQueryImpl> queries) {
    List<VariableConditionGroup> groups = new ArrayList<>();
    int index = 0;

    for (HistoricProcessInstanceQueryImpl query : queries) {
      if (query == null) {
        continue;
      }
      for (Set<QueryVariableValue> values : query.getQueryVariableNameToValuesMap().values()) {
        groups.add(new VariableConditionGroup(query, index, values));
        index++;
      }
    }

    return groups;
  }

  public static List<VariableConditionGroup> variableConditionGroupsForQuery(List<VariableConditionGroup> groups,
      Object query) {
    List<VariableConditionGroup> result = new ArrayList<>();

    for (VariableConditionGroup group : groups) {
      if (group.query() == query) {
        result.add(group);
      }
    }

    return result;
  }

  protected static MyBatisTableMapping getTableMapping(String relation) {
    MyBatisTableMapping mapping = MAPPINGS.get(relation);

    if (mapping == null) {
      throw LOG.missingRelationMappingException(relation);
    }

    return mapping;
  }
}
