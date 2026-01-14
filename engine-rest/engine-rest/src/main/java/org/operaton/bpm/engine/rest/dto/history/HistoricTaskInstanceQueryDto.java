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
package org.operaton.bpm.engine.rest.dto.history;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.history.HistoricTaskInstanceQuery;
import org.operaton.bpm.engine.impl.HistoricTaskInstanceQueryImpl;
import org.operaton.bpm.engine.rest.dto.AbstractQueryDto;
import org.operaton.bpm.engine.rest.dto.OperatonQueryParam;
import org.operaton.bpm.engine.rest.dto.VariableQueryParameterDto;
import org.operaton.bpm.engine.rest.dto.converter.*;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;

import static org.operaton.bpm.engine.rest.dto.ConditionQueryParameterDto.*;
import static java.lang.Boolean.TRUE;

/**
 * @author Roman Smirnov
 *
 */
public class HistoricTaskInstanceQueryDto extends AbstractQueryDto<HistoricTaskInstanceQuery> {

  private static final String SORT_BY_TASK_ID= "taskId";
  private static final String SORT_BY_ACT_INSTANCE_ID = "activityInstanceId";
  private static final String SORT_BY_PROC_DEF_ID = "processDefinitionId";
  private static final String SORT_BY_PROC_INST_ID = "processInstanceId";
  private static final String SORT_BY_EXEC_ID = "executionId";
  private static final String SORT_BY_CASE_DEF_ID = "caseDefinitionId";
  private static final String SORT_BY_CASE_INST_ID = "caseInstanceId";
  private static final String SORT_BY_CASE_EXEC_ID = "caseExecutionId";
  private static final String SORT_BY_TASK_DURATION = "duration";
  private static final String SORT_BY_END_TIME = "endTime";
  private static final String SORT_BY_START_TIME = "startTime";
  private static final String SORT_BY_TASK_NAME = "taskName";
  private static final String SORT_BY_TASK_DESC = "taskDescription";
  private static final String SORT_BY_ASSIGNEE = "assignee";
  private static final String SORT_BY_OWNER = "owner";
  private static final String SORT_BY_DUE_DATE = "dueDate";
  private static final String SORT_BY_FOLLOW_UP_DATE = "followUpDate";
  private static final String SORT_BY_DELETE_REASON = "deleteReason";
  private static final String SORT_BY_TASK_DEF_KEY = "taskDefinitionKey";
  private static final String SORT_BY_PRIORITY = "priority";
  private static final String SORT_BY_TENANT_ID = "tenantId";

  private static final List<String> VALID_SORT_BY_VALUES;
  static {
    VALID_SORT_BY_VALUES = new ArrayList<>();
    VALID_SORT_BY_VALUES.add(SORT_BY_TASK_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_ACT_INSTANCE_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_PROC_DEF_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_PROC_INST_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_EXEC_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_CASE_DEF_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_CASE_INST_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_CASE_EXEC_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_TASK_DURATION);
    VALID_SORT_BY_VALUES.add(SORT_BY_TASK_DURATION);
    VALID_SORT_BY_VALUES.add(SORT_BY_END_TIME);
    VALID_SORT_BY_VALUES.add(SORT_BY_START_TIME);
    VALID_SORT_BY_VALUES.add(SORT_BY_TASK_NAME);
    VALID_SORT_BY_VALUES.add(SORT_BY_TASK_DESC);
    VALID_SORT_BY_VALUES.add(SORT_BY_ASSIGNEE);
    VALID_SORT_BY_VALUES.add(SORT_BY_OWNER);
    VALID_SORT_BY_VALUES.add(SORT_BY_DUE_DATE);
    VALID_SORT_BY_VALUES.add(SORT_BY_FOLLOW_UP_DATE);
    VALID_SORT_BY_VALUES.add(SORT_BY_DELETE_REASON);
    VALID_SORT_BY_VALUES.add(SORT_BY_TASK_DEF_KEY);
    VALID_SORT_BY_VALUES.add(SORT_BY_PRIORITY);
    VALID_SORT_BY_VALUES.add(SORT_BY_TENANT_ID);
  }

