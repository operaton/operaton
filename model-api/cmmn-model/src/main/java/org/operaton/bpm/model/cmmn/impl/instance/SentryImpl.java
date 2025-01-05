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
package org.operaton.bpm.model.cmmn.impl.instance;

import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN11_NS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_NAME;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ELEMENT_SENTRY;

import java.util.Collection;

import org.operaton.bpm.model.cmmn.instance.CmmnElement;
import org.operaton.bpm.model.cmmn.instance.IfPart;
import org.operaton.bpm.model.cmmn.instance.OnPart;
import org.operaton.bpm.model.cmmn.instance.Sentry;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

/**
 * @author Roman Smirnov
 *
 */
public class SentryImpl extends CmmnElementImpl implements Sentry {

  protected static Attribute<String> nameAttribute;
  protected static ChildElementCollection<OnPart> onPartCollection;
  protected static ChildElement<IfPart> ifPartChild;

  public SentryImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getName() {
    return nameAttribute.getValue(this);
  }

  @Override
  public void setName(String name) {
    nameAttribute.setValue(this, name);
  }

  @Override
  public Collection<OnPart> getOnParts() {
    return onPartCollection.get(this);
  }

  @Override
  public IfPart getIfPart() {
    return ifPartChild.getChild(this);
  }

  @Override
  public void setIfPart(IfPart ifPart) {
    ifPartChild.setChild(this, ifPart);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Sentry.class, CMMN_ELEMENT_SENTRY)
        .extendsType(CmmnElement.class)
        .namespaceUri(CMMN11_NS)
        .instanceProvider(instanceContext -> new SentryImpl(instanceContext));

    nameAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_NAME)
        .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    onPartCollection = sequenceBuilder.elementCollection(OnPart.class)
        .build();

    ifPartChild = sequenceBuilder.element(IfPart.class)
        .build();

    typeBuilder.build();
  }

}
