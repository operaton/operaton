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
package org.operaton.bpm.engine.impl.cmmn.handler;

import java.util.HashMap;

import org.operaton.bpm.engine.impl.HistoryTimeToLiveParser;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cmmn.entity.repository.CaseDefinitionEntity;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnCaseDefinition;
import org.operaton.bpm.engine.impl.cmmn.transformer.CmmnTransformerLogger;
import org.operaton.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.model.cmmn.CmmnModelInstance;
import org.operaton.bpm.model.cmmn.instance.Case;
import org.operaton.bpm.model.cmmn.instance.CmmnElement;
import org.operaton.bpm.model.cmmn.instance.Definitions;

/**
 * @author Roman Smirnov
 *
 */
public class CaseHandler extends CmmnElementHandler<Case, CmmnCaseDefinition> {

  protected static final CmmnTransformerLogger LOG = ProcessEngineLogger.CMMN_TRANSFORMER_LOGGER;

  @Override
  public CmmnCaseDefinition handleElement(Case element, CmmnHandlerContext context) {
    CaseDefinitionEntity definition = createActivity(element);

    initializeActivity(element, definition, context);

    return definition;
  }

  protected void initializeActivity(Case element, CmmnActivity activity, CmmnHandlerContext context) {
    CaseDefinitionEntity definition = (CaseDefinitionEntity) activity;

    Deployment deployment = context.getDeployment();

    definition.setKey(element.getId());
    definition.setName(element.getName());
    definition.setDeploymentId(deployment.getId());
    definition.setTaskDefinitions(new HashMap<>());

    boolean skipEnforceTtl = !((DeploymentEntity) deployment).isNew();
    validateAndSetHTTL(element, definition, skipEnforceTtl);

    CmmnModelInstance model = context.getModel();
    Definitions definitions = model.getDefinitions();
    String category = definitions.getTargetNamespace();

    definition.setCategory(category);
  }

  protected void validateAndSetHTTL(Case element, CaseDefinitionEntity definition, boolean skipEnforceTtl) {
    String caseDefinitionKey = definition.getKey();
    Integer historyTimeToLive = HistoryTimeToLiveParser.create().parse(element, caseDefinitionKey, skipEnforceTtl);
    definition.setHistoryTimeToLive(historyTimeToLive);
  }

  /**
   * @deprecated use {@link #createActivity(CmmnElement)} instead
   */
  @Deprecated(since = "1.1", forRemoval = true)
  protected CaseDefinitionEntity createActivity(CmmnElement element, CmmnHandlerContext context) {
    return createActivity(element);
  }

  protected CaseDefinitionEntity createActivity(CmmnElement element) {
    CaseDefinitionEntity definition = new CaseDefinitionEntity();

    definition.setCmmnElement(element);

    return definition;
  }

}
