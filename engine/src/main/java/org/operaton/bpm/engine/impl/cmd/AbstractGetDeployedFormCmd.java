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
package org.operaton.bpm.engine.impl.cmd;

import java.io.InputStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.exception.DeploymentResourceNotFoundException;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.form.FormData;
import org.operaton.bpm.engine.form.OperatonFormRef;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.util.EnsureUtil;
import org.operaton.bpm.engine.repository.OperatonFormDefinition;

import static java.util.Objects.requireNonNull;

/**
 *
 * @author Anna Pazola
 *
 */
public abstract @NullMarked class AbstractGetDeployedFormCmd implements Command<InputStream> {

  protected static final String EMBEDDED_KEY = "embedded:";
  protected static final String OPERATON_FORMS_KEY = "operaton-forms:";
  protected static final String CAMUNDA_FORMS_KEY = "camunda-forms:";
  protected static final int EMBEDDED_KEY_LENGTH = EMBEDDED_KEY.length();
  protected static final int CAMUNDA_FORMS_KEY_LENGTH = CAMUNDA_FORMS_KEY.length();
  protected static final int OPERATON_FORMS_KEY_LENGTH = OPERATON_FORMS_KEY.length();

  protected static final String DEPLOYMENT_KEY = "deployment:";
  protected static final int DEPLOYMENT_KEY_LENGTH = DEPLOYMENT_KEY.length();

  private @Nullable CommandContext commandContext;

  @Override
  public @Nullable InputStream execute(final CommandContext commandContext) {
    this.commandContext = commandContext;
    checkAuthorization();

    final FormData formData = requireNonNull(getFormData());
    String formKey = formData.getFormKey();
    OperatonFormRef operatonFormRef = formData.getOperatonFormRef();

    if (formKey != null) {
      return getResourceForFormKey(formData, formKey);
    } else if(operatonFormRef != null && operatonFormRef.getKey() != null) {
      return getResourceForOperatonFormRef(operatonFormRef, formData.getDeploymentId());
    } else {
      throw new BadUserRequestException("One of the attributes 'formKey' and 'operaton:formRef' must be supplied but none were set.");
    }
  }

  protected final CommandContext getCommandContext() {
    return EnsureUtil.ensureActiveCommandContext(commandContext);
  }

  protected @Nullable InputStream getResourceForFormKey(FormData formData, String formKey) {
    String resourceName = formKey;

    if (resourceName.startsWith(EMBEDDED_KEY)) {
      resourceName = resourceName.substring(EMBEDDED_KEY_LENGTH, resourceName.length());
    } else if (resourceName.startsWith(OPERATON_FORMS_KEY)) {
      resourceName = resourceName.substring(OPERATON_FORMS_KEY_LENGTH, resourceName.length());
    } else if (resourceName.startsWith(CAMUNDA_FORMS_KEY)) {
      resourceName = resourceName.substring(CAMUNDA_FORMS_KEY_LENGTH, resourceName.length());
    }

    if (!resourceName.startsWith(DEPLOYMENT_KEY)) {
      throw new BadUserRequestException("The form key '%s' does not reference a deployed form.".formatted(formKey));
    }

    resourceName = resourceName.substring(DEPLOYMENT_KEY_LENGTH, resourceName.length());

    return getDeploymentResource(formData.getDeploymentId(), resourceName);
  }

  protected @Nullable InputStream getResourceForOperatonFormRef(OperatonFormRef operatonFormRef,
      String deploymentId) {
    OperatonFormDefinition definition = getCommandContext().runWithoutAuthorization(
        new GetOperatonFormDefinitionCmd(operatonFormRef, deploymentId));

    if (definition == null) {
      throw new NotFoundException("No Operaton Form Definition was found for Operaton Form Ref: %s".formatted(operatonFormRef));
    }

    return getDeploymentResource(definition.getDeploymentId(), definition.getResourceName());
  }

  protected @Nullable InputStream getDeploymentResource(String deploymentId, String resourceName) {
    GetDeploymentResourceCmd getDeploymentResourceCmd = new GetDeploymentResourceCmd(deploymentId, resourceName);
    try {
      return getCommandContext().runWithoutAuthorization(getDeploymentResourceCmd);
    } catch (DeploymentResourceNotFoundException e) {
      throw new NotFoundException("The form with the resource name '%s' cannot be found in deployment with id %s".formatted(resourceName, deploymentId), e);
    }
  }

  protected abstract @Nullable FormData getFormData();

  protected abstract void checkAuthorization();

}
