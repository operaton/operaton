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
package org.operaton.bpm.qa.largedata;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.impl.db.sql.DbSqlSessionFactory;
import org.operaton.bpm.engine.impl.util.CollectionUtil;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.qa.largedata.util.EngineDataGenerator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeleteDeploymentCascadeTest {

  @RegisterExtension
  static ProcessEngineExtension processEngineExtension = ProcessEngineExtension.builder().configurationResource("operaton.cfg.xml").build();

  protected static final String DATA_PREFIX = DeleteDeploymentCascadeTest.class.getSimpleName();

  static final int GENERATE_PROCESS_INSTANCES_COUNT = 2500;
  protected ProcessEngine processEngine;
  protected RepositoryService repositoryService;
  protected HistoryService historyService;
  protected EngineDataGenerator generator;

  @BeforeEach
  void init() {
    // generate data
    generator = new EngineDataGenerator(processEngine, GENERATE_PROCESS_INSTANCES_COUNT, DATA_PREFIX);
    generator.deployDefinitions();
    generator.generateCompletedProcessInstanceData();
  }

  @AfterEach
  void teardown() {
    Deployment deployment = repositoryService.createDeploymentQuery().deploymentName(generator.getDeploymentName()).singleResult();
    if (deployment != null) {
      List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
          .processDefinitionKey(generator.getAutoCompleteProcessKey()).list();
      if (!processInstances.isEmpty()) {
        List<String> processInstanceIds = processInstances.stream().map(HistoricProcessInstance::getId).toList();
        List<List<String>> partitions = CollectionUtil.partition(processInstanceIds, DbSqlSessionFactory.MAXIMUM_NUMBER_PARAMS);
        for (List<String> partition : partitions) {
          historyService.deleteHistoricProcessInstances(partition);
        }
      }
      repositoryService.deleteDeployment(deployment.getId(), false);
    }
  }

  @Test
  void shouldDeleteCascadeWithLargeParameterCount() {
    // given
    Deployment deployment = repositoryService.createDeploymentQuery().deploymentName(generator.getDeploymentName()).singleResult();

    // when
    repositoryService.deleteDeployment(deployment.getId(), true);

    // then
    deployment = repositoryService.createDeploymentQuery().deploymentName(generator.getDeploymentName()).singleResult();
    assertThat(deployment).isNull();
    List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery()
        .processDefinitionKey(generator.getAutoCompleteProcessKey()).list();
    assertThat(instances).isEmpty();
  }
}
