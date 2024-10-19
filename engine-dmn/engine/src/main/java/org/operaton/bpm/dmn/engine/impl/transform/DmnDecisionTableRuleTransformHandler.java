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

import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableRuleImpl;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformContext;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformHandler;
import org.operaton.bpm.model.dmn.instance.Rule;

public class DmnDecisionTableRuleTransformHandler implements DmnElementTransformHandler<Rule, DmnDecisionTableRuleImpl> {

    /**
   * Handles a DmnElementTransformContext and a Rule object by creating a DmnDecisionTableRuleImpl object.
   *
   * @param context the DmnElementTransformContext object
   * @param rule the Rule object
   * @return the DmnDecisionTableRuleImpl object created from the Rule object
   */
  public DmnDecisionTableRuleImpl handleElement(DmnElementTransformContext context, Rule rule) {
    return createFromRule(context, rule);
  }

    /**
   * Creates a DmnDecisionTableRuleImpl from a Rule object.
   * 
   * @param context the DmnElementTransformContext
   * @param rule the Rule object to transform
   * @return the created DmnDecisionTableRuleImpl
   */
  protected DmnDecisionTableRuleImpl createFromRule(DmnElementTransformContext context, Rule rule) {
    DmnDecisionTableRuleImpl decisionTableRule = createDmnElement(context, rule);

    decisionTableRule.setId(rule.getId());
    decisionTableRule.setName(rule.getLabel());

    return decisionTableRule;
  }

    /**
   * Creates a DmnDecisionTableRuleImpl element based on the provided Rule object.
   *
   * @param context the transformation context
   * @param rule the rule to be transformed into a DmnDecisionTableRuleImpl element
   * @return a new DmnDecisionTableRuleImpl element
   */
  protected DmnDecisionTableRuleImpl createDmnElement(DmnElementTransformContext context, Rule rule) {
    return new DmnDecisionTableRuleImpl();
  }

}
