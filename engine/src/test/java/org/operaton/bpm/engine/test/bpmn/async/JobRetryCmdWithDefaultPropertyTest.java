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
package org.operaton.bpm.engine.test.bpmn.async;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.impl.test.TestHelper.executeJobExpectingException;

/**
 * @author Stefan Hentschel.
 */
class JobRetryCmdWithDefaultPropertyTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .configurationResource("org/operaton/bpm/engine/test/bpmn/async/default.job.retry.property.operaton.cfg.xml")
    .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  ManagementService managementService;

  /**
   * Check if property "DefaultNumberOfRetries" will be used
   */
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.testFailedTask.bpmn20.xml"})
  @Test
  void testDefaultNumberOfRetryProperty() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("failedTask");
    assertThat(pi).isNotNull();

    Job job = managementService.createJobQuery().processInstanceId(pi.getProcessInstanceId()).singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getProcessInstanceId()).isEqualTo(pi.getProcessInstanceId());
    assertThat(job.getRetries()).isEqualTo(5);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.testFailedServiceTask.bpmn20.xml"})
  @Test
  void testOverwritingPropertyWithBpmnExtension() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("failedServiceTask");
    assertThat(pi).isNotNull();

    Job job = managementService.createJobQuery().processInstanceId(pi.getProcessInstanceId()).singleResult();
    assertThat(job).isNotNull();
    var jobId = job.getId();
    assertThat(job.getProcessInstanceId()).isEqualTo(pi.getProcessInstanceId());

    executeJobExpectingException(managementService, jobId);

    job = managementService.createJobQuery().jobId(job.getId()).singleResult();
    assertThat(job.getRetries()).isEqualTo(4);

  }
}
