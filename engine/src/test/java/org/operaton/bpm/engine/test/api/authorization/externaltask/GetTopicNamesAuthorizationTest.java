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
package org.operaton.bpm.engine.test.api.authorization.externaltask;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.test.api.authorization.AuthorizationTest;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.READ_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;
import static org.assertj.core.api.Assertions.assertThat;

class GetTopicNamesAuthorizationTest extends AuthorizationTest {

  protected String instance1Id;
  protected String instance2Id;

  @BeforeEach
  @Override
  public void setUp() {
    testRule.deploy(
        "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.testFetchMultipleTopics.bpmn20.xml");

    instance1Id = startProcessInstanceByKey("oneExternalTaskProcess").getId();
    instance2Id = startProcessInstanceByKey("parallelExternalTaskProcess").getId();
    super.setUp();
  }

  @Test
  void testGetTopicNamesWithoutAuthorization() {
    // when
    List<String> result = externalTaskService.getTopicNames();

    // then
    assertThat(result).isEmpty();
  }

  @Test
  void testGetTopicNamesWithReadOnProcessInstance() {
    // given
    createGrantAuthorization(PROCESS_INSTANCE, instance1Id, userId, READ);

    // when
    List<String> result = externalTaskService.getTopicNames();

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo("externalTaskTopic");
  }

  @Test
  void testGetTopicNamesWithReadOnAnyProcessInstance() {
    // given
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);

    // when
    List<String> result = externalTaskService.getTopicNames();

    // then
    assertThat(result).hasSize(4);
  }

  @Test
  void testGetTopicNamesWithReadInstanceOnAnyProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_INSTANCE);

    // when
    List<String> result = externalTaskService.getTopicNames();

    // then
    assertThat(result).hasSize(4);
  }

  @Test
  void testGetTopicNamesWithReadDefinitionWithMultiple() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, "oneExternalTaskProcess", userId, READ_INSTANCE);

    // when
    List<String> result = externalTaskService.getTopicNames();

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo("externalTaskTopic");
  }

  @Test
  void testGetTopicNamesWithReadInstanceWithMultiple() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_INSTANCE);
    createGrantAuthorization(PROCESS_DEFINITION, "oneExternalTaskProcess", userId, READ_INSTANCE);
    createGrantAuthorization(PROCESS_INSTANCE, instance1Id, userId, READ);

    // when
    List<String> result = externalTaskService.getTopicNames();

    // then
    assertThat(result).hasSize(4);
  }
}
