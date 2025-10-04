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
package org.operaton.bpm.dmn.engine.impl.hitpolicy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.operaton.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationEvent;
import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedDecisionRule;
import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedOutput;
import org.operaton.bpm.dmn.engine.impl.DmnLogger;
import org.operaton.bpm.dmn.engine.impl.delegate.DmnDecisionTableEvaluationEventImpl;
import org.operaton.bpm.dmn.engine.impl.spi.hitpolicy.DmnHitPolicyHandler;
import org.operaton.bpm.model.dmn.HitPolicy;

/**
 * Implements the ANY hit policy as defined in the DMN 1.3 specification (section 8.2.8).
 *
 * <p>The ANY hit policy allows multiple rules to match, but requires that all matching rules
 * produce identical outputs. If this condition is satisfied, any one of the matching rules
 * (typically the first) is returned. If matching rules have different outputs, an exception
 * is thrown.</p>
 *
 * <p><strong>Key Characteristics:</strong></p>
 * <ul>
 *   <li>Allows multiple rules to match</li>
 *   <li>Validates that all matching rules have identical output values</li>
 *   <li>Returns the first matching rule if validation passes</li>
 *   <li>Throws an exception if outputs differ</li>
 * </ul>
 *
 * <p><strong>Use Case:</strong> The ANY policy is useful when multiple rules express the same
 * decision in different ways (e.g., for readability or completeness checking), but the
 * outputs must always be consistent.</p>
 *
 * <p><strong>Difference to other single hit policies:</strong></p>
 * <ul>
 *   <li><strong>ANY vs UNIQUE:</strong> ANY allows multiple matches if outputs are equal;
 *       UNIQUE forbids multiple matches entirely</li>
 *   <li><strong>ANY vs FIRST:</strong> ANY validates output equality; FIRST simply returns
 *       the first match without validation</li>
 * </ul>
 *
 * @see DmnHitPolicyHandler
 * @see UniqueHitPolicyHandler
 * @see FirstHitPolicyHandler
 */
public class AnyHitPolicyHandler implements DmnHitPolicyHandler {

  public static final DmnHitPolicyLogger LOG = DmnLogger.HIT_POLICY_LOGGER;
  private static final HitPolicyEntry HIT_POLICY = new HitPolicyEntry(HitPolicy.ANY, null);

  /**
   * Retrieves the hit policy entry for the ANY hit policy.
   *
   * @return the hit policy entry defining the ANY hit policy (without aggregator)
   */
  @Override
  public HitPolicyEntry getHitPolicyEntry() {
    return HIT_POLICY;
  }

  /**
   * Applies the ANY hit policy logic to the evaluation event.
   *
   * <p>This method validates that all matching rules have identical outputs. If the validation
   * passes, the first matching rule is retained and returned. If outputs differ, an exception
   * is thrown.</p>
   *
   * @param decisionTableEvaluationEvent the evaluation event containing the matching rules
   * @return the evaluation event with only the first matching rule if all outputs are equal
   * @throws DmnHitPolicyException if matching rules have different output values
   */
  @Override
  public DmnDecisionTableEvaluationEvent apply(DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    List<DmnEvaluatedDecisionRule> matchingRules = decisionTableEvaluationEvent.getMatchingRules();

    if (!matchingRules.isEmpty()) {
      if (allOutputsAreEqual(matchingRules)) {
        DmnEvaluatedDecisionRule firstMatchingRule = matchingRules.get(0);
        if (decisionTableEvaluationEvent instanceof DmnDecisionTableEvaluationEventImpl impl) {
          impl.setMatchingRules(Collections.singletonList(firstMatchingRule));
        }
      } else {
        throw LOG.anyHitPolicyRequiresThatAllOutputsAreEqual(matchingRules);
      }
    }

    return decisionTableEvaluationEvent;
  }

  /**
   * Validates that all matching rules have identical output values.
   *
   * <p>This method compares the output entries of all matching rules using {@link Objects#equals},
   * which properly handles null values and delegates to the {@link Map#equals} method for
   * non-null values.</p>
   *
   * <p><strong>Edge Cases:</strong></p>
   * <ul>
   *   <li>If the first rule has null outputs, all other rules must also have null outputs</li>
   *   <li>Empty output maps are considered equal</li>
   *   <li>Output values are compared using their {@link Object#equals} implementation</li>
   * </ul>
   *
   * @param matchingRules the list of matching rules to validate
   * @return true if all rules have identical outputs, false otherwise
   */
  protected boolean allOutputsAreEqual(List<DmnEvaluatedDecisionRule> matchingRules) {
    Map<String, DmnEvaluatedOutput> firstOutputEntries = matchingRules.get(0).getOutputEntries();

    for (int i = 1; i < matchingRules.size(); i++) {
      if (!Objects.equals(firstOutputEntries, matchingRules.get(i).getOutputEntries())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns a string representation of the AnyHitPolicyHandler.
   *
   * @return a string that represents this handler instance with its hit policy
   */
  @Override
  public String toString() {
    return "AnyHitPolicyHandler{hitPolicy=" + HIT_POLICY.getHitPolicy() + "}";
  }

}
