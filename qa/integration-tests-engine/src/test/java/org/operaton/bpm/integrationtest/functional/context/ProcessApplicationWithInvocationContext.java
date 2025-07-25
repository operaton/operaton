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
package org.operaton.bpm.integrationtest.functional.context;

import java.util.concurrent.Callable;

import org.operaton.bpm.application.InvocationContext;
import org.operaton.bpm.application.ProcessApplication;
import org.operaton.bpm.application.ProcessApplicationExecutionException;

@ProcessApplication("app")
// Using fully-qualified class name instead of import statement to allow for automatic Jakarta transformation
public class ProcessApplicationWithInvocationContext extends org.operaton.bpm.application.impl.JakartaServletProcessApplication {

  private static InvocationContext invocationContext = null;

  @Override
  public <T> T execute(Callable<T> callable, InvocationContext invocationContext) throws ProcessApplicationExecutionException {
    synchronized (ProcessApplicationWithInvocationContext.class) {
      ProcessApplicationWithInvocationContext.invocationContext = invocationContext;
    }

    return execute(callable);
  }

  public static synchronized InvocationContext getInvocationContext() {
    return ProcessApplicationWithInvocationContext.invocationContext;
  }

  public static synchronized void clearInvocationContext() {
    ProcessApplicationWithInvocationContext.invocationContext = null;
  }

  @Override
  public void undeploy() {
    clearInvocationContext();
    super.undeploy();
  }

}
