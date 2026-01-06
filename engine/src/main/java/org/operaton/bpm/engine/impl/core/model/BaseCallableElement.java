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
package org.operaton.bpm.engine.impl.core.model;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.impl.core.variable.mapping.value.ParameterValueProvider;

public class BaseCallableElement {

  protected ParameterValueProvider definitionKeyValueProvider;
  protected CallableElementBinding binding;
  protected ParameterValueProvider versionValueProvider;
  protected ParameterValueProvider versionTagValueProvider;
  protected ParameterValueProvider tenantIdProvider;
  protected String deploymentId;

  public enum CallableElementBinding {
    LATEST("latest"),
    DEPLOYMENT("deployment"),
    VERSION("version"),
    VERSION_TAG("versionTag");

    private String value;

    CallableElementBinding(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  public String getDefinitionKey(VariableScope variableScope) {
    Object result = definitionKeyValueProvider.getValue(variableScope);

    if (result != null && !(result instanceof String)) {
      throw new ClassCastException("Cannot cast '%s' to String".formatted(result));
    }

    return (String) result;
  }

  public ParameterValueProvider getDefinitionKeyValueProvider() {
    return definitionKeyValueProvider;
  }

  public void setDefinitionKeyValueProvider(ParameterValueProvider definitionKey) {
    this.definitionKeyValueProvider = definitionKey;
  }

  public CallableElementBinding getBinding() {
    return binding;
  }

  public void setBinding(CallableElementBinding binding) {
    this.binding = binding;
  }

  public boolean isLatestBinding() {
    CallableElementBinding elementBinding = getBinding();
    return elementBinding == null || CallableElementBinding.LATEST.equals(elementBinding);
  }

  public boolean isDeploymentBinding() {
    return CallableElementBinding.DEPLOYMENT.equals(getBinding());
  }

  public boolean isVersionBinding() {
    return CallableElementBinding.VERSION.equals(getBinding());
  }

  public boolean isVersionTagBinding() {
    return CallableElementBinding.VERSION_TAG.equals(getBinding());
  }

  public Integer getVersion(VariableScope variableScope) {
    Object result = versionValueProvider.getValue(variableScope);

    if (result != null) {
      if (result instanceof String string) {
        return Integer.valueOf(string);
      } else if (result instanceof Integer integer) {
        return integer;
      } else {
        throw new ProcessEngineException("It is not possible to transform '%s' into an Integer.".formatted(result));
      }
    }

    return null;
  }

  public ParameterValueProvider getVersionValueProvider() {
    return versionValueProvider;
  }

  public void setVersionValueProvider(ParameterValueProvider version) {
    this.versionValueProvider = version;
  }

  public String getVersionTag(VariableScope variableScope) {
    Object result = versionTagValueProvider.getValue(variableScope);

    if (result != null) {
      if (result instanceof String string) {
        return string;
      } else {
        throw new ProcessEngineException("It is not possible to transform '%s' into a String.".formatted(result));
      }
    }

    return null;
  }


  public ParameterValueProvider getVersionTagValueProvider() {
    return versionTagValueProvider;
  }

  public void setVersionTagValueProvider(ParameterValueProvider version) {
    this.versionTagValueProvider = version;
  }

  public void setTenantIdProvider(ParameterValueProvider tenantIdProvider) {
    this.tenantIdProvider = tenantIdProvider;
  }

  public String getDeploymentId() {
    return deploymentId;
  }

  public void setDeploymentId(String deploymentId) {
    this.deploymentId = deploymentId;
  }

  public String getDefinitionTenantId(VariableScope variableScope, String defaultTenantId) {
    if (tenantIdProvider != null) {
      return (String) tenantIdProvider.getValue(variableScope);
    } else {
      return defaultTenantId;
    }
  }

  public ParameterValueProvider getTenantIdProvider() {
    return tenantIdProvider;
  }

  /**
   * @return true if any of the references that specify the callable element are non-literal and need to be resolved with
   * potential side effects to determine the process or case definition that is to be called.
   */
  public boolean hasDynamicReferences() {
    return (tenantIdProvider != null && tenantIdProvider.isDynamic())
      || definitionKeyValueProvider.isDynamic()
      || versionValueProvider.isDynamic()
      || versionTagValueProvider.isDynamic();
  }


}
