/*
 * Copyright 2026 the Operaton contributors.
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
package org.operaton.bpm.engine.impl.util.xml;

import javax.xml.validation.Schema;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.util.ReflectUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.util.Throwables.getRootCause;

class ParserSchemaTest {

  private static final String SCHEMA = "org/operaton/bpm/engine/impl/util/xml/greeting.xsd";
  private static final String VALID = "org/operaton/bpm/engine/impl/util/xml/greeting-valid.xml";
  private static final String INVALID = "org/operaton/bpm/engine/impl/util/xml/greeting-invalid.xml";
  private static final String SCHEMA_WITH_EXTERNAL_DTD = "org/operaton/bpm/engine/impl/util/xml/greeting-with-external-dtd.xsd";

  /** Minimal concrete Parse: Parse is abstract but declares no abstract methods. */
  static class TestParse extends Parse {
    TestParse(Parser parser) {
      super(parser);
    }
  }

  static class TestParser extends Parser {
    @Override
    public TestParse createParse() {
      return new TestParse(this);
    }

    Schema schemaFor(String schemaResource) {
      return getSchema(schemaResource);
    }
  }

  @Test
  void shouldReuseTheSameCompiledSchemaInstance() {
    TestParser parser = new TestParser();
    String schemaUrl = ReflectUtil.getResourceUrlAsString(SCHEMA);

    Schema first = parser.schemaFor(schemaUrl);
    Schema second = parser.schemaFor(schemaUrl);

    assertThat(first).isNotNull().isSameAs(second);
  }

  @Test
  void shouldShareCompiledSchemaAcrossParserInstances() {
    String schemaUrl = ReflectUtil.getResourceUrlAsString(SCHEMA);

    Schema first = new TestParser().schemaFor(schemaUrl);
    Schema second = new TestParser().schemaFor(schemaUrl);

    assertThat(first).isSameAs(second);
  }

  @Test
  void shouldReportNoProblemsForValidDocument() {
    TestParser parser = new TestParser();
    Parse parse = parser.createParse().sourceResource(VALID, ParserSchemaTest.class.getClassLoader());
    parse.setSchemaResource(ReflectUtil.getResourceUrlAsString(SCHEMA));

    parse.execute();

    assertThat(parse.hasErrors()).isFalse();
    assertThat(parse.getRootElement()).isNotNull();
  }

  @Test
  void shouldStillReportSchemaViolationsAsProblems() {
    TestParser parser = new TestParser();
    Parse parse = parser.createParse().sourceResource(INVALID, ParserSchemaTest.class.getClassLoader());
    parse.setSchemaResource(ReflectUtil.getResourceUrlAsString(SCHEMA));

    parse.execute();

    assertThat(parse.hasErrors()).isTrue();
    assertThat(parse.getProblems()).isNotEmpty();
  }

  @Test
  void shouldNotValidateWhenSchemaResourceIsNull() {
    TestParser parser = new TestParser();
    Parse parse = parser.createParse().sourceResource(INVALID, ParserSchemaTest.class.getClassLoader());
    parse.setSchemaResource(null);

    parse.execute();

    assertThat(parse.hasErrors()).isFalse();
  }

  /**
   * Regression guard: setSchemaResource(null) used to flip the validating flag on a
   * thread-shared SAXParserFactory, so a non-validating parse silently disabled validation
   * for every later parse on the same thread.
   */
  @Test
  void shouldKeepValidatingAfterANonValidatingParse() {
    TestParser parser = new TestParser();
    String schemaUrl = ReflectUtil.getResourceUrlAsString(SCHEMA);

    Parse nonValidating = parser.createParse().sourceResource(INVALID, ParserSchemaTest.class.getClassLoader());
    nonValidating.setSchemaResource(null);
    nonValidating.execute();

    Parse validating = parser.createParse().sourceResource(INVALID, ParserSchemaTest.class.getClassLoader());
    validating.setSchemaResource(schemaUrl);
    validating.execute();

    assertThat(validating.hasErrors()).isTrue();
  }

  /**
   * Regression guard for the ACCESS_EXTERNAL_DTD hardening in {@code compileSchema}: the schema
   * document itself references an external DTD via a DOCTYPE declaration. The referenced DTD
   * file exists on disk, so if access were still allowed, compilation would succeed; the
   * restriction must make it fail instead, and specifically because access is denied, not
   * because the file is missing.
   */
  @Test
  void shouldDenyExternalDtdAccessWhenCompilingSchema() {
    TestParser parser = new TestParser();
    String schemaUrl = ReflectUtil.getResourceUrlAsString(SCHEMA_WITH_EXTERNAL_DTD);

    assertThatThrownBy(() -> parser.schemaFor(schemaUrl)).satisfies(exception -> {
      String rootCauseMessage = getRootCause(exception).getMessage();
      assertThat(rootCauseMessage).contains("accessExternalDTD", "greeting-with-external-dtd.dtd");
    });
  }
}
