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

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.management.Metrics;
import org.operaton.bpm.engine.management.MetricsQuery;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.CallActivity;

import static org.assertj.core.api.Assertions.assertThat;

public class RootProcessInstanceMetricsTest extends AbstractMetricsTest {

  public static final String DMN_FILE
      = "org/operaton/bpm/engine/test/api/mgmt/metrics/ExecutedDecisionElementsTest.dmn11.xml";
  public static final VariableMap VARIABLES = Variables.createVariables()
      .putValue("status", "")
      .putValue("sum", 100);

  protected static final String BASE_INSTANCE_KEY = "baseProcess";
  protected static final BpmnModelInstance BASE_INSTANCE = Bpmn.createExecutableProcess(BASE_INSTANCE_KEY)
      .operatonHistoryTimeToLive(180)
      .startEvent()
      .endEvent()
      .done();

  protected static final String CALLED_DMN_INSTANCE_KEY = "calledDMNProcess";
  protected static final BpmnModelInstance CALLED_DMN_INSTANCE = Bpmn.createExecutableProcess(CALLED_DMN_INSTANCE_KEY)
      .operatonHistoryTimeToLive(180)
      .startEvent()
      .businessRuleTask()
        .operatonDecisionRef("decision")
      .endEvent()
      .done();

  protected static final String CALLING_INSTANCE_KEY = "callingProcess";

  @Override
  protected void clearMetrics() {
    super.clearMetrics();
    processEngineConfiguration.getDmnEngineConfiguration()
        .getEngineMetricCollector()
        .clearExecutedDecisionElements();
  }

  @Test
  void shouldCountOneRootProcessInstance() {
    // given
    testRule.deploy(BASE_INSTANCE);

    // when
    runtimeService.startProcessInstanceByKey(BASE_INSTANCE_KEY);

    // then
    MetricsQuery query = managementService.createMetricsQuery();
    assertThat(query.name(Metrics.ROOT_PROCESS_INSTANCE_START).sum()).isEqualTo(1l);
    assertThat(query.name(Metrics.PROCESS_INSTANCES).sum()).isEqualTo(1l);

    // and force the db metrics reporter to report
    processEngineConfiguration.getDbMetricsReporter().reportNow();

    // still 1
    assertThat(query.name(Metrics.ROOT_PROCESS_INSTANCE_START).sum()).isEqualTo(1l);
    assertThat(query.name(Metrics.PROCESS_INSTANCES).sum()).isEqualTo(1l);
  }

  @Test
  void shouldCountRootProcessInstanceWithCallActivities() {
    // given
    BpmnModelInstance callingInstance = getCallingInstance(BASE_INSTANCE_KEY, Collections.EMPTY_MAP);
    testRule.deploy(BASE_INSTANCE, callingInstance);

    // when
    runtimeService.startProcessInstanceByKey(CALLING_INSTANCE_KEY);

    // then
    MetricsQuery query = managementService.createMetricsQuery();
    assertThat(query.name(Metrics.ROOT_PROCESS_INSTANCE_START).sum()).isEqualTo(1l);
    assertThat(query.name(Metrics.PROCESS_INSTANCES).sum()).isEqualTo(1l);

    // and force the db metrics reporter to report
    processEngineConfiguration.getDbMetricsReporter().reportNow();

    // still 1
    assertThat(query.name(Metrics.ROOT_PROCESS_INSTANCE_START).sum()).isEqualTo(1l);
    assertThat(query.name(Metrics.PROCESS_INSTANCES).sum()).isEqualTo(1l);
  }

  @Test
  void shouldCountRootProcessInstanceAndDecisionInstanceWithBusinessRuleTask() {
    // given
    BpmnModelInstance callingInstance = getCallingInstance(CALLED_DMN_INSTANCE_KEY, VARIABLES);
    testRule.deploy(repositoryService.createDeployment()
                        .addClasspathResource(DMN_FILE)
                        .addModelInstance("calledProcess.bpmn", CALLED_DMN_INSTANCE)
                        .addModelInstance("callingProcess.bpmn", callingInstance));

    // when
    runtimeService.startProcessInstanceByKey(CALLING_INSTANCE_KEY, VARIABLES);

    // then
    MetricsQuery query = managementService.createMetricsQuery();
    assertThat(query.name(Metrics.ROOT_PROCESS_INSTANCE_START).sum()).isEqualTo(1l);
    assertThat(query.name(Metrics.PROCESS_INSTANCES).sum()).isEqualTo(1l);
    assertThat(query.name(Metrics.EXECUTED_DECISION_INSTANCES).sum()).isEqualTo(1l);

    // and force the db metrics reporter to report
    processEngineConfiguration.getDbMetricsReporter().reportNow();

    // still 1
    assertThat(query.name(Metrics.ROOT_PROCESS_INSTANCE_START).sum()).isEqualTo(1l);
    assertThat(query.name(Metrics.PROCESS_INSTANCES).sum()).isEqualTo(1l);
    assertThat(query.name(Metrics.EXECUTED_DECISION_INSTANCES).sum()).isEqualTo(1l);
  }

  protected BpmnModelInstance getCallingInstance(String calledInstanceKey, Map variables) {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CALLING_INSTANCE_KEY)
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .callActivity("calledProcess")
          .calledElement(calledInstanceKey)
        .endEvent()
        .done();

    // pass any variables to the call activity
    CallActivity callActivity = modelInstance.getModelElementById("calledProcess");
    variables.keySet()
        .iterator()
        .forEachRemaining(name -> callActivity.builder().operatonIn((String) name, (String) name));

    return modelInstance;
  }
}
