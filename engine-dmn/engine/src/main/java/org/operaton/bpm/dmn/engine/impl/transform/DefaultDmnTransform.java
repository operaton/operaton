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
package org.operaton.bpm.dmn.engine.impl.transform;

import static org.operaton.commons.utils.EnsureUtil.ensureNotNull;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.operaton.bpm.dmn.engine.DmnDecision;
import org.operaton.bpm.dmn.engine.DmnDecisionRequirementsGraph;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionImpl;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionLiteralExpressionImpl;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionRequirementsGraphImpl;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableImpl;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableInputImpl;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableOutputImpl;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableRuleImpl;
import org.operaton.bpm.dmn.engine.impl.DmnExpressionImpl;
import org.operaton.bpm.dmn.engine.impl.DmnLogger;
import org.operaton.bpm.dmn.engine.impl.DmnVariableImpl;
import org.operaton.bpm.dmn.engine.impl.spi.hitpolicy.DmnHitPolicyHandlerRegistry;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformContext;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformHandler;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformHandlerRegistry;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnTransform;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnTransformListener;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnTransformer;
import org.operaton.bpm.dmn.engine.impl.spi.type.DmnDataTypeTransformerRegistry;
import org.operaton.bpm.model.dmn.Dmn;
import org.operaton.bpm.model.dmn.DmnModelException;
import org.operaton.bpm.model.dmn.DmnModelInstance;
import org.operaton.bpm.model.dmn.instance.Decision;
import org.operaton.bpm.model.dmn.instance.DecisionTable;
import org.operaton.bpm.model.dmn.instance.Definitions;
import org.operaton.bpm.model.dmn.instance.Expression;
import org.operaton.bpm.model.dmn.instance.InformationRequirement;
import org.operaton.bpm.model.dmn.instance.Input;
import org.operaton.bpm.model.dmn.instance.InputEntry;
import org.operaton.bpm.model.dmn.instance.InputExpression;
import org.operaton.bpm.model.dmn.instance.LiteralExpression;
import org.operaton.bpm.model.dmn.instance.Output;
import org.operaton.bpm.model.dmn.instance.OutputEntry;
import org.operaton.bpm.model.dmn.instance.Rule;
import org.operaton.bpm.model.dmn.instance.Variable;

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

    /**
   * Sets the model instance by reading a DMN model from the specified file.
   *
   * @param file the file containing the DMN model
   */
  public void setModelInstance(File file) {
    ensureNotNull("file", file);
    try {
      modelInstance = Dmn.readModelFromFile(file);
    }
    catch (DmnModelException e) {
      throw LOG.unableToTransformDecisionsFromFile(file, e);
    }
  }

    /**
   * Sets the model instance using the given file and returns the instance of DmnTransform.
   * 
   * @param file the file to set as the model instance
   * @return an instance of DmnTransform
   */
  public DmnTransform modelInstance(File file) {
    setModelInstance(file);
    return this;
  }

    /**
   * Sets the model instance by reading a DMN model from the provided input stream.
   * 
   * @param inputStream the input stream containing the DMN model
   */
  public void setModelInstance(InputStream inputStream) {
    ensureNotNull("inputStream", inputStream);
    try {
      modelInstance = Dmn.readModelFromStream(inputStream);
    }
    catch (DmnModelException e) {
      throw LOG.unableToTransformDecisionsFromInputStream(e);
    }
  }

    /**
   * Sets the model instance using the provided input stream.
   * 
   * @param inputStream the input stream to set as the model instance
   * @return the current DmnTransform instance
   */
  public DmnTransform modelInstance(InputStream inputStream) {
    setModelInstance(inputStream);
    return this;
  }

    /**
   * Sets the DMN model instance for this object.
   * 
   * @param modelInstance the DMN model instance to set
   */
  public void setModelInstance(DmnModelInstance modelInstance) {
    ensureNotNull("dmnModelInstance", modelInstance);
    this.modelInstance = modelInstance;
  }

    /**
   * Sets the given DMN model instance and returns the current DmnTransform instance.
   *
   * @param modelInstance the DMN model instance to set
   * @return the current DmnTransform instance
   */
  public DmnTransform modelInstance(DmnModelInstance modelInstance) {
    setModelInstance(modelInstance);
    return this;
  }

  // transform ////////////////////////////////////////////////////////////////

    /**
   * Transforms a DMN Decision Requirements Graph.
   * 
   * @param <T> the type of the DmnDecisionRequirementsGraph to transform
   * @return the transformed DMN Decision Requirements Graph
   */
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

    /**
   * Transforms the input Definitions object to a DmnDecisionRequirementsGraph object.
   * 
   * @param definitions the Definitions object to be transformed
   * @return the transformed DmnDecisionRequirementsGraph object
   */
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

    /**
   * Transform a list of DMN decisions into a list of specified type.
   * 
   * @return List of transformed decisions
   */
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

    /**
   * Transforms a collection of decisions into a list of DmnDecision objects by 
   * first transforming individual decisions, building decision requirements,
   * notifying transform listeners, and ensuring no loops in the decisions.
   * 
   * @param decisions the collection of decisions to transform
   * @return the list of DmnDecision objects
   */
  protected List<DmnDecision> transformDecisions(Collection<Decision> decisions) {
    Map<String,DmnDecisionImpl> dmnDecisions = transformIndividualDecisions(decisions);
    buildDecisionRequirements(decisions, dmnDecisions);
    List<DmnDecision> dmnDecisionList = new ArrayList<DmnDecision>(dmnDecisions.values());

    for(Decision decision: decisions) {
      DmnDecision dmnDecision = dmnDecisions.get(decision.getId());
      notifyTransformListeners(decision, dmnDecision);
    }
    ensureNoLoopInDecisions(dmnDecisionList);

    return dmnDecisionList;
  }

  protected Map<String,DmnDecisionImpl> transformIndividualDecisions(Collection<Decision> decisions) {
    Map<String, DmnDecisionImpl> dmnDecisions = new HashMap<String, DmnDecisionImpl>();

    for (Decision decision : decisions) {
      DmnDecisionImpl dmnDecision = transformDecision(decision);
      if (dmnDecision != null) {
        dmnDecisions.put(dmnDecision.getKey(), dmnDecision);
      }
      ensureNoLoopInDecisions(dmnDecisionList);
  
      return dmnDecisionList;
  }

    /**
   * Transforms a collection of Decisions into a Map of DmnDecisionImpl objects.
   *
   * @param decisions the collection of Decisions to transform
   * @return a Map containing the transformed DmnDecisionImpl objects
   */
  protected Map<String, DmnDecisionImpl> transformIndividualDecisions(Collection<Decision> decisions) {
      Map<String, DmnDecisionImpl> dmnDecisions = new HashMap<String, DmnDecisionImpl>();
  
      for (Decision decision : decisions) {
        DmnDecisionImpl dmnDecision = transformDecision(decision);
        if (dmnDecision != null) {
          dmnDecisions.put(dmnDecision.getKey(), dmnDecision);
        }
      }
      return dmnDecisions;
  }

    /**
   * Builds the decision requirements for each decision based on the provided decisions and mapping of decision IDs to DmnDecisionImpl objects.
   *
   * @param decisions the collection of Decision objects
   * @param dmnDecisions the mapping of decision IDs to DmnDecisionImpl objects
   */
  protected void buildDecisionRequirements(Collection<Decision> decisions, Map<String, DmnDecisionImpl> dmnDecisions) {
    for(Decision decision: decisions) {
      List<DmnDecision> requiredDmnDecisions = getRequiredDmnDecisions(decision, dmnDecisions);
      DmnDecisionImpl dmnDecision = dmnDecisions.get(decision.getId());

      if(requiredDmnDecisions.size() > 0) {
        dmnDecision.setRequiredDecision(requiredDmnDecisions);
      }
    }
  }

    /**
   * Ensures that there are no loops in the decisions by iterating through a list of DmnDecision objects.
   * 
   * @param dmnDecisionList a list of DmnDecision objects to check for loops
   */
  protected void ensureNoLoopInDecisions(List<DmnDecision> dmnDecisionList) {
    List<String> visitedDecisions = new ArrayList<String>();

    for(DmnDecision decision: dmnDecisionList) {
      ensureNoLoopInDecision(decision, new ArrayList<String>(), visitedDecisions);
    }
  }

    /**
   * Ensures that there are no loops in the decision dependencies by recursively traversing the decision graph starting from the given decision.
   * 
   * @param decision the decision to check for loops
   * @param parentDecisionList the list of parent decisions in the current path
   * @param visitedDecisions the list of visited decisions to prevent revisiting the same decision
   */
  protected void ensureNoLoopInDecision(DmnDecision decision, List<String> parentDecisionList, List<String> visitedDecisions) {

    if (visitedDecisions.contains(decision.getKey())) {
      return;
    }

    parentDecisionList.add(decision.getKey());

    for(DmnDecision requiredDecision : decision.getRequiredDecisions()){

      if (parentDecisionList.contains(requiredDecision.getKey())) {
        throw LOG.requiredDecisionLoopDetected(requiredDecision.getKey());
      }
  
      parentDecisionList.add(decision.getKey());
  
      for(DmnDecision requiredDecision : decision.getRequiredDecisions()){
  
        if (parentDecisionList.contains(requiredDecision.getKey())) {
          throw LOG.requiredDecisionLoopDetected(requiredDecision.getKey());
        }
  
        ensureNoLoopInDecision(requiredDecision, new ArrayList<String>(parentDecisionList), visitedDecisions);
      }
      visitedDecisions.add(decision.getKey());
    }

    /**
       * Retrieves a list of required DMN decisions based on the given Decision and a map of DMN decisions.
       *
       * @param decision the Decision object for which required DMN decisions are to be retrieved
       * @param dmnDecisions a map of DMN decisions with Decision IDs as keys
       * @return a list of required DMN decisions
       */
      protected List<DmnDecision> getRequiredDmnDecisions(Decision decision, Map<String, DmnDecisionImpl> dmnDecisions) {
          List<DmnDecision> requiredDecisionList = new ArrayList<DmnDecision>();
          for(InformationRequirement informationRequirement: decision.getInformationRequirements()) {
  
            Decision requiredDecision = informationRequirement.getRequiredDecision();
            if(requiredDecision != null) {
              DmnDecision requiredDmnDecision = dmnDecisions.get(requiredDecision.getId());
              requiredDecisionList.add(requiredDmnDecision);
            }
          }
          return requiredDecisionList;
      }
    }
    return requiredDecisionList;
  }

    /**
   * Transforms a given decision into a DmnDecisionImpl object by handling its elements,
   * validating the decision ID, and transforming the decision logic based on its expression type.
   * 
   * @param decision the decision to transform
   * @return the transformed DmnDecisionImpl object
   */
  protected DmnDecisionImpl transformDecision(Decision decision) {

    /**
   * Transforms a given DecisionTable into a DmnDecisionTableImpl object.
   * 
   * @param decisionTable the DecisionTable to transform
   * @return the transformed DmnDecisionTableImpl object
   */
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
    Set<String> usedNames = new HashSet<String>();
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

    /**
   * Transforms the given Input object into a DmnDecisionTableInputImpl object
   * 
   * @param input the Input object to be transformed
   * @return the transformed DmnDecisionTableInputImpl object
   */
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

    /**
   * Transforms the provided Output object into a DmnDecisionTableOutputImpl object using the handlerRegistry.
   * Validates the output id and throws an exception if it is missing.
   *
   * @param output the Output object to transform
   * @return the transformed DmnDecisionTableOutputImpl object
   */
  protected DmnDecisionTableOutputImpl transformDecisionTableOutput(Output output) {
    DmnElementTransformHandler<Output, DmnDecisionTableOutputImpl> handler = handlerRegistry.getHandler(Output.class);
    DmnDecisionTableOutputImpl dmnOutput = handler.handleElement(this, output);

    // validate output id
    if (dmnOutput.getId() == null) {
      throw LOG.decisionTableOutputIdIsMissing(decision, dmnOutput);
    }

    return dmnOutput;
  }

    /**
   * Transforms a given Rule into a DmnDecisionTableRuleImpl object by extracting inputs and outputs
   * and validating the rule id.
   *
   * @param rule the Rule object to be transformed
   * @return the transformed DmnDecisionTableRuleImpl object
   */
  protected DmnDecisionTableRuleImpl transformDecisionTableRule(Rule rule) {
    DmnElementTransformHandler<Rule, DmnDecisionTableRuleImpl> handler = handlerRegistry.getHandler(Rule.class);
    DmnDecisionTableRuleImpl dmnRule = handler.handleElement(this, rule);

    // validate rule id
    if (dmnRule.getId() == null) {
      throw LOG.decisionTableRuleIdIsMissing(decision, dmnRule);
    }

    List<DmnDecisionTableInputImpl> inputs = this.decisionTable.getInputs();
    List<InputEntry> inputEntries = new ArrayList<InputEntry>(rule.getInputEntries());
    if (inputs.size() != inputEntries.size()) {
      throw LOG.differentNumberOfInputsAndInputEntries(inputs.size(), inputEntries.size(), dmnRule);
    }

    for (InputEntry inputEntry : inputEntries) {
      parent = dmnRule;

      DmnExpressionImpl condition = transformInputEntry(inputEntry);
      dmnRule.getConditions().add(condition);
    }

    List<DmnDecisionTableOutputImpl> outputs = this.decisionTable.getOutputs();
    List<OutputEntry> outputEntries = new ArrayList<OutputEntry>(rule.getOutputEntries());
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

    /**
   * Transforms the given input expression into a DmnExpressionImpl object using the appropriate handler.
   *
   * @param inputExpression the input expression to transform
   * @return the transformed DmnExpressionImpl object
   */
  protected DmnExpressionImpl transformInputExpression(InputExpression inputExpression) {
    DmnElementTransformHandler<InputExpression, DmnExpressionImpl> handler = handlerRegistry.getHandler(InputExpression.class);
    return handler.handleElement(this, inputExpression);
  }

    /**
   * Transforms the given InputEntry into a DmnExpressionImpl using the appropriate handler from the handler registry.
   * 
   * @param inputEntry the InputEntry to transform
   * @return the transformed DmnExpressionImpl
   */
  protected DmnExpressionImpl transformInputEntry(InputEntry inputEntry) {
    DmnElementTransformHandler<InputEntry, DmnExpressionImpl> handler = handlerRegistry.getHandler(InputEntry.class);
    return handler.handleElement(this, inputEntry);
  }

    /**
   * Transforms the given OutputEntry into a DmnExpressionImpl using the registered handler.
   * 
   * @param outputEntry the OutputEntry to transform
   * @return the transformed DmnExpressionImpl
   */
  protected DmnExpressionImpl transformOutputEntry(OutputEntry outputEntry) {
    DmnElementTransformHandler<OutputEntry, DmnExpressionImpl> handler = handlerRegistry.getHandler(OutputEntry.class);
    return handler.handleElement(this, outputEntry);
  }

    /**
   * Transforms a decision literal expression into a DmnDecisionLiteralExpressionImpl object.
   * 
   * @param decision the decision object
   * @param literalExpression the literal expression object
   * @return the transformed DmnDecisionLiteralExpressionImpl object
   */
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

    /**
   * Transforms a LiteralExpression into a DmnExpressionImpl using the appropriate handler from the handler registry.
   *
   * @param literalExpression the LiteralExpression to transform
   * @return the transformed DmnExpressionImpl
   */
  protected DmnExpressionImpl transformLiteralExpression(LiteralExpression literalExpression) {
    DmnElementTransformHandler<LiteralExpression, DmnExpressionImpl> handler = handlerRegistry.getHandler(LiteralExpression.class);
    return handler.handleElement(this, literalExpression);
  }

    /**
   * Transforms a Variable object into a DmnVariableImpl object using the appropriate handler from the handler registry.
   * 
   * @param variable the Variable object to transform
   * @return the transformed DmnVariableImpl object
   */
  protected DmnVariableImpl transformVariable(Variable variable) {
    DmnElementTransformHandler<Variable, DmnVariableImpl> handler = handlerRegistry.getHandler(Variable.class);
    return handler.handleElement(this, variable);
  }

  // listeners ////////////////////////////////////////////////////////////////

    /**
   * Notifies all transform listeners with the given decision and DmnDecision.
   * 
   * @param decision the decision that was transformed
   * @param dmnDecision the DmnDecision associated with the decision
   */
  protected void notifyTransformListeners(Decision decision, DmnDecision dmnDecision) {
    for (DmnTransformListener transformListener : transformListeners) {
      transformListener.transformDecision(decision, dmnDecision);
    }
  }

    /**
   * Notifies all transform listeners with the given input and decision table input.
   * 
   * @param input the input to be transformed
   * @param dmnInput the decision table input to be transformed
   */
  protected void notifyTransformListeners(Input input, DmnDecisionTableInputImpl dmnInput) {
    for (DmnTransformListener transformListener : transformListeners) {
      transformListener.transformDecisionTableInput(input, dmnInput);
    }
  }

    /**
   * Notifies all transform listeners that a decision requirements graph has been transformed.
   * 
   * @param definitions the definitions associated with the decision requirements graph
   * @param dmnDecisionRequirementsGraph the decision requirements graph that was transformed
   */
  protected void notifyTransformListeners(Definitions definitions, DmnDecisionRequirementsGraphImpl dmnDecisionRequirementsGraph) {
    for (DmnTransformListener transformListener : transformListeners) {
      transformListener.transformDecisionRequirementsGraph(definitions, dmnDecisionRequirementsGraph);
    }
  }

    /**
   * Notifies all registered transform listeners with the given output and decision table output.
   * 
   * @param output the output to be transformed
   * @param dmnOutput the decision table output
   */
  protected void notifyTransformListeners(Output output, DmnDecisionTableOutputImpl dmnOutput) {
    for (DmnTransformListener transformListener : transformListeners) {
      transformListener.transformDecisionTableOutput(output, dmnOutput);
    }
  }

    /**
   * Notifies all registered transform listeners when a decision table rule is transformed.
   *
   * @param rule the rule being transformed
   * @param dmnRule the transformed decision table rule
   */
  protected void notifyTransformListeners(Rule rule, DmnDecisionTableRuleImpl dmnRule) {
    for (DmnTransformListener transformListener : transformListeners) {
      transformListener.transformDecisionTableRule(rule, dmnRule);
    }
  }

  // context //////////////////////////////////////////////////////////////////

    /**
  * Returns the DMN model instance.
  * 
  * @return the DMN model instance
  */
  public DmnModelInstance getModelInstance() {
    return modelInstance;
  }

    /**
   * Returns the parent object.
   *
   * @return the parent object
   */
  public Object getParent() {
    return parent;
  }

    /**
   * Returns the decision associated with this method.
   *
   * @return the decision associated with this method
   */
  public DmnDecision getDecision() {
    return decision;
  }

    /**
   * Returns the data type transformer registry.
   *
   * @return the data type transformer registry
   */
  public DmnDataTypeTransformerRegistry getDataTypeTransformerRegistry() {
    return dataTypeTransformerRegistry;
  }

    /**
   * Returns the hit policy handler registry.
   *
   * @return the hit policy handler registry
   */
  public DmnHitPolicyHandlerRegistry getHitPolicyHandlerRegistry() {
    return hitPolicyHandlerRegistry;
  }

}
