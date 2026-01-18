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
package org.operaton.bpm.model.bpmn.instance;
import java.util.List;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.model.bpmn.impl.instance.Source;
import org.operaton.bpm.model.bpmn.impl.instance.Target;

import static org.assertj.core.api.Assertions.assertThat;

class LinkEventDefinitionTest extends AbstractEventDefinitionTest {

  @Override
  public Collection<ChildElementAssumption> getChildElementAssumptions() {
    return List.of(
      new ChildElementAssumption(Source.class),
      new ChildElementAssumption(Target.class, 0, 1)
    );
  }

  @Override
  public Collection<AttributeAssumption> getAttributesAssumptions() {
    return List.of(
      new AttributeAssumption("name", false, true)
    );
  }

  @Test
  void getEventDefinition() {
    LinkEventDefinition eventDefinition = eventDefinitionQuery.filterByType(LinkEventDefinition.class).singleResult();
    assertThat(eventDefinition).isNotNull();
    assertThat(eventDefinition.getName()).isEqualTo("link");
    assertThat(eventDefinition.getSources().iterator().next().getName()).isEqualTo("link");
    assertThat(eventDefinition.getTarget().getName()).isEqualTo("link");
  }

}
