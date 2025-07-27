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
package org.operaton.bpm.client.variable.impl.value;

import static org.operaton.bpm.client.variable.ClientValues.XML;

import org.operaton.bpm.client.variable.value.XmlValue;

import java.io.Serial;
import org.operaton.bpm.engine.variable.impl.value.PrimitiveTypeValueImpl;

public class XmlValueImpl extends PrimitiveTypeValueImpl<String> implements XmlValue {

  @Serial private static final long serialVersionUID = 1L;

  public XmlValueImpl(String value) {
    super(value, XML);
  }

  public XmlValueImpl(String value, boolean isTransient) {
    this(value);
    this.isTransient = isTransient;
  }


}
