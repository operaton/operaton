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
package org.operaton.bpm.dmn.engine.feel;

import org.operaton.bpm.dmn.engine.DmnEngineException;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.bpm.engine.variable.Variables;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class FeelBehavior extends DmnEngineTest {

  @Test
  @DecisionResource(resource = "mixed_variable_types.dmn")
  public void shouldCompareMixedVariableTypes() {
    variables.putValue("stringInput", "operaton");
    variables.putValue("numberInput", 13.37);
    variables.putValue("booleanInput", true);

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(true);
  }

  @Test
  @DecisionResource(resource = "string_untyped.dmn")
  public void shouldCompareStringUntyped() {
    variables.putValue("stringInput", "foo");

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(false);
  }

  @Test
  @DecisionResource(resource = "boolean_untyped.dmn")
  public void shouldCompareBooleanUntyped() {
    variables.putValue("booleanInput", true);

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(false);
  }

  @Test
  @DecisionResource(resource = "number_untyped.dmn")
  public void shouldCompareLongUntyped() {
    variables.putValue("numberInput", Long.MAX_VALUE);

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(false);
  }

  @Test
  @DecisionResource(resource = "number_untyped.dmn")
  public void shouldCompareFloatUntyped() {
    variables.putValue("numberInput", (float)5.6666);

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(false);
  }

  @Test
  @DecisionResource(resource = "number_untyped.dmn")
  public void shouldCompareDoubleUntyped() {
    variables.putValue("numberInput", Double.MAX_VALUE);

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(false);
  }

  @Test
  @DecisionResource(resource = "compare_dates_non_typed.dmn")
  public void shouldCompareDates() {
    variables.putValue("date1", new Date());
    variables.putValue("date2", new Date());

    assertThatDecisionTableResult()
    .hasSingleResult()
    .hasSingleEntryTyped(Variables.stringValue("foo"));
  }

  @Test
  @DecisionResource(resource = "compare_dates_non_typed.dmn")
  public void shouldCompareLocalDateTimes() {
    variables.putValue("date1", LocalDateTime.now());
    variables.putValue("date2", LocalDateTime.now());

    assertThatDecisionTableResult()
    .hasSingleResult()
    .hasSingleEntryTyped(Variables.stringValue("foo"));
  }

  @Test
  @DecisionResource(resource = "compare_dates_non_typed.dmn")
  public void shouldCompareZonedDateTimes() {
    variables.putValue("date1", ZonedDateTime.now());
    variables.putValue("date2", ZonedDateTime.now());

    assertThatDecisionTableResult()
    .hasSingleResult()
    .hasSingleEntryTyped(Variables.stringValue("foo"));
  }

  @Disabled("CAM-11319")
  @Test
  @DecisionResource(resource = "compare_dates_non_typed.dmn")
  public void shouldCompareJodaLocalDateTimes() {
    variables.putValue("date1", org.joda.time.LocalDateTime.now());
    variables.putValue("date2", org.joda.time.LocalDateTime.now());

    assertThatDecisionTableResult()
    .hasSingleResult()
    .hasSingleEntryTyped(Variables.stringValue("foo"));
  }

  @Disabled("CAM-11319")
  @Test
  @DecisionResource(resource = "compare_dates_non_typed.dmn")
  public void shouldCompareJodaDateTimes() {
    variables.putValue("date1", org.joda.time.DateTime.now());
    variables.putValue("date2", org.joda.time.DateTime.now());

    assertThatDecisionTableResult()
    .hasSingleResult()
    .hasSingleEntryTyped(Variables.stringValue("foo"));
  }

  @Test
  @DecisionResource(resource = "compare_dates_non_typed.dmn")
  public void shouldCompareTypedDateValues() {
    variables.putValue("date1", Variables.dateValue(new Date()));
    variables.putValue("date2", Variables.dateValue(new Date()));

    assertThatDecisionTableResult()
    .hasSingleResult()
    .hasSingleEntryTyped(Variables.stringValue("foo"));
  }


  @Test
  @DecisionResource(resource = "java_object.dmn")
  public void shouldEvaluateInputClause_Object() {
    // given
    getVariables()
      .putValue("date1", new Date())
      .putValue("person", new Person(new Date()));

    // when
    String foo = evaluateDecision().getSingleEntry();

    // then
    assertThat(foo).isEqualTo("bar");
  }

  @Test
  @DecisionResource(resource = "input_date_typed.dmn")
  public void shouldThrowExceptionWhenEvaluateJodaDate_Typed() {
    // given
    getVariables().putValue("date1", org.joda.time.LocalDate.parse("2020-01-17"));

    // when
    assertThatThrownBy(this::evaluateDecision)
      .isInstanceOf(DmnEngineException.class)
      .hasMessageContaining("DMN-01005 Invalid value '2020-01-17' for clause with type 'date'.");
  }

  @Test
  @DecisionResource(resource = "input_date_typed.dmn")
  public void shouldThrowExceptionWhenEvaluateLocalDate_Typed() {
    // given
    getVariables().putValue("date1", LocalDate.parse("2020-01-17"));

    // when
    assertThatThrownBy(this::evaluateDecision)
      .isInstanceOf(DmnEngineException.class)
      .hasMessageContaining("Unsupported type: 'java.time.LocalDate' " + "cannot be converted to 'java.util.Date'");
  }

  public static class Person {

    Date birthday;

    public Person(Date age) {
      this.birthday = age;
    }

    @SuppressWarnings("unused")
    public Date getBirthday() {
      return birthday;
    }

  }

}
