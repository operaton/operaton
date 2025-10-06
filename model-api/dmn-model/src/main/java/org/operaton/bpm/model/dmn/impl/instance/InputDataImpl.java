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
package org.operaton.bpm.model.dmn.impl.instance;

import org.operaton.bpm.model.dmn.instance.DrgElement;
import org.operaton.bpm.model.dmn.instance.InformationItem;
import org.operaton.bpm.model.dmn.instance.InputData;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.DMN_ELEMENT_INPUT_DATA;
import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.LATEST_DMN_NS;

public class InputDataImpl extends DrgElementImpl implements InputData {

  protected static ChildElement<InformationItem> informationItemChild;

  public InputDataImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public InformationItem getInformationItem() {
    return informationItemChild.getChild(this);
  }

  @Override
  public void setInformationItem(InformationItem informationItem) {
    informationItemChild.setChild(this, informationItem);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(InputData.class, DMN_ELEMENT_INPUT_DATA)
      .namespaceUri(LATEST_DMN_NS)
      .extendsType(DrgElement.class)
      .instanceProvider(InputDataImpl::new);

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    informationItemChild = sequenceBuilder.element(InformationItem.class)
      .build();

    typeBuilder.build();
  }

}
