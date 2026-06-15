/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.standalone.scripting;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.core.variable.scope.AbstractVariableScope;
import org.operaton.bpm.engine.impl.scripting.engine.DefaultScriptEngineResolver;
import org.operaton.bpm.engine.impl.scripting.engine.Resolver;
import org.operaton.bpm.engine.impl.scripting.engine.ResolverFactory;
import org.operaton.bpm.engine.impl.scripting.engine.ScriptBindings;
import org.operaton.bpm.engine.impl.scripting.engine.ScriptBindingsFactory;
import org.operaton.bpm.engine.impl.scripting.engine.ScriptingEngines;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ScriptingEngines}, covering the non-cachable engine
 * bindings path that prevents GraalJS memory leaks (see issue #2761).
 */
class ScriptingEnginesTest {

  @Test
  void shouldIdentifyNonCachableEngineWhenThreadingIsNull() {
    // given a script engine whose factory reports THREADING=null
    ScriptEngine nonCachableEngine = mockEngineWithThreading(null);

    // then
    assertThat(ScriptingEngines.isCachableEngine(nonCachableEngine)).isFalse();
  }

  @Test
  void shouldIdentifyCachableEngineWhenThreadingIsSet() {
    // given a script engine whose factory reports THREADING=MULTITHREADED
    ScriptEngine cachableEngine = mockEngineWithThreading("MULTITHREADED");

    // then
    assertThat(ScriptingEngines.isCachableEngine(cachableEngine)).isTrue();
  }

  @Test
  void createBindingsShouldReturnEngineScopeForNonCachableEngine() {
    // given a non-cachable script engine (THREADING=null) with own ENGINE_SCOPE bindings
    ScriptEngine nonCachableEngine = mockEngineWithThreading(null);
    Bindings engineScopeBindings = new SimpleBindings();
    when(nonCachableEngine.getBindings(ScriptContext.ENGINE_SCOPE)).thenReturn(engineScopeBindings);

    // given a scripting engines instance with empty resolver list (avoids variable scope lookups)
    ScriptingEngines engines = new ScriptingEngines(
        new ScriptBindingsFactory(List.of()),
        new DefaultScriptEngineResolver(new ScriptEngineManager()));
    AbstractVariableScope variableScope = mock(AbstractVariableScope.class);

    // when
    Bindings result = engines.createBindings(nonCachableEngine, variableScope);

    // then the ENGINE_SCOPE bindings are returned directly (not wrapped in ScriptBindings)
    assertThat(result).isSameAs(engineScopeBindings);
  }

  @Test
  void createBindingsShouldPopulateResolverValuesForNonCachableEngine() {
    // given a non-cachable engine
    ScriptEngine nonCachableEngine = mockEngineWithThreading(null);
    Bindings engineScopeBindings = new SimpleBindings();
    when(nonCachableEngine.getBindings(ScriptContext.ENGINE_SCOPE)).thenReturn(engineScopeBindings);

    // given a custom resolver that exposes a variable
    ScriptBindingsFactory factory = createBindingsFactoryWithVariable("myVar", "myValue");
    DefaultScriptEngineResolver resolver = new DefaultScriptEngineResolver(new ScriptEngineManager());
    ScriptingEngines engines = new ScriptingEngines(factory, resolver);

    AbstractVariableScope variableScope = mock(AbstractVariableScope.class);

    // when
    Bindings result = engines.createBindings(nonCachableEngine, variableScope);

    // then the resolver variable is present in the returned bindings
    assertThat(result).containsEntry("myVar", "myValue");
  }

  @Test
  void createBindingsShouldReturnScriptBindingsForCachableEngine() {
    // given a cachable script engine (THREADING=MULTITHREADED)
    ScriptEngine cachableEngine = mockEngineWithThreading("MULTITHREADED");
    when(cachableEngine.createBindings()).thenReturn(new SimpleBindings());

    ScriptingEngines engines = new ScriptingEngines(
        new ScriptBindingsFactory(List.of()),
        new DefaultScriptEngineResolver(new ScriptEngineManager()));
    AbstractVariableScope variableScope = mock(AbstractVariableScope.class);

    // when
    Bindings result = engines.createBindings(cachableEngine, variableScope);

    // then the result is a ScriptBindings (not the raw ENGINE_SCOPE)
    assertThat(result).isInstanceOf(ScriptBindings.class);
  }

  // --- helpers ---

  private static ScriptEngine mockEngineWithThreading(String threading) {
    ScriptEngineFactory factory = mock(ScriptEngineFactory.class);
    when(factory.getParameter("THREADING")).thenReturn(threading);
    ScriptEngine engine = mock(ScriptEngine.class);
    when(engine.getFactory()).thenReturn(factory);
    return engine;
  }

  private static ScriptBindingsFactory createBindingsFactoryWithVariable(String key, Object value) {
    ResolverFactory resolverFactory = variableScope -> new Resolver() {
      @Override
      public boolean containsKey(Object k) {
        return key.equals(k);
      }

      @Override
      public Object get(Object k) {
        return key.equals(k) ? value : null;
      }

      @Override
      public java.util.Set<String> keySet() {
        return Collections.singleton(key);
      }
    };
    return new ScriptBindingsFactory(List.of(resolverFactory));
  }
}
