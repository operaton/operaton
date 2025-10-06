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
package org.operaton.bpm.model.xml;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.model.xml.testmodel.TestModel;
import org.operaton.bpm.model.xml.testmodel.instance.*;
import org.operaton.bpm.model.xml.type.ModelElementType;

import static org.operaton.bpm.model.xml.testmodel.TestModelConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastian Menski
 */
class ModelTest {

  private Model model;

  @BeforeEach
  void createModel() {
    model = TestModel.getTestModel();
  }

  @Test
  void testGetTypes() {
    Collection<ModelElementType> types = model.getTypes();
    assertThat(types)
      .isNotEmpty()
      .contains(
        model.getType(Animals.class),
        model.getType(Animal.class),
        model.getType(FlyingAnimal.class),
        model.getType(Bird.class),
        model.getType(RelationshipDefinition.class)
      );
  }

  @Test
  void testGetType() {
    ModelElementType flyingAnimalType = model.getType(FlyingAnimal.class);
    assertThat(flyingAnimalType.getInstanceType()).isEqualTo(FlyingAnimal.class);
  }

  @Test
  void testGetTypeForName() {
    ModelElementType birdType = model.getTypeForName(ELEMENT_NAME_BIRD);
    assertThat(birdType).isNull();
    birdType = model.getTypeForName(MODEL_NAMESPACE, ELEMENT_NAME_BIRD);
    assertThat(birdType.getInstanceType()).isEqualTo(Bird.class);
  }

  @Test
  void testGetModelName() {
    assertThat(model.getModelName()).isEqualTo(MODEL_NAME);
  }

  @Test
  void testEqual() {
    assertThat(model)
            .isNotEqualTo(null)
            .isNotEqualTo(new Object());
    Model otherModel = ModelBuilder.createInstance("Other Model").build();
    assertThat(model).isNotEqualTo(otherModel);
    otherModel = ModelBuilder.createInstance(MODEL_NAME).build();
    assertThat(model).isEqualTo(otherModel);
    otherModel = ModelBuilder.createInstance(null).build();
    assertThat(otherModel).isNotEqualTo(model);
    assertThat(model).isEqualTo(model);
  }
}
