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
package org.operaton.bpm.engine.test.api.multitenancy.query.history;
import java.util.List;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.history.HistoricDecisionInstanceStatisticsQuery;
import org.operaton.bpm.engine.repository.DecisionRequirementsDefinition;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class MultiTenancyHistoricDecisionInstanceStatisticsQueryTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String DISH_DRG_DMN = "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml";

  protected static final String DISH_DECISION = "dish-decision";
  protected static final String TEMPERATURE = "temperature";
  protected static final String DAY_TYPE = "dayType";
  protected static final String WEEKEND = "Weekend";
  protected static final String USER_ID = "user";

  protected DecisionService decisionService;
  protected RepositoryService repositoryService;
  protected HistoryService historyService;
  protected IdentityService identityService;

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  @BeforeEach
  void setUp() {
    testRule.deployForTenant(TENANT_ONE, DISH_DRG_DMN);

    decisionService.evaluateDecisionByKey(DISH_DECISION)
        .decisionDefinitionTenantId(TENANT_ONE)
        .variables(Variables.createVariables().putValue(TEMPERATURE, 21).putValue(DAY_TYPE, WEEKEND))
        .evaluate();

  }

  @Test
  void testQueryNoAuthenticatedTenants() {
    DecisionRequirementsDefinition decisionRequirementsDefinition =
        repositoryService.createDecisionRequirementsDefinitionQuery()
            .tenantIdIn(TENANT_ONE)
            .singleResult();

    identityService.setAuthentication(USER_ID, null, null);

    HistoricDecisionInstanceStatisticsQuery query = historyService.
        createHistoricDecisionInstanceStatisticsQuery(decisionRequirementsDefinition.getId());

    assertThat(query.count()).isZero();
  }

  @Test
  void testQueryAuthenticatedTenant() {
    DecisionRequirementsDefinition decisionRequirementsDefinition =
        repositoryService.createDecisionRequirementsDefinitionQuery()
            .tenantIdIn(TENANT_ONE)
            .singleResult();

    identityService.setAuthentication(USER_ID, null, List.of(TENANT_ONE));

    HistoricDecisionInstanceStatisticsQuery query = historyService.
        createHistoricDecisionInstanceStatisticsQuery(decisionRequirementsDefinition.getId());

    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void testQueryDisabledTenantCheck() {
    DecisionRequirementsDefinition decisionRequirementsDefinition =
        repositoryService.createDecisionRequirementsDefinitionQuery()
            .tenantIdIn(TENANT_ONE)
            .singleResult();

    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    identityService.setAuthentication(USER_ID, null, null);

    HistoricDecisionInstanceStatisticsQuery query = historyService.
        createHistoricDecisionInstanceStatisticsQuery(decisionRequirementsDefinition.getId());

    assertThat(query.count()).isEqualTo(3L);
  }

}
