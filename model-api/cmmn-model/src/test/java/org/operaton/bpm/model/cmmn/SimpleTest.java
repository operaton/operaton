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
package org.operaton.bpm.model.cmmn;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.model.cmmn.instance.Case;
import org.operaton.bpm.model.cmmn.instance.CasePlanModel;
import org.operaton.bpm.model.cmmn.instance.HumanTask;
import org.operaton.bpm.model.cmmn.instance.PlanItem;
import org.operaton.bpm.model.cmmn.instance.PlanItemDefinition;
import org.operaton.bpm.model.cmmn.util.CmmnModelResource;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;

/**
 * @author Roman Smirnov
 *
 */
class SimpleTest extends CmmnModelTest {

  @Test
  @CmmnModelResource
  void shouldGetElements() {

    ModelElementInstance modelElementById = cmmnModelInstance.getModelElementById("Case_1");
    assertThat(modelElementById).isNotNull();

    Collection<Case> caseElements = cmmnModelInstance.getDefinitions().getCases();
    assertThat(caseElements).hasSize(1);
    Case caseElement = caseElements.iterator().next();

    assertThat(caseElement.getId()).isEqualTo("Case_1");
    assertThat(caseElement.getName()).isNull();

    CasePlanModel casePlanModel = caseElement.getCasePlanModel();
    assertThat(casePlanModel).isNotNull();

    assertThat(casePlanModel.getId()).isEqualTo("CasePlanModel_1");

    Collection<PlanItemDefinition> planItemDefinitions = casePlanModel.getPlanItemDefinitions();
    assertThat(planItemDefinitions).hasSize(1);

    PlanItemDefinition planItemDefinition = planItemDefinitions.iterator().next();

    assertThat(planItemDefinition).isInstanceOf(HumanTask.class);
    assertThat(planItemDefinition.getId()).isEqualTo("HumanTask_1");
    assertThat(planItemDefinition.getName()).isEqualTo("A HumanTask");

    Collection<PlanItem> planItems = casePlanModel.getPlanItems();

    PlanItem planItem = planItems.iterator().next();

    assertThat(planItem.getId()).isEqualTo("PI_HumanTask_1");
    assertThat(planItem.getDefinition()).isEqualTo(planItemDefinition);
  }

}
