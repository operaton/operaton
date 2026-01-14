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

import org.junit.jupiter.api.Test;

import org.operaton.bpm.model.xml.ModelInstance;
import org.operaton.bpm.model.xml.testmodel.instance.Animal;
import org.operaton.bpm.model.xml.testmodel.instance.Animals;
import org.operaton.bpm.model.xml.testmodel.instance.Bird;

import static org.assertj.core.api.Assertions.assertThat;

class TestModelInstanceTest {

  @Test
  void testClone() {
    ModelInstance modelInstance = new TestModelParser().getEmptyModel();

    Animals animals = modelInstance.newInstance(Animals.class);
    modelInstance.setDocumentElement(animals);

    Animal animal = modelInstance.newInstance(Bird.class);
    animal.setId("TestId");
    animals.addChildElement(animal);

    ModelInstance copiedInstance = modelInstance.copy();
    getFirstAnimal(copiedInstance).setId("TestId2");

    assertThat(getFirstAnimal(modelInstance).getId()).isEqualTo("TestId");
    assertThat(getFirstAnimal(copiedInstance).getId()).isEqualTo("TestId2");
  }

  protected Animal getFirstAnimal(ModelInstance modelInstance) {
    Animals animals = (Animals) modelInstance.getDocumentElement();
    return animals.getAnimals().iterator().next();
  }

}
