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
package org.operaton.bpm.integrationtest.functional.dmn;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;

/**
 * @author Philipp Ossler
 */
@ExtendWith(ArquillianExtension.class)
public class DmnHistoryTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive processArchive() {

    return initWebArchiveDeployment()
        .addAsResource("org/operaton/bpm/integrationtest/functional/dmn/BusinessRuleProcess.bpmn20.xml", "BusinessRuleProcess.bpmn20.xml")
        .addAsResource("org/operaton/bpm/integrationtest/functional/dmn/Example.dmn11.xml", "Example.dmn11.xml");

  }

  @Test
  public void testHistoricDecisionInstance() {

    VariableMap variables = Variables.createVariables().putValue("status", "bronze").putValue("sum", 100);
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess", variables);

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery().includeInputs().includeOutputs().singleResult();
    assertThat(historicDecisionInstance).isNotNull();
    assertThat(historicDecisionInstance.getDecisionDefinitionKey()).isEqualTo("decision");
    assertThat(historicDecisionInstance.getDecisionDefinitionName()).isEqualTo("Check Order");

    assertThat(historicDecisionInstance.getInputs()).hasSize(2);
    assertThat(historicDecisionInstance.getOutputs()).hasSize(2);

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    taskService.complete(task.getId());
  }

}
