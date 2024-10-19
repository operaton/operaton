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

import org.operaton.bpm.dmn.engine.impl.DmnDecisionImpl;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformContext;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformHandler;
import org.operaton.bpm.model.dmn.instance.Decision;

public class DmnDecisionTransformHandler implements DmnElementTransformHandler<Decision, DmnDecisionImpl> {

    /**
   * Handles a DMN element and returns a DmnDecisionImpl object.
   * 
   * @param context the DmnElementTransformContext object
   * @param decision the Decision object to handle
   * @return a DmnDecisionImpl object created from the provided Decision object
   */
  public DmnDecisionImpl handleElement(DmnElementTransformContext context, Decision decision) {
    return createFromDecision(context, decision);
  }

    /**
   * Creates a DmnDecisionImpl entity from a Decision element.
   *
   * @param context the transformation context
   * @param decision the Decision element to transform
   * @return the created DmnDecisionImpl entity
   */
  protected DmnDecisionImpl createFromDecision(DmnElementTransformContext context, Decision decision) {
    DmnDecisionImpl decisionEntity = createDmnElement();

    decisionEntity.setKey(decision.getId());
    decisionEntity.setName(decision.getName());
    return decisionEntity;
  }

    /**
   * Creates and returns a new instance of DmnDecisionImpl.
   *
   * @return a new DmnDecisionImpl instance
   */
  protected DmnDecisionImpl createDmnElement() {
    return new DmnDecisionImpl();
  }

}
