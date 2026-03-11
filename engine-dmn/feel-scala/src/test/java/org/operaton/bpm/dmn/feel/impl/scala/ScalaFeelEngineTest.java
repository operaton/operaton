/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.dmn.feel.impl.scala;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.dmn.feel.impl.FeelException;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.context.VariableContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

/**
 * Unit tests for the {@code ScalaFeelEngine} implementation.
 *
 * <p>
 * This test class validates the behavior and correctness of the {@code ScalaFeelEngine}
 * by covering a wide range of FEEL (Friendly Enough Expression Language) use cases.
 * It includes test cases for:
 * </p>
 *
 * <ul>
 *   <li>Numeric and arithmetic expressions</li>
 *   <li>String operations and comparisons</li>
 *   <li>Logical operations and Boolean expressions</li>
 *   <li>Unary tests and decision logic</li>
 *   <li>Edge cases derived from integration logs and real-world scenarios</li>
 * </ul>
 *
 * <p>
 * These tests are informed by execution logs across various modules to ensure
 * comprehensive coverage and alignment with actual usage patterns.
 * </p>
 *
 * <p>
 *  Goal of the test is to maintain the behavior of the ScalaFeelEngine after migrating to
 *  {@link org.camunda.feel.api.FeelEngineApi}.
 * </p>
 */
class ScalaFeelEngineTest {

  ScalaFeelEngine engine = new ScalaFeelEngine(Collections.emptyList());

  @Test
  void shouldEvaluateSimpleNumericExpression() {
    VariableContext variableCtx = Variables.putValue("amount", 300.0).asVariableContext();

    Object amountResult = engine.evaluateSimpleExpression("amount", variableCtx);
    assertThat(amountResult).isEqualTo(300L);  // Note: ScalaFeelEngine returns Long for numeric values
  }

  @Test
  void shouldEvaluateNumericComparison_LessThan() {
    VariableContext variableCtx = Variables.putValue("cellInput", 300.0).asVariableContext();

    boolean lessThan250Result = engine.evaluateSimpleUnaryTests("< 250", "cellInput", variableCtx);
    assertThat(lessThan250Result).isFalse();
  }

  @Test
  void shouldEvaluateNumericComparison_Range() {
    VariableContext variableCtx = Variables.putValue("cellInput", 300.0).asVariableContext();

    boolean inRangeResult = engine.evaluateSimpleUnaryTests("[250..1000]", "cellInput", variableCtx);
    assertThat(inRangeResult).isTrue();
  }

  @Test
  void shouldEvaluateNumericComparison_GreaterThan() {
    VariableContext variableCtx = Variables.putValue("cellInput", 300.0).asVariableContext();

    boolean moreThan1000Result = engine.evaluateSimpleUnaryTests("> 1000", "cellInput", variableCtx);
    assertThat(moreThan1000Result).isFalse();
  }

  @Test
  void shouldEvaluateSimpleStringExpression() {
    VariableContext variableCtx = Variables.putValue("invoiceCategory", "Travel Expenses").asVariableContext();

    Object categoryResult = engine.evaluateSimpleExpression("invoiceCategory", variableCtx);
    assertThat(categoryResult).isEqualTo("Travel Expenses");
  }

  @Test
  void shouldEvaluateStringComparisons() {
    VariableContext variableCtx = Variables.putValue("cellInput", "Travel Expenses").asVariableContext();

    boolean isMiscResult = engine.evaluateSimpleUnaryTests("\"Misc\"", "cellInput", variableCtx);
    assertThat(isMiscResult).isFalse();

    boolean isTravelExpensesResult = engine.evaluateSimpleUnaryTests("\"Travel Expenses\"", "cellInput", variableCtx);
    assertThat(isTravelExpensesResult).isTrue();

    boolean isSoftwareLicenseResult = engine.evaluateSimpleUnaryTests("\"Software License Costs\"", "cellInput",
      variableCtx);
    assertThat(isSoftwareLicenseResult).isFalse();
  }

  @Test
  void shouldEvaluateStringLiteral() {
    VariableContext variableCtx = Variables.putValue("dummy", "dummy").asVariableContext();

    Object dayToDayExpResult = engine.evaluateSimpleExpression("\"day-to-day expense\"", variableCtx);
    assertThat(dayToDayExpResult).isEqualTo("day-to-day expense");
  }

