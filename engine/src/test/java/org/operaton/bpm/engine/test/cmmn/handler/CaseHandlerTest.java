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
package org.operaton.bpm.engine.test.cmmn.handler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.cmmn.behavior.CmmnActivityBehavior;
import org.operaton.bpm.engine.impl.cmmn.entity.repository.CaseDefinitionEntity;
import org.operaton.bpm.engine.impl.cmmn.handler.CaseHandler;
import org.operaton.bpm.engine.impl.cmmn.handler.CmmnHandlerContext;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.persistence.entity.DeploymentEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
class CaseHandlerTest extends CmmnElementHandlerTest {

  protected CaseHandler handler = new CaseHandler();
  protected CmmnHandlerContext context;

  @BeforeEach
  void setUp() {
    context = new CmmnHandlerContext();

    DeploymentEntity deployment = new DeploymentEntity();
    deployment.setId("aDeploymentId");

    context.setDeployment(deployment);
    context.setModel(modelInstance);

    Context.setProcessEngineConfiguration(new StandaloneInMemProcessEngineConfiguration().setEnforceHistoryTimeToLive(false));
  }

  @AfterEach
  void tearDown() {
    Context.removeProcessEngineConfiguration();
  }

  @Test
  void testCaseActivityName() {
    // given:
    // the case has a name "A Case"
    String name = "A Case";
    caseDefinition.setName(name);

    // when
    CmmnActivity activity = handler.handleElement(caseDefinition, context);

    // then
    assertThat(activity.getName()).isEqualTo(name);
  }

  @Test
  void testActivityBehavior() {
    // given: a case

    // when
    CmmnActivity activity = handler.handleElement(caseDefinition, context);

    // then
    CmmnActivityBehavior behavior = activity.getActivityBehavior();
    assertThat(behavior).isNull();
  }

  @Test
  void testCaseHasNoParent() {
    // given: a caseDefinition

    // when
    CmmnActivity activity = handler.handleElement(caseDefinition, context);

    // then
    assertThat(activity.getParent()).isNull();
  }

  @Test
  void testCaseDefinitionKey() {
    // given: a caseDefinition

    // when
    CaseDefinitionEntity activity = (CaseDefinitionEntity) handler.handleElement(caseDefinition, context);

    // then
    assertThat(activity.getKey()).isEqualTo(caseDefinition.getId());
  }

  @Test
  void testDeploymentId() {
    // given: a caseDefinition

    // when
    CaseDefinitionEntity activity = (CaseDefinitionEntity) handler.handleElement(caseDefinition, context);

    // then
    String deploymentId = context.getDeployment().getId();
    assertThat(activity.getDeploymentId()).isEqualTo(deploymentId);
  }

  @Test
  void testHistoryTimeToLiveNull() {
    // given: a caseDefinition

    // when
    CaseDefinitionEntity activity = (CaseDefinitionEntity) handler.handleElement(caseDefinition, context);

    // then
    assertThat(activity.getHistoryTimeToLive()).isNull();
  }

  @Test
  void testHistoryTimeToLive() {
    // given: a caseDefinition
    Integer historyTimeToLive = 6;
    caseDefinition.setOperatonHistoryTimeToLiveString(historyTimeToLive.toString());

    // when
    CaseDefinitionEntity activity = (CaseDefinitionEntity) handler.handleElement(caseDefinition, context);

    // then
    assertThat(activity.getHistoryTimeToLive()).isEqualTo(historyTimeToLive);
    assertThat(caseDefinition.getOperatonHistoryTimeToLiveString()).isEqualTo("6");
  }

  @Test
  void testHistoryTimeToLiveNegative() {
    // given: a caseDefinition
    Integer historyTimeToLive = -6;
    caseDefinition.setOperatonHistoryTimeToLiveString(historyTimeToLive.toString());

    // when/then
    assertThatThrownBy(() -> handler.handleElement(caseDefinition, context))
      .isInstanceOf(NotValidException.class)
      .hasMessageContaining("negative value is not allowed");
  }

}
