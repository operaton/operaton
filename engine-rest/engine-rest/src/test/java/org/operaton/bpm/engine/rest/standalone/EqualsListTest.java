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
package org.operaton.bpm.engine.rest.standalone;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.operaton.bpm.engine.rest.helper.EqualsList;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Thorben Lindhauer
 *
 */
public class EqualsListTest {

  protected List<String> list1;
  protected List<String> list2;

  @Before
  public void setUp() {
    list1 = new ArrayList<>();
    list2 = new ArrayList<>();
  }

  @Test
  public void testListsSame() {
    assertThat(new EqualsList(list1).matches(list1)).isTrue();
  }

  @Test
  public void testListsEqual() {
    list1.add("aString");
    list2.add("aString");

    assertThat(new EqualsList(list1).matches(list2)).isTrue();
    assertThat(new EqualsList(list2).matches(list1)).isTrue();
  }

  @Test
  public void testListsNotEqual() {
    list1.add("aString");

    assertThat(new EqualsList(list1).matches(list2)).isFalse();
    assertThat(new EqualsList(list2).matches(list1)).isFalse();
  }

  @Test
  public void testListsNull() {
    assertThat(new EqualsList(null).matches(list1)).isFalse();
    assertThat(new EqualsList(list1).matches(null)).isFalse();
    assertThat(new EqualsList(null).matches(null)).isTrue();
  }

}
