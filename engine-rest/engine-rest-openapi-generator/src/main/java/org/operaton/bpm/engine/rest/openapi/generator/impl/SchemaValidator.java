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
package org.operaton.bpm.engine.rest.openapi.generator.impl;

import com.networknt.schema.SchemaException;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.Error;

/**
 * Utility class to validate a JSON instance against a JSON Schema using the
 * <a href="https://github.com/networknt/json-schema-validator">networknt JSON Schema validator</a>.
 *
 * <p>Usage:
 * <pre>
 * java org.operaton.bpm.engine.rest.openapi.generator.impl.SchemaValidator &lt;json schema&gt; &lt;file to validate&gt;
 * </pre>
 */
public class SchemaValidator {
  /**
   * Main entry point.
   *
   * @param args command line arguments. Must contain exactly two entries:
   *             args[0] = path to the JSON Schema file,
   *             args[1] = path to the JSON instance file to validate.
   * @throws Exception if reading the schema or instance fails, or if the validation
   *                   process encounters an unexpected error.
   * @throws IllegalArgumentException if the argument count is incorrect.
   * @throws SchemaException if schema validation produces one or more errors.
   */
  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      throw new IllegalArgumentException("Must provide two arguments: <json schema> <file to validate>");
    }

    String jsonSchemaPath = args[0];
    String inputFile = args[1];

    ObjectMapper mapper = new ObjectMapper();
    JsonNode schemaNode = mapper.readTree(new File(jsonSchemaPath));
    JsonNode inputNode = mapper.readTree(new File(inputFile));

    SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_4);
    Schema schema = schemaRegistry.getSchema(schemaNode);

    List<Error> errors = schema.validate(inputNode, executionContext -> {});

    if (!errors.isEmpty()) {
      String messages = errors.stream()
                              .map(Error::getMessage)
                              .collect(Collectors.joining("\n"));

      throw new SchemaException("Schema validation errors\n" + messages);
    }
  }
}
