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
package org.operaton.bpm.model.bpmn.impl.instance.operaton;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ELEMENT_CONNECTOR;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;

import org.operaton.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonConnector;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonConnectorId;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonInputOutput;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

/**
 * The BPMN connector operaton extension element
 *
 * @author Sebastian Menski
 */
public class OperatonConnectorImpl extends BpmnModelElementInstanceImpl implements OperatonConnector {

  protected static ChildElement<OperatonConnectorId> operatonConnectorIdChild;
  protected static ChildElement<OperatonInputOutput> operatonInputOutputChild;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OperatonConnector.class, OPERATON_ELEMENT_CONNECTOR)
      .namespaceUri(OPERATON_NS)
      .instanceProvider(OperatonConnectorImpl::new);

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    operatonConnectorIdChild = sequenceBuilder.element(OperatonConnectorId.class)
      .required()
      .build();

    operatonInputOutputChild = sequenceBuilder.element(OperatonInputOutput.class)
      .build();

    typeBuilder.build();
  }

  public OperatonConnectorImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public OperatonConnectorId getOperatonConnectorId() {
    return operatonConnectorIdChild.getChild(this);
  }

  @Override
  public void setOperatonConnectorId(OperatonConnectorId operatonConnectorId) {
    operatonConnectorIdChild.setChild(this, operatonConnectorId);
  }

  @Override
  public OperatonInputOutput getOperatonInputOutput() {
    return operatonInputOutputChild.getChild(this);
  }

  @Override
  public void setOperatonInputOutput(OperatonInputOutput operatonInputOutput) {
    operatonInputOutputChild.setChild(this, operatonInputOutput);
  }

}
