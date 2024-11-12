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
package org.operaton.bpm.model.xml.testmodel.instance;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.operaton.bpm.model.xml.ModelInstance;
import org.operaton.bpm.model.xml.impl.ModelImpl;
import org.operaton.bpm.model.xml.instance.DomElement;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;
import org.operaton.bpm.model.xml.instance.ModelElementInstanceTest;
import org.operaton.bpm.model.xml.testmodel.Gender;
import org.operaton.bpm.model.xml.testmodel.TestModelConstants;
import org.operaton.bpm.model.xml.testmodel.TestModelTest;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ronny Bräunlich
 */
public class AlternativeNsTest extends TestModelTest {

  private static final String MECHANICAL_NS = "http://operaton.org/mechanical";
  private static final String YET_ANOTHER_NS = "http://operaton.org/yans";

  static Stream<Arguments> models() {
    return Stream.of(parseModel(AlternativeNsTest.class)).map(Arguments::of);
  }

  @Override
  public void init(TestModelArgs args) {
    super.init(args);
    ModelImpl modelImpl = (ModelImpl) modelInstance.getModel();
    modelImpl.declareAlternativeNamespace(MECHANICAL_NS, TestModelConstants.NEWER_NAMESPACE);
    modelImpl.declareAlternativeNamespace(YET_ANOTHER_NS, TestModelConstants.NEWER_NAMESPACE);
  }

  @AfterEach
  public void tearDown() {
    if (modelInstance != null) {
      ModelImpl modelImpl = (ModelImpl) modelInstance.getModel();
      modelImpl.undeclareAlternativeNamespace(MECHANICAL_NS);
      modelImpl.undeclareAlternativeNamespace(YET_ANOTHER_NS);
    }
  }

  @ParameterizedTest
  @MethodSource("models")
  void getUniqueChildElementByNameNsForAlternativeNs(TestModelArgs args) {
    init(args);
    ModelElementInstance hedwig = modelInstance.getModelElementById("hedwig");
    assertThat(hedwig).isNotNull();
    ModelElementInstance childElementByNameNs = hedwig.getUniqueChildElementByNameNs(TestModelConstants.NEWER_NAMESPACE, "wings");
    assertThat(childElementByNameNs).isNotNull();
    assertThat(childElementByNameNs.getTextContent()).isEqualTo("wusch");
  }

  @ParameterizedTest
  @MethodSource("models")
  void getUniqueChildElementByNameNsForSecondAlternativeNs(TestModelArgs args) {
    init(args);
    // givne
    ModelElementInstance donald = modelInstance.getModelElementById("donald");

    // when
    ModelElementInstance childElementByNameNs = donald.getUniqueChildElementByNameNs(TestModelConstants.NEWER_NAMESPACE, "wings");

    // then
    assertThat(childElementByNameNs).isNotNull();
    assertThat(childElementByNameNs.getTextContent()).isEqualTo("flappy");
  }

  @ParameterizedTest
  @MethodSource("models")
  void getChildElementsByTypeForAlternativeNs(TestModelArgs args) {
    init(args);
    ModelElementInstance birdo = modelInstance.getModelElementById("birdo");
    assertThat(birdo).isNotNull();
    Collection<Wings> elements = birdo.getChildElementsByType(Wings.class);
    assertThat(elements.size()).isEqualTo(1);
    assertThat(elements.iterator().next().getTextContent()).isEqualTo("zisch");
  }

  @ParameterizedTest
  @MethodSource("models")
  void getChildElementsByTypeForSecondAlternativeNs(TestModelArgs args) {
    init(args);
    // given
    ModelElementInstance donald = modelInstance.getModelElementById("donald");

    // when
    Collection<Wings> elements = donald.getChildElementsByType(Wings.class);

    // then
    assertThat(elements.size()).isEqualTo(1);
    assertThat(elements.iterator().next().getTextContent()).isEqualTo("flappy");
  }

  @ParameterizedTest
  @MethodSource("models")
  void getAttributeValueNsForAlternativeNs(TestModelArgs args) {
    init(args);
    Bird plucky = modelInstance.getModelElementById("plucky");
    assertThat(plucky).isNotNull();
    Boolean extendedWings = plucky.canHazExtendedWings();
    assertThat(extendedWings).isEqualTo(false);
  }

  @ParameterizedTest
  @MethodSource("models")
  void getAttributeValueNsForSecondAlternativeNs(TestModelArgs args) {
    init(args);
    // given
    Bird donald = modelInstance.getModelElementById("donald");

    // when
    Boolean extendedWings = donald.canHazExtendedWings();

    // then
    assertThat(extendedWings).isEqualTo(true);
  }

