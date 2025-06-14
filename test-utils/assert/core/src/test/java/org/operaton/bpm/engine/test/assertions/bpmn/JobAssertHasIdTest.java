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

import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.jobQuery;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.runtimeService;

import org.junit.jupiter.api.Test;

class JobAssertHasIdTest extends ProcessAssertTestCase {

  @Test
  @Deployment(resources = {"bpmn/JobAssert-hasId.bpmn"
  })
  void hasIdSuccess() {
    // When
    runtimeService().startProcessInstanceByKey(
      "JobAssert-hasId"
    );
    // Then
    assertThat(jobQuery().singleResult()).isNotNull();
    // And
    assertThat(jobQuery().singleResult()).hasId(jobQuery().singleResult().getId());
  }

  @Test
  @Deployment(resources = {"bpmn/JobAssert-hasId.bpmn"
  })
  void hasIdFailure() {
    // When
    runtimeService().startProcessInstanceByKey(
      "JobAssert-hasId"
    );
    // Then
    assertThat(jobQuery().singleResult()).isNotNull();
    // And
    expect(() -> assertThat(jobQuery().singleResult()).hasId("otherId"));
  }

  @Test
  @Deployment(resources = {"bpmn/JobAssert-hasId.bpmn"
  })
  void hasIdErrorNull() {
    // When
    runtimeService().startProcessInstanceByKey(
      "JobAssert-hasId"
    );
    // Then
    assertThat(jobQuery().singleResult()).isNotNull();
    // And
    expect(() -> assertThat(jobQuery().singleResult()).hasId(null));
  }

}
