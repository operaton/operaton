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
package org.operaton.bpm.dmn.engine.el;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.operaton.bpm.dmn.engine.impl.el.DefaultScriptEngineResolver;
import org.junit.Test;
import static org.assertj.core.api.Assertions.*;

/**
 * @author Daniel Meyer
 *
 */
public class DefaultScriptEngineResolverTest {

    /**
   * Tests the caching behavior of the script engine resolver by ensuring that the script engine is only created once
   * when requested multiple times for the same script language.
   */
  @Test
  public void testCacheScriptEngine() {
    String scriptLang = "hipster-script";

    ScriptEngineManager mockScriptEngineManager = mock(ScriptEngineManager.class);
    ScriptEngine hipsterScriptEngine = mock(ScriptEngine.class);
    DefaultScriptEngineResolver scriptEngineResolver = new DefaultScriptEngineResolver(mockScriptEngineManager);

    // given
    when(mockScriptEngineManager.getEngineByName(scriptLang)).thenReturn(hipsterScriptEngine);

    // when engine is requested twice
    ScriptEngine engine1 = scriptEngineResolver.getScriptEngineForLanguage(scriptLang);
    ScriptEngine engine2 = scriptEngineResolver.getScriptEngineForLanguage(scriptLang);

    // then it is only created once
    verify(mockScriptEngineManager, times(1)).getEngineByName(scriptLang);
    assertThat(engine1).isSameAs(hipsterScriptEngine);
    assertThat(engine2).isSameAs(engine1);
  }

}
