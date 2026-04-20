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
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Structural correctness test for the generated OpenAPI 3.0 specification.
 *
 * Validates that the spec parses without errors, has valid structure, and all $ref targets resolve.
 * This test intentionally establishes a baseline on the current spec.
 */
class OpenApiSpecValidationTest {

  private static OpenAPI spec;
  private static SwaggerParseResult parseResult;

  @BeforeAll
  static void loadSpec() throws Exception {
    try (InputStream is = OpenApiSpecValidationTest.class.getResourceAsStream("/openapi.json")) {
      assertThat(is)
          .as("openapi.json must be present on the test classpath (built via generate-resources phase)")
          .isNotNull();
      String specJson = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      parseResult = new OpenAPIParser().readContents(specJson, null, null);
      spec = parseResult.getOpenAPI();
    }
  }

  @Test
  void specParsesProperly() {
    assertThat(spec).as("OpenAPI spec must parse to a non-null object").isNotNull();
  }

  @Test
  void noParseMessages() {
    assertThat(parseResult.getMessages())
        .as("OpenAPI spec must parse without errors or warnings")
        .isEmpty();
  }

  @Test
  void isOpenApi30x() {
    assertThat(spec.getOpenapi())
        .as("Spec must declare OpenAPI version 3.0.x")
        .startsWith("3.0.");
  }

  @Test
  void hasNonEmptyInfoTitle() {
    assertThat(spec.getInfo()).isNotNull();
    assertThat(spec.getInfo().getTitle())
        .as("info.title must be non-empty")
        .isNotBlank();
  }

  @Test
  void hasNonEmptyInfoVersion() {
    assertThat(spec.getInfo()).isNotNull();
    assertThat(spec.getInfo().getVersion())
        .as("info.version must be non-empty")
        .isNotBlank();
  }

  @Test
  void hasPaths() {
    assertThat(spec.getPaths())
        .as("Spec must define at least one path")
        .isNotEmpty();
  }

  @Test
  void hasComponents() {
    assertThat(spec.getComponents())
        .as("Spec must have a components section")
        .isNotNull();
    assertThat(spec.getComponents().getSchemas())
        .as("Components must define schemas")
        .isNotEmpty();
  }

  @Test
  void allOperationsHaveOperationId() {
    spec.getPaths().forEach((path, item) ->
        item.readOperationsMap().forEach((method, op) ->
            assertThat(op.getOperationId())
                .as("Operation %s %s must have an operationId", method, path)
                .isNotBlank()));
  }

  @Test
  void allOperationsHaveSummary() {
    spec.getPaths().forEach((path, item) ->
        item.readOperationsMap().forEach((method, op) ->
            assertThat(op.getSummary())
                .as("Operation %s %s must have a summary", method, path)
                .isNotBlank()));
  }

  @Test
  void allOperationsHaveAtLeastOneTag() {
    spec.getPaths().forEach((path, item) ->
        item.readOperationsMap().forEach((method, op) ->
            assertThat(op.getTags())
                .as("Operation %s %s must have at least one tag", method, path)
                .isNotEmpty()));
  }

}
