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
package org.operaton.bpm.model.bpmn;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.model.bpmn.instance.HumanPerformer;
import org.operaton.bpm.model.bpmn.instance.Performer;
import org.operaton.bpm.model.bpmn.instance.PotentialOwner;
import org.operaton.bpm.model.bpmn.instance.ResourceRole;
import org.operaton.bpm.model.bpmn.instance.UserTask;

/**
 * @author Dario Campagna
 */
class ResourceRolesTest {

  private static BpmnModelInstance modelInstance;

  @BeforeAll
  static void parseModel() {
    modelInstance = Bpmn.readModelFromStream(ResourceRolesTest.class.getResourceAsStream("ResourceRolesTest.bpmn"));
  }

  @Test
  void testGetPerformer() {
    UserTask userTask = modelInstance.getModelElementById("_3");
    Collection<ResourceRole> resourceRoles = userTask.getResourceRoles();
    assertThat(resourceRoles).hasSize(1);
    ResourceRole resourceRole = resourceRoles.iterator().next();
    assertThat(resourceRole).isInstanceOf(Performer.class);
    assertThat(resourceRole.getName()).isEqualTo("Task performer");
  }

  @Test
  void testGetHumanPerformer() {
    UserTask userTask = modelInstance.getModelElementById("_7");
    Collection<ResourceRole> resourceRoles = userTask.getResourceRoles();
    assertThat(resourceRoles).hasSize(1);
    ResourceRole resourceRole = resourceRoles.iterator().next();
    assertThat(resourceRole).isInstanceOf(HumanPerformer.class);
    assertThat(resourceRole.getName()).isEqualTo("Task human performer");
  }

  @Test
  void testGetPotentialOwner() {
    UserTask userTask = modelInstance.getModelElementById("_9");
    Collection<ResourceRole> resourceRoles = userTask.getResourceRoles();
    assertThat(resourceRoles).hasSize(1);
    ResourceRole resourceRole = resourceRoles.iterator().next();
    assertThat(resourceRole).isInstanceOf(PotentialOwner.class);
    assertThat(resourceRole.getName()).isEqualTo("Task potential owner");
  }

}
