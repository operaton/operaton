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
package org.operaton.bpm.dmn.engine.impl.transform;

import org.operaton.bpm.dmn.engine.DmnDecision;
import org.operaton.bpm.dmn.engine.DmnDecisionRequirementsGraph;
import org.operaton.bpm.dmn.engine.impl.*;
import org.operaton.bpm.dmn.engine.impl.spi.hitpolicy.DmnHitPolicyHandlerRegistry;
import org.operaton.bpm.dmn.engine.impl.spi.transform.*;
import org.operaton.bpm.dmn.engine.impl.spi.type.DmnDataTypeTransformerRegistry;
import org.operaton.bpm.model.dmn.Dmn;
import org.operaton.bpm.model.dmn.DmnModelException;
import org.operaton.bpm.model.dmn.DmnModelInstance;
import org.operaton.bpm.model.dmn.instance.*;
import static org.operaton.commons.utils.EnsureUtil.ensureNotNull;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.*;

public class DefaultDmnTransform implements DmnTransform, DmnElementTransformContext {

  private static final DmnTransformLogger LOG = DmnLogger.TRANSFORM_LOGGER;

  protected DmnTransformer transformer;

  protected List<DmnTransformListener> transformListeners;
  protected DmnElementTransformHandlerRegistry handlerRegistry;

  // context
  protected DmnModelInstance modelInstance;
  protected Object parent;
  protected DmnDecisionImpl decision;
  protected DmnDecisionTableImpl decisionTable;
  protected DmnDataTypeTransformerRegistry dataTypeTransformerRegistry;
  protected DmnHitPolicyHandlerRegistry hitPolicyHandlerRegistry;

  public DefaultDmnTransform(DmnTransformer transformer) {
    this.transformer = transformer;
    transformListeners = transformer.getTransformListeners();
    handlerRegistry = transformer.getElementTransformHandlerRegistry();
    dataTypeTransformerRegistry = transformer.getDataTypeTransformerRegistry();
    hitPolicyHandlerRegistry = transformer.getHitPolicyHandlerRegistry();
  }

  @Override
  public void setModelInstance(File file) {
    ensureNotNull("file", file);
    try {
      modelInstance = Dmn.readModelFromFile(file);
    }
    catch (DmnModelException e) {
      throw LOG.unableToTransformDecisionsFromFile(file, e);
    }
  }

  @Override
  public DmnTransform modelInstance(File file) {
    setModelInstance(file);
    return this;
  }

  @Override
  public void setModelInstance(InputStream inputStream) {
    ensureNotNull("inputStream", inputStream);
    try {
      modelInstance = Dmn.readModelFromStream(inputStream);
    }
    catch (DmnModelException e) {
      throw LOG.unableToTransformDecisionsFromInputStream(e);
    }
  }

  @Override
  public DmnTransform modelInstance(InputStream inputStream) {
    setModelInstance(inputStream);
    return this;
  }

  @Override
  public void setModelInstance(DmnModelInstance modelInstance) {
    ensureNotNull("dmnModelInstance", modelInstance);
    this.modelInstance = modelInstance;
  }

  @Override
  public DmnTransform modelInstance(DmnModelInstance modelInstance) {
    setModelInstance(modelInstance);
    return this;
  }

  // transform ////////////////////////////////////////////////////////////////

  @Override
  @SuppressWarnings("unchecked")
  public <T extends DmnDecisionRequirementsGraph> T transformDecisionRequirementsGraph() {
    try {
      Definitions definitions = modelInstance.getDefinitions();
      return (T) transformDefinitions(definitions);
    }
    catch (Exception e) {
      throw LOG.errorWhileTransformingDefinitions(e);
    }
  }

