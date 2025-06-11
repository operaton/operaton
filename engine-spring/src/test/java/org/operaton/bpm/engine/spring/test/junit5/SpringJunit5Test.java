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
package org.operaton.bpm.engine.spring.test.junit5;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Joram Barrez
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:org/operaton/bpm/engine/spring/test/junit5/springTypicalUsageTest-context.xml")
class SpringJunit5Test {

  @Autowired
  private ProcessEngine processEngine;

  @Autowired
  private RuntimeService runtimeService;

  @Autowired
  private RepositoryService repositoryService;

  @Autowired
  private TaskService taskService;

  String deploymentId;

  @BeforeEach
  void deploy() {
    deploymentId = repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/spring/test/junit5/SpringJunit5Test.simpleProcessTest.bpmn20.xml")
        .deploy()
        .getId();
  }

  @AfterEach
  void closeProcessEngine() {
    repositoryService.deleteDeployment(deploymentId, true);
    processEngine.close();
    processEngine = null;
    runtimeService = null;
    taskService = null;
  }

  @Test
  @Deployment
  void simpleProcessTest() {
    runtimeService.startProcessInstanceByKey("simpleProcess");
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("My Task");

    taskService.complete(task.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }
}