  @Test
  void shouldEvaluateMultipleCategories() {
    VariableContext variableCtx = Variables.putValue("cellInput", "day-to-day expense")
      .putValue("invoiceClassification", "day-to-day expense")
      .asVariableContext();

    boolean multiCategoryResult = engine.evaluateSimpleUnaryTests("\"budget\", \"exceptional\"", "cellInput",
      variableCtx);
    assertThat(multiCategoryResult).isFalse();
  }

  @Test
  void shouldEvaluateDepartmentAssignments() {
    VariableContext variableCtx = Variables.putValue("dummy", "dummy").asVariableContext();

    Object accountingDept = engine.evaluateSimpleExpression("\"accounting\"", variableCtx);
    assertThat(accountingDept).isEqualTo("accounting");

    Object salesDept = engine.evaluateSimpleExpression("\"sales\"", variableCtx);
    assertThat(salesDept).isEqualTo("sales");
  }

  @Test
  void shouldEvaluateBetweenExpression() {
    VariableContext variableCtx = Variables.putValue("minValue", 0)
      .putValue("maxValue", 10)
      .putValue("x", 5)
      .asVariableContext();

    boolean result = engine.evaluateSimpleUnaryTests("x between minValue and maxValue", "x", variableCtx);
    assertThat(result).isTrue();
  }

  @Test
  void shouldEvaluateStringConcatenation() {
    VariableContext variableCtx = Variables.putValue("user", "john.doe")
      .putValue("domain", "example.com")
      .asVariableContext();

    Object result = engine.evaluateSimpleExpression("\"Email: \" + user + \"@\" + domain", variableCtx);
    assertThat(result).isEqualTo("Email: john.doe@example.com");
  }

  @Test
  void shouldEvaluateSimpleArithmetic() {
    VariableContext variableCtx = Variables.putValue("a", 5).putValue("b", 3).asVariableContext();

    Object result = engine.evaluateSimpleExpression("a + b", variableCtx);
    assertThat(result).isEqualTo(8L);
  }

  @Test
  void shouldEvaluateComparisonWithInputName() {
    boolean result = engine.evaluateSimpleUnaryTests("< 5", "number",
      Variables.putValue("number", 6).asVariableContext());
    assertThat(result).isFalse();
  }

  @Test
  void shouldEvaluateComparisonWithInputNameAndEmptyContext() {
    boolean result = engine.evaluateSimpleUnaryTests("8 < 5", null, Variables.emptyVariableContext());
    assertThat(result).isFalse();
  }

  @Test
  void shouldEvaluatePriceCalculationWithDiscountAndTax() {
    VariableContext variableCtx = Variables.putValue("price", 100)
      .putValue("discount", 20)
      .putValue("tax", 7)
      .asVariableContext();

    Object result = engine.evaluateSimpleExpression("(price - (price * discount / 100)) * (1 + tax / 100)",
      variableCtx);
    assertThat(result).isEqualTo(85.6);
  }

  @Test
  void shouldEvaluateConditionalExpression() {
    VariableContext variableCtx = Variables.putValue("score", 75).asVariableContext();

    Object result = engine.evaluateSimpleExpression(
      "if score >= 90 then \"A\" else if score >= 80 then \"B\" else if score >= 70 then \"C\" else \"F\"",
      variableCtx);
    assertThat(result).isEqualTo("C");
  }

  @Test
  void shouldEvaluateLiteralArithmetic() {
    Object result = engine.evaluateSimpleExpression("1 + 1", null);
    assertThat(result).isEqualTo(2L);
  }

  @Test
  void shouldEvaluateComplexExpression() {
    VariableContext variableCtx = Variables.putValue("cellInput", "day-to-day expense")
      .putValue("invoiceClassification", "day-to-day expense")
      .asVariableContext();

    boolean result = engine.evaluateSimpleUnaryTests("\"budget\", \"exceptional\"", "cellInput", variableCtx);
    assertThat(result).isFalse();
  }

