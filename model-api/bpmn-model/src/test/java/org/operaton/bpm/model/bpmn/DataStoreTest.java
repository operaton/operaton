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
package org.operaton.bpm.model.bpmn;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.model.bpmn.instance.DataStore;
import org.operaton.bpm.model.bpmn.instance.DataStoreReference;

/**
 * @author Falko Menge
 */
class DataStoreTest {

  private static BpmnModelInstance modelInstance;

  @BeforeAll
  static void parseModel() {
    modelInstance = Bpmn.readModelFromStream(DataStoreTest.class.getResourceAsStream("DataStoreTest.bpmn"));
  }

  @Test
  void testGetDataStore() {
    DataStore dataStore = modelInstance.getModelElementById("myDataStore");
    assertThat(dataStore).isNotNull();
    assertThat(dataStore.getName()).isEqualTo("My Data Store");
    assertThat(dataStore.getCapacity()).isEqualTo(23);
    assertThat(dataStore.isUnlimited()).isFalse();
  }

  @Test
  void testGetDataStoreReference() {
    DataStoreReference dataStoreReference = modelInstance.getModelElementById("myDataStoreReference");
    DataStore dataStore = modelInstance.getModelElementById("myDataStore");
    assertThat(dataStoreReference).isNotNull();
    assertThat(dataStoreReference.getName()).isEqualTo("My Data Store Reference");
    assertThat(dataStoreReference.getDataStore()).isEqualTo(dataStore);
  }
}
