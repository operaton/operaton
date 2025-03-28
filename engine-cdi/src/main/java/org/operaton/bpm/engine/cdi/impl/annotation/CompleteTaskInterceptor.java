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
package org.operaton.bpm.engine.cdi.impl.annotation;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.operaton.bpm.engine.cdi.BusinessProcess;
import org.operaton.bpm.engine.cdi.ProcessEngineCdiException;
import org.operaton.bpm.engine.cdi.annotation.CompleteTask;

/**
 * {@link Interceptor} for handling the {@link CompleteTask}-Annotation
 *
 * @author Daniel Meyer
 */
@Interceptor
@CompleteTask
public class CompleteTaskInterceptor implements Serializable {

  private static final long serialVersionUID = 1L;

  @Inject BusinessProcess businessProcess;

  @AroundInvoke
  public Object invoke(InvocationContext ctx) throws Exception {
    try {
      Object result = ctx.proceed();

      CompleteTask completeTaskAnnotation = ctx.getMethod().getAnnotation(CompleteTask.class);
      boolean endConversation = completeTaskAnnotation.endConversation();
      businessProcess.completeTask(endConversation);

      return result;
    } catch (InvocationTargetException e) {
      throw new ProcessEngineCdiException("Error while completing task: "+e.getCause().getMessage(), e.getCause());
    }
  }

}
