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

import org.operaton.spin.SpinList;
import org.operaton.spin.xml.SpinXmlAttribute;
import org.operaton.spin.xml.SpinXmlAttributeException;
import org.operaton.spin.xml.SpinXmlElement;
import org.operaton.spin.xml.SpinXmlElementException;
import static org.operaton.spin.Spin.S;
import static org.operaton.spin.Spin.XML;
import static org.operaton.spin.xml.XmlTestConstants.*;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Sebastian Menski
 */
class XmlDomElementTest {

  protected SpinXmlElement element;

  @BeforeEach
  void parseXml() {
    element = S(exampleXmlFileAsReader());
  }

  // has attribute

  @Test
  void canCheckAttributeByName() {
    boolean hasAttribute = element.hasAttr("order");
    assertThat(hasAttribute).isTrue();
  }

  @Test
  void canCheckAttributeByNonExistingName() {
    boolean hasAttribute = element.hasAttr(NON_EXISTING);
    assertThat(hasAttribute).isFalse();
  }

  @Test
  void cannotCheckAttributeByNullName() {
    assertThrows(IllegalArgumentException.class, () -> element.hasAttr(null));
  }

  @Test
  void canCheckAttributeByNamespaceAndName() {
    boolean hasAttribute = element.hasAttrNs(EXAMPLE_NAMESPACE, "order");
    assertThat(hasAttribute).isFalse();
  }

  @Test
  void canCheckAttributeByNamespaceAndNonExistingName() {
    boolean hasAttribute = element.hasAttrNs(EXAMPLE_NAMESPACE, NON_EXISTING);
    assertThat(hasAttribute).isFalse();
  }

  @Test
  void canCheckAttributeByNamespaceAndNullName() {
    assertThrows(IllegalArgumentException.class, () -> element.hasAttrNs(EXAMPLE_NAMESPACE, null));
  }

  @Test
  void canCheckAttributeByNonExistingNamespaceAndName() {
    boolean hasAttribute = element.hasAttrNs(NON_EXISTING, "order");
    assertThat(hasAttribute).isFalse();
  }

  @Test
  void canCheckAttributeByNullNamespaceAndName() {
    boolean hasAttribute = element.hasAttrNs(null, "order");
    assertThat(hasAttribute).isTrue();
  }

  // read attribute

  @Test
  void canReadAttributeByName() {
    SpinXmlAttribute attribute = element.attr("order");
    String value = attribute.value();
    assertThat(value).isEqualTo("order1");
  }

  @Test
  void cannotReadAttributeByNonExistingName() {
    assertThrows(SpinXmlAttributeException.class, () -> element.attr(NON_EXISTING));
  }

  @Test
  void cannotReadAttributeByNullName() {
    assertThrows(IllegalArgumentException.class, () -> element.attr(null));
  }

  @Test
  void canReadAttributeByNamespaceAndName() {
    SpinXmlAttribute attribute = element.attrNs(EXAMPLE_NAMESPACE, "dueUntil");
    String value = attribute.value();
    assertThat(value).isEqualTo("20150112");
  }

  @Test
  void canReadAttributeByNullNamespaceAndName() {
    SpinXmlAttribute attribute = element.attrNs(null, "order");
    String value = attribute.value();
    assertThat(value).isEqualTo("order1");
  }

  @Test
  void cannotReadAttributeByNonExistingNamespaceAndName() {
    assertThrows(SpinXmlAttributeException.class, () -> element.attrNs(NON_EXISTING, "order"));
  }

  @Test
  void cannotReadAttributeByNamespaceAndNonExistingName() {
    assertThrows(SpinXmlAttributeException.class, () -> element.attrNs(EXAMPLE_NAMESPACE, NON_EXISTING));
  }

  @Test
  void cannotReadAttributeByNamespaceAndNullName() {
    assertThrows(IllegalArgumentException.class, () -> element.attrNs(EXAMPLE_NAMESPACE, null));
  }

