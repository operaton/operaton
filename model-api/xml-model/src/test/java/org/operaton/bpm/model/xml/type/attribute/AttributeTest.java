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
package org.operaton.bpm.model.xml.type.attribute;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.operaton.bpm.model.xml.ModelInstance;
import org.operaton.bpm.model.xml.impl.type.attribute.AttributeImpl;
import org.operaton.bpm.model.xml.testmodel.Gender;
import org.operaton.bpm.model.xml.testmodel.TestModelParser;
import org.operaton.bpm.model.xml.testmodel.TestModelTest;
import org.operaton.bpm.model.xml.testmodel.instance.Animal;
import org.operaton.bpm.model.xml.testmodel.instance.AnimalTest;
import org.operaton.bpm.model.xml.testmodel.instance.Animals;
import org.operaton.bpm.model.xml.testmodel.instance.Bird;
import org.operaton.bpm.model.xml.type.ModelElementType;

import java.util.stream.Stream;

import static org.operaton.bpm.model.xml.test.assertions.ModelAssertions.assertThat;

/**
 * @author Sebastian Menski
 */
public class AttributeTest extends TestModelTest {

  private Bird tweety;
  private Attribute<String> idAttribute;
  private Attribute<String> nameAttribute;
  private Attribute<String> fatherAttribute;

  static Stream<Arguments> models() {
    return Stream.of(createModel(), parseModel(AnimalTest.class)).map(Arguments::of);
  }

  public static TestModelArgs createModel() {
    TestModelParser modelParser = new TestModelParser();
    ModelInstance modelInstance = modelParser.getEmptyModel();

    Animals animals = modelInstance.newInstance(Animals.class);
    modelInstance.setDocumentElement(animals);

    createBird(modelInstance, "tweety", Gender.Female);

    return new TestModelArgs("created", modelInstance, modelParser);
  }

  @Override
  protected void init(TestModelArgs args) {
    super.init(args);

    tweety = modelInstance.getModelElementById("tweety");

    ModelElementType animalType = modelInstance.getModel().getType(Animal.class);
    idAttribute = (Attribute<String>) animalType.getAttribute("id");
    nameAttribute = (Attribute<String>) animalType.getAttribute("name");
    fatherAttribute = (Attribute<String>) animalType.getAttribute("father");
  }

  @ParameterizedTest
  @MethodSource("models")
  void testOwningElementType(TestModelArgs args) {
    init(args);
    ModelElementType animalType = modelInstance.getModel().getType(Animal.class);

    assertThat(idAttribute).hasOwningElementType(animalType);
    assertThat(nameAttribute).hasOwningElementType(animalType);
    assertThat(fatherAttribute).hasOwningElementType(animalType);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testSetAttributeValue(TestModelArgs args) {
    init(args);
    String identifier = "new-" + tweety.getId();
    idAttribute.setValue(tweety, identifier);
    assertThat(idAttribute).hasValue(tweety, identifier);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testSetAttributeValueWithoutUpdateReference(TestModelArgs args) {
    init(args);
    String identifier = "new-" + tweety.getId();
    idAttribute.setValue(tweety, identifier, false);
    assertThat(idAttribute).hasValue(tweety, identifier);
  }

  @MethodSource("models")
  void testSetDefaultValue(TestModelArgs args) {
    init(args);
    String defaultName = "default-name";
    assertThat(tweety.getName()).isNull();
    assertThat(nameAttribute).hasNoDefaultValue();

    ((AttributeImpl<String>) nameAttribute).setDefaultValue(defaultName);
    assertThat(nameAttribute).hasDefaultValue(defaultName);
    assertThat(tweety.getName()).isEqualTo(defaultName);

    tweety.setName("not-" + defaultName);
    assertThat(tweety.getName()).isNotEqualTo(defaultName);

    tweety.removeAttribute("name");
    assertThat(tweety.getName()).isEqualTo(defaultName);
    ((AttributeImpl<String>) nameAttribute).setDefaultValue(null);
    assertThat(nameAttribute).hasNoDefaultValue();
  }

  @MethodSource("models")
  void testRequired(TestModelArgs args) {
    init(args);
    tweety.removeAttribute("name");
    assertThat(nameAttribute).isOptional();

    ((AttributeImpl<String>) nameAttribute).setRequired(true);
    assertThat(nameAttribute).isRequired();

    ((AttributeImpl<String>) nameAttribute).setRequired(false);
  }

  @MethodSource("models")
  void testSetNamespaceUri(TestModelArgs args) {
    init(args);
    String testNamespace = "http://operaton.org/test";

    ((AttributeImpl<String>) idAttribute).setNamespaceUri(testNamespace);
    assertThat(idAttribute).hasNamespaceUri(testNamespace);

    ((AttributeImpl<String>) idAttribute).setNamespaceUri(null);
    assertThat(idAttribute).hasNoNamespaceUri();
  }

  @MethodSource("models")
  void testIdAttribute(TestModelArgs args) {
    init(args);
    assertThat(idAttribute).isIdAttribute();
    assertThat(nameAttribute).isNotIdAttribute();
    assertThat(fatherAttribute).isNotIdAttribute();
  }

  @MethodSource("models")
  void testAttributeName(TestModelArgs args) {
    init(args);
    assertThat(idAttribute).hasAttributeName("id");
    assertThat(nameAttribute).hasAttributeName("name");
    assertThat(fatherAttribute).hasAttributeName("father");
  }

  @MethodSource("models")
  void testRemoveAttribute(TestModelArgs args) {
    init(args);
    tweety.setName("test");
    assertThat(tweety.getName()).isNotNull();
    assertThat(nameAttribute).hasValue(tweety);

    ((AttributeImpl<String>) nameAttribute).removeAttribute(tweety);
    assertThat(tweety.getName()).isNull();
    assertThat(nameAttribute).hasNoValue(tweety);
  }

  @MethodSource("models")
  void testIncomingReferences(TestModelArgs args) {
    init(args);
    assertThat(idAttribute).hasIncomingReferences();
    assertThat(nameAttribute).hasNoIncomingReferences();
    assertThat(fatherAttribute).hasNoIncomingReferences();
  }

  @MethodSource("models")
  void testOutgoingReferences(TestModelArgs args) {
    init(args);
    assertThat(idAttribute).hasNoOutgoingReferences();
    assertThat(nameAttribute).hasNoOutgoingReferences();
    assertThat(fatherAttribute).hasOutgoingReferences();
  }

}
