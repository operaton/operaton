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
package org.operaton.bpm.model.cmmn.impl.instance;

import org.operaton.bpm.model.cmmn.instance.CmmnElement;
import org.operaton.bpm.model.cmmn.instance.Criterion;
import org.operaton.bpm.model.cmmn.instance.Sentry;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN11_NS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_NAME;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_SENTRY_REF;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ELEMENT_CRITERION;

/**
 * @author Roman Smirnov
 *
 */
public abstract class CriterionImpl extends CmmnElementImpl implements Criterion {

  protected static Attribute<String> nameAttribute;
  protected static AttributeReference<Sentry> sentryRefAttribute;

  protected CriterionImpl(ModelTypeInstanceContext instanceContext) {
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
  public Sentry getSentry() {
    return sentryRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setSentry(Sentry sentry) {
    sentryRefAttribute.setReferenceTargetElement(this, sentry);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Criterion.class, CMMN_ELEMENT_CRITERION)
        .extendsType(CmmnElement.class)
        .namespaceUri(CMMN11_NS)
        .abstractType();

    nameAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_NAME)
        .build();

    sentryRefAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_SENTRY_REF)
        .idAttributeReference(Sentry.class)
        .build();

    typeBuilder.build();
  }

}
