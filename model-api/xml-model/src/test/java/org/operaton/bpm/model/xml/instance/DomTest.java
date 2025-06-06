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
package org.operaton.bpm.model.xml.instance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.operaton.bpm.model.xml.ModelInstance;
import org.operaton.bpm.model.xml.testmodel.Gender;
import org.operaton.bpm.model.xml.testmodel.TestModelParser;
import org.operaton.bpm.model.xml.testmodel.TestModelTest;
import org.operaton.bpm.model.xml.testmodel.instance.Animals;
import org.operaton.bpm.model.xml.testmodel.instance.Description;

import java.util.stream.Stream;

import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.model.xml.testmodel.TestModelConstants.MODEL_NAMESPACE;

/**
 * @author Sebastian Menski
 */
public class DomTest extends TestModelTest {

  private static final String TEST_NS = "http://operaton.org/test";
  private static final String UNKNOWN_NS = "http://operaton.org/unknown";
  private static final String CAMUNDA_NS = "http://activiti.org/bpmn";
  private static final String FOX_NS = "http://www.operaton.com/fox";
  private static final String BPMN_NS = "http://www.omg.org/spec/BPMN/20100524/MODEL";

  public static Stream<Arguments> models() {
    return Stream.of(createModel(), parseModel(DomTest.class)).map(Arguments::of);
  }

  private static TestModelArgs createModel() {
    TestModelParser modelParser = new TestModelParser();
    ModelInstance modelInstance = modelParser.getEmptyModel();

    Animals animals = modelInstance.newInstance(Animals.class);
    modelInstance.setDocumentElement(animals);

    Description description = modelInstance.newInstance(Description.class);
    description.getDomElement().addCDataSection("CDATA <test>");
    animals.addChildElement(description);

    return new TestModelArgs("created", modelInstance, modelParser);
  }

  @MethodSource("models")
  @ParameterizedTest
  void testRegisterNamespaces(TestModelArgs args) {
    init(args);
    var document = modelInstance.getDocument();
    document.registerNamespace("test", TEST_NS);
    String prefix = document.registerNamespace(UNKNOWN_NS);
    assertThat(prefix).isEqualTo("ns0");

    DomElement rootElement = document.getRootElement();
    assertThat(rootElement.hasAttribute(XMLNS_ATTRIBUTE_NS_URI, "test")).isTrue();
    assertThat(rootElement.getAttribute(XMLNS_ATTRIBUTE_NS_URI, "test")).isEqualTo(TEST_NS);
    assertThat(rootElement.hasAttribute(XMLNS_ATTRIBUTE_NS_URI, "ns0")).isTrue();
    assertThat(rootElement.getAttribute(XMLNS_ATTRIBUTE_NS_URI, "ns0")).isEqualTo(UNKNOWN_NS);
  }

  @MethodSource("models")
  @ParameterizedTest
  void testGenerateNamespacePrefixes(TestModelArgs args) {
    init(args);
    var document = modelInstance.getDocument();
    // occupy ns0 and ns2
    document.registerNamespace("ns0", UNKNOWN_NS + 0);
    document.registerNamespace("ns2", UNKNOWN_NS + 2);

    // add two generate
    String prefix = document.registerNamespace(UNKNOWN_NS + 1);
    assertThat(prefix).isEqualTo("ns1");
    prefix = document.registerNamespace(UNKNOWN_NS + 3);
    assertThat(prefix).isEqualTo("ns3");

    DomElement rootElement = document.getRootElement();
    assertThat(rootElement.hasAttribute(XMLNS_ATTRIBUTE_NS_URI, "ns0")).isTrue();
    assertThat(rootElement.getAttribute(XMLNS_ATTRIBUTE_NS_URI, "ns0")).isEqualTo(UNKNOWN_NS + 0);
    assertThat(rootElement.hasAttribute(XMLNS_ATTRIBUTE_NS_URI, "ns1")).isTrue();
    assertThat(rootElement.getAttribute(XMLNS_ATTRIBUTE_NS_URI, "ns1")).isEqualTo(UNKNOWN_NS + 1);
    assertThat(rootElement.hasAttribute(XMLNS_ATTRIBUTE_NS_URI, "ns2")).isTrue();
    assertThat(rootElement.getAttribute(XMLNS_ATTRIBUTE_NS_URI, "ns2")).isEqualTo(UNKNOWN_NS + 2);
    assertThat(rootElement.hasAttribute(XMLNS_ATTRIBUTE_NS_URI, "ns3")).isTrue();
    assertThat(rootElement.getAttribute(XMLNS_ATTRIBUTE_NS_URI, "ns3")).isEqualTo(UNKNOWN_NS + 3);
  }

