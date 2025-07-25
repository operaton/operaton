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
package org.operaton.bpm.integrationtest.functional.spin;

import static org.operaton.spin.Spin.JSON;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.spin.json.SpinJsonNode;

/**
 * Created by hawky4s on 04.05.15.
 */
public class SpinJsonPathDelegate implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    String  json = "{\"child\": [{\"id\": 1,\"name\": \"Lucy\",\"sex\": \"female\"},{\"id\": 2,\"name\": \"Tracy\",\"sex\": \"female\"}],\"number\": 1,\"boolean\": true}";
    SpinJsonNode spinJsonNode = JSON(json).jsonPath("$.child[0]").element();

    if (spinJsonNode == null) {
      throw new RuntimeException("Unable to evalute jsonpath");
    }
  }

}