  @Test
  void cannotReadAttributeByNonExistingNamespaceAndNonExistingName() {
    assertThrows(SpinXmlAttributeException.class, () -> element.attrNs(NON_EXISTING, NON_EXISTING));
  }

  @Test
  void cannotReadAttributeByNullNamespaceAndNullName() {
    assertThrows(IllegalArgumentException.class, () -> element.attrNs(null, null));
  }

  // write attribute

  @Test
  void canWriteAttributeByName() {
    String newValue = element.attr("order", "order2").attr("order").value();
    assertThat(newValue).isEqualTo("order2");
  }

  @Test
  void canWriteAttributeByNonExistingName() {
    String newValue = element.attr(NON_EXISTING, "newValue").attr(NON_EXISTING).value();
    assertThat(newValue).isEqualTo("newValue");
  }

  @Test
  void cannotWriteAttributeByNullName() {
    assertThrows(IllegalArgumentException.class, () -> element.attr(null, NON_EXISTING));
  }

  @Test
  void canWriteAttributeByNameWithNullValue() {
    assertThrows(IllegalArgumentException.class, () -> element.attr("order", null));
  }

  @Test
  void canWriteAttributeByNamespaceAndName() {
    String newValue = element.attrNs(EXAMPLE_NAMESPACE, "order", "order2").attrNs(EXAMPLE_NAMESPACE, "order").value();
    assertThat(newValue).isEqualTo("order2");
  }

  @Test
  void canWriteAttributeByNamespaceAndNonExistingName() {
    String newValue = element.attrNs(EXAMPLE_NAMESPACE, NON_EXISTING, "newValue")
        .attrNs(EXAMPLE_NAMESPACE, NON_EXISTING)
        .value();
    assertThat(newValue).isEqualTo("newValue");
  }

  @Test
  void cannotWriteAttributeByNamespaceAndNullName() {
    assertThrows(IllegalArgumentException.class, () -> element.attrNs(EXAMPLE_NAMESPACE, null, "newValue"));
  }

  @Test
  void cannotWriteAttributeByNamespaceAndNameWithNullValue() {
    assertThrows(IllegalArgumentException.class, () -> element.attrNs(EXAMPLE_NAMESPACE, "order", null));
  }

  @Test
  void canWriteAttributeByNonExistingNamespaceAndName() {
    String newValue = element.attrNs(NON_EXISTING, "order", "newValue").attrNs(NON_EXISTING, "order").value();
    assertThat(newValue).isEqualTo("newValue");
  }

  @Test
  void canWriteAttributeByNullNamespaceAndName() {
    String newValue = element.attrNs(null, "order", "order2").attrNs(null, "order").value();
    assertThat(newValue).isEqualTo("order2");
  }

  // remove attribute

  @Test
  void canRemoveAttributeByName() {
    element.removeAttr("order");
    assertThat(element.hasAttr("order")).isFalse();
  }

  @Test
  void canRemoveAttributeByNonExistingName() {
    element.removeAttr(NON_EXISTING);
    assertThat(element.hasAttr(NON_EXISTING)).isFalse();
  }

  @Test
  void cannotRemoveAttributeByNullName() {
    assertThrows(IllegalArgumentException.class, () -> element.removeAttr(null));
  }

  @Test
  void canRemoveAttributeByNamespaceAndName() {
    element.removeAttrNs(EXAMPLE_NAMESPACE, "order");
    assertThat(element.hasAttrNs(EXAMPLE_NAMESPACE, "order")).isFalse();
  }

  @Test
  void canRemoveAttributeByNullNamespaceAndName() {
    element.removeAttrNs(null, "order");
    assertThat(element.hasAttrNs(null, "order")).isFalse();
  }

  @Test
  void cannotRemoveAttributeByNamespaceAndNullName() {
    assertThrows(IllegalArgumentException.class, () -> element.removeAttrNs(EXAMPLE_NAMESPACE, null));
  }

