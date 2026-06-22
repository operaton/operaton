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
package org.operaton.bpm.model.xml.testmodel.instance;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.operaton.bpm.model.xml.ModelInstance;
import org.operaton.bpm.model.xml.testmodel.Gender;
import org.operaton.bpm.model.xml.testmodel.TestModelParser;
import org.operaton.bpm.model.xml.testmodel.TestModelTest;

import static org.operaton.bpm.model.xml.testmodel.TestModelConstants.MODEL_NAMESPACE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastian Menski
 */
public class FlyingAnimalTest extends TestModelTest {

  private FlyingAnimal tweety;
  private FlyingAnimal hedwig;
  private FlyingAnimal birdo;
  private FlyingAnimal plucky;
  private FlyingAnimal fiffy;
  private FlyingAnimal timmy;
  private FlyingAnimal daisy;

  static Stream<Arguments> models() {
    return Stream.of(createModel(), parseModel(FlyingAnimalTest.class)).map(Arguments::of);
  }

  public static TestModelArgs createModel() {
    TestModelParser modelParser = new TestModelParser();
    ModelInstance modelInstance = modelParser.getEmptyModel();

    Animals animals = modelInstance.newInstance(Animals.class);
    modelInstance.setDocumentElement(animals);

    // add a tns namespace prefix for QName testing
    animals.getDomElement().registerNamespace("tns", MODEL_NAMESPACE);

    FlyingAnimal tweety = createBird(modelInstance, "tweety", Gender.Female);
    FlyingAnimal hedwig = createBird(modelInstance, "hedwig", Gender.Male);
    FlyingAnimal birdo = createBird(modelInstance, "birdo", Gender.Female);
    FlyingAnimal plucky = createBird(modelInstance, "plucky", Gender.Unknown);
    FlyingAnimal fiffy = createBird(modelInstance, "fiffy", Gender.Female);
    createBird(modelInstance, "timmy", Gender.Male);
    createBird(modelInstance, "daisy", Gender.Female);

    tweety.setFlightInstructor(hedwig);

    tweety.getFlightPartnerRefs().add(hedwig);
    tweety.getFlightPartnerRefs().add(birdo);
    tweety.getFlightPartnerRefs().add(plucky);
    tweety.getFlightPartnerRefs().add(fiffy);

    return new TestModelArgs("created", modelInstance, modelParser);
  }

  @Override
  protected void init(TestModelArgs args) {
    super.init(args);
    modelInstance = modelInstance.copy();
    tweety = modelInstance.getModelElementById("tweety");
    hedwig = modelInstance.getModelElementById("hedwig");
    birdo = modelInstance.getModelElementById("birdo");
    plucky = modelInstance.getModelElementById("plucky");
    fiffy = modelInstance.getModelElementById("fiffy");
    timmy = modelInstance.getModelElementById("timmy");
    daisy = modelInstance.getModelElementById("daisy");
  }

