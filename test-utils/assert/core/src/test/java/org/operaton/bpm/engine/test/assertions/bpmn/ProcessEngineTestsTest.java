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
package org.operaton.bpm.engine.test.assertions.bpmn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.operaton.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.operaton.bpm.engine.test.assertions.bpmn.AbstractAssertions.processEngine;
import static org.operaton.bpm.engine.test.assertions.bpmn.AbstractAssertions.reset;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.authorizationService;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.executionQuery;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.formService;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.historyService;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.identityService;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.jobQuery;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.managementService;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.processDefinitionQuery;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.processInstanceQuery;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.repositoryService;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.runtimeService;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.taskQuery;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.taskService;
import static org.operaton.bpm.engine.test.assertions.cmmn.CmmnAwareTests.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngines;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinitionQuery;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.ExecutionQuery;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.assertions.cmmn.CaseDefinitionAssert;
import org.operaton.bpm.engine.test.assertions.cmmn.CaseExecutionAssert;
import org.operaton.bpm.engine.test.assertions.cmmn.CaseInstanceAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class ProcessEngineTestsTest {

  ProcessEngine processEngine;
  MockedStatic<ProcessEngines> processEnginesMockedStatic;

  @BeforeEach
  void setUp() {
    processEngine = mock(ProcessEngine.class);
    processEnginesMockedStatic = mockStatic(ProcessEngines.class, CALLS_REAL_METHODS);
    init(processEngine);
  }

  @AfterEach
  void tearDown() {
    reset();
    processEnginesMockedStatic.close();
  }

  @Test
  void testProcessEngine() {
    // When
    ProcessEngine returnedEngine = processEngine();
    // Then
    assertThat(returnedEngine).isNotNull().isSameAs(processEngine);
  }

  @Test
  void noProcessEngineFailure() {
    // Given
    processEnginesMockedStatic.when(ProcessEngines::getProcessEngines).thenReturn(new HashMap<String,ProcessEngine>());
    reset();
    try {
      // When
      processEngine();
      fail("Process engine should not be initialized");
    } catch (IllegalStateException e) {
      // Then
      assertThat(e).hasMessage("No ProcessEngine found to be registered with ProcessEngines!");
    }
  }

  @Test
  void multipleProcessEngineFailure() {
    // Given
    Map<String,ProcessEngine> multipleEnginesMap = new HashMap<>();
    multipleEnginesMap.put("test1", mock(ProcessEngine.class));
    multipleEnginesMap.put("test2", mock(ProcessEngine.class));
    processEnginesMockedStatic.when(ProcessEngines::getProcessEngines).thenReturn(multipleEnginesMap);
    reset();
    try {
      // When
      processEngine();
      fail("Process engine should not be initialized");
    } catch (IllegalStateException e) {
      // Then
      assertThat(e).hasMessage("2 ProcessEngines initialized. Call BpmnAwareTests.init(ProcessEngine processEngine) first!");
    }
  }

  @Test
  void testInit() {
    // Given
    reset();
    // When
    init(processEngine);
    // Then
    assertThat(BpmnAwareTests.processEngine()).isNotNull().isSameAs(processEngine);
  }

  @Test
  void testReset() {
    // When
    reset();
    // Then
    assertThat(BpmnAwareTests.processEngine.get()).isNull();
  }

  @Test
  void assertThatProcessDefinition() {
    // Given
    ProcessDefinition processDefinition = Mockito.mock(ProcessDefinition.class);
    // When
    ProcessDefinitionAssert returnedAssert = assertThat(processDefinition);
    // Then
    assertThat(returnedAssert).isInstanceOf(ProcessDefinitionAssert.class);
    ProcessDefinitionAssert processDefinitionAssert = assertThat(processDefinition);
    assertThat(processDefinitionAssert.getActual()).isSameAs(processDefinition);
  }

  @Test
  void assertThatProcessInstance() {
    // Given
    ProcessInstance processInstance = Mockito.mock(ProcessInstance.class);
    // When
    ProcessInstanceAssert returnedAssert = assertThat(processInstance);
    // Then
    assertThat(returnedAssert).isInstanceOf(ProcessInstanceAssert.class);
    ProcessInstanceAssert processInstanceAssert = assertThat(processInstance);
    assertThat(processInstanceAssert.getActual()).isSameAs(processInstance);
  }

  @Test
  void assertThatTask() {
    // Given
    Task task = Mockito.mock(Task.class);
    // When
    TaskAssert returnedAssert = assertThat(task);
    // Then
    assertThat(returnedAssert).isInstanceOf(TaskAssert.class);
    TaskAssert taskAssert = assertThat(task);
    assertThat(taskAssert.getActual()).isSameAs(task);
  }

  @Test
  void assertThatJob() {
    // Given
    Job job = Mockito.mock(Job.class);
    // When
    JobAssert returnedAssert = assertThat(job);
    // Then
    assertThat(returnedAssert).isInstanceOf(JobAssert.class);
    JobAssert jobAssert = assertThat(job);
    assertThat(jobAssert.getActual()).isSameAs(job);
  }

  @Test
  void assertThatCaseInstance() {
    //Given
    CaseInstance caseInstance = Mockito.mock(CaseInstance.class);
    // When
    CaseInstanceAssert returnedAssert = assertThat(caseInstance);
    // Then
    assertThat(returnedAssert.getActual()).isSameAs(caseInstance);
  }

  @Test
  void assertThatCaseExecution() {
    //Given
    CaseExecution caseExecution = Mockito.mock(CaseExecution.class);
    // When
    CaseExecutionAssert returnedAssert = assertThat(caseExecution);
    // Then
    assertThat(returnedAssert.getActual()).isSameAs(caseExecution);
  }

  @Test
  void assertThatCaseDefinition() {
    //Given
    CaseDefinition caseDefinition = Mockito.mock(CaseDefinition.class);
    // When
    CaseDefinitionAssert returnedAssert = assertThat(caseDefinition);
    // Then
    assertThat(returnedAssert.getActual()).isSameAs(caseDefinition);
  }

  @Test
  void testRuntimeService() {
    // Given
    RuntimeService runtimeService = mock(RuntimeService.class);
    when(processEngine.getRuntimeService()).thenReturn(runtimeService);
    // When
    RuntimeService returnedService = runtimeService();
    // Then
    assertThat(returnedService).isNotNull().isSameAs(runtimeService);
    verify(processEngine, times(1)).getRuntimeService();
    verifyNoMoreInteractions(processEngine);
  }

  @Test
  void testAuthorizationService() {
    // Given
    AuthorizationService authorizationService = mock(AuthorizationService.class);
    when(processEngine.getAuthorizationService()).thenReturn(authorizationService);
    // When
    AuthorizationService returnedService = authorizationService();
    // Then
    assertThat(returnedService).isNotNull().isSameAs(authorizationService);
    verify(processEngine, times(1)).getAuthorizationService();
    verifyNoMoreInteractions(processEngine);
  }

  @Test
  void testFormService() {
    // Given
    FormService formService = mock(FormService.class);
    when(processEngine.getFormService()).thenReturn(formService);
    // When
    FormService returnedService = formService();
    // Then
    assertThat(returnedService).isNotNull().isSameAs(formService);
    verify(processEngine, times(1)).getFormService();
    verifyNoMoreInteractions(processEngine);
  }

  @Test
  void testHistoryService() {
    // Given
    HistoryService historyService = mock(HistoryService.class);
    when(processEngine.getHistoryService()).thenReturn(historyService);
    // When
    HistoryService returnedService = historyService();
    // Then
    assertThat(returnedService).isNotNull().isSameAs(historyService);
    verify(processEngine, times(1)).getHistoryService();
    verifyNoMoreInteractions(processEngine);
  }

  @Test
  void testIdentityService() {
    // Given
    IdentityService identityService = mock(IdentityService.class);
    when(processEngine.getIdentityService()).thenReturn(identityService);
    // When
    IdentityService returnedService = identityService();
    // Then
    assertThat(returnedService).isNotNull().isSameAs(identityService);
    verify(processEngine, times(1)).getIdentityService();
    verifyNoMoreInteractions(processEngine);
  }

  @Test
  void testManagementService() {
    // Given
    ManagementService managementService = mock(ManagementService.class);
    when(processEngine.getManagementService()).thenReturn(managementService);
    // When
    ManagementService returnedService = managementService();
    // Then
    assertThat(returnedService).isNotNull().isSameAs(managementService);
    verify(processEngine, times(1)).getManagementService();
    verifyNoMoreInteractions(processEngine);
  }

  @Test
  void testRepositoryService() {
    // Given
    RepositoryService repositoryService = mock(RepositoryService.class);
    when(processEngine.getRepositoryService()).thenReturn(repositoryService);
    // When
    RepositoryService returnedService = repositoryService();
    // Then
    assertThat(returnedService).isNotNull().isSameAs(repositoryService);
    verify(processEngine, times(1)).getRepositoryService();
    verifyNoMoreInteractions(processEngine);
  }

  @Test
  void testTaskService() {
    // Given
    TaskService taskService = mock(TaskService.class);
    when(processEngine.getTaskService()).thenReturn(taskService);
    // When
    TaskService returnedService = taskService();
    // Then
    assertThat(returnedService).isNotNull().isSameAs(taskService);
    verify(processEngine, times(1)).getTaskService();
    verifyNoMoreInteractions(processEngine);
  }

  @Test
  void testTaskQuery() {
    // Given
    TaskService taskService = mock(TaskService.class);
    TaskQuery taskQuery = mock(TaskQuery.class);
    when(processEngine.getTaskService()).thenReturn(taskService);
    when(taskService.createTaskQuery()).thenReturn(taskQuery);
    // When
    TaskQuery createdQuery = taskQuery();
    // Then
    assertThat(createdQuery).isNotNull().isSameAs(taskQuery);
    verify(taskService, times(1)).createTaskQuery();
    verifyNoMoreInteractions(taskService);
  }

  @Test
  void testJobQuery() {
    // Given
    ManagementService managementService = mock(ManagementService.class);
    JobQuery jobQuery = mock(JobQuery.class);
    when(processEngine.getManagementService()).thenReturn(managementService);
    when(managementService.createJobQuery()).thenReturn(jobQuery);
    // When
    JobQuery createdQuery = jobQuery();
    // Then
    assertThat(createdQuery).isNotNull().isSameAs(jobQuery);
    verify(managementService, times(1)).createJobQuery();
    verifyNoMoreInteractions(managementService);
  }

  @Test
  void testProcessInstanceQuery() {
    // Given
    RuntimeService runtimeService = mock(RuntimeService.class);
    ProcessInstanceQuery processInstanceQuery = mock(ProcessInstanceQuery.class);
    when(processEngine.getRuntimeService()).thenReturn(runtimeService);
    when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
    // When
    ProcessInstanceQuery createdQuery = processInstanceQuery();
    // Then
    assertThat(createdQuery).isNotNull().isSameAs(processInstanceQuery);
    verify(runtimeService, times(1)).createProcessInstanceQuery();
    verifyNoMoreInteractions(runtimeService);
  }

  @Test
  void testProcessDefinitionQuery() {
    // Given
    RepositoryService repositoryService = mock(RepositoryService.class);
    ProcessDefinitionQuery processDefinitionQuery = mock(ProcessDefinitionQuery.class);
    when(processEngine.getRepositoryService()).thenReturn(repositoryService);
    when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
    // When
    ProcessDefinitionQuery createdQuery = processDefinitionQuery();
    // Then
    assertThat(createdQuery).isNotNull().isSameAs(processDefinitionQuery);
    verify(repositoryService, times(1)).createProcessDefinitionQuery();
    verifyNoMoreInteractions(repositoryService);
  }

  @Test
  void testExecutionQuery() {
    // Given
    RuntimeService runtimeService = mock(RuntimeService.class);
    ExecutionQuery executionQuery = mock(ExecutionQuery.class);
    when(processEngine.getRuntimeService()).thenReturn(runtimeService);
    when(runtimeService.createExecutionQuery()).thenReturn(executionQuery);
    // When
    ExecutionQuery createdQuery = executionQuery();
    // Then
    assertThat(createdQuery).isNotNull().isSameAs(executionQuery);
    verify(runtimeService, times(1)).createExecutionQuery();
    verifyNoMoreInteractions(runtimeService);
  }

}
