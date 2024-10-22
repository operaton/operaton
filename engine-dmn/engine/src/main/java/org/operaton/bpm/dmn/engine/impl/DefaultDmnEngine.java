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
package org.operaton.bpm.dmn.engine.impl;

import static org.operaton.commons.utils.EnsureUtil.ensureNotNull;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.dmn.engine.DmnDecision;
import org.operaton.bpm.dmn.engine.DmnDecisionRequirementsGraph;
import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.DmnDecisionTableResult;
import org.operaton.bpm.dmn.engine.DmnEngine;
import org.operaton.bpm.dmn.engine.DmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnTransformer;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.context.VariableContext;
import org.operaton.bpm.model.dmn.DmnModelInstance;

public class DefaultDmnEngine implements DmnEngine {

  protected static final DmnEngineLogger LOG = DmnLogger.ENGINE_LOGGER;

  protected DefaultDmnEngineConfiguration dmnEngineConfiguration;
  protected DmnTransformer transformer;

  public DefaultDmnEngine(DefaultDmnEngineConfiguration dmnEngineConfiguration) {
    this.dmnEngineConfiguration = dmnEngineConfiguration;
    this.transformer = dmnEngineConfiguration.getTransformer();
  }

    /**
   * Returns the DmnEngineConfiguration object associated with this DmnEngine instance.
   *
   * @return the DmnEngineConfiguration object
   */
  public DmnEngineConfiguration getConfiguration() {
    return dmnEngineConfiguration;
  }

    /**
   * Parses the decisions from the input stream using a DMN transformer.
   * 
   * @param inputStream the input stream containing the DMN model
   * @return a list of DMN decisions parsed from the input stream
   */
  public List<DmnDecision> parseDecisions(InputStream inputStream) {
    ensureNotNull("inputStream", inputStream);
    return transformer.createTransform()
      .modelInstance(inputStream)
      .transformDecisions();
  }

    /**
   * Parses decisions from the provided DMN model instance.
   * 
   * @param dmnModelInstance the DMN model instance to parse decisions from
   * @return the list of parsed DMN decisions
   */
  public List<DmnDecision> parseDecisions(DmnModelInstance dmnModelInstance) {
    ensureNotNull("dmnModelInstance", dmnModelInstance);
    return transformer.createTransform()
      .modelInstance(dmnModelInstance)
      .transformDecisions();
  }

    /**
   * Parses a DMN decision from the input stream based on the provided decision key.
   * 
   * @param decisionKey the key of the decision to parse
   * @param inputStream the input stream containing the DMN decisions
   * @return the parsed DMN decision with the matching key
   * @throws IllegalArgumentException if the decisionKey is null
   * @throws IllegalStateException if unable to find a decision with the specified key
   */
  public DmnDecision parseDecision(String decisionKey, InputStream inputStream) {
    ensureNotNull("decisionKey", decisionKey);
    List<DmnDecision> decisions = parseDecisions(inputStream);
    for (DmnDecision decision : decisions) {
      if (decisionKey.equals(decision.getKey())) {
        return decision;
      }
    }
    throw LOG.unableToFindDecisionWithKey(decisionKey);
  }

    /**
   * Parses a DmnDecision object with the given decision key from the provided DmnModelInstance.
   * 
   * @param decisionKey the key of the decision to parse
   * @param dmnModelInstance the DmnModelInstance to parse decisions from
   * @return the parsed DmnDecision object
   * @throws IllegalArgumentException if the decisionKey is null
   * @throws DmnModelException if the decision with the specified key is not found
   */
  public DmnDecision parseDecision(String decisionKey, DmnModelInstance dmnModelInstance) {
    ensureNotNull("decisionKey", decisionKey);
    List<DmnDecision> decisions = parseDecisions(dmnModelInstance);
    for (DmnDecision decision : decisions) {
      if (decisionKey.equals(decision.getKey())) {
        return decision;
      }
    }
    throw LOG.unableToFindDecisionWithKey(decisionKey);
  }

    /**
   * Parses a DMN Decision Requirements Graph from the given input stream.
   *
   * @param inputStream the input stream containing the DMN Decision Requirements Graph
   * @return the parsed DMN Decision Requirements Graph
   */
  public DmnDecisionRequirementsGraph parseDecisionRequirementsGraph(InputStream inputStream) {
    ensureNotNull("inputStream", inputStream);
    return transformer.createTransform()
      .modelInstance(inputStream)
      .transformDecisionRequirementsGraph();
  }