  @Test
  void shouldEvaluateNumericRangeComparisonForInvoices() {
    // Tests the evaluation of numeric range comparisons for invoice amounts
    VariableContext variableCtx = Variables.putValue("cellInput", 300.0).asVariableContext();

    boolean result = engine.evaluateSimpleUnaryTests("[250..1000]", "cellInput", variableCtx);
    assertThat(result).isTrue();

    result = engine.evaluateSimpleUnaryTests("< 250", "cellInput", variableCtx);
    assertThat(result).isFalse();

    result = engine.evaluateSimpleUnaryTests("> 1000", "cellInput", variableCtx);
    assertThat(result).isFalse();
  }

  @Test
  void shouldEvaluateStringLiteralReturnValues() {
    // Tests the evaluation of string literals as return values
    VariableContext dummyVarCtx = Variables.putValue("dummy", "dummy").asVariableContext();

    Object accountingResult = engine.evaluateSimpleExpression("\"accounting\"", dummyVarCtx);
    assertThat(accountingResult).isEqualTo("accounting");

    Object salesResult = engine.evaluateSimpleExpression("\"sales\"", dummyVarCtx);
    assertThat(salesResult).isEqualTo("sales");

    Object dayToDayExpenseResult = engine.evaluateSimpleExpression("\"day-to-day expense\"", dummyVarCtx);
    assertThat(dayToDayExpenseResult).isEqualTo("day-to-day expense");
  }

  @Test
  void shouldEvaluateExpenseCategories() {
    // Tests the evaluation of expense categories
    VariableContext variableCtx = Variables.putValue("cellInput", "Travel Expenses").asVariableContext();

    boolean isMiscResult = engine.evaluateSimpleUnaryTests("\"Misc\"", "cellInput", variableCtx);
    assertThat(isMiscResult).isFalse();

    boolean isTravelExpensesResult = engine.evaluateSimpleUnaryTests("\"Travel Expenses\"", "cellInput", variableCtx);
    assertThat(isTravelExpensesResult).isTrue();

    boolean isSoftwareLicenseCostsResult = engine.evaluateSimpleUnaryTests("\"Software License Costs\"", "cellInput",
      variableCtx);
    assertThat(isSoftwareLicenseCostsResult).isFalse();
  }

  @Test
  void shouldEvaluateListsAndCollections() {
    VariableContext variableCtx = Variables.putValue("items", java.util.List.of("a", "b", "c"))
      .asVariableContext();

    Object result = engine.evaluateSimpleExpression("count(items)", variableCtx);
    assertThat(result).isEqualTo(3L);
  }

  @Test
  void shouldEvaluateNestedExpressions() {
    VariableContext variableCtx = Variables.putValue("level1", Map.of("level2", Map.of("value", 42L)))
      .asVariableContext();

    Object result = engine.evaluateSimpleExpression("level1.level2.value", variableCtx);
    assertThat(result).isEqualTo(42L);
  }

  @Test
  void shouldEvaluateDateAndTimeExpressions() {
    VariableContext variableCtx = Variables.emptyVariableContext();

    Object result = engine.evaluateSimpleExpression("date(\"2023-01-15\")", variableCtx);
    assertThat(result)
            .isNotNull()
            .isInstanceOf(LocalDate.class);
  }

  @Test
  void throwsExceptionForInvalidExpression() {
    // Test for invalid expression syntax
    VariableContext emptyContext = Variables.emptyVariableContext();

    Exception exception = assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
      engine.evaluateSimpleExpression("1 + )", emptyContext)).actual();

