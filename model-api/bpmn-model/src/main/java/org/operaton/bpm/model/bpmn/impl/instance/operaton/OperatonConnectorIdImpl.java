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
package org.operaton.bpm.model.bpmn.impl.instance.operaton;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ELEMENT_CONNECTOR_ID;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;

import org.operaton.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonConnectorId;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;

/**
 * The BPMN connectorId operaton extension element
 *
 * @author Sebastian Menski
 */
public class OperatonConnectorIdImpl extends BpmnModelElementInstanceImpl implements OperatonConnectorId {

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OperatonConnectorId.class, OPERATON_ELEMENT_CONNECTOR_ID)
      .namespaceUri(OPERATON_NS)
      .instanceProvider(OperatonConnectorIdImpl::new);

    typeBuilder.build();
  }

  public OperatonConnectorIdImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

}
