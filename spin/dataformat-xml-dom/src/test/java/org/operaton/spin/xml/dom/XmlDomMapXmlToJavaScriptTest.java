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
package org.operaton.spin.xml.dom;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.operaton.spin.xml.XmlTestConstants.EXAMPLE_VALIDATION_XML_FILE_NAME;
import static org.operaton.spin.xml.XmlTestConstants.assertIsExampleOrder;

import org.junit.jupiter.api.Test;
import org.operaton.spin.impl.test.Script;
import org.operaton.spin.impl.test.ScriptTest;
import org.operaton.spin.impl.test.ScriptVariable;
import org.operaton.spin.xml.SpinXmlDataFormatException;
import org.operaton.spin.xml.mapping.Order;

/**
 * @author Stefan Hentschel.
 */
public abstract class XmlDomMapXmlToJavaScriptTest extends ScriptTest {

  @Test
  @Script
  @ScriptVariable(name = "input", file = EXAMPLE_VALIDATION_XML_FILE_NAME)
  public void shouldMapXmlToJava() {
    Order order = script.getVariable("order");
    assertIsExampleOrder(order);
  }

  @Test
  @Script(execute = false)
  @ScriptVariable(name = "input", file = EXAMPLE_VALIDATION_XML_FILE_NAME)
  public void shouldFailMappingMalformedTypeString() {
    assertThrows(SpinXmlDataFormatException.class, () ->
      failingWithException());
  }

}