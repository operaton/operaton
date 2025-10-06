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

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.mock.Mocks;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;

import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

class ProcessEngineTestsJobTest extends ProcessAssertTestCase {

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-job.bpmn"
  })
  void jobOnlyActivitySuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // When
    assertThat(processInstance).isNotNull();
    // Then
    assertThat(job()).isNotNull();
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-job.bpmn"
  })
  void jobOnlyActivityFailure() {
    // Given
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // Then
    expect(BpmnAwareTests::job, IllegalStateException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-job.bpmn"
  })
  void jobTwoActivitiesFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // And
    assertThat(processInstance).isNotNull();
    // And
    Mocks.register("serviceTask_1", "someService");
    // And
    execute(job());
    // When
    expect(BpmnAwareTests::job, ProcessEngineException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-job.bpmn"
  })
  void jobActivityIdOnlyActivitySuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // When
    assertThat(processInstance).isNotNull();
    // Then
    assertThat(job("ServiceTask_1")).isNotNull();
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-job.bpmn"
  })
  void jobActivityIdTwoActivitiesSuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // When
    assertThat(processInstance).isNotNull();
    // And
    Mocks.register("serviceTask_1", "someService");
    // And
    execute(job());
    // Then
    assertThat(job("ServiceTask_2")).isNotNull();
    // And
    assertThat(job("ServiceTask_3")).isNotNull();
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-job.bpmn"
  })
  void jobActivityIdOnlyActivityFailure() {
    // Given
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // Then
    expect(() -> job("ServiceTask_1"), IllegalStateException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-job.bpmn"
  })
  void jobJobQueryOnlyActivitySuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // When
    assertThat(processInstance).isNotNull();
    // Then
    assertThat(job(jobQuery())).isNotNull();
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-job.bpmn"
  })
  void jobJobQueryOnlyActivityFailure() {
    // Given
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // Then
    expect(() -> job(jobQuery()), IllegalStateException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-job.bpmn"
  })
  void jobJobQueryTwoActivitiesFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // And
    assertThat(processInstance).isNotNull();
    // And
    Mocks.register("serviceTask_1", "someService");
    // And
    execute(job());
    // When
    expect(() -> job(jobQuery()), ProcessEngineException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-job.bpmn"
  })
  void jobProcessInstanceOnlyActivitySuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // Then
    assertThat(job(processInstance)).isNotNull();
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-job.bpmn"
  })
  void jobTwoActivitiesProcessInstanceFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // And
    Mocks.register("serviceTask_1", "someService");
    // And
    execute(job(processInstance));
    // When
    expect(() -> job(processInstance), ProcessEngineException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-job.bpmn"
  })
  void jobJobDefinitionKeyProcessInstanceOnlyActivitySuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // Then
    assertThat(job("ServiceTask_1", processInstance)).isNotNull();
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-job.bpmn"
  })
  void jobJobDefinitionKeyProcessInstanceTwoActivitiesSuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // And
    Mocks.register("serviceTask_1", "someService");
    // When
    execute(job(processInstance));
    // Then
    assertThat(job("ServiceTask_2", processInstance)).isNotNull();
    // And
    assertThat(job("ServiceTask_3", processInstance)).isNotNull();
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-job.bpmn"
  })
  void jobJobQueryProcessInstanceOnlyActivitySuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // Then
    assertThat(job(jobQuery(), processInstance)).isNotNull();
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-job.bpmn"
  })
  void jobJobQueryProcessInstanceTwoActivitiesFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-job"
    );
    // And
    Mocks.register("serviceTask_1", "someService");
    // And
    execute(job(processInstance));
    // When
    expect(() -> job(jobQuery(), processInstance), ProcessEngineException.class);
  }

}
