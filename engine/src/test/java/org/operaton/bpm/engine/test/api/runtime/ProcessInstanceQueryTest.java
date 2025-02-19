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
package org.operaton.bpm.engine.test.api.runtime;

import java.text.SimpleDateFormat;
import java.util.*;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.impl.ProcessInstanceQueryImpl;
import org.operaton.bpm.engine.impl.util.ImmutablePair;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.api.runtime.migration.models.CompensationModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.processInstanceByBusinessKey;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.processInstanceByProcessDefinitionId;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.processInstanceByProcessInstanceId;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.verifySorting;

/**
 * @author Joram Barrez
 * @author Frederik Heremans
 * @author Falko Menge
 */
public class ProcessInstanceQueryTest {

  public static final BpmnModelInstance FORK_JOIN_SUB_PROCESS_MODEL = ProcessModels.newModel()
    .startEvent()
    .subProcess("subProcess")
    .embeddedSubProcess()
      .startEvent()
      .parallelGateway("fork")
        .userTask("userTask1")
        .name("completeMe")
      .parallelGateway("join")
      .endEvent()
      .moveToNode("fork")
        .userTask("userTask2")
      .connectTo("join")
    .subProcessDone()
    .endEvent()
    .done();

  protected ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  protected ProcessEngineTestRule testHelper = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testHelper);

  private static final String PROCESS_DEFINITION_KEY = "oneTaskProcess";
  private static final String PROCESS_DEFINITION_KEY_2 = "otherOneTaskProcess";

  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;
  protected ManagementService managementService;
  protected CaseService caseService;

  protected List<String> processInstanceIds;

  @Before
  public void initServices() {
    runtimeService = engineRule.getRuntimeService();
    repositoryService = engineRule.getRepositoryService();
    managementService = engineRule.getManagementService();
    caseService = engineRule.getCaseService();

    deployTestProcesses();
  }


  /**
   * Setup starts 4 process instances of oneTaskProcess
   * and 1 instance of otherOneTaskProcess
   */
  public void deployTestProcesses() {
    org.operaton.bpm.engine.repository.Deployment deployment = engineRule.getRepositoryService().createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
      .addClasspathResource("org/operaton/bpm/engine/test/api/runtime/otherOneTaskProcess.bpmn20.xml")
      .deploy();

    engineRule.manageDeployment(deployment);

    processInstanceIds = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      processInstanceIds.add(runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY, i + "").getId());
    }
    processInstanceIds.add(runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY_2, "businessKey_123").getId());
  }


  @Test
  public void testQueryNoSpecificsList() {
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.count()).isEqualTo(5);
    assertThat(query.list()).hasSize(5);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testQueryNoSpecificsDeploymentIdMappings() {
    // given
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
    ImmutablePair<String, String>[] expectedMappings = processInstanceIds.stream()
        .map(id -> new ImmutablePair<>(deploymentId, id))
        .toList()
        .toArray(new ImmutablePair[0]);
    // when
    List<ImmutablePair<String, String>> mappings = engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired().execute(c -> {
      ProcessInstanceQuery query = c.getProcessEngineConfiguration().getRuntimeService().createProcessInstanceQuery();
      return ((ProcessInstanceQueryImpl) query).listDeploymentIdMappings();
    });
    // then
    assertThat(mappings)
            .hasSize(5)
            .containsExactlyInAnyOrder(expectedMappings);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testQueryNoSpecificsDeploymentIdMappingsInDifferentDeployments() {
    // given
    List<ImmutablePair<String, String>> expectedMappings = new ArrayList<>();
    String deploymentIdOne = repositoryService.createDeploymentQuery().singleResult().getId();
    processInstanceIds.stream()
      .map(id -> new ImmutablePair<>(deploymentIdOne, id))
      .forEach(expectedMappings::add);
    org.operaton.bpm.engine.repository.Deployment deploymentTwo = repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
        .deploy();
    engineRule.manageDeployment(deploymentTwo);
    ImmutablePair<String, String> newMapping = new ImmutablePair<>(deploymentTwo.getId(),
        runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId());
    expectedMappings.add(newMapping);
    // when
    List<ImmutablePair<String, String>> mappings = engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired().execute(c -> {
      ProcessInstanceQuery query = c.getProcessEngineConfiguration().getRuntimeService().createProcessInstanceQuery();
      return ((ProcessInstanceQueryImpl) query).listDeploymentIdMappings();
    });
    // then
    assertThat(mappings)
      .hasSize(6)
      .containsExactlyInAnyOrder(expectedMappings.toArray(new ImmutablePair[0]));
    // ... and the items are sorted in ascending order by deployment id
    assertThat(mappings.get(mappings.size() - 1)).isEqualTo(newMapping);
  }

  @Test
  public void testQueryNoSpecificsSingleResult() {
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    try {
      query.singleResult();
      fail("Exception expected");
    } catch (ProcessEngineException e) {
      // Exception is expected
    }
  }

  @Test
  public void testQueryByProcessDefinitionKeySingleResult() {
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY_2);
    assertThat(query.count()).isEqualTo(1);
    assertThat(query.list()).hasSize(1);
    assertThat(query.singleResult()).isNotNull();
  }

  @Test
  public void testQueryByInvalidProcessDefinitionKey() {
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("invalid").singleResult()).isNull();
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("invalid").list()).isEmpty();
  }

  @Test
  public void testQueryByProcessDefinitionKeyMultipleResults() {
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY);
    assertThat(query.count()).isEqualTo(4);
    assertThat(query.list()).hasSize(4);

    try {
      query.singleResult();
      fail("Exception expected");
    } catch (ProcessEngineException e) {
      // Exception is expected
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testQueryByProcessDefinitionKeyDeploymentIdMappings() {
    // given
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
    List<String> relevantIds = engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired().execute(c -> {
      ProcessInstanceQuery query = c.getProcessEngineConfiguration().getRuntimeService().createProcessInstanceQuery()
          .processDefinitionKey(PROCESS_DEFINITION_KEY);
      return ((ProcessInstanceQueryImpl) query).listIds();
    });
    List<ImmutablePair<String, String>> expectedMappings = relevantIds.stream()
        .map(id -> new ImmutablePair<>(deploymentId, id))
        .toList();
    // when
    List<ImmutablePair<String, String>> mappings = engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired().execute(c -> {
      ProcessInstanceQuery query = c.getProcessEngineConfiguration().getRuntimeService().createProcessInstanceQuery()
          .processDefinitionKey(PROCESS_DEFINITION_KEY);
      return ((ProcessInstanceQueryImpl)query).listDeploymentIdMappings();
    });
    // then
    assertThat(mappings)
      .hasSize(4)
      .containsExactlyInAnyOrder(expectedMappings.toArray(new ImmutablePair[0]));
  }

  @Test
  public void testQueryByProcessDefinitionKeyIn() {
    // given (deploy another process)
    ProcessDefinition oneTaskProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    String oneTaskProcessDefinitionId = oneTaskProcessDefinition.getId();
    runtimeService.startProcessInstanceById(oneTaskProcessDefinitionId);
    runtimeService.startProcessInstanceById(oneTaskProcessDefinitionId);

    // assume
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(7L);

    // when
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery()
      .processDefinitionKeyIn(PROCESS_DEFINITION_KEY, PROCESS_DEFINITION_KEY_2);

    // then
    assertThat(query.count()).isEqualTo(5L);
    assertThat(query.list()).hasSize(5);
  }

  @Test
  public void testQueryByNonExistingProcessDefinitionKeyIn() {
    // when
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery()
      .processDefinitionKeyIn("not-existing-key");

    // then
    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
  }

  @Test
  public void testQueryByOneInvalidProcessDefinitionKeyIn() {
    var processInstanceQuery = runtimeService.createProcessInstanceQuery();
    try {
      // when
      processInstanceQuery.processDefinitionKeyIn((String) null);
      fail("Exception expected");
    } catch(ProcessEngineException expected) {
      // then Exception is expected
    }
  }

  @Test
  public void testQueryByMultipleInvalidProcessDefinitionKeyIn() {
    var processInstanceQuery = runtimeService.createProcessInstanceQuery();
    try {
      // when
      processInstanceQuery.processDefinitionKeyIn(PROCESS_DEFINITION_KEY, null);
      fail("Exception expected");
    } catch(ProcessEngineException expected) {
      // Exception is expected
    }
  }

  @Test
  public void testQueryByProcessDefinitionKeyNotIn() {
    // given (deploy another process)
    ProcessDefinition oneTaskProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    String oneTaskProcessDefinitionId = oneTaskProcessDefinition.getId();
    runtimeService.startProcessInstanceById(oneTaskProcessDefinitionId);
    runtimeService.startProcessInstanceById(oneTaskProcessDefinitionId);

    // assume
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(7L);

    // when
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery()
      .processDefinitionKeyNotIn(PROCESS_DEFINITION_KEY, PROCESS_DEFINITION_KEY_2);

    // then
    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.list()).hasSize(2);
  }

  @Test
  public void testQueryByNonExistingProcessDefinitionKeyNotIn() {
    // when
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery()
      .processDefinitionKeyNotIn("not-existing-key");

    // then
    assertThat(query.count()).isEqualTo(5L);
    assertThat(query.list()).hasSize(5);
  }

  @Test
  public void testQueryByOneInvalidProcessDefinitionKeyNotIn() {
    var processInstanceQuery = runtimeService.createProcessInstanceQuery();
    try {
      // when
      processInstanceQuery.processDefinitionKeyNotIn((String) null);
      fail("Exception expected");
    } catch(ProcessEngineException expected) {
      // then Exception is expected
    }
  }

  @Test
  public void testQueryByMultipleInvalidProcessDefinitionKeyNotIn() {
    var processInstanceQuery = runtimeService.createProcessInstanceQuery();
    try {
      // when
      processInstanceQuery.processDefinitionKeyNotIn(PROCESS_DEFINITION_KEY, null);
      fail("Exception expected");
    } catch(ProcessEngineException expected) {
      // then Exception is expected
    }
  }

  @Test
  public void testQueryByProcessInstanceId() {
    for (String processInstanceId : processInstanceIds) {
      assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult()).isNotNull();
      assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).list()).hasSize(1);
    }
  }

  @Test
  public void testQueryByBusinessKeyAndProcessDefinitionKey() {
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("0", PROCESS_DEFINITION_KEY).count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("1", PROCESS_DEFINITION_KEY).count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("2", PROCESS_DEFINITION_KEY).count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("3", PROCESS_DEFINITION_KEY).count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("businessKey_123", PROCESS_DEFINITION_KEY_2).count()).isEqualTo(1);
  }

  @Test
  public void testQueryByBusinessKey() {
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("0").count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("1").count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("businessKey_123").count()).isEqualTo(1);
  }

  @Test
  public void testQueryByBusinessKeyLike(){
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKeyLike("business%").count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKeyLike("%sinessKey\\_123").count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKeyLike("%siness%").count()).isEqualTo(1);
  }

  @Test
  public void testQueryByInvalidBusinessKey() {
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("invalid").count()).isZero();
    var processInstanceQuery = runtimeService.createProcessInstanceQuery();

    try {
      processInstanceQuery.processInstanceBusinessKey(null);
      fail("Exception expected");
    } catch(ProcessEngineException ignored) {
      assertThat(ignored.getMessage()).isEqualTo("Business key is null");
    }
  }

  @Test
  public void testQueryByInvalidProcessInstanceId() {
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId("I do not exist").singleResult()).isNull();
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId("I do not exist").list()).isEmpty();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/superProcess.bpmn20.xml",
                           "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"})
  public void testQueryBySuperProcessInstanceId() {
    ProcessInstance superProcessInstance = runtimeService.startProcessInstanceByKey("subProcessQueryTest");

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().superProcessInstanceId(superProcessInstance.getId());
    ProcessInstance subProcessInstance = query.singleResult();
    assertThat(subProcessInstance).isNotNull();
    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  public void testQueryByInvalidSuperProcessInstanceId() {
    assertThat(runtimeService.createProcessInstanceQuery().superProcessInstanceId("invalid").singleResult()).isNull();
    assertThat(runtimeService.createProcessInstanceQuery().superProcessInstanceId("invalid").list()).isEmpty();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/superProcess.bpmn20.xml",
                           "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"})
  @Test
  public void testQueryBySubProcessInstanceId() {
    ProcessInstance superProcessInstance = runtimeService.startProcessInstanceByKey("subProcessQueryTest");

    ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(superProcessInstance.getId()).singleResult();
    assertThat(subProcessInstance).isNotNull();
    assertThat(runtimeService.createProcessInstanceQuery().subProcessInstanceId(subProcessInstance.getId()).singleResult().getId()).isEqualTo(superProcessInstance.getId());
  }

  @Test
  public void testQueryByInvalidSubProcessInstanceId() {
    assertThat(runtimeService.createProcessInstanceQuery().subProcessInstanceId("invalid").singleResult()).isNull();
    assertThat(runtimeService.createProcessInstanceQuery().subProcessInstanceId("invalid").list()).isEmpty();
  }

  // Nested subprocess make the query complexer, hence this test
  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/superProcessWithNestedSubProcess.bpmn20.xml",
                           "org/operaton/bpm/engine/test/api/runtime/nestedSubProcess.bpmn20.xml",
                           "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"})
  public void testQueryBySuperProcessInstanceIdNested() {
    ProcessInstance superProcessInstance = runtimeService.startProcessInstanceByKey("nestedSubProcessQueryTest");

    ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(superProcessInstance.getId()).singleResult();
    assertThat(subProcessInstance).isNotNull();

    ProcessInstance nestedSubProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(subProcessInstance.getId()).singleResult();
    assertThat(nestedSubProcessInstance).isNotNull();
  }

  //Nested subprocess make the query complexer, hence this test
  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/superProcessWithNestedSubProcess.bpmn20.xml",
          "org/operaton/bpm/engine/test/api/runtime/nestedSubProcess.bpmn20.xml",
          "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"})
  public void testQueryBySubProcessInstanceIdNested() {
    ProcessInstance superProcessInstance = runtimeService.startProcessInstanceByKey("nestedSubProcessQueryTest");

    ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(superProcessInstance.getId()).singleResult();
    assertThat(runtimeService.createProcessInstanceQuery().subProcessInstanceId(subProcessInstance.getId()).singleResult().getId()).isEqualTo(superProcessInstance.getId());

    ProcessInstance nestedSubProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(subProcessInstance.getId()).singleResult();
    assertThat(runtimeService.createProcessInstanceQuery().subProcessInstanceId(nestedSubProcessInstance.getId()).singleResult().getId()).isEqualTo(subProcessInstance.getId());
  }

  @Test
  public void testQueryPaging() {
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).count()).isEqualTo(4);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).listPage(0, 2)).hasSize(2);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).listPage(1, 3)).hasSize(3);
  }

  @Test
  public void testQuerySorting() {
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().orderByProcessInstanceId().asc().list();
    assertThat(processInstances).hasSize(5);
    verifySorting(processInstances, processInstanceByProcessInstanceId());

    processInstances = runtimeService.createProcessInstanceQuery().orderByProcessDefinitionId().asc().list();
    assertThat(processInstances).hasSize(5);
    verifySorting(processInstances, processInstanceByProcessDefinitionId());

    processInstances = runtimeService.createProcessInstanceQuery().orderByBusinessKey().asc().list();
    assertThat(processInstances).hasSize(5);
    verifySorting(processInstances, processInstanceByBusinessKey());

    assertThat(runtimeService.createProcessInstanceQuery().orderByProcessDefinitionKey().asc().list()).hasSize(5);

    assertThat(runtimeService.createProcessInstanceQuery().orderByProcessInstanceId().desc().list()).hasSize(5);
    assertThat(runtimeService.createProcessInstanceQuery().orderByProcessDefinitionId().desc().list()).hasSize(5);
    assertThat(runtimeService.createProcessInstanceQuery().orderByProcessDefinitionKey().desc().list()).hasSize(5);
    assertThat(runtimeService.createProcessInstanceQuery().orderByBusinessKey().desc().list()).hasSize(5);

    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).orderByProcessInstanceId().asc().list()).hasSize(4);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).orderByProcessInstanceId().desc().list()).hasSize(4);
  }

  @Test
  public void testQueryInvalidSorting() {
    var processInstanceQuery = runtimeService.createProcessInstanceQuery().orderByProcessDefinitionId();
    try {
      processInstanceQuery.list(); // asc - desc not called -> exception
      fail("Exception expected");
    }catch (ProcessEngineException ignored) {
      // expected
    }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryStringVariable() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("stringVar", "abcdef");
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("stringVar", "abcdef");
    vars.put("stringVar2", "ghijkl");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("stringVar", "azerty");
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    // Test EQUAL on single string variable, should result in 2 matches
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().variableValueEquals("stringVar", "abcdef");
    List<ProcessInstance> processInstances = query.list();
    assertThat(processInstances)
            .isNotNull()
            .hasSize(2);

    // Test EQUAL on two string variables, should result in single match
    query = runtimeService.createProcessInstanceQuery().variableValueEquals("stringVar", "abcdef").variableValueEquals("stringVar2", "ghijkl");
    ProcessInstance resultInstance = query.singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance2.getId());

    // Test NOT_EQUAL, should return only 1 resultInstance
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueNotEquals("stringVar", "abcdef").singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    // Test GREATER_THAN, should return only matching 'azerty'
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("stringVar", "abcdef").singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("stringVar", "z").singleResult();
    assertThat(resultInstance).isNull();

    // Test GREATER_THAN_OR_EQUAL, should return 3 results
    assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("stringVar", "abcdef").count()).isEqualTo(3);
    assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("stringVar", "z").count()).isZero();

    // Test LESS_THAN, should return 2 results
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThan("stringVar", "abcdeg").list();
    assertThat(processInstances).hasSize(2);
    List<String> expecedIds = Arrays.asList(processInstance1.getId(), processInstance2.getId());
    List<String> ids = new ArrayList<>(Arrays.asList(processInstances.get(0).getId(), processInstances.get(1).getId()));
    ids.removeAll(expecedIds);
    assertThat(ids).isEmpty();

    assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThan("stringVar", "abcdef").count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("stringVar", "z").count()).isEqualTo(3);

    // Test LESS_THAN_OR_EQUAL
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("stringVar", "abcdef").list();
    assertThat(processInstances).hasSize(2);
    expecedIds = Arrays.asList(processInstance1.getId(), processInstance2.getId());
    ids = new ArrayList<>(Arrays.asList(processInstances.get(0).getId(), processInstances.get(1).getId()));
    ids.removeAll(expecedIds);
    assertThat(ids).isEmpty();

    assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("stringVar", "z").count()).isEqualTo(3);
    assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("stringVar", "aa").count()).isZero();

    // Test LIKE
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueLike("stringVar", "azert%").singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    resultInstance = runtimeService.createProcessInstanceQuery().variableValueLike("stringVar", "%y").singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    resultInstance = runtimeService.createProcessInstanceQuery().variableValueLike("stringVar", "%zer%").singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    assertThat(runtimeService.createProcessInstanceQuery().variableValueLike("stringVar", "a%").count()).isEqualTo(3);
    assertThat(runtimeService.createProcessInstanceQuery().variableValueLike("stringVar", "%x%").count()).isZero();

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryLongVariable() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("longVar", 12345L);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("longVar", 12345L);
    vars.put("longVar2", 67890L);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("longVar", 55555L);
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    // Query on single long variable, should result in 2 matches
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().variableValueEquals("longVar", 12345L);
    List<ProcessInstance> processInstances = query.list();
    assertThat(processInstances)
            .isNotNull()
            .hasSize(2);

    // Query on two long variables, should result in single match
    query = runtimeService.createProcessInstanceQuery().variableValueEquals("longVar", 12345L).variableValueEquals("longVar2", 67890L);
    ProcessInstance resultInstance = query.singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance2.getId());

    // Query with unexisting variable value
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueEquals("longVar", 999L).singleResult();
    assertThat(resultInstance).isNull();

    // Test NOT_EQUALS
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueNotEquals("longVar", 12345L).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    // Test GREATER_THAN
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("longVar", 44444L).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThan("longVar", 55555L).count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThan("longVar", 1L).count()).isEqualTo(3);

    // Test GREATER_THAN_OR_EQUAL
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("longVar", 44444L).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("longVar", 55555L).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("longVar", 1L).count()).isEqualTo(3);

    // Test LESS_THAN
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThan("longVar", 55555L).list();
    assertThat(processInstances).hasSize(2);

    List<String> expecedIds = Arrays.asList(processInstance1.getId(), processInstance2.getId());
    List<String> ids = new ArrayList<>(Arrays.asList(processInstances.get(0).getId(), processInstances.get(1).getId()));
    ids.removeAll(expecedIds);
    assertThat(ids).isEmpty();

    assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThan("longVar", 12345L).count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThan("longVar", 66666L).count()).isEqualTo(3);

    // Test LESS_THAN_OR_EQUAL
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("longVar", 55555L).list();
    assertThat(processInstances).hasSize(3);

    assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("longVar", 12344L).count()).isZero();

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryDoubleVariable() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("doubleVar", 12345.6789);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("doubleVar", 12345.6789);
    vars.put("doubleVar2", 9876.54321);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("doubleVar", 55555.5555);
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    // Query on single double variable, should result in 2 matches
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().variableValueEquals("doubleVar", 12345.6789);
    List<ProcessInstance> processInstances = query.list();
    assertThat(processInstances)
            .isNotNull()
            .hasSize(2);

    // Query on two double variables, should result in single value
    query = runtimeService.createProcessInstanceQuery().variableValueEquals("doubleVar", 12345.6789).variableValueEquals("doubleVar2", 9876.54321);
    ProcessInstance resultInstance = query.singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance2.getId());

    // Query with unexisting variable value
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueEquals("doubleVar", 9999.99).singleResult();
    assertThat(resultInstance).isNull();

    // Test NOT_EQUALS
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueNotEquals("doubleVar", 12345.6789).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    // Test GREATER_THAN
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("doubleVar", 44444.4444).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThan("doubleVar", 55555.5555).count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThan("doubleVar", 1.234).count()).isEqualTo(3);

    // Test GREATER_THAN_OR_EQUAL
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("doubleVar", 44444.4444).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("doubleVar", 55555.5555).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("doubleVar", 1.234).count()).isEqualTo(3);

    // Test LESS_THAN
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThan("doubleVar", 55555.5555).list();
    assertThat(processInstances).hasSize(2);

    List<String> expecedIds = Arrays.asList(processInstance1.getId(), processInstance2.getId());
    List<String> ids = new ArrayList<>(Arrays.asList(processInstances.get(0).getId(), processInstances.get(1).getId()));
    ids.removeAll(expecedIds);
    assertThat(ids).isEmpty();

    assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThan("doubleVar", 12345.6789).count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThan("doubleVar", 66666.6666).count()).isEqualTo(3);

    // Test LESS_THAN_OR_EQUAL
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("doubleVar", 55555.5555).list();
    assertThat(processInstances).hasSize(3);

    assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("doubleVar", 12344.6789).count()).isZero();

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryIntegerVariable() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("integerVar", 12345);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("integerVar", 12345);
    vars.put("integerVar2", 67890);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("integerVar", 55555);
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    // Query on single integer variable, should result in 2 matches
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().variableValueEquals("integerVar", 12345);
    List<ProcessInstance> processInstances = query.list();
    assertThat(processInstances)
            .isNotNull()
            .hasSize(2);

    // Query on two integer variables, should result in single value
    query = runtimeService.createProcessInstanceQuery().variableValueEquals("integerVar", 12345).variableValueEquals("integerVar2", 67890);
    ProcessInstance resultInstance = query.singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance2.getId());

    // Query with unexisting variable value
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueEquals("integerVar", 9999).singleResult();
    assertThat(resultInstance).isNull();

    // Test NOT_EQUALS
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueNotEquals("integerVar", 12345).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    // Test GREATER_THAN
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("integerVar", 44444).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThan("integerVar", 55555).count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThan("integerVar", 1).count()).isEqualTo(3);

    // Test GREATER_THAN_OR_EQUAL
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("integerVar", 44444).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("integerVar", 55555).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("integerVar", 1).count()).isEqualTo(3);

    // Test LESS_THAN
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThan("integerVar", 55555).list();
    assertThat(processInstances).hasSize(2);

    List<String> expecedIds = Arrays.asList(processInstance1.getId(), processInstance2.getId());
    List<String> ids = new ArrayList<>(Arrays.asList(processInstances.get(0).getId(), processInstances.get(1).getId()));
    ids.removeAll(expecedIds);
    assertThat(ids).isEmpty();

    assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThan("integerVar", 12345).count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThan("integerVar", 66666).count()).isEqualTo(3);

    // Test LESS_THAN_OR_EQUAL
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("integerVar", 55555).list();
    assertThat(processInstances).hasSize(3);

    assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("integerVar", 12344).count()).isZero();

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryShortVariable() {
    Map<String, Object> vars = new HashMap<>();
    short shortVar = 1234;
    vars.put("shortVar", shortVar);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    short shortVar2 = 6789;
    vars = new HashMap<>();
    vars.put("shortVar", shortVar);
    vars.put("shortVar2", shortVar2);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("shortVar", (short)5555);
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    // Query on single short variable, should result in 2 matches
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().variableValueEquals("shortVar", shortVar);
    List<ProcessInstance> processInstances = query.list();
    assertThat(processInstances)
            .isNotNull()
            .hasSize(2);

    // Query on two short variables, should result in single value
    query = runtimeService.createProcessInstanceQuery().variableValueEquals("shortVar", shortVar).variableValueEquals("shortVar2", shortVar2);
    ProcessInstance resultInstance = query.singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance2.getId());

    // Query with unexisting variable value
    short unexistingValue = (short)9999;
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueEquals("shortVar", unexistingValue).singleResult();
    assertThat(resultInstance).isNull();

    // Test NOT_EQUALS
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueNotEquals("shortVar", (short)1234).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    // Test GREATER_THAN
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("shortVar", (short)4444).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThan("shortVar", (short) 5555).count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThan("shortVar", (short) 1).count()).isEqualTo(3);

    // Test GREATER_THAN_OR_EQUAL
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("shortVar", (short)4444).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("shortVar", (short)5555).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("shortVar", (short) 1).count()).isEqualTo(3);

    // Test LESS_THAN
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThan("shortVar", (short)5555).list();
    assertThat(processInstances).hasSize(2);

    List<String> expecedIds = Arrays.asList(processInstance1.getId(), processInstance2.getId());
    List<String> ids = new ArrayList<>(Arrays.asList(processInstances.get(0).getId(), processInstances.get(1).getId()));
    ids.removeAll(expecedIds);
    assertThat(ids).isEmpty();

    assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThan("shortVar", (short) 1234).count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThan("shortVar", (short) 6666).count()).isEqualTo(3);

    // Test LESS_THAN_OR_EQUAL
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("shortVar", (short)5555).list();
    assertThat(processInstances).hasSize(3);

    assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("shortVar", (short) 1233).count()).isZero();

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryDateVariable() throws Exception {
    Map<String, Object> vars = new HashMap<>();
    Date date1 = Calendar.getInstance().getTime();
    vars.put("dateVar", date1);

    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    Date date2 = Calendar.getInstance().getTime();
    vars = new HashMap<>();
    vars.put("dateVar", date1);
    vars.put("dateVar2", date2);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    Calendar nextYear = Calendar.getInstance();
    nextYear.add(Calendar.YEAR, 1);
    vars = new HashMap<>();
    vars.put("dateVar",nextYear.getTime());
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    Calendar nextMonth = Calendar.getInstance();
    nextMonth.add(Calendar.MONTH, 1);

    Calendar twoYearsLater = Calendar.getInstance();
    twoYearsLater.add(Calendar.YEAR, 2);

    Calendar oneYearAgo = Calendar.getInstance();
    oneYearAgo.add(Calendar.YEAR, -1);

    // Query on single short variable, should result in 2 matches
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().variableValueEquals("dateVar", date1);
    List<ProcessInstance> processInstances = query.list();
    assertThat(processInstances)
            .isNotNull()
            .hasSize(2);

    // Query on two short variables, should result in single value
    query = runtimeService.createProcessInstanceQuery().variableValueEquals("dateVar", date1).variableValueEquals("dateVar2", date2);
    ProcessInstance resultInstance = query.singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance2.getId());

    // Query with unexisting variable value
    Date unexistingDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("01/01/1989 12:00:00");
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueEquals("dateVar", unexistingDate).singleResult();
    assertThat(resultInstance).isNull();

    // Test NOT_EQUALS
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueNotEquals("dateVar", date1).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    // Test GREATER_THAN
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("dateVar", nextMonth.getTime()).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThan("dateVar", nextYear.getTime()).count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThan("dateVar", oneYearAgo.getTime()).count()).isEqualTo(3);

    // Test GREATER_THAN_OR_EQUAL
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("dateVar", nextMonth.getTime()).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("dateVar", nextYear.getTime()).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("dateVar", oneYearAgo.getTime()).count()).isEqualTo(3);

    // Test LESS_THAN
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThan("dateVar", nextYear.getTime()).list();
    assertThat(processInstances).hasSize(2);

    List<String> expecedIds = Arrays.asList(processInstance1.getId(), processInstance2.getId());
    List<String> ids = new ArrayList<>(Arrays.asList(processInstances.get(0).getId(), processInstances.get(1).getId()));
    ids.removeAll(expecedIds);
    assertThat(ids).isEmpty();

    assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThan("dateVar", date1).count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThan("dateVar", twoYearsLater.getTime()).count()).isEqualTo(3);

    // Test LESS_THAN_OR_EQUAL
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("dateVar", nextYear.getTime()).list();
    assertThat(processInstances).hasSize(3);

    assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("dateVar", oneYearAgo.getTime()).count()).isZero();

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testBooleanVariable() {

    // TEST EQUALS
    HashMap<String, Object> vars = new HashMap<>();
    vars.put("booleanVar", true);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("booleanVar", false);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().variableValueEquals("booleanVar", true).list();

    assertThat(instances)
            .isNotNull()
            .hasSize(1);
    assertThat(instances.get(0).getId()).isEqualTo(processInstance1.getId());

    instances = runtimeService.createProcessInstanceQuery().variableValueEquals("booleanVar", false).list();

    assertThat(instances)
            .isNotNull()
            .hasSize(1);
    assertThat(instances.get(0).getId()).isEqualTo(processInstance2.getId());

    // TEST NOT_EQUALS
    instances = runtimeService.createProcessInstanceQuery().variableValueNotEquals("booleanVar", true).list();

    assertThat(instances)
            .isNotNull()
            .hasSize(1);
    assertThat(instances.get(0).getId()).isEqualTo(processInstance2.getId());

    instances = runtimeService.createProcessInstanceQuery().variableValueNotEquals("booleanVar", false).list();

    assertThat(instances)
            .isNotNull()
            .hasSize(1);
    assertThat(instances.get(0).getId()).isEqualTo(processInstance1.getId());
    var processInstanceQuery = runtimeService.createProcessInstanceQuery();

    // Test unsupported operations
    try {
      processInstanceQuery.variableValueGreaterThan("booleanVar", true);
      fail("Exception expected");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).contains("Booleans and null cannot be used in 'greater than' condition");
    }

    try {
      processInstanceQuery.variableValueGreaterThanOrEqual("booleanVar", true);
      fail("Exception expected");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).contains("Booleans and null cannot be used in 'greater than or equal' condition");
    }

    try {
      processInstanceQuery.variableValueLessThan("booleanVar", true);
      fail("Exception expected");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).contains("Booleans and null cannot be used in 'less than' condition");
    }

    try {
      processInstanceQuery.variableValueLessThanOrEqual("booleanVar", true);
      fail("Exception expected");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).contains("Booleans and null cannot be used in 'less than or equal' condition");
    }

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryVariablesUpdatedToNullValue() {
    // Start process instance with different types of variables
    Map<String, Object> variables = new HashMap<>();
    variables.put("longVar", 928374L);
    variables.put("shortVar", (short) 123);
    variables.put("integerVar", 1234);
    variables.put("stringVar", "coca-cola");
    variables.put("dateVar", new Date());
    variables.put("booleanVar", true);
    variables.put("nullVar", null);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery()
      .variableValueEquals("longVar", null)
      .variableValueEquals("shortVar", null)
      .variableValueEquals("integerVar", null)
      .variableValueEquals("stringVar", null)
      .variableValueEquals("booleanVar", null)
      .variableValueEquals("dateVar", null);

    ProcessInstanceQuery notQuery = runtimeService.createProcessInstanceQuery()
      .variableValueNotEquals("longVar", null)
      .variableValueNotEquals("shortVar", null)
      .variableValueNotEquals("integerVar", null)
      .variableValueNotEquals("stringVar", null)
      .variableValueNotEquals("booleanVar", null)
      .variableValueNotEquals("dateVar", null);

    assertThat(query.singleResult()).isNull();
    assertThat(notQuery.singleResult()).isNotNull();

    // Set all existing variables values to null
    runtimeService.setVariable(processInstance.getId(), "longVar", null);
    runtimeService.setVariable(processInstance.getId(), "shortVar", null);
    runtimeService.setVariable(processInstance.getId(), "integerVar", null);
    runtimeService.setVariable(processInstance.getId(), "stringVar", null);
    runtimeService.setVariable(processInstance.getId(), "dateVar", null);
    runtimeService.setVariable(processInstance.getId(), "nullVar", null);
    runtimeService.setVariable(processInstance.getId(), "booleanVar", null);

    Execution queryResult = query.singleResult();
    assertThat(queryResult).isNotNull();
    assertThat(queryResult.getId()).isEqualTo(processInstance.getId());
    assertThat(notQuery.singleResult()).isNull();
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryNullVariable() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("nullVar", null);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("nullVar", "notnull");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("nullVarLong", "notnull");
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("nullVarDouble", "notnull");
    ProcessInstance processInstance4 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("nullVarByte", "testbytes".getBytes());
    ProcessInstance processInstance5 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    // Query on null value, should return one value
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().variableValueEquals("nullVar", null);
    List<ProcessInstance> processInstances = query.list();
    assertThat(processInstances)
            .isNotNull()
            .hasSize(1);
    assertThat(processInstances.get(0).getId()).isEqualTo(processInstance1.getId());

    // Test NOT_EQUALS null
    assertThat(runtimeService.createProcessInstanceQuery().variableValueNotEquals("nullVar", null).count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().variableValueNotEquals("nullVarLong", null).count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().variableValueNotEquals("nullVarDouble", null).count()).isEqualTo(1);
    // When a byte-array refrence is present, the variable is not considered null
    assertThat(runtimeService.createProcessInstanceQuery().variableValueNotEquals("nullVarByte", null).count()).isEqualTo(1);
    var processInstanceQuery = runtimeService.createProcessInstanceQuery();

    // All other variable queries with null should throw exception
    try {
      processInstanceQuery.variableValueGreaterThan("nullVar", null);
      fail("Exception expected");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).contains("Booleans and null cannot be used in 'greater than' condition");
    }

    try {
      processInstanceQuery.variableValueGreaterThanOrEqual("nullVar", null);
      fail("Exception expected");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).contains("Booleans and null cannot be used in 'greater than or equal' condition");
    }

    try {
      processInstanceQuery.variableValueLessThan("nullVar", null);
      fail("Exception expected");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).contains("Booleans and null cannot be used in 'less than' condition");
    }

    try {
      processInstanceQuery.variableValueLessThanOrEqual("nullVar", null);
      fail("Exception expected");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).contains("Booleans and null cannot be used in 'less than or equal' condition");
    }

    try {
      processInstanceQuery.variableValueLike("nullVar", null);
      fail("Exception expected");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).contains("Booleans and null cannot be used in 'like' condition");
    }

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance4.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance5.getId(), "test");
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryInvalidTypes() {
    Map<String, Object> vars = new HashMap<>();
    byte[] testBytes = "test".getBytes();
    vars.put("bytesVar", testBytes);
    DummySerializable dummySerializable = new DummySerializable();
    vars.put("serializableVar", dummySerializable);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    var processInstanceQuery = runtimeService.createProcessInstanceQuery().variableValueEquals("bytesVar", testBytes);

    try {
      processInstanceQuery.list();
      fail("Expected exception");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).contains("Variables of type ByteArray cannot be used to query");
    }

    var processInstanceQuery2 = runtimeService.createProcessInstanceQuery().variableValueEquals("serializableVar", dummySerializable);
    try {
      processInstanceQuery2.list();
      fail("Expected exception");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).contains("Object values cannot be used to query");
    }

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");
  }

  @Test
  public void testQueryVariablesNullNameArgument() {
    var processInstanceQuery = runtimeService.createProcessInstanceQuery();
    try {
      processInstanceQuery.variableValueEquals(null, "value");
      fail("Expected exception");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).contains("name is null");
    }
    try {
      processInstanceQuery.variableValueNotEquals(null, "value");
      fail("Expected exception");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).contains("name is null");
    }
    try {
      processInstanceQuery.variableValueGreaterThan(null, "value");
      fail("Expected exception");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).contains("name is null");
    }
    try {
      processInstanceQuery.variableValueGreaterThanOrEqual(null, "value");
      fail("Expected exception");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).contains("name is null");
    }
    try {
      processInstanceQuery.variableValueLessThan(null, "value");
      fail("Expected exception");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).contains("name is null");
    }
    try {
      processInstanceQuery.variableValueLessThanOrEqual(null, "value");
      fail("Expected exception");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).contains("name is null");
    }
    try {
      processInstanceQuery.variableValueLike(null, "value");
      fail("Expected exception");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).contains("name is null");
    }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryAllVariableTypes() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("nullVar", null);
    vars.put("stringVar", "string");
    vars.put("longVar", 10L);
    vars.put("doubleVar", 1.2);
    vars.put("integerVar", 1234);
    vars.put("booleanVar", true);
    vars.put("shortVar", (short) 123);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery()
      .variableValueEquals("nullVar", null)
      .variableValueEquals("stringVar", "string")
      .variableValueEquals("longVar", 10L)
      .variableValueEquals("doubleVar", 1.2)
      .variableValueEquals("integerVar", 1234)
      .variableValueEquals("booleanVar", true)
      .variableValueEquals("shortVar", (short) 123);

    List<ProcessInstance> processInstances = query.list();
    assertThat(processInstances)
            .isNotNull()
            .hasSize(1);
    assertThat(processInstances.get(0).getId()).isEqualTo(processInstance.getId());

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testClashingValues() {
      Map<String, Object> vars = new HashMap<>();
      vars.put("var", 1234L);

      ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

      Map<String, Object> vars2 = new HashMap<>();
      vars2.put("var", 1234);

      ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars2);

      List<ProcessInstance> foundInstances = runtimeService.createProcessInstanceQuery()
      .processDefinitionKey("oneTaskProcess")
      .variableValueEquals("var", 1234L)
      .list();

    assertThat(foundInstances).hasSize(1);
    assertThat(foundInstances.get(0).getId()).isEqualTo(processInstance.getId());

      runtimeService.deleteProcessInstance(processInstance.getId(), "test");
      runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
  }

  @Test
  public void testQueryByProcessInstanceIds() {
    Set<String> ids = new HashSet<>(this.processInstanceIds);

    // start an instance that will not be part of the query
    runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY_2, "2");

    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processInstanceIds(ids);
    assertThat(processInstanceQuery.count()).isEqualTo(5);

    List<ProcessInstance> processInstances = processInstanceQuery.list();
    assertThat(processInstances)
            .isNotNull()
            .hasSize(5);

    for (ProcessInstance processInstance : processInstances) {
      assertThat(ids).contains(processInstance.getId());
    }
  }

  @Test
  public void testQueryByProcessInstanceIdsEmpty() {
    var processInstanceQuery = runtimeService.createProcessInstanceQuery();
    Set<String> emptyProcessInstanceIds = emptySet();
    try {
      processInstanceQuery.processInstanceIds(emptyProcessInstanceIds);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException re) {
      assertThat(re.getMessage()).contains("Set of process instance ids is empty");
    }
  }

  @Test
  public void testQueryByProcessInstanceIdsNull() {
    var processInstanceQuery = runtimeService.createProcessInstanceQuery();
    try {
      processInstanceQuery.processInstanceIds(null);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException re) {
      assertThat(re.getMessage()).contains("Set of process instance ids is null");
    }
  }

  @Test
  public void testQueryByActive() {
    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery();

    assertThat(processInstanceQuery.active().count()).isEqualTo(5);

    repositoryService.suspendProcessDefinitionByKey(PROCESS_DEFINITION_KEY);

    assertThat(processInstanceQuery.active().count()).isEqualTo(5);

    repositoryService.suspendProcessDefinitionByKey(PROCESS_DEFINITION_KEY, true, null);

    assertThat(processInstanceQuery.active().count()).isEqualTo(1);
  }

  @Test
  public void testQueryBySuspended() {
    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery();

    assertThat(processInstanceQuery.suspended().count()).isZero();

    repositoryService.suspendProcessDefinitionByKey(PROCESS_DEFINITION_KEY);

    assertThat(processInstanceQuery.suspended().count()).isZero();

    repositoryService.suspendProcessDefinitionByKey(PROCESS_DEFINITION_KEY, true, null);

    assertThat(processInstanceQuery.suspended().count()).isEqualTo(4);
  }

  @Test
  public void testNativeQuery() {
    String tablePrefix = engineRule.getProcessEngineConfiguration().getDatabaseTablePrefix();
    // just test that the query will be constructed and executed, details are tested in the TaskQueryTest
    assertThat(managementService.getTableName(ProcessInstance.class)).isEqualTo(tablePrefix + "ACT_RU_EXECUTION");

    long piCount = runtimeService.createProcessInstanceQuery().count();

    assertThat(runtimeService.createNativeProcessInstanceQuery().sql("SELECT * FROM " + managementService.getTableName(ProcessInstance.class)).list()).hasSize((int)piCount);
    assertThat(runtimeService.createNativeProcessInstanceQuery().sql("SELECT count(*) FROM " + managementService.getTableName(ProcessInstance.class)).count()).isEqualTo(piCount);
  }

  @Test
  public void testNativeQueryPaging() {
    assertThat(runtimeService.createNativeProcessInstanceQuery().sql("SELECT * FROM " + managementService.getTableName(ProcessInstance.class)).listPage(0, 5)).hasSize(5);
  }

  @Test
  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/failingProcessCreateOneIncident.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  public void testQueryWithIncident() {
    ProcessInstance instanceWithIncident = runtimeService.startProcessInstanceByKey("failingProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    testHelper.executeAvailableJobs();

    List<Incident> incidentList = runtimeService.createIncidentQuery().list();
    assertThat(incidentList).hasSize(1);

    ProcessInstance instance = runtimeService.createProcessInstanceQuery().withIncident().singleResult();
    assertThat(instance.getId()).isEqualTo(instanceWithIncident.getId());
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/failingProcessCreateOneIncident.bpmn20.xml"})
  public void testQueryByIncidentId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingProcess");

    testHelper.executeAvailableJobs();

    List<Incident> incidentList = runtimeService.createIncidentQuery().list();
    assertThat(incidentList).hasSize(1);

    Incident incident = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).singleResult();

    List<ProcessInstance> processInstanceList = runtimeService
        .createProcessInstanceQuery()
        .incidentId(incident.getId()).list();

    assertThat(processInstanceList).hasSize(1);
  }

  @Test
  public void testQueryByInvalidIncidentId() {
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();

    assertThat(query.incidentId("invalid").count()).isZero();

    try {
      query.incidentId(null);
      fail("Exception expected");
    } catch (ProcessEngineException ignored) {
      // expected
    }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/failingProcessCreateOneIncident.bpmn20.xml"})
  public void testQueryByIncidentType() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingProcess");

    testHelper.executeAvailableJobs();

    List<Incident> incidentList = runtimeService.createIncidentQuery().list();
    assertThat(incidentList).hasSize(1);

    Incident incident = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).singleResult();

    List<ProcessInstance> processInstanceList = runtimeService
        .createProcessInstanceQuery()
        .incidentType(incident.getIncidentType()).list();

    assertThat(processInstanceList).hasSize(1);
  }

  @Test
  public void testQueryByInvalidIncidentType() {
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();

    assertThat(query.incidentType("invalid").count()).isZero();

    try {
      query.incidentType(null);
      fail("Exception expected");
    } catch (ProcessEngineException ignored) {
      // expected
    }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/failingProcessCreateOneIncident.bpmn20.xml"})
  public void testQueryByIncidentMessage() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingProcess");

    testHelper.executeAvailableJobs();

    List<Incident> incidentList = runtimeService.createIncidentQuery().list();
    assertThat(incidentList).hasSize(1);

    Incident incident = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).singleResult();

    List<ProcessInstance> processInstanceList = runtimeService
        .createProcessInstanceQuery()
        .incidentMessage(incident.getIncidentMessage()).list();

    assertThat(processInstanceList).hasSize(1);
  }

  @Test
  public void testQueryByInvalidIncidentMessage() {
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();

    assertThat(query.incidentMessage("invalid").count()).isZero();

    try {
      query.incidentMessage(null);
      fail("Exception expected");
    } catch (ProcessEngineException ignored) {
      // expected
    }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/failingProcessCreateOneIncident.bpmn20.xml"})
  public void testQueryByIncidentMessageLike() {
    runtimeService.startProcessInstanceByKey("failingProcess");

    testHelper.executeAvailableJobs();

    List<Incident> incidentList = runtimeService.createIncidentQuery().list();
    assertThat(incidentList).hasSize(1);

    List<ProcessInstance> processInstanceList = runtimeService
        .createProcessInstanceQuery()
        .incidentMessageLike("%\\_exception%").list();

    assertThat(processInstanceList).hasSize(1);
  }

  @Test
  public void testQueryByInvalidIncidentMessageLike() {
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();

    assertThat(query.incidentMessageLike("invalid").count()).isZero();

    try {
      query.incidentMessageLike(null);
      fail("Exception expected");
    } catch (ProcessEngineException ignored) {
      // expected
    }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/failingSubProcessCreateOneIncident.bpmn20.xml"})
  public void testQueryByIncidentIdInSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingSubProcess");

    testHelper.executeAvailableJobs();

    List<Incident> incidentList = runtimeService.createIncidentQuery().list();
    assertThat(incidentList).hasSize(1);

    Incident incident = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).singleResult();

    List<ProcessInstance> processInstanceList = runtimeService
        .createProcessInstanceQuery()
        .incidentId(incident.getId()).list();

    assertThat(processInstanceList).hasSize(1);
    assertThat(processInstanceList.get(0).getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/failingSubProcessCreateOneIncident.bpmn20.xml"})
  public void testQueryByIncidentTypeInSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingSubProcess");

    testHelper.executeAvailableJobs();

    List<Incident> incidentList = runtimeService.createIncidentQuery().list();
    assertThat(incidentList).hasSize(1);

    Incident incident = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).singleResult();

    List<ProcessInstance> processInstanceList = runtimeService
        .createProcessInstanceQuery()
        .incidentType(incident.getIncidentType()).list();

    assertThat(processInstanceList).hasSize(1);
    assertThat(processInstanceList.get(0).getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/failingSubProcessCreateOneIncident.bpmn20.xml"})
  public void testQueryByIncidentMessageInSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingSubProcess");

    testHelper.executeAvailableJobs();

    List<Incident> incidentList = runtimeService.createIncidentQuery().list();
    assertThat(incidentList).hasSize(1);

    Incident incident = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).singleResult();

    List<ProcessInstance> processInstanceList = runtimeService
        .createProcessInstanceQuery()
        .incidentMessage(incident.getIncidentMessage()).list();

    assertThat(processInstanceList).hasSize(1);
    assertThat(processInstanceList.get(0).getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/failingSubProcessCreateOneIncident.bpmn20.xml"})
  public void testQueryByIncidentMessageLikeInSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingSubProcess");

    testHelper.executeAvailableJobs();

    List<Incident> incidentList = runtimeService.createIncidentQuery().list();
    assertThat(incidentList).hasSize(1);

    List<ProcessInstance> processInstanceList = runtimeService
        .createProcessInstanceQuery()
        .incidentMessageLike("%exception%").list();

    assertThat(processInstanceList).hasSize(1);
    assertThat(processInstanceList.get(0).getId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
    })
  public void testQueryByCaseInstanceId() {
    String caseInstanceId = caseService
      .withCaseDefinitionByKey("oneProcessTaskCase")
      .create()
      .getId();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();

    query.caseInstanceId(caseInstanceId);

    assertThat(query.count()).isEqualTo(1);

    List<ProcessInstance> result = query.list();
    assertThat(result).hasSize(1);

    ProcessInstance processInstance = result.get(0);
    assertThat(processInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);
  }

  @Test
  public void testQueryByInvalidCaseInstanceId() {
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();

    query.caseInstanceId("invalid");

    assertThat(query.count()).isZero();

    try {
      query.caseInstanceId(null);
      fail("The passed case instance should not be null.");
    } catch (Exception ignored) {
      // expected
    }

  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/runtime/superCase.cmmn",
      "org/operaton/bpm/engine/test/api/runtime/superProcessWithCallActivityInsideSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"
    })
  public void testQueryByCaseInstanceIdHierarchy() {
    String caseInstanceId = caseService
      .withCaseDefinitionByKey("oneProcessTaskCase")
      .businessKey("aBusinessKey")
      .create()
      .getId();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();

    query.caseInstanceId(caseInstanceId);

    assertThat(query.count()).isEqualTo(2);

    List<ProcessInstance> result = query.list();
    assertThat(result).hasSize(2);

    ProcessInstance firstProcessInstance = result.get(0);
    assertThat(firstProcessInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);

    ProcessInstance secondProcessInstance = result.get(1);
    assertThat(secondProcessInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  public void testProcessVariableValueEqualsNumber() {
    // long
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Map.of("var", 123L));

    // non-matching long
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Map.of("var", 12345L));

    // short
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Map.of("var", (short) 123));

    // double
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Map.of("var", 123.0d));

    // integer
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Map.of("var", 123));

    // untyped null (should not match)
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", null));

    // typed null (should not match)
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Map.of("var", Variables.longValue(null)));

    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Map.of("var", "123"));

    assertThat(runtimeService.createProcessInstanceQuery().variableValueEquals("var", Variables.numberValue(123)).count()).isEqualTo(4);
    assertThat(runtimeService.createProcessInstanceQuery().variableValueEquals("var", Variables.numberValue(123L)).count()).isEqualTo(4);
    assertThat(runtimeService.createProcessInstanceQuery().variableValueEquals("var", Variables.numberValue(123.0d)).count()).isEqualTo(4);
    assertThat(runtimeService.createProcessInstanceQuery().variableValueEquals("var", Variables.numberValue((short) 123)).count()).isEqualTo(4);

    assertThat(runtimeService.createProcessInstanceQuery().variableValueEquals("var", Variables.numberValue(null)).count()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  public void testProcessVariableValueNumberComparison() {
    // long
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Map.of("var", 123L));

    // non-matching long
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Map.of("var", 12345L));

    // short
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Map.of("var", (short) 123));

    // double
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Map.of("var", 123.0d));

    // integer
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Map.of("var", 123));

    // untyped null
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", null));

    // typed null
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Map.of("var", Variables.longValue(null)));

    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Map.of("var", "123"));

    assertThat(runtimeService.createProcessInstanceQuery().variableValueNotEquals("var", Variables.numberValue(123)).count()).isEqualTo(4);
    assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThan("var", Variables.numberValue(123)).count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("var", Variables.numberValue(123)).count()).isEqualTo(5);
    assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThan("var", Variables.numberValue(123)).count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("var", Variables.numberValue(123)).count()).isEqualTo(4);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn"})
  public void testQueryBySuperCaseInstanceId() {
    String superCaseInstanceId = caseService.createCaseInstanceByKey("oneProcessTaskCase").getId();

    ProcessInstanceQuery query = runtimeService
        .createProcessInstanceQuery()
        .superCaseInstanceId(superCaseInstanceId);

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);

    ProcessInstance subProcessInstance = query.singleResult();
    assertThat(subProcessInstance).isNotNull();
  }

  @Test
  public void testQueryByInvalidSuperCaseInstanceId() {
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();

    assertThat(query.superProcessInstanceId("invalid").singleResult()).isNull();
    assertThat(query.superProcessInstanceId("invalid").list()).isEmpty();

    try {
      query.superCaseInstanceId(null);
      fail("Exception expected");
    } catch (NullValueException e) {
      // expected
    }
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/runtime/superProcessWithCaseCallActivity.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn" })
  public void testQueryBySubCaseInstanceId() {
    String superProcessInstanceId = runtimeService.startProcessInstanceByKey("subProcessQueryTest").getId();

    String subCaseInstanceId = caseService
        .createCaseInstanceQuery()
        .superProcessInstanceId(superProcessInstanceId)
        .singleResult()
        .getId();

    ProcessInstanceQuery query = runtimeService
        .createProcessInstanceQuery()
        .subCaseInstanceId(subCaseInstanceId);

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);

    ProcessInstance superProcessInstance = query.singleResult();
    assertThat(superProcessInstance).isNotNull();
    assertThat(superProcessInstance.getId()).isEqualTo(superProcessInstanceId);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/runtime/superProcessWithCaseCallActivityInsideSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn" })
  public void testQueryBySubCaseInstanceIdNested() {
    String superProcessInstanceId = runtimeService.startProcessInstanceByKey("subProcessQueryTest").getId();

    String subCaseInstanceId = caseService
        .createCaseInstanceQuery()
        .superProcessInstanceId(superProcessInstanceId)
        .singleResult()
        .getId();

    ProcessInstanceQuery query = runtimeService
        .createProcessInstanceQuery()
        .subCaseInstanceId(subCaseInstanceId);

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);

    ProcessInstance superProcessInstance = query.singleResult();
    assertThat(superProcessInstance).isNotNull();
    assertThat(superProcessInstance.getId()).isEqualTo(superProcessInstanceId);
  }

  @Test
  public void testQueryByInvalidSubCaseInstanceId() {
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();

    assertThat(query.subProcessInstanceId("invalid").singleResult()).isNull();
    assertThat(query.subProcessInstanceId("invalid").list()).isEmpty();

    try {
      query.subCaseInstanceId(null);
      fail("Exception expected");
    } catch (NullValueException e) {
      // expected
    }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryNullValue() {
    // typed null
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables().putValueTyped("var", Variables.stringValue(null)));

    // untyped null
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables().putValueTyped("var", null));

    // non-null String value
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables().putValue("var", "a String Value"));

    ProcessInstance processInstance4 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables().putValue("var", "another String Value"));

    // (1) query for untyped null: should return typed and untyped null (notEquals: the opposite)
    List<ProcessInstance> instances =
        runtimeService.createProcessInstanceQuery().variableValueEquals("var", null).list();
    verifyResultContainsExactly(instances, asSet(processInstance1.getId(), processInstance2.getId()));
    instances = runtimeService.createProcessInstanceQuery().variableValueNotEquals("var", null).list();
    verifyResultContainsExactly(instances, asSet(processInstance3.getId(), processInstance4.getId()));

    // (2) query for typed null: should return typed null only (notEquals: the opposite)
    instances = runtimeService.createProcessInstanceQuery()
        .variableValueEquals("var", Variables.stringValue(null)).list();
    verifyResultContainsExactly(instances, asSet(processInstance1.getId()));
    instances = runtimeService.createProcessInstanceQuery()
        .variableValueNotEquals("var", Variables.stringValue(null)).list();
    verifyResultContainsExactly(instances, asSet(processInstance2.getId(), processInstance3.getId(), processInstance4.getId()));

    // (3) query for typed value: should return typed value only (notEquals: the opposite)
    instances = runtimeService.createProcessInstanceQuery()
        .variableValueEquals("var", "a String Value").list();
    verifyResultContainsExactly(instances, asSet(processInstance3.getId()));
    instances = runtimeService.createProcessInstanceQuery()
        .variableValueNotEquals("var", "a String Value").list();
    verifyResultContainsExactly(instances, asSet(processInstance1.getId(), processInstance2.getId(), processInstance4.getId()));
  }

  @Test
  public void testQueryByDeploymentId() {
    // given
    String firstDeploymentId = repositoryService
        .createDeploymentQuery()
        .singleResult()
        .getId();

    // make a second deployment and start an instance
    org.operaton.bpm.engine.repository.Deployment secondDeployment = repositoryService.createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
      .deploy();

    ProcessInstance secondProcessInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    ProcessInstanceQuery query = runtimeService
        .createProcessInstanceQuery()
        .deploymentId(firstDeploymentId);

    // then the instance belonging to the second deployment is not returned
    assertThat(query.count()).isEqualTo(5);

    List<ProcessInstance> instances = query.list();
    assertThat(instances).hasSize(5);

    for (ProcessInstance returnedInstance : instances) {
      assertThat(secondProcessInstance.getId()).isNotEqualTo(returnedInstance.getId());
    }

    // cleanup
    repositoryService.deleteDeployment(secondDeployment.getId(), true);

  }

  @Test
  public void testQueryByInvalidDeploymentId() {
    assertThat(runtimeService.createProcessInstanceQuery().deploymentId("invalid").count()).isZero();
    var processInstanceQuery = runtimeService.createProcessInstanceQuery();

    try {
      processInstanceQuery.deploymentId(null);
      fail("Exception expected");
    } catch(ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("Deployment id is null");
    }
  }

  @Test
  public void testQueryByNullActivityId() {
    var processInstanceQuery = runtimeService.createProcessInstanceQuery();
    try {
      processInstanceQuery.activityIdIn((String) null);
      fail("exception expected");
    }
    catch (NullValueException e) {
        assertThat(e.getMessage()).contains("activity ids contains null value");
    }
  }

  @Test
  public void testQueryByNullActivityIds() {
    var processInstanceQuery = runtimeService.createProcessInstanceQuery();
    try {
      processInstanceQuery.activityIdIn((String[]) null);
      fail("exception expected");
    }
    catch (NullValueException e) {
      assertThat(e.getMessage()).contains("activity ids is null");
    }
  }

  @Test
  public void testQueryByUnknownActivityId() {
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery()
      .activityIdIn("unknown");

    assertNoProcessInstancesReturned(query);
  }

  @Test
  public void testQueryByLeafActivityId() {
    // given
    ProcessDefinition oneTaskDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition gatewaySubProcessDefinition = testHelper.deployAndGetDefinition(FORK_JOIN_SUB_PROCESS_MODEL);

    // when
    ProcessInstance oneTaskInstance1 = runtimeService.startProcessInstanceById(oneTaskDefinition.getId());
    ProcessInstance oneTaskInstance2 = runtimeService.startProcessInstanceById(oneTaskDefinition.getId());
    ProcessInstance gatewaySubProcessInstance1 = runtimeService.startProcessInstanceById(gatewaySubProcessDefinition.getId());
    ProcessInstance gatewaySubProcessInstance2 = runtimeService.startProcessInstanceById(gatewaySubProcessDefinition.getId());

    Task task = engineRule.getTaskService().createTaskQuery()
      .processInstanceId(gatewaySubProcessInstance2.getId())
      .taskName("completeMe")
      .singleResult();
    engineRule.getTaskService().complete(task.getId());

    // then
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().activityIdIn("userTask");
    assertReturnedProcessInstances(query, oneTaskInstance1, oneTaskInstance2);

    query = runtimeService.createProcessInstanceQuery().activityIdIn("userTask1", "userTask2");
    assertReturnedProcessInstances(query, gatewaySubProcessInstance1, gatewaySubProcessInstance2);

    query = runtimeService.createProcessInstanceQuery().activityIdIn("userTask", "userTask1");
    assertReturnedProcessInstances(query, oneTaskInstance1, oneTaskInstance2, gatewaySubProcessInstance1);

    query = runtimeService.createProcessInstanceQuery().activityIdIn("userTask", "userTask1", "userTask2");
    assertReturnedProcessInstances(query, oneTaskInstance1, oneTaskInstance2, gatewaySubProcessInstance1, gatewaySubProcessInstance2);

    query = runtimeService.createProcessInstanceQuery().activityIdIn("join");
    assertReturnedProcessInstances(query, gatewaySubProcessInstance2);
  }

  @Test
  public void testQueryByNonLeafActivityId() {
    // given
    ProcessDefinition processDefinition = testHelper.deployAndGetDefinition(FORK_JOIN_SUB_PROCESS_MODEL);

    // when
    runtimeService.startProcessInstanceById(processDefinition.getId());

    // then
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().activityIdIn("subProcess", "fork");
    assertNoProcessInstancesReturned(query);
  }

  @Test
  public void testQueryByAsyncBeforeActivityId() {
    // given
    ProcessDefinition testProcess = testHelper.deployAndGetDefinition(ProcessModels.newModel()
      .startEvent("start").operatonAsyncBefore()
      .subProcess("subProcess").operatonAsyncBefore()
      .embeddedSubProcess()
        .startEvent()
        .serviceTask("task").operatonAsyncBefore().operatonExpression("${true}")
        .endEvent()
      .subProcessDone()
      .endEvent("end").operatonAsyncBefore()
      .done()
    );

    // when
    ProcessInstance instanceBeforeStart = runtimeService.startProcessInstanceById(testProcess.getId());
    ProcessInstance instanceBeforeSubProcess = runtimeService.startProcessInstanceById(testProcess.getId());
    executeJobForProcessInstance(instanceBeforeSubProcess);
    ProcessInstance instanceBeforeTask = runtimeService.startProcessInstanceById(testProcess.getId());
    executeJobForProcessInstance(instanceBeforeTask);
    executeJobForProcessInstance(instanceBeforeTask);
    ProcessInstance instanceBeforeEnd = runtimeService.startProcessInstanceById(testProcess.getId());
    executeJobForProcessInstance(instanceBeforeEnd);
    executeJobForProcessInstance(instanceBeforeEnd);
    executeJobForProcessInstance(instanceBeforeEnd);

    // then
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().activityIdIn("start");
    assertReturnedProcessInstances(query, instanceBeforeStart);

    query = runtimeService.createProcessInstanceQuery().activityIdIn("subProcess");
    assertReturnedProcessInstances(query, instanceBeforeSubProcess);

    query = runtimeService.createProcessInstanceQuery().activityIdIn("task");
    assertReturnedProcessInstances(query, instanceBeforeTask);

    query = runtimeService.createProcessInstanceQuery().activityIdIn("end");
    assertReturnedProcessInstances(query, instanceBeforeEnd);
  }

  @Test
  public void testQueryByAsyncAfterActivityId() {
    // given
    ProcessDefinition testProcess = testHelper.deployAndGetDefinition(ProcessModels.newModel()
      .startEvent("start").operatonAsyncAfter()
      .subProcess("subProcess").operatonAsyncAfter()
      .embeddedSubProcess()
        .startEvent()
        .serviceTask("task").operatonAsyncAfter().operatonExpression("${true}")
        .endEvent()
      .subProcessDone()
      .endEvent("end").operatonAsyncAfter()
      .done()
    );

    // when
    ProcessInstance instanceAfterStart = runtimeService.startProcessInstanceById(testProcess.getId());
    ProcessInstance instanceAfterTask = runtimeService.startProcessInstanceById(testProcess.getId());
    executeJobForProcessInstance(instanceAfterTask);
    ProcessInstance instanceAfterSubProcess = runtimeService.startProcessInstanceById(testProcess.getId());
    executeJobForProcessInstance(instanceAfterSubProcess);
    executeJobForProcessInstance(instanceAfterSubProcess);
    ProcessInstance instanceAfterEnd = runtimeService.startProcessInstanceById(testProcess.getId());
    executeJobForProcessInstance(instanceAfterEnd);
    executeJobForProcessInstance(instanceAfterEnd);
    executeJobForProcessInstance(instanceAfterEnd);

    // then
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().activityIdIn("start");
    assertReturnedProcessInstances(query, instanceAfterStart);

    query = runtimeService.createProcessInstanceQuery().activityIdIn("task");
    assertReturnedProcessInstances(query, instanceAfterTask);

    query = runtimeService.createProcessInstanceQuery().activityIdIn("subProcess");
    assertReturnedProcessInstances(query, instanceAfterSubProcess);

    query = runtimeService.createProcessInstanceQuery().activityIdIn("end");
    assertReturnedProcessInstances(query, instanceAfterEnd);
  }

  @Test
  public void testQueryByActivityIdBeforeCompensation() {
    // given
    ProcessDefinition testProcess = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);

    // when
    runtimeService.startProcessInstanceById(testProcess.getId());
    testHelper.completeTask("userTask1");

    // then
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().activityIdIn("subProcess");
    assertNoProcessInstancesReturned(query);
  }

  @Test
  public void testQueryByActivityIdDuringCompensation() {
    // given
    ProcessDefinition testProcess = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);

    // when
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(testProcess.getId());
    testHelper.completeTask("userTask1");
    testHelper.completeTask("userTask2");

    // then
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().activityIdIn("subProcess");
    assertReturnedProcessInstances(query, processInstance);

    query = runtimeService.createProcessInstanceQuery().activityIdIn("compensationEvent");
    assertReturnedProcessInstances(query, processInstance);

    query = runtimeService.createProcessInstanceQuery().activityIdIn("compensationHandler");
    assertReturnedProcessInstances(query, processInstance);
  }

  @Test
  public void testQueryByRootProcessInstances() {
    // given
    String superProcess = "calling";
    String subProcess = "called";
    BpmnModelInstance callingInstance = ProcessModels.newModel(superProcess)
      .startEvent()
      .callActivity()
      .calledElement(subProcess)
      .endEvent()
      .done();

    BpmnModelInstance calledInstance = ProcessModels.newModel(subProcess)
      .startEvent()
      .userTask()
      .endEvent()
      .done();

    testHelper.deploy(callingInstance, calledInstance);
    String businessKey = "theOne";
    String processInstanceId = runtimeService.startProcessInstanceByKey(superProcess, businessKey).getProcessInstanceId();

    // when
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery()
      .processInstanceBusinessKey(businessKey)
      .rootProcessInstances();

    // then
    assertThat(query.count()).isEqualTo(1);
    List<ProcessInstance> list = query.list();
    assertThat(list).hasSize(1);
    assertThat(list.get(0).getId()).isEqualTo(processInstanceId);
  }

  @Test
  public void testQueryByRootProcessInstancesAndSuperProcess() {
    // when
    ProcessInstanceQuery processInstanceQuery1 = runtimeService.createProcessInstanceQuery()
      .rootProcessInstances();
    try {
      processInstanceQuery1
        .superProcessInstanceId("processInstanceId");

      fail("expected exception");
    } catch (ProcessEngineException e) {
      // then
      assertThat(e.getMessage()).contains("Invalid query usage: cannot set both rootProcessInstances and superProcessInstanceId");
    }

    // when
    ProcessInstanceQuery processInstanceId2 = runtimeService.createProcessInstanceQuery()
      .superProcessInstanceId("processInstanceId");
    try {
      processInstanceId2
        .rootProcessInstances();

      fail("expected exception");
    } catch (ProcessEngineException e) {
      // then
      assertThat(e.getMessage()).contains("Invalid query usage: cannot set both rootProcessInstances and superProcessInstanceId");
    }
  }

  protected void executeJobForProcessInstance(ProcessInstance processInstance) {
    Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
    managementService.executeJob(job.getId());
  }

  @SuppressWarnings("unchecked")
  protected <T> Set<T> asSet(T... elements) {
    return new HashSet<>(Arrays.asList(elements));
  }

  protected void assertNoProcessInstancesReturned(ProcessInstanceQuery query) {
    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
  }

  protected void assertReturnedProcessInstances(ProcessInstanceQuery query, ProcessInstance... processInstances) {
    int expectedSize = processInstances.length;
    assertThat(query.count()).isEqualTo(expectedSize);
    assertThat(query.list()).hasSize(expectedSize);

    verifyResultContainsExactly(query.list(), collectProcessInstanceIds(Arrays.asList(processInstances)));
  }

  protected void verifyResultContainsExactly(List<ProcessInstance> instances, Set<String> processInstanceIds) {
    Set<String> retrievedInstanceIds = collectProcessInstanceIds(instances);
    assertThat(retrievedInstanceIds).isEqualTo(processInstanceIds);
  }

  protected Set<String> collectProcessInstanceIds(List<ProcessInstance> instances) {
    Set<String> retrievedInstanceIds = new HashSet<>();
    for (ProcessInstance instance : instances) {
      retrievedInstanceIds.add(instance.getId());
    }
    return retrievedInstanceIds;
  }

}
