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
package org.operaton.bpm.model.xml.type.child;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.operaton.bpm.model.xml.ModelInstance;
import org.operaton.bpm.model.xml.UnsupportedModelOperationException;
import org.operaton.bpm.model.xml.impl.type.child.ChildElementCollectionImpl;
import org.operaton.bpm.model.xml.impl.type.child.ChildElementImpl;
import org.operaton.bpm.model.xml.testmodel.Gender;
import org.operaton.bpm.model.xml.testmodel.TestModelParser;
import org.operaton.bpm.model.xml.testmodel.TestModelTest;
import org.operaton.bpm.model.xml.testmodel.instance.*;
import org.operaton.bpm.model.xml.type.ModelElementType;

import static org.operaton.bpm.model.xml.test.assertions.ModelAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Sebastian Menski
 */
public class ChildElementCollectionTest extends TestModelTest {

  private Bird tweety;
  private Bird daffy;
  private Bird daisy;
  private Bird plucky;
  private Bird birdo;
  private ChildElement<FlightInstructor> flightInstructorChild;
  private ChildElementCollection<FlightPartnerRef> flightPartnerRefCollection;

  static Stream<Arguments> models() {
    return Stream.of(createModel(), parseModel(ChildElementCollectionTest.class)).map(Arguments::of);
  }


