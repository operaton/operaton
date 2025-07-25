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
package org.operaton.spin.javascript.json.tree;

import org.operaton.spin.impl.test.ScriptEngine;
import org.operaton.spin.json.tree.JsonTreeEditListPropertyScriptTest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Stefan Hentschel
 *
 */
@ScriptEngine("graal.js")
class JsonTreeEditListPropertyJavascriptTest extends JsonTreeEditListPropertyScriptTest {

  @Test
  @Disabled("Ignored since javascript implementation changed, see: https://app.camunda.com/jira/browse/CAM-3612")
  @Override
  public void shouldFailInsertAtWithWrongObject() {
    super.shouldFailInsertAtWithWrongObject();
  }

  @Test
  @Disabled("Ignored since javascript implementation changed, see: https://app.camunda.com/jira/browse/CAM-3612")
  @Override
  public void shouldFailInsertWrongObjectAfterSearchObject() {
    super.shouldFailInsertWrongObjectAfterSearchObject();
  }

  @Test
  @Disabled("Ignored since javascript implementation changed, see: https://app.camunda.com/jira/browse/CAM-3612")
  @Override
  public void shouldFailAppendWrongNode() {
    super.shouldFailAppendWrongNode();
  }

  @Test
  @Disabled("Ignored since javascript implementation changed, see: https://app.camunda.com/jira/browse/CAM-3612")
  @Override
  public void shouldFailInsertWrongObjectBeforeSearchObject() {
    super.shouldFailInsertWrongObjectBeforeSearchObject();
  }
}
