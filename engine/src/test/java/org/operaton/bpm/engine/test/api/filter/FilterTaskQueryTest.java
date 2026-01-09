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
package org.operaton.bpm.engine.test.api.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.FilterService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.filter.Filter;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.Direction;
import org.operaton.bpm.engine.impl.QueryEntityRelationCondition;
import org.operaton.bpm.engine.impl.QueryOperator;
import org.operaton.bpm.engine.impl.QueryOrderingProperty;
import org.operaton.bpm.engine.impl.TaskQueryImpl;
import org.operaton.bpm.engine.impl.TaskQueryProperty;
import org.operaton.bpm.engine.impl.TaskQueryVariableValue;
import org.operaton.bpm.engine.impl.VariableOrderProperty;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.json.JsonTaskQueryConverter;
import org.operaton.bpm.engine.impl.mock.Mocks;
import org.operaton.bpm.engine.impl.persistence.entity.FilterEntity;
import org.operaton.bpm.engine.impl.persistence.entity.SuspensionState;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.DelegationState;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Sebastian Menski
 */
class FilterTaskQueryTest {

  @RegisterExtension
  protected static ProcessEngineExtension engine = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engine);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected FilterService filterService;
  protected TaskService taskService;
  protected IdentityService identityService;
  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;
  protected CaseService caseService;

  protected Filter testFilter;

  protected String testString = "test";
  protected Integer testInteger = 1;
  protected DelegationState testDelegationState = DelegationState.PENDING;
  protected Date testDate = new Date(0);
  protected String[] testActivityInstances = new String[] {"a", "b", "c"};
  protected String[] testKeys = new String[] {"d", "e"};
  protected List<String> testCandidateGroups = new ArrayList<>();
  protected String[] testInstances = new String[] {"x", "y", "z"};

  protected String[] variableNames = new String[] {"a", "b", "c", "d", "e", "f", "g", "h", "i"};
  protected Object[] variableValues = new Object[] {1, 2, "3", "4", "5", 6, 7, "8", "9"};
  protected QueryOperator[] variableOperators = new QueryOperator[] {
      QueryOperator.EQUALS,
      QueryOperator.GREATER_THAN_OR_EQUAL,
      QueryOperator.LESS_THAN,
      QueryOperator.LIKE,
      QueryOperator.NOT_LIKE,
      QueryOperator.NOT_EQUALS,
      QueryOperator.LESS_THAN_OR_EQUAL,
      QueryOperator.LIKE,
      QueryOperator.NOT_LIKE
  };
  protected boolean[] isTaskVariable = new boolean[] {true, true, false, false, false, false, false, false, false};
  protected boolean[] isProcessVariable = new boolean[] {false, false, true, true, true, false, false, false, false};
  protected User testUser;
  protected Group testGroup;

  protected JsonTaskQueryConverter queryConverter;

  @BeforeEach
  void setUp() {
    testFilter = filterService.newTaskFilter("name")
        .setOwner("owner")
        .setQuery(taskService.createTaskQuery())
        .setProperties(new HashMap<>());
    testUser = identityService.newUser("user");
    testGroup = identityService.newGroup("group");
    identityService.saveUser(testUser);
    identityService.saveGroup(testGroup);
    identityService.createMembership(testUser.getId(), testGroup.getId());

    Group anotherGroup = identityService.newGroup("anotherGroup");
    identityService.saveGroup(anotherGroup);
    testCandidateGroups.add(testGroup.getId());
    testCandidateGroups.add(anotherGroup.getId());

    createTasks();

    queryConverter = new JsonTaskQueryConverter();
  }

  @AfterEach
  void tearDown() {
    processEngineConfiguration.setEnableExpressionsInAdhocQueries(false);

    Mocks.reset();

    for (Filter filter : filterService.createTaskFilterQuery().list()) {
      filterService.deleteFilter(filter.getId());
    }
    for (Group group : identityService.createGroupQuery().list()) {
      identityService.deleteGroup(group.getId());
    }
    for (User user : identityService.createUserQuery().list()) {
      identityService.deleteUser(user.getId());
    }
    for (Task task : taskService.createTaskQuery().list()) {
      if (task.getProcessInstanceId() == null) {
        taskService.deleteTask(task.getId(), true);
      }
    }
  }

  @Test
  void testEmptyQuery() {
    TaskQuery emptyQuery = taskService.createTaskQuery();
    String emptyQueryJson = "{}";

    testFilter.setQuery(emptyQuery);

    assertThat(((FilterEntity) testFilter).getQueryInternal()).isEqualTo(emptyQueryJson);
    assertThat(testFilter.<TaskQuery>getQuery()).isNotNull();
  }

  @Test
  void testTaskQuery() {
    // create query
    TaskQueryImpl query = new TaskQueryImpl();
    query.taskId(testString);
    query.taskName(testString);
    query.taskNameNotEqual(testString);
    query.taskNameLike(testString);
    query.taskNameNotLike(testString);
    query.taskDescription(testString);
    query.taskDescriptionLike(testString);
    query.taskPriority(testInteger);
    query.taskMinPriority(testInteger);
    query.taskMaxPriority(testInteger);
    query.taskAssignee(testString);
    query.taskAssigneeExpression(testString);
    query.taskAssigneeLike(testString);
    query.taskAssigneeLikeExpression(testString);
    query.taskAssigneeIn(testString);
    query.taskAssigneeNotIn(testString);
    query.taskInvolvedUser(testString);
    query.taskInvolvedUserExpression(testString);
    query.taskOwner(testString);
    query.taskOwnerExpression(testString);
    query.taskUnassigned();
    query.taskAssigned();
    query.taskDelegationState(testDelegationState);
    query.taskCandidateGroupLike(testString);
    query.taskCandidateGroupIn(testCandidateGroups);
    query.taskCandidateGroupInExpression(testString);
    query.withCandidateGroups();
    query.withoutCandidateGroups();
    query.withCandidateUsers();
    query.withoutCandidateUsers();
    query.processInstanceId(testString);
    query.processInstanceIdIn(testInstances);
    query.executionId(testString);
    query.activityInstanceIdIn(testActivityInstances);
    query.taskCreatedOn(testDate);
    query.taskCreatedOnExpression(testString);
    query.taskCreatedBefore(testDate);
    query.taskCreatedBeforeExpression(testString);
    query.taskCreatedAfter(testDate);
    query.taskCreatedAfterExpression(testString);
    query.taskUpdatedAfter(testDate);
    query.taskUpdatedAfterExpression(testString);
    query.taskDefinitionKey(testString);
    query.taskDefinitionKeyIn(testKeys);
    query.taskDefinitionKeyNotIn(testKeys);
    query.taskDefinitionKeyLike(testString);
    query.processDefinitionKey(testString);
    query.processDefinitionKeyIn(testKeys);
    query.processDefinitionId(testString);
    query.processDefinitionName(testString);
    query.processDefinitionNameLike(testString);
    query.processInstanceBusinessKey(testString);
    query.processInstanceBusinessKeyExpression(testString);
    query.processInstanceBusinessKeyIn(testKeys);
    query.processInstanceBusinessKeyLike(testString);
    query.processInstanceBusinessKeyLikeExpression(testString);

    // variables
    query.taskVariableValueEquals(variableNames[0], variableValues[0]);
    query.taskVariableValueGreaterThanOrEquals(variableNames[1], variableValues[1]);
    query.processVariableValueLessThan(variableNames[2], variableValues[2]);
    query.processVariableValueLike(variableNames[3], (String) variableValues[3]);
    query.processVariableValueNotLike(variableNames[4], (String) variableValues[4]);
    query.caseInstanceVariableValueNotEquals(variableNames[5], variableValues[5]);
    query.caseInstanceVariableValueLessThanOrEquals(variableNames[6], variableValues[6]);
    query.caseInstanceVariableValueLike(variableNames[7], (String) variableValues[7]);
    query.caseInstanceVariableValueNotLike(variableNames[8], (String) variableValues[8]);

    query.dueDate(testDate);
    query.dueDateExpression(testString);
    query.dueBefore(testDate);
    query.dueBeforeExpression(testString);
    query.dueAfter(testDate);
    query.dueAfterExpression(testString);
    query.followUpDate(testDate);
    query.followUpDateExpression(testString);
    query.followUpBefore(testDate);
    query.followUpBeforeExpression(testString);
    query.followUpAfter(testDate);
    query.followUpAfterExpression(testString);
    query.excludeSubtasks();
    query.suspended();
    query.caseDefinitionKey(testString);
    query.caseDefinitionId(testString);
    query.caseDefinitionName(testString);
    query.caseDefinitionNameLike(testString);
    query.caseInstanceId(testString);
    query.caseInstanceBusinessKey(testString);
    query.caseInstanceBusinessKeyLike(testString);
    query.caseExecutionId(testString);

    // ordering
    query.orderByExecutionId().desc();
    query.orderByDueDate().asc();
    query.orderByProcessVariable("var", ValueType.STRING).desc();

    List<QueryOrderingProperty> expectedOrderingProperties = query.getOrderingProperties();

    // save filter
    testFilter.setQuery(query);
    filterService.saveFilter(testFilter);

    // fetch from db
    testFilter = filterService.createTaskFilterQuery().singleResult();

    // test query
    query = testFilter.getQuery();
    assertThat(query.getTaskId()).isEqualTo(testString);
    assertThat(query.getName()).isEqualTo(testString);
    assertThat(query.getNameNotEqual()).isEqualTo(testString);
    assertThat(query.getNameNotLike()).isEqualTo(testString);
    assertThat(query.getNameLike()).isEqualTo(testString);
    assertThat(query.getDescription()).isEqualTo(testString);
    assertThat(query.getDescriptionLike()).isEqualTo(testString);
    assertThat(query.getPriority()).isEqualTo(testInteger);
    assertThat(query.getMinPriority()).isEqualTo(testInteger);
    assertThat(query.getMaxPriority()).isEqualTo(testInteger);
    assertThat(query.getAssignee()).isEqualTo(testString);
    assertThat(query.getExpressions()).containsEntry("taskAssignee", testString);
    assertThat(query.getAssigneeLike()).isEqualTo(testString);
    assertThat(query.getExpressions()).containsEntry("taskAssigneeLike", testString);
    assertThat(query.getAssigneeIn()).contains(testString);
    assertThat(query.getAssigneeNotIn()).contains(testString);
    assertThat(query.getInvolvedUser()).isEqualTo(testString);
    assertThat(query.getExpressions()).containsEntry("taskInvolvedUser", testString);
    assertThat(query.getOwner()).isEqualTo(testString);
    assertThat(query.getExpressions()).containsEntry("taskOwner", testString);
    assertThat(query.isUnassigned()).isTrue();
    assertThat(query.isAssigned()).isTrue();
    assertThat(query.getDelegationState()).isEqualTo(testDelegationState);
    assertThat(query.getCandidateGroupLike()).isEqualTo(testString);
    assertThat(query.getCandidateGroups()).isEqualTo(testCandidateGroups);
    assertThat(query.isWithCandidateGroups()).isTrue();
    assertThat(query.isWithoutCandidateGroups()).isTrue();
    assertThat(query.isWithCandidateUsers()).isTrue();
    assertThat(query.isWithoutCandidateUsers()).isTrue();
    assertThat(query.getExpressions()).containsEntry("taskCandidateGroupIn", testString);
    assertThat(query.getProcessInstanceId()).isEqualTo(testString);
    assertThat(query.getProcessInstanceIdIn()).hasSameSizeAs(testInstances);
    for (int i = 0; i < query.getProcessInstanceIdIn().length; i++) {
      assertThat(query.getProcessInstanceIdIn()[i]).isEqualTo(testInstances[i]);
    }
    assertThat(query.getExecutionId()).isEqualTo(testString);
    assertThat(query.getActivityInstanceIdIn()).hasSameSizeAs(testActivityInstances);
    for (int i = 0; i < query.getActivityInstanceIdIn().length; i++) {
      assertThat(query.getActivityInstanceIdIn()[i]).isEqualTo(testActivityInstances[i]);
    }
    assertThat(query.getCreateTime()).isEqualTo(testDate);
    assertThat(query.getExpressions()).containsEntry("taskCreatedOn", testString);
    assertThat(query.getCreateTimeBefore()).isEqualTo(testDate);
    assertThat(query.getExpressions()).containsEntry("taskCreatedBefore", testString);
    assertThat(query.getCreateTimeAfter()).isEqualTo(testDate);
    assertThat(query.getExpressions()).containsEntry("taskCreatedAfter", testString);
    assertThat(query.getUpdatedAfter()).isEqualTo(testDate);
    assertThat(query.getExpressions()).containsEntry("taskUpdatedAfter", testString);
    assertThat(query.getKey()).isEqualTo(testString);
    assertThat(query.getKeys()).hasSameSizeAs(testKeys);
    for (int i = 0; i < query.getKeys().length; i++) {
      assertThat(query.getKeys()[i]).isEqualTo(testKeys[i]);
    }
    assertThat(query.getKeyNotIn()).hasSameSizeAs(testKeys);
    for (int i = 0; i < query.getKeyNotIn().length; i++) {
      assertThat(query.getKeyNotIn()[i]).isEqualTo(testKeys[i]);
    }
    assertThat(query.getKeyLike()).isEqualTo(testString);
    assertThat(query.getProcessDefinitionKey()).isEqualTo(testString);
    for (int i = 0; i < query.getProcessDefinitionKeys().length; i++) {
      assertThat(query.getProcessDefinitionKeys()[i]).isEqualTo(testKeys[i]);
    }
    assertThat(query.getProcessDefinitionId()).isEqualTo(testString);
    assertThat(query.getProcessDefinitionName()).isEqualTo(testString);
    assertThat(query.getProcessDefinitionNameLike()).isEqualTo(testString);
    assertThat(query.getProcessInstanceBusinessKey()).isEqualTo(testString);
    assertThat(query.getExpressions()).containsEntry("processInstanceBusinessKey", testString);
    for (int i = 0; i < query.getProcessInstanceBusinessKeys().length; i++) {
      assertThat(query.getProcessInstanceBusinessKeys()[i]).isEqualTo(testKeys[i]);
    }
    assertThat(query.getProcessInstanceBusinessKeyLike()).isEqualTo(testString);
    assertThat(query.getExpressions()).containsEntry("processInstanceBusinessKeyLike", testString);

    // variables
    List<TaskQueryVariableValue> variables = query.getVariables();
    for (int i = 0; i < variables.size(); i++) {
      TaskQueryVariableValue variable = variables.get(i);
      assertThat(variable.getName()).isEqualTo(variableNames[i]);
      assertThat(variable.getValue()).isEqualTo(variableValues[i]);
      assertThat(variable.getOperator()).isEqualTo(variableOperators[i]);
      assertThat(variable.isLocal()).isEqualTo(isTaskVariable[i]);
      assertThat(variable.isProcessInstanceVariable()).isEqualTo(isProcessVariable[i]);
    }

    assertThat(query.getDueDate()).isEqualTo(testDate);
    assertThat(query.getExpressions()).containsEntry("dueDate", testString);
    assertThat(query.getDueBefore()).isEqualTo(testDate);
    assertThat(query.getExpressions()).containsEntry("dueBefore", testString);
    assertThat(query.getDueAfter()).isEqualTo(testDate);
    assertThat(query.getExpressions()).containsEntry("dueAfter", testString);
    assertThat(query.getFollowUpDate()).isEqualTo(testDate);
    assertThat(query.getExpressions()).containsEntry("followUpDate", testString);
    assertThat(query.getFollowUpBefore()).isEqualTo(testDate);
    assertThat(query.getExpressions()).containsEntry("followUpBefore", testString);
    assertThat(query.getFollowUpAfter()).isEqualTo(testDate);
    assertThat(query.getExpressions()).containsEntry("followUpAfter", testString);
    assertThat(query.isExcludeSubtasks()).isTrue();
    assertThat(query.getSuspensionState()).isEqualTo(SuspensionState.SUSPENDED);
    assertThat(query.getCaseDefinitionKey()).isEqualTo(testString);
    assertThat(query.getCaseDefinitionId()).isEqualTo(testString);
    assertThat(query.getCaseDefinitionName()).isEqualTo(testString);
    assertThat(query.getCaseDefinitionNameLike()).isEqualTo(testString);
    assertThat(query.getCaseInstanceId()).isEqualTo(testString);
    assertThat(query.getCaseInstanceBusinessKey()).isEqualTo(testString);
    assertThat(query.getCaseInstanceBusinessKeyLike()).isEqualTo(testString);
    assertThat(query.getCaseExecutionId()).isEqualTo(testString);

    // ordering
    verifyOrderingProperties(expectedOrderingProperties, query.getOrderingProperties());
  }

  protected void verifyOrderingProperties(List<QueryOrderingProperty> expectedProperties,
      List<QueryOrderingProperty> actualProperties) {
    assertThat(actualProperties).hasSize(expectedProperties.size());

    for (int i = 0; i < expectedProperties.size(); i++) {
      QueryOrderingProperty expectedProperty = expectedProperties.get(i);
      QueryOrderingProperty actualProperty = actualProperties.get(i);

      assertThat(actualProperty.getRelation()).isEqualTo(expectedProperty.getRelation());
      assertThat(actualProperty.getDirection()).isEqualTo(expectedProperty.getDirection());
      assertThat(actualProperty.isContainedProperty()).isEqualTo(expectedProperty.isContainedProperty());
      assertThat(actualProperty.getQueryProperty()).isEqualTo(expectedProperty.getQueryProperty());

      List<QueryEntityRelationCondition> expectedRelationConditions = expectedProperty.getRelationConditions();
      List<QueryEntityRelationCondition> actualRelationConditions = expectedProperty.getRelationConditions();

      if (expectedRelationConditions != null && actualRelationConditions != null) {
        assertThat(actualRelationConditions).hasSize(expectedRelationConditions.size());

        for (QueryEntityRelationCondition expectedFilteringProperty : expectedRelationConditions) {
          QueryEntityRelationCondition actualFilteringProperty = expectedFilteringProperty;

          assertThat(actualFilteringProperty.getProperty()).isEqualTo(expectedFilteringProperty.getProperty());
          assertThat(actualFilteringProperty.getComparisonProperty()).isEqualTo(expectedFilteringProperty.getComparisonProperty());
          assertThat(actualFilteringProperty.getScalarValue()).isEqualTo(expectedFilteringProperty.getScalarValue());
        }
      } else if ((expectedRelationConditions == null && actualRelationConditions != null) ||
          (expectedRelationConditions != null && actualRelationConditions == null)) {
        fail("Expected filtering properties: %s. Actual filtering properties: %s".formatted(expectedRelationConditions, actualRelationConditions));
      }
    }
  }

  @Test
  void testTaskQueryByBusinessKeyExpression() {
    // given
    String aBusinessKey = "business key";
    Mocks.register("aBusinessKey", aBusinessKey);

    createDeploymentWithBusinessKey(aBusinessKey);

    // when
    TaskQueryImpl extendedQuery = (TaskQueryImpl)taskService.createTaskQuery()
      .processInstanceBusinessKeyExpression("${ " + Mocks.getMocks().keySet().toArray()[0] + " }");

    Filter filter = filterService.newTaskFilter("aFilterName");
    filter.setQuery(extendedQuery);
    filterService.saveFilter(filter);

    TaskQueryImpl filterQuery = filterService.getFilter(filter.getId()).getQuery();

    // then
    assertThat(filterQuery.getExpressions()).containsEntry("processInstanceBusinessKey", extendedQuery.getExpressions().get("processInstanceBusinessKey"));
    assertThat(filterService.list(filter.getId())).hasSize(1);
  }

  @Test
  void testTaskQueryByBusinessKeyExpressionInAdhocQuery() {
    // given
    processEngineConfiguration.setEnableExpressionsInAdhocQueries(true);

    String aBusinessKey = "business key";
    Mocks.register("aBusinessKey", aBusinessKey);

    createDeploymentWithBusinessKey(aBusinessKey);

    // when
    Filter filter = filterService.newTaskFilter("aFilterName");
    filter.setQuery(taskService.createTaskQuery());
    filterService.saveFilter(filter);

    TaskQueryImpl extendingQuery = (TaskQueryImpl)taskService.createTaskQuery()
      .processInstanceBusinessKeyExpression("${ " + Mocks.getMocks().keySet().toArray()[0] + " }");

    // then
    assertThat("${ " + Mocks.getMocks().keySet().toArray()[0] + " }").isEqualTo(extendingQuery.getExpressions().get("processInstanceBusinessKey"));
    assertThat(filterService.list(filter.getId(), extendingQuery)).hasSize(1);
  }

  @Test
  void testTaskQueryByBusinessKeyLikeExpression() {
    // given
    String aBusinessKey = "business key";
    Mocks.register("aBusinessKeyLike", "%" + aBusinessKey.substring(5));

    createDeploymentWithBusinessKey(aBusinessKey);

    // when
    TaskQueryImpl extendedQuery = (TaskQueryImpl)taskService.createTaskQuery()
      .processInstanceBusinessKeyLikeExpression("${ " + Mocks.getMocks().keySet().toArray()[0] + " }");

    Filter filter = filterService.newTaskFilter("aFilterName");
    filter.setQuery(extendedQuery);
    filterService.saveFilter(filter);

    TaskQueryImpl filterQuery = filterService.getFilter(filter.getId()).getQuery();

    // then
    assertThat(filterQuery.getExpressions()).containsEntry("processInstanceBusinessKeyLike", extendedQuery.getExpressions().get("processInstanceBusinessKeyLike"));
    assertThat(filterService.list(filter.getId())).hasSize(1);
  }

  @Test
  void testTaskQueryByBusinessKeyLikeExpressionInAdhocQuery() {
    // given
    processEngineConfiguration.setEnableExpressionsInAdhocQueries(true);

    String aBusinessKey = "business key";
    Mocks.register("aBusinessKeyLike", "%" + aBusinessKey.substring(5));

    createDeploymentWithBusinessKey(aBusinessKey);

    // when
    Filter filter = filterService.newTaskFilter("aFilterName");
    filter.setQuery(taskService.createTaskQuery());
    filterService.saveFilter(filter);

    TaskQueryImpl extendingQuery = (TaskQueryImpl)taskService.createTaskQuery()
      .processInstanceBusinessKeyLikeExpression("${ " + Mocks.getMocks().keySet().toArray()[0] + " }");

    // then
    assertThat("${ " + Mocks.getMocks().keySet().toArray()[0] + " }").isEqualTo(extendingQuery.getExpressions().get("processInstanceBusinessKeyLike"));
    assertThat(filterService.list(filter.getId(), extendingQuery)).hasSize(1);
  }

  protected void createDeploymentWithBusinessKey(String aBusinessKey) {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("aProcessDefinition")
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .userTask()
        .endEvent()
        .done();

   testRule.deploy(modelInstance);

    runtimeService.startProcessInstanceByKey("aProcessDefinition", aBusinessKey);
  }

  @Test
  void testTaskQueryByFollowUpBeforeOrNotExistent() {
    // create query
    TaskQueryImpl query = new TaskQueryImpl();

    query.followUpBeforeOrNotExistent(testDate);

    // save filter
    testFilter.setQuery(query);
    filterService.saveFilter(testFilter);

    // fetch from db
    testFilter = filterService.createTaskFilterQuery().singleResult();

    // test query
    query = testFilter.getQuery();
    assertThat(query.isFollowUpNullAccepted()).isTrue();
    assertThat(query.getFollowUpBefore()).isEqualTo(testDate);
  }

  @Test
  void testTaskQueryByFollowUpBeforeOrNotExistentExtendingQuery() {
    // create query
    TaskQueryImpl query = new TaskQueryImpl();

    query.followUpBeforeOrNotExistent(testDate);

    // save filter without query
    filterService.saveFilter(testFilter);

    // fetch from db
    testFilter = filterService.createTaskFilterQuery().singleResult();

    // use query as extending query
    List<Task> tasks = filterService.list(testFilter.getId(), query);
    assertThat(tasks).hasSize(3);

    // set as filter query and save filter
    testFilter.setQuery(query);
    filterService.saveFilter(testFilter);

    // fetch from db
    testFilter = filterService.createTaskFilterQuery().singleResult();

    tasks = filterService.list(testFilter.getId());
    assertThat(tasks).hasSize(3);

    TaskQuery extendingQuery = taskService.createTaskQuery();

    extendingQuery
      .orderByTaskCreateTime()
      .asc();

    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks).hasSize(3);
  }

  @Test
  void testTaskQueryByFollowUpBeforeOrNotExistentExpression() {
    // create query
    TaskQueryImpl query = new TaskQueryImpl();

    query.followUpBeforeOrNotExistentExpression(testString);

    // save filter
    testFilter.setQuery(query);
    filterService.saveFilter(testFilter);

    // fetch from db
    testFilter = filterService.createTaskFilterQuery().singleResult();

    // test query
    query = testFilter.getQuery();
    assertThat(query.isFollowUpNullAccepted()).isTrue();
    assertThat(query.getExpressions()).containsEntry("followUpBeforeOrNotExistent", testString);
  }

  @Test
  void testTaskQueryByFollowUpBeforeOrNotExistentExpressionExtendingQuery() {
    // create query
    TaskQueryImpl query = new TaskQueryImpl();

    query.followUpBeforeOrNotExistentExpression("${dateTime().withMillis(0)}");

    // save filter without query
    filterService.saveFilter(testFilter);

    // fetch from db
    testFilter = filterService.createTaskFilterQuery().singleResult();

    // use query as extending query
    List<Task> tasks = filterService.list(testFilter.getId(), query);
    assertThat(tasks).hasSize(3);

    // set as filter query and save filter
    testFilter.setQuery(query);
    filterService.saveFilter(testFilter);

    // fetch from db
    testFilter = filterService.createTaskFilterQuery().singleResult();

    tasks = filterService.list(testFilter.getId());
    assertThat(tasks).hasSize(3);

    TaskQuery extendingQuery = taskService.createTaskQuery();

    extendingQuery
      .orderByTaskCreateTime()
      .asc();

    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks).hasSize(3);
  }

  @Test
  void testTaskQueryCandidateUser() {
    TaskQueryImpl query = new TaskQueryImpl();
    query.taskCandidateUser(testUser.getId());
    query.taskCandidateUserExpression(testUser.getId());

    testFilter.setQuery(query);
    query = testFilter.getQuery();

    assertThat(query.getCandidateUser()).isEqualTo(testUser.getId());
    assertThat(query.getExpressions()).containsEntry("taskCandidateUser", testUser.getId());
  }

  @Test
  void testTaskQueryCandidateGroup() {
    TaskQueryImpl query = new TaskQueryImpl();
    query.taskCandidateGroup(testGroup.getId());
    query.taskCandidateGroupExpression(testGroup.getId());

    testFilter.setQuery(query);
    query = testFilter.getQuery();

    assertThat(query.getCandidateGroup()).isEqualTo(testGroup.getId());
    assertThat(query.getExpressions()).containsEntry("taskCandidateGroup", testGroup.getId());
  }

  @Test
  void testTaskQueryCandidateGroupLike() {
    // given
    TaskQueryImpl query = new TaskQueryImpl();
    query.taskCandidateGroupLike(testGroup.getId());

    saveQuery(query);

    // when
    query = testFilter.getQuery();

    // then
    assertThat(query.getCandidateGroupLike()).isEqualTo(testGroup.getId());
  }

  @Test
  void testTaskQueryCandidateUserIncludeAssignedTasks() {
    TaskQueryImpl query = new TaskQueryImpl();
    query.taskCandidateUser(testUser.getId());
    query.includeAssignedTasks();

    saveQuery(query);
    query = filterService.getFilter(testFilter.getId()).getQuery();

    assertThat(query.getCandidateUser()).isEqualTo(testUser.getId());
    assertThat(query.isIncludeAssignedTasks()).isTrue();
  }

  @Test
  void testTaskQueryCandidateUserExpressionIncludeAssignedTasks() {
    TaskQueryImpl query = new TaskQueryImpl();
    query.taskCandidateUserExpression(testString);
    query.includeAssignedTasks();

    saveQuery(query);
    query = filterService.getFilter(testFilter.getId()).getQuery();

    assertThat(query.getExpressions()).containsEntry("taskCandidateUser", testString);
    assertThat(query.isIncludeAssignedTasks()).isTrue();
  }

  @Test
  void testTaskQueryCandidateGroupIncludeAssignedTasks() {
    TaskQueryImpl query = new TaskQueryImpl();
    query.taskCandidateGroup(testGroup.getId());
    query.includeAssignedTasks();

    saveQuery(query);
    query = filterService.getFilter(testFilter.getId()).getQuery();

    assertThat(query.getCandidateGroup()).isEqualTo(testGroup.getId());
    assertThat(query.isIncludeAssignedTasks()).isTrue();
  }

  @Test
  void testTaskQueryCandidateGroupExpressionIncludeAssignedTasks() {
    TaskQueryImpl query = new TaskQueryImpl();
    query.taskCandidateGroupExpression(testString);
    query.includeAssignedTasks();

    saveQuery(query);
    query = filterService.getFilter(testFilter.getId()).getQuery();

    assertThat(query.getExpressions()).containsEntry("taskCandidateGroup", testString);
    assertThat(query.isIncludeAssignedTasks()).isTrue();
  }

  @Test
  void testTaskQueryCandidateGroupLikeIncludeAssignedTasks() {
    // given
    TaskQueryImpl query = new TaskQueryImpl();
    query.taskCandidateGroupLike(testGroup.getId());
    query.includeAssignedTasks();

    saveQuery(query);

    // when
    query = testFilter.getQuery();

    // then
    assertThat(query.getCandidateGroupLike()).isEqualTo(testGroup.getId());
    assertThat(query.isIncludeAssignedTasks()).isTrue();
  }

  @Test
  void testTaskQueryCandidateGroupsIncludeAssignedTasks() {
    TaskQueryImpl query = new TaskQueryImpl();
    query.taskCandidateGroupIn(testCandidateGroups);
    query.includeAssignedTasks();

    saveQuery(query);
    query = filterService.getFilter(testFilter.getId()).getQuery();

    assertThat(query.getCandidateGroupsInternal()).isEqualTo(testCandidateGroups);
    assertThat(query.isIncludeAssignedTasks()).isTrue();
  }

  @Test
  void testTaskQueryCandidateGroupsExpressionIncludeAssignedTasks() {
    TaskQueryImpl query = new TaskQueryImpl();
    query.taskCandidateGroupInExpression(testString);
    query.includeAssignedTasks();

    saveQuery(query);
    query = filterService.getFilter(testFilter.getId()).getQuery();

    assertThat(query.getExpressions()).containsEntry("taskCandidateGroupIn", testString);
    assertThat(query.isIncludeAssignedTasks()).isTrue();
  }

  @Test
  void testExecuteTaskQueryList() {
    TaskQuery query = taskService.createTaskQuery();
    query.taskNameLike("Task%");

    saveQuery(query);

    List<Task> tasks = filterService.list(testFilter.getId());
    assertThat(tasks).hasSize(3);
    for (Task task : tasks) {
      assertThat(task.getOwner()).isEqualTo(testUser.getId());
    }
  }

  @Test
  void testExtendingTaskQueryList() {
    TaskQuery query = taskService.createTaskQuery();

    saveQuery(query);

    List<Task> tasks = filterService.list(testFilter.getId());
    assertThat(tasks).hasSize(3);

    tasks = filterService.list(testFilter.getId(), query);
    assertThat(tasks).hasSize(3);

    TaskQuery extendingQuery = taskService.createTaskQuery();

    extendingQuery.taskDelegationState(DelegationState.RESOLVED);

    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks).hasSize(2);

    for (Task task : tasks) {
      assertThat(task.getDelegationState()).isEqualTo(DelegationState.RESOLVED);
    }
  }

  @Test
  void testExtendingTaskQueryWithAssigneeIn() {
    // given
    Task task = taskService.newTask("assigneeTask");
    task.setName("Task 4");
    task.setOwner(testUser.getId());
    taskService.saveTask(task);
    taskService.setAssignee(task.getId(), "john");

    // then
    TaskQuery query = taskService.createTaskQuery().taskAssigneeIn("john");
    saveQuery(query);
    List<Task> origQueryTasks = filterService.list(testFilter.getId());
    List<Task> selfExtendQueryTasks = filterService.list(testFilter.getId(), query);

    TaskQuery extendingQuery = taskService.createTaskQuery();
    extendingQuery.taskAssigneeIn("john", "kermit");
    List<Task> extendingQueryTasks = filterService.list(testFilter.getId(), extendingQuery);

    // then
    assertThat(origQueryTasks).hasSize(1);
    assertThat(selfExtendQueryTasks).hasSize(1);
    assertThat(extendingQueryTasks).hasSize(2);
  }

  @Test
  void testExtendingTaskQueryWithAssigneeNotIn() {
    // given
    Task task = taskService.newTask("assigneeTask");
    task.setName("Task 5");
    task.setOwner(testUser.getId());
    taskService.saveTask(task);
    taskService.setAssignee(task.getId(), "john");

    // then
    TaskQuery query = taskService.createTaskQuery().taskAssigneeNotIn("kermit");
    saveQuery(query);
    List<Task> origQueryTasks = filterService.list(testFilter.getId());
    List<Task> selfExtendQueryTasks = filterService.list(testFilter.getId(), query);

    TaskQuery extendingQuery = taskService.createTaskQuery();
    extendingQuery.taskAssigneeNotIn("john", "kermit");
    List<Task> extendingQueryTasks = filterService.list(testFilter.getId(), extendingQuery);

    // then
    assertThat(origQueryTasks).hasSize(1);
    assertThat(selfExtendQueryTasks).hasSize(1);
    assertThat(extendingQueryTasks).isEmpty();
  }

  @Test
  void testExtendingEmptyTaskQueryWithCandidateGroupLike() {
    // given 3 test tasks created during setup
    TaskQuery query = taskService.createTaskQuery();
    saveQuery(query);
    List<Task> tasks = filterService.list(testFilter.getId());
    assertThat(tasks).hasSize(3);

    // when extending the query with a "candidate group like"
    TaskQuery extendingQuery = taskService.createTaskQuery();
    extendingQuery.taskCandidateGroupLike("%count%");

    // then there is 1 unassigned task with the candidate group "accounting"
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks).hasSize(1);
  }

  @Test
  void testExtendingCandidateGroupLikeTaskQueryWithEmpty() {
    // given 3 existing tasks but only 1 unassigned task that matches the initial filter
    TaskQuery query = taskService.createTaskQuery().taskCandidateGroupLike("%count%");
    saveQuery(query);
    List<Task> tasks = filterService.list(testFilter.getId());
    assertThat(tasks).hasSize(1);

    // when extending the query with an empty query
    TaskQuery extendingQuery = taskService.createTaskQuery();

    // then the empty query should be ignored in favor of the existing value for "candidate group like"
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks).hasSize(1);
  }

  @Test
  void testExtendingCandidateGroupLikeTaskQueryWithCandidateGroupLike() {
    // given 3 existing tasks but zero match the initial filter
    TaskQuery query = taskService.createTaskQuery().taskCandidateGroupLike("HR");
    saveQuery(query);
    List<Task> tasks = filterService.list(testFilter.getId());
    assertThat(tasks).isEmpty();

    // when extending the query with a "candidate groups like" query
    TaskQuery extendingQuery = taskService.createTaskQuery();
    extendingQuery.taskCandidateGroupLike("acc%");

    // then the query should be return result of task matching the new filter
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks).hasSize(1);
  }

  @Test
  void testExtendingTaskQueryListWithCandidateGroups() {
    TaskQuery query = taskService.createTaskQuery();

    List<String> candidateGroups = new ArrayList<>();
    candidateGroups.add("accounting");
    query.taskCandidateGroupIn(candidateGroups);

    saveQuery(query);

    List<Task> tasks = filterService.list(testFilter.getId());
    assertThat(tasks).hasSize(1);

    tasks = filterService.list(testFilter.getId(), query);
    assertThat(tasks).hasSize(1);

    TaskQuery extendingQuery = taskService.createTaskQuery();

    extendingQuery
      .orderByTaskCreateTime()
      .asc();

    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks).hasSize(1);
  }

  @Test
  void testExtendingTaskQueryListWithIncludeAssignedTasks() {
    TaskQuery query = taskService.createTaskQuery();

    query.taskCandidateGroup("accounting");

    saveQuery(query);

    List<Task> tasks = filterService.list(testFilter.getId());
    assertThat(tasks).hasSize(1);

    TaskQuery extendingQuery = taskService.createTaskQuery();

    extendingQuery
      .taskCandidateGroup("accounting")
      .includeAssignedTasks();

    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks).hasSize(2);
  }

  @Test
  void testExtendTaskQueryWithCandidateUserExpressionAndIncludeAssignedTasks() {
    // create an empty query and save it as a filter
    TaskQuery emptyQuery = taskService.createTaskQuery();
    Filter emptyFilter = filterService.newTaskFilter("empty");
    emptyFilter.setQuery(emptyQuery);

    // create a query with candidate user expression and include assigned tasks
    // and save it as filter
    TaskQuery query = taskService.createTaskQuery();
    query.taskCandidateUserExpression("${'test'}").includeAssignedTasks();
    Filter filter = filterService.newTaskFilter("filter");
    filter.setQuery(query);

    // extend empty query by query with candidate user expression and include assigned tasks
    Filter extendedFilter = emptyFilter.extend(query);
    TaskQueryImpl extendedQuery = extendedFilter.getQuery();
    assertThat(extendedQuery.getExpressions()).containsEntry("taskCandidateUser", "${'test'}");
    assertThat(extendedQuery.isIncludeAssignedTasks()).isTrue();

    // extend query with candidate user expression and include assigned tasks with empty query
    extendedFilter = filter.extend(emptyQuery);
    extendedQuery = extendedFilter.getQuery();
    assertThat(extendedQuery.getExpressions()).containsEntry("taskCandidateUser", "${'test'}");
    assertThat(extendedQuery.isIncludeAssignedTasks()).isTrue();
  }

  @Test
  void testExtendTaskQueryWithCandidateGroupExpressionAndIncludeAssignedTasks() {
    // create an empty query and save it as a filter
    TaskQuery emptyQuery = taskService.createTaskQuery();
    Filter emptyFilter = filterService.newTaskFilter("empty");
    emptyFilter.setQuery(emptyQuery);

    // create a query with candidate group expression and include assigned tasks
    // and save it as filter
    TaskQuery query = taskService.createTaskQuery();
    query.taskCandidateGroupExpression("${'test'}").includeAssignedTasks();
    Filter filter = filterService.newTaskFilter("filter");
    filter.setQuery(query);

    // extend empty query by query with candidate group expression and include assigned tasks
    Filter extendedFilter = emptyFilter.extend(query);
    TaskQueryImpl extendedQuery = extendedFilter.getQuery();
    assertThat(extendedQuery.getExpressions()).containsEntry("taskCandidateGroup", "${'test'}");
    assertThat(extendedQuery.isIncludeAssignedTasks()).isTrue();

    // extend query with candidate group expression and include assigned tasks with empty query
    extendedFilter = filter.extend(emptyQuery);
    extendedQuery = extendedFilter.getQuery();
    assertThat(extendedQuery.getExpressions()).containsEntry("taskCandidateGroup", "${'test'}");
    assertThat(extendedQuery.isIncludeAssignedTasks()).isTrue();
  }

  @Test
  void testExtendTaskQueryWithCandidateGroupInAndCandidateGroup() {
    // create a query with candidate group in and save it as a filter
    TaskQueryImpl candidateGroupInQuery = (TaskQueryImpl)taskService.createTaskQuery().taskCandidateGroupIn(Arrays.asList("testGroup", "testGroup2"));
    assertThat(candidateGroupInQuery.getCandidateGroups()).hasSize(2);
    assertThat(candidateGroupInQuery.getCandidateGroups().get(0)).isEqualTo("testGroup");
    assertThat(candidateGroupInQuery.getCandidateGroups().get(1)).isEqualTo("testGroup2");
    Filter candidateGroupInFilter = filterService.newTaskFilter("Groups filter");
    candidateGroupInFilter.setQuery(candidateGroupInQuery);

    // create a query with candidate group
    // and save it as filter
    TaskQuery candidateGroupQuery = taskService.createTaskQuery().taskCandidateGroup("testGroup2");

    // extend candidate group in filter by query with candidate group
    Filter extendedFilter = candidateGroupInFilter.extend(candidateGroupQuery);
    TaskQueryImpl extendedQuery = extendedFilter.getQuery();
    assertThat(extendedQuery.getCandidateGroups()).hasSize(1);
    assertThat(extendedQuery.getCandidateGroups().get(0)).isEqualTo("testGroup2");
  }

  @Test
  void testTaskQueryWithCandidateGroupInExpressionAndCandidateGroup() {
    // create a query with candidate group in expression and candidate group at once
    TaskQueryImpl candidateGroupInQuery = (TaskQueryImpl)taskService.createTaskQuery().taskCandidateGroupInExpression("${'test'}").taskCandidateGroup("testGroup");
    assertThat(candidateGroupInQuery.getExpressions()).containsEntry("taskCandidateGroupIn", "${'test'}");
    assertThat(candidateGroupInQuery.getCandidateGroup()).isEqualTo("testGroup");
  }

  @Test
  void testTaskQueryWithCandidateGroupInAndCandidateGroupExpression() {
    // create a query with candidate group in and candidate group expression
    TaskQueryImpl candidateGroupInQuery = (TaskQueryImpl)taskService.createTaskQuery().taskCandidateGroupIn(Arrays.asList("testGroup", "testGroup2")).taskCandidateGroupExpression("${'test'}");
    assertThat(candidateGroupInQuery.getExpressions()).containsEntry("taskCandidateGroup", "${'test'}");
    assertThat(candidateGroupInQuery.getCandidateGroups()).hasSize(2);
    assertThat(candidateGroupInQuery.getCandidateGroups().get(0)).isEqualTo("testGroup");
    assertThat(candidateGroupInQuery.getCandidateGroups().get(1)).isEqualTo("testGroup2");
  }

  @Test
  void testExtendTaskQueryWithCandidateGroupInExpressionAndIncludeAssignedTasks() {
    // create an empty query and save it as a filter
    TaskQuery emptyQuery = taskService.createTaskQuery();
    Filter emptyFilter = filterService.newTaskFilter("empty");
    emptyFilter.setQuery(emptyQuery);

    // create a query with candidate group in expression and include assigned tasks
    // and save it as filter
    TaskQuery query = taskService.createTaskQuery();
    query.taskCandidateGroupInExpression("${'test'}").includeAssignedTasks();
    Filter filter = filterService.newTaskFilter("filter");
    filter.setQuery(query);

    // extend empty query by query with candidate group in expression and include assigned tasks
    Filter extendedFilter = emptyFilter.extend(query);
    TaskQueryImpl extendedQuery = extendedFilter.getQuery();
    assertThat(extendedQuery.getExpressions()).containsEntry("taskCandidateGroupIn", "${'test'}");
    assertThat(extendedQuery.isIncludeAssignedTasks()).isTrue();

    // extend query with candidate group in expression and include assigned tasks with empty query
    extendedFilter = filter.extend(emptyQuery);
    extendedQuery = extendedFilter.getQuery();
    assertThat(extendedQuery.getExpressions()).containsEntry("taskCandidateGroupIn", "${'test'}");
    assertThat(extendedQuery.isIncludeAssignedTasks()).isTrue();
  }

  @Test
  void testExecuteTaskQueryListPage() {
    TaskQuery query = taskService.createTaskQuery();
    query.taskNameLike("Task%");

    saveQuery(query);

    List<Task> tasks = filterService.listPage(testFilter.getId(), 1, 2);
    assertThat(tasks).hasSize(2);
    for (Task task : tasks) {
      assertThat(task.getOwner()).isEqualTo(testUser.getId());
    }
  }

  @Test
  void testExtendingTaskQueryListPage() {
    TaskQuery query = taskService.createTaskQuery();

    saveQuery(query);

    List<Task> tasks = filterService.listPage(testFilter.getId(), 1, 2);
    assertThat(tasks).hasSize(2);

    tasks = filterService.listPage(testFilter.getId(), query, 1, 2);
    assertThat(tasks).hasSize(2);

    TaskQuery extendingQuery = taskService.createTaskQuery();

    extendingQuery.taskDelegationState(DelegationState.RESOLVED);

    tasks = filterService.listPage(testFilter.getId(), extendingQuery, 1, 2);
    assertThat(tasks).hasSize(1);

    assertThat(tasks.get(0).getDelegationState()).isEqualTo(DelegationState.RESOLVED);
  }

  @Test
  void testExecuteTaskQuerySingleResult() {
    TaskQuery query = taskService.createTaskQuery();
    query.taskDelegationState(DelegationState.PENDING);

    saveQuery(query);

    Task task = filterService.singleResult(testFilter.getId());
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo("Task 1");
  }

  @Test
  void testFailTaskQuerySingleResult() {
    TaskQuery query = taskService.createTaskQuery();

    saveQuery(query);

    String filterId = testFilter.getId();
    assertThatThrownBy(() -> filterService.singleResult(filterId))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testExtendingTaskQuerySingleResult() {
    TaskQuery query = taskService.createTaskQuery();
    query.taskDelegationState(DelegationState.PENDING);

    saveQuery(query);

    Task task = filterService.singleResult(testFilter.getId());
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo("Task 1");
    assertThat(task.getId()).isEqualTo("task1");

    task = filterService.singleResult(testFilter.getId(), query);
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo("Task 1");
    assertThat(task.getId()).isEqualTo("task1");

    TaskQuery extendingQuery = taskService.createTaskQuery();

    extendingQuery.taskId("task1");

    task = filterService.singleResult(testFilter.getId(), extendingQuery);
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo("Task 1");
    assertThat(task.getId()).isEqualTo("task1");
  }

  /**
   * CAM-6363
   * <p>
   * Verify that search by name returns case-insensitive results
   * </p>
   */
  @Test
  void testTaskQueryLookupByNameCaseInsensitive() {
    TaskQuery query = taskService.createTaskQuery();
    query.taskName("task 1");
    saveQuery(query);

    List<Task> tasks = filterService.list(testFilter.getId());
    assertThat(tasks)
            .isNotNull()
            .hasSize(1);

    query = taskService.createTaskQuery();
    query.taskName("tASk 2");
    saveQuery(query);

    tasks = filterService.list(testFilter.getId());
    assertThat(tasks)
            .isNotNull()
            .hasSize(1);
  }

  /**
   * CAM-12186
   * <p>
   * Verify that search by description returns case-insensitive results
   * </p>
   */
  @Test
  void testTaskQueryLookupByDescriptionCaseInsensitive() {
    TaskQuery query = taskService.createTaskQuery();
    query.taskDescription("description 1");
    saveQuery(query);

    List<Task> tasks = filterService.list(testFilter.getId());
    assertThat(tasks)
            .isNotNull()
            .hasSize(1);

    query = taskService.createTaskQuery();
    query.taskDescription("description 2");
    saveQuery(query);

    tasks = filterService.list(testFilter.getId());
    assertThat(tasks)
            .isNotNull()
            .hasSize(1);
  }

  /**
   * CAM-6165
   * <p>
   * Verify that search by name like returns case-insensitive results
   * </p>
   */
  @Test
  void testTaskQueryLookupByNameLikeCaseInsensitive() {
    TaskQuery query = taskService.createTaskQuery();
    query.taskNameLike("%task%");
    saveQuery(query);

    List<Task> tasks = filterService.list(testFilter.getId());
    assertThat(tasks)
            .isNotNull()
            .hasSize(3);

    query = taskService.createTaskQuery();
    query.taskNameLike("%Task%");
    saveQuery(query);

    tasks = filterService.list(testFilter.getId());
    assertThat(tasks)
            .isNotNull()
            .hasSize(3);
  }

  @Test
  void testExecuteTaskQueryCount() {
    TaskQuery query = taskService.createTaskQuery();

    saveQuery(query);

    long count = filterService.count(testFilter.getId());
    assertThat(count).isEqualTo(3);

    query.taskDelegationState(DelegationState.RESOLVED);

    saveQuery(query);

    count = filterService.count(testFilter.getId());
    assertThat(count).isEqualTo(2);
  }

  @Test
  void testExtendingTaskQueryCount() {
    TaskQuery query = taskService.createTaskQuery();

    saveQuery(query);

    TaskQuery extendingQuery = taskService.createTaskQuery();

    extendingQuery.taskId("task3");

    long count = filterService.count(testFilter.getId());

    assertThat(count).isEqualTo(3);

    count = filterService.count(testFilter.getId(), query);

    assertThat(count).isEqualTo(3);

    count = filterService.count(testFilter.getId(), extendingQuery);

    assertThat(count).isOne();
  }

  @Test
  void testSpecialExtendingQuery() {
    TaskQuery query = taskService.createTaskQuery();

    saveQuery(query);

    long count = filterService.count(testFilter.getId(), null);
    assertThat(count).isEqualTo(3);
  }

  @Test
  void testExtendingSorting() {
    // create empty query
    TaskQueryImpl query = (TaskQueryImpl) taskService.createTaskQuery();
    saveQuery(query);

    // assert default sorting
    query = testFilter.getQuery();
    assertThat(query.getOrderingProperties()).isEmpty();

    // extend query by new task query with sorting
    TaskQueryImpl sortQuery = (TaskQueryImpl) taskService.createTaskQuery().orderByTaskName().asc();
    Filter extendedFilter = testFilter.extend(sortQuery);
    query = extendedFilter.getQuery();

    List<QueryOrderingProperty> expectedOrderingProperties =
        new ArrayList<>(sortQuery.getOrderingProperties());

    verifyOrderingProperties(expectedOrderingProperties, query.getOrderingProperties());

    // extend query by new task query with additional sorting
    TaskQueryImpl extendingQuery = (TaskQueryImpl) taskService.createTaskQuery().orderByTaskAssignee().desc();
    extendedFilter = extendedFilter.extend(extendingQuery);
    query = extendedFilter.getQuery();

    expectedOrderingProperties.addAll(extendingQuery.getOrderingProperties());

    verifyOrderingProperties(expectedOrderingProperties, query.getOrderingProperties());

    // extend query by incomplete sorting query (should add sorting anyway)
    sortQuery = (TaskQueryImpl) taskService.createTaskQuery().orderByCaseExecutionId();
    extendedFilter = extendedFilter.extend(sortQuery);
    query = extendedFilter.getQuery();

    expectedOrderingProperties.addAll(sortQuery.getOrderingProperties());

    verifyOrderingProperties(expectedOrderingProperties, query.getOrderingProperties());
  }


  /**
   * Tests compatibility with serialization format that was used in 7.2
   */
  @SuppressWarnings("deprecation")
  @Test
  void testDeprecatedOrderingFormatDeserializationSingleOrdering() {
    String sortByNameAsc = "RES." + TaskQueryProperty.NAME.getName() + " " + Direction.ASCENDING.getName();

    JsonTaskQueryConverter converter = (JsonTaskQueryConverter) FilterEntity.QUERY_CONVERTER.get(EntityTypes.TASK);
    JsonObject queryJson = converter.toJsonObject(testFilter.getQuery());

    // when I apply a specific ordering by one dimension
    queryJson.addProperty(JsonTaskQueryConverter.ORDER_BY, sortByNameAsc);
    TaskQueryImpl deserializedTaskQuery = (TaskQueryImpl) converter.toObject(queryJson);

    // then the ordering is applied accordingly
    assertThat(deserializedTaskQuery.getOrderingProperties()).hasSize(1);

    QueryOrderingProperty orderingProperty =
        deserializedTaskQuery.getOrderingProperties().get(0);
    assertThat(orderingProperty.getRelation()).isNull();
    assertThat(orderingProperty.getDirection().getName()).isEqualTo("asc");
    assertThat(orderingProperty.getRelationConditions()).isNull();
    assertThat(orderingProperty.isContainedProperty()).isTrue();
    assertThat(orderingProperty.getQueryProperty().getName()).isEqualTo(TaskQueryProperty.NAME.getName());
    assertThat(orderingProperty.getQueryProperty().getFunction()).isNull();

  }

  /**
   * Tests compatibility with serialization format that was used in 7.2
   */
  @SuppressWarnings("deprecation")
  @Test
  void testDeprecatedOrderingFormatDeserializationSecondaryOrdering() {
    String sortByNameAsc = "RES." + TaskQueryProperty.NAME.getName() + " " + Direction.ASCENDING.getName();
    String secondaryOrdering = sortByNameAsc + ", RES." + TaskQueryProperty.ASSIGNEE.getName() + " " + Direction.DESCENDING.getName();

    JsonTaskQueryConverter converter = (JsonTaskQueryConverter) FilterEntity.QUERY_CONVERTER.get(EntityTypes.TASK);
    JsonObject queryJson = converter.toJsonObject(testFilter.getQuery());

    // when I apply a secondary ordering
    queryJson.addProperty(JsonTaskQueryConverter.ORDER_BY, secondaryOrdering);
    TaskQueryImpl deserializedTaskQuery = (TaskQueryImpl) converter.toObject(queryJson);

    // then the ordering is applied accordingly
    assertThat(deserializedTaskQuery.getOrderingProperties()).hasSize(2);

    QueryOrderingProperty orderingProperty1 =
        deserializedTaskQuery.getOrderingProperties().get(0);
    assertThat(orderingProperty1.getRelation()).isNull();
    assertThat(orderingProperty1.getDirection().getName()).isEqualTo("asc");
    assertThat(orderingProperty1.getRelationConditions()).isNull();
    assertThat(orderingProperty1.isContainedProperty()).isTrue();
    assertThat(orderingProperty1.getQueryProperty().getName()).isEqualTo(TaskQueryProperty.NAME.getName());
    assertThat(orderingProperty1.getQueryProperty().getFunction()).isNull();

    QueryOrderingProperty orderingProperty2 =
        deserializedTaskQuery.getOrderingProperties().get(1);
    assertThat(orderingProperty2.getRelation()).isNull();
    assertThat(orderingProperty2.getDirection().getName()).isEqualTo("desc");
    assertThat(orderingProperty2.getRelationConditions()).isNull();
    assertThat(orderingProperty2.isContainedProperty()).isTrue();
    assertThat(orderingProperty2.getQueryProperty().getName()).isEqualTo(TaskQueryProperty.ASSIGNEE.getName());
    assertThat(orderingProperty2.getQueryProperty().getFunction()).isNull();
  }

  /**
   * Tests compatibility with serialization format that was used in 7.2
   */
  @SuppressWarnings("deprecation")
  @Test
  void testDeprecatedOrderingFormatDeserializationFunctionOrdering() {
    String orderingWithFunction = "LOWER(RES." + TaskQueryProperty.NAME.getName() + ") asc";

    JsonTaskQueryConverter converter = (JsonTaskQueryConverter) FilterEntity.QUERY_CONVERTER.get(EntityTypes.TASK);
    JsonObject queryJson = converter.toJsonObject(testFilter.getQuery());

    // when I apply an ordering with a function
    queryJson.addProperty(JsonTaskQueryConverter.ORDER_BY, orderingWithFunction);
    TaskQueryImpl deserializedTaskQuery = (TaskQueryImpl) converter.toObject(queryJson);

    assertThat(deserializedTaskQuery.getOrderingProperties()).hasSize(1);

    // then the ordering is applied accordingly
    QueryOrderingProperty orderingProperty =
        deserializedTaskQuery.getOrderingProperties().get(0);
    assertThat(orderingProperty.getRelation()).isNull();
    assertThat(orderingProperty.getDirection().getName()).isEqualTo("asc");
    assertThat(orderingProperty.getRelationConditions()).isNull();
    assertThat(orderingProperty.isContainedProperty()).isFalse();
    assertThat(orderingProperty.getQueryProperty().getName()).isEqualTo(TaskQueryProperty.NAME_CASE_INSENSITIVE.getName());
    assertThat(orderingProperty.getQueryProperty().getFunction()).isEqualTo(TaskQueryProperty.NAME_CASE_INSENSITIVE.getFunction());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/task/oneTaskWithFormKeyProcess.bpmn20.xml"})
  @Test
  void testInitializeFormKeysEnabled() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    TaskQuery query = taskService.createTaskQuery()
      .processInstanceId(processInstance.getId());

    saveQuery(query);

    Task task = (Task) filterService.list(testFilter.getId()).get(0);

    assertThat(task.getFormKey()).isEqualTo("exampleFormKey");

    task = filterService.singleResult(testFilter.getId());

    assertThat(task.getFormKey()).isEqualTo("exampleFormKey");

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");
  }

  @Test
  void testExtendingVariableQuery() {
    TaskQuery taskQuery = taskService.createTaskQuery().processVariableValueEquals("hello", "world");
    saveQuery(taskQuery);

    // variables won't overridden variables with same name in different scopes
    TaskQuery extendingQuery = taskService.createTaskQuery()
      .taskVariableValueEquals("hello", "world")
      .caseInstanceVariableValueEquals("hello", "world");

    Filter extendedFilter = testFilter.extend(extendingQuery);
    TaskQueryImpl extendedQuery = extendedFilter.getQuery();
    List<TaskQueryVariableValue> variables = extendedQuery.getVariables();

    assertThat(variables).hasSize(3);

    // assert variables (ordering: extending variables are inserted first)
    assertThat(variables.get(0).getName()).isEqualTo("hello");
    assertThat(variables.get(0).getValue()).isEqualTo("world");
    assertThat(variables.get(0).getOperator()).isEqualTo(QueryOperator.EQUALS);
    assertThat(variables.get(0).isProcessInstanceVariable()).isFalse();
    assertThat(variables.get(0).isLocal()).isTrue();
    assertThat(variables.get(1).getName()).isEqualTo("hello");
    assertThat(variables.get(1).getValue()).isEqualTo("world");
    assertThat(variables.get(1).getOperator()).isEqualTo(QueryOperator.EQUALS);
    assertThat(variables.get(1).isProcessInstanceVariable()).isFalse();
    assertThat(variables.get(1).isLocal()).isFalse();
    assertThat(variables.get(2).getName()).isEqualTo("hello");
    assertThat(variables.get(2).getValue()).isEqualTo("world");
    assertThat(variables.get(2).getOperator()).isEqualTo(QueryOperator.EQUALS);
    assertThat(variables.get(2).isProcessInstanceVariable()).isTrue();
    assertThat(variables.get(2).isLocal()).isFalse();

    // variables will override variables with same name in same scope
    extendingQuery = taskService.createTaskQuery()
      .processVariableValueLessThan("hello", 42)
      .taskVariableValueLessThan("hello", 42)
      .caseInstanceVariableValueLessThan("hello", 42);

    extendedFilter = testFilter.extend(extendingQuery);
    extendedQuery = extendedFilter.getQuery();
    variables = extendedQuery.getVariables();

    assertThat(variables).hasSize(3);

    // assert variables (ordering: extending variables are inserted first)
    assertThat(variables.get(0).getName()).isEqualTo("hello");
    assertThat(variables.get(0).getValue()).isEqualTo(42);
    assertThat(variables.get(0).getOperator()).isEqualTo(QueryOperator.LESS_THAN);
    assertThat(variables.get(0).isProcessInstanceVariable()).isTrue();
    assertThat(variables.get(0).isLocal()).isFalse();
    assertThat(variables.get(1).getName()).isEqualTo("hello");
    assertThat(variables.get(1).getValue()).isEqualTo(42);
    assertThat(variables.get(1).getOperator()).isEqualTo(QueryOperator.LESS_THAN);
    assertThat(variables.get(1).isProcessInstanceVariable()).isFalse();
    assertThat(variables.get(1).isLocal()).isTrue();
    assertThat(variables.get(2).getName()).isEqualTo("hello");
    assertThat(variables.get(2).getValue()).isEqualTo(42);
    assertThat(variables.get(2).getOperator()).isEqualTo(QueryOperator.LESS_THAN);
    assertThat(variables.get(2).isProcessInstanceVariable()).isFalse();
    assertThat(variables.get(2).isLocal()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testExtendTaskQueryByOrderByProcessVariable() {
    ProcessInstance instance500 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Variables.createVariables().putValue("var", 500));
    ProcessInstance instance1000 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Variables.createVariables().putValue("var", 1000));
    ProcessInstance instance250 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Variables.createVariables().putValue("var", 250));

    TaskQuery query = taskService.createTaskQuery().processDefinitionKey("oneTaskProcess");
    saveQuery(query);

    // asc
    TaskQuery extendingQuery = taskService
        .createTaskQuery()
        .orderByProcessVariable("var", ValueType.INTEGER)
        .asc();

    List<Task> tasks = filterService.list(testFilter.getId(), extendingQuery);

    assertThat(tasks).hasSize(3);
    assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(instance250.getId());
    assertThat(tasks.get(1).getProcessInstanceId()).isEqualTo(instance500.getId());
    assertThat(tasks.get(2).getProcessInstanceId()).isEqualTo(instance1000.getId());

    // desc
    extendingQuery = taskService
        .createTaskQuery()
        .orderByProcessVariable("var", ValueType.INTEGER)
        .desc();

    tasks = filterService.list(testFilter.getId(), extendingQuery);

    assertThat(tasks).hasSize(3);
    assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(instance1000.getId());
    assertThat(tasks.get(1).getProcessInstanceId()).isEqualTo(instance500.getId());
    assertThat(tasks.get(2).getProcessInstanceId()).isEqualTo(instance250.getId());

    runtimeService.deleteProcessInstance(instance250.getId(), null);
    runtimeService.deleteProcessInstance(instance500.getId(), null);
    runtimeService.deleteProcessInstance(instance1000.getId(), null);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testExtendTaskQueryByOrderByTaskVariable() {
    ProcessInstance instance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    ProcessInstance instance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    ProcessInstance instance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Task task500 = taskService.createTaskQuery().processInstanceId(instance1.getId()).singleResult();
    taskService.setVariableLocal(task500.getId(), "var", 500);

    Task task250 = taskService.createTaskQuery().processInstanceId(instance2.getId()).singleResult();
    taskService.setVariableLocal(task250.getId(), "var", 250);

    Task task1000 = taskService.createTaskQuery().processInstanceId(instance3.getId()).singleResult();
    taskService.setVariableLocal(task1000.getId(), "var", 1000);

    TaskQuery query = taskService.createTaskQuery().processDefinitionKey("oneTaskProcess");
    saveQuery(query);

    // asc
    TaskQuery extendingQuery = taskService
        .createTaskQuery()
        .orderByProcessVariable("var", ValueType.INTEGER)
        .asc();

    List<Task> tasks = filterService.list(testFilter.getId(), extendingQuery);

    assertThat(tasks).hasSize(3);
    assertThat(tasks.get(0).getId()).isEqualTo(task250.getId());
    assertThat(tasks.get(1).getId()).isEqualTo(task500.getId());
    assertThat(tasks.get(2).getId()).isEqualTo(task1000.getId());

    // desc
    extendingQuery = taskService
        .createTaskQuery()
        .orderByProcessVariable("var", ValueType.INTEGER)
        .desc();

    tasks = filterService.list(testFilter.getId(), extendingQuery);

    assertThat(tasks).hasSize(3);
    assertThat(tasks.get(0).getId()).isEqualTo(task1000.getId());
    assertThat(tasks.get(1).getId()).isEqualTo(task500.getId());
    assertThat(tasks.get(2).getId()).isEqualTo(task250.getId());

    runtimeService.deleteProcessInstance(instance1.getId(), null);
    runtimeService.deleteProcessInstance(instance2.getId(), null);
    runtimeService.deleteProcessInstance(instance3.getId(), null);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testExtendTaskQueryByTaskVariableIgnoreCase() {
    String variableName = "variableName";
    String variableValueCamelCase = "someVariableValue";
    String variableValueLowerCase = variableValueCamelCase.toLowerCase();

    ProcessInstance instance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    ProcessInstance instance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    ProcessInstance instance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Task taskCamelCase = taskService.createTaskQuery().processInstanceId(instance1.getId()).singleResult();
    taskService.setVariableLocal(taskCamelCase.getId(), variableName, variableValueCamelCase);

    Task taskLowerCase = taskService.createTaskQuery().processInstanceId(instance2.getId()).singleResult();
    taskService.setVariableLocal(taskLowerCase.getId(), variableName, variableValueLowerCase);

    Task taskWithNoVariable = taskService.createTaskQuery().processInstanceId(instance3.getId()).singleResult();

    TaskQuery query = taskService.createTaskQuery().processDefinitionKey("oneTaskProcess");
    saveQuery(query);

    // all tasks
    List<Task> tasks = filterService.list(testFilter.getId(), query);
    assertThat(tasks)
            .contains(taskCamelCase)
            .contains(taskLowerCase)
            .contains(taskWithNoVariable);

    // equals case-sensitive for comparison
    TaskQuery extendingQuery = taskService.createTaskQuery().taskVariableValueEquals(variableName, variableValueLowerCase);
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .doesNotContain(taskCamelCase)
            .contains(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // equals case-insensitive
    extendingQuery = taskService.createTaskQuery().matchVariableValuesIgnoreCase().taskVariableValueEquals(variableName, variableValueLowerCase);
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .contains(taskCamelCase)
            .contains(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // not equals case-sensitive for comparison
    extendingQuery = taskService.createTaskQuery().taskVariableValueNotEquals(variableName, variableValueLowerCase);
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .contains(taskCamelCase)
            .doesNotContain(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // not equals case-insensitive
    extendingQuery = taskService.createTaskQuery().matchVariableValuesIgnoreCase().taskVariableValueNotEquals(variableName, variableValueLowerCase);
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .doesNotContain(taskCamelCase)
            .doesNotContain(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // like case-sensitive for comparison
    extendingQuery = taskService.createTaskQuery().taskVariableValueLike(variableName, "somevariable%");
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .doesNotContain(taskCamelCase)
            .contains(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // like case-insensitive
    extendingQuery = taskService.createTaskQuery().matchVariableValuesIgnoreCase().taskVariableValueLike(variableName, "somevariable%");
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .contains(taskCamelCase)
            .contains(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // variable name case-insensitive
    extendingQuery = taskService.createTaskQuery().matchVariableNamesIgnoreCase().taskVariableValueEquals(variableName.toLowerCase(), variableValueCamelCase);
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .contains(taskCamelCase)
            .doesNotContain(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // variable name and variable value case-insensitive
    extendingQuery = taskService.createTaskQuery().matchVariableNamesIgnoreCase().matchVariableValuesIgnoreCase().taskVariableValueEquals(variableName.toLowerCase(), variableValueCamelCase.toLowerCase());
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .contains(taskCamelCase)
            .contains(taskLowerCase)
            .doesNotContain(taskWithNoVariable);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testExtendTaskQueryByCaseInstanceVariableIgnoreCase() {
    String variableName = "variableName";
    String variableValueCamelCase = "someVariableValue";
    String variableValueLowerCase = variableValueCamelCase.toLowerCase();
    Map<String, Object> variables = new HashMap<>();

    String caseDefinitionId = repositoryService.createCaseDefinitionQuery().singleResult().getId();

    variables.put(variableName, variableValueCamelCase);
    CaseInstance instanceCamelCase = caseService.createCaseInstanceById(caseDefinitionId, variables);
    variables.put(variableName, variableValueLowerCase);
    CaseInstance instanceLowerCase = caseService.createCaseInstanceById(caseDefinitionId, variables);
    CaseInstance instanceWithoutVariables = caseService.createCaseInstanceById(caseDefinitionId);

    Task taskCamelCase = taskService.createTaskQuery().caseInstanceId(instanceCamelCase.getId()).singleResult();
    Task taskLowerCase = taskService.createTaskQuery().caseInstanceId(instanceLowerCase.getId()).singleResult();
    Task taskWithNoVariable = taskService.createTaskQuery().caseInstanceId(instanceWithoutVariables.getId()).singleResult();

    TaskQuery query = taskService.createTaskQuery().caseDefinitionId(caseDefinitionId);
    saveQuery(query);

    // all tasks
    List<Task> tasks = filterService.list(testFilter.getId(), query);
    assertThat(tasks)
            .contains(taskCamelCase)
            .contains(taskLowerCase)
            .contains(taskWithNoVariable);

    // equals case-sensitive for comparison
    TaskQuery extendingQuery = taskService.createTaskQuery().caseInstanceVariableValueEquals(variableName, variableValueLowerCase);
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .doesNotContain(taskCamelCase)
            .contains(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // equals case-insensitive
    extendingQuery = taskService.createTaskQuery().matchVariableValuesIgnoreCase().caseInstanceVariableValueEquals(variableName, variableValueLowerCase);
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .contains(taskCamelCase)
            .contains(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // not equals case-sensitive for comparison
    extendingQuery = taskService.createTaskQuery().caseInstanceVariableValueNotEquals(variableName, variableValueLowerCase);
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .contains(taskCamelCase)
            .doesNotContain(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // not equals case-insensitive
    extendingQuery = taskService.createTaskQuery().matchVariableValuesIgnoreCase().caseInstanceVariableValueNotEquals(variableName, variableValueLowerCase);
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .doesNotContain(taskCamelCase)
            .doesNotContain(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // like case-sensitive for comparison
    extendingQuery = taskService.createTaskQuery().caseInstanceVariableValueLike(variableName, "somevariable%");
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .doesNotContain(taskCamelCase)
            .contains(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // like case-insensitive
    extendingQuery = taskService.createTaskQuery().matchVariableValuesIgnoreCase().caseInstanceVariableValueLike(variableName, "somevariable%");
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .contains(taskCamelCase)
            .contains(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // not like case-sensitive for comparison
    extendingQuery = taskService.createTaskQuery().caseInstanceVariableValueNotLike(variableName, "somevariable%");
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .contains(taskCamelCase)
            .doesNotContain(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // not like case-insensitive
    extendingQuery = taskService.createTaskQuery().matchVariableValuesIgnoreCase().caseInstanceVariableValueNotLike(variableName, "somevariable%");
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .doesNotContain(taskCamelCase)
            .doesNotContain(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // variable name case-insensitive
    extendingQuery = taskService.createTaskQuery().matchVariableNamesIgnoreCase().caseInstanceVariableValueEquals(variableName.toLowerCase(), variableValueCamelCase);
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .contains(taskCamelCase)
            .doesNotContain(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    //variable name and variable value case-insensitive
    extendingQuery = taskService.createTaskQuery().matchVariableNamesIgnoreCase().matchVariableValuesIgnoreCase().caseInstanceVariableValueEquals(variableName.toLowerCase(), variableValueCamelCase);
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .contains(taskCamelCase)
            .contains(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // cleanup
    caseService.terminateCaseExecution(instanceCamelCase.getId());
    caseService.terminateCaseExecution(instanceLowerCase.getId());
    caseService.terminateCaseExecution(instanceWithoutVariables.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testExtendTaskQueryByProcessVariableIgnoreCase() {
    String variableName = "variableName";
    String variableValueCamelCase = "someVariableValue";
    String variableValueLowerCase = variableValueCamelCase.toLowerCase();
    Map<String, Object> variables = new HashMap<>();

    variables.put(variableName, variableValueCamelCase);
    ProcessInstance instanceCamelCase = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);
    variables.put(variableName, variableValueLowerCase);
    ProcessInstance instanceLowerCase = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);
    ProcessInstance instanceWithoutVariables = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Task taskCamelCase = taskService.createTaskQuery().processInstanceId(instanceCamelCase.getId()).singleResult();
    Task taskLowerCase = taskService.createTaskQuery().processInstanceId(instanceLowerCase.getId()).singleResult();
    Task taskWithNoVariable = taskService.createTaskQuery().processInstanceId(instanceWithoutVariables.getId()).singleResult();

    TaskQuery query = taskService.createTaskQuery().processDefinitionKey("oneTaskProcess");
    saveQuery(query);

    // all tasks
    List<Task> tasks = filterService.list(testFilter.getId(), query);
    assertThat(tasks)
            .contains(taskCamelCase)
            .contains(taskLowerCase)
            .contains(taskWithNoVariable);

    // equals case-sensitive for comparison
    TaskQuery extendingQuery = taskService.createTaskQuery().processVariableValueEquals(variableName, variableValueLowerCase);
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .doesNotContain(taskCamelCase)
            .contains(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // equals case-insensitive
    extendingQuery = taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueEquals(variableName, variableValueLowerCase);
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .contains(taskCamelCase)
            .contains(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // not equals case-sensitive for comparison
    extendingQuery = taskService.createTaskQuery().processVariableValueNotEquals(variableName, variableValueLowerCase);
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .contains(taskCamelCase)
            .doesNotContain(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // not equals case-insensitive
    extendingQuery = taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueNotEquals(variableName, variableValueLowerCase);
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .doesNotContain(taskCamelCase)
            .doesNotContain(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // like case-sensitive for comparison
    extendingQuery = taskService.createTaskQuery().processVariableValueLike(variableName, "somevariable%");
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .doesNotContain(taskCamelCase)
            .contains(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // like case-insensitive
    extendingQuery = taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueLike(variableName, "somevariable%");
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .contains(taskCamelCase)
            .contains(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // not like case-sensitive for comparison
    extendingQuery = taskService.createTaskQuery().processVariableValueNotLike(variableName, "somevariable%");
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .contains(taskCamelCase)
            .doesNotContain(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // not like case-insensitive
    extendingQuery = taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueNotLike(variableName, "somevariable%");
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .doesNotContain(taskCamelCase)
            .doesNotContain(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // variable name case-insensitive
    extendingQuery = taskService.createTaskQuery().matchVariableNamesIgnoreCase().processVariableValueEquals(variableName.toLowerCase(), variableValueCamelCase);
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .contains(taskCamelCase)
            .doesNotContain(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

    // variable name and variable value case-insensitive
    extendingQuery = taskService.createTaskQuery().matchVariableNamesIgnoreCase().matchVariableValuesIgnoreCase().processVariableValueEquals(variableName.toLowerCase(), variableValueCamelCase);
    tasks = filterService.list(testFilter.getId(), extendingQuery);
    assertThat(tasks)
            .contains(taskCamelCase)
            .contains(taskLowerCase)
            .doesNotContain(taskWithNoVariable);

  }

  @Test
  void testExtendTaskQuery_ORInExtendingQuery() {
    // given
    createTasksForOrQueries();

    // when
    TaskQuery extendedQuery = taskService.createTaskQuery()
      .taskName("taskForOr");

    Filter extendedFilter = filterService.newTaskFilter("extendedOrFilter");
    extendedFilter.setQuery(extendedQuery);
    filterService.saveFilter(extendedFilter);

    TaskQuery extendingQuery = taskService.createTaskQuery()
      .or()
        .taskDescription("aTaskDescription")
        .taskOwner("aTaskOwner")
      .endOr()
      .or()
        .taskPriority(3)
        .taskAssignee("aTaskAssignee")
      .endOr();

    // then
    assertThat(extendedQuery.list()).hasSize(4);
    assertThat(filterService.list(extendedFilter.getId())).hasSize(4);
    assertThat(extendingQuery.list()).hasSize(6);
    assertThat(filterService.list(extendedFilter.getId(), extendingQuery)).hasSize(3);
  }

  @Test
  void testExtendTaskQuery_ORInExtendedQuery() {
    // given
    createTasksForOrQueries();

    // when
    TaskQuery extendedQuery = taskService.createTaskQuery()
      .or()
        .taskDescription("aTaskDescription")
        .taskOwner("aTaskOwner")
      .endOr()
      .or()
        .taskPriority(3)
        .taskAssignee("aTaskAssignee")
      .endOr();

    Filter extendedFilter = filterService.newTaskFilter("extendedOrFilter");
    extendedFilter.setQuery(extendedQuery);
    filterService.saveFilter(extendedFilter);

    TaskQuery extendingQuery = taskService.createTaskQuery()
      .taskName("taskForOr");

    // then
    assertThat(extendedQuery.list()).hasSize(6);
    assertThat(filterService.list(extendedFilter.getId())).hasSize(6);
    assertThat(extendingQuery.list()).hasSize(4);
    assertThat(filterService.list(extendedFilter.getId(), extendingQuery)).hasSize(3);
  }

  @Test
  void testExtendTaskQuery_ORInBothExtendedAndExtendingQuery() {
    // given
    createTasksForOrQueries();

    // when
    TaskQuery extendedQuery = taskService.createTaskQuery()
      .or()
        .taskName("taskForOr")
        .taskDescription("aTaskDescription")
      .endOr();

    Filter extendedFilter = filterService.newTaskFilter("extendedOrFilter");
    extendedFilter.setQuery(extendedQuery);
    filterService.saveFilter(extendedFilter);

    TaskQuery extendingQuery = taskService.createTaskQuery()
      .or()
        .tenantIdIn("aTenantId")
        .taskOwner("aTaskOwner")
      .endOr()
      .or()
        .taskPriority(3)
        .taskAssignee("aTaskAssignee")
      .endOr();

    // then
    assertThat(extendedQuery.list()).hasSize(6);
    assertThat(filterService.list(extendedFilter.getId())).hasSize(6);
    assertThat(extendingQuery.list()).hasSize(4);
    assertThat(filterService.list(extendedFilter.getId(), extendingQuery)).hasSize(3);
  }

  @Test
  void testOrderByVariables() {
    // given
    TaskQueryImpl query = (TaskQueryImpl) taskService.createTaskQuery()
        .orderByProcessVariable("foo", ValueType.STRING).asc()
        .orderByExecutionVariable("foo", ValueType.STRING).asc()
        .orderByCaseInstanceVariable("foo", ValueType.STRING).asc()
        .orderByCaseExecutionVariable("foo", ValueType.STRING).asc()
        .orderByTaskVariable("foo", ValueType.STRING).asc();

    Filter filter = filterService.newTaskFilter("extendedOrFilter");
    filter.setQuery(query);
    filterService.saveFilter(filter);

    // when
    filter = filterService.getFilter(filter.getId());

    // then
    List<QueryOrderingProperty> expectedOrderingProperties =
        new ArrayList<>(query.getOrderingProperties());

    verifyOrderingProperties(expectedOrderingProperties, ((TaskQueryImpl) filter.getQuery()).getOrderingProperties());

    for (QueryOrderingProperty prop : ((TaskQueryImpl) filter.getQuery()).getOrderingProperties()) {
      assertThat(prop).isInstanceOf(VariableOrderProperty.class);
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testBooleanVariable() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Variables.createVariables().putValue("booleanVariable", true));

    TaskQuery query = taskService.createTaskQuery()
      .processVariableValueEquals("booleanVariable", true);

    Filter filter = filterService.newTaskFilter("filter");
    filter.setQuery(query);

    // when
    filterService.saveFilter(filter);

    // then
    assertThat(filterService.count(filter.getId())).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testIntVariable() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Variables.createVariables().putValue("intVariable", 7));

    TaskQuery query = taskService.createTaskQuery()
      .processVariableValueEquals("intVariable", 7);

    Filter filter = filterService.newTaskFilter("filter");
    filter.setQuery(query);

    // when
    filterService.saveFilter(filter);

    // then
    assertThat(filterService.count(filter.getId())).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testIntOutOfRangeVariable() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Variables.createVariables().putValue("longVariable", Integer.MAX_VALUE+1L));

    TaskQuery query = taskService.createTaskQuery()
      .processVariableValueEquals("longVariable", Integer.MAX_VALUE+1L);

    Filter filter = filterService.newTaskFilter("filter");
    filter.setQuery(query);

    // when
    filterService.saveFilter(filter);

    // then
    assertThat(filterService.count(filter.getId())).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDoubleVariable() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Variables.createVariables().putValue("doubleVariable", 88.89D));

    TaskQuery query = taskService.createTaskQuery()
      .processVariableValueEquals("doubleVariable", 88.89D);

    Filter filter = filterService.newTaskFilter("filter");
    filter.setQuery(query);

    // when
    filterService.saveFilter(filter);

    // then
    assertThat(filterService.count(filter.getId())).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testStringVariable() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Variables.createVariables().putValue("stringVariable", "aVariableValue"));

    TaskQuery query = taskService.createTaskQuery()
      .processVariableValueEquals("stringVariable", "aVariableValue");

    Filter filter = filterService.newTaskFilter("filter");
    filter.setQuery(query);

    // when
    filterService.saveFilter(filter);

    // then
    assertThat(filterService.count(filter.getId())).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testNullVariable() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Variables.createVariables().putValue("nullVariable", null));

    TaskQuery query = taskService.createTaskQuery()
      .processVariableValueEquals("nullVariable", null);

    Filter filter = filterService.newTaskFilter("filter");
    filter.setQuery(query);

    // when
    filterService.saveFilter(filter);

    // then
    assertThat(filterService.count(filter.getId())).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDueDate() {
    // given
    Date date = new Date();
    String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();

    Task task = taskService.createTaskQuery()
      .processInstanceId(processInstanceId)
      .singleResult();

    task.setDueDate(date);

    taskService.saveTask(task);

    TaskQuery query = taskService.createTaskQuery()
      .dueDate(date);

    Filter filter = filterService.newTaskFilter("filter");
    filter.setQuery(query);

    // when
    filterService.saveFilter(filter);

    // then
    assertThat(filterService.count(filter.getId())).isOne();
  }

  @Test
  void testWithoutDueDate() {
    // given
    Task task = taskService.newTask();
    task.setDueDate(new Date());
    taskService.saveTask(task);

    TaskQuery query = taskService.createTaskQuery()
      .withoutDueDate();

    testFilter.setQuery(query);

    // when
    filterService.saveFilter(testFilter);

    // then
    assertThat(filterService.count(testFilter.getId())).isEqualTo(3L);
  }

  @Test
  void testExtendQueryByWithoutDueDate() {
    // given
    Task task = taskService.newTask();
    task.setDueDate(new Date());
    taskService.saveTask(task);

    TaskQuery query = taskService.createTaskQuery();
    saveQuery(query);

    // assume
    assertThat(filterService.count(testFilter.getId())).isEqualTo(4L);

    // when
    TaskQuery extendingQuery = taskService.createTaskQuery().withoutDueDate();

    // then
    assertThat(filterService.count(testFilter.getId(), extendingQuery)).isEqualTo(3L);
  }

  @Test
  void testTaskIdInPositive() {
    // given
    List<Task> existingTasks = taskService.createTaskQuery().list();
    String task1 = existingTasks.get(0).getId();
    String task2 = existingTasks.get(1).getId();
    TaskQueryImpl query = (TaskQueryImpl) taskService.createTaskQuery().taskIdIn(task1, task2);
    Filter filter = filterService.newTaskFilter("taskIDfilter");
    filter.setQuery(query);

    // when
    // save filter
    filterService.saveFilter(filter);

    // then
    filter = filterService.createTaskFilterQuery().singleResult();
    query = filter.getQuery();

    // then
    assertThat(query.getTaskIdIn()).containsOnly(task1, task2);
  }

  @Test
  void testAssigneeInPositive() {
    // given
    TaskQueryImpl taskQuery = new TaskQueryImpl();
    taskQuery.taskAssigneeIn(testString);

    // when
    // save filter
    testFilter.setQuery(taskQuery);
    filterService.saveFilter(testFilter);

    // fetch from db
    testFilter = filterService.createTaskFilterQuery().singleResult();
    taskQuery = testFilter.getQuery();

    // then
    assertThat(taskQuery.getAssigneeIn()).contains(testString);
  }

  @Test
  void testAssigneeNotInPositive() {
    // given
    TaskQueryImpl taskQuery = new TaskQueryImpl();
    taskQuery.taskAssigneeNotIn(testString);

    // when
    // save filter
    testFilter.setQuery(taskQuery);
    filterService.saveFilter(testFilter);

    // fetch from db
    testFilter = filterService.createTaskFilterQuery().singleResult();
    taskQuery = testFilter.getQuery();

    // then
    assertThat(taskQuery.getAssigneeNotIn()).contains(testString);
  }

  @Test
  void testAssigneeInNegative() {
    // given
    TaskQueryImpl taskQuery = new TaskQueryImpl();

    // when
    // save filter
    testFilter.setQuery(taskQuery);
    filterService.saveFilter(testFilter);

    // fetch from db
    testFilter = filterService.createTaskFilterQuery().singleResult();

    // test query
    taskQuery = testFilter.getQuery();

    // then
    assertThat(taskQuery.getAssigneeIn()).isNull();
  }

  @Test
  void testAssigneeNotInNegative() {
    // given
    TaskQueryImpl taskQuery = new TaskQueryImpl();

    // when
    // save filter
    testFilter.setQuery(taskQuery);
    filterService.saveFilter(testFilter);

    // fetch from db
    testFilter = filterService.createTaskFilterQuery().singleResult();

    // test query
    taskQuery = testFilter.getQuery();

    // then
    assertThat(taskQuery.getAssigneeNotIn()).isNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Disabled("CAM-9613")
  @Test
  void testDateVariable() {
    // given
    Date date = new Date();
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Variables.createVariables().putValue("dateVariable", date));

    TaskQuery query = taskService.createTaskQuery()
      .processVariableValueEquals("dateVariable", date);

    Filter filter = filterService.newTaskFilter("filter");
    filter.setQuery(query);

    // when
    filterService.saveFilter(filter);

    // then
    assertThat(filterService.count(filter.getId())).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Disabled("CAM-9613")
  @Test
  void testByteArrayVariable() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Variables.createVariables().putValue("bytesVariable", "aByteArray".getBytes()));

    TaskQuery query = taskService.createTaskQuery()
      .processVariableValueEquals("bytesVariable", "aByteArray".getBytes());

    Filter filter = filterService.newTaskFilter("filter");
    filter.setQuery(query);

    // when
    filterService.saveFilter(filter);

    // then
    assertThat(filterService.count(filter.getId())).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Disabled("CAM-9613")
  @Test
  void testLongVariable() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Variables.createVariables().putValue("longVariable", 7L));

    TaskQuery query = taskService.createTaskQuery()
      .processVariableValueEquals("longVariable", 7L);

    Filter filter = filterService.newTaskFilter("filter");
    filter.setQuery(query);

    // when
    filterService.saveFilter(filter);

    // then
    assertThat(filterService.count(filter.getId())).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Disabled("CAM-9613")
  @Test
  void testShortVariable() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Variables.createVariables().putValue("shortVariable", (short) 7));

    TaskQuery query = taskService.createTaskQuery()
      .processVariableValueEquals("shortVariable", (short) 7);

    Filter filter = filterService.newTaskFilter("filter");
    filter.setQuery(query);

    // when
    filterService.saveFilter(filter);

    // then
    assertThat(filterService.count(filter.getId())).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testExtendingTaskQueryWithProcessInstanceIn() {
    // given
    String firstId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getProcessInstanceId();
    String secondId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getProcessInstanceId();

    // then
    TaskQuery query = taskService.createTaskQuery().processInstanceIdIn(firstId);
    saveQuery(query);
    List<Task> origQueryTasks = filterService.list(testFilter.getId());
    List<Task> selfExtendQueryTasks = filterService.list(testFilter.getId(), query);

    TaskQuery extendingQuery = taskService.createTaskQuery();
    extendingQuery.processInstanceIdIn(firstId, secondId);
    List<Task> extendingQueryTasks = filterService.list(testFilter.getId(), extendingQuery);

    // then
    assertThat(origQueryTasks).hasSize(1);
    assertThat(selfExtendQueryTasks).hasSize(1);
    assertThat(extendingQueryTasks).hasSize(2);
  }

  @Test
  void shouldDeserializeOrQueryWithCandidateGroupAndUser() {
    // given
    TaskQuery query = taskService.createTaskQuery()
        .or()
          .taskCandidateGroup("foo")
          .taskCandidateUser("bar")
        .endOr();
    JsonObject jsonObject = queryConverter.toJsonObject(query);

    // when deserializing the query
    // then there is no exception
    assertThat(queryConverter.toObject(jsonObject)).isNotNull();
  }

  protected void saveQuery(TaskQuery query) {
    testFilter.setQuery(query);
    filterService.saveFilter(testFilter);
    testFilter = filterService.getFilter(testFilter.getId());
  }

  protected void createTasks() {
    Task task = taskService.newTask("task1");
    task.setName("Task 1");
    task.setDescription("Description 1");
    task.setOwner(testUser.getId());
    task.setDelegationState(DelegationState.PENDING);
    taskService.saveTask(task);
    taskService.addCandidateGroup(task.getId(), "accounting");

    task = taskService.newTask("task2");
    task.setName("Task 2");
    task.setDescription("Description 2");
    task.setOwner(testUser.getId());
    task.setDelegationState(DelegationState.RESOLVED);
    taskService.saveTask(task);
    taskService.setAssignee(task.getId(), "kermit");
    taskService.addCandidateGroup(task.getId(), "accounting");

    task = taskService.newTask("task3");
    task.setName("Task 3");
    task.setDescription("Description 3");
    task.setOwner(testUser.getId());
    task.setDelegationState(DelegationState.RESOLVED);
    taskService.saveTask(task);
  }

  protected void createTasksForOrQueries() {
    Task task1 = taskService.newTask();
    task1.setName("taskForOr");
    task1.setDescription("aTaskDescription");
    task1.setPriority(3);
    taskService.saveTask(task1);

    Task task2 = taskService.newTask();
    task2.setName("taskForOr");
    task2.setDescription("aTaskDescription");
    task2.setAssignee("aTaskAssignee");
    task2.setTenantId("aTenantId");
    taskService.saveTask(task2);

    Task task3 = taskService.newTask();
    task3.setName("taskForOr");
    task3.setOwner("aTaskOwner");
    taskService.saveTask(task3);

    Task task4 = taskService.newTask();
    task4.setName("taskForOr");
    task4.setOwner("aTaskOwner");
    task4.setPriority(3);
    taskService.saveTask(task4);

    Task task5 = taskService.newTask();
    task5.setDescription("aTaskDescription");
    task5.setAssignee("aTaskAssignee");
    taskService.saveTask(task5);

    Task task6 = taskService.newTask();
    task6.setDescription("aTaskDescription");
    task6.setAssignee("aTaskAssignee");
    task6.setTenantId("aTenantId");
    taskService.saveTask(task6);

    Task task7 = taskService.newTask();
    task7.setTenantId("aTenantId");
    task7.setOwner("aTaskOwner");
    task7.setPriority(3);
    task7.setAssignee("aTaskAssignee");
    taskService.saveTask(task7);
  }
}