  @Test
  void canRemoveAttributeByNonExistingNamespaceAndName() {
    element.removeAttrNs(NON_EXISTING, "order");
    assertThat(element.hasAttrNs(NON_EXISTING, "order")).isFalse();
  }

  // get attributes

  @Test
  void canGetAllAttributes() {
    SpinList<SpinXmlAttribute> attributes = element.attrs();
    assertThat(attributes).hasSize(4);
    for (SpinXmlAttribute attribute : attributes) {
      assertThat(attribute.name()).isIn("order", "dueUntil", "xmlns", "ex");
    }
  }

  @Test
  void canGetAllAttributesByNamespace() {
    SpinList<SpinXmlAttribute> attributes = element.attrs(EXAMPLE_NAMESPACE);
    for (SpinXmlAttribute attribute : attributes) {
      assertThat(attribute.name()).isIn("order", "dueUntil");
      assertThat(attribute.value()).isIn("order1", "20150112");
      assertThat(attribute.namespace()).isEqualTo(EXAMPLE_NAMESPACE);
    }
  }

  @Test
  void canGetAllAttributesByNullNamespace() {
    SpinList<SpinXmlAttribute> attributes = element.attrs(null);
    for (SpinXmlAttribute attribute : attributes) {
      assertThat(attribute.name()).isIn("order", "dueUntil");
      assertThat(attribute.value()).isIn("order1", "20150112");
      assertThat(attribute.namespace()).isNull();
    }
  }

  @Test
  void canGetAllAttributesByNonExistingNamespace() {
    SpinList<SpinXmlAttribute> attributes = element.attrs(NON_EXISTING);
    assertThat(attributes).isEmpty();
  }

  // get attribute names

  @Test
  void canGetAllAttributeNames() {
    List<String> names = element.attrNames();
    assertThat(names).containsOnly("order", "dueUntil", "xmlns", "ex");
  }

  @Test
  void canGetAllAttributeNamesByNamespace() {
    List<String> names = element.attrNames(EXAMPLE_NAMESPACE);
    assertThat(names).containsOnly("dueUntil");
  }

  @Test
  void canGetAllAttributeNamesByNullNamespace() {
    List<String> names = element.attrNames(null);
    assertThat(names).containsOnly("order");
  }

  @Test
  void canGetAllAttributeNamesByNonExistingNamespace() {
    List<String> names = element.attrNames(NON_EXISTING);
    assertThat(names).isEmpty();
  }

  // get child element

  @Test
  void canGetSingleChildElementByName() {
    SpinXmlElement childElement = element.childElement("date");
    assertThat(childElement).isNotNull();
    assertThat(childElement.attr("name").value()).isEqualTo("20140512");
  }

  @Test
  void cannotGetSingleChildElementByNonExistingName() {
    assertThrows(SpinXmlElementException.class, () -> element.childElement(NON_EXISTING));
  }

  @Test
  void cannotGetSingleChildElementByNullName() {
    assertThrows(IllegalArgumentException.class, () -> element.childElement(null));
  }

  @Test
  void canGetSingleChildElementByNamespaceAndName() {
    SpinXmlElement childElement = element.childElement(EXAMPLE_NAMESPACE, "date");
    assertThat(childElement).isNotNull();
    assertThat(childElement.attr("name").value()).isEqualTo("20140512");
  }

  @Test
  void canGetSingleChildElementByNullNamespaceAndName() {
    SpinXmlElement childElement = element.childElement(null, "file");
    assertThat(childElement).isNotNull();
  }

  @Test
  void cannotGetChildElementByNamespaceAndNonExistingName() {
    assertThrows(SpinXmlElementException.class, () -> element.childElement(EXAMPLE_NAMESPACE, NON_EXISTING));
  }

