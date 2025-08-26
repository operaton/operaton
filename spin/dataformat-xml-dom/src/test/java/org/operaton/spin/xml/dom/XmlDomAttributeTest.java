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

import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.spin.xml.SpinXmlAttribute;
import org.operaton.spin.xml.SpinXmlAttributeException;
import org.operaton.spin.xml.SpinXmlElement;
import org.operaton.spin.xml.XmlTestConstants;

import static org.operaton.spin.Spin.XML;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Sebastian Menski
 */
class XmlDomAttributeTest {

  private SpinXmlAttribute attribute;

  @BeforeEach
  void getAttribute() {
    attribute = XML(XmlTestConstants.EXAMPLE_XML).attr("order");
  }

  @Test
  void getValue() {
    assertThat(attribute.value()).isEqualTo("order1");
  }

  @Test
  void getName() {
    assertThat(attribute.name()).isEqualTo("order");
  }

  @Test
  void getNamespace() {
    assertThat(attribute.namespace()).isNull();
  }

  @Test
  void hasNamespace() {
    assertThat(attribute.hasNamespace(null)).isTrue();
  }

  @Test
  void setValue() {
    assertThat(attribute.value("order2").value()).isEqualTo("order2");
  }

  @Test
  void setNullValue() {
    assertThrows(SpinXmlAttributeException.class, () ->
      attribute.value(null));
  }

  @Test
  void remove() {
    String namespace = attribute.namespace();
    String name = attribute.name();

    SpinXmlElement element = attribute.remove();
    assertThat(element.hasAttrNs(namespace, name)).isFalse();
  }

  // test io

  @Test
  void canWriteToString() {
    assertThat(attribute).hasToString("order1");
  }

  @Test
  void canWriteToWriter() {
    StringWriter writer = new StringWriter();
    attribute.writeToWriter(writer);
    String value = writer.toString();
    assertThat(value).isEqualTo("order1");
  }

}
