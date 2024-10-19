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

import java.io.File;

import org.operaton.bpm.dmn.engine.DmnDecision;
import org.operaton.bpm.dmn.engine.DmnDecisionRequirementsGraph;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableImpl;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableInputImpl;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableOutputImpl;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableRuleImpl;
import org.operaton.bpm.dmn.engine.impl.DmnLogger;
import org.operaton.bpm.model.dmn.BuiltinAggregator;
import org.operaton.bpm.model.dmn.HitPolicy;
import org.operaton.bpm.model.dmn.instance.Decision;
import org.operaton.bpm.model.dmn.instance.Expression;

public class DmnTransformLogger extends DmnLogger {

    /**
   * Logs an informational message indicating that the expression type of a decision is not supported, and the decision will be ignored.
   * 
   * @param expression the expression object
   * @param decision the decision object
   */
  public void decisionTypeNotSupported(Expression expression, Decision decision) {
    logInfo(
      "001",
      "The expression type '{}' of the decision '{}' is not supported. The decision will be ignored.", expression.getClass().getSimpleName(), decision.getName()
    );
  }

    /**
   * Constructs a new DmnTransformException with a formatted error message indicating inability to transform decisions from a specified file.
   * 
   * @param file the file from which decisions could not be transformed
   * @param cause the cause of the exception
   * @return a new DmnTransformException with the constructed error message and cause
   */
  public DmnTransformException unableToTransformDecisionsFromFile(File file, Throwable cause) {
    return new DmnTransformException(exceptionMessage(
      "002",
      "Unable to transform decisions from file '{}'.", file.getAbsolutePath()),
      cause
    );
  }

    /**
   * Constructs a new DmnTransformException with a specific error code and message, caused by a Throwable.
   * 
   * @param cause the Throwable that caused the exception
   * @return a new DmnTransformException with the specified error code, message, and cause
   */
  public DmnTransformException unableToTransformDecisionsFromInputStream(Throwable cause) {
    return new DmnTransformException(exceptionMessage(
      "003",
      "Unable to transform decisions from input stream."),
      cause
    );
  }

    /**
   * Creates a DmnTransformException with a specific error message based on the cause.
   *
   * @param cause the cause of the exception
   * @return a DmnTransformException with a formatted error message and the specified cause
   */
  public DmnTransformException errorWhileTransformingDecisions(Throwable cause) {
    return new DmnTransformException(exceptionMessage(
      "004",
      "Error while transforming decisions: " + cause.getMessage()),
      cause
    );
  }

    /**
   * Creates a DmnTransformException with a specific message indicating that the number of inputs and input entries differ for a given rule.
   * 
   * @param inputsSize the number of inputs
   * @param inputEntriesSize the number of input entries
   * @param rule the DmnDecisionTableRuleImpl object
   * @return a DmnTransformException with the error message
   */
  public DmnTransformException differentNumberOfInputsAndInputEntries(int inputsSize, int inputEntriesSize, DmnDecisionTableRuleImpl rule) {
    return new DmnTransformException(exceptionMessage(
      "005",
      "The number of inputs '{}' and input entries differ '{}' for rule '{}'.", inputsSize, inputEntriesSize, rule)
    );
  }

  public DmnTransformException differentNumberOfOutputsAndOutputEntries(int outputsSize, int outputEntriesSize, DmnDecisionTableRuleImpl rule) {
    return new DmnTransformException(exceptionMessage(
      "006",
      "The number of outputs '{}' and output entries differ '{}' for rule '{}'.", outputsSize, outputEntriesSize, rule)
    );
  }

