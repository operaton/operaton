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
package org.operaton.bpm.engine.test.cmmn.handler.specification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.operaton.bpm.engine.delegate.BaseDelegateExecution;
import org.operaton.bpm.engine.delegate.DelegateListener;
import org.operaton.bpm.engine.impl.cmmn.listener.ScriptCaseExecutionListener;
import org.operaton.bpm.engine.impl.scripting.ExecutableScript;
import org.operaton.bpm.model.cmmn.CmmnModelInstance;
import org.operaton.bpm.model.cmmn.instance.operaton.OperatonCaseExecutionListener;
import org.operaton.bpm.model.cmmn.instance.operaton.OperatonScript;

public class ScriptExecutionListenerSpec extends AbstractExecutionListenerSpec {

  //could be configurable
  protected static final String SCRIPT_FORMAT = "org.operaton.bpm.test.caseexecutionlistener.ABC";

  public ScriptExecutionListenerSpec(String eventName) {
    super(eventName);
  }

  @Override
  protected void configureCaseExecutionListener(CmmnModelInstance modelInstance, OperatonCaseExecutionListener listener) {
    OperatonScript script = SpecUtil.createElement(modelInstance, listener, null, OperatonScript.class);
    String scriptValue = "${myScript}";
    script.setOperatonScriptFormat(SCRIPT_FORMAT);
    script.setTextContent(scriptValue);
  }

  @Override
  public void verifyListener(DelegateListener<? extends BaseDelegateExecution> listener) {
    assertTrue(listener instanceof ScriptCaseExecutionListener);

    ScriptCaseExecutionListener scriptListener = (ScriptCaseExecutionListener) listener;
    ExecutableScript executableScript = scriptListener.getScript();
    assertNotNull(executableScript);
    assertEquals(SCRIPT_FORMAT, executableScript.getLanguage());
  }

}