  public static TestModelArgs createModel() {
    TestModelParser modelParser = new TestModelParser();
    ModelInstance modelInstance = modelParser.getEmptyModel();

    Animals animals = modelInstance.newInstance(Animals.class);
    modelInstance.setDocumentElement(animals);

    Bird tweety = createBird(modelInstance, "tweety", Gender.Female);
    Bird daffy = createBird(modelInstance, "daffy", Gender.Male);
    Bird daisy = createBird(modelInstance, "daisy", Gender.Female);
    Bird plucky = createBird(modelInstance, "plucky", Gender.Male);
    createBird(modelInstance, "birdo", Gender.Female);

    tweety.setFlightInstructor(daffy);
    tweety.getFlightPartnerRefs().add(daisy);
    tweety.getFlightPartnerRefs().add(plucky);

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

    flightInstructorChild = (ChildElement<FlightInstructor>) FlyingAnimal.flightInstructorChild.getReferenceSourceCollection();
    flightPartnerRefCollection = FlyingAnimal.flightPartnerRefsColl.getReferenceSourceCollection();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testImmutable(TestModelArgs args) {
    init(args);
    assertThat(flightInstructorChild).isMutable();
    assertThat(flightPartnerRefCollection).isMutable();

    ((ChildElementImpl<FlightInstructor>) flightInstructorChild).setImmutable();
    ((ChildElementCollectionImpl<FlightPartnerRef>) flightPartnerRefCollection).setImmutable();
    assertThat(flightInstructorChild).isImmutable();
    assertThat(flightPartnerRefCollection).isImmutable();

    ((ChildElementImpl<FlightInstructor>) flightInstructorChild).setMutable(true);
    ((ChildElementCollectionImpl<FlightPartnerRef>) flightPartnerRefCollection).setMutable(true);
    assertThat(flightInstructorChild).isMutable();
    assertThat(flightPartnerRefCollection).isMutable();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testMinOccurs(TestModelArgs args) {
    init(args);
    assertThat(flightInstructorChild).isOptional();
    assertThat(flightPartnerRefCollection).isOptional();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testMaxOccurs(TestModelArgs args) {
    init(args);
    assertThat(flightInstructorChild).occursMaximal(1);
    assertThat(flightPartnerRefCollection).isUnbounded();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testChildElementType(TestModelArgs args) {
    init(args);
    assertThat(flightInstructorChild).containsType(FlightInstructor.class);
    assertThat(flightPartnerRefCollection).containsType(FlightPartnerRef.class);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testParentElementType(TestModelArgs args) {
    init(args);
    ModelElementType flyingAnimalType = modelInstance.getModel().getType(FlyingAnimal.class);

    assertThat(flightInstructorChild).hasParentElementType(flyingAnimalType);
    assertThat(flightPartnerRefCollection).hasParentElementType(flyingAnimalType);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testGetChildElements(TestModelArgs args) {
    init(args);
    assertThat(flightInstructorChild).hasSize(tweety, 1);
    assertThat(flightPartnerRefCollection).hasSize(tweety, 2);

    FlightInstructor flightInstructor = flightInstructorChild.getChild(tweety);
    assertThat(flightInstructor.getTextContent()).isEqualTo(daffy.getId());

    for (FlightPartnerRef flightPartnerRef : flightPartnerRefCollection.get(tweety)) {
      assertThat(flightPartnerRef.getTextContent()).isIn(daisy.getId(), plucky.getId());
    }
  }

  @ParameterizedTest
  @MethodSource("models")
  void testRemoveChildElements(TestModelArgs args) {
    init(args);
    assertThat(flightInstructorChild).isNotEmpty(tweety);
    assertThat(flightPartnerRefCollection).isNotEmpty(tweety);

    flightInstructorChild.removeChild(tweety);
    flightPartnerRefCollection.get(tweety).clear();

    assertThat(flightInstructorChild).isEmpty(tweety);
    assertThat(flightPartnerRefCollection).isEmpty(tweety);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testChildElementsCollection(TestModelArgs args) {
    init(args);
    Collection<FlightPartnerRef> flightPartnerRefs = flightPartnerRefCollection.get(tweety);

    Iterator<FlightPartnerRef> iterator = flightPartnerRefs.iterator();
    FlightPartnerRef daisyRef = iterator.next();
    FlightPartnerRef pluckyRef = iterator.next();
    assertThat(daisyRef.getTextContent()).isEqualTo(daisy.getId());
    assertThat(pluckyRef.getTextContent()).isEqualTo(plucky.getId());

    FlightPartnerRef birdoRef = modelInstance.newInstance(FlightPartnerRef.class);
    birdoRef.setTextContent(birdo.getId());

    Collection<FlightPartnerRef> flightPartners = List.of(birdoRef, daisyRef, pluckyRef);

    // directly test collection methods and not use the appropriate assertion methods
    assertThat(flightPartnerRefs)
      .hasSize(2)
      .contains(daisyRef);
    assertThat(flightPartnerRefs.toArray()).isEqualTo(new Object[]{daisyRef, pluckyRef});
    assertThat(flightPartnerRefs.toArray(new FlightPartnerRef[1])).isEqualTo(new FlightPartnerRef[]{daisyRef, pluckyRef});

    assertThat(flightPartnerRefs.add(birdoRef)).isTrue();
    assertThat(flightPartnerRefs)
      .hasSize(3)
      .containsOnly(birdoRef, daisyRef, pluckyRef);

    assertThat(flightPartnerRefs.remove(daisyRef)).isTrue();
    assertThat(flightPartnerRefs)
      .hasSize(2)
      .containsOnly(birdoRef, pluckyRef);

    assertThat(flightPartnerRefs.addAll(flightPartners)).isTrue();
    assertThat(flightPartnerRefs)
      .containsAll(flightPartners)
      .hasSize(3)
      .containsOnly(birdoRef, daisyRef, pluckyRef);

    assertThat(flightPartnerRefs.removeAll(flightPartners)).isTrue();
    assertThat(flightPartnerRefs).isEmpty();

    assertThatThrownBy(() -> flightPartnerRefs.retainAll(flightPartners))
      .isInstanceOf(UnsupportedModelOperationException.class);

    flightPartnerRefs.addAll(flightPartners);
    assertThat(flightPartnerRefs).isNotEmpty();
    flightPartnerRefs.clear();
    assertThat(flightPartnerRefs).isEmpty();
  }
}
