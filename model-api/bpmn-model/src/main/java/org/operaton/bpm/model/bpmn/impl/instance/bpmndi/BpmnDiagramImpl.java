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
package org.operaton.bpm.model.bpmn.impl.instance.bpmndi;

import java.util.Collection;

import org.operaton.bpm.model.bpmn.impl.instance.di.DiagramImpl;
import org.operaton.bpm.model.bpmn.instance.bpmndi.BpmnDiagram;
import org.operaton.bpm.model.bpmn.instance.bpmndi.BpmnLabelStyle;
import org.operaton.bpm.model.bpmn.instance.bpmndi.BpmnPlane;
import org.operaton.bpm.model.bpmn.instance.di.Diagram;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMNDI_ELEMENT_BPMN_DIAGRAM;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMNDI_NS;

/**
 * The BPMNDI BPMNDiagram element
 *
 * @author Sebastian Menski
 */
public class BpmnDiagramImpl extends DiagramImpl implements BpmnDiagram {

  protected static ChildElement<BpmnPlane> bpmnPlaneChild;
  protected static ChildElementCollection<BpmnLabelStyle> bpmnLabelStyleCollection;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(BpmnDiagram.class, BPMNDI_ELEMENT_BPMN_DIAGRAM)
      .namespaceUri(BPMNDI_NS)
      .extendsType(Diagram.class)
      .instanceProvider(BpmnDiagramImpl::new);

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    bpmnPlaneChild = sequenceBuilder.element(BpmnPlane.class)
      .required()
      .build();

    bpmnLabelStyleCollection = sequenceBuilder.elementCollection(BpmnLabelStyle.class)
      .build();

    typeBuilder.build();
  }

  public BpmnDiagramImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public BpmnPlane getBpmnPlane() {
    return bpmnPlaneChild.getChild(this);
  }

  @Override
  public void setBpmnPlane(BpmnPlane bpmnPlane) {
    bpmnPlaneChild.setChild(this, bpmnPlane);
  }

  @Override
  public Collection<BpmnLabelStyle> getBpmnLabelStyles() {
    return bpmnLabelStyleCollection.get(this);
  }
}
