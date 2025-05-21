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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.query.Query;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

public abstract class AbstractDefinitionQueryTest {

  protected static final String FIRST_DEPLOYMENT_NAME = "firstDeployment";
  protected static final String SECOND_DEPLOYMENT_NAME = "secondDeployment";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RepositoryService repositoryService;
  RuntimeService runtimeService;

  String deploymentOneId;
  String deploymentTwoId;

  @BeforeEach
  public void before() {
    deploymentOneId = repositoryService
      .createDeployment()
      .name(FIRST_DEPLOYMENT_NAME)
      .addClasspathResource(getResourceOnePath())
      .addClasspathResource(getResourceTwoPath())
      .deploy()
      .getId();

    deploymentTwoId = repositoryService
      .createDeployment()
      .name(SECOND_DEPLOYMENT_NAME)
      .addClasspathResource(getResourceOnePath())
      .deploy()
      .getId();
  }

  protected abstract String getResourceOnePath();

  protected abstract String getResourceTwoPath();

  @AfterEach
  public void after() {
    repositoryService.deleteDeployment(deploymentOneId, true);
    repositoryService.deleteDeployment(deploymentTwoId, true);
  }

  protected void verifyQueryResults(Query query, int countExpected) {
    assertThat(query.list()).hasSize(countExpected);
    assertThat(query.count()).isEqualTo(Long.valueOf(countExpected));

    if (countExpected == 1) {
      assertThat(query.singleResult()).isNotNull();
    } else if (countExpected > 1){
      verifySingleResultFails(query);
    } else if (countExpected == 0) {
      assertThat(query.singleResult()).isNull();
    }
  }

  private void verifySingleResultFails(Query query) {

    // when/then
    assertThatThrownBy(query::singleResult)
      .isInstanceOf(ProcessEngineException.class);
  }
}
