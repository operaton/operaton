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
package org.operaton.spin.json.tree;

import org.operaton.spin.json.SpinJsonException;
import org.operaton.spin.json.SpinJsonNode;
import org.operaton.spin.json.SpinJsonPropertyException;
import static org.operaton.spin.Spin.JSON;
import static org.operaton.spin.json.JsonTestConstants.EXAMPLE_JSON;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Index:
 * 1) indexOf
 * 2) lastIndexOf
 * 3) append
 * 4) insertAt
 * 5) insertBefore
 * 6) insertAfter
 * 7) remove
 * 8) removeLast
 * 9) removeAt
 * 10) contains
 *
 * @author Stefan Hentschel
 */
class JsonTreeEditListPropertyTest {

  protected SpinJsonNode jsonNode;
  protected SpinJsonNode customers;
  protected SpinJsonNode currencies;

  @BeforeEach
  void readJson() {
    jsonNode = JSON(EXAMPLE_JSON);
    customers = jsonNode.prop("customers");
    currencies = jsonNode.prop("orderDetails").prop("currencies");
  }

  // ----------------- 1) indexOf ----------------------

  @Test
  void readIndexOfNonArray() {
    assertThatThrownBy(() -> jsonNode.indexOf("n"))
        .isInstanceOf(SpinJsonException.class);
  }

