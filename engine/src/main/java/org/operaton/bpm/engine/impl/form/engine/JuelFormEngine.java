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
package org.operaton.bpm.engine.impl.form.engine;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.form.FormData;
import org.operaton.bpm.engine.form.StartFormData;
import org.operaton.bpm.engine.form.TaskFormData;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.delegate.ScriptInvocation;
import org.operaton.bpm.engine.impl.persistence.entity.ResourceEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.impl.scripting.ExecutableScript;
import org.operaton.bpm.engine.impl.scripting.ScriptFactory;
import org.operaton.bpm.engine.impl.scripting.engine.ScriptingEngines;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * @author Tom Baeyens
 */
public class JuelFormEngine implements FormEngine {

  @Override
  public String getName() {
    return "juel";
  }

  @Override
  public Object renderStartForm(StartFormData startForm) {
    if (startForm.getFormKey()==null) {
      return null;
    }
    String formTemplateString = getFormTemplateString(startForm, startForm.getFormKey());
    return executeScript(formTemplateString, null);
  }


  @Override
  public Object renderTaskForm(TaskFormData taskForm) {
    if (taskForm.getFormKey()==null) {
      return null;
    }
    String formTemplateString = getFormTemplateString(taskForm, taskForm.getFormKey());
    TaskEntity task = (TaskEntity) taskForm.getTask();
    return executeScript(formTemplateString, task.getExecution());
  }

  protected Object executeScript(String scriptSrc, VariableScope scope) {
    ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
    ScriptFactory scriptFactory = processEngineConfiguration.getScriptFactory();
    ExecutableScript script = scriptFactory.createScriptFromSource(ScriptingEngines.DEFAULT_SCRIPTING_LANGUAGE, scriptSrc);

    ScriptInvocation invocation = new ScriptInvocation(script, scope);
    try {
      processEngineConfiguration
        .getDelegateInterceptor()
        .handleInvocation(invocation);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ProcessEngineException(e);
    }

    return invocation.getInvocationResult();
  }

  protected String getFormTemplateString(FormData formInstance, String formKey) {
    String deploymentId = formInstance.getDeploymentId();

    ResourceEntity resourceStream = Context
      .getCommandContext()
      .getResourceManager()
      .findResourceByDeploymentIdAndResourceName(deploymentId, formKey);

    ensureNotNull("Form with formKey '%s' does not exist".formatted(formKey), "resourceStream", resourceStream);

    byte[] resourceBytes = resourceStream.getBytes();
    return new String(resourceBytes, UTF_8);
  }
}
