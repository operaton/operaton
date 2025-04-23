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
package org.operaton.bpm.engine.test.cmmn.handler;

import org.junit.jupiter.api.BeforeEach;
import org.operaton.bpm.engine.impl.cmmn.entity.repository.CaseDefinitionEntity;
import org.operaton.bpm.engine.impl.cmmn.handler.CmmnHandlerContext;
import org.operaton.bpm.engine.impl.el.ExpressionManager;
import org.operaton.bpm.engine.impl.el.JuelExpressionManager;
import org.operaton.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.operaton.bpm.model.cmmn.Cmmn;
import org.operaton.bpm.model.cmmn.CmmnModelInstance;
import org.operaton.bpm.model.cmmn.instance.*;

import java.util.HashMap;

/**
 * @author Roman Smirnov
 *
 */
public abstract class CmmnElementHandlerTest {

  protected CmmnModelInstance modelInstance;
  protected Definitions definitions;
  protected Case caseDefinition;
  protected CasePlanModel casePlanModel;
  protected CmmnHandlerContext context;

  @BeforeEach
  public void setup() {
    modelInstance = Cmmn.createEmptyModel();
    definitions = modelInstance.newInstance(Definitions.class);
    definitions.setTargetNamespace("http://operaton.org/examples");
    modelInstance.setDefinitions(definitions);

    caseDefinition = createElement(definitions, "aCaseDefinition", Case.class);
    casePlanModel = createElement(caseDefinition, "aCasePlanModel", CasePlanModel.class);

    context = new CmmnHandlerContext();

    CaseDefinitionEntity caseDef = new CaseDefinitionEntity();
    caseDef.setTaskDefinitions(new HashMap<>());
    context.setCaseDefinition(caseDef);

    ExpressionManager expressionManager = new JuelExpressionManager();
    context.setExpressionManager(expressionManager);

    DeploymentEntity deployment = new DeploymentEntity();
    deployment.setId("foo");
    context.setDeployment(deployment);
  }

  protected <T extends CmmnModelElementInstance> T createElement(CmmnModelElementInstance parentElement, Class<T> elementClass) {
    T element = modelInstance.newInstance(elementClass);
    parentElement.addChildElement(element);
    return element;
  }

  protected <T extends CmmnModelElementInstance> T createElement(CmmnModelElementInstance parentElement, String id, Class<T> elementClass) {
    T element = createElement(parentElement, elementClass);
    element.setAttributeValue("id", id, true);
    return element;
  }

  protected ExtensionElements addExtensionElements(CmmnModelElementInstance parentElement) {
    return createElement(parentElement, null, ExtensionElements.class);
  }

}
