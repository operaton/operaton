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
package org.operaton.bpm.model.xml.validation;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.model.xml.ModelInstance;
import org.operaton.bpm.model.xml.impl.validation.ModelValidationResultsImpl;
import org.operaton.bpm.model.xml.testmodel.TestModelParser;
import org.operaton.bpm.model.xml.testmodel.instance.Bird;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Meyer
 */
class ModelValidationTest {

  protected ModelInstance modelInstance;

  @BeforeEach
  void parseModel() {
    TestModelParser modelParser = new TestModelParser();
    String testXml = "org/operaton/bpm/model/xml/testmodel/instance/UnknownAnimalTest.xml";
    InputStream testXmlAsStream = this.getClass().getClassLoader().getResourceAsStream(testXml);

    modelInstance = modelParser.parseModelFromStream(testXmlAsStream);
  }

  @Test
  void shouldValidateWithEmptyList() {
    ValidationResults results = modelInstance.validate(List.of());

    assertThat(results).isNotNull();
    assertThat(results.hasErrors()).isFalse();
  }

  @Test
  void shouldCollectWarnings() {
    List<ModelElementValidator<?>> validators = List.of(new IsAdultWarner());

    ValidationResults results = modelInstance.validate(validators);

    assertThat(results).isNotNull();
    assertThat(results.hasErrors()).isFalse();
    assertThat(results.getErrorCount()).isZero();
    assertThat(results.getWarinigCount()).isEqualTo(7);
  }

  @Test
  void shouldCollectErrors() {
    List<ModelElementValidator<?>> validators = List.of(new IllegalBirdValidator("tweety"));

    ValidationResults results = modelInstance.validate(validators);

    assertThat(results).isNotNull();
    assertThat(results.hasErrors()).isTrue();
    assertThat(results.getErrorCount()).isEqualTo(1);
    assertThat(results.getWarinigCount()).isZero();
  }

  @Test
  void shouldWriteResults() {
    List<ModelElementValidator<?>> validators = List.of(new IllegalBirdValidator("tweety"));

    ValidationResults results = modelInstance.validate(validators);

    StringWriter stringWriter = new StringWriter();
    results.write(stringWriter, new TestResultFormatter());

    // Use platform-independent comparison by normalizing newlines
    assertThat(stringWriter.toString())
        .isEqualToNormalizingNewlines("tweety\n\tERROR (20): Bird tweety is illegal\n");
  }

  @Test
  void shouldWriteResultsUntilMaxSize() {
    // Given
    int maxSize = 120;

    // adds 7 elements with warnings of size 30, and an element prefix of size 8
    // total size for 1 warning = 38
    List<ModelElementValidator<?>> validators = List.of(new IsAdultWarner());

    var results = modelInstance.validate(validators);
    var stringWriter = new StringWriter();

    // When
    results.write(stringWriter, new TestResultFormatter(), maxSize);

    // it has enough size to print 3 warnings, but it will only print 2,
    // because it needs to accommodate the suffix too in the max size.
    assertThat(stringWriter.toString())
        .describedAs("2 lines for 2 element names, 2 line for 2 warnings and one for the suffix")
        .hasLineCount(5)
        .describedAs(
            "shall contain only one error/warning and mention the count of the missing ones")
        .endsWith(TestResultFormatter.OMITTED_RESULTS_SUFFIX_FORMAT.formatted(5));
  }

  @Test
  void shouldCombineDifferentValidationResults() {
    // Given
    int maxSize = 120;

    // has 7 warnings
    var results1 = modelInstance.validate(List.of(new IsAdultWarner()));
    // has 1 error
    var results2 = modelInstance.validate(List.of(new IllegalBirdValidator("tweety")));
    var stringWriter = new StringWriter();

    // When
    var results = new ModelValidationResultsImpl(results1, results2);
    results.write(stringWriter, new TestResultFormatter(), maxSize);

    // it has enough size to print 3 warnings, but it will only print 2,
    // because it needs to accommodate the suffix too in the max size.
    assertThat(stringWriter.toString())
        .describedAs("2 lines for 2 element names, 2 line for 2 warnings and one for the suffix")
        .hasLineCount(5)
        .describedAs(
            "shall contain only one error/warning and mention the count of the missing ones")
        .endsWith(TestResultFormatter.OMITTED_RESULTS_SUFFIX_FORMAT.formatted(6));
  }

  @Test
  void shouldReturnResults() {
    List<ModelElementValidator<?>> validators =
        List.of(new IllegalBirdValidator("tweety"), new IsAdultWarner());

    ValidationResults results = modelInstance.validate(validators);

    assertThat(results.getErrorCount()).isEqualTo(1);
    assertThat(results.getWarinigCount()).isEqualTo(7);

    var resultsByElement = results.getResults();
    assertThat(resultsByElement).hasSize(7);

    for (var resultEntry : resultsByElement.entrySet()) {
      Bird element = (Bird) resultEntry.getKey();
      List<ValidationResult> validationResults = resultEntry.getValue();
      assertThat(element).isNotNull();
      assertThat(validationResults).isNotNull();

      if ("tweety".equals(element.getId())) {
        assertThat(validationResults).hasSize(2);
        ValidationResult error = validationResults.remove(0);
        assertThat(error.getType()).isEqualTo(ValidationResultType.ERROR);
        assertThat(error.getCode()).isEqualTo(20);
        assertThat(error.getMessage()).isEqualTo("Bird tweety is illegal");
        assertThat(error.getElement()).isEqualTo(element);
      } else {
        assertThat(validationResults).hasSize(1);
      }

      ValidationResult warning = validationResults.get(0);
      assertThat(warning.getType()).isEqualTo(ValidationResultType.WARNING);
      assertThat(warning.getCode()).isEqualTo(10);
      assertThat(warning.getMessage()).isEqualTo("Is not an adult");
      assertThat(warning.getElement()).isEqualTo(element);
    }
  }
}
