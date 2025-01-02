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
package org.operaton.bpm.qa.performance.engine.bpmn;

import static org.operaton.bpm.qa.performance.engine.steps.PerfTestConstants.PROCESS_INSTANCE_ID;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.qa.performance.engine.junit.ProcessEnginePerformanceTestCase;
import org.operaton.bpm.qa.performance.engine.steps.SignalExecutionStep;
import org.operaton.bpm.qa.performance.engine.steps.StartProcessInstanceStep;

/**
 * @author Daniel Meyer
 *
 */
class SequencePerformanceTest extends ProcessEnginePerformanceTestCase {

  @Test
  @Deployment
  void syncSequence1Step() {
    performanceTest()
      .step(new StartProcessInstanceStep(engine, "process"))
    .run();
  }

  @Test
  @Deployment
  void syncSequence5Steps() {
    performanceTest()
      .step(new StartProcessInstanceStep(engine, "process"))
    .run();
  }

  @Test
  @Deployment
  void syncSequence15Steps() {
    performanceTest()
      .step(new StartProcessInstanceStep(engine, "process"))
    .run();
  }

  @Test
  @Deployment
  void asyncSequence1Step() {
    performanceTest()
      .step(new StartProcessInstanceStep(engine, "process"))
      .step(new SignalExecutionStep(engine, PROCESS_INSTANCE_ID))
    .run();
  }

  @Test
  @Deployment
  void asyncSequence5Steps() {
    performanceTest()
      .step(new StartProcessInstanceStep(engine, "process"))
      .steps(5, new SignalExecutionStep(engine, PROCESS_INSTANCE_ID))
    .run();
  }

  @Test
  @Deployment
  void asyncSequence15Steps() {
    performanceTest()
      .step(new StartProcessInstanceStep(engine, "process"))
      .steps(15, new SignalExecutionStep(engine, PROCESS_INSTANCE_ID))
    .run();
  }



}
