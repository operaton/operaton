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
package org.operaton.bpm.model.xml.type;

import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;
import org.operaton.bpm.model.xml.type.attribute.AttributeBuilder;
import org.operaton.bpm.model.xml.type.attribute.StringAttributeBuilder;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

/**
 * @author Daniel Meyer
 */
public interface ModelElementTypeBuilder {

  ModelElementTypeBuilder namespaceUri(String namespaceUri);

  ModelElementTypeBuilder extendsType(Class<? extends ModelElementInstance> extendedType);

  <T extends ModelElementInstance> ModelElementTypeBuilder instanceProvider(ModelTypeInstanceProvider<T> instanceProvider);

  ModelElementTypeBuilder abstractType();

  AttributeBuilder<Boolean> booleanAttribute(String attributeName);

  StringAttributeBuilder stringAttribute(String attributeName);

  AttributeBuilder<Integer> integerAttribute(String attributeName);

  AttributeBuilder<Double> doubleAttribute(String attributeName);

  <V extends Enum<V>> AttributeBuilder<V> enumAttribute(String attributeName, Class<V> enumType);

  <V extends Enum<V>> AttributeBuilder<V> namedEnumAttribute(String attributeName, Class<V> enumType);

  SequenceBuilder sequence();

  ModelElementType build();


  interface ModelTypeInstanceProvider<T extends ModelElementInstance> {
    T newInstance(ModelTypeInstanceContext instanceContext);
  }

}
