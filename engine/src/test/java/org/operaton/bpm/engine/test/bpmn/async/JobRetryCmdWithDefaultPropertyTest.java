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
package org.operaton.bpm.engine.test.bpmn.async;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.ProcessEngineBootstrapRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Stefan Hentschel.
 */
public class JobRetryCmdWithDefaultPropertyTest {

  @ClassRule
  public static ProcessEngineBootstrapRule bootstrapRule = new ProcessEngineBootstrapRule(
      "org/operaton/bpm/engine/test/bpmn/async/default.job.retry.property.operaton.cfg.xml");

  @Rule
  public ProvidedProcessEngineRule engineRule = new ProvidedProcessEngineRule(bootstrapRule);

  protected RuntimeService runtimeService;
  protected ManagementService managementService;

  @Before
  public void setUp() {
    runtimeService = engineRule.getRuntimeService();
    managementService = engineRule.getManagementService();
  }
  /**
   * Check if property "DefaultNumberOfRetries" will be used
   */
  @Deployment(resources = { "org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.testFailedTask.bpmn20.xml" })
  @Test
  public void testDefaultNumberOfRetryProperty() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("failedTask");
    assertThat(pi).isNotNull();

    Job job = managementService.createJobQuery().processInstanceId(pi.getProcessInstanceId()).singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getProcessInstanceId()).isEqualTo(pi.getProcessInstanceId());
    assertThat(job.getRetries()).isEqualTo(2);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.testFailedServiceTask.bpmn20.xml" })
  @Test
  public void testOverwritingPropertyWithBpmnExtension() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("failedServiceTask");
    assertThat(pi).isNotNull();

    Job job = managementService.createJobQuery().processInstanceId(pi.getProcessInstanceId()).singleResult();
    assertThat(job).isNotNull();
    var jobId = job.getId();
    assertThat(job.getProcessInstanceId()).isEqualTo(pi.getProcessInstanceId());

    try {
      managementService.executeJob(jobId);
      fail("Exception expected!");
    } catch(Exception e) {
      // expected
    }

    job = managementService.createJobQuery().jobId(job.getId()).singleResult();
    assertThat(job.getRetries()).isEqualTo(4);

  }
}