    /**
   * Parses a DMN decision requirements graph from the given DMN model instance.
   *
   * @param dmnModelInstance the DMN model instance to parse
   * @return the parsed DMN decision requirements graph
   */
  public DmnDecisionRequirementsGraph parseDecisionRequirementsGraph(DmnModelInstance dmnModelInstance) {
    ensureNotNull("dmnModelInstance", dmnModelInstance);
    return transformer.createTransform()
      .modelInstance(dmnModelInstance)
      .transformDecisionRequirementsGraph();
  }

    /**
   * Evaluates a DMN decision table based on the provided decision and variables.
   * 
   * @param decision the DMN decision to evaluate
   * @param variables the input variables for the decision table
   * @return the result of the decision table evaluation
   */
  public DmnDecisionTableResult evaluateDecisionTable(DmnDecision decision, Map<String, Object> variables) {
    ensureNotNull("decision", decision);
    ensureNotNull("variables", variables);
    return evaluateDecisionTable(decision, Variables.fromMap(variables).asVariableContext());
  }

    /**
   * Evaluates a DMN decision table using the given decision and variable context.
   *
   * @param decision the DMN decision to evaluate
   * @param variableContext the variable context containing input variables
   * @return the result of evaluating the decision table
   * @throws IllegalArgumentException if the decision is not a decision table
   */
  public DmnDecisionTableResult evaluateDecisionTable(DmnDecision decision, VariableContext variableContext) {
    ensureNotNull("decision", decision);
    ensureNotNull("variableContext", variableContext);

    if (decision instanceof DmnDecisionImpl && decision.isDecisionTable()) {
      DefaultDmnDecisionContext decisionContext = new DefaultDmnDecisionContext(dmnEngineConfiguration);

      DmnDecisionResult decisionResult = decisionContext.evaluateDecision(decision, variableContext);
      return DmnDecisionTableResultImpl.wrap(decisionResult);
    }
    else {
      throw LOG.decisionIsNotADecisionTable(decision);
    }
  }

    /**
   * Evaluates a decision table based on the provided decision key, input stream, and variables.
   * 
   * @param decisionKey the key identifying the decision table to be evaluated
   * @param inputStream the input stream containing the decision table definition
   * @param variables a map of variables to be used during evaluation
   * @return the result of evaluating the decision table
   */
  public DmnDecisionTableResult evaluateDecisionTable(String decisionKey, InputStream inputStream, Map<String, Object> variables) {
    ensureNotNull("variables", variables);
    return evaluateDecisionTable(decisionKey, inputStream, Variables.fromMap(variables).asVariableContext());
  }

    /**
   * Evaluates a decision table based on the provided decision key, input stream, and variable context.
   * 
   * @param decisionKey the key of the decision table to evaluate
   * @param inputStream the input stream containing the decision tables
   * @param variableContext the context containing variables to evaluate the decision table
   * @return the result of evaluating the decision table
   * @throws IllegalArgumentException if decisionKey is null
   * @throws DmnEngineException if the decision with the specified key cannot be found
   */
  public DmnDecisionTableResult evaluateDecisionTable(String decisionKey, InputStream inputStream, VariableContext variableContext) {
    ensureNotNull("decisionKey", decisionKey);
    List<DmnDecision> decisions = parseDecisions(inputStream);
    for (DmnDecision decision : decisions) {
      if (decisionKey.equals(decision.getKey())) {
        return evaluateDecisionTable(decision, variableContext);
      }
    }
    throw LOG.unableToFindDecisionWithKey(decisionKey);
  }

    /**
   * Evaluates a decision table based on the provided decision key, DMN model instance, and variables.
   * 
   * @param decisionKey the key of the decision table to evaluate
   * @param dmnModelInstance the DMN model instance containing the decision table
   * @param variables the input variables for evaluating the decision table
   * @return the result of evaluating the decision table
   */
  public DmnDecisionTableResult evaluateDecisionTable(String decisionKey, DmnModelInstance dmnModelInstance, Map<String, Object> variables) {
    ensureNotNull("variables", variables);
    return evaluateDecisionTable(decisionKey, dmnModelInstance, Variables.fromMap(variables).asVariableContext());
  }

    /**
   * Evaluates the decision table with the given decision key using the provided DMN model instance and variable context.
   *
   * @param decisionKey the key of the decision table to evaluate
   * @param dmnModelInstance the DMN model instance containing the decision tables
   * @param variableContext the context containing the variables used for evaluation
   * @return the result of evaluating the decision table with the specified key
   * @throws IllegalArgumentException if the decisionKey is null
   * @throws DmnEngineException if the decision table with the specified key cannot be found in the DMN model
   */
  public DmnDecisionTableResult evaluateDecisionTable(String decisionKey, DmnModelInstance dmnModelInstance, VariableContext variableContext) {
    ensureNotNull("decisionKey", decisionKey);
    List<DmnDecision> decisions = parseDecisions(dmnModelInstance);
    for (DmnDecision decision : decisions) {
      if (decisionKey.equals(decision.getKey())) {
        return evaluateDecisionTable(decision, variableContext);
      }
    }
    throw LOG.unableToFindDecisionWithKey(decisionKey);
  }

