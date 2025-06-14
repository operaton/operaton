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
package org.operaton.bpm.model.xml.impl.type.attribute;

import org.operaton.bpm.model.xml.Model;
import org.operaton.bpm.model.xml.impl.ModelBuildOperation;
import org.operaton.bpm.model.xml.impl.type.ModelElementTypeImpl;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.attribute.AttributeBuilder;


/**
 *
 * @author Daniel Meyer
 *
 */
public abstract class AttributeBuilderImpl<T> implements AttributeBuilder<T>, ModelBuildOperation {

  private final AttributeImpl<T> attribute;
  private final ModelElementTypeImpl modelType;

  AttributeBuilderImpl(String attributeName, ModelElementTypeImpl modelType, AttributeImpl<T> attribute) {
    this.modelType = modelType;
    this.attribute = attribute;
    attribute.setAttributeName(attributeName);
  }

  @Override
  public AttributeBuilder<T> namespace(String namespaceUri) {
    attribute.setNamespaceUri(namespaceUri);
    return this;
  }

  @Override
  public AttributeBuilder<T> idAttribute() {
    attribute.setId();
    return this;
  }


  @Override
  public AttributeBuilder<T> defaultValue(T defaultValue) {
    attribute.setDefaultValue(defaultValue);
    return this;
  }

  @Override
  public AttributeBuilder<T> required() {
    attribute.setRequired(true);
    return this;
  }

  @Override
  public Attribute<T> build() {
    modelType.registerAttribute(attribute);
    return attribute;
  }

  @Override
  public void performModelBuild(Model model) {
    // do nothing
  }

}
