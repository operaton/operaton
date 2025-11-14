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
package org.operaton.spin.json.tree;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.spin.json.SpinJsonNode;
import org.operaton.spin.json.SpinJsonPropertyException;

import static org.operaton.spin.Spin.JSON;
import static org.operaton.spin.json.JsonTestConstants.EXAMPLE_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

/**
 * @author Stefan Hentschel
 */
class JsonTreeRemovePropertyTest {

  protected SpinJsonNode jsonNode;
  protected String order;
  protected String active;

  @BeforeEach
  void readJson() {
    jsonNode = JSON(EXAMPLE_JSON);
    order = "order";
    active = "active";
  }

  @Test
  void removePropertyByName() {
    assertThat(jsonNode.hasProp(order)).isTrue();

    jsonNode.deleteProp(order);
    assertThat(jsonNode.hasProp(order)).isFalse();

  }

  @Test
  void removePropertyByList() {
    List<String> names = new ArrayList<>();
    names.add(order);
    names.add(active);

    assertThat(jsonNode.hasProp(names.get(0))).isTrue();
    assertThat(jsonNode.hasProp(names.get(1))).isTrue();

    jsonNode.deleteProp(names);

    assertThat(jsonNode.hasProp(names.get(0))).isFalse();
    assertThat(jsonNode.hasProp(names.get(1))).isFalse();
  }

  @Test
  void failWhileRemovePropertyByName() {
    assertThatExceptionOfType(SpinJsonPropertyException.class).isThrownBy(() -> jsonNode.deleteProp("waldo"));
  }

  @Test
  void failWhileRemovePropertyByList() {
    List<String> names = List.of(active, "waldo");
    assertThatExceptionOfType(SpinJsonPropertyException.class).isThrownBy(() -> jsonNode.deleteProp(names));
  }
}
