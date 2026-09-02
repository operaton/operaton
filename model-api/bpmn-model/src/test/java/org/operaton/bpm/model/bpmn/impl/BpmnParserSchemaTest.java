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
package org.operaton.bpm.model.bpmn.impl;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.xml.ModelParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BpmnParserSchemaTest {

  private static final String VALID = """
      <?xml version="1.0" encoding="UTF-8"?>
      <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                   targetNamespace="http://operaton.org/test">
        <process id="theProcess" isExecutable="true">
          <startEvent id="theStart"/>
        </process>
      </definitions>
      """;

  /** A startEvent may not contain a nested process; this violates BPMN20.xsd. */
  private static final String SCHEMA_INVALID = """
      <?xml version="1.0" encoding="UTF-8"?>
      <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                   targetNamespace="http://operaton.org/test">
        <process id="theProcess" isExecutable="true">
          <startEvent id="theStart">
            <process id="nonsense"/>
          </startEvent>
        </process>
      </definitions>
      """;

  private static ByteArrayInputStream stream(String xml) {
    return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void shouldParseValidModel() {
    BpmnModelInstance modelInstance = Bpmn.readModelFromStream(stream(VALID));

    assertThat(modelInstance.getDefinitions()).isNotNull();
  }

  /**
   * The DOM pass validates via DomUtil.DomErrorHandler, whose error() throws, so a
   * schema-invalid model must still fail with ModelParseException rather than the
   * ModelValidationException produced by the later explicit validateModel() pass.
   */
  @Test
  void shouldStillRejectSchemaInvalidModelWithModelParseException() {
    assertThatThrownBy(() -> Bpmn.readModelFromStream(stream(SCHEMA_INVALID)))
        .isInstanceOf(ModelParseException.class);
  }

  @Test
  void shouldExposeACompiledSchemaForTheDocumentBuilder() {
    BpmnParser parser = new BpmnParser();

    assertThat(parser.getDocumentBuilderSchema()).isNotNull();
  }

  @Test
  void shouldParseRepeatedlyWithoutRecompilingTheSchema() {
    BpmnParser parser = new BpmnParser();

    assertThat(parser.getDocumentBuilderSchema())
        .isSameAs(parser.getDocumentBuilderSchema());
  }
}
