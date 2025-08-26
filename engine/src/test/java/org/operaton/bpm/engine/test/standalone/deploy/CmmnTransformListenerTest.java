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
package org.operaton.bpm.engine.test.standalone.deploy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.model.cmmn.instance.Case;
import org.operaton.bpm.model.cmmn.instance.CasePlanModel;
import org.operaton.bpm.model.cmmn.instance.CaseTask;
import org.operaton.bpm.model.cmmn.instance.DecisionTask;
import org.operaton.bpm.model.cmmn.instance.Definitions;
import org.operaton.bpm.model.cmmn.instance.EventListener;
import org.operaton.bpm.model.cmmn.instance.HumanTask;
import org.operaton.bpm.model.cmmn.instance.Milestone;
import org.operaton.bpm.model.cmmn.instance.ProcessTask;
import org.operaton.bpm.model.cmmn.instance.Sentry;
import org.operaton.bpm.model.cmmn.instance.Stage;
import org.operaton.bpm.model.cmmn.instance.Task;

import static org.operaton.bpm.engine.test.standalone.deploy.TestCmmnTransformListener.numberOfRegistered;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastian Menski
 */
class CmmnTransformListenerTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .closeEngineAfterAllTests()
    .configurationResource("org/operaton/bpm/engine/test/standalone/deploy/cmmn.transform.listener.operaton.cfg.xml")
    .build();

  RepositoryService repositoryService;

  @AfterEach
  void tearDown() {
    TestCmmnTransformListener.reset();
  }

  @Deployment
  @Test
  void testListenerInvocation() {
    // Check if case definition has different key
    assertThat(repositoryService.createCaseDefinitionQuery().caseDefinitionKey("testCase").count()).isZero();
    assertThat(repositoryService.createCaseDefinitionQuery().caseDefinitionKey("testCase-modified").count()).isZero();
    assertThat(repositoryService.createCaseDefinitionQuery().caseDefinitionKey("testCase-modified-modified").count()).isEqualTo(1);

    assertThat(numberOfRegistered(Definitions.class)).isEqualTo(1);
    assertThat(numberOfRegistered(Case.class)).isEqualTo(1);
    assertThat(numberOfRegistered(CasePlanModel.class)).isEqualTo(1);
    assertThat(numberOfRegistered(HumanTask.class)).isEqualTo(3);
    assertThat(numberOfRegistered(ProcessTask.class)).isEqualTo(1);
    assertThat(numberOfRegistered(CaseTask.class)).isEqualTo(1);
    assertThat(numberOfRegistered(DecisionTask.class)).isEqualTo(1);
    // 3x HumanTask, 1x ProcessTask, 1x CaseTask, 1x DecisionTask, 1x Task
    assertThat(numberOfRegistered(Task.class)).isEqualTo(7);
    // 1x CasePlanModel, 1x Stage
    assertThat(numberOfRegistered(Stage.class)).isEqualTo(2);
    assertThat(numberOfRegistered(Milestone.class)).isEqualTo(1);
    // Note: EventListener is currently not supported!
    assertThat(numberOfRegistered(EventListener.class)).isZero();
    assertThat(numberOfRegistered(Sentry.class)).isEqualTo(3);

    assertThat(TestCmmnTransformListener.cmmnActivities).hasSize(11);
    assertThat(TestCmmnTransformListener.modelElementInstances).hasSize(24);
    assertThat(TestCmmnTransformListener.sentryDeclarations).hasSize(3);
  }

}
