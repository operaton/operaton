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
package org.operaton.bpm.model.xml.testmodel;

import java.io.InputStream;

import org.junit.jupiter.api.AfterEach;

import org.operaton.bpm.model.xml.ModelInstance;
import org.operaton.bpm.model.xml.testmodel.instance.*;

/**
 * @author Sebastian Menski
 */
public abstract class TestModelTest {
  protected ModelInstance modelInstance;
  protected TestModelParser modelParser;

  public record TestModelArgs (String name, ModelInstance modelInstance, TestModelParser modelParser) {}

  protected static TestModelArgs parseModel(Class<?> test) {
    TestModelParser modelParser = new TestModelParser();
    String testXml = test.getSimpleName() + ".xml";
    InputStream testXmlAsStream = test.getResourceAsStream(testXml);
    ModelInstance modelInstance = modelParser.parseModelFromStream(testXmlAsStream);
    return new TestModelArgs ("parsed", modelInstance, modelParser);
  }

  public static Bird createBird(ModelInstance modelInstance, String id, Gender gender) {
    Bird bird = modelInstance.newInstance(Bird.class, id);
    bird.setGender(gender);
    Animals animals = (Animals) modelInstance.getDocumentElement();
    animals.getAnimals().add(bird);
    return bird;
  }

  protected static RelationshipDefinition createRelationshipDefinition(ModelInstance modelInstance, Animal animalInRelationshipWith, Class<? extends RelationshipDefinition> relationshipDefinitionClass) {
    RelationshipDefinition relationshipDefinition = modelInstance.newInstance(relationshipDefinitionClass, "relationship-" + animalInRelationshipWith.getId());
    relationshipDefinition.setAnimal(animalInRelationshipWith);
    return relationshipDefinition;
  }

  public static void addRelationshipDefinition(Animal animalWithRelationship, RelationshipDefinition relationshipDefinition) {
    Animal animalInRelationshipWith = relationshipDefinition.getAnimal();
    relationshipDefinition.setId(animalWithRelationship.getId() + "-" + animalInRelationshipWith.getId());
    animalWithRelationship.getRelationshipDefinitions().add(relationshipDefinition);
  }

  public static Egg createEgg(ModelInstance modelInstance, String id) {
    return modelInstance.newInstance(Egg.class, id);
  }

  protected void init (TestModelArgs args) {
    this.modelInstance = args.modelInstance.copy();
    this.modelParser = args.modelParser;
  }

  @AfterEach
  protected void validateModel() {
    if (modelParser != null && modelInstance != null) {
      modelParser.validateModel(modelInstance.getDocument());
      modelParser = null;
      modelInstance = null;
    }
  }
}
