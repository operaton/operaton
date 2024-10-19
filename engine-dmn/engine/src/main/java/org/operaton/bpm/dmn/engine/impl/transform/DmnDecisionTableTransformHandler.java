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

import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableImpl;
import org.operaton.bpm.dmn.engine.impl.DmnLogger;
import org.operaton.bpm.dmn.engine.impl.spi.hitpolicy.DmnHitPolicyHandler;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformContext;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformHandler;
import org.operaton.bpm.model.dmn.BuiltinAggregator;
import org.operaton.bpm.model.dmn.HitPolicy;
import org.operaton.bpm.model.dmn.instance.DecisionTable;

public class DmnDecisionTableTransformHandler implements DmnElementTransformHandler<DecisionTable, DmnDecisionTableImpl> {

  protected static final DmnTransformLogger LOG = DmnLogger.TRANSFORM_LOGGER;

    /**
   * Handles the given DecisionTable element by creating a new DmnDecisionTableImpl object from it.
   *
   * @param context the DmnElementTransformContext object
   * @param decisionTable the DecisionTable object to be transformed
   * @return a new DmnDecisionTableImpl object created from the given DecisionTable
   */
  public DmnDecisionTableImpl handleElement(DmnElementTransformContext context, DecisionTable decisionTable) {
    return createFromDecisionTable(context, decisionTable);
  }

    /**
   * Creates a DmnDecisionTableImpl object from a provided DecisionTable object.
   * 
   * @param context The transformation context
   * @param decisionTable The DecisionTable object to create from
   * @return The created DmnDecisionTableImpl object
   */
  protected DmnDecisionTableImpl createFromDecisionTable(DmnElementTransformContext context, DecisionTable decisionTable) {
    DmnDecisionTableImpl dmnDecisionTable = createDmnElement(context, decisionTable);

    dmnDecisionTable.setHitPolicyHandler(getHitPolicyHandler(context, decisionTable, dmnDecisionTable));

    return dmnDecisionTable;
  }

    /**
   * Creates a DmnDecisionTableImpl element based on the provided DecisionTable element.
   *
   * @param context the transformation context
   * @param decisionTable the DecisionTable element to use for creating the DmnDecisionTableImpl
   * @return the created DmnDecisionTableImpl element
   */
  protected DmnDecisionTableImpl createDmnElement(DmnElementTransformContext context, DecisionTable decisionTable) {
    return new DmnDecisionTableImpl();
  }

    /**
   * Retrieves the hit policy handler for the given decision table based on the hit policy and aggregation specified.
   * If a specific hit policy handler is not found, an exception is thrown.
   * 
   * @param context the transformation context
   * @param decisionTable the decision table
   * @param dmnDecisionTable the DMN decision table
   * @return the hit policy handler for the given decision table
   */
  protected DmnHitPolicyHandler getHitPolicyHandler(DmnElementTransformContext context, DecisionTable decisionTable, DmnDecisionTableImpl dmnDecisionTable) {
    HitPolicy hitPolicy = decisionTable.getHitPolicy();
    if (hitPolicy == null) {
      // use default hit policy
      hitPolicy = HitPolicy.UNIQUE;
    }
    BuiltinAggregator aggregation = decisionTable.getAggregation();
    DmnHitPolicyHandler hitPolicyHandler = context.getHitPolicyHandlerRegistry().getHandler(hitPolicy, aggregation);
    if (hitPolicyHandler != null) {
      return hitPolicyHandler;
    }
    else {
      throw LOG.hitPolicyNotSupported(dmnDecisionTable, hitPolicy, aggregation);
    }
  }

}
