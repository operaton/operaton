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

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_LOOP_CARDINALITY;

import org.operaton.bpm.model.bpmn.instance.Expression;
import org.operaton.bpm.model.bpmn.instance.LoopCardinality;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

/**
 * The loopCardinality element from the tMultiInstanceLoopCharacteristics
 * complex type
 * 
 * @author Filip Hrisafov
 */
public class LoopCardinalityImpl extends ExpressionImpl implements LoopCardinality {

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder
      .defineType(LoopCardinality.class, BPMN_ELEMENT_LOOP_CARDINALITY)
      .namespaceUri(BPMN20_NS)
      .extendsType(Expression.class)
      .instanceProvider(new ModelTypeInstanceProvider<LoopCardinality>() {
      @Override
      public LoopCardinality newInstance(ModelTypeInstanceContext instanceContext) {
          return new LoopCardinalityImpl(instanceContext);
        }
      });

    typeBuilder.build();
  }

  public LoopCardinalityImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

}
