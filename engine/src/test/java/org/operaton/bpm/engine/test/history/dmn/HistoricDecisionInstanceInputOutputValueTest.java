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
package org.operaton.bpm.engine.test.history.dmn;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.history.HistoricDecisionInputInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricDecisionOutputInstance;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.variables.JavaSerializable;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.util.ResetDmnConfigUtil;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;

@Parameterized
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class HistoricDecisionInstanceInputOutputValueTest {

  protected static final String DECISION_PROCESS = "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.processWithBusinessRuleTask.bpmn20.xml";
  protected static final String DECISION_SINGLE_OUTPUT_DMN = "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.decisionSingleOutput.dmn11.xml";

  @Parameters(name = "input({0}) = {1}")
  public static Collection<Object[]> data() {
    return List.of(new Object[][] {
      { "string", "a" },
      { "long", 1L },
      { "double", 2.5 },
      { "bytes", "object".getBytes() },
      { "object", new JavaSerializable("foo") },
      { "object", Collections.singletonList(new JavaSerializable("bar")) }
    });
  }

  @Parameter(0)
  public String valueType;

  @Parameter(1)
  public Object inputValue;

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();

  @AfterEach
  void tearDown() {
    ClockUtil.setCurrentTime(new Date());
  }

  @BeforeEach
  void enableDmnFeelLegacyBehavior() {
    DefaultDmnEngineConfiguration dmnEngineConfiguration =
        engineRule.getProcessEngineConfiguration()
            .getDmnEngineConfiguration();

    ResetDmnConfigUtil.reset(dmnEngineConfiguration)
        .enableFeelLegacyBehavior(true)
        .init();
  }

  @AfterEach
  void disableDmnFeelLegacyBehavior() {

    DefaultDmnEngineConfiguration dmnEngineConfiguration =
        engineRule.getProcessEngineConfiguration()
            .getDmnEngineConfiguration();

    ResetDmnConfigUtil.reset(dmnEngineConfiguration)
        .enableFeelLegacyBehavior(false)
        .init();
  }

  @TestTemplate
  @Deployment(resources = { DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN })
  void decisionInputInstanceValue() throws Exception {
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
    Date fixedDate = sdf.parse("01/01/2001 01:01:01.000");
    ClockUtil.setCurrentTime(fixedDate);

    startProcessInstanceAndEvaluateDecision(inputValue);

    HistoricDecisionInstance historicDecisionInstance = engineRule.getHistoryService().createHistoricDecisionInstanceQuery().includeInputs().singleResult();
    List<HistoricDecisionInputInstance> inputInstances = historicDecisionInstance.getInputs();
    assertThat(inputInstances).hasSize(1);

    HistoricDecisionInputInstance inputInstance = inputInstances.get(0);
    assertThat(inputInstance.getTypeName()).isEqualTo(valueType);
    assertThat(inputInstance.getValue()).isEqualTo(inputValue);
    assertThat(inputInstance.getCreateTime()).isEqualTo(fixedDate);
  }

  @TestTemplate
  @Deployment(resources = { DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN })
  void decisionOutputInstanceValue() throws Exception {
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
    Date fixedDate = sdf.parse("01/01/2001 01:01:01.000");
    ClockUtil.setCurrentTime(fixedDate);

    startProcessInstanceAndEvaluateDecision(inputValue);

    HistoricDecisionInstance historicDecisionInstance = engineRule.getHistoryService().createHistoricDecisionInstanceQuery().includeOutputs().singleResult();
    List<HistoricDecisionOutputInstance> outputInstances = historicDecisionInstance.getOutputs();
    assertThat(outputInstances).hasSize(1);

    HistoricDecisionOutputInstance outputInstance = outputInstances.get(0);
    assertThat(outputInstance.getTypeName()).isEqualTo(valueType);
    assertThat(outputInstance.getValue()).isEqualTo(inputValue);
    assertThat(outputInstance.getCreateTime()).isEqualTo(fixedDate);
  }

  protected ProcessInstance startProcessInstanceAndEvaluateDecision(Object input) {
    return engineRule.getRuntimeService().startProcessInstanceByKey("testProcess",
        Variables.createVariables().putValue("input1", input));
  }

}