    /**
   * Evaluates a DMN decision with the given decision and variables.
   * 
   * @param decision the DMN decision to evaluate
   * @param variables the variables to use in the evaluation
   * @return the result of the decision evaluation
   */
  public DmnDecisionResult evaluateDecision(DmnDecision decision, Map<String, Object> variables) {
    ensureNotNull("decision", decision);
    ensureNotNull("variables", variables);
    return evaluateDecision(decision, Variables.fromMap(variables).asVariableContext());
  }

    /**
   * Evaluates a DMN decision with the given decision and variable context.
   *
   * @param decision the DMN decision to evaluate
   * @param variableContext the variable context for the evaluation
   * @return the result of evaluating the decision
   */
  public DmnDecisionResult evaluateDecision(DmnDecision decision, VariableContext variableContext) {
    ensureNotNull("decision", decision);
    ensureNotNull("variableContext", variableContext);

    if (decision instanceof DmnDecisionImpl) {
      DefaultDmnDecisionContext decisionContext = new DefaultDmnDecisionContext(dmnEngineConfiguration);
      return decisionContext.evaluateDecision(decision, variableContext);
    }
    else {
      throw LOG.decisionTypeNotSupported(decision);
    }
  }

    /**
   * Evaluates a DMN decision with the given key using the provided input stream and variables.
   * Throws an IllegalArgumentException if the variables are null.
   * 
   * @param decisionKey the key of the decision to evaluate
   * @param inputStream the input stream containing the DMN model
   * @param variables a map of variables to use in the evaluation
   * @return the result of evaluating the decision with the given key and variables
   */
  public DmnDecisionResult evaluateDecision(String decisionKey, InputStream inputStream, Map<String, Object> variables) {
    ensureNotNull("variables", variables);
    return evaluateDecision(decisionKey, inputStream, Variables.fromMap(variables).asVariableContext());
  }

    /**
   * Evaluates a decision based on the provided decision key, input stream, and variable context.
   *
   * @param decisionKey The key of the decision to evaluate
   * @param inputStream The input stream containing the decision information
   * @param variableContext The context containing variables for evaluation
   * @return The decision result after evaluation
   * @throws IllegalArgumentException if the decision key is null
   * @throws DmnEngineException if unable to find the decision with the provided key
   */
  public DmnDecisionResult evaluateDecision(String decisionKey, InputStream inputStream, VariableContext variableContext) {
    ensureNotNull("decisionKey", decisionKey);
    List<DmnDecision> decisions = parseDecisions(inputStream);
    for (DmnDecision decision : decisions) {
      if (decisionKey.equals(decision.getKey())) {
        return evaluateDecision(decision, variableContext);
      }
    }
    throw LOG.unableToFindDecisionWithKey(decisionKey);
  }

    /**
   * Evaluates a DMN decision for the given decision key, DMN model instance, and variables.
   * 
   * @param decisionKey the key of the decision to evaluate
   * @param dmnModelInstance the DMN model instance containing the decision
   * @param variables the variables to use in the evaluation
   * @return the result of evaluating the decision
   */
  public DmnDecisionResult evaluateDecision(String decisionKey, DmnModelInstance dmnModelInstance, Map<String, Object> variables) {
    ensureNotNull("variables", variables);
    return evaluateDecision(decisionKey, dmnModelInstance, Variables.fromMap(variables).asVariableContext());
  }

    /**
   * Evaluates a DMN decision based on the decision key provided, using the given DMN model instance and variable context.
   * 
   * @param decisionKey the key of the decision to evaluate
   * @param dmnModelInstance the DMN model instance containing the decisions
   * @param variableContext the context containing the variables for evaluation
   * @return the result of evaluating the decision
   * @throws IllegalArgumentException if the decisionKey is null
   * @throws DmnEngineException if the decision with the provided key cannot be found
   */
  public DmnDecisionResult evaluateDecision(String decisionKey, DmnModelInstance dmnModelInstance, VariableContext variableContext) {
    ensureNotNull("decisionKey", decisionKey);
    List<DmnDecision> decisions = parseDecisions(dmnModelInstance);
    for (DmnDecision decision : decisions) {
      if (decisionKey.equals(decision.getKey())) {
        return evaluateDecision(decision, variableContext);
      }
    }
    throw LOG.unableToFindDecisionWithKey(decisionKey);
  }

}