  @Test
  void cannotGetChildElementByNamespaceAndNullName() {
    assertThrows(IllegalArgumentException.class, () -> element.childElement(EXAMPLE_NAMESPACE, null));
  }

  @Test
  void cannotGetChildElementByNonExistingNamespaceAndName() {
    assertThrows(SpinXmlElementException.class, () -> element.childElement(NON_EXISTING, "date"));
  }

  @Test
  void cannotGetChildElementByNonExistingNamespaceAndNonExistingName() {
    assertThrows(SpinXmlElementException.class, () -> element.childElement(NON_EXISTING, NON_EXISTING));
  }

  @Test
  void cannotGetChildElementByNullNamespaceAndNullName() {
    assertThrows(IllegalArgumentException.class, () -> element.childElement(null, null));
  }

  // append child element

  @Test
  void canAppendChildElement() {
    SpinXmlElement child = XML("<child/>");
    element = element.append(child);

    child.attr("id", "child");
    child = element.childElement(null, "child");

    assertThat(child).isNotNull();
    assertThat(child.attr("id").value()).isEqualTo("child");
  }

  @Test
  void canAppendChildElementWithNamespace() {
    SpinXmlElement child = XML("<child xmlns=\"" + EXAMPLE_NAMESPACE + "\"/>");
    element = element.append(child);

    child.attr("id", "child");
    child = element.childElement(EXAMPLE_NAMESPACE, "child");

    assertThat(child).isNotNull();
    assertThat(child.attr("id").value()).isEqualTo("child");
  }

  @Test
  void canAppendMultipleChildElements() {
    SpinXmlElement child1 = XML("<child/>");
    SpinXmlElement child2 = XML("<child/>");
    SpinXmlElement child3 = XML("<child/>");

    element = element.append(child1, child2, child3);

    child1.attr("id", "child");
    child2.attr("id", "child");
    child3.attr("id", "child");

    SpinList<SpinXmlElement> childs = element.childElements(null, "child");
    assertThat(childs).hasSize(3);

    for (SpinXmlElement childElement : childs) {
      assertThat(childElement).isNotNull();
      assertThat(childElement.attr("id").value()).isEqualTo("child");
    }
  }

  @Test
  void canAppendChildElementCollection() {
    Collection<SpinXmlElement> childElements = new ArrayList<>();
    childElements.add(XML("<child/>"));
    childElements.add(XML("<child/>"));
    childElements.add(XML("<child/>"));

    element = element.append(childElements);

    SpinList<SpinXmlElement> childs = element.childElements(null, "child");
    assertThat(childs).hasSize(3);
  }

  @Test
  void cannotAppendNullChildElements() {
    assertThrows(IllegalArgumentException.class, () -> element.append((SpinXmlElement[]) null));
  }

  @Test
  void cannotAppendNullChildElement() {
    SpinXmlElement child = XML("<child/>");
    assertThrows(IllegalArgumentException.class, () -> element.append(child, null));
  }

  @Test
  void canAppendChildElementBeforeExistingElement() {
    SpinXmlElement child = XML("<child/>");
    SpinXmlElement date = element.childElement("date");
    element.appendBefore(child, date);
    SpinXmlElement insertedElement = element.childElements().get(0);
    assertThat(insertedElement.name()).isEqualTo("child");
  }

  @Test
  void cannotAppendChildElementBeforeNonChildElement() {
    SpinXmlElement child = XML("<child/>");
    assertThrows(SpinXmlElementException.class, () -> element.appendBefore(child, child));
  }

  @Test
  void canAppendChildElementAfterExistingElement() {
    SpinXmlElement child = XML("<child/>");
    SpinXmlElement date = element.childElement("date");
    element.appendAfter(child, date);
    SpinXmlElement insertedElement = element.childElements().get(1);
    assertThat(insertedElement.name()).isEqualTo("child");
  }