  protected String taskId;
  protected String taskParentTaskId;
  protected String processInstanceId;
  protected String rootProcessInstanceId;
  protected String processInstanceBusinessKey;
  protected String[] processInstanceBusinessKeyIn;
  protected String processInstanceBusinessKeyLike;
  protected String executionId;
  protected String[] activityInstanceIdIn;
  protected String processDefinitionId;
  protected String processDefinitionKey;
  protected String processDefinitionName;
  protected String taskName;
  protected String taskNameLike;
  protected String taskDescription;
  protected String taskDescriptionLike;
  protected String taskDefinitionKey;
  protected String[] taskDefinitionKeyIn;
  protected String taskDeleteReason;
  protected String taskDeleteReasonLike;
  protected Boolean assigned;
  protected Boolean unassigned;
  protected String taskAssignee;
  protected String taskAssigneeLike;
  protected String taskOwner;
  protected String taskOwnerLike;
  protected Integer taskPriority;
  protected Boolean finished;
  protected Boolean unfinished;
  protected Boolean processFinished;
  protected Boolean processUnfinished;
  protected Date taskDueDate;
  protected Date taskDueDateBefore;
  protected Date taskDueDateAfter;
  protected Boolean withoutTaskDueDate;
  protected Date taskFollowUpDate;
  protected Date taskFollowUpDateBefore;
  protected Date taskFollowUpDateAfter;
  protected List<String> tenantIds;
  protected Boolean withoutTenantId;

  protected Date startedBefore;
  protected Date startedAfter;
  protected Date finishedBefore;
  protected Date finishedAfter;

  protected String caseDefinitionId;
  protected String caseDefinitionKey;
  protected String caseDefinitionName;
  protected String caseInstanceId;
  protected String caseExecutionId;
  protected String taskInvolvedUser;
  protected String taskInvolvedGroup;
  protected String taskHadCandidateUser;
  protected String taskHadCandidateGroup;
  protected Boolean withCandidateGroups;
  protected Boolean withoutCandidateGroups;
  protected List<VariableQueryParameterDto> taskVariables;
  protected List<VariableQueryParameterDto> processVariables;

  protected Boolean variableValuesIgnoreCase;
  protected Boolean variableNamesIgnoreCase;

  private List<HistoricTaskInstanceQueryDto> orQueries;

  public HistoricTaskInstanceQueryDto() {}

  public HistoricTaskInstanceQueryDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
    super(objectMapper, queryParameters);
  }

  @OperatonQueryParam("orQueries")
  public void setOrQueries(List<HistoricTaskInstanceQueryDto> orQueries) {
    this.orQueries = orQueries;
  }

  @OperatonQueryParam("taskId")
  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  @OperatonQueryParam("taskParentTaskId")
  public void setTaskParentTaskId(String taskParentTaskId) {
    this.taskParentTaskId = taskParentTaskId;
  }

  @OperatonQueryParam("processInstanceId")
  public void setProcessInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  @OperatonQueryParam("rootProcessInstanceId")
  public void setRootProcessInstanceId(String rootProcessInstanceId) {
    this.rootProcessInstanceId = rootProcessInstanceId;
  }

  @OperatonQueryParam("processInstanceBusinessKey")
  public void setProcessInstanceBusinessKey(String businessKey) {
    this.processInstanceBusinessKey = businessKey;
  }

  @OperatonQueryParam(value = "processInstanceBusinessKeyIn", converter = StringArrayConverter.class)
  public void setProcessInstanceBusinessKeyIn(String[] processInstanceBusinessKeyIn) {
    this.processInstanceBusinessKeyIn = processInstanceBusinessKeyIn;
  }

  @OperatonQueryParam("processInstanceBusinessKeyLike")
  public void setProcessInstanceBusinessKeyLike(String businessKeyLike) {
    this.processInstanceBusinessKeyLike = businessKeyLike;
  }

  @OperatonQueryParam("executionId")
  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  @OperatonQueryParam(value="activityInstanceIdIn", converter=StringArrayConverter.class)
  public void setActivityInstanceIdIn(String[] activityInstanceIdIn) {
    this.activityInstanceIdIn = activityInstanceIdIn;
  }

  @OperatonQueryParam("processDefinitionId")
  public void setProcessDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  @OperatonQueryParam("processDefinitionKey")
  public void setProcessDefinitionKey(String processDefinitionKey) {
    this.processDefinitionKey = processDefinitionKey;
  }

  @OperatonQueryParam("processDefinitionName")
  public void setProcessDefinitionName(String processDefinitionName) {
    this.processDefinitionName = processDefinitionName;
  }

  @OperatonQueryParam("taskName")
  public void setTaskName(String taskName) {
    this.taskName = taskName;
  }

  @OperatonQueryParam("taskNameLike")
  public void setTaskNameLike(String taskNameLike) {
    this.taskNameLike = taskNameLike;
  }

  @OperatonQueryParam("taskDescription")
  public void setTaskDescription(String taskDescription) {
    this.taskDescription = taskDescription;
  }

  @OperatonQueryParam("taskDescriptionLike")
  public void setTaskDescriptionLike(String taskDescriptionLike) {
    this.taskDescriptionLike = taskDescriptionLike;
  }

  @OperatonQueryParam("taskDefinitionKey")
  public void setTaskDefinitionKey(String taskDefinitionKey) {
    this.taskDefinitionKey = taskDefinitionKey;
  }

  @OperatonQueryParam(value="taskDefinitionKeyIn", converter=StringArrayConverter.class)
  public void setTaskDefinitionKeyIn(String[] taskDefinitionKeyIn) {
    this.taskDefinitionKeyIn = taskDefinitionKeyIn;
  }

  @OperatonQueryParam("taskDeleteReason")
  public void setTaskDeleteReason(String taskDeleteReason) {
    this.taskDeleteReason = taskDeleteReason;
  }

  @OperatonQueryParam("taskDeleteReasonLike")
  public void setTaskDeleteReasonLike(String taskDeleteReasonLike) {
    this.taskDeleteReasonLike = taskDeleteReasonLike;
  }

  @OperatonQueryParam(value="assigned", converter=BooleanConverter.class)
  public void setAssigned(Boolean assigned) {
    this.assigned = assigned;
  }

  @OperatonQueryParam(value="unassigned", converter=BooleanConverter.class)
  public void setUnassigned(Boolean unassigned) {
    this.unassigned = unassigned;
  }

  @OperatonQueryParam("taskAssignee")
  public void setTaskAssignee(String taskAssignee) {
    this.taskAssignee = taskAssignee;
  }

  @OperatonQueryParam("taskAssigneeLike")
  public void setTaskAssigneeLike(String taskAssigneeLike) {
    this.taskAssigneeLike = taskAssigneeLike;
  }

  @OperatonQueryParam("taskOwner")
  public void setTaskOwner(String taskOwner) {
    this.taskOwner = taskOwner;
  }

  @OperatonQueryParam("taskOwnerLike")
  public void setTaskOwnerLike(String taskOwnerLike) {
    this.taskOwnerLike = taskOwnerLike;
  }

  @OperatonQueryParam(value="taskPriority", converter=IntegerConverter.class)
  public void setTaskPriority(Integer taskPriority) {
    this.taskPriority = taskPriority;
  }

  @OperatonQueryParam(value="finished", converter=BooleanConverter.class)
  public void setFinished(Boolean finished) {
    this.finished = finished;
  }

  @OperatonQueryParam(value="unfinished", converter=BooleanConverter.class)
  public void setUnfinished(Boolean unfinished) {
    this.unfinished = unfinished;
  }

  @OperatonQueryParam(value="processFinished", converter=BooleanConverter.class)
  public void setProcessFinished(Boolean processFinished) {
    this.processFinished = processFinished;
  }

  @OperatonQueryParam(value="processUnfinished", converter=BooleanConverter.class)
  public void setProcessUnfinished(Boolean processUnfinished) {
    this.processUnfinished = processUnfinished;
  }

  @OperatonQueryParam(value="taskDueDate", converter=DateConverter.class)
  public void setTaskDueDate(Date taskDueDate) {
    this.taskDueDate = taskDueDate;
  }

  @OperatonQueryParam(value="taskDueDateBefore", converter=DateConverter.class)
  public void setTaskDueDateBefore(Date taskDueDateBefore) {
    this.taskDueDateBefore = taskDueDateBefore;
  }

  @OperatonQueryParam(value="taskDueDateAfter", converter=DateConverter.class)
  public void setTaskDueDateAfter(Date taskDueDateAfter) {
    this.taskDueDateAfter = taskDueDateAfter;
  }

  @OperatonQueryParam(value = "withoutTaskDueDate", converter = BooleanConverter.class)
  public void setWithoutTaskDueDate(Boolean withoutTaskDueDate) {
    this.withoutTaskDueDate = withoutTaskDueDate;
  }

  @OperatonQueryParam(value="taskFollowUpDate", converter=DateConverter.class)
  public void setTaskFollowUpDate(Date taskFollowUpDate) {
    this.taskFollowUpDate = taskFollowUpDate;
  }

  @OperatonQueryParam(value="taskFollowUpDateBefore", converter=DateConverter.class)
  public void setTaskFollowUpDateBefore(Date taskFollowUpDateBefore) {
    this.taskFollowUpDateBefore = taskFollowUpDateBefore;
  }

  @OperatonQueryParam(value="taskFollowUpDateAfter", converter=DateConverter.class)
  public void setTaskFollowUpDateAfter(Date taskFollowUpDateAfter) {
    this.taskFollowUpDateAfter = taskFollowUpDateAfter;
  }

  @OperatonQueryParam(value="taskVariables", converter = VariableListConverter.class)
  public void setTaskVariables(List<VariableQueryParameterDto> taskVariables) {
    this.taskVariables = taskVariables;
  }

  @OperatonQueryParam(value="processVariables", converter = VariableListConverter.class)
  public void setProcessVariables(List<VariableQueryParameterDto> processVariables) {
    this.processVariables = processVariables;
  }

  @OperatonQueryParam(value="variableValuesIgnoreCase", converter = BooleanConverter.class)
  public void setVariableValuesIgnoreCase(Boolean variableValuesIgnoreCase) {
    this.variableValuesIgnoreCase = variableValuesIgnoreCase;
  }

  @OperatonQueryParam(value="variableNamesIgnoreCase", converter = BooleanConverter.class)
  public void setVariableNamesIgnoreCase(Boolean variableNamesIgnoreCase) {
    this.variableNamesIgnoreCase = variableNamesIgnoreCase;
  }

  @OperatonQueryParam("caseDefinitionId")
  public void setCaseDefinitionId(String caseDefinitionId) {
    this.caseDefinitionId = caseDefinitionId;
  }

  @OperatonQueryParam("caseDefinitionKey")
  public void setCaseDefinitionKey(String caseDefinitionKey) {
    this.caseDefinitionKey = caseDefinitionKey;
  }

  @OperatonQueryParam("caseDefinitionName")
  public void setCaseDefinitionName(String caseDefinitionName) {
    this.caseDefinitionName = caseDefinitionName;
  }

  @OperatonQueryParam("caseInstanceId")
  public void setCaseInstanceId(String caseInstanceId) {
    this.caseInstanceId = caseInstanceId;
  }

  @OperatonQueryParam("caseExecutionId")
  public void setCaseExecutionId(String caseExecutionId) {
    this.caseExecutionId = caseExecutionId;
  }

  @OperatonQueryParam(value = "tenantIdIn", converter = StringListConverter.class)
  public void setTenantIdIn(List<String> tenantIds) {
    this.tenantIds = tenantIds;
  }

  @OperatonQueryParam(value = "withoutTenantId", converter = BooleanConverter.class)
  public void setWithoutTenantId(Boolean withoutTenantId) {
    this.withoutTenantId = withoutTenantId;
  }

  @OperatonQueryParam("taskInvolvedUser")
  public void setTaskInvolvedUser(String taskInvolvedUser) {
    this.taskInvolvedUser = taskInvolvedUser;
  }

  @OperatonQueryParam("taskInvolvedGroup")
  public void setTaskInvolvedGroup(String taskInvolvedGroup) {
    this.taskInvolvedGroup = taskInvolvedGroup;
  }

  @OperatonQueryParam("taskHadCandidateUser")
  public void setTaskHadCandidateUser(String taskHadCandidateUser) {
    this.taskHadCandidateUser = taskHadCandidateUser;
  }

  @OperatonQueryParam("taskHadCandidateGroup")
  public void setTaskHadCandidateGroup(String taskHadCandidateGroup) {
    this.taskHadCandidateGroup = taskHadCandidateGroup;
  }

  @OperatonQueryParam(value="withCandidateGroups", converter=BooleanConverter.class)
  public void setWithCandidateGroups(Boolean withCandidateGroups) {
    this.withCandidateGroups = withCandidateGroups;
  }

  @OperatonQueryParam(value="withoutCandidateGroups", converter=BooleanConverter.class)
  public void setWithoutCandidateGroups(Boolean withoutCandidateGroups) {
    this.withoutCandidateGroups = withoutCandidateGroups;
  }

  @OperatonQueryParam(value="startedBefore", converter=DateConverter.class)
  public void setStartedBefore(Date startedBefore) {
    this.startedBefore = startedBefore;
  }

  @OperatonQueryParam(value="startedAfter", converter=DateConverter.class)
  public void setStartedAfter(Date startedAfter) {
    this.startedAfter = startedAfter;
  }

  @OperatonQueryParam(value="finishedBefore", converter=DateConverter.class)
  public void setFinishedBefore(Date finishedBefore) {
    this.finishedBefore = finishedBefore;
  }

  @OperatonQueryParam(value="finishedAfter", converter=DateConverter.class)
  public void setFinishedAfter(Date finishedAfter) {
    this.finishedAfter = finishedAfter;
  }

  @Override
  protected boolean isValidSortByValue(String value) {
    return VALID_SORT_BY_VALUES.contains(value);
  }

  @Override
  protected HistoricTaskInstanceQuery createNewQuery(ProcessEngine engine) {
    return engine.getHistoryService().createHistoricTaskInstanceQuery();
  }

  public List<HistoricTaskInstanceQueryDto> getOrQueries() {
    return orQueries;
  }

  @Override
  protected void applyFilters(HistoricTaskInstanceQuery query) {
    if (orQueries != null) {
      for (HistoricTaskInstanceQueryDto orQueryDto: orQueries) {
        HistoricTaskInstanceQueryImpl orQuery = new HistoricTaskInstanceQueryImpl();
        orQuery.setOrQueryActive();
        orQueryDto.applyFilters(orQuery);
        ((HistoricTaskInstanceQueryImpl) query).addOrQuery(orQuery);
      }
    }
    if (taskId != null) {
      query.taskId(taskId);
    }
    if (taskParentTaskId != null) {
      query.taskParentTaskId(taskParentTaskId);
    }
    if (processInstanceId != null) {
      query.processInstanceId(processInstanceId);
    }
    if (rootProcessInstanceId != null) {
      query.rootProcessInstanceId(rootProcessInstanceId);
    }
    if (processInstanceBusinessKey != null) {
      query.processInstanceBusinessKey(processInstanceBusinessKey);
    }
    if (processInstanceBusinessKeyIn != null && processInstanceBusinessKeyIn.length > 0) {
      query.processInstanceBusinessKeyIn(processInstanceBusinessKeyIn);
    }
    if (processInstanceBusinessKeyLike != null) {
      query.processInstanceBusinessKeyLike(processInstanceBusinessKeyLike);
    }
    if (executionId != null) {
      query.executionId(executionId);
    }
    if (activityInstanceIdIn != null && activityInstanceIdIn.length > 0 ) {
      query.activityInstanceIdIn(activityInstanceIdIn);
    }
    if (processDefinitionId != null) {
      query.processDefinitionId(processDefinitionId);
    }
    if (processDefinitionKey != null) {
      query.processDefinitionKey(processDefinitionKey);
    }
    if (processDefinitionName != null) {
      query.processDefinitionName(processDefinitionName);
    }
    if (taskName != null) {
      query.taskName(taskName);
    }
    if (taskNameLike != null) {
      query.taskNameLike(taskNameLike);
    }
    if (taskDescription != null) {
      query.taskDescription(taskDescription);
    }
    if (taskDescriptionLike != null) {
      query.taskDescriptionLike(taskDescriptionLike);
    }
    if (taskDefinitionKey != null) {
      query.taskDefinitionKey(taskDefinitionKey);
    }
    if (taskDefinitionKeyIn != null && taskDefinitionKeyIn.length > 0) {
      query.taskDefinitionKeyIn(taskDefinitionKeyIn);
    }
    if (taskDeleteReason != null) {
      query.taskDeleteReason(taskDeleteReason);
    }
    if (taskDeleteReasonLike != null) {
      query.taskDeleteReasonLike(taskDeleteReasonLike);
    }
    if (assigned != null) {
      query.taskAssigned();
    }
    if (unassigned != null) {
      query.taskUnassigned();
    }
    if (taskAssignee != null) {
      query.taskAssignee(taskAssignee);
    }
    if (taskAssigneeLike != null) {
      query.taskAssigneeLike(taskAssigneeLike);
    }
    if (taskOwner != null) {
      query.taskOwner(taskOwner);
    }
    if (taskOwnerLike != null) {
      query.taskOwnerLike(taskOwnerLike);
    }
    if (taskPriority != null) {
      query.taskPriority(taskPriority);
    }
    if (finished != null) {
      query.finished();
    }
    if (unfinished != null) {
      query.unfinished();
    }
    if (processFinished != null) {
      query.processFinished();
    }
    if (processUnfinished != null) {
      query.processUnfinished();
    }
    if (taskDueDate != null) {
      query.taskDueDate(taskDueDate);
    }
    if (taskDueDateBefore != null) {
      query.taskDueBefore(taskDueDateBefore);
    }
    if (taskDueDateAfter != null) {
      query.taskDueAfter(taskDueDateAfter);
    }
    if (TRUE.equals(withoutTaskDueDate)) {
      query.withoutTaskDueDate();
    }
    if (taskFollowUpDate != null) {
      query.taskFollowUpDate(taskFollowUpDate);
    }
    if (taskFollowUpDateBefore != null) {
      query.taskFollowUpBefore(taskFollowUpDateBefore);
    }
    if (taskFollowUpDateAfter != null) {
      query.taskFollowUpAfter(taskFollowUpDateAfter);
    }
    if (caseDefinitionId != null) {
      query.caseDefinitionId(caseDefinitionId);
    }
    if (caseDefinitionKey != null) {
      query.caseDefinitionKey(caseDefinitionKey);
    }
    if (caseDefinitionName != null) {
      query.caseDefinitionName(caseDefinitionName);
    }
    if (caseInstanceId != null) {
      query.caseInstanceId(caseInstanceId);
    }
    if (caseExecutionId != null) {
      query.caseExecutionId(caseExecutionId);
    }
    if (tenantIds != null && !tenantIds.isEmpty()) {
      query.tenantIdIn(tenantIds.toArray(new String[tenantIds.size()]));
    }
    if (TRUE.equals(withoutTenantId)) {
      query.withoutTenantId();
    }
    if(taskInvolvedUser != null){
      query.taskInvolvedUser(taskInvolvedUser);
    }
    if(taskInvolvedGroup != null){
      query.taskInvolvedGroup(taskInvolvedGroup);
    }
    if(taskHadCandidateUser != null){
      query.taskHadCandidateUser(taskHadCandidateUser);
    }
    if(taskHadCandidateGroup != null){
      query.taskHadCandidateGroup(taskHadCandidateGroup);
    }
    if (withCandidateGroups != null) {
      query.withCandidateGroups();
    }
    if (withoutCandidateGroups != null) {
      query.withoutCandidateGroups();
    }

    if (finishedAfter != null) {
      query.finishedAfter(finishedAfter);
    }

    if (finishedBefore != null) {
      query.finishedBefore(finishedBefore);
    }

    if (startedAfter != null) {
      query.startedAfter(startedAfter);
    }

    if (startedBefore != null) {
      query.startedBefore(startedBefore);
    }

    if (TRUE.equals(variableNamesIgnoreCase)) {
      query.matchVariableNamesIgnoreCase();
    }

    if (TRUE.equals(variableValuesIgnoreCase)) {
      query.matchVariableValuesIgnoreCase();
    }

    if (taskVariables != null) {
      for (VariableQueryParameterDto variableQueryParam : taskVariables) {
        String variableName = variableQueryParam.getName();
        String op = variableQueryParam.getOperator();
        Object variableValue = variableQueryParam.resolveValue(objectMapper);

        if (EQUALS_OPERATOR_NAME.equals(op)) {
          query.taskVariableValueEquals(variableName, variableValue);
        } else {
          throw new InvalidRequestException(Status.BAD_REQUEST, "Invalid variable comparator specified: %s".formatted(op));
        }
      }
    }

    if (processVariables != null) {
      for (VariableQueryParameterDto variableQueryParam : processVariables) {
        String variableName = variableQueryParam.getName();
        String op = variableQueryParam.getOperator();
        Object variableValue = variableQueryParam.resolveValue(objectMapper);

        if (EQUALS_OPERATOR_NAME.equals(op)) {
          query.processVariableValueEquals(variableName, variableValue);
        } else if (NOT_EQUALS_OPERATOR_NAME.equals(op)) {
          query.processVariableValueNotEquals(variableName, variableValue);
        } else if (GREATER_THAN_OPERATOR_NAME.equals(op)) {
          query.processVariableValueGreaterThan(variableName, variableValue);
        } else if (GREATER_THAN_OR_EQUALS_OPERATOR_NAME.equals(op)) {
          query.processVariableValueGreaterThanOrEquals(variableName, variableValue);
        } else if (LESS_THAN_OPERATOR_NAME.equals(op)) {
          query.processVariableValueLessThan(variableName, variableValue);
        } else if (LESS_THAN_OR_EQUALS_OPERATOR_NAME.equals(op)) {
          query.processVariableValueLessThanOrEquals(variableName, variableValue);
        } else if (LIKE_OPERATOR_NAME.equals(op)) {
          query.processVariableValueLike(variableName, String.valueOf(variableValue));
        } else if (NOT_LIKE_OPERATOR_NAME.equals(op)) {
          query.processVariableValueNotLike(variableName, String.valueOf(variableValue));
        } else {
          throw new InvalidRequestException(Status.BAD_REQUEST, "Invalid process variable comparator specified: %s".formatted(op));
        }
      }
    }
  }

  @Override
  protected void applySortBy(HistoricTaskInstanceQuery query, String sortBy, Map<String, Object> parameters, ProcessEngine engine) {
    if (SORT_BY_TASK_ID.equals(sortBy)) {
      query.orderByTaskId();
    } else if (SORT_BY_ACT_INSTANCE_ID.equals(sortBy)) {
      query.orderByHistoricActivityInstanceId();
    } else if (SORT_BY_PROC_DEF_ID.equals(sortBy)) {
      query.orderByProcessDefinitionId();
    } else if (SORT_BY_PROC_INST_ID.equals(sortBy)) {
      query.orderByProcessInstanceId();
    } else if (SORT_BY_EXEC_ID.equals(sortBy)) {
      query.orderByExecutionId();
    } else if (SORT_BY_TASK_DURATION.equals(sortBy)) {
      query.orderByHistoricTaskInstanceDuration();
    } else if (SORT_BY_END_TIME.equals(sortBy)) {
      query.orderByHistoricTaskInstanceEndTime();
    } else if (SORT_BY_START_TIME.equals(sortBy)) {
      query.orderByHistoricActivityInstanceStartTime();
    } else if (SORT_BY_TASK_NAME.equals(sortBy)) {
      query.orderByTaskName();
    } else if (SORT_BY_TASK_DESC.equals(sortBy)) {
      query.orderByTaskDescription();
    } else if (SORT_BY_ASSIGNEE.equals(sortBy)) {
      query.orderByTaskAssignee();
    } else if (SORT_BY_OWNER.equals(sortBy)) {
      query.orderByTaskOwner();
    } else if (SORT_BY_DUE_DATE.equals(sortBy)) {
      query.orderByTaskDueDate();
    } else if (SORT_BY_FOLLOW_UP_DATE.equals(sortBy)) {
      query.orderByTaskFollowUpDate();
    } else if (SORT_BY_DELETE_REASON.equals(sortBy)) {
      query.orderByDeleteReason();
    } else if (SORT_BY_TASK_DEF_KEY.equals(sortBy)) {
      query.orderByTaskDefinitionKey();
    } else if (SORT_BY_PRIORITY.equals(sortBy)) {
      query.orderByTaskPriority();
    } else if (SORT_BY_CASE_DEF_ID.equals(sortBy)) {
      query.orderByCaseDefinitionId();
    } else if (SORT_BY_CASE_INST_ID.equals(sortBy)) {
      query.orderByCaseInstanceId();
    } else if (SORT_BY_CASE_EXEC_ID.equals(sortBy)) {
      query.orderByCaseExecutionId();
    } else if (SORT_BY_TENANT_ID.equals(sortBy)) {
      query.orderByTenantId();
    }
  }

}

