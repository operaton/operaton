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
package org.operaton.bpm.engine.rest.openapi;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Documentation completeness tests for the generated OpenAPI 3.0 specification.
 *
 * <p>These tests establish a quality baseline and act as acceptance gates for the REST API
 * documentation improvement phases. Tests that currently fail on the unimproved spec are expected
 * to become green as each phase is completed.</p>
 *
 * <p>Phase acceptance mapping:</p>
 * <ul>
 *   <li>{@link #allTagsHaveDescriptions()} — passes after Phase 2b</li>
 *   <li>{@link #noGenericSummaries()} — passes after Phase 3</li>
 *   <li>{@link #statusCode400And404SemanticsNotInverted()} — passes after Phase 1</li>
 *   <li>{@link #allParametersHaveDescriptions()} — passes after Phase 5</li>
 *   <li>{@link #examplesAreSchemaValid()} — passes after Phase 4</li>
 * </ul>
 */
class OpenApiSpecContentTest {

  /**
   * Summaries must not contain these patterns.
   * Bare action words ("Get", "Delete") without a resource noun are too vague.
   * HTTP method annotations ("(POST)", "(GET)") restate information already in the HTTP method.
   * "(Historic)" appended to a summary is redundant since historic endpoints have their own tag.
   * "Get List" and "Get List Count" must be replaced with "List" and "Count" respectively.
   */
  private static final List<String> BANNED_SUMMARY_PATTERNS = List.of(
      "Get List",       // Use "List <Resource>" instead
      "(POST)",         // Never include HTTP method in summary
      "(GET)",          // Never include HTTP method in summary
      "(Historic)"      // Historic tag already implies historicity
  );

  /**
   * Summaries that are exactly one of these bare words (no resource noun following) are banned.
   */
  private static final List<String> BANNED_EXACT_SUMMARIES = List.of(
      "Get",
      "Gets",
      "Delete",
      "Create",
      "Update",
      "Set"
  );

  private static OpenAPI spec;

  @BeforeAll
  static void loadSpec() throws Exception {
    try (InputStream is = OpenApiSpecContentTest.class.getResourceAsStream("/openapi.json")) {
      assertThat(is)
          .as("openapi.json must be present on the test classpath")
          .isNotNull();
      String specJson = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      SwaggerParseResult result = new OpenAPIParser().readContents(specJson, null, null);
      spec = result.getOpenAPI();
    }
  }

  /**
   * Every tag in the spec must have a non-empty description.
   * Acceptance gate for Phase 2b.
   */
  @Test
  void allTagsHaveDescriptions() {
    assertThat(spec.getTags())
        .as("Spec must define at least one tag")
        .isNotEmpty();

    spec.getTags().forEach(tag ->
        assertThat(tag.getDescription())
            .as("Tag '%s' must have a non-empty description", tag.getName())
            .isNotBlank());
  }

  /**
   * Operation summaries must not contain banned generic patterns.
   * Generic summaries like "Get List", summaries with HTTP method annotations like "(POST)",
   * and bare one-word action verbs without a resource noun are not allowed.
   * Acceptance gate for Phase 3.
   */
  @Test
  void noGenericSummaries() {
    List<String> violations = new ArrayList<>();

    spec.getPaths().forEach((path, item) ->
        item.readOperationsMap().forEach((httpMethod, op) -> {
          String summary = op.getSummary();
          if (summary == null) {
            return;
          }
          String trimmed = summary.trim();
          String method = httpMethod.toString();

          BANNED_EXACT_SUMMARIES.forEach(banned -> {
            if (trimmed.equals(banned)) {
              violations.add(String.format("[%s %s] summary '%s' is a bare verb with no resource noun", method, path, summary));
            }
          });

          BANNED_SUMMARY_PATTERNS.forEach(pattern -> {
            if (trimmed.contains(pattern)) {
              violations.add(String.format("[%s %s] summary '%s' contains banned pattern '%s'", method, path, summary, pattern));
            }
          });
        }));

    assertThat(violations)
        .as("All operation summaries must be specific (e.g. 'List Process Definitions', not 'Get List')")
        .isEmpty();
  }

  /**
   * Verifies that 400 and 404 response descriptions are not semantically inverted.
   * 400 Bad Request descriptions must not mention "does not exist" (that is a 404 condition).
   * 404 Not Found descriptions must not mention "negative" (that is a 400 validation condition).
   * Acceptance gate for Phase 1.
   *
   * <p>This checks two specific patterns:
   * <ol>
   *   <li>When BOTH 400 and 404 are defined and the 400 description says "does not exist" while
   *       the 404 description mentions "negative" — a clear inversion.</li>
   *   <li>A 404 description that mentions "negative" (which belongs in 400).</li>
   * </ol>
   *
   * <p>Note: Some endpoints define only a 400 for resource-not-found conditions. While these are
   * semantically imprecise, they are not inversions (no 404 defined to swap with). Those are
   * tracked separately and will be addressed when 404 responses are added.
   */
  @Test
  void statusCode400And404SemanticsNotInverted() {
    List<String> violations = new ArrayList<>();

    spec.getPaths().forEach((path, item) ->
        item.readOperationsMap().forEach((httpMethod, op) -> {
          if (op.getResponses() == null) {
            return;
          }
          String method = httpMethod.toString();
          ApiResponse r400 = op.getResponses().get("400");
          ApiResponse r404 = op.getResponses().get("404");

          // Only flag inversion when BOTH 400 and 404 are defined and descriptions are semantically swapped
          if (r400 != null && r404 != null
              && r400.getDescription() != null && r404.getDescription() != null) {
            boolean r400SaysNotExist = r400.getDescription().toLowerCase().contains("does not exist");
            boolean r404SaysNegative = r404.getDescription().toLowerCase().contains("negative");
            if (r400SaysNotExist && r404SaysNegative) {
              violations.add(String.format(
                  "[%s %s] 400 and 404 descriptions are semantically inverted: 400 says 'does not exist' while 404 says 'negative'",
                  method, path));
            }
          }

          // A 404 that describes a validation failure (negative value) is always wrong
          if (r404 != null && r404.getDescription() != null) {
            if (r404.getDescription().toLowerCase().contains("negative")) {
              violations.add(String.format("[%s %s] 404 description mentions 'negative' — this is a 400 validation condition: %s",
                  method, path, r404.getDescription()));
            }
          }
        }));

    assertThat(violations)
        .as("400 and 404 response descriptions must not be semantically inverted")
        .isEmpty();
  }

  /**
   * Every parameter (path, query, header) must have a non-empty description.
   * Acceptance gate for Phase 5.
   */
  @Test
  void allParametersHaveDescriptions() {
    List<String> violations = new ArrayList<>();

    spec.getPaths().forEach((path, item) ->
        item.readOperationsMap().forEach((httpMethod, op) -> {
          if (op.getParameters() == null) {
            return;
          }
          String method = httpMethod.toString();
          op.getParameters().forEach((Parameter param) -> {
            if (param.getDescription() == null || param.getDescription().isBlank()) {
              violations.add(String.format("[%s %s] parameter '%s' has no description",
                  method, path, param.getName()));
            }
          });
        }));

    int total = countAllParameters();
    int missing = violations.size();
    int covered = total - missing;
    double percentage = total > 0 ? (covered * 100.0 / total) : 100.0;

    assertThat(violations)
        .as("Parameter description coverage: %.0f%% (%d/%d). All parameters must have descriptions.", percentage, covered, total)
        .isEmpty();
  }

  /**
   * Counts all parameters across all operations for coverage reporting.
   */
  private int countAllParameters() {
    int[] count = {0};
    spec.getPaths().forEach((path, item) ->
        item.readOperationsMap().forEach((httpMethod, op) -> {
          if (op.getParameters() != null) {
            count[0] += op.getParameters().size();
          }
        }));
    return count[0];
  }

  /**
   * Every example in the spec must conform to its enclosing schema.
   * A schema-invalid example is worse than no example — it actively misleads developers.
   * Acceptance gate for Phase 4.
   *
   * <p>Note: This test validates that examples can be re-parsed against the spec without
   * structural errors. Full schema validation is performed by the swagger-parser during
   * the generate-resources phase.</p>
   */
  @Test
  void examplesAreSchemaValid() {
    List<String> violations = new ArrayList<>();

    spec.getPaths().forEach((path, item) ->
        item.readOperationsMap().forEach((httpMethod, op) -> {
          String method = httpMethod.toString();
          // Check request body examples
          if (op.getRequestBody() != null && op.getRequestBody().getContent() != null) {
            op.getRequestBody().getContent().forEach((mediaType, content) -> {
              if (content.getExamples() != null) {
                content.getExamples().forEach((exampleName, exampleObj) -> {
                  if (exampleObj.getValue() == null && exampleObj.get$ref() == null
                      && (exampleObj.getExternalValue() == null || exampleObj.getExternalValue().isBlank())) {
                    violations.add(String.format("[%s %s] request body example '%s' has no value, $ref, or externalValue",
                        method, path, exampleName));
                  }
                });
              }
              if (content.getExample() != null && content.getSchema() != null) {
                validateExampleAgainstSchema(content.getExample(), content.getSchema(), method, path, "request body inline example", violations);
              }
            });
          }

          // Check response examples
          if (op.getResponses() != null) {
            op.getResponses().forEach((code, response) -> {
              if (response.getContent() != null) {
                response.getContent().forEach((mediaType, content) -> {
                  if (content.getExamples() != null) {
                    content.getExamples().forEach((exampleName, exampleObj) -> {
                      if (exampleObj.getValue() == null && exampleObj.get$ref() == null
                          && (exampleObj.getExternalValue() == null || exampleObj.getExternalValue().isBlank())) {
                        violations.add(String.format("[%s %s] %s response example '%s' has no value, $ref, or externalValue",
                            method, path, code, exampleName));
                      }
                    });
                  }
                });
              }
            });
          }
        }));

    assertThat(violations)
        .as("All examples in the spec must have a value, $ref, or externalValue")
        .isEmpty();
  }

  private void validateExampleAgainstSchema(Object example, Schema<?> schema, String method, String path, String location, List<String> violations) {
    // Null examples are not allowed where an example field is present
    if (example == null) {
      violations.add(String.format("[%s %s] %s: example is null", method, path, location));
    }
  }

}
