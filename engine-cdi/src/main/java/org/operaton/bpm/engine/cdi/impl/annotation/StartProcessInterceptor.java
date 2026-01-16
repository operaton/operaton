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
package org.operaton.bpm.engine.cdi.impl.annotation;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.cdi.BusinessProcess;
import org.operaton.bpm.engine.cdi.annotation.ProcessVariable;
import org.operaton.bpm.engine.cdi.annotation.ProcessVariableTyped;
import org.operaton.bpm.engine.cdi.annotation.StartProcess;
import org.operaton.bpm.engine.impl.util.ReflectUtil;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.impl.VariableMapImpl;

/**
 * implementation of the {@link StartProcess} annotation
 *
 * @author Daniel Meyer
 */
@Interceptor
@StartProcess
public class StartProcessInterceptor implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Inject BusinessProcess businessProcess;

  @AroundInvoke
  public Object invoke(InvocationContext ctx) throws Exception {
    try {
      Object result = ctx.proceed();

      StartProcess startProcessAnnotation = ctx.getMethod().getAnnotation(StartProcess.class);

      String key = startProcessAnnotation.value();

      Map<String, Object> variables = extractVariables(ctx);

      businessProcess.startProcessByKey(key, variables);

      return result;
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof Exception exception) {
        throw exception;
      } else {
        throw e;
      }
    } catch (Exception e) {
      throw new ProcessEngineException("Error while starting process using @StartProcess on method  '"+ctx.getMethod()+"': " + e.getMessage(), e);
    }
  }

  Map<String, Object> extractVariables(InvocationContext ctx) {
    VariableMap variables = new VariableMapImpl();
    for (Field field : ctx.getMethod().getDeclaringClass().getDeclaredFields()) {
      if (!field.isAnnotationPresent(ProcessVariable.class) && !field.isAnnotationPresent(ProcessVariableTyped.class)) {
        continue;
      }

      String fieldName;

      ProcessVariable processStartVariable = field.getAnnotation(ProcessVariable.class);
      if (processStartVariable != null) {
        fieldName = processStartVariable.value();

      } else {
        ProcessVariableTyped processStartVariableTyped = field.getAnnotation(ProcessVariableTyped.class);
        fieldName = processStartVariableTyped.value();
      }

      if (fieldName == null || fieldName.isEmpty()) {
        fieldName = field.getName();
      }

      Optional<Object> fieldValue = ReflectUtil.getFieldValue(field, ctx.getTarget());
      variables.put(fieldName, fieldValue.orElse(null));
    }

    return variables;
  }

}
