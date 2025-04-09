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

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.model.cmmn.CmmnModelInstance;
import org.operaton.bpm.model.cmmn.instance.Case;
import org.operaton.bpm.model.cmmn.instance.HumanTask;
import org.operaton.bpm.model.cmmn.instance.PlanItem;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;

/**
 * @author Roman Smirnov
 *
 */
@ExtendWith(ProcessEngineExtension.class)
public class CmmnModelElementInstanceCmdTest {

  private static final String CASE_KEY = "oneTaskCase";

  RepositoryService repositoryService;
  
  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn")
  @Test
  public void testRepositoryService() {
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionKey(CASE_KEY)
        .singleResult()
        .getId();

    CmmnModelInstance modelInstance = repositoryService.getCmmnModelInstance(caseDefinitionId);
    assertThat(modelInstance).isNotNull();

    Collection<ModelElementInstance> humanTasks = modelInstance.getModelElementsByType(modelInstance.getModel().getType(HumanTask.class));
    assertThat(humanTasks).hasSize(1);

    Collection<ModelElementInstance> planItems = modelInstance.getModelElementsByType(modelInstance.getModel().getType(PlanItem.class));
    assertThat(planItems).hasSize(1);

    Collection<ModelElementInstance> cases = modelInstance.getModelElementsByType(modelInstance.getModel().getType(Case.class));
    assertThat(cases).hasSize(1);

  }

}