  @ParameterizedTest
  @MethodSource("models")
  void testSetWingspanAttributeByHelper(TestModelArgs args) {
    init(args);
    double wingspan = 2.123;
    tweety.setWingspan(wingspan);
    assertThat(tweety.getWingspan()).isEqualTo(wingspan);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testSetWingspanAttributeByAttributeName(TestModelArgs args) {
    init(args);
    Double wingspan = 2.123;
    tweety.setAttributeValue("wingspan", wingspan.toString(), false);
    assertThat(tweety.getWingspan()).isEqualTo(wingspan);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testRemoveWingspanAttribute(TestModelArgs args) {
    init(args);
    double wingspan = 2.123;
    tweety.setWingspan(wingspan);
    assertThat(tweety.getWingspan()).isEqualTo(wingspan);

    tweety.removeAttribute("wingspan");

    assertThat(tweety.getWingspan()).isNull();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testSetFlightInstructorByHelper(TestModelArgs args) {
    init(args);
    tweety.setFlightInstructor(timmy);
    assertThat(tweety.getFlightInstructor()).isEqualTo(timmy);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testUpdateFlightInstructorByIdHelper(TestModelArgs args) {
    init(args);
    hedwig.setId("new-" + hedwig.getId());
    assertThat(tweety.getFlightInstructor()).isEqualTo(hedwig);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testUpdateFlightInstructorByIdAttributeName(TestModelArgs args) {
    init(args);
    hedwig.setAttributeValue("id", "new-" + hedwig.getId(), true);
    assertThat(tweety.getFlightInstructor()).isEqualTo(hedwig);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testUpdateFlightInstructorByReplaceElement(TestModelArgs args) {
    init(args);
    hedwig.replaceWithElement(timmy);
    assertThat(tweety.getFlightInstructor()).isEqualTo(timmy);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testUpdateFlightInstructorByRemoveElement(TestModelArgs args) {
    init(args);
    Animals animals = (Animals) modelInstance.getDocumentElement();
    animals.getAnimals().remove(hedwig);
    assertThat(tweety.getFlightInstructor()).isNull();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testClearFlightInstructor(TestModelArgs args) {
    init(args);
    tweety.removeFlightInstructor();
    assertThat(tweety.getFlightInstructor()).isNull();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testAddFlightPartnerRefsByHelper(TestModelArgs args) {
    init(args);
    assertThat(tweety.getFlightPartnerRefs())
      .isNotEmpty()
      .hasSize(4)
      .containsOnly(hedwig, birdo, plucky, fiffy);

    tweety.getFlightPartnerRefs().add(timmy);
    tweety.getFlightPartnerRefs().add(daisy);

    assertThat(tweety.getFlightPartnerRefs())
      .isNotEmpty()
      .hasSize(6)
      .containsOnly(hedwig, birdo, plucky, fiffy, timmy, daisy);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testUpdateFlightPartnerRefsByIdByHelper(TestModelArgs args) {
    init(args);
    hedwig.setId("new-" + hedwig.getId());
    plucky.setId("new-" + plucky.getId());
    assertThat(tweety.getFlightPartnerRefs())
      .hasSize(4)
      .containsOnly(hedwig, birdo, plucky, fiffy);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testUpdateFlightPartnerRefsByIdByAttributeName(TestModelArgs args) {
    init(args);
    birdo.setAttributeValue("id", "new-" + birdo.getId(), true);
    fiffy.setAttributeValue("id", "new-" + fiffy.getId(), true);
    assertThat(tweety.getFlightPartnerRefs())
      .hasSize(4)
      .containsOnly(hedwig, birdo, plucky, fiffy);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testUpdateFlightPartnerRefsByReplaceElements(TestModelArgs args) {
    init(args);
    hedwig.replaceWithElement(timmy);
    plucky.replaceWithElement(daisy);
    assertThat(tweety.getFlightPartnerRefs())
      .hasSize(4)
      .containsOnly(birdo, fiffy, timmy ,daisy);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testUpdateFlightPartnerRefsByRemoveElements(TestModelArgs args) {
    init(args);
    tweety.getFlightPartnerRefs().remove(birdo);
    tweety.getFlightPartnerRefs().remove(fiffy);
    assertThat(tweety.getFlightPartnerRefs())
      .hasSize(2)
      .containsOnly(hedwig, plucky);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testClearFlightPartnerRefs(TestModelArgs args) {
    init(args);
    tweety.getFlightPartnerRefs().clear();
    assertThat(tweety.getFlightPartnerRefs()).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testAddFlightPartnerRefElementsByHelper(TestModelArgs args) {
    init(args);
    assertThat(tweety.getFlightPartnerRefElements())
      .isNotEmpty()
      .hasSize(4);

    FlightPartnerRef timmyFlightPartnerRef = modelInstance.newInstance(FlightPartnerRef.class);
    timmyFlightPartnerRef.setTextContent(timmy.getId());
    tweety.getFlightPartnerRefElements().add(timmyFlightPartnerRef);

    FlightPartnerRef daisyFlightPartnerRef = modelInstance.newInstance(FlightPartnerRef.class);
    daisyFlightPartnerRef.setTextContent(daisy.getId());
    tweety.getFlightPartnerRefElements().add(daisyFlightPartnerRef);

    assertThat(tweety.getFlightPartnerRefElements())
      .isNotEmpty()
      .hasSize(6)
      .contains(timmyFlightPartnerRef, daisyFlightPartnerRef);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testFlightPartnerRefElementsByTextContent(TestModelArgs args) {
    init(args);
    Collection<FlightPartnerRef> flightPartnerRefElements = tweety.getFlightPartnerRefElements();
    Collection<String> textContents = new ArrayList<>();
    for (FlightPartnerRef flightPartnerRefElement : flightPartnerRefElements) {
      String textContent = flightPartnerRefElement.getTextContent();
      assertThat(textContent).isNotEmpty();
      textContents.add(textContent);
    }
    assertThat(textContents)
      .isNotEmpty()
      .hasSize(4)
      .containsOnly(hedwig.getId(), birdo.getId(), plucky.getId(), fiffy.getId());
  }

  @ParameterizedTest
  @MethodSource("models")
  void testUpdateFlightPartnerRefElementsByTextContent(TestModelArgs args) {
    init(args);
    List<FlightPartnerRef> flightPartnerRefs = new ArrayList<>(tweety.getFlightPartnerRefElements());

    flightPartnerRefs.get(0).setTextContent(timmy.getId());
    flightPartnerRefs.get(2).setTextContent(daisy.getId());

    assertThat(tweety.getFlightPartnerRefs())
      .hasSize(4)
      .containsOnly(birdo, fiffy, timmy, daisy);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testUpdateFlightPartnerRefElementsByRemoveElements(TestModelArgs args) {
    init(args);
    List<FlightPartnerRef> flightPartnerRefs = new ArrayList<>(tweety.getFlightPartnerRefElements());
    tweety.getFlightPartnerRefElements().remove(flightPartnerRefs.get(1));
    tweety.getFlightPartnerRefElements().remove(flightPartnerRefs.get(3));
    assertThat(tweety.getFlightPartnerRefs())
      .hasSize(2)
      .containsOnly(hedwig, plucky);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testClearFlightPartnerRefElements(TestModelArgs args) {
    init(args);
    tweety.getFlightPartnerRefElements().clear();
    assertThat(tweety.getFlightPartnerRefElements()).isEmpty();

    // should not affect animals collection
    Animals animals = (Animals) modelInstance.getDocumentElement();
    assertThat(animals.getAnimals())
      .isNotEmpty()
      .hasSize(7);
  }

}
