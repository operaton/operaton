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
package org.operaton.spin.xml.dom.format.spi;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.operaton.spin.DataFormats;
import org.operaton.spin.impl.xml.dom.format.DomXmlDataFormat;
import org.operaton.spin.impl.xml.dom.format.DomXmlDataFormatReader;
import org.operaton.spin.xml.JdkUtil;
import org.operaton.spin.xml.SpinXmlDataFormatException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomXmlDataFormatProtectionTest {

  protected static DomXmlDataFormat format;

  @BeforeAll
  static void setUpMocks() {
    format = (DomXmlDataFormat) DataFormats.xml();
  }

  @Test
  void shouldThrowExceptionForTooManyAttributes() {
    // IBM JDKs do not check on attribute number limits, skip the test there
    Assumptions.assumeFalse(JdkUtil.runsOnIbmJDK());

    // given
    String testXml = "org/operaton/spin/xml/dom/format/spi/FeatureSecureProcessing.xml";
    InputStream testXmlAsStream = this.getClass().getClassLoader().getResourceAsStream(testXml);
    DomXmlDataFormatReader reader = format.getReader();
    var inputStreamReader = new InputStreamReader(testXmlAsStream);

    // when
    assertThatThrownBy(() ->
      reader.readInput(inputStreamReader))
        // then
        .isInstanceOf(SpinXmlDataFormatException.class);
  }

  @Test
  void shouldThrowExceptionForDoctype() {
    // given
    String testXml = "org/operaton/spin/xml/dom/format/spi/XxeProcessing.xml";
    InputStream testXmlAsStream = this.getClass().getClassLoader().getResourceAsStream(testXml);
    DomXmlDataFormatReader reader = format.getReader();
    var inputStreamReader = new InputStreamReader(testXmlAsStream);

    // when
    assertThatThrownBy(() ->
      reader.readInput(inputStreamReader))
        // then
        .isInstanceOf(SpinXmlDataFormatException.class)
        .hasMessageContaining("SPIN/DOM-XML-01009 Unable to parse input into DOM document")
        .hasStackTraceContaining("DOCTYPE")
        .hasStackTraceContaining("http://apache.org/xml/features/disallow-doctype-decl");
  }

}
