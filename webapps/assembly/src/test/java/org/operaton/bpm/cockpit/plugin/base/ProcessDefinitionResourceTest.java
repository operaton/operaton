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
package org.operaton.bpm.cockpit.plugin.base;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.cockpit.impl.plugin.base.dto.ProcessDefinitionDto;
import org.operaton.bpm.cockpit.impl.plugin.base.dto.query.ProcessDefinitionQueryDto;
import org.operaton.bpm.cockpit.impl.plugin.base.sub.resources.ProcessDefinitionResource;
import org.operaton.bpm.cockpit.plugin.test.AbstractCockpitPluginTest;
import org.operaton.bpm.engine.impl.db.sql.DbSqlSessionFactory;
import org.operaton.bpm.engine.impl.test.RequiredDatabase;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.rest.dto.VariableQueryParameterDto;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;

import static org.operaton.bpm.engine.rest.dto.ConditionQueryParameterDto.EQUALS_OPERATOR_NAME;
import static org.operaton.bpm.engine.rest.dto.ConditionQueryParameterDto.LIKE_OPERATOR_NAME;
import static org.operaton.bpm.engine.rest.dto.ConditionQueryParameterDto.NOT_EQUALS_OPERATOR_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;

class ProcessDefinitionResourceTest extends AbstractCockpitPluginTest {
  ProcessDefinitionResource resource;

  @AfterEach
  void clearAuthentication() {
    identityService.clearAuthentication();
  }

  @AfterEach
  void resetQueryMaxResultsLimit() {
    processEngineConfiguration.setQueryMaxResultsLimit(Integer.MAX_VALUE);
  }

  @Test
  @Deployment(resources = {
      "processes/user-task-process.bpmn",
      "processes/calling-user-task-process.bpmn"
  })
  void testCalledProcessDefinitionByParentProcessDefinitionId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("CallingUserTaskProcess");

    resource = new ProcessDefinitionResource(getProcessEngine().getName(), processInstance.getProcessDefinitionId());

    ProcessDefinition userTaskProcess = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("userTaskProcess")
        .singleResult();

    ProcessDefinitionQueryDto queryParameter = new ProcessDefinitionQueryDto();

    List<ProcessDefinitionDto> result = resource.queryCalledProcessDefinitions(queryParameter);
    assertThat(result)
      .isNotEmpty()
      .hasSize(1);

    ProcessDefinitionDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(userTaskProcess.getId());
    assertThat(dto.getKey()).isEqualTo(userTaskProcess.getKey());
    assertThat(dto.getName()).isEqualTo(userTaskProcess.getName());
    assertThat(dto.getVersion()).isEqualTo(userTaskProcess.getVersion());
    assertThat(dto.getCalledFromActivityIds()).hasSize(1);

    String calledFrom = dto.getCalledFromActivityIds().get(0);

