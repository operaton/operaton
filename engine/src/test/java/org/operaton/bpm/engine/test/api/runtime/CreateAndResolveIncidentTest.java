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
package org.operaton.bpm.engine.test.api.runtime;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.incident.IncidentContext;
import org.operaton.bpm.engine.impl.incident.IncidentHandler;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.IncidentEntity;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.impl.test.TestHelper.executeJobIgnoringException;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CreateAndResolveIncidentTest {

  public static final BpmnModelInstance ASYNC_TASK_PROCESS = Bpmn.createExecutableProcess("process")
      .startEvent("start")
      .serviceTask("task")
        .operatonAsyncBefore()
        .operatonExpression("${true}")
      .endEvent("end")
      .done();

  public static final BpmnModelInstance EXTERNAL_TASK_PROCESS = Bpmn.createExecutableProcess("process")
      .startEvent("start")
      .serviceTask("task")
        .operatonExternalTask("topic")
      .endEvent("end")
      .done();

  private static final CustomIncidentHandler CUSTOM_HANDLER = new CustomIncidentHandler("custom");
  private static final CustomIncidentHandler JOB_HANDLER = new CustomIncidentHandler(Incident.FAILED_JOB_HANDLER_TYPE);
  private static final CustomIncidentHandler EXTERNAL_TASK_HANDLER = new CustomIncidentHandler(Incident.EXTERNAL_TASK_HANDLER_TYPE);

  private static final List<IncidentHandler> HANDLERS = new ArrayList<>();
  static {
    HANDLERS.add(CUSTOM_HANDLER);
    HANDLERS.add(JOB_HANDLER);
    HANDLERS.add(EXTERNAL_TASK_HANDLER);
  }

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
      .closeEngineAfterAllTests()
      .randomEngineName()
      .configurator(configuration -> configuration.setCustomIncidentHandlers(HANDLERS))
      .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  ManagementService managementService;
  ExternalTaskService externalTaskService;

  @AfterEach
  void resetHandlers() {
    HANDLERS.forEach(h -> ((CustomIncidentHandler) h).reset());
  }

  @Test
  void createIncident() {
    // given
    testRule.deploy(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");

    // when
    Incident incident = runtimeService.createIncident("foo", processInstance.getId(), "aa", "bar");

    // then
    Incident incident2 = runtimeService.createIncidentQuery().executionId(processInstance.getId()).singleResult();
    assertThat(incident.getId()).isEqualTo(incident2.getId());
    assertThat(incident2.getIncidentType()).isEqualTo("foo");
    assertThat(incident2.getConfiguration()).isEqualTo("aa");
    assertThat(incident2.getIncidentMessage()).isEqualTo("bar");
    assertThat(incident2.getExecutionId()).isEqualTo(processInstance.getId());
  }

  @Test
  void createIncidentWithNullExecution() {
    assertThatThrownBy(() -> runtimeService.createIncident("foo", null, "userTask1", "bar"))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Execution id cannot be null");
  }

  @Test
  void createIncidentWithNullIncidentType() {
    // when/then
    assertThatThrownBy(() -> runtimeService.createIncident(null, "processInstanceId", "foo", "bar"))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("incidentType is null");
  }

  @Test
  void createIncidentWithNonExistingExecution() {
    // when/then
    assertThatThrownBy(() -> runtimeService.createIncident("foo", "aaa", "bbb", "bar"))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Cannot find an execution with executionId 'aaa'");
  }

  @Test
  void resolveIncident() {
    // given
    testRule.deploy(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");
    Incident incident = runtimeService.createIncident("foo", processInstance.getId(), "userTask1", "bar");

    // when
    runtimeService.resolveIncident(incident.getId());

    // then
    Incident incident2 = runtimeService.createIncidentQuery().executionId(processInstance.getId()).singleResult();
    assertThat(incident2).isNull();
  }

  @Test
  void resolveUnexistingIncident() {
    // when/then
    assertThatThrownBy(() -> runtimeService.resolveIncident("foo"))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Cannot find an incident with id 'foo'");
  }

  @Test
  void resolveNullIncident() {
    // when/then
    assertThatThrownBy(() -> runtimeService.resolveIncident(null))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("incidentId is null");
  }

  @Test
  void resolveIncidentOfTypeFailedJob() {
    // given
    testRule.deploy("org/operaton/bpm/engine/test/api/mgmt/IncidentTest.testShouldCreateOneIncident.bpmn");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingProcess");

    // when
    List<Job> jobs = engineRule.getManagementService().createJobQuery().withRetriesLeft().list();

    for (Job job : jobs) {
      engineRule.getManagementService().setJobRetries(job.getId(), 1);
      executeJobIgnoringException(engineRule.getManagementService(), job.getId());
    }

    // then
    Incident incident = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).singleResult();
    var incidentId = incident.getId();
    assertThatThrownBy(() -> runtimeService.resolveIncident(incidentId))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Cannot resolve an incident of type failedJob");
  }

  @Test
  void createIncidentWithIncidentHandler() {
    // given
    testRule.deploy(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");

    // when
    Incident incident = runtimeService.createIncident("custom", processInstance.getId(), "configuration");

    // then
    assertThat(incident).isNotNull();

    Incident incident2 = runtimeService.createIncidentQuery().singleResult();
    assertThat(incident2)
            .isNotNull()
            .isEqualTo(incident);
    assertThat(incident.getIncidentType()).isEqualTo("custom");
    assertThat(incident.getConfiguration()).isEqualTo("configuration");

    assertThat(CUSTOM_HANDLER.getCreateEvents()).hasSize(1);
    assertThat(CUSTOM_HANDLER.getResolveEvents()).isEmpty();
    assertThat(CUSTOM_HANDLER.getDeleteEvents()).isEmpty();
  }

  @Test
  void resolveIncidentWithIncidentHandler() {
    // given
    testRule.deploy(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");
    runtimeService.createIncident("custom", processInstance.getId(), "configuration");
    Incident incident = runtimeService.createIncidentQuery().singleResult();

    // when
    runtimeService.resolveIncident(incident.getId());

    // then
    incident = runtimeService.createIncidentQuery().singleResult();
    assertThat(incident).isNull();

    assertThat(CUSTOM_HANDLER.getCreateEvents()).hasSize(1);
    assertThat(CUSTOM_HANDLER.getResolveEvents()).hasSize(1);
    assertThat(CUSTOM_HANDLER.getDeleteEvents()).isEmpty();
  }

  @Test
  void shouldCallDeleteForCustomHandlerOnProcessInstanceCancellation() {
    // given
    testRule.deploy(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");
    runtimeService.createIncident("custom", processInstance.getId(), "configuration");

    CUSTOM_HANDLER.reset();

    // when
    runtimeService.deleteProcessInstance(processInstance.getId(), null);

    // then
    assertThat(CUSTOM_HANDLER.getCreateEvents()).isEmpty();
    assertThat(CUSTOM_HANDLER.getResolveEvents()).isEmpty();
    assertThat(CUSTOM_HANDLER.getDeleteEvents()).hasSize(1);

    IncidentContext deleteContext = CUSTOM_HANDLER.getDeleteEvents().get(0);
    assertThat(deleteContext.getConfiguration()).isEqualTo("configuration");

    long numIncidents = runtimeService.createIncidentQuery().count();
    assertThat(numIncidents).isZero();
  }

  @Test
  void shouldCallDeleteForJobHandlerOnProcessInstanceCancellation() {

    // given
    testRule.deploy(ASYNC_TASK_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    Job job = managementService.createJobQuery().singleResult();
    managementService.setJobRetries(job.getId(), 0);

    JOB_HANDLER.reset();

    // when
    runtimeService.deleteProcessInstance(processInstance.getId(), null);

    // then
    assertThat(JOB_HANDLER.getCreateEvents()).isEmpty();
    assertThat(JOB_HANDLER.getResolveEvents()).isEmpty();
    assertThat(JOB_HANDLER.getDeleteEvents()).hasSize(1);

    IncidentContext deleteContext = JOB_HANDLER.getDeleteEvents().get(0);
    assertThat(deleteContext.getConfiguration()).isEqualTo(job.getId());

    long numIncidents = runtimeService.createIncidentQuery().count();
    assertThat(numIncidents).isZero();
  }

  @Test
  void shouldCallDeleteForExternalTasksHandlerOnProcessInstanceCancellation() {

    // given
    testRule.deploy(EXTERNAL_TASK_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(1, "foo").topic("topic", 1000L).execute();
    LockedExternalTask task = tasks.get(0);

    externalTaskService.setRetries(task.getId(), 0);

    EXTERNAL_TASK_HANDLER.reset();

    // when
    runtimeService.deleteProcessInstance(processInstance.getId(), null);

    // then
    assertThat(EXTERNAL_TASK_HANDLER.getCreateEvents()).isEmpty();
    assertThat(EXTERNAL_TASK_HANDLER.getResolveEvents()).isEmpty();
    assertThat(EXTERNAL_TASK_HANDLER.getDeleteEvents()).hasSize(1);

    IncidentContext deleteContext = EXTERNAL_TASK_HANDLER.getDeleteEvents().get(0);
    assertThat(deleteContext.getConfiguration()).isEqualTo(task.getId());

    long numIncidents = runtimeService.createIncidentQuery().count();
    assertThat(numIncidents).isZero();
  }

  @Test
  void shouldCallResolveForJobHandler() {
    // given
    testRule.deploy(ASYNC_TASK_PROCESS);
    runtimeService.startProcessInstanceByKey("process");
    Job job = managementService.createJobQuery().singleResult();
    managementService.setJobRetries(job.getId(), 0);

    JOB_HANDLER.reset();

    // when
    managementService.setJobRetries(job.getId(), 1);

    // then
    assertThat(JOB_HANDLER.getCreateEvents()).isEmpty();
    assertThat(JOB_HANDLER.getResolveEvents()).hasSize(1);
    assertThat(JOB_HANDLER.getDeleteEvents()).isEmpty();

    IncidentContext deleteContext = JOB_HANDLER.getResolveEvents().get(0);
    assertThat(deleteContext.getConfiguration()).isEqualTo(job.getId());

    long numIncidents = runtimeService.createIncidentQuery().count();
    assertThat(numIncidents).isZero();
  }

  @Test
  void shouldCallResolveForExternalTaskHandler() {
    // given
    testRule.deploy(EXTERNAL_TASK_PROCESS);
    runtimeService.startProcessInstanceByKey("process");
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(1, "foo").topic("topic", 1000L).execute();
    LockedExternalTask task = tasks.get(0);

    externalTaskService.setRetries(task.getId(), 0);

    EXTERNAL_TASK_HANDLER.reset();

    // when
    externalTaskService.setRetries(task.getId(), 1);

    // then
    assertThat(EXTERNAL_TASK_HANDLER.getCreateEvents()).isEmpty();
    assertThat(EXTERNAL_TASK_HANDLER.getResolveEvents()).hasSize(1);
    assertThat(EXTERNAL_TASK_HANDLER.getDeleteEvents()).isEmpty();

    IncidentContext resolveContext = EXTERNAL_TASK_HANDLER.getResolveEvents().get(0);
    assertThat(resolveContext.getConfiguration()).isEqualTo(task.getId());

    long numIncidents = runtimeService.createIncidentQuery().count();
    assertThat(numIncidents).isZero();
  }

  public static class CustomIncidentHandler implements IncidentHandler {

    private final String incidentType;

    private final List<IncidentContext> createEvents = new ArrayList<>();
    private final List<IncidentContext> resolveEvents = new ArrayList<>();
    private final List<IncidentContext> deleteEvents = new ArrayList<>();

    public CustomIncidentHandler(String type) {
      this.incidentType = type;
    }

    @Override
    public String getIncidentHandlerType() {
      return incidentType;
    }

    @Override
    public Incident handleIncident(IncidentContext context, String message) {
      createEvents.add(context);
      return IncidentEntity.createAndInsertIncident(incidentType, context, message);
    }

    @Override
    public void resolveIncident(IncidentContext context) {
      resolveEvents.add(context);
      deleteIncidentEntity(context);
    }

    @Override
    public void deleteIncident(IncidentContext context) {
      deleteEvents.add(context);
      deleteIncidentEntity(context);
    }

    private void deleteIncidentEntity(IncidentContext context) {
      CommandContext commandContext = requireNonNull(Context.getCommandContext());
      List<Incident> incidents = commandContext.getIncidentManager()
          .findIncidentByConfigurationAndIncidentType(context.getConfiguration(), incidentType);

      incidents.forEach(i -> ((IncidentEntity) i).delete());
    }

    public List<IncidentContext> getCreateEvents() {
      return createEvents;
    }

    public List<IncidentContext> getResolveEvents() {
      return resolveEvents;
    }

    public List<IncidentContext> getDeleteEvents() {
      return deleteEvents;
    }

    public void reset() {
      createEvents.clear();
      resolveEvents.clear();
      deleteEvents.clear();
    }

  }
}
