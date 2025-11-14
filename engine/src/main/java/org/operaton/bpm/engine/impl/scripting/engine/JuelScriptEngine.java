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
package org.operaton.bpm.engine.impl.scripting.engine;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import jakarta.el.*;
import jakarta.el.ELContext;

import javax.script.*;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.el.ExpressionFactoryResolver;
import org.operaton.bpm.engine.impl.util.ReflectUtil;
import org.operaton.bpm.impl.juel.SimpleResolver;


/**
 * ScriptEngine that used JUEL for script evaluation and compilation (JSR-223).
 *
 * Uses EL 1.1 if available, to resolve expressions. Otherwise it reverts to EL
 * 1.0, using {@link ExpressionFactoryResolver}.
 *
 * @author Frederik Heremans
 */
public class JuelScriptEngine extends AbstractScriptEngine {

  private ScriptEngineFactory scriptEngineFactory;
  private ExpressionFactory expressionFactory;

  public JuelScriptEngine(ScriptEngineFactory scriptEngineFactory) {
    this.scriptEngineFactory = scriptEngineFactory;
    // Resolve the ExpressionFactory
    expressionFactory = ExpressionFactoryResolver.resolveExpressionFactory();
  }

  public JuelScriptEngine() {
    this(null);
  }

  @Override
  public Object eval(String script, ScriptContext scriptContext) throws ScriptException {
    ValueExpression expr = parse(script, scriptContext);
    return evaluateExpression(expr, scriptContext);
  }

  @Override
  public Object eval(Reader reader, ScriptContext scriptContext) throws ScriptException {
    return eval(readFully(reader), scriptContext);
  }

  @Override
  public ScriptEngineFactory getFactory() {
    synchronized (this) {
      if (scriptEngineFactory == null) {
        scriptEngineFactory = new JuelScriptEngineFactory();
      }
    }
    return scriptEngineFactory;
  }

  @Override
  public Bindings createBindings() {
    return new SimpleBindings();
  }

  private Object evaluateExpression(ValueExpression expr, ScriptContext ctx) throws ScriptException {
    try {
      return expr.getValue(createElContext(ctx));
    } catch (ELException elexp) {
      throw new ScriptException(elexp);
    }
  }

  private ELResolver createElResolver() {
    CompositeELResolver compositeResolver = new CompositeELResolver();
    compositeResolver.add(new ArrayELResolver());
    compositeResolver.add(new ListELResolver());
    compositeResolver.add(new MapELResolver());
    compositeResolver.add(new ResourceBundleELResolver());
    compositeResolver.add(new BeanELResolver());
    return new SimpleResolver(compositeResolver);
  }

  private String readFully(Reader reader) throws ScriptException {
    char[] array = new char[8192];
    StringBuilder strBuffer = new StringBuilder();
    int count;
    try {
      while ((count = reader.read(array, 0, array.length)) > 0) {
        strBuffer.append(array, 0, count);
      }
    } catch (IOException exp) {
      throw new ScriptException(exp);
    }
    return strBuffer.toString();
  }

  private ValueExpression parse(String script, ScriptContext scriptContext) throws ScriptException {
    try {
      return expressionFactory.createValueExpression(createElContext(scriptContext), script, Object.class);
    } catch (ELException ele) {
      throw new ScriptException(ele);
    }
  }