    assertThat(calledFrom).isEqualTo("CallActivity_1");

  }

  @Test
  @Deployment(resources = {
      "processes/user-task-process.bpmn",
      "processes/two-parallel-call-activities-calling-same-process.bpmn"
  })
  void testCalledProcessDefinitionByParentProcessDefinitionIdWithTwoActivityCallingSameProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TwoParallelCallActivitiesCallingSameProcess");

    resource = new ProcessDefinitionResource(getProcessEngine().getName(), processInstance.getProcessDefinitionId());

    ProcessDefinition userTaskProcess = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("userTaskProcess")
        .singleResult();

    ProcessDefinitionQueryDto queryParameter = new ProcessDefinitionQueryDto();

    List<ProcessDefinitionDto> result = resource.queryCalledProcessDefinitions(queryParameter);
    assertThat(result)
      .isNotEmpty()
      .hasSize(1);

    ProcessDefinitionDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(userTaskProcess.getId());
    assertThat(dto.getKey()).isEqualTo(userTaskProcess.getKey());
    assertThat(dto.getName()).isEqualTo(userTaskProcess.getName());
    assertThat(dto.getVersion()).isEqualTo(userTaskProcess.getVersion());
    assertThat(dto.getCalledFromActivityIds()).hasSize(2);

    for (String activityId : dto.getCalledFromActivityIds()) {
      if ("firstCallActivity".equals(activityId)) {
        assertThat(activityId).isEqualTo("firstCallActivity");
      } else if ("secondCallActivity".equals(activityId)) {
        assertThat(activityId).isEqualTo("secondCallActivity");
      } else {
        fail("Unexpected activity id:" + activityId);
      }
    }
  }

  @Test
  @Deployment(resources = {
      "processes/user-task-process.bpmn",
      "processes/two-parallel-call-activities-calling-same-process.bpmn",
      "processes/calling-user-task-process.bpmn"
  })
  void testCalledProcessDefinitionByCallingSameProcessFromDifferentProcessDefinitions() {
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("TwoParallelCallActivitiesCallingSameProcess");
    ProcessDefinitionResource resource1 = new ProcessDefinitionResource(getProcessEngine().getName(), processInstance1.getProcessDefinitionId());

    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("CallingUserTaskProcess");
    ProcessDefinitionResource resource2 = new ProcessDefinitionResource(getProcessEngine().getName(), processInstance2.getProcessDefinitionId());

    ProcessDefinition userTaskProcess = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("userTaskProcess")
        .singleResult();

    ProcessDefinitionQueryDto queryParameter1 = new ProcessDefinitionQueryDto();

    List<ProcessDefinitionDto> result1 = resource1.queryCalledProcessDefinitions(queryParameter1);
    assertThat(result1)
      .isNotEmpty()
      .hasSize(1);

    ProcessDefinitionDto dto1 = result1.get(0);

    assertThat(dto1.getId()).isEqualTo(userTaskProcess.getId());
    assertThat(dto1.getKey()).isEqualTo(userTaskProcess.getKey());
    assertThat(dto1.getName()).isEqualTo(userTaskProcess.getName());
    assertThat(dto1.getVersion()).isEqualTo(userTaskProcess.getVersion());
    assertThat(dto1.getCalledFromActivityIds()).hasSize(2);

    for (String activityId : dto1.getCalledFromActivityIds()) {
      if ("firstCallActivity".equals(activityId)) {
        assertThat(activityId).isEqualTo("firstCallActivity");
      } else if ("secondCallActivity".equals(activityId)) {
        assertThat(activityId).isEqualTo("secondCallActivity");
      } else {
        fail("Unexpected activity id:" + activityId);
      }
    }

    ProcessDefinitionQueryDto queryParameter2 = new ProcessDefinitionQueryDto();

    List<ProcessDefinitionDto> result2 = resource2.queryCalledProcessDefinitions(queryParameter2);
    assertThat(result2)
      .isNotEmpty()
      .hasSize(1);

    ProcessDefinitionDto dto2 = result2.get(0);

    assertThat(dto2.getId()).isEqualTo(userTaskProcess.getId());
    assertThat(dto2.getKey()).isEqualTo(userTaskProcess.getKey());
    assertThat(dto2.getName()).isEqualTo(userTaskProcess.getName());
    assertThat(dto2.getVersion()).isEqualTo(userTaskProcess.getVersion());
    assertThat(dto2.getCalledFromActivityIds()).hasSize(1);

    String calledFrom = dto2.getCalledFromActivityIds().get(0);

    assertThat(calledFrom).isEqualTo("CallActivity_1");
  }

  @Test
  @Deployment(resources = {
      "processes/user-task-process.bpmn",
      "processes/another-user-task-process.bpmn",
      "processes/two-parallel-call-activities-calling-different-process.bpmn"
  })
  void testCalledProcessDefinitionByCallingDifferentProcessFromSameProcessDefinitions() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TwoParallelCallActivitiesCallingDifferentProcess");

    resource = new ProcessDefinitionResource(getProcessEngine().getName(), processInstance.getProcessDefinitionId());

    ProcessDefinition userTaskProcess = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("userTaskProcess")
        .singleResult();

    ProcessDefinition anotherUserTaskProcess = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("anotherUserTaskProcess")
        .singleResult();

    ProcessDefinitionQueryDto queryParameter = new ProcessDefinitionQueryDto();

    List<ProcessDefinitionDto> result = resource.queryCalledProcessDefinitions(queryParameter);
    assertThat(result)
      .isNotEmpty()
      .hasSize(2);

    ProcessDefinition compareWith = null;
    for (ProcessDefinitionDto dto : result) {
      String id = dto.getId();
      if (id.equals(userTaskProcess.getId())) {
        compareWith = userTaskProcess;
        assertThat(dto.getCalledFromActivityIds()).hasSize(1);

        String calledFrom = dto.getCalledFromActivityIds().get(0);

        assertThat(calledFrom).isEqualTo("firstCallActivity");

      } else if (id.equals(anotherUserTaskProcess.getId())) {
        compareWith = anotherUserTaskProcess;
        assertThat(dto.getCalledFromActivityIds()).hasSize(1);

        String calledFrom = dto.getCalledFromActivityIds().get(0);

        assertThat(calledFrom).isEqualTo("secondCallActivity");

      } else {
        fail("Unexpected process definition: " + id);
      }

      assertThat(dto.getId()).isEqualTo(compareWith.getId());
      assertThat(dto.getKey()).isEqualTo(compareWith.getKey());
      assertThat(dto.getName()).isEqualTo(compareWith.getName());
      assertThat(dto.getVersion()).isEqualTo(compareWith.getVersion());
    }

  }

  @Test
  @Deployment(resources = {
      "processes/user-task-process.bpmn",
      "processes/another-user-task-process.bpmn",
      "processes/dynamic-call-activity.bpmn"
  })
  void testCalledProcessDefinitionByCallingDifferentProcessFromSameCallActivity() {
    Map<String, Object> vars1 = new HashMap<>();
    vars1.put("callProcess", "userTaskProcess");
    runtimeService.startProcessInstanceByKey("DynamicCallActivity", vars1);

    Map<String, Object> vars2 = new HashMap<>();
    vars2.put("callProcess", "anotherUserTaskProcess");
    runtimeService.startProcessInstanceByKey("DynamicCallActivity", vars2);

    ProcessDefinition dynamicCallActivity = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("DynamicCallActivity")
        .singleResult();

    resource = new ProcessDefinitionResource(getProcessEngine().getName(), dynamicCallActivity.getId());

    ProcessDefinition userTaskProcess = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("userTaskProcess")
        .singleResult();

    ProcessDefinition anotherUserTaskProcess = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("anotherUserTaskProcess")
        .singleResult();

    ProcessDefinitionQueryDto queryParameter = new ProcessDefinitionQueryDto();

    List<ProcessDefinitionDto> result = resource.queryCalledProcessDefinitions(queryParameter);
    assertThat(result)
      .isNotEmpty()
      .hasSize(2);

    ProcessDefinition compareWith = null;
    for (ProcessDefinitionDto dto : result) {
      String id = dto.getId();
      if (id.equals(userTaskProcess.getId())) {
        compareWith = userTaskProcess;
      } else if (id.equals(anotherUserTaskProcess.getId())) {
        compareWith = anotherUserTaskProcess;
      } else {
        fail("Unexpected process definition: " + id);
      }

      assertThat(dto.getId()).isEqualTo(compareWith.getId());
      assertThat(dto.getKey()).isEqualTo(compareWith.getKey());
      assertThat(dto.getName()).isEqualTo(compareWith.getName());
      assertThat(dto.getVersion()).isEqualTo(compareWith.getVersion());
      assertThat(dto.getCalledFromActivityIds()).hasSize(1);

      String calledFrom = dto.getCalledFromActivityIds().get(0);

      assertThat(calledFrom).isEqualTo("dynamicCallActivity");
    }
  }

  @Test
  @Deployment(resources = {
      "processes/user-task-process.bpmn",
      "processes/calling-user-task-process.bpmn",
      "processes/nested-calling-user-task-process.bpmn"
  })
  void testCalledProcessDefinitionQueryBySuperProcessDefinitionId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("NestedCallingUserTaskProcess");

    ProcessDefinition callingUserTaskProcess = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("CallingUserTaskProcess")
        .singleResult();

    ProcessDefinition userTaskProcess = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("userTaskProcess")
        .singleResult();

    resource = new ProcessDefinitionResource(getProcessEngine().getName(), callingUserTaskProcess.getId());
    ProcessDefinitionQueryDto queryParameter = new ProcessDefinitionQueryDto();
    queryParameter.setSuperProcessDefinitionId(processInstance.getProcessDefinitionId());

    List<ProcessDefinitionDto> result = resource.queryCalledProcessDefinitions(queryParameter);
    assertThat(result)
      .isNotEmpty()
      .hasSize(1);

    ProcessDefinitionDto dto = result.get(0);

    assertThat(dto.getId()).isEqualTo(userTaskProcess.getId());
    assertThat(dto.getKey()).isEqualTo(userTaskProcess.getKey());
    assertThat(dto.getName()).isEqualTo(userTaskProcess.getName());
    assertThat(dto.getVersion()).isEqualTo(userTaskProcess.getVersion());
    assertThat(dto.getCalledFromActivityIds()).hasSize(1);

    String calledFrom = dto.getCalledFromActivityIds().get(0);

    assertThat(calledFrom).isEqualTo("CallActivity_1");
  }

  @Test
  @Deployment(resources = {
      "processes/user-task-process.bpmn",
      "processes/another-user-task-process.bpmn",
      "processes/two-parallel-call-activities-calling-different-process.bpmn"
  })
  void testCalledProcessDefinitionQueryByActivityId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TwoParallelCallActivitiesCallingDifferentProcess");
    resource = new ProcessDefinitionResource(getProcessEngine().getName(), processInstance.getProcessDefinitionId());

    ProcessDefinition userTaskProcess = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("userTaskProcess")
        .singleResult();

    ProcessDefinition anotherUserTaskProcess = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("anotherUserTaskProcess")
        .singleResult();

    ProcessDefinitionQueryDto queryParameter1 = new ProcessDefinitionQueryDto();
    String[] activityIds1 = {"firstCallActivity"};
    queryParameter1.setActivityIdIn(activityIds1);

    List<ProcessDefinitionDto> result1 = resource.queryCalledProcessDefinitions(queryParameter1);
    assertThat(result1)
      .isNotEmpty()
      .hasSize(1);

    ProcessDefinitionDto dto1 = result1.get(0);

    assertThat(dto1.getId()).isEqualTo(userTaskProcess.getId());
    assertThat(dto1.getKey()).isEqualTo(userTaskProcess.getKey());
    assertThat(dto1.getName()).isEqualTo(userTaskProcess.getName());
    assertThat(dto1.getVersion()).isEqualTo(userTaskProcess.getVersion());
    assertThat(dto1.getCalledFromActivityIds()).hasSize(1);

    String calledFrom1 = dto1.getCalledFromActivityIds().get(0);

    assertThat(calledFrom1).isEqualTo("firstCallActivity");

    ProcessDefinitionQueryDto queryParameter2 = new ProcessDefinitionQueryDto();
    String[] activityIds2 = {"secondCallActivity"};
    queryParameter2.setActivityIdIn(activityIds2);

    List<ProcessDefinitionDto> result2 = resource.queryCalledProcessDefinitions(queryParameter2);
    assertThat(result2)
      .isNotEmpty()
      .hasSize(1);

    ProcessDefinitionDto dto2 = result2.get(0);

    assertThat(dto2.getId()).isEqualTo(anotherUserTaskProcess.getId());
    assertThat(dto2.getKey()).isEqualTo(anotherUserTaskProcess.getKey());
    assertThat(dto2.getName()).isEqualTo(anotherUserTaskProcess.getName());
    assertThat(dto2.getVersion()).isEqualTo(anotherUserTaskProcess.getVersion());
    assertThat(dto2.getCalledFromActivityIds()).hasSize(1);

    String calledFrom2 = dto2.getCalledFromActivityIds().get(0);

    assertThat(calledFrom2).isEqualTo("secondCallActivity");

    ProcessDefinitionQueryDto queryParameter3 = new ProcessDefinitionQueryDto();
    String[] activityIds3 = {"firstCallActivity", "secondCallActivity"};
    queryParameter3.setActivityIdIn(activityIds3);

    List<ProcessDefinitionDto> result3 = resource.queryCalledProcessDefinitions(queryParameter3);
    assertThat(result3)
      .isNotEmpty()
      .hasSize(2);

  }

  @Test
  @Deployment(resources = {
      "processes/user-task-process.bpmn",
      "processes/another-user-task-process.bpmn",
      "processes/two-parallel-call-activities-calling-different-process.bpmn"
  })
  void testCalledProcessDefinitionQueryByBusinessKey() {
    runtimeService.startProcessInstanceByKey("TwoParallelCallActivitiesCallingDifferentProcess", "aBusinessKey");
    runtimeService.startProcessInstanceByKey("TwoParallelCallActivitiesCallingDifferentProcess", "anotherBusinessKey");

    ProcessDefinition parallelProcess = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("TwoParallelCallActivitiesCallingDifferentProcess")
        .singleResult();

    resource = new ProcessDefinitionResource(getProcessEngine().getName(), parallelProcess.getId());

    ProcessDefinitionQueryDto queryParameter1 = new ProcessDefinitionQueryDto();
    queryParameter1.setBusinessKey("aBusinessKey");

    List<ProcessDefinitionDto> result1 = resource.queryCalledProcessDefinitions(queryParameter1);
    assertThat(result1)
      .isNotEmpty()
      .hasSize(2);

    ProcessDefinitionQueryDto queryParameter2 = new ProcessDefinitionQueryDto();
    queryParameter2.setBusinessKey("anotherBusinessKey");

    List<ProcessDefinitionDto> result2 = resource.queryCalledProcessDefinitions(queryParameter2);
    assertThat(result2)
      .isNotEmpty()
      .hasSize(2);
  }

  @Test
  @Deployment(resources = {
      "processes/user-task-process.bpmn",
      "processes/another-user-task-process.bpmn",
      "processes/two-parallel-call-activities-calling-different-process.bpmn"
  })
  void testCalledProcessDefinitionQueryByInvalidBusinessKey() {
    runtimeService.startProcessInstanceByKey("TwoParallelCallActivitiesCallingDifferentProcess", "aBusinessKey");

    ProcessDefinition parallelProcess = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("TwoParallelCallActivitiesCallingDifferentProcess")
        .singleResult();

    resource = new ProcessDefinitionResource(getProcessEngine().getName(), parallelProcess.getId());

    ProcessDefinitionQueryDto queryParameter1 = new ProcessDefinitionQueryDto();
    queryParameter1.setBusinessKey("anInvalidBusinessKey");

    List<ProcessDefinitionDto> result1 = resource.queryCalledProcessDefinitions(queryParameter1);
    assertThat(result1).isEmpty();
  }

  @Test
  @Deployment(resources = {
      "processes/variables-process-with-call-activity.bpmn",
      "processes/user-task-process.bpmn"
  })
  void testQueryWithBooleanVariable() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("variableProcessWithCallActivity");
    resource = new ProcessDefinitionResource(getProcessEngine().getName(), processInstance.getProcessDefinitionId());

    // when
    VariableQueryParameterDto variable = createVariableParameter("varboolean", EQUALS_OPERATOR_NAME, false);

    ProcessDefinitionQueryDto queryParameter = new ProcessDefinitionQueryDto();
    queryParameter.setVariables(List.of(variable));

    List<ProcessDefinitionDto> results = resource.queryCalledProcessDefinitions(queryParameter);

    // then
    assertThat(results).isEmpty();
  }

  @Test
  @Deployment(resources = {
      "processes/user-task-process.bpmn",
      "processes/another-user-task-process.bpmn",
      "processes/dynamic-call-activity.bpmn"
  })
  void testQueryWithVariable() {
    // given
    Map<String, Object> vars1 = new HashMap<>();
    vars1.put("callProcess", "userTaskProcess");
    vars1.put("aVariableName", "test_123");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("DynamicCallActivity", vars1);

    Map<String, Object> vars2 = new HashMap<>();
    vars2.put("callProcess", "anotherUserTaskProcess");
    vars1.put("aVariableName", "test_456");
    runtimeService.startProcessInstanceByKey("DynamicCallActivity", vars2);

    resource = new ProcessDefinitionResource(getProcessEngine().getName(), processInstance.getProcessDefinitionId());

    ProcessDefinition userTaskProcess = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("userTaskProcess")
        .singleResult();

    // when
    VariableQueryParameterDto variable = createVariableParameter("aVariableName", LIKE_OPERATOR_NAME, "test\\_1%");

    ProcessDefinitionQueryDto queryParameter = new ProcessDefinitionQueryDto();
    queryParameter.setVariables(List.of(variable));

    // then
    List<ProcessDefinitionDto> results = resource.queryCalledProcessDefinitions(queryParameter);
    assertThat(results)
      .isNotEmpty()
      .hasSize(1);

    ProcessDefinitionDto dto = results.get(0);

    assertThat(dto.getId()).isEqualTo(userTaskProcess.getId());
    assertThat(dto.getKey()).isEqualTo(userTaskProcess.getKey());
    assertThat(dto.getName()).isEqualTo(userTaskProcess.getName());
  }

  @Test
  @Deployment(resources = {
      "processes/variables-process-with-call-activity.bpmn",
      "processes/user-task-process.bpmn"
  })
  void testQueryWithStringVariable() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("variableProcessWithCallActivity");
    resource = new ProcessDefinitionResource(getProcessEngine().getName(), processInstance.getProcessDefinitionId());

    // when
    VariableQueryParameterDto variable = createVariableParameter("varstring", LIKE_OPERATOR_NAME, "B%");

    ProcessDefinitionQueryDto queryParameter = new ProcessDefinitionQueryDto();
    queryParameter.setVariables(List.of(variable));

    List<ProcessDefinitionDto> results = resource.queryCalledProcessDefinitions(queryParameter);

    // then
    assertThat(results).isEmpty();
  }

  @Test
  @Deployment(resources = {
      "processes/variables-process-with-call-activity.bpmn",
      "processes/user-task-process.bpmn"
  })
  void testQueryWithFloatVariable() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("variableProcessWithCallActivity");
    resource = new ProcessDefinitionResource(getProcessEngine().getName(), processInstance.getProcessDefinitionId());

    // when
    VariableQueryParameterDto variable = createVariableParameter("varfloat", EQUALS_OPERATOR_NAME, 0.0);

    ProcessDefinitionQueryDto queryParameter = new ProcessDefinitionQueryDto();
    queryParameter.setVariables(List.of(variable));

    List<ProcessDefinitionDto> results = resource.queryCalledProcessDefinitions(queryParameter);

    // then
    assertThat(results).isEmpty();
  }

  @Test
  @Deployment(resources = {
      "processes/variables-process-with-call-activity.bpmn",
      "processes/user-task-process.bpmn"
  })
  void testQueryWithIntegerVariable() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("variableProcessWithCallActivity");
    resource = new ProcessDefinitionResource(getProcessEngine().getName(), processInstance.getProcessDefinitionId());

    // when
    VariableQueryParameterDto variable = createVariableParameter("varinteger", NOT_EQUALS_OPERATOR_NAME, 12);

    ProcessDefinitionQueryDto queryParameter = new ProcessDefinitionQueryDto();
    queryParameter.setVariables(List.of(variable));

    List<ProcessDefinitionDto> results = resource.queryCalledProcessDefinitions(queryParameter);

    // then
    assertThat(results).isEmpty();
  }

  @Test
  @Deployment(resources = {
      "processes/variables-process-with-call-activity.bpmn",
      "processes/user-task-process.bpmn"
  })
  @RequiredDatabase(excludes = {DbSqlSessionFactory.MYSQL, DbSqlSessionFactory.MARIADB})
  void testQueryWithComplexVariableFilter() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("variableProcessWithCallActivity");
    resource = new ProcessDefinitionResource(getProcessEngine().getName(), processInstance.getProcessDefinitionId());

    // when
    ProcessDefinitionQueryDto queryParameter = new ProcessDefinitionQueryDto();
    queryParameter.setVariables(List.of(
        createVariableParameter("varinteger", VariableQueryParameterDto.GREATER_THAN_OPERATOR_NAME, 11),
        createVariableParameter("varinteger", VariableQueryParameterDto.LESS_THAN_OR_EQUALS_OPERATOR_NAME, 12),
        createVariableParameter("varinteger", VariableQueryParameterDto.EQUALS_OPERATOR_NAME, 12),
        createVariableParameter("varboolean", VariableQueryParameterDto.EQUALS_OPERATOR_NAME, true),
        createVariableParameter("varboolean", VariableQueryParameterDto.NOT_EQUALS_OPERATOR_NAME, false),
        createVariableParameter("varstring", VariableQueryParameterDto.LIKE_OPERATOR_NAME, "F%"),
        createVariableParameter("varstring", VariableQueryParameterDto.EQUALS_OPERATOR_NAME, "FOO"),
        createVariableParameter("varstring", VariableQueryParameterDto.NOT_EQUALS_OPERATOR_NAME, "BAR"),
        createVariableParameter("varfloat", VariableQueryParameterDto.EQUALS_OPERATOR_NAME, 12.12),
        createVariableParameter("varfloat", VariableQueryParameterDto.NOT_EQUALS_OPERATOR_NAME, 13.0),
        createVariableParameter("varfloat", VariableQueryParameterDto.LESS_THAN_OR_EQUALS_OPERATOR_NAME, 12.13)));

    // then
    List<ProcessDefinitionDto> results = resource.queryCalledProcessDefinitions(queryParameter);
    assertThat(results).hasSize(1);

  }

  @Test
  void shouldNotThrowExceptionWhenQueryUnbounded() {
    // given
    resource = new ProcessDefinitionResource(getProcessEngine().getName(), "anId");

    processEngineConfiguration.setQueryMaxResultsLimit(10);

    identityService.setAuthenticatedUserId("foo");

    ProcessDefinitionQueryDto dto = new ProcessDefinitionQueryDto();

    // when + then
    assertThatCode(() -> resource.queryCalledProcessDefinitions(dto))
      .doesNotThrowAnyException();
  }

  private VariableQueryParameterDto createVariableParameter(String name, String operator, Object value) {
    VariableQueryParameterDto variable = new VariableQueryParameterDto();
    variable.setName(name);
    variable.setOperator(operator);
    variable.setValue(value);

    return variable;
  }

}
