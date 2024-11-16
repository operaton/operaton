/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.model.xml.impl.parser;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.model.xml.ModelInstance;
import org.operaton.bpm.model.xml.ModelParseException;
import org.operaton.bpm.model.xml.testmodel.TestModelParser;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ParserTest {

  private static final String ACCESS_EXTERNAL_SCHEMA_PROP = "javax.xml.accessExternalSchema";

  @Test
  void shouldThrowExceptionForTooManyAttributes() {
    TestModelParser modelParser = new TestModelParser();
    String testXml = "org/operaton/bpm/model/xml/impl/parser/FeatureSecureProcessing.xml";
    InputStream testXmlAsStream = this.getClass().getClassLoader().getResourceAsStream(testXml);
    try {
      modelParser.parseModelFromStream(testXmlAsStream);
    } catch (ModelParseException mpe) {
      assertThat(mpe.getMessage()).isEqualTo("SAXException while parsing input stream");
      assertThat(mpe.getCause()).hasMessageContaining("JAXP00010002");
    }
  }

  @Test
  void shouldProhibitExternalSchemaAccessViaSystemProperty() {
    Assertions.assertThatThrownBy(() -> {
      // given
      // the external schema access property is not supported on certain
      // IBM JDK versions, in which case schema access cannot be restricted
      Assumptions.assumeTrue(doesJdkSupportExternalSchemaAccessProperty());

      System.setProperty(ACCESS_EXTERNAL_SCHEMA_PROP, "");

      try {
        TestModelParser modelParser = new TestModelParser();
        String testXml = "org/operaton/bpm/model/xml/impl/parser/ExternalSchemaAccess.xml";
        InputStream testXmlAsStream = this.getClass().getClassLoader().getResourceAsStream(testXml);

        // when
        modelParser.parseModelFromStream(testXmlAsStream);
      } finally {
        System.clearProperty(ACCESS_EXTERNAL_SCHEMA_PROP);
      }
    }) // then
            .isInstanceOf(ModelParseException.class)
            .hasMessage("SAXException while parsing input stream");

  }

  @Test
  void shouldAllowExternalSchemaAccessViaSystemProperty() {
    // given
    System.setProperty(ACCESS_EXTERNAL_SCHEMA_PROP, "all");

    try {
      TestModelParser modelParser = new TestModelParser();
      String testXml = "org/operaton/bpm/model/xml/impl/parser/ExternalSchemaAccess.xml";
      InputStream testXmlAsStream = this.getClass().getClassLoader().getResourceAsStream(testXml);

      // when
      ModelInstance modelInstance = modelParser.parseModelFromStream(testXmlAsStream);

      // then
      assertThat(modelInstance).isNotNull();
    } finally {
      System.clearProperty(ACCESS_EXTERNAL_SCHEMA_PROP);
    }
  }

  @Test
  void shouldThrowExceptionForDoctype() {
    TestModelParser modelParser = new TestModelParser();
    String testXml = "org/operaton/bpm/model/xml/impl/parser/XxeProcessing.xml";
    InputStream testXmlAsStream = this.getClass().getClassLoader().getResourceAsStream(testXml);
    try {
      modelParser.parseModelFromStream(testXmlAsStream);
    } catch (ModelParseException mpe) {
      assertThat(mpe.getMessage()).isEqualTo("SAXException while parsing input stream");
      assertThat(mpe.getCause()).hasMessageContaining("DOCTYPE");
      assertThat(mpe.getCause()).hasMessageContaining("http://apache.org/xml/features/disallow-doctype-decl");
    }
  }

  protected boolean doesJdkSupportExternalSchemaAccessProperty() {
    String jvmVendor = System.getProperty("java.vm.vendor");
    String javaVersion = System.getProperty("java.version");

    boolean isIbmJDK = jvmVendor != null && jvmVendor.contains("IBM");
    boolean isJava6or7 = javaVersion != null && (javaVersion.startsWith("1.6") || javaVersion.startsWith("1.7"));

    return !(isIbmJDK && isJava6or7);

  }

}
