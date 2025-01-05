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
package org.operaton.bpm.model.bpmn.impl.instance;

import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_OUTPUT_SET_REFS;

/**
 * The BPMN outputSetRefs element of the BPMN tInputSet type
 *
 * @author Sebastian Menski
 */
public class OutputSetRefs extends BpmnModelElementInstanceImpl {

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OutputSetRefs.class, BPMN_ELEMENT_OUTPUT_SET_REFS)
      .namespaceUri(BPMN20_NS)
      .instanceProvider(instanceContext -> new OutputSetRefs(instanceContext));

    typeBuilder.build();
  }

  public OutputSetRefs(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }
}
