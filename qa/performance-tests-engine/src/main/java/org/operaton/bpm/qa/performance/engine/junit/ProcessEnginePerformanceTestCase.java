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
package org.operaton.bpm.qa.performance.engine.junit;

import org.operaton.bpm.engine.*;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.qa.performance.engine.framework.PerfTestBuilder;
import org.operaton.bpm.qa.performance.engine.framework.PerfTestConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * <p>Base class for implementing a process engine performance test</p>
 *
 * @author Daniel Meyer, Ingo Richtsmeier
 *
 */
public abstract class ProcessEnginePerformanceTestCase {

  @RegisterExtension
  protected static ProcessEngineExtension processEngineExtension =
      ProcessEngineExtension.builder().useProcessEngine(PerfTestProcessEngine.getInstance()).build();

  @RegisterExtension
  static PerfTestConfigurationExtension testConfigurationRule = new PerfTestConfigurationExtension();

  @RegisterExtension
  PerfTestResultRecorderExtension resultRecorderRule = new PerfTestResultRecorderExtension();

  protected ProcessEngine engine;
  protected TaskService taskService;
  protected HistoryService historyService;
  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;

  @BeforeEach
  public void setup() {
    engine = processEngineExtension.getProcessEngine();
    taskService = engine.getTaskService();
    historyService = engine.getHistoryService();
    runtimeService = engine.getRuntimeService();
    repositoryService = engine.getRepositoryService();
  }

  protected PerfTestBuilder performanceTest() {
    PerfTestConfiguration configuration = testConfigurationRule.getPerformanceTestConfiguration();
    configuration.setPlatform("operaton BPM");
    return new PerfTestBuilder(configuration, resultRecorderRule);
  }

}
