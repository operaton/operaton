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
package org.operaton.bpm.engine.test.api.authorization.batch.creation;

import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario.scenario;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.grant;

import java.util.Collection;

import org.junit.jupiter.api.TestTemplate;
import org.operaton.bpm.engine.authorization.BatchPermissions;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.ProcessDefinitionPermissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;

@Parameterized
public class CorrelateAllMessageBatchAuthorizationTest extends BatchCreationAuthorizationTest {

  @Parameters
  public static Collection<AuthorizationScenario[]> scenarios() {
    return AuthorizationTestExtension.asParameters(
        scenario()
            .withAuthorizations(
              grant(Resources.PROCESS_DEFINITION, "processDefinitionKey", "userId",
                  ProcessDefinitionPermissions.READ_INSTANCE)
            )
            .failsDueToRequired(
                grant(Resources.BATCH, "batchId", "userId", Permissions.CREATE),
                grant(Resources.BATCH, "batchId", "userId",
                    BatchPermissions.CREATE_BATCH_CORRELATE_MESSAGE)
            ),
        scenario()
            .withAuthorizations(
                grant(Resources.PROCESS_DEFINITION, "processDefinitionKey", "userId",
                    ProcessDefinitionPermissions.READ_INSTANCE),
                grant(Resources.BATCH, "batchId", "userId", Permissions.CREATE)
            ).succeeds(),
        scenario()
            .withAuthorizations(
                grant(Resources.PROCESS_DEFINITION, "processDefinitionKey", "userId",
                    ProcessDefinitionPermissions.READ_INSTANCE),
                grant(Resources.BATCH, "batchId", "userId",
                    BatchPermissions.CREATE_BATCH_CORRELATE_MESSAGE)
            ).succeeds()
    );
  }

  @TestTemplate
  void shouldAuthorizeSetVariablesBatch() {
    // given
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("batchId", "*")
        .bindResource("processDefinitionKey", ProcessModels.PROCESS_KEY)
        .start();

    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery();

    // when
    runtimeService.createMessageCorrelationAsync("test")
      .processInstanceQuery(processInstanceQuery)
      .setVariable("foo", "bar")
      .correlateAllAsync();

    // then
    authRule.assertScenario(scenario);
  }

}
