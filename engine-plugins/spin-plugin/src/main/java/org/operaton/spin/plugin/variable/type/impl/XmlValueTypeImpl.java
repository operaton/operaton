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
package org.operaton.spin.plugin.variable.type.impl;

import java.io.Serial;

import org.operaton.spin.plugin.variable.SpinValues;
import org.operaton.spin.plugin.variable.type.XmlValueType;
import org.operaton.spin.plugin.variable.value.SpinValue;
import org.operaton.spin.plugin.variable.value.builder.XmlValueBuilder;
import org.operaton.spin.xml.SpinXmlElement;

/**
 * @author Roman Smirnov
 *
 */
public class XmlValueTypeImpl extends SpinValueTypeImpl implements XmlValueType {

  @Serial private static final long serialVersionUID = 1L;

  public XmlValueTypeImpl() {
    super(TYPE_NAME);
  }

  @Override
  protected XmlValueBuilder createValue(SpinValue value) {
    return SpinValues.xmlValue((SpinXmlElement) value);
  }

  @Override
  protected XmlValueBuilder createValueFromSerialized(String value) {
    return SpinValues.xmlValue(value);
  }

}