  public DmnTransformException hitPolicyNotSupported(DmnDecisionTableImpl decisionTable, HitPolicy hitPolicy, BuiltinAggregator aggregation) {
    if (aggregation == null) {
      return new DmnTransformException(exceptionMessage(
        "007",
        "The hit policy '{}' of decision table '{}' is not supported.", hitPolicy, decisionTable)
      );
    }

    /**
   * Creates a DmnTransformException with a specific message indicating that the number of outputs and output entries differ for a given rule.
   * 
   * @param outputsSize the number of outputs in the decision table
   * @param outputEntriesSize the number of output entries in the decision table
   * @param rule the DmnDecisionTableRuleImpl that caused the exception
   * @return a DmnTransformException with the formatted exception message
   */
  public DmnTransformException differentNumberOfOutputsAndOutputEntries(int outputsSize, int outputEntriesSize, DmnDecisionTableRuleImpl rule) {
      return new DmnTransformException(exceptionMessage(
        "007",
        "The hit policy '{}' with aggregation '{}' of decision table '{}' is not supported.", hitPolicy, aggregation, decisionTable)
      );
    }
  }

    /**
   * Creates a DmnTransformException with a specific error message based on the hit policy, aggregation, and decision table provided.
   * 
   * @param decisionTable The decision table for which the hit policy is not supported
   * @param hitPolicy The hit policy that is not supported
   * @param aggregation The aggregation type that is not supported
   * @return A DmnTransformException with a custom error message
   */
  public DmnTransformException hitPolicyNotSupported(DmnDecisionTableImpl decisionTable, HitPolicy hitPolicy, BuiltinAggregator aggregation) {
      if (aggregation == null) {
        return new DmnTransformException(exceptionMessage(
          "007",
          "The hit policy '{}' of decision table '{}' is not supported.", hitPolicy, decisionTable)
        );
      }
      else {
        return new DmnTransformException(exceptionMessage(
          "007",
          "The hit policy '{}' with aggregation '{}' of decision table '{}' is not supported.", hitPolicy, aggregation, decisionTable)
        );
      }
    }

    /**
   * Creates a DmnTransformException with a specific error message if a compound output in a decision table does not have an output name.
   *
   * @param dmnDecisionTable the decision table containing the output
   * @param dmnOutput the output that does not have an output name
   * @return a DmnTransformException with the error message
   */
  public DmnTransformException compoundOutputsShouldHaveAnOutputName(DmnDecisionTableImpl dmnDecisionTable, DmnDecisionTableOutputImpl dmnOutput) {
    return new DmnTransformException(exceptionMessage(
      "008",
      "The decision table '{}' has a compound output but output '{}' does not have an output name.", dmnDecisionTable, dmnOutput)
    );
  }

    /**
   * Creates a DmnTransformException with a specific message indicating that a compound output in a decision table has a duplicate name.
   *
   * @param dmnDecisionTable the decision table containing the duplicate output name
   * @param dmnOutput the output with the duplicate name
   * @return a new DmnTransformException with the specific exception message
   */
  public DmnTransformException compoundOutputWithDuplicateName(DmnDecisionTableImpl dmnDecisionTable, DmnDecisionTableOutputImpl dmnOutput) {
    return new DmnTransformException(exceptionMessage(
      "009",
      "The decision table '{}' has a compound output but name of output '{}' is duplicate.", dmnDecisionTable, dmnOutput)
    );
  }

    /**
   * Creates a DmnTransformException with a specific message indicating that the 'id' attribute is missing in the given DmnDecision.
   * 
   * @param dmnDecision the DmnDecision object where the 'id' attribute is missing
   * @return a new DmnTransformException with the appropriate exception message
   */
  public DmnTransformException decisionIdIsMissing(DmnDecision dmnDecision) {
    return new DmnTransformException(exceptionMessage(
      "010",
      "The decision '{}' must have an 'id' attribute set.", dmnDecision)
    );
  }

