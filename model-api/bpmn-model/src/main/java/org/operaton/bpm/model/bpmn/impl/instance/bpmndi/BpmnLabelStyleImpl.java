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
package org.operaton.bpm.model.bpmn.impl.instance.bpmndi;

import org.operaton.bpm.model.bpmn.impl.instance.di.StyleImpl;
import org.operaton.bpm.model.bpmn.instance.bpmndi.BpmnLabelStyle;
import org.operaton.bpm.model.bpmn.instance.dc.Font;
import org.operaton.bpm.model.bpmn.instance.di.Style;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMNDI_ELEMENT_BPMN_LABEL_STYLE;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMNDI_NS;

/**
 * The BPMNDI BPMNLabelStyle element
 *
 * @author Sebastian Menski
 */
public class BpmnLabelStyleImpl extends StyleImpl implements BpmnLabelStyle {

  protected static ChildElement<Font> fontChild;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(BpmnLabelStyle.class, BPMNDI_ELEMENT_BPMN_LABEL_STYLE)
      .namespaceUri(BPMNDI_NS)
      .extendsType(Style.class)
      .instanceProvider(BpmnLabelStyleImpl::new);

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    fontChild = sequenceBuilder.element(Font.class)
      .required()
      .build();

    typeBuilder.build();
  }

  public BpmnLabelStyleImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public Font getFont() {
    return fontChild.getChild(this);
  }

  @Override
  public void setFont(Font font) {
    fontChild.setChild(this, font);
  }
}
