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

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;

import java.util.Collection;

import org.operaton.bpm.model.bpmn.impl.BpmnModelConstants;
import org.operaton.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonEntry;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonMap;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

/**
 * The BPMN operatonMap extension element
 *
 * @author Sebastian Menski
 */
public class OperatonMapImpl extends BpmnModelElementInstanceImpl implements OperatonMap {

  protected static ChildElementCollection<OperatonEntry> operatonEntryCollection;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OperatonMap.class, BpmnModelConstants.OPERATON_ELEMENT_MAP)
      .namespaceUri(OPERATON_NS)
      .instanceProvider(new ModelTypeInstanceProvider<OperatonMap>() {
      @Override
      public OperatonMap newInstance(ModelTypeInstanceContext instanceContext) {
          return new OperatonMapImpl(instanceContext);
        }
      });

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    operatonEntryCollection = sequenceBuilder.elementCollection(OperatonEntry.class)
      .build();

    typeBuilder.build();
  }

  public OperatonMapImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public Collection<OperatonEntry> getOperatonEntries() {
    return operatonEntryCollection.get(this);
  }

}
