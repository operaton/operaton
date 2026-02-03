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
import org.operaton.bpm.model.xml.ModelValidationException;
import org.operaton.bpm.model.xml.testmodel.Gender;
import org.operaton.bpm.model.xml.testmodel.TestModelParser;
import org.operaton.bpm.model.xml.testmodel.TestModelTest;

import static org.operaton.bpm.model.xml.testmodel.TestModelConstants.MODEL_NAMESPACE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Sebastian Menski
 */
public class AnimalTest extends TestModelTest {

  private Animal tweety;
  private Animal hedwig;
  private Animal birdo;
  private Animal plucky;
  private Animal fiffy;
  private Animal timmy;
  private Animal daisy;
  private RelationshipDefinition hedwigRelationship;
  private RelationshipDefinition birdoRelationship;
  private RelationshipDefinition pluckyRelationship;
  private RelationshipDefinition fiffyRelationship;
  private RelationshipDefinition timmyRelationship;
  private RelationshipDefinition daisyRelationship;

  static Stream<Arguments> models() {
    return Stream.of(createModel(), parseModel(AnimalTest.class)).map(Arguments::of);
  }

  public static TestModelArgs createModel() {
    TestModelParser modelParser = new TestModelParser();
    ModelInstance modelInstance = modelParser.getEmptyModel();

    Animals animals = modelInstance.newInstance(Animals.class);
    modelInstance.setDocumentElement(animals);

    // add a tns namespace prefix for QName testing
    animals.getDomElement().registerNamespace("tns", MODEL_NAMESPACE);

    Animal tweety = createBird(modelInstance, "tweety", Gender.Female);
    Animal hedwig = createBird(modelInstance, "hedwig", Gender.Male);
    Animal birdo = createBird(modelInstance, "birdo", Gender.Female);
    Animal plucky = createBird(modelInstance, "plucky", Gender.Unknown);
    Animal fiffy = createBird(modelInstance, "fiffy", Gender.Female);
    createBird(modelInstance, "timmy", Gender.Male);
    createBird(modelInstance, "daisy", Gender.Female);

    // create and add some relationships
    RelationshipDefinition hedwigRelationship = createRelationshipDefinition(modelInstance, hedwig, ChildRelationshipDefinition.class);
    addRelationshipDefinition(tweety, hedwigRelationship);
    RelationshipDefinition birdoRelationship = createRelationshipDefinition(modelInstance, birdo, ChildRelationshipDefinition.class);
    addRelationshipDefinition(tweety, birdoRelationship);
    RelationshipDefinition pluckyRelationship = createRelationshipDefinition(modelInstance, plucky, FriendRelationshipDefinition.class);
    addRelationshipDefinition(tweety, pluckyRelationship);
    RelationshipDefinition fiffyRelationship = createRelationshipDefinition(modelInstance, fiffy, FriendRelationshipDefinition.class);
    addRelationshipDefinition(tweety, fiffyRelationship);

    tweety.getRelationshipDefinitionRefs().add(hedwigRelationship);
    tweety.getRelationshipDefinitionRefs().add(birdoRelationship);
    tweety.getRelationshipDefinitionRefs().add(pluckyRelationship);
    tweety.getRelationshipDefinitionRefs().add(fiffyRelationship);

    tweety.getBestFriends().add(birdo);
    tweety.getBestFriends().add(plucky);

    return new TestModelArgs("created", modelInstance, modelParser);
  }

