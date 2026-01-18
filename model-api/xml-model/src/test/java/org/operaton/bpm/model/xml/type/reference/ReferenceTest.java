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
package org.operaton.bpm.model.xml.type.reference;
import java.util.List;
import java.util.Collection;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.operaton.bpm.model.xml.ModelInstance;
import org.operaton.bpm.model.xml.UnsupportedModelOperationException;
import org.operaton.bpm.model.xml.impl.type.reference.AttributeReferenceImpl;
import org.operaton.bpm.model.xml.impl.type.reference.QNameAttributeReferenceImpl;
import org.operaton.bpm.model.xml.testmodel.Gender;
import org.operaton.bpm.model.xml.testmodel.TestModelParser;
import org.operaton.bpm.model.xml.testmodel.TestModelTest;
import org.operaton.bpm.model.xml.testmodel.instance.*;
import org.operaton.bpm.model.xml.type.ModelElementType;
import org.operaton.bpm.model.xml.type.attribute.Attribute;

import static org.operaton.bpm.model.xml.test.assertions.ModelAssertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Sebastian Menski
 */
public class ReferenceTest extends TestModelTest {

  private Bird tweety;
  private Bird daffy;
  private Bird daisy;
  private Bird plucky;
  private Bird birdo;
  private FlightPartnerRef flightPartnerRef;

  private ModelElementType animalType;
  private QNameAttributeReferenceImpl<Animal> fatherReference;
  private AttributeReferenceImpl<Animal> motherReference;
  private ElementReferenceCollection<FlyingAnimal, FlightPartnerRef> flightPartnerRefsColl;

  static Stream<Arguments> models() {
    return Stream.of(createModel(), parseModel(ReferenceTest.class)).map(Arguments::of);
  }

  public static TestModelArgs createModel() {
    TestModelParser modelParser = new TestModelParser();
    ModelInstance modelInstance = modelParser.getEmptyModel();

    Animals animals = modelInstance.newInstance(Animals.class);
    modelInstance.setDocumentElement(animals);

    Bird tweety = createBird(modelInstance, "tweety", Gender.Female);
    Bird daffy = createBird(modelInstance, "daffy", Gender.Male);
    Bird daisy = createBird(modelInstance, "daisy", Gender.Female);
    createBird(modelInstance, "plucky", Gender.Male);
    createBird(modelInstance, "birdo", Gender.Female);
    tweety.setFather(daffy);
    tweety.setMother(daisy);

    tweety.getFlightPartnerRefs().add(daffy);

    return new TestModelArgs("created", modelInstance, modelParser);
  }

