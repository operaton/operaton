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
package org.operaton.bpm.engine.test.api.variables;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.VariableInstanceQueryImpl;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.test.Deployment;

@Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
class VariableInstanceQueryVariableIgnoreCaseTest extends AbstractVariableIgnoreCaseTest<VariableInstanceQueryImpl, VariableInstance> {

  RuntimeService runtimeService;

  @BeforeEach
  void init() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess", VARIABLES);
    instance = runtimeService.createVariableInstanceQuery().singleResult();
  }

  @Override
  protected VariableInstanceQueryImpl createQuery() {
    return (VariableInstanceQueryImpl) runtimeService.createVariableInstanceQuery();
  }

  @Override
  protected void assertThatTwoInstancesAreEqual(VariableInstance one, VariableInstance two) {
    assertThat(one.getId()).isEqualTo(two.getId());
  }
}
