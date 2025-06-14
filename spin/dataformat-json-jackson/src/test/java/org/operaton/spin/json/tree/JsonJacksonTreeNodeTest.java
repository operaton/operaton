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
package org.operaton.spin.json.tree;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.operaton.spin.Spin.S;
import static org.operaton.spin.json.JsonTestConstants.EXAMPLE_JSON;

import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.spin.impl.json.jackson.JacksonJsonNode;

class JsonJacksonTreeNodeTest {

  protected JacksonJsonNode jsonNode;

  @BeforeEach
  void parseJson() {
    jsonNode = S(EXAMPLE_JSON);
  }

  @Test
  void canWriteToString() {
    assertThatJson(jsonNode.toString()).isEqualTo(EXAMPLE_JSON);
  }

  @Test
  void canWriteToWriter() {
    StringWriter writer = new StringWriter();
    jsonNode.writeToWriter(writer);
    String value = writer.toString();
    assertThatJson(value).isEqualTo(EXAMPLE_JSON);
  }

}
