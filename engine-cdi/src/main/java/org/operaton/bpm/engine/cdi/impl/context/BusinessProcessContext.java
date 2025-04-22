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
package org.operaton.bpm.engine.cdi.impl.context;

import java.lang.annotation.Annotation;
import java.util.logging.Logger;

import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import org.operaton.bpm.engine.cdi.BusinessProcess;
import org.operaton.bpm.engine.cdi.annotation.BusinessProcessScoped;
import org.operaton.bpm.engine.cdi.impl.util.ProgrammaticBeanLookup;

/**
 * Implementation of the BusinessProcessContext-scope.
 *
 * @author Daniel Meyer
 */
@SuppressWarnings("unchecked")
public class BusinessProcessContext implements Context {

  static final Logger logger = Logger.getLogger(BusinessProcessContext.class.getName());

  protected BeanManager beanManager;

  public BusinessProcessContext() {
  }

  public BusinessProcessContext(BeanManager beanManager) {
    this.beanManager = beanManager;
  }

  protected BusinessProcess getBusinessProcess() {
    return ProgrammaticBeanLookup.lookup(BusinessProcess.class, getBeanManager());
  }

  @Override
  public Class< ? extends Annotation> getScope() {
    return BusinessProcessScoped.class;
  }

  @Override
  public <T> T get(Contextual<T> contextual) {
    Bean<T> bean = (Bean<T>) contextual;
    String variableName = bean.getName();

    return get(variableName);
  }

  protected <T> T get(String variableName) {
    BusinessProcess businessProcess = getBusinessProcess();
    Object variable = businessProcess.getVariable(variableName);
    if (variable != null) {

      if(businessProcess.isAssociated()) {
        logger.fine(() -> String.format("Getting instance of bean '%s' from Execution[%s].", variableName, businessProcess.getExecutionId()));
      } else {
        logger.fine(() -> String.format("Getting instance of bean '%s' from transient bean store.", variableName));
      }

      return (T) variable;
    } else {
      return null;
    }
  }

  @Override
  public <T> T get(Contextual<T> contextual, CreationalContext<T> arg1) {
    Bean<T> bean = (Bean<T>) contextual;

    String variableName = bean.getName();
    T beanInstance = bean.create(arg1);

    return get(variableName, beanInstance);
  }

  protected  <T> T get(String variableName, T beanInstance) {
    BusinessProcess businessProcess = getBusinessProcess();
    Object variable = businessProcess.getVariable(variableName);
    if (variable != null) {

      if(businessProcess.isAssociated()) {
        logger.fine(() -> String.format("Getting instance of bean '%s' from Execution[%s].", variableName, businessProcess.getExecutionId()));
      } else {
        logger.fine(() -> String.format("Getting instance of bean '%s' from transient bean store.", variableName));
      }

      return (T) variable;
    } else {
      if(businessProcess.isAssociated()) {
        logger.fine(() -> String.format("Creating instance of bean '%s' in business process context representing Execution[%s].", variableName, businessProcess.getExecutionId()));
      } else {
        logger.fine(() -> String.format("Creating instance of bean '%s' in transient bean store.", variableName));
      }

      businessProcess.setVariable(variableName, beanInstance);
      return beanInstance;
    }
  }

  @Override
  public boolean isActive() {
    // we assume the business process is always 'active'. If no task/execution is
    // associated, temporary instances of @BusinessProcesScoped beans are cached in the
    // conversation / request
    return true;
  }

  protected BeanManager getBeanManager() {
    return beanManager;
  }

}
