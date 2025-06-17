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
package org.operaton.bpm.engine.rest.standalone;

import java.util.HashMap;
import java.util.Map;

import org.operaton.bpm.engine.rest.helper.EqualsMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thorben Lindhauer
 *
 */
public class EqualsMapTest {

  protected Map<String, Object> map1;
  protected Map<String, Object> map2;

  @BeforeEach
  public void setUp() {
    map1 = new HashMap<>();
    map2 = new HashMap<>();
  }

  @Test
  public void testMapsSame() {
    assertThat(new EqualsMap(map1).matches(map1)).isTrue();
  }

  @Test
  public void testMapsEqual() {
    map1.put("aKey", "aValue");
    map2.put("aKey", "aValue");

    assertThat(new EqualsMap(map1).matches(map2)).isTrue();
    assertThat(new EqualsMap(map2).matches(map1)).isTrue();
  }

  @Test
  public void testMapsNotEqual() {
    map1.put("aKey", "aValue");

    assertThat(new EqualsMap(map1).matches(map2)).isFalse();
    assertThat(new EqualsMap(map2).matches(map1)).isFalse();
  }

  @Test
  public void testMapsNull() {
    assertThat(new EqualsMap(null).matches(map1)).isFalse();
    assertThat(new EqualsMap(map1).matches(null)).isFalse();
    assertThat(new EqualsMap(null).matches(null)).isTrue();
  }

}
