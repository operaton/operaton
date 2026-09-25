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
package org.operaton.bpm.model.xml.impl.parser;

import java.net.URL;

import javax.xml.validation.SchemaFactory;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.model.xml.ModelInstance;
import org.operaton.bpm.model.xml.impl.util.ReflectUtil;
import org.operaton.bpm.model.xml.instance.DomDocument;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractModelParserSchemaFactoryTest {

  private static final String SCHEMA_WITH_EXTERNAL_DTD =
      "org/operaton/bpm/model/xml/impl/parser/SchemaWithExternalDtd.xsd";

  /** Minimal concrete AbstractModelParser, only used to expose createSchemaFactory(). */
  static class TestParser extends AbstractModelParser {
    @Override
    protected ModelInstance createModelInstance(DomDocument document) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Regression guard for the ACCESS_EXTERNAL_DTD hardening in
   * {@link AbstractModelParser#createSchemaFactory()}: the schema document itself references an
   * external DTD via a DOCTYPE declaration. The referenced DTD file exists on disk, so if access
   * were still allowed, compilation would succeed; the restriction must make it fail instead, and
   * specifically because access is denied, not because the file is missing.
   */
  @Test
  void shouldDenyExternalDtdAccessWhenCompilingSchema() {
    SchemaFactory schemaFactory = new TestParser().createSchemaFactory();
    URL schemaUrl = ReflectUtil.getResource(SCHEMA_WITH_EXTERNAL_DTD);

    assertThatThrownBy(() -> schemaFactory.newSchema(schemaUrl)).satisfies(exception ->
        assertThat(exception.getMessage()).contains("accessExternalDTD"));
  }
}
