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
package org.operaton.spin.xml.dom;

import org.operaton.spin.DataFormats;
import org.operaton.spin.spi.DataFormat;
import org.operaton.spin.spi.SpinDataFormatException;
import org.operaton.spin.xml.SpinXmlElement;
import static org.operaton.spin.DataFormats.xml;
import static org.operaton.spin.Spin.S;
import static org.operaton.spin.Spin.XML;
import static org.operaton.spin.impl.util.SpinIoUtil.stringAsReader;
import static org.operaton.spin.xml.XmlTestConstants.*;

import java.io.Reader;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Daniel Meyer
 */
class XmlDomCreateTest {

  private final DataFormat<SpinXmlElement> xmlDataFormat = xml();

  @Test
  void shouldCreateForString() {
    SpinXmlElement xml = XML(EXAMPLE_XML);
    assertThat(xml).isNotNull();

    xml = S(EXAMPLE_XML, xmlDataFormat);
    assertThat(xml).isNotNull();

    xml = S(EXAMPLE_XML, DataFormats.XML_DATAFORMAT_NAME);
    assertThat(xml).isNotNull();

    xml = S(EXAMPLE_XML);
    assertThat(xml).isNotNull();
  }

  @Test
  void shouldCreateForReader() {
    SpinXmlElement xml = XML(stringAsReader(EXAMPLE_XML));
    assertThat(xml).isNotNull();

    xml = S(stringAsReader(EXAMPLE_XML), xmlDataFormat);
    assertThat(xml).isNotNull();

    xml = S(stringAsReader(EXAMPLE_XML), DataFormats.XML_DATAFORMAT_NAME);
    assertThat(xml).isNotNull();

    xml = S(stringAsReader(EXAMPLE_XML));
    assertThat(xml).isNotNull();
  }

  @Test
  void shouldBeIdempotent() {
    SpinXmlElement xml = XML(EXAMPLE_XML);
    assertThat(xml)
      .isEqualTo(XML(xml))
      .isEqualTo(S(xml, xmlDataFormat))
      .isEqualTo(S(xml, DataFormats.XML_DATAFORMAT_NAME))
      .isEqualTo(S(xml));
  }

  @Test
  void shouldFailForNull() {
    SpinXmlElement xmlTreeElement = null;

    assertThrows(IllegalArgumentException.class, () -> XML(xmlTreeElement));
    assertThrows(IllegalArgumentException.class, () -> S(xmlTreeElement, xmlDataFormat));
    assertThrows(IllegalArgumentException.class, () -> S(xmlTreeElement));

    Reader reader = null;

    assertThrows(IllegalArgumentException.class, () -> XML(reader));
    assertThrows(IllegalArgumentException.class, () -> S(reader, xmlDataFormat));
    assertThrows(IllegalArgumentException.class, () -> S(reader));

    String inputString = null;

    assertThrows(IllegalArgumentException.class, () -> XML(inputString));
    assertThrows(IllegalArgumentException.class, () -> S(inputString, xmlDataFormat));
    assertThrows(IllegalArgumentException.class, () -> S(inputString, DataFormats.XML_DATAFORMAT_NAME));
    assertThrows(IllegalArgumentException.class, () -> S(inputString));
  }

  @Test
  void shouldFailForInvalidxmlDataFormat () {
    assertThrows(SpinDataFormatException.class, () -> XML(EXAMPLE_INVALID_XML));
    assertThrows(SpinDataFormatException.class, () -> S(EXAMPLE_INVALID_XML, xmlDataFormat));
    assertThrows(SpinDataFormatException.class, () -> S(EXAMPLE_INVALID_XML, DataFormats.XML_DATAFORMAT_NAME));
    assertThrows(SpinDataFormatException.class, () -> S(EXAMPLE_INVALID_XML));
  }

  @Test
  void shouldFailForEmptyString() {
    assertThrows(SpinDataFormatException.class, () -> XML(EXAMPLE_EMPTY_STRING));
    assertThrows(SpinDataFormatException.class, () -> S(EXAMPLE_EMPTY_STRING, xmlDataFormat));
    assertThrows(SpinDataFormatException.class, () -> S(EXAMPLE_EMPTY_STRING, DataFormats.XML_DATAFORMAT_NAME));
    assertThrows(SpinDataFormatException.class, () -> S(EXAMPLE_EMPTY_STRING));
  }

  @Test
  void shouldFailForEmptyReader() {
    var reader1 = stringAsReader(EXAMPLE_EMPTY_STRING);
    var reader2 = stringAsReader(EXAMPLE_EMPTY_STRING);
    var reader3 = stringAsReader(EXAMPLE_EMPTY_STRING);
    assertThrows(SpinDataFormatException.class, () -> XML(reader1));
    assertThrows(SpinDataFormatException.class, () -> S(reader2, xmlDataFormat));
    assertThrows(SpinDataFormatException.class, () -> S(reader3));
  }
}
