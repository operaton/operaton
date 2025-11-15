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
package org.operaton.bpm.engine.rest.openapi.generator.impl;

import com.networknt.schema.SchemaException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SchemaValidatorTest {
  Path schema;
  Path instance;

  @BeforeEach
  public void setup() throws Exception {
    schema = Files.createTempFile("schema", ".json");
    instance = Files.createTempFile("instance", ".json");
  }

  @AfterEach
  public void cleanup() throws Exception {
    Files.deleteIfExists(schema);
    Files.deleteIfExists(instance);
  }

  @Test
  public void validJsonShouldNotThrow() throws Exception {
    String schemaJson = """
      {
        "type": "object",
        "properties": {
          "name": { "type": "string" }
        },
        "required": ["name"]
      }
      """;
    String instanceJson = """
      { "name": "foo" }
      """;

    Files.writeString(schema, schemaJson, StandardCharsets.UTF_8);
    Files.writeString(instance, instanceJson, StandardCharsets.UTF_8);

    assertThatCode(() -> SchemaValidator.main(new String[] { schema.toString(), instance.toString() }))
      .doesNotThrowAnyException();
  }

  @Test
  public void invalidJsonShouldThrowSchemaException() throws Exception {
    String schemaJson = """
      {
        "type": "object",
        "properties": {
          "name": { "type": "string" }
        },
        "required": ["name"]
      }
      """;
    // name is a number -> invalid
    String instanceJson = """
      { "name": 123 }
      """;

    Files.writeString(schema, schemaJson, StandardCharsets.UTF_8);
    Files.writeString(instance, instanceJson, StandardCharsets.UTF_8);

    assertThatThrownBy(() -> SchemaValidator.main(new String[] { schema.toString(), instance.toString() }))
      .isInstanceOf(SchemaException.class);
  }
}
