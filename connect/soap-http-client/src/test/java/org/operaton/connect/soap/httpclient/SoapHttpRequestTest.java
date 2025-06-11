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
package org.operaton.connect.soap.httpclient;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.connect.Connectors;
import org.operaton.connect.httpclient.soap.SoapHttpConnector;
import org.operaton.connect.httpclient.soap.SoapHttpRequest;

class SoapHttpRequestTest {

  private SoapHttpConnector connector;

  @BeforeEach
  void createRequest() {
    connector = Connectors.getConnector(SoapHttpConnector.ID);
  }

  @Test
  void shouldSetSoapAction() {
    SoapHttpRequest request = connector.createRequest().soapAction("test");
    assertThat(request.getSoapAction()).isEqualTo("test");
  }

}
