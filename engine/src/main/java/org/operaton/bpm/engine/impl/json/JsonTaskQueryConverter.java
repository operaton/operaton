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
package org.operaton.bpm.engine.impl.json;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.operaton.bpm.engine.impl.QueryOperator;
import org.operaton.bpm.engine.impl.TaskQueryImpl;
import org.operaton.bpm.engine.impl.TaskQueryVariableValue;
import org.operaton.bpm.engine.impl.persistence.entity.SuspensionState;
import org.operaton.bpm.engine.impl.util.JsonUtil;
import org.operaton.bpm.engine.task.DelegationState;
import org.operaton.bpm.engine.task.TaskQuery;

/**
 * @author Sebastian Menski
 */
@SuppressWarnings({"java:S1133", "java:S5738"}) // ORDER_BY will be removed
public class JsonTaskQueryConverter implements JsonObjectConverter<TaskQuery> {
  private static final JsonLegacyQueryOrderingPropertyConverter LEGACY_QUERY_ORDERING_PROPERTY_CONVERTER =
      new JsonLegacyQueryOrderingPropertyConverter();

  public static final String ID = "id";
  public static final String TASK_ID = "taskId";
  public static final String TASK_ID_IN = "taskIdIn";
  public static final String NAME = "name";
  public static final String NAME_NOT_EQUAL = "nameNotEqual";
  public static final String NAME_LIKE = "nameLike";
  public static final String NAME_NOT_LIKE = "nameNotLike";
  public static final String DESCRIPTION = "description";
  public static final String DESCRIPTION_LIKE = "descriptionLike";
  public static final String PRIORITY = "priority";
  public static final String MIN_PRIORITY = "minPriority";
  public static final String MAX_PRIORITY = "maxPriority";
  public static final String ASSIGNEE = "assignee";
  public static final String ASSIGNEE_LIKE = "assigneeLike";
  public static final String ASSIGNEE_IN = "assigneeIn";
  public static final String ASSIGNEE_NOT_IN = "assigneeNotIn";
  public static final String INVOLVED_USER = "involvedUser";
  public static final String OWNER = "owner";
  public static final String UNASSIGNED = "unassigned";
  public static final String ASSIGNED = "assigned";
  public static final String DELEGATION_STATE = "delegationState";
  public static final String CANDIDATE_USER = "candidateUser";
  public static final String CANDIDATE_GROUP = "candidateGroup";
  public static final String CANDIDATE_GROUP_LIKE = "candidateGroupLike";
  public static final String CANDIDATE_GROUPS = "candidateGroups";
  public static final String WITH_CANDIDATE_GROUPS = "withCandidateGroups";
  public static final String WITHOUT_CANDIDATE_GROUPS = "withoutCandidateGroups";
  public static final String WITH_CANDIDATE_USERS = "withCandidateUsers";
  public static final String WITHOUT_CANDIDATE_USERS = "withoutCandidateUsers";
  public static final String INCLUDE_ASSIGNED_TASKS = "includeAssignedTasks";
  public static final String INSTANCE_ID = "instanceId";
  public static final String PROCESS_INSTANCE_ID = "processInstanceId";
  public static final String PROCESS_INSTANCE_ID_IN = "processInstanceIdIn";
  public static final String EXECUTION_ID = "executionId";
  public static final String ACTIVITY_INSTANCE_ID_IN = "activityInstanceIdIn";
  public static final String CREATED = "created";
  public static final String CREATED_BEFORE = "createdBefore";
  public static final String CREATED_AFTER = "createdAfter";
  public static final String UPDATED_AFTER = "updatedAfter";
  public static final String KEY = "key";
  public static final String KEYS = "keys";
  public static final String KEY_NOT_IN = "keyNotIn";
  public static final String KEY_LIKE = "keyLike";
  public static final String PARENT_TASK_ID = "parentTaskId";
  public static final String PROCESS_DEFINITION_KEY = "processDefinitionKey";
  public static final String PROCESS_DEFINITION_KEYS = "processDefinitionKeys";
  public static final String PROCESS_DEFINITION_ID = "processDefinitionId";
  public static final String PROCESS_DEFINITION_NAME = "processDefinitionName";
  public static final String PROCESS_DEFINITION_NAME_LIKE = "processDefinitionNameLike";
  public static final String PROCESS_INSTANCE_BUSINESS_KEY = "processInstanceBusinessKey";
  public static final String PROCESS_INSTANCE_BUSINESS_KEYS ="processInstanceBusinessKeys";
  public static final String PROCESS_INSTANCE_BUSINESS_KEY_LIKE = "processInstanceBusinessKeyLike";
  public static final String DUE = "due";
  public static final String DUE_DATE = "dueDate";
  public static final String DUE_BEFORE = "dueBefore";
  public static final String DUE_AFTER = "dueAfter";
  public static final String WITHOUT_DUE_DATE = "withoutDueDate";
  public static final String FOLLOW_UP = "followUp";
  public static final String FOLLOW_UP_DATE = "followUpDate";
  public static final String FOLLOW_UP_BEFORE = "followUpBefore";
  public static final String FOLLOW_UP_NULL_ACCEPTED = "followUpNullAccepted";
  public static final String FOLLOW_UP_AFTER = "followUpAfter";
  public static final String EXCLUDE_SUBTASKS = "excludeSubtasks";
  public static final String CASE_DEFINITION_KEY = "caseDefinitionKey";
  public static final String CASE_DEFINITION_ID = "caseDefinitionId";
  public static final String CASE_DEFINITION_NAME = "caseDefinitionName";
  public static final String CASE_DEFINITION_NAME_LIKE = "caseDefinitionNameLike";
  public static final String CASE_INSTANCE_ID = "caseInstanceId";
  public static final String CASE_INSTANCE_BUSINESS_KEY = "caseInstanceBusinessKey";
  public static final String CASE_INSTANCE_BUSINESS_KEY_LIKE = "caseInstanceBusinessKeyLike";
  public static final String CASE_EXECUTION_ID = "caseExecutionId";
  public static final String ACTIVE = "active";
  public static final String SUSPENDED = "suspended";
  public static final String PROCESS_VARIABLES = "processVariables";
  public static final String TASK_VARIABLES = "taskVariables";
  public static final String CASE_INSTANCE_VARIABLES = "caseInstanceVariables";
  public static final String TENANT_IDS = "tenantIds";
  public static final String WITHOUT_TENANT_ID = "withoutTenantId";
  public static final String ORDERING_PROPERTIES = "orderingProperties";
  public static final String OR_QUERIES = "orQueries";

