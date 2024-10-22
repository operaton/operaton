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

import org.operaton.bpm.dmn.engine.impl.DmnDecisionRequirementsGraphImpl;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformContext;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformHandler;
import org.operaton.bpm.model.dmn.instance.Definitions;

public class DmnDecisionRequirementsGraphTransformHandler implements DmnElementTransformHandler<Definitions, DmnDecisionRequirementsGraphImpl> {

    /**
   * Handles the given DMN element and returns a decision requirements graph implementation based on the provided definitions.
   *
   * @param context the element transformation context
   * @param definitions the DMN definitions
   * @return a decision requirements graph implementation created from the definitions
   */
  public DmnDecisionRequirementsGraphImpl handleElement(DmnElementTransformContext context, Definitions definitions) {
    return createFromDefinitions(context, definitions);
  }

    /**
   * Creates a new decision requirements graph from the provided Definitions object.
   * 
   * @param context the element transform context
   * @param definitions the definitions object to create the decision requirements graph from
   * @return the newly created decision requirements graph
   */
  protected DmnDecisionRequirementsGraphImpl createFromDefinitions(DmnElementTransformContext context, Definitions definitions) {
    DmnDecisionRequirementsGraphImpl drd = createDmnElement();

    drd.setKey(definitions.getId());
    drd.setName(definitions.getName());

    return drd;
  }

    /**
   * Creates a new instance of DmnDecisionRequirementsGraphImpl
   * 
   * @return a new DmnDecisionRequirementsGraphImpl instance
   */
  protected DmnDecisionRequirementsGraphImpl createDmnElement() {
    return new DmnDecisionRequirementsGraphImpl();
  }

}
