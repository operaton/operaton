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
package org.operaton.bpm.qa.performance.engine.bpmn;

import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.qa.performance.engine.bpmn.delegate.NoopDelegate;
import org.operaton.bpm.qa.performance.engine.junit.ProcessEnginePerformanceTestCase;
import org.operaton.bpm.qa.performance.engine.steps.StartProcessInstanceStep;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author Daniel Meyer
 *
 */
class ServiceTaskPerformanceTest extends ProcessEnginePerformanceTestCase {

  @Test
  void threeServiceTasksAndAGateway() {

    Map<String, Object> variables = new HashMap<>();
    variables.put("approved", true);

    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(180)
        .startEvent()
      .serviceTask()
        .operatonClass(NoopDelegate.class.getName())
      .exclusiveGateway("decision").condition("approved", "${approved}")
      .serviceTask()
        .operatonClass(NoopDelegate.class.getName())
      .moveToLastGateway().condition("not approved", "${not approved}")
      .serviceTask()
        .operatonClass(NoopDelegate.class.getName())
      .endEvent()
      .done();

    assertThatCode(() -> repositoryService.createDeployment()
      .addModelInstance("process.bpmn", process)
      .deploy()).doesNotThrowAnyException();

    performanceTest()
      .step(new StartProcessInstanceStep(engine, "process", variables))
    .run();
  }

}