  @MethodSource("models")
  @ParameterizedTest
  void testDuplicateNamespaces(TestModelArgs args) {
    init(args);
    var document = modelInstance.getDocument();
    document.registerNamespace("test", TEST_NS);
    String prefix = document.registerNamespace(TEST_NS);
    assertThat(prefix).isEqualTo("test");
    prefix = document.registerNamespace(UNKNOWN_NS);
    assertThat(prefix).isEqualTo("ns0");
    prefix = document.registerNamespace(UNKNOWN_NS);
    assertThat(prefix).isEqualTo("ns0");

    DomElement rootElement = document.getRootElement();
    assertThat(rootElement.hasAttribute(XMLNS_ATTRIBUTE_NS_URI, "test")).isTrue();
    assertThat(rootElement.getAttribute(XMLNS_ATTRIBUTE_NS_URI, "test")).isEqualTo(TEST_NS);
    assertThat(rootElement.hasAttribute(XMLNS_ATTRIBUTE_NS_URI, "ns0")).isTrue();
    assertThat(rootElement.getAttribute(XMLNS_ATTRIBUTE_NS_URI, "ns0")).isEqualTo(UNKNOWN_NS);
    assertThat(rootElement.hasAttribute(XMLNS_ATTRIBUTE_NS_URI, "ns1")).isFalse();
  }

  @MethodSource("models")
  @ParameterizedTest
  void testKnownPrefix(TestModelArgs args) {
    init(args);
    var document = modelInstance.getDocument();
    document.registerNamespace(CAMUNDA_NS);
    document.registerNamespace(FOX_NS);

    DomElement rootElement = document.getRootElement();
    assertThat(rootElement.hasAttribute(XMLNS_ATTRIBUTE_NS_URI, "operaton")).isTrue();
    assertThat(rootElement.getAttribute(XMLNS_ATTRIBUTE_NS_URI, "operaton")).isEqualTo(CAMUNDA_NS);
    assertThat(rootElement.hasAttribute(XMLNS_ATTRIBUTE_NS_URI, "fox")).isTrue();
    assertThat(rootElement.getAttribute(XMLNS_ATTRIBUTE_NS_URI, "fox")).isEqualTo(FOX_NS);
  }

  @MethodSource("models")
  @ParameterizedTest
  void testAlreadyUsedPrefix(TestModelArgs args) {
    init(args);
    var document = modelInstance.getDocument();
    document.registerNamespace("operaton", TEST_NS);
    document.registerNamespace(CAMUNDA_NS);

    DomElement rootElement = document.getRootElement();
    assertThat(rootElement.hasAttribute(XMLNS_ATTRIBUTE_NS_URI, "operaton")).isTrue();
    assertThat(rootElement.getAttribute(XMLNS_ATTRIBUTE_NS_URI, "operaton")).isEqualTo(TEST_NS);
    assertThat(rootElement.hasAttribute(XMLNS_ATTRIBUTE_NS_URI, "ns0")).isTrue();
    assertThat(rootElement.getAttribute(XMLNS_ATTRIBUTE_NS_URI, "ns0")).isEqualTo(CAMUNDA_NS);
  }

