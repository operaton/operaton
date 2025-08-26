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
package org.operaton.bpm.engine.test.cmmn.sentry;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseSentryPartEntity;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseSentryPartQueryImpl;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnSentryDeclaration;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;
import org.operaton.bpm.model.cmmn.VariableTransition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Smirnov
 *
 */
class SentryInitializationTest extends CmmnTest {

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryInitializationTest.testOnPart.cmmn"})
  @Test
  void testOnPart() {
    // given
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // when
    String caseInstanceId = caseService
        .withCaseDefinition(caseDefinitionId)
        .create()
        .getId();

    // then
    List<CaseSentryPartEntity> parts = createCaseSentryPartQuery()
      .list();

    assertThat(parts).hasSize(1);

    CaseSentryPartEntity part = parts.get(0);

    assertThat(part.getCaseExecutionId()).isEqualTo(caseInstanceId);
    assertThat(part.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(part.getSentryId()).isEqualTo("Sentry_1");
    assertThat(part.getType()).isEqualTo(CmmnSentryDeclaration.PLAN_ITEM_ON_PART);
    assertThat(part.getSource()).isEqualTo("PI_HumanTask_1");
    assertThat(part.getStandardEvent()).isEqualTo("complete");
    assertThat(part.isSatisfied()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryInitializationTest.testVariableOnPart.cmmn"})
  @Test
  void testVariableOnPart() {
    // given
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // when
    String caseInstanceId = caseService
        .withCaseDefinition(caseDefinitionId)
        .create()
        .getId();

    // then
    List<CaseSentryPartEntity> parts = createCaseSentryPartQuery()
      .list();

    assertThat(parts).hasSize(1);

    CaseSentryPartEntity part = parts.get(0);

    assertThat(part.getCaseExecutionId()).isEqualTo(caseInstanceId);
    assertThat(part.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(part.getSentryId()).isEqualTo("Sentry_1");
    assertThat(part.getType()).isEqualTo(CmmnSentryDeclaration.VARIABLE_ON_PART);
    assertThat(part.getVariableEvent()).isEqualTo(VariableTransition.create.name());
    assertThat(part.getVariableName()).isEqualTo("variable_1");
    assertThat(part.isSatisfied()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryInitializationTest.testIfPart.cmmn"})
  @Test
  void testIfPart() {
    // given
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // when
    String caseInstanceId = caseService
        .withCaseDefinition(caseDefinitionId)
        .setVariable("myVar", 0)
        .create()
        .getId();

    // then
    List<CaseSentryPartEntity> parts = createCaseSentryPartQuery()
      .list();

    assertThat(parts).hasSize(1);

    CaseSentryPartEntity part = parts.get(0);

    assertThat(part.getCaseExecutionId()).isEqualTo(caseInstanceId);
    assertThat(part.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(part.getSentryId()).isEqualTo("Sentry_1");
    assertThat(part.getType()).isEqualTo(CmmnSentryDeclaration.IF_PART);
    assertThat(part.getSource()).isNull();
    assertThat(part.getStandardEvent()).isNull();
    assertThat(part.isSatisfied()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryInitializationTest.testOnPartIfPartAndVariableOnPart.cmmn"})
  @Test
  void testOnPartIfPartAndVariableOnPart() {
    // given
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // when
    String caseInstanceId = caseService
        .withCaseDefinition(caseDefinitionId)
        .create()
        .getId();

    // then
    CaseSentryPartQueryImpl query = createCaseSentryPartQuery();

    assertThat(query.count()).isEqualTo(3);

    CaseSentryPartEntity part = query
        .type(CmmnSentryDeclaration.IF_PART)
        .singleResult();

    assertThat(part.getCaseExecutionId()).isEqualTo(caseInstanceId);
    assertThat(part.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(part.getSentryId()).isEqualTo("Sentry_1");
    assertThat(part.getType()).isEqualTo(CmmnSentryDeclaration.IF_PART);
    assertThat(part.getSource()).isNull();
    assertThat(part.getStandardEvent()).isNull();
    assertThat(part.isSatisfied()).isFalse();

    part = query
        .type(CmmnSentryDeclaration.PLAN_ITEM_ON_PART)
        .singleResult();

    assertThat(part.getCaseExecutionId()).isEqualTo(caseInstanceId);
    assertThat(part.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(part.getSentryId()).isEqualTo("Sentry_1");
    assertThat(part.getType()).isEqualTo(CmmnSentryDeclaration.PLAN_ITEM_ON_PART);
    assertThat(part.getSource()).isEqualTo("PI_HumanTask_1");
    assertThat(part.getStandardEvent()).isEqualTo("complete");
    assertThat(part.isSatisfied()).isFalse();

    part = query.type(CmmnSentryDeclaration.VARIABLE_ON_PART).singleResult();

    assertThat(part.getCaseExecutionId()).isEqualTo(caseInstanceId);
    assertThat(part.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(part.getSentryId()).isEqualTo("Sentry_1");
    assertThat(part.getType()).isEqualTo(CmmnSentryDeclaration.VARIABLE_ON_PART);
    assertThat(part.getVariableEvent()).isEqualTo(VariableTransition.delete.name());
    assertThat(part.getVariableName()).isEqualTo("variable_1");
    assertThat(part.isSatisfied()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryInitializationTest.testMultipleSentries.cmmn"})
  @Test
  void testMultipleSentries() {
    // given
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // when
    String caseInstanceId = caseService
        .withCaseDefinition(caseDefinitionId)
        .setVariable("myVar", 0)
        .create()
        .getId();

    // then
    CaseSentryPartQueryImpl query = createCaseSentryPartQuery();

    assertThat(query.count()).isEqualTo(2);

    CaseSentryPartEntity part = query
        .sentryId("Sentry_1")
        .singleResult();

    assertThat(part.getCaseExecutionId()).isEqualTo(caseInstanceId);
    assertThat(part.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(part.getSentryId()).isEqualTo("Sentry_1");
    assertThat(part.getType()).isEqualTo(CmmnSentryDeclaration.IF_PART);
    assertThat(part.getSource()).isNull();
    assertThat(part.getStandardEvent()).isNull();
    assertThat(part.isSatisfied()).isFalse();

    part = query
        .sentryId("Sentry_2")
        .singleResult();

    assertThat(part.getCaseExecutionId()).isEqualTo(caseInstanceId);
    assertThat(part.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(part.getSentryId()).isEqualTo("Sentry_2");
    assertThat(part.getType()).isEqualTo(CmmnSentryDeclaration.PLAN_ITEM_ON_PART);
    assertThat(part.getSource()).isEqualTo("PI_HumanTask_1");
    assertThat(part.getStandardEvent()).isEqualTo("complete");
    assertThat(part.isSatisfied()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryInitializationTest.testMultipleSentriesWithinStage.cmmn"})
  @Test
  void testMultipleSentriesWithinStage() {
    // given
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // when
    String caseInstanceId = caseService
        .withCaseDefinition(caseDefinitionId)
        .setVariable("myVar", 0)
        .create()
        .getId();

    // then
    CaseSentryPartQueryImpl query = createCaseSentryPartQuery();

    assertThat(query.count()).isEqualTo(2);

    // when
    String stageId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Stage_1")
        .singleResult()
        .getId();

    // then
    assertThat(query.count()).isEqualTo(2);

    CaseSentryPartEntity part = query
        .sentryId("Sentry_1")
        .singleResult();

    assertThat(part.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(part.getCaseExecutionId()).isEqualTo(stageId);
    assertThat(part.getSentryId()).isEqualTo("Sentry_1");
    assertThat(part.getType()).isEqualTo(CmmnSentryDeclaration.IF_PART);
    assertThat(part.getSource()).isNull();
    assertThat(part.getStandardEvent()).isNull();
    assertThat(part.isSatisfied()).isFalse();

    part = query
        .sentryId("Sentry_2")
        .singleResult();

    assertThat(part.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(part.getCaseExecutionId()).isEqualTo(stageId);
    assertThat(part.getSentryId()).isEqualTo("Sentry_2");
    assertThat(part.getType()).isEqualTo(CmmnSentryDeclaration.PLAN_ITEM_ON_PART);
    assertThat(part.getSource()).isEqualTo("PI_HumanTask_1");
    assertThat(part.getStandardEvent()).isEqualTo("complete");
    assertThat(part.isSatisfied()).isFalse();
  }

}