  @Override
  protected void init(TestModelArgs args) {
    super.init(args);

    tweety = modelInstance.getModelElementById("tweety");
    daffy = modelInstance.getModelElementById("daffy");
    daisy = modelInstance.getModelElementById("daisy");
    plucky = modelInstance.getModelElementById("plucky");
    birdo = modelInstance.getModelElementById("birdo");

    animalType = modelInstance.getModel().getType(Animal.class);

    // QName attribute reference
    fatherReference = (QNameAttributeReferenceImpl<Animal>) animalType.getAttribute("father").getOutgoingReferences().iterator().next();

    // ID attribute reference
    motherReference = (AttributeReferenceImpl<Animal>) animalType.getAttribute("mother").getOutgoingReferences().iterator().next();

    // ID element reference
    flightPartnerRefsColl = FlyingAnimal.flightPartnerRefsColl;

    ModelElementType flightPartnerRefType = modelInstance.getModel().getType(FlightPartnerRef.class);
    flightPartnerRef = (FlightPartnerRef) modelInstance.getModelElementsByType(flightPartnerRefType).iterator().next();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testReferenceIdentifier(TestModelArgs args) {
    init(args);
    assertThat(fatherReference).hasIdentifier(tweety, daffy.getId());
    assertThat(motherReference).hasIdentifier(tweety, daisy.getId());
    assertThat(flightPartnerRefsColl).hasIdentifier(tweety, daffy.getId());
  }

  @ParameterizedTest
  @MethodSource("models")
  void testReferenceTargetElement(TestModelArgs args) {
    init(args);
    assertThat(fatherReference).hasTargetElement(tweety, daffy);
    assertThat(motherReference).hasTargetElement(tweety, daisy);
    assertThat(flightPartnerRefsColl).hasTargetElement(tweety, daffy);

    fatherReference.setReferenceTargetElement(tweety, plucky);
    motherReference.setReferenceTargetElement(tweety, birdo);
    flightPartnerRefsColl.setReferenceTargetElement(flightPartnerRef, daisy);

    assertThat(fatherReference).hasTargetElement(tweety, plucky);
    assertThat(motherReference).hasTargetElement(tweety, birdo);
    assertThat(flightPartnerRefsColl).hasTargetElement(tweety, daisy);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testReferenceTargetAttribute(TestModelArgs args) {
    init(args);
    Attribute<?> idAttribute = animalType.getAttribute("id");
    assertThat(idAttribute).hasIncomingReferences(fatherReference, motherReference);

    assertThat(fatherReference).hasTargetAttribute(idAttribute);
    assertThat(motherReference).hasTargetAttribute(idAttribute);
    assertThat(flightPartnerRefsColl).hasTargetAttribute(idAttribute);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testReferenceSourceAttribute(TestModelArgs args) {
    init(args);
    Attribute<?> fatherAttribute = animalType.getAttribute("father");
    Attribute<?> motherAttribute = animalType.getAttribute("mother");

    assertThat(fatherReference).hasSourceAttribute(fatherAttribute);
    assertThat(motherReference).hasSourceAttribute(motherAttribute);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testRemoveReference(TestModelArgs args) {
    init(args);
    fatherReference.referencedElementRemoved(daffy, daffy.getId());

    assertThat(fatherReference).hasNoTargetElement(tweety);
    assertThat(tweety.getFather()).isNull();

    motherReference.referencedElementRemoved(daisy, daisy.getId());
    assertThat(motherReference).hasNoTargetElement(tweety);
    assertThat(tweety.getMother()).isNull();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testTargetElementsCollection(TestModelArgs args) {
    init(args);
    Collection<FlyingAnimal> referenceTargetElements = flightPartnerRefsColl.getReferenceTargetElements(tweety);
    Collection<FlyingAnimal> flightPartners = List.of(birdo, daffy, daisy, plucky);

    // directly test collection methods and not use the	appropriate assertion methods
    assertThat(referenceTargetElements)
      .isNotEmpty()
      .hasSize(1)
      .contains(daffy);
    assertThat(referenceTargetElements.toArray()).isEqualTo(new Object[]{daffy});
    assertThat(referenceTargetElements.toArray(new FlyingAnimal[1])).isEqualTo(new FlyingAnimal[]{daffy});

    assertThat(referenceTargetElements.add(daisy)).isTrue();
    assertThat(referenceTargetElements)
      .hasSize(2)
      .containsOnly(daffy, daisy);

    assertThat(referenceTargetElements.remove(daisy)).isTrue();
    assertThat(referenceTargetElements)
      .hasSize(1)
      .containsOnly(daffy);

    assertThat(referenceTargetElements.addAll(flightPartners)).isTrue();
    assertThat(referenceTargetElements)
      .containsAll(flightPartners)
      .hasSize(4)
      .containsOnly(daffy, daisy, plucky, birdo);

    assertThat(referenceTargetElements.removeAll(flightPartners)).isTrue();
    assertThat(referenceTargetElements).isEmpty();

    try {
      referenceTargetElements.retainAll(flightPartners);
      fail("retainAll method is not implemented");
    }
    catch (Exception e) {
      assertThat(e).isInstanceOf(UnsupportedModelOperationException.class);
    }

    referenceTargetElements.addAll(flightPartners);
    assertThat(referenceTargetElements).isNotEmpty();
    referenceTargetElements.clear();
    assertThat(referenceTargetElements).isEmpty();
  }

}
