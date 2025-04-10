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
package org.operaton.bpm.engine.test.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

/**
 * @author Stefan Hentschel.
 */
@ExtendWith(ProcessEngineExtension.class)
class VersionTagTest {

  RepositoryService repositoryService;

  @Deployment
  @Test
  void testParsingVersionTag() {
    ProcessDefinition process = repositoryService
      .createProcessDefinitionQuery()
      .orderByProcessDefinitionId()
      .asc()
      .singleResult();

    assertThat(process.getVersionTag()).isEqualTo("ver_tag_1");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/processOne.bpmn20.xml"})
  @Test
  void testParsingNullVersionTag() {
    ProcessDefinition process = repositoryService
      .createProcessDefinitionQuery()
      .orderByProcessDefinitionId()
      .asc()
      .singleResult();

    assertThat(process.getVersionTag()).isNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/versionTag.dmn"})
  @Test
  void testParsingVersionTagDecisionDefinition() {
    DecisionDefinition decision = repositoryService
    .createDecisionDefinitionQuery()
    .orderByDecisionDefinitionVersion()
    .asc()
    .singleResult();

    assertThat(decision.getVersionTag()).isEqualTo("1.0.0");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/noVersionTag.dmn"})
  @Test
  void testParsingNullVersionTagDecisionDefinition() {
    DecisionDefinition decision = repositoryService
      .createDecisionDefinitionQuery()
    .orderByDecisionDefinitionVersion()
    .asc()
    .singleResult();

    assertThat(decision.getVersionTag()).isNull();
  }
}