  private ELContext createElContext(final ScriptContext scriptCtx) {
    // Check if the ELContext is already stored on the ScriptContext
    Object existingELCtx = scriptCtx.getAttribute("elcontext");
    if (existingELCtx instanceof ELContext elContext) {
      return elContext;
    }

    scriptCtx.setAttribute("context", scriptCtx, ScriptContext.ENGINE_SCOPE);

    // Built-in function are added to ScriptCtx
    scriptCtx.setAttribute("out:print", getPrintMethod(), ScriptContext.ENGINE_SCOPE);
    scriptCtx.setAttribute("lang:import", getImportMethod(), ScriptContext.ENGINE_SCOPE);

    ELContext elContext = new ELContext() {

      ELResolver resolver = createElResolver();
      VariableMapper varMapper = new ScriptContextVariableMapper(scriptCtx);
      FunctionMapper funcMapper = new ScriptContextFunctionMapper(scriptCtx);

      @Override
      public ELResolver getELResolver() {
        return resolver;
      }

      @Override
      public VariableMapper getVariableMapper() {
        return varMapper;
      }

      @Override
      public FunctionMapper getFunctionMapper() {
        return funcMapper;
      }
    };
    // Store the elcontext in the scriptContext to be able to reuse
    scriptCtx.setAttribute("elcontext", elContext, ScriptContext.ENGINE_SCOPE);
    return elContext;
  }

  private static Method getPrintMethod() {
    try {
      return JuelScriptEngine.class.getMethod("print", Object.class);
    } catch (Exception exp) {
      // Will never occur
      return null;
    }
  }

  private static Method getImportMethod() {
    try {
      return JuelScriptEngine.class.getMethod("importFunctions", ScriptContext.class, String.class, Object.class);
    } catch (Exception exp) {
      // Will never occur
      return null;
    }
  }

  public static void importFunctions(ScriptContext ctx, String namespace, Object obj) {
    Class< ? > clazz = null;
    if (obj instanceof Class<?> classObj) {
      clazz = classObj;
    } else if (obj instanceof String stringObj) {
      try {
        clazz = ReflectUtil.loadClass(stringObj);
      } catch (ProcessEngineException ae) {
        throw new ELException(ae);
      }
    } else {
      throw new ELException("Class or class name is missing");
    }
    Method[] methods = clazz.getMethods();
    for (Method m : methods) {
      int mod = m.getModifiers();
      if (Modifier.isStatic(mod) && Modifier.isPublic(mod)) {
        String name = namespace + ":" + m.getName();
        ctx.setAttribute(name, m, ScriptContext.ENGINE_SCOPE);
      }
    }
  }

  /**
   * ValueMapper that uses the ScriptContext to get variable values or value
   * expressions.
   *
   * @author Frederik Heremans
   */
  private class ScriptContextVariableMapper extends VariableMapper {

    private ScriptContext scriptContext;

    ScriptContextVariableMapper(ScriptContext scriptCtx) {
      this.scriptContext = scriptCtx;
    }

    @Override
    public ValueExpression resolveVariable(String variableName) {
      int scope = scriptContext.getAttributesScope(variableName);
      if (scope != -1) {
        Object value = scriptContext.getAttribute(variableName, scope);
        if (value instanceof ValueExpression valueExpression) {
          // Just return the existing ValueExpression
          return valueExpression;
        } else {
          // Create a new ValueExpression based on the variable value
          return expressionFactory.createValueExpression(value, Object.class);
        }
      }
      return null;
    }

    @Override
    public ValueExpression setVariable(String name, ValueExpression value) {
      ValueExpression previousValue = resolveVariable(name);
      scriptContext.setAttribute(name, value, ScriptContext.ENGINE_SCOPE);
      return previousValue;
    }
  }

  /**
   * FunctionMapper that uses the ScriptContext to resolve functions in EL.
   *
   * @author Frederik Heremans
   */
  private class ScriptContextFunctionMapper extends FunctionMapper {

    private ScriptContext scriptContext;

    ScriptContextFunctionMapper(ScriptContext ctx) {
      this.scriptContext = ctx;
    }

    private String getFullFunctionName(String prefix, String localName) {
      return prefix + ":" + localName;
    }

    @Override
    public Method resolveFunction(String prefix, String localName) {
      String functionName = getFullFunctionName(prefix, localName);
      int scope = scriptContext.getAttributesScope(functionName);
      if (scope != -1) {
        // Methods are added as variables in the ScriptScope
        Object attributeValue = scriptContext.getAttribute(functionName);
        return attributeValue instanceof Method method ? method : null;
      } else {
        return null;
      }
    }
  }

}