  @Override
  protected void init(TestModelArgs args) {
    super.init(args);

    tweety = modelInstance.getModelElementById("tweety");
    hedwig = modelInstance.getModelElementById("hedwig");
    birdo = modelInstance.getModelElementById("birdo");
    plucky = modelInstance.getModelElementById("plucky");
    fiffy = modelInstance.getModelElementById("fiffy");
    timmy = modelInstance.getModelElementById("timmy");
    daisy = modelInstance.getModelElementById("daisy");

    hedwigRelationship = modelInstance.getModelElementById("tweety-hedwig");
    birdoRelationship = modelInstance.getModelElementById("tweety-birdo");
    pluckyRelationship = modelInstance.getModelElementById("tweety-plucky");
    fiffyRelationship = modelInstance.getModelElementById("tweety-fiffy");

    timmyRelationship = createRelationshipDefinition(modelInstance, timmy, FriendRelationshipDefinition.class);
    daisyRelationship = createRelationshipDefinition(modelInstance, daisy, ChildRelationshipDefinition.class);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testSetIdAttributeByHelper(TestModelArgs args) {
    init(args);
    String newId = "new-" + tweety.getId();
    tweety.setId(newId);
    assertThat(tweety.getId()).isEqualTo(newId);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testSetIdAttributeByAttributeName(TestModelArgs args) {
    init(args);
    tweety.setAttributeValue("id", "duffy", true);
    assertThat(tweety.getId()).isEqualTo("duffy");
  }

  @ParameterizedTest
  @MethodSource("models")
  void testRemoveIdAttribute(TestModelArgs args) {
    init(args);
    tweety.removeAttribute("id");
    assertThat(tweety.getId()).isNull();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testSetNameAttributeByHelper(TestModelArgs args) {
    init(args);
    tweety.setName("tweety");
    assertThat(tweety.getName()).isEqualTo("tweety");
  }

  @ParameterizedTest
  @MethodSource("models")
  void testSetNameAttributeByAttributeName(TestModelArgs args) {
    init(args);
    tweety.setAttributeValue("name", "daisy");
    assertThat(tweety.getName()).isEqualTo("daisy");
  }

  @ParameterizedTest
  @MethodSource("models")
  void testRemoveNameAttribute(TestModelArgs args) {
    init(args);
    tweety.removeAttribute("name");
    assertThat(tweety.getName()).isNull();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testSetFatherAttributeByHelper(TestModelArgs args) {
    init(args);
    tweety.setFather(timmy);
    assertThat(tweety.getFather()).isEqualTo(timmy);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testSetFatherAttributeByAttributeName(TestModelArgs args) {
    init(args);
    tweety.setAttributeValue("father", timmy.getId());
    assertThat(tweety.getFather()).isEqualTo(timmy);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testSetFatherAttributeByAttributeNameWithNamespace(TestModelArgs args) {
    init(args);
    tweety.setAttributeValue("father", "tns:hedwig");
    assertThat(tweety.getFather()).isEqualTo(hedwig);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testRemoveFatherAttribute(TestModelArgs args) {
    init(args);
    tweety.setFather(timmy);
    assertThat(tweety.getFather()).isEqualTo(timmy);
    tweety.removeAttribute("father");
    assertThat(tweety.getFather()).isNull();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testChangeIdAttributeOfFatherReference(TestModelArgs args) {
    init(args);
    tweety.setFather(timmy);
    assertThat(tweety.getFather()).isEqualTo(timmy);
    timmy.setId("new-" + timmy.getId());
    assertThat(tweety.getFather()).isEqualTo(timmy);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testReplaceFatherReferenceWithNewAnimal(TestModelArgs args) {
    init(args);
    tweety.setFather(timmy);
    assertThat(tweety.getFather()).isEqualTo(timmy);
    timmy.replaceWithElement(plucky);
    assertThat(tweety.getFather()).isEqualTo(plucky);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testSetMotherAttributeByHelper(TestModelArgs args) {
    init(args);
    tweety.setMother(daisy);
    assertThat(tweety.getMother()).isEqualTo(daisy);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testSetMotherAttributeByAttributeName(TestModelArgs args) {
    init(args);
    tweety.setAttributeValue("mother", fiffy.getId());
    assertThat(tweety.getMother()).isEqualTo(fiffy);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testRemoveMotherAttribute(TestModelArgs args) {
    init(args);
    tweety.setMother(daisy);
    assertThat(tweety.getMother()).isEqualTo(daisy);
    tweety.removeAttribute("mother");
    assertThat(tweety.getMother()).isNull();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testReplaceMotherReferenceWithNewAnimal(TestModelArgs args) {
    init(args);
    tweety.setMother(daisy);
    assertThat(tweety.getMother()).isEqualTo(daisy);
    daisy.replaceWithElement(birdo);
    assertThat(tweety.getMother()).isEqualTo(birdo);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testChangeIdAttributeOfMotherReference(TestModelArgs args) {
    init(args);
    tweety.setMother(daisy);
    assertThat(tweety.getMother()).isEqualTo(daisy);
    daisy.setId("new-" + daisy.getId());
    assertThat(tweety.getMother()).isEqualTo(daisy);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testSetIsEndangeredAttributeByHelper(TestModelArgs args) {
    init(args);
    tweety.setIsEndangered(true);
    assertThat(tweety.isEndangered()).isTrue();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testSetIsEndangeredAttributeByAttributeName(TestModelArgs args) {
    init(args);
    tweety.setAttributeValue("isEndangered", "false");
    assertThat(tweety.isEndangered()).isFalse();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testRemoveIsEndangeredAttribute(TestModelArgs args) {
    init(args);
    tweety.removeAttribute("isEndangered");
    // default value of isEndangered: false
    assertThat(tweety.isEndangered()).isFalse();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testSetGenderAttributeByHelper(TestModelArgs args) {
    init(args);
    tweety.setGender(Gender.Male);
    assertThat(tweety.getGender()).isEqualTo(Gender.Male);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testSetGenderAttributeByAttributeName(TestModelArgs args) {
    init(args);
    tweety.setAttributeValue("gender", Gender.Unknown.toString());
    assertThat(tweety.getGender()).isEqualTo(Gender.Unknown);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testRemoveGenderAttribute(TestModelArgs args) {
    init(args);tweety.removeAttribute("gender");
    assertThat(tweety.getGender()).isNull();

    // gender is required, so the model is invalid without
    assertThatThrownBy(this::validateModel)
      .isInstanceOf(ModelValidationException.class);

    // add gender to make model valid
    tweety.setGender(Gender.Female);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testSetAgeAttributeByHelper(TestModelArgs args) {
    init(args);
    tweety.setAge(13);
    assertThat(tweety.getAge()).isEqualTo(13);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testSetAgeAttributeByAttributeName(TestModelArgs args) {
    init(args);
    tweety.setAttributeValue("age", "23");
    assertThat(tweety.getAge()).isEqualTo(23);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testRemoveAgeAttribute(TestModelArgs args) {
    init(args);
    tweety.removeAttribute("age");
    assertThat(tweety.getAge()).isNull();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testAddRelationshipDefinitionsByHelper(TestModelArgs args) {
    init(args);
    assertThat(tweety.getRelationshipDefinitions())
      .isNotEmpty()
      .hasSize(4)
      .containsOnly(hedwigRelationship, birdoRelationship, pluckyRelationship, fiffyRelationship);

    tweety.getRelationshipDefinitions().add(timmyRelationship);
    tweety.getRelationshipDefinitions().add(daisyRelationship);

    assertThat(tweety.getRelationshipDefinitions())
      .hasSize(6)
      .containsOnly(hedwigRelationship, birdoRelationship, pluckyRelationship, fiffyRelationship, timmyRelationship, daisyRelationship);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testUpdateRelationshipDefinitionsByIdByHelper(TestModelArgs args) {
    init(args);
    hedwigRelationship.setId("new-" + hedwigRelationship.getId());
    pluckyRelationship.setId("new-" + pluckyRelationship.getId());
    assertThat(tweety.getRelationshipDefinitions())
      .hasSize(4)
      .containsOnly(hedwigRelationship, birdoRelationship, pluckyRelationship, fiffyRelationship);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testUpdateRelationshipDefinitionsByIdByAttributeName(TestModelArgs args) {
    init(args);
    birdoRelationship.setAttributeValue("id", "new-" + birdoRelationship.getId(), true);
    fiffyRelationship.setAttributeValue("id", "new-" + fiffyRelationship.getId(), true);
    assertThat(tweety.getRelationshipDefinitions())
      .hasSize(4)
      .containsOnly(hedwigRelationship, birdoRelationship, pluckyRelationship, fiffyRelationship);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testUpdateRelationshipDefinitionsByReplaceElements(TestModelArgs args) {
    init(args);
    hedwigRelationship.replaceWithElement(timmyRelationship);
    pluckyRelationship.replaceWithElement(daisyRelationship);
    assertThat(tweety.getRelationshipDefinitions())
      .hasSize(4)
      .containsOnly(birdoRelationship, fiffyRelationship, timmyRelationship, daisyRelationship);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testUpdateRelationshipDefinitionsByRemoveElements(TestModelArgs args) {
    init(args);
    tweety.getRelationshipDefinitions().remove(birdoRelationship);
    tweety.getRelationshipDefinitions().remove(fiffyRelationship);
    assertThat(tweety.getRelationshipDefinitions())
      .hasSize(2)
      .containsOnly(hedwigRelationship, pluckyRelationship);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testClearRelationshipDefinitions(TestModelArgs args) {
    init(args);
    tweety.getRelationshipDefinitions().clear();
    assertThat(tweety.getRelationshipDefinitions()).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testAddRelationsDefinitionRefsByHelper(TestModelArgs args) {
    init(args);
    assertThat(tweety.getRelationshipDefinitionRefs())
      .isNotEmpty()
      .hasSize(4)
      .containsOnly(hedwigRelationship, birdoRelationship, pluckyRelationship, fiffyRelationship);

    addRelationshipDefinition(tweety, timmyRelationship);
    addRelationshipDefinition(tweety, daisyRelationship);
    tweety.getRelationshipDefinitionRefs().add(timmyRelationship);
    tweety.getRelationshipDefinitionRefs().add(daisyRelationship);

    assertThat(tweety.getRelationshipDefinitionRefs())
      .isNotEmpty()
      .hasSize(6)
      .containsOnly(hedwigRelationship, birdoRelationship, pluckyRelationship, fiffyRelationship, timmyRelationship, daisyRelationship);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testUpdateRelationshipDefinitionRefsByIdByHelper(TestModelArgs args) {
    init(args);
    hedwigRelationship.setId("child-relationship");
    pluckyRelationship.setId("friend-relationship");
    assertThat(tweety.getRelationshipDefinitionRefs())
      .hasSize(4)
      .containsOnly(hedwigRelationship, birdoRelationship, pluckyRelationship, fiffyRelationship);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testUpdateRelationshipDefinitionRefsByIdByAttributeName(TestModelArgs args) {
    init(args);
    birdoRelationship.setAttributeValue("id", "birdo-relationship", true);
    fiffyRelationship.setAttributeValue("id", "fiffy-relationship", true);
    assertThat(tweety.getRelationshipDefinitionRefs())
      .hasSize(4)
      .containsOnly(hedwigRelationship, birdoRelationship, pluckyRelationship, fiffyRelationship);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testUpdateRelationshipDefinitionRefsByReplaceElements(TestModelArgs args) {
    init(args);
    hedwigRelationship.replaceWithElement(timmyRelationship);
    pluckyRelationship.replaceWithElement(daisyRelationship);
    assertThat(tweety.getRelationshipDefinitionRefs())
      .hasSize(4)
      .containsOnly(birdoRelationship, fiffyRelationship, timmyRelationship, daisyRelationship);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testUpdateRelationshipDefinitionRefsByRemoveElements(TestModelArgs args) {
    init(args);
    tweety.getRelationshipDefinitions().remove(birdoRelationship);
    tweety.getRelationshipDefinitions().remove(fiffyRelationship);
    assertThat(tweety.getRelationshipDefinitionRefs())
      .hasSize(2)
      .containsOnly(hedwigRelationship, pluckyRelationship);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testUpdateRelationshipDefinitionRefsByRemoveIdAttribute(TestModelArgs args) {
    init(args);
    birdoRelationship.removeAttribute("id");
    pluckyRelationship.removeAttribute("id");
    assertThat(tweety.getRelationshipDefinitionRefs())
      .hasSize(2)
      .containsOnly(hedwigRelationship, fiffyRelationship);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testClearRelationshipDefinitionsRefs(TestModelArgs args) {
    init(args);
    tweety.getRelationshipDefinitionRefs().clear();
    assertThat(tweety.getRelationshipDefinitionRefs()).isEmpty();
    // should not affect animal relationship definitions
    assertThat(tweety.getRelationshipDefinitions()).hasSize(4);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testClearRelationshipDefinitionRefsByClearRelationshipDefinitions(TestModelArgs args) {
    init(args);
    assertThat(tweety.getRelationshipDefinitionRefs()).isNotEmpty();
    tweety.getRelationshipDefinitions().clear();
    assertThat(tweety.getRelationshipDefinitions()).isEmpty();
    // should affect animal relationship definition refs
    assertThat(tweety.getRelationshipDefinitionRefs()).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testAddRelationshipDefinitionRefElementsByHelper(TestModelArgs args) {
    init(args);
    assertThat(tweety.getRelationshipDefinitionRefElements())
      .isNotEmpty()
      .hasSize(4);

    addRelationshipDefinition(tweety, timmyRelationship);
    RelationshipDefinitionRef timmyRelationshipDefinitionRef = modelInstance.newInstance(RelationshipDefinitionRef.class);
    timmyRelationshipDefinitionRef.setTextContent(timmyRelationship.getId());
    tweety.getRelationshipDefinitionRefElements().add(timmyRelationshipDefinitionRef);

    addRelationshipDefinition(tweety, daisyRelationship);
    RelationshipDefinitionRef daisyRelationshipDefinitionRef = modelInstance.newInstance(RelationshipDefinitionRef.class);
    daisyRelationshipDefinitionRef.setTextContent(daisyRelationship.getId());
    tweety.getRelationshipDefinitionRefElements().add(daisyRelationshipDefinitionRef);

    assertThat(tweety.getRelationshipDefinitionRefElements())
      .isNotEmpty()
      .hasSize(6)
      .contains(timmyRelationshipDefinitionRef, daisyRelationshipDefinitionRef);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testRelationshipDefinitionRefElementsByTextContent(TestModelArgs args) {
    init(args);
    Collection<RelationshipDefinitionRef> relationshipDefinitionRefElements = tweety.getRelationshipDefinitionRefElements();
    Collection<String> textContents = new ArrayList<>();
    for (RelationshipDefinitionRef relationshipDefinitionRef : relationshipDefinitionRefElements) {
      String textContent = relationshipDefinitionRef.getTextContent();
      assertThat(textContent).isNotEmpty();
      textContents.add(textContent);
    }
    assertThat(textContents)
      .isNotEmpty()
      .hasSize(4)
      .containsOnly(hedwigRelationship.getId(), birdoRelationship.getId(), pluckyRelationship.getId(), fiffyRelationship.getId());
  }

  @ParameterizedTest
  @MethodSource("models")
  void testUpdateRelationshipDefinitionRefElementsByTextContent(TestModelArgs args) {
    init(args);
    List<RelationshipDefinitionRef> relationshipDefinitionRefs = new ArrayList<>(tweety.getRelationshipDefinitionRefElements());

    addRelationshipDefinition(tweety, timmyRelationship);
    relationshipDefinitionRefs.get(0).setTextContent(timmyRelationship.getId());

    addRelationshipDefinition(daisy, daisyRelationship);
    relationshipDefinitionRefs.get(2).setTextContent(daisyRelationship.getId());

    assertThat(tweety.getRelationshipDefinitionRefs())
      .hasSize(4)
      .containsOnly(birdoRelationship, fiffyRelationship, timmyRelationship, daisyRelationship);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testUpdateRelationshipDefinitionRefElementsByTextContentWithNamespace(TestModelArgs args) {
    init(args);
    List<RelationshipDefinitionRef> relationshipDefinitionRefs = new ArrayList<>(tweety.getRelationshipDefinitionRefElements());

    addRelationshipDefinition(tweety, timmyRelationship);
    relationshipDefinitionRefs.get(0).setTextContent("tns:" + timmyRelationship.getId());

    addRelationshipDefinition(daisy, daisyRelationship);
    relationshipDefinitionRefs.get(2).setTextContent("tns:" + daisyRelationship.getId());

    assertThat(tweety.getRelationshipDefinitionRefs())
      .hasSize(4)
      .containsOnly(birdoRelationship, fiffyRelationship, timmyRelationship, daisyRelationship);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testUpdateRelationshipDefinitionRefElementsByRemoveElements(TestModelArgs args) {
    init(args);
    List<RelationshipDefinitionRef> relationshipDefinitionRefs = new ArrayList<>(tweety.getRelationshipDefinitionRefElements());
    tweety.getRelationshipDefinitionRefElements().remove(relationshipDefinitionRefs.get(1));
    tweety.getRelationshipDefinitionRefElements().remove(relationshipDefinitionRefs.get(3));
    assertThat(tweety.getRelationshipDefinitionRefs())
      .hasSize(2)
      .containsOnly(hedwigRelationship, pluckyRelationship);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testClearRelationshipDefinitionRefElements(TestModelArgs args) {
    init(args);
    tweety.getRelationshipDefinitionRefElements().clear();
    assertThat(tweety.getRelationshipDefinitionRefElements()).isEmpty();
    assertThat(tweety.getRelationshipDefinitionRefs()).isEmpty();
    // should not affect animal relationship definitions
    assertThat(tweety.getRelationshipDefinitions())
      .isNotEmpty()
      .hasSize(4);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testClearRelationshipDefinitionRefElementsByClearRelationshipDefinitionRefs(TestModelArgs args) {
    init(args);
    tweety.getRelationshipDefinitionRefs().clear();
    assertThat(tweety.getRelationshipDefinitionRefs()).isEmpty();
    assertThat(tweety.getRelationshipDefinitionRefElements()).isEmpty();
    // should not affect animal relationship definitions
    assertThat(tweety.getRelationshipDefinitions())
      .isNotEmpty()
      .hasSize(4);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testClearRelationshipDefinitionRefElementsByClearRelationshipDefinitions(TestModelArgs args) {
    init(args);
    tweety.getRelationshipDefinitions().clear();
    assertThat(tweety.getRelationshipDefinitionRefs()).isEmpty();
    assertThat(tweety.getRelationshipDefinitionRefElements()).isEmpty();
    // should affect animal relationship definitions
    assertThat(tweety.getRelationshipDefinitions()).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testGetBestFriends(TestModelArgs args) {
    init(args);
    Collection<Animal> bestFriends = tweety.getBestFriends();

    assertThat(bestFriends)
      .isNotEmpty()
      .hasSize(2)
      .containsOnly(birdo, plucky);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testAddBestFriend(TestModelArgs args) {
    init(args);
    tweety.getBestFriends().add(daisy);

    Collection<Animal> bestFriends = tweety.getBestFriends();

    assertThat(bestFriends)
      .isNotEmpty()
      .hasSize(3)
      .containsOnly(birdo, plucky, daisy);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testRemoveBestFriendRef(TestModelArgs args) {
    init(args);
    tweety.getBestFriends().remove(plucky);

    Collection<Animal> bestFriends = tweety.getBestFriends();

    assertThat(bestFriends)
      .isNotEmpty()
      .hasSize(1)
      .containsOnly(birdo);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testClearBestFriendRef(TestModelArgs args) {
    init(args);
    tweety.getBestFriends().clear();

    Collection<Animal> bestFriends = tweety.getBestFriends();

    assertThat(bestFriends)
      .isEmpty();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testClearAndAddBestFriendRef(TestModelArgs args) {
    init(args);
    tweety.getBestFriends().clear();

    Collection<Animal> bestFriends = tweety.getBestFriends();

    assertThat(bestFriends)
      .isEmpty();

    bestFriends.add(daisy);

    assertThat(bestFriends)
      .hasSize(1)
      .containsOnly(daisy);
  }
}
