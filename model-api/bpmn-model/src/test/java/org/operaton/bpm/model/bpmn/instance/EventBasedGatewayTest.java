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

import org.junit.jupiter.api.Test;
import org.operaton.bpm.model.bpmn.EventBasedGatewayType;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class EventBasedGatewayTest extends AbstractGatewayTest<EventBasedGateway> {

  @Override
  public Collection<AttributeAssumption> getAttributesAssumptions() {
    return Arrays.asList(
      new AttributeAssumption("instantiate", false, false, false),
      new AttributeAssumption("eventGatewayType", false, false, EventBasedGatewayType.Exclusive)
    );
  }

  @Test
  void getInstantiate() {
    assertThat(gateway.isInstantiate()).isTrue();
  }

  @Test
  void getEventGatewayType() {
    assertThat(gateway.getEventGatewayType()).isEqualTo(EventBasedGatewayType.Parallel);
  }

  @Test
  void shouldFailSetAsyncAfterToEventBasedGateway() {
    // fetching should fail
    try {
      gateway.isOperatonAsyncAfter();
      fail("Expected: UnsupportedOperationException");
    } catch(UnsupportedOperationException ex) {
      // True
    }

    // set the attribute should fail to!
    try {
      gateway.setOperatonAsyncAfter(false);
      fail("Expected: UnsupportedOperationException");
    } catch(UnsupportedOperationException ex) {
      // True
    }
  }
}
