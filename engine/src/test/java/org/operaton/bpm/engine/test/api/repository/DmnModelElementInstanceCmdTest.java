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
package org.operaton.bpm.engine.test.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.model.dmn.DmnModelInstance;
import org.operaton.bpm.model.dmn.instance.Decision;
import org.operaton.bpm.model.dmn.instance.DecisionTable;
import org.operaton.bpm.model.dmn.instance.Input;
import org.operaton.bpm.model.dmn.instance.Output;
import org.operaton.bpm.model.dmn.instance.Rule;

@ExtendWith(ProcessEngineExtension.class)
class DmnModelElementInstanceCmdTest {

  private static final String DECISION_KEY = "one";

  RepositoryService repositoryService;

  @Deployment(resources = "org/operaton/bpm/engine/test/repository/one.dmn")
  @Test
  void testRepositoryService() {
    String decisionDefinitionId = repositoryService
      .createDecisionDefinitionQuery()
      .decisionDefinitionKey(DECISION_KEY)
      .singleResult()
      .getId();

    DmnModelInstance modelInstance = repositoryService.getDmnModelInstance(decisionDefinitionId);
    assertThat(modelInstance).isNotNull();

    Collection<Decision> decisions = modelInstance.getModelElementsByType(Decision.class);
    assertThat(decisions).hasSize(1);

    Collection<DecisionTable> decisionTables = modelInstance.getModelElementsByType(DecisionTable.class);
    assertThat(decisionTables).hasSize(1);

    Collection<Input> inputs = modelInstance.getModelElementsByType(Input.class);
    assertThat(inputs).hasSize(1);

    Collection<Output> outputs = modelInstance.getModelElementsByType(Output.class);
    assertThat(outputs).hasSize(1);

    Collection<Rule> rules = modelInstance.getModelElementsByType(Rule.class);
    assertThat(rules).hasSize(2);
  }

}