    assertThat(exception.getMessage()).contains("failed to parse expression");
  }

  @Test
  void throwsExceptionForInvalidUnaryTestExpression() {
    VariableContext variableCtx = Variables.putValue("cellInput", 300.0).asVariableContext();

    Exception exception = assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
      engine.evaluateSimpleUnaryTests("in [1..]", "cellInput", variableCtx)).actual();

    assertThat(exception.getMessage()).contains("failed to parse");
  }

  @Test
  void shouldEvaluateComplexLogicalExpressions() {
    VariableContext variableCtx = Variables.putValue("a", true)
      .putValue("b", false)
      .putValue("c", true)
      .asVariableContext();

    Object result = engine.evaluateSimpleExpression("(a and not(b)) or (b and c)", variableCtx);
    assertThat(result).isEqualTo(true);
  }

  @Test
  void shouldEvaluateEmptyContext() {
    // Tests the evaluation of an expression with an empty context
    VariableContext emptyContext = Variables.emptyVariableContext();

    Object result = engine.evaluateSimpleExpression("10 + 32", emptyContext);
    assertThat(result).isEqualTo(42L);
  }

  @Test
  void shouldEvaluateNullValue() {
    // Tests the evaluation of a null value in an expression
    VariableContext variableCtx = Variables.putValue("nullValue", null).asVariableContext();

    Object result = engine.evaluateSimpleExpression("nullValue = null", variableCtx);
    assertThat(result).isEqualTo(true);
  }

  @Test
  void shouldEvaluateInvoiceProcessingWithMultipleConditions() {
    // Tests the evaluation of invoice processing logic with multiple conditions
    VariableContext variableCtx = Variables.putValue("amount", 1500)
      .putValue("category", "Software License Costs")
      .putValue("urgent", true)
      .asVariableContext();

    // Conditional logic to determine approval type
    Object result = engine.evaluateSimpleExpression(
      "if amount > 1000 and category = \"Software License Costs\" and urgent then \"manager-approval\" "
        + "else if amount > 500 then \"team-lead-approval\" " + "else \"auto-approval\"", variableCtx);

    assertThat(result).isEqualTo("manager-approval");
  }

  @Test
  void shouldEvaluateBuiltinFunctions() {
    // Tests the evaluation of built-in functions like string manipulation
    VariableContext variableCtx = Variables.putValue("value", "FEEL ENGINE").asVariableContext();

    Object result = engine.evaluateSimpleExpression("lower case(value)", variableCtx);
    assertThat(result).isEqualTo("feel engine");

    result = engine.evaluateSimpleExpression("upper case(value)", variableCtx);
    assertThat(result).isEqualTo("FEEL ENGINE");

    result = engine.evaluateSimpleExpression("substring(value, 6, 6)", variableCtx);
    assertThat(result).isEqualTo("ENGINE");
  }

  @Test
  void shouldEvaluateMultipleUnaryTests() {
    // Tests the evaluation of multiple unary tests in a single expression
    VariableContext variableCtx = Variables.putValue("cellInput", 75).asVariableContext();

    boolean result = engine.evaluateSimpleUnaryTests("[50..100], >200", "cellInput", variableCtx);
    assertThat(result).isTrue();

    result = engine.evaluateSimpleUnaryTests("<50, >100", "cellInput", variableCtx);
    assertThat(result).isFalse();
  }

  @Test
  void shouldEvaluateNullSafety() {
    // Tests the null safety in expressions
    VariableContext variableCtx = Variables.putValue("possiblyNull", null).asVariableContext();

    Object result = engine.evaluateSimpleExpression("if possiblyNull != null then possiblyNull else \"default\"",
      variableCtx);
    assertThat(result).isEqualTo("default");
  }

  @Test
  void throwsExceptionWithNullExpression() {
    // Tests the behavior when a null expression is passed
    VariableContext variableCtx = Variables.emptyVariableContext();

    Exception exception = assertThatExceptionOfType(FeelException.class).isThrownBy(() -> engine.evaluateSimpleExpression(null, variableCtx)).actual();

    assertThat(exception.getMessage()).contains("FEEL/SCALA-01008 Error while evaluating expression: failed to parse expression");
  }

  @Test
  void shouldEvaluateEmptyExpression() {
    VariableContext variableCtx = Variables.emptyVariableContext();
    Exception exception = assertThatExceptionOfType(FeelException.class).isThrownBy(() -> engine.evaluateSimpleExpression("", variableCtx)).actual();
    assertThat(exception.getMessage()).contains("failed to parse expression");
  }

  @Test
  void shouldEvaluateNonExistingVariable() {
    VariableContext variableCtx = Variables.emptyVariableContext();
    Object result = engine.evaluateSimpleExpression("nonExistingVar", variableCtx);
    assertThat(result).isNull();
  }

  @Test
  void shouldEvaluateTypeConversions() {
    VariableContext variableCtx = Variables.putValue("stringNumber", "42").asVariableContext();
    Object result = engine.evaluateSimpleExpression("number(stringNumber)", variableCtx);
    assertThat(result).isEqualTo(42L);
  }

  @Test
  void shouldEvaluateComplexDataStructures() {
    Map<String, Object> person = Map.of("name", "John", "address",
      Map.of("city", "Berlin", "country", "Germany"),
      "skills", java.util.List.of("Java", "FEEL", "DMN"));
    VariableContext variableCtx = Variables.putValue("person", person).asVariableContext();

    // Tests access to nested properties
    Object city = engine.evaluateSimpleExpression("person.address.city", variableCtx);
    assertThat(city).isEqualTo("Berlin");

    // Tests access to list elements
    Object firstSkill = engine.evaluateSimpleExpression("person.skills[1]", variableCtx);
    assertThat(firstSkill).isEqualTo("Java");

    // Tests list contains operation
    Object containsJava = engine.evaluateSimpleExpression("list contains(person.skills, \"Java\")", variableCtx);
    assertThat(containsJava).isEqualTo(true);
  }

  @Test
  void shouldEvaluateUnaryTestsWithInvalidInput() {
    VariableContext variableCtx = Variables.putValue("cellInput", "notANumber").asVariableContext();

    // Tests non-numeric input for numeric unary tests
    assertThat(engine.evaluateSimpleUnaryTests("[1..100]", "cellInput", variableCtx)).isFalse();
  }

  @Test
  void shouldEvaluateSpecialCharactersAndUnicode() {
    VariableContext variableCtx = Variables.putValue("specialString", "äöü€$@").asVariableContext();

    Object result = engine.evaluateSimpleExpression("specialString", variableCtx);
    assertThat(result).isEqualTo("äöü€$@");

    // Unicode evaluation
    // Unicode escape sequences in strings must be tested in future versions of ScalaFeelEngine
    Object unicodeResult = engine.evaluateSimpleExpression("\"unicode: ❤\"", variableCtx);
    assertThat(unicodeResult).isEqualTo("unicode: ❤");
  }

  @Test
  void shouldEvaluateConditionalEdgeCases() {
    VariableContext variableCtx = Variables.putValue("value", 0).asVariableContext();

    Object result = engine.evaluateSimpleExpression(
      "if value > 0 then \"positive\" else if value < 0 then \"negative\" else \"zero\"", variableCtx);
    assertThat(result).isEqualTo("zero");
  }

  @Test
  void shouldEvaluateDecimalPrecision() {
    VariableContext variableCtx = Variables.emptyVariableContext();

    Object result = engine.evaluateSimpleExpression("0.1 + 0.2", variableCtx);
    assertThat((double) result).isCloseTo(0.3, within(0.0000001));
  }

  @Test
  void shouldEvaluateUnaryTestsWithContextDependentVariables() {
    VariableContext variableCtx = Variables.putValue("min", 10)
      .putValue("max", 50)
      .putValue("cellInput", 30)
      .asVariableContext();

    boolean result = engine.evaluateSimpleUnaryTests("[min..max]", "cellInput", variableCtx);
    assertThat(result).isTrue();
  }

  @Test
  void shouldEvaluateUnaryTestsWithDate() {
    // Tests the evaluation of unary tests with a date input
    VariableContext emptyContext = Variables.putValue("dateInput", new Date()).asVariableContext();

    boolean result = engine.evaluateSimpleUnaryTests("<= date and time(\"2019-09-12T13:00:00@Europe/Berlin\")",
      "dateInput", emptyContext);
    assertThat(result).isFalse();
  }

  @Test
  void shouldEvaluateUnaryTestsWithGregorianDate() {
    GregorianCalendar calendar = new GregorianCalendar(2025, Calendar.SEPTEMBER, 12, 13, 0);

    // Tests the evaluation of unary tests with a date input
    VariableContext emptyContext = Variables.putValue("dateInput", calendar.getTime()).asVariableContext();

    boolean result = engine.evaluateSimpleUnaryTests("<= date and time(\"2019-09-12T13:00:00@Europe/Berlin\")",
      "dateInput", emptyContext);
    assertThat(result).isFalse();
  }

}
