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
package org.operaton.bpm.engine.test.api.mgmt.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.spi.DmnEngineMetricCollector;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.management.Metrics;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.ResetDmnConfigUtil;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.BusinessRuleTask;

public class DecisionMetricsTest extends AbstractMetricsTest {

  public static final String DECISION_DEFINITION_KEY = "decision";
  public static final String DRD_DISH_DECISION_TABLE = "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml";
  public static final String DMN_DECISION_LITERAL_EXPRESSION = "org/operaton/bpm/engine/test/api/dmn/DecisionWithLiteralExpression.dmn";
  public static final String DMN_FILE = "org/operaton/bpm/engine/test/api/mgmt/metrics/ExecutedDecisionElementsTest.dmn11.xml";
  public static final VariableMap VARIABLES = Variables.createVariables().putValue("status", "").putValue("sum", 100);

  protected DecisionService decisionService;

  @BeforeEach
  void setUp() {
    DefaultDmnEngineConfiguration dmnEngineConfiguration = processEngineConfiguration
        .getDmnEngineConfiguration();
    ResetDmnConfigUtil.reset(dmnEngineConfiguration)
        .enableFeelLegacyBehavior(true)
        .init();

    decisionService = engineRule.getDecisionService();
  }

  @AfterEach
  void restore() {
    DefaultDmnEngineConfiguration dmnEngineConfiguration = processEngineConfiguration
        .getDmnEngineConfiguration();
    ResetDmnConfigUtil.reset(dmnEngineConfiguration)
        .enableFeelLegacyBehavior(false)
        .init();
  }

  @Override
  protected void clearMetrics() {
    super.clearMetrics();
    DmnEngineMetricCollector metricCollector = processEngineConfiguration.getDmnEngineConfiguration()
      .getEngineMetricCollector();
    metricCollector.clearExecutedDecisionInstances();
    metricCollector.clearExecutedDecisionElements();
  }

  @Test
  void testBusinessRuleTask() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("testProcess")
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .businessRuleTask("task")
        .endEvent()
        .done();

    BusinessRuleTask task = modelInstance.getModelElementById("task");
    task.setOperatonDecisionRef("decision");

    testRule.deploy(repositoryService.createDeployment()
        .addModelInstance("process.bpmn", modelInstance)
        .addClasspathResource(DMN_FILE));

    assertThat(getExecutedDecisionInstances()).isZero();
    assertThat(getDecisionInstances()).isZero();
    assertThat(getExecutedDecisionElements()).isZero();
    assertThat(getExecutedDecisionInstancesFromDmnEngine()).isZero();
    assertThat(getExecutedDecisionElementsFromDmnEngine()).isZero();

    runtimeService.startProcessInstanceByKey("testProcess", VARIABLES);

    assertThat(getExecutedDecisionInstances()).isEqualTo(1L);
    assertThat(getDecisionInstances()).isEqualTo(1L);
    assertThat(getExecutedDecisionElements()).isEqualTo(16L);
    assertThat(getExecutedDecisionInstancesFromDmnEngine()).isEqualTo(1L);
    assertThat(getExecutedDecisionElementsFromDmnEngine()).isEqualTo(16L);

    processEngineConfiguration.getDbMetricsReporter().reportNow();

    assertThat(getExecutedDecisionInstances()).isEqualTo(1L);
    assertThat(getDecisionInstances()).isEqualTo(1L);
    assertThat(getExecutedDecisionElements()).isEqualTo(16L);
    assertThat(getExecutedDecisionInstancesFromDmnEngine()).isEqualTo(1L);
    assertThat(getExecutedDecisionElementsFromDmnEngine()).isEqualTo(16L);
  }

  @Test
  @Deployment(resources = DMN_DECISION_LITERAL_EXPRESSION)
  void shouldCountDecisionLiteralExpression() {
    // given

    // when
    decisionService
        .evaluateDecisionByKey(DECISION_DEFINITION_KEY)
        .variables(VARIABLES)
        .evaluate();

    // then
    assertThat(getExecutedDecisionInstances()).isEqualTo(1L);
    assertThat(getDecisionInstances()).isEqualTo(1L);

    processEngineConfiguration.getDbMetricsReporter().reportNow();

    assertThat(getExecutedDecisionInstances()).isEqualTo(1L);
    assertThat(getDecisionInstances()).isEqualTo(1L);
  }

  @Test
  @Deployment(resources = DRD_DISH_DECISION_TABLE)
  void shouldCountDecisionDRG() {
    // given

    // when
    decisionService
        .evaluateDecisionByKey("dish-decision")
        .variables(VARIABLES
                       .putValue("temperature", 32)
                       .putValue("dayType", "Weekend"))
        .evaluate();

    // then
    assertThat(getExecutedDecisionInstances()).isEqualTo(3L);
    assertThat(getDecisionInstances()).isEqualTo(3L);

    processEngineConfiguration.getDbMetricsReporter().reportNow();

    assertThat(getExecutedDecisionInstances()).isEqualTo(3L);
    assertThat(getDecisionInstances()).isEqualTo(3L);
  }

  protected long getExecutedDecisionInstances() {
    return managementService.createMetricsQuery()
        .name(Metrics.EXECUTED_DECISION_INSTANCES)
        .sum();
  }

  protected long getDecisionInstances() {
    return managementService.createMetricsQuery()
        .name(Metrics.DECISION_INSTANCES)
        .sum();
  }

  protected long getExecutedDecisionElements() {
    return managementService.createMetricsQuery()
        .name(Metrics.EXECUTED_DECISION_ELEMENTS)
        .sum();
  }

  protected long getExecutedDecisionInstancesFromDmnEngine() {
    return processEngineConfiguration.getDmnEngineConfiguration()
        .getEngineMetricCollector()
        .getExecutedDecisionInstances();
  }

  protected long getExecutedDecisionElementsFromDmnEngine() {
    return processEngineConfiguration.getDmnEngineConfiguration()
        .getEngineMetricCollector()
        .getExecutedDecisionElements();
  }

}