  @Test
  void readIndexOfWithoutSearchNode() {
    assertThatThrownBy(() -> jsonNode.indexOf(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void readIndexOfNonExistentValue() {
    assertThatThrownBy(() -> customers.indexOf("n"))
        .isInstanceOf(SpinJsonException.class);
  }

  @Test
  void readIndexOfExistentValue() {
    // given
    Integer i;

    // when
    i = currencies.indexOf("euro");

    // then
    assertThat(i).isZero();
  }

  // ----------------- 2) lastIndexOf ----------------------

  @Test
  void readLastIndexOfNonArray() {
    assertThatThrownBy(() -> jsonNode.lastIndexOf("n"))
        .isInstanceOf(SpinJsonException.class);
  }

  @Test
  void readLastIndexOfWithoutSearchNode() {
    assertThatThrownBy(() -> jsonNode.lastIndexOf(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void readLastIndexOfNonExistentValue() {
    assertThatThrownBy(() -> customers.lastIndexOf("n"))
        .isInstanceOf(SpinJsonException.class);
  }

  @Test
  void readLastIndexOfExistentValue() {
    // given
    Integer i;

    // when
    i = currencies.lastIndexOf("dollar");

    // then
    assertThat(i).isEqualTo(1);
  }

  // ----------------- 3) append ----------------------

  @Test
  void appendNodeToArray() {
    // given
    Integer oldSize = customers.elements().size();

    // when
    customers.append("Testcustomer");
    Integer newSize = customers.elements().size();

    // then
    assertThat(oldSize).isNotEqualTo(newSize);
    assertThat(oldSize + 1).isEqualTo(newSize);
    assertThat(customers.elements().get(oldSize).isString()).isTrue();
    assertThat(customers.elements().get(oldSize).stringValue()).isEqualTo("Testcustomer");
  }

  @Test
  void appendNodeToNonArray() {
    assertThatThrownBy(() -> jsonNode.append("testcustomer"))
        .isInstanceOf(SpinJsonException.class);
  }

  @Test
  void appendWrongNode() {
    assertThatThrownBy(() -> jsonNode.append(new Date()))
        .isInstanceOf(SpinJsonException.class);
  }

  @Test
  void appendNullNode() {
    assertThatThrownBy(() -> jsonNode.append(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // ----------------- 4) insertAt ----------------------

  @Test
  void insertAtWithIndex() {
    // given
    int oldSize = currencies.elements().size();
    Integer oldPosition = currencies.indexOf("dollar");
    SpinJsonNode oldNode = currencies.elements().get(1);

    // when
    currencies.insertAt(1, "test1");

    // then
    Integer newSize = currencies.elements().size();
    Integer newPosition = currencies.indexOf("dollar");
    SpinJsonNode newNode = currencies.elements().get(1);

    assertThat(oldSize + 1).isEqualTo(newSize);
    assertThat(newNode.equals(oldNode)).isFalse();
    assertThat(newNode.stringValue()).isEqualTo("test1");
    assertThat(oldPosition + 1).isEqualTo(newPosition);
  }

  @Test
  void insertAtNonArray() {
    assertThatThrownBy(() -> jsonNode.insertAt(1, "test"))
        .isInstanceOf(SpinJsonException.class);
  }

  @Test
  void insertAtWithNegativeIndex() {
    // given
    int oldSize = currencies.elements().size();
    Integer oldPosition = currencies.indexOf("dollar");
    SpinJsonNode oldNode = currencies.elements().get(0);

    // when
    currencies.insertAt(-2, "test1");

    // then
    Integer newSize = currencies.elements().size();
    Integer newPosition = currencies.indexOf("dollar");
    SpinJsonNode newNode = currencies.elements().get(0);

    assertThat(oldSize + 1).isEqualTo(newSize);
    assertThat(newNode.equals(oldNode)).isFalse();
    assertThat(newNode.stringValue()).isEqualTo("test1");
    assertThat(oldPosition + 1).isEqualTo(newPosition);
  }

  @Test
  void insertAtWithIndexOutOfBounds() {
    assertThatThrownBy(() -> currencies.insertAt(6, "string"))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void insertAtWithNegativeIndexOutOfBounds() {
    assertThatThrownBy(() -> currencies.insertAt(-6, "string"))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void insertAtWithWrongObject() {
    assertThatThrownBy(() -> currencies.insertAt(1, new Date()))
        .isInstanceOf(SpinJsonException.class);
  }

  @Test
  void insertAtWithNullObject() {
    assertThatThrownBy(() -> currencies.insertAt(1, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // ----------------- 5) insertBefore ----------------------

  @Test
  void insertBeforeNonExistentSearchObject() {
    assertThatThrownBy(() -> currencies.insertBefore(1, "test"))
        .isInstanceOf(SpinJsonPropertyException.class);
  }

  @Test
  void insertBeforeWithNullAsSearchObject() {
    assertThatThrownBy(() -> currencies.insertBefore(null, "test"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void insertBeforeSearchObjectOnBeginning() {
    // given
    SpinJsonNode oldNode = currencies.elements().get(0);
    Integer size = currencies.elements().size();

    // when
    currencies.insertBefore("euro", "Test");

    // then
    SpinJsonNode newNode = currencies.elements().get(0);
    SpinJsonNode oldNodeNewPosition = currencies.elements().get(1);
    Integer newSize = currencies.elements().size();

    assertThat(oldNode.equals(newNode)).isFalse();
    assertThat(newNode.stringValue()).isEqualTo("Test");
    assertThat(oldNode.stringValue()).isEqualTo(oldNodeNewPosition.stringValue());
    assertThat(size).isNotEqualTo(newSize);
  }

  @Test
  void insertBeforeSearchObject() {
    // given
    SpinJsonNode oldNode = currencies.elements().get(1);
    Integer size = currencies.elements().size();

    // when
    currencies.insertBefore("dollar", "Test");

    // then
    SpinJsonNode newNode = currencies.elements().get(1);
    SpinJsonNode oldNodeNewPosition = currencies.elements().get(2);
    Integer newSize = currencies.elements().size();

    assertThat(oldNode).isNotEqualTo(newNode);
    assertThat(newNode.stringValue()).isEqualTo("Test");
    assertThat(oldNode.stringValue()).isEqualTo(oldNodeNewPosition.stringValue());
    assertThat(size).isNotEqualTo(newSize);
  }

  @Test
  void insertNullObjectBeforeSearchObject() {
    assertThatThrownBy(() -> currencies.insertBefore("euro", null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void insertWrongObjectBeforeSearchObject() {
    assertThatThrownBy(() -> currencies.insertBefore("euro", new Date()))
        .isInstanceOf(SpinJsonPropertyException.class);
  }

  @Test
  void insertObjectBeforeWrongSearchObject() {
    assertThatThrownBy(() -> currencies.insertBefore(new Date(), "test"))
        .isInstanceOf(SpinJsonPropertyException.class);
  }

  @Test
  void insertBeforeOnNonArray() {
    assertThatThrownBy(() -> jsonNode.insertBefore("test", "test"))
        .isInstanceOf(SpinJsonException.class);
  }

  // ----------------- 6) insertAfter ----------------------

  @Test
  void insertAfterNonExistentSearchObject() {
    assertThatThrownBy(() -> currencies.insertBefore("test", "test"))
        .isInstanceOf(SpinJsonException.class);
  }

  @Test
  void insertAfterSearchObjectOnEnding() {
    // given
    SpinJsonNode oldNode = currencies.elements().get(1);
    Integer size = currencies.elements().size();

    // when
    currencies.insertAfter("dollar", "Test");

    // then
    SpinJsonNode newNode = currencies.elements().get(2);
    SpinJsonNode oldNodeNewPosition = currencies.elements().get(1);
    Integer newSize = currencies.elements().size();

    assertThat(oldNode.equals(newNode)).isFalse();
    assertThat(oldNode.stringValue()).isEqualTo(oldNodeNewPosition.stringValue());
    assertThat(size).isNotEqualTo(newSize);
  }

  @Test
  void insertAfterSearchObject() {
    // given
    SpinJsonNode oldNode = currencies.elements().get(0);
    Integer size = currencies.elements().size();

    // when
    currencies.insertAfter("dollar", "Test");

    // then
    SpinJsonNode newNode = currencies.elements().get(1);
    SpinJsonNode oldNodeNewPosition = currencies.elements().get(0);
    Integer newSize = currencies.elements().size();

    assertThat(oldNode.equals(newNode)).isFalse();
    assertThat(oldNode.stringValue()).isEqualTo(oldNodeNewPosition.stringValue());
    assertThat(size).isNotEqualTo(newSize);
  }

  @Test
  void insertNullObjectAfterSearchObject() {
    assertThatThrownBy(() -> currencies.insertBefore("euro", null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void insertWrongObjectAfterSearchObject() {
    assertThatThrownBy(() -> currencies.insertBefore("euro", new Date()))
        .isInstanceOf(SpinJsonException.class);
  }

  @Test
  void insertAfterOnNonArray() {
    assertThatThrownBy(() -> jsonNode.insertBefore("euro", "test"))
        .isInstanceOf(SpinJsonException.class);
  }

  @Test
  void insertObjectAfterWrongSearchObject() {
    assertThatThrownBy(() -> currencies.insertAfter(new Date(), "test"))
        .isInstanceOf(SpinJsonPropertyException.class);
  }

  // ----------------- 7) remove ----------------------

  @Test
  void removeObject() {
    // given
    Integer oldSize = currencies.elements().size();

    // when
    currencies.remove("euro");

    // then
    Integer newSize = currencies.elements().size();
    SpinJsonNode node = currencies.elements().get(0);

    assertThat(oldSize).isNotEqualTo(newSize);
    assertThat(oldSize - 1).isEqualTo(newSize);
    assertThat(node.stringValue()).isEqualTo("dollar");
  }

  @Test
  void removeNonExistentObject() {
    assertThatThrownBy(() -> currencies.remove("test"))
        .isInstanceOf(SpinJsonException.class);
  }

  @Test
  void removeObjectInNonExistentArray() {
    assertThatThrownBy(() -> jsonNode.remove("test"))
        .isInstanceOf(SpinJsonException.class);
  }

  @Test
  void removeNullObject() {
    assertThatThrownBy(() -> jsonNode.remove(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // ----------------- 8) removeLast ----------------------

  @Test
  void removeLastNullObject() {
    assertThatThrownBy(() -> currencies.removeLast(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void removeLastWrongObject() {
    assertThatThrownBy(() -> currencies.removeLast(new Date()))
        .isInstanceOf(SpinJsonException.class);
  }

  @Test
  void removeLast() {
    // given
    String testJson = "[\"test\",\"test\",\"new value\",\"test\"]";
    SpinJsonNode testNode = JSON(testJson);
    Integer size = testNode.elements().size();

    // when
    testNode.removeLast("test");

    // then
    Integer newSize = testNode.elements().size();
    SpinJsonNode node = testNode.elements().get(newSize - 1);

    assertThat(newSize).isNotEqualTo(size);
    assertThat(newSize + 1).isEqualTo(size);
    assertThat(node.stringValue()).isEqualTo("new value");
  }

  @Test
  void removeLastNonExistentArray() {
    assertThatThrownBy(() -> jsonNode.removeLast(1))
        .isInstanceOf(SpinJsonException.class);
  }

  // ----------------- 9) removeAt ----------------------

  @Test
  void removeAtWithIndex() {
    // given
    Integer oldSize = currencies.elements().size();

    // when
    currencies.removeAt(1);

    // then
    Integer newSize = currencies.elements().size();
    SpinJsonNode node = currencies.elements().get(newSize - 1);

    assertThat(newSize).isEqualTo(1);
    assertThat(newSize + 1).isEqualTo(oldSize);
    assertThat(node.stringValue()).isEqualTo("euro");
  }

  @Test
  void removeAtNonArray() {
    assertThatThrownBy(() -> jsonNode.removeAt(1))
        .isInstanceOf(SpinJsonException.class);
  }

  @Test
  void removeAtWithNegativeIndex() {
    // given
    Integer oldSize = currencies.elements().size();

    // when
    currencies.removeAt(-2);

    // then
    Integer newSize = currencies.elements().size();
    SpinJsonNode node = currencies.elements().get(newSize - 1);

    assertThat(newSize).isEqualTo(1);
    assertThat(newSize + 1).isEqualTo(oldSize);
    assertThat(node.stringValue()).isEqualTo("dollar");
  }

  @Test
  void removeAtWithIndexOutOfBounds() {
    assertThatThrownBy(() -> currencies.removeAt(6))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void removeAtWithNegativeIndexOutOfBounds() {
    assertThatThrownBy(() -> currencies.removeAt(-6))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  // ----------------- 10) contains ----------------------

  @Test
  void containsOfNonArray() {
    assertThatThrownBy(() -> jsonNode.contains("n"))
        .isInstanceOf(SpinJsonException.class);
  }

  @Test
  void containsWithoutSearchNode() {
    assertThatThrownBy(() -> jsonNode.contains(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void containsNonExistentValue() {
    // given
    boolean res;

    // when
    res = customers.contains("n");

    // then
    assertThat(res).isFalse();
  }

  @Test
  void containsOfExistentValue() {
    // given
    boolean res;

    // when
    res = currencies.contains("euro");

    // then
    assertThat(res).isTrue();
  }
}