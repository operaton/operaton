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
package org.operaton.bpm.engine.cdi;

import java.util.Map;
import java.util.logging.Logger;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.operaton.bpm.engine.cdi.annotation.ProcessVariable;
import org.operaton.bpm.engine.cdi.annotation.ProcessVariableLocal;
import org.operaton.bpm.engine.cdi.annotation.ProcessVariableLocalTyped;
import org.operaton.bpm.engine.cdi.annotation.ProcessVariableTyped;
import org.operaton.bpm.engine.cdi.impl.ProcessVariableLocalMap;
import org.operaton.bpm.engine.cdi.impl.ProcessVariableMap;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * Allows to access the process variables of a managed process instance.
 * A process instance can be managed, using the {@link BusinessProcess}-bean.
 *
 * @author Daniel Meyer
 */
@Dependent
public class ProcessVariables {

  private final Logger logger = Logger.getLogger(ProcessVariables.class.getName());

  @Inject private BusinessProcess businessProcess;
  @Inject private ProcessVariableMap processVariableMap;
  @Inject private ProcessVariableLocalMap processVariableLocalMap;

  protected String getVariableName(InjectionPoint ip) {
    String variableName = ip.getAnnotated().getAnnotation(ProcessVariable.class).value();
    if (variableName.isEmpty()) {
      variableName = ip.getMember().getName();
    }
    return variableName;
  }

  protected String getVariableTypedName(InjectionPoint ip) {
    String variableName = ip.getAnnotated().getAnnotation(ProcessVariableTyped.class).value();
    if (variableName.isEmpty()) {
      variableName = ip.getMember().getName();
    }
    return variableName;
  }

  @Produces
  @ProcessVariable
  protected Object getProcessVariable(InjectionPoint ip) {
    String processVariableName = getVariableName(ip);

    logger.fine(() -> "Getting process variable '%s' from ProcessInstance[%s].".formatted(processVariableName, businessProcess.getProcessInstanceId()));

    return businessProcess.getVariable(processVariableName);
  }

  /**
   */
  @Produces
  @ProcessVariableTyped
  protected TypedValue getProcessVariableTyped(InjectionPoint ip) {
    String processVariableName = getVariableTypedName(ip);

    logger.fine(() -> "Getting typed process variable '%s' from ProcessInstance[%s].".formatted(processVariableName, businessProcess.getProcessInstanceId()));

    return businessProcess.getVariableTyped(processVariableName);
  }

  @Produces
  @Named
  protected Map<String, Object> processVariables() {
    return processVariableMap;
  }

  /**
   */
  @Produces
  @Named
  protected VariableMap processVariableMap() {
    return processVariableMap;
  }

  protected String getVariableLocalName(InjectionPoint ip) {
    String variableName = ip.getAnnotated().getAnnotation(ProcessVariableLocal.class).value();
    if (variableName.isEmpty()) {
      variableName = ip.getMember().getName();
    }
    return variableName;
  }

  protected String getVariableLocalTypedName(InjectionPoint ip) {
    String variableName = ip.getAnnotated().getAnnotation(ProcessVariableLocalTyped.class).value();
    if (variableName.isEmpty()) {
      variableName = ip.getMember().getName();
    }
    return variableName;
  }

  @Produces
  @ProcessVariableLocal
  protected Object getProcessVariableLocal(InjectionPoint ip) {
    String processVariableName = getVariableLocalName(ip);

    logger.fine(() -> "Getting local process variable '%s' from ProcessInstance[%s].".formatted(processVariableName, businessProcess.getProcessInstanceId()));

    return businessProcess.getVariableLocal(processVariableName);
  }

  /**
   */
  @Produces
  @ProcessVariableLocalTyped
  protected TypedValue getProcessVariableLocalTyped(InjectionPoint ip) {
    String processVariableName = getVariableLocalTypedName(ip);

    logger.fine(() -> "Getting local typed process variable '%s' from ProcessInstance[%s].".formatted(processVariableName, businessProcess.getProcessInstanceId()));

    return businessProcess.getVariableLocalTyped(processVariableName);
  }

  @Produces
  @Named
  protected Map<String, Object> processVariablesLocal() {
    return processVariableLocalMap;
  }

  /**
   */
  @Produces
  @Named
  protected VariableMap processVariableMapLocal() {
    return processVariableLocalMap;
  }


}
