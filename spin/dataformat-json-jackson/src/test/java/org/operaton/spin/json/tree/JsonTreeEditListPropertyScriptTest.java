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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.operaton.spin.json.JsonTestConstants.EXAMPLE_JSON_FILE_NAME;

import org.junit.jupiter.api.Test;
import org.operaton.spin.impl.test.Script;
import org.operaton.spin.impl.test.ScriptTest;
import org.operaton.spin.impl.test.ScriptVariable;
import org.operaton.spin.json.SpinJsonException;
import org.operaton.spin.json.SpinJsonPropertyException;

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
 *
 * @author Stefan Hentschel
 *
 */
public abstract class JsonTreeEditListPropertyScriptTest extends ScriptTest {

  // ----------------- 1) indexOf ----------------------

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailReadIndexOfNonArray() {
    assertThrows(SpinJsonException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailReadIndexOfWithoutSearchNode() {
    assertThrows(IllegalArgumentException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailReadIndexOfNonExistentValue() {
    assertThrows(SpinJsonException.class, () ->
      failingWithException());
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldReadIndexOf() {
    Number i = script.getVariable("value");

    // Casts to int because ruby returns long instead of int values!
    assertThat(i.intValue()).isEqualTo(1);
  }

  // ----------------- 2) lastIndexOf ----------------------

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailReadLastIndexOfNonArray() {
    assertThrows(SpinJsonException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailReadLastIndexOfWithoutSearchNode() {
    assertThrows(IllegalArgumentException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailReadLastIndexOfNonExistentValue() {
    assertThrows(SpinJsonException.class, () ->
      failingWithException());
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldReadLastIndexOf() {
    Number i = script.getVariable("value");

    // Casts to int because ruby returns long instead of int values!
    assertThat(i.intValue()).isEqualTo(1);
  }

  // ----------------- 3) append ----------------------

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailAppendToNonArray() {
    assertThrows(SpinJsonException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailAppendWrongNode() {
    assertThrows(SpinJsonException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailAppendNullNode() {
    assertThrows(IllegalArgumentException.class, () ->
      failingWithException());
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldAppendNodeToArray() {
    Number oldSize = script.getVariable("oldSize");
    Number newSize = script.getVariable("newSize");
    String value    = script.getVariable("value");

    // casts to int because ruby returns long instead of int values!
    assertThat(oldSize.intValue() + 1).isEqualTo(newSize.intValue());
    assertThat(value).isEqualTo("Testcustomer");
  }

  // ----------------- 4) insertAt ----------------------

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailInsertAtNonArray() {
    assertThrows(SpinJsonException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailInsertAtWithIndexOutOfBounds() {
    assertThrows(IndexOutOfBoundsException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailInsertAtWithNegativeIndexOutOfBounds() {
    assertThrows(IndexOutOfBoundsException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailInsertAtWithWrongObject() {
    assertThrows(SpinJsonException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailInsertAtWithNullObject() {
    assertThrows(IllegalArgumentException.class, () ->
      failingWithException());
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldInsertAtWithIndex() {
    Number oldSize     = script.getVariable("oldSize");
    Number oldPosition = script.getVariable("oldPosition");
    Number newSize     = script.getVariable("newSize");
    Number newPosition = script.getVariable("newPosition");
    String value        = script.getVariable("value");

    // Casts to int because ruby returns long instead of int!
    assertThat(oldSize.intValue() + 1).isEqualTo(newSize.intValue());
    assertThat(oldPosition.intValue() + 1).isEqualTo(newPosition.intValue());
    assertThat(value).isEqualTo("test1");
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldInsertAtWithNegativeIndex() {
    Number oldSize     = script.getVariable("oldSize");
    Number oldPosition = script.getVariable("oldPosition");
    Number newSize     = script.getVariable("newSize");
    Number newPosition = script.getVariable("newPosition");
    String value        = script.getVariable("value");

    // Casts to Int because Ruby returns long values instead of int
    assertThat(oldSize.intValue() + 1).isEqualTo(newSize.intValue());
    assertThat(oldPosition.intValue() + 1).isEqualTo(newPosition.intValue());
    assertThat(value).isEqualTo("test1");
  }

  // ----------------- 5) insertBefore ----------------------

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailInsertBeforeNonExistentSearchObject() {
    assertThrows(SpinJsonException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailInsertBeforeWithNullAsSearchObject() {
    assertThrows(IllegalArgumentException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailInsertNullObjectBeforeSearchObject() {
    assertThrows(IllegalArgumentException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailInsertWrongObjectBeforeSearchObject() {
    assertThrows(SpinJsonPropertyException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailInsertBeforeWrongSearchObject() {
    assertThrows(SpinJsonPropertyException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailInsertBeforeOnNonArray() {
    assertThrows(SpinJsonException.class, () ->
      failingWithException());
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldInsertBeforeSearchObjectOnBeginning() {
    Number oldSize               = script.getVariable("oldSize");
    Number newSize               = script.getVariable("newSize");
    String oldValue              = script.getVariable("oldValue");
    String newValue              = script.getVariable("newValue");
    String oldValueOnNewPosition  = script.getVariable("oldValueOnNewPosition");

    // casts to int because ruby returns long instead of int
    assertThat(oldSize.intValue() + 1).isEqualTo(newSize.intValue());
    assertThat(oldValue).isEqualTo("euro");
    assertThat(oldValue).isEqualTo(oldValueOnNewPosition);
    assertThat(newValue).isEqualTo("Test");
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldInsertBeforeSearchObject() {
    Number oldSize = script.getVariable("oldSize");
    String oldValue = script.getVariable("oldValue");
    String oldValueOnNewPosition = script.getVariable("oldValueOnNewPosition");

    Number newSize = script.getVariable("newSize");
    String newValue = script.getVariable("newValue");

    // casts to int because ruby returns long instead of int
    assertThat(oldSize.intValue() + 1).isEqualTo(newSize.intValue());
    assertThat(oldValue).isEqualTo("dollar");
    assertThat(oldValue).isEqualTo(oldValueOnNewPosition);
    assertThat(newValue).isEqualTo("Test");
  }

  // ----------------- 6) insertAfter ----------------------

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailInsertAfterNonExistentSearchObject() {
    assertThrows(SpinJsonException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailInsertAfterWithNullAsSearchObject() {
    assertThrows(IllegalArgumentException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailInsertNullObjectAfterSearchObject() {
    assertThrows(IllegalArgumentException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailInsertWrongObjectAfterSearchObject() {
    assertThrows(SpinJsonException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailInsertAfterOnNonArray() {
    assertThrows(SpinJsonException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailInsertAfterWrongSearchObject() {
    assertThrows(SpinJsonPropertyException.class, () ->
      failingWithException());
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldInsertAfterSearchObjectOnEnding() {
    Number oldSize               = script.getVariable("oldSize");
    Number newSize               = script.getVariable("newSize");
    String oldValue              = script.getVariable("oldValue");
    String newValue              = script.getVariable("newValue");
    String oldValueOnNewPosition  = script.getVariable("oldValueOnNewPosition");

    // casts to int because ruby returns long instead of int
    assertThat(oldSize.intValue() + 1).isEqualTo(newSize.intValue());
    assertThat(oldValue).isEqualTo("dollar");
    assertThat(oldValue).isEqualTo(oldValueOnNewPosition);
    assertThat(newValue).isEqualTo("Test");
  }

  // ----------------- 7) remove ----------------------

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailRemoveNonExistentObject() {
    assertThrows(SpinJsonPropertyException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailRemoveNonArray() {
    assertThrows(SpinJsonException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailRemoveNullObject() {
    assertThrows(IllegalArgumentException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailRemoveWrongObject() {
    assertThrows(SpinJsonPropertyException.class, () ->
      failingWithException());
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldRemoveObject() {
    Number oldSize = script.getVariable("oldSize");
    String oldValue = script.getVariable("oldValue");
    Number newSize = script.getVariable("newSize");
    String newValue = script.getVariable("newValue");

    assertThat(oldValue.equals(newValue)).isFalse();

    // Casts to int because ruby returns long instead of int values!
    assertThat(oldSize.intValue() - 1).isEqualTo(newSize.intValue());
  }

  // ----------------- 8) removeLast ----------------------

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailRemoveLastNullObject() {
    assertThrows(IllegalArgumentException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailRemoveLastWrongObject() {
    assertThrows(SpinJsonPropertyException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailRemoveLastNonArray() {
    assertThrows(SpinJsonException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailRemoveLastNonExistentObject() {
    assertThrows(SpinJsonPropertyException.class, () ->
      failingWithException());
  }

  @Test
  @Script
  @ScriptVariable(name = "input", value = "[\"test\",\"test\",\"new value\",\"test\"]")
  public void shouldRemoveLast() {
    Number oldSize = script.getVariable("oldSize");
    Number newSize = script.getVariable("newSize");
    String oldValue = script.getVariable("oldValue");
    String value   = script.getVariable("newValue");

    // casts to int because ruby returns long instead of int
    assertThat(oldSize.intValue() - 1).isEqualTo(newSize.intValue());
    assertThat(oldValue).isEqualTo("test");
    assertThat(value).isEqualTo("new value");
  }

  // ----------------- 9) removeAt ----------------------

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailRemoveAtNonArray() {
    assertThrows(SpinJsonException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailRemoveAtWithIndexOutOfBounds() {
    assertThrows(IndexOutOfBoundsException.class, () ->
      failingWithException());
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldFailRemoveAtWithNegativeIndexOutOfBounds() {
    assertThrows(IndexOutOfBoundsException.class, () ->
      failingWithException());
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldRemoveAtWithIndex() {
    Number oldSize = script.getVariable("oldSize");
    Number newSize = script.getVariable("newSize");
    String value   = script.getVariable("value");


    // casts to int because ruby returns long instead of int
    assertThat(newSize.intValue()).isEqualTo(1);
    assertThat(oldSize.intValue() - 1).isEqualTo(newSize.intValue());
    assertThat(value).isEqualTo("euro");
  }

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_JSON_FILE_NAME)
  public void shouldRemoveAtWithNegativeIndex() {
    Number oldSize = script.getVariable("oldSize");
    Number newSize = script.getVariable("newSize");
    String value   = script.getVariable("value");

    // casts to int because ruby returns long instead of int
    assertThat(newSize.intValue()).isEqualTo(1);
    assertThat(oldSize.intValue() - 1).isEqualTo(newSize.intValue());
    assertThat(value).isEqualTo("dollar");
  }

}
