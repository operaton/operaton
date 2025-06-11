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
package org.operaton.bpm.model.bpmn.impl.instance;

import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.IntermediateThrowEventBuilder;
import org.operaton.bpm.model.bpmn.impl.BpmnModelConstants;
import org.operaton.bpm.model.bpmn.instance.IntermediateThrowEvent;
import org.operaton.bpm.model.bpmn.instance.ThrowEvent;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_INTERMEDIATE_THROW_EVENT;

/**
 * The BPMN intermediateThrowEvent element
 *
 * @author Sebastian Menski
 */
public class IntermediateThrowEventImpl extends ThrowEventImpl implements IntermediateThrowEvent {

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(IntermediateThrowEvent.class, BPMN_ELEMENT_INTERMEDIATE_THROW_EVENT)
      .namespaceUri(BpmnModelConstants.BPMN20_NS)
      .extendsType(ThrowEvent.class)
      .instanceProvider(IntermediateThrowEventImpl::new);

    typeBuilder.build();
  }

  public IntermediateThrowEventImpl(ModelTypeInstanceContext context) {
    super(context);
  }

  @Override
  public IntermediateThrowEventBuilder builder() {
    return new IntermediateThrowEventBuilder((BpmnModelInstance) modelInstance, this);
  }
}
