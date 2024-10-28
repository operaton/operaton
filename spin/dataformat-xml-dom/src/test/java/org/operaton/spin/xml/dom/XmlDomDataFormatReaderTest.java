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
package org.operaton.spin.xml.dom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.spin.xml.XmlTestConstants.EXAMPLE_XML;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.spin.DataFormats;
import org.operaton.spin.impl.xml.dom.format.DomXmlDataFormat;
import org.operaton.spin.impl.xml.dom.format.DomXmlDataFormatReader;

public class XmlDomDataFormatReaderTest {

  private DomXmlDataFormatReader reader;
  private Reader inputReader;

  private static final int REWINDING_LIMIT = 256;

  @BeforeEach
  void setUp() {
    DomXmlDataFormat domXmlDataFormat = new DomXmlDataFormat(DataFormats.XML_DATAFORMAT_NAME);
    reader = domXmlDataFormat.getReader();
  }

  @Test
  void shouldMatchXmlInput() throws IOException {
    inputReader = stringToReader(EXAMPLE_XML);
    assertThat(reader.canRead(inputReader, REWINDING_LIMIT)).isTrue();
    inputReader.close();
  }

  @Test
  void shouldMatchXmlInputWithWhitespace() throws IOException {
    inputReader = stringToReader("   " + EXAMPLE_XML);
    assertThat(reader.canRead(inputReader, REWINDING_LIMIT)).isTrue();
    inputReader.close();

    inputReader = stringToReader("\r\n\t   " + EXAMPLE_XML);
    assertThat(reader.canRead(inputReader, REWINDING_LIMIT)).isTrue();
  }

  @Test
  void shouldNotMatchInvalidXml() {
    inputReader = stringToReader("prefix " + EXAMPLE_XML);
    assertThat(reader.canRead(inputReader, REWINDING_LIMIT)).isFalse();
  }

  public Reader stringToReader(String input) {
    return new StringReader(input);
  }

  @AfterEach
  void tearDown() throws IOException {
    if (inputReader != null) {
      inputReader.close();
    }
  }
}
