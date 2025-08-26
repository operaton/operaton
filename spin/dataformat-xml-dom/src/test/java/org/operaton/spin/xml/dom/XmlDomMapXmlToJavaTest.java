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
package org.operaton.spin.xml.dom;

import org.junit.jupiter.api.Test;

import org.operaton.spin.xml.SpinXmlDataFormatException;
import org.operaton.spin.xml.SpinXmlElement;
import org.operaton.spin.xml.mapping.Order;

import static org.operaton.spin.Spin.XML;
import static org.operaton.spin.xml.XmlTestConstants.EXAMPLE_VALIDATION_XML;
import static org.operaton.spin.xml.XmlTestConstants.assertIsExampleOrder;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XmlDomMapXmlToJavaTest {

  @Test
  void shouldMapXmlObjectToJavaObject() {
    Order order = XML(EXAMPLE_VALIDATION_XML).mapTo(Order.class);
    assertIsExampleOrder(order);
  }

  @Test
  void shouldMapByCanonicalString() {
    Order order = XML(EXAMPLE_VALIDATION_XML).mapTo(Order.class.getCanonicalName());
    assertIsExampleOrder(order);
  }

  @Test
  void shouldFailForMalformedTypeString() {
    SpinXmlElement xmlElement = XML(EXAMPLE_VALIDATION_XML);
    assertThrows(SpinXmlDataFormatException.class, () -> xmlElement.mapTo("rubbish"));
  }
}
