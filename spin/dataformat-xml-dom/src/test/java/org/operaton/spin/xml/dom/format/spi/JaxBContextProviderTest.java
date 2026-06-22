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
package org.operaton.spin.xml.dom.format.spi;

import java.io.StringWriter;
import jakarta.xml.bind.JAXBException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.operaton.spin.DataFormats;
import org.operaton.spin.Spin;
import org.operaton.spin.impl.xml.dom.format.DomXmlDataFormat;
import org.operaton.spin.xml.SpinXmlDataFormatException;
import org.operaton.spin.xml.SpinXmlElement;
import org.operaton.spin.xml.mapping.Customer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
class JaxBContextProviderTest {

  /**
   * This test uses a dataformat with a JAXBContext that cannot resolve any classes.
   * Thus, it is expected that mapping an object to XML using this context fails.
   */
  @Test
  void customJaxBProvider() {

    Object objectToConvert = new Customer();

    // using the default jaxb context provider for conversion should work
    SpinXmlElement spinWrapper = Spin.XML(objectToConvert);
    spinWrapper.writeToWriter(new StringWriter());

    // using the custom jaxb context provider should fail with a JAXBException
    ((DomXmlDataFormat) DataFormats.xml()).setJaxBContextProvider(new EmptyContextProvider());

    // when/then
    assertThatThrownBy(() -> Spin.XML(objectToConvert))
      .withFailMessage("expected a JAXBException in the cause hierarchy of the spin exception")
      .isInstanceOf(SpinXmlDataFormatException.class)
      .hasRootCauseInstanceOf(JAXBException.class);
  }

  @AfterEach
  void tearDown() {
    // reset jaxb context provider
    ((DomXmlDataFormat) DataFormats.xml()).setJaxBContextProvider(DomXmlDataFormat.defaultJaxBContextProvider());
  }
}
