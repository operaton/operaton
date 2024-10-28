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
import static org.assertj.core.api.Assertions.fail;
import static org.operaton.spin.DataFormats.xml;
import static org.operaton.spin.Spin.S;
import static org.operaton.spin.Spin.XML;
import static org.operaton.spin.impl.util.SpinIoUtil.stringAsReader;
import static org.operaton.spin.xml.XmlTestConstants.EXAMPLE_EMPTY_STRING;
import static org.operaton.spin.xml.XmlTestConstants.EXAMPLE_INVALID_XML;
import static org.operaton.spin.xml.XmlTestConstants.EXAMPLE_XML;

import java.io.Reader;

import org.junit.jupiter.api.Test;
import org.operaton.spin.DataFormats;
import org.operaton.spin.spi.SpinDataFormatException;
import org.operaton.spin.xml.SpinXmlElement;

/**
 * @author Daniel Meyer
 *
 */
class XmlDomCreateTest {

  @Test
  void shouldCreateForString() {
    SpinXmlElement xml = XML(EXAMPLE_XML);
    assertThat(xml).isNotNull();

    xml = S(EXAMPLE_XML, xml());
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

    xml = S(stringAsReader(EXAMPLE_XML), xml());
    assertThat(xml).isNotNull();

    xml = S(stringAsReader(EXAMPLE_XML), DataFormats.XML_DATAFORMAT_NAME);
    assertThat(xml).isNotNull();

    xml = S(stringAsReader(EXAMPLE_XML));
    assertThat(xml).isNotNull();
  }

  @Test
  void shouldBeIdempotent() {
    SpinXmlElement xml = XML(EXAMPLE_XML);
    assertThat(xml).isEqualTo(XML(xml));
    assertThat(xml).isEqualTo(S(xml, xml()));
    assertThat(xml).isEqualTo(S(xml, DataFormats.XML_DATAFORMAT_NAME));
    assertThat(xml).isEqualTo(S(xml));
  }

  @Test
  void shouldFailForNull() {
    SpinXmlElement xmlTreeElement = null;

    try {
      XML(xmlTreeElement);
      fail("Expected IllegalArgumentException");
    } catch(IllegalArgumentException e) {
      // expected
    }

    try {
      S(xmlTreeElement, xml());
      fail("Expected IllegalArgumentException");
    } catch(IllegalArgumentException e) {
      // expected
    }

    try {
      S(xmlTreeElement);
      fail("Expected IllegalArgumentException");
    } catch(IllegalArgumentException e) {
      // expected
    }

    Reader reader = null;

    try {
      XML(reader);
      fail("Expected IllegalArgumentException");
    } catch(IllegalArgumentException e) {
      // expected
    }

    try {
      S(reader, xml());
      fail("Expected IllegalArgumentException");
    } catch(IllegalArgumentException e) {
      // expected
    }

    try {
      S(reader);
      fail("Expected IllegalArgumentException");
    } catch(IllegalArgumentException e) {
      // expected
    }

    String inputString = null;

    try {
      XML(inputString);
      fail("Expected IllegalArgumentException");
    } catch(IllegalArgumentException e) {
      // expected
    }

    try {
      S(inputString, xml());
      fail("Expected IllegalArgumentException");
    } catch(IllegalArgumentException e) {
      // expected
    }

    try {
      S(inputString, DataFormats.XML_DATAFORMAT_NAME);
      fail("Expected IllegalArgumentException");
    } catch(IllegalArgumentException e) {
      // expected
    }

    try {
      S(inputString);
      fail("Expected IllegalArgumentException");
    } catch(IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  void shouldFailForInvalidXml() {
    try {
      XML(EXAMPLE_INVALID_XML);
      fail("Expected IllegalArgumentException");
    } catch(SpinDataFormatException e) {
      // expected
    }

    try {
      S(EXAMPLE_INVALID_XML, xml());
      fail("Expected IllegalArgumentException");
    } catch(SpinDataFormatException e) {
      // expected
    }

    try {
      S(EXAMPLE_INVALID_XML, DataFormats.XML_DATAFORMAT_NAME);
      fail("Expected IllegalArgumentException");
    } catch(SpinDataFormatException e) {
      // expected
    }

    try {
      S(EXAMPLE_INVALID_XML);
      fail("Expected IllegalArgumentException");
    } catch(SpinDataFormatException e) {
      // expected
    }
  }

  @Test
  void shouldFailForEmptyString() {
    try {
      XML(EXAMPLE_EMPTY_STRING);
      fail("Expected IllegalArgumentException");
    } catch(SpinDataFormatException e) {
      // expected
    }

    try {
      S(EXAMPLE_EMPTY_STRING, xml());
      fail("Expected IllegalArgumentException");
    } catch(SpinDataFormatException e) {
      // expected
    }

    try {
      S(EXAMPLE_EMPTY_STRING, DataFormats.XML_DATAFORMAT_NAME);
      fail("Expected IllegalArgumentException");
    } catch(SpinDataFormatException e) {
      // expected
    }

    try {
      S(EXAMPLE_EMPTY_STRING);
      fail("Expected IllegalArgumentException");
    } catch(SpinDataFormatException e) {
      // expected
    }
  }

  @Test
  void shouldFailForEmptyReader() {
    try {
      XML(stringAsReader(EXAMPLE_EMPTY_STRING));
      fail("Expected IllegalArgumentException");
    } catch(SpinDataFormatException e) {
      // expected
    }

    try {
      S(stringAsReader(EXAMPLE_EMPTY_STRING), xml());
      fail("Expected IllegalArgumentException");
    } catch(SpinDataFormatException e) {
      // expected
    }

    try {
      S(stringAsReader(EXAMPLE_EMPTY_STRING));
      fail("Expected IllegalArgumentException");
    } catch(SpinDataFormatException e) {
      // expected
    }
  }
}
