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
package org.operaton.bpm.model.bpmn.instance;

import org.operaton.bpm.model.bpmn.impl.instance.ErrorRef;
import org.operaton.bpm.model.bpmn.impl.instance.InMessageRef;
import org.operaton.bpm.model.bpmn.impl.instance.OutMessageRef;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Sebastian Menski
 */
public class OperationTest extends BpmnModelElementInstanceTest {

  @Override
  public TypeAssumption getTypeAssumption() {
    return new TypeAssumption(BaseElement.class, false);
  }

  @Override
  public Collection<ChildElementAssumption> getChildElementAssumptions() {
    return Arrays.asList(
      new ChildElementAssumption(InMessageRef.class, 1, 1),
      new ChildElementAssumption(OutMessageRef.class, 0, 1),
      new ChildElementAssumption(ErrorRef.class)
    );
  }

  @Override
  public Collection<AttributeAssumption> getAttributesAssumptions() {
    return Arrays.asList(
      new AttributeAssumption("name", false, true),
      new AttributeAssumption("implementationRef")
    );
  }
}
