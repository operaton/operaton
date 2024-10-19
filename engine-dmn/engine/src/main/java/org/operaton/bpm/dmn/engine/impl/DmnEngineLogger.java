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

import org.operaton.bpm.dmn.engine.DmnDecision;
import org.operaton.bpm.dmn.engine.DmnDecisionLogic;
import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.DmnDecisionResultEntries;
import org.operaton.bpm.dmn.engine.DmnDecisionRuleResult;
import org.operaton.bpm.dmn.engine.DmnDecisionTableResult;
import org.operaton.bpm.dmn.engine.DmnEngineException;
import org.operaton.bpm.dmn.engine.impl.transform.DmnTransformException;

public class DmnEngineLogger extends DmnLogger {

    /**
   * Creates a DmnTransformException with a specific message if unable to find a decision with the given key.
   * 
   * @param decisionKey the key of the decision that could not be found
   * @return a DmnTransformException with the appropriate error message
   */
  public DmnTransformException unableToFindDecisionWithKey(String decisionKey) {
    return new DmnTransformException(exceptionMessage(
      "001",
      "Unable to find decision with id '{}' in model.", decisionKey)
    );
  }

    /**
   * Constructs a DmnEvaluationException with a specific message based on the expression and expression language, along with a cause.
   * 
   * @param expression the expression being evaluated
   * @param expressionLanguage the language of the expression being evaluated
   * @param cause the cause of the evaluation exception
   * @return a DmnEvaluationException with a formatted exception message
   */
  public DmnEvaluationException unableToEvaluateExpression(String expression, String expressionLanguage, Throwable cause) {
      return new DmnEvaluationException(exceptionMessage(
        "002",
        "Unable to evaluate expression for language '{}': '{}'", expressionLanguage, expression),
        cause
      );
    }

    /**
   * Creates a DmnEvaluationException with an error message stating that no script engine was found
   * for the given expression language.
   *
   * @param expressionLanguage the expression language for which the script engine was not found
   * @return a DmnEvaluationException with the error message
   */
  public DmnEvaluationException noScriptEngineFoundForLanguage(String expressionLanguage) {
    return new DmnEvaluationException(exceptionMessage(
      "003",
      "Unable to find script engine for expression language '{}'.", expressionLanguage)
    );
  }

    /**
   * Creates a DmnEngineException with a message indicating that the decision type is not supported by the DMN engine.
   * 
   * @param decision the DmnDecision object that is not supported
   * @return a new DmnEngineException with the appropriate message
   */
  public DmnEngineException decisionTypeNotSupported(DmnDecision decision) {
      return new DmnEngineException(exceptionMessage(
        "004",
        "Decision type '{}' not supported by DMN engine.", decision.getClass())
      );
    }

    /**
   * Creates a DmnEngineException with a specific message indicating that the provided value is invalid for a given type definition.
   * 
   * @param typeName the name of the type definition
   * @param value the value that is invalid for the type definition
   * @return a DmnEngineException with a formatted error message
   */
  public DmnEngineException invalidValueForTypeDefinition(String typeName, Object value) {
    return new DmnEngineException(exceptionMessage(
      "005",
      "Invalid value '{}' for clause with type '{}'.", value, typeName)
    );
  }

    /**
   * Logs a warning message for an unsupported type definition for a clause.
   * 
   * @param typeName The name of the unsupported type
   */
  public void unsupportedTypeDefinitionForClause(String typeName) {
      logWarn(
        "006",
        "Unsupported type '{}' for clause. Values of this clause will not transform into another type.", typeName
      );
    }

    /**
   * Constructs a DmnDecisionResultException with a specific error message when a decision rule result has more than one value.
   *
   * @param ruleResult the decision rule result with more than one value
   * @return the DmnDecisionResultException with the specific error message
   */
  public DmnDecisionResultException decisionOutputHasMoreThanOneValue(DmnDecisionRuleResult ruleResult) {
    return new DmnDecisionResultException(exceptionMessage(
      "007",
      "Unable to get single decision rule result entry as it has more than one entry '{}'", ruleResult)
    );
  }

    /**
   * Creates a DmnDecisionResultException with a specific exception message if the provided decision table result has more than one output.
   * 
   * @param decisionResult the decision table result to check
   * @return a DmnDecisionResultException with the exception message
   */
  public DmnDecisionResultException decisionResultHasMoreThanOneOutput(DmnDecisionTableResult decisionResult) {
      return new DmnDecisionResultException(exceptionMessage(
        "008",
        "Unable to get single decision rule result as it has more than one rule result '{}'", decisionResult)
      );
    }

    /**
   * Creates a DmnTransformException with a specific exception message indicating that no decision table could be found in the model.
   * 
   * @return a DmnTransformException with the exception message "009 - Unable to find any decision table in model."
   */
  public DmnTransformException unableToFindAnyDecisionTable() {
    return new DmnTransformException(exceptionMessage(
      "009",
      "Unable to find any decision table in model.")
    );
  }

    /**
   * Creates a DmnDecisionResultException with a specific error message when a decision result has more than one value.
   * 
   * @param result the decision result entries with more than one value
   * @return a DmnDecisionResultException with the appropriate error message
   */
  public DmnDecisionResultException decisionOutputHasMoreThanOneValue(DmnDecisionResultEntries result) {
    return new DmnDecisionResultException(exceptionMessage(
      "010",
      "Unable to get single decision result entry as it has more than one entry '{}'", result)
    );
  }

    /**
   * Creates a DmnDecisionResultException with a specific exception message indicating that the decision result has more than one output.
   * 
   * @param decisionResult the decision result with multiple outputs
   * @return the DmnDecisionResultException with the exception message
   */
  public DmnDecisionResultException decisionResultHasMoreThanOneOutput(DmnDecisionResult decisionResult) {
    return new DmnDecisionResultException(exceptionMessage(
      "011",
      "Unable to get single decision result as it has more than one result '{}'", decisionResult)
    );
  }

    /**
   * Creates a DmnEngineException with a message indicating that the decision logic type is not supported by the DMN engine.
   *
   * @param decisionLogic the decision logic that is not supported
   * @return a DmnEngineException with the appropriate exception message
   */
  public DmnEngineException decisionLogicTypeNotSupported(DmnDecisionLogic decisionLogic) {
    return new DmnEngineException(exceptionMessage(
      "012",
      "Decision logic type '{}' not supported by DMN engine.", decisionLogic.getClass())
    );
  }

    /**
   * Creates a DmnEngineException for the case when the provided DmnDecision is not implemented as a decision table.
   * 
   * @param decision the DmnDecision that is not a decision table
   * @return a DmnEngineException with the corresponding exception message
   */
  public DmnEngineException decisionIsNotADecisionTable(DmnDecision decision) {
    return new DmnEngineException(exceptionMessage(
      "013",
      "The decision '{}' is not implemented as decision table.", decision)
    );
  }

}
