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
package org.operaton.bpm.integrationtest.functional.spin.dataformat;

import java.io.IOException;

import org.operaton.bpm.integrationtest.functional.spin.XmlSerializable;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * @author Svetlana Dorokhova.
 */
public class XmlSerializableJsonDeserializer extends JsonDeserializer<XmlSerializable> {

  @Override
  public XmlSerializable deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    XmlSerializable xmlSerializable = new XmlSerializable();
    xmlSerializable.setProperty(p.getValueAsString());
    return xmlSerializable;
  }

}
