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
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_LANGUAGE;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ELEMENT_EXPRESSION;

import org.operaton.bpm.model.cmmn.instance.Body;
import org.operaton.bpm.model.cmmn.instance.CmmnElement;
import org.operaton.bpm.model.cmmn.instance.Expression;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

/**
 * @author Roman Smirnov
 *
 */
public class ExpressionImpl extends CmmnElementImpl implements Expression {

  protected static Attribute<String> languageAttribute;

  // cmmn 1.0
  @Deprecated
  protected static ChildElement<Body> bodyChild;

  public ExpressionImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getText() {
    if (isCmmn11()) {
      return getTextContent();
    }
    else {
      return getBody();
    }
  }

  @Override
  public void setText(String text) {
    if (isCmmn11()) {
      setTextContent(text);
    }
    else {
      setBody(text);
    }
  }

  @Override
  public String getBody() {
    Body body = bodyChild.getChild(this);
    if (body != null) {
      return body.getTextContent();
    }
    return null;
  }

  @Override
  public void setBody(String body) {
    bodyChild.getChild(this).setTextContent(body);
  }

  @Override
  public String getLanguage() {
    return languageAttribute.getValue(this);
  }

  @Override
  public void setLanguage(String language) {
    languageAttribute.setValue(this, language);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Expression.class, CMMN_ELEMENT_EXPRESSION)
        .namespaceUri(CMMN11_NS)
        .extendsType(CmmnElement.class)
        .instanceProvider(ExpressionImpl::new);

    languageAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_LANGUAGE)
        .defaultValue("http://www.w3.org/1999/XPath")
        .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    bodyChild = sequenceBuilder.element(Body.class)
        .build();

    typeBuilder.build();
  }

}