  @MethodSource("models")
  @ParameterizedTest
  void testAddElements(TestModelArgs args) {
    init(args);
    var document = modelInstance.getDocument();
    DomElement element = document.createElement(MODEL_NAMESPACE, "bird");
    element.setAttribute(MODEL_NAMESPACE, "gender", Gender.Unknown.toString());
    document.getRootElement().appendChild(element);
    assertThat(element.getNamespaceURI()).isEqualTo(MODEL_NAMESPACE);
    assertThat(element.getLocalName()).isEqualTo("bird");
    assertThat(element.getPrefix()).isNull();
    assertThat(element.getDocument()).isEqualTo(document);
    assertThat(element.getRootElement()).isEqualTo(document.getRootElement());

    document.registerNamespace("test", TEST_NS);
    element = document.createElement(TEST_NS, "dog");
    document.getRootElement().appendChild(element);
    assertThat(element.getNamespaceURI()).isEqualTo(TEST_NS);
    assertThat(element.getLocalName()).isEqualTo("dog");
    assertThat(element.getPrefix()).isEqualTo("test");
    assertThat(element.getDocument()).isEqualTo(document);
    assertThat(element.getRootElement()).isEqualTo(document.getRootElement());

    element = document.createElement(UNKNOWN_NS, "cat");
    document.getRootElement().appendChild(element);
    assertThat(element.getNamespaceURI()).isEqualTo(UNKNOWN_NS);
    assertThat(element.getLocalName()).isEqualTo("cat");
    assertThat(element.getPrefix()).isEqualTo("ns0");
    assertThat(element.getDocument()).isEqualTo(document);
    assertThat(element.getRootElement()).isEqualTo(document.getRootElement());
  }

  @MethodSource("models")
  @ParameterizedTest
  void testAddAttributes(TestModelArgs args) {
    init(args);
    var document = modelInstance.getDocument();
    DomElement element = document.createElement(MODEL_NAMESPACE, "bird");
    element.setAttribute(MODEL_NAMESPACE, "gender", Gender.Unknown.toString());
    document.getRootElement().appendChild(element);
    element.setIdAttribute("id", "tweety");
    element.setAttribute(MODEL_NAMESPACE, "name", "Tweety");
    assertThat(element.getAttribute(MODEL_NAMESPACE, "id")).isEqualTo("tweety");
    assertThat(element.getAttribute("name")).isEqualTo("Tweety");

    document.registerNamespace("test", TEST_NS);
    element = document.createElement(TEST_NS, "dog");
    document.getRootElement().appendChild(element);
    element.setIdAttribute("id", "snoopy");
    element.setAttribute(TEST_NS, "name", "Snoopy");
    assertThat(element.getAttribute(TEST_NS, "id")).isEqualTo("snoopy");
    assertThat(element.getAttribute("name")).isEqualTo("Snoopy");

    element = document.createElement(UNKNOWN_NS, "cat");
    document.getRootElement().appendChild(element);
    element.setIdAttribute("id", "sylvester");
    element.setAttribute(UNKNOWN_NS, "name", "Sylvester");
    element.setAttribute(BPMN_NS, "id", "test");
    assertThat(element.getAttribute(UNKNOWN_NS, "id")).isEqualTo("sylvester");
    assertThat(element.getAttribute("name")).isEqualTo("Sylvester");
    assertThat(element.getAttribute(BPMN_NS, "id")).isEqualTo("test");
    assertThat(element.hasAttribute(XMLNS_ATTRIBUTE_NS_URI, "bpmn2")).isFalse();
    assertThat(document.getRootElement().hasAttribute(XMLNS_ATTRIBUTE_NS_URI, "bpmn2")).isTrue();
  }

  @MethodSource("models")
  @ParameterizedTest
  void testCData(TestModelArgs args) {
    init(args);
    Animals animals = (Animals) modelInstance.getDocumentElement();
    assertThat(animals.getDescription().getTextContent())
      .isEqualTo("CDATA <test>");
  }

}
