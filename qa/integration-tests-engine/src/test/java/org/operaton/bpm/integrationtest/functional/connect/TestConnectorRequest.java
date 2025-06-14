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
package org.operaton.bpm.integrationtest.functional.connect;

import java.util.HashMap;
import java.util.Map;

import org.operaton.connect.spi.ConnectorRequest;

public class TestConnectorRequest implements ConnectorRequest<TestConnectorResponse> {

  protected Map<String, Object> requestParameters;

  @Override
  public void setRequestParameters(Map<String, Object> parameters) {
    requestParameters = parameters;
  }

  @Override
  public void setRequestParameter(String name, Object value) {
    if (requestParameters == null) {
      requestParameters = new HashMap<>();
    }
    requestParameters.put(name, value);
  }

  @Override
  public Map<String, Object> getRequestParameters() {
    return requestParameters;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getRequestParameter(String name) {
    return (V) requestParameters.get(name);
  }

  @Override
  public TestConnectorResponse execute() {
    TestConnectorResponse response = new TestConnectorResponse();
    if (requestParameters != null) {
      response.setResponseParameters(requestParameters);
    }
    return response;
  }

}