  @Test
  void canAppendChildElementAfterLastChildElement() {
    SpinXmlElement child = XML("<child/>");
    int childCount = element.childElements().size();
    SpinXmlElement lastChildElement = element.childElements().get(childCount - 1);
    element.appendAfter(child, lastChildElement);
    SpinXmlElement insertedElement = element.childElements().get(childCount);
    assertThat(insertedElement.name()).isEqualTo("child");
  }

  @Test
  void cannotAppendChildElementAfterNonChildElement() {
    SpinXmlElement child = XML("<child/>");
    assertThrows(SpinXmlElementException.class, () -> element.appendAfter(child, child));
  }

  // remove child elements

  @Test
  void canRemoveAChildElement() {
    SpinXmlElement child = XML("<child/>");
    element.append(child);

    assertThat(element.childElement(null, "child")).isNotNull();

    element.remove(child);

    assertThrows(SpinXmlElementException.class, () -> element.childElement(null, "child"));
  }

  @Test
  void cannotRemoveANullChildElement() {
    SpinXmlElement child = XML("<child/>");
    element.append(child);
    assertThrows(IllegalArgumentException.class, () -> element.remove(child, null));
  }

  @Test
  void cannotRemoveNonChildElement() {
    SpinXmlElement child1 = XML("<child/>");
    SpinXmlElement child2 = XML("<child/>");
    element.append(child1);
    assertThrows(SpinXmlElementException.class, () -> element.remove(child1, child2));
  }

  @Test
  void canRemoveMultipleChildElements() {
    SpinXmlElement child1 = XML("<child/>");
    SpinXmlElement child2 = XML("<child/>");
    SpinXmlElement child3 = XML("<child/>");
    element.append(child1, child2, child3);

    assertThat(element.childElements(null, "child")).hasSize(3);

    element.remove(child1, child2, child3);

    assertThrows(SpinXmlElementException.class, () -> element.childElements(null, "child"));
  }

  @Test
  void canRemoveChildElementCollection() {
    SpinXmlElement child1 = XML("<child/>");
    SpinXmlElement child2 = XML("<child/>");
    SpinXmlElement child3 = XML("<child/>");
    element.append(child1, child2, child3);

    assertThat(element.childElements(null, "child")).hasSize(3);

    element.remove(element.childElements(null, "child"));

    assertThrows(SpinXmlElementException.class, () -> element.childElements(null, "child"));
  }

  @Test
  void cannotRemoveNullChildElements() {
    assertThrows(IllegalArgumentException.class, () -> element.remove((SpinXmlElement[]) null));
  }

  // get child elements

  @Test
  void canGetAllChildElements() {
    SpinList<SpinXmlElement> childElements = element.childElements();
    assertThat(childElements).hasSize(7);
  }

  @Test
  void canGetAllChildElementsByName() {
    SpinList<SpinXmlElement> childElements = element.childElements("customer");
    assertThat(childElements).hasSize(3);
  }

  @Test
  void cannotGetAllChildElementsByNonExistingName() {
    assertThrows(SpinXmlElementException.class, () -> element.childElements(NON_EXISTING));
  }

  @Test
  void cannotGetAllChildElementsByNullName() {
    assertThrows(IllegalArgumentException.class, () -> element.childElements(null));
  }

  @Test
  void canGetAllChildElementsByNamespaceAndName() {
    SpinList<SpinXmlElement> childElements = element.childElements(EXAMPLE_NAMESPACE, "customer");
    assertThat(childElements).hasSize(3);
  }

  @Test
  void canGetAllChildElementsByNullNamespaceAndName() {
    SpinList<SpinXmlElement> childElements = element.childElements(null, "info");
    assertThat(childElements).hasSize(2);
  }

  @Test
  void cannotGetAllChildElementsByNonExistingNamespaceAndName() {
    assertThrows(SpinXmlElementException.class, () -> element.childElements(NON_EXISTING, "customer"));
  }

