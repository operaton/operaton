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

import static org.operaton.bpm.engine.variable.Variables.emptyVariableContext;
import static org.mockito.Mockito.*;

import javax.script.CompiledScript;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.DmnExpressionImpl;
import org.operaton.bpm.dmn.engine.impl.el.DefaultScriptEngineResolver;
import org.operaton.bpm.dmn.engine.impl.evaluation.ExpressionEvaluationHandler;
import org.operaton.bpm.dmn.engine.impl.spi.el.ElExpression;
import org.operaton.bpm.dmn.engine.impl.spi.el.ElProvider;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

/**
 * @author Daniel Meyer
 *
 */
public class ExpressionCachingTest {

  protected ExpressionEvaluationHandler expressionEvaluationHandler;
  protected ElProvider elProviderSpy;
  private GroovyScriptEngineImpl scriptEngineSpy;

    /**
   * Sets up the necessary configurations for the test by mocking objects and initializing the DefaultDmnEngineConfiguration.
   * @throws ScriptException if an error occurs during script execution
   */
  @Before
  public void setup() throws ScriptException {
    ScriptEngineManager scriptEngineManager = mock(ScriptEngineManager.class);

    scriptEngineSpy = mock(GroovyScriptEngineImpl.class);
    when(scriptEngineSpy.createBindings()).thenReturn(new SimpleBindings());
    when(scriptEngineSpy.compile(anyString())).thenReturn(mock(CompiledScript.class));
    when(scriptEngineManager.getEngineByName(anyString())).thenReturn(scriptEngineSpy);

    DefaultDmnEngineConfiguration configuration = new DefaultDmnEngineConfiguration();

    configuration.setScriptEngineResolver(new DefaultScriptEngineResolver(scriptEngineManager));
    configuration.init();


    elProviderSpy = spy(configuration.getElProvider());
    configuration.setElProvider(elProviderSpy);

    expressionEvaluationHandler = new ExpressionEvaluationHandler(configuration);
  }

    /**
   * This method tests the caching behavior of compiled scripts in a script evaluation handler.
   */
  @Test
  public void testCompiledScriptCaching() throws ScriptException {

    /**
   * Test method to verify caching behavior of EL expressions.
   */
  @Test
  public void testElExpressionCaching() {

    // given
    DmnExpressionImpl expression = createExpression("1 > 2", "juel");

    // when
    expressionEvaluationHandler.evaluateExpression("juel", expression, emptyVariableContext());

    // then
    InOrder inOrder = inOrder(expression, elProviderSpy);
    inOrder.verify(expression, atLeastOnce()).getCachedExpression();
    inOrder.verify(elProviderSpy, times(1)).createExpression(anyString());
    inOrder.verify(expression, times(1)).setCachedExpression(any(ElExpression.class));

    // when (2)
    expressionEvaluationHandler.evaluateExpression("juel", expression, emptyVariableContext());

    // then (2)
    inOrder.verify(expression, atLeastOnce()).getCachedExpression();
    inOrder.verify(elProviderSpy, times(0)).createExpression(anyString());
  }

    /**
   * Creates a new DmnExpressionImpl object with the given text and language.
   * 
   * @param text the text of the expression
   * @param language the language of the expression
   * @return a new DmnExpressionImpl object
   */
  private DmnExpressionImpl createExpression(String text, String language) {
    DmnExpressionImpl expression = spy(new DmnExpressionImpl());
    expression.setExpression(text);
    expression.setExpressionLanguage(language);
    return expression;
  }

}
