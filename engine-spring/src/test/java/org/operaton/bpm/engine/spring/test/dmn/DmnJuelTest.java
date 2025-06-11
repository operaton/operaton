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
package org.operaton.bpm.engine.spring.test.dmn;

import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.RepositoryService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
  "classpath:org/operaton/bpm/engine/spring/test/dmn/DmnJuelTest-applicationContext.xml"})
class DmnJuelTest {

  @Autowired
  protected RepositoryService repositoryService;

  @Autowired
  protected DecisionService decisionService;

  protected String deploymentId;

  @BeforeEach
  void deploy() {
    deploymentId = repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/spring/test/dmn/JuelTest.dmn")
        .deploy()
        .getId();
  }

  @AfterEach
  void clean() {
    repositoryService.deleteDeployment(deploymentId, true);
  }

  @Test
  void shouldResolveBean() {
    // given

    // when
    DmnDecisionResult result = decisionService.evaluateDecisionByKey("drg-with-bean-expression")
        .evaluate();

    // then
    assertThat((String)result.getSingleEntry()).isEqualTo("bar");
  }

}
