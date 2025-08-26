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
package org.operaton.bpm.engine.test.api.authorization.dmn;

import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.repository.DecisionDefinitionQuery;
import org.operaton.bpm.engine.test.api.authorization.AuthorizationTest;
import org.operaton.bpm.model.dmn.DmnModelInstance;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.Resources.DECISION_DEFINITION;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Philipp Ossler
 */
class DecisionDefinitionAuthorizationTest extends AuthorizationTest {

  protected static final String PROCESS_KEY = "testProcess";
  protected static final String DECISION_DEFINITION_KEY = "sampleDecision";

  @Override
  @BeforeEach
  public void setUp() {
    testRule.deploy(
        "org/operaton/bpm/engine/test/api/authorization/singleDecision.dmn11.xml",
        "org/operaton/bpm/engine/test/api/authorization/anotherDecision.dmn11.xml");
    super.setUp();
  }

  @Test
  void testQueryWithoutAuthorization() {
    // given user is not authorized to read any decision definition

    // when
    DecisionDefinitionQuery query = repositoryService.createDecisionDefinitionQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQueryWithReadPermissionOnAnyDecisionDefinition() {
    // given user gets read permission on any decision definition
    createGrantAuthorization(DECISION_DEFINITION, ANY, userId, READ);

    // when
    DecisionDefinitionQuery query = repositoryService.createDecisionDefinitionQuery();

    // then
    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryWithReadPermissionOnOneDecisionDefinition() {
    // given user gets read permission on the decision definition
    createGrantAuthorization(DECISION_DEFINITION, DECISION_DEFINITION_KEY, userId, READ);

    // when
    DecisionDefinitionQuery query = repositoryService.createDecisionDefinitionQuery();

    // then
    verifyQueryResults(query, 1);

    DecisionDefinition definition = query.singleResult();
    assertThat(definition).isNotNull();
    assertThat(definition.getKey()).isEqualTo(DECISION_DEFINITION_KEY);
  }

  @Test
  void testQueryWithMultiple() {
    createGrantAuthorization(DECISION_DEFINITION, DECISION_DEFINITION_KEY, userId, READ);
    createGrantAuthorization(DECISION_DEFINITION, ANY, userId, READ);

    // when
    DecisionDefinitionQuery query = repositoryService.createDecisionDefinitionQuery();

    // then
    verifyQueryResults(query, 2);
  }

  @Test
  void shouldNotFindDefinitionWithRevokedReadPermissionOnDefinition() {
    createGrantAuthorization(DECISION_DEFINITION, ANY, ANY, READ);
    createRevokeAuthorization(DECISION_DEFINITION, ANY, userId, READ);

    // when
    DecisionDefinitionQuery query = repositoryService.createDecisionDefinitionQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testGetDecisionDefinitionWithoutAuthorizations() {
    String decisionDefinitionId = selectDecisionDefinitionByKey(DECISION_DEFINITION_KEY).getId();

    assertThatThrownBy(() -> repositoryService.getDecisionDefinition(decisionDefinitionId))
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ.getName())
        .hasMessageContaining(DECISION_DEFINITION_KEY)
        .hasMessageContaining(DECISION_DEFINITION.resourceName());
  }

  @Test
  void testGetDecisionDefinition() {
    // given
    String decisionDefinitionId = selectDecisionDefinitionByKey(DECISION_DEFINITION_KEY).getId();
    createGrantAuthorization(DECISION_DEFINITION, DECISION_DEFINITION_KEY, userId, READ);

    // when
    DecisionDefinition decisionDefinition = repositoryService.getDecisionDefinition(decisionDefinitionId);

    // then
    assertThat(decisionDefinition).isNotNull();
  }

  @Test
  void testGetDecisionDiagramWithoutAuthorizations() {
    String decisionDefinitionId = selectDecisionDefinitionByKey(DECISION_DEFINITION_KEY).getId();

    assertThatThrownBy(() -> repositoryService.getDecisionDiagram(decisionDefinitionId))
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ.getName())
        .hasMessageContaining(DECISION_DEFINITION_KEY)
        .hasMessageContaining(DECISION_DEFINITION.resourceName());
  }

  @Test
  void testGetDecisionDiagram() {
    // given
    String decisionDefinitionId = selectDecisionDefinitionByKey(DECISION_DEFINITION_KEY).getId();
    createGrantAuthorization(DECISION_DEFINITION, DECISION_DEFINITION_KEY, userId, READ);

    // when
    InputStream stream = repositoryService.getDecisionDiagram(decisionDefinitionId);

    // then
    // no decision diagram deployed
    assertThat(stream).isNull();
  }

  @Test
  void testGetDecisionModelWithoutAuthorizations() {
    String decisionDefinitionId = selectDecisionDefinitionByKey(DECISION_DEFINITION_KEY).getId();

    assertThatThrownBy(() -> repositoryService.getDecisionModel(decisionDefinitionId))
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ.getName())
        .hasMessageContaining(DECISION_DEFINITION_KEY)
        .hasMessageContaining(DECISION_DEFINITION.resourceName());
  }

  @Test
  void testGetDecisionModel() {
    // given
    String decisionDefinitionId = selectDecisionDefinitionByKey(DECISION_DEFINITION_KEY).getId();
    createGrantAuthorization(DECISION_DEFINITION, DECISION_DEFINITION_KEY, userId, READ);

    // when
    InputStream stream = repositoryService.getDecisionModel(decisionDefinitionId);

    // then
    assertThat(stream).isNotNull();
  }

  @Test
  void testGetDmnModelInstanceWithoutAuthorizations() {
    String decisionDefinitionId = selectDecisionDefinitionByKey(DECISION_DEFINITION_KEY).getId();

    assertThatThrownBy(() -> repositoryService.getDmnModelInstance(decisionDefinitionId))
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ.getName())
        .hasMessageContaining(DECISION_DEFINITION_KEY)
        .hasMessageContaining(DECISION_DEFINITION.resourceName());
  }

  @Test
  void testGetDmnModelInstance() {
    // given
    String decisionDefinitionId = selectDecisionDefinitionByKey(DECISION_DEFINITION_KEY).getId();
    createGrantAuthorization(DECISION_DEFINITION, DECISION_DEFINITION_KEY, userId, READ);

    // when
    DmnModelInstance modelInstance = repositoryService.getDmnModelInstance(decisionDefinitionId);

    // then
    assertThat(modelInstance).isNotNull();
  }

  @Test
  void testDecisionDefinitionUpdateTimeToLive() {
    //given
    String decisionDefinitionId = selectDecisionDefinitionByKey(DECISION_DEFINITION_KEY).getId();
    createGrantAuthorization(DECISION_DEFINITION, DECISION_DEFINITION_KEY, userId, UPDATE);

    //when
    repositoryService.updateDecisionDefinitionHistoryTimeToLive(decisionDefinitionId, 6);

    //then
    assertThat(selectDecisionDefinitionByKey(DECISION_DEFINITION_KEY).getHistoryTimeToLive().intValue()).isEqualTo(6);

  }

  @Test
  void testDecisionDefinitionUpdateTimeToLiveWithoutAuthorizations() {
    String decisionDefinitionId = selectDecisionDefinitionByKey(DECISION_DEFINITION_KEY).getId();

    assertThatThrownBy(() -> repositoryService.updateDecisionDefinitionHistoryTimeToLive(decisionDefinitionId, 4))
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(UPDATE.getName())
        .hasMessageContaining(DECISION_DEFINITION_KEY)
        .hasMessageContaining(DECISION_DEFINITION.resourceName());
  }

}