  @Test
  void cannotGetAllChildElementsByNamespaceAndNonExistingName() {
    assertThrows(SpinXmlElementException.class, () -> element.childElements(EXAMPLE_NAMESPACE, NON_EXISTING));
  }

  @Test
  void cannotGetAllChildElementsByNamespaceAndNullName() {
    assertThrows(IllegalArgumentException.class, () -> element.childElements(EXAMPLE_NAMESPACE, null));
  }

  @Test
  void cannotGetAllChildElementsByNonExistingNamespaceAndNonExistingName() {
    assertThrows(SpinXmlElementException.class, () -> element.childElements(NON_EXISTING, NON_EXISTING));
  }

  @Test
  void cannotGetAllChildElementsByNullNamespaceAndNullName() {
    assertThrows(IllegalArgumentException.class, () -> element.childElements(null, null));
  }

  // replace child element

  @Test
  void canReplaceAChildElement() {
    SpinXmlElement child = XML("<child/>");
    SpinXmlElement date = element.childElement("date");
    assertThat(date).isNotNull();

    element.replaceChild(date, child);

    assertThat(element.childElement(null, "child")).isNotNull();
    assertThrows(SpinXmlElementException.class, () -> element.childElement("date"));
  }

  @Test
  void cannotReplaceANullChildElement() {
    SpinXmlElement child = XML("<child/>");
    assertThrows(IllegalArgumentException.class, () -> element.replaceChild(null, child));
  }

  @Test
  void cannotReplaceByANullChildElement() {
    SpinXmlElement date = element.childElement("date");
    assertThrows(IllegalArgumentException.class, () -> element.replaceChild(date, null));
  }

  @Test
  void cannotReplaceANonChildElement() {
    SpinXmlElement child = XML("<child/>");
    SpinXmlElement nonChild = XML("<child/>");
    assertThrows(SpinXmlElementException.class, () -> element.replaceChild(nonChild, child));
  }

  // replace element

  @Test
  void canReplaceAElement() {
    SpinXmlElement child = XML("<child/>");
    SpinXmlElement date = element.childElement("date");
    assertThat(date).isNotNull();

    date.replace(child);

    assertThat(element.childElement(null, "child")).isNotNull();
    assertThrows(SpinXmlElementException.class, () -> element.childElement("date"));
  }

  @Test
  void canReplaceRootElement() {
    SpinXmlElement root = XML("<root/>");
    assertThat(element.name()).isEqualTo("customers");
    assertThat(element.childElements()).isNotEmpty();
    element = element.replace(root);
    assertThat(element.name()).isEqualTo("root");
    assertThat(element.childElements()).isEmpty();
  }

  @Test
  void cannotReplaceByNullElement() {
    assertThrows(IllegalArgumentException.class, () -> element.replace(null));
  }

  // test io

  @Test
  void canWriteToString() {
    assertThat(element.toString()).isXmlEqualTo(EXAMPLE_XML);
  }

  @Test
  void canWriteToWriter() {
    StringWriter writer = new StringWriter();
    element.writeToWriter(writer);
    String value = writer.toString();
    assertThat(value).isXmlEqualTo(EXAMPLE_XML);
  }

  // text content

  @Test
  void canReadTextContent() {
    assertThat(XML("<customer>Foo</customer>").textContent()).isEqualTo("Foo");
  }

  @Test
  void canReadEmptyTextContent() {
    assertThat(XML("<customer/>").textContent()).isEmpty();
  }

  @Test
  void canWriteTextContent() {
    assertThat(XML("<customer/>").textContent("Foo").textContent()).isEqualTo("Foo");
  }

  @Test
  void canWriteEmptyTextContent() {
    assertThat(XML("<customer>Foo</customer>").textContent("").textContent()).isEmpty();
  }

  @Test
  void cannotWriteNullTextContent() {
    SpinXmlElement xml = XML("<customer/>");
    assertThrows(IllegalArgumentException.class, () -> xml.textContent(null));
  }

}
