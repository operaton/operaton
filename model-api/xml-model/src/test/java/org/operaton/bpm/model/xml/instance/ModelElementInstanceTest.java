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
package org.operaton.bpm.model.xml.instance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.operaton.bpm.model.xml.ModelInstance;
import org.operaton.bpm.model.xml.testmodel.Gender;
import org.operaton.bpm.model.xml.testmodel.TestModelParser;
import org.operaton.bpm.model.xml.testmodel.TestModelTest;
import org.operaton.bpm.model.xml.testmodel.instance.Animals;
import org.operaton.bpm.model.xml.testmodel.instance.Bird;
import org.operaton.bpm.model.xml.type.ModelElementType;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.model.xml.testmodel.TestModelConstants.MODEL_NAMESPACE;

/**
 * @author Daniel Meyer
 * @author Sebastian Menski
 */
class ModelElementInstanceTest extends TestModelTest {

  private Animals animals;
  private Bird tweety;
  private Bird donald;
  private Bird daisy;
  private Bird hedwig;

  static Stream<Arguments> models() {
    return Stream.of(createModel(), parseModel(ModelElementInstanceTest.class)).map(Arguments::of);
  }

  private static TestModelArgs createModel() {
    TestModelParser modelParser = new TestModelParser();
    ModelInstance modelInstance = modelParser.getEmptyModel();

    Animals animals = modelInstance.newInstance(Animals.class);
    modelInstance.setDocumentElement(animals);

    createBird(modelInstance, "tweety", Gender.Female);
    Bird donald = createBird(modelInstance, "donald", Gender.Male);
    Bird daisy = createBird(modelInstance, "daisy", Gender.Female);
    Bird hedwig = createBird(modelInstance, "hedwig", Gender.Male);

    donald.setTextContent("some text content");
    daisy.setTextContent("\n        some text content with outer line breaks\n    ");
    hedwig.setTextContent("\n        some text content with inner\n        line breaks\n    ");

    return new TestModelArgs("created", modelInstance, modelParser);
  }

  @Override
  protected void init(TestModelArgs args) {
    super.init(args);

    animals = (Animals) modelInstance.getDocumentElement();
    tweety = modelInstance.getModelElementById("tweety");
    donald = modelInstance.getModelElementById("donald");
    daisy = modelInstance.getModelElementById("daisy");
    hedwig = modelInstance.getModelElementById("hedwig");
  }

  @ParameterizedTest
  @MethodSource("models")
  void testAttribute(TestModelArgs args) {
    init(args);
    String tweetyName = tweety.getId() + "-name";
    tweety.setAttributeValue("name", tweetyName);
    assertThat(tweety.getAttributeValue("name")).isEqualTo(tweetyName);
    tweety.removeAttribute("name");
    assertThat(tweety.getAttributeValue("name")).isNull();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testAttributeWithNamespace(TestModelArgs args) {
    init(args);
    String tweetyName = tweety.getId() + "-name";
    tweety.setAttributeValueNs(MODEL_NAMESPACE, "name", tweetyName);
    assertThat(tweety.getAttributeValue("name")).isEqualTo(tweetyName);
    assertThat(tweety.getAttributeValueNs(MODEL_NAMESPACE, "name")).isEqualTo(tweetyName);
    tweety.removeAttributeNs(MODEL_NAMESPACE, "name");
    assertThat(tweety.getAttributeValue("name")).isNull();
    assertThat(tweety.getAttributeValueNs(MODEL_NAMESPACE, "name")).isNull();
  }

  @ParameterizedTest
  @MethodSource("models")
  void TestElementType(TestModelArgs args) {
    init(args);
    ModelElementType birdType = modelInstance.getModel().getType(Bird.class);
    assertThat(tweety.getElementType()).isEqualTo(birdType);
    assertThat(donald.getElementType()).isEqualTo(birdType);
    assertThat(daisy.getElementType()).isEqualTo(birdType);
    assertThat(hedwig.getElementType()).isEqualTo(birdType);
  }

  @ParameterizedTest
  @MethodSource("models")
  void TestParentElement(TestModelArgs args) {
    init(args);
    assertThat(tweety.getParentElement()).isEqualTo(animals);
    assertThat(donald.getParentElement()).isEqualTo(animals);
    assertThat(daisy.getParentElement()).isEqualTo(animals);
    assertThat(hedwig.getParentElement()).isEqualTo(animals);

    Bird timmy = modelInstance.newInstance(Bird.class);
    timmy.setId("timmy");
    timmy.setGender(Gender.Male);
    assertThat(timmy.getParentElement()).isNull();
  }

  @ParameterizedTest
  @MethodSource("models")
  void TestModelInstance(TestModelArgs args) {
    init(args);
    assertThat(tweety.getModelInstance()).isEqualTo(modelInstance);
    assertThat(donald.getModelInstance()).isEqualTo(modelInstance);
    assertThat(daisy.getModelInstance()).isEqualTo(modelInstance);
    assertThat(hedwig.getModelInstance()).isEqualTo(modelInstance);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testReplaceWithElement(TestModelArgs args) {
    init(args);
    Bird timmy = modelInstance.newInstance(Bird.class);
    timmy.setId("timmy");
    timmy.setGender(Gender.Male);

    assertThat(animals.getAnimals())
      .contains(tweety)
      .doesNotContain(timmy);

    tweety.replaceWithElement(timmy);

    assertThat(animals.getAnimals())
      .contains(timmy)
      .doesNotContain(tweety);
  }

  @ParameterizedTest
  @MethodSource("models")
  void testReplaceRootElement(TestModelArgs args) {
    init(args);
    assertThat(((Animals) modelInstance.getDocumentElement()).getAnimals()).isNotEmpty();
    Animals newAnimals = modelInstance.newInstance(Animals.class);
    modelInstance.setDocumentElement(newAnimals);
    assertThat(((Animals) modelInstance.getDocumentElement()).getAnimals()).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("models")
  void testTextContent(TestModelArgs args) {
    init(args);
    assertThat(tweety.getTextContent()).isEmpty();
    assertThat(donald.getTextContent()).isEqualTo("some text content");
    assertThat(daisy.getTextContent()).isEqualTo("some text content with outer line breaks");
    assertThat(hedwig.getTextContent()).isEqualTo("some text content with inner\n        line breaks");

    String testContent = "\n test content \n \n \t operaton.org \t    \n   ";
    tweety.setTextContent(testContent);
    assertThat(tweety.getTextContent()).isEqualTo(testContent.trim());
  }

  @ParameterizedTest
  @MethodSource("models")
  void testRawTextContent(TestModelArgs args) {
    init(args);
    assertThat(tweety.getRawTextContent()).isEmpty();
    assertThat(donald.getRawTextContent()).isEqualTo("some text content");
    assertThat(daisy.getRawTextContent()).isEqualTo("\n        some text content with outer line breaks\n    ");
    assertThat(hedwig.getRawTextContent()).isEqualTo("\n        some text content with inner\n        line breaks\n    ");

    String testContent = "\n test content \n \n \t operaton.org \t    \n   ";
    tweety.setTextContent(testContent);
    assertThat(tweety.getRawTextContent()).isEqualTo(testContent);
  }

}
