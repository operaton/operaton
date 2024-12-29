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
package org.operaton.bpm.engine.spring.test.taskassignment;

import org.operaton.bpm.engine.impl.util.CollectionUtil;
import org.operaton.bpm.engine.spring.test.SpringProcessEngineTestCase;
import org.operaton.bpm.engine.test.Deployment;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Joram Barrez
 */
@ContextConfiguration("classpath:org/operaton/bpm/engine/spring/test/taskassignment/taskassignment-context.xml")
class CustomTaskAssignmentTest extends SpringProcessEngineTestCase {

  @Deployment
  @Test
  void setAssigneeThroughSpringService() {
    runtimeService.startProcessInstanceByKey("assigneeThroughSpringService", CollectionUtil.singletonMap("emp", "fozzie"));
    assertThat(taskService.createTaskQuery().taskAssignee("Kermit The Frog").count()).isEqualTo(1);
  }

  @Deployment
  @Test
  void setCandidateUsersThroughSpringService() {
    runtimeService.startProcessInstanceByKey("candidateUsersThroughSpringService", CollectionUtil.singletonMap("emp", "fozzie"));
    assertThat(taskService.createTaskQuery().taskCandidateUser("kermit").count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskCandidateUser("fozzie").count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskCandidateUser("gonzo").count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskCandidateUser("misspiggy").count()).isZero();
  }


  @Deployment
  @Test
  void setCandidateGroupsThroughSpringService() {
    runtimeService.startProcessInstanceByKey("candidateUsersThroughSpringService", CollectionUtil.singletonMap("emp", "fozzie"));
    assertThat(taskService.createTaskQuery().taskCandidateGroup("management").count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskCandidateGroup("directors").count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskCandidateGroup("accountancy").count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskCandidateGroup("sales").count()).isZero();
  }
  
}
