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
package org.operaton.bpm.engine.test.bpmn.job;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.jobexecutor.DefaultJobPriorityProvider;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
class JobPrioritizationBpmnExpressionValueTest {

  protected static final long EXPECTED_DEFAULT_PRIORITY = 123;
  protected static final long EXPECTED_DEFAULT_PRIORITY_ON_RESOLUTION_FAILURE = 296;

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  ManagementService managementService;

  protected long originalDefaultPriority;
  protected long originalDefaultPriorityOnFailure;

  @BeforeEach
  void setUp() {
    originalDefaultPriority = DefaultJobPriorityProvider.DEFAULT_PRIORITY;
    originalDefaultPriorityOnFailure = DefaultJobPriorityProvider.DEFAULT_PRIORITY_ON_RESOLUTION_FAILURE;

    DefaultJobPriorityProvider.DEFAULT_PRIORITY = EXPECTED_DEFAULT_PRIORITY;
    DefaultJobPriorityProvider.DEFAULT_PRIORITY_ON_RESOLUTION_FAILURE = EXPECTED_DEFAULT_PRIORITY_ON_RESOLUTION_FAILURE;
  }

  @AfterEach
  void tearDown() {
    // reset default priorities
    DefaultJobPriorityProvider.DEFAULT_PRIORITY = originalDefaultPriority;
    DefaultJobPriorityProvider.DEFAULT_PRIORITY_ON_RESOLUTION_FAILURE = originalDefaultPriorityOnFailure;
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/job/jobPrioExpressionProcess.bpmn20.xml")
  @Test
  void testConstantValueExpressionPrioritization() {
    // when
    runtimeService
      .createProcessInstanceByKey("jobPrioExpressionProcess")
      .startBeforeActivity("task2")
      .execute();

    // then
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getPriority()).isEqualTo(15);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/job/jobPrioExpressionProcess.bpmn20.xml")
  @Test
  void testConstantValueHashExpressionPrioritization() {
    // when
    runtimeService
      .createProcessInstanceByKey("jobPrioExpressionProcess")
      .startBeforeActivity("task4")
      .execute();

    // then
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getPriority()).isEqualTo(16);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/job/jobPrioExpressionProcess.bpmn20.xml")
  @Test
  void testVariableValueExpressionPrioritization() {
    // when
    runtimeService
      .createProcessInstanceByKey("jobPrioExpressionProcess")
      .startBeforeActivity("task1")
      .setVariable("priority", 22)
      .execute();

    // then
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getPriority()).isEqualTo(22);
  }

  /**
   * Can't distinguish this case from the cases we have to tolerate due to CAM-4207
   */
  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/job/jobPrioExpressionProcess.bpmn20.xml")
  @Disabled("CAM-4207")
  @Test
  void testVariableValueExpressionPrioritizationFailsWhenVariableMisses() {
    // given
    var processInstantiationBuilder = runtimeService
        .createProcessInstanceByKey("jobPrioExpressionProcess")
        .startBeforeActivity("task1");

    // when/then
    assertThatThrownBy(processInstantiationBuilder::execute)
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("Unknown property used in expression: ${priority}")
        .hasMessageContaining("Cause: Cannot resolve identifier 'priority'");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/job/jobPrioExpressionProcess.bpmn20.xml")
  @Test
  void testExecutionExpressionPrioritization() {
    // when
    runtimeService
      .createProcessInstanceByKey("jobPrioExpressionProcess")
      .startBeforeActivity("task1")
      .setVariable("priority", 25)
      .execute();

    // then
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getPriority()).isEqualTo(25);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/job/jobPrioExpressionProcess.bpmn20.xml")
  @Test
  void testExpressionEvaluatesToNull() {
    // given
    var processInstantiationBuilder = runtimeService
        .createProcessInstanceByKey("jobPrioExpressionProcess")
        .startBeforeActivity("task3")
        .setVariable("priority", null);

    // when/then
    assertThatThrownBy(processInstantiationBuilder::execute)
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("Priority value is not an Integer");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/job/jobPrioExpressionProcess.bpmn20.xml")
  @Test
  void testExpressionEvaluatesToNonNumericalValue() {
    // given
    var processInstantiationBuilder = runtimeService
        .createProcessInstanceByKey("jobPrioExpressionProcess")
        .startBeforeActivity("task3")
        .setVariable("priority", "aNonNumericalVariableValue");

    // when/then
    assertThatThrownBy(processInstantiationBuilder::execute)
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("Priority value is not an Integer");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/job/jobPrioExpressionProcess.bpmn20.xml")
  @Test
  void testExpressionEvaluatesToNonIntegerValue() {
    // given
    var processInstantiationBuilder = runtimeService
        .createProcessInstanceByKey("jobPrioExpressionProcess")
        .startBeforeActivity("task3")
        .setVariable("priority", 4.2d);

    // when/then
    assertThatThrownBy(processInstantiationBuilder::execute)
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("Priority value must be either Short, Integer, or Long");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/job/jobPrioExpressionProcess.bpmn20.xml")
  @Test
  void testConcurrentLocalVariablesAreAccessible() {
    // when
    runtimeService
      .createProcessInstanceByKey("jobPrioExpressionProcess")
      .startBeforeActivity("task2")
      .startBeforeActivity("task1")
      .setVariableLocal("priority", 14) // this is a local variable on the
                                        // concurrent execution entering the activity
      .execute();

    // then
    Job job = managementService.createJobQuery().activityId("task1").singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getPriority()).isEqualTo(14);
  }

  /**
   * This test case asserts that a non-resolving expression does not fail job creation;
   * This is a unit test scenario, where simply the variable misses (in general a human-made error), but
   * the actual case covered by the behavior are missing beans (e.g. in the case the engine can't perform a
   * context switch)
   */
  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/job/jobPrioExpressionProcess.bpmn20.xml")
  @Test
  void testDefaultPriorityWhenBeanMisses() {
    // creating a job with a priority that can't be resolved does not fail entirely but uses a default priority
    runtimeService
      .createProcessInstanceByKey("jobPrioExpressionProcess")
      .startBeforeActivity("task1")
      .execute();

    // then
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job.getPriority()).isEqualTo(EXPECTED_DEFAULT_PRIORITY_ON_RESOLUTION_FAILURE);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/job/jobPrioExpressionProcess.bpmn20.xml")
  @Test
  void testDisableGracefulDegradation() {
    try {
      processEngineConfiguration.setEnableGracefulDegradationOnContextSwitchFailure(false);

      // given
      var processInstantiationBuilder = runtimeService
          .createProcessInstanceByKey("jobPrioExpressionProcess")
          .startBeforeActivity("task1");

      // when/then
      assertThatThrownBy(processInstantiationBuilder::execute)
          .isInstanceOf(ProcessEngineException.class)
          .hasMessageContaining("Unknown property used in expression");

    } finally {
      processEngineConfiguration.setEnableGracefulDegradationOnContextSwitchFailure(true);
    }
  }

  @Test
  void testDefaultEngineConfigurationSetting() {
    ProcessEngineConfigurationImpl config = new StandaloneInMemProcessEngineConfiguration();

    assertThat(config.isEnableGracefulDegradationOnContextSwitchFailure()).isTrue();
  }

}
