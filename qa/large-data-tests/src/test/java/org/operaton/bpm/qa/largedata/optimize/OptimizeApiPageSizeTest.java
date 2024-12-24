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
package org.operaton.bpm.qa.largedata.optimize;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.operaton.bpm.engine.impl.OptimizeService;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.qa.largedata.util.EngineDataGenerator;

class OptimizeApiPageSizeTest {

  private static OptimizeService optimizeService;
  private static final int OPTIMIZE_PAGE_SIZE = 10_000;

  @RegisterExtension
  static ProcessEngineExtension processEngineExtension = ProcessEngineExtension.builder()
      .configurationResource("operaton.cfg.xml")
      .build();

  @BeforeAll
  static void init() {
    optimizeService = processEngineExtension.getProcessEngineConfiguration().getOptimizeService();

    // given the generated engine data
    // make sure that there are at least two pages of each entity available
    EngineDataGenerator generator = new EngineDataGenerator(processEngineExtension.getProcessEngine(),
        OPTIMIZE_PAGE_SIZE * 2, OptimizeApiPageSizeTest.class.getSimpleName());
    generator.generateData();
  }

  @ParameterizedTest
  @MethodSource("optimizeServiceFunctions")
  void databaseCanCopeWithPageSize(TestScenario scenario) {
    // when
    final List<?> pageOfEntries = scenario.getOptimizeServiceFunction().apply(OPTIMIZE_PAGE_SIZE);

    // then
    assertThat(pageOfEntries).hasSize(OPTIMIZE_PAGE_SIZE);
  }

  private static Object[] optimizeServiceFunctions() {
    return new TestScenario[]{
      new TestScenario(
        pageSize -> optimizeService.getRunningHistoricActivityInstances(null, null, pageSize),
        "running historic activity instances"
      ),
      new TestScenario(
        pageSize -> optimizeService.getCompletedHistoricActivityInstances(null, null, pageSize),
        "completed historic activity instances"
      ),
      new TestScenario(
        pageSize -> optimizeService.getRunningHistoricProcessInstances(null, null, pageSize),
        "running historic process instances"
      ),
      new TestScenario(
        pageSize -> optimizeService.getCompletedHistoricProcessInstances(null, null, pageSize),
        "completed historic process instances"
      ),
      new TestScenario(
        pageSize -> optimizeService.getRunningHistoricTaskInstances(null, null, pageSize),
        "running historic task instances"
      ),
      new TestScenario(
        pageSize -> optimizeService.getCompletedHistoricTaskInstances(null, null, pageSize),
        "completed historic task instances"
      ),
      new TestScenario(
        pageSize -> optimizeService.getHistoricIdentityLinkLogs(null, null, pageSize),
        "historic identity link logs"
      ),
      new TestScenario(
        pageSize -> optimizeService.getHistoricUserOperationLogs(null, null, pageSize),
        "historic user operation logs"
      ),
      new TestScenario(
        pageSize -> optimizeService.getHistoricVariableUpdates(null, null, true, pageSize),
        "historic variable updates"
      ),
      new TestScenario(
        pageSize -> optimizeService.getHistoricDecisionInstances(null, null, pageSize),
        "historic decision instances"
      )
    };
  }

  private static class TestScenario {

    private Function<Integer, List<?>> optimizeServiceFunction;
    private String name;

    public TestScenario(final Function<Integer, List<?>> optimizeServiceFunction, final String name) {
      this.optimizeServiceFunction = optimizeServiceFunction;
      this.name = name;
    }

    public Function<Integer, List<?>> getOptimizeServiceFunction() {
      return optimizeServiceFunction;
    }

    @Override
    public String toString() {
      return name;
    }
  }

}
