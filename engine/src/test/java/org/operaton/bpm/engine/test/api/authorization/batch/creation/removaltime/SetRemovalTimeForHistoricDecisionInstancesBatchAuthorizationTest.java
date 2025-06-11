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
package org.operaton.bpm.engine.test.api.authorization.batch.creation.removaltime;

import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario.scenario;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.grant;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.TestTemplate;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.authorization.BatchPermissions;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.history.HistoricDecisionInstanceQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.authorization.batch.creation.BatchCreationAuthorizationTest;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;
import org.operaton.bpm.engine.variable.Variables;

/**
 * @author Tassilo Weidner
 */
@Parameterized
public class SetRemovalTimeForHistoricDecisionInstancesBatchAuthorizationTest extends BatchCreationAuthorizationTest {

  @Parameters
  public static Collection<AuthorizationScenario[]> scenarios() {
    return AuthorizationTestExtension.asParameters(
        scenario()
            .withAuthorizations(
              grant(Resources.DECISION_DEFINITION, "dish-decision", "userId", Permissions.READ_HISTORY)
            )
            .failsDueToRequired(
                grant(Resources.BATCH, "batchId", "userId", Permissions.CREATE),
                grant(Resources.BATCH, "batchId", "userId", BatchPermissions.CREATE_BATCH_SET_REMOVAL_TIME)
            ),
        scenario()
            .withAuthorizations(
                grant(Resources.DECISION_DEFINITION, "dish-decision", "userId", Permissions.READ_HISTORY),
                grant(Resources.BATCH, "batchId", "userId", Permissions.CREATE)
            ),
        scenario()
            .withAuthorizations(
                grant(Resources.DECISION_DEFINITION, "dish-decision", "userId", Permissions.READ_HISTORY),
                grant(Resources.BATCH, "batchId", "userId", BatchPermissions.CREATE_BATCH_SET_REMOVAL_TIME)
            ).succeeds()
    );
  }

  @TestTemplate
  @Deployment(resources = {
    "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldAuthorizeSetRemovalTimeForHistoricDecisionInstancesBatch() {
    // given
    setupHistory();

    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("batchId", "*")
        .start();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    // when
    historyService.setRemovalTimeToHistoricDecisionInstances()
      .absoluteRemovalTime(new Date())
      .byQuery(query)
      .executeAsync();

    // then
    authRule.assertScenario(scenario);
  }

  @Override
  protected List<String> setupHistory() {
    engineRule.getDecisionService()
      .evaluateDecisionTableByKey("dish-decision", Variables.createVariables()
        .putValue("temperature", 32)
        .putValue("dayType", "Weekend"));

    return null;
  }

}
