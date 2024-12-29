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
package org.operaton.bpm.engine.spring.test.configuration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.spring.SpringProcessEngineServicesConfiguration;
import org.operaton.bpm.engine.spring.test.SpringProcessEngineTestCase;
import org.operaton.bpm.engine.test.Deployment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Philipp Ossler
 */
@ContextConfiguration(classes = {InMemProcessEngineConfiguration.class, SpringProcessEngineServicesConfiguration.class})
class JavaBasedProcessEngineConfigTest extends SpringProcessEngineTestCase {

  @Autowired
  private Counter couter;

  @Autowired
  protected RuntimeService runtimeService;

  @Deployment
  @Test
  void delegateExpression() {
    runtimeService.startProcessInstanceByKey("SpringProcess");

    assertThat(couter.getCount(), is(1));
  }

  @Deployment
  @Test
  void expression() {
    runtimeService.startProcessInstanceByKey("SpringProcess");

    assertThat(couter.getCount(), is(1));
  }

  @Deployment
  @Test
  void delegateExpressionWithProcessServices() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("SpringProcess").getId();

    assertThat(couter.getCount(), is(1));
    assertThat((Integer) runtimeService.getVariable(processInstanceId, "count"), is(1));
  }

}