  @ParameterizedTest
  @MethodSource("models")
  public void modifyingAttributeWithAlternativeNamespaceKeepsAlternativeNamespace(TestModelArgs args) {
    init(args);
    Bird plucky = modelInstance.getModelElementById("plucky");
    assertThat(plucky).isNotNull();
    //validate old value
    Boolean extendedWings = plucky.canHazExtendedWings();
    assertThat(extendedWings).isEqualTo(false);
    //change it
    plucky.setCanHazExtendedWings(true);
    String attributeValueNs = plucky.getAttributeValueNs(MECHANICAL_NS, "canHazExtendedWings");
    assertThat(attributeValueNs).isEqualTo("true");
  }

  @ParameterizedTest
  @MethodSource("models")
  public void modifyingAttributeWithSecondAlternativeNamespaceKeepsSecondAlternativeNamespace(TestModelArgs args) {
    init(args);
    // given
    Bird donald = modelInstance.getModelElementById("donald");

    // when
    donald.setCanHazExtendedWings(false);

    // then
    String attributeValueNs = donald.getAttributeValueNs(YET_ANOTHER_NS, "canHazExtendedWings");
    assertThat(attributeValueNs).isEqualTo("false");
  }

  @ParameterizedTest
  @MethodSource("models")
  public void modifyingAttributeWithNewNamespaceKeepsNewNamespace(TestModelArgs args) {
    init(args);
    Bird bird = createBird(modelInstance, "waldo", Gender.Male);
    bird.setCanHazExtendedWings(true);
    String attributeValueNs = bird.getAttributeValueNs(TestModelConstants.NEWER_NAMESPACE, "canHazExtendedWings");
    assertThat(attributeValueNs).isEqualTo("true");
  }

  @ParameterizedTest
  @MethodSource("models")
  public void modifyingElementWithAlternativeNamespaceKeepsAlternativeNamespace(TestModelArgs args) {
    init(args);
    Bird birdo = modelInstance.getModelElementById("birdo");
    assertThat(birdo).isNotNull();
    Wings wings = birdo.getWings();
    assertThat(wings).isNotNull();
    wings.setTextContent("kawusch");

    List<DomElement> childElementsByNameNs = birdo.getDomElement().getChildElementsByNameNs(MECHANICAL_NS, "wings");
    assertThat(childElementsByNameNs.size()).isEqualTo(1);
    assertThat(childElementsByNameNs.get(0).getTextContent()).isEqualTo("kawusch");
  }

  @ParameterizedTest
  @MethodSource("models")
  public void modifyingElementWithSecondAlternativeNamespaceKeepsSecondAlternativeNamespace(TestModelArgs args) {
    init(args);
    // given
    Bird donald = modelInstance.getModelElementById("donald");
    Wings wings = donald.getWings();

    // when
    wings.setTextContent("kawusch");

    // then
    List<DomElement> childElementsByNameNs = donald.getDomElement().getChildElementsByNameNs(YET_ANOTHER_NS, "wings");
    assertThat(childElementsByNameNs.size()).isEqualTo(1);
    assertThat(childElementsByNameNs.get(0).getTextContent()).isEqualTo("kawusch");
  }

  @ParameterizedTest
  @MethodSource("models")
  public void modifyingElementWithNewNamespaceKeepsNewNamespace(TestModelArgs args) {
    init(args);
    Bird bird = createBird(modelInstance, "waldo", Gender.Male);
    bird.setWings(modelInstance.newInstance(Wings.class));

    List<DomElement> childElementsByNameNs = bird.getDomElement().getChildElementsByNameNs(TestModelConstants.NEWER_NAMESPACE, "wings");
    assertThat(childElementsByNameNs.size()).isEqualTo(1);
  }

  @ParameterizedTest
  @MethodSource("models")
  void useExistingNamespace(TestModelArgs args) {
    init(args);
    assertThatThereIsNoNewerNamespaceUrl(modelInstance);

    Bird plucky = modelInstance.getModelElementById("plucky");
    plucky.setAttributeValueNs(MECHANICAL_NS, "canHazExtendedWings", "true");

    Bird donald = modelInstance.getModelElementById("donald");
    donald.setAttributeValueNs(YET_ANOTHER_NS, "canHazExtendedWings", "false");
    assertThatThereIsNoNewerNamespaceUrl(modelInstance);

    assertTrue(plucky.canHazExtendedWings());
    assertThatThereIsNoNewerNamespaceUrl(modelInstance);
  }

  protected void assertThatThereIsNoNewerNamespaceUrl(ModelInstance modelInstance) {
    Node rootElement = modelInstance.getDocument().getDomSource().getNode().getFirstChild();
    NamedNodeMap attributes = rootElement.getAttributes();
    for (int i = 0; i < attributes.getLength(); i++) {
      Node item = attributes.item(i);
      String nodeValue = item.getNodeValue();
      assertNotEquals(TestModelConstants.NEWER_NAMESPACE, nodeValue, "Found newer namespace url which shouldn't exist");
    }
  }


}
