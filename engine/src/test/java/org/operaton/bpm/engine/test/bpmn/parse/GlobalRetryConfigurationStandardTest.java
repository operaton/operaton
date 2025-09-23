/*
 *  Copyright 2025 the Operaton contributors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.operaton.bpm.engine.test.bpmn.parse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GlobalRetryConfigurationStandardTest {

  private static final String PROCESS_ID = "process";
  private static final String FAILING_CLASS = "this.class.does.not.Exist";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
      .randomEngineName()
      .closeEngineAfterEachTest()
      .configurator(configuration -> configuration.setFailedJobRetryTimeCycle(null))
      .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  ManagementService managementService;

  @Test
  void testFailedServiceTaskStandardStrategy() {
    BpmnModelInstance bpmnModelInstance = prepareFailingServiceTask();

    testRule.deploy(bpmnModelInstance);

    ProcessInstance pi = runtimeService.startProcessInstanceByKey(PROCESS_ID);

    assertJobRetries(pi, 2);
  }

  private void assertJobRetries(ProcessInstance pi, int expectedJobRetries) {
    assertThat(pi).isNotNull();

    final Job job = fetchJob(pi.getProcessInstanceId());

    assertThatThrownBy(() -> managementService.executeJob(job.getId()))
      .isInstanceOf(Exception.class);

    // update job
    assertThat(fetchJob(pi.getProcessInstanceId()).getRetries()).isEqualTo(expectedJobRetries);
  }

  private Job fetchJob(String processInstanceId) {
    return managementService.createJobQuery().processInstanceId(processInstanceId).singleResult();
  }

  private BpmnModelInstance prepareFailingServiceTask() {
    return Bpmn.createExecutableProcess(PROCESS_ID)
        .startEvent()
        .serviceTask()
          .operatonClass(FAILING_CLASS)
          .operatonAsyncBefore()
        .endEvent()
        .done();
  }

}
