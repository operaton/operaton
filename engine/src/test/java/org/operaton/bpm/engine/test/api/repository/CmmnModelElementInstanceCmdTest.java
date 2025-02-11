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
package org.operaton.bpm.engine.test.api.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;

import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.operaton.bpm.model.cmmn.CmmnModelInstance;
import org.operaton.bpm.model.cmmn.instance.Case;
import org.operaton.bpm.model.cmmn.instance.HumanTask;
import org.operaton.bpm.model.cmmn.instance.PlanItem;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;
import org.junit.Test;

/**
 * @author Roman Smirnov
 *
 */
public class CmmnModelElementInstanceCmdTest extends PluggableProcessEngineTest {

  private static final String CASE_KEY = "oneTaskCase";

  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn")
  @Test
  public void testRepositoryService() {
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionKey(CASE_KEY)
        .singleResult()
        .getId();

    CmmnModelInstance modelInstance = repositoryService.getCmmnModelInstance(caseDefinitionId);
    assertNotNull(modelInstance);

    Collection<ModelElementInstance> humanTasks = modelInstance.getModelElementsByType(modelInstance.getModel().getType(HumanTask.class));
    assertThat(humanTasks.size()).isEqualTo(1);

    Collection<ModelElementInstance> planItems = modelInstance.getModelElementsByType(modelInstance.getModel().getType(PlanItem.class));
    assertThat(planItems.size()).isEqualTo(1);

    Collection<ModelElementInstance> cases = modelInstance.getModelElementsByType(modelInstance.getModel().getType(Case.class));
    assertThat(cases.size()).isEqualTo(1);

  }

}