    /**
   * Creates a DmnTransformException with a specific message indicating that the 'id' attribute of a decision table input is missing.
   *
   * @param dmnDecision the DMN decision associated with the missing input ID
   * @param dmnDecisionTableInput the DMN decision table input with the missing ID attribute
   * @return a DmnTransformException with a formatted error message
   */
  public DmnTransformException decisionTableInputIdIsMissing(DmnDecision dmnDecision, DmnDecisionTableInputImpl dmnDecisionTableInput) {
    return new DmnTransformException(exceptionMessage(
      "011",
      "The decision table input '{}' of decision '{}' must have a 'id' attribute set.", dmnDecisionTableInput, dmnDecision)
    );
  }

    /**
   * Checks if the 'id' attribute is missing in the specified decision table output of a given decision.
   *
   * @param dmnDecision the decision associated with the decision table output
   * @param dmnDecisionTableOutput the decision table output to check for missing 'id' attribute
   * @return a DmnTransformException with a specific exception message
   */
  public DmnTransformException decisionTableOutputIdIsMissing(DmnDecision dmnDecision, DmnDecisionTableOutputImpl dmnDecisionTableOutput) {
    return new DmnTransformException(exceptionMessage(
      "012",
      "The decision table output '{}' of decision '{}' must have a 'id' attribute set.", dmnDecisionTableOutput, dmnDecision)
    );
  }

    /**
   * Creates a DmnTransformException for the case when the 'id' attribute is missing in a decision table rule.
   * 
   * @param dmnDecision the DmnDecision object
   * @param dmnDecisionTableRule the DmnDecisionTableRuleImpl object
   * @return a DmnTransformException with the appropriate exception message
   */
  public DmnTransformException decisionTableRuleIdIsMissing(DmnDecision dmnDecision, DmnDecisionTableRuleImpl dmnDecisionTableRule) {
    return new DmnTransformException(exceptionMessage(
      "013",
      "The decision table rule '{}' of decision '{}' must have a 'id' attribute set.", dmnDecisionTableRule, dmnDecision)
    );
  }

    /**
   * Logs an information message indicating that a decision with no expression will be ignored.
   * 
   * @param decision the decision object that has no expression
   */
  public void decisionWithoutExpression(Decision decision) {
    logInfo(
      "014",
      "The decision '{}' has no expression and will be ignored.", decision.getName()
    );
  }

    /**
   * Creates a DmnTransformException with a specific message indicating that a loop has been detected in a required decision.
   * 
   * @param decisionId The ID of the decision where the loop was detected
   * @return DmnTransformException with the error message
   */
  public DmnTransformException requiredDecisionLoopDetected(String decisionId) {
    return new DmnTransformException(exceptionMessage(
      "015",
      "The decision '{}' has a loop.", decisionId)
    );
  }

    /**
   * Creates a DmnTransformException with a specific error message based on the given cause.
   * 
   * @param cause the Throwable that caused the error
   * @return a new DmnTransformException with the error message
   */
  public DmnTransformException errorWhileTransformingDefinitions(Throwable cause) {
    return new DmnTransformException(exceptionMessage(
      "016",
      "Error while transforming decision requirements graph: " + cause.getMessage()),
      cause
    );
  }

    /**
   * Creates a DmnTransformException with a specific message if the provided DmnDecisionRequirementsGraph does not have an 'id' attribute set.
   * 
   * @param drd the DmnDecisionRequirementsGraph to check for the presence of an 'id' attribute
   * @return a DmnTransformException with an error message indicating that the 'id' attribute is missing
   */
  public DmnTransformException drdIdIsMissing(DmnDecisionRequirementsGraph drd) {
    return new DmnTransformException(exceptionMessage(
      "017",
      "The decision requirements graph '{}' must have an 'id' attribute set.", drd)
    );
  }

    /**
   * Creates a DmnTransformException with a specific message indicating that a decision variable is missing.
   *
   * @param decisionId the ID of the decision for which the variable is missing
   * @return a DmnTransformException instance with the generated exception message
   */
  public DmnTransformException decisionVariableIsMissing(String decisionId) {
    return new DmnTransformException(exceptionMessage(
        "018",
        "The decision '{}' must have an 'variable' element if it contains a literal expression.",
        decisionId));
  }

}