  private static final String KEY_SUFFIX_EXPRESSION = "Expression";

  /**
   * Exists for backwards compatibility with Camunda 7.2; deprecated since Camunda 7.3
   * 
   * @deprecated since 1.0, use {@link #ORDERING_PROPERTIES} instead for specifying task query ordering.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  public static final String ORDER_BY = "orderBy";

  protected static JsonTaskQueryVariableValueConverter variableValueConverter = new JsonTaskQueryVariableValueConverter();

  @Override
  public JsonObject toJsonObject(TaskQuery taskQuery) {
    return toJsonObject(taskQuery, false);
  }

  public JsonObject toJsonObject(TaskQuery taskQuery, boolean isOrQueryActive) {
    JsonObject json = JsonUtil.createObject();
    TaskQueryImpl query = (TaskQueryImpl) taskQuery;

    JsonUtil.addField(json, TASK_ID, query.getTaskId());
    JsonUtil.addArrayField(json, TASK_ID_IN, query.getTaskIdIn());
    JsonUtil.addField(json, NAME, query.getName());
    JsonUtil.addField(json, NAME_NOT_EQUAL, query.getNameNotEqual());
    JsonUtil.addField(json, NAME_LIKE, query.getNameLike());
    JsonUtil.addField(json, NAME_NOT_LIKE, query.getNameNotLike());
    JsonUtil.addField(json, DESCRIPTION, query.getDescription());
    JsonUtil.addField(json, DESCRIPTION_LIKE, query.getDescriptionLike());
    JsonUtil.addField(json, PRIORITY, query.getPriority());
    JsonUtil.addField(json, MIN_PRIORITY, query.getMinPriority());
    JsonUtil.addField(json, MAX_PRIORITY, query.getMaxPriority());
    JsonUtil.addField(json, ASSIGNEE, query.getAssignee());

    if (query.getAssigneeIn() != null) {
      JsonUtil.addArrayField(json, ASSIGNEE_IN,
          query.getAssigneeIn().toArray(new String[query.getAssigneeIn().size()]));
    }

    if (query.getAssigneeNotIn() != null) {
      JsonUtil.addArrayField(json, ASSIGNEE_NOT_IN,
              query.getAssigneeNotIn().toArray(new String[query.getAssigneeNotIn().size()]));
    }

    JsonUtil.addField(json, ASSIGNEE_LIKE, query.getAssigneeLike());
    JsonUtil.addField(json, INVOLVED_USER, query.getInvolvedUser());
    JsonUtil.addField(json, OWNER, query.getOwner());
    JsonUtil.addDefaultField(json, UNASSIGNED, false, query.isUnassigned());
    JsonUtil.addDefaultField(json, ASSIGNED, false, query.isAssigned());
    JsonUtil.addField(json, DELEGATION_STATE, query.getDelegationStateString());
    JsonUtil.addField(json, CANDIDATE_USER, query.getCandidateUser());
    JsonUtil.addField(json, CANDIDATE_GROUP, query.getCandidateGroup());
    JsonUtil.addField(json, CANDIDATE_GROUP_LIKE, query.getCandidateGroupLike());
    JsonUtil.addListField(json, CANDIDATE_GROUPS, query.getCandidateGroupsInternal());
    JsonUtil.addDefaultField(json, WITH_CANDIDATE_GROUPS, false, query.isWithCandidateGroups());
    JsonUtil.addDefaultField(json, WITHOUT_CANDIDATE_GROUPS, false, query.isWithoutCandidateGroups());
    JsonUtil.addDefaultField(json, WITH_CANDIDATE_USERS, false, query.isWithCandidateUsers());
    JsonUtil.addDefaultField(json, WITHOUT_CANDIDATE_USERS, false, query.isWithoutCandidateUsers());
    JsonUtil.addField(json, INCLUDE_ASSIGNED_TASKS, query.isIncludeAssignedTasksInternal());
    JsonUtil.addField(json, PROCESS_INSTANCE_ID, query.getProcessInstanceId());
    if (query.getProcessInstanceIdIn() != null) {
      JsonUtil.addArrayField(json, PROCESS_INSTANCE_ID_IN, query.getProcessInstanceIdIn());
    }
    JsonUtil.addField(json, EXECUTION_ID, query.getExecutionId());
    JsonUtil.addArrayField(json, ACTIVITY_INSTANCE_ID_IN, query.getActivityInstanceIdIn());
    JsonUtil.addDateField(json, CREATED, query.getCreateTime());
    JsonUtil.addDateField(json, CREATED_BEFORE, query.getCreateTimeBefore());
    JsonUtil.addDateField(json, CREATED_AFTER, query.getCreateTimeAfter());
    JsonUtil.addDateField(json, UPDATED_AFTER, query.getUpdatedAfter());
    JsonUtil.addField(json, KEY, query.getKey());
    JsonUtil.addArrayField(json, KEYS, query.getKeys());
    JsonUtil.addArrayField(json, KEY_NOT_IN, query.getKeyNotIn());
    JsonUtil.addField(json, KEY_LIKE, query.getKeyLike());
    JsonUtil.addField(json, PARENT_TASK_ID, query.getParentTaskId());
    JsonUtil.addField(json, PROCESS_DEFINITION_KEY, query.getProcessDefinitionKey());
    JsonUtil.addArrayField(json, PROCESS_DEFINITION_KEYS, query.getProcessDefinitionKeys());
    JsonUtil.addField(json, PROCESS_DEFINITION_ID, query.getProcessDefinitionId());
    JsonUtil.addField(json, PROCESS_DEFINITION_NAME, query.getProcessDefinitionName());
    JsonUtil.addField(json, PROCESS_DEFINITION_NAME_LIKE, query.getProcessDefinitionNameLike());
    JsonUtil.addField(json, PROCESS_INSTANCE_BUSINESS_KEY, query.getProcessInstanceBusinessKey());
    JsonUtil.addArrayField(json, PROCESS_INSTANCE_BUSINESS_KEYS, query.getProcessInstanceBusinessKeys());
    JsonUtil.addField(json, PROCESS_INSTANCE_BUSINESS_KEY_LIKE, query.getProcessInstanceBusinessKeyLike());
    addVariablesFields(json, query.getVariables());
    JsonUtil.addDateField(json, DUE, query.getDueDate());
    JsonUtil.addDateField(json, DUE_BEFORE, query.getDueBefore());
    JsonUtil.addDateField(json, DUE_AFTER, query.getDueAfter());
    JsonUtil.addDefaultField(json, WITHOUT_DUE_DATE, false, query.isWithoutDueDate());
    JsonUtil.addDateField(json, FOLLOW_UP, query.getFollowUpDate());
    JsonUtil.addDateField(json, FOLLOW_UP_BEFORE, query.getFollowUpBefore());
    JsonUtil.addDefaultField(json, FOLLOW_UP_NULL_ACCEPTED, false, query.isFollowUpNullAccepted());
    JsonUtil.addDateField(json, FOLLOW_UP_AFTER, query.getFollowUpAfter());
    JsonUtil.addDefaultField(json, EXCLUDE_SUBTASKS, false, query.isExcludeSubtasks());
    addSuspensionStateField(json, query.getSuspensionState());
    JsonUtil.addField(json, CASE_DEFINITION_KEY, query.getCaseDefinitionKey());
    JsonUtil.addField(json, CASE_DEFINITION_ID, query.getCaseDefinitionId());
    JsonUtil.addField(json, CASE_DEFINITION_NAME, query.getCaseDefinitionName());
    JsonUtil.addField(json, CASE_DEFINITION_NAME_LIKE, query.getCaseDefinitionNameLike());
    JsonUtil.addField(json, CASE_INSTANCE_ID, query.getCaseInstanceId());
    JsonUtil.addField(json, CASE_INSTANCE_BUSINESS_KEY, query.getCaseInstanceBusinessKey());
    JsonUtil.addField(json, CASE_INSTANCE_BUSINESS_KEY_LIKE, query.getCaseInstanceBusinessKeyLike());
    JsonUtil.addField(json, CASE_EXECUTION_ID, query.getCaseExecutionId());
    addTenantIdFields(json, query);

    if (query.getQueries().size() > 1 && !isOrQueryActive) {
      JsonArray orQueries = JsonUtil.createArray();

      for (TaskQueryImpl orQuery: query.getQueries()) {
        if (orQuery != null && orQuery.isOrQueryActive()) {
          orQueries.add(toJsonObject(orQuery, true));
        }
      }

      JsonUtil.addField(json, OR_QUERIES, orQueries);
    }

    if (query.getOrderingProperties() != null && !query.getOrderingProperties().isEmpty()) {
      JsonUtil.addField(json, ORDERING_PROPERTIES,
          JsonQueryOrderingPropertyConverter.ARRAY_CONVERTER.toJsonArray(query.getOrderingProperties()));
    }


    // expressions
    for (Map.Entry<String, String> expressionEntry : query.getExpressions().entrySet()) {
      JsonUtil.addField(json, expressionEntry.getKey() + KEY_SUFFIX_EXPRESSION, expressionEntry.getValue());
    }

    return json;
  }

  protected void addSuspensionStateField(JsonObject jsonObject, SuspensionState suspensionState) {
    if (suspensionState != null) {
      if (suspensionState.equals(SuspensionState.ACTIVE)) {
        JsonUtil.addField(jsonObject, ACTIVE, true);
      }
      else if (suspensionState.equals(SuspensionState.SUSPENDED)) {
        JsonUtil.addField(jsonObject, SUSPENDED, true);
      }
    }
  }

  protected void addTenantIdFields(JsonObject jsonObject, TaskQueryImpl query) {
    if (query.getTenantIds() != null) {
      JsonUtil.addArrayField(jsonObject, TENANT_IDS, query.getTenantIds());
    }
    if (query.isWithoutTenantId()) {
      JsonUtil.addField(jsonObject, WITHOUT_TENANT_ID, true);
    }
  }

  protected void addVariablesFields(JsonObject jsonObject, List<TaskQueryVariableValue> variables) {
    for (TaskQueryVariableValue variable : variables) {
      if (variable.isProcessInstanceVariable()) {
        addVariable(jsonObject, PROCESS_VARIABLES, variable);
      }
      else if(variable.isLocal()) {
        addVariable(jsonObject, TASK_VARIABLES, variable);
      }
      else {
        addVariable(jsonObject, CASE_INSTANCE_VARIABLES, variable);
      }
    }
  }

  protected void addVariable(JsonObject jsonObject, String variableType, TaskQueryVariableValue variable) {
    JsonArray variables = JsonUtil.getArray(jsonObject, variableType);

    JsonUtil.addElement(variables, variableValueConverter, variable);
    JsonUtil.addField(jsonObject, variableType, variables);
  }

  @Override
  public TaskQuery toObject(JsonObject json) {
    return toObject(json, false);
  }

  @SuppressWarnings("java:S3776")
  protected TaskQuery toObject(JsonObject json, boolean isOrQuery) {
    TaskQueryImpl query = new TaskQueryImpl();
    if (isOrQuery) {
      query.setOrQueryActive();
    }
    if (json.has(OR_QUERIES)) {
      for (JsonElement jsonElement : JsonUtil.getArray(json, OR_QUERIES)) {
        query.addOrQuery((TaskQueryImpl) toObject(JsonUtil.getObject(jsonElement), true));
      }
    }

    for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
      String key = entry.getKey();
      BiConsumer<TaskQueryImpl, JsonObject> handler = handlers.get(key);
      if (handler != null) {
        handler.accept(query, json);
      } else if (key.endsWith(KEY_SUFFIX_EXPRESSION)) {
        String expression = JsonUtil.getString(json, key);
        query.addExpression(key.substring(0, key.length() - KEY_SUFFIX_EXPRESSION.length()), expression);
      }
    }

    return query;
  }

  protected static final Map<String, BiConsumer<TaskQueryImpl, JsonObject>> handlers = Map.ofEntries(
      Map.entry(TASK_ID, (query, json) -> query.taskId(JsonUtil.getString(json, TASK_ID))),
      Map.entry(TASK_ID_IN, (query, json) -> query.taskIdIn(getArray(JsonUtil.getArray(json, TASK_ID_IN)))),
      Map.entry(NAME, (query, json) -> query.taskName(JsonUtil.getString(json, NAME))),
      Map.entry(NAME_NOT_EQUAL, (query, json) -> query.taskNameNotEqual(JsonUtil.getString(json, NAME_NOT_EQUAL))),
      Map.entry(NAME_LIKE, (query, json) -> query.taskNameLike(JsonUtil.getString(json, NAME_LIKE))),
      Map.entry(NAME_NOT_LIKE, (query, json) -> query.taskNameNotLike(JsonUtil.getString(json, NAME_NOT_LIKE))),
      Map.entry(DESCRIPTION, (query, json) -> query.taskDescription(JsonUtil.getString(json, DESCRIPTION))),
      Map.entry(DESCRIPTION_LIKE, (query, json) -> query.taskDescriptionLike(JsonUtil.getString(json, DESCRIPTION_LIKE))),
      Map.entry(PRIORITY, (query, json) -> query.taskPriority(JsonUtil.getInt(json, PRIORITY))),
      Map.entry(MIN_PRIORITY, (query, json) -> query.taskMinPriority(JsonUtil.getInt(json, MIN_PRIORITY))),
      Map.entry(MAX_PRIORITY, (query, json) -> query.taskMaxPriority(JsonUtil.getInt(json, MAX_PRIORITY))),
      Map.entry(ASSIGNEE, (query, json) -> query.taskAssignee(JsonUtil.getString(json, ASSIGNEE))),
      Map.entry(ASSIGNEE_LIKE, (query, json) -> query.taskAssigneeLike(JsonUtil.getString(json, ASSIGNEE_LIKE))),
      Map.entry(ASSIGNEE_IN, (query, json) -> query.taskAssigneeIn(getArray(JsonUtil.getArray(json, ASSIGNEE_IN)))),
      Map.entry(ASSIGNEE_NOT_IN, (query, json) -> query.taskAssigneeNotIn(getArray(JsonUtil.getArray(json, ASSIGNEE_NOT_IN)))),
      Map.entry(INVOLVED_USER, (query, json) -> query.taskInvolvedUser(JsonUtil.getString(json, INVOLVED_USER))),
      Map.entry(OWNER, (query, json) -> query.taskOwner(JsonUtil.getString(json, OWNER))),
      Map.entry(ASSIGNED, (query, json) -> {
        if (JsonUtil.getBoolean(json, ASSIGNED)) {
          query.taskAssigned();
        }
      }),
      Map.entry(UNASSIGNED, (query, json) -> {
        if (JsonUtil.getBoolean(json, UNASSIGNED)) {
          query.taskUnassigned();
        }
      }),
      Map.entry(DELEGATION_STATE, (query, json) -> query.taskDelegationState(DelegationState.valueOf(JsonUtil.getString(json, DELEGATION_STATE)))),
      Map.entry(CANDIDATE_USER, (query, json) -> query.taskCandidateUser(JsonUtil.getString(json, CANDIDATE_USER))),
      Map.entry(CANDIDATE_GROUP, (query, json) -> query.taskCandidateGroup(JsonUtil.getString(json, CANDIDATE_GROUP))),
      Map.entry(CANDIDATE_GROUP_LIKE, (query, json) -> query.taskCandidateGroupLike(JsonUtil.getString(json, CANDIDATE_GROUP_LIKE))),
      Map.entry(CANDIDATE_GROUPS, (query, json) -> {
        if (!json.has(CANDIDATE_USER) && !json.has(CANDIDATE_GROUP)) {
          query.taskCandidateGroupIn(getList(JsonUtil.getArray(json, CANDIDATE_GROUPS)));
        }
      }),
      Map.entry(WITH_CANDIDATE_GROUPS, (query, json) -> {
        if (JsonUtil.getBoolean(json, WITH_CANDIDATE_GROUPS)) {
          query.withCandidateGroups();
        }
      }),
      Map.entry(WITHOUT_CANDIDATE_GROUPS, (query, json) -> {
        if (JsonUtil.getBoolean(json, WITHOUT_CANDIDATE_GROUPS)) {
          query.withoutCandidateGroups();
        }
      }),
      Map.entry(WITH_CANDIDATE_USERS, (query, json) -> {
        if (JsonUtil.getBoolean(json, WITH_CANDIDATE_USERS)) {
          query.withCandidateUsers();
        }
      }),
      Map.entry(WITHOUT_CANDIDATE_USERS, (query, json) -> {
        if (JsonUtil.getBoolean(json, WITHOUT_CANDIDATE_USERS)) {
          query.withoutCandidateUsers();
        }
      }),
      Map.entry(INCLUDE_ASSIGNED_TASKS, (query, json) -> {
        if (JsonUtil.getBoolean(json, INCLUDE_ASSIGNED_TASKS)) {
          query.includeAssignedTasksInternal();
        }
      }),
      Map.entry(PROCESS_INSTANCE_ID, (query, json) -> query.processInstanceId(JsonUtil.getString(json, PROCESS_INSTANCE_ID))),
      Map.entry(PROCESS_INSTANCE_ID_IN, (query, json) -> query.processInstanceIdIn(getArray(JsonUtil.getArray(json, PROCESS_INSTANCE_ID_IN)))),
      Map.entry(EXECUTION_ID, (query, json) -> query.executionId(JsonUtil.getString(json, EXECUTION_ID))),
      Map.entry(ACTIVITY_INSTANCE_ID_IN, (query, json) -> query.activityInstanceIdIn(getArray(JsonUtil.getArray(json, ACTIVITY_INSTANCE_ID_IN)))),
      Map.entry(CREATED, (query, json) -> query.taskCreatedOn(new Date(JsonUtil.getLong(json, CREATED)))),
      Map.entry(CREATED_BEFORE, (query, json) -> query.taskCreatedBefore(new Date(JsonUtil.getLong(json, CREATED_BEFORE)))),
      Map.entry(CREATED_AFTER, (query, json) -> query.taskCreatedAfter(new Date(JsonUtil.getLong(json, CREATED_AFTER)))),
      Map.entry(UPDATED_AFTER, (query, json) -> query.taskUpdatedAfter(new Date(JsonUtil.getLong(json, UPDATED_AFTER)))),
      Map.entry(KEY, (query, json) -> query.taskDefinitionKey(JsonUtil.getString(json, KEY))),
      Map.entry(KEYS, (query, json) -> query.taskDefinitionKeyIn(getArray(JsonUtil.getArray(json, KEYS)))),
      Map.entry(KEY_NOT_IN, (query, json) -> query.taskDefinitionKeyNotIn(getArray(JsonUtil.getArray(json, KEY_NOT_IN)))),
      Map.entry(KEY_LIKE, (query, json) -> query.taskDefinitionKeyLike(JsonUtil.getString(json, KEY_LIKE))),
      Map.entry(PARENT_TASK_ID, (query, json) -> query.taskParentTaskId(JsonUtil.getString(json, PARENT_TASK_ID))),
      Map.entry(PROCESS_DEFINITION_KEY, (query, json) -> query.processDefinitionKey(JsonUtil.getString(json, PROCESS_DEFINITION_KEY))),
      Map.entry(PROCESS_DEFINITION_KEYS, (query, json) -> query.processDefinitionKeyIn(getArray(JsonUtil.getArray(json, PROCESS_DEFINITION_KEYS)))),
      Map.entry(PROCESS_DEFINITION_ID, (query, json) -> query.processDefinitionId(JsonUtil.getString(json, PROCESS_DEFINITION_ID))),
      Map.entry(PROCESS_DEFINITION_NAME, (query, json) -> query.processDefinitionName(JsonUtil.getString(json, PROCESS_DEFINITION_NAME))),
      Map.entry(PROCESS_DEFINITION_NAME_LIKE, (query, json) -> query.processDefinitionNameLike(JsonUtil.getString(json, PROCESS_DEFINITION_NAME_LIKE))),
      Map.entry(PROCESS_INSTANCE_BUSINESS_KEY, (query, json) -> query.processInstanceBusinessKey(JsonUtil.getString(json, PROCESS_INSTANCE_BUSINESS_KEY))),
      Map.entry(PROCESS_INSTANCE_BUSINESS_KEYS, (query, json) -> query.processInstanceBusinessKeyIn(getArray(JsonUtil.getArray(json, PROCESS_INSTANCE_BUSINESS_KEYS)))),
      Map.entry(PROCESS_INSTANCE_BUSINESS_KEY_LIKE, (query, json) -> query.processInstanceBusinessKeyLike(JsonUtil.getString(json, PROCESS_INSTANCE_BUSINESS_KEY_LIKE))),
      Map.entry(TASK_VARIABLES, (query, json) -> addVariables(query, JsonUtil.getArray(json, TASK_VARIABLES), true, false)),
      Map.entry(PROCESS_VARIABLES, (query, json) -> addVariables(query, JsonUtil.getArray(json, PROCESS_VARIABLES), false, true)),
      Map.entry(CASE_INSTANCE_VARIABLES, (query, json) -> addVariables(query, JsonUtil.getArray(json, CASE_INSTANCE_VARIABLES), false, false)),
      Map.entry(DUE, (query, json) -> query.dueDate(new Date(JsonUtil.getLong(json, DUE)))),
      Map.entry(DUE_BEFORE, (query, json) -> query.dueBefore(new Date(JsonUtil.getLong(json, DUE_BEFORE)))),
      Map.entry(DUE_AFTER, (query, json) -> query.dueAfter(new Date(JsonUtil.getLong(json, DUE_AFTER)))),
      Map.entry(WITHOUT_DUE_DATE, (query, json) -> query.withoutDueDate()),
      Map.entry(FOLLOW_UP, (query, json) -> query.followUpDate(new Date(JsonUtil.getLong(json, FOLLOW_UP)))),
      Map.entry(FOLLOW_UP_BEFORE, (query, json) -> query.followUpBefore(new Date(JsonUtil.getLong(json, FOLLOW_UP_BEFORE)))),
      Map.entry(FOLLOW_UP_AFTER, (query, json) -> query.followUpAfter(new Date(JsonUtil.getLong(json, FOLLOW_UP_AFTER)))),
      Map.entry(FOLLOW_UP_NULL_ACCEPTED, (query, json) -> query.setFollowUpNullAccepted(JsonUtil.getBoolean(json, FOLLOW_UP_NULL_ACCEPTED))),
      Map.entry(EXCLUDE_SUBTASKS, (query, json) -> {
        if (JsonUtil.getBoolean(json, EXCLUDE_SUBTASKS)) {
          query.excludeSubtasks();
        }
      }),
      Map.entry(SUSPENDED, (query, json) -> {
        if (JsonUtil.getBoolean(json, SUSPENDED)) {
          query.suspended();
        }
      }),
      Map.entry(ACTIVE, (query, json) -> {
        if (JsonUtil.getBoolean(json, ACTIVE)) {
          query.active();
        }
      }),
      Map.entry(CASE_DEFINITION_KEY, (query, json) -> query.caseDefinitionKey(JsonUtil.getString(json, CASE_DEFINITION_KEY))),
      Map.entry(CASE_DEFINITION_ID, (query, json) -> query.caseDefinitionId(JsonUtil.getString(json, CASE_DEFINITION_ID))),
      Map.entry(CASE_DEFINITION_NAME, (query, json) -> query.caseDefinitionName(JsonUtil.getString(json, CASE_DEFINITION_NAME))),
      Map.entry(CASE_DEFINITION_NAME_LIKE, (query, json) -> query.caseDefinitionNameLike(JsonUtil.getString(json, CASE_DEFINITION_NAME_LIKE))),
      Map.entry(CASE_INSTANCE_ID, (query, json) -> query.caseInstanceId(JsonUtil.getString(json, CASE_INSTANCE_ID))),
      Map.entry(CASE_INSTANCE_BUSINESS_KEY, (query, json) -> query.caseInstanceBusinessKey(JsonUtil.getString(json, CASE_INSTANCE_BUSINESS_KEY))),
      Map.entry(CASE_INSTANCE_BUSINESS_KEY_LIKE, (query, json) -> query.caseInstanceBusinessKeyLike(JsonUtil.getString(json, CASE_INSTANCE_BUSINESS_KEY_LIKE))),
      Map.entry(CASE_EXECUTION_ID, (query, json) -> query.caseExecutionId(JsonUtil.getString(json, CASE_EXECUTION_ID))),
      Map.entry(TENANT_IDS, (query, json) -> query.tenantIdIn(getArray(JsonUtil.getArray(json, TENANT_IDS)))),
      Map.entry(WITHOUT_TENANT_ID, (query, json) -> query.withoutTenantId()),
      Map.entry(ORDER_BY, (query, json) -> query.setOrderingProperties(LEGACY_QUERY_ORDERING_PROPERTY_CONVERTER.fromOrderByString(JsonUtil.getString(json, ORDER_BY)))),
      Map.entry(ORDERING_PROPERTIES, (query, json) -> query.setOrderingProperties(JsonQueryOrderingPropertyConverter.ARRAY_CONVERTER.toObject(JsonUtil.getArray(json, ORDERING_PROPERTIES))))
  );

  protected static String[] getArray(JsonArray array) {
    return getList(array).toArray(new String[array.size()]);
  }

  protected static List<String> getList(JsonArray array) {
    List<String> list = new ArrayList<>();
    for (JsonElement entry : array) {
      list.add(JsonUtil.getString(entry));
    }
    return list;
  }

  protected static void addVariables(TaskQueryImpl query, JsonArray variables, boolean isTaskVariable, boolean isProcessVariable) {
    for (JsonElement variable : variables) {
      JsonObject variableObj = JsonUtil.getObject(variable);
      String name = JsonUtil.getString(variableObj, NAME);
      Object rawValue = JsonUtil.getRawObject(variableObj, "value");
      QueryOperator operator = QueryOperator.valueOf(JsonUtil.getString(variableObj, "operator"));
      query.addVariable(name, rawValue, operator, isTaskVariable, isProcessVariable);
    }
  }

}