  protected DmnDecisionRequirementsGraph transformDefinitions(Definitions definitions) {
    DmnElementTransformHandler<Definitions, DmnDecisionRequirementsGraphImpl> handler = handlerRegistry.getHandler(Definitions.class);
    DmnDecisionRequirementsGraphImpl dmnDrg = handler.handleElement(this, definitions);

    // validate id of drd
    if (dmnDrg.getKey() == null) {
      throw LOG.drdIdIsMissing(dmnDrg);
    }

    Collection<Decision> decisions = definitions.getChildElementsByType(Decision.class);
    List<DmnDecision> dmnDecisions = transformDecisions(decisions);
    for (DmnDecision dmnDecision : dmnDecisions) {
      dmnDrg.addDecision(dmnDecision);
    }

    notifyTransformListeners(definitions, dmnDrg);
    return dmnDrg;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends DmnDecision> List<T> transformDecisions() {
    try {
      Definitions definitions = modelInstance.getDefinitions();
      Collection<Decision> decisions = definitions.getChildElementsByType(Decision.class);
      return (List<T>) transformDecisions(decisions);
    }
    catch (Exception e) {
      throw LOG.errorWhileTransformingDecisions(e);
    }
  }

  protected List<DmnDecision> transformDecisions(Collection<Decision> decisions) {
    Map<String,DmnDecisionImpl> dmnDecisions = transformIndividualDecisions(decisions);
    buildDecisionRequirements(decisions, dmnDecisions);
    List<DmnDecision> dmnDecisionList = new ArrayList<>(dmnDecisions.values());

    for(Decision dec: decisions) {
      DmnDecision dmnDecision = dmnDecisions.get(dec.getId());
      notifyTransformListeners(dec, dmnDecision);
    }
    ensureNoLoopInDecisions(dmnDecisionList);

    return dmnDecisionList;
  }

  protected Map<String,DmnDecisionImpl> transformIndividualDecisions(Collection<Decision> decisions) {
    Map<String, DmnDecisionImpl> dmnDecisions = new HashMap<>();

    for (Decision dec : decisions) {
      DmnDecisionImpl dmnDecision = transformDecision(dec);
      if (dmnDecision != null) {
        dmnDecisions.put(dmnDecision.getKey(), dmnDecision);
      }
    }
    return dmnDecisions;
  }

  protected void buildDecisionRequirements(Collection<Decision> decisions, Map<String, DmnDecisionImpl> dmnDecisions) {
    for(Decision dec: decisions) {
      List<DmnDecision> requiredDmnDecisions = getRequiredDmnDecisions(dec, dmnDecisions);
      DmnDecisionImpl dmnDecision = dmnDecisions.get(dec.getId());

      if(!requiredDmnDecisions.isEmpty()) {
        dmnDecision.setRequiredDecision(requiredDmnDecisions);
      }
    }
  }

  protected void ensureNoLoopInDecisions(List<DmnDecision> dmnDecisionList) {
    List<String> visitedDecisions = new ArrayList<>();

    for(DmnDecision dec: dmnDecisionList) {
      ensureNoLoopInDecision(dec, new ArrayList<>(), visitedDecisions);
    }
  }

  protected void ensureNoLoopInDecision(DmnDecision decision, List<String> parentDecisionList, List<String> visitedDecisions) {

    if (visitedDecisions.contains(decision.getKey())) {
      return;
    }

    parentDecisionList.add(decision.getKey());

    for(DmnDecision requiredDecision : decision.getRequiredDecisions()){

      if (parentDecisionList.contains(requiredDecision.getKey())) {
        throw LOG.requiredDecisionLoopDetected(requiredDecision.getKey());
      }

      ensureNoLoopInDecision(requiredDecision, new ArrayList<>(parentDecisionList), visitedDecisions);
    }
    visitedDecisions.add(decision.getKey());
  }

  protected List<DmnDecision> getRequiredDmnDecisions(Decision decision, Map<String, DmnDecisionImpl> dmnDecisions) {
    List<DmnDecision> requiredDecisionList = new ArrayList<>();
    for(InformationRequirement informationRequirement: decision.getInformationRequirements()) {

      Decision requiredDecision = informationRequirement.getRequiredDecision();
      if(requiredDecision != null) {
        DmnDecision requiredDmnDecision = dmnDecisions.get(requiredDecision.getId());
        requiredDecisionList.add(requiredDmnDecision);
      }
    }
    return requiredDecisionList;
  }

  protected DmnDecisionImpl transformDecision(Decision decision) {

    DmnElementTransformHandler<Decision, DmnDecisionImpl> handler = handlerRegistry.getHandler(Decision.class);
    DmnDecisionImpl dmnDecision = handler.handleElement(this, decision);
    this.decision = dmnDecision;
    // validate decision id
    if (dmnDecision.getKey() == null) {
      throw LOG.decisionIdIsMissing(dmnDecision);
    }

    Expression expression = decision.getExpression();
    if (expression == null) {
      LOG.decisionWithoutExpression(decision);
      return null;
    }

    if (expression instanceof DecisionTable decisionTbl) {
      DmnDecisionTableImpl dmnDecisionTable = transformDecisionTable(decisionTbl);
      dmnDecision.setDecisionLogic(dmnDecisionTable);

    } else if (expression instanceof LiteralExpression literalExpression) {
      DmnDecisionLiteralExpressionImpl dmnDecisionLiteralExpression = transformDecisionLiteralExpression(decision, literalExpression);
      dmnDecision.setDecisionLogic(dmnDecisionLiteralExpression);

    } else {
      LOG.decisionTypeNotSupported(expression, decision);
      return null;
    }

    return dmnDecision;
  }

  protected DmnDecisionTableImpl transformDecisionTable(DecisionTable decisionTable) {
    DmnElementTransformHandler<DecisionTable, DmnDecisionTableImpl> handler = handlerRegistry.getHandler(DecisionTable.class);
    DmnDecisionTableImpl dmnDecisionTable = handler.handleElement(this, decisionTable);

    for (Input input : decisionTable.getInputs()) {
      parent = dmnDecisionTable;
      this.decisionTable = dmnDecisionTable;
      DmnDecisionTableInputImpl dmnInput = transformDecisionTableInput(input);
      if (dmnInput != null) {
        dmnDecisionTable.getInputs().add(dmnInput);
        notifyTransformListeners(input, dmnInput);
      }
    }

    boolean needsName = decisionTable.getOutputs().size() > 1;
    Set<String> usedNames = new HashSet<>();
    for (Output output : decisionTable.getOutputs()) {
      parent = dmnDecisionTable;
      this.decisionTable = dmnDecisionTable;
      DmnDecisionTableOutputImpl dmnOutput = transformDecisionTableOutput(output);
      if (dmnOutput != null) {
        // validate output name
        String outputName = dmnOutput.getOutputName();
        if (needsName && outputName == null) {
          throw LOG.compoundOutputsShouldHaveAnOutputName(dmnDecisionTable, dmnOutput);
        }
        if (usedNames.contains(outputName)) {
          throw LOG.compoundOutputWithDuplicateName(dmnDecisionTable, dmnOutput);
        }
        usedNames.add(outputName);

        dmnDecisionTable.getOutputs().add(dmnOutput);
        notifyTransformListeners(output, dmnOutput);
      }
    }

    for (Rule rule : decisionTable.getRules()) {
      parent = dmnDecisionTable;
      this.decisionTable = dmnDecisionTable;
      DmnDecisionTableRuleImpl dmnRule = transformDecisionTableRule(rule);
      if (dmnRule != null) {
        dmnDecisionTable.getRules().add(dmnRule);
        notifyTransformListeners(rule, dmnRule);
      }
    }

    return dmnDecisionTable;
  }

  protected DmnDecisionTableInputImpl transformDecisionTableInput(Input input) {
    DmnElementTransformHandler<Input, DmnDecisionTableInputImpl> handler = handlerRegistry.getHandler(Input.class);
    DmnDecisionTableInputImpl dmnInput = handler.handleElement(this, input);

    // validate input id
    if (dmnInput.getId() == null) {
      throw LOG.decisionTableInputIdIsMissing(decision, dmnInput);
    }

    InputExpression inputExpression = input.getInputExpression();
    if (inputExpression != null) {
      parent = dmnInput;
      DmnExpressionImpl dmnExpression = transformInputExpression(inputExpression);
      if (dmnExpression != null) {
        dmnInput.setExpression(dmnExpression);
      }
    }

    return dmnInput;
  }

  protected DmnDecisionTableOutputImpl transformDecisionTableOutput(Output output) {
    DmnElementTransformHandler<Output, DmnDecisionTableOutputImpl> handler = handlerRegistry.getHandler(Output.class);
    DmnDecisionTableOutputImpl dmnOutput = handler.handleElement(this, output);

    // validate output id
    if (dmnOutput.getId() == null) {
      throw LOG.decisionTableOutputIdIsMissing(decision, dmnOutput);
    }

    return dmnOutput;
  }

  protected DmnDecisionTableRuleImpl transformDecisionTableRule(Rule rule) {
    DmnElementTransformHandler<Rule, DmnDecisionTableRuleImpl> handler = handlerRegistry.getHandler(Rule.class);
    DmnDecisionTableRuleImpl dmnRule = handler.handleElement(this, rule);

    // validate rule id
    if (dmnRule.getId() == null) {
      throw LOG.decisionTableRuleIdIsMissing(decision, dmnRule);
    }

    List<DmnDecisionTableInputImpl> inputs = this.decisionTable.getInputs();
    List<InputEntry> inputEntries = new ArrayList<>(rule.getInputEntries());
    if (inputs.size() != inputEntries.size()) {
      throw LOG.differentNumberOfInputsAndInputEntries(inputs.size(), inputEntries.size(), dmnRule);
    }

    for (InputEntry inputEntry : inputEntries) {
      parent = dmnRule;

      DmnExpressionImpl condition = transformInputEntry(inputEntry);
      dmnRule.getConditions().add(condition);
    }

    List<DmnDecisionTableOutputImpl> outputs = this.decisionTable.getOutputs();
    List<OutputEntry> outputEntries = new ArrayList<>(rule.getOutputEntries());
    if (outputs.size() != outputEntries.size()) {
      throw LOG.differentNumberOfOutputsAndOutputEntries(outputs.size(), outputEntries.size(), dmnRule);
    }

    for (OutputEntry outputEntry : outputEntries) {
      parent = dmnRule;
      DmnExpressionImpl conclusion = transformOutputEntry(outputEntry);
      dmnRule.getConclusions().add(conclusion);
    }

    return dmnRule;
  }

  protected DmnExpressionImpl transformInputExpression(InputExpression inputExpression) {
    DmnElementTransformHandler<InputExpression, DmnExpressionImpl> handler = handlerRegistry.getHandler(InputExpression.class);
    return handler.handleElement(this, inputExpression);
  }

  protected DmnExpressionImpl transformInputEntry(InputEntry inputEntry) {
    DmnElementTransformHandler<InputEntry, DmnExpressionImpl> handler = handlerRegistry.getHandler(InputEntry.class);
    return handler.handleElement(this, inputEntry);
  }

  protected DmnExpressionImpl transformOutputEntry(OutputEntry outputEntry) {
    DmnElementTransformHandler<OutputEntry, DmnExpressionImpl> handler = handlerRegistry.getHandler(OutputEntry.class);
    return handler.handleElement(this, outputEntry);
  }

  protected DmnDecisionLiteralExpressionImpl transformDecisionLiteralExpression(Decision decision, LiteralExpression literalExpression) {
    DmnDecisionLiteralExpressionImpl dmnDecisionLiteralExpression = new DmnDecisionLiteralExpressionImpl();

    Variable variable = decision.getVariable();
    if (variable == null) {
      throw LOG.decisionVariableIsMissing(decision.getId());
    }

    DmnVariableImpl dmnVariable = transformVariable(variable);
    dmnDecisionLiteralExpression.setVariable(dmnVariable);

    DmnExpressionImpl dmnLiteralExpression = transformLiteralExpression(literalExpression);
    dmnDecisionLiteralExpression.setExpression(dmnLiteralExpression);

    return dmnDecisionLiteralExpression;
  }

  protected DmnExpressionImpl transformLiteralExpression(LiteralExpression literalExpression) {
    DmnElementTransformHandler<LiteralExpression, DmnExpressionImpl> handler = handlerRegistry.getHandler(LiteralExpression.class);
    return handler.handleElement(this, literalExpression);
  }

  protected DmnVariableImpl transformVariable(Variable variable) {
    DmnElementTransformHandler<Variable, DmnVariableImpl> handler = handlerRegistry.getHandler(Variable.class);
    return handler.handleElement(this, variable);
  }

  // listeners ////////////////////////////////////////////////////////////////

  protected void notifyTransformListeners(Decision decision, DmnDecision dmnDecision) {
    for (DmnTransformListener transformListener : transformListeners) {
      transformListener.transformDecision(decision, dmnDecision);
    }
  }

  protected void notifyTransformListeners(Input input, DmnDecisionTableInputImpl dmnInput) {
    for (DmnTransformListener transformListener : transformListeners) {
      transformListener.transformDecisionTableInput(input, dmnInput);
    }
  }

  protected void notifyTransformListeners(Definitions definitions, DmnDecisionRequirementsGraphImpl dmnDecisionRequirementsGraph) {
    for (DmnTransformListener transformListener : transformListeners) {
      transformListener.transformDecisionRequirementsGraph(definitions, dmnDecisionRequirementsGraph);
    }
  }

  protected void notifyTransformListeners(Output output, DmnDecisionTableOutputImpl dmnOutput) {
    for (DmnTransformListener transformListener : transformListeners) {
      transformListener.transformDecisionTableOutput(output, dmnOutput);
    }
  }

  protected void notifyTransformListeners(Rule rule, DmnDecisionTableRuleImpl dmnRule) {
    for (DmnTransformListener transformListener : transformListeners) {
      transformListener.transformDecisionTableRule(rule, dmnRule);
    }
  }

  // context //////////////////////////////////////////////////////////////////

  @Override
  public DmnModelInstance getModelInstance() {
    return modelInstance;
  }

  @Override
  public Object getParent() {
    return parent;
  }

  @Override
  public DmnDecision getDecision() {
    return decision;
  }

  @Override
  public DmnDataTypeTransformerRegistry getDataTypeTransformerRegistry() {
    return dataTypeTransformerRegistry;
  }

  @Override
  public DmnHitPolicyHandlerRegistry getHitPolicyHandlerRegistry() {
    return hitPolicyHandlerRegistry;
  }

}
