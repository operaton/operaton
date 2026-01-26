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
import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.spin.SpinList;
import org.operaton.spin.impl.test.Script;
import org.operaton.spin.impl.test.ScriptTest;
import org.operaton.spin.impl.test.ScriptVariable;
import org.operaton.spin.xml.SpinXmlAttribute;
import org.operaton.spin.xml.SpinXmlAttributeException;
import org.operaton.spin.xml.SpinXmlElement;
import org.operaton.spin.xml.SpinXmlElementException;

import static org.operaton.spin.Spin.S;
import static org.operaton.spin.Spin.XML;
import static org.operaton.spin.xml.XmlTestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

/**
 * @author Sebastian Menski
 */
@SuppressWarnings({"java:S4144", "java:S5976"}) // tests are different by expression in @Script annotation
public abstract class XmlDomElementScriptTest extends ScriptTest {

  // has attribute

  @Test
  @Script(name = "XmlDomElementScriptTest.checkAttributeByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME), @ScriptVariable(name = "name", value = "order") })
  public void canCheckAttributeByName() {
    Boolean hasAttribute = script.getVariable("hasAttribute");
    assertThat(hasAttribute).isTrue();
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.checkAttributeByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "name", value = NON_EXISTING) })
  public void canCheckAttributeByNonExistingName() {
    Boolean hasAttribute = script.getVariable("hasAttribute");
    assertThat(hasAttribute).isFalse();
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.checkAttributeByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "name", isNull = true) }, execute = false)
  public void canCheckAttributeByNullName() {
    assertThatIllegalArgumentException().isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.checkAttributeByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = EXAMPLE_NAMESPACE),
      @ScriptVariable(name = "name", value = "dueUntil") })
  public void canCheckAttributeByNamespaceAndName() {
    Boolean hasAttribute = script.getVariable("hasAttribute");
    assertThat(hasAttribute).isTrue();
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.checkAttributeByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = NON_EXISTING), @ScriptVariable(name = "name", value = "order") })
  public void canCheckAttributeByNonExistingNamespaceAndName() {
    Boolean hasAttribute = script.getVariable("hasAttribute");
    assertThat(hasAttribute).isFalse();
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.checkAttributeByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = EXAMPLE_NAMESPACE),
      @ScriptVariable(name = "name", isNull = true) }, execute = false)
  public void canCheckAttributeByNamespaceAndNullName() {
    assertThatIllegalArgumentException().isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.checkAttributeByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = EXAMPLE_NAMESPACE),
      @ScriptVariable(name = "name", value = NON_EXISTING) })
  public void canCheckAttributeByNamespaceAndNonExistingName() {
    Boolean hasAttribute = script.getVariable("hasAttribute");
    assertThat(hasAttribute).isFalse();
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.checkAttributeByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME), @ScriptVariable(name = "namespace", isNull = true),
      @ScriptVariable(name = "name", value = "order") })
  public void canCheckAttributeByNullNamespaceAndName() {
    Boolean hasAttribute = script.getVariable("hasAttribute");
    assertThat(hasAttribute).isTrue();
  }

  // read attribute

  @Test
  @Script(name = "XmlDomElementScriptTest.readAttributeValueByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "name", value = "order") }, execute = false)
  public void canReadAttributeByName() throws Exception {
    script.setVariable("variables", new HashMap<String, Object>());
    script.execute();
    String value = script.getVariable("value");
    assertThat(value).isEqualTo("order1");
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.readAttributeValueByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "name", value = NON_EXISTING) }, execute = false)
  public void cannotReadAttributeByNonExistingName() {
    assertThatExceptionOfType(SpinXmlAttributeException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.readAttributeValueByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "name", isNull = true) }, execute = false)
  public void cannotReadAttributeByNullName() {
    assertThatIllegalArgumentException().isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.readAttributeValueByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = EXAMPLE_NAMESPACE),
      @ScriptVariable(name = "name", value = "dueUntil") })
  public void canReadAttributeByNamespaceAndName() {
    String value = script.getVariable("value");
    assertThat(value).isEqualTo("20150112");
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.readAttributeValueByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME), @ScriptVariable(name = "namespace", isNull = true),
      @ScriptVariable(name = "name", value = "order") })
  public void canReadAttributeByNullNamespaceAndName() {
    String value = script.getVariable("value");
    assertThat(value).isEqualTo("order1");
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.readAttributeValueByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = NON_EXISTING),
      @ScriptVariable(name = "name", value = "order") }, execute = false)
  public void cannotReadAttributeByNonExistingNamespaceAndName() {
    assertThatExceptionOfType(SpinXmlAttributeException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.readAttributeValueByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = EXAMPLE_NAMESPACE),
      @ScriptVariable(name = "name", value = NON_EXISTING) }, execute = false)
  public void cannotReadAttributeByNamespaceAndNonExistingName() {
    assertThatExceptionOfType(SpinXmlAttributeException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.readAttributeValueByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = EXAMPLE_NAMESPACE),
      @ScriptVariable(name = "name", isNull = true) }, execute = false)
  public void cannotReadAttributeByNamespaceAndNullName() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.readAttributeValueByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = NON_EXISTING),
      @ScriptVariable(name = "name", value = NON_EXISTING) }, execute = false)
  public void cannotReadAttributeByNonExistingNamespaceAndNonExistingName() {
    assertThatExceptionOfType(SpinXmlAttributeException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.readAttributeValueByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME), @ScriptVariable(name = "namespace", isNull = true),
      @ScriptVariable(name = "name", isNull = true) }, execute = false)
  public void cannotReadAttributeByNullNamespaceAndNullName() {
    assertThatIllegalArgumentException().isThrownBy(this::failingWithException);
  }

  // write attribute

  @Test
  @Script(name = "XmlDomElementScriptTest.writeAttributeValueByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME), @ScriptVariable(name = "name", value = "order"),
      @ScriptVariable(name = "value", value = "order2") })
  public void canWriteAttributeByName() {
    String newValue = script.getVariable("newValue");
    assertThat(newValue).isEqualTo("order2");
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.writeAttributeValueByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "name", value = NON_EXISTING), @ScriptVariable(name = "value", value = "newValue") })
  public void canWriteAttributeByNonExistingName() {
    String newValue = script.getVariable("newValue");
    assertThat(newValue).isEqualTo("newValue");
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.writeAttributeValueByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME), @ScriptVariable(name = "name", isNull = true),
      @ScriptVariable(name = "value", value = "order2") }, execute = false)
  public void canWriteAttributeByNullName() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.writeAttributeValueByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME), @ScriptVariable(name = "name", value = "order"),
      @ScriptVariable(name = "value", isNull = true) }, execute = false)
  public void cannotWriteAttributeByNameWithNullValue() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.writeAttributeValueByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = EXAMPLE_NAMESPACE), @ScriptVariable(name = "name", value = "order"),
      @ScriptVariable(name = "value", value = "order2") })
  public void canWriteAttributeByNamespaceAndName() {
    String newValue = script.getVariable("newValue");
    assertThat(newValue).isEqualTo("order2");
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.writeAttributeValueByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = EXAMPLE_NAMESPACE),
      @ScriptVariable(name = "name", value = NON_EXISTING), @ScriptVariable(name = "value", value = "order2") })
  public void canWriteAttributeByNamespaceAndNonExistingName() {
    String newValue = script.getVariable("newValue");
    assertThat(newValue).isEqualTo("order2");
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.writeAttributeValueByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = EXAMPLE_NAMESPACE), @ScriptVariable(name = "name", isNull = true),
      @ScriptVariable(name = "value", value = "order2") }, execute = false)
  public void canWriteAttributeByNamespaceAndNullName() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.writeAttributeValueByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = EXAMPLE_NAMESPACE), @ScriptVariable(name = "name", value = "order"),
      @ScriptVariable(name = "value", isNull = true) }, execute = false)
  public void canWriteAttributeByNamespaceAndNameWithNullValue() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.writeAttributeValueByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME), @ScriptVariable(name = "namespace", isNull = true),
      @ScriptVariable(name = "name", value = "order"), @ScriptVariable(name = "value", value = "order2") })
  public void canWriteAttributeByNullNamespaceAndName() {
    String newValue = script.getVariable("newValue");
    assertThat(newValue).isEqualTo("order2");
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.writeAttributeValueByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = NON_EXISTING), @ScriptVariable(name = "name", value = "order"),
      @ScriptVariable(name = "value", value = "order2") })
  public void canWriteAttributeByNonExistingNamespaceAndName() {
    String newValue = script.getVariable("newValue");
    assertThat(newValue).isEqualTo("order2");
  }

  // remove attribute

  @Test
  @Script(name = "XmlDomElementScriptTest.removeAttributeByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME), @ScriptVariable(name = "name", value = "order") })
  public void canRemoveAttributeByName() {
    SpinXmlElement element = script.getVariable("element");
    assertThat(element.hasAttr("order")).isFalse();
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.removeAttributeByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "name", value = NON_EXISTING) })
  public void canRemoveAttributeByNonExistingName() {
    SpinXmlElement element = script.getVariable("element");
    assertThat(element.hasAttr(NON_EXISTING)).isFalse();
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.removeAttributeByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "name", isNull = true) }, execute = false)
  public void cannotRemoveAttributeByNullName() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.removeAttributeByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = EXAMPLE_NAMESPACE), @ScriptVariable(name = "name", value = "order") })
  public void canRemoveAttributeByNamespaceAndName() {
    SpinXmlElement element = script.getVariable("element");
    assertThat(element.hasAttrNs(EXAMPLE_NAMESPACE, "order")).isFalse();
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.removeAttributeByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME), @ScriptVariable(name = "namespace", isNull = true),
      @ScriptVariable(name = "name", value = "order") })
  public void canRemoveAttributeByNullNamespaceAndName() {
    SpinXmlElement element = script.getVariable("element");
    assertThat(element.hasAttrNs(null, "order")).isFalse();
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.removeAttributeByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = EXAMPLE_NAMESPACE),
      @ScriptVariable(name = "name", isNull = true) }, execute = false)
  public void canRemoveAttributeByNamespaceAndNullName() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.removeAttributeByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = NON_EXISTING), @ScriptVariable(name = "name", value = "order") })
  public void canRemoveAttributeByNonExistingNamespaceAndName() {
    SpinXmlElement element = script.getVariable("element");
    assertThat(element.hasAttrNs(NON_EXISTING, "order")).isFalse();
  }

  // get attributes

  @Test
  @Script("XmlDomElementScriptTest.getAllAttributesAndNames")
  @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME)
  public void canGetAllAttributes() {
    SpinList<SpinXmlAttribute> attributes = script.getVariable("attributes");
    for (SpinXmlAttribute attribute : attributes) {
      assertThat(attribute.name()).isIn("order", "dueUntil", "xmlns", "ex");
    }
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getAllAttributesAndNamesByNamespace", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = EXAMPLE_NAMESPACE) })
  public void canGetAllAttributesByNamespace() {
    SpinList<SpinXmlAttribute> attributes = script.getVariable("attributes");
    for (SpinXmlAttribute attribute : attributes) {
      assertThat(attribute.name()).isIn("order", "dueUntil");
      assertThat(attribute.value()).isIn("order1", "20150112");
      assertThat(attribute.namespace()).isEqualTo(EXAMPLE_NAMESPACE);
    }
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getAllAttributesAndNamesByNamespace", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", isNull = true) })
  public void canGetAllAttributesByNullNamespace() {
    SpinList<SpinXmlAttribute> attributes = script.getVariable("attributes");
    for (SpinXmlAttribute attribute : attributes) {
      assertThat(attribute.name()).isIn("order", "dueUntil");
      assertThat(attribute.value()).isIn("order1", "20150112");
      assertThat(attribute.namespace()).isNull();
    }
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getAllAttributesAndNamesByNamespace", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = NON_EXISTING) })
  public void canGetAllAttributesByNonExistingNamespace() {
    SpinList<SpinXmlAttribute> attributes = script.getVariable("attributes");
    assertThat(attributes).isEmpty();
  }

  @Test
  @Script("XmlDomElementScriptTest.getAllAttributesAndNames")
  @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME)
  public void canGetAllAttributeNames() {
    List<String> names = script.getVariable("names");
    assertThat(names).containsOnly("order", "dueUntil", "xmlns", "ex");
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getAllAttributesAndNamesByNamespace", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = EXAMPLE_NAMESPACE) })
  public void canGetAllAttributeNamesByNamespace() {
    List<String> names = script.getVariable("names");
    assertThat(names).containsOnly("dueUntil");
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getAllAttributesAndNamesByNamespace", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", isNull = true) })
  public void canGetAllAttributeNamesByNullNamespace() {
    List<String> names = script.getVariable("names");
    assertThat(names).containsOnly("order");
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getAllAttributesAndNamesByNamespace", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = NON_EXISTING) })
  public void canGetAllAttributeNamesByNonExistingNamespace() {
    List<String> names = script.getVariable("names");
    assertThat(names).isEmpty();
  }

  // get child element

  @Test
  @Script(name = "XmlDomElementScriptTest.getChildElementByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME), @ScriptVariable(name = "name", value = "date") })
  public void canGetSingleChildElementByName() {
    SpinXmlElement childElement = script.getVariable("childElement");
    assertThat(childElement).isNotNull();
    assertThat(childElement.attr("name").value()).isEqualTo("20140512");
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getChildElementByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "name", value = NON_EXISTING) }, execute = false)
  public void cannotGetSingleChildElementByNonExistingName() {
    assertThatExceptionOfType(SpinXmlElementException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getChildElementByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "name", isNull = true) }, execute = false)
  public void cannotGetSingleChildElementByNullName() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getChildElementByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = EXAMPLE_NAMESPACE), @ScriptVariable(name = "name", value = "date") })
  public void canGetSingleChildElementByNamespaceAndName() {
    SpinXmlElement childElement = script.getVariable("childElement");
    assertThat(childElement).isNotNull();
    assertThat(childElement.attr("name").value()).isEqualTo("20140512");
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getChildElementByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME), @ScriptVariable(name = "namespace", isNull = true),
      @ScriptVariable(name = "name", value = "file") })
  public void canGetSingleChildElementByNullNamespaceAndName() {
    SpinXmlElement childElement = script.getVariable("childElement");
    assertThat(childElement).isNotNull();
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getChildElementByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = EXAMPLE_NAMESPACE),
      @ScriptVariable(name = "name", value = NON_EXISTING) }, execute = false)
  public void cannotGetChildElementByNamespaceAndNonExistingName() {
    assertThatExceptionOfType(SpinXmlElementException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getChildElementByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = EXAMPLE_NAMESPACE),
      @ScriptVariable(name = "name", isNull = true) }, execute = false)
  public void cannotGetChildElementByNamespaceAndNullName() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getChildElementByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = NON_EXISTING),
      @ScriptVariable(name = "name", value = "date") }, execute = false)
  public void cannotGetChildElementByNonExistingNamespaceAndName() {
    assertThatExceptionOfType(SpinXmlElementException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getChildElementByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = NON_EXISTING),
      @ScriptVariable(name = "name", value = NON_EXISTING) }, execute = false)
  public void cannotGetChildElementByNonExistingNamespaceAndNonExistingName() {
    assertThatExceptionOfType(SpinXmlElementException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getChildElementByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME), @ScriptVariable(name = "namespace", isNull = true),
      @ScriptVariable(name = "name", isNull = true) }, execute = false)
  public void cannotGetChildElementByNullNamespaceAndNullName() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(this::failingWithException);
  }

  // append child element

  @Test
  @Script(name = "XmlDomElementScriptTest.appendChildElement", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "child", value = "<child/>") })
  public void canAppendChildElement() {
    SpinXmlElement element = script.getVariable("element");

    SpinXmlElement child = element.childElement(null, "child");
    assertThat(child).isNotNull();
    assertThat(child.attr("id").value()).isEqualTo("child");
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.appendChildElement", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "child", value = "<child xmlns=\"" + EXAMPLE_NAMESPACE + "\"/>") })
  public void canAppendChildElementWithNamespace() {
    SpinXmlElement element = script.getVariable("element");

    SpinXmlElement child = element.childElement(EXAMPLE_NAMESPACE, "child");
    assertThat(child).isNotNull();
    assertThat(child.attr("id").value()).isEqualTo("child");
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.appendChildElement", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "child", isNull = true) }, execute = false)
  public void cannotAppendNullChildElement() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.appendChildElementAtPosition", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "child", value = "<child/>") })
  public void canAppendChildElementAtPosition() {
    SpinXmlElement element = script.getVariable("element");

    SpinList<SpinXmlElement> childs = element.childElements();

    assertThat(childs.get(0).name()).isEqualTo("child");
    assertThat(childs.get(2).name()).isEqualTo("child");
    assertThat(childs.get(childs.size() - 1).name()).isEqualTo("child");
  }

  // remove child elements

  @Test
  @Script(name = "XmlDomElementScriptTest.removeChildElement", variables = {
      @ScriptVariable(name = "input", isNull = true),
      @ScriptVariable(name = "child2", isNull = true) }, execute = false)
  public void canRemoveAChildElement() throws Exception {
    SpinXmlElement element = XML(exampleXmlFileAsReader());
    SpinXmlElement child = XML("<child/>");
    element.append(child);

    SpinXmlElement finalElement = script.setVariable("element", element)
        .setVariable("child", child)
        .execute()
        .getVariable("element");

    assertThatExceptionOfType(SpinXmlElementException.class).isThrownBy(() -> finalElement.childElement(null, "child"));
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.removeChildElement", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME), @ScriptVariable(name = "child", isNull = true),
      @ScriptVariable(name = "child2", isNull = true) }, execute = false)
  public void cannotRemoveANullChildElement() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.removeChildElement", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "child2", isNull = true) }, execute = false)
  public void cannotRemoveANonChildElement() {
    script.setVariable("child", S("<child/>"));
    assertThatExceptionOfType(SpinXmlElementException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.removeChildElement", variables = {
      @ScriptVariable(name = "input", isNull = true) }, execute = false)
  public void canRemoveMultipleChildElements() throws Exception {
    SpinXmlElement element = XML(exampleXmlFileAsReader());
    SpinXmlElement child1 = XML("<child/>");
    SpinXmlElement child2 = XML("<child/>");
    element.append(child1, child2);

    SpinXmlElement finalElement = script.setVariable("element", element)
        .setVariable("child", child1)
        .setVariable("child2", child2)
        .execute()
        .getVariable("element");

    assertThatExceptionOfType(SpinXmlElementException.class).isThrownBy(() -> finalElement.childElement(null, "child"));
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.removeChildElement", variables = {
      @ScriptVariable(name = "input", isNull = true),
      @ScriptVariable(name = "child2", isNull = true) }, execute = false)
  public void canRemoveChildElementCollection() throws Exception {
    SpinXmlElement element = XML(exampleXmlFileAsReader());
    element.append(XML("<child/>"), XML("<child/>"), XML("<child/>"));

    SpinXmlElement finalElement = script.setVariable("element", element)
        .setVariable("child", element.childElements(null, "child"))
        .execute()
        .getVariable("element");

    assertThatExceptionOfType(SpinXmlElementException.class).isThrownBy(() -> finalElement.childElement(null, "child"));
  }

  // get child elements

  @Test
  @Script(name = "XmlDomElementScriptTest.getChildElementsByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "name", value = "customer") })
  public void canGetAllChildElementsByName() {
    SpinList<SpinXmlElement> childElements = script.getVariable("childElements");
    assertThat(childElements).hasSize(3);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getChildElementsByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "name", value = NON_EXISTING) }, execute = false)
  public void cannotGetAllChildElementsByNonExistingName() {
    assertThatExceptionOfType(SpinXmlElementException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getChildElementsByName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "name", isNull = true) }, execute = false)
  public void cannotGetAllChildElementsByNullName() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getChildElementsByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = EXAMPLE_NAMESPACE),
      @ScriptVariable(name = "name", value = "customer") })
  public void canGetAllChildElementsByNamespaceAndName() {
    SpinList<SpinXmlElement> childElements = script.getVariable("childElements");
    assertThat(childElements).hasSize(3);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getChildElementsByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME), @ScriptVariable(name = "namespace", isNull = true),
      @ScriptVariable(name = "name", value = "info") })
  public void canGetAllChildElementsByNullNamespaceAndName() {
    SpinList<SpinXmlElement> childElements = script.getVariable("childElements");
    assertThat(childElements).hasSize(2);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getChildElementsByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = NON_EXISTING),
      @ScriptVariable(name = "name", value = "customer") }, execute = false)
  public void cannotGetAllChildElementsByNonExistingNamespaceAndName() {
    assertThatExceptionOfType(SpinXmlElementException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getChildElementsByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = EXAMPLE_NAMESPACE),
      @ScriptVariable(name = "name", value = NON_EXISTING) }, execute = false)
  public void cannotGetAllChildElementsByNamespaceAndNonExistingName() {
    assertThatExceptionOfType(SpinXmlElementException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getChildElementsByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = EXAMPLE_NAMESPACE),
      @ScriptVariable(name = "name", isNull = true) }, execute = false)
  public void cannotGetAllChildElementsByNamespaceAndNonNullName() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getChildElementsByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "namespace", value = NON_EXISTING),
      @ScriptVariable(name = "name", value = NON_EXISTING) }, execute = false)
  public void cannotGetAllChildElementsByNonExistingNamespaceAndNonExistingName() {
    assertThatExceptionOfType(SpinXmlElementException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.getChildElementsByNamespaceAndName", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME), @ScriptVariable(name = "namespace", isNull = true),
      @ScriptVariable(name = "name", isNull = true) }, execute = false)
  public void cannotGetAllChildElementsByNullNamespaceAndNullName() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(this::failingWithException);
  }

  // replace child element

  @Test
  @Script(name = "XmlDomElementScriptTest.replaceChildElement", variables = {
      @ScriptVariable(name = "input", isNull = true),
      @ScriptVariable(name = "newChild", value = "<child/>") }, execute = false)
  public void canReplaceAChildElement() throws Exception {
    SpinXmlElement element = XML(exampleXmlFileAsReader());
    SpinXmlElement date = element.childElement("date");

    SpinXmlElement finalElement = script.setVariable("element", element)
        .setVariable("existingChild", date)
        .execute()
        .getVariable("element");

    assertThat(finalElement.childElement(null, "child")).isNotNull();
    assertThatExceptionOfType(SpinXmlElementException.class).isThrownBy(() -> finalElement.childElement("date"));
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.replaceChildElement", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "existingChild", isNull = true),
      @ScriptVariable(name = "newChild", value = "<child/>") }, execute = false)
  public void cannotReplaceANullChildElement() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.replaceChildElement", variables = {
      @ScriptVariable(name = "input", isNull = true),
      @ScriptVariable(name = "newChild", isNull = true) }, execute = false)
  public void cannotReplaceByANullChildElement() {
    SpinXmlElement element = XML(exampleXmlFileAsReader());
    SpinXmlElement date = element.childElement("date");
    script.setVariable("element", element).setVariable("existingChild", date);
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.replaceChildElement", variables = {
      @ScriptVariable(name = "input", file = EXAMPLE_XML_FILE_NAME),
      @ScriptVariable(name = "newChild", value = "<child/>") }, execute = false)
  public void cannotReplaceANoneChildElement() {
    script.setVariable("existingChild", XML("<child/>"));
    assertThatExceptionOfType(SpinXmlElementException.class).isThrownBy(this::failingWithException);
  }

  // replace element

  @Test
  @Script(name = "XmlDomElementScriptTest.replaceElement", variables = {
      @ScriptVariable(name = "newElement", value = "<child/>") }, execute = false)
  public void canReplaceElement() throws Exception {
    SpinXmlElement element = XML(exampleXmlFileAsReader());
    script.setVariable("oldElement", element.childElement("date")).execute();

    assertThat(element.childElement(null, "child")).isNotNull();
    assertThatExceptionOfType(SpinXmlElementException.class).isThrownBy(() -> element.childElement("date"));
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.replaceElement", variables = {
      @ScriptVariable(name = "newElement", value = "<root/>") }, execute = false)
  public void canReplaceRootElement() throws Exception {
    SpinXmlElement element = XML(exampleXmlFileAsReader());
    element = script.setVariable("oldElement", element).execute().getVariable("element");

    assertThat(element.name()).isEqualTo("root");
    assertThat(element.childElements()).isEmpty();
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.replaceElement", variables = {
      @ScriptVariable(name = "newElement", isNull = true) }, execute = false)
  public void cannotReplaceByNullElement() {
    SpinXmlElement element = XML(exampleXmlFileAsReader());
    script.setVariable("oldElement", element);
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(this::failingWithException);
  }

  @Test
  @Script("XmlDomElementScriptTest.readTextContent")
  @ScriptVariable(name = "input", value = "<customer>Foo</customer>")
  public void canReadTextContent() {
    String textContent = script.getVariable("textContent");
    assertThat(textContent).isEqualTo("Foo");
  }

  @Test
  @Script("XmlDomElementScriptTest.readTextContent")
  @ScriptVariable(name = "input", value = "<customer/>")
  public void canEmptyReadTextContent() {
    String textContent = script.getVariable("textContent");
    assertThat(textContent).isEmpty();
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.writeTextContent", variables = {
      @ScriptVariable(name = "input", value = "<customer/>"), @ScriptVariable(name = "text", value = "Foo") })
  public void canWriteTextContent() {
    String textContent = script.getVariable("textContent");
    assertThat(textContent).isEqualTo("Foo");
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.writeTextContent", variables = {
      @ScriptVariable(name = "input", value = "<customer/>"), @ScriptVariable(name = "text", value = "") })
  public void canWriteEmptyTextContent() {
    String textContent = script.getVariable("textContent");
    assertThat(textContent).isEmpty();
  }

  @Test
  @Script(name = "XmlDomElementScriptTest.writeTextContent", variables = {
      @ScriptVariable(name = "input", value = "<customer/>"),
      @ScriptVariable(name = "text", isNull = true) }, execute = false)
  public void cannotWriteNullTextContent() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(this::failingWithException);
  }

}
