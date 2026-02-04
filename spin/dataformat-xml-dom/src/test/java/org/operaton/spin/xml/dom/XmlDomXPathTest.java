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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.operaton.spin.SpinList;
import org.operaton.spin.xml.SpinXPathException;
import org.operaton.spin.xml.SpinXPathQuery;
import org.operaton.spin.xml.SpinXmlAttribute;
import org.operaton.spin.xml.SpinXmlElement;

import static org.operaton.spin.Spin.S;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

/**
 * @author Sebastian Menski
 */
class XmlDomXPathTest {

  protected SpinXmlElement element;
  protected SpinXmlElement elementWithNamespace;
  protected SpinXmlElement elementWithDefaultNamespace;

  @BeforeEach
  void parseXml() {
    element = S("<root><child id=\"child\"><a id=\"a\"/><b id=\"b\"/><a id=\"c\"/></child></root>");
    elementWithNamespace = S("<root xmlns:bar=\"http://operaton.org\" xmlns:foo=\"http://operaton.com\"><foo:child id=\"child\"><bar:a id=\"a\"/><foo:b id=\"b\"/><a id=\"c\"/></foo:child></root>");
    elementWithDefaultNamespace = S("<root xmlns=\"http://operaton.com/example\" xmlns:bar=\"http://operaton.org\" xmlns:foo=\"http://operaton.com\"><foo:child id=\"child\"><bar:a id=\"a\"/><foo:b id=\"b\"/><a id=\"c\"/></foo:child></root>");
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
      "Document, /",
      "Attribute, /root/child/@id",
      "Non existing element, /root/nonExisting"
  })
  void canNotQueryAsElement(String name, String expression) {
    SpinXPathQuery pathQuery = element.xPath(expression);
    assertThatExceptionOfType(SpinXPathException.class).isThrownBy(pathQuery::element);
  }


  @ParameterizedTest(name = "{0}")
  @CsvSource({
      "Document, /",
      "Element, /root/child/",
      "Non existing attribute, /root/child/@nonExisting"
  })
  void canNotQueryAsAttribute(String name, String expression) {
    SpinXPathQuery pathQuery = element.xPath(expression);
    assertThatExceptionOfType(SpinXPathException.class).isThrownBy(pathQuery::attribute);
  }

  @Test
  void canNotQueryDocumentAsElementList() {
    SpinXPathQuery pathQuery = element.xPath("/");
    assertThatExceptionOfType(SpinXPathException.class).isThrownBy(pathQuery::elementList);
  }

  @Test
  void canNotQueryDocumentAsAttributeList() {
    SpinXPathQuery pathQuery = element.xPath("/");
    assertThatExceptionOfType(SpinXPathException.class).isThrownBy(pathQuery::attributeList);
  }

  @Test
  void canNotQueryDocumentAsString() {
    SpinXPathQuery pathQuery = element.xPath("/");
    assertThatExceptionOfType(SpinXPathException.class).isThrownBy(pathQuery::string);
  }

  @Test
  void canNotQueryDocumentAsNumber() {
    SpinXPathQuery pathQuery = element.xPath("/");
    assertThatExceptionOfType(SpinXPathException.class).isThrownBy(pathQuery::number);
  }

  @Test
  void canNotQueryDocumentAsBoolean() {
    SpinXPathQuery pathQuery = element.xPath("/");
    assertThatExceptionOfType(SpinXPathException.class).isThrownBy(pathQuery::bool);
  }

  @Test
  void canQueryElement() {
    SpinXmlElement child = element.xPath("/root/child").element();
    assertThat(child.name()).isEqualTo("child");
    assertThat(child.attr("id").value()).isEqualTo("child");

    SpinXmlElement b = child.xPath("b").element();
    assertThat(b.name()).isEqualTo("b");
    assertThat(b.attr("id").value()).isEqualTo("b");
  }

  @Test
  void canQueryElementList() {
    SpinList<SpinXmlElement> childs = element.xPath("/root/child/a").elementList();
    assertThat(childs).hasSize(2);
  }

  @Test
  void canNotQueryElementList() {
    SpinXPathQuery pathQuery = element.xPath("/root/child/nonExisting");
    assertThatExceptionOfType(SpinXPathException.class).isThrownBy(pathQuery::elementList);
  }

  @Test
  void canQueryAttribute() {
    SpinXmlAttribute attribute = element.xPath("/root/child/@id").attribute();
    assertThat(attribute.value()).isEqualTo("child");
  }

  @Test
  void canQueryAttributeList() {
    SpinList<SpinXmlAttribute> attributes = element.xPath("/root/child/a/@id").attributeList();
    assertThat(attributes).hasSize(2);
  }

  @Test
  void canNotQueryAttributeList() {
    SpinXPathQuery pathQuery = element.xPath("/root/child/a/@nonExisting");
    assertThatExceptionOfType(SpinXPathException.class).isThrownBy(pathQuery::attributeList);
  }

  @Test
  void canQueryString() {
    String value = element.xPath("string(/root/child/@id)").string();
    assertThat(value).isEqualTo("child");

    // can query not existing string
    value = element.xPath("string(/root/child/@nonExisting)").string();
    assertThat(value).isEmpty();

    // can query string as document
    value = element.xPath("string(/)").string();
    assertThat(value).isEmpty();
  }

  @Test
  void canQueryNumber() {
    Double count = element.xPath("count(/root/child/a)").number();
    assertThat(count).isEqualTo(2);

    // can query not existing number
    count = element.xPath("count(/root/child/nonExisting)").number();
    assertThat(count).isZero();

    // can query number as document
    count = element.xPath("count(/)").number();
    assertThat(count).isEqualTo(1);
  }

  @Test
  void canQueryBoolean() {
    Boolean exists = element.xPath("boolean(/root/child)").bool();
    assertThat(exists).isTrue();

    // can query not existing boolean
    exists = element.xPath("boolean(/root/nonExisting)").bool();
    assertThat(exists).isFalse();

    // can query boolean as document
    exists = element.xPath("boolean(/)").bool();
    assertThat(exists).isTrue();
  }

  @Test
  void canQueryElementWithNamespace() {
    SpinXmlElement child = elementWithNamespace.xPath("/root/a:child")
      .ns("a", "http://operaton.com")
      .element();

    assertThat(child.name()).isEqualTo("child");
    assertThat(child.namespace()).isEqualTo("http://operaton.com");
    assertThat(child.attr("id").value()).isEqualTo("child");
  }

  @Test
  void canQueryElementWithNamespaceMap() {
    Map<String, String> namespaces = new HashMap<>();
    namespaces.put("a", "http://operaton.com");
    namespaces.put("b", "http://operaton.org");

    SpinXmlElement child = elementWithNamespace.xPath("/root/a:child/b:a")
      .ns(namespaces)
      .element();

    assertThat(child.name()).isEqualTo("a");
    assertThat(child.namespace()).isEqualTo("http://operaton.org");
    assertThat(child.attr("id").value()).isEqualTo("a");
  }

  @Test
  void canQueryElementWithDefaultNamespace() {
    SpinXmlElement child = elementWithDefaultNamespace.xPath("/:root/a:child")
      .ns("a", "http://operaton.com")
      .element();

    assertThat(child.name()).isEqualTo("child");
    assertThat(child.namespace()).isEqualTo("http://operaton.com");
    assertThat(child.attr("id").value()).isEqualTo("child");
  }
}
